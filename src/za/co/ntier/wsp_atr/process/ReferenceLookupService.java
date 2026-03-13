package za.co.ntier.wsp_atr.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MRefTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Cached reference lookup service for import/validation processes.
 *
 * Intended for repeated lookups of relatively small reference tables.
 *
 * Supports:
 * - DisplayType.Table
 * - DisplayType.TableDir
 * - DisplayType.Search
 *
 * Lookup strategy:
 * - if useValueForRef = true, prefer Value, fallback to Name
 * - if useValueForRef = false, prefer Name, fallback to Value
 *
 * Cache scope:
 * - per ReferenceLookupService instance
 *
 * Normalization:
 * - trim + upper-case
 */
public class ReferenceLookupService {

    private static final CLogger log = CLogger.getCLogger(ReferenceLookupService.class);

    /**
     * Cache:
     *   key   = tableName|idColumn|lookupColumn|clientId
     *   value = normalized lookup text -> record id
     */
    private final Map<String, Map<String, Integer>> lookupCache = new HashMap<>();

    /**
     * Cache of AD metadata table columns:
     *   key   = tableName
     *   value = set of upper-case column names
     */
    private final Map<String, Set<String>> tableColumnsCache = new HashMap<>();

    /**
     * Cache of resolved reference metadata per AD_Column_ID and lookup mode.
     * key = AD_Column_ID|useValueForRef
     */
    private final Map<String, RefInfo> refInfoCache = new HashMap<>();

    public void clearCache() {
        lookupCache.clear();
        tableColumnsCache.clear();
        refInfoCache.clear();
    }

    /**
     * Optional preload if you want to warm the cache before row processing.
     */
    public void preload(Properties ctx, Collection<LookupSpec> specs, String trxName) {
        if (specs == null || specs.isEmpty()) {
            return;
        }

        for (LookupSpec spec : specs) {
            if (spec == null || spec.column == null) {
                continue;
            }

            int ref = spec.column.getAD_Reference_ID();
            boolean isRef = (ref == DisplayType.Table
                    || ref == DisplayType.TableDir
                    || ref == DisplayType.Search);

            if (!isRef) {
                continue;
            }

            RefInfo info = resolveRefInfo(ctx, spec.column, spec.useValueForRef, trxName);
            ensureLookupMapLoaded(ctx, info, trxName);
        }
    }

    /**
     * Main method used by validators/importers.
     */
    public Integer tryResolveRefId(Properties ctx,
                                   MColumn column,
                                   String rawValue,
                                   boolean useValueForRef,
                                   String trxName) {
        if (column == null || Util.isEmpty(rawValue, true)) {
            return null;
        }

        int ref = column.getAD_Reference_ID();
        boolean isRef = (ref == DisplayType.Table
                || ref == DisplayType.TableDir
                || ref == DisplayType.Search);

        if (!isRef) {
            return null;
        }

        RefInfo info = resolveRefInfo(ctx, column, useValueForRef, trxName);
        Map<String, Integer> lookupMap = ensureLookupMapLoaded(ctx, info, trxName);

        return lookupMap.get(normalize(rawValue));
    }

    /**
     * Older style helper if you need direct lookup by table/columns.
     */
    public Integer findIdByColumn(Properties ctx,
                                  String tableName,
                                  String idColumn,
                                  String lookupColumn,
                                  String rawValue,
                                  String trxName) {
        if (Util.isEmpty(tableName, true)
                || Util.isEmpty(idColumn, true)
                || Util.isEmpty(lookupColumn, true)
                || Util.isEmpty(rawValue, true)) {
            return null;
        }

        RefInfo info = new RefInfo();
        info.tableName = tableName;
        info.idColumn = idColumn;
        info.lookupColumn = lookupColumn;
        info.hasIsActive = hasColumn(tableName, "IsActive", trxName);
        info.hasADClientID = hasColumn(tableName, "AD_Client_ID", trxName);

        Map<String, Integer> lookupMap = ensureLookupMapLoaded(ctx, info, trxName);
        return lookupMap.get(normalize(rawValue));
    }

    private Map<String, Integer> ensureLookupMapLoaded(Properties ctx, RefInfo info, String trxName) {
        String cacheKey = buildLookupCacheKey(ctx, info);
        Map<String, Integer> map = lookupCache.get(cacheKey);
        if (map != null) {
            return map;
        }

        map = loadLookupMap(ctx, info, trxName);
        lookupCache.put(cacheKey, map);
        return map;
    }

    private String buildLookupCacheKey(Properties ctx, RefInfo info) {
        int clientId = info.hasADClientID ? Env.getAD_Client_ID(ctx) : -1;
        return safe(info.tableName) + "|"
                + safe(info.idColumn) + "|"
                + safe(info.lookupColumn) + "|"
                + clientId;
    }

    private RefInfo resolveRefInfo(Properties ctx,
                                   MColumn column,
                                   boolean useValueForRef,
                                   String trxName) {
        String refInfoKey = column.getAD_Column_ID() + "|" + useValueForRef;
        RefInfo cached = refInfoCache.get(refInfoKey);
        if (cached != null) {
            return cached;
        }

        int ref = column.getAD_Reference_ID();
        RefInfo info;

        if (ref == DisplayType.Table || ref == DisplayType.Search) {
            info = resolveFromRefTable(ctx, column, useValueForRef, trxName);
        } else if (ref == DisplayType.TableDir) {
            info = resolveFromTableDir(ctx, column, useValueForRef, trxName);
        } else {
            throw new AdempiereException("Unsupported reference type for column "
                    + column.getColumnName() + " (AD_Column_ID=" + column.getAD_Column_ID() + ")");
        }

        if (info == null) {
            throw new AdempiereException("Could not resolve reference info for column "
                    + column.getColumnName() + " (AD_Column_ID=" + column.getAD_Column_ID() + ")");
        }

        info.hasIsActive = hasColumn(info.tableName, "IsActive", trxName);
        info.hasADClientID = hasColumn(info.tableName, "AD_Client_ID", trxName);

        refInfoCache.put(refInfoKey, info);
        return info;
    }

    /**
     * Resolve reference info for Table/Search using AD_Ref_Table metadata.
     */
    private RefInfo resolveFromRefTable(Properties ctx,
                                        MColumn column,
                                        boolean useValueForRef,
                                        String trxName) {
        int refValueId = column.getAD_Reference_Value_ID();
        if (refValueId <= 0) {
            throw new AdempiereException("AD_Reference_Value_ID missing for column "
                    + column.getColumnName() + " (AD_Column_ID=" + column.getAD_Column_ID() + ")");
        }

        MRefTable refTable = new MRefTable(ctx, refValueId, trxName);
        if (refTable.get_ID() <= 0) {
            throw new AdempiereException("AD_Ref_Table not found for AD_Reference_Value_ID=" + refValueId
                    + ", column=" + column.getColumnName());
        }

        RefInfo info = new RefInfo();
        info.tableName = loadTableNameById(refTable.getAD_Table_ID(), trxName);
        info.idColumn = loadColumnNameById(refTable.getAD_Key(), trxName);

        if (Util.isEmpty(info.tableName, true) || Util.isEmpty(info.idColumn, true)) {
            throw new AdempiereException("Invalid AD_Ref_Table metadata for column "
                    + column.getColumnName());
        }

        info.lookupColumn = chooseLookupColumn(info.tableName, useValueForRef, trxName);

        return info;
    }

    /**
     * Resolve reference info for TableDir using column name convention:
     * e.g. C_BPartner_ID -> table C_BPartner, key column C_BPartner_ID
     */
    private RefInfo resolveFromTableDir(Properties ctx,
                                        MColumn column,
                                        boolean useValueForRef,
                                        String trxName) {
        String idColumn = column.getColumnName();
        if (Util.isEmpty(idColumn, true) || !idColumn.toUpperCase().endsWith("_ID")) {
            throw new AdempiereException("Cannot derive TableDir target table from column "
                    + column.getColumnName() + " (AD_Column_ID=" + column.getAD_Column_ID() + ")");
        }

        String tableName = idColumn.substring(0, idColumn.length() - 3);

        RefInfo info = new RefInfo();
        info.tableName = tableName;
        info.idColumn = idColumn;
        info.lookupColumn = chooseLookupColumn(info.tableName, useValueForRef, trxName);

        return info;
    }

    /**
     * Choose which text column to use for lookup.
     *
     * Priority:
     * - if useValueForRef: Value then Name
     * - else: Name then Value
     */
    private String chooseLookupColumn(String tableName, boolean useValueForRef, String trxName) {
        boolean hasName = hasColumn(tableName, "Name", trxName);
        boolean hasValue = hasColumn(tableName, "Value", trxName);

        if (useValueForRef) {
            if (hasValue) {
                return "Value";
            }
            if (hasName) {
                return "Name";
            }
        } else {
            if (hasName) {
                return "Name";
            }
            if (hasValue) {
                return "Value";
            }
        }

        throw new AdempiereException("Neither Name nor Value exists on reference table " + tableName);
    }

    private Map<String, Integer> loadLookupMap(Properties ctx, RefInfo info, String trxName) {
        Map<String, Integer> map = new HashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append(info.idColumn)
           .append(", ")
           .append(info.lookupColumn)
           .append(" FROM ")
           .append(info.tableName)
           .append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (info.hasIsActive) {
            sql.append(" AND IsActive='Y' ");
        }

        if (info.hasADClientID) {
            sql.append(" AND AD_Client_ID IN (0, ?) ");
            params.add(Env.getAD_Client_ID(ctx));
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql.toString(), trxName);

            int idx = 1;
            for (Object param : params) {
                pstmt.setObject(idx++, param);
            }

            rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt(1);
                String raw = rs.getString(2);

                String key = normalize(raw);
                if (Util.isEmpty(key, true)) {
                    continue;
                }

                Integer old = map.putIfAbsent(key, id);
                if (old != null && old.intValue() != id) {
                    log.warning("Duplicate normalized lookup value detected. table="
                            + info.tableName
                            + ", lookupColumn=" + info.lookupColumn
                            + ", value=" + key
                            + ", existingID=" + old
                            + ", duplicateID=" + id);
                }
            }

            if (log.isLoggable(java.util.logging.Level.FINE)) {
                log.fine("Loaded reference cache: table=" + info.tableName
                        + ", lookupColumn=" + info.lookupColumn
                        + ", rows=" + map.size());
            }

            return map;
        } catch (Exception e) {
            throw new AdempiereException("Failed to load reference cache for table "
                    + info.tableName + ", lookupColumn=" + info.lookupColumn, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
    }

    private boolean hasColumn(String tableName, String columnName, String trxName) {
        if (Util.isEmpty(tableName, true) || Util.isEmpty(columnName, true)) {
            return false;
        }

        Set<String> cols = getTableColumns(tableName, trxName);
        return cols.contains(columnName.trim().toUpperCase());
    }

    private Set<String> getTableColumns(String tableName, String trxName) {
        String key = tableName.trim().toUpperCase();
        Set<String> cached = tableColumnsCache.get(key);
        if (cached != null) {
            return cached;
        }

        Set<String> cols = loadTableColumns(tableName, trxName);
        tableColumnsCache.put(key, cols);
        return cols;
    }

    /**
     * Loads column names from AD metadata.
     */
    private Set<String> loadTableColumns(String tableName, String trxName) {
        if (Util.isEmpty(tableName, true)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();

        final String sql =
                "SELECT c.ColumnName " +
                "FROM AD_Column c " +
                "INNER JOIN AD_Table t ON (t.AD_Table_ID = c.AD_Table_ID) " +
                "WHERE UPPER(t.TableName) = UPPER(?)";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, trxName);
            pstmt.setString(1, tableName);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String col = rs.getString(1);
                if (!Util.isEmpty(col, true)) {
                    result.add(col.trim().toUpperCase());
                }
            }

            return result;
        } catch (Exception e) {
            throw new AdempiereException("Failed to load columns for table " + tableName, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
    }

    private String loadTableNameById(int adTableId, String trxName) {
        if (adTableId <= 0) {
            return null;
        }

        final String sql = "SELECT TableName FROM AD_Table WHERE AD_Table_ID=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, trxName);
            pstmt.setInt(1, adTableId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (Exception e) {
            throw new AdempiereException("Failed to load table name for AD_Table_ID=" + adTableId, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
    }

    private String loadColumnNameById(int adColumnId, String trxName) {
        if (adColumnId <= 0) {
            return null;
        }

        final String sql = "SELECT ColumnName FROM AD_Column WHERE AD_Column_ID=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, trxName);
            pstmt.setInt(1, adColumnId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (Exception e) {
            throw new AdempiereException("Failed to load column name for AD_Column_ID=" + adColumnId, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static class RefInfo {
        String tableName;
        String idColumn;
        String lookupColumn;
        boolean hasIsActive;
        boolean hasADClientID;
    }

    public static class LookupSpec {
        private final MColumn column;
        private final boolean useValueForRef;

        public LookupSpec(MColumn column, boolean useValueForRef) {
            this.column = column;
            this.useValueForRef = useValueForRef;
        }

        public MColumn getColumn() {
            return column;
        }

        public boolean isUseValueForRef() {
            return useValueForRef;
        }
    }
}

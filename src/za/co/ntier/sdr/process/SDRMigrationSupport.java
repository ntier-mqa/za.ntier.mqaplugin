package za.co.ntier.sdr.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;

/**
 * Shared helpers for the SDR_ data-migration (Migrate*) processes - mirrors
 * za.co.ntier.learner.process.MigrationSupport's shape, but leaner: every SDR_ reference/business
 * table this project builds carries a plain "id" recon column holding the original mssdr_*.id (see
 * AddColumnsSupport's createReconIdColumn/createNewTableSchema), so almost every LOOKUP resolution
 * across all 8 SDR phases is the same generic id-crosswalk - unlike the Learner project's mix of
 * name-matching/value-matching/List-mapping strategies needed there because ms_person etc. merge
 * into a pre-existing, differently-shaped target. SDR_ tables are freshly built 1:1 from mssdr_*, so
 * "source id -&gt; target's own PK, via the target's id column" covers the overwhelming majority of
 * cases.
 *
 * <p>The "0 = not set" sentinel convention (confirmed platform-wide, e.g. Person's address lookups)
 * needs no special handling here: a genuine 0 sentinel and a genuine SQL NULL both come back from
 * {@code ResultSet.getInt()} as Java {@code 0}, and {@link #buildIdCrosswalk} naturally has no entry
 * for key 0 (mssdr_lkp*.id always starts at 1) - callers just get {@code null} back either way via
 * {@link #resolveLookup}, which is exactly "leave unset".
 */
final class SDRMigrationSupport {

    private SDRMigrationSupport() {
    }

    /**
     * Generic recon-column crosswalk: mssdr_* source id -&gt; already-built SDR_ target row's PK,
     * read off the "id" recon column every SDR_ table carries. Used for the large majority of LOOKUP
     * columns in this migration, whether the target is a small reference/catalog table (e.g.
     * SDR_Title) or another already-migrated SDR_ business table in the same family (e.g.
     * SDR_PersonAddress.personid -&gt; SDR_Person, via SDR_Person's own "id" column - which requires
     * SDR_Person to have been fully migrated first).
     */
    static Map<Integer, Integer> buildIdCrosswalk(String targetTable, String targetIdCol, String trxName) {
        Map<Integer, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT id, " + targetIdCol + " FROM " + targetTable + " WHERE id IS NOT NULL", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("id"), rs.getInt(targetIdCol));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building id crosswalk for " + targetTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * Looks up {@code sourceId} in {@code crosswalk}, returning {@code null} (not 0) when there is no
     * entry - the standard "leave this FK column unset" outcome for a 0-sentinel, a genuine SQL NULL,
     * or a source id that simply never resolved (e.g. a partially-populated lookup).
     */
    static Integer resolveLookup(Map<Integer, Integer> crosswalk, int sourceId) {
        return crosswalk.get(sourceId);
    }

    /** MS tinyint 0/1 (or null) flag -&gt; Y/N. 0 = No, non-zero = Yes. */
    static String flagToYN(Integer flag) {
        if (flag == null) {
            return null;
        }
        return flag != 0 ? "Y" : "N";
    }

    /**
     * Wraps a plain int as BigDecimal - REQUIRED for any column whose AD_Reference_ID is one of
     * Integer/Number/Amount/Quantity/CostPrice. Confirmed the hard way on the first live
     * MigrateSDRPersonTable run: PO.set_ValueOfColumn() threw "WrongDataType ... Class invalid:
     * class java.lang.Integer, Should be class java.math.BigDecimal" on every single row for
     * SDR_YearsInOccupation (DisplayType.Number) - despite DisplayType.getClass() misleadingly
     * returning Integer.class for DisplayType.Integer specifically, DisplayType.isNumeric()'s own
     * doc comment is explicit: "Amount, Number, Quantity, Integer ... stored as BigDecimal". Only
     * Table/TableDir/Search/List/ID-shaped columns (real FK/id columns) stay plain Integer -
     * lookups resolved via {@link #resolveLookup} for those should NOT be routed through this.
     */
    static BigDecimal toBD(int value) {
        return BigDecimal.valueOf(value);
    }

    /**
     * Same as {@link #toBD(int)} but null-safe - for a plain Integer/Number-shaped column whose
     * value is itself optional (e.g. a still-deferred cross-family FK like SDR_WSPATR_ID/SDR_SDF_ID
     * built as DisplayType.Integer rather than a real Table reference, resolved via
     * {@link #resolveLookup} and possibly absent).
     */
    static BigDecimal toBD(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * Stamps the source row's own Created/Updated timestamps (which PO.saveEx() would otherwise
     * overwrite with "now", since Created/Updated are PO-managed standard columns with no public
     * setter) onto the just-saved target row, identified by its own PK. CreatedBy/UpdatedBy are
     * deliberately NOT touched here - per the platform-wide SDR convention confirmed across every
     * family's mapping doc, those stay whatever PO.saveEx() naturally stamped (the running migration
     * process's own AD_User), unlike the Learner project's MigrationSupport.stampCreatedUpdated,
     * which overrides both timestamp AND actor columns to historical values.
     */
    static void stampCreatedUpdated(String table, String pkColumn, int pkValue, Timestamp created,
            Timestamp updated, String trxName) {
        DB.executeUpdateEx("UPDATE " + table + " SET created = ?, updated = ? WHERE " + pkColumn + " = ?",
                new Object[] { created, updated, pkValue }, trxName);
    }
}

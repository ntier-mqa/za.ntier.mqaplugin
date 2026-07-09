package za.co.ntier.learner.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocation;
import org.compiere.util.DB;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZPerson;

/**
 * Shared lookup/crosswalk helpers for the ms_person / ms_learner -&gt; ZZPerson / ZZLearner
 * migration. Every resolution rule implemented here is documented in
 * "Column Mapping - ms_person and ms_learner to zzperson and zzlearner.txt"
 * (Learners Data Migration runbook) - see that file for the "why" behind each mapping.
 */
final class MigrationSupport {

    private MigrationSupport() {
    }

    static int getSouthAfricaCountryId(String trxName) {
        int id = DB.getSQLValueEx(trxName, "SELECT c_country_id FROM c_country WHERE countrycode = 'ZA'");
        if (id <= 0) {
            throw new AdempiereException("Setup error: no C_Country row for countrycode='ZA'");
        }
        return id;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    /** MS lkpprovince.id -&gt; adempiere C_Region_ID, matched by name ignoring case/whitespace. */
    static Map<Integer, Integer> buildProvinceCrosswalk(String trxName) {
        Map<String, Integer> byNormalizedName = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT c_region_id, name FROM c_region", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                byNormalizedName.put(normalize(rs.getString("name")), rs.getInt("c_region_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading c_region", e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, Integer> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, description FROM lkpprovince", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                Integer regionId = byNormalizedName.get(normalize(rs.getString("description")));
                if (regionId != null) {
                    result.put(rs.getInt("id"), regionId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading lkpprovince", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS lkpcity.id -&gt; adempiere C_City_ID, matched by name only (case/whitespace
     * insensitive). c_city has many more rows than lkpcity and duplicate names occur
     * across different provinces - the first match (lowest C_City_ID) wins. This is an
     * approximation, not a guaranteed-correct match; the free-text city name is always
     * kept on C_Location.City regardless of whether this crosswalk resolves an id.
     */
    static Map<Integer, Integer> buildCityCrosswalk(String trxName) {
        Map<String, Integer> byNormalizedName = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT c_city_id, name FROM c_city ORDER BY c_city_id", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                byNormalizedName.putIfAbsent(normalize(rs.getString("name")), rs.getInt("c_city_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading c_city", e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, Integer> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, description FROM lkpcity", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                Integer cityId = byNormalizedName.get(normalize(rs.getString("description")));
                if (cityId != null) {
                    result.put(rs.getInt("id"), cityId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading lkpcity", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS lkp&lt;table&gt;.id -&gt; adempiere &lt;targetTable&gt;.&lt;targetIdCol&gt;, matched by
     * a case-insensitive exact match between lkp&lt;table&gt;.description and
     * &lt;targetTable&gt;.name. Used for the "Table Direct" lookups where the MS description
     * text and the already-staged adempiere reference table's name are verbatim matches
     * (confirmed while building the column mapping doc): disability, homelanguage,
     * nationality, citizenresidentialstatus, socioeconomicstatus, alternateidtype.
     */
    static Map<Integer, Integer> buildNameMatchCrosswalk(String trxName, String lkpTable,
            String targetTable, String targetIdCol) {
        Map<String, Integer> byLowerName = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT " + targetIdCol + ", name FROM " + targetTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    byLowerName.putIfAbsent(name.trim().toLowerCase(), rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading " + targetTable, e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, Integer> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, description FROM " + lkpTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String description = rs.getString("description");
                Integer targetId = description == null ? null
                        : byLowerName.get(description.trim().toLowerCase());
                if (targetId != null) {
                    result.put(rs.getInt("id"), targetId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading " + lkpTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * Same idea as {@link #buildNameMatchCrosswalk} but for the schoolemis / statssaareacode
     * lookups, where the adempiere side is already staged and matched by "value" (the EMIS
     * code / SAQA code), not by name - and the MS side hop is via lkpValueCol (code/saqacode
     * on the lkp&lt;table&gt; row), not the MS id directly.
     */
    static Map<Integer, Integer> buildValueMatchCrosswalk(String trxName, String lkpTable,
            String lkpValueCol, String targetTable, String targetIdCol) {
        Map<String, Integer> byValue = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT " + targetIdCol + ", value FROM " + targetTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String value = rs.getString("value");
                if (value != null) {
                    byValue.putIfAbsent(value.trim(), rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading " + targetTable, e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, Integer> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, " + lkpValueCol + " FROM " + lkpTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String code = rs.getString(lkpValueCol);
                Integer targetId = code == null ? null : byValue.get(code.trim());
                if (targetId != null) {
                    result.put(rs.getInt("id"), targetId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading " + lkpTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * free-text highesteducation on ms_person (not an id) -&gt; ZZ_LI_HighestEducation_ID,
     * matched case-insensitively against the target table's name.
     */
    static Map<String, Integer> buildHighestEducationCrosswalk(String trxName) {
        Map<String, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT zz_li_highesteducation_id, name FROM zz_li_highesteducation", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    result.putIfAbsent(name.trim().toLowerCase(), rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading zz_li_highesteducation", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /** Only unique (non-ambiguous), active emails are included - see AD_USER RESOLUTION STRATEGY. */
    static Map<String, Integer> buildUniqueEmailIndex(String trxName) {
        Map<String, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                    + "FROM ad_user "
                    + "WHERE isactive = 'Y' AND email IS NOT NULL AND trim(email) <> '' "
                    + "GROUP BY lower(trim(email)) HAVING count(*) = 1", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("email_key"), rs.getInt("ad_user_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building AD_User email index", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * The health ratings are NOT pre-loaded into a Java map (that would mean holding up to
     * 881,799 String[6] arrays in the JVM heap on top of everything else this process
     * already caches). Instead {@link MigrateMsPersonToZZPerson} pivots
     * ms_personhealthfunctioningstatusrating straight in SQL via a LATERAL join keyed off
     * personid (see that class for the query + the required index), and only calls
     * {@link #mapHealthRatingValue(String)} once per resolved column, per row.
     */

    /** MS lkphealthfunctioningrating.description -&gt; ZZHealth* List value (same 8 values,
     * identical across all 6 ZZHealth* columns). Target has two spelling quirks vs the MS
     * text ("determind", "difficultys") - already baked into the generated constants. */
    static String mapHealthRatingValue(String msDescription) {
        if (Util.isEmpty(msDescription, true)) {
            return null;
        }
        String s = msDescription.trim().toLowerCase();
        if (s.equals("no difficulty")) return X_ZZPerson.ZZHEALTHSEEING_NoDifficulty;
        if (s.equals("some difficulty")) return X_ZZPerson.ZZHEALTHSEEING_SomeDifficulty;
        if (s.equals("a lot of difficulty")) return X_ZZPerson.ZZHEALTHSEEING_ALotOfDifficulty;
        if (s.equals("cannot do at all")) return X_ZZPerson.ZZHEALTHSEEING_CannotDoAtAll;
        if (s.equals("cannot yet be determined")) return X_ZZPerson.ZZHEALTHSEEING_CannotYetBeDetermind;
        if (s.equals("former difficulty - none now")) return X_ZZPerson.ZZHEALTHSEEING_FormerDifficulty_NoneNow;
        if (s.startsWith("may be part of multiple difficult")) return X_ZZPerson.ZZHEALTHSEEING_MayBePartOfMultipleDifficultysTBC;
        if (s.startsWith("may have difficulty")) return X_ZZPerson.ZZHEALTHSEEING_MayHaveDifficultyTBC;
        return null;
    }

    /** MS lkptitle.description -&gt; ZZLkpTitle List value. */
    static String mapTitle(String msDescription) {
        if (Util.isEmpty(msDescription, true)) {
            return null;
        }
        String s = msDescription.trim().toLowerCase();
        switch (s) {
        case "mr": return X_ZZPerson.ZZLKPTITLE_Mr;
        case "mrs": return X_ZZPerson.ZZLKPTITLE_Mrs;
        case "ms": return X_ZZPerson.ZZLKPTITLE_Ms;
        case "miss": return X_ZZPerson.ZZLKPTITLE_Miss;
        case "dr": return X_ZZPerson.ZZLKPTITLE_Dr;
        case "prof": return X_ZZPerson.ZZLKPTITLE_Prof;
        case "adv": return X_ZZPerson.ZZLKPTITLE_Adv;
        case "me": return X_ZZPerson.ZZLKPTITLE_Me;
        default: return X_ZZPerson.ZZLKPTITLE_Other;
        }
    }

    /** MS lkpgender.description -&gt; ZZGender List value. */
    static String mapGender(String msDescription) {
        if (Util.isEmpty(msDescription, true)) {
            return null;
        }
        String s = msDescription.trim().toLowerCase();
        if (s.startsWith("male")) return X_ZZPerson.ZZGENDER_Male;
        if (s.startsWith("female")) return X_ZZPerson.ZZGENDER_Female;
        return X_ZZPerson.ZZGENDER_Other;
    }

    /** MS lkpequity.description -&gt; ZZEquity List value (contains-based, same style as
     * ImportSgSdfDocuments.mapEquity()). */
    static String mapEquity(String msDescription) {
        if (Util.isEmpty(msDescription, true)) {
            return null;
        }
        String s = msDescription.trim().toLowerCase();
        if (s.contains("african")) return X_ZZPerson.ZZEQUITY_African;
        if (s.contains("coloured")) return X_ZZPerson.ZZEQUITY_Coloured;
        if (s.contains("indian")) return X_ZZPerson.ZZEQUITY_Indian;
        if (s.contains("white")) return X_ZZPerson.ZZEQUITY_White;
        return null;
    }

    /** MS lkppopiactstatus.description -&gt; ZZPopiActStatus List value. "N/A" has no
     * equivalent target value and is left null. */
    static String mapPopiActStatus(String msDescription) {
        if (Util.isEmpty(msDescription, true)) {
            return null;
        }
        String s = msDescription.trim().toLowerCase();
        if (s.equals("agree")) return X_ZZPerson.ZZPOPIACTSTATUS_Agree;
        if (s.equals("disagree")) return X_ZZPerson.ZZPOPIACTSTATUS_Disagree;
        return null;
    }

    /**
     * Creates and saves a new C_Location from the given (already-resolved) parts.
     * Returns 0 (no location) if every text field is blank and no region/city resolved -
     * i.e. there is nothing meaningful to store.
     */
    static int createLocation(java.util.Properties ctx,
            String trxName, int countryId, Integer regionId, Integer cityId, String cityText,
            String address1, String address2, String address3, String postal) {
        boolean anyText = !Util.isEmpty(address1, true) || !Util.isEmpty(address2, true)
                || !Util.isEmpty(address3, true) || !Util.isEmpty(postal, true)
                || !Util.isEmpty(cityText, true);
        if (!anyText && regionId == null && cityId == null) {
            return 0;
        }
        MLocation loc = new MLocation(ctx, 0, trxName);
        loc.setC_Country_ID(countryId);
        if (regionId != null) {
            loc.setC_Region_ID(regionId.intValue());
        }
        if (cityId != null) {
            loc.setC_City_ID(cityId.intValue());
        }
        if (!Util.isEmpty(cityText, true)) {
            loc.setCity(cityText.trim());
        }
        if (!Util.isEmpty(address1, true)) {
            loc.setAddress1(address1.trim());
        }
        if (!Util.isEmpty(address2, true)) {
            loc.setAddress2(address2.trim());
        }
        if (!Util.isEmpty(address3, true)) {
            loc.setAddress3(address3.trim());
        }
        if (!Util.isEmpty(postal, true)) {
            loc.setPostal(postal.trim());
        }
        loc.saveEx();
        return loc.get_ID();
    }

    /**
     * Stamps the historical created/updated/createdby/updatedby values (which PO.saveEx()
     * would otherwise overwrite with "now"/the running user) and the source record's own id
     * - kept on the target row (column "id", same name as the source table's PK) purely for
     * recon: matching a migrated ZZPerson/ZZLearner row back to its ms_person/ms_learner row
     * without having to go through the ms_person_xref/ms_learner_xref crosswalk tables.
     */
    static void stampCreatedUpdated(String table, String pkColumn, int pkValue,
            java.sql.Timestamp created, int createdBy, java.sql.Timestamp updated, int updatedBy,
            long sourceId, String trxName) {
        DB.executeUpdateEx(
                "UPDATE " + table + " SET created = ?, createdby = ?, updated = ?, updatedby = ?, id = ? "
                + "WHERE " + pkColumn + " = ?",
                new Object[] { created, createdBy, updated, updatedBy, sourceId, pkValue }, trxName);
    }
}

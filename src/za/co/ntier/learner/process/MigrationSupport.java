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
     * Same rule as {@link #buildUniqueEmailIndex} but keyed off ms_user.email (the staged
     * MSSQL "User" table, not ad_user) - used to resolve the various "who did this" actor
     * columns on the QCTO join tables (enrolledby, registeredby, certificatecreatedby, etc.):
     * look up the ms_user row's email, then find the matching ad_user by that same email.
     * Returns MSSQL User.id -&gt; AD_User_ID directly so callers don't need two lookups.
     * Link-only - never creates an AD_User (same rule as zzperson.ad_user_id, confirmed
     * 2026-07-08 and re-confirmed 2026-07-10 for these actor columns).
     */
    static Map<Integer, Integer> buildMsUserToAdUserCrosswalk(String trxName) {
        Map<String, Integer> emailToAdUser = buildUniqueEmailIndex(trxName);
        Map<Integer, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT id, lower(trim(email)) AS email_key FROM ms_user WHERE email IS NOT NULL AND trim(email) <> ''",
                    trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                Integer adUserId = emailToAdUser.get(rs.getString("email_key"));
                if (adUserId != null) {
                    result.put(rs.getInt("id"), adUserId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building ms_user -> AD_User crosswalk", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * ms_organisation.id -&gt; c_bpartner_id, matched via ms_organisation.sdlnumber =
     * c_bpartner.zz_sdl_no (trimmed, case-sensitive exact match - confirmed 2026-07-10 as the
     * resolution for zzprovider/zzworkplaceapproval/zzassessmentcentre.C_BPartner_ID and
     * zzlearnerqctoartisans/zzlearnerqctolearnership.Employer_ID - see "ZZProvider - New
     * Columns to Add.txt" Section C). Link-only: never creates a new C_BPartner. Only ~7
     * c_bpartner rows currently have zz_sdl_no populated (as of 2026-07-10), so most
     * organisationid/employerid values are expected to resolve to nothing for now - not a bug.
     */
    static Map<Integer, Integer> buildOrganisationToBPartnerCrosswalk(String trxName) {
        Map<String, Integer> bpartnerBySdlNo = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT c_bpartner_id, zz_sdl_no FROM c_bpartner WHERE zz_sdl_no IS NOT NULL AND trim(zz_sdl_no) <> ''",
                    trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                bpartnerBySdlNo.putIfAbsent(rs.getString("zz_sdl_no").trim(), rs.getInt("c_bpartner_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading c_bpartner.zz_sdl_no", e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, Integer> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT id, sdlnumber FROM ms_organisation WHERE sdlnumber IS NOT NULL AND trim(sdlnumber) <> ''",
                    trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                Integer bpartnerId = bpartnerBySdlNo.get(rs.getString("sdlnumber").trim());
                if (bpartnerId != null) {
                    result.put(rs.getInt("id"), bpartnerId);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading ms_organisation.sdlnumber", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS source id (of a lookup table's OWN row) -&gt; already-created reference table's own
     * PK, matched via the target table's "Value" column rather than its "id" recon column -
     * used for zzprovider/zzassessmentcentre.Saqa_QA_ID, where the source column
     * (ms_skillsdevelopmentprovider.saqaqaid / ms_assessmentcentre.saqaqaid) stores
     * lkpSAQADataSuppliers.ETQAID (text) directly, not lkpSAQADataSuppliers.ID - so the usual
     * id-based {@link #buildIdCrosswalk} doesn't apply; the source int value itself (cast to
     * text) is looked up directly against the target's Value column.
     */
    static Map<String, Integer> buildValueCrosswalk(String targetTable, String targetIdCol, String trxName) {
        Map<String, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT " + targetIdCol + ", value FROM " + targetTable + " WHERE value IS NOT NULL", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                result.putIfAbsent(rs.getString("value").trim(), rs.getInt(1));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building value crosswalk for " + targetTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * Generic recon-column crosswalk: MS source id -&gt; already-migrated target row's PK,
     * read straight off the "id" recon column this migration project adds to every target
     * table (see Column Mapping doc, "Recon (new)" columns). Used to resolve FK columns that
     * point at a parent/catalog table migrated earlier in the same run
     * (e.g. ms_learnerqctoartisans.qctolearnershipid -&gt; zzqctolearnership_id via
     * zzqctolearnership.id). Only rows where the recon column has been populated are
     * included, so this naturally reflects "already migrated" state.
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
     * Generic plain-text lookup: MS lkp&lt;table&gt;.id -&gt; description. Used for every
     * "Lookup" column on the QCTO tables whose target is a free-text column rather than a
     * foreign key (socioeconomicstatus, sponsorship, project, terminationreason, seta,
     * nqflevel, qualityassurancebody, etc. - see Column Mapping doc Section C).
     */
    static Map<Integer, String> buildDescriptionMap(String lkpTable, String trxName) {
        Map<Integer, String> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, description FROM " + lkpTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("id"), rs.getString("description"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building description map for " + lkpTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS source table's own id -&gt; an already-fully-populated catalog target's PK, matched
     * ORDINALLY (Nth source row by id &lt;-&gt; Nth target row by targetOrdinalCol) - same
     * row_number()-based join MigrateMsQualificationToZZQualification (and its Tier 2 siblings)
     * already use to match placeholder rows, but read-only here: for a catalog that's already
     * fully migrated (e.g. zzqctomodule, confirmed 2026-07-16 to already hold all 579
     * QCTOModule rows via zzmigrationcode ordinal matching, with no plain "id" recon column of
     * its own) this just builds the crosswalk needed by OTHER tables' FK columns, without
     * re-running any migration. Verify the ordinal assumption holds for a new catalog (e.g. by
     * comparing a natural-language column on a few rows on both sides) before trusting this -
     * it is not self-verifying.
     */
    static Map<Integer, Integer> buildOrdinalCrosswalk(String sourceTable, String targetTable, String targetIdCol,
            String targetOrdinalCol, String trxName) {
        Map<Integer, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT s.id AS source_id, t.target_id FROM "
                    + "(SELECT id, row_number() OVER (ORDER BY id) AS rn FROM " + sourceTable + ") s "
                    + "JOIN (SELECT " + targetIdCol + " AS target_id, row_number() OVER (ORDER BY " + targetOrdinalCol + ") AS rn "
                    + "      FROM " + targetTable + ") t ON t.rn = s.rn", trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("source_id"), rs.getInt("target_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building ordinal crosswalk for " + sourceTable + " -> " + targetTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS lkp&lt;table&gt;.id -&gt; the matching AD_Ref_List.Value for the given
     * adReferenceId - matched by lkp&lt;table&gt;.description against AD_Ref_List.Name
     * (case-insensitive, trimmed). Discovered 2026-07-16: several "resolved via description"
     * target columns on the QCTO join tables (ZZSocioEconomicStatus, ZZSponsorship, ZZProject,
     * ZZArtisanProject, ZZEnrolmentStatusReason, ZZOtherSeta/ZZSeta, ZZQualificationRequirements,
     * ZZLearnerQCTOLearnershipType, ZZCertificateReasonForReprint) turned out to be List
     * references (AD_Reference_ID=17), NOT plain String columns as originally assumed - a live
     * migration run failed PO validation trying to store the raw description text
     * ("Unemployed") into a column whose valid List values are short codes ("02"). Matching by
     * NAME (not by the source lkp table's own SAQACode column) is the general fix: some of
     * these lists use a short numeric/SAQA-style code as Value with a different, longer Name
     * (e.g. SocioEconomicStatus: Value="01", Name="Employed"), while others self-map
     * (Value=Name=the same free text, e.g. ArtisanProject, and mixed-case lists like
     * EnrolmentStatusReason where SOME entries have a numeric code and SOME don't - matching by
     * name handles both uniformly, unlike matching by SAQACode which doesn't exist on every
     * lkp table (e.g. lkpproject) and wouldn't cover the self-mapped entries either. If a
     * source description has no matching List entry, that id is simply absent from the result
     * (safe no-op when looked up - same "not set" outcome as before this class existed, not a
     * crash) - callers don't need to special-case this.
     */
    static Map<Integer, String> buildListValueCrosswalk(String lkpTable, int adReferenceId, String trxName) {
        Map<String, String> valueByLowerName = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT value, name FROM ad_ref_list WHERE ad_reference_id = ?", trxName);
            pst.setInt(1, adReferenceId);
            rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    valueByLowerName.putIfAbsent(name.trim().toLowerCase(), rs.getString("value"));
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading ad_ref_list for ad_reference_id=" + adReferenceId, e);
        } finally {
            DB.close(rs, pst);
        }

        Map<Integer, String> result = new HashMap<>();
        pst = null;
        rs = null;
        try {
            pst = DB.prepareStatement("SELECT id, description FROM " + lkpTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String description = rs.getString("description");
                String value = description == null ? null : valueByLowerName.get(description.trim().toLowerCase());
                if (value != null) {
                    result.put(rs.getInt("id"), value);
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed building list-value crosswalk for " + lkpTable, e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    /**
     * MS lkpYesNo.id -&gt; Y/N flag. Confirmed directly against the MSSQL lkpYesNo table
     * (2026-07-10): id=1 is "No", id=2 is "Yes" - NOT the more intuitive 1=Yes. Used for every
     * "*YesNoId" column (leadsdproviderlevyyesnoid, walevyyesnoid, artisanlearnershipyesnoid,
     * etc.). Returns null (leave column unset) if the id is missing/unrecognised.
     */
    static String yesNoIdToFlag(Integer yesNoId) {
        if (yesNoId == null) {
            return null;
        }
        if (yesNoId == 1) {
            return "N";
        }
        if (yesNoId == 2) {
            return "Y";
        }
        return null;
    }

    /** Direct 0/1 (or null) flag columns (isapproved, canassociategrants, isohs, rpl,
     * belongtofasset, isendorsed, isreplacement, isreregistered, etc.) -&gt; Y/N. Unlike
     * {@link #yesNoIdToFlag} these are NOT a lookup id - 0 = No, non-zero = Yes. */
    static String flagToYN(Integer flag) {
        if (flag == null) {
            return null;
        }
        return flag != 0 ? "Y" : "N";
    }

    /** Same as {@link #flagToYN(Integer)} but for source columns already staged as a real
     * Postgres boolean (e.g. ms_granttype.ispayable). */
    static String flagToYN(Boolean flag) {
        if (flag == null) {
            return null;
        }
        return flag ? "Y" : "N";
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
            int sourceId, String trxName) {
        // NOTE: sourceId must stay `int` (not `long`) - DB.setParameter() (org.compiere.util.DB)
        // only knows how to bind String/Integer/BigDecimal/Timestamp/Boolean/byte[]/Clob; a
        // boxed Long blows up with "Unknown parameter type" (confirmed 2026-07-09). ms_person.id
        // / ms_learner.id are well within int range, and the bigint recon column accepts an
        // Integer parameter fine.
        DB.executeUpdateEx(
                "UPDATE " + table + " SET created = ?, createdby = ?, updated = ?, updatedby = ?, id = ? "
                + "WHERE " + pkColumn + " = ?",
                new Object[] { created, createdBy, updated, updatedBy, sourceId, pkValue }, trxName);
    }
}

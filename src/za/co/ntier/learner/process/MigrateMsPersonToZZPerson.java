package za.co.ntier.learner.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZPerson;

/**
 * Migrates the staged ms_person / ms_personaddress / ms_personhealthfunctioningstatusrating
 * tables (loaded earlier via pgloader, see myloader_Person_dev.load and
 * myloader_PersonRelated_dev.load) into real ZZPerson records, linking an existing AD_User
 * (by email) and creating physical/postal C_Location records along the way.
 *
 * <p>Per explicit instruction (2026-07-08): this process does NOT create AD_User records.
 * If an existing AD_User is found by email it is linked; if not, ZZPerson.AD_User_ID is left
 * null and the row is counted/logged as "unlinked" - it is not an error, just a fact worth
 * reporting since AD_User_ID is nullable on ZZPerson.
 *
 * <p>Every resolution rule this class implements (which lookup goes where, the AD_User
 * link-only logic, the health rating pivot, the physical/postal location strategy, the
 * ID/passport vs "other ID" routing) is documented in "Column Mapping - ms_person and
 * ms_learner to zzperson and zzlearner.txt" in the Learners Data Migration runbook - that
 * file is the source of truth for the "why", this class is the "how".
 *
 * <p>Strategy:
 * <ol>
 *   <li>Build a handful of small in-memory crosswalk maps once (province, city, disability,
 *       home language, nationality, citizen status, socio-economic status, alternate ID
 *       type, highest education, school EMIS, StatsSA area code, unique AD_User emails).
 *       None of these hold more than a few tens of thousands of rows.</li>
 *   <li>Stream ms_person with a single large query that LEFT JOIN LATERALs the latest
 *       ms_personaddress row and a SQL-side pivot of ms_personhealthfunctioningstatusrating,
 *       so neither of those (881k and 5.2M rows respectively) is ever loaded into the JVM
 *       heap - only one row at a time comes back to Java.</li>
 *   <li>Each ms_person row is processed in its own short-lived transaction (same pattern as
 *       {@code ImportSgSdfDocuments.createSdfFromExcel}) so one bad row can't corrupt or
 *       roll back the rest of an 800k+ row run.</li>
 *   <li>Already-migrated persons (present in ms_person_xref) are skipped, so the process can
 *       be safely re-run/resumed after a partial failure.</li>
 * </ol>
 *
 * <p>REQUIRES a recon column: {@code ALTER TABLE adempiere.zzperson ADD COLUMN id bigint;}
 * must be run before this process - it stamps ms_person.id into that column (same name as
 * the source table's PK) alongside created/updated, purely so a migrated row can be traced
 * back to its source row without going through ms_person_xref.
 *
 * <p>NOT handled by this process (still open items per the mapping doc):
 * <ul>
 *   <li>ZZCVFileName / ZZPhotographFileName - ms_persondocumentupload has no document-type
 *       column and sample data looks like scanned ID documents, not CVs/photos.</li>
 *   <li>ZZLastSchoolYear - unclear whether ms_person.lastschoolyearid is a plain year or an
 *       MS lookup id; left null pending confirmation.</li>
 *   <li>ZZMigrationCode / ZZMigrateValues - confirmed unrelated to ms_person.migrationrecordid,
 *       intentionally left null.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsPersonToZZPerson")
public class MigrateMsPersonToZZPerson extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst; // Y/N - deletes previously-migrated data before reloading

    private static final int DEFAULT_CREATED_BY = 1000017; // matches the Person-family loader convention
    private static final int MAX_LOGGED_ERRORS = 1000;

    private int countryId;
    private Map<Integer, Integer> provinceCrosswalk;
    private Map<Integer, Integer> cityCrosswalk;
    private Map<Integer, Integer> disabilityCrosswalk;
    private Map<Integer, Integer> homeLanguageCrosswalk;
    private Map<Integer, Integer> nationalityCrosswalk;
    private Map<Integer, Integer> citizenStatusCrosswalk;
    private Map<Integer, Integer> socioEconomicCrosswalk;
    private Map<Integer, Integer> alternateIdTypeCrosswalk;
    private Map<String, Integer> highestEducationCrosswalk;
    private Map<Integer, Integer> schoolEmisCrosswalk;
    private Map<Integer, Integer> statssaAreaCodeCrosswalk;
    private Map<String, Integer> emailIndex;

    private final List<String> errors = new ArrayList<>();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        addLog("Ensuring supporting indexes and crosswalk table exist...");
        DB.executeUpdateEx(
                "CREATE INDEX IF NOT EXISTS ms_personaddress_personid_idx ON ms_personaddress(personid)",
                null, get_TrxName());
        DB.executeUpdateEx(
                "CREATE INDEX IF NOT EXISTS ms_personhealthstatusrating_personid_idx "
                + "ON ms_personhealthfunctioningstatusrating(personid)",
                null, get_TrxName());
        DB.executeUpdateEx(
                "CREATE TABLE IF NOT EXISTS ms_person_xref ("
                + "ms_person_id integer PRIMARY KEY, zzperson_id numeric(10) NOT NULL)",
                null, get_TrxName());

        if ("Y".equals(p_ClearDataFirst)) {
            clearPreviouslyMigratedData();
        }

        addLog("Building lookup crosswalks...");
        countryId = MigrationSupport.getSouthAfricaCountryId(get_TrxName());
        provinceCrosswalk = MigrationSupport.buildProvinceCrosswalk(get_TrxName());
        cityCrosswalk = MigrationSupport.buildCityCrosswalk(get_TrxName());
        disabilityCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkpdisability", "zz_li_disability", "zz_li_disability_id");
        homeLanguageCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkphomelanguage", "zz_li_homelanguage", "zz_li_homelanguage_id");
        nationalityCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkpnationality", "zz_nationality", "zz_nationality_id");
        citizenStatusCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkpcitizenresidentialstatus", "zz_li_citizenresidentialstatus",
                "zz_li_citizenresidentialstatus_id");
        socioEconomicCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkpsocioeconomicstatus", "zz_li_socioeconomicstatus", "zz_li_socioeconomicstatus_id");
        alternateIdTypeCrosswalk = MigrationSupport.buildNameMatchCrosswalk(get_TrxName(),
                "lkpalternateidtype", "zz_alternateidtype", "zz_alternateidtype_id");
        highestEducationCrosswalk = MigrationSupport.buildHighestEducationCrosswalk(get_TrxName());
        schoolEmisCrosswalk = MigrationSupport.buildValueMatchCrosswalk(get_TrxName(),
                "lkpschoolemis", "code", "zzlkpschoolemis", "zzlkpschoolemis_id");
        statssaAreaCodeCrosswalk = MigrationSupport.buildValueMatchCrosswalk(get_TrxName(),
                "lkpstatssaareacode", "saqacode", "zzlkpstatssaareacode", "zzlkpstatssaareacode_id");
        emailIndex = MigrationSupport.buildUniqueEmailIndex(get_TrxName());
        addLog("Crosswalks ready: " + provinceCrosswalk.size() + " provinces, " + cityCrosswalk.size()
                + " cities, " + emailIndex.size() + " unique existing AD_User emails.");

        String sql =
                "SELECT p.id, p.titleid, p.firstname, p.middlename, p.surname, p.initials, p.idno, "
                + "       p.alternateidtypeid, p.dateofbirth, p.genderid, p.equityid, p.disabilityid, "
                + "       p.homelanguageid, p.nationalityid, p.citizenresidentialstatusid, "
                + "       p.socioeconomicstatusid, p.telephonenumber, p.cellphonenumber, p.faxnumber, "
                + "       p.email, p.created, p.updated, p.isdeleted, p.schoolemisid, p.lastschoolyearid, "
                + "       p.statssaareacodeid, p.popiactstatusid, p.popiactstatusdate, p.highesteducation, "
                + "       p.currentoccupation, p.yearsinoccupation, p.experience, p.parentpersonid, "
                + "       p.middlename2, "
                + "       t.description AS title_desc, g.description AS gender_desc, "
                + "       eq.description AS equity_desc, pa.description AS popiactstatus_desc, "
                + "       addr.physicaladdress1, addr.physicaladdress2, addr.physicaladdress3, "
                + "       addr.physicalcode, addr.physicalprovinceid, addr.physicalcityid, "
                + "       pcity.description AS physical_city_desc, addr.usephysicalaspostal, "
                + "       addr.postaladdressline1, addr.postaladdressline2, addr.postaladdressline3, "
                + "       addr.postalcode, addr.postalprovinceid, addr.postalcityid, "
                + "       qcity.description AS postal_city_desc, "
                + "       hlt.seeing, hlt.hearing, hlt.communicating, hlt.walking, hlt.remembering, hlt.selfcare "
                + "FROM ms_person p "
                + "LEFT JOIN lkptitle t ON t.id = p.titleid "
                + "LEFT JOIN lkpgender g ON g.id = p.genderid "
                + "LEFT JOIN lkpequity eq ON eq.id = p.equityid "
                + "LEFT JOIN lkppopiactstatus pa ON pa.id = p.popiactstatusid "
                + "LEFT JOIN LATERAL ( "
                + "    SELECT * FROM ms_personaddress a2 WHERE a2.personid = p.id "
                + "    ORDER BY a2.updated DESC LIMIT 1 "
                + ") addr ON true "
                + "LEFT JOIN lkpcity pcity ON pcity.id = addr.physicalcityid "
                + "LEFT JOIN lkpcity qcity ON qcity.id = addr.postalcityid "
                + "LEFT JOIN LATERAL ( "
                + "    SELECT "
                + "        max(rating) FILTER (WHERE status = 'Seeing') AS seeing, "
                + "        max(rating) FILTER (WHERE status = 'Hearing') AS hearing, "
                + "        max(rating) FILTER (WHERE status = 'Communicating') AS communicating, "
                + "        max(rating) FILTER (WHERE status = 'Walking') AS walking, "
                + "        max(rating) FILTER (WHERE status = 'Remembering') AS remembering, "
                + "        max(rating) FILTER (WHERE status = 'Selfcare') AS selfcare "
                + "    FROM ( "
                + "        SELECT s.description AS status, r.description AS rating "
                + "        FROM ms_personhealthfunctioningstatusrating h "
                + "        JOIN lkphealthfunctioningstatus s ON s.id = h.healthfunctioningstatusid "
                + "        JOIN lkphealthfunctioningrating r ON r.id = h.healthfunctioningratingid "
                + "        WHERE h.personid = p.id "
                + "    ) x "
                + ") hlt ON true "
                + "WHERE NOT EXISTS (SELECT 1 FROM ms_person_xref x WHERE x.ms_person_id = p.id) "
                + "ORDER BY p.id"
                + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsPersonRead");
        Trx readTrx = Trx.get(readTrxName, true);
        int processed = 0;
        int created = 0;
        int unlinkedUsers = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    boolean linkedUser = processOneRow(rs);
                    created++;
                    if (!linkedUser) {
                        unlinkedUsers++;
                    }
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_person rows (" + created + " ZZPerson created, "
                            + unlinkedUsers + " with no AD_User match, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        addLog("Backfilling ZZParentPerson_ID from ms_person.parentpersonid...");
        int backfilled = DB.executeUpdateEx(
                "UPDATE zzperson child "
                + "SET zzparentperson_id = parentx.zzperson_id "
                + "FROM ms_person p "
                + "JOIN ms_person_xref childx ON childx.ms_person_id = p.id "
                + "JOIN ms_person_xref parentx ON parentx.ms_person_id = p.parentpersonid "
                + "WHERE child.zzperson_id = childx.zzperson_id "
                + "AND p.parentpersonid > 0",
                null, get_TrxName());

        writeErrorLogIfAny();

        return "Processed " + processed + " ms_person row(s): " + created + " ZZPerson created ("
                + unlinkedUsers + " with no matching AD_User - left unlinked), " + backfilled
                + " ZZParentPerson_ID backfilled, " + errors.size() + " error(s).";
    }

    /**
     * ClearDataFirst=Y support: deletes everything this migration previously created, so the
     * reload that follows starts from a clean slate. Only rows this migration created are
     * touched - identified via the "id" recon column (see class Javadoc), never a blanket
     * delete of zzperson/zzlearner. ZZLearner rows that reference a to-be-deleted ZZPerson
     * are deleted first (FK: zzlearner.zzperson_id -&gt; zzperson), along with their
     * matriculated C_Location, then the ZZPerson rows and their physical/postal C_Location,
     * then both crosswalk tables are truncated so every source row is treated as unmigrated
     * again. Runs as one committed step before the main migration loop starts.
     */
    private void clearPreviouslyMigratedData() throws Exception {
        int personCount = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzperson WHERE id IS NOT NULL");
        int dependentLearnerCount = DB.getSQLValueEx(get_TrxName(),
                "SELECT count(*) FROM zzlearner l JOIN zzperson p ON p.zzperson_id = l.zzperson_id "
                + "WHERE p.id IS NOT NULL");
        addLog("ClearDataFirst=Y: deleting " + personCount + " previously-migrated ZZPerson row(s) and "
                + dependentLearnerCount + " dependent ZZLearner row(s) (plus their C_Location rows) "
                + "before reloading...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_person_clear_locs AS "
                + "SELECT zzphysicallocation_id AS loc_id FROM zzperson "
                + "WHERE id IS NOT NULL AND zzphysicallocation_id IS NOT NULL "
                + "UNION "
                + "SELECT zzpostallocation_id FROM zzperson "
                + "WHERE id IS NOT NULL AND zzpostallocation_id IS NOT NULL "
                + "UNION "
                + "SELECT l.zzmatriculatedlocation_id FROM zzlearner l "
                + "JOIN zzperson p ON p.zzperson_id = l.zzperson_id "
                + "WHERE p.id IS NOT NULL AND l.zzmatriculatedlocation_id IS NOT NULL",
                null, get_TrxName());

        DB.executeUpdateEx(
                "DELETE FROM zzlearner WHERE zzperson_id IN "
                + "(SELECT zzperson_id FROM zzperson WHERE id IS NOT NULL)",
                null, get_TrxName());

        DB.executeUpdateEx("DELETE FROM zzperson WHERE id IS NOT NULL", null, get_TrxName());

        DB.executeUpdateEx(
                "DELETE FROM c_location WHERE c_location_id IN (SELECT loc_id FROM tmp_person_clear_locs)",
                null, get_TrxName());

        DB.executeUpdateEx("DROP TABLE tmp_person_clear_locs", null, get_TrxName());

        DB.executeUpdateEx("TRUNCATE TABLE ms_person_xref", null, get_TrxName());
        DB.executeUpdateEx(
                "CREATE TABLE IF NOT EXISTS ms_learner_xref ("
                + "ms_learner_id integer PRIMARY KEY, zzlearner_id numeric(10) NOT NULL)",
                null, get_TrxName());
        DB.executeUpdateEx("TRUNCATE TABLE ms_learner_xref", null, get_TrxName());

        DB.commit(true, get_TrxName());
        addLog("ClearDataFirst: done - ms_person_xref and ms_learner_xref reset, reload starting fresh.");
    }

    /** @return true if an existing AD_User was found and linked by email, false if left unlinked. */
    private boolean processOneRow(ResultSet rs) throws Exception {
        int msPersonId = rs.getInt("id");
        String firstName = rs.getString("firstname");
        String middleName = rs.getString("middlename");
        String middleName2 = rs.getString("middlename2");
        String surname = rs.getString("surname");
        String initials = rs.getString("initials");
        String idNo = rs.getString("idno");
        int alternateIdTypeId = rs.getInt("alternateidtypeid");
        Timestamp dateOfBirth = rs.getTimestamp("dateofbirth");
        String telephone = rs.getString("telephonenumber");
        String cellphone = rs.getString("cellphonenumber");
        String fax = rs.getString("faxnumber");
        String email = rs.getString("email");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");
        Timestamp popiActStatusDate = rs.getTimestamp("popiactstatusdate");
        String highestEducation = rs.getString("highesteducation");
        String currentOccupation = rs.getString("currentoccupation");
        int yearsInOccupation = rs.getInt("yearsinoccupation");
        String experience = rs.getString("experience");

        String titleDesc = rs.getString("title_desc");
        String genderDesc = rs.getString("gender_desc");
        String equityDesc = rs.getString("equity_desc");
        String popiActStatusDesc = rs.getString("popiactstatus_desc");

        boolean routeToPassportField = (alternateIdTypeId == 2 || alternateIdTypeId == 11); // Passport / RSA ID Number

        String trxName = Trx.createTrxName("MsPersonMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            // Link-only: no AD_User is ever created here (2026-07-08 instruction). If no
            // existing AD_User matches by email, ZZPerson.AD_User_ID is simply left null -
            // AD_User_ID is nullable on ZZPerson, so this is not an error condition.
            int adUserId = 0;
            String emailKey = Util.isEmpty(email, true) ? null : email.trim().toLowerCase();
            if (emailKey != null && emailIndex.containsKey(emailKey)) {
                adUserId = emailIndex.get(emailKey);
            }
            boolean linkedUser = adUserId > 0;

            int physicalLocationId = MigrationSupport.createLocation(getCtx(), trxName, countryId,
                    provinceCrosswalk.get(rs.getInt("physicalprovinceid")),
                    cityCrosswalk.get(rs.getInt("physicalcityid")),
                    rs.getString("physical_city_desc"),
                    rs.getString("physicaladdress1"), rs.getString("physicaladdress2"),
                    rs.getString("physicaladdress3"), rs.getString("physicalcode"));

            int usePhysicalAsPostal = rs.getInt("usephysicalaspostal");
            int postalLocationId;
            if (usePhysicalAsPostal != 0 && physicalLocationId > 0) {
                postalLocationId = physicalLocationId;
            } else {
                postalLocationId = MigrationSupport.createLocation(getCtx(), trxName, countryId,
                        provinceCrosswalk.get(rs.getInt("postalprovinceid")),
                        cityCrosswalk.get(rs.getInt("postalcityid")),
                        rs.getString("postal_city_desc"),
                        rs.getString("postaladdressline1"), rs.getString("postaladdressline2"),
                        rs.getString("postaladdressline3"), rs.getString("postalcode"));
            }

            X_ZZPerson person = new X_ZZPerson(getCtx(), 0, trxName);
            person.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            if (adUserId > 0) {
                person.setAD_User_ID(adUserId);
            }
            person.setIsActive(isDeleted == 0);
            if (!Util.isEmpty(surname, true)) {
                person.setSurname(surname.trim());
            }
            if (!Util.isEmpty(firstName, true)) {
                person.setZZFirstName(firstName.trim());
            }
            if (!Util.isEmpty(middleName, true)) {
                person.setZZMiddleName(middleName.trim());
            }
            if (!Util.isEmpty(middleName2, true)) {
                person.setZZMiddleName2(middleName2.trim());
            }
            if (!Util.isEmpty(initials, true)) {
                person.setZZInitials(initials.trim());
            }
            if (!Util.isEmpty(email, true)) {
                person.setEMail(email.trim());
            }
            if (!Util.isEmpty(telephone, true)) {
                person.setPhone(telephone.trim());
            }
            if (!Util.isEmpty(cellphone, true)) {
                person.setPhone2(cellphone.trim());
            }
            if (!Util.isEmpty(fax, true)) {
                person.setFax(fax.trim());
            }
            if (dateOfBirth != null) {
                person.setBirthday(dateOfBirth);
            }
            String titleCode = MigrationSupport.mapTitle(titleDesc);
            if (titleCode != null) {
                person.setZZLkpTitle(titleCode);
            }
            String genderCode = MigrationSupport.mapGender(genderDesc);
            if (genderCode != null) {
                person.setZZGender(genderCode);
            }
            String equityCode = MigrationSupport.mapEquity(equityDesc);
            if (equityCode != null) {
                person.setZZEquity(equityCode);
            }
            String popiCode = MigrationSupport.mapPopiActStatus(popiActStatusDesc);
            if (popiCode != null) {
                person.setZZPopiActStatus(popiCode);
            }
            if (popiActStatusDate != null) {
                person.setZZPopiActStatusDate(popiActStatusDate);
            }
            if (!Util.isEmpty(currentOccupation, true)) {
                person.setZZCurrentOccupation(currentOccupation.trim());
            }
            if (yearsInOccupation > 0) {
                person.setZZYearsInOccupation(yearsInOccupation);
            }
            int experienceYears = parseDigits(experience);
            if (experienceYears > 0) {
                person.setZZExperience(experienceYears);
            }
            Integer highEduId = Util.isEmpty(highestEducation, true) ? null
                    : highestEducationCrosswalk.get(highestEducation.trim().toLowerCase());
            if (highEduId != null) {
                person.setZZ_LI_HighestEducation_ID(highEduId.intValue());
            }
            Integer schoolEmisId = schoolEmisCrosswalk.get(rs.getInt("schoolemisid"));
            if (schoolEmisId != null) {
                person.setZZLkpSchoolEmis_ID(schoolEmisId.intValue());
            }
            Integer statssaId = statssaAreaCodeCrosswalk.get(rs.getInt("statssaareacodeid"));
            if (statssaId != null) {
                person.setZZLkpStatssaAreaCode_ID(statssaId.intValue());
            }
            Integer disabilityId = disabilityCrosswalk.get(rs.getInt("disabilityid"));
            if (disabilityId != null) {
                person.setZZ_LI_Disability_ID(disabilityId.intValue());
            }
            Integer homeLangId = homeLanguageCrosswalk.get(rs.getInt("homelanguageid"));
            if (homeLangId != null) {
                person.setZZ_LI_HomeLanguage_ID(homeLangId.intValue());
            }
            Integer nationalityId = nationalityCrosswalk.get(rs.getInt("nationalityid"));
            if (nationalityId != null) {
                person.setZZ_Nationality_ID(nationalityId.intValue());
            }
            Integer citizenId = citizenStatusCrosswalk.get(rs.getInt("citizenresidentialstatusid"));
            if (citizenId != null) {
                person.setZZ_LI_CitizenResidentialStatus_ID(citizenId.intValue());
            }
            Integer socioId = socioEconomicCrosswalk.get(rs.getInt("socioeconomicstatusid"));
            if (socioId != null) {
                person.setZZ_LI_SocioEconomicStatus_ID(socioId.intValue());
            }
            Integer altIdTypeId = alternateIdTypeCrosswalk.get(alternateIdTypeId);
            if (altIdTypeId != null) {
                person.setZZ_AlternateIDType_ID(altIdTypeId.intValue());
            }
            if (!Util.isEmpty(idNo, true)) {
                if (routeToPassportField) {
                    person.setZZ_ID_Passport_No(idNo.trim());
                } else {
                    person.setZZOtherIDNo(idNo.trim());
                }
            }
            String seeing = MigrationSupport.mapHealthRatingValue(rs.getString("seeing"));
            if (seeing != null) {
                person.setZZHealthSeeing(seeing);
            }
            String hearing = MigrationSupport.mapHealthRatingValue(rs.getString("hearing"));
            if (hearing != null) {
                person.setZZHealthHearing(hearing);
            }
            String communicating = MigrationSupport.mapHealthRatingValue(rs.getString("communicating"));
            if (communicating != null) {
                person.setZZHealthCommunicating(communicating);
            }
            String walking = MigrationSupport.mapHealthRatingValue(rs.getString("walking"));
            if (walking != null) {
                person.setZZHealthWalking(walking);
            }
            String remembering = MigrationSupport.mapHealthRatingValue(rs.getString("remembering"));
            if (remembering != null) {
                person.setZZHealthRemembering(remembering);
            }
            String selfcare = MigrationSupport.mapHealthRatingValue(rs.getString("selfcare"));
            if (selfcare != null) {
                person.setZZHealthSelfcare(selfcare);
            }
            if (physicalLocationId > 0) {
                person.setZZPhysicalLocation_ID(physicalLocationId);
            }
            if (postalLocationId > 0) {
                person.setZZPostalLocation_ID(postalLocationId);
            }
            // ZZCVFileName / ZZPhotographFileName / ZZLastSchoolYear / ZZMigrationCode /
            // ZZMigrateValues intentionally left unset - see class Javadoc.

            person.saveEx();
            int zzPersonId = person.get_ID();

            if (created != null) {
                MigrationSupport.stampCreatedUpdated("zzperson", "zzperson_id", zzPersonId,
                        created, DEFAULT_CREATED_BY, updated, DEFAULT_CREATED_BY, msPersonId, trxName);
            }

            DB.executeUpdateEx("INSERT INTO ms_person_xref (ms_person_id, zzperson_id) VALUES (?, ?)",
                    new Object[] { msPersonId, zzPersonId }, trxName);

            trx.commit(true);
            return linkedUser;
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private void logError(int msPersonId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_person.id=" + msPersonId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-person-errors-" + ts + ".txt");
        try (PrintWriter out = new PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(logFile)))) {
            for (String err : errors) {
                out.println(err);
            }
            addLog("Error log written to: " + logFile.getAbsolutePath()
                    + (errors.size() >= MAX_LOGGED_ERRORS ? " (truncated at " + MAX_LOGGED_ERRORS + ")" : ""));
        } catch (Exception e) {
            addLog("WARN: could not write error log: " + e.getMessage());
        }
    }

    /** Extracts the leading/embedded digits from strings like "18years" or "29". */
    private static int parseDigits(String s) {
        if (Util.isEmpty(s, true)) {
            return 0;
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.adempiere.base.annotation.Parameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process(name = "za.ntier.process.DeleteBursaryApplicationsByDocumentNo")
public class DeleteBursaryApplicationsByDocumentNo extends SvrProcess {

    @Parameter(name = "DocumentNos")
    private String documentNosText;

    // Children must be deleted before the zz_application_form parent.
    private static final String[] CHILD_TABLES = {
        "zz_formcontact",
        "zz_formdiscipline",
        "zzsubannex",
        "zzannexure",
        "zzdocumentuploadfile",
        "zzlearnersapplied",
        "zzdetailsmallbusinesse",
        "zzstandardsetting",
        "zzlearningmaterial",
        "zzbankingdetails"
    };

    @Override
    protected void prepare() {
    }

    @Override
    protected String doIt() throws Exception {
        List<String> documentNos = parseDocumentNos(documentNosText);
        if (documentNos.isEmpty())
            throw new AdempiereUserError("DocumentNos must contain at least one document number.");

        String placeholders = placeholders(documentNos.size());
        Object[] params = documentNos.toArray();

        Set<String> found = findExistingDocumentNos(placeholders, params);
        List<String> missing = new ArrayList<>();
        for (String docNo : documentNos) {
            if (!found.contains(docNo))
                missing.add(docNo);
        }
        if (!missing.isEmpty())
            throw new AdempiereUserError("Aborted, nothing deleted - these document numbers were not found in zz_application_form: "
                    + String.join(", ", missing));

        StringBuilder result = new StringBuilder();
        result.append("DocumentNos=").append(String.join(",", documentNos)).append(" | ");

        for (String table : CHILD_TABLES) {
            String sql = "DELETE FROM " + table + " t USING zz_application_form f "
                    + "WHERE f.zz_application_form_id = t.zz_application_form_id AND f.documentno IN (" + placeholders + ")";
            int deleted = DB.executeUpdateEx(sql, params, get_TrxName());
            addLog(table + ": deleted " + deleted + " row(s)");
            result.append(table).append("=").append(deleted).append(", ");
        }

        int deletedParents = DB.executeUpdateEx(
                "DELETE FROM zz_application_form WHERE documentno IN (" + placeholders + ")",
                params, get_TrxName());
        addLog("zz_application_form: deleted " + deletedParents + " row(s)");
        result.append("zz_application_form=").append(deletedParents);

        return result.toString();
    }

    private List<String> parseDocumentNos(String text) {
        List<String> result = new ArrayList<>();
        if (text == null)
            return result;
        for (String token : text.split("[,\\r\\n]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed))
                result.add(trimmed);
        }
        return result;
    }

    private String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private Set<String> findExistingDocumentNos(String placeholders, Object[] params) throws Exception {
        Set<String> found = new LinkedHashSet<>();
        String sql = "SELECT documentno FROM zz_application_form WHERE documentno IN (" + placeholders + ")";
        try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    found.add(rs.getString("documentno"));
                }
            }
        }
        return found;
    }
}

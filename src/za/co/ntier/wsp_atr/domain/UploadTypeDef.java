package za.co.ntier.wsp_atr.domain;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;

public enum UploadTypeDef {
    WSP_ATR_REPORT(
        X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadWSP_ATRReport,
        "Upload WSP-ATR Report"
    ),
    SIGNED_MINUTES(
        X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadSignedMinutes,
        "Upload Signed Minutes"
    ),
    ATTENDANCE_REGISTER(
        X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadAttendanceRegister,
        "Upload Attendance Register"
    );

    public final String code;
    public final String initialLabel;

    UploadTypeDef(String code, String initialLabel) {
        this.code = code;
        this.initialLabel = initialLabel;
    }
}

package za.ntier.utils;

public final class MQAConstants {

    /**
     * List of table/view names that use UUIDs for keys and require 
     * custom zoom or multi-selection handling in Info Windows.
     * Add new tables to this array as needed.
     */
    public static final String[] CUSTOM_ZOOM_TABLES = {
        "ZZ_QCTO_APPLICATION_INFO_V",
        "ZZ_AuditSchedule_v"
    };

    private MQAConstants() {
        // Private constructor to prevent instantiation
    }
}

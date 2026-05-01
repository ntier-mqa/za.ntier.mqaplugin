package za.ntier.utils;

import java.util.HashMap;
import java.util.Map;

public final class MonthUtil {
    private MonthUtil() {}

    private static final Map<String, Integer> NAME_TO_ORDER = new HashMap<>();
    private static final String[] ORDER_TO_NAME = {
        null,                                                           // index 0 unused
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };
    static {
        NAME_TO_ORDER.put("january",   1);
        NAME_TO_ORDER.put("february",  2);
        NAME_TO_ORDER.put("march",     3);
        NAME_TO_ORDER.put("april",     4);
        NAME_TO_ORDER.put("may",       5);
        NAME_TO_ORDER.put("june",      6);
        NAME_TO_ORDER.put("july",      7);
        NAME_TO_ORDER.put("august",    8);
        NAME_TO_ORDER.put("september", 9);
        NAME_TO_ORDER.put("october",  10);
        NAME_TO_ORDER.put("november", 11);
        NAME_TO_ORDER.put("december", 12);
    }

    /**
     * Returns 1-12 for a month name (case-insensitive) or numeric code ("1"-"12"),
     * or 0 if unrecognised.
     */
    public static int order(String month) {
        if (month == null) return 0;
        String s = month.trim();
        // Handle numeric codes stored by iDempiere list columns (e.g. "12" for December)
        try {
            int n = Integer.parseInt(s);
            if (n >= 1 && n <= 12) return n;
        } catch (NumberFormatException ignored) {}
        // Handle full month names
        return NAME_TO_ORDER.getOrDefault(s.toLowerCase(), 0);
    }

    /**
     * Converts a month value (numeric code "1"-"12" or name) to the full English
     * month name (e.g. "12" → "December", "december" → "December").
     * Returns the original value unchanged if it cannot be resolved.
     */
    public static String toName(String month) {
        int ord = order(month);
        if (ord >= 1 && ord <= 12) return ORDER_TO_NAME[ord];
        return month == null ? "" : month.trim();
    }
}

package za.ntier.utils;

import java.util.HashMap;
import java.util.Map;

public final class MonthUtil {
    private MonthUtil() {}

    private static final Map<String, Integer> ORDER = new HashMap<>();
    static {
        ORDER.put("january",   1);
        ORDER.put("february",  2);
        ORDER.put("march",     3);
        ORDER.put("april",     4);
        ORDER.put("may",       5);
        ORDER.put("june",      6);
        ORDER.put("july",      7);
        ORDER.put("august",    8);
        ORDER.put("september", 9);
        ORDER.put("october",  10);
        ORDER.put("november", 11);
        ORDER.put("december", 12);
    }

    /** Returns 1-12 for a month name (case-insensitive), or 0 if unrecognised. */
    public static int order(String monthName) {
        if (monthName == null) return 0;
        return ORDER.getOrDefault(monthName.trim().toLowerCase(), 0);
    }
}

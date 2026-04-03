package za.ntier.utils;

public class StringUtil {

    /**
     * Checks if a target string equals any of the provided strings, ignoring case.
     * 
     * @param target The string to check
     * @param compareStrings The list of strings to compare against
     * @return true if the target is equal to any of the strings, false otherwise
     */
    public static boolean equalsAnyIgnoreCase(String target, String... compareStrings) {
        if (target == null || compareStrings == null) {
            return false;
        }
        for (String compareString : compareStrings) {
            if (target.equalsIgnoreCase(compareString)) {
                return true;
            }
        }
        return false;
    }
}

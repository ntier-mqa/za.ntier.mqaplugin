package za.ntier.utils;

import java.math.BigDecimal;

public final class DescriptionBuilder {
    private DescriptionBuilder() {}

    /**
     * Builds the batch line description.
     * Example: "MG levy year 2025 month November SDL L010745364 (3 lines) amt=-240.00"
     */
    public static String buildLineDescription(String tag, String year, String month,
                                              String sdlNo, int lineCount, BigDecimal total) {
        return tag + " levy year " + year
                + " month " + month
                + " SDL " + sdlNo
                + " (" + lineCount + (lineCount == 1 ? " line" : " lines") + ")"
                + " amt=" + total.toPlainString();
    }
}

package de.shopplugin.util;

import java.util.Locale;

/**
 * Formatiert Preise mit K (Tausend) / M (Million) Suffix.
 *
 * Beispiele:
 *   999      -> "999"
 *   250000   -> "250K"
 *   5000000  -> "5M"
 *   1500000  -> "1.5M"
 */
public final class PriceFormatter {

    private PriceFormatter() {
    }

    public static String formatPrice(double amount) {
        if (amount >= 1_000_000d) {
            return trimTrailingZero(amount / 1_000_000d) + "M";
        } else if (amount >= 1_000d) {
            return trimTrailingZero(amount / 1_000d) + "K";
        } else {
            return String.valueOf((long) amount);
        }
    }

    private static String trimTrailingZero(double value) {
        double rounded = Math.round(value * 10d) / 10d;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.US, "%.1f", rounded);
    }
}

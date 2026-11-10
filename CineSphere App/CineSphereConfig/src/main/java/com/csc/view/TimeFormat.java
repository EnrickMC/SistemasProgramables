package com.csc.view;

import java.util.Locale;

/** Utilidades para mostrar tiempos (segundos) como texto. */
public final class TimeFormat {

    private TimeFormat() {}

    /** Formato mm:ss.d, p. ej. 01:23.4 */
    public static String format(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0) seconds = 0;
        long tenths = Math.round(seconds * 10);
        long minutes = tenths / 600;
        double rest = (tenths % 600) / 10.0;
        return String.format(Locale.ROOT, "%02d:%04.1f", minutes, rest);
    }

    /** Formato compacto para la regla: 1:05 o 1:05.5 */
    public static String ruler(double seconds, boolean decimals) {
        if (decimals) {
            long tenths = Math.round(seconds * 10);
            return String.format(Locale.ROOT, "%d:%04.1f", tenths / 600, (tenths % 600) / 10.0);
        }
        long total = Math.round(seconds);
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }
}

package com.example.campusrunner.ui

import java.util.Locale

/** Formats a distance in meters as a compact human-readable string. */
internal fun formatDistance(distance: Double): String {
    return if (distance >= 1000) {
        String.format(Locale.US, "%.2f km", distance / 1000.0)
    } else {
        String.format(Locale.US, "%.0f m", distance)
    }
}

/** Formats a number, dropping trailing zeros (e.g. 5.26 -> "5.26", 4.0 -> "4"). */
internal fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

/** Formats a WGS-84 coordinate with 6 decimals (matches the picker input format). */
internal fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.6f", value)
}

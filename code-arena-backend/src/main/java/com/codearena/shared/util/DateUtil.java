package com.codearena.shared.util;

import java.time.Instant;

public final class DateUtil {
    private DateUtil() {
    }

    /**
     * Returns current UTC instant.
     *
     * @return current instant
     */
    public static Instant nowUtc() {
        // TODO: Add centralized timezone and formatting helpers.
        return Instant.now();
    }
}

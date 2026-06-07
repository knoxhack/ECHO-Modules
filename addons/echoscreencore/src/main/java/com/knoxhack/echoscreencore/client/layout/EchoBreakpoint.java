package com.knoxhack.echoscreencore.client.layout;

import java.util.Locale;

public enum EchoBreakpoint {
    XS(320),
    SM(480),
    MD(720),
    LG(960),
    XL(1280);

    private final int width;

    EchoBreakpoint(int width) {
        this.width = width;
    }

    public int width() {
        return width;
    }

    public static int threshold(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.strip().toLowerCase(Locale.ROOT);
        for (EchoBreakpoint breakpoint : values()) {
            if (breakpoint.name().toLowerCase(Locale.ROOT).equals(value)) {
                return breakpoint.width();
            }
        }
        try {
            return Math.max(0, Math.round(Float.parseFloat(value.replace("px", ""))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static EchoBreakpoint active(int width) {
        EchoBreakpoint active = XS;
        for (EchoBreakpoint breakpoint : values()) {
            if (width >= breakpoint.width()) {
                active = breakpoint;
            }
        }
        return active;
    }
}

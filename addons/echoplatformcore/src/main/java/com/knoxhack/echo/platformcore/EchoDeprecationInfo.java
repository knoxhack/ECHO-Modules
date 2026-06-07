package com.knoxhack.echo.platformcore;

public record EchoDeprecationInfo(
        boolean deprecated,
        String sinceVersion,
        String removalVersion,
        String replacement,
        String note
) {
    public EchoDeprecationInfo {
        sinceVersion = EchoContractGuards.optionalText(sinceVersion);
        removalVersion = EchoContractGuards.optionalText(removalVersion);
        replacement = EchoContractGuards.optionalText(replacement);
        note = EchoContractGuards.optionalText(note);
    }

    public static EchoDeprecationInfo notDeprecated() {
        return new EchoDeprecationInfo(false, "", "", "", "");
    }

    public static EchoDeprecationInfo deprecated(String sinceVersion, String removalVersion, String replacement, String note) {
        return new EchoDeprecationInfo(true, sinceVersion, removalVersion, replacement, note);
    }
}

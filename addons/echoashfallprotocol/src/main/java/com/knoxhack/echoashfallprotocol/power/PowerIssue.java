package com.knoxhack.echoashfallprotocol.power;

public enum PowerIssue {
    OK("power_issue.EchoAshfallProtocol.ok", "power_issue.EchoAshfallProtocol.ok.hint", false),
    NO_LINK("power_issue.EchoAshfallProtocol.no_link", "power_issue.EchoAshfallProtocol.no_link.hint", true),
    NETWORK_EMPTY("power_issue.EchoAshfallProtocol.network_empty", "power_issue.EchoAshfallProtocol.network_empty.hint", true),
    CABLE_BOTTLENECK("power_issue.EchoAshfallProtocol.cable_bottleneck", "power_issue.EchoAshfallProtocol.cable_bottleneck.hint", true),
    PRIORITY_PAUSED("power_issue.EchoAshfallProtocol.priority_paused", "power_issue.EchoAshfallProtocol.priority_paused.hint", true),
    BLACKOUT_STORAGE_ONLY("power_issue.EchoAshfallProtocol.blackout_storage_only", "power_issue.EchoAshfallProtocol.blackout_storage_only.hint", true),
    CONTROLLER_DISABLED("power_issue.EchoAshfallProtocol.controller_disabled", "power_issue.EchoAshfallProtocol.controller_disabled.hint", true),
    LOCAL_BUFFER_EMPTY("power_issue.EchoAshfallProtocol.local_buffer_empty", "power_issue.EchoAshfallProtocol.local_buffer_empty.hint", true);

    private final String translationKey;
    private final String hintKey;
    private final boolean blocking;

    PowerIssue(String translationKey, String hintKey, boolean blocking) {
        this.translationKey = translationKey;
        this.hintKey = hintKey;
        this.blocking = blocking;
    }

    public String translationKey() {
        return translationKey;
    }

    public String hintKey() {
        return hintKey;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public int code() {
        return ordinal();
    }

    public static PowerIssue fromCode(int code) {
        PowerIssue[] values = values();
        return code >= 0 && code < values.length ? values[code] : OK;
    }
}

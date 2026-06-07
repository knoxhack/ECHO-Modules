package com.knoxhack.echorecovery.api;

public enum EchoRecoveryMode {
    NORMAL("normal"),
    SAFE_MODE("safe_mode"),
    RECOVERY_MODE("recovery_mode"),
    VALIDATION_ONLY("validation_only"),
    SUPPORT_EXPORT("support_export"),
    LAST_KNOWN_GOOD("last_known_good");

    private final String serializedName;

    EchoRecoveryMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

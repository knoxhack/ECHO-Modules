package com.knoxhack.echoprimecore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class PrimeConfig {
    public static final EchoNativeConfigSpec SPEC;
    private static final EchoNativeConfigSpec.BooleanValue GIVE_FIELD_MANUAL_ON_FIRST_JOIN;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
        builder.push("first_join");
        GIVE_FIELD_MANUAL_ON_FIRST_JOIN = builder
                .comment("Give new players the Prime Field Manual during the Prime first-join flow.")
                .define("give_field_manual_on_first_join", true);
        builder.pop();
        SPEC = builder.build();
    }

    private PrimeConfig() {
    }

    public static void registerEchoConfig() {
        // Native config host consumes SPEC through AdapterCore when a host is present.
    }

    public static boolean giveFieldManualOnFirstJoin() {
        return GIVE_FIELD_MANUAL_ON_FIRST_JOIN.get();
    }
}

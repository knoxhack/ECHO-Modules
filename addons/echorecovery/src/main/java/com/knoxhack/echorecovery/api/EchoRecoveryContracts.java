package com.knoxhack.echorecovery.api;

import java.util.List;

public final class EchoRecoveryContracts {
    public static final List<EchoRecoveryTrigger> REQUIRED_TRIGGERS = List.of(EchoRecoveryTrigger.values());
    public static final List<EchoRecoveryActionKind> REQUIRED_ACTIONS = List.of(EchoRecoveryActionKind.values());
    public static final EchoSafeModeProfile STANDARD_SAFE_MODE = EchoSafeModeProfile.standard();

    private EchoRecoveryContracts() {
    }
}

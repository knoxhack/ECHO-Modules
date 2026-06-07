package com.knoxhack.echoruntimeguard.api;

public interface RuntimeGuardAwareEntity {
    boolean canThrottleAi();

    RuntimeGuardEntityPriority getRuntimePriority();
}

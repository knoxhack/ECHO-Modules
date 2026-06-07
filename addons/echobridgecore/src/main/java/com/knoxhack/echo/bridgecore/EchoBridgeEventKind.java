package com.knoxhack.echo.bridgecore;

public enum EchoBridgeEventKind {
    SESSION_CREATED("session_created"),
    PROMPT_SUBMITTED("prompt_submitted"),
    JOB_STARTED("job_started"),
    LOG_CHUNK("log_chunk"),
    DIAGNOSTIC_UPDATE("diagnostic_update"),
    SAFE_ACTION_REQUESTED("safe_action_requested"),
    CONFIRMATION_REQUIRED("confirmation_required"),
    JOB_COMPLETED("job_completed"),
    NEXT_PROMPT_GENERATED("next_prompt_generated");

    private final String serializedName;

    EchoBridgeEventKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

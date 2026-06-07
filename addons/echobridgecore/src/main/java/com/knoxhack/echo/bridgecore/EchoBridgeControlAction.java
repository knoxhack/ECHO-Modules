package com.knoxhack.echo.bridgecore;

public enum EchoBridgeControlAction {
    CREATE_SESSION("create_session"),
    SUBMIT_PROMPT("submit_prompt"),
    START_CODEX_JOB("start_codex_job"),
    STREAM_STDOUT("stream_stdout"),
    STREAM_STDERR("stream_stderr"),
    STREAM_DIAGNOSTICS("stream_diagnostics"),
    STREAM_JOB_STATE("stream_job_state"),
    REQUEST_CONFIRMATION("request_confirmation"),
    RESUME_AFTER_CONFIRMATION("resume_after_confirmation"),
    CANCEL_JOB("cancel_job"),
    SAVE_RUN_REPORT("save_run_report"),
    GENERATE_NEXT_PHASE_PROMPT("generate_next_phase_prompt"),
    EXPORT_AI_TASK_REPORT("export_ai_task_report");

    private final String serializedName;

    EchoBridgeControlAction(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

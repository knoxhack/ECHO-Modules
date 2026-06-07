package com.knoxhack.echomultiblockcore.api;

public record AutomationEffectResult(Status status, String reason) {
    public AutomationEffectResult {
        status = status == null ? Status.ALLOW : status;
        reason = reason == null ? "" : reason.strip();
    }

    public static AutomationEffectResult allow() {
        return new AutomationEffectResult(Status.ALLOW, "");
    }

    public static AutomationEffectResult allow(String diagnostic) {
        return new AutomationEffectResult(Status.ALLOW, diagnostic);
    }

    public static AutomationEffectResult block(String reason) {
        return new AutomationEffectResult(Status.BLOCK, reason == null || reason.isBlank() ? "Automation effect blocked task." : reason);
    }

    public static AutomationEffectResult fail(String reason) {
        return new AutomationEffectResult(Status.FAIL, reason == null || reason.isBlank() ? "Automation effect failed." : reason);
    }

    public boolean allowed() {
        return status == Status.ALLOW;
    }

    public boolean blocked() {
        return status == Status.BLOCK;
    }

    public boolean failed() {
        return status == Status.FAIL;
    }

    public enum Status {
        ALLOW,
        BLOCK,
        FAIL
    }
}

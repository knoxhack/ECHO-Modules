package com.knoxhack.echo.agentcore;

public enum EchoAiProtectionLevel {
    READ_ONLY("read_only", true),
    REVIEW_REQUIRED("review_required", true),
    GENERATED_ONLY("generated_only", false),
    SAFE_EDIT_ZONE("safe_edit_zone", false),
    BLOCKED("blocked", true);

    private final String serializedName;
    private final boolean requiresHumanReview;

    EchoAiProtectionLevel(String serializedName, boolean requiresHumanReview) {
        this.serializedName = serializedName;
        this.requiresHumanReview = requiresHumanReview;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean requiresHumanReview() {
        return requiresHumanReview;
    }
}

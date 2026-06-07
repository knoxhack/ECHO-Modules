package com.knoxhack.echo.agentcore;

public enum EchoAiAcceptanceCriterionType {
    BUILD_PASSES("build_passes"),
    TEST_PASSES("test_passes"),
    FILE_EXISTS("file_exists"),
    SCHEMA_VALID("schema_valid"),
    FEATURE_PRESENT("feature_present"),
    DIAGNOSTIC_ABSENT("diagnostic_absent"),
    SCREENSHOT_PASSES("screenshot_passes"),
    ASSET_EXISTS("asset_exists"),
    PACK_VALIDATES("pack_validates"),
    MODULE_COMPILES("module_compiles"),
    DOCS_UPDATED("docs_updated"),
    MANUAL_REVIEW_REQUIRED("manual_review_required");

    private final String serializedName;

    EchoAiAcceptanceCriterionType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

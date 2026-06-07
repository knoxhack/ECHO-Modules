package com.knoxhack.echo.validationcore;

public enum EchoValidationCategory {
    MODULE_MANIFEST("module_manifest"),
    MODULE_DEPENDENCY("module_dependency"),
    MODULE_ROLE("module_role"),
    FEATURE_PROVIDER("feature_provider"),
    FEATURE_CONSUMER("feature_consumer"),
    CAPABILITY("capability"),
    API_STABILITY("api_stability"),
    DEPRECATED_API("deprecated_api"),
    TRUST_LEVEL("trust_level"),
    PERMISSIONS("permissions"),
    PACK_PROFILE("pack_profile"),
    PACK_VARIANT("pack_variant"),
    PACK_CHANNEL("pack_channel"),
    LOCKFILE("lockfile"),
    SAVE_COMPATIBILITY("save_compatibility"),
    ASSET_REFERENCE("asset_reference"),
    MISSING_TEXTURE("missing_texture"),
    MISSING_MODEL("missing_model"),
    MISSING_SOUND("missing_sound"),
    MISSING_ICON("missing_icon"),
    CONTENT_REFERENCE("content_reference"),
    RECIPE_REFERENCE("recipe_reference"),
    MISSION_REFERENCE("mission_reference"),
    OBJECTIVE_REFERENCE("objective_reference"),
    FACTION_REFERENCE("faction_reference"),
    NPC_REFERENCE("npc_reference"),
    UI_LAYOUT("ui_layout"),
    THEME_TOKEN("theme_token"),
    RUNTIME_HEALTH("runtime_health"),
    SERVER_CLIENT("server_client"),
    AI_TASK("ai_task"),
    SAFE_ACTION("safe_action"),
    BRIDGE_CONTRACT("bridge_contract"),
    SCHEMA("schema"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoValidationCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

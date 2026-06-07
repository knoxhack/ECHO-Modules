package com.knoxhack.echo.schemacore;

public enum EchoSchemaDocumentKind {
    ECHO_MOD_MANIFEST("echo_mod_manifest"),
    ECHO_AI_METADATA("echo_ai_metadata"),
    ECHO_PACK_PROFILE("echo_pack_profile"),
    ECHO_LOCKFILE("echo_lockfile"),
    ECHO_STATE("echo_state"),
    ECHO_VARIANT("echo_variant"),
    ECHO_CHANNEL("echo_channel"),
    ECHO_REPAIR_PLAN("echo_repair_plan"),
    ECHO_SUPPORT_BUNDLE("echo_support_bundle"),
    ECHO_SAFE_ACTIONS("echo_safe_actions"),
    ECHO_AI_TASK("echo_ai_task"),
    ECHO_PROMPT_BUNDLE("echo_prompt_bundle"),
    SCREEN_LAYOUT("screen_layout"),
    THEME_TOKENS("theme_tokens"),
    MISSION("mission"),
    OBJECTIVE("objective"),
    ROUTE("route"),
    REWARD("reward"),
    RECIPE("recipe"),
    MACHINE_RECIPE("machine_recipe"),
    WEATHER_EVENT("weather_event"),
    STATUS_EFFECT("status_effect"),
    REGION("region"),
    POI("poi"),
    FACTION("faction"),
    DIALOGUE_TREE("dialogue_tree"),
    NPC_PROFILE("npc_profile"),
    LOOT_PROFILE("loot_profile"),
    STRUCTURE_PROFILE("structure_profile"),
    SOUND_PROFILE("sound_profile"),
    RENDER_PROFILE("render_profile"),
    LENS_SCAN("lens_scan"),
    HOLOMAP_LAYER("holomap_layer"),
    TUTORIAL_CARD("tutorial_card"),
    GUIDE_PAGE("guide_page"),
    CODEX_ENTRY("codex_entry"),
    LORE_ENTRY("lore_entry"),
    ASSET_MANIFEST("asset_manifest"),
    TEXTUREFORGE_PROMPT_SHEET("textureforge_prompt_sheet");

    private final String serializedName;

    EchoSchemaDocumentKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

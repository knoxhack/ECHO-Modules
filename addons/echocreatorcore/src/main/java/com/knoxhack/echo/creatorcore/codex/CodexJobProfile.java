package com.knoxhack.echo.creatorcore.codex;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum CodexJobProfile {
    MOB_MODEL("mob_model", "Mob model", "Generate or repair RenderCore mob model assets, textures, animations, and validation coverage."),
    ENTITY_RENDERER("entity_renderer", "Entity renderer", "Generate or repair Java entity renderer integration and RenderCore profile wiring."),
    BLOCK_MODEL("block_model", "Block model", "Generate or repair blockstates, block models, item models, and textures."),
    BLOCK_ENTITY_MODEL("block_entity_model", "Block entity model", "Generate or repair block entity render assets and RenderCore profile wiring."),
    MULTIBLOCK_VISUAL("multiblock_visual", "Multiblock visual", "Generate or repair multiblock visuals, controller assets, holograms, and error states."),
    RENDERCORE_PROFILE("rendercore_profile", "RenderCore profile", "Generate or repair RenderCore visual, animation, particle, budget, fallback, and QA profiles."),
    ASSET_REPAIR("asset_repair", "Asset repair", "Inspect and repair broken asset references, missing textures, malformed JSON, and validation failures.");

    private final String id;
    private final String title;
    private final String defaultPrompt;

    CodexJobProfile(String id, String title, String defaultPrompt) {
        this.id = id;
        this.title = title;
        this.defaultPrompt = defaultPrompt;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String defaultPrompt() {
        return defaultPrompt;
    }

    public static Optional<CodexJobProfile> byId(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(profile -> profile.id.equals(normalized)).findFirst();
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(CodexJobProfile::id).toList();
    }
}

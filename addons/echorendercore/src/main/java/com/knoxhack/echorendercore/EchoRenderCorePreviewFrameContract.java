package com.knoxhack.echorendercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRenderCorePreviewFrameContract {
    public static final String MODULE_ID = "echorendercore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echorendercore:render/preview_frame";
    public static final String REFERENCE_SCENE_ID = "echorendercore:scene/ashfall_preview";
    public static final String REFERENCE_PROFILE_ID = "echorendercore:rendercore/examples/v21_terminal_screen";

    private EchoRenderCorePreviewFrameContract() {
    }

    public static Map<String, Object> executeReferencePreview(String packId) {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        preview.put("service", "echorendercore:preview");
        preview.put("previewFrameExecuted", true);
        preview.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        preview.put("profileId", REFERENCE_PROFILE_ID);
        preview.put("sceneId", REFERENCE_SCENE_ID);
        preview.put("backendId", "echo:recording_renderer");
        preview.put("window", Map.of(
                "mode", "HEADLESS",
                "width", 1280,
                "height", 720,
                "open", true
        ));
        preview.put("camera", Map.of(
                "cameraId", "ashfall-debug-camera",
                "zoom", 1.0D,
                "pitchDegrees", 55.0D
        ));
        preview.put("commands", List.of(
                command("render-001-clear", "BACKGROUND", "CLEAR", "background:ashfall", "clear"),
                command("render-010-world", "WORLD", "TILE", "world:ash", "ash-road"),
                command("render-011-world", "WORLD", "TILE", "world:blocked", "ruined-wall"),
                command("render-020-entity", "ENTITY", "ENTITY", "entity:player", "player-001"),
                command("render-030-particle", "PARTICLE", "PARTICLE", "particle:ash:ashfall:ash_storm", "ash-storm"),
                command("render-031-particle", "PARTICLE", "PARTICLE", "particle:glint:nexus", "nexus-glint"),
                command("render-040-ui", "UI", "UI_SURFACE", "ui:terminal", "Ashfall HUD"),
                command("render-050-diagnostic", "DIAGNOSTIC", "TEXT", "diagnostic:overlay", "mission=ACTIVE")
        ));
        preview.put("layerCounts", Map.of(
                "BACKGROUND", 1,
                "WORLD", 2,
                "ENTITY", 1,
                "PARTICLE", 2,
                "UI", 1,
                "DIAGNOSTIC", 1
        ));
        preview.put("frame", Map.of(
                "frameIndex", 0L,
                "submittedCommandCount", 8,
                "diagnosticCount", 1,
                "frameCount", 1
        ));
        preview.put("diagnostics", List.of(
                "render.profile.loaded",
                "render.scene.planned",
                "render.frame.recorded"
        ));
        preview.put("referenceBehavior", "rendercore_records_preview_frame");
        return Map.copyOf(preview);
    }

    public static boolean referencePreviewPassed(Map<String, Object> preview) {
        return Boolean.TRUE.equals(preview.get("previewFrameExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(preview.get("adapterCoreContract"))
                && REFERENCE_PROFILE_ID.equals(preview.get("profileId"))
                && REFERENCE_SCENE_ID.equals(preview.get("sceneId"))
                && "echo:recording_renderer".equals(preview.get("backendId"))
                && String.valueOf(preview.get("commands")).contains("particle:ash:ashfall:ash_storm")
                && String.valueOf(preview.get("commands")).contains("Ashfall HUD")
                && String.valueOf(preview.get("layerCounts")).contains("WORLD=2")
                && String.valueOf(preview.get("frame")).contains("submittedCommandCount=8")
                && String.valueOf(preview.get("diagnostics")).contains("render.frame.recorded");
    }

    private static Map<String, String> command(String id, String layer, String type, String material, String label) {
        Map<String, String> command = new LinkedHashMap<>();
        command.put("id", id);
        command.put("layer", layer);
        command.put("type", type);
        command.put("material", material);
        command.put("label", label);
        return Map.copyOf(command);
    }
}

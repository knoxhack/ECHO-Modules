package com.knoxhack.echoriftworlds;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRiftWorldsPocketLifecycleContract {
    public static final String MODULE_ID = "echoriftworlds";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoriftworlds:worldgen/pocket_rift_lifecycle";
    public static final String REFERENCE_RIFT_ID = "echoriftworlds:pocket_rift/cache_echo_001";
    public static final String REFERENCE_DIMENSION_ID = "echoriftworlds:pocket_rift";

    private EchoRiftWorldsPocketLifecycleContract() {
    }

    public static Map<String, Object> executeReferenceLifecycle(String packId) {
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        lifecycle.put("service", "echoriftworlds:pocket_rift_lifecycle_service");
        lifecycle.put("pocketLifecycleExecuted", true);
        lifecycle.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        lifecycle.put("riftSnapshot", Map.of(
                "riftId", REFERENCE_RIFT_ID,
                "riftEvent", "echoriftworlds:rift_event/cache_echo",
                "dimension", REFERENCE_DIMENSION_ID,
                "center", "144,72,-32",
                "returnDimension", "minecraft:overworld",
                "returnPosition", "144,64,-32",
                "expiresAfterTicks", 24000L,
                "state", "active"
        ));
        lifecycle.put("chamberPlan", List.of(
                chamberStep("clear_chamber_volume", "CLEAR_VOLUME", "7x6x7", false),
                chamberStep("build_bedrock_floor", "PLACE_BOUNDARY", "bedrock_floor", false),
                chamberStep("seal_tinted_glass_shell", "PLACE_BOUNDARY", "tinted_glass_shell", false),
                chamberStep("place_amethyst_anchor", "PLACE_ANCHOR", "amethyst_center", false),
                chamberStep("spawn_cache_shards", "SPAWN_REWARD", "amethyst_shard_x2", false)
        ));
        lifecycle.put("hazardProfile", Map.of(
                "hazardId", "echoriftworlds:hazard/rift_static",
                "pressure", 3,
                "effects", List.of("slow_falling", "absorption", "night_vision"),
                "hostileSignal", "endermite_echo",
                "destructiveActions", 0
        ));
        lifecycle.put("storyRoute", Map.of(
                "chapter", "signalos:chapter/cache_handoff",
                "storyFlag", "echoriftworlds:story_flag/cache_echo_seen",
                "missionSubject", "echoriftworlds:pocket_rift",
                "missionAction", "pocket_encounter"
        ));
        lifecycle.put("exitPlan", List.of(
                exitAction("return_anchor", "TELEPORT_RETURN", "minecraft:overworld@144,65,-32"),
                exitAction("expire_instance", "EXPIRE_AFTER_TTL", "24000"),
                exitAction("cleanup_chamber", "CLEANUP_VOLUME", "post_return")
        ));
        lifecycle.put("diagnostics", List.of(
                "riftworlds.rift.triggered",
                "riftworlds.chamber.planned",
                "riftworlds.hazard.profile_applied",
                "riftworlds.return_anchor.marked"
        ));
        lifecycle.put("referenceBehavior", "riftworlds_builds_pocket_rift_lifecycle");
        return Map.copyOf(lifecycle);
    }

    public static boolean referenceLifecyclePassed(Map<String, Object> lifecycle) {
        return Boolean.TRUE.equals(lifecycle.get("pocketLifecycleExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(lifecycle.get("adapterCoreContract"))
                && String.valueOf(lifecycle.get("riftSnapshot")).contains(REFERENCE_RIFT_ID)
                && String.valueOf(lifecycle.get("riftSnapshot")).contains(REFERENCE_DIMENSION_ID)
                && String.valueOf(lifecycle.get("chamberPlan")).contains("PLACE_ANCHOR")
                && String.valueOf(lifecycle.get("hazardProfile")).contains("rift_static")
                && String.valueOf(lifecycle.get("storyRoute")).contains("cache_handoff")
                && String.valueOf(lifecycle.get("exitPlan")).contains("TELEPORT_RETURN")
                && String.valueOf(lifecycle.get("diagnostics")).contains("riftworlds.return_anchor.marked");
    }

    private static Map<String, Object> chamberStep(
            String id,
            String kind,
            String target,
            boolean destructive
    ) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("id", id);
        step.put("kind", kind);
        step.put("target", target);
        step.put("destructive", destructive);
        return Map.copyOf(step);
    }

    private static Map<String, Object> exitAction(String id, String kind, String target) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("kind", kind);
        action.put("target", target);
        action.put("requiresConfirmation", false);
        return Map.copyOf(action);
    }
}

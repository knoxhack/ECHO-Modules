package com.knoxhack.echodatacore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoDataCoreRuntimeProfileSyncContract {
    public static final String MODULE_ID = "echodatacore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echodatacore:data/runtime_profile_sync";
    public static final String REFERENCE_PLAYER_ID = "ashfall:player/drop_pod_17";
    public static final String TERMINAL_PROBE_CONTRACT_ID = "echodatacore:system/terminal_probe";
    public static final String PLAYER_SCHEMA_VERSION_CONTRACT_ID = "echodatacore:system/player_schema_version";
    public static final String WORLD_SCHEMA_VERSION_CONTRACT_ID = "echodatacore:system/world_schema_version";
    public static final String LAST_REGION_CONTRACT_ID = "echodatacore:worldcore/last_region";
    public static final String LAST_MARKER_CONTRACT_ID = "echodatacore:worldcore/last_marker";
    public static final String ACTIVE_HAZARDS_CONTRACT_ID = "echodatacore:worldcore/active_hazards";
    public static final String DATA_SERVICE_CONTRACT_ID = "echodatacore:data_service";
    public static final String DATA_SYNC_CONTRACT_ID = "echodatacore:data_sync";

    public Map<String, Object> execute(String playerId, String runtime) {
        String normalizedPlayerId = normalizeText(playerId, REFERENCE_PLAYER_ID);
        Map<String, Object> playerValues = new LinkedHashMap<>();
        playerValues.put(TERMINAL_PROBE_CONTRACT_ID, "online");
        playerValues.put(PLAYER_SCHEMA_VERSION_CONTRACT_ID, 2L);
        playerValues.put(LAST_REGION_CONTRACT_ID, "echoashfallprotocol:regions/crash_valley");
        playerValues.put("echodatacore:worldcore/last_discovery_source", "echoworldcore:region_enter");
        playerValues.put("echodatacore:worldcore/region_discoveries", 1L);
        playerValues.put(LAST_MARKER_CONTRACT_ID, "echoashfallprotocol:markers/power_cell_cache");
        playerValues.put("echodatacore:worldcore/markers_revealed", 1L);
        playerValues.put(ACTIVE_HAZARDS_CONTRACT_ID, "ash_storm,radiation_pocket");
        playerValues.put("echodatacore:worldcore/active_hazard_severity", 41L);

        Map<String, Object> worldValues = new LinkedHashMap<>();
        worldValues.put(WORLD_SCHEMA_VERSION_CONTRACT_ID, 2L);
        worldValues.put("echodatacore:worldcore/world_region_discoveries", 7L);
        worldValues.put("echodatacore:worldcore/world_markers_revealed", 4L);
        worldValues.put("echodatacore:worldcore/world_hazard_changes", 3L);

        Map<String, Object> teamValues = new LinkedHashMap<>();
        teamValues.put("echodatacore:team/field_cache_unlocked", true);
        teamValues.put("echodatacore:team/shared_power_cells", 1L);

        Map<String, Object> persistenceSnapshot = new LinkedHashMap<>();
        persistenceSnapshot.put("playerId", normalizedPlayerId);
        persistenceSnapshot.put("playerValues", Map.copyOf(playerValues));
        persistenceSnapshot.put("worldValues", Map.copyOf(worldValues));
        persistenceSnapshot.put("teamId", "echoashfallprotocol:teams/drop_pod_17");
        persistenceSnapshot.put("teamValues", Map.copyOf(teamValues));
        persistenceSnapshot.put("migrationMarkers", Map.of(
                "echodatacore", 2L,
                "echodatacore:team_values_v1", 2L
        ));

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        plan.put("service", "echodatacore:data_service/runtime_profile_sync");
        plan.put("dataRuntimeProfileExecuted", true);
        plan.put("runtime", normalizeText(runtime, "echo_native"));
        plan.put("playerId", normalizedPlayerId);
        plan.put("registeredKeys", registeredKeys());
        plan.put("metadataReload", metadataReload());
        plan.put("persistenceSnapshot", Map.copyOf(persistenceSnapshot));
        plan.put("syncPayload", syncPayload(normalizedPlayerId, playerValues));
        plan.put("diagnostics", diagnostics());
        plan.put("writeSafety", Map.of(
                "destructiveActions", 0,
                "legacyRootsMutated", false,
                "syncPayloadClipped", false,
                "requiresConfirmationForWriteActions", true
        ));
        plan.put("events", List.of(
                "data.keys.registered",
                "metadata.reload.applied",
                "player.profile.persisted",
                "sync.payload.emitted"
        ));
        plan.put("referenceBehavior", "datacore_applies_runtime_profile_sync_reload_persistence");
        return Map.copyOf(plan);
    }

    public boolean referencePlanPassed(Map<String, Object> plan) {
        return Boolean.TRUE.equals(plan.get("dataRuntimeProfileExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(plan.get("adapterCoreContract"))
                && String.valueOf(plan.get("registeredKeys")).contains(TERMINAL_PROBE_CONTRACT_ID)
                && String.valueOf(plan.get("registeredKeys")).contains(ACTIVE_HAZARDS_CONTRACT_ID)
                && String.valueOf(plan.get("metadataReload")).contains("echodatacore:native_probe/metadata_flag")
                && String.valueOf(plan.get("persistenceSnapshot")).contains("echoashfallprotocol:regions/crash_valley")
                && String.valueOf(plan.get("persistenceSnapshot")).contains("echodatacore:team_values_v1")
                && String.valueOf(plan.get("syncPayload")).contains(DATA_SYNC_CONTRACT_ID)
                && String.valueOf(plan.get("syncPayload")).contains("echodatacore:worldcore/active_hazard_severity")
                && String.valueOf(plan.get("diagnostics")).contains("registeredKeyCount=14")
                && String.valueOf(plan.get("writeSafety")).contains("destructiveActions=0")
                && String.valueOf(plan.get("events")).contains("sync.payload.emitted");
    }

    private static List<Map<String, Object>> registeredKeys() {
        return List.of(
                key(TERMINAL_PROBE_CONTRACT_ID, "player", "string", "offline", true),
                key(PLAYER_SCHEMA_VERSION_CONTRACT_ID, "player", "counter", "0", true),
                key(WORLD_SCHEMA_VERSION_CONTRACT_ID, "world", "counter", "0", true),
                key(LAST_REGION_CONTRACT_ID, "player", "string", "", true),
                key("echodatacore:worldcore/last_discovery_source", "player", "string", "", true),
                key("echodatacore:worldcore/region_discoveries", "player", "counter", "0", true),
                key(LAST_MARKER_CONTRACT_ID, "player", "string", "", true),
                key("echodatacore:worldcore/markers_revealed", "player", "counter", "0", true),
                key(ACTIVE_HAZARDS_CONTRACT_ID, "player", "string", "", true),
                key("echodatacore:worldcore/active_hazard_severity", "player", "counter", "0", true),
                key("echodatacore:worldcore/world_region_discoveries", "world", "counter", "0", true),
                key("echodatacore:worldcore/world_markers_revealed", "world", "counter", "0", true),
                key("echodatacore:worldcore/world_hazard_changes", "world", "counter", "0", true),
                key("echodatacore:native_probe/metadata_flag", "player", "flag", "false", true)
        );
    }

    private static Map<String, Object> metadataReload() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "data/echodatacore/echodatacore/data_keys/native_probe/metadata_flag.json");
        metadata.put("replacedDatapackMetadata", true);
        metadata.put("revision", 2L);
        metadata.put("visibleMetadataCount", 14);
        metadata.put("accepted", List.of(Map.of(
                "id", "echodatacore:native_probe/metadata_flag",
                "scope", "player",
                "kind", "flag",
                "owner", MODULE_ID,
                "synced", true
        )));
        metadata.put("removedStaleKeys", List.of("echodatacore:native_probe/stale_flag"));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> syncPayload(String playerId, Map<String, Object> playerValues) {
        Map<String, Object> syncPayload = new LinkedHashMap<>();
        syncPayload.put("payloadContract", DATA_SYNC_CONTRACT_ID);
        syncPayload.put("scope", "player");
        syncPayload.put("ownerId", playerId);
        syncPayload.put("fullSnapshot", true);
        syncPayload.put("revision", 3L);
        syncPayload.put("entryCount", playerValues.size());
        syncPayload.put("entries", playerValues.entrySet().stream()
                .map(entry -> Map.of(
                        "key", entry.getKey(),
                        "value", entry.getValue(),
                        "synced", true
                ))
                .toList());
        return Map.copyOf(syncPayload);
    }

    private static Map<String, Object> diagnostics() {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("available", true);
        diagnostics.put("provider", "echodatacore:data_service");
        diagnostics.put("registeredKeyCount", 14);
        diagnostics.put("syncedKeyCount", 14);
        diagnostics.put("metadataKeyCount", 14);
        diagnostics.put("dirtyOwnerCount", 1);
        diagnostics.put("recentChanges", List.of(
                "metadata_reload:1",
                "player_set:" + TERMINAL_PROBE_CONTRACT_ID,
                "sync_emit:" + DATA_SYNC_CONTRACT_ID
        ));
        return Map.copyOf(diagnostics);
    }

    private static Map<String, Object> key(
            String id,
            String scope,
            String kind,
            String defaultValue,
            boolean synced
    ) {
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("id", id);
        key.put("scope", scope);
        key.put("kind", kind);
        key.put("default", defaultValue);
        key.put("synced", synced);
        key.put("owner", MODULE_ID);
        return Map.copyOf(key);
    }

    private static String normalizeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}

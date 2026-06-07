package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * AdapterCore Truth Layer.
 *
 * <p>Every playable action must flow through one real path:
 * <pre>
 * player action → AdapterCore action/event → runtime host → real state mutation → mission update → save update → visible feedback
 * </pre>
 *
 * <p>Contract rules (stop-the-bleeding):
 * <ul>
 *   <li>No method may return {@code MUTATED} unless it actually changed game state.</li>
 *   <li>No method may return {@code NOOP} if a side-effect occurred.</li>
 *   <li>{@code UNSUPPORTED} means this host does not implement the operation.</li>
 *   <li>{@code FAILED} means the host attempted the operation and it failed.</li>
 *   <li>{@code QUEUED} means the operation is queued only and is not complete.</li>
 *   <li>A command queue is <b>not</b> considered done unless something consumes it and mutates state.</li>
 * </ul>
 *
 * <p>Acceptance gate: calling any AdapterCore method must tell the truth about whether real state changed.
 */
public interface EchoNativeRuntimeHost {
    String API_VERSION = "1.0.0-rc1";

    PlayerInventory playerInventory();

    PlayerState playerState();

    WorldBlocks worldBlocks();

    WorldState worldState();

    Structures structures();

    BlockEntities blockEntities();

    Capabilities capabilities();

    Events events();

    Packets packets();

    Hud hud();

    SaveData saveData();

    default RuntimeSurfaces runtimeSurfaces() {
        return new RuntimeSurfaces() {
        };
    }

    default ContentRegistries contentRegistries() {
        return new ContentRegistries() {
            @Override
            public NativeResult register(NativeContentRegistration registration, NativeMutationContext context) {
                return NativeResult.unsupported("Runtime host does not implement content registry registration.", Map.of(
                        "nativeInterface", "EchoNativeRuntimeHost.ContentRegistries",
                        "nativeMethod", "register",
                        "contentId", registration == null ? "" : registration.contentId(),
                        "failureReason", "unsupported runtime host method"));
            }

            @Override
            public List<NativeContentRegistration> registrations(String domain, NativeMutationContext context) {
                return List.of();
            }
        };
    }

    interface PlayerInventory {
        NativeResult grant(NativePlayerRef player, NativeItemStack stack, NativeMutationContext context);

        NativeResult remove(NativePlayerRef player, String itemId, int count, NativeMutationContext context);

        List<NativeItemStack> snapshot(NativePlayerRef player, NativeMutationContext context);
    }

    interface PlayerState {
        NativeResult teleport(NativePlayerRef player, NativePosition position, NativeMutationContext context);

        NativeResult bindRespawn(NativePlayerRef player, NativePosition position, boolean forced,
                                 NativeMutationContext context);

        NativeResult grantAdvancement(NativePlayerRef player, String advancementId, String criterion,
                                      NativeMutationContext context);

        NativeResult writePersistentState(NativePlayerRef player, String key, Object value,
                                          NativeMutationContext context);
    }

    interface WorldBlocks {
        NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context);

        NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context);

        NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context);

        boolean isLoaded(NativeBlockRef block, NativeMutationContext context);
    }

    interface WorldState {
        NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context);

        NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context);

        NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context);
    }

    interface Structures {
        NativeResult placeStructure(NativeStructurePlacement placement, NativeMutationContext context);
    }

    interface BlockEntities {
        NativeResult tick(NativeBlockRef block, NativeMutationContext context);

        NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context);

        NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context);
    }

    interface Capabilities {
        NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context);

        NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count,
                                 NativeMutationContext context);

        NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context);

        NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context);

        Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context);
    }

    interface Events {
        NativeResult publish(NativeEvent event, NativeMutationContext context);
    }

    interface Packets {
        NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context);

        NativeResult broadcast(NativePacket packet, NativeMutationContext context);
    }

    interface Hud {
        NativeResult publishNotification(NativePlayerRef player, Map<String, Object> payload,
                                         NativeMutationContext context);
    }

    interface SaveData {
        NativeResult write(NativeSaveData data, NativeMutationContext context);

        Map<String, Object> read(String scope, String key, NativeMutationContext context);

        NativeResult delete(String scope, String key, NativeMutationContext context);
    }

    interface RuntimeSurfaces {
        default NativeResult clientTick(String phase, Map<String, Object> payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("clientTick", phase);
        }

        default NativeResult renderLayer(String layerId, Map<String, Object> payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("renderLayer", layerId);
        }

        default NativeResult screenEvent(String screenId, String eventType, Map<String, Object> payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("screenEvent", screenId + ":" + eventType);
        }

        default NativeResult keybind(String keybindId, String action, Map<String, Object> payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("keybind", keybindId + ":" + action);
        }

        default NativeResult registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence,
                NativeMutationContext context) {
            return unsupportedRuntimeSurface("registerCommand", moduleId + ":" + commandId);
        }

        default NativeResult registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence,
                NativeMutationContext context) {
            return unsupportedRuntimeSurface("registerNetworkPacket", moduleId + ":" + packetId);
        }

        default NativeResult reloadConfig(String moduleId, String configId, String scope, Map<String, Object> evidence, NativeMutationContext context) {
            return unsupportedRuntimeSurface("reloadConfig", moduleId + ":" + configId);
        }

        default NativeResult reloadResources(String moduleId, String resourceId, String scope, Map<String, Object> evidence, NativeMutationContext context) {
            return unsupportedRuntimeSurface("reloadResources", moduleId + ":" + resourceId);
        }

        default NativeResult saveHook(String hookId, Map<String, Object> payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("saveHook", hookId);
        }

        default NativeResult lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence, NativeMutationContext context) {
            return unsupportedRuntimeSurface("lifecyclePhase", moduleId + ":" + phaseId);
        }

        default NativeResult publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                String status,
                NativeMutationContext context) {
            return unsupportedRuntimeSurface("publishRuntimeEvent", sourceModule + ":" + eventId);
        }

        default NativeResult syncServerClient(String channel, String payload, NativeMutationContext context) {
            return unsupportedRuntimeSurface("syncServerClient", channel);
        }

        private static NativeResult unsupportedRuntimeSurface(String method, String target) {
            return NativeResult.unsupported("Runtime host does not implement Agent 5 runtime surface mutation.", Map.of(
                    "nativeInterface", "EchoNativeRuntimeHost.RuntimeSurfaces",
                    "nativeMethod", method == null ? "" : method,
                    "target", target == null ? "" : target,
                    "failureReason", "unsupported runtime host method"));
        }
    }

    interface ContentRegistries {
        NativeResult register(NativeContentRegistration registration, NativeMutationContext context);

        List<NativeContentRegistration> registrations(String domain, NativeMutationContext context);
    }

    record NativeMutationContext(
            String moduleId,
            String dimensionId,
            String idempotencyKey,
            String logicalSide,
            long gameTime,
            Map<String, Object> metadata) {
        public NativeMutationContext {
            moduleId = AdapterContractGuards.requireText(moduleId, "native mutation module id");
            dimensionId = AdapterContractGuards.optionalText(dimensionId);
            idempotencyKey = AdapterContractGuards.requireText(idempotencyKey, "native mutation idempotency key");
            logicalSide = AdapterContractGuards.optionalText(logicalSide);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record NativeResult(
            boolean mutated,
            String status,
            String message,
            Map<String, Object> snapshot) {
        public NativeResult {
            boolean requestedMutationFlag = mutated;
            String requestedStatus = AdapterContractGuards.optionalText(status);
            NativeResultStatus resultStatus = NativeResultStatus.from(requestedStatus, requestedMutationFlag);
            mutated = resultStatus == NativeResultStatus.MUTATED;
            status = resultStatus.name();
            message = AdapterContractGuards.optionalText(message);
            Map<String, Object> normalizedSnapshot = new LinkedHashMap<>();
            if (snapshot != null) {
                normalizedSnapshot.putAll(snapshot);
            }
            normalizedSnapshot.put("resultStatus", status);
            normalizedSnapshot.put("stateMutated", mutated);
            if (!requestedStatus.isBlank() && !requestedStatus.equals(status)) {
                normalizedSnapshot.put("rawStatus", requestedStatus);
            }
            if (requestedMutationFlag != mutated) {
                normalizedSnapshot.put("rawMutationFlag", requestedMutationFlag);
            }
            snapshot = Map.copyOf(normalizedSnapshot);
        }

        public NativeResult(NativeResultStatus status, String message, Map<String, Object> snapshot) {
            this(status == NativeResultStatus.MUTATED, status.name(), message, snapshot);
        }

        public NativeResultStatus resultStatus() {
            return NativeResultStatus.valueOf(status);
        }

        public boolean completedWithMutation() {
            return resultStatus() == NativeResultStatus.MUTATED;
        }

        public boolean completedWithoutMutation() {
            return resultStatus() == NativeResultStatus.NOOP;
        }

        public boolean queuedOnly() {
            return resultStatus() == NativeResultStatus.QUEUED;
        }

        public boolean terminalFailure() {
            return resultStatus() == NativeResultStatus.FAILED
                    || resultStatus() == NativeResultStatus.UNSUPPORTED;
        }

        public String failureReason() {
            Object reason = snapshot.get("failureReason");
            if (reason == null && terminalFailure()) {
                reason = message;
            }
            return reason == null ? "" : String.valueOf(reason);
        }

        public static NativeResult mutated(String message, Map<String, Object> snapshot) {
            return new NativeResult(NativeResultStatus.MUTATED, message, snapshot);
        }

        public static NativeResult noop(String message, Map<String, Object> snapshot) {
            return new NativeResult(NativeResultStatus.NOOP, message, snapshot);
        }

        public static NativeResult unsupported(String message, Map<String, Object> snapshot) {
            return new NativeResult(NativeResultStatus.UNSUPPORTED, message, snapshot);
        }

        public static NativeResult failed(String message, Map<String, Object> snapshot) {
            return new NativeResult(NativeResultStatus.FAILED, message, snapshot);
        }

        public static NativeResult queued(String message, Map<String, Object> snapshot) {
            return new NativeResult(NativeResultStatus.QUEUED, message, snapshot);
        }
    }

    enum NativeResultStatus {
        MUTATED,
        NOOP,
        UNSUPPORTED,
        FAILED,
        QUEUED;

        public static NativeResultStatus from(String status, boolean mutated) {
            String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
            if (normalized.isBlank()) {
                return mutated ? MUTATED : NOOP;
            }
            if (normalized.equals("MUTATED") || normalized.startsWith("MUTATED_")
                    || normalized.equals("SUCCESS") || normalized.equals("SUCCEEDED")
                    || normalized.equals("PASS") || normalized.equals("APPLIED")) {
                return MUTATED;
            }
            if (normalized.equals("NOOP") || normalized.equals("NO_OP")
                    || normalized.startsWith("SKIPPED") || normalized.startsWith("UNCHANGED")
                    || normalized.equals("ALREADY_DONE") || normalized.equals("NO_CHANGE")) {
                return NOOP;
            }
            if (normalized.equals("UNSUPPORTED") || normalized.startsWith("UNSUPPORTED_")
                    || normalized.startsWith("NOT_IMPLEMENTED")) {
                return UNSUPPORTED;
            }
            if (normalized.equals("FAILED") || normalized.startsWith("FAILED_")
                    || normalized.startsWith("FAIL") || normalized.startsWith("INVALID")
                    || normalized.startsWith("ERROR")) {
                return FAILED;
            }
            if (normalized.equals("QUEUED") || normalized.startsWith("QUEUED_")
                    || normalized.equals("PLANNED") || normalized.equals("PENDING")) {
                return QUEUED;
            }
            return mutated ? MUTATED : NOOP;
        }
    }

    record NativeMutationTarget(
            NativePlayerRef player,
            String worldId,
            NativePosition position,
            NativeBlockRef block) {
        public NativeMutationTarget {
            worldId = AdapterContractGuards.optionalText(worldId);
        }

        public static NativeMutationTarget none() {
            return new NativeMutationTarget(null, "", null, null);
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> target = new LinkedHashMap<>();
            if (player != null) {
                target.put("playerId", player.playerId());
            }
            if (!worldId.isBlank()) {
                target.put("worldId", worldId);
            }
            if (position != null) {
                target.put("position", Map.of(
                        "dimensionId", position.dimensionId(),
                        "x", position.x(),
                        "y", position.y(),
                        "z", position.z(),
                        "yaw", position.yaw(),
                        "pitch", position.pitch()));
            }
            if (block != null) {
                target.put("block", Map.of(
                        "dimensionId", block.dimensionId(),
                        "x", block.x(),
                        "y", block.y(),
                        "z", block.z()));
            }
            return Map.copyOf(target);
        }
    }

    record NativeMutationLedgerEntry(
            String actionId,
            String runtimeHostId,
            Map<String, Object> inputPayload,
            NativeMutationTarget target,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            NativeResultStatus resultStatus,
            String failureReason,
            boolean saveTouched,
            boolean hudOrEventEmitted) {
        public NativeMutationLedgerEntry {
            actionId = AdapterContractGuards.requireText(actionId, "mutation ledger action id");
            runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "mutation ledger runtime host id");
            inputPayload = inputPayload == null ? Map.of() : Map.copyOf(inputPayload);
            target = Objects.requireNonNullElseGet(target, NativeMutationTarget::none);
            beforeSummary = beforeSummary == null ? Map.of() : Map.copyOf(beforeSummary);
            afterSummary = afterSummary == null ? Map.of() : Map.copyOf(afterSummary);
            resultStatus = resultStatus == null ? NativeResultStatus.FAILED : resultStatus;
            failureReason = AdapterContractGuards.optionalText(failureReason);
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("actionId", actionId);
            entry.put("runtimeHostId", runtimeHostId);
            entry.put("inputPayload", inputPayload);
            entry.put("target", target.snapshot());
            entry.put("beforeSummary", beforeSummary);
            entry.put("afterSummary", afterSummary);
            entry.put("resultStatus", resultStatus.name());
            entry.put("failureReason", failureReason);
            entry.put("saveTouched", saveTouched);
            entry.put("hudOrEventEmitted", hudOrEventEmitted);
            return Map.copyOf(entry);
        }
    }

    record NativePlayerRef(String playerId) {
        public NativePlayerRef {
            playerId = AdapterContractGuards.requireText(playerId, "native player id");
        }
    }

    record NativePosition(String dimensionId, double x, double y, double z, float yaw, float pitch) {
        public NativePosition {
            dimensionId = AdapterContractGuards.requireText(dimensionId, "native position dimension id");
        }
    }

    record NativeBlockRef(String dimensionId, int x, int y, int z) {
        public NativeBlockRef {
            dimensionId = AdapterContractGuards.requireText(dimensionId, "native block dimension id");
        }
    }

    record NativeBlockState(String blockId, Map<String, Object> properties) {
        public NativeBlockState {
            blockId = AdapterContractGuards.requireText(blockId, "native block id");
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }
    }

    record NativeItemStack(String itemId, int count, Map<String, Object> components) {
        public NativeItemStack {
            itemId = AdapterContractGuards.requireText(itemId, "native item id");
            if (count < 1) {
                throw new IllegalArgumentException("native item count must be positive");
            }
            components = components == null ? Map.of() : Map.copyOf(components);
        }
    }

    record NativeStructurePlacement(
            String structureId,
            String dimensionId,
            int originX,
            int originY,
            int originZ,
            String anchor,
            Map<String, Object> constraints) {
        public NativeStructurePlacement {
            structureId = AdapterContractGuards.requireText(structureId, "native structure id");
            dimensionId = AdapterContractGuards.requireText(dimensionId, "native structure dimension id");
            anchor = AdapterContractGuards.optionalText(anchor);
            constraints = constraints == null ? Map.of() : Map.copyOf(constraints);
        }
    }

    record NativeBlockEntitySnapshot(String blockEntityId, NativeBlockRef block, Map<String, Object> state) {
        public NativeBlockEntitySnapshot {
            blockEntityId = AdapterContractGuards.requireText(blockEntityId, "native block entity id");
            if (block == null) {
                throw new IllegalArgumentException("native block entity block must not be null");
            }
            state = state == null ? Map.of() : Map.copyOf(state);
        }
    }

    record NativeCapabilityRequest(
            String capabilityId,
            NativeBlockRef block,
            String side,
            Map<String, Object> query) {
        public NativeCapabilityRequest {
            capabilityId = AdapterContractGuards.requireText(capabilityId, "native capability id");
            if (block == null) {
                throw new IllegalArgumentException("native capability block must not be null");
            }
            side = AdapterContractGuards.optionalText(side);
            query = query == null ? Map.of() : Map.copyOf(query);
        }
    }

    record NativeEvent(String eventId, NativePlayerRef player, Map<String, Object> payload) {
        public NativeEvent {
            eventId = AdapterContractGuards.requireText(eventId, "native event id");
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record NativePacket(String packetId, NativePlayerRef player, String channel, Map<String, Object> payload) {
        public NativePacket {
            packetId = AdapterContractGuards.requireText(packetId, "native packet id");
            channel = AdapterContractGuards.optionalText(channel);
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record NativeSaveData(String scope, String key, Map<String, Object> payload) {
        public NativeSaveData {
            scope = AdapterContractGuards.requireText(scope, "native save data scope");
            key = AdapterContractGuards.requireText(key, "native save data key");
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    record NativeContentRegistration(
            String moduleId,
            String contentId,
            String contentKind,
            String domain,
            String displayName,
            String adapterKey,
            String neoForgeId,
            String nativeLoaderId,
            String standaloneRuntimeId,
            Map<String, Object> metadata) {
        public NativeContentRegistration {
            moduleId = AdapterContractGuards.requireText(moduleId, "native content module id");
            contentId = AdapterContractGuards.requireText(contentId, "native content id");
            contentKind = AdapterContractGuards.requireText(contentKind, "native content kind");
            domain = AdapterContractGuards.requireText(domain, "native content domain");
            displayName = AdapterContractGuards.requireText(displayName, "native content display name");
            adapterKey = AdapterContractGuards.optionalText(adapterKey);
            if (adapterKey.isBlank()) {
                adapterKey = contentId;
            }
            neoForgeId = AdapterContractGuards.optionalText(neoForgeId);
            if (neoForgeId.isBlank()) {
                neoForgeId = contentId;
            }
            nativeLoaderId = AdapterContractGuards.optionalText(nativeLoaderId);
            if (nativeLoaderId.isBlank()) {
                nativeLoaderId = contentId;
            }
            standaloneRuntimeId = AdapterContractGuards.optionalText(standaloneRuntimeId);
            if (standaloneRuntimeId.isBlank()) {
                standaloneRuntimeId = contentId;
            }
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("moduleId", moduleId);
            result.put("contentId", contentId);
            result.put("contentKind", contentKind);
            result.put("domain", domain);
            result.put("displayName", displayName);
            result.put("adapterKey", adapterKey);
            result.put("neoForgeId", neoForgeId);
            result.put("nativeLoaderId", nativeLoaderId);
            result.put("standaloneRuntimeId", standaloneRuntimeId);
            result.put("metadata", metadata);
            return Map.copyOf(result);
        }
    }

    /**
     * Validates that a {@link NativeResult} truthfully reflects whether state changed.
     *
     * @param result              the result to validate
     * @param actualStateChanged  whether real state actually changed
     * @throws IllegalStateException if the result lies about mutation
     */
    static void validateTruth(NativeResult result, boolean actualStateChanged) {
        if (result == null) {
            throw new IllegalStateException("AdapterCore truth violation: result is null.");
        }
        NativeResultStatus status = result.resultStatus();
        boolean claimedMutated = result.mutated();
        if (actualStateChanged && !claimedMutated) {
            throw new IllegalStateException(
                    "AdapterCore truth violation: result status is " + status
                            + " but actual state changed. A host must return MUTATED when it mutates state.");
        }
        if (!actualStateChanged && claimedMutated) {
            throw new IllegalStateException(
                    "AdapterCore truth violation: result status is " + status
                            + " but actual state did NOT change. A host must not return MUTATED unless it mutates state.");
        }
    }

    static String interfaceForHostApi(String hostApi) {
        String api = hostApi == null ? "" : hostApi.toLowerCase(Locale.ROOT);
        if (api.contains("inventory")) {
            return "EchoNativeRuntimeHost.PlayerInventory";
        }
        if (api.contains("registry") || api.contains("registries")
                || api.contains("register_content") || api.contains("content_registration")
                || api.startsWith("screencore.register") || api.startsWith("assetcore.mount")
                || api.startsWith("datacore.reload")) {
            return "EchoNativeRuntimeHost.ContentRegistries";
        }
        if (api.contains("persistent_state") || api.contains("teleport")
                || api.contains("respawn") || api.contains("advancement")) {
            return "EchoNativeRuntimeHost.PlayerState";
        }
        if (api.contains("place_structure") || api.contains("structure")) {
            return "EchoNativeRuntimeHost.Structures";
        }
        if (api.contains("block_entity") || api.contains("machine")) {
            return "EchoNativeRuntimeHost.BlockEntities";
        }
        if (api.contains("capability") || api.contains("energy")) {
            return "EchoNativeRuntimeHost.Capabilities";
        }
        if (api.startsWith("hudcore.") || api.contains(".hud") || api.contains("hazard_readout")) {
            return "EchoNativeRuntimeHost.Hud";
        }
        if (api.startsWith("network.") || api.startsWith("screencore.")
                || api.startsWith("terminal.") || api.startsWith("wiki.")
                || api.startsWith("lens.") || api.startsWith("codex.")) {
            return "EchoNativeRuntimeHost.Packets";
        }
        if (api.startsWith("weathercore.") || api.startsWith("soundcore.")
                || api.startsWith("atmospherecore.") || api.startsWith("holomap.")
                || api.contains("route_hazard")) {
            return "EchoNativeRuntimeHost.WorldState";
        }
        if (api.startsWith("recovery.") || api.contains("save") || api.contains("field_cache")) {
            return "EchoNativeRuntimeHost.SaveData";
        }
        if (api.contains("event")) {
            return "EchoNativeRuntimeHost.Events";
        }
        if (api.contains("block")) {
            return "EchoNativeRuntimeHost.WorldBlocks";
        }
        return "EchoNativeRuntimeHost.Events";
    }

    static String methodForHostApi(String hostApi) {
        if (hostApi == null || hostApi.isBlank()) {
            return "";
        }
        String tail = hostApi.contains(".") ? hostApi.substring(hostApi.lastIndexOf('.') + 1) : hostApi;
        StringBuilder method = new StringBuilder();
        boolean uppercaseNext = false;
        for (char character : tail.toCharArray()) {
            if (character == '_' || character == '-' || character == ':') {
                uppercaseNext = true;
            } else if (uppercaseNext) {
                method.append(Character.toUpperCase(character));
                uppercaseNext = false;
            } else {
                method.append(character);
            }
        }
        return method.toString();
    }
}

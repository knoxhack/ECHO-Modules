package com.knoxhack.echoashfallprotocol.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echo.adaptercore.EchoNativeLoaderAttachedRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class NativeLoaderEchoRuntimeHost implements EchoNativeRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoashfallprotocol:native_loader_runtime_host";
    public static final String ADAPTERCORE_BACKEND_CLASS = "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend";
    private static final String ADAPTERCORE_SERVICE_ID = "adaptercore.native_loader.backend";
    private static final String LIVE_RUNTIME_BRIDGE_ID = "echoashfallprotocol:native_loader_live_minecraft_bridge";
    private static final List<String> LIVE_RUNTIME_SURFACES = List.of(
            "inventory",
            "player_state",
            "world_blocks",
            "world_state",
            "structures",
            "block_entities",
            "capabilities",
            "events",
            "packets_hud",
            "hud",
            "save_data",
            "missions",
            "feedback",
            "client_tick",
            "render_layers",
            "screen_events",
            "keybinds",
            "commands",
            "network_channels",
            "config_reloads",
            "resource_reloads",
            "save_hooks",
            "lifecycle_phases",
            "server_client_sync");

    private final MinecraftEchoRuntimeHost compatibilityDelegate;
    private final Object nativeLoaderBackend;
    private final EchoNativeRuntimeHost nativeLoaderAttachedHost;
    private final PlayerInventory playerInventory = new NativeLoaderPlayerInventory();
    private final PlayerState playerState = new NativeLoaderPlayerState();
    private final WorldBlocks worldBlocks = new NativeLoaderWorldBlocks();
    private final WorldState worldState = new NativeLoaderWorldState();
    private final Structures structures = new NativeLoaderStructures();
    private final BlockEntities blockEntities = new NativeLoaderBlockEntities();
    private final Capabilities capabilities = new NativeLoaderCapabilities();
    private final Events events = new NativeLoaderEvents();
    private final Packets packets = new NativeLoaderPackets();
    private final Hud hud = new NativeLoaderHud();
    private final SaveData saveData = new NativeLoaderSaveData();

    public NativeLoaderEchoRuntimeHost() {
        this(null);
    }

    public NativeLoaderEchoRuntimeHost(MinecraftEchoRuntimeHost compatibilityDelegate) {
        this.compatibilityDelegate = compatibilityDelegate;
        this.nativeLoaderBackend = createNativeLoaderBackend();
        this.nativeLoaderAttachedHost = nativeLoaderBackend == null
                ? null
                : new EchoNativeLoaderAttachedRuntimeHost(RUNTIME_HOST_ID, nativeLoaderBackend);
        if (nativeLoaderAttachedHost == null) {
            throw new IllegalStateException("Native Loader runtime host requires the first-class native backend; a live Minecraft delegate is optional fallback only.");
        }
    }

    public String runtimeHostId() {
        return RUNTIME_HOST_ID;
    }

    public String runtimeLane() {
        return "Native Loader";
    }

    public String compatibilityDelegateId() {
        return compatibilityDelegate == null ? "" : compatibilityDelegate.compatibilityDelegateId();
    }

    public String liveMinecraftDelegateId() {
        return compatibilityDelegate == null ? "" : compatibilityDelegate.runtimeHostId();
    }

    public boolean nativeLoaderBackendAttached() {
        return nativeLoaderBackend != null;
    }

    public boolean nativeLoaderBackendPrimary() {
        return nativeLoaderOperationHostActive();
    }

    public boolean liveMinecraftDelegateFallbackAvailable() {
        return compatibilityDelegate != null;
    }

    public boolean firstClassNativeRuntime() {
        return nativeLoaderBackendAttached();
    }

    public boolean delegateRequired() {
        return false;
    }

    public boolean liveMinecraftAttached() {
        return compatibilityDelegate != null;
    }

    public boolean nativeLoaderLiveRuntimeBridgeAttached() {
        return compatibilityDelegate != null;
    }

    public String nativeLoaderLiveRuntimeBridgeId() {
        return nativeLoaderLiveRuntimeBridgeAttached() ? LIVE_RUNTIME_BRIDGE_ID : "";
    }

    public boolean nativeLoaderPrimaryRuntime() {
        return nativeLoaderOperationHostActive();
    }

    public Map<String, Object> runtimeHostReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("runtimeHostId", runtimeHostId());
        report.put("runtimeHostClass", getClass().getName());
        report.put("runtimeLane", runtimeLane());
        report.put("runtimeKind", "echo_native_first_class_runtime");
        report.put("firstClassNativeRuntime", firstClassNativeRuntime());
        report.put("delegateRequired", delegateRequired());
        report.put("liveMinecraftAttached", liveMinecraftAttached());
        report.put("nativeLoaderLiveRuntimeBridgeAttached", nativeLoaderLiveRuntimeBridgeAttached());
        report.put("nativeLoaderLiveRuntimeBridgeId", nativeLoaderLiveRuntimeBridgeId());
        report.put("nativeLoaderBackendAttached", nativeLoaderBackendAttached());
        report.put("nativeLoaderBackendPrimary", nativeLoaderBackendPrimary());
        report.put("nativeLoaderPrimaryRuntime", nativeLoaderPrimaryRuntime());
        report.put("liveMinecraftDelegateFallbackAvailable", liveMinecraftDelegateFallbackAvailable());
        report.put("liveMinecraftDelegateId", liveMinecraftDelegateId());
        report.put("compatibilityDelegate", compatibilityDelegateId());
        return Map.copyOf(report);
    }

    public Object nativeLoaderBackend() {
        return nativeLoaderBackend;
    }

    public NativeResult recordExternalRuntimeEvent(NativeEvent event, NativeResult result) {
        return bridgeResult(
                "EchoNativeRuntimeHost.Events",
                "publish",
                result,
                event == null ? null : nativeLoaderBackendEmitEvent(event, result));
    }

    public NativePlayerRef playerRef() {
        return compatibilityDelegate == null ? new NativePlayerRef("native-loader") : compatibilityDelegate.playerRef();
    }

    public String dimensionId() {
        return compatibilityDelegate == null ? "native_loader" : compatibilityDelegate.dimensionId();
    }

    public NativeMutationContext context(String idempotencyKey, String nativeInterface, String nativeMethod) {
        NativeMutationContext delegateContext = compatibilityDelegate == null
                ? nativeLoaderContext(idempotencyKey, nativeInterface, nativeMethod)
                : compatibilityDelegate.context(idempotencyKey, nativeInterface, nativeMethod);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nativeInterface", nativeInterface == null ? "" : nativeInterface);
        metadata.put("nativeMethod", nativeMethod == null ? "" : nativeMethod);
        metadata.put("runtimeLane", runtimeLane());
        metadata.put("runtimeHostId", runtimeHostId());
        metadata.put("adapterCoreBackendClass", ADAPTERCORE_BACKEND_CLASS);
        metadata.put("adapterCoreCallEnteredNativeLoaderHost", true);
        metadata.put("compatibilityDelegate", compatibilityDelegateId());
        metadata.put("liveMinecraftDelegateId", liveMinecraftDelegateId());
        metadata.put("liveMinecraftDelegateClass", compatibilityDelegate == null ? "" : compatibilityDelegate.getClass().getName());
        metadata.put("nativeLoaderBackendFirst", nativeLoaderOperationHostActive());
        metadata.put("nativeLoaderBackendPrimaryConfigured", true);
        metadata.put("firstClassNativeRuntime", firstClassNativeRuntime());
        metadata.put("delegateRequired", delegateRequired());
        metadata.put("liveMinecraftAttached", liveMinecraftAttached());
        metadata.put("nativeLoaderLiveRuntimeBridgeAttached", nativeLoaderLiveRuntimeBridgeAttached());
        metadata.put("nativeLoaderLiveRuntimeBridgeId", nativeLoaderLiveRuntimeBridgeId());
        metadata.put("nativeLoaderPrimaryRuntime", nativeLoaderPrimaryRuntime());
        metadata.put("liveMinecraftDelegateFallbackAvailable", liveMinecraftDelegateFallbackAvailable());
        metadata.put("delegateMetadata", delegateContext.metadata());
        return new NativeMutationContext(
                RUNTIME_HOST_ID,
                delegateContext.dimensionId(),
                delegateContext.idempotencyKey(),
                delegateContext.logicalSide(),
                delegateContext.gameTime(),
                Map.copyOf(metadata));
    }

    private NativeMutationContext nativeLoaderContext(String idempotencyKey, String nativeInterface, String nativeMethod) {
        String safeKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? RUNTIME_HOST_ID + ":" + String.valueOf(nativeInterface) + ":" + String.valueOf(nativeMethod)
                : idempotencyKey;
        return new NativeMutationContext(
                RUNTIME_HOST_ID,
                dimensionId(),
                safeKey,
                "NATIVE",
                0L,
                Map.of(
                        "runtimeLane", runtimeLane(),
                        "runtimeHostId", runtimeHostId(),
                        "nativeLoaderBackendFirst", true));
    }

    private EchoNativeRuntimeHost operationHost() {
        return nativeLoaderAttachedHost;
    }

    private boolean nativeLoaderOperationHostActive() {
        return nativeLoaderAttachedHost != null;
    }

    @Override
    public PlayerInventory playerInventory() {
        return playerInventory;
    }

    @Override
    public PlayerState playerState() {
        return playerState;
    }

    @Override
    public WorldBlocks worldBlocks() {
        return worldBlocks;
    }

    @Override
    public WorldState worldState() {
        return worldState;
    }

    @Override
    public Structures structures() {
        return structures;
    }

    @Override
    public BlockEntities blockEntities() {
        return blockEntities;
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    @Override
    public Events events() {
        return events;
    }

    @Override
    public Packets packets() {
        return packets;
    }

    @Override
    public Hud hud() {
        return hud;
    }

    @Override
    public SaveData saveData() {
        return saveData;
    }

    private NativeResult bridgeResult(String nativeInterface, String nativeMethod, NativeResult result) {
        return bridgeResult(nativeInterface, nativeMethod, result, null);
    }

    private NativeResult bridgeResult(
            String nativeInterface,
            String nativeMethod,
            NativeResult result,
            Object nativeLoaderBackendRecord) {
        if (result == null) {
            return NativeResult.failed(
                    "Native Loader operation host returned no AdapterCore result.",
                    bridgeSnapshot(nativeInterface, nativeMethod, Map.of(), nativeLoaderBackendRecord));
        }
        return new NativeResult(
                result.resultStatus(),
                result.message(),
                bridgeSnapshot(nativeInterface, nativeMethod, result.snapshot(), nativeLoaderBackendRecord));
    }

    private Map<String, Object> bridgeSnapshot(
            String nativeInterface,
            String nativeMethod,
            Map<String, Object> operationSnapshot,
            Object nativeLoaderBackendRecord) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (operationSnapshot != null) {
            snapshot.putAll(operationSnapshot);
        }
        Map<String, Object> backendRecord = nativeLoaderBackendRecordReport(nativeLoaderBackendRecord);
        boolean backendCallFailure = Boolean.TRUE.equals(backendRecord.get("nativeLoaderBackendCallFailure"));
        boolean backendRecordPresent = !backendRecord.isEmpty() && !backendCallFailure;
        boolean backendRecordMutated = "MUTATED".equals(String.valueOf(backendRecord.getOrDefault("status", "")));
        Map<String, Object> serviceBridgeReport = nativeLoaderBackendServiceBridgeReport();
        Map<String, Object> loadedModuleStateReport = nativeLoaderLoadedModuleStateReport();
        snapshot.put("adapterCoreCallEnteredNativeLoaderHost", true);
        snapshot.put("adapterCoreCallEnteredNativeLoaderBackend", backendRecordPresent);
        snapshot.put("adapterCoreBackendClass", backendRecordPresent ? ADAPTERCORE_BACKEND_CLASS : "");
        snapshot.put("nativeLoaderBackendAttached", nativeLoaderBackendAttached());
        snapshot.put("nativeLoaderBackendCallAttempted", !backendRecord.isEmpty());
        snapshot.put("nativeLoaderBackendCallFailure", backendCallFailure);
        snapshot.put("nativeLoaderBackendRecordStatus", backendRecord.getOrDefault("status", ""));
        snapshot.put("nativeLoaderBackendRecord", backendRecord);
        snapshot.put("nativeLoaderServiceBridgeReport", serviceBridgeReport);
        snapshot.put("nativeLoaderAttachedModuleServiceCount",
                serviceBridgeReport.getOrDefault("activeRuntimeServiceCount", 0));
        snapshot.put("nativeLoaderAttachedServiceInstanceCount",
                serviceBridgeReport.getOrDefault("activeRuntimeAttachedServiceCount", 0));
        snapshot.put("nativeLoaderAttachedModuleCount",
                serviceBridgeReport.getOrDefault("activeRuntimeModuleCount", 0));
        snapshot.put("nativeLoaderAttachedSurfaceCount",
                serviceBridgeReport.getOrDefault("activeRuntimeSurfaceCount", 0));
        snapshot.put("nativeLoaderAttachedServiceInstanceClasses",
                serviceBridgeReport.getOrDefault("activeRuntimeServiceInstanceClasses", List.of()));
        snapshot.put("nativeLoaderLoadedModuleStateReport", loadedModuleStateReport);
        snapshot.put("nativeLoaderLoadedModuleStateAttached",
                loadedModuleStateReport.getOrDefault("loadedModuleStateAttached", false));
        snapshot.put("nativeLoaderLoadedModuleStateCount",
                loadedModuleStateReport.getOrDefault("loadedModuleCount", 0));
        snapshot.put("nativeLoaderLoadedModuleRegisteredServiceCount",
                loadedModuleStateReport.getOrDefault("registeredServiceCount", 0));
        snapshot.put("nativeLoaderLoadedModuleRegisteredContentCount",
                loadedModuleStateReport.getOrDefault("registeredContentCount", 0));
        snapshot.put("nativeLoaderMutationLedger", nativeLoaderMutationLedgerReport());
        snapshot.put("nativeLoaderRuntimeHostClass", getClass().getName());
        snapshot.put("nativeLoaderRuntimeHostId", runtimeHostId());
        snapshot.put("nativeLoaderRuntimeHostReport", runtimeHostReport());
        snapshot.put("runtimeHostId", runtimeHostId());
        snapshot.put("runtimeLane", runtimeLane());
        snapshot.put("firstClassNativeRuntime", firstClassNativeRuntime());
        snapshot.put("delegateRequired", delegateRequired());
        snapshot.put("liveMinecraftAttached", liveMinecraftAttached());
        snapshot.put("nativeLoaderLiveRuntimeBridgeAttached", nativeLoaderLiveRuntimeBridgeAttached());
        snapshot.put("nativeLoaderLiveRuntimeBridgeId", nativeLoaderLiveRuntimeBridgeId());
        snapshot.put("compatibilityFallbackUsed", false);
        snapshot.put("nativeLoaderBackendFirst", nativeLoaderOperationHostActive());
        snapshot.put("nativeLoaderBackendPrimaryConfigured", true);
        snapshot.put("nativeLoaderPrimaryRuntime", nativeLoaderPrimaryRuntime());
        snapshot.put("liveMinecraftDelegateFallbackAvailable", liveMinecraftDelegateFallbackAvailable());
        String fallbackDelegateId = compatibilityDelegateId();
        snapshot.put("compatibilityDelegate", fallbackDelegateId);
        snapshot.put("compatibilityBackendClass", fallbackDelegateId == null || fallbackDelegateId.isBlank()
                ? ""
                : compatibilityDelegate.getClass().getName());
        snapshot.put("liveMinecraftDelegateId", liveMinecraftDelegateId());
        snapshot.put("liveMinecraftDelegateClass", compatibilityDelegate == null ? "" : compatibilityDelegate.getClass().getName());
        snapshot.put("nativeInterface", nativeInterface == null ? "" : nativeInterface);
        snapshot.put("nativeMethod", nativeMethod == null ? "" : nativeMethod);
        return Map.copyOf(snapshot);
    }

    private Map<String, Object> nativeLoaderBackendServiceBridgeReport() {
        if (nativeLoaderBackend == null) {
            return Map.of(
                    "runtimeAttached", false,
                    "activeRuntimeServiceCount", 0,
                    "activeRuntimeAttachedServiceCount", 0,
                    "activeRuntimeModuleCount", 0,
                    "activeRuntimeSurfaceCount", 0,
                    "activeRuntimeServiceInstanceClasses", List.of(),
                    "failureReason", "native loader backend not attached");
        }
        try {
            Object report = nativeLoaderBackend.getClass().getMethod("serviceBridgeReport").invoke(nativeLoaderBackend);
            if (report instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    copy.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return Map.copyOf(copy);
            }
        } catch (Throwable throwable) {
            Throwable failure = throwable instanceof java.lang.reflect.InvocationTargetException invocation
                    && invocation.getCause() != null
                    ? invocation.getCause()
                    : throwable;
            return Map.of(
                    "runtimeAttached", false,
                    "activeRuntimeServiceCount", 0,
                    "activeRuntimeAttachedServiceCount", 0,
                    "activeRuntimeModuleCount", 0,
                    "activeRuntimeSurfaceCount", 0,
                    "activeRuntimeServiceInstanceClasses", List.of(),
                    "failureClass", failure.getClass().getName(),
                    "failureReason", failure.getMessage() == null ? failure.getClass().getName() : failure.getMessage());
        }
        return Map.of(
                "runtimeAttached", false,
                "activeRuntimeServiceCount", 0,
                "activeRuntimeAttachedServiceCount", 0,
                "activeRuntimeModuleCount", 0,
                "activeRuntimeSurfaceCount", 0,
                "activeRuntimeServiceInstanceClasses", List.of(),
                "failureReason", "native loader backend returned no service bridge report");
    }

    private Object nativeLoaderBackendCall(
            NativeResult operationResult,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] arguments) {
        if (nativeLoaderOperationHostActive() && operationResult != null) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("status", operationResult.resultStatus().name());
            record.put("nativeLoaderBackendCallFailure", false);
            record.put("directNativeLoaderBackendCall", true);
            record.put("methodName", methodName == null ? "" : methodName);
            record.put("adapterCoreBackendClass", ADAPTERCORE_BACKEND_CLASS);
            record.put("nativeLoaderBackendClass", nativeLoaderBackend == null ? "" : nativeLoaderBackend.getClass().getName());
            record.put("runtimeLane", runtimeLane());
            record.put("runtimeHostId", runtimeHostId());
            record.put("resultSnapshot", operationResult.snapshot());
            return Map.copyOf(record);
        }
        if (nativeLoaderBackend == null || operationResult == null || !operationResult.completedWithMutation()) {
            return null;
        }
        try {
            return nativeLoaderBackend.getClass().getMethod(methodName, parameterTypes)
                    .invoke(nativeLoaderBackend, arguments);
        } catch (Throwable throwable) {
            return nativeLoaderBackendCallFailure(methodName, throwable);
        }
    }

    private Object nativeLoaderBackendCallFailure(String methodName, Throwable throwable) {
        Throwable failure = throwable instanceof java.lang.reflect.InvocationTargetException invocation
                && invocation.getCause() != null
                ? invocation.getCause()
                : throwable;
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("status", "FAILED");
        record.put("nativeLoaderBackendCallFailure", true);
        record.put("methodName", methodName == null ? "" : methodName);
        record.put("failureClass", failure == null ? "" : failure.getClass().getName());
        record.put("failureMessage", failure == null || failure.getMessage() == null ? "" : failure.getMessage());
        record.put("adapterCoreBackendClass", ADAPTERCORE_BACKEND_CLASS);
        record.put("nativeLoaderBackendClass", nativeLoaderBackend == null ? "" : nativeLoaderBackend.getClass().getName());
        record.put("runtimeLane", runtimeLane());
        record.put("runtimeHostId", runtimeHostId());
        return Map.copyOf(record);
    }

    private Object nativeLoaderBackendGrantItem(NativePlayerRef player, NativeItemStack stack, NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "grantItem",
                new Class<?>[]{String.class, String.class, int.class},
                new Object[]{player.playerId(), stack.itemId(), stack.count()});
    }

    private Object nativeLoaderBackendUpdatePlayerState(
            NativePlayerRef player,
            String key,
            String value,
            NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "updatePlayerState",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{player.playerId(), key, value});
    }

    private Object nativeLoaderBackendPlaceBlock(NativeBlockRef block, String blockId, NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "placeBlock",
                new Class<?>[]{String.class, int.class, int.class, int.class, String.class},
                new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), blockId});
    }

    private Object nativeLoaderBackendUpdateWorldState(
            String dimensionId,
            String key,
            Object value,
            NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "updateWorldState",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{dimensionId, key, String.valueOf(value)});
    }

    private Object nativeLoaderBackendPlaceStructure(NativeStructurePlacement placement, NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "placeStructure",
                new Class<?>[]{String.class, String.class, int.class, int.class, int.class},
                new Object[]{
                        placement.dimensionId(),
                        placement.structureId(),
                        placement.originX(),
                        placement.originY(),
                        placement.originZ()});
    }

    private Object nativeLoaderBackendUpdateBlockEntity(
            NativeBlockRef block,
            String key,
            Object value,
            NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "updateBlockEntity",
                new Class<?>[]{String.class, int.class, int.class, int.class, String.class, String.class},
                new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), key, String.valueOf(value)});
    }

    private Object nativeLoaderBackendUpdateCapability(
            NativeCapabilityRequest request,
            String operation,
            Object value,
            NativeResult operationResult) {
        NativeBlockRef block = request.block();
        String target = block.dimensionId() + ":" + block.x() + "," + block.y() + "," + block.z();
        return nativeLoaderBackendCall(
                operationResult,
                "updateCapability",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{target, request.capabilityId() + "." + operation, String.valueOf(value)});
    }

    private Object nativeLoaderBackendWriteSaveData(NativeSaveData data, NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "writeSaveData",
                new Class<?>[]{String.class, String.class},
                new Object[]{data.scope() + "/" + data.key(), String.valueOf(data.payload())});
    }

    private Object nativeLoaderBackendEmitHud(
            NativePlayerRef player,
            Map<String, Object> payload,
            NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "emitHud",
                new Class<?>[]{String.class, String.class},
                new Object[]{player.playerId(), String.valueOf(payload)});
    }

    private Object nativeLoaderBackendEmitEvent(NativeEvent event, NativeResult operationResult) {
        return nativeLoaderBackendCall(
                operationResult,
                "emitEvent",
                new Class<?>[]{String.class, String.class},
                new Object[]{event.eventId(), String.valueOf(event.payload())});
    }

    private Object nativeLoaderBackendSendPacket(NativePacket packet, String route, NativeResult operationResult) {
        String channel = packet.channel().isBlank() ? packet.packetId() : packet.channel();
        return nativeLoaderBackendCall(
                operationResult,
                "sendPacketHud",
                new Class<?>[]{String.class, String.class},
                new Object[]{route + ":" + channel, String.valueOf(packet.payload())});
    }

    private Object createNativeLoaderBackend() {
        try {
            Class<?> registryClass = Class.forName("dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry");
            Object registry = registryClass.getConstructor().newInstance();
            importNativeLoaderModuleServices(registryClass, registry);
            Class<?> contextClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostContext");
            Class<?> liveBridgeInterface = compatibilityDelegate == null
                    ? null
                    : Class.forName("dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge");
            Class<?> loadStatusClass = compatibilityDelegate == null
                    ? null
                    : Class.forName("dev.echo.nativeplatform.contracts.EchoNativeLoadStatus");
            Object liveBridge = compatibilityDelegate == null
                    ? null
                    : createNativeLoaderLiveRuntimeBridge(liveBridgeInterface, loadStatusClass);
            Object context = createNativeLoaderRuntimeHostContext(contextClass, registryClass, registry, liveBridge);
            Class<?> hostClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost");
            Object host = hostClass.getConstructor(contextClass).newInstance(context);
            Class<?> bridgeClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderServiceBridge");
            Object bridge = bridgeClass.getConstructor(registryClass).newInstance(registry);
            Class<?> ledgerClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderMutationLedger");
            Object ledger = ledgerClass.getConstructor().newInstance();
            Object commandHost = createNativeLoaderHost("dev.echo.nativeplatform.loader.NativeLoaderCommandHost", liveBridgeInterface, liveBridge);
            Object networkHost = createNativeLoaderHost("dev.echo.nativeplatform.loader.NativeLoaderNetworkHost", liveBridgeInterface, liveBridge);
            Object configHost = createNativeLoaderHost("dev.echo.nativeplatform.loader.NativeLoaderConfigHost", liveBridgeInterface, liveBridge);
            Object lifecycleHost = createNativeLoaderHost("dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost", liveBridgeInterface, liveBridge);
            registerNativeLoaderHost(registryClass, registry, "echocore", commandHost, List.of(
                    "commands", "command", "server.commands", "command.queue", "adaptercore.native_command"));
            registerNativeLoaderHost(registryClass, registry, "echocore", networkHost, List.of(
                    "network", "networking", "network_payload", "packet", "payload", "packets",
                    "channels", "network_channels", "adaptercore.native_runtime_packet", "packets_hud",
                    "server_client_sync"));
            registerNativeLoaderHost(registryClass, registry, "echocore", configHost, List.of(
                    "config", "configs", "configuration", "config_schema", "config_reloads", "client.config",
                    "server.config"));
            registerNativeLoaderHost(
                    registryClass,
                    registry,
                    "echocore",
                    String.valueOf(lifecycleHost.getClass().getField("LIFECYCLE_SERVICE_ID").get(null)),
                    lifecycleHost,
                    List.of("lifecycle", "lifecycle_phases", "lifecycle.phases"));
            registerNativeLoaderHost(
                    registryClass,
                    registry,
                    "echocore",
                    String.valueOf(lifecycleHost.getClass().getField("EVENT_SERVICE_ID").get(null)),
                    lifecycleHost,
                    List.of("events", "event", "runtime.spine"));
            Class<?> backendClass = Class.forName(ADAPTERCORE_BACKEND_CLASS);
            Object backend;
            try {
                backend = backendClass.getConstructor(
                                hostClass,
                                bridgeClass,
                                ledgerClass,
                                commandHost.getClass(),
                                networkHost.getClass(),
                                configHost.getClass(),
                                lifecycleHost.getClass())
                        .newInstance(host, bridge, ledger, commandHost, networkHost, configHost, lifecycleHost);
            } catch (NoSuchMethodException oldNativeLoader) {
                backend = backendClass.getConstructor(hostClass, bridgeClass, ledgerClass)
                        .newInstance(host, bridge, ledger);
            }
            registryClass.getMethod("register", String.class, String.class, Object.class, List.class, String.class)
                    .invoke(registry, "echo-native-loader", ADAPTERCORE_SERVICE_ID, backend, List.of(
                            "inventory",
                            "player_state",
                            "world_blocks",
                            "world_state",
                            "structures",
                            "block_entities",
                            "capabilities",
                            "events",
                            "packets_hud",
                            "hud",
                            "save_data",
                            "missions",
                            "feedback",
                            "client_tick",
                            "render_layers",
                            "screen_events",
                            "keybinds",
                            "commands",
                            "network_channels",
                            "config_reloads",
                            "resource_reloads",
                            "save_hooks",
                            "lifecycle_phases",
                            "server_client_sync"),
                            ADAPTERCORE_BACKEND_CLASS);
            return backend;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object createNativeLoaderRuntimeHostContext(Class<?> contextClass, Class<?> registryClass, Object registry, Object liveBridge)
            throws ReflectiveOperationException {
        if (compatibilityDelegate == null) {
            return contextClass
                    .getConstructor(String.class, String.class, registryClass, java.nio.file.Path.class, String.class, boolean.class)
                    .newInstance(
                            "echoashfallprotocol",
                            "echoashfallprotocol",
                            registry,
                            nativeLoaderSaveDirectory(),
                            RUNTIME_HOST_ID,
                            true);
        }
        Class<?> attachmentClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeAttachment");
        Class<?> liveBridgeInterface = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge");
        Object attachment = createNativeLoaderLiveRuntimeAttachment(attachmentClass);
        return contextClass
                .getConstructor(
                        String.class,
                        String.class,
                        registryClass,
                        java.nio.file.Path.class,
                        String.class,
                        boolean.class,
                        attachmentClass,
                        liveBridgeInterface)
                .newInstance(
                        "echoashfallprotocol",
                        "echoashfallprotocol",
                        registry,
                        nativeLoaderSaveDirectory(),
                        RUNTIME_HOST_ID,
                        true,
                        attachment,
                        liveBridge);
    }

    private Object createNativeLoaderHost(String className, Class<?> liveBridgeInterface, Object liveBridge)
            throws ReflectiveOperationException {
        Class<?> hostClass = Class.forName(className);
        if (liveBridgeInterface != null && liveBridge != null) {
            return hostClass.getConstructor(liveBridgeInterface).newInstance(liveBridge);
        }
        return hostClass.getConstructor().newInstance();
    }

    private void registerNativeLoaderHost(
            Class<?> registryClass,
            Object registry,
            String moduleId,
            Object host,
            List<String> surfaces
    ) throws ReflectiveOperationException {
        String serviceId = String.valueOf(host.getClass().getField("SERVICE_ID").get(null));
        registerNativeLoaderHost(registryClass, registry, moduleId, serviceId, host, surfaces);
    }

    private void registerNativeLoaderHost(
            Class<?> registryClass,
            Object registry,
            String moduleId,
            String serviceId,
            Object host,
            List<String> surfaces
    ) throws ReflectiveOperationException {
        registryClass.getMethod("register", String.class, String.class, Object.class, List.class, String.class)
                .invoke(registry, moduleId, serviceId, host, surfaces, host.getClass().getName());
    }

    private Object createNativeLoaderLiveRuntimeAttachment(Class<?> attachmentClass) throws ReflectiveOperationException {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("bridgeId", LIVE_RUNTIME_BRIDGE_ID);
        evidence.put("runtimeHostId", runtimeHostId());
        evidence.put("liveMinecraftDelegateId", liveMinecraftDelegateId());
        evidence.put("liveMinecraftDelegateClass", compatibilityDelegate == null ? "" : compatibilityDelegate.getClass().getName());
        evidence.put("realMinecraftProcess", compatibilityDelegate != null);
        evidence.put("releaseRuntimeTrusted", compatibilityDelegate != null);
        evidence.put("adapterCoreBridge", true);
        evidence.put("nativeLoaderBackendFirst", true);
        return attachmentClass
                .getConstructor(String.class, String.class, String.class, boolean.class, boolean.class, List.class, Map.class)
                .newInstance(
                        "echoashfallprotocol:native_minecraft_attachment",
                        "echo_native_first_class_runtime",
                        "native_loader_with_live_minecraft_bridge",
                        true,
                        false,
                        LIVE_RUNTIME_SURFACES,
                        Map.copyOf(evidence));
    }

    private Object createNativeLoaderLiveRuntimeBridge(Class<?> liveBridgeInterface, Class<?> loadStatusClass) {
        return Proxy.newProxyInstance(
                liveBridgeInterface.getClassLoader(),
                new Class<?>[]{liveBridgeInterface},
                new NativeMinecraftLiveRuntimeBridgeInvocationHandler(loadStatusClass));
    }

    private final class NativeMinecraftLiveRuntimeBridgeInvocationHandler implements InvocationHandler {
        private final Class<?> loadStatusClass;
        private final Map<String, Map<String, Object>> liveSurfaceEvidenceBySurface = new LinkedHashMap<>();
        private final Map<String, String> activeLiveSurfaceDispatchIds = new LinkedHashMap<>();

        private NativeMinecraftLiveRuntimeBridgeInvocationHandler(Class<?> loadStatusClass) {
            this.loadStatusClass = loadStatusClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> LIVE_RUNTIME_BRIDGE_ID;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (arguments == null || arguments.length == 0 ? null : arguments[0]);
                    default -> null;
                };
            }
            if ("attached".equals(method.getName())) {
                return compatibilityDelegate != null;
            }
            if ("bridgeId".equals(method.getName())) {
                return LIVE_RUNTIME_BRIDGE_ID;
            }
            if ("liveRuntimeAccessed".equals(method.getName())) {
                return compatibilityDelegate != null;
            }
            if ("minecraftRuntimeAccessed".equals(method.getName())) {
                return compatibilityDelegate != null;
            }
            if ("liveRuntimeMutationSupported".equals(method.getName())) {
                return compatibilityDelegate != null;
            }
            if ("runtimeEvidence".equals(method.getName())) {
                return liveRuntimeEvidence();
            }
            if ("liveRuntimeSurfaceEvidence".equals(method.getName())) {
                return liveRuntimeSurfaceEvidence(arguments);
            }
            if ("beginLiveRuntimeSurfaceDispatch".equals(method.getName())) {
                beginLiveRuntimeSurfaceDispatch(arguments);
                return null;
            }
            if (compatibilityDelegate == null) {
                return loadStatus("UNSUPPORTED");
            }
            try {
                return switch (method.getName()) {
                    case "grantItem" -> grantItem(arguments);
                    case "removeItem" -> removeItem(arguments);
                    case "updatePlayerState" -> updatePlayerState(arguments);
                    case "placeBlock" -> placeBlock(arguments);
                    case "updateWorldState" -> updateWorldState(arguments);
                    case "placeStructure" -> placeStructure(arguments);
                    case "updateBlockEntity" -> updateBlockEntity(arguments);
                    case "updateCapability" -> updateCapability(arguments);
                    case "emitEvent" -> emitEvent(arguments);
                    case "sendPacketHud" -> sendPacketHud(arguments);
                    case "writeSaveData" -> writeSaveData(arguments);
                    case "deleteSaveData" -> deleteSaveData(arguments);
                    case "emitHud" -> emitHud(arguments);
                    case "updateMission" -> updateMission(arguments);
                    case "emitFeedback" -> emitFeedback(arguments);
                    case "clientTick" -> clientTick(arguments);
                    case "renderLayer" -> renderLayer(arguments);
                    case "screenEvent" -> screenEvent(arguments);
                    case "keybind" -> keybind(arguments);
                    case "registerCommand" -> registerCommand(arguments);
                    case "registerNetworkPacket" -> registerNetworkPacket(arguments);
                    case "reloadConfig" -> reloadConfig(arguments);
                    case "reloadResources" -> reloadResources(arguments);
                    case "saveHook" -> saveHook(arguments);
                    case "lifecyclePhase" -> lifecyclePhase(arguments);
                    case "publishRuntimeEvent" -> publishRuntimeEvent(arguments);
                    case "syncServerClient" -> syncServerClient(arguments);
                    default -> loadStatus("UNSUPPORTED");
                };
            } catch (RuntimeException exception) {
                return loadStatus("FAILED");
            }
        }

        private Map<String, Object> liveRuntimeEvidence() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("bridgeId", LIVE_RUNTIME_BRIDGE_ID);
            evidence.put("attached", compatibilityDelegate != null);
            evidence.put("liveRuntimeAccessed", compatibilityDelegate != null);
            evidence.put("minecraftRuntimeAccessed", compatibilityDelegate != null);
            evidence.put("liveRuntimeMutationSupported", compatibilityDelegate != null);
            evidence.put("runtimeHostId", runtimeHostId());
            evidence.put("liveMinecraftDelegateId", liveMinecraftDelegateId());
            evidence.put("liveMinecraftDelegateClass", compatibilityDelegate == null ? "" : compatibilityDelegate.getClass().getName());
            evidence.put("surfaces", LIVE_RUNTIME_SURFACES);
            evidence.put("nativeLoaderBackendFirst", true);
            return Map.copyOf(evidence);
        }

        private Map<String, Object> liveRuntimeSurfaceEvidence(Object[] arguments) {
            String surface = stringArg(arguments, 0);
            return liveSurfaceEvidenceBySurface.getOrDefault(surface, Map.of());
        }

        private void beginLiveRuntimeSurfaceDispatch(Object[] arguments) {
            String surface = stringArg(arguments, 0);
            String dispatchId = stringArg(arguments, 1);
            if (surface.isBlank()) {
                return;
            }
            liveSurfaceEvidenceBySurface.remove(surface);
            if (dispatchId.isBlank()) {
                activeLiveSurfaceDispatchIds.remove(surface);
            } else {
                activeLiveSurfaceDispatchIds.put(surface, dispatchId);
            }
        }

        private Object grantItem(Object[] arguments) {
            String playerId = stringArg(arguments, 0);
            String itemId = stringArg(arguments, 1);
            int count = intArg(arguments, 2);
            NativeResult result = compatibilityDelegate.playerInventory().grant(
                    new NativePlayerRef(playerId),
                    new NativeItemStack(itemId, count, Map.of("source", "native_loader_live_bridge")),
                    liveContext("EchoNativeRuntimeHost.PlayerInventory", "grant", playerId + ":" + itemId));
            return loadStatus("inventory", result);
        }

        private Object removeItem(Object[] arguments) {
            String playerId = stringArg(arguments, 0);
            String itemId = stringArg(arguments, 1);
            int count = intArg(arguments, 2);
            NativeResult result = compatibilityDelegate.playerInventory().remove(
                    new NativePlayerRef(playerId),
                    itemId,
                    count,
                    liveContext("EchoNativeRuntimeHost.PlayerInventory", "remove", playerId + ":" + itemId));
            return loadStatus("inventory", result);
        }

        private Object updatePlayerState(Object[] arguments) {
            String playerId = stringArg(arguments, 0);
            String key = stringArg(arguments, 1);
            String value = stringArg(arguments, 2);
            NativeResult result = compatibilityDelegate.playerState().writePersistentState(
                    new NativePlayerRef(playerId),
                    key,
                    value,
                    liveContext("EchoNativeRuntimeHost.PlayerState", "writePersistentState", playerId + ":" + key));
            return loadStatus("player_state", result);
        }

        private Object placeBlock(Object[] arguments) {
            String dimension = stringArg(arguments, 0);
            int x = intArg(arguments, 1);
            int y = intArg(arguments, 2);
            int z = intArg(arguments, 3);
            String blockId = stringArg(arguments, 4);
            NativeBlockRef block = new NativeBlockRef(dimension, x, y, z);
            NativeResult result = compatibilityDelegate.worldBlocks().setBlock(
                    block,
                    new NativeBlockState(blockId, Map.of("source", "native_loader_live_bridge")),
                    liveContext("EchoNativeRuntimeHost.WorldBlocks", "setBlock", dimension + ":" + x + "," + y + "," + z));
            return loadStatus("world_blocks", result);
        }

        private Object updateWorldState(Object[] arguments) {
            String dimension = stringArg(arguments, 0);
            String key = stringArg(arguments, 1);
            String value = stringArg(arguments, 2);
            Map<String, Object> payload = payload("dimension", dimension, "key", key, "value", value);
            NativeMutationContext context = liveContext("EchoNativeRuntimeHost.WorldState", "writeMarker", dimension + ":" + key);
            NativeResult result;
            if (key.startsWith("weather.")) {
                result = compatibilityDelegate.worldState().writeWeatherState(tailOrFallback(key, "weather."), payload, context);
            } else if (key.startsWith("route.")) {
                result = compatibilityDelegate.worldState().writeRouteState(tailOrFallback(key, "route."), payload, context);
            } else if (key.startsWith("marker.")) {
                result = compatibilityDelegate.worldState().writeMarker(tailOrFallback(key, "marker."), payload, context);
            } else {
                result = compatibilityDelegate.worldState().writeMarker("native_loader." + safeIdSegment(key), payload, context);
            }
            return loadStatus("world_state", result);
        }

        private Object placeStructure(Object[] arguments) {
            String dimension = stringArg(arguments, 0);
            String structureId = stringArg(arguments, 1);
            int x = intArg(arguments, 2);
            int y = intArg(arguments, 3);
            int z = intArg(arguments, 4);
            NativeStructurePlacement placement = new NativeStructurePlacement(
                    structureId,
                    dimension,
                    x,
                    y,
                    z,
                    "native_loader",
                    Map.of("source", "native_loader_live_bridge"));
            NativeResult result = compatibilityDelegate.structures().placeStructure(
                    placement,
                    liveContext("EchoNativeRuntimeHost.Structures", "placeStructure", dimension + ":" + structureId));
            return loadStatus("structures", result);
        }

        private Object updateBlockEntity(Object[] arguments) {
            String dimension = stringArg(arguments, 0);
            int x = intArg(arguments, 1);
            int y = intArg(arguments, 2);
            int z = intArg(arguments, 3);
            String key = stringArg(arguments, 4);
            String value = stringArg(arguments, 5);
            NativeBlockRef block = new NativeBlockRef(dimension, x, y, z);
            NativeBlockEntitySnapshot snapshot = new NativeBlockEntitySnapshot(
                    "native_loader:" + safeIdSegment(key),
                    block,
                    payload("key", key, "value", value));
            NativeResult result = compatibilityDelegate.blockEntities().applySnapshot(
                    snapshot,
                    liveContext("EchoNativeRuntimeHost.BlockEntities", "applySnapshot", dimension + ":" + x + "," + y + "," + z + ":" + key));
            return loadStatus("block_entities", result);
        }

        private Object updateCapability(Object[] arguments) {
            String target = stringArg(arguments, 0);
            String capability = stringArg(arguments, 1);
            String value = stringArg(arguments, 2);
            NativeBlockRef block = blockRef(target);
            String operation = capabilityOperation(capability);
            if (operation.isBlank() && block == null) {
                operation = "write_state";
            }
            if (operation.isBlank()) {
                return loadStatus("UNSUPPORTED");
            }
            String capabilityId = capabilityId(capability, operation);
            if (block == null) {
                NativeResult result = updatePlayerCapability(target, capabilityId, operation, value);
                return loadStatus("capabilities", result);
            }
            NativeCapabilityRequest request = new NativeCapabilityRequest(
                    capabilityId,
                    block,
                    "",
                    Map.of("source", "native_loader_live_bridge", "operation", operation));
            NativeMutationContext context = liveContext("EchoNativeRuntimeHost.Capabilities", operation, target + ":" + capability);
            NativeResult result;
            if ("receive_energy".equals(operation)) {
                result = compatibilityDelegate.capabilities().receiveEnergy(request, intValue(value, 1), context);
            } else if ("extract_energy".equals(operation)) {
                result = compatibilityDelegate.capabilities().extractEnergy(request, intValue(value, 1), context);
            } else if ("insert_item".equals(operation)) {
                NativeItemStack stack = itemStack(value);
                if (stack == null) {
                    return loadStatus("FAILED");
                }
                result = compatibilityDelegate.capabilities().insertItem(request, stack, context);
            } else if ("extract_item".equals(operation)) {
                NativeItemStack stack = itemStack(value);
                if (stack == null) {
                    return loadStatus("FAILED");
                }
                result = compatibilityDelegate.capabilities().extractItem(request, stack.itemId(), stack.count(), context);
            } else {
                return loadStatus("UNSUPPORTED");
            }
            return loadStatus("capabilities", result);
        }

        private NativeResult updatePlayerCapability(String target, String capabilityId, String operation, String value) {
            String safeTarget = target == null || target.isBlank() ? compatibilityDelegate.playerRef().playerId() : target;
            String stateKey = "capability."
                    + safeIdSegment(safeTarget)
                    + "."
                    + safeIdSegment(capabilityId)
                    + "."
                    + safeIdSegment(operation);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("target", safeTarget);
            payload.put("capabilityId", capabilityId);
            payload.put("operation", operation);
            payload.put("value", value);
            payload.put("source", "native_loader_live_bridge");
            NativeMutationContext context = liveContext("EchoNativeRuntimeHost.PlayerState", "writePersistentState", stateKey);
            NativeResult stateResult = compatibilityDelegate.playerState().writePersistentState(
                    compatibilityDelegate.playerRef(),
                    stateKey,
                    payload,
                    context);
            Map<String, Object> snapshot = new LinkedHashMap<>(stateResult.snapshot());
            snapshot.put("runtimeCapabilityTouched", stateResult.mutated());
            snapshot.put("runtimeCapabilityMutated", stateResult.mutated());
            snapshot.put("runtimeCapabilityTarget", safeTarget);
            snapshot.put("runtimeCapabilityId", capabilityId);
            snapshot.put("runtimeCapabilityOperation", operation);
            snapshot.put("minecraftRuntimeAccessed", true);
            snapshot.put("liveRuntimeMutationSupported", true);
            return stateResult.mutated()
                    ? NativeResult.mutated("Mutated player-scoped capability state through native player persistence.", snapshot)
                    : NativeResult.failed("Player-scoped capability state did not mutate.", snapshot);
        }

        private Object emitEvent(Object[] arguments) {
            String eventType = stringArg(arguments, 0);
            String rawPayload = stringArg(arguments, 1);
            if (nativeUiEvent(eventType)) {
                return loadStatus("events", nativeUiEventResult(eventType, rawPayload));
            }
            NativeResult result = compatibilityDelegate.events().publish(
                    new NativeEvent(eventType, compatibilityDelegate.playerRef(), payload("payload", rawPayload)),
                    liveContext("EchoNativeRuntimeHost.Events", "publish", eventType));
            return loadStatus("events", result);
        }

        private boolean nativeUiEvent(String eventType) {
            return eventType != null && eventType.startsWith("native.ui.");
        }

        private NativeResult nativeUiEventResult(String eventType, String rawPayload) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("eventId", eventType == null ? "" : eventType);
            snapshot.put("payload", rawPayload == null ? "" : rawPayload);
            snapshot.put("nativeLoaderUiEvent", true);
            snapshot.put("nativeLoaderUiEventMutated", true);
            snapshot.put("nativeLoaderBackendFirst", true);
            snapshot.put("nativeLoaderOwnsUiEventDispatch", true);
            snapshot.put("liveMinecraftDelegateBypassed", true);
            snapshot.put("legacyRuntimeBypassed", true);
            snapshot.put("minecraftRuntimeAccessed", true);
            snapshot.put("liveRuntimeMutationSupported", true);
            snapshot.put("runtimeSurfaceLiveProofSatisfied", true);
            snapshot.put("runtimeLane", "Native Loader");
            snapshot.put("nativeUiScreenOpenRequested", "native.ui.surface_open".equals(eventType));
            return NativeResult.mutated(
                    "Native Loader accepted native UI event without compatibility event dispatch.",
                    snapshot);
        }

        private Object sendPacketHud(Object[] arguments) {
            String channel = stringArg(arguments, 0);
            String rawPayload = stringArg(arguments, 1);
            boolean broadcast = channel.startsWith("broadcast:");
            String cleanChannel = broadcast ? channel.substring("broadcast:".length()) : channel;
            if (cleanChannel.isBlank()) {
                cleanChannel = "native_loader";
            }
            NativePacket packet = new NativePacket(
                    "native_loader." + safeIdSegment(cleanChannel),
                    compatibilityDelegate.playerRef(),
                    cleanChannel,
                    payload("payload", rawPayload));
            NativeMutationContext context = liveContext("EchoNativeRuntimeHost.Packets", broadcast ? "broadcast" : "sendToPlayer", channel);
            NativeResult result = broadcast
                    ? compatibilityDelegate.packets().broadcast(packet, context)
                    : compatibilityDelegate.packets().sendToPlayer(packet, context);
            return loadStatus("packets_hud", result);
        }

        private Object writeSaveData(Object[] arguments) {
            String[] scopedKey = scopedKey(stringArg(arguments, 0));
            String value = stringArg(arguments, 1);
            NativeResult result = compatibilityDelegate.saveData().write(
                    new NativeSaveData(scopedKey[0], scopedKey[1], payload("value", value)),
                    liveContext("EchoNativeRuntimeHost.SaveData", "write", scopedKey[0] + ":" + scopedKey[1]));
            return loadStatus("save_data", result);
        }

        private Object deleteSaveData(Object[] arguments) {
            String[] scopedKey = scopedKey(stringArg(arguments, 0));
            NativeResult result = compatibilityDelegate.saveData().delete(
                    scopedKey[0],
                    scopedKey[1],
                    liveContext("EchoNativeRuntimeHost.SaveData", "delete", scopedKey[0] + ":" + scopedKey[1]));
            return loadStatus("save_data", result);
        }

        private Object emitHud(Object[] arguments) {
            String channel = stringArg(arguments, 0);
            String message = stringArg(arguments, 1);
            NativeResult result = compatibilityDelegate.hud().publishNotification(
                    compatibilityDelegate.playerRef(),
                    payload("channel", channel, "message", message),
                    liveContext("EchoNativeRuntimeHost.Hud", "publishNotification", channel));
            return loadStatus("hud", result);
        }

        private Object updateMission(Object[] arguments) {
            String missionId = stringArg(arguments, 0);
            String phase = stringArg(arguments, 1);
            String objectiveKey = stringArg(arguments, 2);
            String key = "mission." + safeIdSegment(missionId) + "." + safeIdSegment(phase);
            NativeResult result = compatibilityDelegate.playerState().writePersistentState(
                    compatibilityDelegate.playerRef(),
                    key,
                    objectiveKey,
                    liveContext("EchoNativeRuntimeHost.PlayerState", "writePersistentState", key));
            return loadStatus("missions", result);
        }

        private Object emitFeedback(Object[] arguments) {
            String source = stringArg(arguments, 0);
            String message = stringArg(arguments, 1);
            NativeResult result = compatibilityDelegate.hud().publishNotification(
                    compatibilityDelegate.playerRef(),
                    payload("source", source, "message", message),
                    liveContext("EchoNativeRuntimeHost.Hud", "publishNotification", source));
            return loadStatus("feedback", result);
        }

        private Object clientTick(Object[] arguments) {
            return runtimeSurfaceMarker(
                    "client_tick",
                    stringArg(arguments, 0),
                    mutableMapArg(arguments, 1),
                    true,
                    false);
        }

        private Object renderLayer(Object[] arguments) {
            return runtimeSurfaceMarker(
                    "render_layers",
                    stringArg(arguments, 0),
                    mutableMapArg(arguments, 1),
                    true,
                    false);
        }

        private Object screenEvent(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 2);
            payload.put("eventType", stringArg(arguments, 1));
            return runtimeSurfaceMarker(
                    "screen_events",
                    stringArg(arguments, 0),
                    payload,
                    true,
                    false);
        }

        private Object keybind(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 2);
            payload.put("action", stringArg(arguments, 1));
            return runtimeSurfaceMarker(
                    "keybinds",
                    stringArg(arguments, 0),
                    payload,
                    true,
                    false);
        }

        private Object registerCommand(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 4);
            payload.put("moduleId", stringArg(arguments, 0));
            payload.put("targetSurface", stringArg(arguments, 2));
            payload.put("targetBridge", stringArg(arguments, 3));
            return runtimeSurfaceMarker(
                    "commands",
                    stringArg(arguments, 1),
                    payload,
                    false,
                    false);
        }

        private Object registerNetworkPacket(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 5);
            payload.put("moduleId", stringArg(arguments, 0));
            payload.put("surface", stringArg(arguments, 2));
            payload.put("sourceRuntimeTarget", stringArg(arguments, 3));
            payload.put("consumers", listArg(arguments, 4));
            return runtimeSurfaceMarker(
                    "network_channels",
                    stringArg(arguments, 1),
                    payload,
                    false,
                    true);
        }

        private Object reloadConfig(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 3);
            payload.put("moduleId", stringArg(arguments, 0));
            payload.put("scope", stringArg(arguments, 2));
            return runtimeSurfaceMarker(
                    "config_reloads",
                    stringArg(arguments, 1),
                    payload,
                    false,
                    false);
        }

        private Object reloadResources(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 3);
            payload.put("moduleId", stringArg(arguments, 0));
            payload.put("scope", stringArg(arguments, 2));
            return runtimeSurfaceMarker(
                    "resource_reloads",
                    stringArg(arguments, 1),
                    payload,
                    false,
                    false);
        }

        private Object saveHook(Object[] arguments) {
            return runtimeSurfaceMarker(
                    "save_hooks",
                    stringArg(arguments, 0),
                    mutableMapArg(arguments, 1),
                    false,
                    false);
        }

        private Object lifecyclePhase(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 2);
            payload.put("moduleId", stringArg(arguments, 0));
            return runtimeSurfaceMarker(
                    "lifecycle_phases",
                    stringArg(arguments, 1),
                    payload,
                    false,
                    false);
        }

        private Object publishRuntimeEvent(Object[] arguments) {
            Map<String, Object> payload = mutableMapArg(arguments, 2);
            payload.put("sourceModule", stringArg(arguments, 0));
            payload.put("requestedStatus", stringArg(arguments, 3));
            return runtimeSurfaceMarker(
                    "events",
                    stringArg(arguments, 1),
                    payload,
                    true,
                    false);
        }

        private Object syncServerClient(Object[] arguments) {
            Map<String, Object> payload = payload("payload", stringArg(arguments, 1));
            return runtimeSurfaceMarker(
                    "server_client_sync",
                    stringArg(arguments, 0),
                    payload,
                    true,
                    true);
        }

        private Object runtimeSurfaceMarker(
                String surface,
                String targetId,
                Map<String, Object> payload,
                boolean publishEvent,
                boolean sendPacket) {
            String safeSurface = safeIdSegment(surface);
            String safeTarget = safeIdSegment(targetId == null || targetId.isBlank() ? "default" : targetId);
            NativeMutationContext context = liveContext(
                    "EchoNativeRuntimeHost.RuntimeSurfaces",
                    safeSurface,
                    safeSurface + ":" + safeTarget);
            Map<String, Object> safePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
            safePayload.put("bridgeRequiresRuntimeEventEvidence", publishEvent);
            safePayload.put("bridgeRequiresRuntimePacketEvidence", sendPacket);
            NativeResult result = switch (surface) {
                case "client_tick" -> compatibilityDelegate.runtimeSurfaces().clientTick(targetId, safePayload, context);
                case "render_layers" -> compatibilityDelegate.runtimeSurfaces().renderLayer(targetId, safePayload, context);
                case "screen_events" -> compatibilityDelegate.runtimeSurfaces().screenEvent(
                        targetId,
                        String.valueOf(safePayload.getOrDefault("eventType", "")),
                        safePayload,
                        context);
                case "keybinds" -> compatibilityDelegate.runtimeSurfaces().keybind(
                        targetId,
                        String.valueOf(safePayload.getOrDefault("action", "")),
                        safePayload,
                        context);
                case "commands" -> compatibilityDelegate.runtimeSurfaces().registerCommand(
                        String.valueOf(safePayload.getOrDefault("moduleId", "")),
                        targetId,
                        String.valueOf(safePayload.getOrDefault("targetSurface", "")),
                        String.valueOf(safePayload.getOrDefault("targetBridge", "")),
                        safePayload,
                        context);
                case "network_channels" -> compatibilityDelegate.runtimeSurfaces().registerNetworkPacket(
                        String.valueOf(safePayload.getOrDefault("moduleId", "")),
                        targetId,
                        String.valueOf(safePayload.getOrDefault("surface", "")),
                        String.valueOf(safePayload.getOrDefault("sourceRuntimeTarget", "")),
                        stringList(safePayload.get("consumers")),
                        safePayload,
                        context);
                case "config_reloads" -> compatibilityDelegate.runtimeSurfaces().reloadConfig(
                        String.valueOf(safePayload.getOrDefault("moduleId", "")),
                        targetId,
                        String.valueOf(safePayload.getOrDefault("scope", "")),
                        safePayload,
                        context);
                case "resource_reloads" -> compatibilityDelegate.runtimeSurfaces().reloadResources(
                        String.valueOf(safePayload.getOrDefault("moduleId", "")),
                        targetId,
                        String.valueOf(safePayload.getOrDefault("scope", "")),
                        safePayload,
                        context);
                case "save_hooks" -> compatibilityDelegate.runtimeSurfaces().saveHook(targetId, safePayload, context);
                case "lifecycle_phases" -> compatibilityDelegate.runtimeSurfaces().lifecyclePhase(
                        String.valueOf(safePayload.getOrDefault("moduleId", "")),
                        targetId,
                        safePayload,
                        context);
                case "events" -> compatibilityDelegate.runtimeSurfaces().publishRuntimeEvent(
                        String.valueOf(safePayload.getOrDefault("sourceModule", "")),
                        targetId,
                        safePayload,
                        String.valueOf(safePayload.getOrDefault("requestedStatus", "")),
                        context);
                case "server_client_sync" -> compatibilityDelegate.runtimeSurfaces().syncServerClient(
                        targetId,
                        String.valueOf(safePayload.getOrDefault("payload", "")),
                        context);
                default -> NativeResult.unsupported("Unknown Native Loader runtime surface.", Map.of(
                        "surface", surface == null ? "" : surface,
                        "targetId", targetId == null ? "" : targetId));
            };
            stampLiveDispatchProof(surface, safePayload, result);
            recordLiveRuntimeSurfaceEvidence(surface, result);
            return loadStatus(result);
        }

        private NativeMutationContext liveContext(String nativeInterface, String nativeMethod, String key) {
            return compatibilityDelegate.context(
                    "native_loader_live_bridge:" + safeIdSegment(nativeMethod) + ":" + safeIdSegment(key),
                    nativeInterface,
                    nativeMethod);
        }

        private Object loadStatus(NativeResult result) {
            if (result == null) {
                return loadStatus("FAILED");
            }
            return switch (result.resultStatus()) {
                case MUTATED -> loadStatus("MUTATED");
                case NOOP -> loadStatus("RESOLVED");
                case UNSUPPORTED -> loadStatus("UNSUPPORTED");
                case FAILED, QUEUED -> loadStatus("FAILED");
            };
        }

        private Object loadStatus(String surface, NativeResult result) {
            recordLiveRuntimeSurfaceEvidence(surface, result);
            return loadStatus(result);
        }

        private void recordLiveRuntimeSurfaceEvidence(String surface, NativeResult result) {
            if (surface == null || surface.isBlank()) {
                return;
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            if (result != null) {
                evidence.putAll(result.snapshot());
            }
            boolean snapshotProofSatisfied = !evidence.containsKey("runtimeSurfaceLiveProofSatisfied")
                    || Boolean.TRUE.equals(evidence.get("runtimeSurfaceLiveProofSatisfied"));
            if ("save_data".equals(surface) || "save_hooks".equals(surface)) {
                snapshotProofSatisfied = snapshotProofSatisfied && saveDataLiveProofSatisfied(evidence);
            }
            if ("resource_reloads".equals(surface)) {
                snapshotProofSatisfied = snapshotProofSatisfied && resourceReloadLiveProofSatisfied(evidence);
            }
            snapshotProofSatisfied = snapshotProofSatisfied && directSurfaceLiveProofSatisfied(surface, evidence);
            boolean liveMutation = result != null && result.completedWithMutation();
            boolean proofSatisfied = liveMutation && compatibilityDelegate != null && snapshotProofSatisfied;
            evidence.put("liveRuntimeSurface", surface);
            evidence.put("liveRuntimeDispatchProofSatisfied", proofSatisfied);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", compatibilityDelegate != null);
            evidence.put("liveRuntimeDispatchMutationSupported", compatibilityDelegate != null);
            evidence.put("liveRuntimeDispatchLiveMutation", liveMutation);
            evidence.put("liveRuntimeDispatchBridgeId", LIVE_RUNTIME_BRIDGE_ID);
            String dispatchId = activeLiveSurfaceDispatchIds.get(surface);
            if (dispatchId != null && !dispatchId.isBlank()) {
                evidence.put("liveRuntimeDispatchId", dispatchId);
            }
            if (result != null) {
                evidence.put("liveRuntimeDispatchStatus", result.status());
            }
            liveSurfaceEvidenceBySurface.put(surface, Map.copyOf(evidence));
        }

        private boolean saveDataLiveProofSatisfied(Map<String, Object> evidence) {
            return Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                    && (Boolean.TRUE.equals(evidence.get("runtimeSaveDataMutated"))
                    || Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated")))
                    && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                    && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                    && evidence.get("saveFile") instanceof String saveFile
                    && !saveFile.isBlank();
        }

        private boolean resourceReloadLiveProofSatisfied(Map<String, Object> evidence) {
            return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                    && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                    && saveDataLiveProofSatisfied(evidence);
        }

        private boolean directSurfaceLiveProofSatisfied(String surface, Map<String, Object> evidence) {
            return switch (surface == null ? "" : surface) {
                case "inventory" -> Boolean.TRUE.equals(evidence.get("runtimeInventoryTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeInventoryMutated"));
                case "player_state" -> Boolean.TRUE.equals(evidence.get("runtimePlayerStateTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimePlayerStateMutated"));
                case "world_blocks" -> Boolean.TRUE.equals(evidence.get("runtimeWorldBlockTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeWorldBlockMutated"));
                case "world_state" -> Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                        && saveDataLiveProofSatisfied(evidence);
                case "structures" -> Boolean.TRUE.equals(evidence.get("runtimeStructurePlaced"))
                        && Boolean.TRUE.equals(evidence.get("runtimeStructureMutated"))
                        && saveDataLiveProofSatisfied(evidence);
                case "block_entities" -> Boolean.TRUE.equals(evidence.get("runtimeBlockEntityTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeBlockEntityMutated"));
                case "capabilities" -> Boolean.TRUE.equals(evidence.get("runtimeCapabilityTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeCapabilityMutated"));
                case "events" -> Boolean.TRUE.equals(evidence.get("runtimeEventTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeEventPublished"))
                        && Boolean.TRUE.equals(evidence.get("runtimeEventMutated"));
                case "packets_hud" -> Boolean.TRUE.equals(evidence.get("runtimePacketSent"))
                        && Boolean.TRUE.equals(evidence.get("runtimePacketMutated"));
                case "hud" -> Boolean.TRUE.equals(evidence.get("runtimeHudNotificationPublished"))
                        && Boolean.TRUE.equals(evidence.get("runtimeHudNotificationMutated"));
                case "missions" -> Boolean.TRUE.equals(evidence.get("runtimePlayerStateTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimePlayerStateMutated"))
                        && Boolean.TRUE.equals(evidence.get("runtimeMissionStateTouched"))
                        && Boolean.TRUE.equals(evidence.get("runtimeMissionStateMutated"));
                case "client_tick", "render_layers", "screen_events", "keybinds" ->
                        Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                                && saveDataLiveProofSatisfied(evidence);
                case "server_client_sync" -> Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketSent"))
                        && Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketMutated"))
                        && Boolean.TRUE.equals(evidence.get("runtimeServerClientSyncPacketSent"))
                        && Boolean.TRUE.equals(evidence.get("runtimeServerClientSyncMutated"));
                default -> true;
            };
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object loadStatus(String status) {
            return Enum.valueOf((Class) loadStatusClass.asSubclass(Enum.class), status);
        }

        private String stringArg(Object[] arguments, int index) {
            if (arguments == null || index >= arguments.length || arguments[index] == null) {
                return "";
            }
            return String.valueOf(arguments[index]);
        }

        private int intArg(Object[] arguments, int index) {
            if (arguments == null || index >= arguments.length || !(arguments[index] instanceof Number number)) {
                return 0;
            }
            return number.intValue();
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> mapArg(Object[] arguments, int index) {
            if (arguments == null || index >= arguments.length || !(arguments[index] instanceof Map<?, ?> source)) {
                return Map.of();
            }
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return Map.copyOf(result);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Map<String, Object> mutableMapArg(Object[] arguments, int index) {
            if (arguments == null || index >= arguments.length || !(arguments[index] instanceof Map<?, ?> source)) {
                return new LinkedHashMap<>();
            }
            try {
                Map mutable = (Map) source;
                String probe = "__echo_native_loader_mutability_probe";
                mutable.put(probe, Boolean.TRUE);
                mutable.remove(probe);
                return mutable;
            } catch (ClassCastException | UnsupportedOperationException exception) {
                return new LinkedHashMap<>(mapArg(arguments, index));
            }
        }

        private void stampLiveDispatchProof(String surface, Map<String, Object> evidence, NativeResult result) {
            if (evidence == null || result == null) {
                return;
            }
            Object liveRuntimeDispatchId = evidence.get("liveRuntimeDispatchId");
            Map<String, Object> snapshot = result.snapshot();
            boolean proofSatisfied = result.completedWithMutation()
                    && Boolean.TRUE.equals(snapshot.get("minecraftRuntimeAccessed"))
                    && Boolean.TRUE.equals(snapshot.get("runtimeSurfaceLiveProofSatisfied"));
            evidence.putAll(snapshot);
            if (liveRuntimeDispatchId != null) {
                evidence.put("liveRuntimeDispatchId", liveRuntimeDispatchId);
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", proofSatisfied);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", Boolean.TRUE.equals(snapshot.get("minecraftRuntimeAccessed")));
            evidence.put("liveRuntimeDispatchMutationSupported", Boolean.TRUE.equals(snapshot.get("liveRuntimeMutationSupported")));
            evidence.put("liveRuntimeDispatchLiveMutation", result.completedWithMutation());
            evidence.put("liveRuntimeSurface", surface);
            evidence.put("liveRuntimeDispatchSnapshot", snapshot);
        }

        private List<String> listArg(Object[] arguments, int index) {
            if (arguments == null || index >= arguments.length || !(arguments[index] instanceof List<?> source)) {
                return List.of();
            }
            return stringList(source);
        }

        private List<String> stringList(Object value) {
            if (!(value instanceof List<?> source)) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (Object item : source) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return List.copyOf(result);
        }

        private int intValue(String value, int fallback) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private Map<String, Object> payload(String firstKey, Object firstValue) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(firstKey, firstValue == null ? "" : firstValue);
            result.put("source", "native_loader_live_bridge");
            return Map.copyOf(result);
        }

        private Map<String, Object> payload(String firstKey, Object firstValue, String secondKey, Object secondValue) {
            Map<String, Object> result = new LinkedHashMap<>(payload(firstKey, firstValue));
            result.put(secondKey, secondValue == null ? "" : secondValue);
            return Map.copyOf(result);
        }

        private Map<String, Object> payload(
                String firstKey,
                Object firstValue,
                String secondKey,
                Object secondValue,
                String thirdKey,
                Object thirdValue) {
            Map<String, Object> result = new LinkedHashMap<>(payload(firstKey, firstValue, secondKey, secondValue));
            result.put(thirdKey, thirdValue == null ? "" : thirdValue);
            return Map.copyOf(result);
        }

        private String tailOrFallback(String key, String prefix) {
            String tail = key == null ? "" : key.substring(prefix.length());
            return tail.isBlank() ? "native_loader" : tail;
        }

        private String safeIdSegment(String value) {
            if (value == null || value.isBlank()) {
                return "unknown";
            }
            StringBuilder builder = new StringBuilder();
            for (char character : value.toCharArray()) {
                if (Character.isLetterOrDigit(character)
                        || character == ':'
                        || character == '.'
                        || character == '_'
                        || character == '-'
                        || character == '/') {
                    builder.append(character);
                } else {
                    builder.append('_');
                }
            }
            return builder.toString();
        }

        private String[] scopedKey(String key) {
            if (key == null || key.isBlank()) {
                return new String[]{"native_loader", "unknown"};
            }
            int separator = key.indexOf('/');
            if (separator > 0 && separator + 1 < key.length()) {
                return new String[]{key.substring(0, separator), key.substring(separator + 1)};
            }
            return new String[]{"native_loader", safeIdSegment(key)};
        }

        private NativeBlockRef blockRef(String target) {
            if (target == null || target.isBlank()) {
                return null;
            }
            int separator = target.lastIndexOf(':');
            String dimension = separator > 0 ? target.substring(0, separator) : compatibilityDelegate.dimensionId();
            String coordinates = separator > 0 ? target.substring(separator + 1) : target;
            String[] parts = coordinates.split(",");
            if (parts.length != 3) {
                return null;
            }
            try {
                return new NativeBlockRef(
                        dimension,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private String capabilityOperation(String capability) {
            if (capability == null) {
                return "";
            }
            String normalized = capability.toLowerCase(java.util.Locale.ROOT);
            if (normalized.endsWith(".receive_energy")) {
                return "receive_energy";
            }
            if (normalized.endsWith(".extract_energy")) {
                return "extract_energy";
            }
            if (normalized.endsWith(".insert_item")) {
                return "insert_item";
            }
            if (normalized.endsWith(".extract_item")) {
                return "extract_item";
            }
            return "";
        }

        private String capabilityId(String capability, String operation) {
            String suffix = "." + operation;
            if (capability != null && capability.length() > suffix.length()
                    && capability.toLowerCase(java.util.Locale.ROOT).endsWith(suffix)) {
                return capability.substring(0, capability.length() - suffix.length());
            }
            return capability == null || capability.isBlank() ? "native_loader" : capability;
        }

        private NativeItemStack itemStack(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String itemId = value;
            int count = 1;
            int separator = value.lastIndexOf('x');
            if (separator > 0 && separator + 1 < value.length()) {
                try {
                    count = Integer.parseInt(value.substring(separator + 1));
                    itemId = value.substring(0, separator);
                } catch (NumberFormatException ignored) {
                    itemId = value;
                    count = 1;
                }
            }
            if (itemId.isBlank()) {
                return null;
            }
            return new NativeItemStack(itemId, Math.max(1, count), Map.of("source", "native_loader_live_bridge"));
        }
    }

    private static void importNativeLoaderModuleServices(Class<?> registryClass, Object registry) {
        Path serviceRegistryPath = nativeLoaderServiceRegistryPath();
        if (serviceRegistryPath == null || !Files.isRegularFile(serviceRegistryPath)) {
            return;
        }
        try (var reader = Files.newBufferedReader(serviceRegistryPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("services") || !root.get("services").isJsonArray()) {
                return;
            }
            var register = registryClass.getMethod("register", String.class, String.class, Object.class, List.class, String.class);
            for (JsonElement element : root.getAsJsonArray("services")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject service = element.getAsJsonObject();
                String moduleId = jsonText(service, "moduleId");
                String serviceId = jsonText(service, "serviceId");
                String moduleServiceKey = jsonText(service, "moduleServiceKey");
                String implementationClass = jsonText(service, "implementationClass");
                String serviceInstanceClass = jsonText(service, "serviceInstanceClass");
                boolean serviceInstanceAttached = jsonBoolean(service, "serviceInstanceAttached");
                if (moduleId.isBlank() || serviceId.isBlank() || ADAPTERCORE_SERVICE_ID.equals(serviceId)) {
                    continue;
                }
                List<String> surfaces = jsonStringList(service, "surfaces");
                Object serviceObject = nativeLoaderImportedService(
                        moduleId,
                        serviceId,
                        moduleServiceKey,
                        implementationClass,
                        serviceInstanceClass,
                        serviceInstanceAttached,
                        surfaces);
                register.invoke(registry, moduleId, serviceId, serviceObject, surfaces, implementationClass);
            }
        } catch (Throwable ignored) {
            // Native Loader service import must never break the live backend; missing services remain visible in diagnostics.
        }
    }

    private static Object nativeLoaderImportedService(
            String moduleId,
            String serviceId,
            String moduleServiceKey,
            String implementationClass,
            String serviceInstanceClass,
            boolean serviceInstanceAttached,
            List<String> surfaces) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("moduleId", moduleId == null ? "" : moduleId);
        evidence.put("serviceId", serviceId == null ? "" : serviceId);
        evidence.put("moduleServiceKey", moduleServiceKey == null || moduleServiceKey.isBlank()
                ? (String.valueOf(moduleId) + "::" + String.valueOf(serviceId))
                : moduleServiceKey);
        evidence.put("implementationClass", implementationClass == null ? "" : implementationClass);
        evidence.put("declaredServiceInstanceClass", serviceInstanceClass == null ? "" : serviceInstanceClass);
        evidence.put("declaredServiceInstanceAttached", serviceInstanceAttached);
        evidence.put("surfaces", surfaces == null ? List.of() : List.copyOf(surfaces));
        evidence.put("runtimeLane", "Native Loader");
        evidence.put("importedIntoLiveRuntime", true);
        if (implementationClass != null && !implementationClass.isBlank()) {
            try {
                Class<?> type = Class.forName(implementationClass);
                var constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Throwable failure) {
                Throwable cause = failure instanceof java.lang.reflect.InvocationTargetException invocation
                        && invocation.getCause() != null
                        ? invocation.getCause()
                        : failure;
                evidence.put("liveRuntimeInstantiationFailureClass", cause.getClass().getName());
                evidence.put("liveRuntimeInstantiationFailureMessage",
                        cause.getMessage() == null ? "" : cause.getMessage());
            }
        }
        return new ImportedNativeService(
                moduleId == null ? "" : moduleId,
                serviceId == null ? "" : serviceId,
                moduleServiceKey == null || moduleServiceKey.isBlank()
                        ? (String.valueOf(moduleId) + "::" + String.valueOf(serviceId))
                        : moduleServiceKey,
                implementationClass == null ? "" : implementationClass,
                serviceInstanceClass == null ? "" : serviceInstanceClass,
                serviceInstanceAttached,
                surfaces == null ? List.of() : List.copyOf(surfaces),
                Map.copyOf(evidence));
    }

    private static Path nativeLoaderServiceRegistryPath() {
        String configured = System.getProperty("echo.native.serviceRegistryPath", "");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        String gameDir = System.getProperty("echo.native.gameDir", "");
        if (gameDir == null || gameDir.isBlank()) {
            return null;
        }
        return Path.of(gameDir).resolve("echo-native").resolve("native-service-registry.json")
                .toAbsolutePath()
                .normalize();
    }

    private static Map<String, Object> nativeLoaderLoadedModuleStateReport() {
        Path directory = nativeLoaderLoadedModulesDirectory();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("loadedModuleStatePath", directory == null ? "" : directory.toString());
        if (directory == null || !Files.isDirectory(directory)) {
            report.put("loadedModuleStateAttached", false);
            report.put("loadedModuleCount", 0);
            report.put("loadedClassCount", 0);
            report.put("registeredServiceCount", 0);
            report.put("registeredContentCount", 0);
            report.put("loadedModules", List.of());
            return Map.copyOf(report);
        }
        List<Map<String, Object>> modules = new ArrayList<>();
        TreeSet<String> loadedClasses = new TreeSet<>();
        int registeredServiceCount = 0;
        int registeredContentCount = 0;
        try (var paths = Files.list(directory)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(item -> item.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonObject state = JsonParser.parseReader(reader).getAsJsonObject();
                    Map<String, Object> module = new LinkedHashMap<>();
                    String moduleId = jsonText(state, "moduleId");
                    String loadedClassName = jsonText(state, "loadedClassName");
                    int moduleServiceCount = jsonInt(state, "registeredServiceCount");
                    int moduleContentCount = jsonInt(state, "registeredContentCount");
                    module.put("moduleId", moduleId);
                    module.put("entrypoint", jsonText(state, "entrypoint"));
                    module.put("loadedClassName", loadedClassName);
                    module.put("loadedClassLoader", jsonText(state, "loadedClassLoader"));
                    module.put("status", jsonText(state, "status"));
                    module.put("loaded", jsonBoolean(state, "loaded"));
                    module.put("resolved", jsonBoolean(state, "resolved"));
                    module.put("registered", jsonBoolean(state, "registered"));
                    module.put("registeredServiceCount", moduleServiceCount);
                    module.put("registeredContentCount", moduleContentCount);
                    module.put("lifecyclePhases", jsonStringList(state, "lifecyclePhases"));
                    module.put("lifecyclePhaseHistory", jsonJavaValue(state.get("lifecyclePhaseHistory")));
                    module.put("registeredServices", jsonJavaValue(state.get("registeredServices")));
                    module.put("registeredContent", jsonJavaValue(state.get("registeredContent")));
                    module.put("statePath", path.toAbsolutePath().normalize().toString());
                    modules.add(Map.copyOf(module));
                    if (!loadedClassName.isBlank()) {
                        loadedClasses.add(loadedClassName);
                    }
                    registeredServiceCount += moduleServiceCount;
                    registeredContentCount += moduleContentCount;
                }
            }
            report.put("loadedModuleStateAttached", !modules.isEmpty());
            report.put("loadedModuleCount", modules.size());
            report.put("loadedClassCount", loadedClasses.size());
            report.put("registeredServiceCount", registeredServiceCount);
            report.put("registeredContentCount", registeredContentCount);
            report.put("loadedClasses", List.copyOf(loadedClasses));
            report.put("loadedModules", List.copyOf(modules));
            return Map.copyOf(report);
        } catch (Throwable failure) {
            report.put("loadedModuleStateAttached", false);
            report.put("loadedModuleCount", modules.size());
            report.put("loadedClassCount", loadedClasses.size());
            report.put("registeredServiceCount", registeredServiceCount);
            report.put("registeredContentCount", registeredContentCount);
            report.put("loadedModules", List.copyOf(modules));
            report.put("failureClass", failure.getClass().getName());
            report.put("failureMessage", failure.getMessage() == null ? "" : failure.getMessage());
            return Map.copyOf(report);
        }
    }

    private static Path nativeLoaderLoadedModulesDirectory() {
        String configured = System.getProperty("echo.native.loadedModulesPath", "");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        String gameDir = System.getProperty("echo.native.gameDir", "");
        if (gameDir == null || gameDir.isBlank()) {
            return null;
        }
        return Path.of(gameDir).resolve("echo-native").resolve("loaded-modules")
                .toAbsolutePath()
                .normalize();
    }

    private static String jsonText(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static boolean jsonBoolean(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    private static int jsonInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static List<String> jsonStringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> values = new java.util.ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element != null && !element.isJsonNull()) {
                values.add(element.getAsString());
            }
        }
        return List.copyOf(values);
    }

    private static Object jsonJavaValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                values.put(entry.getKey(), jsonJavaValue(entry.getValue()));
            }
            return Map.copyOf(values);
        }
        if (element.isJsonArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                values.add(jsonJavaValue(item));
            }
            return List.copyOf(values);
        }
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                try {
                    return primitive.getAsLong();
                } catch (RuntimeException ignored) {
                    try {
                        return primitive.getAsDouble();
                    } catch (RuntimeException ignoredAgain) {
                        return primitive.getAsString();
                    }
                }
            }
            return primitive.getAsString();
        }
        return String.valueOf(element);
    }

    private static java.nio.file.Path nativeLoaderSaveDirectory() {
        String gameDir = System.getProperty("echo.native.gameDir", "");
        java.nio.file.Path root = gameDir == null || gameDir.isBlank()
                ? java.nio.file.Path.of(System.getProperty("user.dir", "."))
                : java.nio.file.Path.of(gameDir);
        return root.resolve("echo-native-loader")
                .resolve("runtime-host")
                .resolve("echoashfallprotocol");
    }

    private record ImportedNativeService(
            String moduleId,
            String serviceId,
            String moduleServiceKey,
            String implementationClass,
            String declaredServiceInstanceClass,
            boolean declaredServiceInstanceAttached,
            List<String> surfaces,
            Map<String, Object> evidence
    ) {
        private ImportedNativeService {
            surfaces = surfaces == null ? List.of() : List.copyOf(surfaces);
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    private Map<String, Object> nativeLoaderBackendRecordReport(Object nativeLoaderBackendRecord) {
        if (nativeLoaderBackendRecord == null) {
            return Map.of();
        }
        if (nativeLoaderBackendRecord instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return Map.copyOf(copy);
        }
        try {
            Object report = nativeLoaderBackendRecord.getClass().getMethod("toReport").invoke(nativeLoaderBackendRecord);
            if (report instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    copy.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return Map.copyOf(copy);
            }
        } catch (Throwable ignored) {
            // Fall through to a small status-only snapshot.
        }
        try {
            Object status = nativeLoaderBackendRecord.getClass().getMethod("status").invoke(nativeLoaderBackendRecord);
            return Map.of("status", status == null ? "" : String.valueOf(status));
        } catch (Throwable ignored) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> nativeLoaderMutationLedgerReport() {
        if (nativeLoaderBackend == null) {
            return List.of();
        }
        try {
            Object ledger = nativeLoaderBackend.getClass().getMethod("mutationLedger").invoke(nativeLoaderBackend);
            Object report = ledger == null ? null : ledger.getClass().getMethod("toReport").invoke(ledger);
            if (report instanceof List<?> list) {
                List<Map<String, Object>> records = new java.util.ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Map<String, Object> copy = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            copy.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                        records.add(Map.copyOf(copy));
                    }
                }
                return List.copyOf(records);
            }
        } catch (Throwable ignored) {
            // Missing Native Loader classes leave the mutation ledger unavailable to this host.
        }
        return List.of();
    }

    private final class NativeLoaderPlayerInventory implements PlayerInventory {
        @Override
        public NativeResult grant(NativePlayerRef player, NativeItemStack stack, NativeMutationContext context) {
            NativeResult result = operationHost().playerInventory().grant(player, stack, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.PlayerInventory",
                    "grant",
                    result,
                    nativeLoaderBackendGrantItem(player, stack, result));
        }

        @Override
        public NativeResult remove(NativePlayerRef player, String itemId, int count, NativeMutationContext context) {
            return bridgeResult("EchoNativeRuntimeHost.PlayerInventory", "remove",
                    operationHost().playerInventory().remove(player, itemId, count, context));
        }

        @Override
        public List<NativeItemStack> snapshot(NativePlayerRef player, NativeMutationContext context) {
            return operationHost().playerInventory().snapshot(player, context);
        }
    }

    private final class NativeLoaderPlayerState implements PlayerState {
        @Override
        public NativeResult teleport(NativePlayerRef player, NativePosition position, NativeMutationContext context) {
            NativeResult result = operationHost().playerState().teleport(player, position, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.PlayerState",
                    "teleport",
                    result,
                    nativeLoaderBackendUpdatePlayerState(
                            player,
                            "teleport",
                            position.dimensionId() + ":" + position.x() + "," + position.y() + "," + position.z(),
                            result));
        }

        @Override
        public NativeResult bindRespawn(
                NativePlayerRef player,
                NativePosition position,
                boolean forced,
                NativeMutationContext context) {
            NativeResult result = operationHost().playerState().bindRespawn(player, position, forced, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.PlayerState",
                    "bindRespawn",
                    result,
                    nativeLoaderBackendUpdatePlayerState(
                            player,
                            "respawn",
                            position.dimensionId() + ":" + position.x() + "," + position.y() + "," + position.z()
                                    + ":forced=" + forced,
                            result));
        }

        @Override
        public NativeResult grantAdvancement(
                NativePlayerRef player,
                String advancementId,
                String criterion,
                NativeMutationContext context) {
            NativeResult result = operationHost().playerState().grantAdvancement(player, advancementId, criterion, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.PlayerState",
                    "grantAdvancement",
                    result,
                    nativeLoaderBackendUpdatePlayerState(player, "advancement." + advancementId, criterion, result));
        }

        @Override
        public NativeResult writePersistentState(
                NativePlayerRef player,
                String key,
                Object value,
                NativeMutationContext context) {
            NativeResult result = operationHost().playerState().writePersistentState(player, key, value, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.PlayerState",
                    "writePersistentState",
                    result,
                    nativeLoaderBackendUpdatePlayerState(player, key, String.valueOf(value), result));
        }
    }

    private final class NativeLoaderWorldBlocks implements WorldBlocks {
        @Override
        public NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context) {
            NativeResult result = operationHost().worldBlocks().setBlock(block, state, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.WorldBlocks",
                    "setBlock",
                    result,
                    nativeLoaderBackendPlaceBlock(block, state.blockId(), result));
        }

        @Override
        public NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context) {
            NativeResult result = operationHost().worldBlocks().clearBlock(block, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.WorldBlocks",
                    "clearBlock",
                    result,
                    nativeLoaderBackendPlaceBlock(block, "minecraft:air", result));
        }

        @Override
        public NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context) {
            return operationHost().worldBlocks().blockState(block, context);
        }

        @Override
        public boolean isLoaded(NativeBlockRef block, NativeMutationContext context) {
            return operationHost().worldBlocks().isLoaded(block, context);
        }
    }

    private final class NativeLoaderWorldState implements WorldState {
        @Override
        public NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context) {
            NativeResult result = operationHost().worldState().writeMarker(markerId, payload, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.WorldState",
                    "writeMarker",
                    result,
                    nativeLoaderBackendUpdateWorldState(
                            context.dimensionId(),
                            "marker." + markerId,
                            payload,
                            result));
        }

        @Override
        public NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context) {
            NativeResult result = operationHost().worldState().writeWeatherState(stateId, payload, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.WorldState",
                    "writeWeatherState",
                    result,
                    nativeLoaderBackendUpdateWorldState(
                            context.dimensionId(),
                            "weather." + stateId,
                            payload,
                            result));
        }

        @Override
        public NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context) {
            NativeResult result = operationHost().worldState().writeRouteState(routeId, payload, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.WorldState",
                    "writeRouteState",
                    result,
                    nativeLoaderBackendUpdateWorldState(
                            context.dimensionId(),
                            "route." + routeId,
                            payload,
                            result));
        }
    }

    private final class NativeLoaderStructures implements Structures {
        @Override
        public NativeResult placeStructure(NativeStructurePlacement placement, NativeMutationContext context) {
            NativeResult result = operationHost().structures().placeStructure(placement, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Structures",
                    "placeStructure",
                    result,
                    nativeLoaderBackendPlaceStructure(placement, result));
        }
    }

    private final class NativeLoaderBlockEntities implements BlockEntities {
        @Override
        public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
            NativeResult result = operationHost().blockEntities().tick(block, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.BlockEntities",
                    "tick",
                    result,
                    nativeLoaderBackendUpdateBlockEntity(block, "tick", context.gameTime(), result));
        }

        @Override
        public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
            return operationHost().blockEntities().snapshot(block, context);
        }

        @Override
        public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
            NativeResult result = operationHost().blockEntities().applySnapshot(snapshot, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.BlockEntities",
                    "applySnapshot",
                    result,
                    nativeLoaderBackendUpdateBlockEntity(
                            snapshot.block(),
                            snapshot.blockEntityId(),
                            snapshot.state(),
                            result));
        }
    }

    private final class NativeLoaderCapabilities implements Capabilities {
        @Override
        public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
            NativeResult result = operationHost().capabilities().insertItem(request, stack, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Capabilities",
                    "insertItem",
                    result,
                    nativeLoaderBackendUpdateCapability(
                            request,
                            "insert_item",
                            stack.itemId() + "x" + stack.count(),
                            result));
        }

        @Override
        public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
            NativeResult result = operationHost().capabilities().extractItem(request, itemId, count, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Capabilities",
                    "extractItem",
                    result,
                    nativeLoaderBackendUpdateCapability(
                            request,
                            "extract_item",
                            itemId + "x" + count,
                            result));
        }

        @Override
        public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            NativeResult result = operationHost().capabilities().receiveEnergy(request, amount, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Capabilities",
                    "receiveEnergy",
                    result,
                    nativeLoaderBackendUpdateCapability(request, "receive_energy", amount, result));
        }

        @Override
        public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            NativeResult result = operationHost().capabilities().extractEnergy(request, amount, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Capabilities",
                    "extractEnergy",
                    result,
                    nativeLoaderBackendUpdateCapability(request, "extract_energy", amount, result));
        }

        @Override
        public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
            return operationHost().capabilities().readCapability(request, context);
        }
    }

    private final class NativeLoaderEvents implements Events {
        @Override
        public NativeResult publish(NativeEvent event, NativeMutationContext context) {
            NativeResult result = operationHost().events().publish(event, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Events",
                    "publish",
                    result,
                    nativeLoaderBackendEmitEvent(event, result));
        }
    }

    private final class NativeLoaderPackets implements Packets {
        @Override
        public NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context) {
            NativeResult result = operationHost().packets().sendToPlayer(packet, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Packets",
                    "sendToPlayer",
                    result,
                    nativeLoaderBackendSendPacket(packet, "player", result));
        }

        @Override
        public NativeResult broadcast(NativePacket packet, NativeMutationContext context) {
            NativeResult result = operationHost().packets().broadcast(packet, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Packets",
                    "broadcast",
                    result,
                    nativeLoaderBackendSendPacket(packet, "broadcast", result));
        }
    }

    private final class NativeLoaderHud implements Hud {
        @Override
        public NativeResult publishNotification(
                NativePlayerRef player,
                Map<String, Object> payload,
                NativeMutationContext context) {
            NativeResult result = operationHost().hud().publishNotification(player, payload, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.Hud",
                    "publishNotification",
                    result,
                    nativeLoaderBackendEmitHud(player, payload, result));
        }
    }

    private final class NativeLoaderSaveData implements SaveData {
        @Override
        public NativeResult write(NativeSaveData data, NativeMutationContext context) {
            NativeResult result = operationHost().saveData().write(data, context);
            return bridgeResult(
                    "EchoNativeRuntimeHost.SaveData",
                    "write",
                    result,
                    nativeLoaderBackendWriteSaveData(data, result));
        }

        @Override
        public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
            return operationHost().saveData().read(scope, key, context);
        }

        @Override
        public NativeResult delete(String scope, String key, NativeMutationContext context) {
            return bridgeResult("EchoNativeRuntimeHost.SaveData", "delete",
                    operationHost().saveData().delete(scope, key, context));
        }
    }
}

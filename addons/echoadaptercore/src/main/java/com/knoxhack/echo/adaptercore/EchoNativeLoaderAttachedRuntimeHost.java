package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePacket;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeStructurePlacement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AdapterCore host facade for an attached Native Loader backend.
 *
 * <p>The Native Loader is the primary loader lane, while AdapterCore remains the
 * shared gameplay contract. This class keeps that boundary honest: AdapterCore
 * methods delegate into the Native Loader backend object when it is present in
 * the same runtime, and mutation claims are copied from the Native Loader
 * mutation ledger status instead of being invented locally.</p>
 */
public final class EchoNativeLoaderAttachedRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String DEFAULT_RUNTIME_HOST_ID = "echo_native_loader:adaptercore_runtime_host";

    private final Object nativeLoaderBackend;

    public EchoNativeLoaderAttachedRuntimeHost(Object nativeLoaderBackend) {
        this(DEFAULT_RUNTIME_HOST_ID, nativeLoaderBackend);
    }

    public EchoNativeLoaderAttachedRuntimeHost(String runtimeHostId, Object nativeLoaderBackend) {
        super(runtimeHostId);
        if (nativeLoaderBackend == null) {
            throw new IllegalArgumentException("native loader backend must not be null");
        }
        this.nativeLoaderBackend = nativeLoaderBackend;
    }

    public Object nativeLoaderBackend() {
        return nativeLoaderBackend;
    }

    public Map<String, Object> nativeLoaderSnapshot() {
        Object backendSnapshot = invokeNoArg(nativeLoaderBackend, "snapshot");
        if (backendSnapshot instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        Object host = invokeNoArg(nativeLoaderBackend, "host");
        if (host == null) {
            return Map.of();
        }
        Object snapshot = invokeNoArg(host, "snapshot");
        if (snapshot instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        return Map.of();
    }

    public List<Map<String, Object>> nativeLoaderMutationLedger() {
        Object ledger = invokeNoArg(nativeLoaderBackend, "mutationLedger");
        if (ledger == null) {
            return List.of();
        }
        Object report = invokeNoArg(ledger, "toReport");
        if (report instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(EchoNativeLoaderAttachedRuntimeHost::copyMap)
                    .toList();
        }
        return List.of();
    }

    public static EchoRuntimeHostCapabilities capabilities(String runtimeHostId) {
        return new EchoRuntimeHostCapabilities(
                runtimeHostId,
                Set.of(
                        "EchoNativeRuntimeHost.PlayerInventory",
                        "EchoNativeRuntimeHost.PlayerState",
                        "EchoNativeRuntimeHost.WorldBlocks",
                        "EchoNativeRuntimeHost.WorldState",
                        "EchoNativeRuntimeHost.Structures",
                        "EchoNativeRuntimeHost.BlockEntities",
                        "EchoNativeRuntimeHost.Capabilities",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Packets",
                        "EchoNativeRuntimeHost.Hud",
                        "EchoNativeRuntimeHost.SaveData",
                        "EchoNativeRuntimeHost.RuntimeSurfaces"
                ),
                Set.of(
                        "ashfall.native_loader.first_spawn",
                        "adaptercore.native_loader.grant_item",
                        "adaptercore.native_loader.remove_item",
                        "adaptercore.native_loader.place_block",
                        "adaptercore.native_loader.write_save_data",
                        "adaptercore.native_loader.delete_save_data",
                        "adaptercore.native_loader.emit_hud",
                        "adaptercore.native_loader.client_tick",
                        "adaptercore.native_loader.render_layer",
                        "adaptercore.native_loader.screen_event",
                        "adaptercore.native_loader.keybind",
                        "adaptercore.native_loader.register_command",
                        "adaptercore.native_loader.register_network_packet",
                        "adaptercore.native_loader.config_reload",
                        "adaptercore.native_loader.resource_reload",
                        "adaptercore.native_loader.save_hook",
                        "adaptercore.native_loader.lifecycle_phase",
                        "adaptercore.native_loader.runtime_event",
                        "adaptercore.native_loader.server_client_sync"
                ),
                Set.of(
                        "echoashfallprotocol:drop_pod_beacon",
                        "echoashfallprotocol:drop_pod_marker",
                        "echoashfallprotocol:starter_crash_zone"
                ),
                true,
                true,
                true);
    }

    public static EchoRuntimeHostRegistry.RegisteredRuntimeHost register(
            EchoRuntimeHostRegistry registry,
            String runtimeHostId,
            Object nativeLoaderBackend) {
        EchoRuntimeHostRegistry targetRegistry = registry == null ? EchoRuntimeHostRegistry.global() : registry;
        EchoNativeLoaderAttachedRuntimeHost host = new EchoNativeLoaderAttachedRuntimeHost(runtimeHostId, nativeLoaderBackend);
        return targetRegistry.register(host, capabilities(runtimeHostId));
    }

    @Override
    public PlayerInventory playerInventory() {
        return new PlayerInventory() {
            @Override
            public NativeResult grant(NativePlayerRef player, NativeItemStack stack, NativeMutationContext context) {
                return backendCall(
                        "grantItem",
                        new Class<?>[]{String.class, String.class, int.class},
                        new Object[]{player.playerId(), stack.itemId(), stack.count()});
            }

            @Override
            public NativeResult remove(NativePlayerRef player, String itemId, int count, NativeMutationContext context) {
                return backendCall(
                        "removeItem",
                        new Class<?>[]{String.class, String.class, int.class},
                        new Object[]{player.playerId(), itemId, count});
            }

            @Override
            public List<NativeItemStack> snapshot(NativePlayerRef player, NativeMutationContext context) {
                Object inventory = nativeLoaderSnapshot().get("inventory");
                if (!(inventory instanceof Map<?, ?> map)) {
                    return List.of();
                }
                String prefix = player.playerId() + ":";
                return map.entrySet().stream()
                        .filter(entry -> String.valueOf(entry.getKey()).startsWith(prefix))
                        .map(entry -> new NativeItemStack(
                                String.valueOf(entry.getKey()).substring(prefix.length()),
                                count(entry.getValue()),
                                Map.of()))
                        .toList();
            }
        };
    }

    @Override
    public PlayerState playerState() {
        return new PlayerState() {
            @Override
            public NativeResult teleport(NativePlayerRef player, NativePosition position, NativeMutationContext context) {
                return backendCall(
                        "updatePlayerState",
                        new Class<?>[]{String.class, String.class, String.class},
                        new Object[]{player.playerId(), "teleport", position.dimensionId() + ":" + position.x() + "," + position.y() + "," + position.z()});
            }

            @Override
            public NativeResult bindRespawn(
                    NativePlayerRef player,
                    NativePosition position,
                    boolean forced,
                    NativeMutationContext context) {
                return backendCall(
                        "updatePlayerState",
                        new Class<?>[]{String.class, String.class, String.class},
                        new Object[]{player.playerId(), "respawn", position.dimensionId() + ":" + position.x() + "," + position.y() + "," + position.z() + ":forced=" + forced});
            }

            @Override
            public NativeResult grantAdvancement(
                    NativePlayerRef player,
                    String advancementId,
                    String criterion,
                    NativeMutationContext context) {
                return backendCall(
                        "updatePlayerState",
                        new Class<?>[]{String.class, String.class, String.class},
                        new Object[]{player.playerId(), "advancement." + advancementId, criterion});
            }

            @Override
            public NativeResult writePersistentState(
                    NativePlayerRef player,
                    String key,
                    Object value,
                    NativeMutationContext context) {
                return backendCall(
                        "updatePlayerState",
                        new Class<?>[]{String.class, String.class, String.class},
                        new Object[]{player.playerId(), key, String.valueOf(value)});
            }
        };
    }

    @Override
    public WorldBlocks worldBlocks() {
        return new WorldBlocks() {
            @Override
            public NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context) {
                return backendCall(
                        "placeBlock",
                        new Class<?>[]{String.class, int.class, int.class, int.class, String.class},
                        new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), state.blockId()});
            }

            @Override
            public NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context) {
                return backendCall(
                        "placeBlock",
                        new Class<?>[]{String.class, int.class, int.class, int.class, String.class},
                        new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), "minecraft:air"});
            }

            @Override
            public NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context) {
                Object blocks = nativeLoaderSnapshot().get("worldBlocks");
                String key = block.dimensionId() + ":" + block.x() + "," + block.y() + "," + block.z();
                if (blocks instanceof Map<?, ?> map && map.containsKey(key)) {
                    return new NativeBlockState(String.valueOf(map.get(key)), Map.of("source", "native_loader_runtime_host"));
                }
                return new NativeBlockState("minecraft:air", Map.of());
            }

            @Override
            public boolean isLoaded(NativeBlockRef block, NativeMutationContext context) {
                return block != null && !block.dimensionId().isBlank();
            }
        };
    }

    @Override
    public WorldState worldState() {
        return new WorldState() {
            @Override
            public NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context) {
                return writeWorldState(context.dimensionId(), "marker." + markerId, payload);
            }

            @Override
            public NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context) {
                return writeWorldState(context.dimensionId(), "weather." + stateId, payload);
            }

            @Override
            public NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context) {
                return writeWorldState(context.dimensionId(), "route." + routeId, payload);
            }
        };
    }

    @Override
    public Structures structures() {
        return (placement, context) -> backendCall(
                "placeStructure",
                new Class<?>[]{String.class, String.class, int.class, int.class, int.class},
                new Object[]{placement.dimensionId(), placement.structureId(), placement.originX(), placement.originY(), placement.originZ()});
    }

    @Override
    public BlockEntities blockEntities() {
        return new BlockEntities() {
            @Override
            public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
                return backendCall(
                        "updateBlockEntity",
                        new Class<?>[]{String.class, int.class, int.class, int.class, String.class, String.class},
                        new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), "tick", String.valueOf(context.gameTime())});
            }

            @Override
            public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
                Object blockEntities = nativeLoaderSnapshot().get("blockEntities");
                Map<String, Object> state = blockEntities instanceof Map<?, ?> map ? copyMap(map) : Map.of();
                return new NativeBlockEntitySnapshot("native_loader:block_entity", block, state);
            }

            @Override
            public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
                NativeBlockRef block = snapshot.block();
                return backendCall(
                        "updateBlockEntity",
                        new Class<?>[]{String.class, int.class, int.class, int.class, String.class, String.class},
                        new Object[]{block.dimensionId(), block.x(), block.y(), block.z(), snapshot.blockEntityId(), String.valueOf(snapshot.state())});
            }
        };
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities() {
            @Override
            public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
                return capabilityUpdate(request, "insert_item", stack.itemId() + "x" + stack.count());
            }

            @Override
            public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
                return capabilityUpdate(request, "extract_item", itemId + "x" + count);
            }

            @Override
            public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                return capabilityUpdate(request, "receive_energy", String.valueOf(amount));
            }

            @Override
            public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                return capabilityUpdate(request, "extract_energy", String.valueOf(amount));
            }

            @Override
            public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
                Object capabilities = nativeLoaderSnapshot().get("capabilities");
                if (capabilities instanceof Map<?, ?> map) {
                    return copyMap(map);
                }
                return Map.of();
            }
        };
    }

    @Override
    public Events events() {
        return (event, context) -> backendCall(
                "emitEvent",
                new Class<?>[]{String.class, String.class},
                new Object[]{event.eventId(), String.valueOf(event.payload())});
    }

    @Override
    public Packets packets() {
        return new Packets() {
            @Override
            public NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context) {
                return backendCall(
                        "sendPacketHud",
                        new Class<?>[]{String.class, String.class},
                        new Object[]{packet.channel().isBlank() ? packet.packetId() : packet.channel(), String.valueOf(packet.payload())});
            }

            @Override
            public NativeResult broadcast(NativePacket packet, NativeMutationContext context) {
                return backendCall(
                        "sendPacketHud",
                        new Class<?>[]{String.class, String.class},
                        new Object[]{"broadcast:" + packet.channel(), String.valueOf(packet.payload())});
            }
        };
    }

    @Override
    public Hud hud() {
        return (player, payload, context) -> backendCall(
                "emitHud",
                new Class<?>[]{String.class, String.class},
                new Object[]{player.playerId(), String.valueOf(payload)});
    }

    @Override
    public SaveData saveData() {
        return new SaveData() {
            @Override
            public NativeResult write(NativeSaveData data, NativeMutationContext context) {
                return backendCall(
                        "writeSaveData",
                        new Class<?>[]{String.class, String.class},
                        new Object[]{data.scope() + "/" + data.key(), String.valueOf(data.payload())});
            }

            @Override
            public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
                Object saveData = nativeLoaderSnapshot().get("saveData");
                String saveKey = scope + "/" + key;
                if (saveData instanceof Map<?, ?> map && map.containsKey(saveKey)) {
                    return Map.of("scope", scope, "key", key, "value", String.valueOf(map.get(saveKey)));
                }
                return Map.of();
            }

            @Override
            public NativeResult delete(String scope, String key, NativeMutationContext context) {
                return backendCall(
                        "deleteSaveData",
                        new Class<?>[]{String.class},
                        new Object[]{scope + "/" + key});
            }
        };
    }

    @Override
    public RuntimeSurfaces runtimeSurfaces() {
        return new RuntimeSurfaces() {
            @Override
            public NativeResult clientTick(String phase, Map<String, Object> payload, NativeMutationContext context) {
                return backendCall(
                        "clientTick",
                        new Class<?>[]{String.class, Map.class},
                        new Object[]{phase, payload == null ? Map.of() : Map.copyOf(payload)});
            }

            @Override
            public NativeResult renderLayer(String layerId, Map<String, Object> payload, NativeMutationContext context) {
                return backendCall(
                        "renderLayer",
                        new Class<?>[]{String.class, Map.class},
                        new Object[]{layerId, payload == null ? Map.of() : Map.copyOf(payload)});
            }

            @Override
            public NativeResult screenEvent(String screenId, String eventType, Map<String, Object> payload, NativeMutationContext context) {
                return backendCall(
                        "screenEvent",
                        new Class<?>[]{String.class, String.class, Map.class},
                        new Object[]{screenId, eventType, payload == null ? Map.of() : Map.copyOf(payload)});
            }

            @Override
            public NativeResult keybind(String keybindId, String action, Map<String, Object> payload, NativeMutationContext context) {
                return backendCall(
                        "keybind",
                        new Class<?>[]{String.class, String.class, Map.class},
                        new Object[]{keybindId, action, payload == null ? Map.of() : Map.copyOf(payload)});
            }

            @Override
            public NativeResult registerCommand(
                    String moduleId,
                    String commandId,
                    String targetSurface,
                    String targetBridge,
                    Map<String, Object> evidence,
                    NativeMutationContext context) {
                return backendCall(
                        "registerCommand",
                        new Class<?>[]{String.class, String.class, String.class, String.class, Map.class},
                        new Object[]{
                                moduleId,
                                commandId,
                                targetSurface,
                                targetBridge,
                                evidence == null ? Map.of() : Map.copyOf(evidence)});
            }

            @Override
            public NativeResult registerNetworkPacket(
                    String moduleId,
                    String packetId,
                    String surface,
                    String sourceRuntimeTarget,
                    List<String> consumers,
                    Map<String, Object> evidence,
                    NativeMutationContext context) {
                return backendCall(
                        "registerNetworkPacket",
                        new Class<?>[]{String.class, String.class, String.class, String.class, List.class, Map.class},
                        new Object[]{
                                moduleId,
                                packetId,
                                surface,
                                sourceRuntimeTarget,
                                consumers == null ? List.of() : List.copyOf(consumers),
                                evidence == null ? Map.of() : Map.copyOf(evidence)});
            }

            @Override
            public NativeResult reloadConfig(
                    String moduleId,
                    String configId,
                    String scope,
                    Map<String, Object> evidence,
                    NativeMutationContext context) {
                return backendCall(
                        "reloadConfig",
                        new Class<?>[]{String.class, String.class, String.class, Map.class},
                        new Object[]{moduleId, configId, scope, evidence == null ? Map.of() : Map.copyOf(evidence)});
            }

            @Override
            public NativeResult reloadResources(
                    String moduleId,
                    String resourceId,
                    String scope,
                    Map<String, Object> evidence,
                    NativeMutationContext context) {
                return backendCall(
                        "reloadResources",
                        new Class<?>[]{String.class, String.class, String.class, Map.class},
                        new Object[]{moduleId, resourceId, scope, evidence == null ? Map.of() : Map.copyOf(evidence)});
            }

            @Override
            public NativeResult saveHook(String hookId, Map<String, Object> payload, NativeMutationContext context) {
                return backendCall(
                        "saveHook",
                        new Class<?>[]{String.class, Map.class},
                        new Object[]{hookId, payload == null ? Map.of() : Map.copyOf(payload)});
            }

            @Override
            public NativeResult lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence, NativeMutationContext context) {
                return backendCall(
                        "lifecyclePhase",
                        new Class<?>[]{String.class, String.class, Map.class},
                        new Object[]{moduleId, phaseId, evidence == null ? Map.of() : Map.copyOf(evidence)});
            }

            @Override
            public NativeResult publishRuntimeEvent(
                    String sourceModule,
                    String eventId,
                    Map<String, Object> payload,
                    String status,
                    NativeMutationContext context) {
                return backendCall(
                        "publishRuntimeEvent",
                        new Class<?>[]{String.class, String.class, Map.class, String.class},
                        new Object[]{sourceModule, eventId, payload == null ? Map.of() : Map.copyOf(payload), status});
            }

            @Override
            public NativeResult syncServerClient(String channel, String payload, NativeMutationContext context) {
                return backendCall(
                        "syncServerClient",
                        new Class<?>[]{String.class, String.class},
                        new Object[]{channel, payload == null ? "" : payload});
            }
        };
    }

    private NativeResult writeWorldState(String dimensionId, String key, Map<String, Object> payload) {
        return backendCall(
                "updateWorldState",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId, key, String.valueOf(payload)});
    }

    private NativeResult capabilityUpdate(NativeCapabilityRequest request, String operation, String value) {
        NativeBlockRef block = request.block();
        String target = block.dimensionId() + ":" + block.x() + "," + block.y() + "," + block.z();
        return backendCall(
                "updateCapability",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{target, request.capabilityId() + "." + operation, value});
    }

    private NativeResult backendCall(String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = nativeLoaderBackend.getClass().getMethod(methodName, parameterTypes);
            Object record = method.invoke(nativeLoaderBackend, args);
            return resultFromNativeLoaderRecord(record, methodName);
        } catch (NoSuchMethodException exception) {
            return NativeResult.unsupported("Native Loader backend method is not available.", Map.of(
                    "backendClass", nativeLoaderBackend.getClass().getName(),
                    "method", methodName,
                    "failureReason", exception.getClass().getName() + ": " + exception.getMessage()));
        } catch (IllegalAccessException exception) {
            return NativeResult.failed("Native Loader backend method is not accessible.", Map.of(
                    "backendClass", nativeLoaderBackend.getClass().getName(),
                    "method", methodName,
                    "failureReason", exception.getClass().getName() + ": " + exception.getMessage()));
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            return NativeResult.failed("Native Loader backend method failed.", Map.of(
                    "backendClass", nativeLoaderBackend.getClass().getName(),
                    "method", methodName,
                    "failureReason", target.getClass().getName() + ": " + target.getMessage()));
        }
    }

    private NativeResult resultFromNativeLoaderRecord(Object record, String methodName) {
        if (record == null) {
            return NativeResult.failed("Native Loader backend returned no mutation record.", Map.of(
                    "method", methodName,
                    "failureReason", "missing native loader mutation record"));
        }
        Map<String, Object> report = recordReport(record);
        String status = String.valueOf(report.getOrDefault("status", ""));
        Map<String, Object> snapshot = new LinkedHashMap<>(report);
        snapshot.put("nativeLoaderBackendClass", nativeLoaderBackend.getClass().getName());
        snapshot.put("adapterCoreRuntimeHostClass", getClass().getName());
        snapshot.put("adapterCoreEnteredNativeLoaderBackend", true);
        snapshot.put("runtimeHostId", runtimeHostId());
        return switch (status) {
            case "MUTATED" -> adapterCoreLiveProofSatisfied(methodName, snapshot)
                    ? NativeResult.mutated("Native Loader backend mutated live state.", snapshot)
                    : NativeResult.failed("Native Loader backend mutation did not include release-grade live runtime proof.",
                            proofFailureSnapshot(methodName, snapshot));
            case "FAILED" -> NativeResult.failed("Native Loader backend attempted mutation and failed.", snapshot);
            case "UNSUPPORTED" -> NativeResult.unsupported("Native Loader backend does not support this surface.", snapshot);
            default -> NativeResult.noop("Native Loader backend did not mutate state.", snapshot);
        };
    }

    private static Map<String, Object> proofFailureSnapshot(String methodName, Map<String, Object> snapshot) {
        Map<String, Object> failure = new LinkedHashMap<>(snapshot);
        failure.put("adapterCoreRejectedMirrorOnlyMutation", true);
        failure.put("adapterCoreRequiredLiveProofMethod", methodName == null ? "" : methodName);
        failure.put("failureReason", "missing AdapterCore live Minecraft proof for Native Loader backend mutation");
        return Map.copyOf(failure);
    }

    private static boolean adapterCoreLiveProofSatisfied(String methodName, Map<String, Object> snapshot) {
        if (!boolDeep(snapshot, "liveRuntimeAccessed")
                || !boolDeep(snapshot, "minecraftRuntimeAccessed")
                || !boolDeep(snapshot, "liveRuntimeMutationSupported")
                || !boolDeep(snapshot, "liveRuntimeReleaseProofSatisfied")
                || !boolDeep(snapshot, "liveRuntimeSurfaceMutationSatisfied")
                || boolDeep(snapshot, "mirrorOnlyReleaseProof")) {
            return false;
        }
        String proofField = requiredProofField(methodName);
        return (proofField.isBlank() || boolDeep(snapshot, proofField))
                && saveDataMutationProofSatisfied(methodName, snapshot);
    }

    private static boolean saveDataMutationProofSatisfied(String methodName, Map<String, Object> snapshot) {
        if (!"writeSaveData".equals(methodName) && !"deleteSaveData".equals(methodName)) {
            return true;
        }
        return boolDeep(snapshot, "liveSaveDataFileTouched")
                && (boolDeep(snapshot, "runtimeSaveDataMutated")
                || boolDeep(snapshot, "runtimeSurfaceSaveMutated"));
    }

    private static String requiredProofField(String methodName) {
        return switch (methodName == null ? "" : methodName) {
            case "grantItem", "removeItem" -> "runtimeInventoryMutated";
            case "updatePlayerState" -> "runtimePlayerStateMutated";
            case "placeBlock" -> "runtimeWorldBlockMutated";
            case "updateWorldState" -> "runtimeWorldStateMutated";
            case "placeStructure" -> "runtimeStructureMutated";
            case "updateBlockEntity" -> "runtimeBlockEntityMutated";
            case "updateCapability" -> "runtimeCapabilityMutated";
            case "emitEvent", "publishRuntimeEvent" -> "runtimeEventMutated";
            case "sendPacketHud" -> "runtimePacketMutated";
            case "writeSaveData", "deleteSaveData" -> "runtimeSaveDataTouched";
            case "emitHud" -> "runtimeHudNotificationMutated";
            case "clientTick" -> "runtimeSurfaceSaveMutated";
            case "renderLayer" -> "runtimeSurfaceSaveMutated";
            case "screenEvent" -> "runtimeSurfaceSaveMutated";
            case "keybind" -> "runtimeSurfaceSaveMutated";
            case "registerCommand" -> "runtimeCommandRegistryMutated";
            case "registerNetworkPacket" -> "runtimeNetworkChannelMutated";
            case "reloadConfig" -> "runtimeConfigReloadMutated";
            case "reloadResources" -> "runtimeResourceReloadMutated";
            case "saveHook" -> "runtimeSaveHookMutated";
            case "lifecyclePhase" -> "runtimeLifecyclePhaseMutated";
            case "syncServerClient" -> "runtimeServerClientSyncMutated";
            default -> "";
        };
    }

    private static boolean boolDeep(Object value, String key) {
        if (key == null || key.isBlank() || value == null) {
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey(key) && bool(map.get(key))) {
                return true;
            }
            for (Object nested : map.values()) {
                if (boolDeep(nested, key)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                if (boolDeep(nested, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Object> recordReport(Object record) {
        Object report = invokeNoArg(record, "toReport");
        if (report instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        Object status = invokeNoArg(record, "status");
        return Map.of("status", status == null ? "FAILED" : String.valueOf(status));
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(copy);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int count(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}

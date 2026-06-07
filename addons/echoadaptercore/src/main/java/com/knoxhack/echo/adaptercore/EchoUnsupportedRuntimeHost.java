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

import java.util.List;
import java.util.Map;

public class EchoUnsupportedRuntimeHost implements EchoNativeRuntimeHost {
    private final String runtimeHostId;

    public EchoUnsupportedRuntimeHost(String runtimeHostId) {
        this.runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
    }

    public final String runtimeHostId() {
        return runtimeHostId;
    }

    protected final NativeResult unsupported(String nativeInterface, String nativeMethod, Map<String, Object> details) {
        return NativeResult.unsupported("Runtime host does not implement this AdapterCore method.", Map.of(
                "runtimeHostId", runtimeHostId,
                "nativeInterface", AdapterContractGuards.optionalText(nativeInterface),
                "nativeMethod", AdapterContractGuards.optionalText(nativeMethod),
                "failureReason", "unsupported runtime host method",
                "details", details == null ? Map.of() : details));
    }

    @Override
    public PlayerInventory playerInventory() {
        return new PlayerInventory() {
            @Override
            public NativeResult grant(NativePlayerRef player, NativeItemStack stack, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerInventory", "grant", Map.of());
            }

            @Override
            public NativeResult remove(NativePlayerRef player, String itemId, int count, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerInventory", "remove", Map.of());
            }

            @Override
            public List<NativeItemStack> snapshot(NativePlayerRef player, NativeMutationContext context) {
                return List.of();
            }
        };
    }

    @Override
    public PlayerState playerState() {
        return new PlayerState() {
            @Override
            public NativeResult teleport(NativePlayerRef player, NativePosition position, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerState", "teleport", Map.of());
            }

            @Override
            public NativeResult bindRespawn(
                    NativePlayerRef player,
                    NativePosition position,
                    boolean forced,
                    NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerState", "bindRespawn", Map.of());
            }

            @Override
            public NativeResult grantAdvancement(
                    NativePlayerRef player,
                    String advancementId,
                    String criterion,
                    NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerState", "grantAdvancement", Map.of());
            }

            @Override
            public NativeResult writePersistentState(
                    NativePlayerRef player,
                    String key,
                    Object value,
                    NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.PlayerState", "writePersistentState", Map.of());
            }
        };
    }

    @Override
    public WorldBlocks worldBlocks() {
        return new WorldBlocks() {
            @Override
            public NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.WorldBlocks", "setBlock", Map.of());
            }

            @Override
            public NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.WorldBlocks", "clearBlock", Map.of());
            }

            @Override
            public NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context) {
                return new NativeBlockState("minecraft:air", Map.of("status", "UNSUPPORTED"));
            }

            @Override
            public boolean isLoaded(NativeBlockRef block, NativeMutationContext context) {
                return false;
            }
        };
    }

    @Override
    public WorldState worldState() {
        return new WorldState() {
            @Override
            public NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.WorldState", "writeMarker", Map.of());
            }

            @Override
            public NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.WorldState", "writeWeatherState", Map.of());
            }

            @Override
            public NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.WorldState", "writeRouteState", Map.of());
            }
        };
    }

    @Override
    public Structures structures() {
        return new Structures() {
            @Override
            public NativeResult placeStructure(NativeStructurePlacement placement, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Structures", "placeStructure", Map.of());
            }
        };
    }

    @Override
    public BlockEntities blockEntities() {
        return new BlockEntities() {
            @Override
            public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.BlockEntities", "tick", Map.of());
            }

            @Override
            public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
                return new NativeBlockEntitySnapshot("adaptercore:unsupported", block, Map.of("status", "UNSUPPORTED"));
            }

            @Override
            public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.BlockEntities", "applySnapshot", Map.of());
            }
        };
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities() {
            @Override
            public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Capabilities", "insertItem", Map.of());
            }

            @Override
            public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Capabilities", "extractItem", Map.of());
            }

            @Override
            public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Capabilities", "receiveEnergy", Map.of());
            }

            @Override
            public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Capabilities", "extractEnergy", Map.of());
            }

            @Override
            public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
                return Map.of("status", "UNSUPPORTED", "runtimeHostId", runtimeHostId);
            }
        };
    }

    @Override
    public Events events() {
        return new Events() {
            @Override
            public NativeResult publish(NativeEvent event, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Events", "publish", Map.of(
                        "eventId", event == null ? "" : event.eventId()));
            }
        };
    }

    @Override
    public Packets packets() {
        return new Packets() {
            @Override
            public NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Packets", "sendToPlayer", Map.of());
            }

            @Override
            public NativeResult broadcast(NativePacket packet, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Packets", "broadcast", Map.of());
            }
        };
    }

    @Override
    public Hud hud() {
        return new Hud() {
            @Override
            public NativeResult publishNotification(
                    NativePlayerRef player,
                    Map<String, Object> payload,
                    NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.Hud", "publishNotification", Map.of());
            }
        };
    }

    @Override
    public SaveData saveData() {
        return new SaveData() {
            @Override
            public NativeResult write(NativeSaveData data, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.SaveData", "write", Map.of(
                        "scope", data == null ? "" : data.scope(),
                        "key", data == null ? "" : data.key()));
            }

            @Override
            public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
                return Map.of("status", "UNSUPPORTED", "scope", scope == null ? "" : scope, "key", key == null ? "" : key);
            }

            @Override
            public NativeResult delete(String scope, String key, NativeMutationContext context) {
                return unsupported("EchoNativeRuntimeHost.SaveData", "delete", Map.of(
                        "scope", scope == null ? "" : scope,
                        "key", key == null ? "" : key));
            }
        };
    }
}

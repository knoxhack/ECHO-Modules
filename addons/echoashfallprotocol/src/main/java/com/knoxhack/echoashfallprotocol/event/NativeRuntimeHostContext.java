package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class NativeRuntimeHostContext implements MinecraftRuntimeHostContext {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final String moduleId;
    private final String runtimeHostId;
    private final String dimensionId;
    private final NativePlayerRef playerRef;
    private final NativeRuntimeMutationLedgerSink ledgerSink;

    public NativeRuntimeHostContext(
            ServerPlayer player,
            ServerLevel level,
            String moduleId,
            NativeRuntimeMutationLedgerSink ledgerSink) {
        this(player, level, moduleId, NativeEchoRuntimeHost.RUNTIME_HOST_ID, ledgerSink);
    }

    public NativeRuntimeHostContext(
            ServerPlayer player,
            ServerLevel level,
            String moduleId,
            String runtimeHostId,
            NativeRuntimeMutationLedgerSink ledgerSink) {
        if (player == null) {
            throw new IllegalArgumentException("Native runtime host player must not be null.");
        }
        if (level == null) {
            throw new IllegalArgumentException("Native runtime host level must not be null.");
        }
        this.player = player;
        this.level = level;
        this.moduleId = moduleId == null || moduleId.isBlank() ? EchoAshfallProtocol.MODID : moduleId;
        this.runtimeHostId = runtimeHostId == null || runtimeHostId.isBlank()
                ? NativeEchoRuntimeHost.RUNTIME_HOST_ID
                : runtimeHostId;
        this.dimensionId = level.dimension().identifier().toString();
        this.playerRef = new NativePlayerRef(player.getUUID().toString());
        this.ledgerSink = ledgerSink == null ? NativeRuntimeMutationLedgerSink.noop() : ledgerSink;
    }

    @Override
    public ServerPlayer player() {
        return player;
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    @Override
    public String runtimeHostId() {
        return runtimeHostId;
    }

    @Override
    public String dimensionId() {
        return dimensionId;
    }

    @Override
    public NativePlayerRef playerRef() {
        return playerRef;
    }

    @Override
    public NativeRuntimeMutationLedgerSink ledgerSink() {
        return ledgerSink;
    }

    @Override
    public NativeMutationContext context(String idempotencyKey, String nativeInterface, String nativeMethod) {
        boolean nativeHost = NativeEchoRuntimeHost.RUNTIME_HOST_ID.equals(runtimeHostId);
        boolean nativeMinecraftHost = NativeMinecraftEchoRuntimeHost.RUNTIME_HOST_ID.equals(runtimeHostId);
        return new NativeMutationContext(
                moduleId,
                dimensionId,
                idempotencyKey,
                "SERVER",
                level.getGameTime(),
                Map.of(
                        "nativeInterface", nativeInterface == null ? "" : nativeInterface,
                        "nativeMethod", nativeMethod == null ? "" : nativeMethod,
                        "hostRuntime", nativeHost
                                ? "native"
                                : nativeMinecraftHost ? "native_minecraft" : "native_loader",
                        "runtimeHostId", runtimeHostId,
                        "compatibilityDelegate", nativeHost || nativeMinecraftHost
                                ? ""
                                : NativeEchoRuntimeHost.RUNTIME_HOST_ID));
    }

    @Override
    public boolean matchesPlayer(NativePlayerRef targetPlayer) {
        return targetPlayer != null && playerRef.playerId().equals(targetPlayer.playerId());
    }

    @Override
    public ServerPlayer resolvePlayer(NativePlayerRef targetPlayer) {
        if (matchesPlayer(targetPlayer)) {
            return player;
        }
        if (targetPlayer == null) {
            return null;
        }
        try {
            UUID playerId = UUID.fromString(targetPlayer.playerId());
            return level.getServer().getPlayerList().getPlayer(playerId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public ServerLevel resolveLevel(String requestedDimensionId) {
        if (requestedDimensionId == null || requestedDimensionId.isBlank()
                || dimensionId.equals(requestedDimensionId)) {
            return level;
        }
        Identifier id = Identifier.tryParse(requestedDimensionId);
        if (id == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return level.getServer().getLevel(key);
    }
}

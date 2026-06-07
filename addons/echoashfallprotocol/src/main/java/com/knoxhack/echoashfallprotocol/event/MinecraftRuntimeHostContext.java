package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface MinecraftRuntimeHostContext {
    ServerPlayer player();

    ServerLevel level();

    String moduleId();

    String runtimeHostId();

    String dimensionId();

    NativePlayerRef playerRef();

    MinecraftRuntimeMutationLedgerSink ledgerSink();

    NativeMutationContext context(String idempotencyKey, String nativeInterface, String nativeMethod);

    boolean matchesPlayer(NativePlayerRef targetPlayer);

    ServerPlayer resolvePlayer(NativePlayerRef targetPlayer);

    ServerLevel resolveLevel(String requestedDimensionId);
}

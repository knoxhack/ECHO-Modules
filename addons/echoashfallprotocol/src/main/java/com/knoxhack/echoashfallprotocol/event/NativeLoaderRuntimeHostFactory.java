package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeLoaderAttachedRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class NativeLoaderRuntimeHostFactory {
    private static final Set<String> NATIVE_INTERFACES = Set.of(
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
            "EchoCoreServices");
    private static final Set<String> RUNTIME_ACTION_IDS = Set.of(
            "player.scanner_used",
            "native.ui.use_scanner",
            "player.inventory.grant",
            "native.ui.terminal_command",
            "native.ui.index_search",
            "native.ui.hud_refresh",
            "native.ui.mission_log_update",
            "native.ui.surface_open",
            "native.ui.index_bookmark",
            "native.ui.holomap_state",
            "native.ui.signalos_terminal",
            "native.ui.ashfall_drone_command");
    private static final Set<String> CANONICAL_CONTENT_IDS = Set.of(
            "echoterminal:echo_terminal_remote",
            "echoashfallprotocol:portable_signal_scanner",
            "echoashfallprotocol:relay_scanner_lens",
            "echoweathercore:storm_scanner",
            "echoashfallprotocol:power_cell",
            "echoashfallprotocol:native_loader_proof_marker",
            "echoashfallprotocol:wasteland_dirt",
            "echoashfallprotocol:ash_layer",
            "echoashfallprotocol:echo_cache",
            "echoashfallprotocol:relay_station",
            "echoashfallprotocol:map_table",
            "echoashfallprotocol:scrap_dynamo",
            "echoashfallprotocol:power_node",
            "minecraft:barrel",
            "minecraft:lodestone",
            "minecraft:redstone_block",
            "minecraft:coarse_dirt",
            "minecraft:polished_deepslate");

    private NativeLoaderRuntimeHostFactory() {
    }

    public static NativeLoaderEchoRuntimeHost createBackendFirst() {
        return register(new NativeLoaderEchoRuntimeHost());
    }

    public static NativeLoaderEchoRuntimeHost create(ServerPlayer player, ServerLevel level) {
        return create(player, level, NativeLoaderRuntimeMutationLedgerSink.playerPersistent(player));
    }

    public static NativeLoaderEchoRuntimeHost create(
            ServerPlayer player,
            ServerLevel level,
            NativeLoaderRuntimeMutationLedgerSink ledgerSink
    ) {
        return register(new NativeLoaderEchoRuntimeHost(new NativeMinecraftEchoRuntimeHost(new NativeLoaderRuntimeHostContext(
                player,
                level,
                "echoashfallprotocol",
                NativeMinecraftEchoRuntimeHost.RUNTIME_HOST_ID,
                ledgerSink))));
    }

    public static NativeLoaderEchoRuntimeHost create(NativeLoaderRuntimeHostContext context) {
        return register(new NativeLoaderEchoRuntimeHost(new NativeMinecraftEchoRuntimeHost(new NativeLoaderRuntimeHostContext(
                context.player(),
                context.level(),
                context.moduleId(),
                NativeMinecraftEchoRuntimeHost.RUNTIME_HOST_ID,
                context.ledgerSink()))));
    }

    private static NativeLoaderEchoRuntimeHost register(NativeLoaderEchoRuntimeHost host) {
        EchoRuntimeHostRegistry.global().register(host, new EchoRuntimeHostCapabilities(
                NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID,
                NATIVE_INTERFACES,
                RUNTIME_ACTION_IDS,
                CANONICAL_CONTENT_IDS,
                true,
                true,
                true));
        if (host.nativeLoaderBackendAttached()) {
            EchoNativeLoaderAttachedRuntimeHost.register(
                    EchoRuntimeHostRegistry.global(),
                    EchoNativeLoaderAttachedRuntimeHost.DEFAULT_RUNTIME_HOST_ID,
                    host.nativeLoaderBackend());
        }
        return host;
    }
}

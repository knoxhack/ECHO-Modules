package com.knoxhack.echocommunitybridge.integration;

import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.discord.DiscordMessageQueue;
import com.knoxhack.echocommunitybridge.server.ServerStatusService;
import com.knoxhack.echocommunitybridge.server.StatusHttpServer;
import com.knoxhack.signalos.api.SignalOsApi;
import com.knoxhack.signalos.api.SignalOsDataProvider;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsProviderStatus;
import com.knoxhack.signalos.api.TerminalDiagnosticProvider;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class CommunityBridgeSignalOsIntegration {
    private static final Identifier PROVIDER_ID = Identifier.fromNamespaceAndPath(EchoCommunityBridge.MODID, "official_bridge");

    private CommunityBridgeSignalOsIntegration() {
    }

    public static void register() {
        SignalOsApi.registerDataProvider(new BridgeDataProvider());
        SignalOsApi.registerDiagnostics(new BridgeDiagnosticsProvider());
        EchoCommunityBridge.LOGGER.info("ECHO Community Bridge registered SignalOS diagnostics.");
    }

    private static final class BridgeDataProvider implements SignalOsDataProvider {
        @Override
        public Identifier id() {
            return PROVIDER_ID;
        }

        @Override
        public List<SignalOsDataRecord> records(Player player) {
            String body = ServerStatusService.INSTANCE.summaryLine()
                    + "\nHTTP: " + (StatusHttpServer.INSTANCE.running() ? "running" : "stopped")
                    + "\nDiscord queue: " + DiscordMessageQueue.INSTANCE.pendingCount();
            return List.of(new SignalOsDataRecord(
                    Identifier.fromNamespaceAndPath(EchoCommunityBridge.MODID, "official_bridge_status"),
                    "Official Bridge Status",
                    "status",
                    "ECHO Community Bridge",
                    body,
                    20,
                    false));
        }

        @Override
        public SignalOsProviderStatus providerStatus(Player player) {
            return SignalOsProviderStatus.online(id(), "Community Bridge");
        }
    }

    private static final class BridgeDiagnosticsProvider implements TerminalDiagnosticProvider {
        @Override
        public Identifier id() {
            return PROVIDER_ID;
        }

        @Override
        public List<Diagnostic> diagnostics(Player player) {
            TerminalDiagnosticProvider.Severity severity = StatusHttpServer.INSTANCE.running()
                    ? TerminalDiagnosticProvider.Severity.INFO
                    : TerminalDiagnosticProvider.Severity.WARNING;
            String detail = ServerStatusService.INSTANCE.summaryLine()
                    + " Public HTTP status is " + (StatusHttpServer.INSTANCE.running() ? "running." : "stopped.");
            return List.of(new Diagnostic(
                    Identifier.fromNamespaceAndPath(EchoCommunityBridge.MODID, "official_bridge_http"),
                    "Official Bridge HTTP",
                    detail,
                    severity));
        }

        @Override
        public SignalOsProviderStatus providerStatus(Player player) {
            return SignalOsProviderStatus.online(id(), "Community Bridge Diagnostics");
        }
    }
}

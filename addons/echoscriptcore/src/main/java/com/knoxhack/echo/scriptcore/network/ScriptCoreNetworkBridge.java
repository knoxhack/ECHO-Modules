package com.knoxhack.echo.scriptcore.network;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public final class ScriptCoreNetworkBridge {
    private ScriptCoreNetworkBridge() {
    }

    public static void register(Object modEventBus) {
        if (modEventBus != null) {
            EchoBackendLifecycleBridge.registerModListener(modEventBus, ScriptCoreNetworkBridge::registerPayloads);
        }
    }

    private static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(
                registrar,
                ScriptCoreUiResultPacket.TYPE,
                ScriptCoreUiResultPacket.CODEC,
                ScriptCoreNetworkBridge::handleUiResult);
        EchoNetPayloads.serverboundAction(
                registrar,
                ScriptCoreUiActionPacket.TYPE,
                ScriptCoreUiActionPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("scriptcore_ui_action"),
                ScriptCoreNetworkBridge::handleUiAction);
    }

    private static void handleUiAction(
            ScriptCoreUiActionPacket packet,
            ServerPlayer player,
            EchoPayloadContext context) {
        ScriptCoreUiExecutionService.UiExecutionResult result = ScriptCoreUiExecutionService.INSTANCE.evaluate(
                new ScriptCoreUiExecutionService.UiExecutionIntent(
                        packet.mode(),
                        player,
                        packet.definitionId(),
                        packet.slot(),
                        packet.pageId(),
                        packet.componentId(),
                        packet.actionValue(),
                        packet.params()));
        EchoNetSend.toPlayer(player, ScriptCoreUiResultPacket.from(result));
        if (!result.success()) {
            String detail = ScriptCoreConfigDevLogging.enabled()
                    ? result.message()
                    : result.code();
            EchoScriptCore.LOGGER.debug("Rejected ScriptCore UI {} {} slot {} page {} component {} for {}: {}",
                    packet.mode().wireName(), packet.definitionId(), packet.slot(), packet.pageId(),
                    packet.componentId(), player.getScoreboardName(), detail);
        }
    }

    private static void handleUiResult(
            ScriptCoreUiResultPacket packet,
            Player player,
            EchoPayloadContext context) {
        try {
            Class.forName("com.knoxhack.echo.scriptcore.client.screencore.ScriptCoreScreenCoreClientState")
                    .getMethod("apply", ScriptCoreUiResultPacket.class)
                    .invoke(null, packet);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoScriptCore.LOGGER.debug("Could not apply ScriptCore ScreenCore UI result packet.", exception);
        }
    }

    private static final class ScriptCoreConfigDevLogging {
        private ScriptCoreConfigDevLogging() {
        }

        static boolean enabled() {
            try {
                return com.knoxhack.echo.scriptcore.config.ScriptCoreConfig.bool(
                        com.knoxhack.echo.scriptcore.config.ScriptCoreConfig.DEV_MODE, false);
            } catch (RuntimeException exception) {
                return false;
            }
        }
    }
}

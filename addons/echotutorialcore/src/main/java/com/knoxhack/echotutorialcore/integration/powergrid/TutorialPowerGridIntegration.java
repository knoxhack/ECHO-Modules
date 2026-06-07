package com.knoxhack.echotutorialcore.integration.powergrid;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridAlert;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.api.TutorialPowerEventType;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class TutorialPowerGridIntegration {
    private static boolean registered;

    private TutorialPowerGridIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoBackendLifecycleBridge.registerGameEventHandler(TutorialPowerGridIntegration::onServerTick);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with PowerGrid. Alert polling registered.");
    }

    private static void onServerTick(Object event) {
        MinecraftServer server = EchoBackendWorldEventBridge.serverTickServer(event);
        if (server == null) {
            return;
        }
        long time = server.overworld().getGameTime();
        if (time % 200L != 0L) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            List<PowerGridAlert> alerts = EchoPowerGridApi.alerts(level);
            if (alerts.isEmpty()) {
                continue;
            }
            String code = alerts.get(0).code();
            for (var player : level.players()) {
                TutorialCoreApi.reportPowerAlert(player, code);
                TutorialCoreApi.reportPowerEvent(player, alerts.get(0).pos(), eventType(code));
            }
        }
    }

    private static TutorialPowerEventType eventType(String code) {
        if (code == null) {
            return TutorialPowerEventType.NO_POWER;
        }
        return switch (code.toLowerCase(java.util.Locale.ROOT)) {
            case "brownout" -> TutorialPowerEventType.BROWNOUT;
            case "overload" -> TutorialPowerEventType.OVERLOAD;
            case "tripped", "breaker_tripped" -> TutorialPowerEventType.BREAKER_TRIPPED;
            default -> TutorialPowerEventType.NO_POWER;
        };
    }
}

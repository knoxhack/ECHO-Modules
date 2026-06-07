package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.client.hud.HudState;
import com.knoxhack.echoashfallprotocol.client.drone.DroneClientState;
import com.knoxhack.echoashfallprotocol.client.screen.FactionDialogueScreen;
import com.knoxhack.echoashfallprotocol.client.screen.WelcomeScreen;
import com.knoxhack.echoashfallprotocol.network.BossNavigationPacket;
import com.knoxhack.echoashfallprotocol.network.DroneMarkersPacket;
import com.knoxhack.echoashfallprotocol.network.EnvironmentalSyncPacket;
import com.knoxhack.echoashfallprotocol.network.FactionDialogueOpenPacket;
import com.knoxhack.echoashfallprotocol.network.GraceCountdownPacket;
import com.knoxhack.echoashfallprotocol.network.NexusStatePacket;
import com.knoxhack.echoashfallprotocol.network.WelcomeScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * Physical-client handlers for packets registered from common code.
 */
public final class ClientNetworkHandlers {

    private ClientNetworkHandlers() {
    }

    public static void handleEnvironmentalSync(EnvironmentalSyncPacket packet) {
        Minecraft.getInstance().execute(() ->
            HudState.setEnvEvent(packet.eventType(), packet.eventStartTime(), packet.eventDuration(),
                    packet.gameTime(), packet.intensity(), packet.phase(), packet.seed(),
                    packet.radiationStormsSurvived(), packet.toxicStormsSurvived(),
                    packet.blackoutsSurvived(), packet.ashStormsSurvived(),
                    packet.cryoFrontsSurvived(), packet.nexusSurgesSurvived())
        );
    }

    public static void handleGraceCountdown(GraceCountdownPacket packet) {
        Minecraft.getInstance().execute(() ->
            HudState.setGraceCountdown(packet.graceTicksRemaining(), packet.graceActive())
        );
    }

    public static void handleNexusState(NexusStatePacket packet) {
        Minecraft.getInstance().execute(() -> {
            HudState.setNexusState(packet.getState());
            HudState.setNexusPlayer(packet.playerName());
            HudState.setNexusPos(packet.nexusX(), packet.nexusY(), packet.nexusZ());
            HudState.setNexusCampaign(packet.campaignAwakened(), packet.nexusInstability(),
                    packet.relaysScanned(), packet.relaysResolved(), packet.readinessRestore(),
                    packet.readinessDestroy(), packet.readinessControl(), packet.siegeComplete(),
                    packet.warfrontComplete(), packet.wardenDefeated(), packet.finaleComplete());
            HudState.setNexusRelaySummaryPayload(packet.relaySummaryPayload());
        });
    }

    public static void handleBossNavigation(BossNavigationPacket packet) {
        Minecraft.getInstance().execute(() -> {
            if (!packet.active()) {
                HudState.clearBossTarget(packet.bossId());
                return;
            }
            HudState.setBossTarget(new HudState.BossTarget(
                    true,
                    packet.bossId(),
                    packet.title(),
                    packet.subtitle(),
                    packet.dimension(),
                    packet.x(),
                    packet.y(),
                    packet.z(),
                    packet.phase(),
                    packet.healthPercent(),
                    packet.accentColor(),
                    packet.compassLabel(),
                    packet.category(),
                    packet.targetKind()
            ));
        });
    }

    public static void handleWelcomeScreen(WelcomeScreenPacket packet) {
        Minecraft.getInstance().execute(WelcomeScreen::requestOpen);
    }

    public static void handleDroneMarkers(DroneMarkersPacket packet) {
        Minecraft.getInstance().execute(() -> DroneClientState.accept(packet));
    }

    public static void handleFactionDialogueOpen(FactionDialogueOpenPacket packet) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new FactionDialogueScreen(packet)));
    }
}

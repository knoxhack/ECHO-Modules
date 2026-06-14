package com.knoxhack.echoterminal.mission;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class VanillaJourneyProgression {
    private static final String ADVANCEMENT_EARNED_EVENT =
            "net.neoforged.neoforge.event.entity.player.AdvancementEvent$AdvancementEarnEvent";
    private static final String PLAYER_LOGGED_IN_EVENT =
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent";
    private static final String PLAYER_RESPAWNED_EVENT =
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerRespawnEvent";
    private static final String PLAYER_CHANGED_DIMENSION_EVENT =
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent";

    private VanillaJourneyProgression() {
    }

    public static void register() {
        EchoBackendLifecycleBridge.registerGameEventHandler(ADVANCEMENT_EARNED_EVENT,
                VanillaJourneyProgression::onAdvancementEarned);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_LOGGED_IN_EVENT,
                VanillaJourneyProgression::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_RESPAWNED_EVENT,
                VanillaJourneyProgression::onPlayerRespawned);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_CHANGED_DIMENSION_EVENT,
                VanillaJourneyProgression::onPlayerChangedDimension);
    }

    private static void onAdvancementEarned(Object event) {
        Identifier advancementId = EchoBackendWorldEventBridge.advancementId(event);
        if (advancementId == null || !VanillaJourneyProvider.INSTANCE.tracksAdvancement(advancementId)) {
            return;
        }
        sync(EchoBackendWorldEventBridge.advancementServerPlayer(event));
    }

    private static void onPlayerLoggedIn(Object event) {
        sync(EchoBackendWorldEventBridge.loggedInServerPlayer(event));
    }

    private static void onPlayerRespawned(Object event) {
        sync(EchoBackendWorldEventBridge.playerEventServerPlayer(event));
    }

    private static void onPlayerChangedDimension(Object event) {
        sync(EchoBackendWorldEventBridge.playerEventServerPlayer(event));
    }

    static boolean sync(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && VanillaJourneyProvider.INSTANCE.refreshIfChanged(serverPlayer);
    }
}

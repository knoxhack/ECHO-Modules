package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoAttachmentHandle;
import com.knoxhack.echo.adaptercore.EchoBackendAttachmentBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.mission.VanillaJourneyData;
import com.knoxhack.echoterminal.player.TerminalPlayerData;

public final class ModAttachments {
    public static final Object ATTACHMENT_TYPES =
            EchoBackendAttachmentBridge.createAttachmentRegistry(EchoTerminal.MODID);

    public static final EchoAttachmentHandle<VanillaJourneyData> VANILLA_JOURNEY_DATA =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
                    ATTACHMENT_TYPES,
                    "vanilla_journey_data",
                    VanillaJourneyData::new,
                    VanillaJourneyData.STREAM_CODEC);

    public static final EchoAttachmentHandle<TerminalPlayerData> TERMINAL_PLAYER_DATA =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
                    ATTACHMENT_TYPES,
                    "terminal_player_data",
                    TerminalPlayerData::new,
                    TerminalPlayerData.STREAM_CODEC);

    private ModAttachments() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ATTACHMENT_TYPES, eventBus);
    }
}

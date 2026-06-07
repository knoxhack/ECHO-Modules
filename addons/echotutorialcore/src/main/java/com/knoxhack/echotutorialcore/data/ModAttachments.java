package com.knoxhack.echotutorialcore.data;

import com.knoxhack.echo.adaptercore.EchoAttachmentHandle;
import com.knoxhack.echo.adaptercore.EchoBackendAttachmentBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echotutorialcore.EchoTutorialCore;

public final class ModAttachments {
    public static final Object ATTACHMENT_TYPES =
            EchoBackendAttachmentBridge.createAttachmentRegistry(EchoTutorialCore.MODID);

    public static final EchoAttachmentHandle<TutorialPlayerData> TUTORIAL_PLAYER_DATA =
            EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
                    ATTACHMENT_TYPES,
                    "tutorial_player_data",
                    TutorialPlayerData::new,
                    TutorialPlayerData.STREAM_CODEC);

    private ModAttachments() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ATTACHMENT_TYPES, eventBus);
    }
}

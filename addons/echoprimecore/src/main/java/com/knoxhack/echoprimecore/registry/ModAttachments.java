package com.knoxhack.echoprimecore.registry;

import com.knoxhack.echo.adaptercore.EchoAttachmentHandle;
import com.knoxhack.echo.adaptercore.EchoBackendAttachmentBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;

public final class ModAttachments {
    public static final Object ATTACHMENTS =
            EchoBackendAttachmentBridge.createAttachmentRegistry(EchoPrimeCore.MODID);

    public static final EchoAttachmentHandle<PrimePlayerData> PRIME_PLAYER_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(ATTACHMENTS,
            "prime_player_data",
            PrimePlayerData::new,
            PrimePlayerData.STREAM_CODEC);

    private ModAttachments() {
    }

    public static void register(Object bus) {
        EchoBackendRegistryBridge.registerEventBus(ATTACHMENTS, bus);
    }
}

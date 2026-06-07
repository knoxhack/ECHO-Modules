package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoAttachmentHandle;
import com.knoxhack.echo.adaptercore.EchoBackendAttachmentBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;

public final class ModAttachments {
   private static final Object ATTACHMENTS = EchoBackendAttachmentBridge.createAttachmentRegistry(EchoNexusProtocol.MODID);
   public static final EchoAttachmentHandle<NexusPlayerData> NEXUS_PLAYER_DATA = EchoBackendAttachmentBridge.registerSyncedCopyOnDeath(
      ATTACHMENTS,
      "nexus_player_data",
      NexusPlayerData::new,
      NexusPlayerData.STREAM_CODEC
   );

   private ModAttachments() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ATTACHMENTS, eventBus);
   }
}

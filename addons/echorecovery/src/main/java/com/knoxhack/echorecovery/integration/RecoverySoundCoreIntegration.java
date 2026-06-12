package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryEventHooks;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class RecoverySoundCoreIntegration {
    private RecoverySoundCoreIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerEventHooks(new RecoveryEventHooks() {
            @Override
            public void graveCreated(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
                EchoCoreServices.soundService().playEvent(id("grave_created"));
            }

            @Override
            public void graveRecovered(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
                EchoCoreServices.soundService().playEvent(id("grave_recovered"));
            }
        });
        EchoRecovery.LOGGER.info("Recovery SoundCore event hooks registered.");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}

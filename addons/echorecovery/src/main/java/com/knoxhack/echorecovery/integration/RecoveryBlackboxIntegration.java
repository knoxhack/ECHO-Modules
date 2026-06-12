package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryEventHooks;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryBlackboxIntegration {
    private static boolean registered;

    private RecoveryBlackboxIntegration() {
    }

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        RecoveryIntegrations.registerEventHooks(Hooks.INSTANCE);
    }

    private enum Hooks implements RecoveryEventHooks {
        INSTANCE;

        @Override
        public void graveCreated(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
            mirror(player, "death_evidence_" + shortId(snapshot), "Death Evidence",
                    "Recovery cache " + shortId(snapshot) + " created in " + snapshot.dimension()
                            + " with " + snapshot.storedItemCount() + " stored item(s).");
        }

        @Override
        public void graveRecovered(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
            mirror(player, "recovery_evidence_" + shortId(snapshot), "Recovery Evidence",
                    "Recovery cache " + shortId(snapshot) + " was recovered by "
                            + (player == null ? snapshot.ownerName() : player.getScoreboardName()) + ".");
        }

        private static void mirror(ServerPlayer player, String id, String title, String body) {
            if (player != null) {
                EchoCoreServices.mirrorIntel(player, EchoRecovery.MODID, id, title, body);
            }
        }

        private static String shortId(RecoveryGraveSnapshot snapshot) {
            String id = snapshot == null ? "" : snapshot.graveId();
            return id.length() <= 8 ? id : id.substring(0, 8);
        }
    }
}

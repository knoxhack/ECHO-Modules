package com.knoxhack.echorecovery.integration;

import com.knoxhack.echoplayercore.api.EchoPlayerCoreApi;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import com.knoxhack.echorecovery.api.RecoveryEventHooks;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class RecoveryPlayerCoreIntegration {
    private static boolean registered;

    private RecoveryPlayerCoreIntegration() {
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
            if (player == null || snapshot == null) {
                return;
            }
            Identifier dimensionId = Identifier.tryParse(snapshot.dimension());
            ResourceKey<Level> dimension = dimensionId == null
                    ? player.level().dimension()
                    : ResourceKey.create(Registries.DIMENSION, dimensionId);
            EchoPlayerCoreApi.setBackLocation(player, new TeleportLocation(
                    dimension,
                    snapshot.pos().getX() + 0.5D,
                    snapshot.pos().getY(),
                    snapshot.pos().getZ() + 0.5D,
                    player.getYRot(),
                    player.getXRot(),
                    "recovery_grave",
                    System.currentTimeMillis()));
        }
    }
}

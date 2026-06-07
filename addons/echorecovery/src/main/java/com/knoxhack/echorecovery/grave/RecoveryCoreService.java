package com.knoxhack.echorecovery.grave;

import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import net.minecraft.server.level.ServerPlayer;

public enum RecoveryCoreService implements com.knoxhack.echocore.api.EchoRecoveryService {
    INSTANCE;

    @Override
    public boolean recover(ServerPlayer player, String recoveryId) {
        if (player == null || recoveryId == null || recoveryId.isBlank()) {
            return false;
        }
        if (!RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()) {
            return false;
        }
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(player, player.getUUID(), recoveryId);
        if (lookup.ambiguous() || lookup.entry().isEmpty() || lookup.level() == null) {
            return false;
        }
        RecoveryWorldData.GraveEntry entry = lookup.entry().get();
        if (lookup.level().getBlockEntity(entry.pos()) instanceof GraveBlockEntity grave
                && GraveManager.accessGrave(grave, player, false) == GraveAccessResult.ALLOWED) {
            RecoveryIntegrations.remoteRecoveryRequested(player, grave.snapshot());
            boolean recovered = GraveManager.recoverGrave(grave, player);
            RecoveryIntegrations.remoteRecoveryCompleted(player, grave.snapshot(), recovered);
            return recovered;
        }
        return false;
    }
}

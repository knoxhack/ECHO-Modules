package com.knoxhack.echospellcore.client;

import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import com.knoxhack.echospellcore.network.SpellProjectileSyncPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

public final class SpellPredictionClientState {
    private static final Map<Integer, Snapshot> PROJECTILES = new HashMap<>();

    private SpellPredictionClientState() {
    }

    public static void apply(SpellProjectileSyncPacket packet) {
        if (packet == null) {
            return;
        }
        PROJECTILES.put(packet.entityId(), new Snapshot(
                packet.x(), packet.y(), packet.z(),
                packet.velocityX(), packet.velocityY(), packet.velocityZ(),
                packet.life(), System.currentTimeMillis()));
    }

    public static void reconcile(SpellProjectileEntity projectile) {
        Snapshot snapshot = PROJECTILES.get(projectile.getId());
        if (snapshot == null) {
            return;
        }
        Vec3 target = new Vec3(snapshot.x(), snapshot.y(), snapshot.z());
        Vec3 delta = target.subtract(projectile.position());
        if (delta.lengthSqr() > 16.0D) {
            projectile.setPos(target.x, target.y, target.z);
        } else if (delta.lengthSqr() > 0.0025D) {
            Vec3 corrected = projectile.position().add(delta.scale(0.35D));
            projectile.setPos(corrected.x, corrected.y, corrected.z);
        }
        projectile.setDeltaMovement(snapshot.velocityX(), snapshot.velocityY(), snapshot.velocityZ());
        if (snapshot.life() <= 0 || System.currentTimeMillis() - snapshot.receivedAtMillis() > 2_000L) {
            PROJECTILES.remove(projectile.getId());
        }
    }

    private record Snapshot(double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            int life, long receivedAtMillis) {
    }
}

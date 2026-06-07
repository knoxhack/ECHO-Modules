package com.knoxhack.echoruntimeguard.api;

import net.minecraft.world.phys.Vec3;

public final class RuntimeGuardParticles {
    private RuntimeGuardParticles() {
    }

    public static boolean canSpawnBudgeted(ParticlePriority priority, Vec3 pos) {
        return RuntimeGuardServices.particles().canSpawnParticle(priority, pos);
    }

    public static void recordBudgetedSpawn(ParticlePriority priority) {
        RuntimeGuardServices.particles().recordParticleSpawn(priority);
    }
}

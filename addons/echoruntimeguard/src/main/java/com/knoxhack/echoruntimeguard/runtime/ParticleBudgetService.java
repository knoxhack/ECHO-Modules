package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.ParticleBudgetSnapshot;
import com.knoxhack.echoruntimeguard.api.ParticleMode;
import com.knoxhack.echoruntimeguard.api.ParticlePriority;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

public final class ParticleBudgetService {
    public static final ParticleBudgetService INSTANCE = new ParticleBudgetService();
    private final EnumMap<ParticlePriority, Integer> byPriority = new EnumMap<>(ParticlePriority.class);
    private int used;
    private int denied;

    private ParticleBudgetService() {
    }

    public synchronized void beginTick() {
        used = 0;
        denied = 0;
        byPriority.clear();
    }

    public synchronized boolean canSpawnParticle(ParticlePriority priority, Vec3 pos) {
        ParticlePriority safePriority = priority == null ? ParticlePriority.DECORATIVE : priority;
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.PARTICLE_BUDGET_ENABLED, true)
                || safePriority.protectedSignal()) {
            return true;
        }
        int budget = currentBudget();
        boolean allowed = used < budget;
        if (allowed && safePriority == ParticlePriority.FAR_DECORATIVE
                && RuntimeGuardConfig.safeBool(RuntimeGuardConfig.REDUCE_FAR_PARTICLES, true)) {
            allowed = used < Math.max(1, (int) (budget * 0.75D));
        }
        if (!allowed) {
            denied++;
        }
        return allowed;
    }

    public synchronized void recordParticleSpawn(ParticlePriority priority) {
        ParticlePriority safePriority = priority == null ? ParticlePriority.DECORATIVE : priority;
        used++;
        byPriority.merge(safePriority, 1, Integer::sum);
    }

    public synchronized ParticleBudgetSnapshot getSnapshot() {
        return new ParticleBudgetSnapshot(currentBudget(), used, denied, RuntimeModeService.INSTANCE.mode(), Map.copyOf(byPriority));
    }

    public synchronized void reset() {
        beginTick();
    }

    private static int currentBudget() {
        ParticleMode particleMode;
        try {
            particleMode = RuntimeGuardConfig.PARTICLE_MODE.get();
        } catch (RuntimeException exception) {
            particleMode = ParticleMode.AUTO;
        }
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        ParticleMode effective = particleMode == ParticleMode.AUTO
                ? switch (mode) {
                    case POTATO, SERVER -> ParticleMode.POTATO;
                    case CINEMATIC, DEBUG -> ParticleMode.CINEMATIC;
                    case EMERGENCY -> ParticleMode.EMERGENCY;
                    case BALANCED -> ParticleMode.BALANCED;
                }
                : particleMode;
        return switch (effective) {
            case POTATO -> RuntimeGuardConfig.safeInt(RuntimeGuardConfig.POTATO_PARTICLE_BUDGET, 300);
            case CINEMATIC -> RuntimeGuardConfig.safeInt(RuntimeGuardConfig.CINEMATIC_PARTICLE_BUDGET, 3500);
            case EMERGENCY -> RuntimeGuardConfig.safeInt(RuntimeGuardConfig.EMERGENCY_PARTICLE_BUDGET, 150);
            case AUTO, BALANCED -> RuntimeGuardConfig.safeInt(RuntimeGuardConfig.BALANCED_PARTICLE_BUDGET, 1200);
        };
    }
}

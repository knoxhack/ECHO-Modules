package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.echo.EchoMessages;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.research.PerkEffectHandler;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Handles mutation rolls based on radiation levels.
 * Radiation is NOT just negative — it unlocks mutations with trade-offs.
 */
public class MutationManager {
    private static final Random RANDOM = new Random();

    public static boolean shouldShowInstabilityWarning(SurvivalData survivalData, MutationData mutationData) {
        if (survivalData == null || mutationData == null) {
            return false;
        }
        boolean hasMutationWarningLoad = mutationData.getMutationCount() >= 3
                || !mutationData.getActiveSideEffects().isEmpty();
        if (!hasMutationWarningLoad) {
            return false;
        }
        return survivalData.getRadiationLevel() >= Math.max(0.0D, Config.MUTATION_THRESHOLD.get());
    }

    /**
     * Attempt to mutate the player based on current radiation level.
     * Higher radiation = higher chance of mutation.
     */
    public static void tryMutate(ServerPlayer player, float radiationLevel) {
        MutationData data = player.getData(ModAttachments.MUTATION_DATA.get());

        // Mutation chance scales with radiation (10-50% at radiation 50-100)
        float chance = (radiationLevel - 40.0f) / 200.0f; // 5% at 50, 30% at 100
        if (RANDOM.nextFloat() > chance) return;

        // Already have all mutations?
        if (data.getMutationCount() >= MutationData.MutationType.values().length) return;

        // Pick a random mutation the player doesn't have
        MutationData.MutationType[] types = MutationData.MutationType.values();
        MutationData.MutationType chosen = null;
        for (int attempts = 0; attempts < 10; attempts++) {
            MutationData.MutationType candidate = types[RANDOM.nextInt(types.length)];
            if (!data.hasMutation(candidate)) {
                chosen = candidate;
                break;
            }
        }

        if (chosen == null) return;

        // Apply mutation
        data.addMutation(chosen);
        stabilizeSideEffects(player, data);
        player.setData(ModAttachments.MUTATION_DATA.get(), data);
        AshfallAdapterCoreHazardRuntime.mutationGained(
                player,
                chosen,
                data.getMutationCount(),
                data.getActiveSideEffects(),
                radiationLevel);

        // ECHO-7 reacts
        player.sendSystemMessage(Component.literal(
                EchoMessages.getMessage(EchoMessages.Context.MUTATION_GAINED)));
        player.sendSystemMessage(Component.literal(
                "§d[MUTATION]§r " + chosen.getDisplayName() + " — " + chosen.getDescription()));

        // Apply side effects
        applySideEffects(player, data);
    }

    /**
     * Apply ongoing side effects from mutations.
     * Called from the survival tick handler.
     */
    public static void applySideEffects(ServerPlayer player, MutationData data) {
        // Damage spikes
        if (data.hasSideEffect(MutationData.SideEffect.DAMAGE_SPIKE)) {
            float sideEffectMultiplier = PerkEffectHandler.getMutationSideEffectMultiplier(player);
            if (sideEffectMultiplier > 0.0F && RANDOM.nextFloat() < 0.005f * sideEffectMultiplier) {
                player.hurtServer((net.minecraft.server.level.ServerLevel) player.level(), player.damageSources().magic(), 2.0f);
                player.sendSystemMessage(Component.literal(
                        "§c[MUTATION]§r Genetic instability spike detected!").withColor(0xFF5555));
            }
        }

        // AI aggro: periodically force nearby hostile mobs to target the player.
        // Throttled (every 5s) so it doesn't thrash mob AI every tick.
        if (data.hasSideEffect(MutationData.SideEffect.AI_AGGRO)
                && PerkEffectHandler.getMutationSideEffectMultiplier(player) > 0.0F
                && player.tickCount % 100 == 0) {
            applyAggroPulse(player);
        }

        // Visual glitch is client-side (overlay rendering) — no server-side work.
    }

    private static void stabilizeSideEffects(ServerPlayer player, MutationData data) {
        if (PerkEffectHandler.hasResearchPerk(player, "mutant.synergy.2")) {
            data.clearSideEffects();
            return;
        }

        if (PerkEffectHandler.hasResearchPerk(player, "mutant.synergy.1")) {
            data.removeSideEffect(MutationData.SideEffect.AI_AGGRO);
        }
    }

    private static void applyAggroPulse(ServerPlayer player) {
        AABB zone = player.getBoundingBox().inflate(12.0);
        List<Mob> nearby = player.level().getEntitiesOfClass(Mob.class, zone,
                e -> e instanceof Enemy && e.isAlive());
        for (Mob mob : nearby) {
            LivingEntity current = mob.getTarget();
            if (current == null || current != player) {
                mob.setTarget(player);
            }
        }
    }

    /**
     * Apply passive mutation benefits.
     */
    public static void applyMutationEffects(ServerPlayer player) {
        MutationData data = player.getData(ModAttachments.MUTATION_DATA.get());

        if (data.hasMutation(MutationData.MutationType.NIGHT_VISION)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.NIGHT_VISION, 400, 0, false, false));
        }

        if (data.hasMutation(MutationData.MutationType.REGENERATION)) {
            // Previously 5% × 0.5 HP (~1 HP / 40s, negligible). Bumped to 10% × 1.0 HP
            // and backed by a short Regen I effect so the mutation is actually felt.
            if (player.getHealth() < player.getMaxHealth() && RANDOM.nextFloat() < 0.10f) {
                player.heal(1.0f);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 40, 0, true, false));
            }
        }
    }
}

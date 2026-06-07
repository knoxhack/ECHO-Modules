package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EuropaCryoWardenEntity extends VacuumWraithEntity {
    private final ServerBossEvent bossEvent = BossEncounterSupport.bossBar(this,
            "Europa Cryo Warden", BossEvent.BossBarColor.BLUE);
    private int phase = 1;
    private boolean announced;

    public EuropaCryoWardenEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VacuumWraithEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 54.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 3.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        updatePhaseCue();
        int pulseRate = phase == 3 ? 40 : phase == 2 ? 50 : 60;
        if (tickCount % pulseRate == Math.max(1, pulseRate - 12) && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            if (!announced) {
                player.sendSystemMessage(Component.literal("ECHO-7 // First contact: Europa Cryo Warden. Snow venting warns of oxygen and pressure loss."));
                announced = true;
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 0.75F, 0.75F + phase * 0.08F);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5D, getZ(), 12 + phase * 4, 0.5D, 0.35D, 0.5D, 0.02D);
            }
        }
        if (tickCount % pulseRate == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            SuitState state = SuitState.get(player);
            if (nearThermalCounterplay(player)) {
                state.applyThermalRecovery();
                player.sendSystemMessage(Component.literal("ECHO-7 // Thermal array caught the Cryo Warden vent pulse. Suit pressure recovering."));
            } else {
                state.compromisePressure(5 + phase * 3);
                state.drainOxygen(3 + phase * 2);
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70 + phase * 25, phase >= 3 ? 1 : 0));
                BossEncounterSupport.reportSuitPressure(player, BossEncounterSupport.EUROPA_WARDEN, phase);
            }
            state.save(player);
        }
        if (phase >= 2 && tickCount % 145 == 0 && level() instanceof ServerLevel level && getTarget() instanceof Player target) {
            Entity summon = (phase == 3 ? ModEntities.VACUUM_WRAITH.get() : ModEntities.ECHO_DEFENSE_DRONE.get())
                    .create(level, EntitySpawnReason.MOB_SUMMONED);
            if (summon != null) {
                summon.setPos(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3);
                if (summon instanceof Mob mob) {
                    mob.setTarget(target);
                }
                level.addFreshEntity(summon);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        BossEncounterSupport.update(bossEvent, this);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!level().isClientSide() && damageSource.getEntity() instanceof Player player) {
            BossEncounterSupport.report(player, BossEncounterSupport.EUROPA_WARDEN.terminalArchiveCopy());
            BossEncounterSupport.give(player, new ItemStack(ModItems.THERMAL_STABILIZER.get()));
            BossEncounterSupport.give(player, new ItemStack(ModItems.EUROPA_PROBE_ARRAY.get()));
            BossEncounterSupport.giveBlackBox(player, BossEncounterSupport.EUROPA_WARDEN);
        }
        BossEncounterSupport.clear(bossEvent);
        super.die(damageSource);
    }

    private void updatePhaseCue() {
        phase = BossEncounterSupport.updatePhase(this, phase, BossEncounterSupport.EUROPA_WARDEN);
    }

    private boolean nearThermalCounterplay(Player player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            if (level().getBlockState(pos).is(ModBlocks.THERMAL_VENT.get())
                    || level().getBlockState(pos).is(ModBlocks.EUROPA_THERMAL_ARRAY.get())) {
                return true;
            }
        }
        return false;
    }

    public int getEncounterPhase() {
        return phase;
    }
}

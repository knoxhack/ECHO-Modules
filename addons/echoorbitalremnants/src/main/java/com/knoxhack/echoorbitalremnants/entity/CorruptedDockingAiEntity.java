package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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

public class CorruptedDockingAiEntity extends EchoDefenseDroneEntity {
    private final ServerBossEvent bossEvent = BossEncounterSupport.bossBar(this,
            "Corrupted Docking AI", BossEvent.BossBarColor.RED);
    private int phase = 1;
    private boolean announced;

    public CorruptedDockingAiEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EchoDefenseDroneEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        updatePhaseCue();
        int pulseRate = phase == 3 ? 45 : phase == 2 ? 60 : 80;
        if (tickCount % pulseRate == Math.max(1, pulseRate - 12) && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            if (!announced) {
                player.sendSystemMessage(Component.literal("ECHO-7 // First contact: Corrupted Docking AI. Red spark buildup means pressure shear is next."));
                announced = true;
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.HOSTILE, 0.7F, 0.62F + phase * 0.12F);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5D, getZ(), 14 + phase * 3, 0.45D, 0.35D, 0.45D, 0.02D);
            }
        }
        if (tickCount % pulseRate == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            SuitState state = SuitState.get(player);
            state.compromisePressure(6 + phase * 4);
            if (phase >= 2) {
                state.drainOxygen(phase * 2);
            }
            state.save(player);
            BossEncounterSupport.reportSuitPressure(player, BossEncounterSupport.DOCKING_AI, phase);
        }
        if (phase >= 2 && tickCount % 150 == 0 && level() instanceof ServerLevel level && getTarget() instanceof Player target) {
            Entity drone = com.knoxhack.echoorbitalremnants.registry.ModEntities.ECHO_DEFENSE_DRONE.get()
                    .create(level, EntitySpawnReason.MOB_SUMMONED);
            if (drone != null) {
                drone.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                if (drone instanceof Mob mob) {
                    mob.setTarget(target);
                }
                level.addFreshEntity(drone);
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
            BossEncounterSupport.report(player, BossEncounterSupport.DOCKING_AI.terminalArchiveCopy());
            BossEncounterSupport.give(player, new ItemStack(ModItems.NAVIGATION_CHIP.get()));
            BossEncounterSupport.giveBlackBox(player, BossEncounterSupport.DOCKING_AI);
        }
        BossEncounterSupport.clear(bossEvent);
        super.die(damageSource);
    }

    private void updatePhaseCue() {
        phase = BossEncounterSupport.updatePhase(this, phase, BossEncounterSupport.DOCKING_AI);
    }

    public int getEncounterPhase() {
        return phase;
    }
}

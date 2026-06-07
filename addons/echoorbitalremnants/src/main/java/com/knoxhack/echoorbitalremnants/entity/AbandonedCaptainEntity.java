package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AbandonedCaptainEntity extends BrokenAstronautEntity {
    private final ServerBossEvent bossEvent = BossEncounterSupport.bossBar(this,
            "The Abandoned Captain", BossEvent.BossBarColor.YELLOW);
    private int phase = 1;

    public AbandonedCaptainEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BrokenAstronautEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 110.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        updatePhaseCue();
        int pulseRate = phase == 3 ? 45 : phase == 2 ? 58 : 70;
        if (tickCount % pulseRate == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 49.0D) {
            SuitState state = SuitState.get(player);
            state.drainOxygen(4 + phase * 3);
            state.compromisePressure(4 + phase * 2);
            state.save(player);
            BossEncounterSupport.reportSuitPressure(player, BossEncounterSupport.CAPTAIN, phase);
        }
        if (phase >= 2 && tickCount % 135 == 0 && level() instanceof ServerLevel level && getTarget() instanceof Player target) {
            Entity crew = (phase == 3
                    ? com.knoxhack.echoorbitalremnants.registry.ModEntities.NEXUS_HUSK.get()
                    : com.knoxhack.echoorbitalremnants.registry.ModEntities.BROKEN_ASTRONAUT.get())
                    .create(level, EntitySpawnReason.MOB_SUMMONED);
            if (crew != null) {
                crew.setPos(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3);
                if (crew instanceof Mob mob) {
                    mob.setTarget(target);
                }
                level.addFreshEntity(crew);
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
            BossEncounterSupport.report(player, BossEncounterSupport.CAPTAIN.terminalArchiveCopy());
            BossEncounterSupport.give(player, new ItemStack(ModItems.MARTIAN_SILICA.get(), 2));
            BossEncounterSupport.giveBlackBox(player, BossEncounterSupport.CAPTAIN);
        }
        BossEncounterSupport.clear(bossEvent);
        super.die(damageSource);
    }

    private void updatePhaseCue() {
        phase = BossEncounterSupport.updatePhase(this, phase, BossEncounterSupport.CAPTAIN);
    }

    public int getEncounterPhase() {
        return phase;
    }
}

package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.FactionStanding;
import com.knoxhack.echoorbitalremnants.progression.ModAdvancements;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EchoZeroEntity extends NexusHuskEntity {
    private final ServerBossEvent bossEvent = BossEncounterSupport.bossBar(this,
            "ECHO-0", BossEvent.BossBarColor.PURPLE);
    private int phase = 1;
    private boolean announced;

    public EchoZeroEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NexusHuskEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 180.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 12.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        updatePhaseCue();

        int pulseRate = phase == 3 ? 35 : phase == 2 ? 45 : 55;
        if (tickCount % pulseRate == Math.max(1, pulseRate - 12) && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            if (!announced) {
                player.sendSystemMessage(Component.literal("ECHO-7 // First contact: ECHO-0. Purple reverse-pulse means quarantine pressure is building."));
                announced = true;
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 0.9F, 0.68F + phase * 0.1F);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.7D, getZ(), 18 + phase * 5, 0.7D, 0.45D, 0.7D, 0.03D);
            }
        }
        if (tickCount % pulseRate == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 100.0D) {
            SuitState state = SuitState.get(player);
            for (int i = 0; i < phase + 1; i++) {
                state.addRadiation(false);
            }
            state.drainOxygen(3 + phase * 3);
            if (phase >= 2) {
                state.compromisePressure(phase * 2);
            }
            state.save(player);
            BossEncounterSupport.reportSuitPressure(player, BossEncounterSupport.ECHO_ZERO, phase);
        }
        if (phase >= 2 && tickCount % (phase == 3 ? 95 : 130) == 0 && level() instanceof ServerLevel level && getTarget() instanceof Player target) {
            Entity echo = (phase == 3 && random.nextBoolean() ? ModEntities.VACUUM_WRAITH.get() : ModEntities.NEXUS_HUSK.get())
                    .create(level, EntitySpawnReason.MOB_SUMMONED);
            if (echo != null) {
                echo.setPos(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4);
                if (echo instanceof Mob mob) {
                    mob.setTarget(target);
                }
                level.addFreshEntity(echo);
            }
        }
        if (phase == 3 && tickCount % 115 == 0) {
            quarantineReposition();
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!level().isClientSide() && damageSource.getEntity() instanceof Player player) {
            completeEncounter(player);
        }
        BossEncounterSupport.clear(bossEvent);
        super.die(damageSource);
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

    public static boolean completeEncounter(Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        if (progress.echoZeroEncountered() && progress.echoZeroRewardClaimed()) {
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.grantEchoZeroResolved(serverPlayer);
            }
            return false;
        }

        FactionStanding remnant = progress.orbitalRemnantStanding();
        FactionStanding salvagers = progress.voidSalvagerStanding();
        FactionStanding choir = progress.nexusChoirStanding();
        if (!progress.echoZeroEncountered()) {
            progress.markEchoZeroEncountered(player);
        }
        progress.setLastTerminalReport(player, "ECHO-0 resolved. Quarantine broken. Nexus anchor stabilization is now available in the SURVEY tab.");
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.grantEchoZeroResolved(serverPlayer);
        }

        give(player, new ItemStack(ModItems.NEXUS_DRIVE_CORE.get()));
        give(player, new ItemStack(ModItems.NEXUS_DUST.get(), 8));
        BossEncounterSupport.giveBlackBox(player, BossEncounterSupport.ECHO_ZERO);
        if (choir == FactionStanding.ALIGNED) {
            give(player, new ItemStack(ModItems.NEXUS_PULSE_BLADE.get()));
            give(player, new ItemStack(ModItems.NEXUS_DUST.get(), 16));
        } else if (salvagers == FactionStanding.ALIGNED) {
            give(player, new ItemStack(ModItems.ORBITAL_ALLOY.get(), 8));
            give(player, new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 4));
        } else if (remnant == FactionStanding.ALIGNED) {
            give(player, new ItemStack(ModItems.OXYGEN_BOOSTER.get()));
            give(player, new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 6));
        } else {
            give(player, new ItemStack(ModItems.ECHO_FLIGHT_CORE.get()));
            give(player, new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 4));
        }
        progress.markEchoZeroRewardClaimed(player);

        player.sendSystemMessage(Component.literal("ECHO-7 // ECHO-0 resolved. Quarantine broken. Stabilize Nexus anchors to close the post-ECHO-0 network."));
        return true;
    }

    private void updatePhaseCue() {
        phase = BossEncounterSupport.updatePhase(this, phase, BossEncounterSupport.ECHO_ZERO);
    }

    private void quarantineReposition() {
        if (!(getTarget() instanceof Player target)) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 4.0D + random.nextDouble() * 4.0D;
        teleportTo(target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
        target.sendSystemMessage(Component.literal("ECHO-0 // Quarantine vector shifted. Do not let the signal stabilize."));
    }

    public int getEncounterPhase() {
        return phase;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

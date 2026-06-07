package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LunarNexusHuskEntity extends NexusHuskEntity {
    private final ServerBossEvent bossEvent = BossEncounterSupport.bossBar(this,
            "Lunar Nexus Husk", BossEvent.BossBarColor.PURPLE);

    public LunarNexusHuskEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NexusHuskEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 96.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 60 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 49.0D) {
            SuitState state = SuitState.get(player);
            state.addRadiation(false);
            state.compromisePressure(4);
            state.save(player);
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
            BossEncounterSupport.report(player, "Lunar Nexus Husk collapsed. Helium-3 telemetry is cleaner.");
            BossEncounterSupport.give(player, new ItemStack(ModItems.LUNAR_CORE_FRAGMENT.get()));
        }
        BossEncounterSupport.clear(bossEvent);
        super.die(damageSource);
    }
}

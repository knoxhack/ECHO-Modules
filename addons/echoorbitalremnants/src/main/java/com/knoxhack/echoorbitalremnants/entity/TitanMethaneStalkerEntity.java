package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class TitanMethaneStalkerEntity extends Zombie {
    private boolean announced;

    public TitanMethaneStalkerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 60 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 25.0D) {
            if (!announced) {
                announced = true;
                player.sendSystemMessage(Component.literal("ECHO-7 // First contact: Titan Methane Stalker. Orange venting means pressure loss is coming."));
            }
            level().playSound(null, blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 0.8F, 0.55F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 10, 0.35D, 0.4D, 0.35D, 0.03D);
            }
            SuitState state = SuitState.get(player);
            state.compromisePressure(4);
            state.save(player);
        }
    }
}

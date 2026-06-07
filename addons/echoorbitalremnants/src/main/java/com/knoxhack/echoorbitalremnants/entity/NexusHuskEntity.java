package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NexusHuskEntity extends Zombie {
    public NexusHuskEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 5.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 80 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 25.0D) {
            SuitState state = SuitState.get(player);
            state.addRadiation(false);
            state.addRadiation(false);
            state.save(player);
        }
        if (!level().isClientSide() && tickCount % 120 == 0 && getTarget() != null && level() instanceof ServerLevel) {
            double dx = random.nextInt(7) - 3;
            double dz = random.nextInt(7) - 3;
            setPos(getTarget().getX() + dx, getTarget().getY(), getTarget().getZ() + dz);
        }
    }
}

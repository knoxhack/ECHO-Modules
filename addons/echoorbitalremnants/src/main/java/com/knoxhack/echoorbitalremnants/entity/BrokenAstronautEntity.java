package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BrokenAstronautEntity extends Zombie {
    public BrokenAstronautEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 60 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 9.0D) {
            SuitState state = SuitState.get(player);
            state.compromisePressure(12);
            state.save(player);
        }
    }
}

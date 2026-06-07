package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class VacuumWraithEntity extends Vex {
    public VacuumWraithEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 40 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 16.0D) {
            SuitState state = SuitState.get(player);
            state.drainOxygen(8);
            state.save(player);
        }
    }
}

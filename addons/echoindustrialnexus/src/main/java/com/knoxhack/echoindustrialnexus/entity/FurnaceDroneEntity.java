package com.knoxhack.echoindustrialnexus.entity;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class FurnaceDroneEntity extends Zombie {
   public FurnaceDroneEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.xpReward = 8;
   }

   public static Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.MOVEMENT_SPEED, 0.31)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
   }
}

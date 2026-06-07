package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.entity.ArmoryProjectileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoArmory.MODID);

   public static final EchoBackendRegistryEntry<EntityType<ArmoryProjectileEntity>> ENERGY_BOLT =
      projectile("energy_bolt");
   public static final EchoBackendRegistryEntry<EntityType<ArmoryProjectileEntity>> VEIL_ARROW =
      projectile("veil_arrow");
   public static final EchoBackendRegistryEntry<EntityType<ArmoryProjectileEntity>> SIGIL_CHAKRAM =
      projectile("sigil_chakram");

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   private static EchoBackendRegistryEntry<EntityType<ArmoryProjectileEntity>> projectile(String name) {
      return EchoBackendEntityBridge.registerEntityType(
         ENTITIES,
         name,
         ArmoryProjectileEntity::new,
         MobCategory.MISC,
         builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(8)
      );
   }
}

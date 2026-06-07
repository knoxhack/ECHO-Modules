package com.knoxhack.echoarmory.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.item.ArmoryGearItem;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryEvents {
   private ArmoryEvents() {
   }

   public static void onLivingDamage(Object event) {
      Entity source = EchoBackendWorldEventBridge.livingDamageSourceEntity(event);
      if (source instanceof ServerPlayer attacker) {
         ItemStack weapon = attacker.getMainHandItem();
         if (weapon.getItem() instanceof ArmoryGearItem gear && gear.gearKind() == ArmoryGearItem.ArmoryGearKind.WEAPON) {
            ArmoryData.initialize(weapon);
            EchoBackendWorldEventBridge.setLivingDamageAmount(
               event,
               EchoBackendWorldEventBridge.livingDamageAmount(event) + ArmoryData.damageBonus(weapon) * 0.35F);
         }
      }
      ServerPlayer defender = EchoBackendWorldEventBridge.livingDamageServerPlayer(event);
      if (defender != null) {
         int defense = 0;
         for (ItemStack armor : ArmoryData.armorStacks(defender)) {
            if (armor.getItem() instanceof ArmoryGearItem gear && gear.gearKind() == ArmoryGearItem.ArmoryGearKind.ARMOR) {
               ArmoryData.initialize(armor);
               if (ArmoryData.factionGateSatisfied(defender, armor)) {
                  defense += ArmoryData.defenseBonus(armor);
               }
            }
         }
         if (defense > 0) {
            float reduction = Math.min(0.45F, defense * 0.025F);
            EchoBackendWorldEventBridge.setLivingDamageAmount(
               event,
               EchoBackendWorldEventBridge.livingDamageAmount(event) * (1.0F - reduction));
         }
      }
   }

   public static void onPlayerTick(Object event) {
      Player tickPlayer = EchoBackendWorldEventBridge.postTickPlayer(event);
      if (!(tickPlayer instanceof ServerPlayer player) || player.level().isClientSide()) {
         return;
      }
      long time = player.level().getGameTime();
      List<ItemStack> armoryStacks = List.of(
         player.getMainHandItem(),
         player.getOffhandItem(),
         player.getItemBySlot(EquipmentSlot.HEAD),
         player.getItemBySlot(EquipmentSlot.CHEST),
         player.getItemBySlot(EquipmentSlot.LEGS),
         player.getItemBySlot(EquipmentSlot.FEET)
      );
      for (ItemStack stack : armoryStacks) {
         if (stack.getItem() instanceof ArmoryGearItem) {
            ArmoryData.initialize(stack);
            if (time % 100L == 0L) {
               InstabilityState state = stack.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE);
               if (state.instability() > 0 || state.cooldownTicks() > 0) {
                  stack.set(ModDataComponents.INSTABILITY_STATE.get(), state.decay());
               }
            }
         }
      }
      if (time % 120L == 0L && hasModule(player, "echoarmory:drone_dock")) {
         if (player.getHealth() < player.getMaxHealth()) {
            player.heal(hasConstructSynergy(player) ? 2.0F : 1.0F);
         } else {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, hasConstructSynergy(player) ? 1 : 0, true, false));
         }
      }
      if (time % 80L == 0L && hasFrostAura(player)) {
         player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, true, false));
      }
   }

   public static void onItemCrafted(Object event) {
      ItemStack crafted = EchoBackendWorldEventBridge.itemCraftedStack(event);
      if (crafted.isEmpty()) {
         return;
      }
      Player player = EchoBackendWorldEventBridge.itemCraftedPlayer(event);
      if (player == null) {
         return;
      }
      ArmoryData.gear(crafted).ifPresent(gear -> {
         if (!ArmoryData.factionGateSatisfied(player, gear)) {
            player.sendSystemMessage(Component.literal("ECHO ARMORY // Faction lock warning. " + ArmoryData.factionGateLine(gear) + " The item will remain unusable until unlocked."));
         }
      });
   }

   private static boolean hasModule(ServerPlayer player, String moduleId) {
      for (ItemStack stack : ArmoryData.armorStacks(player)) {
         if (ArmoryData.modules(stack).contains(moduleId)) {
            return true;
         }
      }
      return ArmoryData.modules(player.getMainHandItem()).contains(moduleId);
   }

   private static boolean hasConstructSynergy(ServerPlayer player) {
      return ArmoryData.hasActiveSynergyEffect(player, "drone_scaling")
         || (hasModule(player, "echoarmory:drone_dock") && player.getMainHandItem().getItem() instanceof ArmoryGearItem gear
            && gear.gearId().contains("construct_gauntlet"));
   }

   private static boolean hasFrostAura(ServerPlayer player) {
      return ArmoryData.hasActiveSynergyEffect(player, "ice_aura")
         || (hasModule(player, "echoarmory:frost_core")
            && ArmoryData.moduleDefinitions(player.getItemBySlot(EquipmentSlot.CHEST)).stream().anyMatch(module -> module.synergyTags().contains("frost")));
   }
}

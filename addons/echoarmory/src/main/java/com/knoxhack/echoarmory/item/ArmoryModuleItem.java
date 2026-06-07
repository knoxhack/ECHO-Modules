package com.knoxhack.echoarmory.item;

import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ArmoryModuleItem extends Item implements ArmoryGearItem {
   private final String moduleId;

   public ArmoryModuleItem(String moduleId, Properties properties) {
      super(properties);
      this.moduleId = moduleId;
   }

   @Override
   public String gearId() {
      return moduleId;
   }

   @Override
   public ArmoryGearKind gearKind() {
      return ArmoryGearKind.MODULE;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      ArmoryContent.module(moduleId).ifPresentOrElse(module -> appendModuleTooltip(module, tooltip), () -> tooltip.accept(Component.literal("Armory module data missing: " + moduleId)));
   }

   private static void appendModuleTooltip(ModuleDefinition module, Consumer<Component> tooltip) {
      tooltip.accept(Component.literal("Slot: " + module.slotType() + " | Effect: " + module.effectType()));
      if (module.damageBonus() > 0.0F) {
         tooltip.accept(Component.literal("Damage +" + module.damageBonus()));
      }
      if (module.defenseBonus() > 0) {
         tooltip.accept(Component.literal("Defense +" + module.defenseBonus()));
      }
      int protection = module.toxicProtection() + module.radiationProtection() + module.coldProtection() + module.heatProtection() + module.fractureProtection();
      if (protection > 0) {
         tooltip.accept(Component.literal("Protection profile +" + protection));
      }
      if (module.instability() > 0) {
         tooltip.accept(Component.literal("Instability +" + module.instability()));
      }
   }
}

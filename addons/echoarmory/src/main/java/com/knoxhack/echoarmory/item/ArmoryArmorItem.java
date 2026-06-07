package com.knoxhack.echoarmory.item;

import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.InstalledModules;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ArmoryArmorItem extends Item implements ArmoryGearItem {
   private final String gearId;

   public ArmoryArmorItem(String gearId, Properties properties) {
      super(properties);
      this.gearId = gearId;
   }

   @Override
   public String gearId() {
      return gearId;
   }

   @Override
   public ArmoryGearKind gearKind() {
      return ArmoryGearKind.ARMOR;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      ArmoryData.initialize(stack);
      ArmoryContent.gear(gearId).ifPresent(gear -> appendGearTooltip(stack, gear, tooltip));
   }

   private static void appendGearTooltip(ItemStack stack, GearDefinition gear, Consumer<Component> tooltip) {
      InstalledModules modules = ArmoryData.modules(stack);
      tooltip.accept(Component.literal("Tier " + gear.tier() + " " + gear.craftingStage()));
      tooltip.accept(Component.literal("Defense " + (gear.baseDefense() + ArmoryData.defenseBonus(stack)) + " | Modules " + modules.modules().size() + "/" + gear.moduleSlots()));
      tooltip.accept(Component.literal("Protection T/R/C/H/F "
         + ArmoryData.protection(stack, ArmoryData.ProtectionType.TOXIC) + "/"
         + ArmoryData.protection(stack, ArmoryData.ProtectionType.RADIATION) + "/"
         + ArmoryData.protection(stack, ArmoryData.ProtectionType.COLD) + "/"
         + ArmoryData.protection(stack, ArmoryData.ProtectionType.HEAT) + "/"
         + ArmoryData.protection(stack, ArmoryData.ProtectionType.FRACTURE)));
      EnergyState energy = stack.get(ModDataComponents.ENERGY_STATE.get());
      if (energy != null && energy.capacity() > 0) {
         tooltip.accept(Component.literal("Energy " + energy.stored() + "/" + energy.capacity()));
      }
      if (!gear.factionGate().isBlank()) {
         tooltip.accept(Component.literal("Faction gate: " + gear.factionGate() + " rep " + ArmoryData.requiredReputation(gear)));
      }
      for (ModuleDefinition module : ArmoryData.moduleDefinitions(stack)) {
         tooltip.accept(Component.literal("Module: " + module.title()));
      }
   }
}

package com.knoxhack.echonexusprotocol.compat.jei;

import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NexusJeiRecipe(
   Identifier id,
   NexusMachineBlock.MachineKind machine,
   List<ItemStack> inputs,
   List<ItemStack> outputs,
   int duration,
   int chargeCost,
   int chargeOutput,
   int corruptionDelta,
   int fieldDelta
) {
   public List<Component> notes() {
      java.util.ArrayList<Component> notes = new java.util.ArrayList<>();
      if (this.chargeCost > 0) {
         notes.add(Component.literal("Cost: " + this.chargeCost + " Nexus Charge"));
      }
      if (this.chargeOutput > 0) {
         notes.add(Component.literal("Output: " + this.chargeOutput + " Nexus Charge"));
      }
      if (this.corruptionDelta != 0) {
         notes.add(Component.literal((this.corruptionDelta > 0 ? "+" : "") + this.corruptionDelta + "% contamination"));
      }
      if (this.fieldDelta != 0) {
         notes.add(Component.literal((this.fieldDelta > 0 ? "+" : "") + this.fieldDelta + " field stability"));
      }
      return List.copyOf(notes);
   }
}

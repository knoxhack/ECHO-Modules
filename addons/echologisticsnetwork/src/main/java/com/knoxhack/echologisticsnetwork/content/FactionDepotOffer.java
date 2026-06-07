package com.knoxhack.echologisticsnetwork.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record FactionDepotOffer(
   Identifier id,
   Identifier factionId,
   StackSpec inputSpec,
   StackSpec outputSpec,
   int reputationDelta,
   int minReputation,
   int cooldownTicks
) {
   public FactionDepotOffer {
      if (id == null) {
         throw new IllegalArgumentException("Faction depot offer id is required.");
      }
      if (factionId == null) {
         factionId = Identifier.fromNamespaceAndPath(id.getNamespace(), "logistics_exchange");
      }
      inputSpec = inputSpec == null ? StackSpec.empty() : inputSpec;
      outputSpec = outputSpec == null ? StackSpec.empty() : outputSpec;
      cooldownTicks = Math.max(0, cooldownTicks);
   }

   public ItemStack input() {
      return inputSpec.stack();
   }

   public ItemStack output() {
      return outputSpec.stack();
   }

   public record StackSpec(Identifier itemId, int count) {
      public StackSpec {
         itemId = itemId == null ? Identifier.withDefaultNamespace("air") : itemId;
         count = Math.max(0, count);
      }

      public static StackSpec empty() {
         return new StackSpec(Identifier.withDefaultNamespace("air"), 0);
      }

      public ItemStack stack() {
         Item item = BuiltInRegistries.ITEM.getValue(itemId);
         return item == null || item == Items.AIR || count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
      }
   }
}

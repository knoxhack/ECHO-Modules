package com.knoxhack.echologisticsnetwork.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record SupplyCategory(
   Identifier id,
   String title,
   int order,
   int accentColor,
   int lowStockTarget,
   Identifier tagId
) {
   public SupplyCategory {
      if (id == null) {
         throw new IllegalArgumentException("Supply category id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath() : title.strip();
      tagId = tagId == null ? Identifier.fromNamespaceAndPath(id.getNamespace(), "echo_logistics/" + id.getPath()) : tagId;
      lowStockTarget = Math.max(0, lowStockTarget);
   }

   public TagKey<Item> tagKey() {
      return TagKey.create(Registries.ITEM, tagId);
   }

   public boolean matches(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (stack.is(tagKey())) {
         return true;
      }
      return switch (id.getPath()) {
         case "water" -> stack.is(Items.POTION) || stack.is(Items.WATER_BUCKET);
         case "food" -> stack.is(Items.APPLE) || stack.is(Items.BREAD) || stack.is(Items.COOKED_BEEF) || stack.is(Items.BAKED_POTATO);
         case "medicine" -> stack.is(Items.GOLDEN_APPLE) || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.MILK_BUCKET);
         case "filters" -> stack.is(Items.PAPER) || stack.is(Items.CHARCOAL);
         case "ammo" -> stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.FIREWORK_ROCKET);
         case "machine_parts" -> stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.REDSTONE) || stack.is(Items.PISTON);
         case "rocket_parts" -> stack.is(Items.FIREWORK_ROCKET) || stack.is(Items.BLAST_FURNACE);
         case "station_parts" -> stack.is(Items.IRON_DOOR) || stack.is(Items.IRON_BARS) || stack.is(Items.IRON_INGOT);
         case "blackbox_parts" -> stack.is(Items.ECHO_SHARD) || stack.is(Items.ENDER_EYE) || stack.is(Items.SCULK_SENSOR);
         case "faction_goods" -> stack.is(Items.EMERALD) || stack.is(Items.COPPER_INGOT) || stack.is(Items.PAPER);
         default -> false;
      };
   }
}

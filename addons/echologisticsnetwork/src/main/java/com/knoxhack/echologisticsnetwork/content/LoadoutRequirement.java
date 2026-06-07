package com.knoxhack.echologisticsnetwork.content;

import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record LoadoutRequirement(Kind kind, Identifier target, int count, boolean optional) {
   public LoadoutRequirement {
      kind = kind == null ? Kind.ITEM : kind;
      if (target == null) {
         throw new IllegalArgumentException("Loadout requirement target is required.");
      }
      count = Math.max(1, count);
   }

   public boolean matches(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      return switch (kind) {
         case ITEM -> stack.is(resolveItem());
         case TAG -> stack.is(TagKey.create(Registries.ITEM, target));
         case CATEGORY -> LogisticsContent.category(target.toString())
            .map(category -> category.matches(stack))
            .orElse(false);
      };
   }

   public Item resolveItem() {
      return kind == Kind.ITEM ? BuiltInRegistries.ITEM.getValue(target) : Items.AIR;
   }

   public String displayName() {
      return switch (kind) {
         case ITEM -> new ItemStack(resolveItem()).getHoverName().getString();
         case TAG -> "#" + target;
         case CATEGORY -> LogisticsContent.category(target.toString()).map(SupplyCategory::title).orElse(target.toString());
      };
   }

   public enum Kind {
      ITEM,
      TAG,
      CATEGORY;

      public static Kind parse(String name) {
         try {
            return valueOf((name == null ? "" : name).strip().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException exception) {
            return ITEM;
         }
      }
   }
}

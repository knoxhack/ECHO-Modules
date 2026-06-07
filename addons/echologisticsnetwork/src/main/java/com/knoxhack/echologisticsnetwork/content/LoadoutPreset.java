package com.knoxhack.echologisticsnetwork.content;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record LoadoutPreset(
   Identifier id,
   String title,
   int order,
   Identifier icon,
   List<LoadoutRequirement> requirements,
   List<Identifier> targetBlockTypes,
   int deliveryTicks,
   FactoryRestockPolicy restockPolicy
) {
   public LoadoutPreset(Identifier id,
                        String title,
                        int order,
                        Identifier icon,
                        List<LoadoutRequirement> requirements,
                        List<Identifier> targetBlockTypes,
                        int deliveryTicks) {
      this(id, title, order, icon, requirements, targetBlockTypes, deliveryTicks, FactoryRestockPolicy.disabled());
   }

   public LoadoutPreset {
      if (id == null) {
         throw new IllegalArgumentException("Loadout preset id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath() : title.strip();
      icon = icon == null ? Identifier.withDefaultNamespace("chest") : icon;
      requirements = List.copyOf(requirements == null ? List.of() : requirements);
      targetBlockTypes = List.copyOf(targetBlockTypes == null ? List.of() : targetBlockTypes);
      deliveryTicks = Math.max(40, deliveryTicks);
      restockPolicy = restockPolicy == null ? FactoryRestockPolicy.disabled() : restockPolicy;
   }

   public Item iconItem() {
      Item item = BuiltInRegistries.ITEM.getValue(icon);
      return item == null ? Items.CHEST : item;
   }
}

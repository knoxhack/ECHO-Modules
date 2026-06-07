package com.knoxhack.echoindustrialnexus.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Stable source card shared by recipe index, tutorials, and trace/debug surfaces.
 */
public record IndustrialProcessSource(
   Identifier itemId,
   Identifier sourceId,
   String sourceKind,
   String title,
   List<String> notes,
   ItemStack icon
) {
   public IndustrialProcessSource {
      if (itemId == null) {
         throw new IllegalArgumentException("Industrial process source item id is required.");
      }
      sourceId = sourceId == null ? itemId : sourceId;
      sourceKind = clean(sourceKind, "Source");
      title = clean(title, itemId.getPath().replace('_', ' '));
      notes = List.copyOf(notes == null ? List.of() : notes.stream()
         .filter(note -> note != null && !note.isBlank())
         .map(String::strip)
         .toList());
      icon = icon == null ? ItemStack.EMPTY : icon.copy();
   }

   private static String clean(String value, String fallback) {
      String cleaned = value == null ? "" : value.strip();
      return cleaned.isBlank() ? fallback : cleaned;
   }
}

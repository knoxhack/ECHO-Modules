package com.knoxhack.echoarmory.content;

import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import java.util.List;
import net.minecraft.resources.Identifier;

public record StationRecipeDefinition(
   Identifier id,
   String title,
   StationKind stationKind,
   String operation,
   List<String> gearTags,
   List<String> moduleTags,
   List<String> auxItems,
   String result,
   int energyCost,
   int order
) {
   public StationRecipeDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Station recipe id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      stationKind = stationKind == null ? StationKind.ARMORY_BENCH : stationKind;
      operation = operation == null || operation.isBlank() ? stationKind.getSerializedName() : operation.strip();
      gearTags = List.copyOf(gearTags == null ? List.of() : gearTags);
      moduleTags = List.copyOf(moduleTags == null ? List.of() : moduleTags);
      auxItems = List.copyOf(auxItems == null ? List.of() : auxItems);
      result = result == null ? "" : result.strip();
      energyCost = Math.max(0, energyCost);
      order = Math.max(0, order);
   }
}

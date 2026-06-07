package com.knoxhack.echoagriculturereclamation.content;

import java.util.List;
import java.util.Locale;

public record ReclamationProcessDefinition(
   String id,
   String machine,
   String title,
   List<String> inputs,
   List<String> catalysts,
   List<String> outputs,
   int ticks,
   int powerCost,
   List<String> notes
) {
   public ReclamationProcessDefinition {
      id = clean(id, "unknown");
      machine = clean(machine, "generic");
      title = title == null || title.isBlank() ? id : title.strip();
      inputs = cleanList(inputs);
      catalysts = cleanList(catalysts);
      outputs = cleanList(outputs);
      ticks = Math.max(0, ticks);
      powerCost = Math.max(0, powerCost);
      notes = cleanList(notes);
   }

   public ReclamationProcessDefinition normalized() {
      return new ReclamationProcessDefinition(id, machine, title, inputs, catalysts, outputs, ticks, powerCost, notes);
   }

   private static String clean(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value.trim().toLowerCase(Locale.ROOT).replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
   }

   private static List<String> cleanList(List<String> values) {
      if (values == null) {
         return List.of();
      }
      return values.stream()
         .filter(value -> value != null && !value.isBlank())
         .map(String::strip)
         .toList();
   }
}

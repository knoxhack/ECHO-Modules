package com.knoxhack.echoagriculturereclamation.content;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record CropSpec(
   String path,
   String displayName,
   CropCategory category,
   int baseGrowthChance,
   int baseYield,
   int restorationWeight
) {
   public static final List<CropSpec> ALL = List.of(
      new CropSpec("ash_wheat", "Ash Wheat", CropCategory.EMERGENCY, 34, 2, 1),
      new CropSpec("hardroot", "Hardroot", CropCategory.EMERGENCY, 28, 2, 1),
      new CropSpec("glow_beans", "Glow Beans", CropCategory.MUTATED, 24, 2, 1),
      new CropSpec("radleaf", "Radleaf", CropCategory.MUTATED, 20, 1, 1),
      new CropSpec("mutant_berries", "Mutant Berries", CropCategory.MUTATED, 30, 2, 1),
      new CropSpec("cryo_moss", "Cryo Moss", CropCategory.RESTORATION, 22, 1, 3),
      new CropSpec("clean_corn", "Clean Corn", CropCategory.EMERGENCY, 26, 3, 2),
      new CropSpec("medicinal_aloe", "Medicinal Aloe", CropCategory.MEDICINAL, 24, 2, 2),
      new CropSpec("filter_reed", "Filter Reed", CropCategory.INDUSTRIAL, 25, 2, 3),
      new CropSpec("nexus_orchid", "Nexus Orchid", CropCategory.NEXUS_TOUCHED, 14, 1, 4),
      new CropSpec("signal_fungus", "Signal Fungus", CropCategory.INDUSTRIAL, 20, 2, 2)
   );
   public static final CropSpec DEFAULT = ALL.get(0);

   public Identifier id() {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }

   public String key() {
      return path.toLowerCase(Locale.ROOT);
   }

   public static CropSpec byPath(String path) {
      if (path == null || path.isBlank()) {
         return DEFAULT;
      }
      String clean = path.toLowerCase(Locale.ROOT);
      return ALL.stream().filter(spec -> spec.path.equals(clean)).findFirst().orElse(DEFAULT);
   }

   public static CropSpec byIndex(int index) {
      return ALL.get(Math.floorMod(index, ALL.size()));
   }

   public static List<CropSpec> sorted() {
      return ALL.stream().sorted(Comparator.comparing(CropSpec::path)).toList();
   }
}

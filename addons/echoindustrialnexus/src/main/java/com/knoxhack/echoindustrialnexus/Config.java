package com.knoxhack.echoindustrialnexus;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import java.util.List;

public final class Config {
   public static final EchoNativeConfigSpec SPEC;
   public static final EchoNativeConfigSpec.IntValue SCRAP_DYNAMO_OUTPUT;
   public static final EchoNativeConfigSpec.IntValue THERMAL_ARRAY_OUTPUT;
   public static final EchoNativeConfigSpec.IntValue PROCESSOR_FLUX_CAPACITY;
   public static final EchoNativeConfigSpec.IntValue CAPACITOR_FLUX_CAPACITY;
   public static final EchoNativeConfigSpec.IntValue DUCT_TRANSFER_RATE;
   public static final EchoNativeConfigSpec.IntValue SCRUBBER_FLUX_PER_TICK;
   public static final EchoNativeConfigSpec.BooleanValue PROCEDURAL_POIS_ENABLED;
   public static final EchoNativeConfigSpec.IntValue POI_SPACING_CHUNKS;
   public static final EchoNativeConfigSpec.IntValue POI_SEARCH_RADIUS;
   public static final EchoNativeConfigSpec.IntValue THERMAL_PLANT_SPACING_BONUS;
   public static final EchoNativeConfigSpec.IntValue FACTORY_SPACING_BONUS;
   public static final EchoNativeConfigSpec.IntValue GEOTHERMAL_SPACING_BONUS;
   public static final EchoNativeConfigSpec.IntValue REACTOR_SPACING_BONUS;
   public static final EchoNativeConfigSpec.IntValue NEXUS_EXCHANGER_SPACING_BONUS;

   private Config() {
   }

   public static void registerEchoConfig() {
      EchoConfigRegistry.register(EchoConfigProvider.of(EchoIndustrialNexus.MODID, () -> new EchoConfigModule(
         EchoIndustrialNexus.MODID,
         "Industrial Nexus",
         List.of(
            new EchoConfigCategory("thermal_flux", "Thermal Flux", List.of(
               EchoConfigEntry.intSpec("scrap_dynamo_output", "Scrap Dynamo Output",
                  "Thermal Flux generated per tick while the Scrap Dynamo burns fuel.",
                  EchoConfigSide.COMMON, SCRAP_DYNAMO_OUTPUT, 1, 4096, true, false, false),
               EchoConfigEntry.intSpec("thermal_array_output", "Thermal Array Output",
                  "Thermal Flux generated per tick while the Thermal Array burns a heat source.",
                  EchoConfigSide.COMMON, THERMAL_ARRAY_OUTPUT, 1, 8192, true, false, false),
               EchoConfigEntry.intSpec("processor_capacity", "Processor Capacity",
                  "Internal Thermal Flux buffer for Industrial Nexus processors.",
                  EchoConfigSide.COMMON, PROCESSOR_FLUX_CAPACITY, 100, 1000000, true, false, false),
               EchoConfigEntry.intSpec("capacitor_capacity", "Capacitor Capacity",
                  "Thermal Flux stored by the basic Flux Capacitor Bank.",
                  EchoConfigSide.COMMON, CAPACITOR_FLUX_CAPACITY, 1000, 10000000, true, false, false),
               EchoConfigEntry.intSpec("duct_transfer_rate", "Duct Transfer Rate",
                  "Thermal Flux a Copper Flux Duct network can move per pull.",
                  EchoConfigSide.COMMON, DUCT_TRANSFER_RATE, 1, 8192, true, false, false),
               EchoConfigEntry.intSpec("scrubber_flux_per_tick", "Scrubber Flux Per Tick",
                  "Thermal Flux consumed per tick by an active Industrial Scrubber.",
                  EchoConfigSide.COMMON, SCRUBBER_FLUX_PER_TICK, 1, 1024, true, false, false))),
            new EchoConfigCategory("worldgen", "Worldgen", List.of(
               EchoConfigEntry.booleanSpec("procedural_pois", "Procedural POIs",
                  "Generate compact Industrial Nexus POIs during new chunk loads.",
                  EchoConfigSide.COMMON, PROCEDURAL_POIS_ENABLED, true, true, true),
               EchoConfigEntry.intSpec("poi_spacing_chunks", "POI Spacing Chunks",
                  "Base chunk spacing for Industrial Nexus POI attempts.",
                  EchoConfigSide.COMMON, POI_SPACING_CHUNKS, 12, 160, true, true, true),
               EchoConfigEntry.intSpec("poi_search_radius", "POI Search Radius",
                  "Surface search radius used when placing Industrial Nexus POIs.",
                  EchoConfigSide.COMMON, POI_SEARCH_RADIUS, 2, 32, true, true, true),
               EchoConfigEntry.intSpec("thermal_plant_bonus", "Thermal Plant Bonus",
                  "Extra chunks between Abandoned Thermal Plant attempts.",
                  EchoConfigSide.COMMON, THERMAL_PLANT_SPACING_BONUS, 0, 120, true, true, true),
               EchoConfigEntry.intSpec("factory_bonus", "Factory Bonus",
                  "Extra chunks between Rusted Factory Complex attempts.",
                  EchoConfigSide.COMMON, FACTORY_SPACING_BONUS, 0, 120, true, true, true),
               EchoConfigEntry.intSpec("geothermal_bonus", "Geothermal Bonus",
                  "Extra chunks between Geothermal Drill Site attempts.",
                  EchoConfigSide.COMMON, GEOTHERMAL_SPACING_BONUS, 0, 120, true, true, true),
               EchoConfigEntry.intSpec("reactor_bonus", "Reactor Bonus",
                  "Extra chunks between Reactor Cooling Station attempts.",
                  EchoConfigSide.COMMON, REACTOR_SPACING_BONUS, 0, 120, true, true, true),
               EchoConfigEntry.intSpec("nexus_exchanger_bonus", "Nexus Exchanger Bonus",
                  "Extra chunks between Nexus Heat Exchanger Ruins attempts.",
                  EchoConfigSide.COMMON, NEXUS_EXCHANGER_SPACING_BONUS, 0, 120, true, true, true)))))));
   }

   static {
      EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
      builder.push("thermal_flux");
      SCRAP_DYNAMO_OUTPUT = builder.comment("Thermal Flux generated per tick while the Scrap Dynamo is burning fuel.").defineInRange("scrapDynamoOutput", 18, 1, 4096);
      THERMAL_ARRAY_OUTPUT = builder.comment("Thermal Flux generated per tick while the Thermal Array is burning a heat source.").defineInRange("thermalArrayOutput", 80, 1, 8192);
      PROCESSOR_FLUX_CAPACITY = builder.comment("Internal Thermal Flux buffer for Industrial Nexus processors.").defineInRange("processorFluxCapacity", 10000, 100, 1000000);
      CAPACITOR_FLUX_CAPACITY = builder.comment("Thermal Flux stored by the basic Flux Capacitor Bank.").defineInRange("capacitorFluxCapacity", 180000, 1000, 10000000);
      DUCT_TRANSFER_RATE = builder.comment("Thermal Flux a Copper Flux Duct network can move per pull.").defineInRange("ductTransferRate", 448, 1, 8192);
      SCRUBBER_FLUX_PER_TICK = builder.comment("Thermal Flux consumed per tick by an active Industrial Scrubber.").defineInRange("scrubberFluxPerTick", 6, 1, 1024);
      builder.pop();
      builder.push("worldgen");
      PROCEDURAL_POIS_ENABLED = builder.comment("Generate compact Industrial Nexus procedural POIs during new chunk loads.").define("proceduralPoisEnabled", true);
      POI_SPACING_CHUNKS = builder.comment("Base chunk spacing for Industrial Nexus procedural POI attempts.").defineInRange("poiSpacingChunks", 42, 12, 160);
      POI_SEARCH_RADIUS = builder.comment("Surface search radius used when placing Industrial Nexus POIs.").defineInRange("poiSearchRadius", 16, 2, 32);
      THERMAL_PLANT_SPACING_BONUS = builder.comment("Extra chunks between Abandoned Thermal Plant attempts.").defineInRange("thermalPlantSpacingBonus", 6, 0, 120);
      FACTORY_SPACING_BONUS = builder.comment("Extra chunks between Rusted Factory Complex attempts.").defineInRange("factorySpacingBonus", 0, 0, 120);
      GEOTHERMAL_SPACING_BONUS = builder.comment("Extra chunks between Geothermal Drill Site attempts.").defineInRange("geothermalSpacingBonus", 4, 0, 120);
      REACTOR_SPACING_BONUS = builder.comment("Extra chunks between Reactor Cooling Station attempts.").defineInRange("reactorSpacingBonus", 10, 0, 120);
      NEXUS_EXCHANGER_SPACING_BONUS = builder.comment("Extra chunks between Nexus Heat Exchanger Ruins attempts.").defineInRange("nexusExchangerSpacingBonus", 14, 0, 120);
      builder.pop();
      SPEC = builder.build();
   }
}

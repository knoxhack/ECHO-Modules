package com.knoxhack.echoindustrialnexus.worldgen;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echoindustrialnexus.registry.ModBlocks;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.server.level.ServerLevel;

public final class IndustrialPoiGenerator {
   private IndustrialPoiGenerator() {
   }

   public static void generate(ServerLevel level, BlockPos origin, IndustrialPoiType type, RandomSource random) {
      BlockPos base = terrainBase(level, origin, type);
      if (!canGenerate(level, base, type)) {
         EchoIndustrialNexus.LOGGER.debug("Skipped Industrial Nexus POI {} at {} because terrain failed placement checks.", type.id(), base);
         return;
      }
      int variant = Math.abs((int)(base.asLong() + type.salt())) % 3;
      int radiusX = type == IndustrialPoiType.ABANDONED_THERMAL_PLANT ? 8 + variant : 6 + variant;
      int radiusZ = type == IndustrialPoiType.RUSTED_FACTORY_COMPLEX ? 8 + variant : 6 + (variant % 2);
      radiusX += biomeWidthBonus(level, base, type);
      buildPad(level, base, radiusX, radiusZ);
      buildShell(level, base, type, radiusX, radiusZ, variant);
      buildApproachPad(level, base, type, radiusX, radiusZ, variant);
      breakPerimeter(level, base, type, radiusX, radiusZ, variant);
      placeTypeReadability(level, base, type, radiusX, radiusZ, variant);
      placeCore(level, base, type, variant);
      placeLoot(level, base.offset(radiusX - 2, 1, radiusZ - 2), type, random);
      if (variant >= 1) {
         placeLoot(level, base.offset(-radiusX + 2, 1, radiusZ - 2), type, random);
      }
      placeHazards(level, base, type, radiusX, radiusZ, variant);
      if (type == IndustrialPoiType.ABANDONED_THERMAL_PLANT) {
         buildWardenArena(level, base, radiusX, radiusZ);
      }
      IndustrialProgress.recordPoiGenerated(level, type.id(), base);
      EchoIndustrialNexus.LOGGER.info("Generated Industrial Nexus POI {} at {}", type.id(), base);
   }

   public static boolean canGenerate(ServerLevel level, BlockPos origin, IndustrialPoiType type) {
      BlockPos base = terrainBase(level, origin, type);
      int radius = type == IndustrialPoiType.ABANDONED_THERMAL_PLANT || type == IndustrialPoiType.RUSTED_FACTORY_COMPLEX ? 10 : 8;
      if (!footprintChunksLoaded(level, base, radius)) {
         return false;
      }
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      int water = 0;
      int solid = 0;
      for (int x = -radius; x <= radius; x += 4) {
         for (int z = -radius; z <= radius; z += 4) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX() + x, base.getZ() + z);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            BlockPos surface = new BlockPos(base.getX() + x, y, base.getZ() + z);
            if (!level.getFluidState(surface).isEmpty() || !level.getFluidState(surface.below()).isEmpty()) {
               water++;
            }
            if (!level.getBlockState(surface.below()).isAir()) {
               solid++;
            }
         }
      }
      return maxY - minY <= 7 && water <= 2 && solid >= 12 && biomeAllows(level, base, type);
   }

   private static boolean footprintChunksLoaded(ServerLevel level, BlockPos base, int radius) {
      int minChunkX = Math.floorDiv(base.getX() - radius, 16);
      int maxChunkX = Math.floorDiv(base.getX() + radius, 16);
      int minChunkZ = Math.floorDiv(base.getZ() - radius, 16);
      int maxChunkZ = Math.floorDiv(base.getZ() + radius, 16);
      for (int cx = minChunkX; cx <= maxChunkX; cx++) {
         for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            if (!level.hasChunk(cx, cz)) {
               return false;
            }
         }
      }
      return true;
   }

   private static boolean biomeAllows(ServerLevel level, BlockPos base, IndustrialPoiType type) {
      String biome = level.getBiome(base).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
      return switch (type) {
         case GEOTHERMAL_DRILL_SITE -> !biome.contains("ocean") && !biome.contains("river");
         case REACTOR_COOLING_STATION -> !biome.contains("ice") && !biome.contains("frozen") && !biome.contains("ocean");
         case NEXUS_HEAT_EXCHANGER_RUINS -> !biome.contains("ocean");
         default -> !biome.contains("ocean") && !biome.contains("deep");
      };
   }

   private static BlockPos terrainBase(ServerLevel level, BlockPos origin, IndustrialPoiType type) {
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
      BlockPos surface = new BlockPos(origin.getX(), y, origin.getZ());
      if (type == IndustrialPoiType.GEOTHERMAL_DRILL_SITE || type == IndustrialPoiType.REACTOR_COOLING_STATION) {
         surface = surface.below(1 + Math.abs((int)(origin.asLong() + type.salt())) % 3);
      }
      while (surface.getY() > level.getMinY() + 4 && level.getBlockState(surface.below()).isAir()) {
         surface = surface.below();
      }
      if (level.getFluidState(surface).isSource() || level.getFluidState(surface.below()).isSource()) {
         surface = surface.above(2);
      }
      return surface;
   }

   private static int biomeWidthBonus(ServerLevel level, BlockPos base, IndustrialPoiType type) {
      String biome = level.getBiome(base).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
      if (type == IndustrialPoiType.GEOTHERMAL_DRILL_SITE && (biome.contains("mountain") || biome.contains("peak") || biome.contains("badlands"))) {
         return 1;
      }
      if (type == IndustrialPoiType.REACTOR_COOLING_STATION && (biome.contains("desert") || biome.contains("badlands"))) {
         return 1;
      }
      if (type == IndustrialPoiType.NEXUS_HEAT_EXCHANGER_RUINS && (biome.contains("swamp") || biome.contains("dark"))) {
         return 1;
      }
      return 0;
   }

   private static void buildPad(ServerLevel level, BlockPos base, int radiusX, int radiusZ) {
      BlockState floor = ModBlocks.INDUSTRIAL_GRATE.get().defaultBlockState();
      BlockState wall = ModBlocks.REINFORCED_FACTORY_WALL.get().defaultBlockState();
      for (int x = -radiusX; x <= radiusX; x++) {
         for (int z = -radiusZ; z <= radiusZ; z++) {
            for (int y = -2; y < 0; y++) {
               if (level.getBlockState(base.offset(x, y, z)).isAir()) {
                  level.setBlock(base.offset(x, y, z), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
               }
            }
            level.setBlock(base.offset(x, 0, z), floor, 3);
            if (Math.abs(x) == radiusX || Math.abs(z) == radiusZ) {
               level.setBlock(base.offset(x, 1, z), wall, 3);
               if ((Math.abs(x) + Math.abs(z)) % 4 == 0) {
                  level.setBlock(base.offset(x, 2, z), wall, 3);
               }
            }
         }
      }
   }

   private static void buildShell(ServerLevel level, BlockPos base, IndustrialPoiType type, int radiusX, int radiusZ, int variant) {
      BlockState pipe = ModBlocks.PIPE_WALL.get().defaultBlockState();
      BlockState light = ModBlocks.FACTORY_LIGHT.get().defaultBlockState();
      BlockState emergency = ModBlocks.EMERGENCY_LIGHT.get().defaultBlockState();
      BlockState warning = ModBlocks.WARNING_STRIPE_BLOCK.get().defaultBlockState();
      for (int x = -radiusX + 2; x <= radiusX - 2; x += 3) {
         level.setBlock(base.offset(x, 2, -radiusZ), pipe, 3);
         level.setBlock(base.offset(x, 2, radiusZ), pipe, 3);
      }
      for (int z = -radiusZ + 2; z <= radiusZ - 2; z += 3) {
         level.setBlock(base.offset(-radiusX, 2, z), pipe, 3);
         level.setBlock(base.offset(radiusX, 2, z), pipe, 3);
      }
      for (int x = -radiusX + 2; x <= radiusX - 2; x++) {
         if (x % 2 == 0) {
            level.setBlock(base.offset(x, 1, 0), ModBlocks.CONVEYOR_FLOOR.get().defaultBlockState(), 3);
         }
      }
      level.setBlock(base.offset(0, 1, -radiusZ), warning, 3);
      level.setBlock(base.offset(1, 1, -radiusZ), warning, 3);
      level.setBlock(base.offset(-1, 1, -radiusZ), warning, 3);
      level.setBlock(base.offset(0, 2, 0), type == IndustrialPoiType.NEXUS_HEAT_EXCHANGER_RUINS ? ModBlocks.ECHO_MONITOR.get().defaultBlockState() : light, 3);
      level.setBlock(base.offset(radiusX - 2, 2, -radiusZ + 2), variant == 2 ? emergency : light, 3);
      level.setBlock(base.offset(-radiusX + 2, 2, radiusZ - 2), type == IndustrialPoiType.REACTOR_COOLING_STATION ? emergency : light, 3);
      level.setBlock(base.offset(radiusX - 1, 1, 0), ModBlocks.HAZARD_DOOR.get().defaultBlockState(), 3);
      level.setBlock(base.offset(-radiusX + 1, 1, 0), ModBlocks.MAINTENANCE_HATCH.get().defaultBlockState(), 3);
      buildPartition(level, base, radiusX, radiusZ, variant);
   }

   private static void buildApproachPad(ServerLevel level, BlockPos base, IndustrialPoiType type, int radiusX, int radiusZ, int variant) {
      BlockState approach = type == IndustrialPoiType.GEOTHERMAL_DRILL_SITE
         ? Blocks.MAGMA_BLOCK.defaultBlockState()
         : ModBlocks.WARNING_STRIPE_BLOCK.get().defaultBlockState();
      BlockState grate = ModBlocks.INDUSTRIAL_GRATE.get().defaultBlockState();
      int entryZ = -radiusZ - 1;
      for (int z = entryZ - 5; z <= entryZ; z++) {
         for (int x = -2; x <= 2; x++) {
            level.setBlock(base.offset(x, 0, z), Math.abs(x) == 2 && Math.floorMod(z + variant, 2) == 0 ? approach : grate, 3);
         }
      }
      level.setBlock(base.offset(-3, 1, entryZ - 1), ModBlocks.EMERGENCY_LIGHT.get().defaultBlockState(), 3);
      level.setBlock(base.offset(3, 1, entryZ - 1), ModBlocks.FACTORY_LIGHT.get().defaultBlockState(), 3);
   }

   private static void breakPerimeter(ServerLevel level, BlockPos base, IndustrialPoiType type, int radiusX, int radiusZ, int variant) {
      BlockState rubble = type == IndustrialPoiType.NEXUS_HEAT_EXCHANGER_RUINS
         ? ModBlocks.STATIC_PIPE.get().defaultBlockState()
         : ModBlocks.RUSTED_CATWALK.get().defaultBlockState();
      int southGap = -radiusZ;
      for (int x = -2; x <= 2; x++) {
         level.setBlock(base.offset(x, 1, southGap), Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(base.offset(x, 2, southGap), Blocks.AIR.defaultBlockState(), 3);
      }
      BlockPos[] collapsed = new BlockPos[]{
         base.offset(-radiusX + 1 + variant, 1, -radiusZ + 1),
         base.offset(radiusX - 1, 1, radiusZ - 2 - variant),
         base.offset(-radiusX + 2, 1, radiusZ - 1)
      };
      for (BlockPos pos : collapsed) {
         level.setBlock(pos, rubble, 3);
         level.setBlock(pos.above(), ModBlocks.PIPE_WALL.get().defaultBlockState(), 3);
      }
   }

   private static void placeTypeReadability(ServerLevel level, BlockPos base, IndustrialPoiType type, int radiusX, int radiusZ, int variant) {
      BlockState signal = switch (type) {
         case ABANDONED_THERMAL_PLANT -> ModBlocks.THERMAL_COIL_BLOCK.get().defaultBlockState();
         case RUSTED_FACTORY_COMPLEX -> ModBlocks.CONVEYOR_FLOOR.get().defaultBlockState();
         case GEOTHERMAL_DRILL_SITE -> Blocks.MAGMA_BLOCK.defaultBlockState();
         case REACTOR_COOLING_STATION -> ModBlocks.PRESSURE_GAUGE_BLOCK.get().defaultBlockState();
         case NEXUS_HEAT_EXCHANGER_RUINS -> ModBlocks.NEXUS_SAFE_DUCT.get().defaultBlockState();
      };
      for (int offset = -3; offset <= 3; offset += 3) {
         level.setBlock(base.offset(offset, 1, -radiusZ + 3), signal, 3);
      }
      level.setBlock(base.offset(radiusX - 3, 1, radiusZ - 3), ModBlocks.CONTROL_PANEL.get().defaultBlockState(), 3);
      if (variant == 2) {
         level.setBlock(base.offset(-radiusX + 3, 1, -radiusZ + 3), ModBlocks.BROKEN_MONITOR.get().defaultBlockState(), 3);
      }
   }

   private static void buildPartition(ServerLevel level, BlockPos base, int radiusX, int radiusZ, int variant) {
      BlockState wall = ModBlocks.MACHINE_CASING.get().defaultBlockState();
      int z = variant == 0 ? 3 : -3;
      for (int x = -radiusX + 2; x <= radiusX - 2; x++) {
         if (Math.abs(x) <= 1) {
            continue;
         }
         level.setBlock(base.offset(x, 1, z), wall, 3);
      }
      int x = variant == 2 ? 4 : -4;
      for (int dz = -radiusZ + 2; dz <= radiusZ - 2; dz++) {
         if (Math.abs(dz) <= 1) {
            continue;
         }
         level.setBlock(base.offset(x, 1, dz), ModBlocks.RUSTED_CATWALK.get().defaultBlockState(), 3);
      }
   }

   private static void placeCore(ServerLevel level, BlockPos base, IndustrialPoiType type, int variant) {
      Block block = switch (type) {
         case ABANDONED_THERMAL_PLANT -> ModBlocks.THERMAL_ARRAY.get();
         case RUSTED_FACTORY_COMPLEX -> ModBlocks.SALVAGE_SHREDDER.get();
         case GEOTHERMAL_DRILL_SITE -> ModBlocks.GEOTHERMAL_PUMP.get();
         case REACTOR_COOLING_STATION -> ModBlocks.REACTOR_HEAT_EXCHANGER.get();
         case NEXUS_HEAT_EXCHANGER_RUINS -> ModBlocks.STATIC_HEAT_EXCHANGER.get();
      };
      level.setBlock(base.offset(0, 1, 0), block.defaultBlockState(), 3);
      level.setBlock(base.offset(1, 1, 0), ModBlocks.COPPER_FLUX_DUCT.get().defaultBlockState(), 3);
      level.setBlock(base.offset(2, 1, 0), ModBlocks.FLUX_CAPACITOR_BANK.get().defaultBlockState(), 3);
      if (variant >= 1) {
         level.setBlock(base.offset(-1, 1, 0), ModBlocks.SIGNAL_DUCT.get().defaultBlockState(), 3);
         level.setBlock(base.offset(-2, 1, 0), ModBlocks.CONTROL_PANEL.get().defaultBlockState(), 3);
      }
      if (variant == 2) {
         level.setBlock(base.offset(0, 1, 2), ModBlocks.INDUSTRIAL_SCRUBBER.get().defaultBlockState(), 3);
      }
   }

   private static void placeHazards(ServerLevel level, BlockPos base, IndustrialPoiType type, int radiusX, int radiusZ, int variant) {
      BlockPos[] vents = new BlockPos[]{
         base.offset(-radiusX + 2, 1, 0),
         base.offset(radiusX - 2, 1, 1),
         base.offset(0, 1, radiusZ - 2),
         base.offset(variant - 1, 1, -radiusZ + 2)
      };
      for (BlockPos vent : vents) {
         level.setBlock(vent, ModBlocks.VENT_BLOCK.get().defaultBlockState(), 3);
         level.setBlock(vent.above(), ModBlocks.SMOKE_VENT.get().defaultBlockState(), 3);
      }
      level.setBlock(base.offset(-3, 1, 0), ModBlocks.THERMAL_COIL_BLOCK.get().defaultBlockState(), 3);
      level.setBlock(base.offset(3, 1, -1), ModBlocks.THERMAL_COIL_BLOCK.get().defaultBlockState(), 3);
      if (type == IndustrialPoiType.REACTOR_COOLING_STATION) {
         level.setBlock(base.offset(-1, 1, 2), ModBlocks.SHIELDED_PIPE.get().defaultBlockState(), 3);
         level.setBlock(base.offset(1, 1, 2), ModBlocks.PRESSURE_GAUGE_BLOCK.get().defaultBlockState(), 3);
      } else if (type == IndustrialPoiType.NEXUS_HEAT_EXCHANGER_RUINS) {
         level.setBlock(base.offset(-1, 1, 2), ModBlocks.NEXUS_SAFE_DUCT.get().defaultBlockState(), 3);
         level.setBlock(base.offset(1, 1, 2), ModBlocks.STATIC_PIPE.get().defaultBlockState(), 3);
      } else if (type == IndustrialPoiType.GEOTHERMAL_DRILL_SITE) {
         level.setBlock(base.offset(1, 1, 2), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
      }
   }

   private static void buildWardenArena(ServerLevel level, BlockPos base, int radiusX, int radiusZ) {
      BlockState warning = ModBlocks.WARNING_STRIPE_BLOCK.get().defaultBlockState();
      BlockState grate = ModBlocks.INDUSTRIAL_GRATE.get().defaultBlockState();
      for (int x = -5; x <= 5; x++) {
         for (int z = -5; z <= 5; z++) {
            if (Math.abs(x) == 5 || Math.abs(z) == 5) {
               level.setBlock(base.offset(x, 0, z), warning, 3);
            } else {
               level.setBlock(base.offset(x, 0, z), grate, 3);
            }
         }
      }
      level.setBlock(base.offset(0, 1, -3), ModBlocks.FURNACE_WARDEN_CORE.get().defaultBlockState(), 3);
      level.setBlock(base.offset(4, 1, 0), ModBlocks.COOLING_FAN_BLOCK.get().defaultBlockState(), 3);
      level.setBlock(base.offset(-4, 1, 0), ModBlocks.COOLING_FAN_BLOCK.get().defaultBlockState(), 3);
      level.setBlock(base.offset(0, 1, 4), ModBlocks.COOLING_FAN_BLOCK.get().defaultBlockState(), 3);
      level.setBlock(base.offset(0, 1, -5), ModBlocks.EMERGENCY_LIGHT.get().defaultBlockState(), 3);
      level.setBlock(base.offset(radiusX - 1, 1, -radiusZ + 1), ModBlocks.ECHO_MONITOR.get().defaultBlockState(), 3);
   }

   private static void placeLoot(ServerLevel level, BlockPos pos, IndustrialPoiType type, RandomSource random) {
      level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
      if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
         ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "chests/" + type.id()));
         chest.setLootTable(key, random.nextLong());
      }
      if (type == IndustrialPoiType.ABANDONED_THERMAL_PLANT && level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
         container.setItem(0, new ItemStack(ModItems.FURNACE_WARDEN_WAKE_CORE.get()));
      } else if (type == IndustrialPoiType.REACTOR_COOLING_STATION && level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
         container.setItem(0, new ItemStack(ModItems.RADIATION_SHIELDING_UPGRADE.get()));
      } else if (type == IndustrialPoiType.NEXUS_HEAT_EXCHANGER_RUINS && level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
         container.setItem(0, new ItemStack(ModItems.NEXUS_STABILIZER_UPGRADE.get()));
      }
   }
}

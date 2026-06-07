package com.knoxhack.echoblackboxprotocol.world;

import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonSeeder {
   private static final int ROOM_HEIGHT = 5;
   private static final long ENCOUNTER_RESPAWN_COOLDOWN_TICKS = 200L;
   private static final Map<String, Long> RECENT_ENCOUNTER_SPAWNS = new ConcurrentHashMap<>();

   private DungeonSeeder() {
   }

   public static void seed(ServerLevel level, BlockPos center, BlackboxDungeon dungeon) {
      BlockState floor = floor(dungeon);
      BlockState accent = accent(dungeon);
      carveRoom(level, center, 8, 8, floor, accent, dungeon == BlackboxDungeon.CORE_CHAMBER);
      BlockPos north = center.north(18);
      BlockPos south = center.south(18);
      BlockPos east = center.east(18);
      BlockPos west = center.west(18);
      carveRoom(level, north, 9, 7, floor, accent, false);
      carveRoom(level, south, 9, 7, floor, accent, dungeon == BlackboxDungeon.CORE_CHAMBER);
      carveRoom(level, east, 7, 9, floor, accent, false);
      carveRoom(level, west, 7, 9, floor, accent, false);
      carveCorridor(level, center.north(8), north.south(7), floor, accent);
      carveCorridor(level, center.south(8), south.north(7), floor, accent);
      carveCorridor(level, center.east(8), east.west(7), floor, accent);
      carveCorridor(level, center.west(8), west.east(7), floor, accent);
      level.setBlockAndUpdate(center, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      polishRoomAnchors(level, center, north, south, east, west, dungeon);
      switch (dungeon) {
         case VAULT:
            seedVault(level, center, north, south, east, west);
            break;
         case BUNKER:
            seedBunker(level, center, north, south, east, west);
            break;
         case LABYRINTH:
            seedLabyrinth(level, center, north, south, east, west);
            break;
         case TEMPLE:
            seedTemple(level, center, north, south, east, west);
            break;
         case CORE_CHAMBER:
            seedCoreChamber(level, center, north, south, east, west);
      }
   }

   private static void seedVault(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west) {
      machine(level, north, ((Block)ModBlocks.ARCHIVE_TERMINAL.get()).defaultBlockState());
      machine(level, east, ((Block)ModBlocks.BLACKBOX_DECODER.get()).defaultBlockState());
      machine(level, east.south(3), ((Block)ModBlocks.MEMORY_PROJECTOR.get()).defaultBlockState());
      machine(level, west, ((Block)ModBlocks.MEMORY_STABILIZER.get()).defaultBlockState());
      bars(level, west, 5);
      dataCore(level, south, 6);
      pillars(level, center, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      corruptedLobby(level, center);
      serverHall(level, east.east(15));
      containmentCells(level, west.west(14));
      archiveLock(level, north.south(4), 7);
      spawnEncounter(level, center.offset(6, 1, -5), (EntityType<?>)ModEntities.ARCHIVE_HUSK.get());
      spawnEncounter(level, east.east(12).above(), (EntityType<?>)ModEntities.SECURITY_ECHO.get());
      spawnEncounter(level, west.west(11).above(), (EntityType<?>)ModEntities.ARCHIVE_HUSK.get());
      cache(level, north.west(5), new ItemStack(ModItems.PERSONAL_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.SECURITY_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.STATIC_FLUID.get()), new ItemStack(ModItems.BLACK_METAL.get(), 2));
      cache(level, south.east(5), new ItemStack(ModItems.ECHO_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.CORE_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.CORRUPTED_FERRITE.get(), 2), new ItemStack(ModItems.STATIC_FLUID.get()));
   }

   private static void seedBunker(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west) {
      machine(level, north, ((Block)ModBlocks.PROTOCOL_EXTRACTOR.get()).defaultBlockState());
      machine(level, south, ((Block)ModBlocks.ARCHIVE_TERMINAL.get()).defaultBlockState());
      turrets(level, east);
      bars(level, west, 7);
      line(level, center.west(5), center.east(5), Blocks.RED_CONCRETE.defaultBlockState());
      pillars(level, center, Blocks.RED_CONCRETE.defaultBlockState());
      lockdownDoor(level, north.south(4), 7);
      turretLane(level, center.east(12));
      commandTable(level, south);
      spawnEncounter(level, east.east(7).above(), (EntityType<?>)ModEntities.COMMAND_REMNANT_MINION.get());
      spawnEncounter(level, west.west(7).above(), (EntityType<?>)ModEntities.BLACKBOX_SENTINEL.get());
      spawnEncounter(level, south.north(4).above(), (EntityType<?>)ModEntities.COMMAND_REMNANT_MINION.get());
      cache(level, east.north(4), new ItemStack(ModItems.COMMAND_BLACKBOX_FRAGMENT.get(), 2), new ItemStack(ModItems.BLACK_METAL.get(), 3), new ItemStack(ModItems.CORRUPTED_FERRITE.get(), 2));
      cache(level, west.south(4), new ItemStack(ModItems.CORE_ACCESS_KEY_MATRIX.get()), new ItemStack(ModItems.STATIC_FLUID.get(), 2), new ItemStack(ModItems.BLACK_METAL.get(), 2));
   }

   private static void seedLabyrinth(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west) {
      machine(level, north, ((Block)ModBlocks.MEMORY_PROJECTOR.get()).defaultBlockState());
      machine(level, west, ((Block)ModBlocks.MEMORY_STABILIZER.get()).defaultBlockState());
      falseExit(level, south);
      falseExit(level, east);
      line(level, center.north(5).west(5), center.south(5).east(5), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      line(level, center.north(5).east(5), center.south(5).west(5), Blocks.PURPLE_CONCRETE.defaultBlockState());
      mirroredCorridor(level, west.west(13));
      unstableMemoryPath(level, center.south(10));
      falseExit(level, north.north(12));
      spawnEncounter(level, center.offset(5, 1, 6), (EntityType<?>)ModEntities.MEMORY_PARASITE.get());
      spawnEncounter(level, west.west(10).above(), (EntityType<?>)ModEntities.FALSE_ECHO_MINION.get());
      spawnEncounter(level, south.south(8).above(), (EntityType<?>)ModEntities.MEMORY_PARASITE.get());
      cache(level, north.east(5), new ItemStack(ModItems.ECHO_BLACKBOX_FRAGMENT.get(), 2), new ItemStack(ModItems.STATIC_FLUID.get(), 2), new ItemStack(ModItems.CORRUPTED_FERRITE.get()));
      cache(level, south.west(5), new ItemStack(ModItems.DELETED_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.CORRUPTED_FERRITE.get(), 3), new ItemStack(ModItems.STATIC_FLUID.get()));
   }

   private static void seedTemple(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west) {
      machine(level, north, ((Block)ModBlocks.CORE_KEY_ASSEMBLER.get()).defaultBlockState());
      machine(level, east, ((Block)ModBlocks.PROTOCOL_EXTRACTOR.get()).defaultBlockState());
      machine(level, west, ((Block)ModBlocks.MEMORY_STABILIZER.get()).defaultBlockState());
      beam(level, center, 6);
      dataCore(level, south, 5);
      machineTempleLanes(level, center, north, east, west);
      coreSeal(level, south, 6);
      sentinelPosts(level, center, 7);
      spawnEncounter(level, north.south(5).above(), (EntityType<?>)ModEntities.BLACKBOX_SENTINEL.get());
      spawnEncounter(level, east.west(4).above(), (EntityType<?>)ModEntities.SECURITY_ECHO.get());
      cache(level, east.south(5), new ItemStack(ModItems.CORE_BLACKBOX_FRAGMENT.get(), 2), new ItemStack(ModItems.CORE_ACCESS_KEY_LEFT.get()), new ItemStack(ModItems.STATIC_FLUID.get()));
      cache(level, west.north(5), new ItemStack(ModItems.DELETED_BLACKBOX_FRAGMENT.get()), new ItemStack(ModItems.CORE_ACCESS_KEY_RIGHT.get()), new ItemStack(ModItems.CORRUPTED_FERRITE.get(), 2));
   }

   private static void seedCoreChamber(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west) {
      machine(level, south, ((Block)ModBlocks.TRUTH_ENGINE.get()).defaultBlockState());
      machine(level, north, ((Block)ModBlocks.ARCHIVE_TERMINAL.get()).defaultBlockState());
      machine(level, east, ((Block)ModBlocks.MEMORY_PROJECTOR.get()).defaultBlockState());
      machine(level, west, ((Block)ModBlocks.MEMORY_STABILIZER.get()).defaultBlockState());
      ring(level, center, 4, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      ring(level, center, 7, ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
      beam(level, center, 8);
      guardianArena(level, center);
      endingApproach(level, south);
      rotatingCoreRings(level, center);
      spawnEncounter(level, center.north(9).above(), (EntityType<?>)ModEntities.BLACKBOX_SENTINEL.get());
      spawnEncounter(level, center.south(9).above(), (EntityType<?>)ModEntities.SECURITY_ECHO.get());
      cache(level, north.west(5), new ItemStack(ModItems.RESTORE_DIRECTIVE.get()), new ItemStack(ModItems.CONTROL_DIRECTIVE.get()), new ItemStack(ModItems.DESTROY_DIRECTIVE.get()), new ItemStack(ModItems.STATIC_FLUID.get(), 2));
      cache(level, south.east(5), new ItemStack(ModItems.DELETED_BLACKBOX_FRAGMENT.get(), 2), new ItemStack(ModItems.MERGE_DIRECTIVE.get()), new ItemStack(ModItems.BLACK_METAL.get(), 3));
   }

   private static void carveRoom(ServerLevel level, BlockPos center, int halfX, int halfZ, BlockState floor, BlockState wall, boolean glassRoof) {
      for (int x = -halfX; x <= halfX; x++) {
         for (int z = -halfZ; z <= halfZ; z++) {
            BlockPos base = center.offset(x, 0, z);
            boolean edge = Math.abs(x) == halfX || Math.abs(z) == halfZ;
            setDungeonBlock(level, base, floor);

            for (int y = 1; y <= 5; y++) {
               setDungeonBlock(level, base.above(y), edge ? wall : Blocks.AIR.defaultBlockState());
            }

            if (glassRoof) {
               setDungeonBlock(level, base.above(6), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
            }
         }
      }
   }

   private static void carveCorridor(ServerLevel level, BlockPos from, BlockPos to, BlockState floor, BlockState wall) {
      int minX = Math.min(from.getX(), to.getX());
      int maxX = Math.max(from.getX(), to.getX());
      int minZ = Math.min(from.getZ(), to.getZ());
      int maxZ = Math.max(from.getZ(), to.getZ());

      for (int x = minX - 2; x <= maxX + 2; x++) {
         for (int z = minZ - 2; z <= maxZ + 2; z++) {
            BlockPos base = new BlockPos(x, from.getY(), z);
            boolean edge = x == minX - 2 || x == maxX + 2 || z == minZ - 2 || z == maxZ + 2;
            setDungeonBlock(level, base, floor);

            for (int y = 1; y <= 3; y++) {
               setDungeonBlock(level, base.above(y), edge ? wall : Blocks.AIR.defaultBlockState());
            }
         }
      }
   }

   private static void machine(ServerLevel level, BlockPos pos, BlockState state) {
      BlockPos machinePos = pos.above();
      if (!(level.getBlockEntity(machinePos) instanceof BlackboxMachineBlockEntity)) {
         level.setBlockAndUpdate(machinePos, state);
      }

      level.setBlockAndUpdate(pos.above(2), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
   }

   private static void polishRoomAnchors(ServerLevel level, BlockPos center, BlockPos north, BlockPos south, BlockPos east, BlockPos west, BlackboxDungeon dungeon) {
      BlockState signal = ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState();
      BlockState dark = ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState();
      for (BlockPos room : new BlockPos[]{north, south, east, west}) {
         for (BlockPos marker : new BlockPos[]{room.north(5), room.south(5), room.east(5), room.west(5)}) {
            setDungeonBlock(level, marker.above(), signal);
         }
      }
      for (int offset = -6; offset <= 6; offset += 3) {
         setDungeonBlock(level, center.offset(offset, 1, 0), signal);
         setDungeonBlock(level, center.offset(0, 1, offset), signal);
      }
      if (dungeon == BlackboxDungeon.CORE_CHAMBER || dungeon == BlackboxDungeon.TEMPLE) {
         ring(level, center.above(1), 5, signal);
      } else {
         setDungeonBlock(level, center.north(6).above(), dark);
         setDungeonBlock(level, center.south(6).above(), dark);
      }
   }

   private static void cache(ServerLevel level, BlockPos pos, ItemStack... stacks) {
      BlockPos chestPos = pos.above();
      if (level.getBlockEntity(chestPos) instanceof Container) {
         return;
      }

      level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
      if (level.getBlockEntity(chestPos) instanceof Container container) {
         for (int i = 0; i < stacks.length && i < container.getContainerSize(); i++) {
            container.setItem(i, stacks[i].copy());
         }
      }
   }

   private static void pillars(ServerLevel level, BlockPos center, BlockState state) {
      for (BlockPos base : new BlockPos[]{center.offset(5, 0, 5), center.offset(-5, 0, 5), center.offset(5, 0, -5), center.offset(-5, 0, -5)}) {
         for (int y = 1; y <= 4; y++) {
            setDungeonBlock(level, base.above(y), state);
         }
      }
   }

   private static void bars(ServerLevel level, BlockPos center, int width) {
      for (int offset = -width; offset <= width; offset += 2) {
         for (int y = 1; y <= 3; y++) {
            setDungeonBlock(level, center.offset(offset, y, 3), Blocks.IRON_BARS.defaultBlockState());
         }
      }
   }

   private static void turrets(ServerLevel level, BlockPos center) {
      for (BlockPos base : new BlockPos[]{center.offset(4, 0, 4), center.offset(-4, 0, 4), center.offset(4, 0, -4), center.offset(-4, 0, -4)}) {
         dispenser(level, base.above(), new ItemStack(Items.ARROW, 12));
         setDungeonBlock(level, base.above(2), Blocks.REDSTONE_BLOCK.defaultBlockState());
      }
   }

   private static void falseExit(ServerLevel level, BlockPos center) {
      for (int x = -2; x <= 2; x++) {
         for (int y = 1; y <= 4; y++) {
            setDungeonBlock(level, center.offset(x, y, 7), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
         }
      }
   }

   private static void dataCore(ServerLevel level, BlockPos center, int radius) {
      ring(level, center, radius, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());

      for (int y = 1; y <= 4; y++) {
         setDungeonBlock(
            level,
            center.above(y),
            y % 2 == 0 ? ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState() : ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState()
         );
      }
   }

   private static void beam(ServerLevel level, BlockPos center, int height) {
      for (int y = 1; y <= height; y++) {
         setDungeonBlock(level, center.above(y), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      }
   }

   private static void ring(ServerLevel level, BlockPos center, int radius, BlockState state) {
      for (int x = -radius; x <= radius; x++) {
         for (int z = -radius; z <= radius; z++) {
            int distance = Math.abs(x) + Math.abs(z);
            if (distance == radius || distance == radius + 1) {
               setDungeonBlock(level, center.offset(x, 1, z), state);
            }
         }
      }
   }

   private static void line(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
      int steps = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));

      for (int i = 0; i <= steps; i++) {
         int x = from.getX() + (to.getX() - from.getX()) * i / Math.max(1, steps);
         int z = from.getZ() + (to.getZ() - from.getZ()) * i / Math.max(1, steps);
         setDungeonBlock(level, new BlockPos(x, from.getY() + 1, z), state);
      }
   }

   public static boolean spawnEncounter(ServerLevel level, BlockPos pos, EntityType<?> type) {
      AABB box = new AABB(pos).inflate(10.0);
      boolean nearby = !level.getEntitiesOfClass(Entity.class, box, entity -> entity.getType() == type).isEmpty();
      String key = encounterKey(level, pos, type);
      long gameTime = level.getGameTime();
      Long lastSpawn = RECENT_ENCOUNTER_SPAWNS.get(key);
      if (nearby || lastSpawn != null && gameTime - lastSpawn < ENCOUNTER_RESPAWN_COOLDOWN_TICKS) {
         return false;
      }

      Entity entity = type.create(level, EntitySpawnReason.EVENT);
      if (entity == null) {
         return false;
      }

      entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      boolean spawned = level.addFreshEntity(entity);
      if (spawned) {
         RECENT_ENCOUNTER_SPAWNS.put(key, gameTime);
      }

      return spawned;
   }

   private static String encounterKey(ServerLevel level, BlockPos pos, EntityType<?> type) {
      return System.identityHashCode(level) + "|" + type + "|" + (pos.getX() >> 3) + "|" + (pos.getY() >> 3) + "|" + (pos.getZ() >> 3);
   }

   private static void corruptedLobby(ServerLevel level, BlockPos center) {
      for (BlockPos pos : new BlockPos[]{center.north(3).west(3), center.north(2).east(4), center.south(4).west(1), center.east(3).south(2)}) {
         setDungeonBlock(level, pos.above(), Blocks.COBWEB.defaultBlockState());
         setDungeonBlock(level, pos, Blocks.PURPLE_CONCRETE.defaultBlockState());
      }
   }

   private static void serverHall(ServerLevel level, BlockPos center) {
      carveRoom(level, center, 5, 10, ((Block)ModBlocks.CORE_BRICK.get()).defaultBlockState(), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState(), false);
      for (int z = -7; z <= 7; z += 2) {
         for (int y = 1; y <= 3; y++) {
            setDungeonBlock(level, center.offset(-3, y, z), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
            setDungeonBlock(level, center.offset(3, y, z), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
         }
      }
      line(level, center.north(9), center.south(9), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
   }

   private static void containmentCells(ServerLevel level, BlockPos center) {
      carveRoom(level, center, 9, 5, ((Block)ModBlocks.CORE_BRICK.get()).defaultBlockState(), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState(), false);
      for (int x = -6; x <= 6; x += 6) {
         for (int z = -3; z <= 3; z += 6) {
            BlockPos cell = center.offset(x, 0, z);
            bars(level, cell, 3);
            setDungeonBlock(level, cell.above(), Blocks.COBWEB.defaultBlockState());
         }
      }
   }

   private static void archiveLock(ServerLevel level, BlockPos center, int width) {
      for (int x = -width; x <= width; x++) {
         setDungeonBlock(level, center.offset(x, 1, 0), x % 2 == 0 ? Blocks.IRON_BARS.defaultBlockState() : ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      }
   }

   private static void lockdownDoor(ServerLevel level, BlockPos center, int width) {
      for (int x = -width; x <= width; x++) {
         for (int y = 1; y <= 3; y++) {
            setDungeonBlock(level, center.offset(x, y, 0), x % 3 == 0 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.RED_CONCRETE.defaultBlockState());
         }
      }
   }

   private static void turretLane(ServerLevel level, BlockPos center) {
      carveRoom(level, center, 5, 12, ((Block)ModBlocks.CORE_BRICK.get()).defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState(), false);
      for (int z = -8; z <= 8; z += 4) {
         dispenser(level, center.offset(-4, 1, z), new ItemStack(Items.ARROW, 8));
         dispenser(level, center.offset(4, 1, z), new ItemStack(Items.FIRE_CHARGE, 3));
         setDungeonBlock(level, center.offset(0, 1, z), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
         setDungeonBlock(level, center.offset(0, 0, z), Blocks.REDSTONE_BLOCK.defaultBlockState());
      }
   }

   private static void commandTable(ServerLevel level, BlockPos center) {
      for (int x = -3; x <= 3; x++) {
         for (int z = -1; z <= 1; z++) {
            setDungeonBlock(level, center.offset(x, 1, z), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
         }
      }
      setDungeonBlock(level, center.above(2), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
   }

   private static void mirroredCorridor(ServerLevel level, BlockPos center) {
      carveRoom(level, center, 4, 14, ((Block)ModBlocks.CORRUPTED_FERRITE_BLOCK.get()).defaultBlockState(), Blocks.PURPLE_CONCRETE.defaultBlockState(), false);
      for (int z = -12; z <= 12; z += 3) {
         setDungeonBlock(level, center.offset(-2, 1, z), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
         setDungeonBlock(level, center.offset(2, 1, z), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      }
   }

   private static void unstableMemoryPath(ServerLevel level, BlockPos center) {
      for (int z = -8; z <= 8; z++) {
         BlockState state = Math.floorMod(z, 3) == 0 ? Blocks.COBWEB.defaultBlockState() : ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState();
         setDungeonBlock(level, center.offset(Math.floorMod(z, 5) - 2, 1, z), state);
      }
   }

   private static void machineTempleLanes(ServerLevel level, BlockPos center, BlockPos north, BlockPos east, BlockPos west) {
      line(level, center, north, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      line(level, center, east, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      line(level, center, west, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      for (BlockPos lane : new BlockPos[]{north.south(5), east.west(5), west.east(5)}) {
         beam(level, lane, 5);
      }
   }

   private static void coreSeal(ServerLevel level, BlockPos center, int radius) {
      ring(level, center, radius, ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      ring(level, center, radius + 3, ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
   }

   private static void sentinelPosts(ServerLevel level, BlockPos center, int radius) {
      for (BlockPos post : new BlockPos[]{center.offset(radius, 0, radius), center.offset(-radius, 0, radius), center.offset(radius, 0, -radius), center.offset(-radius, 0, -radius)}) {
         for (int y = 1; y <= 3; y++) {
            setDungeonBlock(level, post.above(y), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
         }
         setDungeonBlock(level, post.above(4), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
      }
   }

   private static void guardianArena(ServerLevel level, BlockPos center) {
      ring(level, center, 10, Blocks.RED_CONCRETE.defaultBlockState());
      for (int offset = -8; offset <= 8; offset += 4) {
         setDungeonBlock(level, center.offset(offset, 1, -10), Blocks.IRON_BARS.defaultBlockState());
         setDungeonBlock(level, center.offset(offset, 1, 10), Blocks.IRON_BARS.defaultBlockState());
         setDungeonBlock(level, center.offset(-10, 1, offset), Blocks.IRON_BARS.defaultBlockState());
         setDungeonBlock(level, center.offset(10, 1, offset), Blocks.IRON_BARS.defaultBlockState());
      }
   }

   private static void endingApproach(ServerLevel level, BlockPos center) {
      for (int z = -5; z <= 5; z++) {
         setDungeonBlock(level, center.offset(0, 1, z), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState());
         if (Math.abs(z) % 2 == 0) {
            setDungeonBlock(level, center.offset(-2, 1, z), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
            setDungeonBlock(level, center.offset(2, 1, z), ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState());
         }
      }
   }

   private static void rotatingCoreRings(ServerLevel level, BlockPos center) {
      for (int radius = 3; radius <= 9; radius += 3) {
         BlockState state = radius % 2 == 0 ? ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState() : ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState();
         ring(level, center.above(radius / 3), radius, state);
      }
   }

   private static void setDungeonBlock(ServerLevel level, BlockPos pos, BlockState state) {
      if (level.getBlockEntity(pos) instanceof Container || level.getBlockEntity(pos) instanceof BlackboxMachineBlockEntity) {
         return;
      }

      level.setBlockAndUpdate(pos, state);
   }

   private static void dispenser(ServerLevel level, BlockPos pos, ItemStack ammo) {
      if (level.getBlockEntity(pos) instanceof Container container) {
         if (container.getItem(0).isEmpty()) {
            container.setItem(0, ammo.copy());
         }
         return;
      }

      setDungeonBlock(level, pos, Blocks.DISPENSER.defaultBlockState());
      if (level.getBlockEntity(pos) instanceof Container container) {
         container.setItem(0, ammo.copy());
      }
   }

   private static BlockState floor(BlackboxDungeon dungeon) {
      return switch (dungeon) {
         case VAULT, BUNKER -> ((Block)ModBlocks.CORE_BRICK.get()).defaultBlockState();
         case LABYRINTH -> ((Block)ModBlocks.CORRUPTED_FERRITE_BLOCK.get()).defaultBlockState();
         case TEMPLE, CORE_CHAMBER -> ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState();
      };
   }

   private static BlockState accent(BlackboxDungeon dungeon) {
      return switch (dungeon) {
         case BUNKER -> Blocks.RED_CONCRETE.defaultBlockState();
         case LABYRINTH -> Blocks.PURPLE_CONCRETE.defaultBlockState();
         default -> ((Block)ModBlocks.BLACK_METAL_BLOCK.get()).defaultBlockState();
         case CORE_CHAMBER -> ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState();
      };
   }
}

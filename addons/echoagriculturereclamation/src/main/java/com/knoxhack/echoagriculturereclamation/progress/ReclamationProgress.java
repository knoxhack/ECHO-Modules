package com.knoxhack.echoagriculturereclamation.progress;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.block.ReclamationCropBlock;
import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionHooks;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModDataComponents;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ReclamationProgress {
   public static final String ROOT = "echoagriculturereclamation_progress";
   private static final int GROWTH_GREENHOUSE_CACHE_TICKS = 20;
   private static final int GROWTH_GREENHOUSE_CACHE_PRUNE_TICKS = 200;
   private static final int GREENHOUSE_STRAINED_MARGIN = 20;
   private static final int GREENHOUSE_STRAINED_GROWTH_PENALTY = 5;
   private static final int GREENHOUSE_UNSAFE_GROWTH_PENALTY = 12;
   private static final int GREENHOUSE_STRAINED_SEED_PENALTY = 10;
   private static final int GREENHOUSE_UNSAFE_SEED_PENALTY = 25;
   private static final Map<GrowthGreenhouseCacheKey, GrowthGreenhouseCacheEntry> GROWTH_GREENHOUSE_CACHE = new HashMap<>();
   private static long lastGrowthGreenhouseCachePruneTick = Long.MIN_VALUE;

   private ReclamationProgress() {
   }

   public static CompoundTag data(Player player) {
      if (player == null) {
         return new CompoundTag();
      }
      CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
      player.getPersistentData().put(ROOT, root);
      return root;
   }

   public static Set<String> knownSeeds(Player player) {
      CompoundTag data = data(player);
      int count = data.getIntOr("known_seed_count", 0);
      LinkedHashSet<String> seeds = new LinkedHashSet<>();
      for (int index = 0; index < count; index++) {
         String value = data.getStringOr("known_seed_" + index, "");
         if (!value.isBlank()) {
            seeds.add(value);
         }
      }
      return seeds;
   }

   public static boolean discoverSeed(Player player, CropSpec spec) {
      LinkedHashSet<String> seeds = new LinkedHashSet<>(knownSeeds(player));
      boolean changed = seeds.add(spec.path());
      writeKnownSeeds(player, seeds);
      mark(player, "seed_recovered");
      mark(player, "seed_analyzed");
      max(player, "known_seed_total", seeds.size());
      discoverRoutes(player);
      return changed;
   }

   public static void writeKnownSeeds(Player player, Set<String> seeds) {
      CompoundTag data = data(player);
      int index = 0;
      for (String seed : seeds) {
         data.putString("known_seed_" + index++, seed);
      }
      data.putInt("known_seed_count", index);
   }

   public static void mark(Player player, String flag) {
      boolean first = !flag(player, flag);
      data(player).putBoolean(flag, true);
      if (first) {
         recordCoreMilestone(player, flag);
         ReclamationMissionHooks.recordFlag(player, flag);
         ReclamationCrossAddonIntegration.markTutorialProgress(player, flag);
      }
   }

   public static boolean flag(Player player, String flag) {
      return data(player).getBoolean(flag).orElse(false);
   }

   public static int value(Player player, String key) {
      return data(player).getIntOr(key, 0);
   }

   public static void add(Player player, String key, int amount) {
      if (amount <= 0) {
         return;
      }
      CompoundTag data = data(player);
      data.putInt(key, Math.min(2_000_000_000, data.getIntOr(key, 0) + amount));
      ReclamationMissionHooks.recordCounter(player, key, amount);
   }

   public static void max(Player player, String key, int value) {
      CompoundTag data = data(player);
      data.putInt(key, Math.max(data.getIntOr(key, 0), value));
   }

   public static boolean claimed(Player player, String id) {
      return flag(player, "claimed_" + id);
   }

   public static void claim(Player player, String id) {
      mark(player, "claimed_" + id);
      discoverRoutes(player);
   }

   public static void recordGrowth(Player player, CropSpec spec, boolean stabilized) {
      mark(player, "first_growth");
      add(player, "crops_grown", 1);
      if (stabilized) {
         max(player, "crop_stability", 100);
      } else {
         max(player, "crop_stability", cropStability(player));
      }
      max(player, "food_security", foodSecurity(player));
      int restorationWeight = ReclamationContent.crop(spec).restorationWeight();
      if (restorationWeight > 1) {
         add(player, "restoration_crop_growth", restorationWeight);
      }
      discoverRoutes(player);
   }

   public static boolean needsStabilizationSeed(Player player) {
      return !flag(player, "gene_stabilization") && !flag(player, "stabilization_seed_recovered");
   }

   public static void recordStabilizationSeed(Player player) {
      mark(player, "stabilization_seed_recovered");
      discoverRoutes(player);
   }

   public static void recordStabilization(Player player) {
      mark(player, "gene_stabilization");
      max(player, "crop_stability", 100);
      add(player, "stabilized_seeds", 1);
      discoverRoutes(player);
   }

   public static ReclamationMetrics metrics(Player player) {
      if (player == null) {
         return new ReclamationMetrics(0, SoilState.DEAD, 0, 0, 0, 0);
      }
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      SoilState soil = detectSoil(level, pos);
      GreenhouseContext greenhouse = greenhouseContext(level, pos);
      int stability = cropStability(player);
      int food = foodSecurity(player);
      int restoration = 0;
      if (level instanceof ServerLevel serverLevel) {
         ReclamationWorldData world = ReclamationWorldData.get(serverLevel);
         ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
         restoration = world.restorationScore(chunk);
         world.setGreenhouseSafety(chunk, greenhouse.score());
         world.setLastSoilState(chunk, soil.displayName());
      }
      max(player, "greenhouse_safety", greenhouse.score());
      max(player, "crop_stability", stability);
      max(player, "food_security", food);
      max(player, "restoration_score", restoration);
      return new ReclamationMetrics(knownSeeds(player).size(), soil, greenhouse.score(), stability, food, restoration);
   }

   public static SoilState detectSoil(Level level, BlockPos center) {
      SoilState best = SoilState.DEAD;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 1, 2))) {
         SoilState state = SoilState.fromBlock(level.getBlockState(pos));
         if (state.ordinal() > best.ordinal()) {
            best = state;
         }
      }
      return best;
   }

   public static int scanGreenhouseSafety(Level level, BlockPos center) {
      return scanGreenhouse(level, center).score();
   }

   public static GreenhouseContext greenhouseContext(Level level, BlockPos center) {
      GreenhouseScan live = scanGreenhouse(level, center);
      if (!(level instanceof ServerLevel serverLevel)) {
         return GreenhouseContext.unregistered(live);
      }
      ReclamationWorldData.GreenhouseZoneProfile zone = savedGreenhouseZone(serverLevel, center);
      if (zone == null) {
         return GreenhouseContext.unregistered(live);
      }
      boolean controllerPresent = serverLevel.getBlockState(zone.controllerPos()).is(ModBlocks.GREENHOUSE_CONTROLLER.get());
      GreenhouseScan zoneScan = controllerPresent ? scanGreenhouse(serverLevel, zone.controllerPos()) : GreenhouseScan.empty();
      int effectiveScore = controllerPresent ? Math.min(zone.score(), zoneScan.score()) : 0;
      return GreenhouseContext.established(zoneScan, zone, effectiveScore, zoneScan.score());
   }

   public static int growthGreenhouseSafety(Level level, BlockPos center) {
      return growthGreenhouseContext(level, center).score();
   }

   public static GreenhouseContext growthGreenhouseContext(Level level, BlockPos center) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return greenhouseContext(level, center);
      }
      long gameTime = serverLevel.getGameTime();
      GrowthGreenhouseCacheKey key = GrowthGreenhouseCacheKey.of(serverLevel, center);
      GrowthGreenhouseCacheEntry cached = GROWTH_GREENHOUSE_CACHE.get(key);
      if (cached != null && gameTime >= cached.gameTime() && gameTime - cached.gameTime() <= GROWTH_GREENHOUSE_CACHE_TICKS) {
         return cached.context();
      }

      GreenhouseContext context = greenhouseContext(serverLevel, center);
      GROWTH_GREENHOUSE_CACHE.put(key, new GrowthGreenhouseCacheEntry(context, gameTime));
      pruneGrowthGreenhouseCache(gameTime);
      return context;
   }

   public static void clearGrowthGreenhouseSafetyCacheForTests() {
      GROWTH_GREENHOUSE_CACHE.clear();
      lastGrowthGreenhouseCachePruneTick = Long.MIN_VALUE;
   }

   public static int growthGreenhouseSafetyCacheSizeForTests() {
      return GROWTH_GREENHOUSE_CACHE.size();
   }

   public static ReclamationWorldData.GreenhouseZoneProfile recordGreenhouseZone(ServerLevel level, BlockPos controller, GreenhouseScan scan) {
      ReclamationWorldData.GreenhouseZoneProfile profile = new ReclamationWorldData.GreenhouseZoneProfile(
         scan.score(),
         scan.supportScore(),
         scan.enclosureScore(),
         scan.glass(),
         scan.filters(),
         scan.activeDocks(),
         scan.idleDocks(),
         scan.cropTargets(),
         scan.deployedDrones(),
         scan.serviceTargets(),
         scan.enclosed(),
         scan.greenhouseRoof(),
         scan.floor(),
         controller.getX(),
         controller.getY(),
         controller.getZ(),
         level.getGameTime()
      );
      ReclamationWorldData.get(level).setGreenhouseZone(chunkPos(controller), profile);
      ReclamationWorldData.get(level).recordFieldHistory(
         chunkPos(controller),
         level.getGameTime(),
         GreenhouseContext.established(scan, profile, scan.score(), scan.score()).qualityLabel(),
         0,
         GreenhouseContext.established(scan, profile, scan.score(), scan.score()).nextAction()
      );
      clearGrowthGreenhouseSafetyCacheForTests();
      return profile;
   }

   public static GreenhouseScan scanGreenhouse(Level level, BlockPos center) {
      int glass = 0;
      int filters = 0;
      int activeDocks = 0;
      int idleDocks = 0;
      int controllers = 0;
      int trays = 0;
      int cropTargets = 0;
      int deployedDrones = 0;
      int serviceTargets = 0;
      var rules = ReclamationContent.machines();
      BlockPos min = center.offset(-rules.greenhouseHorizontalRange(), -rules.greenhouseDownRange(), -rules.greenhouseHorizontalRange());
      BlockPos max = center.offset(rules.greenhouseHorizontalRange(), rules.greenhouseUpRange(), rules.greenhouseHorizontalRange());
      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         var block = level.getBlockState(pos).getBlock();
         if (block == ModBlocks.GREENHOUSE_GLASS.get()) {
            glass++;
         } else if (block == ModBlocks.SPORE_FILTER.get()) {
            filters++;
         } else if (block == ModBlocks.POLLINATOR_DRONE_DOCK.get()) {
            if (pollinationTargets(level, pos) > 0) {
               activeDocks++;
            } else {
               idleDocks++;
            }
            serviceTargets += pollinationServiceTargets(level, pos);
            deployedDrones += PollinatorDroneEntity.boundDroneCount(level, pos);
         } else if (block == ModBlocks.GREENHOUSE_CONTROLLER.get()) {
            controllers++;
         } else if (block == ModBlocks.HYDROPONIC_TRAY.get()) {
            trays++;
            cropTargets++;
         } else if (block instanceof ReclamationCropBlock) {
            cropTargets++;
         }
      }

      EnclosureScan enclosure = scanEnclosure(level, center, min, max);
      int supportScore = glass * rules.greenhouseGlassWeight()
         + filters * rules.greenhouseFilterWeight()
         + activeDocks * rules.greenhouseDockWeight()
         + idleDocks * Math.max(1, rules.greenhouseDockWeight() / 3)
         + controllers * rules.greenhouseControllerWeight()
         + trays * rules.greenhouseTrayWeight();
      int enclosureScore = enclosure.enclosed() ? 18 : 0;
      if (enclosure.enclosed() && enclosure.hasGreenhouseRoof()) {
         enclosureScore += 10;
      }
      if (enclosure.enclosed() && enclosure.hasFloor()) {
         enclosureScore += 4;
      }
      if (enclosure.enclosed() && cropTargets > 0) {
         enclosureScore += 3;
      }
      if (enclosure.enclosed() && filters > 0) {
         enclosureScore += 3;
      }
      enclosureScore = Math.min(35, enclosureScore);

      int score = Math.min(100, supportScore + enclosureScore);
      int safeThreshold = ReclamationContent.progression().greenhouseSafeThreshold();
      if (!enclosure.enclosed()) {
         score = Math.min(score, Math.max(0, safeThreshold - 10));
      } else if (!enclosure.hasGreenhouseRoof()) {
         score = Math.min(score, Math.max(0, safeThreshold - 5));
      }
      return new GreenhouseScan(
         score,
         Math.min(100, supportScore),
         enclosureScore,
         glass,
         filters,
         activeDocks,
         idleDocks,
         controllers,
         trays,
         cropTargets,
         deployedDrones,
         serviceTargets,
         enclosure.enclosed(),
         enclosure.hasGreenhouseRoof(),
         enclosure.hasFloor(),
         enclosure.interiorVolume()
      );
   }

   private static ReclamationWorldData.GreenhouseZoneProfile savedGreenhouseZone(ServerLevel level, BlockPos center) {
      ReclamationWorldData world = ReclamationWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      ReclamationWorldData.GreenhouseZoneProfile best = null;
      long bestDistance = Long.MAX_VALUE;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            ReclamationWorldData.GreenhouseZoneProfile candidate = world.greenhouseZone(new ChunkPos(chunk.x() + dx, chunk.z() + dz));
            if (candidate != null && insideSavedZone(candidate, center)) {
               long distance = distanceSquared(candidate.controllerPos(), center);
               if (distance < bestDistance) {
                  best = candidate;
                  bestDistance = distance;
               }
            }
         }
      }
      return best;
   }

   private static boolean insideSavedZone(ReclamationWorldData.GreenhouseZoneProfile zone, BlockPos pos) {
      var rules = ReclamationContent.machines();
      return Math.abs(pos.getX() - zone.controllerX()) <= rules.greenhouseHorizontalRange()
         && Math.abs(pos.getZ() - zone.controllerZ()) <= rules.greenhouseHorizontalRange()
         && pos.getY() >= zone.controllerY() - rules.greenhouseDownRange()
         && pos.getY() <= zone.controllerY() + rules.greenhouseUpRange();
   }

   public static int pollinationTargets(Level level, BlockPos dockPos) {
      return pollinationTargetPositions(level, dockPos).size();
   }

   public static int pollinationServiceTargets(Level level, BlockPos dockPos) {
      int targets = 0;
      for (BlockPos pos : pollinationTargetPositions(level, dockPos)) {
         if (canReceivePollinationService(level, pos)) {
            targets++;
         }
      }
      return targets;
   }

   public static List<BlockPos> pollinationTargetPositions(Level level, BlockPos dockPos) {
      List<BlockPos> targets = new ArrayList<>();
      int radius = ReclamationContent.machines().pollinatorDroneServiceRadius();
      for (BlockPos pos : BlockPos.betweenClosed(dockPos.offset(-radius, -2, -radius), dockPos.offset(radius, 2, radius))) {
         if (isPollinationTarget(level.getBlockState(pos).getBlock())) {
            targets.add(pos.immutable());
         }
      }
      return targets;
   }

   public static boolean canReceivePollinationService(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof ReclamationCropBlock) {
         return state.getValue(ReclamationCropBlock.AGE) < 7;
      }
      if (level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray) {
         return tray.profile() != null && tray.age() < 7;
      }
      return false;
   }

   public static boolean servicePollinationTarget(ServerLevel level, BlockPos pos, int growthBonus) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof ReclamationCropBlock crop) {
         return crop.serviceFromPollinator(level, pos, growthBonus);
      }
      if (level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray) {
         return tray.serviceFromPollinator(level, growthBonus);
      }
      return false;
   }

   private static EnclosureScan scanEnclosure(Level level, BlockPos center, BlockPos min, BlockPos max) {
      List<BlockPos> candidates = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 3, 3))) {
         BlockPos candidate = pos.immutable();
         if (inside(candidate, min, max) && isInteriorPassable(level, candidate)) {
            candidates.add(candidate);
         }
      }
      candidates.sort(Comparator.comparingLong(pos -> distanceSquared(pos, center)));
      EnclosureScan best = EnclosureScan.open();
      Set<BlockPos> scannedInterior = new HashSet<>();
      for (BlockPos candidate : candidates) {
         if (scannedInterior.contains(candidate)) {
            continue;
         }
         EnclosureScan scan = floodInterior(level, candidate, min, max, scannedInterior);
         if (scan.enclosed()) {
            if (!best.enclosed() || scan.interiorVolume() > best.interiorVolume()) {
               best = scan;
            }
         } else if (!best.enclosed() && scan.interiorVolume() > best.interiorVolume()) {
            best = scan;
         }
      }
      return best;
   }

   private static EnclosureScan floodInterior(Level level, BlockPos start, BlockPos min, BlockPos max, Set<BlockPos> scannedInterior) {
      Queue<BlockPos> queue = new ArrayDeque<>();
      Set<BlockPos> visited = new HashSet<>();
      queue.add(start);
      visited.add(start);
      boolean escaped = false;
      boolean hasRoof = false;
      boolean hasFloor = false;
      while (!queue.isEmpty()) {
         BlockPos pos = queue.remove();
         if (touchesBoundary(pos, min, max)) {
            escaped = true;
         }
         hasRoof = hasRoof || hasGreenhouseRoofAbove(level, pos, max.getY());
         BlockPos below = pos.below();
         hasFloor = hasFloor || !inside(below, min, max) || !isInteriorPassable(level, below);
         for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction).immutable();
            if (!inside(next, min, max)) {
               escaped = true;
            } else if (!visited.contains(next) && isInteriorPassable(level, next)) {
               visited.add(next);
               queue.add(next);
            }
         }
      }
      scannedInterior.addAll(visited);
      return new EnclosureScan(!escaped && visited.size() >= 4, hasRoof, hasFloor, visited.size());
   }

   private static void pruneGrowthGreenhouseCache(long gameTime) {
      if (gameTime >= lastGrowthGreenhouseCachePruneTick
         && gameTime - lastGrowthGreenhouseCachePruneTick < GROWTH_GREENHOUSE_CACHE_PRUNE_TICKS) {
         return;
      }
      lastGrowthGreenhouseCachePruneTick = gameTime;
      Iterator<Map.Entry<GrowthGreenhouseCacheKey, GrowthGreenhouseCacheEntry>> iterator = GROWTH_GREENHOUSE_CACHE.entrySet().iterator();
      while (iterator.hasNext()) {
         GrowthGreenhouseCacheEntry cached = iterator.next().getValue();
         if (gameTime < cached.gameTime() || gameTime - cached.gameTime() > GROWTH_GREENHOUSE_CACHE_TICKS * 4L) {
            iterator.remove();
         }
      }
   }

   private static boolean hasGreenhouseRoofAbove(Level level, BlockPos pos, int maxY) {
      for (int y = pos.getY() + 1; y <= maxY; y++) {
         BlockState state = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
         if (state.getBlock() == ModBlocks.GREENHOUSE_GLASS.get()) {
            return true;
         }
         if (!isInteriorPassable(state)) {
            return false;
         }
      }
      return false;
   }

   private static boolean isInteriorPassable(Level level, BlockPos pos) {
      return isInteriorPassable(level.getBlockState(pos));
   }

   private static boolean isInteriorPassable(BlockState state) {
      Block block = state.getBlock();
      return state.isAir() || block instanceof ReclamationCropBlock || block == ModBlocks.HYDROPONIC_TRAY.get();
   }

   private static boolean isPollinationTarget(Block block) {
      return block instanceof ReclamationCropBlock || block == ModBlocks.HYDROPONIC_TRAY.get();
   }

   private static boolean inside(BlockPos pos, BlockPos min, BlockPos max) {
      return pos.getX() >= min.getX() && pos.getX() <= max.getX()
         && pos.getY() >= min.getY() && pos.getY() <= max.getY()
         && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
   }

   private static boolean touchesBoundary(BlockPos pos, BlockPos min, BlockPos max) {
      return pos.getX() == min.getX() || pos.getX() == max.getX()
         || pos.getY() == min.getY() || pos.getY() == max.getY()
         || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
   }

   private static ChunkPos chunkPos(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }

   private static long distanceSquared(BlockPos pos, BlockPos center) {
      long x = pos.getX() - center.getX();
      long y = pos.getY() - center.getY();
      long z = pos.getZ() - center.getZ();
      return x * x + y * y + z * z;
   }

   public record GreenhouseScan(
      int score,
      int supportScore,
      int enclosureScore,
      int glass,
      int filters,
      int activeDocks,
      int idleDocks,
      int controllers,
      int trays,
      int cropTargets,
      int deployedDrones,
      int serviceTargets,
      boolean enclosed,
      boolean greenhouseRoof,
      boolean floor,
      int interiorVolume
   ) {
      public static GreenhouseScan empty() {
         return new GreenhouseScan(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, 0);
      }

      public GreenhouseContext asContext() {
         return GreenhouseContext.unregistered(this);
      }

      public String enclosureLabel() {
         if (enclosed && greenhouseRoof && floor) {
            return "sealed";
         }
         if (enclosed) {
            return "bounded";
         }
         return "leaking";
      }
   }

   private record EnclosureScan(boolean enclosed, boolean hasGreenhouseRoof, boolean hasFloor, int interiorVolume) {
      private static EnclosureScan open() {
         return new EnclosureScan(false, false, false, 0);
      }
   }

   public enum GreenhouseZoneQuality {
      UNREGISTERED("unregistered"),
      UNSAFE("unsafe"),
      STRAINED("strained"),
      SAFE("safe");

      private final String label;

      GreenhouseZoneQuality(String label) {
         this.label = label;
      }

      public String label() {
         return label;
      }
   }

   public record GreenhouseContext(
      GreenhouseScan scan,
      ReclamationWorldData.GreenhouseZoneProfile zone,
      int score,
      int savedScore,
      int liveScore,
      GreenhouseZoneQuality quality
   ) {
      private static GreenhouseContext unregistered(GreenhouseScan scan) {
         return new GreenhouseContext(scan, null, scan.score(), scan.score(), scan.score(), GreenhouseZoneQuality.UNREGISTERED);
      }

      private static GreenhouseContext established(GreenhouseScan scan, ReclamationWorldData.GreenhouseZoneProfile zone, int score, int liveScore) {
         return new GreenhouseContext(scan, zone, Math.max(0, Math.min(100, score)), zone.score(), liveScore, qualityFor(score));
      }

      public boolean established() {
         return zone != null;
      }

      public int growthPenalty() {
         if (!established() || quality == GreenhouseZoneQuality.SAFE) {
            return 0;
         }
         return quality == GreenhouseZoneQuality.STRAINED ? GREENHOUSE_STRAINED_GROWTH_PENALTY : GREENHOUSE_UNSAFE_GROWTH_PENALTY;
      }

      public int seedSafety() {
         if (!established() || quality == GreenhouseZoneQuality.SAFE) {
            return score;
         }
         int penalty = quality == GreenhouseZoneQuality.STRAINED ? GREENHOUSE_STRAINED_SEED_PENALTY : GREENHOUSE_UNSAFE_SEED_PENALTY;
         return Math.max(0, score - penalty);
      }

      public int restorationGain(int baseGain) {
         if (!established() || quality == GreenhouseZoneQuality.SAFE) {
            return baseGain;
         }
         return Math.max(1, baseGain - (quality == GreenhouseZoneQuality.STRAINED ? 1 : 2));
      }

      public int pollinationBonus(int baseBonus) {
         if (!established() || baseBonus <= 0) {
            return 0;
         }
         if (quality == GreenhouseZoneQuality.SAFE) {
            return baseBonus;
         }
         if (quality == GreenhouseZoneQuality.STRAINED) {
            return Math.max(1, baseBonus / 2);
         }
         return Math.max(1, baseBonus / 3);
      }

      public String qualityLabel() {
         return quality.label();
      }

      public String summaryLabel() {
         if (!established()) {
            return "unregistered greenhouse";
         }
         return quality.label() + " zone";
      }

      public String nextAction() {
         if (!established()) {
            return "Scan a Greenhouse Controller to establish a saved greenhouse zone.";
         }
         if (quality == GreenhouseZoneQuality.SAFE) {
            if (scan.serviceTargets() > 0 && scan.deployedDrones() == 0) {
               return "Zone stable; deploy a Pollinator Drone from the dock for active crop service.";
            }
            return "Zone stable; crops can use the safe growth envelope.";
         }
         if (liveScore <= 0 && savedScore > 0) {
            return "Controller or greenhouse structure is missing; replace the controller/support blocks, then rescan the zone.";
         }
         if (liveScore < savedScore) {
            return "Rescan or repair the controller zone; current structure is below the saved profile.";
         }
         if (!scan.enclosed()) {
            return "Seal the Greenhouse Glass shell around the interior air pocket.";
         }
         if (!scan.greenhouseRoof()) {
            return "Add Greenhouse Glass overhead to finish the growth envelope.";
         }
         if (scan.activeDocks() == 0 && scan.idleDocks() > 0) {
            return "Place crops or Hydroponic Trays within Pollinator Dock service radius.";
         }
         return "Add Greenhouse Glass, a Spore Filter, Pollinator Dock support, or trays.";
      }

      private static GreenhouseZoneQuality qualityFor(int score) {
         int safe = ReclamationContent.progression().greenhouseSafeThreshold();
         if (score >= safe) {
            return GreenhouseZoneQuality.SAFE;
         }
         if (score >= Math.max(0, safe - GREENHOUSE_STRAINED_MARGIN)) {
            return GreenhouseZoneQuality.STRAINED;
         }
         return GreenhouseZoneQuality.UNSAFE;
      }
   }

   private record GrowthGreenhouseCacheKey(String dimension, int sectionX, int sectionY, int sectionZ) {
      private static GrowthGreenhouseCacheKey of(ServerLevel level, BlockPos pos) {
         ReclamationWorldData.GreenhouseZoneProfile zone = savedGreenhouseZone(level, pos);
         if (zone != null) {
            BlockPos controller = zone.controllerPos();
            return new GrowthGreenhouseCacheKey(
               level.dimension().identifier().toString(),
               controller.getX(),
               controller.getY(),
               controller.getZ()
            );
         }
         return new GrowthGreenhouseCacheKey(
            level.dimension().identifier().toString(),
            pos.getX() >> 4,
            pos.getY() >> 4,
            pos.getZ() >> 4
         );
      }
   }

   private record GrowthGreenhouseCacheEntry(GreenhouseContext context, long gameTime) {
   }

   public static int cropStability(Player player) {
      int total = 0;
      int count = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         SeedProfile profile = stack.get(ModDataComponents.SEED_PROFILE.get());
         if (profile != null) {
            total += profile.stability();
            count++;
         }
      }
      if (count == 0) {
         return value(player, "crop_stability");
      }
      return Math.max(value(player, "crop_stability"), total / count);
   }

   public static int foodSecurity(Player player) {
      int food = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(ModItems.ASH_WHEAT.get()) || stack.is(ModItems.HARDROOT.get()) || stack.is(ModItems.GLOW_BEANS.get())
            || stack.is(ModItems.MUTANT_BERRIES.get()) || stack.is(ModItems.CLEAN_CORN.get())) {
            food += stack.getCount();
         }
      }
      return Math.min(100, knownSeeds(player).size() * ReclamationContent.progression().foodKnownSeedBonus()
         + food * ReclamationContent.progression().foodItemValue());
   }

   public static int count(Player player, Item item) {
      int total = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            total += stack.getCount();
         }
      }
      return total;
   }

   public static void syncServerMetric(ServerPlayer player) {
      metrics(player);
      discoverRoutes(player);
   }

   private static void discoverRoutes(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
      }
   }

   public static String coreMilestoneForFlag(String flag) {
      return switch (flag) {
         case "seed_recovered", "seed_analyzed" -> "recover_seed";
         case "soil_analyzed" -> "analyze_soil";
         case "first_growth" -> "first_growth";
         case "gene_stabilization" -> "gene_stabilization";
         case "greenhouse_online" -> "greenhouse_online";
         case "restore_chunk" -> "restore_chunk";
         case "hydroponics_online" -> "hydroponics_online";
         case "bio_reactor_online" -> "bio_reactor_online";
         case "compost_recycler_online" -> "compost_recycler_online";
         default -> "";
      };
   }

   private static void recordCoreMilestone(Player player, String flag) {
      String milestone = coreMilestoneForFlag(flag);
      if (!milestone.isBlank()) {
         ReclamationCrossAddonIntegration.recordMilestone(player, milestone);
      }
   }
}

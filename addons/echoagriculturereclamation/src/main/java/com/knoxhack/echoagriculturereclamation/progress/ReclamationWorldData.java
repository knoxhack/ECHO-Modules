package com.knoxhack.echoagriculturereclamation.progress;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ReclamationWorldData extends SavedData {
   public static final Codec<ReclamationWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("restorationScores", Map.of()).forGetter(data -> data.restorationScores),
      Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("greenhouseSafety", Map.of()).forGetter(data -> data.greenhouseSafety),
      Codec.unboundedMap(Codec.STRING, GreenhouseZoneProfile.CODEC).optionalFieldOf("greenhouseZones", Map.of()).forGetter(data -> data.greenhouseZones),
      Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("lastSoilStates", Map.of()).forGetter(data -> data.lastSoilStates),
      Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("worldStats", Map.of()).forGetter(data -> data.worldStats),
      Codec.unboundedMap(Codec.STRING, FieldHistory.CODEC).optionalFieldOf("fieldHistory", Map.of()).forGetter(data -> data.fieldHistory)
   ).apply(instance, (restorationScores, greenhouseSafety, greenhouseZones, lastSoilStates, worldStats, fieldHistory) -> {
      ReclamationWorldData data = new ReclamationWorldData();
      data.restorationScores.putAll(restorationScores);
      data.greenhouseSafety.putAll(greenhouseSafety);
      data.greenhouseZones.putAll(greenhouseZones);
      data.lastSoilStates.putAll(lastSoilStates);
      data.worldStats.putAll(worldStats);
      data.fieldHistory.putAll(fieldHistory);
      return data;
   }));

   public static final SavedDataType<ReclamationWorldData> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, "reclamation_world_data"), ReclamationWorldData::new, CODEC
   );

   private final Map<String, Integer> restorationScores = new LinkedHashMap<>();
   private final Map<String, Integer> greenhouseSafety = new LinkedHashMap<>();
   private final Map<String, GreenhouseZoneProfile> greenhouseZones = new LinkedHashMap<>();
   private final Map<String, String> lastSoilStates = new LinkedHashMap<>();
   private final Map<String, Integer> worldStats = new LinkedHashMap<>();
   private final Map<String, FieldHistory> fieldHistory = new LinkedHashMap<>();

   public static ReclamationWorldData get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(TYPE);
   }

   public int restorationScore(ChunkPos chunk) {
      return Math.max(0, Math.min(100, restorationScores.getOrDefault(key(chunk), 0)));
   }

   public int addRestoration(ChunkPos chunk, int amount) {
      if (amount <= 0) {
         return restorationScore(chunk);
      }
      int previous = restorationScore(chunk);
      int next = Math.min(100, previous + amount);
      restorationScores.put(key(chunk), next);
      worldStats.put("restoration_events", stat("restoration_events") + 1);
      if (threshold(next) > threshold(previous)) {
         recordFieldHistory(chunk, -1L, "", threshold(next), "Restoration threshold " + threshold(next) + "% reached.");
      }
      setDirty();
      return next;
   }

   public void setRestorationScore(ChunkPos chunk, int score) {
      restorationScores.put(key(chunk), Math.max(0, Math.min(100, score)));
      setDirty();
   }

   public int greenhouseSafety(ChunkPos chunk) {
      return Math.max(0, Math.min(100, greenhouseSafety.getOrDefault(key(chunk), 0)));
   }

   public void setGreenhouseSafety(ChunkPos chunk, int safety) {
      greenhouseSafety.put(key(chunk), Math.max(0, Math.min(100, safety)));
      setDirty();
   }

   public GreenhouseZoneProfile greenhouseZone(ChunkPos chunk) {
      return greenhouseZones.get(key(chunk));
   }

   public void setGreenhouseZone(ChunkPos chunk, GreenhouseZoneProfile profile) {
      if (profile == null) {
         greenhouseZones.remove(key(chunk));
      } else {
         greenhouseZones.put(key(chunk), profile.clamped());
         greenhouseSafety.put(key(chunk), profile.clamped().score());
         worldStats.put("greenhouse_zone_scans", stat("greenhouse_zone_scans") + 1);
      }
      setDirty();
   }

   public String lastSoilState(ChunkPos chunk) {
      return lastSoilStates.getOrDefault(key(chunk), "Dead Soil");
   }

   public void setLastSoilState(ChunkPos chunk, String state) {
      lastSoilStates.put(key(chunk), state == null || state.isBlank() ? "Dead Soil" : state);
      setDirty();
   }

   public int stat(String key) {
      return Math.max(0, worldStats.getOrDefault(key, 0));
   }

   public void addStat(String key, int amount) {
      if (amount <= 0) {
         return;
      }
      worldStats.put(key, Math.min(2_000_000_000, stat(key) + amount));
      setDirty();
   }

   public FieldHistory fieldHistory(ChunkPos chunk) {
      return fieldHistory.getOrDefault(key(chunk), FieldHistory.empty());
   }

   public void recordFieldHistory(ChunkPos chunk, long scanTime, String greenhouseQuality, int restorationThreshold, String blocker) {
      FieldHistory previous = fieldHistory(chunk);
      FieldHistory next = new FieldHistory(
         scanTime >= 0L ? scanTime : previous.lastScanTime(),
         greenhouseQuality == null || greenhouseQuality.isBlank() ? previous.lastGreenhouseQuality() : greenhouseQuality,
         Math.max(previous.lastRestorationThreshold(), restorationThreshold),
         blocker == null || blocker.isBlank() ? previous.lastMeaningfulBlocker() : blocker
      );
      fieldHistory.put(key(chunk), next);
      setDirty();
   }

   public Map<String, FieldHistory> fieldHistoryByChunkKey() {
      return Map.copyOf(fieldHistory);
   }

   private static String key(ChunkPos chunk) {
      return chunk.x() + "," + chunk.z();
   }

   private static int threshold(int score) {
      if (score >= 100) {
         return 100;
      }
      if (score >= 60) {
         return 60;
      }
      if (score >= 25) {
         return 25;
      }
      return 0;
   }

   public record FieldHistory(
      long lastScanTime,
      String lastGreenhouseQuality,
      int lastRestorationThreshold,
      String lastMeaningfulBlocker
   ) {
      public static final Codec<FieldHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.LONG.optionalFieldOf("lastScanTime", 0L).forGetter(FieldHistory::lastScanTime),
         Codec.STRING.optionalFieldOf("lastGreenhouseQuality", "unregistered").forGetter(FieldHistory::lastGreenhouseQuality),
         Codec.INT.optionalFieldOf("lastRestorationThreshold", 0).forGetter(FieldHistory::lastRestorationThreshold),
         Codec.STRING.optionalFieldOf("lastMeaningfulBlocker", "").forGetter(FieldHistory::lastMeaningfulBlocker)
      ).apply(instance, FieldHistory::new));

      public FieldHistory {
         lastScanTime = Math.max(0L, lastScanTime);
         lastGreenhouseQuality = lastGreenhouseQuality == null || lastGreenhouseQuality.isBlank() ? "unregistered" : lastGreenhouseQuality.strip();
         lastRestorationThreshold = clamp(lastRestorationThreshold, 0, 100);
         lastMeaningfulBlocker = lastMeaningfulBlocker == null ? "" : lastMeaningfulBlocker.strip();
      }

      public static FieldHistory empty() {
         return new FieldHistory(0L, "unregistered", 0, "");
      }
   }

   public record GreenhouseZoneProfile(
      int score,
      int supportScore,
      int enclosureScore,
      int glass,
      int filters,
      int activeDocks,
      int idleDocks,
      int cropTargets,
      int deployedDrones,
      int serviceTargets,
      boolean enclosed,
      boolean greenhouseRoof,
      boolean floor,
      int controllerX,
      int controllerY,
      int controllerZ,
      long scanTime
   ) {
      public GreenhouseZoneProfile(
         int score,
         int supportScore,
         int enclosureScore,
         int glass,
         int filters,
         int activeDocks,
         int idleDocks,
         int cropTargets,
         boolean enclosed,
         boolean greenhouseRoof,
         boolean floor,
         int controllerX,
         int controllerY,
         int controllerZ,
         long scanTime
      ) {
         this(score, supportScore, enclosureScore, glass, filters, activeDocks, idleDocks, cropTargets,
            0, 0, enclosed, greenhouseRoof, floor, controllerX, controllerY, controllerZ, scanTime);
      }

      private static final Codec<int[]> CONTROLLER_CODEC = Codec.INT_STREAM.xmap(stream -> {
         int[] values = stream.limit(3).toArray();
         return new int[] {
            values.length > 0 ? values[0] : 0,
            values.length > 1 ? values[1] : 0,
            values.length > 2 ? values[2] : 0
         };
      }, Arrays::stream);

      public static final Codec<GreenhouseZoneProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.INT.optionalFieldOf("score", 0).forGetter(GreenhouseZoneProfile::score),
         Codec.INT.optionalFieldOf("supportScore", 0).forGetter(GreenhouseZoneProfile::supportScore),
         Codec.INT.optionalFieldOf("enclosureScore", 0).forGetter(GreenhouseZoneProfile::enclosureScore),
         Codec.INT.optionalFieldOf("glass", 0).forGetter(GreenhouseZoneProfile::glass),
         Codec.INT.optionalFieldOf("filters", 0).forGetter(GreenhouseZoneProfile::filters),
         Codec.INT.optionalFieldOf("activeDocks", 0).forGetter(GreenhouseZoneProfile::activeDocks),
         Codec.INT.optionalFieldOf("idleDocks", 0).forGetter(GreenhouseZoneProfile::idleDocks),
         Codec.INT.optionalFieldOf("cropTargets", 0).forGetter(GreenhouseZoneProfile::cropTargets),
         Codec.INT.optionalFieldOf("deployedDrones", 0).forGetter(GreenhouseZoneProfile::deployedDrones),
         Codec.INT.optionalFieldOf("serviceTargets", 0).forGetter(GreenhouseZoneProfile::serviceTargets),
         Codec.BOOL.optionalFieldOf("enclosed", false).forGetter(GreenhouseZoneProfile::enclosed),
         Codec.BOOL.optionalFieldOf("greenhouseRoof", false).forGetter(GreenhouseZoneProfile::greenhouseRoof),
         Codec.BOOL.optionalFieldOf("floor", false).forGetter(GreenhouseZoneProfile::floor),
         CONTROLLER_CODEC.optionalFieldOf("controller", new int[] {0, 0, 0})
            .forGetter(profile -> new int[] {profile.controllerX(), profile.controllerY(), profile.controllerZ()}),
         Codec.LONG.optionalFieldOf("scanTime", 0L).forGetter(GreenhouseZoneProfile::scanTime)
      ).apply(instance, (score, supportScore, enclosureScore, glass, filters, activeDocks, idleDocks,
         cropTargets, deployedDrones, serviceTargets, enclosed, greenhouseRoof, floor, controller, scanTime) ->
            new GreenhouseZoneProfile(
               score,
               supportScore,
               enclosureScore,
               glass,
               filters,
               activeDocks,
               idleDocks,
               cropTargets,
               deployedDrones,
               serviceTargets,
               enclosed,
               greenhouseRoof,
               floor,
               controller[0],
               controller[1],
               controller[2],
               scanTime
            )));

      public BlockPos controllerPos() {
         return new BlockPos(controllerX, controllerY, controllerZ);
      }

      private GreenhouseZoneProfile clamped() {
         return new GreenhouseZoneProfile(
            clamp(score, 0, 100),
            clamp(supportScore, 0, 100),
            Math.max(0, enclosureScore),
            Math.max(0, glass),
            Math.max(0, filters),
            Math.max(0, activeDocks),
            Math.max(0, idleDocks),
            Math.max(0, cropTargets),
            Math.max(0, deployedDrones),
            Math.max(0, serviceTargets),
            enclosed,
            greenhouseRoof,
            floor,
            controllerX,
            controllerY,
            controllerZ,
            Math.max(0L, scanTime)
         );
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}

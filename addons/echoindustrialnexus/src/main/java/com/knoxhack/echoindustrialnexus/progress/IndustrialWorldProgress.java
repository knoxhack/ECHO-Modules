package com.knoxhack.echoindustrialnexus.progress;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class IndustrialWorldProgress extends SavedData {
   public static final Codec<IndustrialWorldProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("playerStats", Map.of()).forGetter(data -> data.playerStats),
         Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("worldStats", Map.of()).forGetter(data -> data.worldStats),
         Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("playerFlags", Map.of()).forGetter(data -> data.playerFlags),
         Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("worldFlags", Map.of()).forGetter(data -> data.worldFlags),
         Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("pois", Map.of()).forGetter(data -> data.poiRecords),
         Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("scrubberZones", Map.of()).forGetter(data -> data.scrubberZones),
         Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("pendingPois", Map.of()).forGetter(data -> data.pendingPois)
      ).apply(instance, (playerStats, worldStats, playerFlags, worldFlags, poiRecords, scrubberZones, pendingPois) -> {
         IndustrialWorldProgress data = new IndustrialWorldProgress();
         data.playerStats.putAll(playerStats);
         data.worldStats.putAll(worldStats);
         data.playerFlags.putAll(playerFlags);
         data.worldFlags.putAll(worldFlags);
         data.poiRecords.putAll(poiRecords);
         data.scrubberZones.putAll(scrubberZones);
         data.pendingPois.putAll(pendingPois);
         return data;
      }));

   public static final SavedDataType<IndustrialWorldProgress> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_progress"), IndustrialWorldProgress::new, CODEC
   );

   private final Map<String, Long> playerStats = new LinkedHashMap<>();
   private final Map<String, Long> worldStats = new LinkedHashMap<>();
   private final Map<String, String> playerFlags = new LinkedHashMap<>();
   private final Map<String, String> worldFlags = new LinkedHashMap<>();
   private final Map<String, String> poiRecords = new LinkedHashMap<>();
   private final Map<String, String> scrubberZones = new LinkedHashMap<>();
   private final Map<String, String> pendingPois = new LinkedHashMap<>();

   public static IndustrialWorldProgress get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(TYPE);
   }

   public long playerStat(UUID player, String key) {
      return Math.max(0L, playerStats.getOrDefault(playerKey(player, key), 0L));
   }

   public void addPlayerStat(UUID player, String key, long amount) {
      if (amount <= 0L) {
         return;
      }
      playerStats.put(playerKey(player, key), Math.min(2_000_000_000L, playerStat(player, key) + amount));
      setDirty();
   }

   public void maxPlayerStat(UUID player, String key, long value) {
      long safe = Math.max(0L, value);
      String fullKey = playerKey(player, key);
      if (safe > playerStats.getOrDefault(fullKey, 0L)) {
         playerStats.put(fullKey, safe);
         setDirty();
      }
   }

   public long worldStat(String key) {
      return Math.max(0L, worldStats.getOrDefault(key, 0L));
   }

   public void addWorldStat(String key, long amount) {
      if (amount <= 0L) {
         return;
      }
      worldStats.put(key, Math.min(2_000_000_000L, worldStat(key) + amount));
      setDirty();
   }

   public boolean playerFlag(UUID player, String key) {
      return Boolean.parseBoolean(playerFlags.getOrDefault(playerKey(player, key), "false"));
   }

   public void setPlayerFlag(UUID player, String key, boolean value) {
      playerFlags.put(playerKey(player, key), Boolean.toString(value));
      setDirty();
   }

   public boolean claimReward(UUID player, String key) {
      String fullKey = playerKey(player, "claimed_" + key);
      if (Boolean.parseBoolean(playerFlags.getOrDefault(fullKey, "false"))) {
         return false;
      }
      playerFlags.put(fullKey, "true");
      setDirty();
      return true;
   }

   public void recordPoi(String type, BlockPos pos, String state) {
      poiRecords.put(type + "@" + pos.toShortString(), state);
      pendingPois.remove(type + "@" + pos.toShortString());
      worldFlags.put("poi_" + type, "true");
      addWorldStat("pois_recorded", 1L);
      setDirty();
   }

   public PoiRecord nearestPoi(String type, BlockPos origin) {
      PoiRecord nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (String key : poiRecords.keySet()) {
         PoiRecord record = parsePoi(key);
         if (record == null || type != null && !type.isBlank() && !record.type().equals(type)) {
            continue;
         }
         double distance = origin == null ? 0.0D : record.pos().distSqr(origin);
         if (nearest == null || distance < nearestDistance) {
            nearest = record;
            nearestDistance = distance;
         }
      }
      return nearest;
   }

   public void schedulePendingPoi(String type, BlockPos pos) {
      String key = type + "@" + pos.toShortString();
      pendingPois.putIfAbsent(key, type + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|0");
      setDirty();
   }

   public void reschedulePendingPoi(PendingPoi poi) {
      if (poi.attempts() >= 12) {
         pendingPois.remove(poi.key());
      } else {
         pendingPois.put(poi.key(), poi.type() + "|" + poi.pos().getX() + "|" + poi.pos().getY() + "|" + poi.pos().getZ() + "|" + (poi.attempts() + 1));
      }
      setDirty();
   }

   public void removePendingPoi(PendingPoi poi) {
      pendingPois.remove(poi.key());
      setDirty();
   }

   public List<PendingPoi> pendingPoiCandidates(int limit) {
      List<PendingPoi> result = new ArrayList<>();
      for (Map.Entry<String, String> entry : pendingPois.entrySet()) {
         if (result.size() >= limit) {
            break;
         }
         String[] parts = entry.getValue().split("\\|");
         if (parts.length != 5) {
            continue;
         }
         try {
            result.add(new PendingPoi(entry.getKey(), parts[0], new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])), Integer.parseInt(parts[4])));
         } catch (NumberFormatException ignored) {
         }
      }
      return result;
   }

   public void recordScrubberZone(BlockPos pos, String mode, int storedFlux, int heat) {
      scrubberZones.put(pos.toShortString(), mode + "|tf=" + storedFlux + "|heat=" + heat);
      worldFlags.put("scrubber_zone", "true");
      setDirty();
   }

   public int poiCount() {
      return poiRecords.size();
   }

   public int scrubberZoneCount() {
      return scrubberZones.size();
   }

   private static String playerKey(UUID player, String key) {
      return player + ":" + key;
   }

   public record PendingPoi(String key, String type, BlockPos pos, int attempts) {
   }

   public record PoiRecord(String type, BlockPos pos) {
   }

   private static PoiRecord parsePoi(String key) {
      if (key == null || key.isBlank()) {
         return null;
      }
      int split = key.indexOf('@');
      if (split <= 0 || split >= key.length() - 1) {
         return null;
      }
      String type = key.substring(0, split);
      String[] coords = key.substring(split + 1).split(", ");
      if (coords.length != 3) {
         return null;
      }
      try {
         return new PoiRecord(type, new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2])));
      } catch (NumberFormatException ignored) {
         return null;
      }
   }
}

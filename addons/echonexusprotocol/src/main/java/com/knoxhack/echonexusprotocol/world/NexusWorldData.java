package com.knoxhack.echonexusprotocol.world;

import com.knoxhack.echonexusprotocol.block.ProtocolSealBlock;
import com.knoxhack.echonexusprotocol.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class NexusWorldData extends SavedData {
   public static final int DEFAULT_FIELD = 86;
   public static final Codec<NexusWorldData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("fields", Map.of()).forGetter(d -> d.fields),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("corruption", Map.of()).forGetter(d -> d.corruption),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("quarantine", Map.of()).forGetter(d -> d.quarantineTicks),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("storms", Map.of()).forGetter(d -> d.stormTicks),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("realityTears", Map.of()).forGetter(d -> d.realityTears),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("flags", Map.of()).forGetter(NexusWorldData::serializedFlags),
            Codec.STRING.optionalFieldOf("endingState", "").forGetter(d -> d.endingState),
            Codec.LONG.optionalFieldOf("lastStormTick", 0L).forGetter(d -> d.lastStormTick)
         )
         .apply(instance, (fields, corruption, quarantine, storms, realityTears, flags, endingState, lastStormTick) -> {
            NexusWorldData data = new NexusWorldData();
            data.fields.putAll(fields);
            data.corruption.putAll(corruption);
            data.quarantineTicks.putAll(quarantine);
            data.stormTicks.putAll(storms);
            data.realityTears.putAll(realityTears);
            data.loadFlags(flags);
            data.endingState = endingState == null ? "" : endingState;
            data.lastStormTick = Math.max(0L, lastStormTick);
            return data;
         })
   );
   public static final SavedDataType<NexusWorldData> TYPE = new SavedDataType(
      Identifier.fromNamespaceAndPath("echonexusprotocol", "nexus_world"), NexusWorldData::new, CODEC
   );
   private static final int DEFAULT_QUARANTINE_TICKS = 600;
   private final Map<String, Integer> fields = new LinkedHashMap<>();
   private final Map<String, Integer> corruption = new LinkedHashMap<>();
   private final Map<String, Integer> quarantineTicks = new LinkedHashMap<>();
   private final Map<String, Long> stormTicks = new LinkedHashMap<>();
   private final Map<String, Integer> realityTears = new LinkedHashMap<>();
   private boolean blackboxMonolithActivated;
   private boolean wardenDefeated;
   private boolean guardianDefeated;
   private boolean activeRealityTear;
   private boolean endingFeedbackApplied;
   private String endingState = "";
   private long lastStormTick;

   public static NexusWorldData get(ServerLevel level) {
      return (NexusWorldData)level.getDataStorage().computeIfAbsent(TYPE);
   }

   public int fieldValue(ChunkPos pos) {
      return clampField(this.fields.getOrDefault(key(pos), 86));
   }

   public void setFieldValue(ChunkPos pos, int value) {
      this.fields.put(key(pos), clampField(value));
      this.setDirty();
   }

   public void addFieldValue(ChunkPos pos, int delta) {
      this.setFieldValue(pos, this.fieldValue(pos) + delta);
   }

   public int corruptionPressure(ChunkPos pos) {
      return clampPercent(this.corruption.getOrDefault(key(pos), Math.max(0, 86 - this.fieldValue(pos))));
   }

   public void setCorruptionPressure(ChunkPos pos, int value) {
      this.corruption.put(key(pos), clampPercent(value));
      this.setDirty();
   }

   public void addCorruptionPressure(ChunkPos pos, int delta) {
      this.setCorruptionPressure(pos, this.corruptionPressure(pos) + delta);
   }

   public int quarantineTicks(ChunkPos pos) {
      return Math.max(0, this.quarantineTicks.getOrDefault(key(pos), 0));
   }

   public boolean isQuarantined(ChunkPos pos) {
      return this.quarantineTicks(pos) > 0;
   }

   public void quarantineChunk(ChunkPos pos, int ticks) {
      int safeTicks = Math.max(0, ticks);
      if (safeTicks <= 0) {
         if (this.quarantineTicks.remove(key(pos)) != null) {
            this.setDirty();
         }
      } else {
         this.quarantineTicks.put(key(pos), Math.max(this.quarantineTicks(pos), safeTicks));
         this.setDirty();
      }
   }

   public long lastStormTick(ChunkPos pos) {
      return Math.max(0L, this.stormTicks.getOrDefault(key(pos), 0L));
   }

   public boolean hasActiveStorm(ChunkPos pos, long gameTime, long windowTicks) {
      long lastTick = this.lastStormTick(pos);
      boolean active = lastTick > 0L && gameTime - lastTick <= Math.max(0L, windowTicks);
      if (!active && lastTick > 0L && windowTicks > 0L) {
         this.stormTicks.remove(key(pos));
         this.setDirty();
      }
      return active;
   }

   public int realityTearCount(ChunkPos pos) {
      return Math.max(0, this.realityTears.getOrDefault(key(pos), 0));
   }

   public int activeStormChunkCount() {
      return this.stormTicks.size();
   }

   public int activeStormChunkCount(long gameTime, long windowTicks) {
      this.pruneExpiredStorms(gameTime, windowTicks);
      return this.stormTicks.size();
   }

   public NexusWorldData.FieldState fieldState(ChunkPos pos) {
      return NexusWorldData.FieldState.fromValue(this.fieldValue(pos));
   }

   public void applySeal(ChunkPos pos, ProtocolSealBlock.SealMode mode) {
      int controlBoost = "control".equals(this.endingState) ? 2 : 0;
      switch (mode) {
         case QUARANTINE:
            this.quarantineChunk(pos, DEFAULT_QUARANTINE_TICKS);
            this.addFieldValue(pos, 2 + controlBoost);
            this.addCorruptionPressure(pos, -2 - controlBoost);
            break;
         case PURIFY:
            this.addFieldValue(pos, 3 + controlBoost);
            this.addCorruptionPressure(pos, -(Integer)Config.SEAL_PURIFY_PRESSURE_REDUCTION.get() - controlBoost);
            break;
         case COLLAPSE:
            this.addFieldValue(pos, -(Integer)Config.SEAL_COLLAPSE_FIELD_LOSS.get());
            this.addCorruptionPressure(pos, "control".equals(this.endingState) ? 4 : 8);
            break;
         case EXTRACT:
            this.addCorruptionPressure(pos, -2 - controlBoost);
            break;
         case REWRITE:
            this.addFieldValue(pos, -1);
      }
   }

   public void tickAffectedChunk(ServerLevel level, ChunkPos pos) {
      if ("destroy".equals(this.endingState)) {
         this.setCorruptionPressure(pos, 0);
         boolean changed = this.stormTicks.remove(key(pos)) != null | this.realityTears.remove(key(pos)) != null;
         if (changed) {
            this.setDirty();
         }
         return;
      }
      if ("restore".equals(this.endingState)) {
         if (this.corruptionPressure(pos) > 0) {
            this.addCorruptionPressure(pos, -3);
            this.addFieldValue(pos, 1);
         }
         if (this.stormTicks.remove(key(pos)) != null) {
            this.setDirty();
         }
      }
      if (this.tickQuarantine(pos)) {
         this.addFieldValue(pos, 1);
         this.addCorruptionPressure(pos, -2);
      } else {
         int pressure = this.corruptionPressure(pos);
         NexusWorldData.FieldState state = this.fieldState(pos);
         if (pressure > 0 || state != NexusWorldData.FieldState.STABLE) {
            if (pressure > 0) {
               this.addFieldValue(pos, -(pressure >= 45 ? (Integer)Config.FIELD_DECAY_HIGH_PRESSURE.get() : (Integer)Config.FIELD_DECAY_LOW_PRESSURE.get()));
               this.addCorruptionPressure(pos, -1);
            }

            if (state == NexusWorldData.FieldState.CRITICAL || state == NexusWorldData.FieldState.COLLAPSED) {
               this.markStorm(pos, level.getGameTime());
               if (state == NexusWorldData.FieldState.COLLAPSED) {
                  this.markRealityTearActive(pos);
               }
            }
         }
      }
   }

   private boolean tickQuarantine(ChunkPos pos) {
      String key = key(pos);
      int remaining = this.quarantineTicks.getOrDefault(key, 0);
      if (remaining <= 0) {
         return false;
      } else {
         int next = Math.max(0, remaining - 80);
         if (next == 0) {
            this.quarantineTicks.remove(key);
         } else {
            this.quarantineTicks.put(key, next);
         }

         this.setDirty();
         return true;
      }
   }

   private void markStorm(ChunkPos pos, long gameTime) {
      this.lastStormTick = Math.max(this.lastStormTick, gameTime);
      this.stormTicks.put(key(pos), Math.max(0L, gameTime));
      this.setDirty();
   }

   public void startAnomalyStorm(ChunkPos pos, long gameTime) {
      this.markStorm(pos, gameTime);
      this.addFieldValue(pos, -12);
      this.addCorruptionPressure(pos, 18);
      this.markRealityTearActive(pos);
   }

   public boolean activateBlackboxMonolith() {
      boolean changed = !this.blackboxMonolithActivated;
      this.blackboxMonolithActivated = true;
      if (changed) {
         this.setDirty();
      }

      return changed;
   }

   public boolean blackboxMonolithActivated() {
      return this.blackboxMonolithActivated;
   }

   public boolean markWardenDefeated() {
      boolean changed = !this.wardenDefeated;
      this.wardenDefeated = true;
      if (changed) {
         this.setDirty();
      }

      return changed;
   }

   public boolean wardenDefeated() {
      return this.wardenDefeated;
   }

   public boolean markGuardianDefeated() {
      boolean changed = !this.guardianDefeated;
      this.guardianDefeated = true;
      if (changed) {
         this.setDirty();
      }

      return changed;
   }

   public boolean guardianDefeated() {
      return this.guardianDefeated;
   }

   public void markRealityTearActive() {
      this.activeRealityTear = true;
      this.setDirty();
   }

   public void markRealityTearActive(ChunkPos pos) {
      this.activeRealityTear = true;
      this.realityTears.put(key(pos), Math.max(1, this.realityTearCount(pos)));
      this.setDirty();
   }

   public boolean activeRealityTear() {
      return this.activeRealityTear;
   }

   public void setEndingState(String endingState) {
      String next = normalizeEnding(endingState);
      if (next.isBlank()) {
         if (!this.endingState.isBlank() || this.endingFeedbackApplied) {
            this.endingState = "";
            this.endingFeedbackApplied = false;
            this.setDirty();
         }
         return;
      }

      boolean changedPath = !this.endingState.equals(next);
      this.endingState = next;
      this.guardianDefeated = true;
      if (changedPath || !this.endingFeedbackApplied) {
         this.applyEndingFeedback();
         this.endingFeedbackApplied = true;
         this.setDirty();
      }
   }

   public boolean commitEndingState(String endingState) {
      String next = normalizeEnding(endingState);
      if (next.isBlank()) {
         if (!this.endingState.isBlank() || this.endingFeedbackApplied) {
            this.endingState = "";
            this.endingFeedbackApplied = false;
            this.setDirty();
            return true;
         }
         return false;
      }
      if (!this.endingState.isBlank() && !this.endingState.equals(next)) {
         return false;
      }
      boolean changed = !this.endingState.equals(next);
      this.endingState = next;
      this.guardianDefeated = true;
      if (!this.endingFeedbackApplied) {
         this.applyEndingFeedback();
         this.endingFeedbackApplied = true;
         changed = true;
      }
      if (changed) {
         this.setDirty();
      }
      return changed;
   }

   public String endingState() {
      return this.endingState;
   }

   public long lastStormTick() {
      return this.lastStormTick;
   }

   private Map<String, String> serializedFlags() {
      Map<String, String> flags = new LinkedHashMap<>();
      flags.put("blackboxMonolithActivated", Boolean.toString(this.blackboxMonolithActivated));
      flags.put("wardenDefeated", Boolean.toString(this.wardenDefeated));
      flags.put("guardianDefeated", Boolean.toString(this.guardianDefeated));
      flags.put("activeRealityTear", Boolean.toString(this.activeRealityTear));
      flags.put("endingFeedbackApplied", Boolean.toString(this.endingFeedbackApplied));
      return flags;
   }

   private void loadFlags(Map<String, String> flags) {
      this.blackboxMonolithActivated = Boolean.parseBoolean(flags.getOrDefault("blackboxMonolithActivated", "false"));
      this.wardenDefeated = Boolean.parseBoolean(flags.getOrDefault("wardenDefeated", "false"));
      this.guardianDefeated = Boolean.parseBoolean(flags.getOrDefault("guardianDefeated", "false"));
      this.activeRealityTear = Boolean.parseBoolean(flags.getOrDefault("activeRealityTear", "false"));
      this.endingFeedbackApplied = Boolean.parseBoolean(flags.getOrDefault("endingFeedbackApplied", "false"));
   }

   public void pruneExpiredStorms(long gameTime, long windowTicks) {
      long safeWindow = Math.max(0L, windowTicks);
      boolean changed = this.stormTicks.entrySet().removeIf(entry -> entry.getValue() <= 0L || gameTime - entry.getValue() > safeWindow);
      if (changed) {
         this.setDirty();
      }
   }

   private void applyEndingFeedback() {
      if (this.endingState.isBlank()) {
         return;
      }
      String ending = this.endingState.trim().toLowerCase(java.util.Locale.ROOT);
      List<String> trackedChunks = new ArrayList<>();
      trackedChunks.addAll(this.fields.keySet());
      for (String key : this.corruption.keySet()) {
         if (!trackedChunks.contains(key)) {
            trackedChunks.add(key);
         }
      }
      for (String key : this.realityTears.keySet()) {
         if (!trackedChunks.contains(key)) {
            trackedChunks.add(key);
         }
      }
      if (trackedChunks.isEmpty()) {
         trackedChunks.add("0,0");
      }
      for (String key : trackedChunks) {
         ChunkPos pos = parseKey(key);
         if (pos == null) {
            continue;
         }
         switch (ending) {
            case "restore" -> {
               this.addFieldValue(pos, 12);
               this.addCorruptionPressure(pos, -24);
               this.stormTicks.remove(key(pos));
            }
            case "control" -> {
               this.addFieldValue(pos, 4);
               this.addCorruptionPressure(pos, -10);
            }
            case "destroy" -> {
               this.addFieldValue(pos, 8);
               this.setCorruptionPressure(pos, 0);
               this.stormTicks.remove(key(pos));
               this.realityTears.remove(key(pos));
               this.activeRealityTear = false;
            }
            case "merge" -> {
               this.addFieldValue(pos, -4);
               this.addCorruptionPressure(pos, 8);
               this.markRealityTearActive(pos);
            }
            default -> {
            }
         }
      }
   }

   private static String key(ChunkPos pos) {
      return pos.x() + "," + pos.z();
   }

   private static ChunkPos parseKey(String key) {
      if (key == null) {
         return null;
      }
      String[] parts = key.split(",", 2);
      if (parts.length != 2) {
         return null;
      }
      try {
         return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } catch (NumberFormatException ex) {
         return null;
      }
   }

   private static int clampField(int value) {
      return Math.max(0, Math.min(100, value));
   }

   private static int clampPercent(int value) {
      return Math.max(0, Math.min(100, value));
   }

   private static String normalizeEnding(String value) {
      return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
   }

   public static enum FieldState {
      STABLE,
      UNSTABLE,
      FRACTURED,
      CRITICAL,
      COLLAPSED;

      public static NexusWorldData.FieldState fromValue(int value) {
         if (value >= 80) {
            return STABLE;
         } else if (value >= 60) {
            return UNSTABLE;
         } else if (value >= 40) {
            return FRACTURED;
         } else {
            return value >= 20 ? CRITICAL : COLLAPSED;
         }
      }
   }
}

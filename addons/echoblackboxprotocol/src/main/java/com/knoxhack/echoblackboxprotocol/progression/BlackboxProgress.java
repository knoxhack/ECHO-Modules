package com.knoxhack.echoblackboxprotocol.progression;

import com.knoxhack.echoblackboxprotocol.integration.BlackboxMissionHooks;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class BlackboxProgress {
   private static final String ROOT = "echoblackboxprotocol_progress";
   private static final int MEMORY_GATE_COUNT = 2;
   private static final int DELETED_LOG_SECRET_COUNT = 3;
   private final Player player;
   private final CompoundTag root;

   private BlackboxProgress() {
      this.player = null;
      this.root = new CompoundTag();
   }

   private BlackboxProgress(Player player) {
      this.player = player;
      this.root = player.getPersistentData().getCompoundOrEmpty("echoblackboxprotocol_progress");
   }

   public static BlackboxProgress get(Player player) {
      return player == null ? new BlackboxProgress() : new BlackboxProgress(player);
   }

   public boolean decode(Player serverPlayer, MemoryType type) {
      int previous = this.memoryCount(type);
      this.root.putInt(memoryKey(type), previous + 1);
      this.stability(Math.max(0, this.stability() - (type == MemoryType.DELETED ? 12 : 4)));
      this.falseSignals(this.falseSignalCount() + (type == MemoryType.DELETED ? 2 : 1));
      this.save();
      BlackboxMissionHooks.recordMemory(serverPlayer, type);
      return previous == 0;
   }

   public int memoryCount(MemoryType type) {
      return this.root.getIntOr(memoryKey(type), 0);
   }

   public int decodedMemoryTotal() {
      int total = 0;

      for (MemoryType type : MemoryType.values()) {
         total += this.memoryCount(type);
      }

      return total;
   }

   public boolean hasMemory(MemoryType type, int count) {
      return this.memoryCount(type) >= count;
   }

   public boolean hasAllDeletedLogs() {
      return this.memoryCount(MemoryType.DELETED) >= 3;
   }

   public boolean completed(BlackboxDungeon dungeon) {
      return this.root.getBooleanOr("dungeon_" + dungeon.getSerializedName(), false);
   }

   public void completeDungeon(BlackboxDungeon dungeon) {
      this.root.putBoolean("dungeon_" + dungeon.getSerializedName(), true);
      this.save();
      BlackboxMissionHooks.recordDungeon(this.player, dungeon);
   }

   public boolean bossDefeated(String id) {
      return this.root.getBooleanOr("boss_" + id, false);
   }

   public boolean markBossDefeated(String id) {
      String key = "boss_" + id;
      if (this.root.getBooleanOr(key, false)) {
         return false;
      } else {
         this.root.putBoolean(key, true);
         this.save();
         BlackboxMissionHooks.recordBoss(this.player, id);
         return true;
      }
   }

   public boolean canEnter(BlackboxDungeon dungeon) {
      return switch (dungeon) {
         case VAULT -> this.hasMemory(MemoryType.PERSONAL, 2) && this.hasMemory(MemoryType.SECURITY, 2);
         case BUNKER -> this.completed(BlackboxDungeon.VAULT) && this.hasMemory(MemoryType.COMMAND, 2);
         case LABYRINTH -> this.bossDefeated("false_echo") && this.hasMemory(MemoryType.ECHO, 2);
         case TEMPLE -> this.bossDefeated("command_remnant") && this.hasMemory(MemoryType.CORE, 2);
         case CORE_CHAMBER -> this.hasNexusCoreAccessKey();
      };
   }

   public String lockReason(BlackboxDungeon dungeon) {
      return switch (dungeon) {
         case VAULT -> "Decode Personal and Security Logs.";
         case BUNKER -> "Clear the Blackbox Vault and decode Command Logs.";
         case LABYRINTH -> "Defeat The False ECHO and decode ECHO Logs.";
         case TEMPLE -> "Defeat the Command Remnant and decode Core Logs.";
         case CORE_CHAMBER -> "Assemble the Nexus Core Access Key.";
      };
   }

   public boolean hasNexusCoreAccessKey() {
      return this.root.getBooleanOr("nexus_core_access_key", false);
   }

   public void markNexusCoreAccessKey() {
      this.root.putBoolean("nexus_core_access_key", true);
      this.save();
      BlackboxMissionHooks.recordCoreKey(this.player);
   }

   public int stability() {
      return this.root.getIntOr("memory_stability", 100);
   }

   public void stability(int value) {
      this.root.putInt("memory_stability", Math.max(0, Math.min(100, value)));
      this.save();
   }

   public int falseSignalCount() {
      return this.root.getIntOr("false_signal_count", 0);
   }

   public void falseSignals(int value) {
      this.root.putInt("false_signal_count", Math.max(0, value));
      this.save();
   }

   public BlackboxEnding ending() {
      return BlackboxEnding.byName(this.root.getStringOr("ending", "none"));
   }

   public boolean setEnding(BlackboxEnding ending) {
      if (ending != null && ending != BlackboxEnding.NONE && this.ending() == BlackboxEnding.NONE) {
         this.root.putString("ending", ending.getSerializedName());
         this.applyEndingState(ending);
         this.save();
         BlackboxMissionHooks.recordEnding(this.player, ending.getSerializedName());
         return true;
      } else {
         return false;
      }
   }

   private void applyEndingState(BlackboxEnding ending) {
      switch (ending) {
         case RESTORE:
            this.root.putBoolean("restore_stabilized", true);
            this.root.putInt("memory_stability", 100);
            this.root.putInt("false_signal_count", Math.max(0, this.root.getIntOr("false_signal_count", 0) - 6));
            break;
         case CONTROL:
            this.root.putBoolean("corruption_directed", true);
            this.root.putBoolean("echo_distrust", true);
            break;
         case DESTROY:
            this.root.putBoolean("nexus_spread_stopped", true);
            this.root.putInt("false_signal_count", 0);
            break;
         case MERGE:
            this.root.putBoolean("merged_identity", true);
            this.root.putInt("memory_stability", 100);
            this.root.putInt("false_signal_count", 0);
            break;
         case NONE:
            break;
      }
   }

   public boolean restoreStabilized() {
      return this.root.getBooleanOr("restore_stabilized", false);
   }

   public void markRestoreStabilized() {
      this.root.putBoolean("restore_stabilized", true);
      this.save();
   }

   public boolean corruptionDirected() {
      return this.root.getBooleanOr("corruption_directed", false);
   }

   public void markCorruptionDirected() {
      this.root.putBoolean("corruption_directed", true);
      this.save();
   }

   public boolean echoDistrusts() {
      return this.root.getBooleanOr("echo_distrust", false);
   }

   public void markEchoDistrust() {
      this.root.putBoolean("echo_distrust", true);
      this.save();
   }

   public boolean nexusSpreadStopped() {
      return this.root.getBooleanOr("nexus_spread_stopped", false);
   }

   public void markNexusSpreadStopped() {
      this.root.putBoolean("nexus_spread_stopped", true);
      this.save();
   }

   public boolean mergedIdentity() {
      return this.root.getBooleanOr("merged_identity", false);
   }

   public void markMergedIdentity() {
      this.root.putBoolean("merged_identity", true);
      this.save();
   }

   public boolean hasTerminalCacheClaimed(String missionId) {
      return this.terminalClaims().contains(missionId);
   }

   public boolean markTerminalCacheClaimed(String missionId) {
      Set<String> claims = this.terminalClaims();
      if (!claims.add(missionId)) {
         return false;
      } else {
         this.writeSet("terminal_cache", claims);
         this.save();
         return true;
      }
   }

   private Set<String> terminalClaims() {
      return this.readSet("terminal_cache");
   }

   private Set<String> readSet(String prefix) {
      int count = this.root.getIntOr(prefix + "_count", 0);
      LinkedHashSet<String> values = new LinkedHashSet<>();

      for (int i = 0; i < count; i++) {
         String value = this.root.getStringOr(prefix + "_" + i, "");
         if (!value.isBlank()) {
            values.add(value);
         }
      }

      return values;
   }

   private void writeSet(String prefix, Set<String> values) {
      int index = 0;

      for (String value : values) {
         if (value != null && !value.isBlank()) {
            this.root.putString(prefix + "_" + index++, value);
         }
      }

      this.root.putInt(prefix + "_count", index);
   }

   private void save() {
      this.player.getPersistentData().put("echoblackboxprotocol_progress", this.root);
   }

   private static String memoryKey(MemoryType type) {
      return "memory_" + type.getSerializedName();
   }
}

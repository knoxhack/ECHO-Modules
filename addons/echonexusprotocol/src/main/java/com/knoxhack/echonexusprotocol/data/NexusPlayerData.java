package com.knoxhack.echonexusprotocol.data;

import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echonexusprotocol.registry.ModAttachments;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.block.ProtocolSealBlock;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NexusPlayerData implements EchoValueIOSerializable {
   public static final String RESEARCH_NEXUS_THEORY = "nexus_theory";
   public static final String RESEARCH_FIELD_STABILIZATION = "field_stabilization";
   public static final String RESEARCH_MATTER_REWRITING = "matter_rewriting";
   public static final String RESEARCH_MEMORY_RECOVERY = "memory_recovery";
   public static final String RESEARCH_PROTOCOL_AUTOMATION = "protocol_automation";
   public static final String RESEARCH_FORBIDDEN_CORE_ACCESS = "forbidden_core_access";
   public static final int FIELD_MAP_RADIUS = 2;
   public static final int FIELD_MAP_DIAMETER = FIELD_MAP_RADIUS * 2 + 1;
   public static final int FIELD_MAP_SIZE = FIELD_MAP_DIAMETER * FIELD_MAP_DIAMETER;
   public static final StreamCodec<RegistryFriendlyByteBuf, NexusPlayerData> STREAM_CODEC = StreamCodec.of(NexusPlayerData::writeSync, NexusPlayerData::readSync);
   private final Set<String> researchUnlocks = new HashSet<>();
   private final Set<String> scannedIds = new HashSet<>();
   private final Set<String> claimedTerminalCaches = new HashSet<>();
   private final Set<String> usedMachines = new HashSet<>();
   private final Set<String> usedSeals = new HashSet<>();
   private final Set<String> usedGear = new HashSet<>();
   private int blackboxFragments;
   private boolean blackboxMonolithActivated;
   private boolean wardenDefeated;
   private boolean guardianDefeated;
   private boolean coreEntered;
   private String endingPath = "";
   private int armorLockCooldown;
   private boolean hasNexusReturn;
   private String nexusReturnDimension = "minecraft:overworld";
   private double nexusReturnX = 0.5D;
   private double nexusReturnY = 80.0D;
   private double nexusReturnZ = 0.5D;
   private float nexusReturnYRot;
   private float nexusReturnXRot;
   private String finalChoiceState = "";
   private int telemetryFieldValue = NexusWorldData.DEFAULT_FIELD;
   private int telemetryCorruptionPressure;
   private int telemetryQuarantineTicks;
   private boolean telemetryActiveStorm;
   private int telemetryRealityTears;
   private boolean telemetryWorldMonolithActivated;
   private boolean telemetryWorldWardenDefeated;
   private boolean telemetryWorldGuardianDefeated;
   private String telemetryWorldEndingState = "";
   private final int[] telemetryMapFields = defaultFieldMap();
   private final int[] telemetryMapCorruption = new int[FIELD_MAP_SIZE];
   private final int[] telemetryMapTears = new int[FIELD_MAP_SIZE];
   private final boolean[] telemetryMapStorms = new boolean[FIELD_MAP_SIZE];

   public static NexusPlayerData get(Player player) { return player == null ? new NexusPlayerData() : (NexusPlayerData)player.getData(ModAttachments.NEXUS_PLAYER_DATA.get()); }
   public static void saveAndSync(ServerPlayer player, NexusPlayerData data) { player.setData(ModAttachments.NEXUS_PLAYER_DATA.get(), data); player.syncData(ModAttachments.NEXUS_PLAYER_DATA.get()); }
   public boolean unlockResearch(String id) { return this.researchUnlocks.add(clean(id)); }
   public boolean hasResearch(String id) { return this.researchUnlocks.contains(clean(id)); }
   public Set<String> researchUnlocks() { return Set.copyOf(this.researchUnlocks); }
   public boolean markScanned(Identifier id) { return id != null && this.scannedIds.add(id.toString()); }
   public int scanCount() { return this.scannedIds.size(); }
   public Set<String> scannedIds() { return Set.copyOf(this.scannedIds); }
   public boolean markMachineUsed(String id) { return this.usedMachines.add(clean(id)); }
   public boolean markMachineUsed(NexusMachineBlock.MachineKind kind) {
      if (kind == null) return false;
      switch (kind) {
         case NEXUS_RECYCLER, NEXUS_CHARGE_TANK -> this.unlockResearch(RESEARCH_NEXUS_THEORY);
         case CORRUPTION_FILTER, NEXUS_FIELD_STABILIZER -> this.unlockResearch(RESEARCH_FIELD_STABILIZATION);
         case NEXUS_INFUSER, REALITY_FORGE -> this.unlockResearch(RESEARCH_MATTER_REWRITING);
         case MEMORY_DECODER -> this.unlockResearch(RESEARCH_MEMORY_RECOVERY);
         case CORRUPTION_REACTOR -> this.unlockResearch(RESEARCH_FORBIDDEN_CORE_ACCESS);
      }
      return this.markMachineUsed(kind.getSerializedName());
   }
   public boolean hasUsedMachine(String id) { return this.usedMachines.contains(clean(id)); }
   public Set<String> usedMachines() { return Set.copyOf(this.usedMachines); }
   public boolean markSealUsed(String id) {
      boolean changed = this.usedSeals.add(clean(id));
      this.unlockResearch(RESEARCH_PROTOCOL_AUTOMATION);
      return changed;
   }
   public boolean markSealUsed(ProtocolSealBlock.SealMode mode) { return mode != null && this.markSealUsed(mode.getSerializedName()); }
   public boolean hasUsedSeal(String id) { return this.usedSeals.contains(clean(id)); }
   public Set<String> usedSeals() { return Set.copyOf(this.usedSeals); }
   public boolean markGearUsed(String id) { return this.usedGear.add(clean(id)); }
   public boolean hasUsedGear(String id) { return this.usedGear.contains(clean(id)); }
   public Set<String> usedGear() { return Set.copyOf(this.usedGear); }
   public void addBlackboxFragment() { this.blackboxFragments++; this.unlockResearch(RESEARCH_MEMORY_RECOVERY); }
   public int blackboxFragments() { return this.blackboxFragments; }
   public boolean activateBlackboxMonolith() { boolean changed = !this.blackboxMonolithActivated; this.blackboxMonolithActivated = true; this.unlockResearch(RESEARCH_FORBIDDEN_CORE_ACCESS); return changed; }
   public boolean blackboxMonolithActivated() { return this.blackboxMonolithActivated; }
   public void markWardenDefeated() { this.wardenDefeated = true; this.unlockResearch(RESEARCH_FORBIDDEN_CORE_ACCESS); }
   public boolean wardenDefeated() { return this.wardenDefeated; }
   public void markGuardianDefeated() { this.guardianDefeated = true; this.finalChoiceState = this.endingPath.isBlank() ? "ready" : "committed"; }
   public boolean guardianDefeated() { return this.guardianDefeated; }
   public void markCoreEntered() { this.coreEntered = true; this.unlockResearch(RESEARCH_FORBIDDEN_CORE_ACCESS); }
   public boolean coreEntered() { return this.coreEntered; }
   public void setEndingPath(String path) { this.endingPath = clean(path); if (!this.endingPath.isBlank()) { this.guardianDefeated = true; this.finalChoiceState = "committed"; } }
   public String endingPath() { return this.endingPath; }
   public boolean hasEndingPath() { return !this.endingPath.isBlank(); }
   public int armorLockCooldown() { return Math.max(0, this.armorLockCooldown); }
   public void setArmorLockCooldown(int ticks) { this.armorLockCooldown = Math.max(0, ticks); }
   public void tickArmorLockCooldown() { if (this.armorLockCooldown > 0) { this.armorLockCooldown--; } }
   public void setNexusReturn(Player player) { this.hasNexusReturn = true; this.nexusReturnDimension = player.level().dimension().identifier().toString(); this.nexusReturnX = player.getX(); this.nexusReturnY = player.getY(); this.nexusReturnZ = player.getZ(); this.nexusReturnYRot = player.getYRot(); this.nexusReturnXRot = player.getXRot(); }
   public boolean hasNexusReturn() { return this.hasNexusReturn; }
   public void clearNexusReturn() { this.hasNexusReturn = false; }
   public String nexusReturnDimension() { return this.nexusReturnDimension; }
   public double nexusReturnX() { return this.nexusReturnX; }
   public double nexusReturnY() { return this.nexusReturnY; }
   public double nexusReturnZ() { return this.nexusReturnZ; }
   public float nexusReturnYRot() { return this.nexusReturnYRot; }
   public float nexusReturnXRot() { return this.nexusReturnXRot; }
   public String finalChoiceState() { return this.finalChoiceState; }
   public void setFinalChoiceState(String state) { this.finalChoiceState = clean(state); }
   public int telemetryFieldValue() { return Math.max(0, Math.min(100, this.telemetryFieldValue)); }
   public int telemetryCorruptionPressure() { return Math.max(0, Math.min(100, this.telemetryCorruptionPressure)); }
   public int telemetryQuarantineTicks() { return Math.max(0, this.telemetryQuarantineTicks); }
   public boolean telemetryActiveStorm() { return this.telemetryActiveStorm; }
   public int telemetryRealityTears() { return Math.max(0, this.telemetryRealityTears); }
   public boolean telemetryWorldMonolithActivated() { return this.telemetryWorldMonolithActivated || this.blackboxMonolithActivated; }
   public boolean telemetryWorldWardenDefeated() { return this.telemetryWorldWardenDefeated || this.wardenDefeated; }
   public boolean telemetryWorldGuardianDefeated() { return this.telemetryWorldGuardianDefeated || this.guardianDefeated; }
   public String telemetryWorldEndingState() { return this.telemetryWorldEndingState.isBlank() ? this.endingPath : this.telemetryWorldEndingState; }
   public int telemetryFieldMapSize() { return FIELD_MAP_SIZE; }
   public int telemetryMapField(int index) { return index >= 0 && index < FIELD_MAP_SIZE ? Math.max(0, Math.min(100, this.telemetryMapFields[index])) : NexusWorldData.DEFAULT_FIELD; }
   public int telemetryMapCorruption(int index) { return index >= 0 && index < FIELD_MAP_SIZE ? Math.max(0, Math.min(100, this.telemetryMapCorruption[index])) : 0; }
   public int telemetryMapTears(int index) { return index >= 0 && index < FIELD_MAP_SIZE ? Math.max(0, this.telemetryMapTears[index]) : 0; }
   public boolean telemetryMapStorm(int index) { return index >= 0 && index < FIELD_MAP_SIZE && this.telemetryMapStorms[index]; }
   public boolean refreshFieldTelemetry(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      NexusWorldData worldData = NexusWorldData.get(level);
      ChunkPos chunk = player.chunkPosition();
      int interval = Math.max(20, (Integer)Config.FIELD_TICK_INTERVAL.get());
      long stormWindow = interval * (long)Math.max(1, (Integer)Config.STORM_WINDOW_MULTIPLIER.get());
      int field = worldData.fieldValue(chunk);
      int corruption = worldData.corruptionPressure(chunk);
      int quarantine = worldData.quarantineTicks(chunk);
      boolean storm = worldData.hasActiveStorm(chunk, level.getGameTime(), stormWindow);
      int tears = worldData.realityTearCount(chunk);
      boolean monolith = worldData.blackboxMonolithActivated();
      boolean warden = worldData.wardenDefeated();
      boolean guardian = worldData.guardianDefeated();
      String ending = clean(worldData.endingState());
      boolean mapChanged = refreshFieldMap(worldData, chunk, level.getGameTime(), stormWindow);
      boolean changed = this.telemetryFieldValue != field
         || this.telemetryCorruptionPressure != corruption
         || this.telemetryQuarantineTicks != quarantine
         || this.telemetryActiveStorm != storm
         || this.telemetryRealityTears != tears
         || this.telemetryWorldMonolithActivated != monolith
         || this.telemetryWorldWardenDefeated != warden
         || this.telemetryWorldGuardianDefeated != guardian
         || !this.telemetryWorldEndingState.equals(ending)
         || mapChanged;
      this.telemetryFieldValue = field;
      this.telemetryCorruptionPressure = corruption;
      this.telemetryQuarantineTicks = quarantine;
      this.telemetryActiveStorm = storm;
      this.telemetryRealityTears = tears;
      this.telemetryWorldMonolithActivated = monolith;
      this.telemetryWorldWardenDefeated = warden;
      this.telemetryWorldGuardianDefeated = guardian;
      this.telemetryWorldEndingState = ending;
      return changed;
   }
   public boolean markTerminalCacheClaimed(String missionId) { return this.claimedTerminalCaches.add(clean(missionId)); }
   public boolean isTerminalCacheClaimed(String missionId) { return this.claimedTerminalCaches.contains(clean(missionId)); }

   public void serialize(ValueOutput output) {
      writeSet(output, "research", this.researchUnlocks); writeSet(output, "scans", this.scannedIds); writeSet(output, "claimed", this.claimedTerminalCaches); writeSet(output, "usedMachines", this.usedMachines); writeSet(output, "usedSeals", this.usedSeals); writeSet(output, "usedGear", this.usedGear);
      output.putInt("blackboxFragments", this.blackboxFragments); output.putBoolean("blackboxMonolithActivated", this.blackboxMonolithActivated); output.putBoolean("wardenDefeated", this.wardenDefeated); output.putBoolean("guardianDefeated", this.guardianDefeated); output.putBoolean("coreEntered", this.coreEntered); output.putString("endingPath", this.endingPath);
      output.putInt("armorLockCooldown", this.armorLockCooldown); output.putBoolean("hasNexusReturn", this.hasNexusReturn); output.putString("nexusReturnDimension", this.nexusReturnDimension); output.putString("nexusReturnX", Double.toString(this.nexusReturnX)); output.putString("nexusReturnY", Double.toString(this.nexusReturnY)); output.putString("nexusReturnZ", Double.toString(this.nexusReturnZ)); output.putString("nexusReturnYRot", Float.toString(this.nexusReturnYRot)); output.putString("nexusReturnXRot", Float.toString(this.nexusReturnXRot)); output.putString("finalChoiceState", this.finalChoiceState);
      output.putInt("telemetryFieldValue", this.telemetryFieldValue); output.putInt("telemetryCorruptionPressure", this.telemetryCorruptionPressure); output.putInt("telemetryQuarantineTicks", this.telemetryQuarantineTicks); output.putBoolean("telemetryActiveStorm", this.telemetryActiveStorm); output.putInt("telemetryRealityTears", this.telemetryRealityTears); output.putBoolean("telemetryWorldMonolithActivated", this.telemetryWorldMonolithActivated); output.putBoolean("telemetryWorldWardenDefeated", this.telemetryWorldWardenDefeated); output.putBoolean("telemetryWorldGuardianDefeated", this.telemetryWorldGuardianDefeated); output.putString("telemetryWorldEndingState", this.telemetryWorldEndingState);
      for (int i = 0; i < FIELD_MAP_SIZE; i++) { output.putInt("telemetryMapField_" + i, this.telemetryMapFields[i]); output.putInt("telemetryMapCorruption_" + i, this.telemetryMapCorruption[i]); output.putInt("telemetryMapTears_" + i, this.telemetryMapTears[i]); output.putBoolean("telemetryMapStorm_" + i, this.telemetryMapStorms[i]); }
   }

   public void deserialize(ValueInput input) {
      readSet(input, "research", this.researchUnlocks); readSet(input, "scans", this.scannedIds); readSet(input, "claimed", this.claimedTerminalCaches); readSet(input, "usedMachines", this.usedMachines); readSet(input, "usedSeals", this.usedSeals); readSet(input, "usedGear", this.usedGear);
      this.blackboxFragments = Math.max(0, input.getIntOr("blackboxFragments", 0)); this.blackboxMonolithActivated = input.getBooleanOr("blackboxMonolithActivated", false); this.wardenDefeated = input.getBooleanOr("wardenDefeated", false); this.guardianDefeated = input.getBooleanOr("guardianDefeated", false); this.coreEntered = input.getBooleanOr("coreEntered", false); this.endingPath = clean(input.getStringOr("endingPath", ""));
      this.armorLockCooldown = Math.max(0, input.getIntOr("armorLockCooldown", 0)); this.hasNexusReturn = input.getBooleanOr("hasNexusReturn", false); this.nexusReturnDimension = input.getStringOr("nexusReturnDimension", "minecraft:overworld"); this.nexusReturnX = parseDouble(input.getStringOr("nexusReturnX", "0.5"), 0.5D); this.nexusReturnY = parseDouble(input.getStringOr("nexusReturnY", "80"), 80.0D); this.nexusReturnZ = parseDouble(input.getStringOr("nexusReturnZ", "0.5"), 0.5D); this.nexusReturnYRot = parseFloat(input.getStringOr("nexusReturnYRot", "0"), 0.0F); this.nexusReturnXRot = parseFloat(input.getStringOr("nexusReturnXRot", "0"), 0.0F); this.finalChoiceState = clean(input.getStringOr("finalChoiceState", ""));
      this.telemetryFieldValue = Math.max(0, Math.min(100, input.getIntOr("telemetryFieldValue", NexusWorldData.DEFAULT_FIELD))); this.telemetryCorruptionPressure = Math.max(0, Math.min(100, input.getIntOr("telemetryCorruptionPressure", 0))); this.telemetryQuarantineTicks = Math.max(0, input.getIntOr("telemetryQuarantineTicks", 0)); this.telemetryActiveStorm = input.getBooleanOr("telemetryActiveStorm", false); this.telemetryRealityTears = Math.max(0, input.getIntOr("telemetryRealityTears", 0)); this.telemetryWorldMonolithActivated = input.getBooleanOr("telemetryWorldMonolithActivated", false); this.telemetryWorldWardenDefeated = input.getBooleanOr("telemetryWorldWardenDefeated", false); this.telemetryWorldGuardianDefeated = input.getBooleanOr("telemetryWorldGuardianDefeated", false); this.telemetryWorldEndingState = clean(input.getStringOr("telemetryWorldEndingState", ""));
      for (int i = 0; i < FIELD_MAP_SIZE; i++) { this.telemetryMapFields[i] = Math.max(0, Math.min(100, input.getIntOr("telemetryMapField_" + i, NexusWorldData.DEFAULT_FIELD))); this.telemetryMapCorruption[i] = Math.max(0, Math.min(100, input.getIntOr("telemetryMapCorruption_" + i, 0))); this.telemetryMapTears[i] = Math.max(0, input.getIntOr("telemetryMapTears_" + i, 0)); this.telemetryMapStorms[i] = input.getBooleanOr("telemetryMapStorm_" + i, false); }
   }

   private static void writeSync(RegistryFriendlyByteBuf buf, NexusPlayerData data) {
      writeBufferSet(buf, data.researchUnlocks); writeBufferSet(buf, data.scannedIds); writeBufferSet(buf, data.claimedTerminalCaches); writeBufferSet(buf, data.usedMachines); writeBufferSet(buf, data.usedSeals); writeBufferSet(buf, data.usedGear);
      buf.writeVarInt(data.blackboxFragments); buf.writeBoolean(data.blackboxMonolithActivated); buf.writeBoolean(data.wardenDefeated); buf.writeBoolean(data.guardianDefeated); buf.writeBoolean(data.coreEntered); buf.writeUtf(data.endingPath);
      buf.writeVarInt(data.armorLockCooldown); buf.writeBoolean(data.hasNexusReturn); buf.writeUtf(data.nexusReturnDimension); buf.writeDouble(data.nexusReturnX); buf.writeDouble(data.nexusReturnY); buf.writeDouble(data.nexusReturnZ); buf.writeFloat(data.nexusReturnYRot); buf.writeFloat(data.nexusReturnXRot); buf.writeUtf(data.finalChoiceState);
      buf.writeVarInt(data.telemetryFieldValue); buf.writeVarInt(data.telemetryCorruptionPressure); buf.writeVarInt(data.telemetryQuarantineTicks); buf.writeBoolean(data.telemetryActiveStorm); buf.writeVarInt(data.telemetryRealityTears); buf.writeBoolean(data.telemetryWorldMonolithActivated); buf.writeBoolean(data.telemetryWorldWardenDefeated); buf.writeBoolean(data.telemetryWorldGuardianDefeated); buf.writeUtf(data.telemetryWorldEndingState);
      for (int i = 0; i < FIELD_MAP_SIZE; i++) { buf.writeVarInt(data.telemetryMapFields[i]); buf.writeVarInt(data.telemetryMapCorruption[i]); buf.writeVarInt(data.telemetryMapTears[i]); buf.writeBoolean(data.telemetryMapStorms[i]); }
   }

   private static NexusPlayerData readSync(RegistryFriendlyByteBuf buf) {
      NexusPlayerData data = new NexusPlayerData(); readBufferSet(buf, data.researchUnlocks); readBufferSet(buf, data.scannedIds); readBufferSet(buf, data.claimedTerminalCaches); readBufferSet(buf, data.usedMachines); readBufferSet(buf, data.usedSeals); readBufferSet(buf, data.usedGear);
      data.blackboxFragments = buf.readVarInt(); data.blackboxMonolithActivated = buf.readBoolean(); data.wardenDefeated = buf.readBoolean(); data.guardianDefeated = buf.readBoolean(); data.coreEntered = buf.readBoolean(); data.endingPath = clean(buf.readUtf());
      data.armorLockCooldown = buf.readVarInt(); data.hasNexusReturn = buf.readBoolean(); data.nexusReturnDimension = buf.readUtf(); data.nexusReturnX = buf.readDouble(); data.nexusReturnY = buf.readDouble(); data.nexusReturnZ = buf.readDouble(); data.nexusReturnYRot = buf.readFloat(); data.nexusReturnXRot = buf.readFloat(); data.finalChoiceState = clean(buf.readUtf());
      data.telemetryFieldValue = buf.readVarInt(); data.telemetryCorruptionPressure = buf.readVarInt(); data.telemetryQuarantineTicks = buf.readVarInt(); data.telemetryActiveStorm = buf.readBoolean(); data.telemetryRealityTears = buf.readVarInt(); data.telemetryWorldMonolithActivated = buf.readBoolean(); data.telemetryWorldWardenDefeated = buf.readBoolean(); data.telemetryWorldGuardianDefeated = buf.readBoolean(); data.telemetryWorldEndingState = clean(buf.readUtf());
      for (int i = 0; i < FIELD_MAP_SIZE; i++) { data.telemetryMapFields[i] = buf.readVarInt(); data.telemetryMapCorruption[i] = buf.readVarInt(); data.telemetryMapTears[i] = buf.readVarInt(); data.telemetryMapStorms[i] = buf.readBoolean(); }
      return data;
   }

   private boolean refreshFieldMap(NexusWorldData worldData, ChunkPos center, long gameTime, long stormWindow) {
      int visibleRadius = Math.max(1, Math.min(FIELD_MAP_RADIUS, (Integer)Config.FIELD_MAP_RADIUS.get()));
      boolean changed = false;
      int index = 0;
      for (int dz = -FIELD_MAP_RADIUS; dz <= FIELD_MAP_RADIUS; dz++) {
         for (int dx = -FIELD_MAP_RADIUS; dx <= FIELD_MAP_RADIUS; dx++) {
            boolean visible = Math.abs(dx) <= visibleRadius && Math.abs(dz) <= visibleRadius;
            ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
            int field = visible ? worldData.fieldValue(pos) : NexusWorldData.DEFAULT_FIELD;
            int corruption = visible ? worldData.corruptionPressure(pos) : 0;
            int tears = visible ? worldData.realityTearCount(pos) : 0;
            boolean storm = visible && worldData.hasActiveStorm(pos, gameTime, stormWindow);
            changed |= this.telemetryMapFields[index] != field
               || this.telemetryMapCorruption[index] != corruption
               || this.telemetryMapTears[index] != tears
               || this.telemetryMapStorms[index] != storm;
            this.telemetryMapFields[index] = field;
            this.telemetryMapCorruption[index] = corruption;
            this.telemetryMapTears[index] = tears;
            this.telemetryMapStorms[index] = storm;
            index++;
         }
      }
      return changed;
   }

   private static int[] defaultFieldMap() { int[] values = new int[FIELD_MAP_SIZE]; Arrays.fill(values, NexusWorldData.DEFAULT_FIELD); return values; }
   private static void writeSet(ValueOutput output, String key, Set<String> values) { output.putInt(key + "Count", values.size()); int index = 0; for (String value : values) { output.putString(key + "_" + index++, value); } }
   private static void readSet(ValueInput input, String key, Set<String> values) { values.clear(); int count = input.getIntOr(key + "Count", 0); for (int i = 0; i < count; i++) { String value = clean(input.getStringOr(key + "_" + i, "")); if (!value.isBlank()) { values.add(value); } } }
   private static void writeBufferSet(RegistryFriendlyByteBuf buf, Set<String> values) { buf.writeVarInt(values.size()); for (String value : values) { buf.writeUtf(value); } }
   private static void readBufferSet(RegistryFriendlyByteBuf buf, Set<String> values) { values.clear(); int count = buf.readVarInt(); for (int i = 0; i < count; i++) { String value = clean(buf.readUtf()); if (!value.isBlank()) { values.add(value); } } }
   private static double parseDouble(String value, double fallback) { try { return Double.parseDouble(value); } catch (NumberFormatException ex) { return fallback; } }
   private static float parseFloat(String value, float fallback) { try { return Float.parseFloat(value); } catch (NumberFormatException ex) { return fallback; } }
   private static String clean(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}

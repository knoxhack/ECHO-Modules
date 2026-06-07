package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.event.PostNexusEventHandler;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import java.util.*;

/**
 * Quest/mission tracking data for the ECHO-7 AI guide system.
 * Expanded to support conversation history and asset discovery.
 */
public class QuestData implements EchoValueIOSerializable {
    private static final Identifier NEXUS_CORE_FOUND_ADVANCEMENT =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "nexus_core_found");
    private static final String NEXUS_CORE_FOUND_CRITERION = "found_nexus_core";

    private int currentPhase = 0;
    private int currentMissionIndex = 0;
    private boolean echoIntroPlayed = false;
    private long lastMessageTick = 0;
    private int introStep = 0;
    private boolean dropPodInitialized = false;
    private long nextObjectiveHintTick = 0;
    private long nextDroneChatterTick = 0;
    private String lastBiomeId = "";
    private String seenEntities = "";
    
    // New Fields for Modernization
    private String discoveredAssets = ""; // Comma-separated IDs
    private List<String> archive = new ArrayList<>(); // AI Message History
    private static final int MAX_ARCHIVE_SIZE = 40;
    
    // ECHO Terminal System Fields
    private int terminalHealth = 100; // 0-100 durability
    private boolean terminalOnline = true;
    private long lastTerminalInteraction = 0;
    private int echoRelationship = 0; // -100 to 100, affects dialogue tone
    private boolean droneUnlocked = true; // Unlocked by default - starts damaged
    private boolean droneDeployed = false;
    private String lastTerminalDimension = "";
    
    // Drone Repair System Fields
    private int droneHealth = 15; // 0-100, starts at 15% (damaged in crash)

    // Exploration & Endgame Tracking
    private final Set<String> discoveredPOIs = new HashSet<>();
    private final Set<String> poiObjectiveStates = new HashSet<>();
    private int collectedPowerNodes = 0;
    
    // Entity Kill Tracking for Mission Requirements
    private final Map<String, Integer> entityKillCounts = new HashMap<>();

    // Block Placement Tracking for Mission Requirements (lifetime count, not live)
    private final Map<String, Integer> placedBlockCounts = new HashMap<>();
    
    // Location Visit Tracking for Mission Requirements (biomes, dimensions, special locations)
    private final Set<String> visitedBiomes = new HashSet<>();
    private final Set<String> visitedDimensions = new HashSet<>();
    private final Set<String> visitedSpecialLocations = new HashSet<>();
    
    public enum DroneRepairStage {
        BROKEN(0, 25),      // 0-25%: Follow only, slow
        PARTIAL(25, 50),    // 25-50%: Follow + Scout, light unlocked
        OPERATIONAL(50, 75), // 50-75%: Follow, Scout, Combat, Scavenge
        ENHANCED(75, 100);   // 75-100%: All modes, full speed, full inventory
        
        private final int minHealth, maxHealth;
        DroneRepairStage(int min, int max) { this.minHealth = min; this.maxHealth = max; }
        public int getMinHealth() { return minHealth; }
        public int getMaxHealth() { return maxHealth; }
        
        public static DroneRepairStage fromHealth(int health) {
            for (DroneRepairStage stage : values()) {
                if (health >= stage.minHealth && health < stage.maxHealth) return stage;
            }
            return ENHANCED; // 100% or above
        }
    }
    private DroneRepairStage droneStage = DroneRepairStage.BROKEN;
    
    // Mission System Tracking - Full Feature
    private final Set<String> completedMissionIds = new HashSet<>();
    private final Set<String> unlockedMissionIds = new HashSet<>();
    private final Map<String, List<ItemStack>> pendingRewards = new HashMap<>();
    private String selectedMissionId = ""; // Currently selected mission in UI
    
    public QuestData() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestData> STREAM_CODEC = StreamCodec.of(
            QuestData::writeSync,
            QuestData::readSync
    );

    private static void writeSync(RegistryFriendlyByteBuf buf, QuestData data) {
        buf.writeVarInt(data.currentPhase);
        buf.writeVarInt(data.currentMissionIndex);
        buf.writeBoolean(data.echoIntroPlayed);
        buf.writeLong(data.lastMessageTick);
        buf.writeVarInt(data.introStep);
        buf.writeBoolean(data.dropPodInitialized);
        buf.writeLong(data.nextObjectiveHintTick);
        buf.writeLong(data.nextDroneChatterTick);
        buf.writeUtf(data.lastBiomeId);
        buf.writeUtf(data.seenEntities);
        buf.writeUtf(data.discoveredAssets);
        writeStringList(buf, data.archive);

        buf.writeVarInt(data.terminalHealth);
        buf.writeBoolean(data.terminalOnline);
        buf.writeLong(data.lastTerminalInteraction);
        buf.writeVarInt(data.echoRelationship);
        buf.writeBoolean(data.droneUnlocked);
        buf.writeBoolean(data.droneDeployed);
        buf.writeUtf(data.lastTerminalDimension);
        buf.writeVarInt(data.droneHealth);
        buf.writeUtf(data.droneStage.name());

        writeStringSet(buf, data.completedMissionIds);
        writeStringSet(buf, data.unlockedMissionIds);
        writeRewards(buf, data.pendingRewards);
        buf.writeUtf(data.selectedMissionId);
        writeStringSet(buf, data.turnInReminders);

        writeStringSet(buf, data.discoveredPOIs);
        writeStringSet(buf, data.poiObjectiveStates);
        buf.writeVarInt(data.collectedPowerNodes);
        writeStringIntMap(buf, data.entityKillCounts);
        writeStringIntMap(buf, data.placedBlockCounts);
        writeStringSet(buf, data.visitedBiomes);
        writeStringSet(buf, data.visitedDimensions);
        writeStringSet(buf, data.visitedSpecialLocations);
    }

    private static QuestData readSync(RegistryFriendlyByteBuf buf) {
        QuestData data = new QuestData();
        data.currentPhase = buf.readVarInt();
        data.currentMissionIndex = buf.readVarInt();
        data.echoIntroPlayed = buf.readBoolean();
        data.lastMessageTick = buf.readLong();
        data.introStep = buf.readVarInt();
        data.dropPodInitialized = buf.readBoolean();
        data.nextObjectiveHintTick = buf.readLong();
        data.nextDroneChatterTick = buf.readLong();
        data.lastBiomeId = buf.readUtf();
        data.seenEntities = buf.readUtf();
        data.discoveredAssets = buf.readUtf();
        readStringList(buf, data.archive);

        data.terminalHealth = buf.readVarInt();
        data.terminalOnline = buf.readBoolean();
        data.lastTerminalInteraction = buf.readLong();
        data.echoRelationship = buf.readVarInt();
        data.droneUnlocked = buf.readBoolean();
        data.droneDeployed = buf.readBoolean();
        data.lastTerminalDimension = buf.readUtf();
        data.droneHealth = buf.readVarInt();
        try {
            data.droneStage = DroneRepairStage.valueOf(buf.readUtf());
        } catch (IllegalArgumentException e) {
            data.droneStage = DroneRepairStage.fromHealth(data.droneHealth);
        }

        readStringSet(buf, data.completedMissionIds);
        readStringSet(buf, data.unlockedMissionIds);
        readRewards(buf, data.pendingRewards);
        data.selectedMissionId = buf.readUtf();
        readStringSet(buf, data.turnInReminders);

        readStringSet(buf, data.discoveredPOIs);
        readStringSet(buf, data.poiObjectiveStates);
        normalizePOIProgress(data);
        data.collectedPowerNodes = buf.readVarInt();
        readStringIntMap(buf, data.entityKillCounts);
        readStringIntMap(buf, data.placedBlockCounts);
        readStringSet(buf, data.visitedBiomes);
        readStringSet(buf, data.visitedDimensions);
        readStringSet(buf, data.visitedSpecialLocations);
        return data;
    }

    private static void writeStringList(RegistryFriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringList(RegistryFriendlyByteBuf buf, List<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            values.add(buf.readUtf());
        }
    }

    private static void writeStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf();
            if (!value.isEmpty()) values.add(value);
        }
    }

    private static void writeStringIntMap(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    private static void readStringIntMap(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            int value = buf.readVarInt();
            if (!key.isEmpty() && value > 0) values.put(key, value);
        }
    }

    private static void writeRewards(RegistryFriendlyByteBuf buf, Map<String, List<ItemStack>> rewards) {
        buf.writeVarInt(rewards.size());
        for (Map.Entry<String, List<ItemStack>> entry : rewards.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (ItemStack stack : entry.getValue()) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            }
        }
    }

    private static void readRewards(RegistryFriendlyByteBuf buf, Map<String, List<ItemStack>> rewards) {
        rewards.clear();
        int missionCount = buf.readVarInt();
        for (int i = 0; i < missionCount; i++) {
            String missionId = buf.readUtf();
            int rewardCount = buf.readVarInt();
            List<ItemStack> stacks = new ArrayList<>();
            for (int j = 0; j < rewardCount; j++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            if (!missionId.isEmpty() && !stacks.isEmpty()) {
                rewards.put(missionId, stacks);
            }
        }
    }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int phase) { currentPhase = phase; }
    public int getCurrentMissionIndex() { return currentMissionIndex; }
    public void setCurrentMissionIndex(int index) { currentMissionIndex = index; }
    public boolean isEchoIntroPlayed() { return echoIntroPlayed; }
    public void setEchoIntroPlayed(boolean played) { echoIntroPlayed = played; }
    public long getLastMessageTick() { return lastMessageTick; }
    public void setLastMessageTick(long tick) { lastMessageTick = tick; }
    public long getNextDroneChatterTick() { return nextDroneChatterTick; }
    public void setNextDroneChatterTick(long tick) { nextDroneChatterTick = tick; }
    public int getIntroStep() { return introStep; }
    public void setIntroStep(int step) { introStep = step; }
    public boolean isDropPodInitialized() { return dropPodInitialized; }
    public void setDropPodInitialized(boolean initialized) { dropPodInitialized = initialized; }
    public long getNextObjectiveHintTick() { return nextObjectiveHintTick; }
    public void setNextObjectiveHintTick(long tick) { nextObjectiveHintTick = tick; }
    public String getLastBiomeId() { return lastBiomeId; }
    public void setLastBiomeId(String id) { lastBiomeId = id; }
    
    public boolean hasSeenEntity(String entityId) {
        return seenEntities.contains(entityId + ",");
    }
    
    public void addSeenEntity(String entityId) {
        if (!hasSeenEntity(entityId)) {
            seenEntities += entityId + ",";
        }
    }

    // --- Discovery & Archive logic ---
    
    public boolean isAssetDiscovered(String assetId) {
        return discoveredAssets.contains(assetId + ",");
    }

    public void discoverAsset(String assetId) {
        if (!isAssetDiscovered(assetId)) {
            discoveredAssets += assetId + ",";
        }
    }

    public List<String> getArchive() { return archive; }

    public void addToArchive(String message) {
        archive.add(0, message); // Add to top
        if (archive.size() > MAX_ARCHIVE_SIZE) {
            archive.remove(archive.size() - 1);
        }
    }

    public void advanceMission() { currentMissionIndex++; }
    public void advancePhase() { currentPhase++; currentMissionIndex = 0; }
    
    // --- Mission System Methods - Full Feature ---
    
    /**
     * Mark a mission as completed and store its rewards
     */
    public void completeMission(String missionId, List<ItemStack> rewards) {
        completedMissionIds.add(missionId);
        
        // Store rewards for claiming
        if (!rewards.isEmpty()) {
            pendingRewards.put(missionId, new ArrayList<>(rewards));
        }

        reconcileDeprecatedMissionAliases();
        
        // Check for newly unlocked missions
        updateUnlockedMissions();
    }

    public void completeMission(ServerPlayer player, String missionId, List<ItemStack> rewards) {
        completedMissionIds.add(missionId);

        if (!rewards.isEmpty()) {
            pendingRewards.put(missionId, new ArrayList<>(rewards));
        }

        reconcileDeprecatedMissionAliases();
        updateUnlockedMissions(player);
        if ("find_nexus_core".equals(missionId)) {
            awardNexusCoreFoundAdvancement(player);
        }
        if (isPostNexusEpilogueMission(missionId)) {
            PostNexusEventHandler.completeFinalProtocol(player, missionId);
        }
    }

    private static void awardNexusCoreFoundAdvancement(ServerPlayer player) {
        var holder = player.level().getServer().getAdvancements().get(NEXUS_CORE_FOUND_ADVANCEMENT);
        if (holder != null) {
            player.getAdvancements().award(holder, NEXUS_CORE_FOUND_CRITERION);
        }
    }
    
    /**
     * Check if a specific mission is completed
     */
    public boolean isMissionCompleted(String missionId) {
        return completedMissionIds.contains(missionId);
    }

    private static boolean isPostNexusEpilogueMission(String missionId) {
        return "restore_epilogue".equals(missionId)
                || "destroy_epilogue".equals(missionId)
                || "control_epilogue".equals(missionId);
    }
    
    /**
     * Get all completed mission IDs
     */
    public Set<String> getCompletedMissionIds() {
        return Collections.unmodifiableSet(completedMissionIds);
    }
    
    /**
     * Check if a mission is unlocked (prerequisites met)
     */
    public boolean isMissionUnlocked(String missionId) {
        return unlockedMissionIds.contains(missionId) || isMissionCompleted(missionId);
    }
    
    /**
     * Unlock a mission (called when prerequisites are met)
     */
    public void unlockMission(String missionId) {
        unlockedMissionIds.add(missionId);
    }
    
    /**
     * Get all unlocked mission IDs
     */
    public Set<String> getUnlockedMissionIds() {
        return Collections.unmodifiableSet(unlockedMissionIds);
    }
    
    /**
     * Update unlocked missions based on prerequisites
     */
    public boolean updateUnlockedMissions() {
        return updateUnlockedMissions(null);
    }

    public boolean updateUnlockedMissions(ServerPlayer player) {
        boolean changed = false;
        Set<String> allowedUnlocked = new HashSet<>();
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            if (!arePreviousPhasesCompleted(phase, player)) {
                continue;
            }
            List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
            for (Mission mission : missions) {
                if (!isMissionCompleted(mission.id())
                        && isMissionRelevantForPlayer(mission, player)
                        && canStartForRoute(mission)) {
                    allowedUnlocked.add(mission.id());
                }
            }
        }
        if (unlockedMissionIds.removeIf(id -> !isMissionCompleted(id) && !allowedUnlocked.contains(id))) {
            changed = true;
        }
        for (String missionId : allowedUnlocked) {
            if (unlockedMissionIds.add(missionId)) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean isPhaseCompleted(int phase) {
        return isPhaseCompleted(phase, null);
    }

    public boolean isPhaseCompleted(int phase, ServerPlayer player) {
        int missionCount = MissionRegistry.getMissionCount(phase);
        if (missionCount <= 0) return false;
        boolean hasRelevantMission = false;
        for (Mission mission : MissionRegistry.getMissionsForPhase(phase)) {
            if (!isMissionRelevantForPlayer(mission, player)) {
                continue;
            }
            if (!AshfallMissionRoute.blocksPhase(mission)) {
                continue;
            }
            hasRelevantMission = true;
            if (!isMissionCompleted(mission.id())) {
                return false;
            }
        }
        return hasRelevantMission;
    }

    private boolean arePreviousPhasesCompleted(int phase) {
        return arePreviousPhasesCompleted(phase, null);
    }

    private boolean arePreviousPhasesCompleted(int phase, ServerPlayer player) {
        for (int p = 0; p < phase; p++) {
            if (!isPhaseCompleted(p, player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Repair old or partially synced quest data so current mission and unlock
     * state agree with completed prerequisites.
     */
    public boolean repairMissionState() {
        return repairMissionState(null);
    }

    public boolean repairMissionState(ServerPlayer player) {
        boolean changed = false;
        int phaseCount = MissionRegistry.getPhaseCount();
        if (phaseCount <= 0) return false;

        if (reconcileDeprecatedMissionAliases()) {
            changed = true;
        }

        int clampedPhase = Math.max(0, Math.min(currentPhase, phaseCount - 1));
        if (clampedPhase != currentPhase) {
            currentPhase = clampedPhase;
            changed = true;
        }

        int missionCount = MissionRegistry.getMissionCount(currentPhase);
        if (missionCount > 0) {
            int clampedMission = Math.max(0, Math.min(currentMissionIndex, missionCount - 1));
            if (clampedMission != currentMissionIndex) {
                currentMissionIndex = clampedMission;
                changed = true;
            }
        }

        if (updateUnlockedMissions(player)) {
            changed = true;
        }

        int repairedPhase = currentPhase;
        int repairedMissionIndex = currentMissionIndex;
        boolean foundIncomplete = false;

        for (int phase = 0; phase < phaseCount && !foundIncomplete; phase++) {
            List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
            for (int missionIndex = 0; missionIndex < missions.size(); missionIndex++) {
                Mission mission = missions.get(missionIndex);
                if (!isMissionCompleted(mission.id())
                        && AshfallMissionRoute.blocksPhase(mission)
                        && isMissionRelevantForPlayer(mission, player)
                        && arePreviousPhasesCompleted(phase, player)
                        && canStartForRoute(mission)) {
                    repairedPhase = phase;
                    repairedMissionIndex = missionIndex;
                    foundIncomplete = true;
                    break;
                }
            }
        }

        if (!foundIncomplete) {
            int[] lastRelevant = findLastRelevantMission(player);
            repairedPhase = lastRelevant[0];
            repairedMissionIndex = lastRelevant[1];
        }

        if (repairedPhase != currentPhase || repairedMissionIndex != currentMissionIndex) {
            currentPhase = repairedPhase;
            currentMissionIndex = repairedMissionIndex;
            changed = true;
        }

        Mission current = MissionRegistry.getMission(currentPhase, currentMissionIndex);
        if (current != null
                && !isMissionCompleted(current.id())
                && AshfallMissionRoute.blocksPhase(current)
                && isMissionRelevantForPlayer(current, player)
                && arePreviousPhasesCompleted(currentPhase, player)
                && canStartForRoute(current)) {
            if (unlockedMissionIds.add(current.id())) {
                changed = true;
            }
        }

        return changed;
    }

    private boolean canStartForRoute(Mission mission) {
        if (AshfallMissionRoute.blocksPhase(mission)) {
            return AshfallMissionRoute.canStartMainSpine(mission, completedMissionIds);
        }
        return mission != null && mission.canStart(completedMissionIds);
    }

    private boolean reconcileDeprecatedMissionAliases() {
        boolean changed = false;
        if (completedMissionIds.contains("get_dirty_water")
                && visitedSpecialLocations.add("water:dirty_collected")) {
            changed = true;
        }
        if (completedMissionIds.contains("emergency_filter_water")
                && visitedSpecialLocations.add("water:emergency_filtered")) {
            changed = true;
        }
        for (String replacementMissionId : AshfallMissionRoute.replacementMissionIds()) {
            boolean replacementComplete = completedMissionIds.contains(replacementMissionId);
            boolean legacyComplete = AshfallMissionRoute.allDeprecatedAliasesComplete(completedMissionIds, replacementMissionId);
            if (!replacementComplete && legacyComplete && completedMissionIds.add(replacementMissionId)) {
                changed = true;
                replacementComplete = true;
            }
            if (replacementComplete) {
                for (String deprecatedMissionId : AshfallMissionRoute.deprecatedAliasesFor(replacementMissionId)) {
                    if (completedMissionIds.add(deprecatedMissionId)) {
                        changed = true;
                    }
                }
            }
        }
        if (unlockedMissionIds.removeIf(AshfallMissionRoute::isDeprecated)) {
            changed = true;
        }
        if (turnInReminders.removeIf(AshfallMissionRoute::isDeprecated)) {
            changed = true;
        }
        if (AshfallMissionRoute.isDeprecated(selectedMissionId)) {
            String replacementMissionId = AshfallMissionRoute.replacementForDeprecated(selectedMissionId);
            selectedMissionId = replacementMissionId == null ? "" : replacementMissionId;
            changed = true;
        }
        return changed;
    }

    private boolean isMissionRelevantForPlayer(Mission mission, ServerPlayer player) {
        if (mission == null || player == null || !mission.isPathRestricted()) return true;
        PostNexusData post = PostNexusData.get(player);
        return post.hasMadeChoice() && post.getSelectedPath() == mission.requiredPath();
    }

    private int[] findLastRelevantMission(ServerPlayer player) {
        for (int phase = MissionRegistry.getPhaseCount() - 1; phase >= 0; phase--) {
            List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
            for (int i = missions.size() - 1; i >= 0; i--) {
                Mission mission = missions.get(i);
                if (AshfallMissionRoute.blocksPhase(mission) && isMissionRelevantForPlayer(mission, player)) {
                    return new int[] { phase, i };
                }
            }
        }
        return new int[] { 0, 0 };
    }

    public static void saveAndSync(ServerPlayer player, QuestData quest) {
        player.setData(ModAttachments.QUEST_DATA.get(), quest);
        player.syncData(ModAttachments.QUEST_DATA.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.QUEST_DATA.get());
    }
    
    /**
     * Check if there are pending rewards for a mission
     */
    public boolean hasPendingRewards(String missionId) {
        return pendingRewards.containsKey(missionId);
    }
    
    // --- Turn-in Reminder Tracking ---
    
    private final java.util.Set<String> turnInReminders = new HashSet<>();
    
    /**
     * Check if a turn-in reminder has been sent for this mission
     */
    public boolean hasTurnInReminder(String missionId) {
        return turnInReminders.contains(missionId);
    }
    
    /**
     * Mark that a turn-in reminder has been sent for this mission
     */
    public void setTurnInReminder(String missionId) {
        turnInReminders.add(missionId);
    }

    /**
     * Clear turn-in reminder marker for a mission.
     */
    public void clearTurnInReminder(String missionId) {
        turnInReminders.remove(missionId);
    }
    
    /**
     * Get pending rewards for a mission
     */
    public List<ItemStack> getPendingRewards(String missionId) {
        return pendingRewards.getOrDefault(missionId, Collections.emptyList());
    }
    
    /**
     * Get all missions with pending rewards
     */
    public Map<String, List<ItemStack>> getAllPendingRewards() {
        return Collections.unmodifiableMap(pendingRewards);
    }
    
    /**
     * Claim rewards for a mission (removes them from pending)
     */
    public List<ItemStack> claimRewards(String missionId) {
        List<ItemStack> rewards = pendingRewards.remove(missionId);
        return rewards != null ? rewards : Collections.emptyList();
    }
    
    /**
     * Set the currently selected mission ID (for UI)
     */
    public void setSelectedMissionId(String missionId) {
        selectedMissionId = missionId != null ? missionId : "";
    }

    // --- Exploration & Endgame Tracking ---

    /**
     * Mark a Point of Interest (POI) as discovered
     */
    public void discoverPOI(String poiId) {
        discoveredPOIs.add(ExplorationSiteRegistry.normalize(poiId));
    }

    /**
     * Check if a POI has been discovered
     */
    public boolean isPOIDiscovered(String poiId) {
        String normalized = ExplorationSiteRegistry.normalize(poiId);
        if (discoveredPOIs.contains(normalized)) {
            return true;
        }
        for (String alias : ExplorationSiteRegistry.aliasesFor(normalized)) {
            if (discoveredPOIs.contains(alias)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Alias for isPOIDiscovered
     */
    public boolean hasDiscoveredPOI(String poiId) {
        return isPOIDiscovered(poiId);
    }

    /**
     * Get count of discovered POIs
     */
    public int getDiscoveredPOICount() {
        Set<String> normalized = new HashSet<>();
        for (String poiId : discoveredPOIs) {
            normalized.add(ExplorationSiteRegistry.normalize(poiId));
        }
        return normalized.size();
    }

    /**
     * Get all discovered POI IDs
     */
    public Set<String> getDiscoveredPOIs() {
        Set<String> normalized = new LinkedHashSet<>();
        for (String poiId : discoveredPOIs) {
            normalized.add(ExplorationSiteRegistry.normalize(poiId));
        }
        return Collections.unmodifiableSet(normalized);
    }

    public enum POIObjectiveState {
        SCANNED("Scanned"),
        ENTERED("Entered"),
        CACHE_LOOTED("Cache looted"),
        DATA_RECOVERED("Data recovered"),
        SAMPLE_RECOVERED("Sample recovered"),
        BOSS_DEFEATED("Boss defeated"),
        CLEARED("Cleared"),
        REWARD_CLAIMED("Reward claimed");

        private final String displayName;

        POIObjectiveState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public void recordPOIState(String poiId, POIObjectiveState state) {
        if (poiId == null || poiId.isEmpty() || state == null) return;
        String normalized = ExplorationSiteRegistry.normalize(poiId);
        poiObjectiveStates.add(normalized + ":" + state.name());
        if (state != POIObjectiveState.REWARD_CLAIMED) {
            discoveredPOIs.add(normalized);
        }
    }

    public boolean hasPOIState(String poiId, POIObjectiveState state) {
        if (poiId == null || poiId.isEmpty() || state == null) return false;
        String normalized = ExplorationSiteRegistry.normalize(poiId);
        if (poiObjectiveStates.contains(normalized + ":" + state.name())) {
            return true;
        }
        for (String alias : ExplorationSiteRegistry.aliasesFor(normalized)) {
            if (poiObjectiveStates.contains(alias + ":" + state.name())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getPOIObjectiveStates() {
        return Collections.unmodifiableSet(poiObjectiveStates);
    }

    public String getPOIStateSummary(String poiId) {
        if (poiId == null || poiId.isEmpty()) return "No field state";
        String normalized = ExplorationSiteRegistry.normalize(poiId);
        POIObjectiveState best = null;
        for (POIObjectiveState state : POIObjectiveState.values()) {
            if (hasPOIState(normalized, state)) {
                best = state;
            }
        }
        return best != null ? best.getDisplayName() : isPOIDiscovered(normalized) ? "Discovered" : "Unscanned";
    }

    // --- Entity Kill Tracking ---

    /**
     * Record a kill for a specific entity type
     */
    public void recordEntityKill(String entityType) {
        String normalized = normalizeRegistryId(entityType);
        entityKillCounts.merge(normalized, 1, Integer::sum);
    }

    /**
     * Get the number of kills for a specific entity type
     */
    public int getEntityKills(String entityType) {
        String normalized = normalizeRegistryId(entityType);
        int direct = entityKillCounts.getOrDefault(normalized, 0);
        if (direct > 0) {
            return direct;
        }

        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        int total = 0;
        for (Map.Entry<String, Integer> entry : entityKillCounts.entrySet()) {
            String key = normalizeRegistryId(entry.getKey());
            String keyPath = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
            if (key.equals(normalized) || keyPath.equals(path)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    /**
     * Get all entity kill counts (unmodifiable)
     */
    public Map<String, Integer> getAllEntityKillCounts() {
        return Collections.unmodifiableMap(entityKillCounts);
    }

    private static String normalizeRegistryId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            String[] parts = normalized.split("\\.");
            if (parts.length >= 3) {
                return parts[1] + ":" + parts[2];
            }
        }
        return normalized;
    }

    // --- Block Placement Tracking ---

    /**
     * Record that the player placed a block of the given type.
     * Block id should be the canonical ResourceLocation string (e.g. "echoashfallprotocol:hand_recycler")
     * but bare paths are also accepted for convenience.
     */
    public void recordBlockPlacement(String blockId) {
        if (blockId == null || blockId.isEmpty()) return;
        placedBlockCounts.merge(blockId, 1, Integer::sum);
    }

    /**
     * @return lifetime placement count for the block id, or 0 if never placed.
     *         Matches both "echoashfallprotocol:hand_recycler" and bare "hand_recycler"
     *         since mission BlockRequirements sometimes use the short form.
     */
    public int getBlockPlaceCount(String blockId) {
        if (blockId == null || blockId.isEmpty()) return 0;
        Integer direct = placedBlockCounts.get(blockId);
        if (direct != null) return direct;
        // Fall back to matching by path (unnamespaced)
        String needle = blockId.contains(":") ? blockId.substring(blockId.indexOf(':') + 1) : blockId;
        int total = 0;
        for (Map.Entry<String, Integer> e : placedBlockCounts.entrySet()) {
            String key = e.getKey();
            String path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
            if (path.equals(needle)) total += e.getValue();
        }
        return total;
    }

    // --- Location Visit Tracking ---

    /**
     * Mark a location as visited based on type and ID
     * @param locationType "biome", "dimension", "poi", "special"
     * @param locationId the identifier for the location
     */
    public void visitLocation(String locationType, String locationId) {
        switch (locationType.toLowerCase()) {
            case "biome" -> visitedBiomes.add(locationId);
            case "dimension" -> visitedDimensions.add(locationId);
            case "poi" -> visitedSpecialLocations.add(ExplorationSiteRegistry.normalize(locationId));
            case "special" -> visitedSpecialLocations.add(locationId);
            default -> visitedSpecialLocations.add(locationType + ":" + locationId);
        }
    }

    /**
     * Check if a location has been visited
     * @param locationType "biome", "dimension", "poi", "special"
     * @param locationId the identifier for the location
     */
    public boolean hasVisitedLocation(String locationType, String locationId) {
        return switch (locationType.toLowerCase()) {
            case "biome" -> visitedBiomes.contains(locationId);
            case "dimension" -> visitedDimensions.contains(locationId);
            case "poi" -> visitedSpecialLocations.contains(ExplorationSiteRegistry.normalize(locationId))
                    || isPOIDiscovered(locationId)
                    || ("faction_hub".equals(locationId)
                            && (isPOIDiscovered("radwarden_outpost")
                                    || isPOIDiscovered("crashbreak_salvage_yard")
                                    || isPOIDiscovered("sporebound_sanctum")));
            case "special" -> visitedSpecialLocations.contains(locationId);
            default -> visitedSpecialLocations.contains(locationType + ":" + locationId);
        };
    }

    /**
     * Get all visited biome IDs
     */
    public Set<String> getVisitedBiomes() {
        return Collections.unmodifiableSet(visitedBiomes);
    }

    /**
     * Get all visited dimension IDs
     */
    public Set<String> getVisitedDimensions() {
        return Collections.unmodifiableSet(visitedDimensions);
    }

    /**
     * Get all visited special location IDs
     */
    public Set<String> getVisitedSpecialLocations() {
        return Collections.unmodifiableSet(visitedSpecialLocations);
    }

    /**
     * Add a collected Power Node
     */
    public void addPowerNode() {
        collectedPowerNodes++;
    }

    /**
     * Get count of collected Power Nodes
     */
    public int getCollectedPowerNodes() {
        return Math.min(collectedPowerNodes, 5); // Cap at 5 for display
    }

    /**
     * Set the collected Power Node count (for save/load)
     */
    public void setCollectedPowerNodes(int count) {
        this.collectedPowerNodes = count;
    }
    
    /**
     * Get mission status for UI display
     */
    public MissionStatus getMissionStatus(String missionId) {
        if (isMissionCompleted(missionId)) return MissionStatus.COMPLETED;
        if (isMissionUnlocked(missionId)) return MissionStatus.UNLOCKED;
        return MissionStatus.LOCKED;
    }
    
    /**
     * Mission status enum for UI
     */
    public enum MissionStatus {
        LOCKED,     // Prerequisites not met
        UNLOCKED,   // Available to start
        COMPLETED   // Finished
    }

    // --- ECHO Terminal System Getters/Setters ---
    
    public int getTerminalHealth() { return terminalHealth; }
    public void setTerminalHealth(int health) { terminalHealth = Math.max(0, Math.min(100, health)); }
    public void damageTerminal(int amount) { setTerminalHealth(terminalHealth - amount); }
    public void repairTerminal(int amount) { setTerminalHealth(terminalHealth + amount); }
    
    public boolean isTerminalOnline() { return terminalOnline && terminalHealth > 0; }
    public void setTerminalOnline(boolean online) { terminalOnline = online; }
    
    public long getLastTerminalInteraction() { return lastTerminalInteraction; }
    public void setLastTerminalInteraction(long tick) { lastTerminalInteraction = tick; }
    
    public int getEchoRelationship() { return echoRelationship; }
    public void setEchoRelationship(int relationship) { echoRelationship = Math.max(-100, Math.min(100, relationship)); }
    public void adjustEchoRelationship(int delta) { setEchoRelationship(echoRelationship + delta); }
    
    public boolean isDroneUnlocked() { return droneUnlocked; }
    public void setDroneUnlocked(boolean unlocked) { droneUnlocked = unlocked; }
    
    public boolean isDroneDeployed() { return droneDeployed; }
    public void setDroneDeployed(boolean deployed) { droneDeployed = deployed; }
    
    public String getLastTerminalDimension() { return lastTerminalDimension; }
    public void setLastTerminalDimension(String dimension) { lastTerminalDimension = dimension; }
    
    // --- Drone Repair System Getters/Setters ---
    
    public int getDroneHealth() { return droneHealth; }
    public void setDroneHealth(int health) { 
        droneHealth = Math.max(0, Math.min(100, health));
        droneStage = DroneRepairStage.fromHealth(droneHealth);
    }
    public void repairDrone(int amount) { setDroneHealth(droneHealth + amount); }
    public void damageDrone(int amount) { setDroneHealth(droneHealth - amount); }
    
    public DroneRepairStage getDroneStage() { return droneStage; }
    public void setDroneStage(DroneRepairStage stage) { droneStage = stage; }
    
    public boolean canUseDroneMode(String mode) {
        return switch (mode.toUpperCase()) {
            case "FOLLOW" -> droneStage.ordinal() >= DroneRepairStage.BROKEN.ordinal();
            case "SCOUT" -> droneStage.ordinal() >= DroneRepairStage.PARTIAL.ordinal();
            case "COMBAT", "SCAVENGE" -> droneStage.ordinal() >= DroneRepairStage.OPERATIONAL.ordinal();
            case "PATROL" -> droneStage.ordinal() >= DroneRepairStage.ENHANCED.ordinal();
            default -> false;
        };
    }
    
    public int getDroneInventorySlots() {
        return switch (droneStage) {
            case BROKEN -> 0;       // Locked
            case PARTIAL -> 3;      // Limited
            case OPERATIONAL -> 6;  // Expanded
            case ENHANCED -> 9;     // Full
        };
    }
    
    public float getDroneSpeedMultiplier() {
        return switch (droneStage) {
            case BROKEN -> 0.3f;      // 30% speed
            case PARTIAL -> 0.5f;     // 50% speed
            case OPERATIONAL -> 0.75f; // 75% speed
            case ENHANCED -> 1.0f;    // 100% speed
        };
    }
    
    public boolean isDroneLightEnabled() {
        return droneStage.ordinal() >= DroneRepairStage.PARTIAL.ordinal();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("currentPhase", currentPhase);
        output.putInt("currentMissionIndex", currentMissionIndex);
        output.putBoolean("echoIntroPlayed", echoIntroPlayed);
        output.putLong("lastMessageTick", lastMessageTick);
        output.putInt("introStep", introStep);
        output.putBoolean("dropPodInitialized", dropPodInitialized);
        output.putLong("nextObjectiveHintTick", nextObjectiveHintTick);
        output.putLong("nextDroneChatterTick", nextDroneChatterTick);
        output.putString("lastBiomeId", lastBiomeId);
        output.putString("seenEntities", seenEntities);
        
        // Modernization fields
        output.putString("discoveredAssets", discoveredAssets);
        output.putInt("archiveCount", archive.size());
        for (int i = 0; i < archive.size(); i++) {
            output.putString("msg" + i, archive.get(i));
        }
        
        // Terminal System fields
        output.putInt("terminalHealth", terminalHealth);
        output.putBoolean("terminalOnline", terminalOnline);
        output.putLong("lastTerminalInteraction", lastTerminalInteraction);
        output.putInt("echoRelationship", echoRelationship);
        output.putBoolean("droneUnlocked", droneUnlocked);
        output.putBoolean("droneDeployed", droneDeployed);
        output.putString("lastTerminalDimension", lastTerminalDimension);
        
        // Drone Repair System fields
        output.putInt("droneHealth", droneHealth);
        output.putString("droneStage", droneStage.name());
        
        // Mission System Tracking
        output.putInt("completedMissions", completedMissionIds.size());
        int idx = 0;
        for (String id : completedMissionIds) {
            output.putString("completed_" + idx++, id);
        }
        output.putInt("unlockedMissions", unlockedMissionIds.size());
        idx = 0;
        for (String id : unlockedMissionIds) {
            output.putString("unlocked_" + idx++, id);
        }
        output.putInt("pendingRewardMissions", pendingRewards.size());
        idx = 0;
        for (Map.Entry<String, List<ItemStack>> entry : pendingRewards.entrySet()) {
            output.putString("rewardMission_" + idx, entry.getKey());
            output.putInt("rewardCount_" + idx, entry.getValue().size());
            ValueOutput.TypedOutputList<ItemStack> fullRewardStacks =
                    output.list("rewardStacks_" + idx, ItemStack.CODEC);
            for (int i = 0; i < entry.getValue().size(); i++) {
                ItemStack stack = entry.getValue().get(i);
                if (!stack.isEmpty()) {
                    fullRewardStacks.add(stack.copy());
                }
                // Keep the legacy id/count keys so older or malformed entries remain readable.
                output.putString("rewardItem_" + idx + "_" + i + "_id",
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                output.putInt("rewardItem_" + idx + "_" + i + "_count", stack.getCount());
            }
            idx++;
        }
        output.putString("selectedMissionId", selectedMissionId);
        
        // Turn-in reminders
        output.putInt("turnInReminders", turnInReminders.size());
        idx = 0;
        for (String id : turnInReminders) {
            output.putString("turnInReminder_" + idx++, id);
        }

        // Exploration & Endgame Tracking
        output.putInt("collectedPowerNodes", collectedPowerNodes);
        output.putInt("discoveredPOICount", discoveredPOIs.size());
        idx = 0;
        for (String poiId : discoveredPOIs) {
            output.putString("discoveredPOI_" + idx++, poiId);
        }
        output.putInt("poiObjectiveStateCount", poiObjectiveStates.size());
        idx = 0;
        for (String state : poiObjectiveStates) {
            output.putString("poiObjectiveState_" + idx++, state);
        }
        
        // Entity Kill Tracking
        output.putInt("entityKillCount", entityKillCounts.size());
        idx = 0;
        for (Map.Entry<String, Integer> entry : entityKillCounts.entrySet()) {
            output.putString("entityKillType_" + idx, entry.getKey());
            output.putInt("entityKillValue_" + idx, entry.getValue());
            idx++;
        }

        // Block Placement Tracking
        output.putInt("placedBlockCount", placedBlockCounts.size());
        idx = 0;
        for (Map.Entry<String, Integer> entry : placedBlockCounts.entrySet()) {
            output.putString("placedBlockType_" + idx, entry.getKey());
            output.putInt("placedBlockValue_" + idx, entry.getValue());
            idx++;
        }
        
        // Location Visit Tracking
        output.putInt("visitedBiomesCount", visitedBiomes.size());
        idx = 0;
        for (String biome : visitedBiomes) {
            output.putString("visitedBiome_" + idx++, biome);
        }
        output.putInt("visitedDimensionsCount", visitedDimensions.size());
        idx = 0;
        for (String dim : visitedDimensions) {
            output.putString("visitedDimension_" + idx++, dim);
        }
        output.putInt("visitedSpecialCount", visitedSpecialLocations.size());
        idx = 0;
        for (String loc : visitedSpecialLocations) {
            output.putString("visitedSpecial_" + idx++, loc);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        currentPhase = input.getIntOr("currentPhase", 0);
        currentMissionIndex = input.getIntOr("currentMissionIndex", 0);
        echoIntroPlayed = input.getBooleanOr("echoIntroPlayed", false);
        lastMessageTick = input.getLongOr("lastMessageTick", 0L);
        introStep = input.getIntOr("introStep", 0);
        dropPodInitialized = input.getBooleanOr("dropPodInitialized", false);
        nextObjectiveHintTick = input.getLongOr("nextObjectiveHintTick", 0L);
        nextDroneChatterTick = input.getLongOr("nextDroneChatterTick", 0L);
        lastBiomeId = input.getStringOr("lastBiomeId", "");
        seenEntities = input.getStringOr("seenEntities", "");
        
        // Modernization fields
        discoveredAssets = input.getStringOr("discoveredAssets", "");
        int count = input.getIntOr("archiveCount", 0);
        archive.clear();
        for (int i = 0; i < count; i++) {
            archive.add(input.getStringOr("msg" + i, ""));
        }
        
        // Terminal System fields
        terminalHealth = input.getIntOr("terminalHealth", 100);
        terminalOnline = input.getBooleanOr("terminalOnline", true);
        lastTerminalInteraction = input.getLongOr("lastTerminalInteraction", 0);
        echoRelationship = input.getIntOr("echoRelationship", 0);
        droneUnlocked = input.getBooleanOr("droneUnlocked", true); // Default unlocked (damaged)
        droneDeployed = input.getBooleanOr("droneDeployed", false);
        lastTerminalDimension = input.getStringOr("lastTerminalDimension", "");
        
        // Drone Repair System fields
        droneHealth = input.getIntOr("droneHealth", 15); // Default 15% (damaged)
        String stageName = input.getStringOr("droneStage", "BROKEN");
        try {
            droneStage = DroneRepairStage.valueOf(stageName);
        } catch (IllegalArgumentException e) {
            droneStage = DroneRepairStage.fromHealth(droneHealth);
        }
        
        // Mission System Tracking
        completedMissionIds.clear();
        int completedCount = input.getIntOr("completedMissions", 0);
        for (int i = 0; i < completedCount; i++) {
            String id = input.getStringOr("completed_" + i, "");
            if (!id.isEmpty()) completedMissionIds.add(id);
        }
        
        unlockedMissionIds.clear();
        int unlockedCount = input.getIntOr("unlockedMissions", 0);
        for (int i = 0; i < unlockedCount; i++) {
            String id = input.getStringOr("unlocked_" + i, "");
            if (!id.isEmpty()) unlockedMissionIds.add(id);
        }
        
        // Turn-in reminders
        turnInReminders.clear();
        int reminderCount = input.getIntOr("turnInReminders", 0);
        for (int i = 0; i < reminderCount; i++) {
            String id = input.getStringOr("turnInReminder_" + i, "");
            if (!id.isEmpty()) turnInReminders.add(id);
        }
        
        // Exploration & Endgame Tracking
        collectedPowerNodes = input.getIntOr("collectedPowerNodes", 0);
        discoveredPOIs.clear();
        int poiCount = input.getIntOr("discoveredPOICount", 0);
        for (int i = 0; i < poiCount; i++) {
            String poiId = input.getStringOr("discoveredPOI_" + i, "");
            if (!poiId.isEmpty()) discoveredPOIs.add(poiId);
        }
        poiObjectiveStates.clear();
        int poiStateCount = input.getIntOr("poiObjectiveStateCount", 0);
        for (int i = 0; i < poiStateCount; i++) {
            String state = input.getStringOr("poiObjectiveState_" + i, "");
            if (!state.isEmpty()) poiObjectiveStates.add(state);
        }
        normalizePOIProgress(this);
        
        // Entity Kill Tracking
        entityKillCounts.clear();
        int killCount = input.getIntOr("entityKillCount", 0);
        for (int i = 0; i < killCount; i++) {
            String entityType = input.getStringOr("entityKillType_" + i, "");
            int killValue = input.getIntOr("entityKillValue_" + i, 0);
            if (!entityType.isEmpty() && killValue > 0) {
                entityKillCounts.put(entityType, killValue);
            }
        }

        // Block Placement Tracking
        placedBlockCounts.clear();
        int placeCount = input.getIntOr("placedBlockCount", 0);
        for (int i = 0; i < placeCount; i++) {
            String blockType = input.getStringOr("placedBlockType_" + i, "");
            int placeValue = input.getIntOr("placedBlockValue_" + i, 0);
            if (!blockType.isEmpty() && placeValue > 0) {
                placedBlockCounts.put(blockType, placeValue);
            }
        }
        
        // Location Visit Tracking
        visitedBiomes.clear();
        int biomeCount = input.getIntOr("visitedBiomesCount", 0);
        for (int i = 0; i < biomeCount; i++) {
            String biome = input.getStringOr("visitedBiome_" + i, "");
            if (!biome.isEmpty()) visitedBiomes.add(biome);
        }
        visitedDimensions.clear();
        int dimCount = input.getIntOr("visitedDimensionsCount", 0);
        for (int i = 0; i < dimCount; i++) {
            String dim = input.getStringOr("visitedDimension_" + i, "");
            if (!dim.isEmpty()) visitedDimensions.add(dim);
        }
        visitedSpecialLocations.clear();
        int specialCount = input.getIntOr("visitedSpecialCount", 0);
        for (int i = 0; i < specialCount; i++) {
            String loc = input.getStringOr("visitedSpecial_" + i, "");
            if (!loc.isEmpty()) visitedSpecialLocations.add(loc);
        }

        pendingRewards.clear();
        int rewardMissionCount = input.getIntOr("pendingRewardMissions", 0);
        for (int i = 0; i < rewardMissionCount; i++) {
            String missionId = input.getStringOr("rewardMission_" + i, "");
            if (missionId.isEmpty()) continue;
            int rewardCount = input.getIntOr("rewardCount_" + i, 0);
            List<ItemStack> rewards = new ArrayList<>();
            for (ItemStack stack : input.listOrEmpty("rewardStacks_" + i, ItemStack.CODEC)) {
                if (!stack.isEmpty()) {
                    rewards.add(stack.copy());
                }
            }
            if (rewards.isEmpty()) {
                for (int j = 0; j < rewardCount; j++) {
                    String itemId = input.getStringOr("rewardItem_" + i + "_" + j + "_id", "");
                    int itemCount = input.getIntOr("rewardItem_" + i + "_" + j + "_count", 0);
                    if (itemId.isEmpty() || itemCount <= 0) continue;
                    Identifier id = Identifier.tryParse(itemId);
                    if (id == null) continue;
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    if (item != null) {
                        rewards.add(new ItemStack(item, itemCount));
                    }
                }
            }
            if (!rewards.isEmpty()) {
                pendingRewards.put(missionId, rewards);
            }
        }
        selectedMissionId = input.getStringOr("selectedMissionId", "");
    }

    private static void normalizePOIProgress(QuestData data) {
        if (!data.discoveredPOIs.isEmpty()) {
            Set<String> normalized = new LinkedHashSet<>();
            for (String poiId : data.discoveredPOIs) {
                normalized.add(ExplorationSiteRegistry.normalize(poiId));
            }
            data.discoveredPOIs.clear();
            data.discoveredPOIs.addAll(normalized);
        }

        if (!data.poiObjectiveStates.isEmpty()) {
            Set<String> normalized = new LinkedHashSet<>();
            for (String state : data.poiObjectiveStates) {
                int sep = state.lastIndexOf(':');
                if (sep <= 0 || sep >= state.length() - 1) {
                    continue;
                }
                String poiId = ExplorationSiteRegistry.normalize(state.substring(0, sep));
                String stateName = state.substring(sep + 1);
                normalized.add(poiId + ":" + stateName);
            }
            data.poiObjectiveStates.clear();
            data.poiObjectiveStates.addAll(normalized);
        }
    }
    
    /**
     * Get QuestData for a player
     */
    public static QuestData get(Player player) {
        return player.getData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.QUEST_DATA.get());
    }
}

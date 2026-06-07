package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Player data attachment for tracking post-Nexus choice progression.
 * Stores path selection, mission progress, and boss completion status.
 */
public class PostNexusData implements EchoValueIOSerializable {

    public enum NexusPath {
        NONE, RESTORE, DESTROY, CONTROL
    }

    private NexusPath selectedPath = NexusPath.NONE;
    private int missionProgress = 0; // 0-5 branch progress marker
    private int relaysResolved = 0;
    private int siegesSurvived = 0;
    private int pathOperationsComplete = 0;
    private boolean finalBossDefeated = false;
    
    // RESTORE path tracking
    private int nodesRepaired = 0;
    private int corruptedMobsKilled = 0;
    
    // DESTROY path tracking
    private int nodesDestroyed = 0;
    private int stormsSurvived = 0;
    
    // CONTROL path tracking
    private int signalBoostersPlaced = 0;
    private int denseAlloyCollected = 0;
    private int nexusCrystalsCollected = 0;
    private int energyCellsCollected = 0;
    private int mobsDefeatedWithScepter = 0;
    
    // Boss completion
    private boolean wardenDefeated = false;
    private boolean archivesEntered = false;
    private boolean epilogueComplete = false;
    private boolean wardenRewardClaimed = false;
    private boolean finalRewardClaimed = false;
    private boolean hasArchivesReturnPoint = false;
    private String archivesReturnDimension = "minecraft:overworld";
    private double archivesReturnX = 0.5D;
    private double archivesReturnY = 70.0D;
    private double archivesReturnZ = 0.5D;
    private long choiceTimestamp = 0;
    
    // Storm tracking (transient - not saved)
    private transient boolean countedCurrentStorm = false;

    public PostNexusData() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, PostNexusData> STREAM_CODEC = StreamCodec.of(
            PostNexusData::writeSync,
            PostNexusData::readSync
    );

    private static void writeSync(RegistryFriendlyByteBuf buf, PostNexusData data) {
        buf.writeUtf(data.selectedPath.name());
        buf.writeVarInt(data.missionProgress);
        buf.writeVarInt(data.nodesRepaired);
        buf.writeVarInt(data.corruptedMobsKilled);
        buf.writeVarInt(data.nodesDestroyed);
        buf.writeVarInt(data.stormsSurvived);
        buf.writeVarInt(data.signalBoostersPlaced);
        buf.writeVarInt(data.denseAlloyCollected);
        buf.writeVarInt(data.nexusCrystalsCollected);
        buf.writeVarInt(data.energyCellsCollected);
        buf.writeVarInt(data.mobsDefeatedWithScepter);
        buf.writeBoolean(data.wardenDefeated);
        buf.writeBoolean(data.archivesEntered);
        buf.writeBoolean(data.epilogueComplete);
        buf.writeBoolean(data.wardenRewardClaimed);
        buf.writeBoolean(data.finalRewardClaimed);
        buf.writeBoolean(data.hasArchivesReturnPoint);
        buf.writeUtf(data.archivesReturnDimension);
        buf.writeDouble(data.archivesReturnX);
        buf.writeDouble(data.archivesReturnY);
        buf.writeDouble(data.archivesReturnZ);
        buf.writeLong(data.choiceTimestamp);
        buf.writeVarInt(data.relaysResolved);
        buf.writeVarInt(data.siegesSurvived);
        buf.writeVarInt(data.pathOperationsComplete);
        buf.writeBoolean(data.finalBossDefeated);
    }

    private static PostNexusData readSync(RegistryFriendlyByteBuf buf) {
        PostNexusData data = new PostNexusData();
        try {
            data.selectedPath = NexusPath.valueOf(buf.readUtf());
        } catch (IllegalArgumentException e) {
            data.selectedPath = NexusPath.NONE;
        }
        data.missionProgress = buf.readVarInt();
        data.nodesRepaired = buf.readVarInt();
        data.corruptedMobsKilled = buf.readVarInt();
        data.nodesDestroyed = buf.readVarInt();
        data.stormsSurvived = buf.readVarInt();
        data.signalBoostersPlaced = buf.readVarInt();
        data.denseAlloyCollected = buf.readVarInt();
        data.nexusCrystalsCollected = buf.readVarInt();
        data.energyCellsCollected = buf.readVarInt();
        data.mobsDefeatedWithScepter = buf.readVarInt();
        data.wardenDefeated = buf.readBoolean();
        data.archivesEntered = buf.readBoolean();
        data.epilogueComplete = buf.readBoolean();
        data.wardenRewardClaimed = buf.readBoolean();
        data.finalRewardClaimed = buf.readBoolean();
        data.hasArchivesReturnPoint = buf.readBoolean();
        data.archivesReturnDimension = buf.readUtf();
        data.archivesReturnX = buf.readDouble();
        data.archivesReturnY = buf.readDouble();
        data.archivesReturnZ = buf.readDouble();
        data.choiceTimestamp = buf.readLong();
        data.relaysResolved = buf.readVarInt();
        data.siegesSurvived = buf.readVarInt();
        data.pathOperationsComplete = buf.readVarInt();
        data.finalBossDefeated = buf.readBoolean();
        return data;
    }

    // Getters and Setters
    public NexusPath getSelectedPath() { return selectedPath; }
    public void setSelectedPath(NexusPath path) { 
        this.selectedPath = path; 
        this.choiceTimestamp = System.currentTimeMillis();
    }

    public int getMissionProgress() { return missionProgress; }
    public void setMissionProgress(int progress) { this.missionProgress = Math.min(5, Math.max(0, progress)); }
    public void advanceMission() { this.missionProgress = Math.min(5, missionProgress + 1); }

    public int getRelaysResolved() { return relaysResolved; }
    public void setRelaysResolved(int count) { this.relaysResolved = Math.max(0, count); }
    public void incrementRelaysResolved() { this.relaysResolved++; }
    public int getSiegesSurvived() { return siegesSurvived; }
    public void setSiegesSurvived(int count) { this.siegesSurvived = Math.max(0, count); }
    public void incrementSiegesSurvived() { this.siegesSurvived++; }
    public int getPathOperationsComplete() { return pathOperationsComplete; }
    public void setPathOperationsComplete(int count) { this.pathOperationsComplete = Math.max(0, count); }
    public void incrementPathOperationsComplete() { this.pathOperationsComplete++; }
    public boolean isFinalBossDefeated() { return finalBossDefeated; }
    public void setFinalBossDefeated(boolean defeated) { this.finalBossDefeated = defeated; }

    // RESTORE tracking
    public int getNodesRepaired() { return nodesRepaired; }
    public void incrementNodesRepaired() { this.nodesRepaired++; }
    public int getCorruptedMobsKilled() { return corruptedMobsKilled; }
    public void incrementCorruptedMobsKilled() { this.corruptedMobsKilled++; }

    // DESTROY tracking
    public int getNodesDestroyed() { return nodesDestroyed; }
    public void incrementNodesDestroyed() { this.nodesDestroyed++; }
    public int getStormsSurvived() { return stormsSurvived; }
    public void incrementStormsSurvived() { this.stormsSurvived++; }

    // CONTROL tracking
    public int getSignalBoostersPlaced() { return signalBoostersPlaced; }
    public void incrementSignalBoostersPlaced() { this.signalBoostersPlaced++; }
    public int getMobsDefeatedWithScepter() { return mobsDefeatedWithScepter; }
    public void incrementMobsDefeatedWithScepter() { this.mobsDefeatedWithScepter++; }
    
    public int getDenseAlloyCollected() { return denseAlloyCollected; }
    public void addDenseAlloy(int amount) { this.denseAlloyCollected += amount; }
    public int getNexusCrystalsCollected() { return nexusCrystalsCollected; }
    public void addNexusCrystals(int amount) { this.nexusCrystalsCollected += amount; }
    public int getEnergyCellsCollected() { return energyCellsCollected; }
    public void addEnergyCells(int amount) { this.energyCellsCollected += amount; }
    
    // Required totals for each resource on the CONTROL path.
    // Previously 1000/1000/1000 (~3000 items total) which dwarfed RESTORE/DESTROY by ~30x.
    // Rebalanced to 50/50/50 so all three endings converge on a similar effort budget.
    public static final int CONTROL_DENSE_ALLOY_REQUIRED = 50;
    public static final int CONTROL_NEXUS_CRYSTALS_REQUIRED = 50;
    public static final int CONTROL_ENERGY_CELLS_REQUIRED = 50;

    // Check if resource dominance mission complete
    public boolean isResourceDominanceComplete() {
        return denseAlloyCollected >= CONTROL_DENSE_ALLOY_REQUIRED
                && nexusCrystalsCollected >= CONTROL_NEXUS_CRYSTALS_REQUIRED
                && energyCellsCollected >= CONTROL_ENERGY_CELLS_REQUIRED;
    }

    // Boss tracking
    public boolean isWardenDefeated() { return wardenDefeated; }
    public void setWardenDefeated(boolean defeated) { this.wardenDefeated = defeated; }
    public boolean hasEnteredArchives() { return archivesEntered; }
    public void setArchivesEntered(boolean entered) { this.archivesEntered = entered; }
    public boolean isEpilogueComplete() { return epilogueComplete; }
    public void setEpilogueComplete(boolean complete) { this.epilogueComplete = complete; }
    public boolean isWardenRewardClaimed() { return wardenRewardClaimed; }
    public void setWardenRewardClaimed(boolean claimed) { this.wardenRewardClaimed = claimed; }
    public boolean isFinalRewardClaimed() { return finalRewardClaimed; }
    public void setFinalRewardClaimed(boolean claimed) { this.finalRewardClaimed = claimed; }
    public boolean hasArchivesReturnPoint() { return hasArchivesReturnPoint; }
    public String getArchivesReturnDimension() { return archivesReturnDimension; }
    public double getArchivesReturnX() { return archivesReturnX; }
    public double getArchivesReturnY() { return archivesReturnY; }
    public double getArchivesReturnZ() { return archivesReturnZ; }
    public void setArchivesReturnPoint(String dimension, double x, double y, double z) {
        this.hasArchivesReturnPoint = true;
        this.archivesReturnDimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension;
        this.archivesReturnX = x;
        this.archivesReturnY = y;
        this.archivesReturnZ = z;
    }
    public void clearArchivesReturnPoint() {
        this.hasArchivesReturnPoint = false;
        this.archivesReturnDimension = "minecraft:overworld";
        this.archivesReturnX = 0.5D;
        this.archivesReturnY = 70.0D;
        this.archivesReturnZ = 0.5D;
    }
    public void completeEpilogue() {
        this.epilogueComplete = true;
        this.finalRewardClaimed = true;
    }
    public long getChoiceTimestamp() { return choiceTimestamp; }
    
    // Storm tracking methods
    public boolean hasCountedCurrentStorm() { return countedCurrentStorm; }
    public void setCountedCurrentStorm(boolean counted) { this.countedCurrentStorm = counted; }

    // Utility methods
    public boolean hasMadeChoice() { return selectedPath != NexusPath.NONE; }
    public boolean isPath(NexusPath path) { return selectedPath == path; }
    public boolean isFinalProtocolComplete() { return wardenDefeated && finalBossDefeated && epilogueComplete; }
    
    public boolean isMission1Complete() {
        return switch (selectedPath) {
            case NONE -> false;
            case RESTORE -> nodesRepaired >= 3;
            case DESTROY -> nodesDestroyed >= 5;
            case CONTROL -> signalBoostersPlaced >= 3;
        };
    }
    
    public boolean isMission2Complete() {
        return switch (selectedPath) {
            case NONE -> false;
            case RESTORE -> corruptedMobsKilled >= 20;
            case DESTROY -> stormsSurvived >= 1;
            case CONTROL -> isResourceDominanceComplete();
        };
    }
    
    public boolean isMission3Complete() {
        return archivesEntered;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString("selectedPath", selectedPath.name());
        output.putInt("missionProgress", missionProgress);
        output.putInt("nodesRepaired", nodesRepaired);
        output.putInt("corruptedMobsKilled", corruptedMobsKilled);
        output.putInt("nodesDestroyed", nodesDestroyed);
        output.putInt("stormsSurvived", stormsSurvived);
        output.putInt("signalBoostersPlaced", signalBoostersPlaced);
        output.putInt("denseAlloyCollected", denseAlloyCollected);
        output.putInt("nexusCrystalsCollected", nexusCrystalsCollected);
        output.putInt("energyCellsCollected", energyCellsCollected);
        output.putInt("mobsDefeatedWithScepter", mobsDefeatedWithScepter);
        output.putBoolean("wardenDefeated", wardenDefeated);
        output.putBoolean("archivesEntered", archivesEntered);
        output.putBoolean("epilogueComplete", epilogueComplete);
        output.putBoolean("wardenRewardClaimed", wardenRewardClaimed);
        output.putBoolean("finalRewardClaimed", finalRewardClaimed);
        output.putBoolean("hasArchivesReturnPoint", hasArchivesReturnPoint);
        output.putString("archivesReturnDimension", archivesReturnDimension);
        output.putDouble("archivesReturnX", archivesReturnX);
        output.putDouble("archivesReturnY", archivesReturnY);
        output.putDouble("archivesReturnZ", archivesReturnZ);
        output.putLong("choiceTimestamp", choiceTimestamp);
        output.putInt("relaysResolved", relaysResolved);
        output.putInt("siegesSurvived", siegesSurvived);
        output.putInt("pathOperationsComplete", pathOperationsComplete);
        output.putBoolean("finalBossDefeated", finalBossDefeated);
    }

    @Override
    public void deserialize(ValueInput input) {
        String pathStr = input.getStringOr("selectedPath", "NONE");
        try {
            this.selectedPath = NexusPath.valueOf(pathStr);
        } catch (IllegalArgumentException e) {
            this.selectedPath = NexusPath.NONE;
        }
        this.missionProgress = input.getIntOr("missionProgress", 0);
        this.nodesRepaired = input.getIntOr("nodesRepaired", 0);
        this.corruptedMobsKilled = input.getIntOr("corruptedMobsKilled", 0);
        this.nodesDestroyed = input.getIntOr("nodesDestroyed", 0);
        this.stormsSurvived = input.getIntOr("stormsSurvived", 0);
        this.signalBoostersPlaced = input.getIntOr("signalBoostersPlaced", 0);
        this.denseAlloyCollected = input.getIntOr("denseAlloyCollected", 0);
        this.nexusCrystalsCollected = input.getIntOr("nexusCrystalsCollected", 0);
        this.energyCellsCollected = input.getIntOr("energyCellsCollected", 0);
        this.mobsDefeatedWithScepter = input.getIntOr("mobsDefeatedWithScepter", 0);
        this.wardenDefeated = input.getBooleanOr("wardenDefeated", false);
        this.archivesEntered = input.getBooleanOr("archivesEntered", false);
        this.epilogueComplete = input.getBooleanOr("epilogueComplete", false);
        this.wardenRewardClaimed = input.getBooleanOr("wardenRewardClaimed", false);
        this.finalRewardClaimed = input.getBooleanOr("finalRewardClaimed", false);
        this.hasArchivesReturnPoint = input.getBooleanOr("hasArchivesReturnPoint", false);
        this.archivesReturnDimension = input.getStringOr("archivesReturnDimension", "minecraft:overworld");
        this.archivesReturnX = input.getDoubleOr("archivesReturnX", 0.5D);
        this.archivesReturnY = input.getDoubleOr("archivesReturnY", 70.0D);
        this.archivesReturnZ = input.getDoubleOr("archivesReturnZ", 0.5D);
        this.choiceTimestamp = input.getLongOr("choiceTimestamp", 0);
        this.relaysResolved = input.getIntOr("relaysResolved", 0);
        this.siegesSurvived = input.getIntOr("siegesSurvived", 0);
        this.pathOperationsComplete = input.getIntOr("pathOperationsComplete", 0);
        this.finalBossDefeated = input.getBooleanOr("finalBossDefeated", false);
    }

    public static PostNexusData get(Player player) {
        return player.getData(ModAttachments.POST_NEXUS_DATA.get());
    }

    public static void saveAndSync(ServerPlayer player, PostNexusData data) {
        player.setData(ModAttachments.POST_NEXUS_DATA.get(), data);
        player.syncData(ModAttachments.POST_NEXUS_DATA.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.POST_NEXUS_DATA.get());
    }
}

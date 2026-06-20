package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-saved data for the Nexus Core endgame choice.
 * Persists across server restarts and is available to all players.
 * Uses the current saved-data API with Codec serialization.
 */
public class NexusWorldData extends SavedData {

    public enum WorldState {
        NORMAL,      // Before Nexus interaction
        RESTORED,    // Grid restored - fewer environmental events
        DESTROYED,   // Core destroyed - more chaotic events, hostile mobs
        CONTROLLED   // Player control - custom effects
    }

    // Codec for BlockPos
    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
        ).apply(instance, BlockPos::new)
    );

    // Codec for WorldState
    private static final Codec<WorldState> WORLD_STATE_CODEC = Codec.STRING.xmap(
        s -> {
            try {
                return WorldState.valueOf(s);
            } catch (IllegalArgumentException e) {
                return WorldState.NORMAL;
            }
        },
        WorldState::name
    );

    // Main codec for NexusWorldData
    public static final Codec<NexusWorldData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            WORLD_STATE_CODEC.optionalFieldOf("state", WorldState.NORMAL).forGetter(d -> d.state),
            Codec.LONG.optionalFieldOf("choiceTime", -1L).forGetter(d -> d.choiceTime),
            BLOCK_POS_CODEC.optionalFieldOf("nexusPos", BlockPos.ZERO).forGetter(d -> d.nexusPos),
            Codec.STRING.optionalFieldOf("playerName", "").forGetter(d -> d.playerName),
            Codec.list(BLOCK_POS_CODEC).optionalFieldOf("activeNodes", List.of()).forGetter(d -> List.copyOf(d.activePowerNodes)),
            Codec.LONG.optionalFieldOf("lastEndgameEventTick", 0L).forGetter(d -> d.lastEndgameEventTick)
        ).apply(instance, (state, choiceTime, nexusPos, playerName, activeNodes, lastEndgameEventTick) -> {
            NexusWorldData data = new NexusWorldData();
            data.state = state;
            data.choiceTime = choiceTime;
            data.nexusPos = nexusPos;
            data.playerName = playerName;
            data.activePowerNodes.addAll(activeNodes);
            data.lastEndgameEventTick = lastEndgameEventTick;
            return data;
        })
    );

    // SavedDataType for registration
    public static final SavedDataType<NexusWorldData> TYPE = new SavedDataType<NexusWorldData>(
        Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "nexus"),
        NexusWorldData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private WorldState state = WorldState.NORMAL;
    private long choiceTime = -1;
    private BlockPos nexusPos = BlockPos.ZERO;
    private String playerName = "";
    private final Set<BlockPos> activePowerNodes = new LinkedHashSet<>();
    private long lastEndgameEventTick = 0L;

    public NexusWorldData() {}

    /**
     * Gets or creates the NexusWorldData for a server level.
     * Data is automatically persisted to the world's data folder.
     */
    public static NexusWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // Getters
    public WorldState getState() { return state; }
    public long getChoiceTime() { return choiceTime; }
    public BlockPos getNexusPos() { return nexusPos; }
    public String getPlayerName() { return playerName; }
    public long getLastEndgameEventTick() { return lastEndgameEventTick; }
    public List<BlockPos> getActiveNodePositions() { return List.copyOf(activePowerNodes); }

    // Check if choice has been made
    public boolean hasChoiceBeenMade() {
        return state != WorldState.NORMAL;
    }

    // Set the choice
    public void setChoice(WorldState newState, BlockPos pos, String player) {
        this.state = newState;
        this.choiceTime = System.currentTimeMillis();
        this.nexusPos = pos;
        this.playerName = player;
        setDirty();
    }

    public void setLastEndgameEventTick(long gameTime) {
        this.lastEndgameEventTick = gameTime;
        setDirty();
    }

    public void recordPowerNodeActivated(BlockPos pos) {
        if (activePowerNodes.add(copy(pos))) {
            setDirty();
        }
    }

    public void removePowerNode(BlockPos pos) {
        if (activePowerNodes.remove(copy(pos))) {
            setDirty();
        }
    }

    public int countActiveNodes(ServerLevel level, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        int count = 0;
        boolean pruned = false;

        for (BlockPos nodePos : new ArrayList<>(activePowerNodes)) {
            if (!isActivePowerNode(level, nodePos)) {
                activePowerNodes.remove(nodePos);
                pruned = true;
                continue;
            }
            if (center.distSqr(nodePos) <= radiusSqr) {
                count++;
            }
        }

        if (pruned) {
            setDirty();
        }
        return count;
    }

    public boolean isTrackedActiveNode(ServerLevel level, BlockPos pos) {
        return activePowerNodes.contains(copy(pos)) && isActivePowerNode(level, pos);
    }

    private static boolean isActivePowerNode(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.POWER_NODE.get())) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof PowerNodeBlockEntity node && node.isActivated()) {
            return true;
        }
        return level.getBlockState(pos).hasProperty(PowerNodeBlock.ACTIVE)
                && level.getBlockState(pos).getValue(PowerNodeBlock.ACTIVE);
    }

    private static BlockPos copy(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    // Effect helpers
    public boolean isRestored() { return state == WorldState.RESTORED; }
    public boolean isDestroyed() { return state == WorldState.DESTROYED; }
    public boolean isControlled() { return state == WorldState.CONTROLLED; }
}

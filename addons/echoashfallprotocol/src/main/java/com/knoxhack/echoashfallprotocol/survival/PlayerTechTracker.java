package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.network.NexusStatePacket;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

/**
 * Tracks player technology progression based on machines built.
 * Higher tech level attracts more AI patrol attention.
 */
public class PlayerTechTracker {

    // Machine point values - higher = more attention from AI patrols
    private static Map<Block, Integer> machinePointsCache = null;

    private static Map<Block, Integer> getMachinePoints() {
        if (machinePointsCache == null) {
            machinePointsCache = new HashMap<>();
            machinePointsCache.put(ModBlocks.HAND_RECYCLER.get(), 1);
            machinePointsCache.put(ModBlocks.THERMAL_BURNER.get(), 2);
            machinePointsCache.put(ModBlocks.WATER_PURIFIER.get(), 2);
            machinePointsCache.put(ModBlocks.MICRO_GENERATOR.get(), 3);
            machinePointsCache.put(ModBlocks.FILTER_WORKBENCH.get(), 3);
            machinePointsCache.put(ModBlocks.BATTERY_BANK.get(), 4);
            machinePointsCache.put(ModBlocks.SCRAP_PRESS.get(), 4);
            machinePointsCache.put(ModBlocks.SIGNAL_SCANNER.get(), 5);
            machinePointsCache.put(ModBlocks.FIELD_MED_BAY.get(), 5);
            machinePointsCache.put(ModBlocks.ATMOSPHERIC_SCRUBBER.get(), 6);
            machinePointsCache.put(ModBlocks.AUTOFEED_HOPPER.get(), 4);
            machinePointsCache.put(ModBlocks.CONTAMINANT_CONDENSER.get(), 6);
            machinePointsCache.put(ModBlocks.POWER_NODE.get(), 8);
            machinePointsCache.put(ModBlocks.NEXUS_CORE.get(), 20);
        }
        return machinePointsCache;
    }

    public static class PlayerTechData implements EchoValueIOSerializable {
        private int totalTechPoints = 0;
        private int machinesBuilt = 0;
        private final Set<BlockPos> trackedMachines = new HashSet<>();

        public int getTechLevel() {
            // Tech levels: 0-5 (0=primitive, 5=high-tech)
            if (totalTechPoints < 5) return 0;
            if (totalTechPoints < 15) return 1;
            if (totalTechPoints < 30) return 2;
            if (totalTechPoints < 50) return 3;
            if (totalTechPoints < 80) return 4;
            return 5;
        }

        public int getTotalPoints() { return totalTechPoints; }
        public int getMachineCount() { return machinesBuilt; }

        public void addMachine(Block block, BlockPos pos) {
            if (!trackedMachines.contains(pos)) {
                int points = getMachinePoints().getOrDefault(block, 1);
                totalTechPoints += points;
                machinesBuilt++;
                trackedMachines.add(pos);
            }
        }

        public void removeMachine(BlockPos pos) {
            if (trackedMachines.remove(pos)) {
                machinesBuilt = Math.max(0, machinesBuilt - 1);
                // Note: We don't subtract points to prevent exploitation
            }
        }

        public float getThreatMultiplier() {
            // Higher tech = more patrol attention (1.0 to 2.5x)
            return 1.0f + (getTechLevel() * 0.3f);
        }

        @Override
        public void serialize(ValueOutput output) {
            output.putInt("totalTechPoints", totalTechPoints);
            output.putInt("machinesBuilt", machinesBuilt);
            output.putInt("machineCount", trackedMachines.size());
            int i = 0;
            for (BlockPos pos : trackedMachines) {
                output.putInt("machine_" + i + "_x", pos.getX());
                output.putInt("machine_" + i + "_y", pos.getY());
                output.putInt("machine_" + i + "_z", pos.getZ());
                i++;
            }
        }

        @Override
        public void deserialize(ValueInput input) {
            totalTechPoints = input.getIntOr("totalTechPoints", 0);
            machinesBuilt = input.getIntOr("machinesBuilt", 0);
            int count = input.getIntOr("machineCount", 0);
            trackedMachines.clear();
            for (int i = 0; i < count; i++) {
                int x = input.getIntOr("machine_" + i + "_x", 0);
                int y = input.getIntOr("machine_" + i + "_y", 0);
                int z = input.getIntOr("machine_" + i + "_z", 0);
                trackedMachines.add(new BlockPos(x, y, z));
            }
        }
    }

    public static void onBlockPlace(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        if (!(eventValue(event, "getPlacedBlock") instanceof BlockState placedBlock)) return;
        if (!(eventValue(event, "getPos") instanceof BlockPos pos)) return;

        Block block = placedBlock.getBlock();
        if (getMachinePoints().containsKey(block)) {
            PlayerTechData data = getOrCreateData(player);
            data.addMachine(block, pos);
            player.setData(ModAttachments.PLAYER_TECH_DATA.get(), data);

            int newLevel = data.getTechLevel();
            int oldLevel = Math.max(0, newLevel - 1);
            if (newLevel > oldLevel) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c[ECHO-7]§r Tech signature elevated. AI patrol detection probability increased."));
            }
        }
    }

    public static void onBlockBreak(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof ServerPlayer player)) return;
        if (!(eventValue(event, "getState") instanceof BlockState state)) return;
        if (!(eventValue(event, "getPos") instanceof BlockPos pos)) return;

        Block block = state.getBlock();
        if (getMachinePoints().containsKey(block)) {
            PlayerTechData data = getOrCreateData(player);
            data.removeMachine(pos);
            player.setData(ModAttachments.PLAYER_TECH_DATA.get(), data);
        }
    }

    public static void onPlayerLoggedIn(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            getOrCreateData(player);
            
            // Sync Nexus world state to the newly joined player
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                NexusWorldData worldData = NexusWorldData.get(serverLevel.getServer().overworld());
                NexusStatePacket packet = NexusStatePacket.fromWorldData(worldData);
                sendOptionalPayload(player, packet);
                if (worldData.hasChoiceBeenMade()) {
                    com.knoxhack.echoashfallprotocol.event.PostNexusEventHandler.syncPlayerToWorldChoice(player);
                    com.knoxhack.echoashfallprotocol.endgame.PostNexusData.syncToClient(player);
                }
            }
        }
    }

    public static PlayerTechData getOrCreateData(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_TECH_DATA.get());
    }

    public static float getThreatMultiplierForPlayer(ServerPlayer player) {
        return getOrCreateData(player).getThreatMultiplier();
    }

    public static int getTechLevel(ServerPlayer player) {
        return getOrCreateData(player).getTechLevel();
    }

    private static void sendOptionalPayload(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        EchoNetSend.toPlayer(player, payload, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}

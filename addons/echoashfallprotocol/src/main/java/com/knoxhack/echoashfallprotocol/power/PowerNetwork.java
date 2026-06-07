package com.knoxhack.echoashfallprotocol.power;

import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapDynamoBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalBurnerBlockEntity;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventHandler;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * FE network lookup for wired machine power.
 *
 * Machines may draw from direct adjacent power blocks or from relay networks
 * made of cables, nodes, banks, capacitors, and load distributors. This replaces
 * the old broad "nearby block aura" so cable routing is meaningful.
 */
public final class PowerNetwork {
    private static final int MAX_RELAY_BLOCKS = 256;

    private PowerNetwork() {
    }

    public static boolean tryConsumePower(Level level, BlockPos pos, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (FactoryControllerBlockEntity.isMachinePausedByController(level, pos)) {
            return false;
        }
        if (EnvironmentalEventHandler.isEventActive(level,
                com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType.BLACKOUT)) {
            return tryConsumeEmergencyPower(level, pos, amount);
        }
        if (tryConsumeAdjacentPower(level, pos, amount)) {
            return true;
        }
        return tryConsumeNetworkPower(level, pos, amount);
    }

    public static boolean hasPowerAccess(Level level, BlockPos pos) {
        if (FactoryControllerBlockEntity.isMachinePausedByController(level, pos)) {
            return false;
        }
        if (EnvironmentalEventHandler.isEventActive(level,
                com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType.BLACKOUT)) {
            return hasEmergencyReserve(level, pos);
        }
        return hasAdjacentPower(level, pos, 1) || scan(level, pos).storedEnergy() > 0;
    }

    public static int getAvailablePower(Level level, BlockPos pos) {
        if (FactoryControllerBlockEntity.isMachinePausedByController(level, pos)) {
            return 0;
        }
        if (EnvironmentalEventHandler.isEventActive(level,
                com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType.BLACKOUT)) {
            return getEmergencyReserve(level, pos);
        }
        int adjacent = 0;
        Set<BlockPos> counted = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (canSupplyNetwork(neighbor) && counted.add(neighborPos)) {
                adjacent += EnergyAccess.getBlockEnergyStored(level, neighborPos, dir.getOpposite());
            }
        }
        return adjacent + scan(level, pos).storedEnergy();
    }

    public static PowerDiagnostic diagnose(Level level, BlockPos pos) {
        return diagnose(level, pos, Math.max(1, estimateDemand(level, pos)));
    }

    public static PowerDiagnostic diagnose(Level level, BlockPos pos, int demandFe) {
        int demand = Math.max(0, demandFe);
        BlockEntity consumer = level.getBlockEntity(pos);
        int localStored = getLocalEnergyStored(consumer);
        int localCapacity = getLocalEnergyCapacity(consumer);
        NetworkReport report = scan(level, pos);
        int transferLimit = report.transferLimit();
        int adjacentExtractable = getAdjacentExtractable(level, pos, demand);
        int adjacentCapacity = getAdjacentCapacity(level, pos);
        boolean blackout = EnvironmentalEventHandler.isEventActive(level, EnvironmentalEventType.BLACKOUT);
        boolean controllerPaused = FactoryControllerBlockEntity.isMachinePausedByController(level, pos);
        boolean priorityPaused = report.priorityMode() == LoadDistributorBlockEntity.PriorityMode.SURVIVAL
                && !isSurvivalConsumer(consumer)
                && report.relayCount() > 0
                && report.storedEnergy() < Math.max(demand * 40, 400);

        PowerIssue issue;
        if (controllerPaused) {
            issue = PowerIssue.CONTROLLER_DISABLED;
        } else if (demand <= 0 || localStored >= demand || adjacentExtractable >= demand) {
            issue = PowerIssue.OK;
        } else if (blackout) {
            boolean emergencyOnline = report.storedEnergy() >= demand && transferLimit >= demand && !priorityPaused;
            issue = emergencyOnline ? PowerIssue.OK : PowerIssue.BLACKOUT_STORAGE_ONLY;
        } else if (report.relayCount() <= 0 && adjacentCapacity <= 0) {
            issue = localCapacity > 0 && localStored > 0 ? PowerIssue.LOCAL_BUFFER_EMPTY : PowerIssue.NO_LINK;
        } else if (priorityPaused) {
            issue = PowerIssue.PRIORITY_PAUSED;
        } else if (transferLimit > 0 && transferLimit < demand && report.storedEnergy() > 0) {
            issue = PowerIssue.CABLE_BOTTLENECK;
        } else if (report.storedEnergy() <= 0 && adjacentExtractable <= 0) {
            issue = PowerIssue.NETWORK_EMPTY;
        } else if (transferLimit > 0 && transferLimit < demand) {
            issue = PowerIssue.CABLE_BOTTLENECK;
        } else if (localCapacity > 0 && localStored < demand && report.storedEnergy() < demand) {
            issue = PowerIssue.LOCAL_BUFFER_EMPTY;
        } else {
            issue = PowerIssue.OK;
        }

        return new PowerDiagnostic(
                localStored,
                localCapacity,
                report.storedEnergy(),
                report.capacity(),
                transferLimit,
                demand,
                report.priorityMode(),
                issue
        );
    }

    public static NetworkReport scan(Level level, BlockPos pos) {
        Set<BlockPos> relays = new HashSet<>();
        Set<BlockPos> sources = new HashSet<>();
        ArrayDeque<SearchNode> queue = new ArrayDeque<>();

        for (Direction dir : Direction.values()) {
            BlockPos relayPos = pos.relative(dir);
            BlockEntity relay = level.getBlockEntity(relayPos);
            if (isRelay(relay) && relays.add(relayPos)) {
                queue.add(new SearchNode(relayPos, getRelayTransferRate(relay)));
            }
        }

        int stored = 0;
        int capacity = 0;
        int transferLimit = Integer.MAX_VALUE;
        int demand = estimateDemand(level, pos);
        LoadDistributorBlockEntity.PriorityMode priorityMode = LoadDistributorBlockEntity.PriorityMode.BALANCED;

        while (!queue.isEmpty() && relays.size() <= MAX_RELAY_BLOCKS) {
            SearchNode node = queue.removeFirst();
            BlockEntity relay = level.getBlockEntity(node.pos());
            if (!isRelay(relay)) {
                continue;
            }

            transferLimit = Math.min(transferLimit, Math.max(1, node.pathLimit()));
            if (relay instanceof LoadDistributorBlockEntity distributor) {
                priorityMode = distributor.getPriorityMode();
            }
            if (canSupplyNetwork(relay) && sources.add(node.pos())) {
                stored += EnergyAccess.getBlockEnergyStored(level, node.pos(), null);
                capacity += EnergyAccess.getBlockEnergyCapacity(level, node.pos(), null);
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = node.pos().relative(dir);
                BlockEntity neighbor = level.getBlockEntity(next);
                if (isRelay(neighbor)) {
                    if (relays.add(next)) {
                        queue.add(new SearchNode(next, Math.min(node.pathLimit(), getRelayTransferRate(neighbor))));
                    }
                    continue;
                }
                if (canSupplyNetwork(neighbor) && sources.add(next)) {
                    stored += EnergyAccess.getBlockEnergyStored(level, next, dir.getOpposite());
                    capacity += EnergyAccess.getBlockEnergyCapacity(level, next, dir.getOpposite());
                }
                if (isPowerConsumer(neighbor)) {
                    demand += estimateDemand(level, next);
                }
            }
        }

        if (transferLimit == Integer.MAX_VALUE) {
            transferLimit = 0;
        }
        return new NetworkReport(stored, capacity, transferLimit, relays.size(), sources.size(), demand, priorityMode);
    }

    public static boolean isRelay(BlockEntity be) {
        return be instanceof PowerCableBlockEntity
                || be instanceof PowerNodeBlockEntity
                || be instanceof BatteryBankBlockEntity
                || be instanceof NexusCapacitorBlockEntity
                || be instanceof LoadDistributorBlockEntity;
    }

    public static boolean canSupplyNetwork(BlockEntity be) {
        return be instanceof MicroGeneratorBlockEntity
                || be instanceof ThermalArrayBlockEntity
                || be instanceof ThermalBurnerBlockEntity
                || be instanceof ScrapDynamoBlockEntity
                || be instanceof BatteryBankBlockEntity
                || be instanceof NexusCapacitorBlockEntity
                || be instanceof PowerNodeBlockEntity
                || be instanceof PowerCableBlockEntity
                || be instanceof LoadDistributorBlockEntity;
    }

    public static int getRelayTransferRate(BlockEntity be) {
        if (be instanceof PowerCableBlockEntity cable) return cable.getMaxTransfer();
        if (be instanceof PowerNodeBlockEntity) return PowerNodeBlockEntity.MAX_TRANSFER;
        if (be instanceof BatteryBankBlockEntity) return BatteryBankBlockEntity.MAX_TRANSFER;
        if (be instanceof NexusCapacitorBlockEntity) return NexusCapacitorBlockEntity.MAX_TRANSFER;
        if (be instanceof LoadDistributorBlockEntity) return LoadDistributorBlockEntity.MAX_TRANSFER;
        return 0;
    }

    public static boolean isSurvivalConsumer(BlockEntity be) {
        String name = be == null ? "" : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getPath();
        return name.contains("water_purifier")
                || name.contains("field_med_bay")
                || name.contains("atmospheric_scrubber")
                || name.contains("radiation_cleanser");
    }

    private static boolean tryConsumeAdjacentPower(Level level, BlockPos pos, int amount) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (canSupplyNetwork(neighbor)
                    && !isRelay(neighbor)
                    && EnergyAccess.simulateExtractBlockEnergy(level, neighborPos, dir.getOpposite(), amount) >= amount) {
                EnergyAccess.extractBlockEnergy(level, neighborPos, dir.getOpposite(), amount);
                return true;
            }
        }
        return false;
    }

    private static boolean hasAdjacentPower(Level level, BlockPos pos, int amount) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (canSupplyNetwork(neighbor)
                    && !isRelay(neighbor)
                    && EnergyAccess.simulateExtractBlockEnergy(level, neighborPos, dir.getOpposite(), amount) >= amount) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryConsumeNetworkPower(Level level, BlockPos pos, int amount) {
        BlockEntity consumer = level.getBlockEntity(pos);
        NetworkReport report = scan(level, pos);
        if (report.transferLimit() < amount) {
            return false;
        }
        if (report.priorityMode() == LoadDistributorBlockEntity.PriorityMode.SURVIVAL
                && !isSurvivalConsumer(consumer)
                && report.storedEnergy() < Math.max(amount * 40, 400)) {
            return false;
        }

        Set<BlockPos> relays = new HashSet<>();
        ArrayDeque<SearchNode> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            BlockPos relayPos = pos.relative(dir);
            BlockEntity relay = level.getBlockEntity(relayPos);
            if (isRelay(relay) && relays.add(relayPos)) {
                queue.add(new SearchNode(relayPos, getRelayTransferRate(relay)));
            }
        }

        while (!queue.isEmpty() && relays.size() <= MAX_RELAY_BLOCKS) {
            SearchNode node = queue.removeFirst();
            BlockEntity relay = level.getBlockEntity(node.pos());
            if (!isRelay(relay) || node.pathLimit() < amount) {
                continue;
            }
            if (tryExtractCandidate(level, node.pos(), null, relay, amount)) {
                return true;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = node.pos().relative(dir);
                BlockEntity neighbor = level.getBlockEntity(next);
                if (isRelay(neighbor)) {
                    if (relays.add(next)) {
                        queue.add(new SearchNode(next, Math.min(node.pathLimit(), getRelayTransferRate(neighbor))));
                    }
                } else if (tryExtractCandidate(level, next, dir.getOpposite(), neighbor, amount)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryExtractCandidate(Level level, BlockPos pos, Direction side, BlockEntity be, int amount) {
        if (!canSupplyNetwork(be)) {
            return false;
        }
        if (EnergyAccess.simulateExtractBlockEnergy(level, pos, side, amount) < amount) {
            return false;
        }
        EnergyAccess.extractBlockEnergy(level, pos, side, amount);
        return true;
    }

    private static boolean isPowerConsumer(BlockEntity be) {
        return be instanceof com.knoxhack.echoashfallprotocol.capability.IEnergyStorage && !canSupplyNetwork(be);
    }

    public static int estimateDemand(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return 0;
        String path = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getPath();
        if (path.contains("water_purifier")) return 20;
        if (path.contains("field_med_bay")) return 20;
        if (path.contains("atmospheric_scrubber")) return 2;
        if (path.contains("scrap_press")) return 1;
        if (path.contains("filter_workbench")) return 2;
        if (path.contains("ore_grinder")) return 4;
        if (path.contains("isotope_refiner")) return 8;
        if (path.contains("crystalline_synthesizer")) return 24;
        if (path.contains("deep_core_miner")) return 40;
        if (path.contains("signal_scanner")) return 50;
        if (path.contains("autofeed_hopper")) return 10;
        if (path.contains("contaminant_condenser")) return 50;
        if (path.contains("radiation_cleanser")) return 8;
        return 1;
    }

    private static int getLocalEnergyStored(BlockEntity be) {
        return be instanceof IEnergyStorage storage ? storage.getEnergyStored() : 0;
    }

    private static int getLocalEnergyCapacity(BlockEntity be) {
        return be instanceof IEnergyStorage storage ? storage.getMaxEnergyStored() : 0;
    }

    private static int getAdjacentExtractable(Level level, BlockPos pos, int amount) {
        int extractable = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (canSupplyNetwork(neighbor) && !isRelay(neighbor)) {
                extractable += EnergyAccess.simulateExtractBlockEnergy(level, neighborPos, dir.getOpposite(), amount);
            }
        }
        return extractable;
    }

    private static int getAdjacentCapacity(Level level, BlockPos pos) {
        int capacity = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (canSupplyNetwork(neighbor) && !isRelay(neighbor)) {
                capacity += EnergyAccess.getBlockEnergyCapacity(level, neighborPos, dir.getOpposite());
            }
        }
        return capacity;
    }

    public static boolean hasEmergencyReserve(Level level, BlockPos pos) {
        return getEmergencyReserve(level, pos) > 0;
    }

    public static int getEmergencyReserve(Level level, BlockPos pos) {
        return scan(level, pos).storedEnergy();
    }

    private static boolean tryConsumeEmergencyPower(Level level, BlockPos pos, int amount) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof BatteryBankBlockEntity || be instanceof NexusCapacitorBlockEntity) {
                if (EnergyAccess.simulateExtractBlockEnergy(level, neighborPos, dir.getOpposite(), amount) >= amount) {
                    EnergyAccess.extractBlockEnergy(level, neighborPos, dir.getOpposite(), amount);
                    return true;
                }
            }
        }
        return tryConsumeNetworkPower(level, pos, amount);
    }

    private record SearchNode(BlockPos pos, int pathLimit) {
    }

    public record NetworkReport(int storedEnergy, int capacity, int transferLimit, int relayCount, int sourceCount,
                                int estimatedDemand, LoadDistributorBlockEntity.PriorityMode priorityMode) {
        public String bottleneckLabel() {
            return transferLimit <= 0 ? "NO CABLE LINK" : transferLimit + " FE/t";
        }
    }
}

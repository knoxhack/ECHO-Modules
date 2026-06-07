package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.FactoryControllerBlock;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Factory Controller Block Entity - monitors and controls linked machines.
 * Scans for connected machines and provides centralized management.
 */
public class FactoryControllerBlockEntity extends BlockEntity {
    private static final int SCAN_RADIUS = 16;
    private static final int SCAN_INTERVAL = 20; // Scan every second
    
    private int scanTimer = 0;
    private boolean networkEnabled = true;
    private int connectedMachines = 0;
    private int activeMachines = 0;
    private int totalPowerStored = 0;
    private int totalPowerCapacity = 0;
    private boolean hasErrors = false;
    
    // Cached list of connected machines
    private final List<BlockPos> machinePositions = new ArrayList<>();
    
    public FactoryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_CONTROLLER.get(), pos, state);
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, 
            FactoryControllerBlockEntity entity) {
        if (level.isClientSide()) return;
        
        // Periodic scan
        if (++entity.scanTimer >= SCAN_INTERVAL) {
            entity.scanTimer = 0;
            entity.scanNetwork(level, pos);
            entity.updateBlockState(level, pos, state);
        }
    }
    
    /**
     * Scan for connected machines in radius using BFS.
     */
    private void scanNetwork(Level level, BlockPos pos) {
        machinePositions.clear();
        connectedMachines = 0;
        activeMachines = 0;
        totalPowerStored = 0;
        totalPowerCapacity = 0;
        hasErrors = false;
        
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> toCheck = new ArrayList<>();
        toCheck.add(pos);
        visited.add(pos);
        
        while (!toCheck.isEmpty() && visited.size() < 100) {
            BlockPos current = toCheck.remove(0);
            
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                if (visited.contains(neighborPos)) continue;
                if (neighborPos.distManhattan(pos) > SCAN_RADIUS) continue;
                
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be == null) continue;
                
                visited.add(neighborPos);
                
                // Check if this is a machine we can track
                if (isTrackableMachine(be)) {
                    machinePositions.add(neighborPos);
                    connectedMachines++;
                    
                    BlockState machineBlockState = level.getBlockState(neighborPos);
                    MachineState machineState = machineState(level, neighborPos, machineBlockState);
                    if (isActiveState(machineState, machineBlockState)) {
                        activeMachines++;
                    }
                    if (isProblemState(machineState)) {
                        hasErrors = true;
                    }
                    
                    // Check for energy storage
                    if (be instanceof IEnergyStorage energy) {
                        totalPowerStored += energy.getEnergyStored();
                        totalPowerCapacity += energy.getMaxEnergyStored();
                    }
                    
                    // Continue scanning from machines (network propagation)
                    toCheck.add(neighborPos);
                }
                
                // Always scan through Item Pipes and Power Cables
                if (isControllerNetworkNode(be)) {
                    toCheck.add(neighborPos);
                }
            }
        }
    }
    
    public static boolean isMachinePausedByController(Level level, BlockPos machinePos) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> toCheck = new ArrayDeque<>();
        toCheck.add(machinePos);
        visited.add(machinePos);

        while (!toCheck.isEmpty() && visited.size() < 100) {
            BlockPos current = toCheck.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                if (visited.contains(neighborPos)) {
                    continue;
                }
                if (neighborPos.distManhattan(machinePos) > SCAN_RADIUS) {
                    continue;
                }

                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be == null) {
                    continue;
                }
                visited.add(neighborPos);
                if (be instanceof FactoryControllerBlockEntity controller) {
                    return !controller.isNetworkEnabled();
                }
                if (isControllerNetworkNode(be)) {
                    toCheck.add(neighborPos);
                }
            }
        }
        return false;
    }

    private static boolean isControllerNetworkNode(BlockEntity be) {
        return isTrackableMachine(be)
                || be instanceof ItemPipeBlockEntity
                || be instanceof PowerCableBlockEntity;
    }

    private static boolean isTrackableMachine(BlockEntity be) {
        return be instanceof HandRecyclerBlockEntity ||
               be instanceof OreGrinderBlockEntity ||
               be instanceof FilterWorkbenchBlockEntity ||
               be instanceof ScrapPressBlockEntity ||
               be instanceof ThermalBurnerBlockEntity ||
               be instanceof WaterPurifierBlockEntity ||
               be instanceof PowerCableBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.ScrapDynamoBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity ||
               be instanceof com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
    }

    private static MachineState machineState(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof MachineStateProvider provider) {
            return provider.getMachineState(level, pos, state);
        }
        return null;
    }

    private static boolean isActiveState(MachineState machineState, BlockState state) {
        return machineState == MachineState.PROCESSING
                || machineState == MachineState.GENERATING
                || booleanPropertyValue(state, "active");
    }

    private static boolean isProblemState(MachineState machineState) {
        return machineState == MachineState.UNPOWERED
                || machineState == MachineState.JAMMED
                || machineState == MachineState.OFFLINE
                || machineState == MachineState.BLOCKED
                || machineState == MachineState.BROWNOUT
                || machineState == MachineState.BOTTLENECK
                || machineState == MachineState.PRIORITY_PAUSED
                || machineState == MachineState.CONTROLLER_DISABLED
                || machineState == MachineState.UNSTABLE;
    }

    private static boolean booleanPropertyValue(BlockState state, String propertyName) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && propertyName.equals(property.getName())) {
                return state.getValue(booleanProperty);
            }
        }
        return false;
    }
    
    private void updateBlockState(Level level, BlockPos pos, BlockState state) {
        boolean shouldBeActive = networkEnabled && connectedMachines > 0;
        boolean shouldHaveError = hasErrors || !networkEnabled;
        
        boolean wasActive = state.getValue(FactoryControllerBlock.ACTIVE);
        boolean hadError = state.getValue(FactoryControllerBlock.ERROR);
        
        if (wasActive != shouldBeActive || hadError != shouldHaveError) {
            level.setBlockAndUpdate(pos, state
                .setValue(FactoryControllerBlock.ACTIVE, shouldBeActive)
                .setValue(FactoryControllerBlock.ERROR, shouldHaveError));
        }
    }
    
    /**
     * Toggle all connected machines on/off.
     */
    public void toggleNetworkState() {
        networkEnabled = !networkEnabled;
        refreshScanAndState();
        setChanged();
    }
    
    public void setNetworkEnabled(boolean enabled) {
        networkEnabled = enabled;
        refreshScanAndState();
        setChanged();
    }

    private void refreshScanAndState() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            scanNetwork(level, worldPosition);
            updateBlockState(level, worldPosition, state);
        }
    }
    
    // === Getters for UI/Redstone ===
    
    public int getConnectedMachines() { return connectedMachines; }
    public int getActiveMachines() { return activeMachines; }
    public int getTotalPowerStored() { return totalPowerStored; }
    public int getTotalPowerCapacity() { return totalPowerCapacity; }
    public boolean isNetworkEnabled() { return networkEnabled; }
    public int getNetworkPowerPercentage() {
        return totalPowerCapacity > 0 ? (totalPowerStored * 100 / totalPowerCapacity) : 0;
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("networkEnabled", networkEnabled);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        networkEnabled = input.getBooleanOr("networkEnabled", true);
    }
}

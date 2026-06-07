package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.WaterPurifierBlock;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echoashfallprotocol.block.menu.WaterPurifierMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Water Purifier — converts dirty water + filter cartridge into clean water.
 */
public class WaterPurifierBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    private static final int WATER_INPUT_SLOT = 0;
    private static final int FILTER_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    public static final int BATTERY_SLOT = 3;
    private static final int PROCESS_TIME = 60;
    private static final int ENERGY_PER_PURIFY = 20;
    private static final int ENERGY_CAPACITY = 1_000;
    private static final int ENERGY_TRANSFER = 64;

    private final MachineInventory inventory = new MachineInventory(4, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);

    private int progress = 0;
    private int maxProgress = PROCESS_TIME;
    private boolean hasPower = false;
    private int wearPercent = 0;
    private int batchSize = 1;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> hasPower ? 1 : 0;
                case 3 -> wearPercent;
                case 4 -> energyStorage.getEnergyStored();
                case 5 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> hasPower = value != 0;
                case 3 -> wearPercent = value;
                case 4 -> energyStorage.setEnergyStored(value);
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    private static final Map<Item, Item> CONTAMINATED_PURIFY = Map.of(
            ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT,
            ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT,
            ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE,
            ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI
    );

    public static Map<Item, Item> getContaminatedPurify() {
        return CONTAMINATED_PURIFY;
    }

    public WaterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PURIFIER.get(), pos, state);
    }

    public MachineInventory getInventory() { return inventory; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.EchoAshfallProtocol.water_purifier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WaterPurifierMenu(containerId, playerInventory, this, data);
    }

    private boolean tryExtractPower(Level level, BlockPos pos) {
        int energyCost = MachineGameplayHelper.getAdjustedPowerCost(level, pos, ENERGY_PER_PURIFY * batchSize);
        return EnergyAccess.tryConsumeLocalOrNetworkPower(this, level, pos, energyCost)
                || tryConsumeOneHopRelayPower(level, pos, energyCost);
    }

    private static boolean tryConsumeOneHopRelayPower(Level level, BlockPos pos, int amount) {
        for (Direction direction : Direction.values()) {
            BlockPos relayPos = pos.relative(direction);
            BlockEntity relay = level.getBlockEntity(relayPos);
            if (!PowerNetwork.isRelay(relay)) {
                continue;
            }
            if (EnergyAccess.simulateExtractBlockEnergy(level, relayPos, null, amount) >= amount) {
                EnergyAccess.extractBlockEnergy(level, relayPos, null, amount);
                return true;
            }
            for (Direction sourceDirection : Direction.values()) {
                BlockPos sourcePos = relayPos.relative(sourceDirection);
                if (sourcePos.equals(pos)) {
                    continue;
                }
                BlockEntity source = level.getBlockEntity(sourcePos);
                if (PowerNetwork.canSupplyNetwork(source)
                        && EnergyAccess.simulateExtractBlockEnergy(level, sourcePos, null, amount) >= amount) {
                    EnergyAccess.extractBlockEnergy(level, sourcePos, null, amount);
                    return true;
                }
            }
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterPurifierBlockEntity entity) {
        boolean wasActive = state.getValue(WaterPurifierBlock.ACTIVE);
        boolean isProcessing = false;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        entity.maxProgress = MachineGameplayHelper.getAdjustedProcessTime(level, pos, PROCESS_TIME * entity.batchSize);

        // Check machine wear and jam status
        MachineWearData wearData = new MachineWearData(level);
        boolean isJammed = wearData.isJammed(pos);
        entity.wearPercent = (int)(wearData.getWearPercent(pos) * 100);

        if (!isJammed && entity.hasRecipe()) {
            entity.hasPower = entity.tryExtractPower(level, pos);
            if (entity.hasPower) {
                isProcessing = true;
                entity.progress++;
                entity.setChanged();

                // Accumulate wear during processing (1 wear per 10 ticks)
                if (entity.progress % 10 == 0) {
                    wearData.addWear(pos, 1, level.getRandom());

                    // Check for jam at high wear
                    if (wearData.checkJamChance(pos, level.getRandom())) {
                        level.players().forEach(player -> {
                            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                        "§c[ECHO-7]§r Water Purifier has jammed! Repair needed."));
                            }
                        });
                    }
                }

                if (entity.progress >= entity.maxProgress) {
                    entity.craftItem();
                    AshfallAdapterCoreMachineRuntimeHost.outputCreated(
                            level, pos, entity, entity.inventory.getStackInSlot(OUTPUT_SLOT).copy());
                    entity.progress = 0;
                    // Try to chain output to adjacent machines
                    entity.tryPushOutputToNeighbors(level, pos);
                }
            } else {
                entity.progress = Math.max(0, entity.progress - 1);
            }
        } else {
            entity.progress = 0;
        }

        if (wasActive != isProcessing) {
            level.setBlockAndUpdate(pos, state.setValue(WaterPurifierBlock.ACTIVE, isProcessing));
        }
    }

    private boolean hasRecipe() {
        ItemStack waterInput = inventory.getStackInSlot(WATER_INPUT_SLOT);
        ItemStack filter = inventory.getStackInSlot(FILTER_SLOT);
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);

        boolean hasFilter = !filter.isEmpty() && (
                filter.is(ModItems.FILTER_CARTRIDGE_BASIC.get()) ||
                filter.is(ModItems.FILTER_CARTRIDGE_ADVANCED.get()) ||
                filter.is(ModItems.FILTER_CARTRIDGE_ELITE.get()));

        // Standard dirty water purification - batch processing up to 3 bottles
        if (!waterInput.isEmpty() && waterInput.is(ModItems.DIRTY_WATER_BOTTLE.get()) && hasFilter) {
            int availableBottles = waterInput.getCount();
            int maxBatch = Math.min(3, availableBottles);
            int availableOutputSpace = output.isEmpty() ? output.getMaxStackSize() :
                    output.getMaxStackSize() - output.getCount();

            batchSize = Math.min(maxBatch, availableOutputSpace);
            return batchSize > 0;
        }

        // Reset batch size for other recipes
        batchSize = 1;

        // Contaminated resource purification (uses filter cartridge)
        if (!waterInput.isEmpty() && hasFilter) {
            Item purified = CONTAMINATED_PURIFY.get(waterInput.getItem());
            if (purified != null) {
                boolean canOutput = output.isEmpty() ||
                        (output.is(purified) && output.getCount() < output.getMaxStackSize());
                if (canOutput) return true;
            }
        }

        return false;
    }

    private void craftItem() {
        ItemStack input = inventory.getStackInSlot(WATER_INPUT_SLOT);
        ItemStack filter = inventory.getStackInSlot(FILTER_SLOT);
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);

        // Determine output item
        Item outputItem;
        Item contamPurified = CONTAMINATED_PURIFY.get(input.getItem());
        if (contamPurified != null) {
            outputItem = contamPurified;
        } else {
            outputItem = ModItems.CLEAN_WATER_BOTTLE.get();
        }

        // Process the batch
        input.shrink(batchSize);

        // Chance to consume filter scales with batch size
        for (int i = 0; i < batchSize; i++) {
            if (level != null && level.getRandom().nextFloat() < 0.15f) {
                filter.shrink(1);
                if (filter.getDamageValue() >= filter.getMaxDamage()) {
                    filter.shrink(1);
                }
            }
        }

        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(outputItem, batchSize));
        } else {
            output.grow(batchSize);
        }
    }

    /**
     * Try to push output items to adjacent machines (machine chaining).
     */
    private void tryPushOutputToNeighbors(Level level, BlockPos pos) {
        MachineChainingHelper.tryPushOutput(level, pos, inventory, OUTPUT_SLOT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput invOut = output.child("inventory");
        inventory.serialize(invOut);
        output.putInt("progress", progress);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = input.getIntOr("progress", 0);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
    }

    @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int received = energyStorage.receiveEnergy(amount, simulate);
        if (received > 0 && !simulate) setChanged();
        return received;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int extracted = energyStorage.extractEnergy(amount, simulate);
        if (extracted > 0 && !simulate) setChanged();
        return extracted;
    }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return energyStorage.getEnergyStored() > 0; }
    @Override public void setEnergyStored(int energy) {
        energyStorage.setEnergyStored(energy);
        setChanged();
    }

    // === HopperHandler Implementation ===
    @Override
    public int[] getInputSlots(Direction side) {
        // Accept dirty water and filters from top or sides
        return new int[]{WATER_INPUT_SLOT, FILTER_SLOT};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Extract clean water from bottom only
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        // Slot 0: Accept dirty water or contaminated items
        // Slot 1: Accept filter cartridges
        if (slot == WATER_INPUT_SLOT) {
            return stack.is(ModItems.DIRTY_WATER_BOTTLE.get()) ||
                   stack.is(ModItems.CONTAMINATED_IRON.get()) ||
                   stack.is(ModItems.CONTAMINATED_GOLD.get()) ||
                   stack.is(ModItems.CONTAMINATED_REDSTONE.get()) ||
                   stack.is(ModItems.CONTAMINATED_LAPIS.get());
        }
        if (slot == FILTER_SLOT) {
            return stack.is(ModItems.FILTER_CARTRIDGE_BASIC.get()) ||
                   stack.is(ModItems.FILTER_CARTRIDGE_ADVANCED.get()) ||
                   stack.is(ModItems.FILTER_CARTRIDGE_ELITE.get());
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        // Only extract from output slot
        return slot == OUTPUT_SLOT;
    }
}

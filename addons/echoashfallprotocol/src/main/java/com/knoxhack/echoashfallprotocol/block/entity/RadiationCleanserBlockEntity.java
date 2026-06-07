package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.menu.RadiationCleanserMenu;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Radiation Cleanser Block Entity — Removes contamination from items.
 * Input: Contaminated item + Filter Cartridge
 * Output: Clean item
 * Duration: 400 ticks (20 seconds)
 * Power: 8 FE/tick
 */
public class RadiationCleanserBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {
    public static final int INPUT_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int BATTERY_SLOT = 3;
    public static final int TOTAL_TICKS = 400;
    public static final int POWER_PER_TICK = 8;
    private static final int ENERGY_CAPACITY = 4_000;
    private static final int ENERGY_TRANSFER = 256;

    private final MachineInventory inventory = new MachineInventory(4, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    private int progress = 0;
    private int wearLevel = 0;
    private long nextFeedbackTick = 0L;

    // Mapping of contaminated items to their clean versions
    private static final java.util.Map<Item, Item> DECONTAMINATION_MAP = new java.util.HashMap<>();
    static {
        // Contaminated iron -> iron ingot
        DECONTAMINATION_MAP.put(ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT);
        DECONTAMINATION_MAP.put(ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT);
        DECONTAMINATION_MAP.put(ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE);
        DECONTAMINATION_MAP.put(ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI);
    }

    public static java.util.Map<Item, Item> getDecontaminationMap() {
        return DECONTAMINATION_MAP;
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> TOTAL_TICKS;
                case 2 -> wearLevel;
                case 3 -> energyStorage.getEnergyStored();
                case 4 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }
        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 2 -> wearLevel = v;
                case 3 -> energyStorage.setEnergyStored(v);
                case 4 -> {
                }
            }
        }
        @Override
        public int getCount() { return 5; }
    };

    public RadiationCleanserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATION_CLEANSER.get(), pos, state);
    }

    public MachineInventory getInventory() { return inventory; }

    @Override
    public Component getDisplayName() { return Component.literal("Radiation Cleanser"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadiationCleanserMenu(id, inv, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RadiationCleanserBlockEntity entity) {
        long gameTime = level.getGameTime();
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        ItemStack input = entity.inventory.getStackInSlot(INPUT_SLOT);
        ItemStack filter = entity.inventory.getStackInSlot(FILTER_SLOT);
        ItemStack output = entity.inventory.getStackInSlot(OUTPUT_SLOT);

        // Check if we have valid input (contaminated item)
        Item cleanVersion = DECONTAMINATION_MAP.get(input.getItem());
        if (cleanVersion == null) {
            entity.progress = 0;
            if (!input.isEmpty()) {
                entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Cleanser blocked: input is not a known contaminated item.", gameTime);
            }
            return;
        }

        // Check for filter
        if (filter.isEmpty() || !filter.is(ModItems.FILTER_CARTRIDGE_ADVANCED.get())) {
            entity.progress = 0;
            entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Cleanser blocked: advanced filter cartridge required.", gameTime);
            return;
        }

        // Check output slot can accept result
        if (!output.isEmpty() && (!output.is(cleanVersion) || output.getCount() >= output.getMaxStackSize())) {
            entity.progress = 0;
            entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Cleanser blocked: output slot full or incompatible.", gameTime);
            return;
        }

        // Check power
        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_PER_TICK)) {
            entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Cleanser idle: missing power.", gameTime);
            return; // No power
        }

        // Progress
        entity.progress++;
        entity.wearLevel++;
        entity.setChanged();
        entity.notifyNearby(level, pos, "\u00A7b[ECHO-7]\u00A7r Radiation Cleanser active: contaminated salvage is being stabilized.", gameTime);

        if (entity.progress >= TOTAL_TICKS) {
            entity.completeCleansing(input.getItem(), cleanVersion);
            entity.nextFeedbackTick = 0L;
            entity.notifyNearby(level, pos, "\u00A7a[ECHO-7]\u00A7r Cleanser cycle complete. Output ready.", gameTime);
        }
    }

    private void completeCleansing(Item contaminatedItem, Item cleanItem) {
        // Consume input
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        input.shrink(1);

        // Consume filter (20% chance per cleanse)
        ItemStack filter = inventory.getStackInSlot(FILTER_SLOT);
        if (level != null && level.getRandom().nextFloat() < 0.2f) {
            filter.shrink(1);
        }

        // Produce output
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(cleanItem, 1));
        } else if (output.is(cleanItem)) {
            output.grow(1);
        }

        progress = 0;
        setChanged();
        if (level != null) {
            AshfallAdapterCoreHazardRuntime.radiationCleanserUsed(level, worldPosition, contaminatedItem, cleanItem);
        }
    }

    private void notifyNearby(Level level, BlockPos pos, String message, long gameTime) {
        if (gameTime < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = gameTime + 160L;
        AABB area = new AABB(pos).inflate(6.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            player.sendSystemMessage(Component.literal(message), true);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inventory.serialize(out.child("inventory"));
        out.putInt("progress", progress);
        out.putInt("wear", wearLevel);
        out.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = in.getIntOr("progress", 0);
        wearLevel = in.getIntOr("wear", 0);
        energyStorage.setEnergyStored(in.getIntOr("energy", 0));
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
}

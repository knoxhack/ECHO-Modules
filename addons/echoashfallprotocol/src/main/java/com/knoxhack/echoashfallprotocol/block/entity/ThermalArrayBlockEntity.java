package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.ThermalArrayBlock;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Thermal Array Block Entity — Tier 2.5 power generator.
 * Generates 30 FE/tick from fuel (3x Micro Generator output).
 * 3 fuel slots for longer autonomous operation.
 * Can output power to adjacent IEnergyStorage blocks.
 */
public class ThermalArrayBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {
    private static final int BURN_TICKS_PER_COAL = 240; // 12 seconds per coal (2x Micro Gen efficiency)
    private static final int FE_PER_TICK = 30; // 3x Micro Generator output
    private static final int MAX_ENERGY = 9000; // 3x Micro Generator capacity
    public static final int BATTERY_SLOT = 3;
    private static final int WEAR_ACCUMULATION_INTERVAL = 300; // Every 15 seconds

    private final MachineInventory inventory = new MachineInventory(4, this::setChanged);

    private int energy = 0;
    private int maxEnergy = MAX_ENERGY;
    private int burnTimeRemaining = 0;
    private int maxBurnTime = 0;
    private boolean failed = false;
    private int failureChance = 2; // 2% base failure chance (lower than Micro Gen)

    private int wearCounter = 0;
    private MachineWearData wearData;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> maxEnergy;
                case 2 -> burnTimeRemaining;
                case 3 -> maxBurnTime;
                case 4 -> failed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = value;
                case 1 -> maxEnergy = value;
                case 2 -> burnTimeRemaining = value;
                case 3 -> maxBurnTime = value;
                case 4 -> failed = value != 0;
            }
        }

        @Override
        public int getCount() { return 5; }
    };

    public ThermalArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_ARRAY.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }

    public MachineInventory getInventory() { return inventory; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.EchoAshfallProtocol.thermal_array");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.knoxhack.echoashfallprotocol.block.menu.ThermalArrayMenu(containerId, playerInventory, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalArrayBlockEntity entity) {
        boolean wasActive = state.getValue(ThermalArrayBlock.ACTIVE);
        boolean isActive = false;
        EnergyAccess.chargeBatteryFromStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);

        if (entity.failed) {
            if (wasActive) {
                level.setBlockAndUpdate(pos, state.setValue(ThermalArrayBlock.ACTIVE, false));
            }
            return;
        }

        // Check machine wear
        if (entity.wearData != null) {
            float wearPercent = entity.wearData.getWearPercent(pos);
            // Increase failure chance as wear increases
            entity.failureChance = 2 + (int)(wearPercent * 5); // 2% to 7%

            // Check for jam at high wear
            if (entity.wearData.isJammed(pos)) {
                entity.failed = true;
                level.players().forEach(player -> {
                    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 144) {
                        player.sendSystemMessage(Component.literal(
                                "§c[ECHO-7]§r Thermal Array has jammed! Repair needed."));
                    }
                });
                level.setBlockAndUpdate(pos, state.setValue(ThermalArrayBlock.ACTIVE, false));
                return;
            }
        }

        // Try to consume fuel if not burning
        if (entity.burnTimeRemaining <= 0) {
            ItemStack fuelStack = entity.findNextFuel();
            if (!fuelStack.isEmpty()) {
                entity.burnTimeRemaining = entity.getBurnTimeForFuel(fuelStack);
                entity.maxBurnTime = entity.burnTimeRemaining;
                fuelStack.shrink(1);
                entity.setChanged();
            }
        }

        // Generate power if burning
        if (entity.burnTimeRemaining > 0) {
            entity.burnTimeRemaining--;
            isActive = true;

            // Generate energy up to max
            if (entity.energy < entity.maxEnergy) {
                entity.energy = Math.min(entity.maxEnergy, entity.energy + FE_PER_TICK);
            }

            // Random failure check (less frequent than Micro Gen)
            if (level.getRandom().nextInt(100) < entity.failureChance) {
                entity.failed = true;
                entity.burnTimeRemaining = 0;
                level.players().forEach(player -> {
                    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 144) {
                        player.sendSystemMessage(Component.literal(
                                "§c[ECHO-7]§r Thermal Array failure detected! Manual restart required."));
                    }
                });
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
                isActive = false;
            }

            // Accumulate wear
            entity.wearCounter++;
            if (entity.wearCounter >= WEAR_ACCUMULATION_INTERVAL) {
                entity.wearCounter = 0;
                if (entity.wearData != null) {
                    entity.wearData.addWear(pos, 1, level.getRandom());
                }
            }

            // Visual effects when active
            if (level.getRandom().nextFloat() < 0.1f && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                double py = pos.getY() + 0.8;
                double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0.02, 0, 0.01);
            }

            // Output power to adjacent blocks
            entity.distributePower(level, pos);

            entity.setChanged();
        }

        if (wasActive != isActive) {
            level.setBlockAndUpdate(pos, state.setValue(ThermalArrayBlock.ACTIVE, isActive));
        }
    }

    private ItemStack findNextFuel() {
        // Check fuel slots in order
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (isFuel(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ||
               stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) ||
               stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS) ||
               stack.is(Items.STICK);
    }

    private int getBurnTimeForFuel(ItemStack stack) {
        if (stack.is(Items.COAL)) return BURN_TICKS_PER_COAL;
        if (stack.is(Items.CHARCOAL)) return BURN_TICKS_PER_COAL;
        if (stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) ||
            stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS)) {
            return BURN_TICKS_PER_COAL / 4; // Planks burn 1/4 as long as coal
        }
        if (stack.is(Items.STICK)) return BURN_TICKS_PER_COAL / 8; // Sticks burn 1/8 as long
        return 0;
    }

    private void distributePower(Level level, BlockPos pos) {
        if (energy <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            EnergyAccess.transferFromStorageToBlock(this, level, neighborPos, dir.getOpposite(), FE_PER_TICK);
            if (energy <= 0) break;
        }
    }

    public void restart() {
        if (failed) {
            failed = false;
            burnTimeRemaining = 0;
            setChanged();
        }
    }

    public boolean isFailed() {
        return failed;
    }

    public float getWearPercent() {
        return wearData != null ? wearData.getWearPercent(worldPosition) : 0f;
    }

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    // IEnergyStorage implementation
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0; // Cannot receive, only generate
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int toExtract = Math.min(energy, maxExtract);
        if (!simulate) {
            energy -= toExtract;
            setChanged();
        }
        return toExtract;
    }

    @Override
    public int getEnergyStored() { return energy; }

    @Override
    public int getMaxEnergyStored() { return maxEnergy; }

    @Override
    public boolean canExtract() { return energy > 0; }

    @Override
    public boolean canReceive() { return false; }

    @Override
    public void setEnergyStored(int energy) {
        this.energy = Math.min(energy, maxEnergy);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("energy", energy);
        out.putInt("burnTime", burnTimeRemaining);
        out.putInt("maxBurnTime", maxBurnTime);
        out.putBoolean("failed", failed);
        inventory.serialize(out.child("inventory"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy = in.getIntOr("energy", 0);
        burnTimeRemaining = in.getIntOr("burnTime", 0);
        maxBurnTime = in.getIntOr("maxBurnTime", 0);
        failed = in.getBooleanOr("failed", false);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
    }
}

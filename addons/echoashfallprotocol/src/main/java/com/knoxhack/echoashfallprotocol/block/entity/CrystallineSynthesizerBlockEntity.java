package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.CrystallineSynthesizerBlock;
import com.knoxhack.echoashfallprotocol.block.menu.CrystallineSynthesizerMenu;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Crystalline Synthesizer Block Entity - Tier 3 extraction.
 */
public class CrystallineSynthesizerBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int TOTAL_TICKS = 400;
    private static final int ENERGY_CAPACITY = 8_000;
    private static final int ENERGY_TRANSFER = 512;

    private static final int PHASE2_START = (int) (TOTAL_TICKS * 0.25f);
    private static final int PHASE3_START = (int) (TOTAL_TICKS * 0.60f);
    private static final int PHASE4_START = (int) (TOTAL_TICKS * 0.90f);

    private final MachineInventory inventory = new MachineInventory(5, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    private int progress = 0;
    private int currentPhase = 0;
    private int determinedOutputIndex = -1;
    private boolean hadPowerFailure = false;
    private int wearPercent = 0;
    private boolean jammed = false;

    private static final Item[] POSSIBLE_OUTPUTS = {
            Items.DIAMOND,
            Items.EMERALD,
            Items.NETHERITE_SCRAP
    };

    public static Item[] getPossibleOutputs() {
        return POSSIBLE_OUTPUTS.clone();
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> TOTAL_TICKS;
                case 2 -> currentPhase;
                case 3 -> wearPercent;
                case 4 -> jammed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 2 -> currentPhase = v;
                case 3 -> wearPercent = v;
                case 4 -> jammed = v != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CrystallineSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLINE_SYNTHESIZER.get(), pos, state);
    }

    public MachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Crystalline Synthesizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrystallineSynthesizerMenu(id, inv, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystallineSynthesizerBlockEntity entity) {
        int oldPhase = entity.currentPhase;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        MachineWearData wearData = new MachineWearData(level);
        entity.wearPercent = (int) (wearData.getWearPercent(pos) * 100);
        entity.jammed = wearData.isJammed(pos);

        if (entity.currentPhase == 0 && !entity.hasIngredients()) {
            return;
        }

        int powerCost = switch (entity.currentPhase) {
            case 1 -> 3;
            case 4 -> 1;
            default -> 2;
        };

        boolean hasPower = entity.currentPhase > 0 && !entity.jammed &&
                EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos,
                        MachineGameplayHelper.getAdjustedPowerCost(level, pos, powerCost));

        if (entity.currentPhase == 0 && entity.hasIngredients()) {
            entity.currentPhase = 1;
            entity.progress = 0;
            entity.hadPowerFailure = false;
            entity.determinedOutputIndex = -1;
            entity.setChanged();
            return;
        }

        if (!hasPower || entity.jammed) {
            if (entity.currentPhase == 2 || entity.currentPhase == 3) {
                entity.hadPowerFailure = true;
            }
            if (entity.currentPhase == 1) {
                entity.currentPhase = 0;
                entity.progress = 0;
            }
            return;
        }

        entity.progress++;
        entity.setChanged();

        if (entity.progress % 20 == 0) {
            int wearAmount = entity.currentPhase >= 3 ? 2 : 1;
            wearData.addWear(pos, wearAmount, level.getRandom());
            if (wearData.checkJamChance(pos, level.getRandom())) {
                entity.jammed = true;
                return;
            }
        }

        if (entity.progress >= PHASE2_START && entity.currentPhase == 1) {
            entity.currentPhase = 2;
        } else if (entity.progress >= PHASE3_START && entity.currentPhase == 2) {
            entity.currentPhase = 3;
            if (entity.determinedOutputIndex < 0) {
                entity.determinedOutputIndex = level.getRandom().nextInt(POSSIBLE_OUTPUTS.length);
            }
        } else if (entity.progress >= PHASE4_START && entity.currentPhase == 3) {
            entity.currentPhase = 4;
        }

        if (entity.progress >= TOTAL_TICKS) {
            entity.finishReaction(level);
        }

        if (entity.currentPhase != oldPhase) {
            level.setBlockAndUpdate(pos, state.setValue(CrystallineSynthesizerBlock.PHASE, entity.currentPhase));
        }
    }

    private boolean hasIngredients() {
        ItemStack in1 = inventory.getStackInSlot(INPUT_SLOT_1);
        ItemStack in2 = inventory.getStackInSlot(INPUT_SLOT_2);
        ItemStack cat = inventory.getStackInSlot(CATALYST_SLOT);
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);

        boolean hasGemFrag = in1.is(ModItems.GEM_FRAGMENT.get()) && in1.getCount() >= 4;
        boolean hasDenseAlloy = in2.is(ModItems.DENSE_ALLOY_CHUNK.get()) && in2.getCount() >= 1;
        boolean hasCatalyst = cat.is(ModItems.ENERGY_CELL.get()) && cat.getCount() >= 2;
        boolean outputClear = out.isEmpty() || out.getCount() < out.getMaxStackSize();

        return hasGemFrag && hasDenseAlloy && hasCatalyst && outputClear;
    }

    private void finishReaction(Level level) {
        inventory.getStackInSlot(INPUT_SLOT_1).shrink(4);
        inventory.getStackInSlot(INPUT_SLOT_2).shrink(1);
        inventory.getStackInSlot(CATALYST_SLOT).shrink(2);

        Item output;
        if (hadPowerFailure && determinedOutputIndex < 2) {
            output = Items.NETHERITE_SCRAP;
        } else if (determinedOutputIndex >= 0) {
            output = POSSIBLE_OUTPUTS[determinedOutputIndex];
        } else {
            output = Items.DIAMOND;
        }

        ItemStack outStack = inventory.getStackInSlot(OUTPUT_SLOT);
        if (outStack.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(output, 1));
        } else if (outStack.is(output)) {
            outStack.grow(1);
        }
        AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, worldPosition, this, new ItemStack(output, 1));

        progress = 0;
        currentPhase = 0;
        determinedOutputIndex = -1;
        hadPowerFailure = false;
        jammed = false;
        setChanged();
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CrystallineSynthesizerBlock.PHASE, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inventory.serialize(out.child("inventory"));
        out.putInt("progress", progress);
        out.putInt("phase", currentPhase);
        out.putInt("outputIndex", determinedOutputIndex);
        out.putBoolean("powerFailure", hadPowerFailure);
        out.putBoolean("jammed", jammed);
        out.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = in.getIntOr("progress", 0);
        currentPhase = in.getIntOr("phase", 0);
        determinedOutputIndex = in.getIntOr("outputIndex", -1);
        hadPowerFailure = in.getBooleanOr("powerFailure", false);
        jammed = in.getBooleanOr("jammed", false);
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

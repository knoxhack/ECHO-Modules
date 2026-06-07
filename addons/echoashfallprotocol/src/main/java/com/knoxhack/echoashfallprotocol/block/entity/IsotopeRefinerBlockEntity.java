package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.IsotopeRefinerBlock;
import com.knoxhack.echoashfallprotocol.block.menu.IsotopeRefinerMenu;
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

import java.util.Map;

/**
 * Isotope Refiner Block Entity - Tier 2 extraction.
 * Converts Tier 1 outputs + crystal dust into gold, redstone, and lapis.
 */
public class IsotopeRefinerBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {
    public static final int INPUT_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int OUTPUT_SLOT_1 = 2;
    public static final int OUTPUT_SLOT_2 = 3;
    public static final int BATTERY_SLOT = 4;
    private static final int PROCESS_TIME = 160;
    private static final int POWER_PER_OP = 500;
    private static final int ENERGY_CAPACITY = 4_000;
    private static final int ENERGY_TRANSFER = 256;
    private static final float CONTAMINATION_CHANCE = 0.20f;

    private final MachineInventory inventory = new MachineInventory(5, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    private int progress = 0;
    private int maxProgress = PROCESS_TIME;
    private int contaminationLevel = 0;
    private int wearPercent = 0;
    private boolean jammed = false;

    private static final Map<Item, Item[]> REFINER_RECIPES = Map.of(
            Items.IRON_INGOT, new Item[]{Items.GOLD_INGOT, ModItems.CONTAMINATED_GOLD.get()},
            Items.COPPER_INGOT, new Item[]{Items.REDSTONE, ModItems.CONTAMINATED_REDSTONE.get()},
            Items.COAL, new Item[]{Items.LAPIS_LAZULI, ModItems.CONTAMINATED_LAPIS.get()}
    );

    public static Map<Item, Item[]> getRefinerRecipes() {
        return REFINER_RECIPES;
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> contaminationLevel;
                case 3 -> wearPercent;
                case 4 -> jammed ? 1 : 0;
                case 5 -> energyStorage.getEnergyStored();
                case 6 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> maxProgress = v;
                case 2 -> contaminationLevel = v;
                case 3 -> wearPercent = v;
                case 4 -> jammed = v != 0;
                case 5 -> energyStorage.setEnergyStored(v);
                case 6 -> {
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public IsotopeRefinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ISOTOPE_REFINER.get(), pos, state);
    }

    public MachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Isotope Refiner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IsotopeRefinerMenu(id, inv, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IsotopeRefinerBlockEntity entity) {
        boolean wasActive = state.getValue(IsotopeRefinerBlock.ACTIVE);
        boolean isProcessing = false;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        MachineWearData wearData = new MachineWearData(level);

        entity.maxProgress = MachineGameplayHelper.getAdjustedProcessTime(level, pos, PROCESS_TIME);
        entity.wearPercent = (int) (wearData.getWearPercent(pos) * 100);
        entity.jammed = wearData.isJammed(pos);

        boolean hasPower = !entity.jammed && EnergyAccess.tryConsumeLocalOrNetworkPower(
                entity,
                level,
                pos,
                MachineGameplayHelper.getAdjustedPowerCost(level, pos, POWER_PER_OP / Math.max(1, entity.maxProgress))
        );

        if (entity.hasRecipe() && hasPower && !entity.jammed) {
            isProcessing = true;
            entity.progress++;
            entity.contaminationLevel = (int) (entity.progress * 100f / entity.maxProgress);
            entity.setChanged();

            if (entity.progress % 20 == 0) {
                wearData.addWear(pos, 1, level.getRandom());
                if (wearData.checkJamChance(pos, level.getRandom())) {
                    entity.jammed = true;
                    level.players().forEach(player -> {
                        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                            player.sendSystemMessage(Component.literal(
                                    "§c[ECHO-7]§r Isotope Refiner has jammed! Calibration required."));
                        }
                    });
                }
            }

            if (entity.progress >= entity.maxProgress) {
                entity.craftItem(level);
                entity.progress = 0;
                entity.contaminationLevel = 0;
            }
        } else if (!hasPower || entity.jammed) {
            entity.progress = 0;
        }

        if (wasActive != isProcessing) {
            level.setBlockAndUpdate(pos, state.setValue(IsotopeRefinerBlock.ACTIVE, isProcessing));
        }
    }

    public boolean hasRecipe() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack catalyst = inventory.getStackInSlot(CATALYST_SLOT);
        if (input.isEmpty() || !catalyst.is(ModItems.CRYSTAL_DUST.get())) {
            return false;
        }

        Item[] outputs = REFINER_RECIPES.get(input.getItem());
        if (outputs == null) {
            return false;
        }

        ItemStack out1 = inventory.getStackInSlot(OUTPUT_SLOT_1);
        ItemStack out2 = inventory.getStackInSlot(OUTPUT_SLOT_2);
        return (out1.isEmpty() || out1.getCount() < out1.getMaxStackSize()) &&
                (out2.isEmpty() || out2.getCount() < out2.getMaxStackSize());
    }

    private void craftItem(Level level) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack catalyst = inventory.getStackInSlot(CATALYST_SLOT);
        Item[] outputs = REFINER_RECIPES.get(input.getItem());
        if (outputs == null) {
            return;
        }

        input.shrink(2);
        catalyst.shrink(1);

        boolean contaminated = level.getRandom().nextFloat() < CONTAMINATION_CHANCE;
        Item outputItem = contaminated ? outputs[1] : outputs[0];
        int targetSlot = contaminated ? OUTPUT_SLOT_2 : OUTPUT_SLOT_1;

        ItemStack out = inventory.getStackInSlot(targetSlot);
        if (out.isEmpty()) {
            inventory.setStackInSlot(targetSlot, new ItemStack(outputItem, 1));
        } else {
            out.grow(1);
        }
        AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, worldPosition, this, new ItemStack(outputItem, 1));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inventory.serialize(out.child("inventory"));
        out.putInt("progress", progress);
        out.putInt("contamination", contaminationLevel);
        out.putBoolean("jammed", jammed);
        out.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = in.getIntOr("progress", 0);
        contaminationLevel = in.getIntOr("contamination", 0);
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

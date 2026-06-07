package com.knoxhack.echoorbitalremnants.block.entity;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.menu.OrbitalMachineMenu;
import com.knoxhack.echoorbitalremnants.recipe.OrbitalProcessingRecipe;
import com.knoxhack.echoorbitalremnants.registry.ModBlockEntities;
import com.knoxhack.echoorbitalremnants.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class OrbitalMachineBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_CHARGE = 2;
    public static final int DATA_MAX_CHARGE = 3;
    public static final int DATA_KIND = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_COUNT = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;
    private int maxProgress;
    private int charge;
    private MachineStatus status = MachineStatus.IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> maxProgress;
                case DATA_CHARGE -> charge;
                case DATA_MAX_CHARGE -> Config.MACHINE_MAX_CHARGE.get();
                case DATA_KIND -> kind().ordinal();
                case DATA_STATUS -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX_PROGRESS -> maxProgress = value;
                case DATA_CHARGE -> charge = value;
                case DATA_STATUS -> status = MachineStatus.byId(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OrbitalMachineBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.ORBITAL_MACHINE.get(), worldPosition, blockState);
        this.charge = Config.MACHINE_MAX_CHARGE.get() / 2;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OrbitalMachineBlockEntity machine) {
        if (level.isClientSide()) {
            return;
        }

        machine.regenerateCharge();
        MachineKind kind = machine.kind();
        if (!kind.processingRecipeDriven()) {
            machine.progress = 0;
            machine.maxProgress = 0;
            machine.status = kind == MachineKind.NAVIGATION_CONSOLE ? MachineStatus.DIAGNOSTIC : MachineStatus.IDLE;
            machine.setChanged();
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack input = machine.items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            machine.resetProcessing(MachineStatus.IDLE);
            return;
        }

        RecipeHolder<OrbitalProcessingRecipe> holder = findRecipe(serverLevel, kind, input);
        if (holder == null) {
            machine.resetProcessing(MachineStatus.BAD_INPUT);
            return;
        }

        OrbitalProcessingRecipe recipe = holder.value();
        ItemStack output = recipe.result().copy();
        if (!machine.canOutput(output)) {
            machine.resetProcessing(MachineStatus.OUTPUT_BLOCKED);
            return;
        }

        int chargeCost = Math.max(1, recipe.chargeCost());
        if (machine.charge < chargeCost) {
            machine.maxProgress = Config.tunedMachineDuration(recipe.duration());
            machine.status = MachineStatus.CHARGING;
            machine.setChanged();
            return;
        }

        machine.maxProgress = Config.tunedMachineDuration(recipe.duration());
        machine.status = MachineStatus.PROCESSING;
        machine.progress++;
        if (machine.progress >= machine.maxProgress) {
            machine.completeRecipe(recipe);
        }
        machine.setChanged();
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<OrbitalProcessingRecipe> findRecipe(ServerLevel level, MachineKind kind, ItemStack input) {
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == ModRecipes.ORBITAL_PROCESSING_TYPE.get())
                .map(holder -> (RecipeHolder<OrbitalProcessingRecipe>) holder)
                .filter(holder -> holder.value().matches(kind, input, level))
                .findFirst()
                .orElse(null);
    }

    public ContainerData data() {
        return data;
    }

    public MachineKind kind() {
        return getBlockState().getBlock() instanceof OrbitalMachineBlock machineBlock
                ? machineBlock.kind()
                : MachineKind.OXYGEN_COMPRESSOR;
    }

    public MachineStatus status() {
        return status;
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("ECHO-7 " + kind().displayName());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < Math.min(this.items.size(), items.size()); i++) {
            this.items.set(i, items.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new OrbitalMachineMenu(containerId, inventory, this, data);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        if (kind() == MachineKind.ROCKET_ASSEMBLY_FRAME) {
            items.set(OUTPUT_SLOT, ItemStack.EMPTY);
        }
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("max_progress", 0);
        charge = input.getIntOr("charge", Config.MACHINE_MAX_CHARGE.get() / 2);
        status = MachineStatus.byId(input.getIntOr("status", MachineStatus.IDLE.ordinal()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (kind() == MachineKind.ROCKET_ASSEMBLY_FRAME) {
            NonNullList<ItemStack> storedItems = NonNullList.withSize(items.size(), ItemStack.EMPTY);
            for (int i = 0; i < items.size(); i++) {
                if (i != OUTPUT_SLOT) {
                    storedItems.set(i, items.get(i));
                }
            }
            ContainerHelper.saveAllItems(output, storedItems);
        } else {
            ContainerHelper.saveAllItems(output, items);
        }
        output.putInt("progress", progress);
        output.putInt("max_progress", maxProgress);
        output.putInt("charge", charge);
        output.putInt("status", status.ordinal());
    }

    public void dropStoredContents(Level level, BlockPos pos) {
        dropStoredContents(level, pos, kind());
    }

    private void dropStoredContents(Level level, BlockPos pos, MachineKind removalKind) {
        if (level.isClientSide()) {
            return;
        }
        for (int slot = 0; slot < items.size(); slot++) {
            if (removalKind == MachineKind.ROCKET_ASSEMBLY_FRAME && slot == OUTPUT_SLOT) {
                items.set(slot, ItemStack.EMPTY);
                continue;
            }
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
                items.set(slot, ItemStack.EMPTY);
            }
        }
        progress = 0;
        maxProgress = 0;
        status = MachineStatus.IDLE;
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            MachineKind removalKind = state.getBlock() instanceof OrbitalMachineBlock machineBlock
                    ? machineBlock.kind()
                    : kind();
            dropStoredContents(level, pos, removalKind);
        }
    }

    private void regenerateCharge() {
        int maxCharge = Config.MACHINE_MAX_CHARGE.get();
        if (charge < maxCharge && level != null && level.getGameTime() % Config.tunedMachineChargeRegenTicks() == 0L) {
            charge++;
        }
    }

    private void resetProcessing(MachineStatus newStatus) {
        if (progress != 0 || maxProgress != 0 || status != newStatus) {
            progress = 0;
            maxProgress = 0;
            status = newStatus;
            setChanged();
        }
    }

    private boolean canOutput(ItemStack output) {
        ItemStack current = items.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, output)
                && current.getCount() + output.getCount() <= Math.min(current.getMaxStackSize(), getMaxStackSize(current));
    }

    private void completeRecipe(OrbitalProcessingRecipe recipe) {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = recipe.result().copy();
        input.shrink(1);
        ItemStack currentOutput = items.get(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            items.set(OUTPUT_SLOT, output);
        } else {
            currentOutput.grow(output.getCount());
        }
        charge = Math.max(0, charge - Math.max(1, recipe.chargeCost()));
        progress = 0;
        status = MachineStatus.COMPLETE;
    }

    public enum MachineStatus {
        IDLE("Idle"),
        PROCESSING("Processing"),
        CHARGING("Charging"),
        OUTPUT_BLOCKED("Output blocked"),
        BAD_INPUT("Input rejected"),
        COMPLETE("Complete"),
        DIAGNOSTIC("Diagnostics online");

        private static final MachineStatus[] BY_ID = values();
        private final String label;

        MachineStatus(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static MachineStatus byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : IDLE;
        }
    }
}

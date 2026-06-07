package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class OreGrinderMenu extends AbstractContainerMenu {
    private final OreGrinderBlockEntity blockEntity;
    private final ContainerData data;

    public OreGrinderMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory,
                (OreGrinderBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(7));
    }

    public OreGrinderMenu(int id, Inventory playerInventory, OreGrinderBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ORE_GRINDER.get(), id);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        var inv = entity.getInventory();
        // Input slots
        this.addSlot(new Slot(inv, OreGrinderBlockEntity.INPUT_SLOT_1, 62, 88) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OreGrinderBlockEntity.hasSubstrateRecipe(stack);
            }
        });
        this.addSlot(new Slot(inv, OreGrinderBlockEntity.INPUT_SLOT_2, 88, 88) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OreGrinderBlockEntity.hasSubstrateRecipe(stack);
            }
        });
        // Output slot
        this.addSlot(new Slot(inv, OreGrinderBlockEntity.OUTPUT_SLOT, 244, 82));
        // Byproduct slot
        this.addSlot(new Slot(inv, OreGrinderBlockEntity.BYPRODUCT_SLOT, 244, 118));
        this.addSlot(new BatterySlot(inv, OreGrinderBlockEntity.BATTERY_SLOT, 306, 100));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean hasPower() { return data.get(2) != 0; }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max == 0 ? 0 : (float) getProgress() / max;
    }

    // Wear status getters
    public int getWearPercent() { return data.get(3); }
    public boolean isJammed() { return data.get(4) != 0; }
    public int getEnergy() { return data.get(5); }
    public int getMaxEnergy() { return data.get(6); }

    public OreGrinderBlockEntity.GrinderRecipe getPreviewRecipe() {
        for (int slotIndex : new int[]{OreGrinderBlockEntity.INPUT_SLOT_1, OreGrinderBlockEntity.INPUT_SLOT_2}) {
            Slot slot = this.slots.get(slotIndex);
            OreGrinderBlockEntity.GrinderRecipe recipe =
                    OreGrinderBlockEntity.getSubstrateRecipe(slot.getItem());
            if (recipe != null) {
                return recipe;
            }
        }
        return null;
    }

    public ItemStack getPreviewInput() {
        for (int slotIndex : new int[]{OreGrinderBlockEntity.INPUT_SLOT_1, OreGrinderBlockEntity.INPUT_SLOT_2}) {
            Slot slot = this.slots.get(slotIndex);
            if (OreGrinderBlockEntity.getSubstrateRecipe(slot.getItem()) != null) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean hasRecipeReady() {
        ItemStack input = getPreviewInput();
        OreGrinderBlockEntity.GrinderRecipe recipe = OreGrinderBlockEntity.getSubstrateRecipe(input);
        return recipe != null && recipe.matches(input);
    }

    public String getWearStatus() {
        int wp = getWearPercent();
        if (wp < 20) return "Good";
        if (wp < 40) return "Worn";
        if (wp < 60) return "Degraded";
        if (wp < 80) return "Critical";
        return "Failing";
    }

    public int getWearColor() {
        int wp = getWearPercent();
        if (wp < 20) return 0x55FF55; // Green
        if (wp < 40) return 0xFFFF55; // Yellow
        if (wp < 60) return 0xFFAA00; // Orange
        if (wp < 80) return 0xFF5555; // Red
        return 0xFF00FF; // Magenta
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < 5) {
                if (!this.moveItemStackTo(current, 5, 41, true)) return ItemStack.EMPTY;
            } else {
                if (EnergyAccess.isEnergyItem(current) && this.moveItemStackTo(current, 4, 5, false)) {
                    if (current.isEmpty()) slot.set(ItemStack.EMPTY);
                    else slot.setChanged();
                    return original;
                }
                if (!OreGrinderBlockEntity.hasSubstrateRecipe(current)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(current, 0, 2, false)) return ItemStack.EMPTY;
            }
            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++)
            for (int l = 0; l < 9; l++)
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9,
                        MachineMenuLayout.PLAYER_INV_X + l * 18, MachineMenuLayout.PLAYER_INV_Y + i * 18));
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(playerInventory, i,
                    MachineMenuLayout.PLAYER_INV_X + i * 18, MachineMenuLayout.HOTBAR_Y));
    }
}

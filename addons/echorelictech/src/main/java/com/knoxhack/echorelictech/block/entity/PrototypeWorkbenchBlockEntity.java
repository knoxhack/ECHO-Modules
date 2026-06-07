package com.knoxhack.echorelictech.block.entity;

import com.knoxhack.echorelictech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PrototypeWorkbenchBlockEntity extends BlockEntity implements Container {
    public static final int RELIC_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private String lastAction = "";

    public PrototypeWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROTOTYPE_WORKBENCH.get(), pos, state);
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) { if (!stack.isEmpty()) return false; }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() { items.clear(); setChanged(); }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        lastAction = input.getStringOr("last_action", "");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("last_action", lastAction);
    }

    public ItemStack getRelicSlot() { return items.get(RELIC_SLOT); }
    public ItemStack getMaterialSlot() { return items.get(MATERIAL_SLOT); }
    public void setRelicSlot(ItemStack stack) { items.set(RELIC_SLOT, stack); setChanged(); }
    public void setMaterialSlot(ItemStack stack) { items.set(MATERIAL_SLOT, stack); setChanged(); }
    public String lastAction() { return lastAction; }
    public void setLastAction(String action) { this.lastAction = action; setChanged(); }
}

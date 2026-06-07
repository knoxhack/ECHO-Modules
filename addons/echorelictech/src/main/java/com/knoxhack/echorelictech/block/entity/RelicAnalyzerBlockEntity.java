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

public class RelicAnalyzerBlockEntity extends BlockEntity implements Container {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress = 0;
    private static final int PROGRESS_MAX = 40;

    public RelicAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELIC_ANALYZER.get(), pos, state);
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
        progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
    }

    public ItemStack getInput() { return items.get(INPUT_SLOT); }
    public ItemStack getOutput() { return items.get(OUTPUT_SLOT); }
    public void setInput(ItemStack stack) { items.set(INPUT_SLOT, stack); setChanged(); }
    public void setOutput(ItemStack stack) { items.set(OUTPUT_SLOT, stack); setChanged(); }
    public boolean hasOutput() { return !items.get(OUTPUT_SLOT).isEmpty(); }
    public ItemStack takeOutput() {
        ItemStack out = items.get(OUTPUT_SLOT);
        items.set(OUTPUT_SLOT, ItemStack.EMPTY);
        setChanged();
        return out;
    }
    public int progress() { return progress; }
    public int progressMax() { return PROGRESS_MAX; }
    public void setProgress(int p) { this.progress = p; setChanged(); }
}

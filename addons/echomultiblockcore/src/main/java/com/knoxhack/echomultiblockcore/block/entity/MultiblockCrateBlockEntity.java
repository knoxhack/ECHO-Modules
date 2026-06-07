package com.knoxhack.echomultiblockcore.block.entity;

import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echomultiblockcore.menu.MultiblockCrateMenu;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import java.util.function.Predicate;

public class MultiblockCrateBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_COUNT = 18;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public MultiblockCrateBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CRATE.get(), pos, blockState);
    }

    protected MultiblockCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public MultiblockCrateBlock.CrateKind kind() {
        return getBlockState().getBlock() instanceof MultiblockCrateBlock crate ? crate.kind() : MultiblockCrateBlock.CrateKind.INPUT;
    }

    public int insertStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int before = stack.getCount();
        for (int slot = 0; slot < items.size() && !stack.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                ItemStack moved = stack.copy();
                moved.setCount(Math.min(stack.getCount(), moved.getMaxStackSize()));
                items.set(slot, moved);
                stack.shrink(moved.getCount());
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            }
        }
        int inserted = before - stack.getCount();
        if (inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    public boolean canInsert(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        int remaining = stack.getCount();
        for (ItemStack existing : items) {
            if (existing.isEmpty()) {
                remaining -= Math.min(remaining, stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                remaining -= Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public ItemStack extractFirst() {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                items.set(slot, ItemStack.EMPTY);
                setChanged();
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean consume(Item item, int count) {
        if (item == null || count <= 0 || countItem(item) < count) {
            return false;
        }
        int remaining = count;
        for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.is(item)) {
                int moved = Math.min(remaining, stack.getCount());
                stack.shrink(moved);
                remaining -= moved;
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
        return true;
    }

    public boolean consumeMatching(Predicate<ItemStack> matcher, int count) {
        if (matcher == null || count <= 0 || countMatching(matcher) < count) {
            return false;
        }
        int remaining = count;
        for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && matcher.test(stack)) {
                int moved = Math.min(remaining, stack.getCount());
                stack.shrink(moved);
                remaining -= moved;
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
        return true;
    }

    public int countItem(Item item) {
        int count = 0;
        for (ItemStack stack : items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public int countMatching(Predicate<ItemStack> matcher) {
        if (matcher == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && matcher.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public String statusLine() {
        int occupied = 0;
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                occupied++;
                total += stack.getCount();
            }
        }
        return "ECHO " + kind().label().toUpperCase(java.util.Locale.ROOT) + " CRATE // " + occupied
                + " occupied slot(s), " + total + " item(s).";
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("ECHO " + kind().label() + " Crate");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> replacement) {
        for (int i = 0; i < Math.min(items.size(), replacement.size()); i++) {
            items.set(i, replacement.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[items.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MultiblockCrateMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}

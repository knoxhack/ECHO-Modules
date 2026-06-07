package com.knoxhack.echorelictech.block.entity;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.registry.ModBlockEntities;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ContainmentLockerBlockEntity extends BlockEntity implements Container {
    public static final int SLOTS = 5;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public ContainmentLockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_LOCKER.get(), pos, state);
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
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() { items.clear(); setChanged(); }

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

    public boolean addRelic(ItemStack stack, Player player) {
        if (!RelicTechApi.isRelic(stack)) return false;
        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data != null) {
            stack.set(ModDataComponents.RELIC_DATA.get(), new RelicInstanceData(
                    data.relicId(), data.condition(), data.instabilityModifier(), data.boundPos(), data.boundDimension(),
                    data.charge(), data.corruptionFlag(), data.overclockFlag(), true, data.identified(), data.cooldownRemaining()));
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.split(1));
                setChanged();
                if (!getLevel().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    RelicTechEvents.fireContain(serverPlayer, stack);
                }
                return true;
            }
        }
        return false;
    }

    public ItemStack removeRelic(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data != null) {
            stack.set(ModDataComponents.RELIC_DATA.get(), new RelicInstanceData(
                    data.relicId(), data.condition(), data.instabilityModifier(), data.boundPos(), data.boundDimension(),
                    data.charge(), data.corruptionFlag(), data.overclockFlag(), false, data.identified(), data.cooldownRemaining()));
        }
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    public NonNullList<ItemStack> getContents() {
        return NonNullList.copyOf(items);
    }
}

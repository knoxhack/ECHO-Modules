package com.knoxhack.echorelictech.block.entity;

import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModBlockEntities;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NullBatteryDockBlockEntity extends BlockEntity implements Container {
    public static final int BATTERY_SLOT = 0;
    public static final int CELL_SLOT = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int powerGridChargeTick = 0;

    public NullBatteryDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NULL_BATTERY_DOCK.get(), pos, state);
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
        powerGridChargeTick = input.getIntOr("power_grid_charge_tick", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("power_grid_charge_tick", powerGridChargeTick);
    }

    public ItemStack getBattery() { return items.get(BATTERY_SLOT); }
    public ItemStack getCell() { return items.get(CELL_SLOT); }

    public boolean insertBattery(ItemStack stack) {
        if (!stack.isEmpty() && items.get(BATTERY_SLOT).isEmpty()) {
            items.set(BATTERY_SLOT, stack.split(1));
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack removeBattery() {
        ItemStack stack = items.get(BATTERY_SLOT);
        items.set(BATTERY_SLOT, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    public boolean insertCell(ItemStack stack) {
        if (!stack.isEmpty() && items.get(CELL_SLOT).isEmpty()) {
            items.set(CELL_SLOT, stack.split(1));
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack removeCell() {
        ItemStack stack = items.get(CELL_SLOT);
        items.set(CELL_SLOT, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NullBatteryDockBlockEntity be) {
        if (level.isClientSide()) return;
        ItemStack battery = be.items.get(BATTERY_SLOT);
        if (battery.isEmpty()) return;
        int charge = battery.getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
        int max = RelicTechConfig.NULL_BATTERY_MAX_CHARGE.get();
        if (charge >= max) return;

        // Null Cell charging fallback
        ItemStack cell = be.items.get(CELL_SLOT);
        if (!cell.isEmpty()) {
            int gain = Math.min(max - charge, 2);
            battery.set(ModDataComponents.NULL_CHARGE.get(), charge + gain);
            cell.shrink(1);
            if (cell.isEmpty()) be.items.set(CELL_SLOT, ItemStack.EMPTY);
            be.setChanged();
            return;
        }

        // PowerGrid-powered charging
        be.powerGridChargeTick++;
        if (be.powerGridChargeTick >= 20) {
            be.powerGridChargeTick = 0;
            boolean powered = false;
            try {
                Class<?> apiClass = Class.forName("com.knoxhack.echopowergrid.api.EchoPowerGridApi");
                java.lang.reflect.Method requestPower = apiClass.getMethod("requestPower", Level.class, BlockPos.class, long.class);
                Object result = requestPower.invoke(null, level, pos, 1L);
                if (result instanceof Boolean b && b) {
                    powered = true;
                }
            } catch (Exception | LinkageError ignored) {}
            if (powered) {
                battery.set(ModDataComponents.NULL_CHARGE.get(), Math.min(max, charge + 1));
                be.setChanged();
            }
        }
    }
}

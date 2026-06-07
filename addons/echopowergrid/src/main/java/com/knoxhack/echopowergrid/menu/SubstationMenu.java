package com.knoxhack.echopowergrid.menu;

import com.knoxhack.echopowergrid.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SubstationMenu extends AbstractContainerMenu {
    public static final int DATA_GENERATION = 0;
    public static final int DATA_DEMAND = 1;
    public static final int DATA_STORED = 2;
    public static final int DATA_CAPACITY = 3;
    public static final int DATA_STATE = 4;
    public static final int DATA_NODE_COUNT = 5;
    public static final int DATA_POLICY = 6;
    public static final int DATA_COUNT = 7;

    private final ContainerData data;

    public SubstationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    public SubstationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    public SubstationMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenus.SUBSTATION.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        addDataSlots(data);
    }

    public long getGeneration() { return Integer.toUnsignedLong(data.get(DATA_GENERATION)); }
    public long getDemand() { return Integer.toUnsignedLong(data.get(DATA_DEMAND)); }
    public long getStored() { return Integer.toUnsignedLong(data.get(DATA_STORED)); }
    public long getCapacity() { return Integer.toUnsignedLong(data.get(DATA_CAPACITY)); }
    public int getState() { return data.get(DATA_STATE); }
    public int getNodeCount() { return data.get(DATA_NODE_COUNT); }
    public int getPolicy() { return data.get(DATA_POLICY); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

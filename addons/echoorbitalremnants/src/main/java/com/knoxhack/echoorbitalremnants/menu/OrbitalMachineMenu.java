package com.knoxhack.echoorbitalremnants.menu;

import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.registry.ModMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OrbitalMachineMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 352;
    public static final int GUI_HEIGHT = 332;
    public static final int INPUT_X = 72;
    public static final int OUTPUT_X = 246;
    public static final int MACHINE_SLOT_Y = 94;
    public static final int PLAYER_INV_X = 95;
    public static final int PLAYER_INV_Y = 226;

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container container;
    private final ContainerData data;
    private final Inventory playerInventory;

    public OrbitalMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.ORBITAL_MACHINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, OrbitalMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.playerInventory = playerInventory;

        this.addSlot(new Slot(container, OrbitalMachineBlockEntity.INPUT_SLOT, INPUT_X, MACHINE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return machineKind() != MachineKind.ROCKET_ASSEMBLY_FRAME
                        && machineKind() != MachineKind.NAVIGATION_CONSOLE
                        && machineKind() != MachineKind.STATION_LIFE_SUPPORT_CORE;
            }
        });
        this.addSlot(new AssemblyAwareOutputSlot(container, OrbitalMachineBlockEntity.OUTPUT_SLOT, OUTPUT_X, MACHINE_SLOT_Y));
        this.addStandardInventorySlots(playerInventory, PLAYER_INV_X, PLAYER_INV_Y);
        this.addDataSlots(data);
        refreshAssemblyOutput();
    }

    public static OrbitalMachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof OrbitalMachineBlockEntity machine) {
            return new OrbitalMachineMenu(containerId, inventory, machine, machine.data());
        }
        return new OrbitalMachineMenu(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(OrbitalMachineBlockEntity.DATA_COUNT));
    }

    @Override
    public void broadcastChanges() {
        refreshAssemblyOutput();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (slotIndex == OrbitalMachineBlockEntity.OUTPUT_SLOT) {
                if (!slot.mayPickup(player)) {
                    return ItemStack.EMPTY;
                }
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, stack);
            } else if (slotIndex == OrbitalMachineBlockEntity.INPUT_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, OrbitalMachineBlockEntity.INPUT_SLOT, OrbitalMachineBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public MachineKind machineKind() {
        int id = data.get(OrbitalMachineBlockEntity.DATA_KIND);
        MachineKind[] values = MachineKind.values();
        return id >= 0 && id < values.length ? values[id] : MachineKind.OXYGEN_COMPRESSOR;
    }

    public int progress() {
        return data.get(OrbitalMachineBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return data.get(OrbitalMachineBlockEntity.DATA_MAX_PROGRESS);
    }

    public int charge() {
        return data.get(OrbitalMachineBlockEntity.DATA_CHARGE);
    }

    public int maxCharge() {
        return data.get(OrbitalMachineBlockEntity.DATA_MAX_CHARGE);
    }

    public int statusId() {
        return data.get(OrbitalMachineBlockEntity.DATA_STATUS);
    }

    public LaunchReadiness assemblyReadiness() {
        return LaunchReadiness.evaluateForAssembly(playerInventory.player);
    }

    private void refreshAssemblyOutput() {
        if (machineKind() != MachineKind.ROCKET_ASSEMBLY_FRAME) {
            return;
        }
        ItemStack output = container.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT);
        boolean ready = assemblyReadiness().ready();
        if (ready && output.isEmpty()) {
            container.setItem(OrbitalMachineBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        } else if (!ready && !output.isEmpty()) {
            container.setItem(OrbitalMachineBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private class AssemblyAwareOutputSlot extends Slot {
        AssemblyAwareOutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return machineKind() != MachineKind.ROCKET_ASSEMBLY_FRAME || assemblyReadiness().ready();
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            if (machineKind() == MachineKind.ROCKET_ASSEMBLY_FRAME && !player.hasInfiniteMaterials()) {
                consumeAssemblyParts(player);
            }
            super.onTake(player, carried);
        }
    }

    private static void consumeAssemblyParts(Player player) {
        List<Item> required = List.of(
                ModItems.ROCKET_NOSE_CONE.get(),
                ModItems.FUEL_TANK.get(),
                ModItems.SALVAGED_ENGINE.get(),
                ModItems.LANDING_GEAR.get(),
                ModItems.ECHO_FLIGHT_CORE.get(),
                ModItems.NAVIGATION_COMPUTER.get());
        required.forEach(item -> consumeOne(player, item));
    }

    private static void consumeOne(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return;
            }
        }
    }
}

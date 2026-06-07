package com.knoxhack.echonexusprotocol.menu;

import com.knoxhack.echonexusprotocol.block.NexusMachineBlock.MachineKind;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import com.knoxhack.echonexusprotocol.registry.ModMenus;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NexusMachineMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352, GUI_HEIGHT = 332, INPUT_X = 72, OUTPUT_X = 246, MACHINE_SLOT_Y = 94, PLAYER_INV_X = 95, PLAYER_INV_Y = 226;
   private static final int MACHINE_SLOT_COUNT = 2, PLAYER_INV_START = 2, PLAYER_INV_END = PLAYER_INV_START + 27, HOTBAR_END = PLAYER_INV_END + 9;
   private final Container container;
   private final ContainerData data;
   public NexusMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
      super(ModMenus.NEXUS_MACHINE.get(), containerId); checkContainerSize(container, MACHINE_SLOT_COUNT); checkContainerDataCount(data, NexusMachineBlockEntity.DATA_COUNT); this.container = container; this.data = data;
      this.addSlot(new Slot(container, NexusMachineBlockEntity.INPUT_SLOT, INPUT_X, MACHINE_SLOT_Y) { public boolean mayPlace(ItemStack stack) { return acceptsMachineInput(stack); } });
      this.addSlot(new Slot(container, NexusMachineBlockEntity.OUTPUT_SLOT, OUTPUT_X, MACHINE_SLOT_Y) { public boolean mayPlace(ItemStack stack) { return false; } });
      this.addStandardInventorySlots(inventory, PLAYER_INV_X, PLAYER_INV_Y); this.addDataSlots(data);
   }
   public static NexusMachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) { BlockPos pos = buffer.readBlockPos(); BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos); if (blockEntity instanceof NexusMachineBlockEntity machine) { return new NexusMachineMenu(containerId, inventory, machine, machine.data()); } return new NexusMachineMenu(containerId, inventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(NexusMachineBlockEntity.DATA_COUNT)); }
   public ItemStack quickMoveStack(Player player, int slotIndex) { ItemStack copy = ItemStack.EMPTY; Slot slot = this.slots.get(slotIndex); if (slot != null && slot.hasItem()) { ItemStack stack = slot.getItem(); copy = stack.copy(); if (slotIndex == NexusMachineBlockEntity.OUTPUT_SLOT) { if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY; slot.onTake(player, stack); } else if (slotIndex == NexusMachineBlockEntity.INPUT_SLOT) { if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY; } else if (!this.acceptsMachineInput(stack) || !this.moveItemStackTo(stack, NexusMachineBlockEntity.INPUT_SLOT, NexusMachineBlockEntity.INPUT_SLOT + 1, false)) { return ItemStack.EMPTY; } if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); } return copy; }
   public boolean stillValid(Player player) { return this.container.stillValid(player); }
   public MachineKind machineKind() { int id = this.data.get(NexusMachineBlockEntity.DATA_KIND); MachineKind[] values = MachineKind.values(); return id >= 0 && id < values.length ? values[id] : MachineKind.NEXUS_RECYCLER; }
   public int progress() { return this.data.get(NexusMachineBlockEntity.DATA_PROGRESS); } public int maxProgress() { return this.data.get(NexusMachineBlockEntity.DATA_MAX_PROGRESS); } public int charge() { return this.data.get(NexusMachineBlockEntity.DATA_CHARGE); } public int maxCharge() { return this.data.get(NexusMachineBlockEntity.DATA_MAX_CHARGE); } public int contamination() { return this.data.get(NexusMachineBlockEntity.DATA_CORRUPTION); } public int statusId() { return this.data.get(NexusMachineBlockEntity.DATA_STATUS); }
   public boolean acceptsMachineInput(ItemStack stack) { return this.container instanceof NexusMachineBlockEntity machine ? machine.acceptsInput(stack) : (this.machineKind().recipeDriven() || this.machineKind() == MachineKind.CORRUPTION_REACTOR) && !stack.isEmpty(); }
}

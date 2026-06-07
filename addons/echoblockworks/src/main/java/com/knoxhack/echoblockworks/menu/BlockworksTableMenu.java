package com.knoxhack.echoblockworks.menu;

import com.knoxhack.echoblockworks.block.entity.BlockworksTableBlockEntity;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksPaletteKit;
import com.knoxhack.echoblockworks.registry.ModMenus;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlockworksTableMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 304;
   public static final int GUI_HEIGHT = 220;
   public static final int INPUT_X = 35;
   public static final int OUTPUT_X = 253;
   public static final int MACHINE_SLOT_Y = 71;
   public static final int PLAYER_INV_X = 72;
   public static final int PLAYER_INV_Y = 130;
   public static final int VARIANT_BUTTON_X = 83;
   public static final int VARIANT_BUTTON_Y = 45;
   public static final int VARIANT_BUTTON_WIDTH = 122;
   public static final int VARIANT_BUTTON_HEIGHT = 14;
   public static final int PAGE_BUTTON_WIDTH = 28;
   public static final int PAGE_BUTTON_Y = 176;
   public static final int PAGE_SIZE = 8;
   public static final int BUTTON_PREVIOUS_PAGE = 8;
   public static final int BUTTON_NEXT_PAGE = 9;
   public static final int BUTTON_TOGGLE_VIEW = 10;
   public static final int BUTTON_PREVIOUS_KIT = 11;
   public static final int BUTTON_NEXT_KIT = 12;
   public static final int MODE_BUTTON_X = 83;
   public static final int MODE_BUTTON_Y = 27;
   public static final int MODE_BUTTON_WIDTH = 44;
   public static final int KIT_BUTTON_WIDTH = 18;

   private static final int SLOT_COUNT = 2;
   private static final int PLAYER_INV_START = SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Container container;
   private final ContainerData data;

   public BlockworksTableMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.BLOCKWORKS_TABLE.get(), containerId);
      checkContainerSize(container, SLOT_COUNT);
      checkContainerDataCount(data, BlockworksTableBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;

      this.addSlot(new Slot(container, BlockworksTableBlockEntity.INPUT_SLOT, INPUT_X, MACHINE_SLOT_Y) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return isBlockworks(stack);
         }
      });
      this.addSlot(new OutputSlot(container, BlockworksTableBlockEntity.OUTPUT_SLOT, OUTPUT_X, MACHINE_SLOT_Y));
      this.addStandardInventorySlots(playerInventory, PLAYER_INV_X, PLAYER_INV_Y);
      this.addDataSlots(data);
   }

   public static BlockworksTableMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof BlockworksTableBlockEntity table) {
         return new BlockworksTableMenu(containerId, inventory, table, table.data());
      }
      return new BlockworksTableMenu(containerId, inventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(BlockworksTableBlockEntity.DATA_COUNT));
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (id == BUTTON_PREVIOUS_PAGE) {
         data.set(BlockworksTableBlockEntity.DATA_PAGE, selectedPage() - 1);
         return true;
      }
      if (id == BUTTON_NEXT_PAGE) {
         data.set(BlockworksTableBlockEntity.DATA_PAGE, selectedPage() + 1);
         return true;
      }
      if (id == BUTTON_TOGGLE_VIEW) {
         data.set(BlockworksTableBlockEntity.DATA_VIEW_MODE, kitMode() ? BlockworksTableBlockEntity.VIEW_ALL : BlockworksTableBlockEntity.VIEW_KIT);
         data.set(BlockworksTableBlockEntity.DATA_SELECTED, 0);
         data.set(BlockworksTableBlockEntity.DATA_PAGE, 0);
         return true;
      }
      if (id == BUTTON_PREVIOUS_KIT || id == BUTTON_NEXT_KIT) {
         int kitCount = BlockworksCatalog.paletteKits().size();
         if (kitCount <= 0) {
            return false;
         }
         int delta = id == BUTTON_PREVIOUS_KIT ? -1 : 1;
         data.set(BlockworksTableBlockEntity.DATA_VIEW_MODE, BlockworksTableBlockEntity.VIEW_KIT);
         data.set(BlockworksTableBlockEntity.DATA_KIT, Math.floorMod(selectedKitIndex() + delta, kitCount));
         data.set(BlockworksTableBlockEntity.DATA_SELECTED, 0);
         data.set(BlockworksTableBlockEntity.DATA_PAGE, 0);
         return true;
      }
      if (id < 0 || id >= PAGE_SIZE) {
         return false;
      }
      int absolute = selectedPage() * PAGE_SIZE + id;
      if (targets().size() <= absolute) {
         return false;
      }
      data.set(BlockworksTableBlockEntity.DATA_SELECTED, absolute);
      return true;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex == BlockworksTableBlockEntity.OUTPUT_SLOT) {
            int moved = moveConvertedOutputStack(player);
            if (moved <= 0) {
               return ItemStack.EMPTY;
            }
            copy.setCount(moved);
         } else if (slotIndex == BlockworksTableBlockEntity.INPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
               return ItemStack.EMPTY;
            }
         } else if (isBlockworks(stack)) {
            if (!moveItemStackTo(stack, BlockworksTableBlockEntity.INPUT_SLOT, BlockworksTableBlockEntity.INPUT_SLOT + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else {
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

   public int selectedVariant() {
      return data.get(BlockworksTableBlockEntity.DATA_SELECTED);
   }

   public int selectedPage() {
      return data.get(BlockworksTableBlockEntity.DATA_PAGE);
   }

   public int viewMode() {
      return data.get(BlockworksTableBlockEntity.DATA_VIEW_MODE);
   }

   public boolean kitMode() {
      return viewMode() == BlockworksTableBlockEntity.VIEW_KIT;
   }

   public int selectedKitIndex() {
      int kitCount = BlockworksCatalog.paletteKits().size();
      return kitCount <= 0 ? 0 : Math.floorMod(data.get(BlockworksTableBlockEntity.DATA_KIT), kitCount);
   }

   public java.util.Optional<BlockworksPaletteKit> activeKit() {
      if (BlockworksCatalog.paletteKits().isEmpty()) {
         return java.util.Optional.empty();
      }
      return java.util.Optional.of(BlockworksCatalog.paletteKits().get(selectedKitIndex()));
   }

   public boolean kitFallbackActive() {
      if (!kitMode()) {
         return false;
      }
      java.util.Optional<BlockworksBlockInfo> input = inputInfo();
      java.util.Optional<BlockworksPaletteKit> kit = activeKit();
      return input.isPresent()
         && kit.isPresent()
         && !BlockworksCatalog.conversionTargets(input.get()).isEmpty()
         && BlockworksCatalog.conversionTargets(input.get(), kit.get()).isEmpty();
   }

   public List<BlockworksBlockInfo> visibleTargets() {
      List<BlockworksBlockInfo> targets = targets();
      if (targets.isEmpty()) {
         return List.of();
      }
      int page = Math.max(0, Math.min(selectedPage(), maxPage(targets.size())));
      int start = page * PAGE_SIZE;
      int end = Math.min(targets.size(), start + PAGE_SIZE);
      return targets.subList(start, end);
   }

   public int selectedVisibleIndex() {
      return selectedVariant() - selectedPage() * PAGE_SIZE;
   }

   public boolean hasPreviousPage() {
      return selectedPage() > 0;
   }

   public boolean hasNextPage() {
      return selectedPage() < maxPage(targets().size());
   }

   public static int maxPage(int targetCount) {
      return Math.max(0, (Math.max(0, targetCount) - 1) / PAGE_SIZE);
   }

   public static int pageFor(int selectedVariant, int targetCount) {
      if (targetCount <= 0) {
         return 0;
      }
      int clamped = Math.max(0, Math.min(selectedVariant, targetCount - 1));
      return clamped / PAGE_SIZE;
   }

   public List<BlockworksBlockInfo> targets() {
      return inputInfo()
         .map(this::targetsFor)
         .orElse(List.of());
   }

   public java.util.Optional<BlockworksBlockInfo> inputInfo() {
      return infoFor(container.getItem(BlockworksTableBlockEntity.INPUT_SLOT));
   }

   public java.util.Optional<BlockworksBlockInfo> selectedTarget() {
      List<BlockworksBlockInfo> targets = targets();
      if (targets.isEmpty()) {
         return java.util.Optional.empty();
      }
      int index = Math.max(0, Math.min(selectedVariant(), targets.size() - 1));
      return java.util.Optional.of(targets.get(index));
   }

   public static java.util.Optional<BlockworksBlockInfo> infoFor(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem blockItem)) {
         return java.util.Optional.empty();
      }
      return BlockworksCatalog.blockInfo(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath());
   }

   private static boolean isBlockworks(ItemStack stack) {
      return infoFor(stack).isPresent();
   }

   private List<BlockworksBlockInfo> targetsFor(BlockworksBlockInfo source) {
      List<BlockworksBlockInfo> allTargets = BlockworksCatalog.conversionTargets(source);
      if (!kitMode()) {
         return allTargets;
      }
      java.util.Optional<BlockworksPaletteKit> kit = activeKit();
      if (kit.isEmpty()) {
         return allTargets;
      }
      List<BlockworksBlockInfo> kitTargets = BlockworksCatalog.conversionTargets(source, kit.get());
      return kitTargets.isEmpty() ? allTargets : kitTargets;
   }

   private int moveConvertedOutputStack(Player player) {
      java.util.Optional<BlockworksBlockInfo> target = selectedTarget();
      ItemStack input = container.getItem(BlockworksTableBlockEntity.INPUT_SLOT);
      if (target.isEmpty() || input.isEmpty()) {
         return 0;
      }
      ItemStack template = new ItemStack(ModBlocks.blockFor(target.get()).get());
      int remaining = input.getCount();
      int movedTotal = 0;
      while (remaining > 0) {
         ItemStack batch = template.copy();
         batch.setCount(Math.min(remaining, batch.getMaxStackSize()));
         int before = batch.getCount();
         if (!moveItemStackTo(batch, PLAYER_INV_START, HOTBAR_END, true)) {
            break;
         }
         int moved = before - batch.getCount();
         if (moved <= 0) {
            break;
         }
         movedTotal += moved;
         remaining -= moved;
         if (!batch.isEmpty()) {
            break;
         }
      }
      if (movedTotal <= 0) {
         return 0;
      }
      if (container instanceof BlockworksTableBlockEntity table) {
         table.completeConversions(player, movedTotal);
      } else {
         input.shrink(movedTotal);
         if (input.isEmpty()) {
            container.setItem(BlockworksTableBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
         }
         container.setItem(BlockworksTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
      }
      container.setChanged();
      return movedTotal;
   }

   private final class OutputSlot extends Slot {
      private OutputSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }

      @Override
      public boolean mayPickup(Player player) {
         return !container.getItem(BlockworksTableBlockEntity.INPUT_SLOT).isEmpty() && selectedTarget().isPresent();
      }

      @Override
      public void onTake(Player player, ItemStack carried) {
         if (container instanceof BlockworksTableBlockEntity table) {
            table.completeConversion(player);
         } else {
            container.getItem(BlockworksTableBlockEntity.INPUT_SLOT).shrink(1);
            container.setItem(BlockworksTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
         }
         super.onTake(player, carried);
      }
   }
}

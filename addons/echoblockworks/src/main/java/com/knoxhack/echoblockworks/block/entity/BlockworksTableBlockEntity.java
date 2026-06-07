package com.knoxhack.echoblockworks.block.entity;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksPaletteKit;
import com.knoxhack.echoblockworks.integration.BlockworksMissionHooks;
import com.knoxhack.echoblockworks.menu.BlockworksTableMenu;
import com.knoxhack.echoblockworks.registry.ModBlockEntities;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockworksTableBlockEntity extends BaseContainerBlockEntity {
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   public static final int DATA_SELECTED = 0;
   public static final int DATA_PAGE = 1;
   public static final int DATA_VIEW_MODE = 2;
   public static final int DATA_KIT = 3;
   public static final int DATA_COUNT = 4;
   public static final int VIEW_ALL = 0;
   public static final int VIEW_KIT = 1;

   private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
   private int selectedVariant;
   private int selectedPage;
   private int selectedViewMode;
   private int selectedKitIndex;
   private String lastSelectionKey = "";

   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int id) {
         return switch (id) {
            case DATA_SELECTED -> selectedVariant;
            case DATA_PAGE -> selectedPage;
            case DATA_VIEW_MODE -> selectedViewMode;
            case DATA_KIT -> selectedKitIndex;
            default -> 0;
         };
      }

      @Override
      public void set(int id, int value) {
         if (id == DATA_SELECTED) {
            setSelectedVariant(value);
         } else if (id == DATA_PAGE) {
            setSelectedPage(value);
         } else if (id == DATA_VIEW_MODE) {
            setSelectedViewMode(value);
         } else if (id == DATA_KIT) {
            setSelectedKitIndex(value);
         }
      }

      @Override
      public int getCount() {
         return DATA_COUNT;
      }
   };

   public BlockworksTableBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.BLOCKWORKS_TABLE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BlockworksTableBlockEntity table) {
      if (!level.isClientSide()) {
         table.refreshOutput();
      }
   }

   public ContainerData data() {
      return data;
   }

   public List<BlockworksBlockInfo> targets() {
      return inputInfo()
         .map(this::targetsFor)
         .orElse(List.of());
   }

   public java.util.Optional<BlockworksBlockInfo> selectedTarget() {
      refreshSelectionForInput();
      List<BlockworksBlockInfo> targets = targets();
      if (targets.isEmpty()) {
         return java.util.Optional.empty();
      }
      int index = Math.max(0, Math.min(selectedVariant, targets.size() - 1));
      return java.util.Optional.of(targets.get(index));
   }

   public void setSelectedVariant(int selectedVariant) {
      List<BlockworksBlockInfo> targets = targets();
      int max = Math.max(0, targets.size() - 1);
      this.selectedVariant = Math.max(0, Math.min(selectedVariant, max));
      this.selectedPage = BlockworksTableMenu.pageFor(this.selectedVariant, targets.size());
      refreshOutput();
      setChanged();
   }

   public void setSelectedPage(int selectedPage) {
      int maxPage = BlockworksTableMenu.maxPage(targets().size());
      this.selectedPage = Math.max(0, Math.min(selectedPage, maxPage));
      refreshOutput();
      setChanged();
   }

   public void setSelectedViewMode(int selectedViewMode) {
      this.selectedViewMode = selectedViewMode == VIEW_KIT ? VIEW_KIT : VIEW_ALL;
      refreshSelectionForInput();
      refreshOutput();
      setChanged();
   }

   public void setSelectedKitIndex(int selectedKitIndex) {
      int kitCount = BlockworksCatalog.paletteKits().size();
      this.selectedKitIndex = kitCount <= 0 ? 0 : Math.floorMod(selectedKitIndex, kitCount);
      refreshSelectionForInput();
      refreshOutput();
      setChanged();
   }

   public boolean isBlockworksInput(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      return blockInfo(stack).isPresent();
   }

   public void completeConversion(Player player) {
      completeConversions(player, 1);
   }

   public void completeConversions(Player player, int count) {
      ItemStack input = items.get(INPUT_SLOT);
      int conversions = Math.max(0, Math.min(count, input.getCount()));
      if (input.isEmpty() || conversions <= 0 || selectedTarget().isEmpty()) {
         items.set(OUTPUT_SLOT, ItemStack.EMPTY);
         return;
      }
      String targetId = selectedTarget().map(BlockworksBlockInfo::blockId).orElse("unknown");
      input.shrink(conversions);
      if (input.isEmpty()) {
         items.set(INPUT_SLOT, ItemStack.EMPTY);
      }
      items.set(OUTPUT_SLOT, ItemStack.EMPTY);
      refreshOutput();
      BlockworksMissionHooks.recordTableUsed(player, targetId);
      BlockworksMissionHooks.recordVariantConverted(player, targetId, conversions);
      setChanged();
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      super.setItem(slot, stack);
      if (slot == INPUT_SLOT) {
         refreshSelectionForInput();
      }
      refreshOutput();
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.echoblockworks.blockworks_table");
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> items) {
      for (int i = 0; i < Math.min(this.items.size(), items.size()); i++) {
         this.items.set(i, i == OUTPUT_SLOT ? ItemStack.EMPTY : items.get(i));
      }
      refreshOutput();
   }

   @Override
   protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new BlockworksTableMenu(containerId, inventory, this, data);
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(getBlockPos());
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      items.set(OUTPUT_SLOT, ItemStack.EMPTY);
      selectedVariant = input.getIntOr("selected_variant", 0);
      selectedPage = input.getIntOr("selected_page", 0);
      selectedViewMode = input.getIntOr("selected_view_mode", VIEW_ALL);
      selectedKitIndex = input.getIntOr("selected_kit_index", 0);
      lastSelectionKey = selectionKey();
      clampSelection();
      refreshOutput();
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      NonNullList<ItemStack> storedItems = NonNullList.withSize(items.size(), ItemStack.EMPTY);
      storedItems.set(INPUT_SLOT, items.get(INPUT_SLOT));
      ContainerHelper.saveAllItems(output, storedItems);
      output.putInt("selected_variant", selectedVariant);
      output.putInt("selected_page", selectedPage);
      output.putInt("selected_view_mode", selectedViewMode);
      output.putInt("selected_kit_index", selectedKitIndex);
   }

   @Override
   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (level != null && !level.isClientSide()) {
         ItemStack input = items.get(INPUT_SLOT);
         if (!input.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, input.copy());
            items.set(INPUT_SLOT, ItemStack.EMPTY);
         }
         items.set(OUTPUT_SLOT, ItemStack.EMPTY);
      }
   }

   private java.util.Optional<BlockworksBlockInfo> inputInfo() {
      return blockInfo(items.get(INPUT_SLOT));
   }

   private static java.util.Optional<BlockworksBlockInfo> blockInfo(ItemStack stack) {
      if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
         return java.util.Optional.empty();
      }
      return BlockworksCatalog.blockInfo(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath());
   }

   private void refreshOutput() {
      refreshSelectionForInput();
      if (items.get(INPUT_SLOT).isEmpty()) {
         items.set(OUTPUT_SLOT, ItemStack.EMPTY);
         return;
      }
      java.util.Optional<BlockworksBlockInfo> target = selectedTarget();
      if (target.isEmpty()) {
         items.set(OUTPUT_SLOT, ItemStack.EMPTY);
         return;
      }
      items.set(OUTPUT_SLOT, new ItemStack(ModBlocks.blockFor(target.get()).get()));
   }

   private void refreshSelectionForInput() {
      String key = selectionKey();
      if (!key.equals(lastSelectionKey)) {
         selectedVariant = 0;
         selectedPage = 0;
         lastSelectionKey = key;
      }
      clampSelection();
   }

   private void clampSelection() {
      selectedViewMode = selectedViewMode == VIEW_KIT ? VIEW_KIT : VIEW_ALL;
      int kitCount = BlockworksCatalog.paletteKits().size();
      selectedKitIndex = kitCount <= 0 ? 0 : Math.floorMod(selectedKitIndex, kitCount);
      List<BlockworksBlockInfo> targets = targets();
      if (targets.isEmpty()) {
         selectedVariant = 0;
         selectedPage = 0;
         return;
      }
      selectedVariant = Math.max(0, Math.min(selectedVariant, targets.size() - 1));
      selectedPage = Math.max(0, Math.min(selectedPage, BlockworksTableMenu.maxPage(targets.size())));
      int pageStart = selectedPage * BlockworksTableMenu.PAGE_SIZE;
      if (selectedVariant < pageStart || selectedVariant >= pageStart + BlockworksTableMenu.PAGE_SIZE) {
         selectedPage = BlockworksTableMenu.pageFor(selectedVariant, targets.size());
      }
   }

   private List<BlockworksBlockInfo> targetsFor(BlockworksBlockInfo input) {
      List<BlockworksBlockInfo> allTargets = BlockworksCatalog.conversionTargets(input);
      if (selectedViewMode != VIEW_KIT || BlockworksCatalog.paletteKits().isEmpty()) {
         return allTargets;
      }
      BlockworksPaletteKit kit = BlockworksCatalog.paletteKits().get(Math.floorMod(selectedKitIndex, BlockworksCatalog.paletteKits().size()));
      List<BlockworksBlockInfo> kitTargets = BlockworksCatalog.conversionTargets(input, kit);
      return kitTargets.isEmpty() ? allTargets : kitTargets;
   }

   private String selectionKey() {
      return inputKey(items.get(INPUT_SLOT)) + "/" + selectedViewMode + "/" + selectedKitIndex + "/" + targets().stream()
         .map(BlockworksBlockInfo::blockId)
         .reduce("", (left, right) -> left + "," + right);
   }

   private static String inputKey(ItemStack stack) {
      return blockInfo(stack)
         .map(info -> info.family().id() + "/" + info.shape().name())
         .orElse("");
   }
}

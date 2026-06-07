package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echoindustrialnexus.block.IndustrialItemDuctBlock;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class IndustrialItemDuctBlockEntity extends BlockEntity {
   private static final int MAX_ROUTED_DUCTS = 96;
   private int transferCooldown;
   private final NonNullList<ItemStack> filter = NonNullList.withSize(1, ItemStack.EMPTY);
   private boolean blacklist;
   private BlockPos lastSource;
   private BlockPos lastTarget;
   private long lastTransferGameTime;
   private int routeCursor;

   public IndustrialItemDuctBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.ITEM_DUCT.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, IndustrialItemDuctBlockEntity ductEntity) {
      if (level.isClientSide() || !(state.getBlock() instanceof IndustrialItemDuctBlock duct)) {
         return;
      }
      if (ductEntity.transferCooldown > 0) {
         ductEntity.transferCooldown--;
         return;
      }
      if (duct.vacuum() && pullDroppedItem(level, pos, duct, ductEntity)) {
         ductEntity.transferCooldown = duct.transferInterval() - 1;
         return;
      }
      moveFromAdjacentInventory(level, pos, duct, ductEntity);
      ductEntity.transferCooldown = duct.transferInterval() - 1;
   }

   private static boolean pullDroppedItem(Level level, BlockPos pos, IndustrialItemDuctBlock duct, IndustrialItemDuctBlockEntity ductEntity) {
      for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5))) {
         ItemStack stack = entity.getItem();
         if (stack.isEmpty() || !ductEntity.allows(duct, stack)) {
            continue;
         }
         ItemStack one = stack.copyWithCount(1);
         BlockPos target = routeItem(level, pos, duct, ductEntity, one, null);
         if (target != null) {
            stack.shrink(1);
            if (stack.isEmpty()) {
               entity.discard();
            }
            ductEntity.rememberTransfer(pos, target, level.getGameTime());
            return true;
         }
      }
      return false;
   }

   private static boolean moveFromAdjacentInventory(Level level, BlockPos pos, IndustrialItemDuctBlock duct, IndustrialItemDuctBlockEntity ductEntity) {
      for (Direction sourceDirection : Direction.values()) {
         BlockPos sourcePos = pos.relative(sourceDirection);
         if (ductEntity.isImmediateBacktrack(sourcePos, level.getGameTime())) {
            continue;
         }
         if (!(level.getBlockEntity(sourcePos) instanceof Container source)) {
            continue;
         }
         Direction sourceFace = sourceDirection.getOpposite();
         for (int sourceSlot : slotsFor(source, sourceFace)) {
            ItemStack sourceStack = source.getItem(sourceSlot);
            if (sourceStack.isEmpty() || !ductEntity.allows(duct, sourceStack) || !canExtract(source, sourceSlot, sourceStack, sourceFace)) {
               continue;
            }
            ItemStack one = sourceStack.copyWithCount(1);
            BlockPos target = routeItem(level, pos, duct, ductEntity, one, sourcePos);
            if (target != null) {
               source.removeItem(sourceSlot, 1);
               source.setChanged();
               ductEntity.rememberTransfer(sourcePos, target, level.getGameTime());
               return true;
            }
         }
      }
      return false;
   }

   private static BlockPos routeItem(Level level, BlockPos ductPos, IndustrialItemDuctBlock startDuct, IndustrialItemDuctBlockEntity startEntity, ItemStack stack, BlockPos excludedPos) {
      Set<BlockPos> visited = new HashSet<>();
      Queue<BlockPos> queue = new ArrayDeque<>();
      if (!startEntity.allows(startDuct, stack) || !visited.add(ductPos)) {
         return null;
      }
      queue.add(ductPos);
      while (!queue.isEmpty() && visited.size() <= MAX_ROUTED_DUCTS) {
         BlockPos current = queue.remove();
         IndustrialItemDuctBlockEntity currentEntity = level.getBlockEntity(current) instanceof IndustrialItemDuctBlockEntity entity ? entity : startEntity;
         BlockPos target = insertIntoAnyNeighbor(level, current, stack, excludedPos, currentEntity.nextDirectionOffset());
         if (target != null) {
            currentEntity.advanceRouteCursor();
            return target;
         }
         for (Direction direction : orderedDirections(currentEntity.nextDirectionOffset())) {
            BlockPos next = current.relative(direction);
            if (next.equals(excludedPos) || !visited.add(next)) {
               continue;
            }
            if (level.getBlockState(next).getBlock() instanceof IndustrialItemDuctBlock nextDuct
               && level.getBlockEntity(next) instanceof IndustrialItemDuctBlockEntity nextEntity
               && nextEntity.allows(nextDuct, stack)) {
               queue.add(next);
            }
         }
      }
      return null;
   }

   private static BlockPos insertIntoAnyNeighbor(Level level, BlockPos ductPos, ItemStack stack, BlockPos excludedPos, int directionOffset) {
      for (Direction receiverDirection : orderedDirections(directionOffset)) {
         BlockPos receiverPos = ductPos.relative(receiverDirection);
         if (receiverPos.equals(excludedPos)) {
            continue;
         }
         if (level.getBlockState(receiverPos).getBlock() instanceof IndustrialItemDuctBlock) {
            continue;
         }
         if (level.getBlockEntity(receiverPos) instanceof Container receiver && canInsert(receiver, stack, receiverDirection.getOpposite())) {
            insert(receiver, stack, receiverDirection.getOpposite());
            receiver.setChanged();
            return receiverPos;
         }
      }
      return null;
   }

   private static Direction[] orderedDirections(int offset) {
      Direction[] values = Direction.values();
      Direction[] ordered = new Direction[values.length];
      for (int i = 0; i < values.length; i++) {
         ordered[i] = values[Math.floorMod(i + offset, values.length)];
      }
      return ordered;
   }

   private boolean allows(IndustrialItemDuctBlock duct, ItemStack stack) {
      if (!duct.nexusSafe() && IndustrialMachineBlockEntity.isNexusMaterial(stack)) {
         return false;
      }
      if (!duct.smart()) {
         return true;
      }
      ItemStack filterStack = this.filter.get(0);
      if (filterStack.isEmpty()) {
         return true;
      }
      boolean matches = ItemStack.isSameItemSameComponents(filterStack, stack);
      return this.blacklist ? !matches : matches;
   }

   public boolean installFilter(Player player, ItemStack held) {
      if (held.isEmpty()) {
         if (!this.filter.get(0).isEmpty()) {
            if (!player.addItem(this.filter.get(0).copy())) {
               player.drop(this.filter.get(0).copy(), false);
            }
            this.filter.set(0, ItemStack.EMPTY);
            this.setChanged();
            return true;
         }
         return false;
      }
      ItemStack copy = held.copyWithCount(1);
      ItemStack previous = this.filter.get(0);
      if (!previous.isEmpty() && !player.addItem(previous.copy())) {
         player.drop(previous.copy(), false);
      }
      this.filter.set(0, copy);
      if (!player.getAbilities().instabuild) {
         held.shrink(1);
      }
      this.setChanged();
      return true;
   }

   public void toggleFilterMode() {
      this.blacklist = !this.blacklist;
      this.setChanged();
   }

   public void dropFilterContents(Level level, BlockPos pos) {
      ItemStack filterStack = this.filter.get(0);
      if (!filterStack.isEmpty()) {
         ItemEntity droppedFilter = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, filterStack.copy());
         droppedFilter.setDefaultPickUpDelay();
         level.addFreshEntity(droppedFilter);
         this.filter.set(0, ItemStack.EMPTY);
         this.setChanged();
      }
   }

   public Component filterStatus(IndustrialItemDuctBlock duct) {
      String safety = duct.nexusSafe() ? "Nexus-safe" : "Nexus blocked";
      if (!duct.smart()) {
         return Component.literal("ECHO INDUSTRIAL // " + duct.displayName() + " routes every " + duct.transferInterval() + " ticks. " + safety + ".");
      }
      ItemStack filterStack = this.filter.get(0);
      String mode = this.blacklist ? "blacklist" : "whitelist";
      String filtered = filterStack.isEmpty() ? "pass all" : filterStack.getHoverName().getString();
      return Component.literal("ECHO INDUSTRIAL // " + duct.displayName() + " " + mode + " filter: " + filtered + ". " + safety + ".");
   }

   public ItemStack filterStack() {
      return this.filter.get(0).copy();
   }

   public boolean blacklistMode() {
      return this.blacklist;
   }

   public BlockPos lastSource() {
      return this.lastSource;
   }

   public BlockPos lastTarget() {
      return this.lastTarget;
   }

   public long lastTransferGameTime() {
      return this.lastTransferGameTime;
   }

   private boolean isImmediateBacktrack(BlockPos sourcePos, long gameTime) {
      return this.lastTarget != null && this.lastTarget.equals(sourcePos) && gameTime - this.lastTransferGameTime <= 2L;
   }

   private void rememberTransfer(BlockPos source, BlockPos target, long gameTime) {
      this.lastSource = source;
      this.lastTarget = target;
      this.lastTransferGameTime = gameTime;
      this.setChanged();
   }

   private int nextDirectionOffset() {
      return Math.floorMod(this.routeCursor, Direction.values().length);
   }

   private void advanceRouteCursor() {
      this.routeCursor = (this.routeCursor + 1) % Direction.values().length;
      this.setChanged();
   }

   private static int[] slotsFor(Container container, Direction face) {
      if (container instanceof WorldlyContainer sided) {
         return sided.getSlotsForFace(face);
      }
      int[] slots = new int[container.getContainerSize()];
      for (int i = 0; i < slots.length; i++) {
         slots[i] = i;
      }
      return slots;
   }

   private static boolean canExtract(Container container, int slot, ItemStack stack, Direction face) {
      return !(container instanceof WorldlyContainer sided) || sided.canTakeItemThroughFace(slot, stack, face);
   }

   private static boolean canInsert(Container container, ItemStack stack, Direction face) {
      for (int slot : slotsFor(container, face)) {
         if (canInsertSlot(container, slot, stack, face)) {
            return true;
         }
      }
      return false;
   }

   private static boolean canInsertSlot(Container container, int slot, ItemStack stack, Direction face) {
      if (container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, stack, face)) {
         return false;
      }
      if (!container.canPlaceItem(slot, stack)) {
         return false;
      }
      ItemStack existing = container.getItem(slot);
      return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize();
   }

   private static void insert(Container container, ItemStack stack, Direction face) {
      for (int slot : slotsFor(container, face)) {
         if (!canInsertSlot(container, slot, stack, face)) {
            continue;
         }
         ItemStack existing = container.getItem(slot);
         if (existing.isEmpty()) {
            container.setItem(slot, stack.copy());
         } else {
            existing.grow(stack.getCount());
         }
         return;
      }
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, this.filter);
      this.blacklist = input.getBooleanOr("blacklist", false);
      this.routeCursor = input.getIntOr("route_cursor", 0);
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.filter);
      output.putBoolean("blacklist", this.blacklist);
      output.putInt("route_cursor", this.routeCursor);
   }
}

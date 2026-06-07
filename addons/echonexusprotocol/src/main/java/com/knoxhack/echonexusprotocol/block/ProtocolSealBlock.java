package com.knoxhack.echonexusprotocol.block;

import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ProtocolSealBlock extends Block {
   public static final MapCodec<ProtocolSealBlock> CODEC = simpleCodec(ProtocolSealBlock::new);
   public static final EnumProperty<ProtocolSealBlock.SealMode> MODE = EnumProperty.create("mode", ProtocolSealBlock.SealMode.class);
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

   public ProtocolSealBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(MODE, ProtocolSealBlock.SealMode.COLLECT));
   }

   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{MODE});
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      if (!level.isClientSide()) {
         level.scheduleTick(pos, this, sealTickInterval());
      }
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      applySeal(level, pos, (ProtocolSealBlock.SealMode)state.getValue(MODE));
      level.scheduleTick(pos, this, sealTickInterval());
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide()) {
         ProtocolSealBlock.SealMode next = ((ProtocolSealBlock.SealMode)state.getValue(MODE)).next();
         level.setBlock(pos, (BlockState)state.setValue(MODE, next), 3);
         player.sendSystemMessage(Component.literal("ECHO-7 // Protocol Seal set to " + next.displayName() + "."));
         level.scheduleTick(pos, this, 5);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public static void applySeal(ServerLevel level, BlockPos pos, ProtocolSealBlock.SealMode mode) {
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(pos);
      boolean acted = false;
      switch (mode) {
         case COLLECT:
            acted = collectItems(level, pos);
            break;
         case EXTRACT:
            acted = extractCharge(level, pos, data, chunk);
            break;
         case REPAIR:
            acted = repairNearbyMachines(level, pos);
            break;
         case QUARANTINE:
            data.applySeal(chunk, mode);
            data.addCorruptionPressure(chunk, -2);
            acted = true;
            break;
         case PURIFY:
            acted = purifyNearbyBlocks(level, pos);
            if (acted || data.corruptionPressure(chunk) > 0) {
               data.applySeal(chunk, mode);
               acted = true;
            }
            break;
         case RELAY:
            acted = relayCharge(level, pos);
            break;
         case DEFENSE:
            acted = defend(level, pos);
            break;
         case REWRITE:
            acted = rewriteNearby(level, pos);
            if (acted) {
               data.applySeal(chunk, mode);
            }
            break;
         case COLLAPSE:
            acted = corruptNearby(level, pos);
            if (acted) {
               data.applySeal(chunk, mode);
               feedCollapsePower(level, pos);
            }
      }
      if (acted) {
         markNearbyPlayers(level, pos, mode);
         if (level.getGameTime() % Math.max(20, sealTickInterval() * 2L) == 0L) {
            level.playSound(null, pos, ModSounds.SEAL_ACTIVATE.get(), SoundSource.BLOCKS, 0.22F, 0.85F + mode.ordinal() * 0.04F);
         }
      }
   }

   private static boolean collectItems(ServerLevel level, BlockPos pos) {
      boolean acted = false;
      for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(sealRadius()))) {
         ItemStack remaining = insertIntoNearbyContainers(level, pos, item.getItem());
         if (remaining.getCount() != item.getItem().getCount()) {
            acted = true;
         }
         item.setItem(remaining);
         if (remaining.isEmpty()) {
            item.discard();
         } else {
            double dx = (pos.getX() + 0.5 - item.getX()) * 0.08;
            double dy = (pos.getY() + 0.2 - item.getY()) * 0.08;
            double dz = (pos.getZ() + 0.5 - item.getZ()) * 0.08;
            item.setDeltaMovement(dx, dy, dz);
         }
      }
      return acted;
   }

   private static boolean extractCharge(ServerLevel level, BlockPos pos, NexusWorldData data, ChunkPos chunk) {
      int pressure = data.corruptionPressure(chunk);
      int charge = pressure > 0 ? Math.min(120, 20 + pressure * 2) : 20;
      List<NexusMachineBlockEntity> machines = nearbyMachine(level, pos);
      boolean moved = false;
      for (NexusMachineBlockEntity machine : machines) {
         if (machine.kind() == NexusMachineBlock.MachineKind.NEXUS_CHARGE_TANK) {
            int before = machine.energyStored();
            machine.receiveCharge(charge);
            moved |= machine.energyStored() > before;
         }
      }
      if (!moved) {
         for (NexusMachineBlockEntity machine : machines) {
            int before = machine.energyStored();
            machine.receiveCharge(Math.max(10, charge / 2));
            moved |= machine.energyStored() > before;
         }
      }
      if (pressure > 0) {
         data.addCorruptionPressure(chunk, -Math.max(3, charge / 20));
         moved = true;
      }
      return moved;
   }

   private static boolean repairNearbyMachines(ServerLevel level, BlockPos pos) {
      boolean acted = false;
      int amount = Math.max(1, (Integer)Config.FILTER_CORRUPTION_REDUCTION.get() / 2);
      for (NexusMachineBlockEntity machine : nearbyMachine(level, pos)) {
         int before = machine.contamination();
         machine.reduceContamination(amount);
         acted |= machine.contamination() < before;
      }
      return acted;
   }

   private static boolean relayCharge(ServerLevel level, BlockPos pos) {
      List<NexusMachineBlockEntity> machines = nearbyMachine(level, pos);
      if (machines.size() < 2) {
         return false;
      }

      List<NexusMachineBlockEntity> ordered = new ArrayList<>(machines);
      ordered.sort((left, right) -> Double.compare(left.getBlockPos().distSqr(pos), right.getBlockPos().distSqr(pos)));

      int relayAmount = Math.max(1, (Integer)Config.SEAL_RELAY_AMOUNT.get());
      for (NexusMachineBlockEntity target : ordered) {
         if (target.energySpace() <= 0) {
            continue;
         }

         NexusMachineBlockEntity source = ordered.stream()
            .filter(machine -> machine != target)
            .filter(machine -> machine.energyStored() > target.energyStored())
            .max((left, right) -> Integer.compare(left.energyStored(), right.energyStored()))
            .orElse(null);
         if (source != null) {
            int moved = Math.min(Math.min(relayAmount, source.energyStored()), target.energySpace());
            if (moved > 0) {
               int before = target.energyStored();
               target.receiveCharge(moved);
               int accepted = target.energyStored() - before;
               if (accepted > 0 && source.consumeCharge(accepted)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean defend(ServerLevel level, BlockPos pos) {
      double radius = sealRadius();
      List<LivingEntity> nexusTargets = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius), entity -> entity instanceof NexusMobEntity);
      if (!nexusTargets.isEmpty()) {
         nexusTargets.forEach(entity -> {
            entity.invulnerableTime = 0;
            entity.hurt(level.damageSources().magic(), (Integer)Config.SEAL_DEFENSE_DAMAGE.get());
         });
         return true;
      }

      boolean acted = false;
      for (Monster monster : level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(radius))) {
         monster.hurt(level.damageSources().magic(), Math.max(1.0F, (Integer)Config.SEAL_DEFENSE_DAMAGE.get() * 0.6F));
         acted = true;
      }
      return acted;
   }

   private static boolean purifyNearbyBlocks(ServerLevel level, BlockPos pos) {
      int radius = Math.min(6, Math.max(1, (Integer)Config.SEAL_RADIUS.get() / 2));
      for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
         BlockState state = level.getBlockState(target);
         BlockState clean = ModBlocks.cleanVariant(state);
         if (clean != null) {
            level.setBlock(target, clean, 3);
            return true;
         }
      }
      return false;
   }

   private static boolean rewriteNearby(ServerLevel level, BlockPos pos) {
      for (BlockPos target : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
         if (level.getBlockState(target).is((Block)ModBlocks.DATA_CRACKED_STONE.get())) {
            if (consumeNearbyCharge(level, pos, 40)) {
               level.setBlock(target, ((Block)ModBlocks.BLACKBOX_PLATE.get()).defaultBlockState(), 3);
               return true;
            }
            return false;
         }
      }
      return false;
   }

   private static boolean corruptNearby(ServerLevel level, BlockPos pos) {
      int radius = Math.min(6, Math.max(1, (Integer)Config.SEAL_RADIUS.get() / 2));
      for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
         BlockState state = level.getBlockState(target);
         BlockState corrupted = ModBlocks.corruptedVariant(state);
         if (corrupted != null) {
            level.setBlock(target, corrupted, 3);
            return true;
         }
      }
      return false;
   }

   private static void feedCollapsePower(ServerLevel level, BlockPos pos) {
      for (NexusMachineBlockEntity machine : nearbyMachine(level, pos)) {
         machine.receiveCharge(40);
      }
   }

   private static List<NexusMachineBlockEntity> nearbyMachine(ServerLevel level, BlockPos pos) {
      List<NexusMachineBlockEntity> machines = new ArrayList<>();

      int radius = Math.max(1, (Integer)Config.SEAL_RADIUS.get());
      for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -2, -radius), pos.offset(radius, 2, radius))) {
         if (level.getBlockEntity(target) instanceof NexusMachineBlockEntity machine) {
            machines.add(machine);
         }
      }

      return List.copyOf(machines);
   }

   private static boolean consumeNearbyCharge(ServerLevel level, BlockPos pos, int amount) {
      for (NexusMachineBlockEntity machine : nearbyMachine(level, pos)) {
         if (machine.consumeCharge(amount)) {
            return true;
         }
      }
      return false;
   }

   private static ItemStack insertIntoNearbyContainers(ServerLevel level, BlockPos pos, ItemStack stack) {
      ItemStack remaining = stack.copy();
      remaining = insertIntoNearbyContainers(level, pos, remaining, false);
      if (remaining.isEmpty()) {
         return ItemStack.EMPTY;
      }
      return insertIntoNearbyContainers(level, pos, remaining, true);
   }

   private static ItemStack insertIntoNearbyContainers(ServerLevel level, BlockPos pos, ItemStack stack, boolean includeMachines) {
      ItemStack remaining = stack.copy();
      List<BlockPos> targets = new ArrayList<>();
      for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 2, 2))) {
         targets.add(target.immutable());
      }
      targets.sort((left, right) -> Double.compare(left.distSqr(pos), right.distSqr(pos)));

      for (BlockPos target : targets) {
         BlockEntity blockEntity = level.getBlockEntity(target);
         if (!includeMachines && blockEntity instanceof NexusMachineBlockEntity) {
            continue;
         }
         if (blockEntity instanceof Container container) {
            remaining = insertIntoContainer(container, remaining);
            if (remaining.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }
      }
      return remaining;
   }

   private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
      ItemStack remaining = stack.copy();
      for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
         if (!container.canPlaceItem(slot, remaining)) {
            continue;
         }

         ItemStack current = container.getItem(slot);
         int max = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
         if (current.isEmpty()) {
            int moved = Math.min(max, remaining.getCount());
            ItemStack inserted = remaining.copyWithCount(moved);
            container.setItem(slot, inserted);
            remaining.shrink(moved);
         } else if (ItemStack.isSameItemSameComponents(current, remaining) && current.getCount() < max) {
            int moved = Math.min(max - current.getCount(), remaining.getCount());
            current.grow(moved);
            remaining.shrink(moved);
            container.setItem(slot, current);
         }
      }
      container.setChanged();
      return remaining;
   }

   private static void markNearbyPlayers(ServerLevel level, BlockPos pos, ProtocolSealBlock.SealMode mode) {
      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Math.max(8.0, sealRadius() + 3.0)))) {
         NexusPlayerData data = NexusPlayerData.get(player);
         data.markSealUsed(mode);
         NexusPlayerData.saveAndSync(player, data);
      }
   }

   private static ChunkPos chunkPos(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }

   private static int sealTickInterval() {
      return Math.max(10, (Integer)Config.SEAL_TICK_INTERVAL.get());
   }

   private static double sealRadius() {
      return Math.max(1, (Integer)Config.SEAL_RADIUS.get());
   }

   public static enum SealMode implements StringRepresentable {
      COLLECT("collect", "Collect"),
      EXTRACT("extract", "Extract"),
      REPAIR("repair", "Repair"),
      QUARANTINE("quarantine", "Quarantine"),
      PURIFY("purify", "Purify"),
      RELAY("relay", "Relay"),
      DEFENSE("defense", "Defense"),
      REWRITE("rewrite", "Rewrite"),
      COLLAPSE("collapse", "Collapse");

      private static final ProtocolSealBlock.SealMode[] BY_ID = values();
      private final String serializedName;
      private final String displayName;

      private SealMode(String serializedName, String displayName) {
         this.serializedName = serializedName;
         this.displayName = displayName;
      }

      public ProtocolSealBlock.SealMode next() {
         return BY_ID[(this.ordinal() + 1) % BY_ID.length];
      }

      public String displayName() {
         return this.displayName;
      }

      public String getSerializedName() {
         return this.serializedName;
      }
   }
}

package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echo.adaptercore.EchoBackendFluidBridge;
import com.knoxhack.echoindustrialnexus.block.IndustrialFluidPipeBlock;
import com.knoxhack.echoindustrialnexus.integration.IndustrialCompat;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoindustrialnexus.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class IndustrialFluidPipeBlockEntity extends BlockEntity {
   private int fluidId;
   private int amount;
   private int leakCooldown;
   private final Object fluidSnapshots = EchoBackendFluidBridge.createSnapshotJournal(
      () -> new PipeFluidSnapshot(IndustrialFluidPipeBlockEntity.this.fluidId, IndustrialFluidPipeBlockEntity.this.amount),
      snapshot -> {
         PipeFluidSnapshot pipeSnapshot = (PipeFluidSnapshot)snapshot;
         IndustrialFluidPipeBlockEntity.this.fluidId = pipeSnapshot.fluidId();
         IndustrialFluidPipeBlockEntity.this.amount = pipeSnapshot.amount();
         IndustrialFluidPipeBlockEntity.this.setChanged();
      }
   );
   private final ResourceHandler<FluidResource> fluidHandler = EchoBackendFluidBridge.createFluidHandler(new PipeFluidHandler());

   public IndustrialFluidPipeBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.FLUID_PIPE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, IndustrialFluidPipeBlockEntity pipeEntity) {
      if (level.isClientSide() || !(state.getBlock() instanceof IndustrialFluidPipeBlock pipeBlock)) {
         return;
      }
      IndustrialFluidPipeBlock.PipeTier tier = pipeBlock.tier();
      pipeEntity.pullFromNeighbors(level, pos, tier);
      pipeEntity.pushToNeighbors(level, pos, tier);
      pipeEntity.tickLeak(level, pos, tier);
   }

   public ResourceHandler<FluidResource> fluidHandler(Direction direction) {
      return this.fluidHandler;
   }

   public int fluidIdForTest() {
      return this.fluidId;
   }

   public int amountForTest() {
      return this.amount;
   }

   public int fluidId() {
      return this.fluidId;
   }

   public int amount() {
      return this.amount;
   }

   public boolean leakRisk(IndustrialFluidPipeBlock.PipeTier tier) {
      return this.fluidId > 0 && this.amount > 0 && ModFluids.isHazardousFluid(this.fluidId) && !this.accepts(tier, this.fluidId);
   }

   private void pullFromNeighbors(Level level, BlockPos pos, IndustrialFluidPipeBlock.PipeTier tier) {
      if (this.amount >= tier.capacity()) {
         return;
      }
      for (Direction direction : Direction.values()) {
         Object handler = EchoBackendFluidBridge.fluidCapability(level, pos.relative(direction), direction.getOpposite());
         if (handler == null) {
            continue;
         }
         int pulled = tryPull(handler, tier);
         if (pulled > 0) {
            this.setChanged();
            return;
         }
      }
   }

   private int tryPull(Object handler, IndustrialFluidPipeBlock.PipeTier tier) {
      int room = Math.min(tier.transferRate(), tier.capacity() - this.amount);
      if (room <= 0) {
         return 0;
      }
      for (int slot = 0; slot < EchoBackendFluidBridge.fluidHandlerSize(handler); slot++) {
         Object resource = EchoBackendFluidBridge.fluidHandlerResource(handler, slot);
         int sourceFluidId = ModFluids.idFor(resource);
         if (sourceFluidId <= 0 || !this.accepts(tier, sourceFluidId)) {
            continue;
         }
         int request = (int)Math.min(room, EchoBackendFluidBridge.fluidHandlerAmount(handler, slot));
         if (request <= 0) {
            continue;
         }
         Object tx = EchoBackendFluidBridge.openRootTransaction();
         try {
            int extracted = EchoBackendFluidBridge.extractFluid(handler, slot, resource, request, tx);
            int accepted = this.insertInternal(sourceFluidId, extracted, tier, tx);
            if (accepted > 0) {
               EchoBackendFluidBridge.commitTransaction(tx);
               return accepted;
            }
         } finally {
            EchoBackendFluidBridge.closeTransaction(tx);
         }
      }
      return 0;
   }

   private void pushToNeighbors(Level level, BlockPos pos, IndustrialFluidPipeBlock.PipeTier tier) {
      if (this.fluidId <= 0 || this.amount <= 0) {
         return;
      }
      Object resource = ModFluids.resourceFor(this.fluidId);
      for (Direction direction : Direction.values()) {
         Object handler = EchoBackendFluidBridge.fluidCapability(level, pos.relative(direction), direction.getOpposite());
         if (handler == null) {
            continue;
         }
         int request = Math.min(tier.transferRate(), this.amount);
         Object tx = EchoBackendFluidBridge.openRootTransaction();
         try {
            int inserted = EchoBackendFluidBridge.insertFluid(handler, resource, request, tx);
            if (inserted > 0) {
               this.drainInternal(this.fluidId, inserted + tier.lossPerTransfer(), tx);
               EchoBackendFluidBridge.commitTransaction(tx);
               if (level.getGameTime() % 60L == 0L) {
                  level.playSound(null, pos, ModSounds.PIPE_TRANSFER.get(), SoundSource.BLOCKS, 0.28F, 0.95F + level.getRandom().nextFloat() * 0.1F);
               }
               this.setChanged();
               return;
            }
         } finally {
            EchoBackendFluidBridge.closeTransaction(tx);
         }
      }
   }

   private void tickLeak(Level level, BlockPos pos, IndustrialFluidPipeBlock.PipeTier tier) {
      if (this.fluidId <= 0 || this.amount <= 0 || !ModFluids.isHazardousFluid(this.fluidId) || this.accepts(tier, this.fluidId)) {
         return;
      }
      if (this.leakCooldown-- > 0) {
         return;
      }
      this.leakCooldown = 40;
      this.amount = Math.max(0, this.amount - 50);
      if (this.amount == 0) {
         this.fluidId = IndustrialMachineBlockEntity.FLUID_NONE;
      }
      if (level instanceof ServerLevel serverLevel) {
         BlockPos leakPos = pos.above();
         if (serverLevel.getBlockState(leakPos).isAir() && this.fluidId == IndustrialMachineBlockEntity.FLUID_OIL_RESIDUE) {
            serverLevel.setBlock(leakPos, Blocks.FIRE.defaultBlockState(), 3);
         }
         IndustrialCompat.recordStaticFluidLeak(serverLevel, pos, this.fluidId, 50);
      }
      this.setChanged();
   }

   private boolean accepts(IndustrialFluidPipeBlock.PipeTier tier, int id) {
      if (id <= 0) {
         return false;
      }
      if (tier.nexusOnly()) {
         return ModFluids.isNexusFluid(id);
      }
      if (tier.dirtyOnly()) {
         return id == IndustrialMachineBlockEntity.FLUID_DIRTY_WATER
            || id == IndustrialMachineBlockEntity.FLUID_TOXIC_SLUDGE
            || id == IndustrialMachineBlockEntity.FLUID_OIL_RESIDUE;
      }
      if (ModFluids.isNexusFluid(id) && tier != IndustrialFluidPipeBlock.PipeTier.STATIC) {
         return false;
      }
      if (ModFluids.isHazardousFluid(id) && !tier.acceptsHazard()) {
         return false;
      }
      return tier.acceptsPressurized() || !ModFluids.isPressurizedSafe(id);
   }

   private int insertInternal(int id, int requested, IndustrialFluidPipeBlock.PipeTier tier, Object transaction) {
      if (id <= 0 || requested <= 0 || !this.accepts(tier, id)) {
         return 0;
      }
      if (this.fluidId != IndustrialMachineBlockEntity.FLUID_NONE && this.fluidId != id) {
         return 0;
      }
      int inserted = Math.min(requested, tier.capacity() - this.amount);
      if (inserted <= 0) {
         return 0;
      }
      if (transaction != null) {
         EchoBackendFluidBridge.updateSnapshots(this.fluidSnapshots, transaction);
      }
      this.fluidId = id;
      this.amount += inserted;
      return inserted;
   }

   private int drainInternal(int id, int requested, Object transaction) {
      if (id <= 0 || requested <= 0 || id != this.fluidId || this.amount <= 0) {
         return 0;
      }
      int extracted = Math.min(requested, this.amount);
      if (transaction != null) {
         EchoBackendFluidBridge.updateSnapshots(this.fluidSnapshots, transaction);
      }
      this.amount -= extracted;
      if (this.amount == 0) {
         this.fluidId = IndustrialMachineBlockEntity.FLUID_NONE;
      }
      return extracted;
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.fluidId = input.getIntOr("fluid_id", 0);
      this.amount = input.getIntOr("amount", 0);
      this.leakCooldown = input.getIntOr("leak_cooldown", 0);
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("fluid_id", this.fluidId);
      output.putInt("amount", this.amount);
      output.putInt("leak_cooldown", this.leakCooldown);
   }

   private final class PipeFluidHandler implements EchoBackendFluidBridge.EchoFluidHandlerDelegate {
      public int size() {
         return 1;
      }

      public Object getResource(int slot) {
         return slot == 0 && IndustrialFluidPipeBlockEntity.this.amount > 0
            ? ModFluids.resourceFor(IndustrialFluidPipeBlockEntity.this.fluidId)
            : EchoBackendFluidBridge.emptyFluidResource();
      }

      public long getAmountAsLong(int slot) {
         return slot == 0 ? IndustrialFluidPipeBlockEntity.this.amount : 0L;
      }

      public long getCapacityAsLong(int slot, Object resource) {
         return slot == 0 && IndustrialFluidPipeBlockEntity.this.getBlockState().getBlock() instanceof IndustrialFluidPipeBlock pipe ? pipe.tier().capacity() : 0L;
      }

      public boolean isValid(int slot, Object resource) {
         return slot == 0
            && IndustrialFluidPipeBlockEntity.this.getBlockState().getBlock() instanceof IndustrialFluidPipeBlock pipe
            && IndustrialFluidPipeBlockEntity.this.accepts(pipe.tier(), ModFluids.idFor(resource));
      }

      public int insert(int slot, Object resource, int maxAmount, Object transaction) {
         if (slot != 0 || !(IndustrialFluidPipeBlockEntity.this.getBlockState().getBlock() instanceof IndustrialFluidPipeBlock pipe)) {
            return 0;
         }
         int inserted = IndustrialFluidPipeBlockEntity.this.insertInternal(ModFluids.idFor(resource), maxAmount, pipe.tier(), transaction);
         if (inserted > 0) {
            IndustrialFluidPipeBlockEntity.this.setChanged();
         }
         return inserted;
      }

      public int extract(int slot, Object resource, int maxAmount, Object transaction) {
         if (slot != 0 || maxAmount <= 0 || IndustrialFluidPipeBlockEntity.this.amount <= 0) {
            return 0;
         }
         int id = ModFluids.idFor(resource);
         if (id <= 0 || id != IndustrialFluidPipeBlockEntity.this.fluidId) {
            return 0;
         }
         int extracted = IndustrialFluidPipeBlockEntity.this.drainInternal(id, maxAmount, transaction);
         IndustrialFluidPipeBlockEntity.this.setChanged();
         return extracted;
      }
   }

   private record PipeFluidSnapshot(int fluidId, int amount) {
   }
}

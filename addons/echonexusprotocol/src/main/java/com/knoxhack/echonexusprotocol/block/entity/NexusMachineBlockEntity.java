package com.knoxhack.echonexusprotocol.block.entity;

import com.knoxhack.echo.adaptercore.EchoBackendEnergyBridge;
import com.knoxhack.echo.adaptercore.EchoEnergyHandler;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.data.NexusEnergyStorage;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusMissionHooks;
import com.knoxhack.echonexusprotocol.menu.NexusMachineMenu;
import com.knoxhack.echonexusprotocol.recipe.NexusProcessingRecipe;
import com.knoxhack.echonexusprotocol.registry.ModBlockEntities;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.registry.ModRecipes;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class NexusMachineBlockEntity extends BaseContainerBlockEntity {
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   public static final int DATA_PROGRESS = 0;
   public static final int DATA_MAX_PROGRESS = 1;
   public static final int DATA_CHARGE = 2;
   public static final int DATA_MAX_CHARGE = 3;
   public static final int DATA_CORRUPTION = 4;
   public static final int DATA_KIND = 5;
   public static final int DATA_STATUS = 6;
   public static final int DATA_COUNT = 7;
   private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
   private final NexusEnergyStorage energy = new NexusEnergyStorage(
      (Integer)Config.MACHINE_CAPACITY.get(), (Integer)Config.MACHINE_TRANSFER.get(), (Integer)Config.MACHINE_TRANSFER.get(), this::setChanged
   );
   private int progress;
   private int maxProgress;
   private int contamination;
   private NexusMachineBlockEntity.MachineStatus status = NexusMachineBlockEntity.MachineStatus.IDLE;
   private NexusMachineBlock.MachineKind cachedRecipeKind;
   private ItemStack cachedRecipeInput = ItemStack.EMPTY;
   private RecipeHolder<NexusProcessingRecipe> cachedRecipe;
   private final ContainerData data = new ContainerData() {
      {
         Objects.requireNonNull(NexusMachineBlockEntity.this);
         Objects.requireNonNull(NexusMachineBlockEntity.this);
      }

      public int get(int dataId) {
         return switch (dataId) {
            case 0 -> NexusMachineBlockEntity.this.progress;
            case 1 -> NexusMachineBlockEntity.this.maxProgress;
            case 2 -> NexusMachineBlockEntity.this.energy.getEnergyStored();
            case 3 -> NexusMachineBlockEntity.this.energy.getCapacityAsInt();
            case 4 -> NexusMachineBlockEntity.this.contamination;
            case 5 -> NexusMachineBlockEntity.this.kind().ordinal();
            case 6 -> NexusMachineBlockEntity.this.status.ordinal();
            default -> 0;
         };
      }

      public void set(int dataId, int value) {
         switch (dataId) {
            case 0:
               NexusMachineBlockEntity.this.progress = value;
               break;
            case 1:
               NexusMachineBlockEntity.this.maxProgress = value;
               break;
            case 2:
               NexusMachineBlockEntity.this.energy.setEnergyStored(value);
            case 3:
            case 5:
            default:
               break;
            case 4:
               NexusMachineBlockEntity.this.contamination = Math.max(0, value);
               break;
            case 6:
               NexusMachineBlockEntity.this.status = NexusMachineBlockEntity.MachineStatus.byId(value);
         }
      }

      public int getCount() {
         return 7;
      }
   };

   public NexusMachineBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.NEXUS_MACHINE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, NexusMachineBlockEntity machine) {
      if (!level.isClientSide()) {
         NexusMachineBlock.MachineKind kind = machine.kind();
         if (kind == NexusMachineBlock.MachineKind.NEXUS_CHARGE_TANK || kind == NexusMachineBlock.MachineKind.CORRUPTION_REACTOR) {
            machine.transferChargeToNeighbors();
         }

         if (kind == NexusMachineBlock.MachineKind.CORRUPTION_FILTER) {
            machine.tickFilter();
         } else if (kind == NexusMachineBlock.MachineKind.NEXUS_FIELD_STABILIZER) {
            machine.tickStabilizer();
         } else if (kind == NexusMachineBlock.MachineKind.CORRUPTION_REACTOR) {
            if (level instanceof ServerLevel serverLevel) {
               machine.tickReactorFuel(serverLevel);
            }
            machine.tickReactor();
         } else if (!kind.recipeDriven()) {
            machine.resetProcessing(NexusMachineBlockEntity.MachineStatus.IDLE);
         } else if (level instanceof ServerLevel serverLevel) {
            ItemStack input = (ItemStack)machine.items.get(0);
            if (input.isEmpty()) {
               machine.resetProcessing(NexusMachineBlockEntity.MachineStatus.IDLE);
            } else {
               RecipeHolder<NexusProcessingRecipe> holder = machine.findRecipeFor(serverLevel, kind, input);
               if (holder == null) {
                  machine.resetProcessing(NexusMachineBlockEntity.MachineStatus.BAD_INPUT);
               } else {
                  NexusProcessingRecipe recipe = (NexusProcessingRecipe)holder.value();
                  ItemStack output = recipe.result().copy();
                  if (!machine.canOutput(output)) {
                     machine.resetProcessing(NexusMachineBlockEntity.MachineStatus.OUTPUT_BLOCKED);
                  } else if (scaledChargeCost(recipe) > 0 && machine.energy.getEnergyStored() < scaledChargeCost(recipe)) {
                     machine.maxProgress = scaledDuration(recipe);
                     machine.status = NexusMachineBlockEntity.MachineStatus.CHARGING;
                     machine.setChanged();
                  } else {
                     machine.maxProgress = scaledDuration(recipe);
                     machine.status = NexusMachineBlockEntity.MachineStatus.PROCESSING;
                     machine.progress++;
                     if (machine.progress >= machine.maxProgress) {
                        machine.completeRecipe(serverLevel, recipe);
                     }

                     machine.setChanged();
                  }
               }
            }
         }
      }
   }

   private static RecipeHolder<NexusProcessingRecipe> findRecipe(ServerLevel level, NexusMachineBlock.MachineKind kind, ItemStack input) {
      return level.getServer()
         .getRecipeManager()
         .getRecipes()
         .stream()
         .filter(holder -> holder.value().getType() == ModRecipes.NEXUS_PROCESSING_TYPE.get())
         .map(holder -> (RecipeHolder<NexusProcessingRecipe>)holder)
         .filter(holder -> ((NexusProcessingRecipe)holder.value()).matches(kind, input, level))
         .findFirst()
         .orElse(null);
   }

   public NexusEnergyStorage energyStorage() {
      return this.energy;
   }

   public int energyStored() {
      return this.energy.getEnergyStored();
   }

   public int maxEnergyStored() {
      return this.energy.getCapacityAsInt();
   }

   public int energySpace() {
      return this.energy.getSpace();
   }

   public void receiveCharge(int amount) {
      this.energy.receiveDirect(amount);
   }

   public boolean consumeCharge(int amount) {
      return this.energy.consume(amount);
   }

   public int contamination() {
      return this.contamination;
   }

   public void addContamination(int amount) {
      this.contamination = Math.max(0, Math.min(100, this.contamination + amount));
      this.setChanged();
   }

   public void reduceContamination(int amount) {
      this.addContamination(-Math.max(0, amount));
   }

   public ContainerData data() {
      return this.data;
   }

   public NexusMachineBlock.MachineKind kind() {
      return this.getBlockState().getBlock() instanceof NexusMachineBlock machine ? machine.kind() : NexusMachineBlock.MachineKind.NEXUS_RECYCLER;
   }

   public String statusLine() {
      return "ECHO-7 // "
         + this.kind().displayName()
         + " | CHARGE "
         + this.energy.getEnergyStored()
         + "/"
         + this.energy.getCapacityAsInt()
         + " | CORRUPTION "
         + this.contamination
         + "% | "
         + this.status.label();
   }

   public boolean acceptsInput(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      NexusMachineBlock.MachineKind kind = this.kind();
      if (!kind.recipeDriven() && kind != NexusMachineBlock.MachineKind.CORRUPTION_REACTOR) {
         return false;
      }
      return !(this.level instanceof ServerLevel serverLevel) || this.findRecipeFor(serverLevel, kind, stack) != null;
   }

   protected Component getDefaultName() {
      return Component.literal("ECHO-7 " + this.kind().displayName());
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> items) {
      for (int i = 0; i < Math.min(this.items.size(), items.size()); i++) {
         this.items.set(i, (ItemStack)items.get(i));
      }
   }

   public int getContainerSize() {
      return this.items.size();
   }

   protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new NexusMachineMenu(containerId, inventory, this, this.data);
   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.getBlockPos());
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, this.items);
      this.progress = input.getIntOr("progress", 0);
      this.maxProgress = input.getIntOr("maxProgress", 0);
      this.energy.setEnergyStored(input.getIntOr("nexusCharge", 0));
      this.contamination = input.getIntOr("contamination", 0);
      this.status = NexusMachineBlockEntity.MachineStatus.byId(input.getIntOr("status", NexusMachineBlockEntity.MachineStatus.IDLE.ordinal()));
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.items);
      output.putInt("progress", this.progress);
      output.putInt("maxProgress", this.maxProgress);
      output.putInt("nexusCharge", this.energy.getEnergyStored());
      output.putInt("contamination", this.contamination);
      output.putInt("status", this.status.ordinal());
   }

   private void tickFilter() {
      if (this.level != null && this.level.getGameTime() % 40L == 0L) {
         if (this.contamination > 0 && this.energy.consume(8)) {
            this.reduceContamination((Integer)Config.FILTER_CORRUPTION_REDUCTION.get());
            if (this.level instanceof ServerLevel serverLevel) {
               NexusWorldData.get(serverLevel).addCorruptionPressure(chunkPos(this.worldPosition), -(Integer)Config.FILTER_CORRUPTION_REDUCTION.get());
               this.play(serverLevel, ModSounds.FIELD_STABILIZE.get(), 0.45F, 1.25F);
            }

            this.status = NexusMachineBlockEntity.MachineStatus.PURIFYING;
            this.markNearbyPlayersUsed(this.kind(), null);
         } else if (this.contamination >= 75 && this.level instanceof ServerLevel serverLevel) {
            NexusWorldData.get(serverLevel).addCorruptionPressure(chunkPos(this.worldPosition), (Integer)Config.FILTER_LEAK_PRESSURE.get());
            this.play(serverLevel, ModSounds.CORRUPTION_LEAK.get(), 0.55F, 0.85F);
            this.status = NexusMachineBlockEntity.MachineStatus.LEAKING;
         } else {
            this.status = NexusMachineBlockEntity.MachineStatus.IDLE;
         }

         this.setChanged();
      }
   }

   private void tickStabilizer() {
      if (this.level instanceof ServerLevel serverLevel && this.level.getGameTime() % 80L == 0L) {
         this.stabilizeField(serverLevel);
      }
   }

   public void stabilizeField(ServerLevel serverLevel) {
      if (this.energy.consume(24)) {
         NexusWorldData data = NexusWorldData.get(serverLevel);
         ChunkPos chunk = chunkPos(this.worldPosition);
         data.addFieldValue(chunk, (Integer)Config.STABILIZER_FIELD_GAIN.get());
         data.addCorruptionPressure(chunk, -(Integer)Config.STABILIZER_CORRUPTION_REDUCTION.get());
         this.status = NexusMachineBlockEntity.MachineStatus.STABILIZING;
         this.markNearbyPlayersUsed(this.kind(), null);
         this.play(serverLevel, ModSounds.FIELD_STABILIZE.get(), 0.65F, 1.05F);
      } else {
         this.status = NexusMachineBlockEntity.MachineStatus.CHARGING;
      }

      this.setChanged();
   }

   private void tickReactor() {
      if (this.level instanceof ServerLevel serverLevel && this.level.getGameTime() % 60L == 0L) {
         int burn = Math.max(1, this.contamination / 10);
         if (this.contamination <= 0) {
            this.status = NexusMachineBlockEntity.MachineStatus.BAD_INPUT;
            this.setChanged();
         } else {
            this.reduceContamination(burn);
            this.energy.receiveDirect(scalePercent(120 * burn, (Integer)Config.REACTOR_OUTPUT_PERCENT.get()));
            NexusWorldData data = NexusWorldData.get(serverLevel);
            ChunkPos chunk = chunkPos(this.worldPosition);
            data.addFieldValue(chunk, -burn);
            data.addCorruptionPressure(chunk, burn * 2);
            this.status = NexusMachineBlockEntity.MachineStatus.REACTING;
            this.markNearbyPlayersUsed(this.kind(), null);
            this.play(serverLevel, ModSounds.CORRUPTION_LEAK.get(), 0.5F, 0.65F);
            this.setChanged();
         }
      }
   }

   private void tickReactorFuel(ServerLevel level) {
      ItemStack input = (ItemStack)this.items.get(0);
      if (!input.isEmpty()) {
            RecipeHolder<NexusProcessingRecipe> holder = this.findRecipeFor(level, NexusMachineBlock.MachineKind.CORRUPTION_REACTOR, input);
         if (holder != null) {
            NexusProcessingRecipe recipe = (NexusProcessingRecipe)holder.value();
            int chargeCost = scaledChargeCost(recipe);
            if (chargeCost <= 0 || this.energy.getEnergyStored() >= chargeCost) {
               this.maxProgress = scaledDuration(recipe);
               this.status = NexusMachineBlockEntity.MachineStatus.PROCESSING;
               this.progress++;
               if (this.progress >= this.maxProgress) {
                  this.completeRecipe(level, recipe);
               }

               this.setChanged();
            }
         }
      }
   }

   private boolean canOutput(ItemStack output) {
      if (output.isEmpty()) {
         return true;
      } else {
         ItemStack current = (ItemStack)this.items.get(1);
         return current.isEmpty()
            ? true
            : ItemStack.isSameItemSameComponents(current, output)
               && current.getCount() + output.getCount() <= Math.min(current.getMaxStackSize(), this.getMaxStackSize(current));
      }
   }

   private void completeRecipe(ServerLevel level, NexusProcessingRecipe recipe) {
      ItemStack input = (ItemStack)this.items.get(0);
      ItemStack originalInput = input.copy();
      ItemStack output = recipe.result().copy();
      input.shrink(1);
      if (!output.isEmpty()) {
         ItemStack currentOutput = (ItemStack)this.items.get(1);
         if (currentOutput.isEmpty()) {
            this.items.set(1, output);
         } else {
            currentOutput.grow(output.getCount());
         }
      }

      int chargeCost = scaledChargeCost(recipe);
      if (chargeCost > 0) {
         this.energy.consume(chargeCost);
      }

      NexusWorldData data = NexusWorldData.get(level);
      if (recipe.chargeOutput() > 0) {
         int outputCharge = scaledChargeOutput(recipe);
         if ("destroy".equals(data.endingState())) {
            outputCharge = Math.max(0, outputCharge / 2);
         }
         this.energy.receiveDirect(outputCharge);
      }

      this.addContamination(recipe.corruptionDelta());
      ChunkPos chunk = chunkPos(this.worldPosition);
      data.addCorruptionPressure(chunk, recipe.corruptionDelta());
      data.addFieldValue(chunk, recipe.fieldDelta());
      this.markNearbyPlayersUsed(recipe.machine(), originalInput);
      this.progress = 0;
      this.status = NexusMachineBlockEntity.MachineStatus.COMPLETE;
      this.play(level, ModSounds.MACHINE_PROCESS.get(), 0.45F, 1.0F);
   }

   private void markNearbyPlayersUsed(NexusMachineBlock.MachineKind kind, ItemStack input) {
      if (!(this.level instanceof ServerLevel serverLevel)) {
         return;
      }

      for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(12.0))) {
         NexusPlayerData data = NexusPlayerData.get(player);
         data.markMachineUsed(kind);
         NexusMissionHooks.recordMachine(player, kind);
         if (kind == NexusMachineBlock.MachineKind.MEMORY_DECODER && input != null && input.is(ModItems.BLACKBOX_FRAGMENT.get())) {
            data.addBlackboxFragment();
            NexusMissionHooks.recordBlackboxFragment(player);
         }
         NexusPlayerData.saveAndSync(player, data);
      }
   }

   private void resetProcessing(NexusMachineBlockEntity.MachineStatus next) {
      if (this.progress != 0 || this.maxProgress != 0 || this.status != next) {
         this.progress = 0;
         this.maxProgress = 0;
         this.status = next;
         this.setChanged();
      }
   }

   private RecipeHolder<NexusProcessingRecipe> findRecipeFor(ServerLevel level, NexusMachineBlock.MachineKind kind, ItemStack input) {
      if (!input.isEmpty()
         && this.cachedRecipe != null
         && this.cachedRecipeKind == kind
         && ItemStack.isSameItemSameComponents(this.cachedRecipeInput, input)
         && ((NexusProcessingRecipe)this.cachedRecipe.value()).matches(kind, input, level)) {
         return this.cachedRecipe;
      }
      RecipeHolder<NexusProcessingRecipe> holder = findRecipe(level, kind, input);
      this.cachedRecipeKind = kind;
      this.cachedRecipeInput = input.isEmpty() ? ItemStack.EMPTY : input.copyWithCount(1);
      this.cachedRecipe = holder;
      return holder;
   }

   private void transferChargeToNeighbors() {
      if (this.level != null && this.level.getGameTime() % 20L == 0L && this.energy.getEnergyStored() > 0) {
         for (Direction direction : Direction.values()) {
            BlockPos target = this.worldPosition.relative(direction);
            BlockState targetState = this.level.getBlockState(target);
            BlockEntity targetEntity = this.level.getBlockEntity(target);
            EchoEnergyHandler handler = EchoBackendEnergyBridge.blockEnergyHandler(this.level, target, direction.getOpposite());
            if (handler != null) {
               int moved = Math.min(64, this.energy.getEnergyStored());
               int accepted = handler.simulateInsertEnergy(moved);
               if (accepted > 0) {
                  int extracted = this.energy.extractEnergy(accepted, false);
                  int inserted = handler.insertEnergy(extracted);
                  if (inserted < extracted) {
                     this.energy.receiveEnergy(extracted - inserted, false);
                  }
               }
            }
         }
      }
   }

   private static ChunkPos chunkPos(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }

   private static int scaledDuration(NexusProcessingRecipe recipe) {
      return Math.max(1, scalePercent(recipe.duration(), (Integer)Config.MACHINE_DURATION_PERCENT.get()));
   }

   private static int scaledChargeCost(NexusProcessingRecipe recipe) {
      return Math.max(0, scalePercent(recipe.chargeCost(), (Integer)Config.MACHINE_CHARGE_COST_PERCENT.get()));
   }

   private static int scaledChargeOutput(NexusProcessingRecipe recipe) {
      return Math.max(0, scalePercent(recipe.chargeOutput(), (Integer)Config.MACHINE_CHARGE_OUTPUT_PERCENT.get()));
   }

   private static int scalePercent(int value, int percent) {
      return Math.max(0, Math.round(value * Math.max(0, percent) / 100.0F));
   }

   private void play(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
      level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
   }

   public static enum MachineStatus {
      IDLE("Idle"),
      PROCESSING("Processing"),
      CHARGING("Insufficient Nexus Charge"),
      OUTPUT_BLOCKED("Output blocked"),
      BAD_INPUT("Input rejected"),
      COMPLETE("Complete"),
      PURIFYING("Filtering corruption"),
      STABILIZING("Stabilizing local field"),
      REACTING("Forbidden reactor active"),
      LEAKING("Containment leak");

      private static final NexusMachineBlockEntity.MachineStatus[] BY_ID = values();
      private final String label;

      private MachineStatus(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }

      public static NexusMachineBlockEntity.MachineStatus byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : IDLE;
      }
   }
}

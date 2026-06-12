package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echo.adaptercore.EchoBackendFluidBridge;
import com.knoxhack.echoindustrialnexus.Config;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxNetwork;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxStorage;
import com.knoxhack.echoindustrialnexus.integration.IndustrialCompat;
import com.knoxhack.echoindustrialnexus.integration.IndustrialPowerGridBridge;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMachineMenu;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echoindustrialnexus.recipe.IndustrialProcessingRecipe;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import com.knoxhack.echoindustrialnexus.registry.ModRecipes;
import com.knoxhack.echoindustrialnexus.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class IndustrialMachineBlockEntity extends BaseContainerBlockEntity implements ThermalFluxStorage, WorldlyContainer {
   private static final Map<String, RecipeHolder<IndustrialProcessingRecipe>> FLUID_RECIPE_CACHE = new HashMap<>();
   private static int cachedRecipeCount = -1;
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   public static final int BYPRODUCT_SLOT = 2;
   public static final int AUX_SLOT = 3;
   public static final int UPGRADE_SLOT_START = 4;
   public static final int UPGRADE_SLOT_END = 8;
   public static final int SLOT_COUNT = 9;
   public static final int FLUID_TANK_CAPACITY = 4000;
   public static final int DATA_KIND = 0;
   public static final int DATA_FLUX = 1;
   public static final int DATA_MAX_FLUX = 2;
   public static final int DATA_PROGRESS = 3;
   public static final int DATA_MAX_PROGRESS = 4;
   public static final int DATA_HEAT = 5;
   public static final int DATA_STATUS = 6;
   public static final int DATA_SCRUBBER_MODE = 7;
   public static final int DATA_SIDE_CONFIG = 8;
   public static final int DATA_REMOTE_SHUTDOWN = 9;
   public static final int DATA_MELTDOWN_COOLDOWN = 10;
   public static final int DATA_INPUT_FLUID_ID = 11;
   public static final int DATA_INPUT_FLUID_AMOUNT = 12;
   public static final int DATA_OUTPUT_FLUID_ID = 13;
   public static final int DATA_OUTPUT_FLUID_AMOUNT = 14;
   public static final int DATA_ALERTS = 15;
   public static final int DATA_LINKED = 16;
   public static final int DATA_COUNT = 17;
   public static final int FLUID_NONE = 0;
   public static final int FLUID_DIRTY_WATER = 1;
   public static final int FLUID_CLEAN_WATER = 2;
   public static final int FLUID_TOXIC_SLUDGE = 3;
   public static final int FLUID_STATIC = 4;
   public static final int FLUID_CRYO_GEL = 5;
   public static final int FLUID_COOLANT = 6;
   public static final int FLUID_SOLVENT = 7;
   public static final int FLUID_NEXUS_GEL = 8;
   public static final int FLUID_OIL_RESIDUE = 9;
   private static final int[] INPUT_SLOTS = new int[]{INPUT_SLOT};
   private static final int[] OUTPUT_SLOTS = new int[]{OUTPUT_SLOT, BYPRODUCT_SLOT};
   private static final int[] AUX_SLOTS = new int[]{AUX_SLOT};
   private static final int[] UPGRADE_SLOTS = new int[]{UPGRADE_SLOT_START, 5, 6, 7, 8};
   private static final int[] INPUT_AND_AUX_SLOTS = new int[]{INPUT_SLOT, AUX_SLOT};
   private static final int[] ALL_AUTOMATION_INPUT_SLOTS = new int[]{INPUT_SLOT, AUX_SLOT, UPGRADE_SLOT_START, 5, 6, 7, 8};
   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case DATA_KIND -> IndustrialMachineBlockEntity.this.kind().ordinal();
            case DATA_FLUX -> IndustrialMachineBlockEntity.this.thermalFlux;
            case DATA_MAX_FLUX -> IndustrialMachineBlockEntity.this.getMaxFluxStored();
            case DATA_PROGRESS -> IndustrialMachineBlockEntity.this.progress;
            case DATA_MAX_PROGRESS -> IndustrialMachineBlockEntity.this.maxProgress;
            case DATA_HEAT -> IndustrialMachineBlockEntity.this.heat;
            case DATA_STATUS -> IndustrialMachineBlockEntity.this.status.ordinal();
            case DATA_SCRUBBER_MODE -> IndustrialMachineBlockEntity.this.scrubberMode;
            case DATA_SIDE_CONFIG -> IndustrialMachineBlockEntity.this.sideConfig;
            case DATA_REMOTE_SHUTDOWN -> IndustrialMachineBlockEntity.this.remoteShutdown ? 1 : 0;
            case DATA_MELTDOWN_COOLDOWN -> IndustrialMachineBlockEntity.this.meltdownCooldown;
            case DATA_INPUT_FLUID_ID -> IndustrialMachineBlockEntity.this.inputFluidId;
            case DATA_INPUT_FLUID_AMOUNT -> IndustrialMachineBlockEntity.this.inputFluidAmount;
            case DATA_OUTPUT_FLUID_ID -> IndustrialMachineBlockEntity.this.outputFluidId;
            case DATA_OUTPUT_FLUID_AMOUNT -> IndustrialMachineBlockEntity.this.outputFluidAmount;
            case DATA_ALERTS -> IndustrialMachineBlockEntity.this.alertCount();
            case DATA_LINKED -> IndustrialMachineBlockEntity.this.linkedCount;
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
      }

      @Override
      public int getCount() {
         return DATA_COUNT;
      }
   };
   private int thermalFlux;
   private int progress;
   private int maxProgress;
   private int heat;
   private int burnTime;
   private int scrubberMode;
   private int sideConfig;
   private boolean remoteShutdown;
   private int meltdownCooldown;
   private int inputFluidId;
   private int inputFluidAmount;
   private int outputFluidId;
   private int outputFluidAmount;
   private int linkedCount;
   private int controllerAlerts;
   private int controllerScanCooldown;
   private int cachedControllerDucts;
   private int cachedControllerSuppliers;
   private int cachedControllerReceivers;
   private int cachedControllerStoredFlux;
   private int cachedControllerCapacity;
   private List<BlockPos> cachedLinkedMachines = List.of();
   private IndustrialMachineBlockEntity.MachineStatus status = IndustrialMachineBlockEntity.MachineStatus.IDLE;
   private final Object fluidSnapshots = EchoBackendFluidBridge.createSnapshotJournal(
      () -> new FluidSnapshot(
            IndustrialMachineBlockEntity.this.inputFluidId,
            IndustrialMachineBlockEntity.this.inputFluidAmount,
            IndustrialMachineBlockEntity.this.outputFluidId,
            IndustrialMachineBlockEntity.this.outputFluidAmount
      ),
      snapshot -> {
         FluidSnapshot fluidSnapshot = (FluidSnapshot)snapshot;
         IndustrialMachineBlockEntity.this.inputFluidId = fluidSnapshot.inputFluidId();
         IndustrialMachineBlockEntity.this.inputFluidAmount = fluidSnapshot.inputFluidAmount();
         IndustrialMachineBlockEntity.this.outputFluidId = fluidSnapshot.outputFluidId();
         IndustrialMachineBlockEntity.this.outputFluidAmount = fluidSnapshot.outputFluidAmount();
         IndustrialMachineBlockEntity.this.setChanged();
      }
   );
   private final ResourceHandler<FluidResource> fluidHandler = EchoBackendFluidBridge.createFluidHandler(new MachineFluidHandler());

   public IndustrialMachineBlockEntity(BlockPos worldPosition, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.INDUSTRIAL_MACHINE.get(), worldPosition, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, IndustrialMachineBlockEntity machine) {
      if (!level.isClientSide()) {
         IndustrialMachineBlock.MachineKind kind = machine.kind();
         boolean active = false;
         if (machine.remoteShutdown) {
            machine.status = IndustrialMachineBlockEntity.MachineStatus.REMOTE_SHUTDOWN;
            machine.coolPassively(false);
         } else if (kind.generator()) {
            active = machine.tickGenerator(level, kind);
         } else if (kind.recipeDriven() && level instanceof ServerLevel serverLevel) {
            active = machine.tickProcessor(serverLevel, kind);
         } else if (kind.factoryController()) {
            active = machine.tickFactoryController(level);
         } else if (kind == IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER) {
            active = machine.tickScrubber(level);
         } else if (kind.storesFlux()) {
            machine.status = machine.thermalFlux > 0 ? IndustrialMachineBlockEntity.MachineStatus.STORED : IndustrialMachineBlockEntity.MachineStatus.IDLE;
         }

         machine.handleHeatFailure(level, active);
         machine.coolPassively(active);
         machine.updateActiveState(level, pos, state, active);
         machine.setChanged();
      }
   }

   private boolean tickGenerator(Level level, IndustrialMachineBlock.MachineKind kind) {
      if (kind == IndustrialMachineBlock.MachineKind.SOLAR_CONCENTRATOR) {
         long dayTime = level.getGameTime() % 24000L;
         if (dayTime < 13000L && level.canSeeSky(this.worldPosition.above())) {
            int output = this.adjustedGeneratorOutput(kind);
            this.generateFlux(output);
            IndustrialProgress.recordFluxGeneratedNearby(level, this.worldPosition, output);
            this.heat = Math.min(100, this.heat + this.adjustedHeat(1));
            this.status = this.heat >= 90 ? IndustrialMachineBlockEntity.MachineStatus.CRITICAL_HEAT : IndustrialMachineBlockEntity.MachineStatus.GENERATING;
            return true;
         } else {
            this.status = this.thermalFlux > 0 ? IndustrialMachineBlockEntity.MachineStatus.STORED : IndustrialMachineBlockEntity.MachineStatus.IDLE;
            return false;
         }
      } else {
         if (this.burnTime <= 0 && this.thermalFlux < this.getMaxFluxStored()) {
            this.burnTime = this.consumeFuel(kind);
         }

         if (this.burnTime <= 0) {
            this.status = this.thermalFlux > 0 ? IndustrialMachineBlockEntity.MachineStatus.STORED : IndustrialMachineBlockEntity.MachineStatus.IDLE;
            return false;
         } else {
            this.burnTime--;
            int output = this.adjustedGeneratorOutput(kind);
            this.generateFlux(output);
            IndustrialProgress.recordFluxGeneratedNearby(level, this.worldPosition, output);
            this.heat = Math.min(100, this.heat + this.adjustedHeat(kind == IndustrialMachineBlock.MachineKind.SCRAP_DYNAMO ? 1 : 2));
            this.status = this.heat >= 90 ? IndustrialMachineBlockEntity.MachineStatus.CRITICAL_HEAT : IndustrialMachineBlockEntity.MachineStatus.GENERATING;
            return true;
         }
      }
   }

   private boolean tickProcessor(ServerLevel level, IndustrialMachineBlock.MachineKind kind) {
      this.tickFluidContainerSlots();
      ItemStack input = (ItemStack)this.items.get(0);
      if (input.isEmpty()) {
         if (kind.usesFluidHandling()) {
            return this.tickFluidTankRecipe(level, kind);
         }
         this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.IDLE);
         return false;
      } else if (isUnsafeNexusInput(kind, input)) {
         if (level.getGameTime() % 20L == 0L) {
            this.heat = Math.min(100, this.heat + 4);
         }

         this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.NEXUS_CONTAMINATION);
         return false;
      } else {
         RecipeHolder<IndustrialProcessingRecipe> holder = findRecipe(level, kind, input);
         if (holder == null) {
            this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.BAD_INPUT);
            return false;
         } else {
            IndustrialProcessingRecipe recipe = (IndustrialProcessingRecipe)holder.value();
            if (!this.canOutput(recipe.result()) || !this.canOutputByproduct(recipe.byproduct())) {
               this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.OUTPUT_BLOCKED);
               return false;
            } else if (!this.hasCatalyst(recipe)) {
               this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.CATALYST_REQUIRED);
               return false;
            } else if (!this.hasInputFluid(recipe)) {
               this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.FLUID_REQUIRED);
               return false;
            } else if (!this.canOutputFluid(recipe.outputFluidId(), recipe.outputFluidAmount())) {
               this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.FLUID_OUTPUT_BLOCKED);
               return false;
            } else if (this.heat < 96) {
               this.maxProgress = this.adjustedDuration(recipe.duration());
               int costPerTick = Math.max(1, (int)Math.ceil((double)Math.max(1, this.adjustedFluxCost(recipe.fluxCost())) / this.maxProgress));
               this.requestFlux(costPerTick);
               if (this.thermalFlux < costPerTick) {
                  this.status = IndustrialMachineBlockEntity.MachineStatus.CHARGING;
                  return false;
               } else {
                  this.extractFlux(costPerTick, false);
                  this.progress++;
                  if (this.progress % 20 == 0) {
                     this.heat = Math.min(100, this.heat + this.adjustedHeat(recipe.heat()));
                  }

                  this.status = this.heat >= 85
                     ? IndustrialMachineBlockEntity.MachineStatus.HOT_PROCESSING
                     : IndustrialMachineBlockEntity.MachineStatus.PROCESSING;
                  if (this.progress >= this.maxProgress) {
                     this.completeRecipe(recipe);
                  }

                  return true;
               }
            } else if (this.consumeEmergencyShutdownModule()) {
               this.reduceHeat(45);
               this.status = IndustrialMachineBlockEntity.MachineStatus.EMERGENCY_SHUTDOWN;
               IndustrialProgress.recordOverheatEvent(level, this.worldPosition, true);
               return false;
            } else {
               this.resetProcessing(IndustrialMachineBlockEntity.MachineStatus.CRITICAL_HEAT);
               return false;
            }
         }
      }
   }

   private boolean tickFactoryController(Level level) {
      if (this.controllerScanCooldown > 0) {
         this.controllerScanCooldown--;
      }
      if (this.controllerScanCooldown <= 0) {
         this.refreshControllerScan(level);
      }
      this.status = this.linkedCount > 0 ? IndustrialMachineBlockEntity.MachineStatus.CONTROLLING : IndustrialMachineBlockEntity.MachineStatus.IDLE;
      return false;
   }

   private void refreshControllerScan(Level level) {
      ThermalFluxNetwork.ScanReport scan = ThermalFluxNetwork.scan(level, this.worldPosition);
      this.cachedControllerDucts = scan.ducts();
      this.cachedControllerSuppliers = scan.suppliers();
      this.cachedControllerReceivers = scan.receivers();
      this.cachedControllerStoredFlux = scan.storedFlux();
      this.cachedControllerCapacity = scan.capacity();
      this.cachedLinkedMachines = scan.linkedMachines();
      this.linkedCount = scan.suppliers() + scan.receivers();
      this.controllerAlerts = countHotLinkedMachines(level, this.cachedLinkedMachines);
      this.controllerScanCooldown = 40;
      this.setChanged();
   }

   private boolean tickScrubber(Level level) {
      this.tickFluidContainerSlots();
      int needed = (Integer)Config.SCRUBBER_FLUX_PER_TICK.get();
      this.requestFlux(needed);
      if (this.thermalFlux < needed) {
         this.status = IndustrialMachineBlockEntity.MachineStatus.CHARGING;
         return false;
      } else {
         this.extractFlux(needed, false);
         if (level.getGameTime() % 20L == 0L) {
            this.heat = Math.max(0, this.heat - (this.scrubberMode == ScrubberMode.COOLING.ordinal() ? 4 : 1));
         }

         this.status = IndustrialMachineBlockEntity.MachineStatus.SCRUBBING;
         if (level.getGameTime() % 80L == 0L) {
            level.playSound(null, this.worldPosition, ModSounds.SCRUBBER_OPERATION.get(), SoundSource.BLOCKS, 0.55F, 0.9F + level.getRandom().nextFloat() * 0.2F);
         }
         IndustrialProgress.markScrubberZone(level, this.worldPosition, this.scrubberModeName(), this.thermalFlux, this.heat);
         if (level instanceof ServerLevel serverLevel && level.getGameTime() % 40L == 0L) {
            IndustrialCompat.applyScrubber(serverLevel, this.worldPosition, this.scrubberModeName());
         }
         return true;
      }
   }

   private static @Nullable RecipeHolder<IndustrialProcessingRecipe> findRecipe(ServerLevel level, IndustrialMachineBlock.MachineKind kind, ItemStack input) {
      return level.getServer()
         .getRecipeManager()
         .getRecipes()
         .stream()
         .filter(holder -> holder.value().getType() == ModRecipes.INDUSTRIAL_PROCESSING_TYPE.get())
         .map(holder -> (RecipeHolder<IndustrialProcessingRecipe>)holder)
         .filter(holder -> ((IndustrialProcessingRecipe)holder.value()).matches(kind, input, level))
         .findFirst()
         .orElse(null);
   }

   private static @Nullable RecipeHolder<IndustrialProcessingRecipe> findFluidRecipe(ServerLevel level, IndustrialMachineBlock.MachineKind kind, int inputFluidId) {
      int recipeCount = level.getServer().getRecipeManager().getRecipes().size();
      if (recipeCount != cachedRecipeCount) {
         FLUID_RECIPE_CACHE.clear();
         cachedRecipeCount = recipeCount;
      }
      String cacheKey = kind.name() + ":" + inputFluidId;
      if (FLUID_RECIPE_CACHE.containsKey(cacheKey)) {
         return FLUID_RECIPE_CACHE.get(cacheKey);
      }
      RecipeHolder<IndustrialProcessingRecipe> holder = level.getServer()
         .getRecipeManager()
         .getRecipes()
         .stream()
         .filter(candidate -> candidate.value().getType() == ModRecipes.INDUSTRIAL_PROCESSING_TYPE.get())
         .map(candidate -> (RecipeHolder<IndustrialProcessingRecipe>)candidate)
         .filter(candidate -> ((IndustrialProcessingRecipe)candidate.value()).matchesFluid(kind, inputFluidId))
         .findFirst()
         .orElse(null);
      FLUID_RECIPE_CACHE.put(cacheKey, holder);
      return holder;
   }

   private void completeRecipe(IndustrialProcessingRecipe recipe) {
      ((ItemStack)this.items.get(0)).shrink(1);
      this.consumeCatalyst(recipe);
      this.mergeIntoSlot(1, recipe.result());
      if (!recipe.byproduct().isEmpty() && this.shouldOutputByproduct(recipe)) {
         this.mergeIntoSlot(2, recipe.byproduct());
      }
      if (recipe.fluxGeneration() > 0) {
         this.receiveFlux(recipe.fluxGeneration(), false);
      }
      if (recipe.outputFluidId() > 0 && recipe.outputFluidAmount() > 0) {
         this.fillOutputFluid(recipe.outputFluidId(), recipe.outputFluidAmount());
      }
      if (recipe.inputFluidId() > 0 && recipe.inputFluidAmount() > 0) {
         this.drainInputFluid(recipe.inputFluidId(), recipe.inputFluidAmount());
      }

      this.progress = 0;
      this.status = IndustrialMachineBlockEntity.MachineStatus.COMPLETE;
      if (this.level instanceof ServerLevel serverLevel) {
         IndustrialCompat.recordIndustrialOutput(serverLevel, this.worldPosition, recipe.result());
         if (this.kind() == IndustrialMachineBlock.MachineKind.NEXUS_THERMAL_INJECTOR || this.kind() == IndustrialMachineBlock.MachineKind.STATIC_HEAT_EXCHANGER) {
            IndustrialCompat.recordNexusThermalPressure(serverLevel, this.worldPosition, 2);
         }
      }
   }

   private boolean hasCatalyst(IndustrialProcessingRecipe recipe) {
      ItemStack catalyst = recipe.catalyst();
      if (catalyst.isEmpty()) {
         return true;
      }
      for (int slot = AUX_SLOT; slot < this.items.size(); slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (ItemStack.isSameItemSameComponents(stack, catalyst) && stack.getCount() >= catalyst.getCount()) {
            return true;
         }
      }
      return false;
   }

   private void consumeCatalyst(IndustrialProcessingRecipe recipe) {
      ItemStack catalyst = recipe.catalyst();
      if (catalyst.isEmpty()) {
         return;
      }
      for (int slot = AUX_SLOT; slot < this.items.size(); slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (ItemStack.isSameItemSameComponents(stack, catalyst) && stack.getCount() >= catalyst.getCount()) {
            stack.shrink(catalyst.getCount());
            if (stack.isEmpty()) {
               this.items.set(slot, ItemStack.EMPTY);
            }
            return;
         }
      }
   }

   private void tickFluidContainerSlots() {
      if (!this.kind().usesFluidHandling()) {
         return;
      }
      drainCellFromSlot(AUX_SLOT);
      fillOutputCell();
      if (this.kind() == IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER && this.inputFluidId == FLUID_COOLANT && this.inputFluidAmount >= 100) {
         this.inputFluidAmount -= 100;
         this.reduceHeat(2);
      }
   }

   private void drainCellFromSlot(int slot) {
      ItemStack stack = this.items.get(slot);
      int fluidId = fluidIdForFilledCell(stack);
      if (fluidId <= 0 || !canInputFluid(fluidId, 1000)) {
         return;
      }
      stack.shrink(1);
      if (stack.isEmpty()) {
         this.items.set(slot, ItemStack.EMPTY);
      }
      if (this.inputFluidId == FLUID_NONE) {
         this.inputFluidId = fluidId;
      }
      this.inputFluidAmount = Math.min(FLUID_TANK_CAPACITY, this.inputFluidAmount + 1000);
      this.setChanged();
   }

   private void fillOutputCell() {
      if (this.outputFluidId <= 0 || this.outputFluidAmount < 1000) {
         return;
      }
      ItemStack cell = filledCellForFluid(this.outputFluidId);
      if (cell.isEmpty() || !canFit(OUTPUT_SLOT, cell)) {
         return;
      }
      this.mergeIntoSlot(OUTPUT_SLOT, cell);
      this.outputFluidAmount -= 1000;
      if (this.outputFluidAmount <= 0) {
         this.outputFluidAmount = 0;
         this.outputFluidId = FLUID_NONE;
      }
      this.setChanged();
   }

   private boolean hasInputFluid(IndustrialProcessingRecipe recipe) {
      return recipe.inputFluidId() <= 0 || this.inputFluidId == recipe.inputFluidId() && this.inputFluidAmount >= recipe.inputFluidAmount();
   }

   private boolean tickFluidTankRecipe(ServerLevel level, IndustrialMachineBlock.MachineKind kind) {
      RecipeHolder<IndustrialProcessingRecipe> holder = findFluidRecipe(level, kind, this.inputFluidId);
      if (holder == null) {
         this.resetProcessing(this.inputFluidAmount > 0 ? MachineStatus.FLUID_REQUIRED : MachineStatus.IDLE);
         return false;
      }
      IndustrialProcessingRecipe recipe = holder.value();
      if (this.inputFluidAmount < recipe.inputFluidAmount()) {
         this.resetProcessing(MachineStatus.FLUID_REQUIRED);
         return false;
      }
      if (!this.hasCatalyst(recipe)) {
         this.resetProcessing(MachineStatus.CATALYST_REQUIRED);
         return false;
      }
      if (!recipe.byproduct().isEmpty() && !this.canFit(BYPRODUCT_SLOT, recipe.byproduct())) {
         this.resetProcessing(MachineStatus.OUTPUT_BLOCKED);
         return false;
      }
      if (!this.canOutput(recipe.result()) || !this.canOutputFluid(recipe.outputFluidId(), recipe.outputFluidAmount())) {
         this.resetProcessing(MachineStatus.FLUID_OUTPUT_BLOCKED);
         return false;
      }
      this.maxProgress = this.adjustedDuration(recipe.duration());
      int costPerTick = Math.max(1, (int)Math.ceil((double)Math.max(1, this.adjustedFluxCost(recipe.fluxCost())) / this.maxProgress));
      this.requestFlux(costPerTick);
      if (this.thermalFlux < costPerTick) {
         this.status = MachineStatus.CHARGING;
         return false;
      }
      this.extractFlux(costPerTick, false);
      this.progress++;
      if (this.progress % 20 == 0) {
         this.heat = Math.min(100, this.heat + this.adjustedHeat(recipe.heat()));
      }
      this.status = this.heat >= 85 ? MachineStatus.HOT_PROCESSING : MachineStatus.PROCESSING;
      if (this.progress >= this.maxProgress) {
         this.drainInputFluid(recipe.inputFluidId(), recipe.inputFluidAmount());
         this.consumeCatalyst(recipe);
         if (!recipe.result().isEmpty()) {
            this.mergeIntoSlot(OUTPUT_SLOT, recipe.result());
         }
         this.fillOutputFluid(recipe.outputFluidId(), recipe.outputFluidAmount());
         if (!recipe.byproduct().isEmpty() && this.shouldOutputByproduct(recipe)) {
            this.mergeIntoSlot(BYPRODUCT_SLOT, recipe.byproduct());
         }
         this.progress = 0;
         this.status = MachineStatus.COMPLETE;
      }
      return true;
   }

   private void drainInputFluid(int fluidId, int amount) {
      if (fluidId <= 0 || amount <= 0 || this.inputFluidId != fluidId) {
         return;
      }
      this.inputFluidAmount = Math.max(0, this.inputFluidAmount - amount);
      if (this.inputFluidAmount == 0) {
         this.inputFluidId = FLUID_NONE;
      }
      this.setChanged();
   }

   private boolean canInputFluid(int fluidId, int amount) {
      return fluidId > 0 && amount > 0 && (this.inputFluidId == FLUID_NONE || this.inputFluidId == fluidId)
         && this.inputFluidAmount + amount <= FLUID_TANK_CAPACITY;
   }

   private boolean canOutputFluid(int fluidId, int amount) {
      return fluidId <= 0 || amount <= 0 || (this.outputFluidId == FLUID_NONE || this.outputFluidId == fluidId)
         && this.outputFluidAmount + amount <= FLUID_TANK_CAPACITY;
   }

   private void fillOutputFluid(int fluidId, int amount) {
      if (!canOutputFluid(fluidId, amount)) {
         return;
      }
      if (this.outputFluidId == FLUID_NONE) {
         this.outputFluidId = fluidId;
      }
      this.outputFluidAmount = Math.min(FLUID_TANK_CAPACITY, this.outputFluidAmount + amount);
      this.setChanged();
   }

   public int fillInputFluidForTest(int fluidId, int amount) {
      return this.fillInputFluid(fluidId, amount);
   }

   public ResourceHandler<FluidResource> fluidHandler(Direction direction) {
      return this.kind().usesFluidHandling() ? this.fluidHandler : null;
   }

   public int fillInputFluid(int fluidId, int amount) {
      int fill = Math.max(0, Math.min(amount, FLUID_TANK_CAPACITY - this.inputFluidAmount));
      if (fluidId <= 0 || fill <= 0 || (this.inputFluidId != FLUID_NONE && this.inputFluidId != fluidId)) {
         return 0;
      }
      this.inputFluidId = fluidId;
      this.inputFluidAmount += fill;
      this.setChanged();
      return fill;
   }

   public int drainOutputFluid(int fluidId, int amount) {
      if (fluidId <= 0 || amount <= 0 || this.outputFluidId != fluidId || this.outputFluidAmount <= 0) {
         return 0;
      }
      int drained = Math.min(amount, this.outputFluidAmount);
      this.outputFluidAmount -= drained;
      if (this.outputFluidAmount == 0) {
         this.outputFluidId = FLUID_NONE;
      }
      this.setChanged();
      return drained;
   }

   public int inputFluidId() {
      return this.inputFluidId;
   }

   public int outputFluidId() {
      return this.outputFluidId;
   }

   public int inputFluidAmount() {
      return this.inputFluidAmount;
   }

   public int outputFluidAmount() {
      return this.outputFluidAmount;
   }

   private boolean shouldOutputByproduct(IndustrialProcessingRecipe recipe) {
      return recipe.byproductChance() >= 100 || this.level == null || this.level.getRandom().nextInt(100) < recipe.byproductChance();
   }

   private int consumeFuel(IndustrialMachineBlock.MachineKind kind) {
      ItemStack fuel = (ItemStack)this.items.get(0);
      int burn = this.fuelBurnTime(kind, fuel);
      if (burn <= 0) {
         return 0;
      } else {
         if (fuel.is(Items.LAVA_BUCKET)) {
            this.items.set(INPUT_SLOT, new ItemStack(Items.BUCKET));
         } else {
            fuel.shrink(1);
            if (fuel.isEmpty()) {
               this.items.set(INPUT_SLOT, ItemStack.EMPTY);
            }
         }
         this.setChanged();
         return burn;
      }
   }

   private int fuelBurnTime(IndustrialMachineBlock.MachineKind kind, ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      } else if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
         return kind == IndustrialMachineBlock.MachineKind.THERMAL_ARRAY ? 160 : 240;
      } else if (stack.is(Items.LAVA_BUCKET)) {
         return kind != IndustrialMachineBlock.MachineKind.THERMAL_ARRAY && kind != IndustrialMachineBlock.MachineKind.GEOTHERMAL_PUMP ? 600 : 1200;
      } else if (stack.is(Items.MAGMA_BLOCK)) {
         return kind == IndustrialMachineBlock.MachineKind.GEOTHERMAL_PUMP ? 420 : 0;
      } else if (stack.is((Item)ModItems.SCRAP_FUEL.get())) {
         return 180;
      } else if (stack.is((Item)ModItems.COMPACTED_ASH_FUEL.get())) {
         return 320;
      } else if (stack.is((Item)ModItems.SCRAP_METAL.get())) {
         return 80;
      } else if (stack.is((Item)ModItems.HEAT_COIL.get())) {
         return kind != IndustrialMachineBlock.MachineKind.THERMAL_ARRAY
               && kind != IndustrialMachineBlock.MachineKind.GEOTHERMAL_PUMP
               && kind != IndustrialMachineBlock.MachineKind.FURNACE_WARDEN_CORE
            ? 0
            : 600;
      } else if (stack.is((Item)ModItems.RAD_SLAG.get())) {
         return kind == IndustrialMachineBlock.MachineKind.REACTOR_HEAT_EXCHANGER ? 480 : 0;
      } else if (stack.is((Item)ModItems.URANIUM_DUST.get())) {
         return kind == IndustrialMachineBlock.MachineKind.REACTOR_HEAT_EXCHANGER ? 900 : 0;
      } else if (stack.is((Item)ModItems.NEXUS_DUST.get())) {
         return kind == IndustrialMachineBlock.MachineKind.STATIC_HEAT_EXCHANGER ? 420 : 0;
      } else if (stack.is((Item)ModItems.STABLE_NEXUS_CORE.get())) {
         return kind == IndustrialMachineBlock.MachineKind.STATIC_HEAT_EXCHANGER ? 1600 : 0;
      } else if (stack.is((Item)ModItems.HYBRID_THERMAL_CORE.get())) {
         return kind == IndustrialMachineBlock.MachineKind.FURNACE_WARDEN_CORE ? 3200 : 0;
      } else {
         return 0;
      }
   }

   private int adjustedGeneratorOutput(IndustrialMachineBlock.MachineKind kind) {
      int base = switch (kind) {
         case THERMAL_ARRAY -> Config.THERMAL_ARRAY_OUTPUT.get();
         case GEOTHERMAL_PUMP -> 96;
         case REACTOR_HEAT_EXCHANGER -> 144;
         case SOLAR_CONCENTRATOR -> 40;
         case STATIC_HEAT_EXCHANGER -> 128;
         case FURNACE_WARDEN_CORE -> 240;
         default -> Config.SCRAP_DYNAMO_OUTPUT.get();
      };
      if (this.hasUpgrade((Item)ModItems.EFFICIENCY_COIL.get())) {
         base = (int)Math.ceil(base * 1.15);
      }

      if (this.hasUpgrade((Item)ModItems.OVERCLOCK_CORE.get())) {
         base = (int)Math.ceil(base * 1.65);
      }

      return Math.max(1, base);
   }

   private int adjustedDuration(int duration) {
      int adjusted = Math.max(20, duration);
      if (this.hasUpgrade((Item)ModItems.SPEED_SERVO.get())) {
         adjusted = (int)Math.ceil(adjusted * 0.75);
      }

      if (this.hasUpgrade((Item)ModItems.OVERCLOCK_CORE.get())) {
         adjusted = (int)Math.ceil(adjusted * 0.5);
      }

      return Math.max(20, adjusted);
   }

   private int adjustedFluxCost(int cost) {
      int adjusted = Math.max(1, cost);
      if (this.hasUpgrade((Item)ModItems.EFFICIENCY_COIL.get())) {
         adjusted = (int)Math.ceil(adjusted * 0.7);
      }

      if (this.hasUpgrade((Item)ModItems.OVERCLOCK_CORE.get())) {
         adjusted = (int)Math.ceil(adjusted * 1.35);
      }

      return Math.max(1, adjusted);
   }

   private int adjustedHeat(int heatAdded) {
      int adjusted = Math.max(0, heatAdded);
      if (this.hasUpgrade((Item)ModItems.HEAT_SINK_UPGRADE.get())) {
         adjusted -= 2;
      }

      if (this.hasUpgrade((Item)ModItems.RADIATION_SHIELDING_UPGRADE.get()) && this.kind() == IndustrialMachineBlock.MachineKind.REACTOR_HEAT_EXCHANGER) {
         adjusted -= 2;
      }

      if (this.hasUpgrade((Item)ModItems.NEXUS_STABILIZER_UPGRADE.get()) && this.kind().handlesNexusMaterials()) {
         adjusted -= 3;
      }

      if (this.hasUpgrade((Item)ModItems.OVERCLOCK_CORE.get())) {
         adjusted += 3;
      }

      return Math.max(0, adjusted);
   }

   public boolean insertFromHand(Player player, InteractionHand hand, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.is((Item)ModItems.COOLANT_CELL.get()) && this.heat > 0) {
         this.reduceHeat(35);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
         }
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Coolant cell injected. Heat now " + this.heat + "%."));
         return true;
      } else if (isUpgrade(stack)) {
         int upgradeSlot = this.findUpgradeSlot(stack);
         if (upgradeSlot < 0) {
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Upgrade rejected. Module type already installed or upgrade bay full."));
            return false;
         } else {
            ItemStack one = stack.copy();
            one.setCount(1);
            this.items.set(upgradeSlot, one);
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
               player.setItemInHand(hand, stack);
            }
            player.sendSystemMessage(
               Component.literal("ECHO INDUSTRIAL // Installed " + one.getHoverName().getString() + " in " + this.kind().displayName() + ".")
            );
            this.setChanged();
            return true;
         }
      } else if (this.kind().usesFluidHandling() && fluidIdForFilledCell(stack) > 0 && this.canInputFluid(fluidIdForFilledCell(stack), 1000)) {
         ItemStack one = stack.copy();
         one.setCount(1);
         this.drainCellFromTemporary(one);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
         }
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Loaded " + fluidLabel(this.inputFluidId) + " into internal tank (" + this.inputFluidAmount + " mB)."));
         return true;
      } else if (!this.canInsertIntoSlot(0, stack)) {
         return false;
      } else {
         ItemStack one = stack.copy();
         one.setCount(1);
         this.mergeIntoSlot(0, one);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
         }
         String warning = isUnsafeNexusInput(this.kind(), one) ? " WARNING: non-stabilized machine field drift detected." : "";
         player.sendSystemMessage(
            Component.literal("ECHO INDUSTRIAL // Inserted " + one.getHoverName().getString() + " into " + this.kind().displayName() + "." + warning)
         );
         return true;
      }
   }

   private void drainCellFromTemporary(ItemStack one) {
      int fluidId = fluidIdForFilledCell(one);
      if (fluidId <= 0 || !canInputFluid(fluidId, 1000)) {
         return;
      }
      if (this.inputFluidId == FLUID_NONE) {
         this.inputFluidId = fluidId;
      }
      this.inputFluidAmount += 1000;
      this.setChanged();
   }

   private static boolean isUnsafeNexusInput(IndustrialMachineBlock.MachineKind kind, ItemStack stack) {
      return !kind.handlesNexusMaterials() && isNexusMaterial(stack);
   }

   public static boolean isNexusMaterial(ItemStack stack) {
      return stack.is((Item)ModItems.NEXUS_DUST.get())
         || stack.is((Item)ModItems.STABLE_NEXUS_CORE.get())
         || stack.is((Item)ModItems.HYBRID_THERMAL_CORE.get());
   }

   public static int fluidIdForFilledCell(ItemStack stack) {
      if (stack.is((Item)ModItems.DIRTY_WATER_CELL.get())) {
         return FLUID_DIRTY_WATER;
      }
      if (stack.is((Item)ModItems.CLEAN_WATER_CELL.get())) {
         return FLUID_CLEAN_WATER;
      }
      if (stack.is((Item)ModItems.TOXIC_SLUDGE_CELL.get())) {
         return FLUID_TOXIC_SLUDGE;
      }
      if (stack.is((Item)ModItems.STATIC_FLUID_CELL.get())) {
         return FLUID_STATIC;
      }
      if (stack.is((Item)ModItems.CRYO_GEL.get())) {
         return FLUID_CRYO_GEL;
      }
      if (stack.is((Item)ModItems.COOLANT_CELL.get())) {
         return FLUID_COOLANT;
      }
      if (stack.is((Item)ModItems.CHEMICAL_SOLVENT.get())) {
         return FLUID_SOLVENT;
      }
      if (stack.is((Item)ModItems.NEXUS_GEL.get())) {
         return FLUID_NEXUS_GEL;
      }
      return FLUID_NONE;
   }

   public static ItemStack filledCellForFluid(int fluidId) {
      return switch (fluidId) {
         case FLUID_DIRTY_WATER -> new ItemStack((Item)ModItems.DIRTY_WATER_CELL.get());
         case FLUID_CLEAN_WATER -> new ItemStack((Item)ModItems.CLEAN_WATER_CELL.get());
         case FLUID_TOXIC_SLUDGE -> new ItemStack((Item)ModItems.TOXIC_SLUDGE_CELL.get());
         case FLUID_STATIC -> new ItemStack((Item)ModItems.STATIC_FLUID_CELL.get());
         case FLUID_CRYO_GEL -> new ItemStack((Item)ModItems.CRYO_GEL.get());
         case FLUID_COOLANT -> new ItemStack((Item)ModItems.COOLANT_CELL.get());
         case FLUID_SOLVENT -> new ItemStack((Item)ModItems.CHEMICAL_SOLVENT.get());
         case FLUID_NEXUS_GEL -> new ItemStack((Item)ModItems.NEXUS_GEL.get());
         default -> ItemStack.EMPTY;
      };
   }

   public static String fluidLabel(int fluidId) {
      return switch (fluidId) {
         case FLUID_DIRTY_WATER -> "Dirty Water";
         case FLUID_CLEAN_WATER -> "Clean Water";
         case FLUID_TOXIC_SLUDGE -> "Toxic Sludge";
         case FLUID_STATIC -> "Static Fluid";
         case FLUID_CRYO_GEL -> "Cryo Gel";
         case FLUID_COOLANT -> "Coolant";
         case FLUID_SOLVENT -> "Chemical Solvent";
         case FLUID_NEXUS_GEL -> "Nexus Gel";
         case FLUID_OIL_RESIDUE -> "Oil Residue";
         default -> "Empty";
      };
   }

   public static boolean isUpgrade(ItemStack stack) {
      return stack.is((Item)ModItems.SPEED_SERVO.get())
         || stack.is((Item)ModItems.EFFICIENCY_COIL.get())
         || stack.is((Item)ModItems.HEAT_SINK_UPGRADE.get())
         || stack.is((Item)ModItems.FILTER_MODULE.get())
         || stack.is((Item)ModItems.SECONDARY_OUTPUT_MODULE.get())
         || stack.is((Item)ModItems.RADIATION_SHIELDING_UPGRADE.get())
         || stack.is((Item)ModItems.NEXUS_STABILIZER_UPGRADE.get())
         || stack.is((Item)ModItems.FACTORY_LINK_CHIP.get())
         || stack.is((Item)ModItems.OVERCLOCK_CORE.get())
         || stack.is((Item)ModItems.EMERGENCY_SHUTDOWN_MODULE.get());
   }

   private boolean hasUpgrade(Item item) {
      for (int slot = UPGRADE_SLOT_START; slot <= UPGRADE_SLOT_END; slot++) {
         if (((ItemStack)this.items.get(slot)).is(item)) {
            return true;
         }
      }
      return false;
   }

   private int findUpgradeSlot(ItemStack stack) {
      if (hasUpgrade(stack.getItem())) {
         return -1;
      }
      for (int slot = UPGRADE_SLOT_START; slot <= UPGRADE_SLOT_END; slot++) {
         if (this.items.get(slot).isEmpty()) {
            return slot;
         }
      }
      return -1;
   }

   private boolean canInsertIntoSlot(int slot, ItemStack stack) {
      ItemStack existing = (ItemStack)this.items.get(slot);
      return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize();
   }

   public ItemStack extractToPlayer(Player player) {
      ItemStack extracted = this.extractSlot(2);
      if (extracted.isEmpty()) {
         extracted = this.extractSlot(1);
      }

      if (extracted.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         player.getInventory().placeItemBackInInventory(extracted.copy());
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Retrieved " + extracted.getCount() + "x " + extracted.getHoverName().getString() + "."));
         return extracted;
      }
   }

   private ItemStack extractSlot(int slot) {
      ItemStack current = (ItemStack)this.items.get(slot);
      if (current.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack extracted = current.copy();
         this.items.set(slot, ItemStack.EMPTY);
         this.setChanged();
         return extracted;
      }
   }

   private void mergeIntoSlot(int slot, ItemStack stack) {
      if (!stack.isEmpty()) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (existing.isEmpty()) {
            this.items.set(slot, stack.copy());
         } else {
            existing.grow(stack.getCount());
         }
      }
   }

   private boolean canOutput(ItemStack output) {
      return this.canFit(1, output);
   }

   private boolean canOutputByproduct(ItemStack byproduct) {
      return byproduct.isEmpty() || this.canFit(2, byproduct);
   }

   private boolean canFit(int slot, ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         ItemStack existing = (ItemStack)this.items.get(slot);
         return existing.isEmpty()
            ? stack.getCount() <= stack.getMaxStackSize()
            : ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
      }
   }

   private void requestFlux(int amount) {
      if (amount > 0 && this.level != null && this.thermalFlux < amount) {
         int needed = Math.min(this.getMaxFluxStored() - this.thermalFlux, Math.max(0, amount - this.thermalFlux));
         if (needed > 0) {
            int drawn = ThermalFluxNetwork.drawFlux(this.level, this.worldPosition, needed);
            this.receiveFlux(drawn, false);
            int remaining = needed - drawn;
            if (remaining > 0) {
               this.receiveFlux(IndustrialPowerGridBridge.drawThermalFlux(this.level, this.worldPosition, remaining, false), false);
            }
         }
      }
   }

   private void handleHeatFailure(Level level, boolean active) {
      if (this.meltdownCooldown > 0) {
         this.meltdownCooldown--;
      }
      if (this.heat < 100 || this.meltdownCooldown > 0 || !active) {
         return;
      }
      if (this.consumeEmergencyShutdownModule()) {
         this.reduceHeat(55);
         this.progress = 0;
         this.thermalFlux = Math.max(0, this.thermalFlux / 2);
         this.status = IndustrialMachineBlockEntity.MachineStatus.EMERGENCY_SHUTDOWN;
         IndustrialProgress.recordOverheatEvent(level, this.worldPosition, true);
         this.meltdownCooldown = 80;
         return;
      }

      this.status = IndustrialMachineBlockEntity.MachineStatus.MELTDOWN;
      this.progress = 0;
      this.thermalFlux = Math.max(0, this.thermalFlux / 3);
      this.heat = 72;
      this.meltdownCooldown = 160;
      IndustrialProgress.recordOverheatEvent(level, this.worldPosition, false);
      if (level instanceof ServerLevel serverLevel) {
         BlockPos vent = this.worldPosition.above();
         if (serverLevel.getBlockState(vent).isAir()) {
            serverLevel.setBlockAndUpdate(vent, Blocks.FIRE.defaultBlockState());
         }
         serverLevel.playSound(null, this.worldPosition, ModSounds.OVERHEAT_ALARM.get(), SoundSource.BLOCKS, 1.0F, 0.75F);
         serverLevel.levelEvent(2001, this.worldPosition, Block.getId(this.getBlockState()));
         if (this.kind().handlesNexusMaterials() || isNexusMaterial((ItemStack)this.items.get(INPUT_SLOT))) {
            IndustrialProgress.recordNexusThermalWarning(serverLevel, this.worldPosition);
            IndustrialCompat.recordNexusThermalPressure(serverLevel, this.worldPosition, 3);
         }
      }
   }

   private boolean consumeEmergencyShutdownModule() {
      for (int slot = UPGRADE_SLOT_START; slot <= UPGRADE_SLOT_END; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (stack.is((Item)ModItems.EMERGENCY_SHUTDOWN_MODULE.get())) {
            stack.shrink(1);
            if (stack.isEmpty()) {
               this.items.set(slot, ItemStack.EMPTY);
            }
            return true;
         }
      }
      return false;
   }

   private void resetProcessing(IndustrialMachineBlockEntity.MachineStatus newStatus) {
      this.progress = 0;
      this.maxProgress = 0;
      this.status = newStatus;
   }

   private void reduceHeat(int amount) {
      this.heat = Math.max(0, this.heat - amount);
      this.setChanged();
   }

   private void coolPassively(boolean active) {
      if (this.level != null && this.level.getGameTime() % 40L == 0L && this.heat > 0 && !active) {
         this.heat--;
      }
   }

   private void updateActiveState(Level level, BlockPos pos, BlockState state, boolean active) {
      if (state.getBlock() instanceof IndustrialMachineBlock && (Boolean)state.getValue(IndustrialMachineBlock.ACTIVE) != active) {
         level.setBlockAndUpdate(pos, (BlockState)state.setValue(IndustrialMachineBlock.ACTIVE, active));
      }
   }

   public String statusLine() {
      return "ECHO INDUSTRIAL // "
         + this.kind().displayName()
         + " | STATUS: "
         + this.status.label()
         + " | POWER: "
         + this.thermalFlux
         + "/"
         + this.getMaxFluxStored()
         + " TF | HEAT: "
         + this.heat
         + "% | PROGRESS: "
         + this.progress
         + "/"
         + this.maxProgress;
   }

   public String diagnosticLine() {
      return "ECHO INDUSTRIAL DIAGNOSTIC // "
         + this.kind().displayName()
         + " | TF: "
         + this.thermalFlux
         + "/"
         + this.getMaxFluxStored()
         + " | HEAT: "
         + this.heat
         + "% | BURN: "
         + this.burnTime
         + " | STATE: "
         + this.status.label();
   }

   public String scrubberModeName() {
      return ScrubberMode.byId(this.scrubberMode).label();
   }

   public static String scrubberModeLabel(int id) {
      return ScrubberMode.byId(id).label();
   }

   public static String sideConfigLabel(int id) {
      return SideConfig.byId(id).label();
   }

   public boolean cycleScrubberMode(Player player) {
      if (this.kind() != IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER) {
         return false;
      }
      this.scrubberMode = (this.scrubberMode + 1) % ScrubberMode.values().length;
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Scrubber mode set to " + this.scrubberModeName() + "."));
      return true;
   }

   public ContainerData data() {
      return this.data;
   }

   public String factoryControllerLine() {
      if (this.level == null) {
         return this.statusLine();
      } else {
         return "ECHO FACTORY CONTROLLER // DUCTS: "
            + this.cachedControllerDucts
            + " | SUPPLIERS: "
            + this.cachedControllerSuppliers
            + " | RECEIVERS: "
            + this.cachedControllerReceivers
            + " | STORED: "
            + this.cachedControllerStoredFlux
            + "/"
            + this.cachedControllerCapacity
            + " TF";
      }
   }

   private int countHotLinkedMachines(Level level, List<BlockPos> linkedMachines) {
      int hot = 0;
      for (BlockPos pos : linkedMachines) {
         if (level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine && machine != this
            && (machine.heatLevel() >= 85 || machine.remoteShutdown || machine.status == MachineStatus.MELTDOWN)) {
            hot++;
         }
      }
      return hot;
   }

   private int alertCount() {
      int alerts = this.controllerAlerts;
      if (this.remoteShutdown) {
         alerts++;
      }
      if (this.heat >= 85) {
         alerts++;
      }
      if (this.status == MachineStatus.NEXUS_CONTAMINATION || this.status == MachineStatus.MELTDOWN || this.status == MachineStatus.FLUID_OUTPUT_BLOCKED) {
         alerts++;
      }
      return alerts;
   }

   public boolean handleMenuButton(Player player, int id) {
      return switch (id) {
         case IndustrialMachineMenu.BUTTON_CYCLE_SCRUBBER -> this.cycleScrubberMode(player);
         case IndustrialMachineMenu.BUTTON_CYCLE_SIDE_CONFIG -> this.cycleSideConfig(player);
         case IndustrialMachineMenu.BUTTON_TOGGLE_SHUTDOWN -> this.toggleRemoteShutdown(player);
         case IndustrialMachineMenu.BUTTON_CONTROLLER_SHUTDOWN -> this.controllerEmergencyShutdown(player);
         default -> false;
      };
   }

   public boolean cycleSideConfig(Player player) {
      this.sideConfig = (this.sideConfig + 1) % SideConfig.values().length;
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Side configuration set to " + sideConfigLabel(this.sideConfig) + "."));
      return true;
   }

   public boolean toggleRemoteShutdown(Player player) {
      this.remoteShutdown = !this.remoteShutdown;
      this.progress = 0;
      this.status = this.remoteShutdown ? MachineStatus.REMOTE_SHUTDOWN : MachineStatus.IDLE;
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.kind().displayName() + " " + (this.remoteShutdown ? "remote shutdown engaged." : "remote shutdown cleared.")));
      return true;
   }

   public boolean controllerEmergencyShutdown(Player player) {
      if (!this.kind().factoryController() || this.level == null) {
         return false;
      }
      this.refreshControllerScan(this.level);
      int affected = 0;
      for (BlockPos pos : this.cachedLinkedMachines) {
         if (this.level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine && machine != this && !machine.kind().factoryController()) {
            machine.remoteShutdown = true;
            machine.progress = 0;
            machine.status = MachineStatus.REMOTE_SHUTDOWN;
            machine.setChanged();
            affected++;
         }
      }
      this.controllerAlerts = affected;
      this.controllerScanCooldown = 0;
      this.refreshControllerScan(this.level);
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO FACTORY CONTROLLER // Emergency shutdown broadcast to " + affected + " linked machines."));
      return affected > 0;
   }

   public boolean serviceWithWrench(Player player) {
      if (player.isShiftKeyDown() && this.cycleScrubberMode(player)) {
         return true;
      }
      if (player.isShiftKeyDown()) {
         return this.cycleSideConfig(player);
      }
      if (this.heat <= 0) {
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.kind().displayName() + " maintenance check clean. No heat stress detected."));
         return false;
      } else {
         int before = this.heat;
         this.reduceHeat(12);
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.kind().displayName() + " heat vented: " + before + "% -> " + this.heat + "%."));
         return true;
      }
   }

   public boolean applyEmergencyCoolant() {
      if (this.heat <= 0) {
         return false;
      } else {
         this.reduceHeat(60);
         this.status = IndustrialMachineBlockEntity.MachineStatus.EMERGENCY_SHUTDOWN;
         return true;
      }
   }

   public IndustrialMachineBlock.MachineKind kind() {
      return this.getBlockState().getBlock() instanceof IndustrialMachineBlock machineBlock
         ? machineBlock.kind()
         : IndustrialMachineBlock.MachineKind.ORE_GRINDER;
   }

   protected Component getDefaultName() {
      return Component.literal("ECHO " + this.kind().displayName());
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


   public int[] getSlotsForFace(Direction side) {
      SideConfig config = SideConfig.byId(this.sideConfig);
      return switch (config) {
         case LOCKED -> new int[0];
         case INPUT_ONLY -> side == Direction.DOWN ? new int[0] : INPUT_AND_AUX_SLOTS;
         case OUTPUT_ONLY -> OUTPUT_SLOTS;
         case UPGRADES -> side == Direction.DOWN ? OUTPUT_SLOTS : UPGRADE_SLOTS;
         case STANDARD -> side == Direction.DOWN ? OUTPUT_SLOTS : side == Direction.UP ? INPUT_SLOTS : ALL_AUTOMATION_INPUT_SLOTS;
      };
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      return switch (slot) {
         case INPUT_SLOT -> !isUpgrade(stack) && this.canInsertIntoSlot(INPUT_SLOT, stack);
         case AUX_SLOT -> !isUpgrade(stack) && this.canInsertIntoSlot(slot, stack);
         case UPGRADE_SLOT_START, 5, 6, 7, 8 -> isUpgrade(stack) && this.findUpgradeSlot(stack) >= 0;
         default -> false;
      };
   }

   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
      return this.canPlaceItem(slot, stack) && SideConfig.byId(this.sideConfig) != SideConfig.OUTPUT_ONLY;
   }

   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
      SideConfig config = SideConfig.byId(this.sideConfig);
      return config != SideConfig.INPUT_ONLY && (slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT);
   }

   public int heatLevel() {
      return this.heat;
   }

   public boolean remoteShutdown() {
      return this.remoteShutdown;
   }

   public int scrubberModeId() {
      return this.scrubberMode;
   }

   public int sideConfigId() {
      return this.sideConfig;
   }

   public int alertCountForTelemetry() {
      return this.alertCount();
   }

   public int progressTicks() {
      return this.progress;
   }

   public int maxProgressTicks() {
      return this.maxProgress;
   }

   public IndustrialMachineBlockEntity.MachineStatus machineStatus() {
      return this.status;
   }

   public String statusLabel() {
      return this.status.label();
   }

   protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new IndustrialMachineMenu(containerId, inventory, this, this.data);
   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.getBlockPos());
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, this.items);
      this.thermalFlux = input.getIntOr("thermal_flux", 0);
      this.progress = input.getIntOr("progress", 0);
      this.maxProgress = input.getIntOr("max_progress", 0);
      this.heat = input.getIntOr("heat", 0);
      this.burnTime = input.getIntOr("burn_time", 0);
      this.scrubberMode = input.getIntOr("scrubber_mode", 0);
      this.sideConfig = input.getIntOr("side_config", 0);
      this.remoteShutdown = input.getBooleanOr("remote_shutdown", false);
      this.meltdownCooldown = input.getIntOr("meltdown_cooldown", 0);
      this.inputFluidId = input.getIntOr("input_fluid_id", 0);
      this.inputFluidAmount = input.getIntOr("input_fluid_amount", 0);
      this.outputFluidId = input.getIntOr("output_fluid_id", 0);
      this.outputFluidAmount = input.getIntOr("output_fluid_amount", 0);
      this.linkedCount = input.getIntOr("linked_count", 0);
      this.controllerAlerts = input.getIntOr("controller_alerts", 0);
      this.cachedControllerDucts = input.getIntOr("cached_controller_ducts", 0);
      this.cachedControllerSuppliers = input.getIntOr("cached_controller_suppliers", 0);
      this.cachedControllerReceivers = input.getIntOr("cached_controller_receivers", 0);
      this.cachedControllerStoredFlux = input.getIntOr("cached_controller_stored_flux", 0);
      this.cachedControllerCapacity = input.getIntOr("cached_controller_capacity", 0);
      this.status = IndustrialMachineBlockEntity.MachineStatus.byId(input.getIntOr("status", IndustrialMachineBlockEntity.MachineStatus.IDLE.ordinal()));
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.items);
      output.putInt("thermal_flux", this.thermalFlux);
      output.putInt("progress", this.progress);
      output.putInt("max_progress", this.maxProgress);
      output.putInt("heat", this.heat);
      output.putInt("burn_time", this.burnTime);
      output.putInt("scrubber_mode", this.scrubberMode);
      output.putInt("side_config", this.sideConfig);
      output.putBoolean("remote_shutdown", this.remoteShutdown);
      output.putInt("meltdown_cooldown", this.meltdownCooldown);
      output.putInt("input_fluid_id", this.inputFluidId);
      output.putInt("input_fluid_amount", this.inputFluidAmount);
      output.putInt("output_fluid_id", this.outputFluidId);
      output.putInt("output_fluid_amount", this.outputFluidAmount);
      output.putInt("linked_count", this.linkedCount);
      output.putInt("controller_alerts", this.controllerAlerts);
      output.putInt("cached_controller_ducts", this.cachedControllerDucts);
      output.putInt("cached_controller_suppliers", this.cachedControllerSuppliers);
      output.putInt("cached_controller_receivers", this.cachedControllerReceivers);
      output.putInt("cached_controller_stored_flux", this.cachedControllerStoredFlux);
      output.putInt("cached_controller_capacity", this.cachedControllerCapacity);
      output.putInt("status", this.status.ordinal());
   }

   @Override
   public int getFluxStored() {
      return this.thermalFlux;
   }

   @Override
   public int getMaxFluxStored() {
      return switch (this.kind()) {
         case REINFORCED_CAPACITOR -> Config.CAPACITOR_FLUX_CAPACITY.get() * 2;
         case STABILIZED_FLUX_BANK -> Config.CAPACITOR_FLUX_CAPACITY.get() * 4;
         case HYBRID_FLUX_BANK -> Config.CAPACITOR_FLUX_CAPACITY.get() * 6;
         case CORE_FLUX_BANK -> Config.CAPACITOR_FLUX_CAPACITY.get() * 10;
         default -> this.kind().storesFlux() ? (Integer)Config.CAPACITOR_FLUX_CAPACITY.get() : (Integer)Config.PROCESSOR_FLUX_CAPACITY.get();
      };
   }

   @Override
   public int receiveFlux(int amount, boolean simulate) {
      if (this.canReceive() && amount > 0) {
         int received = Math.min(amount, this.getMaxFluxStored() - this.thermalFlux);
         if (received > 0 && !simulate) {
            this.thermalFlux += received;
            this.setChanged();
         }

         return received;
      } else {
         return 0;
      }
   }

   private int generateFlux(int amount) {
      if (amount <= 0) {
         return 0;
      }
      int generated = Math.min(amount, this.getMaxFluxStored() - this.thermalFlux);
      if (generated > 0) {
         this.thermalFlux += generated;
         this.setChanged();
      }
      return generated;
   }

   @Override
   public int extractFlux(int amount, boolean simulate) {
      if (this.canExtract() && amount > 0) {
         int extracted = Math.min(amount, this.thermalFlux);
         if (extracted > 0 && !simulate) {
            this.thermalFlux -= extracted;
            this.setChanged();
         }

         return extracted;
      } else {
         return 0;
      }
   }

   @Override
   public boolean canReceive() {
      return !this.kind().generator();
   }

   @Override
   public boolean canExtract() {
      return (this.kind().generator() || this.kind().storesFlux()) && this.thermalFlux > 0;
   }

   public static enum MachineStatus {
      IDLE("Idle"),
      GENERATING("Generating"),
      STORED("Stored"),
      PROCESSING("Processing"),
      HOT_PROCESSING("Hot"),
      SCRUBBING("Scrubbing"),
      CHARGING("Power low"),
      OUTPUT_BLOCKED("Output blocked"),
      BAD_INPUT("Input rejected"),
      CATALYST_REQUIRED("Catalyst required"),
      COMPLETE("Complete"),
      CRITICAL_HEAT("Thermal load critical"),
      MELTDOWN("Meltdown vented"),
      NEXUS_CONTAMINATION("Nexus contamination"),
      EMERGENCY_SHUTDOWN("Emergency shutdown"),
      CONTROLLING("Factory network linked"),
      FLUID_REQUIRED("Fluid required"),
      FLUID_OUTPUT_BLOCKED("Fluid output blocked"),
      REMOTE_SHUTDOWN("Remote shutdown");

      private static final IndustrialMachineBlockEntity.MachineStatus[] BY_ID = values();
      private final String label;

      private MachineStatus(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }

      public static IndustrialMachineBlockEntity.MachineStatus byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : IDLE;
      }
   }

   private final class MachineFluidHandler implements EchoBackendFluidBridge.EchoFluidHandlerDelegate {
      public int size() {
         return 2;
      }

      public Object getResource(int slot) {
         if (slot == 0 && IndustrialMachineBlockEntity.this.inputFluidAmount > 0) {
            return ModFluids.resourceFor(IndustrialMachineBlockEntity.this.inputFluidId);
         }
         if (slot == 1 && IndustrialMachineBlockEntity.this.outputFluidAmount > 0) {
            return ModFluids.resourceFor(IndustrialMachineBlockEntity.this.outputFluidId);
         }
         return EchoBackendFluidBridge.emptyFluidResource();
      }

      public long getAmountAsLong(int slot) {
         return switch (slot) {
            case 0 -> IndustrialMachineBlockEntity.this.inputFluidAmount;
            case 1 -> IndustrialMachineBlockEntity.this.outputFluidAmount;
            default -> 0L;
         };
      }

      public long getCapacityAsLong(int slot, Object resource) {
         return slot == 0 || slot == 1 ? FLUID_TANK_CAPACITY : 0L;
      }

      public boolean isValid(int slot, Object resource) {
         int id = ModFluids.idFor(resource);
         if (slot == 0) {
            return id > 0 && (IndustrialMachineBlockEntity.this.inputFluidId == FLUID_NONE || IndustrialMachineBlockEntity.this.inputFluidId == id);
         }
      return slot == 1 && id > 0 && IndustrialMachineBlockEntity.this.outputFluidId == id;
      }

      public int insert(int slot, Object resource, int maxAmount, Object transaction) {
         if (slot != 0) {
            return 0;
         }
         int id = ModFluids.idFor(resource);
         int fill = Math.max(0, Math.min(maxAmount, FLUID_TANK_CAPACITY - IndustrialMachineBlockEntity.this.inputFluidAmount));
         if (id <= 0 || fill <= 0 || (IndustrialMachineBlockEntity.this.inputFluidId != FLUID_NONE && IndustrialMachineBlockEntity.this.inputFluidId != id)) {
            return 0;
         }
         if (transaction != null) {
            EchoBackendFluidBridge.updateSnapshots(IndustrialMachineBlockEntity.this.fluidSnapshots, transaction);
         }
         return IndustrialMachineBlockEntity.this.fillInputFluid(id, fill);
      }

      public int extract(int slot, Object resource, int maxAmount, Object transaction) {
         if (slot != 1) {
            return 0;
         }
         int id = ModFluids.idFor(resource);
         if (id <= 0 || id != IndustrialMachineBlockEntity.this.outputFluidId || IndustrialMachineBlockEntity.this.outputFluidAmount <= 0) {
            return 0;
         }
         if (transaction != null) {
            EchoBackendFluidBridge.updateSnapshots(IndustrialMachineBlockEntity.this.fluidSnapshots, transaction);
         }
         return IndustrialMachineBlockEntity.this.drainOutputFluid(id, maxAmount);
      }
   }

   private record FluidSnapshot(int inputFluidId, int inputFluidAmount, int outputFluidId, int outputFluidAmount) {
   }

   private static enum SideConfig {
      STANDARD("Standard"),
      INPUT_ONLY("Input only"),
      OUTPUT_ONLY("Output only"),
      UPGRADES("Upgrade bay"),
      LOCKED("Locked");

      private static final SideConfig[] BY_ID = values();
      private final String label;

      SideConfig(String label) {
         this.label = label;
      }

      String label() {
         return this.label;
      }

      static SideConfig byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : STANDARD;
      }
   }

   private static enum ScrubberMode {
      AIR("Air Mode"),
      RADIATION("Radiation Mode"),
      BLIGHT("Blight Mode"),
      STATION("Station Mode"),
      COOLING("Cooling Mode");

      private static final ScrubberMode[] BY_ID = values();
      private final String label;

      ScrubberMode(String label) {
         this.label = label;
      }

      String label() {
         return this.label;
      }

      static ScrubberMode byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : AIR;
      }
   }
}

package com.knoxhack.echoagriculturereclamation.block.entity;

import com.knoxhack.echoagriculturereclamation.block.ReclamationMachineBlock;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.ReclamationProcessDefinition;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.menu.ReclamationMachineMenu;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationRestoration;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ReclamationMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   public static final int INPUT_SLOT = 0;
   public static final int CATALYST_SLOT = 1;
   public static final int OUTPUT_SLOT = 2;
   public static final int AUX_SLOT = 3;
   public static final int SLOT_COUNT = 4;
   public static final int DATA_KIND = 0;
   public static final int DATA_PROGRESS = 1;
   public static final int DATA_PROGRESS_MAX = 2;
   public static final int DATA_STATUS = 3;
   public static final int DATA_POWERED = 4;
   public static final int DATA_OUTPUT_COUNT = 5;
   public static final int DATA_COUNT = 6;
   public static final int STATUS_READY = 0;
   public static final int STATUS_ACTIVE = 1;
   public static final int STATUS_BLOCKED = 2;
   public static final int STATUS_COMPLETE = 3;
   private static final int[] SLOTS = {INPUT_SLOT, CATALYST_SLOT, OUTPUT_SLOT, AUX_SLOT};

   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case DATA_KIND -> machineKind().ordinal();
            case DATA_PROGRESS -> progress;
            case DATA_PROGRESS_MAX -> progressMax;
            case DATA_STATUS -> statusCode();
            case DATA_POWERED -> powered() ? 1 : 0;
            case DATA_OUTPUT_COUNT -> items.get(OUTPUT_SLOT).getCount();
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
   private String activeProcessId = "";
   private String lastOperation = "";
   private int progress;
   private int progressMax;
   private String lastResult = "";
   private String blockedReason = "";
   private String nextAction = "Insert input, then run the machine.";
   private long updatedGameTime;
   private String operatorUuid = "";

   public ReclamationMachineBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.MACHINE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ReclamationMachineBlockEntity machine) {
      if (level.isClientSide()) {
         return;
      }
      if (machine.progress <= 0 || machine.progressMax <= 0) {
         return;
      }
      if (!machine.canCompleteActiveProcess()) {
         machine.block("Process paused: input, catalyst, output, or field state changed.");
         machine.clearActiveProcess();
         return;
      }
      machine.progress++;
      if (machine.progress >= machine.progressMax) {
         machine.completeActiveProcess();
      } else {
         machine.sync();
      }
   }

   public ReclamationMachineBlock.MachineKind machineKind() {
      return getBlockState().getBlock() instanceof ReclamationMachineBlock machine
         ? machine.kind()
         : ReclamationMachineBlock.MachineKind.ECOLOGY_SCANNER;
   }

   public boolean handleMenuButton(Player player, int id) {
      if (id == ReclamationMachineMenu.BUTTON_SCAN) {
         inspect(player, true);
         return true;
      }
      if (id == ReclamationMachineMenu.BUTTON_RUN) {
         return startProcess(player);
      }
      if (id == ReclamationMachineMenu.BUTTON_RECALL && machineKind() == ReclamationMachineBlock.MachineKind.POLLINATOR_DRONE_DOCK) {
         recallDrones(player);
         return true;
      }
      return false;
   }

   public boolean quickUse(Player player, ItemStack held) {
      if (held != null && !held.isEmpty()) {
         if (insertQuickInput(player, held)) {
            return startProcess(player);
         }
         inspect(player, true);
         return false;
      }
      inspect(player, true);
      return true;
   }

   public void recordOperation(String operation, int progressMax, String result, String blockedReason) {
      this.lastOperation = clean(operation);
      this.progressMax = Math.max(0, progressMax);
      this.progress = this.progressMax;
      this.lastResult = clean(result);
      this.blockedReason = clean(blockedReason);
      this.nextAction = this.blockedReason.isBlank() ? defaultNextAction() : this.blockedReason;
      this.updatedGameTime = level == null ? 0L : level.getGameTime();
      sync();
   }

   public boolean startProcess(Player player) {
      if (progress > 0 && progress < progressMax) {
         message(player, "Process already running: " + processTitle() + ".");
         return true;
      }
      ReclamationMachineBlock.MachineKind kind = machineKind();
      if (kind == ReclamationMachineBlock.MachineKind.GREENHOUSE_CONTROLLER) {
         scanGreenhouse(player);
         return true;
      }
      if (kind == ReclamationMachineBlock.MachineKind.ECOLOGY_SCANNER) {
         scanEcology(player);
         return true;
      }
      if (kind == ReclamationMachineBlock.MachineKind.POLLINATOR_DRONE_DOCK) {
         deployDrone(player);
         return true;
      }
      if (kind == ReclamationMachineBlock.MachineKind.SPORE_FILTER) {
         inspect(player, true);
         complete("Spore Filter ready: greenhouse scans count this block as filter support.");
         return true;
      }
      String blocker = validateProcess(kind);
      if (!blocker.isBlank()) {
         block(blocker);
         message(player, "Blocked: " + blocker);
         return false;
      }
      ReclamationProcessDefinition process = processFor(kind);
      activeProcessId = process.id();
      lastOperation = "process/" + activeProcessId;
      progress = 1;
      progressMax = Math.max(10, Math.max(20, process.ticks()) / Math.max(1, ReclamationCrossAddonIntegration.poweredThroughputDivisor(level, worldPosition)));
      blockedReason = "";
      lastResult = "";
      nextAction = "Processing " + process.title() + ".";
      operatorUuid = player == null ? "" : player.getUUID().toString();
      updatedGameTime = level == null ? 0L : level.getGameTime();
      message(player, "Started " + process.title() + ".");
      sync();
      return true;
   }

   public boolean isOperationActive() {
      return progress > 0 && progress < progressMax;
   }

   public boolean isProtectedOperationSlot(int slot) {
      return isOperationActive() && (slot == INPUT_SLOT || slot == CATALYST_SLOT);
   }

   public ContainerData data() {
      return data;
   }

   public String activeProcessId() {
      return activeProcessId;
   }

   public String processTitle() {
      if (!activeProcessId.isBlank()) {
         return ReclamationContent.processes().getOrDefault(activeProcessId, processFor(machineKind())).title();
      }
      return processFor(machineKind()).title();
   }

   public String lastOperation() {
      return lastOperation;
   }

   public int progress() {
      return progress;
   }

   public int progressMax() {
      return progressMax;
   }

   public String lastResult() {
      return lastResult;
   }

   public String blockedReason() {
      return blockedReason;
   }

   public String nextAction() {
      return nextAction == null || nextAction.isBlank() ? defaultNextAction() : nextAction;
   }

   public long updatedGameTime() {
      return updatedGameTime;
   }

   public String statusLine() {
      if (!blockedReason.isBlank()) {
         return blockedReason;
      }
      if (isOperationActive()) {
         return processTitle() + " " + progress + "/" + progressMax;
      }
      if (!lastResult.isBlank()) {
         return lastResult;
      }
      return defaultNextAction();
   }

   public String slotSummary() {
      return "input=" + itemLabel(items.get(INPUT_SLOT))
         + ", catalyst=" + itemLabel(items.get(CATALYST_SLOT))
         + ", output=" + itemLabel(items.get(OUTPUT_SLOT))
         + ", aux=" + itemLabel(items.get(AUX_SLOT));
   }

   @Override
   protected Component getDefaultName() {
      return Component.literal("ECHO " + machineKind().displayName());
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> replacement) {
      for (int index = 0; index < Math.min(items.size(), replacement.size()); index++) {
         items.set(index, replacement.get(index));
      }
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (slot == OUTPUT_SLOT) {
         return false;
      }
      ReclamationMachineBlock.MachineKind kind = machineKind();
      if (slot == CATALYST_SLOT) {
         return kind == ReclamationMachineBlock.MachineKind.GENE_STABILIZER
            && (stack.is(ModItems.GENE_SAMPLE.get()) || stack.is(ModItems.BIO_GEL.get()));
      }
      if (slot == AUX_SLOT) {
         return false;
      }
      return switch (kind) {
         case SEED_VAULT_TERMINAL -> stack.is(ModItems.RECOVERED_SEED_CAPSULE.get());
         case SOIL_PURIFIER -> stack.is(ModItems.PURIFICATION_ENZYME.get()) || stack.is(ModItems.SOIL_NUTRIENT_MIX.get());
         case GENE_STABILIZER -> stack.is(ModItems.CONTAMINATED_SEED.get()) && stack.get(ModItems.seedProfileComponent()) != null;
         case BIO_REACTOR -> stack.is(ModItems.GENE_SAMPLE.get()) || isOrganic(stack);
         case COMPOST_RECYCLER -> isOrganic(stack);
         default -> false;
      };
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
      return canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
      return slot == OUTPUT_SLOT || !isProtectedOperationSlot(slot);
   }

   @Override
   protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new ReclamationMachineMenu(containerId, inventory, this, data);
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(getBlockPos());
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      activeProcessId = input.getStringOr("activeProcessId", "");
      lastOperation = input.getStringOr("lastOperation", "");
      progress = Math.max(0, input.getIntOr("progress", 0));
      progressMax = Math.max(0, input.getIntOr("progressMax", 0));
      lastResult = input.getStringOr("lastResult", "");
      blockedReason = input.getStringOr("blockedReason", "");
      nextAction = input.getStringOr("nextAction", "Insert input, then run the machine.");
      updatedGameTime = Math.max(0L, input.getLongOr("updatedGameTime", 0L));
      operatorUuid = input.getStringOr("operatorUuid", "");
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, items);
      output.putString("activeProcessId", activeProcessId);
      output.putString("lastOperation", lastOperation);
      output.putInt("progress", progress);
      output.putInt("progressMax", progressMax);
      output.putString("lastResult", lastResult);
      output.putString("blockedReason", blockedReason);
      output.putString("nextAction", nextAction == null ? "" : nextAction);
      output.putLong("updatedGameTime", updatedGameTime);
      output.putString("operatorUuid", operatorUuid == null ? "" : operatorUuid);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return saveWithoutMetadata(registries);
   }

   private boolean insertQuickInput(Player player, ItemStack held) {
      int slot = canPlaceItem(INPUT_SLOT, held) ? INPUT_SLOT : canPlaceItem(CATALYST_SLOT, held) ? CATALYST_SLOT : -1;
      if (slot < 0) {
         block(machineKind().displayName() + " does not accept " + held.getHoverName().getString() + ".");
         return false;
      }
      ItemStack current = items.get(slot);
      if (!current.isEmpty() && (!ItemStack.isSameItemSameComponents(current, held) || current.getCount() >= current.getMaxStackSize())) {
         block(slot == INPUT_SLOT ? "Input slot is occupied." : "Catalyst slot is occupied.");
         return false;
      }
      ItemStack inserted = held.copyWithCount(1);
      if (current.isEmpty()) {
         items.set(slot, inserted);
      } else {
         current.grow(1);
      }
      if (player == null || !player.getAbilities().instabuild) {
         held.shrink(1);
      }
      complete("Loaded " + inserted.getHoverName().getString() + " into " + machineKind().displayName() + ".");
      return true;
   }

   private boolean canCompleteActiveProcess() {
      return validateProcess(machineKind()).isBlank();
   }

   private void completeActiveProcess() {
      Player player = operator();
      ReclamationMachineBlock.MachineKind kind = machineKind();
      boolean complete = switch (kind) {
         case SEED_VAULT_TERMINAL -> completeSeedVault(player);
         case SOIL_PURIFIER -> completeSoilPurifier(player);
         case GENE_STABILIZER -> completeGeneStabilizer(player);
         case BIO_REACTOR -> completeBioReactor(player);
         case COMPOST_RECYCLER -> completeCompostRecycler(player);
         default -> false;
      };
      if (complete) {
         ReclamationCrossAddonIntegration.playFieldCue(player, "complete");
      }
      clearActiveProcess();
      sync();
   }

   private boolean completeSeedVault(Player player) {
      ItemStack input = items.get(INPUT_SLOT);
      CropSpec spec = ReclamationCrossAddonIntegration.recoveredCrop(player, level.getRandom());
      SeedProfile profile = ReclamationCrossAddonIntegration.recoveredProfile(player, spec, level.getRandom());
      ItemStack seed = new ItemStack(ModItems.CONTAMINATED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), profile);
      if (!insertOutput(seed)) {
         block("Output slot is blocked.");
         return false;
      }
      input.shrink(1);
      if (player != null) {
         ReclamationProgress.discoverSeed(player, spec);
      }
      complete("Seed identified: " + spec.displayName() + " contamination " + profile.contaminationTier() + ", stability " + profile.stability() + "%.");
      return true;
   }

   private boolean completeSoilPurifier(Player player) {
      ItemStack input = items.get(INPUT_SLOT);
      boolean enzyme = input.is(ModItems.PURIFICATION_ENZYME.get());
      int changed = ReclamationRestoration.purifyArea(
         level,
         worldPosition,
         ReclamationContent.machines().soilPurifierRadius(),
         enzyme ? ReclamationContent.machines().soilPurifierEnzymeBlocks() : ReclamationContent.machines().soilPurifierNutrientBlocks()
      );
      if (changed <= 0) {
         block("No dead, contaminated, irradiated, or toxic reclamation soil in range.");
         return false;
      }
      input.shrink(1);
      if (player != null) {
         ReclamationProgress.mark(player, "soil_analyzed");
         ReclamationProgress.add(player, "soil_purified", changed);
      }
      complete("Soil purification converted " + changed + " blocks.");
      return true;
   }

   private boolean completeGeneStabilizer(Player player) {
      ItemStack input = items.get(INPUT_SLOT);
      ItemStack catalyst = items.get(CATALYST_SLOT);
      SeedProfile profile = input.get(ModItems.seedProfileComponent());
      if (profile == null) {
         block("Input seed lost its profile.");
         return false;
      }
      ItemStack stable = new ItemStack(ModItems.STABILIZED_SEED.get());
      stable.set(ModItems.seedProfileComponent(), profile.stabilized());
      if (!insertOutput(stable)) {
         block("Output slot is blocked.");
         return false;
      }
      input.shrink(1);
      catalyst.shrink(1);
      if (player != null) {
         ReclamationProgress.recordStabilization(player);
      }
      complete(profile.spec().displayName() + " seed stabilized.");
      return true;
   }

   private boolean completeBioReactor(Player player) {
      ItemStack input = items.get(INPUT_SLOT);
      MachineOutput output = bioReactorOutput(input);
      if (!insertOutputs(output.stacks())) {
         block("Output slot is blocked.");
         return false;
      }
      input.shrink(1);
      if (player != null) {
         if (output.bioGel() > 0) {
            ReclamationProgress.add(player, "bio_gel_created", output.bioGel());
         }
         ReclamationProgress.mark(player, "bio_reactor_online");
      }
      complete("Bio-Reactor processed " + output.inputName() + " into " + describe(output.stacks()) + ".");
      return true;
   }

   private boolean completeCompostRecycler(Player player) {
      ItemStack input = items.get(INPUT_SLOT);
      MachineOutput output = compostRecyclerOutput(input);
      if (!insertOutputs(output.stacks())) {
         block("Output slot is blocked.");
         return false;
      }
      input.shrink(1);
      if (player != null) {
         if (output.nutrientMix() > 0) {
            ReclamationProgress.add(player, "nutrient_mix_created", output.nutrientMix());
         }
         ReclamationProgress.mark(player, "compost_recycler_online");
      }
      complete("Compost Recycler processed " + output.inputName() + " into " + describe(output.stacks()) + ".");
      return true;
   }

   private String validateProcess(ReclamationMachineBlock.MachineKind kind) {
      if (level == null) {
         return "Machine is not attached to a level.";
      }
      ItemStack input = items.get(INPUT_SLOT);
      ItemStack catalyst = items.get(CATALYST_SLOT);
      return switch (kind) {
         case SEED_VAULT_TERMINAL -> input.is(ModItems.RECOVERED_SEED_CAPSULE.get())
            ? outputCanFit(new ItemStack(ModItems.CONTAMINATED_SEED.get())) ? "" : "Output slot is blocked."
            : "Insert a Recovered Seed Capsule.";
         case SOIL_PURIFIER -> input.is(ModItems.PURIFICATION_ENZYME.get()) || input.is(ModItems.SOIL_NUTRIENT_MIX.get())
            ? "" : "Insert Purification Enzyme or Soil Nutrient Mix.";
         case GENE_STABILIZER -> {
            if (!input.is(ModItems.CONTAMINATED_SEED.get()) || input.get(ModItems.seedProfileComponent()) == null) {
               yield "Insert a profiled Contaminated Seed.";
            }
            if (!catalyst.is(ModItems.GENE_SAMPLE.get()) && !catalyst.is(ModItems.BIO_GEL.get())) {
               yield "Insert Gene Sample or Bio-Gel in the catalyst slot.";
            }
            ItemStack stable = new ItemStack(ModItems.STABILIZED_SEED.get());
            SeedProfile profile = input.get(ModItems.seedProfileComponent());
            if (profile != null) {
               stable.set(ModItems.seedProfileComponent(), profile.stabilized());
            }
            yield outputCanFit(stable) ? "" : "Output slot is blocked.";
         }
         case BIO_REACTOR -> input.is(ModItems.GENE_SAMPLE.get()) || isOrganic(input)
            ? outputCanFit(bioReactorOutput(input).stacks()) ? "" : "Output slot is blocked."
            : "Insert crop matter, seed biomass, or Gene Sample.";
         case COMPOST_RECYCLER -> isOrganic(input)
            ? outputCanFit(compostRecyclerOutput(input).stacks()) ? "" : "Output slot is blocked."
            : "Insert crop matter or seed biomass.";
         default -> "";
      };
   }

   private void inspect(Player player, boolean chat) {
      String line = switch (machineKind()) {
         case GREENHOUSE_CONTROLLER -> greenhouseLine(player);
         case ECOLOGY_SCANNER -> ecologyLine(player);
         case POLLINATOR_DRONE_DOCK -> dockLine(player);
         case SPORE_FILTER -> "Spore Filter installed. It improves greenhouse scans when placed inside a field envelope.";
         default -> statusLine() + " | " + slotSummary();
      };
      lastOperation = "inspect/" + machineKind().getSerializedName();
      lastResult = line;
      blockedReason = "";
      nextAction = defaultNextAction();
      updatedGameTime = level == null ? 0L : level.getGameTime();
      if (chat) {
         message(player, line);
      }
      sync();
   }

   private String greenhouseLine(Player player) {
      ReclamationProgress.GreenhouseScan scan = ReclamationProgress.scanGreenhouse(level, worldPosition);
      ReclamationProgress.GreenhouseContext context = scan.asContext();
      if (level instanceof ServerLevel serverLevel) {
         ReclamationProgress.recordGreenhouseZone(serverLevel, worldPosition, scan);
         context = ReclamationProgress.greenhouseContext(serverLevel, worldPosition);
      }
      if (player != null) {
         ReclamationProgress.max(player, "greenhouse_safety", context.score());
         if (context.score() >= ReclamationContent.progression().greenhouseSafeThreshold()) {
            ReclamationProgress.mark(player, "greenhouse_online");
         }
      }
      nextAction = context.nextAction();
      return "Greenhouse " + context.summaryLabel() + " safety " + context.score() + "/100, "
         + countLabel(scan.activeDocks(), "active dock") + ", " + countLabel(scan.idleDocks(), "idle dock") + ", "
         + countLabel(scan.serviceTargets(), "service target") + ". " + context.nextAction();
   }

   private String ecologyLine(Player player) {
      ReclamationProgress.GreenhouseContext greenhouse = ReclamationProgress.greenhouseContext(level, worldPosition);
      if (player != null) {
         ReclamationProgress.mark(player, "soil_analyzed");
      }
      if (level instanceof ServerLevel serverLevel) {
         ReclamationRestoration.scanPulse(serverLevel, worldPosition, player, greenhouse);
      }
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      nextAction = greenhouse.nextAction();
      return "Soil " + metrics.soilLabel() + ", greenhouse " + greenhouse.summaryLabel() + ", restoration "
         + metrics.restorationScore() + "/" + ReclamationContent.progression().restoreThreshold() + ". " + greenhouse.nextAction();
   }

   private String dockLine(Player player) {
      int targets = ReclamationProgress.pollinationTargets(level, worldPosition);
      int serviceTargets = ReclamationProgress.pollinationServiceTargets(level, worldPosition);
      ReclamationProgress.GreenhouseContext context = ReclamationProgress.greenhouseContext(level, worldPosition);
      nextAction = context.nextAction();
      return "Pollinator dock " + (targets > 0 ? "active" : "idle") + ": "
         + countLabel(targets, "target") + ", " + countLabel(serviceTargets, "service target")
         + ", greenhouse " + context.summaryLabel() + ".";
   }

   private void scanGreenhouse(Player player) {
      complete(greenhouseLine(player));
      message(player, lastResult);
   }

   private void scanEcology(Player player) {
      complete(ecologyLine(player));
      message(player, lastResult);
   }

   private void deployDrone(Player player) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      PollinatorDroneEntityAccess.deploy(serverLevel, worldPosition);
      complete(dockLine(player) + " Drone deployed or already bound.");
      message(player, lastResult);
   }

   private void recallDrones(Player player) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      int recalled = PollinatorDroneEntityAccess.recall(serverLevel, worldPosition);
      complete("Pollinator dock recall: " + countLabel(recalled, "drone") + " recalled.");
      message(player, lastResult);
   }

   private boolean insertOutputs(List<ItemStack> outputs) {
      for (ItemStack stack : outputs) {
         if (!outputCanFit(stack)) {
            return false;
         }
      }
      for (ItemStack stack : outputs) {
         if (!insertOutput(stack.copy())) {
            return false;
         }
      }
      return true;
   }

   private boolean insertOutput(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      }
      ItemStack current = items.get(OUTPUT_SLOT);
      if (current.isEmpty()) {
         items.set(OUTPUT_SLOT, stack.copy());
         return true;
      }
      if (!ItemStack.isSameItemSameComponents(current, stack)) {
         return false;
      }
      int move = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
      if (move <= 0) {
         return false;
      }
      current.grow(move);
      return move == stack.getCount();
   }

   private boolean outputCanFit(List<ItemStack> stacks) {
      ItemStack simulated = items.get(OUTPUT_SLOT).copy();
      for (ItemStack stack : stacks) {
         if (stack.isEmpty()) {
            continue;
         }
         if (simulated.isEmpty()) {
            simulated = stack.copy();
         } else if (!ItemStack.isSameItemSameComponents(simulated, stack) || simulated.getCount() + stack.getCount() > simulated.getMaxStackSize()) {
            return false;
         } else {
            simulated.grow(stack.getCount());
         }
      }
      return true;
   }

   private boolean outputCanFit(ItemStack stack) {
      return outputCanFit(List.of(stack));
   }

   private void complete(String result) {
      blockedReason = "";
      lastResult = clean(result);
      nextAction = defaultNextAction();
      updatedGameTime = level == null ? 0L : level.getGameTime();
      sync();
   }

   private void block(String reason) {
      blockedReason = clean(reason);
      lastResult = "";
      nextAction = blockedReason;
      updatedGameTime = level == null ? 0L : level.getGameTime();
      sync();
   }

   private void clearActiveProcess() {
      activeProcessId = "";
      progress = 0;
      progressMax = 0;
      operatorUuid = "";
   }

   private int statusCode() {
      if (!blockedReason.isBlank()) {
         return STATUS_BLOCKED;
      }
      if (isOperationActive()) {
         return STATUS_ACTIVE;
      }
      if (!lastResult.isBlank()) {
         return STATUS_COMPLETE;
      }
      return STATUS_READY;
   }

   private boolean powered() {
      return ReclamationCrossAddonIntegration.poweredThroughputDivisor(level, worldPosition) > 1;
   }

   private ReclamationProcessDefinition processFor(ReclamationMachineBlock.MachineKind kind) {
      String machine = kind.getSerializedName();
      return ReclamationContent.processes().values().stream()
         .filter(process -> process.machine().equals(machine))
         .findFirst()
         .orElseGet(() -> new ReclamationProcessDefinition("inspect_" + machine, machine, kind.displayName() + " Diagnostics", List.of(), List.of(), List.of(), 40, 0, List.of()));
   }

   private Player operator() {
      if (!(level instanceof ServerLevel serverLevel) || operatorUuid == null || operatorUuid.isBlank()) {
         return null;
      }
      try {
         UUID uuid = UUID.fromString(operatorUuid);
         return serverLevel.getServer().getPlayerList().getPlayer(uuid);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   private String defaultNextAction() {
      return switch (machineKind()) {
         case SEED_VAULT_TERMINAL -> "Insert a Recovered Seed Capsule and run Seed Vault Analysis.";
         case SOIL_PURIFIER -> "Insert Purification Enzyme or Soil Nutrient Mix, then run a local purification pass.";
         case GENE_STABILIZER -> "Insert a profiled Contaminated Seed plus Bio-Gel or Gene Sample.";
         case BIO_REACTOR -> "Insert crop matter, seed biomass, or Gene Sample to create Bio-Gel.";
         case COMPOST_RECYCLER -> "Insert crop matter or seed biomass to create Soil Nutrient Mix.";
         case GREENHOUSE_CONTROLLER -> "Run scan to save greenhouse quality and active blockers.";
         case POLLINATOR_DRONE_DOCK -> "Run to deploy a bound drone; recall removes bound drones.";
         case SPORE_FILTER -> "Place inside a greenhouse envelope for filter support.";
         case ECOLOGY_SCANNER -> "Run scan to pulse local restoration and report blockers.";
      };
   }

   private void sync() {
      setChanged();
      if (level != null && !level.isClientSide()) {
         BlockState state = getBlockState();
         level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
      }
   }

   private static boolean isOrganic(ItemStack stack) {
      return stack.is(ModItems.CONTAMINATED_SEED.get())
         || stack.is(ModItems.STABILIZED_SEED.get())
         || produceSpec(stack) != null;
   }

   private static MachineOutput bioReactorOutput(ItemStack stack) {
      int organic = ReclamationContent.machines().bioReactorOrganicOutput();
      if (stack.is(ModItems.GENE_SAMPLE.get())) {
         int amount = ReclamationContent.machines().bioReactorGeneSampleOutput();
         return MachineOutput.bio("Gene Sample", new ItemStack(ModItems.BIO_GEL.get(), amount), amount);
      }
      CropSpec spec = produceSpec(stack);
      if (spec == null) {
         return MachineOutput.bio("seed mass", new ItemStack(ModItems.BIO_GEL.get(), organic), organic);
      }
      return switch (spec.path()) {
         case "medicinal_aloe" -> MachineOutput.bio(spec.displayName(), listOf(new ItemStack(ModItems.BIO_GEL.get(), organic), optionalBridge("echoashfallprotocol", "bandage", 1)), organic);
         case "signal_fungus" -> {
            int amount = Math.max(organic + 1, 2);
            yield MachineOutput.bio(spec.displayName(), new ItemStack(ModItems.BIO_GEL.get(), amount), amount);
         }
         case "cryo_moss" -> MachineOutput.bio(spec.displayName(), listOf(new ItemStack(ModItems.BIO_GEL.get(), organic), new ItemStack(ModItems.PURIFICATION_ENZYME.get())), organic);
         case "nexus_orchid" -> MachineOutput.bio(spec.displayName(), listOf(new ItemStack(ModItems.BIO_GEL.get(), organic), new ItemStack(ModItems.GENE_SAMPLE.get()), optionalBridge("echonexusprotocol", "nexus_gel", 1)), organic);
         default -> MachineOutput.bio(spec.displayName(), new ItemStack(ModItems.BIO_GEL.get(), organic), organic);
      };
   }

   private static MachineOutput compostRecyclerOutput(ItemStack stack) {
      int compost = ReclamationContent.machines().compostRecyclerOutput();
      CropSpec spec = produceSpec(stack);
      if (spec == null) {
         return MachineOutput.nutrient("seed mass", new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), compost), compost);
      }
      return switch (spec.path()) {
         case "filter_reed" -> {
            int amount = Math.max(compost + 2, 3);
            yield MachineOutput.nutrient(spec.displayName(), listOf(new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), amount), optionalBridge("echoashfallprotocol", "plant_fiber", 1)), amount);
         }
         case "cryo_moss", "signal_fungus" -> {
            int amount = Math.max(compost + 1, 2);
            yield MachineOutput.nutrient(spec.displayName(), new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), amount), amount);
         }
         default -> MachineOutput.nutrient(spec.displayName(), new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), compost), compost);
      };
   }

   private static CropSpec produceSpec(ItemStack stack) {
      for (CropSpec spec : CropSpec.ALL) {
         if (stack.is(ModItems.produceFor(spec).get())) {
            return spec;
         }
      }
      return null;
   }

   private static ItemStack optionalBridge(String namespace, String path, int count) {
      if (count <= 0) {
         return ItemStack.EMPTY;
      }
      Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(namespace, path)).orElse(Items.AIR);
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
   }

   private static List<ItemStack> listOf(ItemStack... stacks) {
      List<ItemStack> result = new ArrayList<>();
      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            result.add(stack);
         }
      }
      return result;
   }

   private static String describe(List<ItemStack> stacks) {
      if (stacks.isEmpty()) {
         return "no usable output";
      }
      List<String> parts = new ArrayList<>();
      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            parts.add(stack.getCount() + "x " + stack.getHoverName().getString());
         }
      }
      return String.join(", ", parts);
   }

   private static String itemLabel(ItemStack stack) {
      return stack.isEmpty() ? "empty" : stack.getCount() + "x " + stack.getHoverName().getString();
   }

   private static String countLabel(int count, String noun) {
      return count + " " + noun + (count == 1 ? "" : "s");
   }

   private static String clean(String value) {
      return value == null ? "" : value.strip();
   }

   private static void message(Player player, String text) {
      if (player != null && text != null && !text.isBlank()) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // " + text));
      }
   }

   private record MachineOutput(String inputName, List<ItemStack> stacks, int bioGel, int nutrientMix) {
      private static MachineOutput bio(String inputName, ItemStack stack, int bioGel) {
         return bio(inputName, listOf(stack), bioGel);
      }

      private static MachineOutput bio(String inputName, List<ItemStack> stacks, int bioGel) {
         return new MachineOutput(inputName, stacks, bioGel, 0);
      }

      private static MachineOutput nutrient(String inputName, ItemStack stack, int nutrientMix) {
         return nutrient(inputName, listOf(stack), nutrientMix);
      }

      private static MachineOutput nutrient(String inputName, List<ItemStack> stacks, int nutrientMix) {
         return new MachineOutput(inputName, stacks, 0, nutrientMix);
      }
   }

   private static final class PollinatorDroneEntityAccess {
      private static void deploy(ServerLevel level, BlockPos dock) {
         com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity.deployOrFind(level, dock);
      }

      private static int recall(ServerLevel level, BlockPos dock) {
         return com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity.recallDrones(level, dock);
      }
   }
}

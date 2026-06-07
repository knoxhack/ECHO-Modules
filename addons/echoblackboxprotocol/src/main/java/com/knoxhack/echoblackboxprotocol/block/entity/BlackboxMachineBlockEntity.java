package com.knoxhack.echoblackboxprotocol.block.entity;

import com.knoxhack.echoblackboxprotocol.block.BlackboxMachineBlock;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.item.BlackboxFragmentItem;
import com.knoxhack.echoblackboxprotocol.item.EndingDirectiveItem;
import com.knoxhack.echoblackboxprotocol.menu.BlackboxMachineMenu;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEndings;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.recipe.BlackboxProcessingRecipe;
import com.knoxhack.echoblackboxprotocol.registry.ModBlockEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import com.knoxhack.echoblackboxprotocol.registry.ModRecipes;
import com.knoxhack.echoblackboxprotocol.world.BlackboxWorldData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlackboxMachineBlockEntity extends BaseContainerBlockEntity {
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   public static final int DATA_PROGRESS = 0;
   public static final int DATA_MAX_PROGRESS = 1;
   public static final int DATA_KIND = 2;
   public static final int DATA_STATUS = 3;
   public static final int DATA_COUNT = 4;
   public static final int DEFAULT_OPERATION_TICKS = 60;

   private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
   private final ContainerData data = new ContainerData() {
      public int get(int dataId) {
         return switch (dataId) {
            case DATA_PROGRESS -> BlackboxMachineBlockEntity.this.progress;
            case DATA_MAX_PROGRESS -> BlackboxMachineBlockEntity.this.maxProgress;
            case DATA_KIND -> BlackboxMachineBlockEntity.this.kind().ordinal();
            case DATA_STATUS -> BlackboxMachineBlockEntity.this.status.ordinal();
            default -> 0;
         };
      }

      public void set(int dataId, int value) {
         switch (dataId) {
            case DATA_PROGRESS -> BlackboxMachineBlockEntity.this.progress = Math.max(0, value);
            case DATA_MAX_PROGRESS -> BlackboxMachineBlockEntity.this.maxProgress = Math.max(0, value);
            case DATA_STATUS -> BlackboxMachineBlockEntity.this.status = MachineStatus.byId(value);
            default -> {
            }
         }
      }

      public int getCount() {
         return DATA_COUNT;
      }
   };
   private int progress;
   private int maxProgress;
   private MachineStatus status = MachineStatus.IDLE;
   private UUID activePlayer;
   private Player activePlayerEntity;
   private String lastMessage = "Awaiting input.";

   public BlackboxMachineBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.BLACKBOX_MACHINE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BlackboxMachineBlockEntity machine) {
      if (!level.isClientSide() && machine.status == MachineStatus.PROCESSING && level instanceof ServerLevel serverLevel) {
         machine.progress++;
         if (machine.progress >= Math.max(1, machine.maxProgress)) {
            machine.completeOperation(serverLevel);
         }

         machine.setChanged();
      }
   }

   public boolean startOperation(Player player) {
      if (this.status == MachineStatus.PROCESSING) {
         this.message(player, "Machine is already processing.");
         return false;
      }

      Operation operation = this.planOperation(player);
      if (!operation.accepted()) {
         this.reset(operation.status(), operation.message());
         this.message(player, operation.message());
         return false;
      }

      this.activePlayer = player instanceof ServerPlayer ? player.getUUID() : null;
      this.activePlayerEntity = player;
      this.progress = 0;
      this.maxProgress = operation.duration();
      this.status = MachineStatus.PROCESSING;
      this.lastMessage = operation.message();
      this.message(player, operation.message());
      this.setChanged();
      return true;
   }

   private Operation planOperation(Player player) {
      ItemStack input = this.items.get(INPUT_SLOT);
      BlackboxMachineKind kind = this.kind();
      if (!input.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         RecipeHolder<BlackboxProcessingRecipe> holder = findRecipe(serverLevel, kind, input);
         if (holder != null) {
            BlackboxProcessingRecipe recipe = holder.value();
            if (!this.canOutput(recipe.result())) {
               return Operation.rejected(MachineStatus.OUTPUT_BLOCKED, "Output slot is blocked.");
            }

            return Operation.accepted(Math.max(20, recipe.duration() / 4), "Processing " + input.getHoverName().getString() + ".");
         }
      }

      return switch (kind) {
         case BLACKBOX_DECODER -> input.getItem() instanceof BlackboxFragmentItem
            ? (
               this.canOutput(new ItemStack(ModItems.recordFor(((BlackboxFragmentItem)input.getItem()).type()).get()))
                  ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Decoding " + input.getHoverName().getString() + ".")
                  : Operation.rejected(MachineStatus.OUTPUT_BLOCKED, "Output slot is blocked.")
            )
            : Operation.rejected(MachineStatus.BAD_INPUT, "Decoder input requires a typed Blackbox Fragment.");
         case ARCHIVE_TERMINAL -> input.isEmpty()
            ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Archive Terminal route audit started.")
            : Operation.rejected(MachineStatus.BAD_INPUT, "Archive Terminal action does not use an input item.");
         case MEMORY_PROJECTOR -> input.isEmpty()
            ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Memory Projector alignment started.")
            : Operation.rejected(MachineStatus.BAD_INPUT, "Memory Projector action does not use an input item.");
         case CORE_KEY_ASSEMBLER -> input.isEmpty()
            ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Core Key proof checklist started.")
            : Operation.rejected(MachineStatus.BAD_INPUT, "Core Key Assembler input accepts Command Key matrix processing only.");
         case PROTOCOL_EXTRACTOR -> input.isEmpty()
            ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Protocol extraction handshake started.")
            : Operation.rejected(MachineStatus.BAD_INPUT, "Protocol Extractor input accepts Deleted Memory Records only.");
         case MEMORY_STABILIZER -> isStabilizerReagent(input) || player.hasInfiniteMaterials()
            ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Memory stabilizer cycle started.")
            : Operation.rejected(MachineStatus.BAD_INPUT, "Stabilizer input requires Static Fluid or the Memory Stabilizer Core.");
         case TRUTH_ENGINE -> input.getItem() instanceof EndingDirectiveItem directive
            ? (
               BlackboxEndings.eligible(player, BlackboxProgress.get(player), directive.ending())
                  ? Operation.accepted(DEFAULT_OPERATION_TICKS, "Truth Engine final choice commit started.")
                  : Operation.rejected(MachineStatus.LOCKED, "Truth Engine rejects this directive. Required proof is incomplete.")
            )
            : Operation.rejected(MachineStatus.BAD_INPUT, "Truth Engine input requires an ending directive.");
      };
   }

   private void completeOperation(ServerLevel level) {
      Player player = this.activePlayer == null ? this.activePlayerEntity : level.getServer().getPlayerList().getPlayer(this.activePlayer);
      if (player == null) {
         this.reset(MachineStatus.LOCKED, "Operation paused; operator signal lost.");
         return;
      }

      ItemStack input = this.items.get(INPUT_SLOT);
      RecipeHolder<BlackboxProcessingRecipe> holder = input.isEmpty() ? null : findRecipe(level, this.kind(), input);
      if (holder != null) {
         this.completeRecipe(player, holder.value(), level);
      } else {
         this.completeAction(player, level);
      }

      this.progress = 0;
      this.maxProgress = 0;
      this.activePlayer = null;
      this.activePlayerEntity = null;
      this.setChanged();
   }

   private void completeRecipe(Player player, BlackboxProcessingRecipe recipe, ServerLevel level) {
      ItemStack input = this.items.get(INPUT_SLOT);
      ItemStack output = recipe.result().copy();
      if (!this.canOutput(output)) {
         this.reset(MachineStatus.OUTPUT_BLOCKED, "Output slot is blocked.");
         this.message(player, this.lastMessage);
         return;
      }

      boolean retainProofKey = input.is(ModItems.COMMAND_KEY.get()) && recipe.resultItem() == ModItems.CORE_ACCESS_KEY_MATRIX.get();
      if (!player.hasInfiniteMaterials() && !retainProofKey) {
         input.shrink(1);
      }

      BlackboxProgress progressData = BlackboxProgress.get(player);
      int stabilityCost = scaledStabilityCost(player, recipe);
      progressData.stability(progressData.stability() - stabilityCost);
      progressData.falseSignals(worldEnding(player) == BlackboxEnding.MERGE ? 0 : progressData.falseSignalCount() + 1);
      if (worldEnding(player) == BlackboxEnding.CONTROL && !isUniqueResult(output.getItem())) {
         output.grow(1);
      }

      this.mergeOutput(output);
      this.reset(MachineStatus.COMPLETE, "Produced " + output.getHoverName().getString() + ". Stability " + progressData.stability() + "%.");
      this.pulse(level, ParticleTypes.ELECTRIC_SPARK, SoundEvents.AMETHYST_BLOCK_CHIME);
      this.message(player, this.lastMessage);
   }

   private void completeAction(Player player, ServerLevel level) {
      switch (this.kind()) {
         case BLACKBOX_DECODER -> this.completeDecode(player, level);
         case ARCHIVE_TERMINAL -> this.completeArchiveTerminal(player, level);
         case MEMORY_PROJECTOR -> this.completeMemoryProjector(player, level);
         case CORE_KEY_ASSEMBLER -> this.completeCoreKeyAssembly(player, level);
         case TRUTH_ENGINE -> this.completeTruth(player, level);
         case MEMORY_STABILIZER -> this.completeStabilize(player, level);
         case PROTOCOL_EXTRACTOR -> this.completeProtocolExtractor(player, level);
      }
   }

   private void completeDecode(Player player, ServerLevel level) {
      ItemStack input = this.items.get(INPUT_SLOT);
      if (!(input.getItem() instanceof BlackboxFragmentItem fragment)) {
         this.reset(MachineStatus.BAD_INPUT, "Decoder input was removed.");
         return;
      }

      ItemStack record = new ItemStack(ModItems.recordFor(fragment.type()).get());
      if (!this.canOutput(record)) {
         this.reset(MachineStatus.OUTPUT_BLOCKED, "Output slot is blocked.");
         this.message(player, this.lastMessage);
         return;
      }

      BlackboxProgress progressData = BlackboxProgress.get(player);
      boolean first = progressData.decode(player, fragment.type());
      if (!player.hasInfiniteMaterials()) {
         input.shrink(1);
      }

      this.mergeOutput(record);

      if (player instanceof ServerPlayer serverPlayer) {
         BlackboxCoreIntegration.mirrorDecodedMemory(serverPlayer, fragment.type(), first);
      }
      this.reset(MachineStatus.COMPLETE, fragment.type().displayName() + " decoded. Memory stability " + progressData.stability() + "%.");
      this.pulse(level, ParticleTypes.END_ROD, SoundEvents.ENCHANTMENT_TABLE_USE);
      this.message(player, this.lastMessage);
   }

   private void completeArchiveTerminal(Player player, ServerLevel level) {
      BlackboxProgress progressData = BlackboxProgress.get(player);
      if (!progressData.canEnter(BlackboxDungeon.VAULT) && !player.hasInfiniteMaterials()) {
         this.reset(MachineStatus.LOCKED, "Archive Terminal sealed. Decode Personal and Security Logs before Vault certification.");
      } else if (!progressData.completed(BlackboxDungeon.VAULT)) {
         progressData.completeDungeon(BlackboxDungeon.VAULT);
         this.giveIfMissing(player, ModItems.CORE_ACCESS_KEY_LEFT.get());
         this.reset(MachineStatus.COMPLETE, "Blackbox Vault route sealed. Left key segment recovered.");
         this.pulse(level, ParticleTypes.GLOW, SoundEvents.BEACON_ACTIVATE);
      } else {
         this.giveIfMissing(player, ModItems.CORE_ACCESS_KEY_LEFT.get());
         this.reset(MachineStatus.COMPLETE, "Archive cache lists " + progressData.decodedMemoryTotal() + " memories and ending " + progressData.ending().displayName() + ".");
      }

      this.message(player, this.lastMessage);
   }

   private void completeMemoryProjector(Player player, ServerLevel level) {
      BlackboxProgress progressData = BlackboxProgress.get(player);
      if (!progressData.hasMemory(MemoryType.ECHO, 2) && !player.hasInfiniteMaterials()) {
         this.reset(MachineStatus.LOCKED, "Memory Projector cannot align. Decode two ECHO Logs first.");
      } else if (!progressData.bossDefeated("false_echo")) {
         this.spawnBoss(level, ModEntities.FALSE_ECHO.get(), "Memory Projector breached. The False ECHO is using your tutorial channel.");
      } else if (!progressData.completed(BlackboxDungeon.LABYRINTH)) {
         progressData.completeDungeon(BlackboxDungeon.LABYRINTH);
         this.giveIfMissing(player, ModItems.CORE_ACCESS_KEY_RIGHT.get());
         this.reset(MachineStatus.COMPLETE, "Memory Labyrinth stabilized. Right key segment recovered.");
         this.pulse(level, ParticleTypes.PORTAL, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN);
      } else {
         this.giveIfMissing(player, ModItems.CORE_ACCESS_KEY_RIGHT.get());
         this.reset(MachineStatus.COMPLETE, "Memory Projector reconstructing " + progressData.decodedMemoryTotal() + " decoded logs.");
      }

      this.message(player, this.lastMessage);
   }

   private void completeCoreKeyAssembly(Player player, ServerLevel level) {
      BlackboxProgress progressData = BlackboxProgress.get(player);
      if (progressData.hasNexusCoreAccessKey()) {
         this.reset(MachineStatus.COMPLETE, "Nexus Core Access Key already assembled.");
      } else if (!BlackboxMachineBlock.canAssembleCoreKey(player, progressData)) {
         this.reset(MachineStatus.LOCKED, "Core Key Assembler locked. Left/right segments, matrix, Core Logs, and boss proofs are required.");
      } else {
         ItemStack output = new ItemStack(ModItems.NEXUS_CORE_ACCESS_KEY.get());
         if (!this.canOutput(output)) {
            this.reset(MachineStatus.OUTPUT_BLOCKED, "Output slot is blocked.");
            this.message(player, this.lastMessage);
            return;
         }

         if (!player.hasInfiniteMaterials()) {
            consume(player, ModItems.CORE_ACCESS_KEY_LEFT.get());
            consume(player, ModItems.CORE_ACCESS_KEY_RIGHT.get());
            consume(player, ModItems.CORE_ACCESS_KEY_MATRIX.get());
            consume(player, ModItems.ECHO_IDENTITY_FRAGMENT.get());
            consume(player, ModItems.COMMAND_KEY.get());
            consume(player, ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get());
         }

         progressData.markNexusCoreAccessKey();
         progressData.completeDungeon(BlackboxDungeon.TEMPLE);
         this.mergeOutput(output);
         this.reset(MachineStatus.COMPLETE, "Nexus Core Access Key assembled. Core Chamber route can open.");
         this.pulse(level, ParticleTypes.PORTAL, SoundEvents.BEACON_POWER_SELECT);
      }

      this.message(player, this.lastMessage);
   }

   private void completeStabilize(Player player, ServerLevel level) {
      ItemStack input = this.items.get(INPUT_SLOT);
      if (!isStabilizerReagent(input) && !player.hasInfiniteMaterials()) {
         this.reset(MachineStatus.BAD_INPUT, "Stabilizer reagent missing.");
         return;
      }

      if (!player.hasInfiniteMaterials()) {
         input.shrink(1);
      }

      BlackboxProgress progressData = BlackboxProgress.get(player);
      BlackboxEnding ending = worldEnding(player);
      int stabilityGain = switch (ending) {
         case RESTORE -> 50;
         case CONTROL -> 40;
         case DESTROY -> 15;
         case MERGE -> 60;
         case NONE -> 30;
      };
      int falseSignalRelief = switch (ending) {
         case RESTORE, CONTROL -> 5;
         case MERGE -> progressData.falseSignalCount();
         default -> 3;
      };
      progressData.stability(Math.min(100, progressData.stability() + stabilityGain));
      progressData.falseSignals(Math.max(0, progressData.falseSignalCount() - falseSignalRelief));
      player.removeEffect(MobEffects.NAUSEA);
      player.removeEffect(MobEffects.BLINDNESS);
      player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 0));
      this.reset(MachineStatus.COMPLETE, "Memory stabilized at " + progressData.stability() + "%.");
      this.pulse(level, ParticleTypes.HAPPY_VILLAGER, SoundEvents.BREWING_STAND_BREW);
      this.message(player, this.lastMessage);
   }

   private void completeProtocolExtractor(Player player, ServerLevel level) {
      BlackboxProgress progressData = BlackboxProgress.get(player);
      if (!progressData.completed(BlackboxDungeon.VAULT) && !player.hasInfiniteMaterials()) {
         this.reset(MachineStatus.LOCKED, "Protocol Extractor cannot read command residue until the Vault route is sealed.");
      } else if (!progressData.hasMemory(MemoryType.COMMAND, 2) && !player.hasInfiniteMaterials()) {
         this.reset(MachineStatus.LOCKED, "Protocol Extractor needs two Command Logs to reconstruct the final command.");
      } else if (progressData.bossDefeated("command_remnant")) {
         this.giveIfMissing(player, ModItems.CORE_ACCESS_KEY_MATRIX.get());
         this.reset(MachineStatus.COMPLETE, "Command Remnant already broken. Matrix proof remains cached.");
      } else {
         this.spawnBoss(level, ModEntities.COMMAND_REMNANT.get(), "Protocol Extractor pulled the last command into live memory. Command Remnant inbound.");
      }

      this.message(player, this.lastMessage);
   }

   private void completeTruth(Player player, ServerLevel level) {
      ItemStack input = this.items.get(INPUT_SLOT);
      if (input.getItem() instanceof EndingDirectiveItem directive && BlackboxEndings.apply(player, directive.ending(), this.worldPosition)) {
         if (!player.hasInfiniteMaterials()) {
            input.shrink(1);
         }
         this.reset(MachineStatus.COMPLETE, directive.ending().displayName() + " ending committed.");
         this.pulse(level, ParticleTypes.REVERSE_PORTAL, SoundEvents.END_PORTAL_SPAWN);
      } else {
         this.reset(MachineStatus.LOCKED, "Truth Engine rejected the directive.");
      }
   }

   private static RecipeHolder<BlackboxProcessingRecipe> findRecipe(ServerLevel level, BlackboxMachineKind kind, ItemStack input) {
      return level.getServer()
         .getRecipeManager()
         .getRecipes()
         .stream()
         .filter(holder -> holder.value().getType() == ModRecipes.BLACKBOX_PROCESSING_TYPE.get())
         .map(holder -> (RecipeHolder<BlackboxProcessingRecipe>)holder)
         .filter(holder -> holder.value().matches(kind, input, level))
         .findFirst()
         .orElse(null);
   }

   public static boolean validInput(BlackboxMachineKind kind, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      return switch (kind) {
         case BLACKBOX_DECODER -> stack.getItem() instanceof BlackboxFragmentItem;
         case MEMORY_STABILIZER -> isStabilizerReagent(stack);
         case PROTOCOL_EXTRACTOR -> stack.is(ModItems.DELETED_MEMORY_RECORD.get());
         case CORE_KEY_ASSEMBLER -> stack.is(ModItems.COMMAND_KEY.get());
         case TRUTH_ENGINE -> stack.getItem() instanceof EndingDirectiveItem;
         case MEMORY_PROJECTOR, ARCHIVE_TERMINAL -> false;
      };
   }

   public ContainerData data() {
      return this.data;
   }

   public MachineStatus status() {
      return this.status;
   }

   public String lastMessage() {
      return this.lastMessage;
   }

   public BlackboxMachineKind kind() {
      return this.getBlockState().getBlock() instanceof BlackboxMachineBlock machine ? machine.kind() : BlackboxMachineKind.BLACKBOX_DECODER;
   }

   public boolean canOutput(ItemStack output) {
      ItemStack current = this.items.get(OUTPUT_SLOT);
      return output.isEmpty()
         || current.isEmpty()
         || ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= Math.min(current.getMaxStackSize(), this.getMaxStackSize(current));
   }

   private void mergeOutput(ItemStack output) {
      if (output.isEmpty()) {
         return;
      }

      ItemStack current = this.items.get(OUTPUT_SLOT);
      if (current.isEmpty()) {
         this.items.set(OUTPUT_SLOT, output);
      } else {
         current.grow(output.getCount());
      }
   }

   private void reset(MachineStatus status, String message) {
      this.progress = 0;
      this.maxProgress = 0;
      this.status = status;
      this.lastMessage = message;
      this.setChanged();
   }

   private void message(Player player, String message) {
      player.sendSystemMessage(Component.literal("ECHO-7 // " + message));
   }

   private void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private void giveIfMissing(Player player, Item item) {
      if (!has(player, item) && !player.hasInfiniteMaterials()) {
         ItemStack output = new ItemStack(item);
         if (this.canOutput(output)) {
            this.mergeOutput(output);
         } else {
            this.give(player, output);
         }
      }
   }

   private void spawnBoss(ServerLevel level, EntityType<?> type, String message) {
      Entity entity = type.create(level, EntitySpawnReason.EVENT);
      if (entity == null) {
         this.reset(MachineStatus.LOCKED, "Projection failed. The Blackbox refused to instantiate the memory.");
         return;
      }

      entity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5);
      level.addFreshEntity(entity);
      this.reset(MachineStatus.COMPLETE, message);
      this.pulse(level, ParticleTypes.SONIC_BOOM, SoundEvents.SCULK_SHRIEKER_SHRIEK);
   }

   private void pulse(ServerLevel level, ParticleOptions particle, net.minecraft.sounds.SoundEvent sound) {
      level.sendParticles(particle, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.2, this.worldPosition.getZ() + 0.5, 18, 0.35, 0.35, 0.35, 0.02);
      level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
   }

   private static boolean isStabilizerReagent(ItemStack stack) {
      return stack.is(ModItems.STATIC_FLUID.get()) || stack.is(ModItems.MEMORY_STABILIZER_CORE.get());
   }

   private static boolean has(Player player, Item item) {
      return player.getInventory().contains(new ItemStack((ItemLike)item));
   }

   private static void consume(Player player, Item item) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            stack.shrink(1);
            return;
         }
      }
   }

   private static int scaledStabilityCost(Player player, BlackboxProcessingRecipe recipe) {
      return switch (worldEnding(player)) {
         case RESTORE -> Math.max(0, recipe.stabilityCost() - 4);
         case CONTROL -> Math.max(1, recipe.stabilityCost() / 2);
         case DESTROY -> recipe.stabilityCost() + 4;
         case MERGE -> 0;
         case NONE -> recipe.stabilityCost();
      };
   }

   private static BlackboxEnding worldEnding(Player player) {
      if (player.level() instanceof ServerLevel level) {
         return BlackboxWorldData.get(level.getServer().overworld()).ending();
      }

      return BlackboxEnding.NONE;
   }

   private static boolean isUniqueResult(Item item) {
      return item == ModItems.CORE_ACCESS_KEY_LEFT.get()
         || item == ModItems.CORE_ACCESS_KEY_RIGHT.get()
         || item == ModItems.CORE_ACCESS_KEY_MATRIX.get()
         || item == ModItems.NEXUS_CORE_ACCESS_KEY.get()
         || item == ModItems.ECHO_IDENTITY_FRAGMENT.get()
         || item == ModItems.MEMORY_STABILIZER_CORE.get()
         || item == ModItems.COMMAND_KEY.get()
         || item == ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get()
         || item == ModItems.GUARDIAN_CORE.get()
         || item == ModItems.RESTORE_DIRECTIVE.get()
         || item == ModItems.CONTROL_DIRECTIVE.get()
         || item == ModItems.DESTROY_DIRECTIVE.get()
         || item == ModItems.MERGE_DIRECTIVE.get();
   }

   protected Component getDefaultName() {
      return Component.literal("ECHO-7 " + this.kind().displayName());
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> items) {
      for (int i = 0; i < Math.min(this.items.size(), items.size()); i++) {
         this.items.set(i, items.get(i));
      }
   }

   public int getContainerSize() {
      return this.items.size();
   }

   protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new BlackboxMachineMenu(containerId, inventory, this, this.data, this.worldPosition);
   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.getBlockPos());
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, this.items);
      this.progress = input.getIntOr("progress", 0);
      this.maxProgress = input.getIntOr("maxProgress", 0);
      this.status = MachineStatus.byId(input.getIntOr("status", MachineStatus.IDLE.ordinal()));
      this.lastMessage = input.getStringOr("lastMessage", "Awaiting input.");
      input.getString("activePlayer").ifPresent(value -> this.activePlayer = UUID.fromString(value));
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.items);
      output.putInt("progress", this.progress);
      output.putInt("maxProgress", this.maxProgress);
      output.putInt("status", this.status.ordinal());
      output.putString("lastMessage", this.lastMessage);
      if (this.activePlayer != null) {
         output.putString("activePlayer", this.activePlayer.toString());
      }
   }

   public enum MachineStatus {
      IDLE("Idle"),
      PROCESSING("Processing"),
      COMPLETE("Complete"),
      BAD_INPUT("Bad input"),
      OUTPUT_BLOCKED("Output blocked"),
      LOCKED("Locked");

      private static final MachineStatus[] BY_ID = values();
      private final String label;

      MachineStatus(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }

      public static MachineStatus byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : IDLE;
      }
   }

   private record Operation(boolean accepted, int duration, MachineStatus status, String message) {
      static Operation accepted(int duration, String message) {
         return new Operation(true, Math.max(20, duration), MachineStatus.PROCESSING, message);
      }

      static Operation rejected(MachineStatus status, String message) {
         return new Operation(false, 0, status, message);
      }
   }
}

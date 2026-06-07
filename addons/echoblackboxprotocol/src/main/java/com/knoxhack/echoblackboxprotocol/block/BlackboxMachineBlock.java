package com.knoxhack.echoblackboxprotocol.block;

import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import com.knoxhack.echoblackboxprotocol.item.BlackboxFragmentItem;
import com.knoxhack.echoblackboxprotocol.item.EndingDirectiveItem;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlackboxMachineBlock extends Block implements EntityBlock {
   private final BlackboxMachineKind kind;

   public BlackboxMachineBlock(BlackboxMachineKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public BlackboxMachineKind kind() {
      return this.kind;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BlackboxMachineBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.BLACKBOX_MACHINE.get()
         ? (tickLevel, pos, blockState, blockEntity) -> BlackboxMachineBlockEntity.tick(tickLevel, pos, blockState, (BlackboxMachineBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         if (player.isShiftKeyDown()) {
            return runPrimaryAction(serverPlayer, this.kind, pos);
         }

         if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider);
         }
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.SUCCESS_SERVER;
      }
   }

   protected InteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         if (player.isShiftKeyDown()) {
            return runHeldItemAction(serverPlayer, this.kind, pos, stack);
         }

         if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider);
         }
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   @Override
   public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
      if (!level.isClientSide() && blockEntity instanceof BlackboxMachineBlockEntity machine) {
         Containers.dropContents(level, pos, machine);
      }

      super.playerDestroy(level, player, pos, state, blockEntity, tool);
   }

   public static InteractionResult runPrimaryAction(ServerPlayer player, BlackboxMachineKind kind, BlockPos pos) {
      ItemStack held = player.getMainHandItem();
      return held.isEmpty() ? runMachineAction(player, kind, pos, held) : runHeldItemAction(player, kind, pos, held);
   }

   public static InteractionResult runHeldItemAction(ServerPlayer player, BlackboxMachineKind kind, BlockPos pos, ItemStack stack) {
      return runMachineAction(player, kind, pos, stack);
   }

   private static InteractionResult runMachineAction(ServerPlayer player, BlackboxMachineKind kind, BlockPos pos, ItemStack stack) {
      BlackboxProgress progress = BlackboxProgress.get(player);
      return switch (kind) {
         case ARCHIVE_TERMINAL -> sealVault(player, progress);
         case MEMORY_PROJECTOR -> projectMemory(player, progress, pos);
         case CORE_KEY_ASSEMBLER -> !stack.isEmpty() && processRecipe(player, kind, stack) ? InteractionResult.SUCCESS_SERVER : assembleKey(player, progress);
         case PROTOCOL_EXTRACTOR -> !stack.isEmpty() && processRecipe(player, kind, stack)
            ? InteractionResult.SUCCESS_SERVER
            : (!stack.isEmpty() && stack.is((Item)ModItems.DELETED_MEMORY_RECORD.get()) ? extract(player, progress, stack) : commandProtocol(player, progress, pos));
         case BLACKBOX_DECODER -> decode(player, stack);
         case TRUTH_ENGINE -> truth(player, stack, pos);
         case MEMORY_STABILIZER -> stack.isEmpty()
            ? report(player, "Memory Stabilizer online. Stability " + progress.stability() + "% | false signals " + progress.falseSignalCount() + ".")
            : stabilize(player, progress, stack);
      };
   }

   private static InteractionResult decode(ServerPlayer player, ItemStack stack) {
      return (InteractionResult)(BlackboxFragmentItem.decode(player, stack)
         ? InteractionResult.SUCCESS_SERVER
         : report(player, "Blackbox Decoder requires a typed Blackbox Fragment."));
   }

   public static boolean processRecipe(Player player, BlackboxMachineKind kind, ItemStack stack) {
      if (stack.isEmpty() || !(player.level() instanceof ServerLevel serverLevel)) {
         return false;
      } else {
         BlackboxProcessingRecipe recipe = findRecipe(serverLevel, kind, stack);
         if (recipe == null) {
            return false;
         } else {
            BlackboxProgress progress = BlackboxProgress.get(player);
            String inputName = stack.getHoverName().getString();
            boolean retainProofKey = stack.is((Item)ModItems.COMMAND_KEY.get()) && recipe.resultItem() == ModItems.CORE_ACCESS_KEY_MATRIX.get();
            if (!player.hasInfiniteMaterials() && !retainProofKey) {
               stack.shrink(1);
            }

            BlackboxEnding ending = worldEnding(player);
            int stabilityCost = switch (ending) {
               case RESTORE -> Math.max(0, recipe.stabilityCost() - 4);
               case CONTROL -> Math.max(1, recipe.stabilityCost() / 2);
               case DESTROY -> recipe.stabilityCost() + 4;
               case MERGE -> 0;
               case NONE -> recipe.stabilityCost();
            };
            progress.stability(progress.stability() - stabilityCost);
            progress.falseSignals(ending == BlackboxEnding.MERGE ? 0 : progress.falseSignalCount() + 1);
            ItemStack result = recipe.result();
            if (ending == BlackboxEnding.CONTROL && !isUniqueResult(result.getItem())) {
               result.grow(1);
            }
            if (isUniqueResult(result.getItem())) {
               giveIfMissing(player, result.getItem());
            } else {
               give(player, result);
            }

            player.sendSystemMessage(
               Component.literal(
                  "ECHO-7 // "
                     + kind.displayName()
                     + " processed "
                     + inputName
                     + " into "
                     + result.getHoverName().getString()
                     + ". Stability "
                     + progress.stability()
                     + "%."
               )
            );
            return true;
         }
      }
   }

   @SuppressWarnings("unchecked")
   private static BlackboxProcessingRecipe findRecipe(ServerLevel level, BlackboxMachineKind kind, ItemStack stack) {
      return level.getServer().getRecipeManager().getRecipes().stream()
         .filter(holder -> holder.value().getType() == ModRecipes.BLACKBOX_PROCESSING_TYPE.get())
         .map(holder -> (RecipeHolder<BlackboxProcessingRecipe>)holder)
         .filter(holder -> holder.value().matches(kind, stack, level))
         .map(RecipeHolder::value)
         .findFirst()
         .orElse(null);
   }

   private static InteractionResult stabilize(ServerPlayer player, BlackboxProgress progress, ItemStack stack) {
      if (!stack.is((Item)ModItems.STATIC_FLUID.get()) && !stack.is((Item)ModItems.MEMORY_STABILIZER_CORE.get()) && !player.hasInfiniteMaterials()) {
         return report(player, "Memory Stabilizer requires Static Fluid or the Memory Stabilizer Core.");
      } else {
         if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
         }

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
            case MERGE -> progress.falseSignalCount();
            default -> 3;
         };
         progress.stability(Math.min(100, progress.stability() + stabilityGain));
         progress.falseSignals(Math.max(0, progress.falseSignalCount() - falseSignalRelief));
         player.removeEffect(MobEffects.NAUSEA);
         player.removeEffect(MobEffects.BLINDNESS);
         player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 0));
         return report(player, "Memory Stabilizer locked hallucination drift. Stability " + progress.stability() + "%.");
      }
   }

   private static InteractionResult sealVault(ServerPlayer player, BlackboxProgress progress) {
      if (!progress.canEnter(BlackboxDungeon.VAULT) && !player.hasInfiniteMaterials()) {
         return report(player, "Archive Terminal sealed. Decode Personal and Security Logs before the Vault can be certified.");
      } else if (!progress.completed(BlackboxDungeon.VAULT)) {
         progress.completeDungeon(BlackboxDungeon.VAULT);
         giveIfMissing(player, (Item)ModItems.CORE_ACCESS_KEY_LEFT.get());
         return report(player, "Blackbox Vault route record sealed. Core Access Key left segment recovered.");
      } else {
         giveIfMissing(player, (Item)ModItems.CORE_ACCESS_KEY_LEFT.get());
         return report(
            player, "Archive Terminal cache lists " + progress.decodedMemoryTotal() + " memories and ending " + progress.ending().displayName() + "."
         );
      }
   }

   private static InteractionResult projectMemory(ServerPlayer player, BlackboxProgress progress, BlockPos pos) {
      if (!progress.hasMemory(MemoryType.ECHO, 2) && !player.hasInfiniteMaterials()) {
         return report(player, "Memory Projector cannot align. Decode two ECHO Logs first.");
      } else if (!progress.bossDefeated("false_echo")) {
         return spawnBoss(player, pos, (EntityType<?>)ModEntities.FALSE_ECHO.get(), "Memory Projector breached. The False ECHO is using your tutorial channel.");
      } else if (!progress.completed(BlackboxDungeon.LABYRINTH)) {
         progress.completeDungeon(BlackboxDungeon.LABYRINTH);
         giveIfMissing(player, (Item)ModItems.CORE_ACCESS_KEY_RIGHT.get());
         return report(player, "Memory Labyrinth geometry stabilized. Core Access Key right segment recovered.");
      } else {
         giveIfMissing(player, (Item)ModItems.CORE_ACCESS_KEY_RIGHT.get());
         return report(player, "Memory Projector reconstructing " + progress.decodedMemoryTotal() + " decoded logs.");
      }
   }

   private static InteractionResult assembleKey(ServerPlayer player, BlackboxProgress progress) {
      if (progress.hasNexusCoreAccessKey()) {
         return report(player, "Nexus Core Access Key already assembled.");
      } else {
         boolean ready = canAssembleCoreKey(player, progress);

         if (!ready) {
            return report(player, "Core Key Assembler locked. Left/right key segments, matrix, Core Logs, and boss proof are required.");
         } else {
            if (!player.hasInfiniteMaterials()) {
               consume(player, (Item)ModItems.CORE_ACCESS_KEY_LEFT.get());
               consume(player, (Item)ModItems.CORE_ACCESS_KEY_RIGHT.get());
               consume(player, (Item)ModItems.CORE_ACCESS_KEY_MATRIX.get());
               consume(player, (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get());
               consume(player, (Item)ModItems.COMMAND_KEY.get());
               consume(player, (Item)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get());
            }

            progress.markNexusCoreAccessKey();
            progress.completeDungeon(BlackboxDungeon.TEMPLE);
            give(player, new ItemStack((ItemLike)ModItems.NEXUS_CORE_ACCESS_KEY.get()));
            return report(player, "Nexus Core Access Key assembled. The Core Chamber route can open.");
         }
      }
   }

   public static boolean canAssembleCoreKey(Player player, BlackboxProgress progress) {
      return progress.bossDefeated("false_echo")
         && progress.bossDefeated("command_remnant")
         && progress.hasMemory(MemoryType.CORE, 2)
         && (has(player, (Item)ModItems.CORE_ACCESS_KEY_LEFT.get()) || player.hasInfiniteMaterials())
         && (has(player, (Item)ModItems.CORE_ACCESS_KEY_RIGHT.get()) || player.hasInfiniteMaterials())
         && (has(player, (Item)ModItems.CORE_ACCESS_KEY_MATRIX.get()) || player.hasInfiniteMaterials())
         && (has(player, (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get()) || player.hasInfiniteMaterials())
         && (has(player, (Item)ModItems.COMMAND_KEY.get()) || player.hasInfiniteMaterials())
         && (has(player, (Item)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get()) || player.hasInfiniteMaterials());
   }
   private static InteractionResult truth(ServerPlayer player, ItemStack stack, BlockPos pos) {
      if (stack.getItem() instanceof EndingDirectiveItem directive) {
         BlackboxEndings.apply(player, directive.ending(), pos);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return report(player, "Truth Engine requires a Restore, Control, Destroy, or Merge directive.");
      }
   }

   private static InteractionResult commandProtocol(ServerPlayer player, BlackboxProgress progress, BlockPos pos) {
      if (!progress.completed(BlackboxDungeon.VAULT) && !player.hasInfiniteMaterials()) {
         return report(player, "Protocol Extractor cannot read command residue until the Blackbox Vault route is sealed.");
      } else if (!progress.hasMemory(MemoryType.COMMAND, 2) && !player.hasInfiniteMaterials()) {
         return report(player, "Protocol Extractor needs two Command Logs to reconstruct the last world-order command.");
      } else if (progress.bossDefeated("command_remnant")) {
         giveIfMissing(player, (Item)ModItems.CORE_ACCESS_KEY_MATRIX.get());
         return report(player, "Command Remnant already broken. Matrix proof remains cached.");
      } else {
         return spawnBoss(
            player,
            pos,
            (EntityType<?>)ModEntities.COMMAND_REMNANT.get(),
            "Protocol Extractor pulled the last command into live memory. Command Remnant inbound."
         );
      }
   }

   private static InteractionResult extract(ServerPlayer player, BlackboxProgress progress, ItemStack stack) {
      if (!stack.is((Item)ModItems.DELETED_MEMORY_RECORD.get())) {
         return report(player, "Protocol Extractor needs Deleted Memory Records to expose secret routing.");
      } else {
         progress.falseSignals(Math.max(0, progress.falseSignalCount() - 2));
         player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 900, 0));
         return report(player, "Deleted command residue extracted. Merge eligibility index recalculated.");
      }
   }

   private static InteractionResult report(Player player, String message) {
      player.sendSystemMessage(Component.literal("ECHO-7 // " + message));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult spawnBoss(ServerPlayer player, BlockPos pos, EntityType<?> type, String message) {
      ServerLevel entity = player.level();
      if (entity instanceof ServerLevel) {
         Entity entityx = type.create(entity, EntitySpawnReason.EVENT);
         if (entityx == null) {
            return report(player, "Projection failed. The Blackbox refused to instantiate the memory.");
         } else {
            entityx.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            entity.addFreshEntity(entityx);
            return report(player, message);
         }
      } else {
         return InteractionResult.CONSUME;
      }
   }

   private static BlackboxEnding worldEnding(Player player) {
      if (player.level() instanceof ServerLevel level) {
         return BlackboxWorldData.get(level.getServer().overworld()).ending();
      } else {
         return BlackboxEnding.NONE;
      }
   }

   private static boolean has(Player player, Item item) {
      return player.getInventory().contains(new ItemStack(item));
   }

   private static void giveIfMissing(Player player, Item item) {
      if (!has(player, item) && !player.hasInfiniteMaterials()) {
         give(player, new ItemStack(item));
      }
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

   private static void consume(Player player, Item item) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            stack.shrink(1);
            return;
         }
      }
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }
}

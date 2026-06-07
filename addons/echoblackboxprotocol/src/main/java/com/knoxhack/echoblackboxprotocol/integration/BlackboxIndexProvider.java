package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.recipe.BlackboxProcessingRecipe;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModRecipes;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

public enum BlackboxIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final int ACCENT = 0xFF9CA3AF;

   public static void register() {
      EchoCoreServices.registerIndexContentProvider(INSTANCE);
   }

   @Override
   public Identifier id() {
      return id("provider/index_recipes");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      Player player = context == null ? null : context.player();
      List<IndexRecipeView> recipes = recipes(player);
      return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
         IndexMachineLayoutTemplates.representativeAll(recipes, "blackbox_station"),
         List.of(), List.of(), List.of());
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      Set<BlackboxMachineKind> kinds = new LinkedHashSet<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (holder.value() instanceof BlackboxProcessingRecipe recipe) {
            kinds.add(recipe.machine());
         }
      }
      List<IndexRecipeCategory> categories = new ArrayList<>();
      for (BlackboxMachineKind kind : kinds) {
         categories.add(new IndexRecipeCategory(categoryId(kind), kind.displayName(), machineStack(kind), ACCENT, 700 + kind.ordinal()));
      }
      return categories;
   }

   public List<IndexRecipeView> recipes(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      List<IndexRecipeView> views = new ArrayList<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (holder.value() instanceof BlackboxProcessingRecipe recipe) {
            views.add(view(holder, recipe));
         }
      }
      return List.copyOf(views);
   }

   private static IndexRecipeView view(RecipeHolder<?> holder, BlackboxProcessingRecipe recipe) {
      BlackboxMachineKind kind = recipe.machine();
      ItemStack machine = machineStack(kind);
      ItemStack output = recipe.result();
      List<IndexRecipeSlot> slots = new ArrayList<>();
      List<ItemStack> inputs = stacks(recipe.ingredient(), 1);
      if (!inputs.isEmpty()) {
         slots.add(IndexRecipeSlot.inputs(inputs));
      }
      if (!machine.isEmpty()) {
         slots.add(IndexRecipeSlot.machine(machine));
      }
      if (!output.isEmpty()) {
         slots.add(IndexRecipeSlot.output(output));
      }
      return new IndexRecipeView(
         holder.id().identifier(),
         categoryId(kind),
         kind.displayName() + ": " + output.getHoverName().getString(),
         machine,
         slots,
         List.of("Stability cost: " + recipe.stabilityCost() + ".", "Duration: " + recipe.duration() + " ticks."),
         recipe.duration(),
         false,
         EchoBlackboxProtocol.MODID);
   }

   private static List<RecipeHolder<?>> recipeHolders(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      MinecraftServer server = player.level().getServer();
      if (server == null) {
         return List.of();
      }
      try {
         List<RecipeHolder<?>> holders = new ArrayList<>();
         for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.value().getType() == ModRecipes.BLACKBOX_PROCESSING_TYPE.get()) {
               holders.add(holder);
            }
         }
         return holders;
      } catch (RuntimeException exception) {
         EchoBlackboxProtocol.LOGGER.debug("ECHO: Index could not enumerate Blackbox recipes.", exception);
      }
      return List.of();
   }

   private static List<ItemStack> stacks(Ingredient ingredient, int count) {
      if (ingredient == null || ingredient.isEmpty()) {
         return List.of();
      }
      return ingredient.items()
         .map(Holder::value)
         .map(item -> new ItemStack(item, Math.max(1, count)))
         .filter(stack -> !stack.isEmpty())
         .limit(24)
         .toList();
   }

   private static Identifier categoryId(BlackboxMachineKind kind) {
      return id("recipe/" + kind.getSerializedName());
   }

   private static ItemStack machineStack(BlackboxMachineKind kind) {
      return new ItemStack(machineBlock(kind).get());
   }

   private static EchoBackendRegistryEntry<Block> machineBlock(BlackboxMachineKind kind) {
      return switch (kind) {
         case BLACKBOX_DECODER -> ModBlocks.BLACKBOX_DECODER;
         case MEMORY_PROJECTOR -> ModBlocks.MEMORY_PROJECTOR;
         case ARCHIVE_TERMINAL -> ModBlocks.ARCHIVE_TERMINAL;
         case CORE_KEY_ASSEMBLER -> ModBlocks.CORE_KEY_ASSEMBLER;
         case TRUTH_ENGINE -> ModBlocks.TRUTH_ENGINE;
         case MEMORY_STABILIZER -> ModBlocks.MEMORY_STABILIZER;
         case PROTOCOL_EXTRACTOR -> ModBlocks.PROTOCOL_EXTRACTOR;
      };
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
   }
}

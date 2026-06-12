package com.knoxhack.echonexusprotocol.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexMachineLayout;
import com.echoplatform.echocore.api.index.IndexMachineLayoutGauge;
import com.echoplatform.echocore.api.index.IndexMachineLayoutSlot;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.menu.NexusMachineMenu;
import com.knoxhack.echonexusprotocol.recipe.NexusProcessingRecipe;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

public enum NexusIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final int ACCENT = 0xFFC77DFF;

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
         machineLayouts(recipes), List.of(), List.of(), List.of());
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      List<IndexRecipeCategory> categories = new ArrayList<>();
      for (NexusMachineBlock.MachineKind kind : NexusMachineBlock.MachineKind.values()) {
         if (kind.recipeDriven() || kind == NexusMachineBlock.MachineKind.CORRUPTION_REACTOR) {
            categories.add(new IndexRecipeCategory(categoryId(kind), kind.displayName(), machineStack(kind), ACCENT, 500 + kind.ordinal()));
         }
      }
      return categories;
   }

   public List<IndexRecipeView> recipes(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      List<IndexRecipeView> views = new ArrayList<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (holder.value() instanceof NexusProcessingRecipe recipe) {
            views.add(view(holder, recipe));
         }
      }
      return List.copyOf(views);
   }

   private static IndexRecipeView view(RecipeHolder<?> holder, NexusProcessingRecipe recipe) {
      NexusMachineBlock.MachineKind kind = recipe.machine();
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
      if (recipe.chargeOutput() > 0) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Nexus charge +" + recipe.chargeOutput()));
      }

      List<String> notes = new ArrayList<>();
      if (recipe.chargeCost() > 0) {
         notes.add("Consumes " + recipe.chargeCost() + " Nexus charge.");
      }
      if (recipe.chargeOutput() > 0) {
         notes.add("Generates " + recipe.chargeOutput() + " Nexus charge.");
      }
      if (recipe.corruptionDelta() != 0) {
         notes.add("Corruption delta: " + signed(recipe.corruptionDelta()) + ".");
      }
      if (recipe.fieldDelta() != 0) {
         notes.add("Field delta: " + signed(recipe.fieldDelta()) + ".");
      }

      String title = kind.displayName() + ": " + (output.isEmpty() ? "Field Process" : output.getHoverName().getString());
      return new IndexRecipeView(
         holder.id().identifier(),
         categoryId(kind),
         title,
         machine,
         slots,
         notes,
         recipe.duration(),
         false,
         EchoNexusProtocol.MODID);
   }

   private static List<IndexMachineLayout> machineLayouts(List<IndexRecipeView> recipes) {
      List<IndexMachineLayout> layouts = new ArrayList<>();
      for (IndexRecipeView recipe : recipes) {
         List<IndexMachineLayoutSlot> slots = new ArrayList<>();
         addLayoutSlot(slots, recipe, IndexSlotRole.INPUT, 0, NexusMachineMenu.INPUT_X, NexusMachineMenu.MACHINE_SLOT_Y);
         addLayoutSlot(slots, recipe, IndexSlotRole.MACHINE, 0, 164, NexusMachineMenu.MACHINE_SLOT_Y);
         addLayoutSlot(slots, recipe, IndexSlotRole.OUTPUT, 0, NexusMachineMenu.OUTPUT_X, NexusMachineMenu.MACHINE_SLOT_Y);
         layouts.add(new IndexMachineLayout(
            recipe.id(),
            "nexus_machine",
            recipe.machine().isEmpty() ? recipe.title() : recipe.machine().getHoverName().getString(),
            NexusMachineMenu.GUI_WIDTH,
            164,
            true,
            slots,
            List.of(
               new IndexMachineLayoutGauge("progress", "Progress", 118, 96, 112, 8, 0xFF6FE6FF),
               new IndexMachineLayoutGauge("charge", "Charge", 118, 118, 112, 8, 0xFF70F0D8),
               new IndexMachineLayoutGauge("contamination", "Contamination", 118, 140, 112, 8, 0xFFB85CFF))));
      }
      return List.copyOf(layouts);
   }

   private static void addLayoutSlot(List<IndexMachineLayoutSlot> slots, IndexRecipeView recipe, IndexSlotRole role,
         int occurrence, int x, int y) {
      int index = slotIndex(recipe, role, occurrence);
      if (index >= 0) {
         IndexRecipeSlot slot = recipe.slots().get(index);
         slots.add(new IndexMachineLayoutSlot(index, role, slot.label(), x, y, 18, false));
      }
   }

   private static int slotIndex(IndexRecipeView recipe, IndexSlotRole role, int occurrence) {
      int seen = 0;
      for (int i = 0; i < recipe.slots().size(); i++) {
         if (recipe.slots().get(i).role() != role) {
            continue;
         }
         if (seen == occurrence) {
            return i;
         }
         seen++;
      }
      return -1;
   }

   private static List<RecipeHolder<?>> recipeHolders(Player player) {
      MinecraftServer server = player.level().getServer();
      if (server == null) {
         return List.of();
      }
      try {
         List<RecipeHolder<?>> holders = new ArrayList<>();
         for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.value().getType() == ModRecipes.NEXUS_PROCESSING_TYPE.get()) {
               holders.add(holder);
            }
         }
         return holders;
      } catch (RuntimeException exception) {
         EchoNexusProtocol.LOGGER.debug("ECHO: Index could not enumerate Nexus recipes.", exception);
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

   private static String signed(int value) {
      return value > 0 ? "+" + value : String.valueOf(value);
   }

   private static Identifier categoryId(NexusMachineBlock.MachineKind kind) {
      return id("recipe/" + kind.getSerializedName());
   }

   private static ItemStack machineStack(NexusMachineBlock.MachineKind kind) {
      return new ItemStack(machineBlock(kind).get());
   }

   private static EchoBackendRegistryEntry<Block> machineBlock(NexusMachineBlock.MachineKind kind) {
      return switch (kind) {
         case NEXUS_RECYCLER -> ModBlocks.NEXUS_RECYCLER;
         case NEXUS_CHARGE_TANK -> ModBlocks.NEXUS_CHARGE_TANK;
         case CORRUPTION_FILTER -> ModBlocks.CORRUPTION_FILTER;
         case NEXUS_FIELD_STABILIZER -> ModBlocks.NEXUS_FIELD_STABILIZER;
         case NEXUS_INFUSER -> ModBlocks.NEXUS_INFUSER;
         case MEMORY_DECODER -> ModBlocks.MEMORY_DECODER;
         case REALITY_FORGE -> ModBlocks.REALITY_FORGE;
         case CORRUPTION_REACTOR -> ModBlocks.CORRUPTION_REACTOR;
      };
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, path);
   }
}

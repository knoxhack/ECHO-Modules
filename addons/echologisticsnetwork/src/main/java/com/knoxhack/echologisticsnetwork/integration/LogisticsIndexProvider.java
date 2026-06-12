package com.knoxhack.echologisticsnetwork.integration;

import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexMachineLayoutTemplates;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.content.FactionDepotOffer;
import com.knoxhack.echologisticsnetwork.content.FactoryRestockPolicy;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.LoadoutRequirement;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.registry.ModBlocks;
import com.knoxhack.echologisticsnetwork.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public enum LogisticsIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final Identifier CATEGORY_LOADOUTS = id("recipe/logistics_loadouts");
   private static final Identifier CATEGORY_DEPOT_EXCHANGES = id("recipe/depot_exchanges");

   @Override
   public Identifier id() {
      return id("provider/index_recipes");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      Player player = context == null ? null : context.player();
      List<IndexRecipeView> recipes = recipes(player);
      return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
         IndexMachineLayoutTemplates.representativeAll(recipes, "logistics_station"),
         List.of(), List.of(), List.of());
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      return List.of(
         new IndexRecipeCategory(CATEGORY_LOADOUTS, "Logistics Loadouts",
            new ItemStack(ModItems.LOADOUT_CARD.get()), 0xFF92F7A6, 520),
         new IndexRecipeCategory(CATEGORY_DEPOT_EXCHANGES, "Depot Exchanges",
            new ItemStack(ModBlocks.FACTION_TRADE_DEPOT.asItem()), 0xFFFFD166, 530)
      );
   }

   public List<IndexRecipeView> recipes(Player player) {
      List<IndexRecipeView> views = new ArrayList<>();
      for (LoadoutPreset loadout : LogisticsContent.loadouts()) {
         views.add(loadoutView(loadout));
      }
      for (FactionDepotOffer offer : LogisticsContent.offers()) {
         views.add(offerView(offer));
      }
      return List.copyOf(views);
   }

   private static IndexRecipeView loadoutView(LoadoutPreset loadout) {
      ItemStack machine = new ItemStack(ModBlocks.LOADOUT_LOCKER.asItem());
      List<IndexRecipeSlot> slots = new ArrayList<>();
      for (LoadoutRequirement requirement : loadout.requirements()) {
         slots.add(requirementSlot(requirement));
      }
      slots.add(IndexRecipeSlot.machine(machine));
      for (Identifier target : loadout.targetBlockTypes()) {
         ItemStack blockStack = blockStack(target);
         if (blockStack.isEmpty()) {
            slots.add(IndexRecipeSlot.info("Target block: " + readable(target)));
         } else {
            slots.add(IndexRecipeSlot.of(IndexSlotRole.MACHINE, blockStack, "Target block"));
         }
      }
      slots.add(IndexRecipeSlot.catalyst(new ItemStack(ModItems.LOADOUT_CARD.get()), "Loadout card"));
      slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Loadout: " + loadout.title()));

      List<String> notes = new ArrayList<>();
      notes.add("Delivery: " + loadout.deliveryTicks() + " ticks");
      notes.add("Targets: " + loadout.targetBlockTypes().size() + " block type(s)");
      FactoryRestockPolicy restock = loadout.restockPolicy();
      if (restock.enabled()) {
         notes.add("Auto-restock task: " + restock.factoryTaskId());
         notes.add("Restock runs: target " + restock.targetRuns() + ", min " + restock.minRuns()
            + ", in-flight " + restock.maxInFlight());
         notes.add("Restock cooldown: " + restock.cooldownTicks() + " ticks");
      } else {
         notes.add("Restock: manual or disabled");
      }

      return new IndexRecipeView(
         id("recipe/loadout/" + loadout.id().getNamespace() + "/" + loadout.id().getPath()),
         CATEGORY_LOADOUTS,
         loadout.title(),
         machine,
         slots,
         notes,
         loadout.deliveryTicks(),
         false,
         EchoLogisticsNetwork.MODID);
   }

   private static IndexRecipeView offerView(FactionDepotOffer offer) {
      ItemStack machine = new ItemStack(ModBlocks.FACTION_TRADE_DEPOT.asItem());
      List<IndexRecipeSlot> slots = new ArrayList<>();
      ItemStack input = offer.input();
      if (input.isEmpty()) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), "Depot payment: " + offer.inputSpec().itemId()));
      } else {
         slots.add(IndexRecipeSlot.input(input));
      }
      slots.add(IndexRecipeSlot.machine(machine));
      ItemStack output = offer.output();
      if (output.isEmpty()) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Depot reward: " + offer.outputSpec().itemId()));
      } else {
         slots.add(IndexRecipeSlot.output(output));
      }
      return new IndexRecipeView(
         id("recipe/depot_exchange/" + offer.id().getNamespace() + "/" + offer.id().getPath()),
         CATEGORY_DEPOT_EXCHANGES,
         "Depot Exchange: " + readable(offer.id()),
         machine,
         slots,
         List.of(
            "Faction: " + offer.factionId(),
            "Minimum reputation: " + offer.minReputation(),
            "Reputation delta: " + offer.reputationDelta(),
            "Cooldown: " + offer.cooldownTicks() + " ticks"),
         offer.cooldownTicks(),
         false,
         EchoLogisticsNetwork.MODID);
   }

   private static IndexRecipeSlot requirementSlot(LoadoutRequirement requirement) {
      if (requirement.kind() == LoadoutRequirement.Kind.ITEM) {
         ItemStack stack = itemStack(requirement.target(), requirement.count());
         if (!stack.isEmpty()) {
            return IndexRecipeSlot.input(stack);
         }
      }
      String label = (requirement.optional() ? "Optional " : "") + requirement.count() + "x "
         + requirement.displayName();
      return new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), label);
   }

   private static ItemStack itemStack(Identifier itemId, int count) {
      if (itemId == null) {
         return ItemStack.EMPTY;
      }
      Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
   }

   private static ItemStack blockStack(Identifier blockId) {
      if (blockId == null) {
         return ItemStack.EMPTY;
      }
      Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
      if (block == null || block.asItem() == Items.AIR) {
         return ItemStack.EMPTY;
      }
      return new ItemStack(block.asItem());
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, sanitize(path));
   }

   private static String readable(Identifier id) {
      String value = id == null ? "unknown" : id.getPath();
      StringBuilder title = new StringBuilder();
      for (String part : value.replace('/', '_').split("_+")) {
         if (part.isBlank()) {
            continue;
         }
         if (!title.isEmpty()) {
            title.append(' ');
         }
         title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
      }
      return title.isEmpty() ? value : title.toString();
   }

   private static String sanitize(String path) {
      String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
      clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
      while (clean.contains("//")) {
         clean = clean.replace("//", "/");
      }
      return clean.isBlank() ? "unknown" : clean;
   }
}

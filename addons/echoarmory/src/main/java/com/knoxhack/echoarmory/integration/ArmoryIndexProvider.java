package com.knoxhack.echoarmory.integration;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.item.ArmoryGearItem;
import com.knoxhack.echoarmory.registry.ModBlocks;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IIndexRegistry;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public enum ArmoryIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final Identifier CATEGORY_MACHINES = id("machines");
   private static final Identifier MODULE_INSTALL = id("recipe/module_install");
   private static final Identifier RECHARGE = id("recipe/recharge");
   private static final Identifier REPAIR = id("recipe/repair");
   private static final Identifier TIER_UPGRADE = id("recipe/tier_upgrade");
   private static final Identifier SIGIL = id("recipe/sigil_engraving");
   private static final Identifier LOADOUT = id("recipe/loadout_binding");
   private static final Identifier STORAGE = id("recipe/storage_scan");

   public static void register() {
      EchoCoreServices.registerIndexContentProvider(INSTANCE);
   }

   @Override
   public Identifier id() {
      return id("provider/index");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      Player player = context == null ? null : context.player();
      IndexContentBuilder builder = IndexContentBuilder.create(id());
      register(builder);
      builder.addRecipeCategories(recipeCategories(player));
      List<IndexRecipeView> recipes = recipes(player);
      builder.addRecipes(recipes);
      builder.addMachineLayouts(IndexMachineLayoutTemplates.representativeAll(recipes, "armory_station"));
      return builder.snapshot();
   }

   public void register(IIndexRegistry registry) {
      registry.registerCategory(new IndexCategory(
         CATEGORY_MACHINES,
         "Armory Stations",
         "Modular gear stations for upgrades, repairs, recharge, and loadouts.",
         stationStack(StationKind.ARMORY_BENCH),
         150,
         EchoArmory.MODID));
      for (StationKind kind : StationKind.values()) {
         registry.registerEntry(new IndexEntry(
            id("station/" + kind.getSerializedName()),
            CATEGORY_MACHINES,
            kind.displayName(),
            "",
            summary(kind),
            kind.displayName() + " exposes its station operation through ECHO: Index.",
            stationStack(kind),
            EchoArmory.MODID,
            List.of("machine", "armory", "station", kind.getSerializedName()),
            IndexEntryState.VISIBLE,
            List.of(),
            List.of(itemId(stationStack(kind))),
            List.of(),
            10 + kind.ordinal()));
      }
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      return List.of(
         category(MODULE_INSTALL, "Armory Module Install", StationKind.MODULE_UPGRADE_TABLE, 410),
         category(RECHARGE, "Armory Recharge", StationKind.ENERGY_CORE_CHARGING_STATION, 420),
         category(REPAIR, "Armory Repair", StationKind.ARMORY_BENCH, 430),
         category(TIER_UPGRADE, "Armory Tier Upgrade", StationKind.WEAPON_FORGE, 440),
         category(SIGIL, "Sigil Engraving", StationKind.SIGIL_ENGRAVER, 450),
         category(LOADOUT, "Loadout Binding", StationKind.LOADOUT_TERMINAL, 460),
         category(STORAGE, "Armory Storage Scan", StationKind.WEAPON_RACK, 470));
   }

   public List<IndexRecipeView> recipes(Player player) {
      List<IndexRecipeView> views = new ArrayList<>();
      addModuleInstalls(views);
      addRecharge(views);
      addRepairs(views);
      addTierUpgrades(views);
      addGearOperationViews(views);
      addStorageViews(views);
      return views;
   }

   private static void addModuleInstalls(List<IndexRecipeView> views) {
      for (GearDefinition gear : ArmoryContent.gear()) {
         ItemStack gearStack = stack(gear.id()).orElse(ItemStack.EMPTY);
         if (gearStack.isEmpty()) {
            continue;
         }
         for (ModuleDefinition module : ArmoryContent.modules()) {
            if (!gear.allows(module) || !module.compatibleWith(gear)) {
               continue;
            }
            ItemStack moduleStack = stack(module.id()).orElse(ItemStack.EMPTY);
            if (moduleStack.isEmpty()) {
               continue;
            }
            ItemStack machine = stationStack(StationKind.MODULE_UPGRADE_TABLE);
            views.add(view(
               id("module_install/" + gear.id().getPath() + "/" + module.id().getPath()),
               MODULE_INSTALL,
               gear.title() + " + " + module.title(),
               machine,
               List.of(IndexRecipeSlot.input(gearStack), IndexRecipeSlot.input(moduleStack),
                  IndexRecipeSlot.machine(machine), IndexRecipeSlot.output(gearStack)),
               List.of("Slot: " + module.slotType(), "Effect: " + module.effectType(),
                  "Instability: +" + module.instability())));
         }
      }
   }

   private static void addRecharge(List<IndexRecipeView> views) {
      ItemStack machine = stationStack(StationKind.ENERGY_CORE_CHARGING_STATION);
      for (GearDefinition gear : ArmoryContent.gear()) {
         if (gear.energyCapacity() <= 0) {
            continue;
         }
         stack(gear.id()).ifPresent(gearStack -> views.add(view(
            id("recharge/" + gear.id().getPath()),
            RECHARGE,
            "Recharge " + gear.title(),
            machine,
            List.of(IndexRecipeSlot.input(gearStack),
               IndexRecipeSlot.inputs(List.of(new ItemStack(ModItems.VEIL_CRYSTAL.get()),
                  new ItemStack(ModItems.RESONANCE_SHARD.get()))),
               IndexRecipeSlot.machine(machine), IndexRecipeSlot.output(gearStack)),
            List.of("Restores energy to " + gear.energyCapacity(), "Consumes one valid energy fuel."))));
      }
   }

   private static void addRepairs(List<IndexRecipeView> views) {
      ItemStack machine = stationStack(StationKind.ARMORY_BENCH);
      for (GearDefinition gear : ArmoryContent.gear()) {
         stack(gear.id()).filter(ItemStack::isDamageableItem).ifPresent(gearStack -> views.add(view(
            id("repair/" + gear.id().getPath()),
            REPAIR,
            "Repair " + gear.title(),
            machine,
            List.of(IndexRecipeSlot.input(gearStack),
               IndexRecipeSlot.input(new ItemStack(ModItems.ARMORY_ALLOY_PLATE.get())),
               IndexRecipeSlot.machine(machine), IndexRecipeSlot.output(gearStack)),
            List.of("Restores roughly one third durability.", "Consumes one Armory Alloy Plate."))));
      }
   }

   private static void addTierUpgrades(List<IndexRecipeView> views) {
      for (GearDefinition gear : ArmoryContent.gear()) {
         ItemStack gearStack = stack(gear.id()).orElse(ItemStack.EMPTY);
         if (gearStack.isEmpty()) {
            continue;
         }
         ArmoryGearItem.ArmoryGearKind kind = gearKind(gearStack);
         if (kind != ArmoryGearItem.ArmoryGearKind.WEAPON && kind != ArmoryGearItem.ArmoryGearKind.ARMOR) {
            continue;
         }
         boolean weapon = kind == ArmoryGearItem.ArmoryGearKind.WEAPON;
         ItemStack machine = stationStack(weapon ? StationKind.WEAPON_FORGE : StationKind.ARMOR_FORGE);
         for (int tier = Math.max(1, gear.tier()); tier < 4; tier++) {
            views.add(view(
               id("tier_upgrade/" + gear.id().getPath() + "/" + tier),
               TIER_UPGRADE,
               gear.title() + " Tier " + tier + " Upgrade",
               machine,
               List.of(IndexRecipeSlot.input(gearStack), IndexRecipeSlot.input(upgradeMaterial(tier, weapon)),
                  IndexRecipeSlot.machine(machine), IndexRecipeSlot.output(gearStack)),
               List.of("Tier " + tier + " -> " + (tier + 1), gear.craftingStage())));
         }
      }
   }

   private static void addGearOperationViews(List<IndexRecipeView> views) {
      for (GearDefinition gear : ArmoryContent.gear()) {
         ItemStack gearStack = stack(gear.id()).orElse(ItemStack.EMPTY);
         if (gearStack.isEmpty()) {
            continue;
         }
         ItemStack sigil = stationStack(StationKind.SIGIL_ENGRAVER);
         views.add(view(
            id("sigil/" + gear.id().getPath()),
            SIGIL,
            "Engrave " + gear.title(),
            sigil,
            List.of(IndexRecipeSlot.input(gearStack), IndexRecipeSlot.machine(sigil),
               IndexRecipeSlot.output(gearStack), IndexRecipeSlot.info("AUX item or station signature becomes trim data.")),
            List.of("Applies a cosmetic sigil without changing the base gear item.")));

         ItemStack loadout = stationStack(StationKind.LOADOUT_TERMINAL);
         views.add(view(
            id("loadout/" + gear.id().getPath()),
            LOADOUT,
            "Bind " + gear.title() + " Loadout",
            loadout,
            List.of(IndexRecipeSlot.input(gearStack), IndexRecipeSlot.machine(loadout),
               IndexRecipeSlot.output(gearStack), IndexRecipeSlot.info("Binds the gear to the current operator kit.")),
            List.of("Stores a manual loadout id on the inserted gear.")));
      }
   }

   private static void addStorageViews(List<IndexRecipeView> views) {
      for (StationKind kind : List.of(StationKind.WEAPON_RACK, StationKind.ARMOR_STAND, StationKind.CONSTRUCT_DOCK)) {
         ItemStack machine = stationStack(kind);
         views.add(view(
            id("storage/" + kind.getSerializedName()),
            STORAGE,
            kind.displayName() + " Scan",
            machine,
            List.of(IndexRecipeSlot.machine(machine), IndexRecipeSlot.info("Stores, scans, or stages Armory gear.")),
            List.of("Storage stations preserve inserted gear state and expose station feedback.")));
      }
   }

   private static IndexRecipeView view(Identifier id, Identifier category, String title, ItemStack machine,
         List<IndexRecipeSlot> slots, List<String> notes) {
      return new IndexRecipeView(id, category, title, machine, slots, notes, 80, false, EchoArmory.MODID);
   }

   private static IndexRecipeCategory category(Identifier id, String title, StationKind station, int order) {
      return new IndexRecipeCategory(id, title, stationStack(station), 0xFF66E8FF, order);
   }

   private static String summary(StationKind kind) {
      return switch (kind) {
         case MODULE_UPGRADE_TABLE, VEIL_INFUSER, CONSTRUCT_DOCK -> "Installs compatible modules into Armory gear.";
         case ENERGY_CORE_CHARGING_STATION -> "Recharges energy gear using Veil or resonance fuel.";
         case ARMORY_BENCH -> "Tunes and repairs Armory gear.";
         case WEAPON_FORGE -> "Upgrades weapon tiers using staged materials.";
         case ARMOR_FORGE -> "Upgrades armor tiers using staged materials.";
         case SIGIL_ENGRAVER -> "Applies cosmetic sigil trim data.";
         case LOADOUT_TERMINAL -> "Binds operator loadouts to gear.";
         case WEAPON_RACK, ARMOR_STAND -> "Stores and scans Armory equipment.";
      };
   }

   private static ItemStack upgradeMaterial(int tier, boolean weapon) {
      if (tier <= 1) {
         return weapon ? new ItemStack(ModItems.RESONANCE_SHARD.get()) : new ItemStack(ModItems.ARMORY_ALLOY_PLATE.get());
      }
      if (tier == 2) {
         return new ItemStack(ModItems.VEIL_CRYSTAL.get());
      }
      return new ItemStack(ModItems.BLACKBOX_FRAGMENT.get());
   }

   private static ArmoryGearItem.ArmoryGearKind gearKind(ItemStack stack) {
      return stack.getItem() instanceof ArmoryGearItem gearItem ? gearItem.gearKind() : ArmoryGearItem.ArmoryGearKind.UTILITY;
   }

   private static Optional<ItemStack> stack(Identifier id) {
      return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new);
   }

   private static ItemStack stationStack(StationKind kind) {
      return new ItemStack(stationBlock(kind).get());
   }

   private static EchoBackendRegistryEntry<Block> stationBlock(StationKind kind) {
      return switch (kind) {
         case ARMORY_BENCH -> ModBlocks.ARMORY_BENCH;
         case WEAPON_FORGE -> ModBlocks.WEAPON_FORGE;
         case ARMOR_FORGE -> ModBlocks.ARMOR_FORGE;
         case ENERGY_CORE_CHARGING_STATION -> ModBlocks.ENERGY_CORE_CHARGING_STATION;
         case MODULE_UPGRADE_TABLE -> ModBlocks.MODULE_UPGRADE_TABLE;
         case SIGIL_ENGRAVER -> ModBlocks.SIGIL_ENGRAVER;
         case LOADOUT_TERMINAL -> ModBlocks.LOADOUT_TERMINAL;
         case WEAPON_RACK -> ModBlocks.WEAPON_RACK;
         case ARMOR_STAND -> ModBlocks.ARMOR_STAND;
         case VEIL_INFUSER -> ModBlocks.VEIL_INFUSER;
         case CONSTRUCT_DOCK -> ModBlocks.CONSTRUCT_DOCK;
      };
   }

   private static Identifier itemId(ItemStack stack) {
      Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? Identifier.withDefaultNamespace("air") : id;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoArmory.MODID, sanitize(path));
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

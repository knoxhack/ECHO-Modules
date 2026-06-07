package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.item.ReclamationSeedItem;
import com.knoxhack.echoagriculturereclamation.item.ReclamationUtilityItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoAgricultureReclamation.MODID);
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();
   private static final Map<String, EchoBackendRegistryEntry<Item>> PRODUCE = new LinkedHashMap<>();

   public static final EchoBackendRegistryEntry<Item> RECOVERED_SEED_CAPSULE = tracked(item(
      "recovered_seed_capsule", p -> new ReclamationSeedItem(ReclamationSeedItem.Mode.CAPSULE, p.stacksTo(16))));
   public static final EchoBackendRegistryEntry<Item> CONTAMINATED_SEED = tracked(item(
      "contaminated_seed", p -> new ReclamationSeedItem(ReclamationSeedItem.Mode.CONTAMINATED, p.stacksTo(64))));
   public static final EchoBackendRegistryEntry<Item> STABILIZED_SEED = tracked(item(
      "stabilized_seed", p -> new ReclamationSeedItem(ReclamationSeedItem.Mode.STABILIZED, p.stacksTo(64))));
   public static final EchoBackendRegistryEntry<Item> GENE_SAMPLE = tracked(item(
      "gene_sample", p -> new ReclamationUtilityItem("tooltip.echoagriculturereclamation.gene_sample", p)));
   public static final EchoBackendRegistryEntry<Item> SOIL_NUTRIENT_MIX = tracked(item(
      "soil_nutrient_mix", p -> new ReclamationUtilityItem("tooltip.echoagriculturereclamation.soil_nutrient_mix", p)));
   public static final EchoBackendRegistryEntry<Item> PURIFICATION_ENZYME = tracked(item(
      "purification_enzyme", p -> new ReclamationUtilityItem("tooltip.echoagriculturereclamation.purification_enzyme", p)));
   public static final EchoBackendRegistryEntry<Item> BIO_GEL = tracked(item(
      "bio_gel", p -> new ReclamationUtilityItem("tooltip.echoagriculturereclamation.bio_gel", p)));

   public static final EchoBackendRegistryEntry<Item> ASH_WHEAT = produce(CropSpec.byPath("ash_wheat"));
   public static final EchoBackendRegistryEntry<Item> HARDROOT = produce(CropSpec.byPath("hardroot"));
   public static final EchoBackendRegistryEntry<Item> GLOW_BEANS = produce(CropSpec.byPath("glow_beans"));
   public static final EchoBackendRegistryEntry<Item> RADLEAF = produce(CropSpec.byPath("radleaf"));
   public static final EchoBackendRegistryEntry<Item> MUTANT_BERRIES = produce(CropSpec.byPath("mutant_berries"));
   public static final EchoBackendRegistryEntry<Item> CRYO_MOSS = produce(CropSpec.byPath("cryo_moss"));
   public static final EchoBackendRegistryEntry<Item> CLEAN_CORN = produce(CropSpec.byPath("clean_corn"));
   public static final EchoBackendRegistryEntry<Item> MEDICINAL_ALOE = produce(CropSpec.byPath("medicinal_aloe"));
   public static final EchoBackendRegistryEntry<Item> FILTER_REED = produce(CropSpec.byPath("filter_reed"));
   public static final EchoBackendRegistryEntry<Item> NEXUS_ORCHID = produce(CropSpec.byPath("nexus_orchid"));
   public static final EchoBackendRegistryEntry<Item> SIGNAL_FUNGUS = produce(CropSpec.byPath("signal_fungus"));

   static {
      ModBlocks.blockItems().forEach(block -> tracked(EchoBackendRegistryBridge.registerWithId(ITEMS, block.id().getPath(),
         id -> new BlockItem(block.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()))));
   }

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   public static EchoBackendRegistryEntry<Item> produceFor(CropSpec spec) {
      return PRODUCE.get(spec.path());
   }

   public static DataComponentType<SeedProfile> seedProfileComponent() {
      return ModDataComponents.SEED_PROFILE.get();
   }

   private static EchoBackendRegistryEntry<Item> produce(CropSpec spec) {
      FoodProperties food = foodFor(spec);
      EchoBackendRegistryEntry<Item> item = food == null
         ? tracked(item(spec.path(), properties -> new ReclamationUtilityItem("tooltip.echoagriculturereclamation.crop." + spec.path(), properties)))
         : tracked(item(spec.path(), properties -> new Item(properties.food(food))));
      PRODUCE.put(spec.path(), item);
      return item;
   }

   private static FoodProperties foodFor(CropSpec spec) {
      return switch (spec.path()) {
         case "ash_wheat" -> new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build();
         case "hardroot" -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build();
         case "glow_beans" -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.25F).build();
         case "mutant_berries" -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build();
         case "clean_corn" -> new FoodProperties.Builder().nutrition(3).saturationModifier(0.35F).build();
         default -> null;
      };
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> item(String name, java.util.function.Function<Item.Properties, T> factory) {
      return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id ->
         factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }
}

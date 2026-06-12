package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.schematic.SchematicTier;
import com.knoxhack.echoashfallprotocol.schematic.SchematicUnlockTable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AshfallRecipeInfoCatalog {
    private AshfallRecipeInfoCatalog() {
    }

    public static List<InfoEntry> entries() {
        List<InfoEntry> entries = new ArrayList<>();
        add(entries, new ItemStack(ModBlocks.RAIN_COLLECTOR_ITEM.get()),
                Component.literal("Collects dirty water during rain. Use the block while rain reaches the collector."));
        add(entries, new ItemStack(ModBlocks.HAND_RECYCLER_ITEM.get()),
                Component.literal("First-hour machine bridge: craft with 1 Machine Casing, 4 Scrap Metal, and 4 Scrap Wire. Completing the route step issues a 1000 FE starter battery for the first casing."));
        add(entries, new ItemStack(ModBlocks.MICRO_GENERATOR_ITEM.get()),
                Component.literal("Starter power source. The first-hour recipe uses 1 Machine Casing, 3 Scrap Wire, 1 Energy Cell, 1 Circuit Board, and 1 Scrap Metal."));
        add(entries, new ItemStack(ModBlocks.WATER_PURIFIER_ITEM.get()),
                Component.literal("Stable-base water loop. Build with 3 Machine Casings, 2 Scrap Plastic, 1 Filtration Membrane, and 1 Circuit Board, then feed dirty water and filters."));
        add(entries, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()),
                Component.literal("Basic filters have three early recipes: Scrap Plastic + Ash + Scrap Wire, String, or Ashgrass Fiber + extra Ash. Rarely recovered from toxic salvage and scavengers."));
        add(entries, new ItemStack(ModItems.RAD_AWAY.get()), Component.translatable("jei.EchoAshfallProtocol.rad_away.info"));
        add(entries, new ItemStack(ModItems.BASIC_BATTERY.get()), Component.translatable("jei.EchoAshfallProtocol.battery.info"));
        add(entries, new ItemStack(ModItems.ADVANCED_BATTERY.get()), Component.translatable("jei.EchoAshfallProtocol.battery.info"));
        add(entries, new ItemStack(ModItems.ELITE_BATTERY.get()), Component.translatable("jei.EchoAshfallProtocol.battery.info"));
        add(entries, new ItemStack(ModBlocks.POWER_CABLE_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.cables"));
        add(entries, new ItemStack(ModBlocks.REINFORCED_POWER_CABLE_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.cables"));
        add(entries, new ItemStack(ModBlocks.HIGH_VOLTAGE_POWER_CABLE_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.cables"));
        add(entries, new ItemStack(ModBlocks.ENERGY_METER_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.meter"));
        add(entries, new ItemStack(ModBlocks.LOAD_DISTRIBUTOR_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.distributor"));
        add(entries, new ItemStack(ModBlocks.SCRAP_DYNAMO_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.scrap_dynamo"));
        add(entries, new ItemStack(ModBlocks.NEXUS_CAPACITOR_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.energy.nexus_capacitor"));
        add(entries, new ItemStack(ModBlocks.THERMAL_ARRAY_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.thermal_array"));
        add(entries, new ItemStack(ModBlocks.ORE_GRINDER_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.ore_grinder"));
        add(entries, new ItemStack(ModBlocks.ISOTOPE_REFINER_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.isotope_refiner"));
        add(entries, new ItemStack(ModBlocks.FIELD_MED_BAY_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.field_med_bay"));
        add(entries, new ItemStack(ModBlocks.ATMOSPHERIC_SCRUBBER_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.scrubber"));
        add(entries, new ItemStack(ModBlocks.RADIATION_CLEANSER_ITEM.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.cleanser"));
        add(entries, new ItemStack(ModItems.ALLOY_BLADE.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_weapon"));
        add(entries, new ItemStack(ModItems.ALLOY_HAMMER.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_weapon"));
        add(entries, new ItemStack(ModItems.ALLOY_HELMET.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_armor"));
        add(entries, new ItemStack(ModItems.ALLOY_CHESTPLATE.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_armor"));
        add(entries, new ItemStack(ModItems.ALLOY_LEGGINGS.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_armor"));
        add(entries, new ItemStack(ModItems.ALLOY_BOOTS.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.alloy_armor"));
        add(entries, new ItemStack(ModItems.HAZMAT_HELMET.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.hazmat"));
        add(entries, new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.hazmat"));
        add(entries, new ItemStack(ModItems.HAZMAT_LEGGINGS.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.hazmat"));
        add(entries, new ItemStack(ModItems.HAZMAT_BOOTS.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.hazmat"));
        add(entries, new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.advanced_filter"));
        add(entries, new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get()), Component.translatable("jei.EchoAshfallProtocol.midgame.route_supplies"));
        add(entries, new ItemStack(ModItems.MACHINE_UPGRADE_EFFICIENCY.get()), Component.translatable("jei.EchoAshfallProtocol.energy.upgrades"));
        add(entries, new ItemStack(ModItems.MACHINE_UPGRADE_OVERCLOCK.get()), Component.translatable("jei.EchoAshfallProtocol.energy.upgrades"));
        add(entries, new ItemStack(ModBlocks.DEEP_CORE_MINER_ITEM.get()),
                Component.literal("Generates random core samples only when powered and placed at Y <= -32."));
        gatedItemInfo().forEach((item, info) -> add(entries, new ItemStack(item), info));
        return List.copyOf(entries);
    }

    public static Map<Item, Component> gatedItemInfo() {
        Map<Item, Component> info = new java.util.LinkedHashMap<>();
        for (Map.Entry<SchematicTier, List<String>> entry : gatedByTier().entrySet()) {
            for (String recipeId : entry.getValue()) {
                Identifier id = Identifier.tryParse(recipeId);
                if (id == null) {
                    continue;
                }
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item ->
                        info.put(item, Component.literal("Requires " + entry.getKey().name() + " schematic unlock.")));
            }
        }
        return Map.copyOf(info);
    }

    private static void add(List<InfoEntry> entries, ItemStack stack, Component text) {
        entries.add(new InfoEntry(stack, text));
    }

    private static Map<SchematicTier, List<String>> gatedByTier() {
        Map<SchematicTier, List<String>> map = new EnumMap<>(SchematicTier.class);
        for (SchematicTier tier : SchematicTier.values()) {
            map.put(tier, SchematicUnlockTable.recipesFor(tier));
        }
        return map;
    }

    public record InfoEntry(ItemStack stack, Component text) {
        public InfoEntry {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            text = text == null ? Component.empty() : text;
        }
    }
}

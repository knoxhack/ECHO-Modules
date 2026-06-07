package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.item.BlueprintItem;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoMultiblockCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> GRIPPER_HEAD = toolHead("gripper_head", RobotToolType.GRIPPER);
    public static final EchoBackendRegistryEntry<Item> WELDER_HEAD = toolHead("welder_head", RobotToolType.WELDER);
    public static final EchoBackendRegistryEntry<Item> SCANNER_HEAD = toolHead("scanner_head", RobotToolType.SCANNER);
    public static final EchoBackendRegistryEntry<Item> ASSEMBLER_HEAD = toolHead("assembler_head", RobotToolType.ASSEMBLER);
    public static final EchoBackendRegistryEntry<Item> INJECTOR_HEAD = toolHead("injector_head", RobotToolType.INJECTOR);
    public static final EchoBackendRegistryEntry<Item> CUTTER_HEAD = toolHead("cutter_head", RobotToolType.CUTTER);
    public static final EchoBackendRegistryEntry<Item> CLAMP_HEAD = toolHead("clamp_head", RobotToolType.CLAMP);
    public static final EchoBackendRegistryEntry<Item> DRILL_HEAD = toolHead("drill_head", RobotToolType.DRILL);
    public static final EchoBackendRegistryEntry<Item> CALIBRATOR_HEAD = toolHead("calibrator_head", RobotToolType.CALIBRATOR);
    public static final EchoBackendRegistryEntry<Item> STABILIZER_HEAD = toolHead("stabilizer_head", RobotToolType.STABILIZER);
    public static final EchoBackendRegistryEntry<Item> SIGNAL_CIRCUIT = simple("signal_circuit");
    public static final EchoBackendRegistryEntry<Item> CALIBRATED_BUS_MODULE = simple("calibrated_bus_module");
    public static final EchoBackendRegistryEntry<Item> MACHINE_CASING = simple("machine_casing");
    public static final EchoBackendRegistryEntry<Item> SUPPLY_MANIFEST = simple("supply_manifest");
    public static final EchoBackendRegistryEntry<Item> SCANNER_MATRIX = simple("scanner_matrix");
    public static final EchoBackendRegistryEntry<Item> VEHICLE_FRAME_KIT = simple("vehicle_frame_kit");
    public static final EchoBackendRegistryEntry<Item> LAUNCH_GUIDANCE_CORE = simple("launch_guidance_core");
    public static final EchoBackendRegistryEntry<Item> ARCHIVE_MEMORY_CELL = simple("archive_memory_cell");
    public static final EchoBackendRegistryEntry<Item> RECLAMATION_GROWTH_MATRIX = simple("reclamation_growth_matrix");
    public static final EchoBackendRegistryEntry<Item> NEXUS_FIELD_COIL = simple("nexus_field_coil");
    public static final EchoBackendRegistryEntry<Item> ARMORY_PATTERN_CORE = simple("armory_pattern_core");
    public static final EchoBackendRegistryEntry<Item> CONSTRUCTION_PLANNER = simple("construction_planner");
    public static final EchoBackendRegistryEntry<Item> SPEED_UPGRADE = simple("speed_upgrade");
    public static final EchoBackendRegistryEntry<Item> REACH_UPGRADE = simple("reach_upgrade");
    public static final EchoBackendRegistryEntry<Item> COOLING_UPGRADE = simple("cooling_upgrade");
    public static final EchoBackendRegistryEntry<Item> INTEGRITY_UPGRADE = simple("integrity_upgrade");
    public static final EchoBackendRegistryEntry<Item> AUTO_BUILDER_CORE = simple("auto_builder_core");
    public static final EchoBackendRegistryEntry<Item> SIGNAL_TOWER_BLUEPRINT = tracked(register(
            "signal_tower_blueprint",
            properties -> new BlueprintItem(EchoMultiblockCore.id("signal_tower_tier_1"), properties),
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_ASSEMBLY_LINE_BLUEPRINT = tracked(register(
            "industrial_assembly_line_blueprint",
            properties -> new BlueprintItem(EchoMultiblockCore.id("industrial_assembly_line"), properties),
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final EchoBackendRegistryEntry<Item> LOGISTICS_DEPOT_BLUEPRINT = blueprint("logistics_depot_blueprint", "logistics_depot");
    public static final EchoBackendRegistryEntry<Item> SCANNER_ARRAY_BLUEPRINT = blueprint("scanner_array_blueprint", "scanner_array");
    public static final EchoBackendRegistryEntry<Item> VEHICLE_REPAIR_GANTRY_BLUEPRINT = blueprint("vehicle_repair_gantry_blueprint", "vehicle_repair_gantry");
    public static final EchoBackendRegistryEntry<Item> ORBITAL_LAUNCH_PLATFORM_BLUEPRINT = blueprint("orbital_launch_platform_blueprint", "orbital_launch_platform");
    public static final EchoBackendRegistryEntry<Item> ARCHIVE_DATA_CHAMBER_BLUEPRINT = blueprint("archive_data_chamber_blueprint", "archive_data_chamber");
    public static final EchoBackendRegistryEntry<Item> AGRICULTURE_DOME_BLUEPRINT = blueprint("agriculture_dome_blueprint", "agriculture_dome");
    public static final EchoBackendRegistryEntry<Item> NEXUS_STABILIZER_BLUEPRINT = blueprint("nexus_stabilizer_blueprint", "nexus_stabilizer");
    public static final EchoBackendRegistryEntry<Item> ARMORY_FABRICATOR_BLUEPRINT = blueprint("armory_fabricator_blueprint", "armory_fabricator");
    public static final EchoBackendRegistryEntry<Item> AUTO_BUILDER_YARD_BLUEPRINT = blueprint("auto_builder_yard_blueprint", "auto_builder_yard");

    static {
        ModBlocks.ALL_BLOCKS.forEach(block -> tracked(registerBlockItem(block.id().getPath(), block)));
    }

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static EchoBackendRegistryEntry<Item> toolHead(String name, RobotToolType toolType) {
        return tracked(register(name, properties -> new ToolHeadItem(toolType, properties), p -> p.stacksTo(1)));
    }

    private static EchoBackendRegistryEntry<Item> blueprint(String itemName, String definitionPath) {
        return tracked(register(
                itemName,
                properties -> new BlueprintItem(EchoMultiblockCore.id(definitionPath), properties),
                p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    }

    private static EchoBackendRegistryEntry<Item> simple(String name) {
        return tracked(register(name, Item::new, Function.identity()));
    }

    private static EchoBackendRegistryEntry<Item> registerBlockItem(String name, EchoBackendRegistryEntry<? extends net.minecraft.world.level.block.Block> block) {
        return register(name, properties -> new BlockItem(block.get(), properties), Function.identity());
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> register(
            String name,
            Function<Item.Properties, ? extends T> factory,
            Function<Item.Properties, Item.Properties> propertiesFactory
    ) {
        return EchoBackendRegistryBridge.registerWithId(
                ITEMS,
                name,
                id -> factory.apply(withId(propertiesFactory.apply(new Item.Properties()), id)));
    }

    private static Item.Properties withId(Item.Properties properties, Identifier id) {
        return properties.setId(ResourceKey.create(Registries.ITEM, id));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}

package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.world.item.Item;
import java.util.HashMap;
import java.util.Map;

/**
 * Static database for the ECHO Terminal Encyclopedia.
 */
public class EncyclopediaRegistry {
    private static final Map<Item, Entry> DATABASE = new HashMap<>();

    public record Entry(String title, String description, String usage) {}

    static {
        register(ModItems.SCRAP_METAL.get(), new Entry(
            "Scrap Metal",
            "Twisted, rusted iron salvaged from various pre-fall structures. While structurally weak, it remains highly conductive.",
            "Primary component for Machine Casings and basic tools. Can be refined into Iron Shards."
        ));

        register(ModItems.SCRAP_WIRE.get(), new Entry(
            "Scrap Wire",
            "Tangled copper filaments with degraded insulation. High electrical resistance due to corrosion.",
            "Used to craft basic Circuit Boards and connects machines to early-game Power Networks."
        ));

        register(ModItems.SCRAP_CIRCUIT.get(), new Entry(
            "Scrap Circuit",
            "A scorched logic board from a pre-fall diagnostic unit. Most components are fried, but the core processor is viable.",
            "Essential for constructing high-tier machines like the Isotope Refiner and Signal Scanner."
        ));

        register(ModItems.ENERGY_CELL.get(), new Entry(
            "Energy Cell",
            "A standard-issue lithium-ion storage unit. Highly unstable if punctured. Provides sustained voltage for machines.",
            "Powers the Thermal Burner and can be inserted into Power Nodes to jumpstart the grid."
        ));

        register(ModItems.FILTER_CARTRIDGE_BASIC.get(), new Entry(
            "Basic Filter",
            "A rudimentary carbon-based filtration unit. Effective against large particles but fails against fine radioactive dust.",
            "Provides limited air protection inside toxic-air hazard zones. Storms do not drain filters by themselves."
        ));

        register(ModItems.DENSE_ALLOY_CHUNK.get(), new Entry(
            "Dense Alloy",
            "A heavy, dark compound created by smelting refined isotopes with scrap metal. Nearly impervious to environmental decay.",
            "Used for Phase 4 gear and machines. Necessary for surviving the deepest radiation zones."
        ));

        // Machine Integration Items
        register(ModBlocks.ITEM_PIPE_ITEM.get(), new Entry(
            "Item Pipe",
            "Directional transport conduit for automated material handling between machines.",
            "Place between machines to transfer items. Direction determines extraction side. 8-tick cooldown."
        ));

        register(ModBlocks.POWER_CABLE_ITEM.get(), new Entry(
            "Power Cable",
            "Wired energy distribution for extending power networks beyond generator range.",
            "1000 FE capacity, 50 FE/t transfer. Connects machines to power sources."
        ));

        register(ModBlocks.FACTORY_CONTROLLER_ITEM.get(), new Entry(
            "Factory Controller",
            "Centralized monitoring and control unit for factory automation networks.",
            "16-block scan radius. View machine status, toggle power. Respects redstone signals."
        ));

        register(ModItems.MACHINE_CASING.get(), new Entry(
            "Machine Casing",
            "Reinforced structural shell used in all machine construction.",
            "Primary component for Hand Recycler, Micro Generator, and all Tier 2+ machines."
        ));

        register(ModItems.CIRCUIT_BOARD.get(), new Entry(
            "Circuit Board",
            "Processed electronics for advanced machine control systems.",
            "Required for Tier 2 machines: Signal Scanner, Isotope Refiner, Research Lab, Item Pipes."
        ));

        register(ModItems.NEXUS_CRYSTAL.get(), new Entry(
            "Nexus Crystal",
            "Resonant crystal formed in the Crystalline Synthesizer. Contains Grid energy signatures.",
            "Endgame material for Nexus Core activation and high-tier equipment."
        ));

        register(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get(), new Entry(
            "Schematic Fragment",
            "Damaged data storage from pre-fall technology. Contains partial blueprints.",
            "Process at Research Lab to unlock perks. 5 types: Power, Mechanical, Bio, Defense, Comms."
        ));
    }

    private static void register(Item item, Entry entry) {
        DATABASE.put(item, entry);
    }

    public static Entry getEntry(Item item) {
        return DATABASE.get(item);
    }

    public static Map<Item, Entry> getDatabase() {
        return DATABASE;
    }
}

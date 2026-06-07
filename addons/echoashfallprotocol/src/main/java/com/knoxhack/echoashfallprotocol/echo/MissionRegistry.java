package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.item.GasMaskItem;
import com.knoxhack.echoashfallprotocol.item.DataLogItem;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/**
 * Static registry of all ECHO-7 missions organized by 9 narrative phases.
 *
 * Protocols follow the "Full Game Vision":
 * 0. CRASH_LANDING - Tutorial and basic survival
 * 1. WILDERNESS - First night primitive tools
 * 2. RESOURCE_STABILITY - Machine construction
 * 3. FACTION_BRIDGE - Research and reputation
 * 4. BIOLOGICAL_ADAPTATION - Mutations and medical
 * 5. GEOLOGICAL_EXTRACTION - Dense alloys and refinement
 * 6. GRID_RESTORATION - Power nodes and exploration
 * 7. NEXUS_INTEGRATION - The final choice
 * 8. POST_NEXUS_ENDGAME - Path-dependent completion
 */
public class MissionRegistry {
    private static final Map<Integer, List<Mission>> PHASES = new LinkedHashMap<>();
    private static final Map<BlockProbeKey, BlockProbeResult> BLOCK_PROBE_CACHE = new HashMap<>();
    private static final int BLOCK_PROBE_CACHE_TICKS = 120;
    private static final int BLOCK_PROBE_CACHE_MAX = 1024;

    static {
        // === PHASE 0: CRASH LANDING ===
        List<Mission> phase0 = new ArrayList<>();
        phase0.add(new Mission(
                "drink_clean_water",
                "[ECHO-7] Hydration buffer required. Drink or carry Clean Water before extending beyond the pod; ECHO needs proof that the starter supply can still keep you upright.",
                "Confirm Clean Water",
                "[ECHO-7] Clean water confirmed. Starter supply buys time; it does not solve the water problem.",
                List.of(new ItemStack(Items.GLASS_BOTTLE, 2), new ItemStack(ModItems.EMERGENCY_RATION.get(), 1)),
                player -> hasSpecialMarker(player, "water:clean_consumed") || player.getInventory().contains(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get())),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.TRIVIAL,
                List.of("craft_scrap_knife"),
                true,
                "clean_water_bottle"
        ));
        phase0.add(new Mission(
                "secure_crash_outpost",
                "[ECHO-7] Anchor the pod route first. Craft and place an Ash Campfire near the crash site, then keep storage, light, and ECHO objectives at the outpost.",
                "Anchor Pod Outpost",
                "[ECHO-7] Pod outpost anchored. Keep storage, light, and early machines near this position until water and power are stable.",
                List.of(new ItemStack(Items.TORCH, 16), new ItemStack(Items.CHEST, 1), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.SCRAP_METAL.get(), 4), new ItemStack(ModItems.PLANT_FIBER.get(), 4)),
                player -> hasBlockNearPlayer(player, "ash_campfire"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                Collections.emptyList(),
                true,
                "ash_campfire",
                List.of(new Mission.BlockRequirement("ash_campfire", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase0.add(new Mission(
                "craft_scrap_knife",
                "[ECHO-7] The Scrap Knife is a salvage tool first. Cut wire, open loose wreckage, and recover scrap near the pod before machine metal becomes scarce.",
                "Craft Scrap Knife",
                "[ECHO-7] Scrap Knife logged as salvage gear. Keep it for wreckage work; the travel kit can use bone and hide while metal feeds machines.",
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 12), new ItemStack(ModItems.SCRAP_WIRE.get(), 4), new ItemStack(ModItems.EMERGENCY_RATION.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModItems.SCRAP_KNIFE.get())),
                List.of(new ItemStack(ModItems.SCRAP_KNIFE.get(), 1)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.EASY,
                List.of("secure_crash_outpost"),
                true,
                "scrap_knife"
        ));
        phase0.add(new Mission(
                "get_dirty_water",
                "[ECHO-7] Long-term hydration requires input. Fill a bottle from any water source, then return the protocol for ECHO validation.",
                "Collect Dirty Water + Turn In",
                "[ECHO-7] Water sample acquired. Dirty water is emergency-usable, but clean water remains the plan. Purify before long routes.",
                List.of(new ItemStack(ModItems.ASH.get(), 8), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get())),
                List.of(new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("drink_clean_water"),
                true,
                "dirty_water_bottle"
        ));
        phase0.add(new Mission(
                "emergency_filter_water",
                "[ECHO-7] Dirty water can keep you moving, but purification is safer. Emergency-filter one Clean Water Bottle with ash and a Basic Filter, then report back.",
                "Make Clean Water + Turn In",
                "[ECHO-7] Clean sample confirmed. This manual method burns a full filter; build a Water Purifier for sustained hydration.",
                List.of(new ItemStack(Items.GLASS_BOTTLE, 2), new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get())),
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("get_dirty_water"),
                true,
                "emergency_clean_water"
        ));
        phase0.add(new Mission(
                "secure_emergency_water_loop",
                "[ECHO-7] Starter water will run dry. Collect unsafe water, then emergency-filter one bottle with ash and a Basic Filter so the pod route has a manual fallback before machines exist.",
                "Prove Emergency Water Loop",
                "[ECHO-7] Emergency water loop proven. It is slow and filter-hungry, but it keeps you alive long enough to build powered purification.",
                List.of(new ItemStack(ModItems.ASH.get(), 8), new ItemStack(Items.GLASS_BOTTLE, 2), new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1)),
                player -> hasSpecialMarker(player, "water:dirty_collected")
                        && hasSpecialMarker(player, "water:emergency_filtered"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("drink_clean_water"),
                true,
                "emergency_clean_water",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        new Mission.LocationRequirement("special", "water:dirty_collected", "Dirty Water Collected"),
                        new Mission.LocationRequirement("special", "water:emergency_filtered", "Emergency Filtration Proven")
                )
        ));
        phase0.sort(Comparator.comparingInt(m -> switch (m.id()) {
            case "secure_crash_outpost" -> 0;
            case "craft_scrap_knife" -> 1;
            case "drink_clean_water" -> 2;
            case "secure_emergency_water_loop" -> 3;
            case "get_dirty_water" -> 4;
            case "emergency_filter_water" -> 5;
            default -> 99;
        }));
        PHASES.put(0, phase0); // Phase 0: Crash Landing
        // === PHASE 1: WILDERNESS (Night 1 Survival) ===
        List<Mission> phase05 = new ArrayList<>();
        phase05.add(new Mission(
                "forage_wasteland_food",
                "[ECHO-7] Confirm a food buffer before leaving the crash outpost. Starter rations qualify; wild berries extend the margin.",
                "Confirm Food Buffer + Turn In",
                "[ECHO-7] Food buffer logged. Mutated sapling recovered from the supply scan; plant it near the pod before expanding the route.",
                List.of(new ItemStack(ModItems.PLANT_FIBER.get(), 6), new ItemStack(ModItems.WILD_BERRY.get(), 4), new ItemStack(ModBlocks.MUTATED_SAPLING_ITEM.get(), 1)),
                player -> hasWastelandFood(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("craft_scrap_knife"),
                true,
                "wild_berry"
        ));
        phase05.add(new Mission(
                "plant_mutated_sapling",
                "[ECHO-7] Renewable material source required. Plant the Mutated Sapling near shelter before the first route expands.",
                "Plant Mutated Sapling",
                "[ECHO-7] Growth anchor detected. Rain Collector parts released; claim the cauldron and scrap plastic before water reserves collapse.",
                List.of(new ItemStack(Items.BONE_MEAL, 4), new ItemStack(ModItems.PLANT_FIBER.get(), 6), new ItemStack(Items.CAULDRON, 1), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 8)),
                player -> hasBlockNearPlayer(player, "mutated_sapling"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("forage_wasteland_food"),
                true,
                "mutated_sapling",
                List.of(new Mission.BlockRequirement("mutated_sapling", 1, "Mutated Sapling")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase05.add(new Mission(
                "build_rain_collector",
                "[ECHO-7] Weather can refill bottles if you catch it. Use the rewarded cauldron and scrap plastic to place a Rain Collector near shelter.",
                "Build Rain Collector",
                "[ECHO-7] Rain catchment online. It produces dirty water; purify it before drinking.",
                List.of(new ItemStack(Items.GLASS_BOTTLE, 3), new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 1)),
                player -> hasBlockNearPlayer(player, "rain_collector"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("plant_mutated_sapling"),
                true,
                "rain_collector",
                List.of(new Mission.BlockRequirement("rain_collector", 1, "Rain Collector")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase05.add(new Mission(
                "stockpile_rations",
                "[ECHO-7] Do not scout hungry. Keep 4 Emergency Rations or 12 Wild Berries at shelter before the next push.",
                "Confirm Ration Buffer + Turn In",
                "[ECHO-7] Food buffer established. Emergency bunk released; keep it at shelter so night travel stays optional.",
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 1), new ItemStack(ModBlocks.EMERGENCY_BUNK_ITEM.get(), 1)),
                player -> hasRationStockpile(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("build_rain_collector"),
                true,
                "emergency_ration"
        ));
        phase05.add(new Mission(
                "secure_sleep_shelter",
                "[ECHO-7] Night movement is optional if shelter exists. Carry or place the bunk and keep it near your crash outpost.",
                "Secure Sleep Shelter",
                "[ECHO-7] Rest point confirmed. Organic tool materials released; build a field kit before scouting so scrap can stay reserved for machines.",
                List.of(new ItemStack(Items.TORCH, 12), new ItemStack(ModItems.EMERGENCY_RATION.get(), 2), new ItemStack(ModItems.ANIMAL_BONE.get(), 2), new ItemStack(ModItems.ANIMAL_HIDE.get(), 4), new ItemStack(Items.STICK, 3)),
                player -> hasAnyBed(player) || QuestData.get(player).hasVisitedLocation("special", "shelter:slept"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("stockpile_rations"),
                true,
                "bed"
        ));
        phase05.add(new Mission(
                "assemble_wasteland_field_kit",
                "[ECHO-7] Stop spending recovered metal on every emergency. Assemble a Bone Knife, Crude Spear, and Hide Wrap so scrap stays in the machine chain.",
                "Assemble Wasteland Field Kit",
                "[ECHO-7] Field kit confirmed. Bone handles close work, the spear keeps distance, and hide buys breathing margin; fabrication can claim the metal now.",
                List.of(
                        new ItemStack(ModItems.FIBER_ROPE.get(), 4),
                        new ItemStack(ModItems.BANDAGE.get(), 3),
                        new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1),
                        new ItemStack(ModItems.STIM_PACK.get(), 1),
                        new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1),
                        new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)
                ),
                player -> player.getInventory().contains(new ItemStack(ModItems.BONE_KNIFE.get()))
                        && player.getInventory().contains(new ItemStack(ModItems.CRUDE_SPEAR.get()))
                        && player.getInventory().contains(new ItemStack(ModItems.HIDE_WRAP.get())),
                List.of(
                        new ItemStack(ModItems.BONE_KNIFE.get(), 1),
                        new ItemStack(ModItems.CRUDE_SPEAR.get(), 1),
                        new ItemStack(ModItems.HIDE_WRAP.get(), 1)
                ),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("secure_sleep_shelter"),
                true,
                null
        ));
        phase05.add(new Mission(
                "craft_bone_knife",
                "[ECHO-7] Metal reserves are exhausted. Improvise with organic materials. Craft a Bone Knife and report back.",
                "Craft Bone Knife + Turn In",
                "[ECHO-7] Primitive blade accepted. Continue assembling wilderness-grade survival tools.",
                List.of(new ItemStack(ModItems.FIBER_ROPE.get(), 4), new ItemStack(ModItems.BANDAGE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.BONE_KNIFE.get())),
                List.of(new ItemStack(ModItems.BONE_KNIFE.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EASY,
                List.of("secure_sleep_shelter"),
                true,
                "bone_knife"
        ));
        phase05.add(new Mission(
                "craft_crude_spear",
                "[ECHO-7] Hostile lifeforms detected nearby. Craft a Crude Spear for first-night defense.",
                "Craft Crude Spear + Turn In",
                "[ECHO-7] Spear frame confirmed. Keep moving and avoid prolonged combat.",
                List.of(new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.CRUDE_SPEAR.get())),
                List.of(new ItemStack(ModItems.CRUDE_SPEAR.get(), 1)),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EASY,
                List.of("craft_bone_knife"),
                true,
                "crude_spear"
        ));
        phase05.add(new Mission(
                "craft_hide_wrap",
                "[ECHO-7] Dust concentration rising. Assemble a Hide Wrap to improve breathing tolerance.",
                "Craft Hide Wrap + Turn In",
                "[ECHO-7] Respiratory wrap accepted. Schematic fragment released; machine progression can begin from the crash outpost.",
                List.of(new ItemStack(ModItems.STIM_PACK.get(), 1), new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1), new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.HIDE_WRAP.get())),
                List.of(new ItemStack(ModItems.HIDE_WRAP.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("craft_crude_spear"),
                true,
                "hide_wrap"
        ));
        phase05.add(new Mission(
                "find_schematic_fragment",
                "[ECHO-7] Authenticate the Schematic Fragment issued through the field kit bridge, or recover one from surface ruins and survivor exchanges.",
                "Authenticate Schematic Fragment",
                "[ECHO-7] Fragment authenticated. Hand Recycler fabrication procedures restored; casing bridge released for the first machine loop.",
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.EMERGENCY_RATION.get(), 4), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get())),
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("assemble_wasteland_field_kit"),
                true,
                "schematic_fragment"
        ));
        PHASES.put(1, phase05); // Phase 1: Wilderness

        // === PHASE 2: RESOURCE STABILITY (Machine Infrastructure) ===
        List<Mission> phase1 = new ArrayList<>();
        phase1.add(new Mission(
                "build_hand_recycler",
                "[ECHO-7] Raw salvage is inefficient. Use the authenticated casing, 4 Scrap Metal, and 4 Scrap Wire to build a Hand Recycler at your crash outpost.",
                "Build a Hand Recycler",
                "[ECHO-7] Recycling unit online. Starter battery reserve issued; use it to turn scrap into the first casing.",
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 12), new ItemStack(ModItems.SCRAP_WIRE.get(), 6)),
                player -> hasBlockNearPlayer(player, "hand_recycler"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.SCRAP_METAL.get(), 4), new ItemStack(ModItems.SCRAP_WIRE.get(), 4)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.NORMAL,
                List.of("find_schematic_fragment"),
                true, // Turn-in mission
                "hand_recycler",
                List.of(new Mission.BlockRequirement("hand_recycler", 1)), // Block requirement
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "make_machine_casing",
                "[ECHO-7] All machines require casings. Use crafted or recycled casings, then claim the generator bridge components.",
                "Make a Machine Casing",
                "[ECHO-7] First casing complete. Generator bridge released: cell, circuit, wire, and one spare casing.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 6), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.MACHINE_CASING.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.MACHINE_CASING.get())),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 1)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.NORMAL,
                List.of("build_hand_recycler"), // Need recycler first
                true, // Turn-in mission
                "machine_casing"
        ));
        phase1.add(new Mission(
                "build_micro_generator",
                "[ECHO-7] Machines require power. Assemble a Micro Generator from one casing, wire, cell, circuit, and scrap beside your recycler.",
                "Build a Micro Generator",
                "[ECHO-7] Power generation online. Purifier bridge released: membrane, plastic, circuit, and scrap for recycler casings.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 4), new ItemStack(ModItems.SCRAP_METAL.get(), 6), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                player -> hasBlockNearPlayer(player, "micro_generator"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.SCRAP_WIRE.get(), 3), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.SCRAP_METAL.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("make_machine_casing"), // Need casing first
                true, // Turn-in mission
                "micro_generator",
                List.of(new Mission.BlockRequirement("micro_generator", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "build_water_purifier",
                "[ECHO-7] Dehydration is imminent. Build a Water Purifier near your generator: scrap plastic, a membrane, three machine casings, and a circuit board.",
                "Build a Water Purifier",
                "[ECHO-7] Clean water supply secured. Starter filters and dirty samples released so the loop can prove itself immediately.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 4), new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 2), new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 2), new ItemStack(Items.GLASS_BOTTLE, 2)),
                player -> hasBlockNearPlayer(player, "water_purifier"),
                List.of(new ItemStack(ModItems.SCRAP_PLASTIC.get(), 2), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1), new ItemStack(ModItems.MACHINE_CASING.get(), 3), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("build_micro_generator"), // Need power first
                true, // Turn-in mission
                "water_purifier",
                List.of(new Mission.BlockRequirement("water_purifier", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "stockpile_clean_water",
                "[ECHO-7] A purifier is hardware. Survival requires reserves. Stockpile 3 Clean Water Bottles before expanding the base.",
                "Stockpile 3 Clean Water",
                "[ECHO-7] Hydration buffer established. Carry one bottle and keep the rest near the purifier.",
                List.of(new ItemStack(ModItems.ASH.get(), 6), new ItemStack(ModItems.BANDAGE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3)),
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("build_water_purifier"),
                true,
                "clean_water_bottle"
        ));
        phase1.add(new Mission(
                "build_battery_bank",
                "[ECHO-7] Single-generator output is unstable. Build a Battery Bank to buffer power before expanding the workshop.",
                "Build a Battery Bank",
                "[ECHO-7] Stored power online. Use the bank to smooth machine demand and prevent processing stalls.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 6), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 2)),
                player -> hasBlockNearPlayer(player, "battery_bank"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 6), new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("stockpile_clean_water"),
                true,
                "battery_bank",
                List.of(new Mission.BlockRequirement("battery_bank", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "build_scrap_dynamo",
                "[ECHO-7] Your first generator is unstable. Build a Scrap Dynamo to turn salvage into a steadier midgame FE source.",
                "Build a Scrap Dynamo",
                "[ECHO-7] Scrap Dynamo online. It burns scrap and fuel into steadier workshop power.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.SCRAP_WIRE.get(), 4)),
                player -> hasBlockNearPlayer(player, "scrap_dynamo"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 4), new ItemStack(ModItems.SCRAP_WIRE.get(), 4), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("build_battery_bank"),
                true,
                "scrap_dynamo",
                List.of(new Mission.BlockRequirement("scrap_dynamo", 1, "Scrap Dynamo")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "charge_basic_battery",
                "[ECHO-7] Portable power prevents machine stalls away from cables. Keep a charged Basic Battery in inventory, then return this protocol.",
                "Charge Basic Battery + Turn In",
                "[ECHO-7] Portable FE reserve confirmed. Insert batteries into machines, generators, or banks, or sneak-right-click energy blocks to transfer charge.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 4), new ItemStack(Items.REDSTONE, 2)),
                player -> hasChargedBattery(player, 1_000),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.EASY,
                List.of("build_scrap_dynamo"),
                true,
                "basic_battery"
        ));
        phase1.add(new Mission(
                "route_power_cable",
                "[ECHO-7] Power routing required. Place Power Cable near the generator and battery so machines can share the network.",
                "Route Power Cable",
                "[ECHO-7] Cable route confirmed. Power can now extend beyond direct machine adjacency.",
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(Items.REDSTONE, 4)),
                player -> hasBlockNearPlayer(player, "power_cable"),
                List.of(new ItemStack(Items.COPPER_INGOT, 6), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(Items.REDSTONE, 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("charge_basic_battery"),
                true,
                "power_cable",
                List.of(new Mission.BlockRequirement("power_cable", 1, "Power Cable")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "upgrade_power_cable",
                "[ECHO-7] Basic cable is a bottleneck. Craft and place Reinforced Power Cable to move more FE through the workshop.",
                "Upgrade Power Cable",
                "[ECHO-7] Reinforced cable route confirmed. Larger machine chains can now draw power without starving instantly.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 4), new ItemStack(Items.REDSTONE, 2)),
                player -> hasBlockNearPlayer(player, "reinforced_power_cable"),
                List.of(new ItemStack(ModBlocks.POWER_CABLE_ITEM.get(), 4), new ItemStack(ModItems.SCRAP_WIRE.get(), 6)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("route_power_cable"),
                true,
                "reinforced_power_cable",
                List.of(new Mission.BlockRequirement("reinforced_power_cable", 1, "Reinforced Power Cable")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "install_energy_meter",
                "[ECHO-7] Guesswork wastes fuel. Install an Energy Meter on the cable route and read the network report.",
                "Install Energy Meter",
                "[ECHO-7] Diagnostics online. Watch stored FE, demand, and the weakest cable bottleneck before adding machines.",
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                player -> hasBlockNearPlayer(player, "energy_meter"),
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(Items.GLASS_PANE, 1), new ItemStack(Items.REDSTONE, 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("upgrade_power_cable"),
                true,
                "energy_meter",
                List.of(new Mission.BlockRequirement("energy_meter", 1, "Energy Meter")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "set_power_priority",
                "[ECHO-7] Brownouts are survivable if water and medical systems stay powered. Place a Load Distributor and set it to Survival First.",
                "Set Power Priority",
                "[ECHO-7] Priority routing available. Low-priority machines will yield reserve power when the grid is low.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.SCRAP_WIRE.get(), 4)),
                player -> hasBlockNearPlayer(player, "load_distributor")
                        && QuestData.get(player).hasVisitedLocation("special", "power:priority_set"),
                List.of(new ItemStack(ModBlocks.BATTERY_BANK_ITEM.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(ModItems.SCRAP_WIRE.get(), 4)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("install_energy_meter"),
                true,
                "load_distributor",
                List.of(new Mission.BlockRequirement("load_distributor", 1, "Load Distributor")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "build_scrap_press",
                "[ECHO-7] Loose scrap is inefficient. Build a powered Scrap Press to compress base materials and prepare machine-scale storage.",
                "Build a Scrap Press",
                "[ECHO-7] Scrap Press online. Feed it scrap metal when powered to compact stockpiles and support higher processing.",
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 8), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 4)),
                player -> hasBlockNearPlayer(player, "scrap_press"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 7), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 2), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("set_power_priority"),
                true,
                "scrap_press",
                List.of(new Mission.BlockRequirement("scrap_press", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "overclock_machine",
                "[ECHO-7] Speed has a cost. Craft an Overclock Module before pushing machine throughput beyond starter-grid limits.",
                "Craft Overclock Module",
                "[ECHO-7] Overclock module verified. Expect faster cycles and higher FE demand.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(Items.REDSTONE, 4)),
                player -> player.getInventory().contains(new ItemStack(ModItems.MACHINE_UPGRADE_OVERCLOCK.get())),
                List.of(new ItemStack(ModItems.MACHINE_UPGRADE_OVERCLOCK.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("build_scrap_press"),
                true,
                "machine_upgrade_overclock"
        ));
        phase1.add(new Mission(
                "install_item_pipe",
                "[ECHO-7] Manual transfer wastes daylight. Install an Item Pipe to begin moving outputs between nearby machines.",
                "Install Item Pipe",
                "[ECHO-7] Item routing confirmed. Keep pipes near machine faces and storage to reduce manual hauling.",
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                player -> hasBlockNearPlayer(player, "item_pipe"),
                List.of(new ItemStack(Items.IRON_INGOT, 6), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("overclock_machine"),
                true,
                "item_pipe",
                List.of(new Mission.BlockRequirement("item_pipe", 1, "Item Pipe")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "build_thermal_burner",
                "[ECHO-7] Higher temperatures are needed for processing. Construct a Thermal Burner to finish the first outpost machine line.",
                "Build a Thermal Burner",
                "[ECHO-7] Burner operational. First base online: storage, power, water, and heat are now established.",
                List.of(new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                player -> hasBlockNearPlayer(player, "thermal_burner"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 6), new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.ASH.get(), 4)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.NORMAL,
                List.of("install_item_pipe"), // Need a connected starter workshop first
                true, // Turn-in mission
                "thermal_burner",
                List.of(new Mission.BlockRequirement("thermal_burner", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "base_stability_check",
                "[ECHO-7] Before hazard prep, confirm the outpost can carry you home. Keep storage, water purification, medicine, and buffered power active near the pod.",
                "Run Base Stability Check",
                "[ECHO-7] Base stability confirmed. You now have a fallback point for expedition prep and longer routes.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2), new ItemStack(ModItems.BANDAGE.get(), 2)),
                player -> QuestData.get(player).isMissionCompleted("first_faction_contact")
                        || (hasBlockNearPlayer(player, "chest")
                                && hasBlockNearPlayer(player, "water_purifier")
                                && hasBlockNearPlayer(player, "battery_bank")
                                && hasItemCount(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3))
                                && hasItemCount(player, new ItemStack(ModItems.BANDAGE.get(), 2))
                                && hasItemCount(player, new ItemStack(ModItems.ENERGY_CELL.get(), 1))),
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3), new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("build_thermal_burner"),
                false,
                "base_stability_check",
                List.of(
                        new Mission.BlockRequirement("chest", 1, "Storage Chest"),
                        new Mission.BlockRequirement("water_purifier", 1, "Water Purifier"),
                        new Mission.BlockRequirement("battery_bank", 1, "Battery Bank")
                ),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "equip_gas_mask",
                "[ECHO-7] First hazard route unlocked. Equip your Gas Mask before entering toxic pockets, then return this protocol.",
                "Equip Gas Mask + Turn In",
                "[ECHO-7] Respiratory seal confirmed. Filters drain only inside toxic hazard zones.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 2)),
                player -> player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.TRIVIAL,
                List.of("base_stability_check"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.EquipmentRequirement(EquipmentSlot.HEAD, new ItemStack(ModItems.GAS_MASK.get()), "Gas Mask Equipped"))
        ));
        phase1.add(new Mission(
                "fix_mask_filter",
                "[ECHO-7] Filter reserve check. Secure 1 spare Basic Filter Cartridge before your first toxic-zone expedition, then return this protocol.",
                "Secure Filter Cartridge + Turn In",
                "[ECHO-7] Spare filter logged. Cartridges drain only when a mask is filtering toxic air.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get())),
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.TRIVIAL,
                List.of("equip_gas_mask"),
                true,
                "filter_cartridge_basic"
        ));
        phase1.add(new Mission(
                "build_filter_workbench",
                "[ECHO-7] Basic filters are route supplies, not a daily oxygen tax. Build a Filter Workbench before pushing into marked toxic zones.",
                "Build a Filter Workbench",
                "[ECHO-7] Filter Workbench online. Toxic-zone supplies are now planned instead of lucky finds.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 2)),
                player -> hasBlockNearPlayer(player, "filter_workbench"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 3), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 2), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.SCRAP_WIRE.get(), 3)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("fix_mask_filter"),
                true,
                "filter_workbench",
                List.of(new Mission.BlockRequirement("filter_workbench", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase1.add(new Mission(
                "craft_advanced_filter",
                "[ECHO-7] Toxic pockets can still empty basic cartridges on long routes. Craft 1 Advanced Filter Cartridge and turn it in for verification.",
                "Craft Advanced Filter + Turn In",
                "[ECHO-7] Advanced filtration verified. Keep one installed and one spare only for toxic-zone expeditions.",
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get())),
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.NORMAL,
                List.of("build_filter_workbench"),
                true,
                "filter_cartridge_advanced"
        ));
        PHASES.put(2, phase1); // Phase 2: Resource Stability

        // === PHASE 3: FACTION BRIDGE (Research & Diplomacy) ===
        // 8 missions bridging resource stability to advanced technology
        List<Mission> phase25 = new ArrayList<>();
        
        // Mission 1: First Contact - interact with any faction job site
        phase25.add(new Mission(
                "first_faction_contact",
                "[ECHO-7] Survivor networks detected. Right-click any faction job-site block to open local task channels. The ruins are inhabited, and every inhabited ruin has rules.",
                "Contact Any Faction Job Site",
                "[ECHO-7] Faction channel confirmed. Job-site blocks can offer tasks, deliveries, and reputation routes.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 3)),
                MissionRegistry::hasAnyFactionContact,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("base_stability_check"), // Need a stable outpost before external channels
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction_contact:any", "Any Faction Job Site"))
        ));
        
        // Mission 2: Contact Radwarden Compact
        phase25.add(new Mission(
                "contact_radwarden_compact",
                "[ECHO-7] Radwarden containment channels require physical access. Right-click a warning beacon, supply crate, or contact near a hot-zone route.",
                "Contact Radwarden Compact",
                "[ECHO-7] Radwarden channel open. Expect containment work, decon requests, and structured hazard support.",
                List.of(new ItemStack(Items.ARROW, 16), new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                player -> hasFactionContact(player, "radwarden_compact"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("first_faction_contact"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction_contact:radwarden_compact", "Radwarden Contact"))
        ));

        // Mission 3: Contact Crashbreak Salvage
        phase25.add(new Mission(
                "contact_crashbreak_salvage",
                "[ECHO-7] Crashbreak salvage channels are local and opportunistic. Right-click a contact near wreckage, a trade counter, or a route map table.",
                "Contact Crashbreak Salvage",
                "[ECHO-7] Crashbreak channel open. Wreck routes, salvage contracts, and exploration leads are now on your map.",
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 12), new ItemStack(ModItems.SCRAP_WIRE.get(), 6), new ItemStack(Items.EMERALD, 2)),
                player -> hasFactionContact(player, "crashbreak_salvage"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("first_faction_contact"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction_contact:crashbreak_salvage", "Crashbreak Contact"))
        ));

        // Mission 4: Contact Sporebound Sanctum
        phase25.add(new Mission(
                "contact_sporebound_sanctum",
                "[ECHO-7] Sporebound biological circles respond through living work sites. Right-click a Bio Processing Station, Spore Garden, or Sanctum contact.",
                "Contact Sporebound Sanctum",
                "[ECHO-7] Sporebound channel open. Medical exchange, adaptation research, and bio-recovery tasks are available.",
                List.of(new ItemStack(ModItems.BANDAGE.get(), 3), new ItemStack(ModItems.RAD_AWAY.get(), 1), new ItemStack(ModItems.MUTATED_TISSUE.get(), 2)),
                player -> hasFactionContact(player, "sporebound_sanctum"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("first_faction_contact"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction_contact:sporebound_sanctum", "Sporebound Contact"))
        ));

        phase25.add(new Mission(
                "complete_first_faction_task",
                "[ECHO-7] Contact is not trust. Complete one faction job from any Ashfall job-site block to prove you can work inside survivor rules.",
                "Complete One Faction Task",
                "[ECHO-7] Contract record accepted. Faction work is now a viable supply route; individual faction dossiers remain optional intel.",
                List.of(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.BANDAGE.get(), 1)),
                player -> hasSpecialMarker(player, "faction:first_task_complete"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("first_faction_contact"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction:first_task_complete", "Faction Task Complete"))
        ));
        phase25.add(new Mission(
                "complete_radwarden_contract",
                "[ECHO-7] Radwarden trust is earned through containment discipline. Complete one Radwarden field contract.",
                "Complete Radwarden Contract",
                "[ECHO-7] Radwarden contract logged. Hazard support channels are warming up.",
                List.of(new ItemStack(Items.ARROW, 24), new ItemStack(ModItems.STIM_PACK.get(), 1)),
                player -> hasSpecialMarker(player, "faction:radwarden_compact:contract_complete"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("complete_first_faction_task"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction:radwarden_compact:contract_complete", "Radwarden Contract Complete"))
        ));
        phase25.add(new Mission(
                "complete_crashbreak_contract",
                "[ECHO-7] Crashbreak work teaches routes and value. Complete one Crashbreak field contract.",
                "Complete Crashbreak Contract",
                "[ECHO-7] Crashbreak contract logged. Salvage networks are now part of your survival plan.",
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 16), new ItemStack(Items.EMERALD, 3)),
                player -> hasSpecialMarker(player, "faction:crashbreak_salvage:contract_complete"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("complete_first_faction_task"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction:crashbreak_salvage:contract_complete", "Crashbreak Contract Complete"))
        ));
        phase25.add(new Mission(
                "complete_sporebound_contract",
                "[ECHO-7] Sporebound work reveals biological adaptation routes. Complete one Sporebound field contract.",
                "Complete Sporebound Contract",
                "[ECHO-7] Sporebound contract logged. Medical and adaptation channels are accessible.",
                List.of(new ItemStack(ModItems.MUTATED_TISSUE.get(), 3), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                player -> hasSpecialMarker(player, "faction:sporebound_sanctum:contract_complete"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("complete_first_faction_task"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "faction:sporebound_sanctum:contract_complete", "Sporebound Contract Complete"))
        ));

        // Mission 5: Build Research Lab
        phase25.add(new Mission(
                "build_research_lab",
                "[ECHO-7] Research infrastructure required. Construct a Research Lab to translate schematic fragments into usable survival doctrine.",
                "Build a Research Lab",
                "[ECHO-7] Research Lab operational. You can now spend research points to unlock perks and process schematic fragments.",
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)),
                player -> hasBlockNearPlayer(player, "research_lab"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 16), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 4), new ItemStack(ModItems.MACHINE_CASING.get(), 2)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("complete_radwarden_contract", "complete_crashbreak_contract", "complete_sporebound_contract"),
                true,
                "research_lab",
                List.of(new Mission.BlockRequirement("research_lab", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        
        // Mission 3: First Schematic
        phase25.add(new Mission(
                "first_schematic",
                "[ECHO-7] Ancient technology does not awaken cleanly. Process one Schematic Fragment at the Research Lab and recover the first lost procedure.",
                "Unlock Your First Schematic",
                "[ECHO-7] Schematic knowledge integrated. Tier 2 recipes are now available for crafting.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 6)),
                player -> com.knoxhack.echoashfallprotocol.research.ResearchData.get(player).getUnlockedSchematics().size() >= 1,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("build_research_lab"),
                false,
                null
        ));

        // Mission 3.25: Factory Controller
        phase25.add(new Mission(
                "build_factory_controller",
                "[ECHO-7] Your workshop has enough moving parts to require oversight. Build a Factory Controller near connected machines.",
                "Build a Factory Controller",
                "[ECHO-7] Factory Controller online. It scans connected machines through cables and pipes so the base can scale safely.",
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(Items.REDSTONE, 6)),
                player -> hasBlockNearPlayer(player, "factory_controller"),
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 5), new ItemStack(ModItems.MACHINE_CASING.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(Items.REDSTONE_BLOCK, 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("first_schematic"),
                true,
                "factory_controller",
                List.of(new Mission.BlockRequirement("factory_controller", 1, "Factory Controller")),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // Mission 3.5: Craft Portable Signal Scanner
        // This mission starts the 1.1 scanner-led POI route loop.
        phase25.add(new Mission(
                "craft_portable_scanner",
                "[ECHO-7] POI detection capability required. Craft a Portable Signal Scanner so each route reports its site, hazard, prep kit, and likely supplies before you commit.",
                "Craft a Portable Signal Scanner",
                "[ECHO-7] Portable scanner acquired. Use it to turn unknown ruins into readable expedition routes.",
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4)),
                player -> player.getInventory().contains(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get())),
                List.of(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("build_factory_controller"),
                false, // Auto-completes on pickup
                "portable_signal_scanner"
        ));

        phase25.add(new Mission(
                "expedition_readiness",
                "[ECHO-7] Scanner range does not replace preparation. Pack clean water, field medicine, and the Portable Signal Scanner before the first serious route. Spare filters are optional unless the route is toxic.",
                "Pack Expedition Supplies",
                "[ECHO-7] Expedition kit confirmed. Scan first, carry a retreat buffer, and enter only one hazard layer at a time.",
                List.of(new ItemStack(Items.MAP, 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.EMERGENCY_RATION.get(), 2)),
                player -> QuestData.get(player).getDiscoveredPOICount() >= 1
                        || player.getInventory().contains(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get()))
                        || (player.getInventory().contains(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get()))
                                && hasItemCount(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2))
                                && (hasItemCount(player, new ItemStack(ModItems.BANDAGE.get(), 2))
                                        || player.getInventory().contains(new ItemStack(ModItems.STIM_PACK.get()))
                                        || player.getInventory().contains(new ItemStack(ModItems.RAD_AWAY.get())))),
                List.of(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get(), 1), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2), new ItemStack(ModItems.BANDAGE.get(), 2)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("craft_portable_scanner"),
                false,
                "expedition_readiness"
        ));

        phase25.add(new Mission(
                "scan_first_poi",
                "[ECHO-7] Scanner protocol online. Use the Portable Signal Scanner until a point of interest is logged, then choose gear from that route's hazard profile instead of carrying every system at once.",
                "Scan First POI",
                "[ECHO-7] POI signature archived. The field log now tracks scanned, entered, cleared, and reward-claimed site states.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.EMERGENCY_RATION.get(), 2)),
                player -> QuestData.get(player).getDiscoveredPOICount() >= 1,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("expedition_readiness"),
                false,
                null
        ));
        phase25.add(new Mission(
                "loot_survivor_cache",
                "[ECHO-7] Survivor caches teach the old emergency rhythm: water, food, medicine, and notes. Locate one cache or recover a survivor data log.",
                "Loot Survivor Cache",
                "[ECHO-7] Cache route logged. Small shelters are safer supply stops than open-field wandering.",
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 2)),
                player -> hasDiscoveredPOI(player, "observation_post") || hasAnyDataLog(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("scan_first_poi"),
                true,
                "data_log"
        ));
        phase25.add(new Mission(
                "enter_bio_lab",
                "[ECHO-7] Bio labs explain the living half of the wasteland. Scan or enter a Bio Lab-class site with medicine and toxic-zone filtration ready.",
                "Enter Bio Lab",
                "[ECHO-7] Bio hazard site archived. Expect medicine, tissue samples, toxic pockets, and unstable lifeforms.",
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1), new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1)),
                player -> hasDiscoveredPOI(player, "sporebound_sanctum") || hasDiscoveredPOI(player, "bio_lab"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("loot_survivor_cache"),
                false,
                null
        ));
        phase25.add(new Mission(
                "recover_data_log",
                "[ECHO-7] The old world left warnings behind. Recover any Data Log and archive it before entering harder ruins.",
                "Recover Data Log + Turn In",
                "[ECHO-7] Log decrypted. Context is survival equipment when the terrain is lying to you.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 3), new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)),
                player -> hasAnyDataLog(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("enter_bio_lab"),
                true,
                "data_log"
        ));
        phase25.add(new Mission(
                "clear_military_vault",
                "[ECHO-7] Military vaults hold dense materials and severe threats. Scan a Radwarden or vault-class site before alloy hunting.",
                "Survey Military Vault",
                "[ECHO-7] Military route identified. Bring armor, medicine, clean water, and filters only if the site flags toxic air.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 8), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                player -> hasDiscoveredPOI(player, "radwarden_outpost") || hasDiscoveredPOI(player, "military_vault"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("recover_data_log"),
                false,
                null
        ));
        phase25.add(new Mission(
                "survey_reactor_ruin",
                "[ECHO-7] Reactor ruins are radiation lessons with teeth. Scan a reactor or relay-class hot site and plan a short entry, retreat, and cleanup loop.",
                "Survey Reactor Ruin",
                "[ECHO-7] Reactor route logged. Carry RadAway, use scrubber pockets where possible, and retreat before severe exposure sustains.",
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2)),
                player -> hasDiscoveredPOI(player, "relay_station_east") || hasDiscoveredPOI(player, "reactor_ruin"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("clear_military_vault"),
                false,
                null
        ));

        // Mission 3.6: Find Dense Alloy (Bridge to Geological Phase)
        phase25.add(new Mission(
                "find_dense_alloy",
                "[ECHO-7] Advanced machinery requires Dense Alloy. Use your Portable Signal Scanner to locate Military Vaults, or deploy a Deep Core Miner to acquire this essential material.",
                "Find Dense Alloy",
                "[ECHO-7] Dense Alloy acquired. This high-grade material unlocks steadier midgame power, extraction, and alloy-route preparation.",
                List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get())),
                List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("survey_reactor_ruin"),
                true,
                "dense_alloy_chunk"
        ));

        phase25.add(new Mission(
                "repair_echo_drone",
                "[ECHO-7] Recon support required. Repair the linked ECHO drone to 25 percent integrity before longer routes start eating supplies blind.",
                "Restore Drone Recon Support",
                "[ECHO-7] Drone mobility confirmed. It can now support safer scouting patterns without turning every POI into a face-check.",
                List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                player -> hasEchoDroneRepairAtLeast(player, 25) || hasScoutDroneSupport(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("complete_first_faction_task"),
                false,
                null
        ));
        phase25.add(new Mission(
                "upgrade_drone_support",
                "[ECHO-7] Scout routines require stable recon hardware. Repair the companion drone to 50 percent integrity, or verify Scout Drone support.",
                "Upgrade Drone Support",
                "[ECHO-7] Drone support upgraded. Scout mode is now a practical expedition tool.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4), new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1)),
                player -> hasEchoDroneRepairAtLeast(player, 50) || hasScoutDroneSupport(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("repair_echo_drone"),
                false,
                null
        ));
        phase25.add(new Mission(
                "set_drone_scout_mode",
                "[ECHO-7] Set the companion drone to Scout mode from the terminal controls, or command a Scout Drone into scavenging support.",
                "Set Drone Scout Mode",
                "[ECHO-7] Scout routine acknowledged. Let the drone check danger before your body does.",
                List.of(new ItemStack(ModItems.EMERGENCY_RATION.get(), 2), new ItemStack(ModItems.BANDAGE.get(), 1)),
                player -> hasEchoDroneMode(player, com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone.DroneMode.SCOUT)
                        || hasSpecialMarker(player, "drone:scout_mode"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("upgrade_drone_support"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "drone:scout_mode", "Drone Scout Mode"))
        ));
        phase25.add(new Mission(
                "recover_drone_intel",
                "[ECHO-7] Recover one intel pulse with the repaired drone near an expedition route. Let the machine test the edge before you cross it.",
                "Recover Drone Intel",
                "[ECHO-7] Drone intel recovered. Recon before entry is the difference between a route and a trap.",
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                player -> hasSpecialMarker(player, "drone:intel_recovered"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("repair_echo_drone"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "drone:intel_recovered", "Drone Intel Recovered"))
        ));
        phase25.add(new Mission(
                "deploy_scout_drone",
                "[ECHO-7] Keep a fallback recon unit. Deploy or carry a Scout Drone before longer routes.",
                "Deploy Scout Drone",
                "[ECHO-7] Scout Drone support confirmed. Companion damaged or not, you have a remote option.",
                List.of(new ItemStack(ModItems.SCRAP_WIRE.get(), 6), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                player -> hasScoutDroneSupport(player),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("recover_drone_intel"),
                false,
                "scout_drone"
        ));

        // Mission 4: Faction Reputation
        phase25.add(new Mission(
                "faction_reputation",
                "[ECHO-7] Alliances matter. Earn visible trust with any faction by completing contract work and reading the terminal reports.",
                "Earn Faction Trust",
                "[ECHO-7] Faction relationship established. Better supplies, intel, and support routes are now visible.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1)),
                player -> EchoCoreServices.factionProfiles(player).stream()
                        .filter(profile -> AshfallFactionMap.isAshfall(profile.definition().id()))
                        .anyMatch(profile -> profile.reputation() >= 5),
                null,
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.HARD,
                List.of("recover_drone_intel"),
                false,
                null
        ));

        // Mission 5: First Perk
        phase25.add(new Mission(
                "first_perk",
                "[ECHO-7] Personal enhancement available. Unlock your first research perk; survival is becoming a controlled modification problem.",
                "Unlock Your First Perk",
                "[ECHO-7] Perk acquired. Your abilities have been permanently enhanced through research.",
                List.of(new ItemStack(ModItems.MUTAGEN_VIAL.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.research.ResearchData.get(player).getUnlockedPerks().size() >= 1,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("faction_reputation"),
                false,
                null
        ));
        
        // Mission 6: POI Explorer
        phase25.add(new Mission(
                "poi_explorer",
                "[ECHO-7] The wasteland holds patterns, not secrets. Discover 3 unique Points of Interest so the map can stop guessing.",
                "Discover 3 POIs",
                "[ECHO-7] Exploration milestone reached. Your map now shows additional points of interest.",
                List.of(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 4)),
                player -> com.knoxhack.echoashfallprotocol.echo.QuestData.get(player).getDiscoveredPOICount() >= 3,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.NORMAL,
                List.of("first_perk"),
                false,
                null
        ));
        
        PHASES.put(3, phase25); // Phase 3: Faction Bridge (Expansion)

        // === PHASE 4: BIOLOGICAL ADAPTATION (Mutations & Medical) ===
        List<Mission> phase2 = new ArrayList<>();
        phase2.add(new Mission(
                "build_field_med_bay",
                "[ECHO-7] Genetic instability detected in environment. Construct a Field Med Bay before the wasteland starts editing you without consent.",
                "Build a Field Med Bay",
                "[ECHO-7] Med Bay initialized. Use this to mitigate radiation side effects and manage mutations.",
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 2)),
                player -> hasBlockNearPlayer(player, "field_med_bay"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 8), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 2), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("poi_explorer"), // Phase 3 capstone hands off into biological adaptation
                true, // Turn-in mission
                "field_med_bay",
                List.of(new Mission.BlockRequirement("field_med_bay", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase2.add(new Mission(
                "scan_mutation_status",
                "[ECHO-7] Mutation risk is now measurable. Stand near the Field Med Bay and check your biological status before treating symptoms.",
                "Scan Mutation Status",
                "[ECHO-7] Mutation scan baseline recorded. Adaptation has benefits, but every benefit has a cost.",
                List.of(new ItemStack(ModItems.BANDAGE.get(), 2)),
                player -> hasBlockNearPlayer(player, "field_med_bay"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("build_field_med_bay"),
                true,
                "field_med_bay",
                List.of(new Mission.BlockRequirement("field_med_bay", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase2.add(new Mission(
                "use_field_med_bay",
                "[ECHO-7] Power the Field Med Bay and let it run a treatment pulse and mutation baseline while you are nearby.",
                "Use Field Med Bay",
                "[ECHO-7] Treatment and mutation baseline confirmed. This stabilizes symptoms; it does not make exposure safe.",
                List.of(new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1), new ItemStack(ModItems.STIM_PACK.get(), 1)),
                player -> hasSpecialMarker(player, "medical:field_med_bay_used"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("build_field_med_bay"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "medical:field_med_bay_used", "Med Bay Treatment"))
        ));
        phase2.add(new Mission(
                "craft_radaway",
                "[ECHO-7] Severe radiation takes sustained exposure, but cleanup must be ready first. Synthesize RadAway before entering hot zones.",
                "Craft RadAway + Turn In",
                "[ECHO-7] RadAway verified. It is emergency cleanup after retreat, not permission to live inside fallout.",
                List.of(new ItemStack(ModItems.BANDAGE.get(), 2), new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.RAD_AWAY.get())),
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.EASY,
                List.of("use_field_med_bay"), // Need med bay first
                true, // Turn-in mission
                "rad_away"
        ));
        phase2.add(new Mission(
                "stabilize_mutation_effects",
                "[ECHO-7] Stabilization protocol: keep RadAway, bandages, and med bay access available before experimenting with mutation catalysts.",
                "Stabilize Mutation Effects",
                "[ECHO-7] Stabilization kit confirmed. Mutation is a tradeoff, not a free upgrade.",
                List.of(new ItemStack(ModItems.MUTATED_TISSUE.get(), 1), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.RAD_AWAY.get()))
                        && player.getInventory().contains(new ItemStack(ModItems.BANDAGE.get()))
                        && hasSpecialMarker(player, "medical:field_med_bay_used"),
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.NORMAL,
                List.of("craft_radaway"),
                true,
                "rad_away"
        ));
        phase2.add(new Mission(
                "scout_radiation_zone",
                "[ECHO-7] Conduct a controlled radiation-zone scout. Carry RadAway, enter briefly, then return before exposure becomes severe.",
                "Scout Radiation Zone + Turn In",
                "[ECHO-7] Radiation-zone exposure profile recorded. Mutation rolls require sustained severe exposure; still, do not linger without shielding, scrubbers, and cleanup equipment.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                player -> hasRadiationZoneScout(player)
                        && player.getInventory().contains(new ItemStack(ModItems.RAD_AWAY.get())),
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("stabilize_mutation_effects"),
                true,
                "rad_away",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("biome", "radiation_zone", "Radiation Zone"))
        ));
        phase2.add(new Mission(
                "build_atmospheric_scrubber",
                "[ECHO-7] Portable medicine is not a habitat. Build an Atmospheric Scrubber to create a recovery pocket for expeditions.",
                "Build Atmospheric Scrubber",
                "[ECHO-7] Scrubber online. It suppresses toxic air, stops filter drain, and speeds radiation decay inside its field.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                player -> hasBlockNearPlayer(player, "atmospheric_scrubber"),
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 2), new ItemStack(ModItems.MACHINE_CASING.get(), 3), new ItemStack(ModItems.ENERGY_CELL.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("scout_radiation_zone"),
                true,
                "atmospheric_scrubber",
                List.of(new Mission.BlockRequirement("atmospheric_scrubber", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase2.add(new Mission(
                "build_radiation_cleanser",
                "[ECHO-7] Contaminated materials will poison the production chain. Build a Radiation Cleanser for sustained cleanup.",
                "Build Radiation Cleanser",
                "[ECHO-7] Radiation Cleanser online. Contaminated salvage can now become reliable material instead of a liability.",
                List.of(new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 2), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                player -> hasBlockNearPlayer(player, "radiation_cleanser"),
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 2), new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("build_atmospheric_scrubber"),
                true,
                "radiation_cleanser",
                List.of(new Mission.BlockRequirement("radiation_cleanser", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase2.add(new Mission(
                "collect_mutated_tissue",
                "[ECHO-7] Mutation research needs a sample. Recover Mutated Tissue from biological threats, faction tasks, or bio-sites.",
                "Collect Mutated Tissue + Turn In",
                "[ECHO-7] Tissue sample sealed. Keep it contained until the med systems are ready.",
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModItems.MUTATED_TISSUE.get())),
                List.of(new ItemStack(ModItems.MUTATED_TISSUE.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("build_radiation_cleanser"),
                true,
                "mutated_tissue"
        ));
        phase2.add(new Mission(
                "craft_mutagen_vial",
                "[ECHO-7] Craft or recover a Mutagen Vial only after tissue handling and cleanup are stable.",
                "Craft Mutagen Vial + Turn In",
                "[ECHO-7] Mutagen catalyst verified. Biology route is stable enough to move pressure back to deep extraction.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4), new ItemStack(ModItems.FILTRATION_MEMBRANE.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.MUTAGEN_VIAL.get())),
                List.of(new ItemStack(ModItems.MUTAGEN_VIAL.get(), 1)),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.HARD,
                List.of("collect_mutated_tissue"),
                true,
                "mutagen_vial"
        ));
        phase2.add(new Mission(
                "acquire_mutagen",
                "[ECHO-7] Mutagen proof is already sealed. Confirm handling records before pushing into extraction work.",
                "Confirm Mutagen Handling",
                "[ECHO-7] Mutagen handling confirmed. Biology route complete; geological extraction can begin.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4)),
                player -> QuestData.get(player).isMissionCompleted("craft_mutagen_vial")
                        || player.getInventory().contains(new ItemStack(ModItems.MUTAGEN_VIAL.get())),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.CRAFTING,
                Mission.Difficulty.HARD,
                List.of("craft_mutagen_vial"), // Need controlled radiation cleanup first
                false,
                null
        ));
        PHASES.put(4, phase2); // Phase 4: Biological Adaptation

        // === PHASE 5: GEOLOGICAL EXTRACTION (Dense Alloys) ===
        List<Mission> phase3 = new ArrayList<>();
        phase3.add(new Mission(
                "build_thermal_array",
                "[ECHO-7] Dense alloy confirms midgame material access. Build a Thermal Array so extraction machines have a steadier FE source than the starter grid.",
                "Build a Thermal Array",
                "[ECHO-7] Thermal Array online. Route it through storage and reinforced cable before adding extraction demand.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.SCRAP_WIRE.get(), 6)),
                player -> hasBlockNearPlayer(player, "thermal_array"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 3), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("find_dense_alloy"),
                true,
                "thermal_array",
                List.of(new Mission.BlockRequirement("thermal_array", 1, "Thermal Array")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase3.add(new Mission(
                "build_ore_grinder",
                "[ECHO-7] Surface scrap is insufficient. Deploy a Substrate Grinder beside the upgraded power route to extract traces from biome substrate.",
                "Build a Substrate Grinder",
                "[ECHO-7] Grinding mechanisms enabled. Trace shards can now be processed for high-tier alloys.",
                List.of(new ItemStack(ModItems.COAL_DUST.get(), 8), new ItemStack(ModItems.IRON_SHARD.get(), 4), new ItemStack(ModItems.COPPER_SHARD.get(), 4), new ItemStack(ModItems.CRYSTAL_DUST.get(), 1)),
                player -> hasBlockNearPlayer(player, "ore_grinder"),
                List.of(new ItemStack(ModItems.SCRAP_METAL.get(), 4), new ItemStack(ModItems.MACHINE_CASING.get(), 1), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 2), new ItemStack(ModItems.SCRAP_WIRE.get(), 2)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("build_thermal_array"),
                true, // Turn-in mission
                "ore_grinder",
                List.of(new Mission.BlockRequirement("ore_grinder", 1, "Substrate Grinder")),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase3.add(new Mission(
                "build_isotope_refiner",
                "[ECHO-7] These minerals are highly contaminated. Use an Isotope Refiner to separate useful trace material from whatever the Gridfall left inside it.",
                "Build an Isotope Refiner",
                "[ECHO-7] Refiner active. This is the only path to nuclear-grade materials and alloys.",
                List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4)),
                player -> hasBlockNearPlayer(player, "isotope_refiner"),
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 4), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4), new ItemStack(ModItems.MACHINE_CASING.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("build_ore_grinder"), // Need ore processing first
                true, // Turn-in mission
                "isotope_refiner",
                List.of(new Mission.BlockRequirement("isotope_refiner", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase3.add(new Mission(
                "forge_alloy_weapon",
                "[ECHO-7] Guardian routes will not respect primitive tools. Forge an Alloy Blade or Alloy Hammer before entering buried control nodes.",
                "Forge an Alloy Weapon",
                "[ECHO-7] Alloy weapon verified. Your route can now survive midgame defenders instead of merely disturbing them.",
                List.of(new ItemStack(ModItems.MACHINE_UPGRADE_SPEED.get(), 1), new ItemStack(ModItems.BANDAGE.get(), 2)),
                MissionRegistry::hasAnyAlloyWeapon,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.HARD,
                List.of("build_isotope_refiner"),
                true, // Turn-in mission
                "alloy_blade"
        ));
        phase3.add(new Mission(
                "equip_alloy_kit",
                "[ECHO-7] Weapon alone is not a survival plan. Equip Alloy Helmet and Alloy Chestplate while carrying an alloy weapon.",
                "Equip Alloy Route Kit",
                "[ECHO-7] Alloy kit confirmed. You can absorb midgame ambushes long enough to retreat or finish the fight.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                MissionRegistry::hasEquippedAlloyKit,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.HARD,
                List.of("forge_alloy_weapon"),
                false,
                "alloy_chestplate",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        new Mission.EquipmentRequirement(EquipmentSlot.HEAD, new ItemStack(ModItems.ALLOY_HELMET.get()), "Alloy Helmet"),
                        new Mission.EquipmentRequirement(EquipmentSlot.CHEST, new ItemStack(ModItems.ALLOY_CHESTPLATE.get()), "Alloy Chestplate")
                )
        ));
        phase3.add(new Mission(
                "stockpile_route_supplies",
                "[ECHO-7] Guardian entrances are one-way mistakes without reserves. Stockpile water, food, medicine, RadAway, filtration, and portable FE before the grid route opens.",
                "Stockpile Route Supplies",
                "[ECHO-7] Route supplies confirmed. Scan first, enter prepared, retreat before reserves collapse.",
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT_MACHINES.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                MissionRegistry::hasMidgameRouteSupplies,
                List.of(
                        new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3),
                        new ItemStack(ModItems.EMERGENCY_RATION.get(), 3),
                        new ItemStack(ModItems.BANDAGE.get(), 3),
                        new ItemStack(ModItems.RAD_AWAY.get(), 2),
                        new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get(), 1)
                ),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.HARD,
                List.of("equip_alloy_kit"),
                true,
                "expedition_readiness"
        ));
        phase3.add(new Mission(
                "calibrate_midgame_grid",
                "[ECHO-7] Final check before guardian routes: Thermal Array, buffered storage, reinforced cable, Energy Meter, and Factory Controller must all be present near the workshop.",
                "Calibrate Midgame Grid",
                "[ECHO-7] Midgame grid calibrated. Guardian-route infrastructure is now stable enough for stationary scans and buried-node operations.",
                List.of(new ItemStack(ModBlocks.HIGH_VOLTAGE_POWER_CABLE_ITEM.get(), 4), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                MissionRegistry::hasMidgameGridReady,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("stockpile_route_supplies"),
                true,
                "factory_controller",
                List.of(
                        new Mission.BlockRequirement("thermal_array", 1, "Thermal Array"),
                        new Mission.BlockRequirement("factory_controller", 1, "Factory Controller"),
                        new Mission.BlockRequirement("energy_meter", 1, "Energy Meter"),
                        new Mission.BlockRequirement("reinforced_power_cable", 1, "Reinforced Power Cable"),
                        new Mission.BlockRequirement("battery_bank", 1, "Battery Bank / Nexus Capacitor")
                ),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        PHASES.put(5, phase3); // Phase 5: Geological Extraction

        // === PHASE 6: GRID RESTORATION (Power Network) ===
        List<Mission> phase4 = new ArrayList<>();
        phase4.add(new Mission(
                "deploy_stationary_scanner",
                "[ECHO-7] The world is vast and broken. Deploy a Stationary Signal Scanner to listen for hidden ruins, Power Nodes, and guardian entrances your portable scan may miss.",
                "Deploy a Stationary Signal Scanner",
                "[ECHO-7] Stationary scanner online. Low-frequency signatures marked. The base now listens while you move.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 4)),
                player -> hasBlockNearPlayer(player, "signal_scanner"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 4), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("calibrate_midgame_grid"),
                true, // Turn-in mission
                "signal_scanner",
                List.of(new Mission.BlockRequirement("signal_scanner", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase4.add(new Mission(
                "build_scout_drone",
                "[ECHO-7] Grid work needs remote eyes before guardian contact. Assemble a Scout Drone and keep it ready for node routes.",
                "Build a Scout Drone",
                "[ECHO-7] Drone uplink stable. Use it to scout salvage, node sites, and guardian arenas before entering.",
                List.of(new ItemStack(ModBlocks.POWER_NODE_ITEM.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModItems.SCOUT_DRONE_ITEM.get())),
                List.of(new ItemStack(ModItems.SCOUT_DRONE_ITEM.get(), 1)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("deploy_stationary_scanner"),
                true,
                "scout_drone"
        ));
        phase4.add(new Mission(
                "activate_power_node",
                "[ECHO-7] Build and place a Power Node to rejoin the damaged grid. Treat this as your first controlled grid anchor, not the final Core lock.",
                "Place a Power Node",
                "[ECHO-7] Grid node placed. The route is responding; now connect outposts before the guardian sweep.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4)),
                player -> hasBlockNearPlayer(player, "power_node"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("deploy_stationary_scanner"),
                false,
                "",
                List.of(new Mission.BlockRequirement("power_node", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase4.add(new Mission(
                "build_nexus_capacitor",
                "[ECHO-7] Late grid routes need deep storage. Build a Nexus Capacitor so high-voltage cable runs and guardian-route machines can survive demand spikes.",
                "Build a Nexus Capacitor",
                "[ECHO-7] Nexus Capacitor linked. Your grid can now carry late-game machine demand without collapsing at every surge.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 4)),
                player -> hasBlockNearPlayer(player, "nexus_capacitor"),
                List.of(new ItemStack(ModBlocks.BATTERY_BANK_ITEM.get(), 1), new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4), new ItemStack(ModItems.ENERGY_CELL.get(), 2)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("activate_power_node"),
                true,
                "nexus_capacitor",
                List.of(new Mission.BlockRequirement("nexus_capacitor", 1, "Nexus Capacitor")),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // Workshop Tutorial Mission (Medium Gap Fix)
        phase4.add(new Mission(
                "build_workshop",
                "[ECHO-7] Advanced crafting requires specialized facilities. Deploy a Workshop near your grid hardware to support guardian-route gear.",
                "Build a Workshop",
                "[ECHO-7] Workshop online. Use it to prepare stronger weapons, armor, and upgrade paths before guardian sites.",
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT_WEAPONS.get(), 1), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 8)),
                player -> hasBlockNearPlayer(player, "workshop_block"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 2), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4), new ItemStack(ModItems.SCRAP_PLASTIC.get(), 8)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.HARD,
                List.of("build_nexus_capacitor"),
                true,
                "workshop",
                List.of(new Mission.BlockRequirement("workshop_block", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // Relay Station Tutorial Mission (Medium Gap Fix)
        phase4.add(new Mission(
                "activate_relay_station",
                "[ECHO-7] Long-distance travel is inefficient. Place and fuel a Relay Station so grid routes have a return path before the guardian sweep.",
                "Activate a Relay Station",
                "[ECHO-7] Fast travel network initialized. You now have scanner, drone, node, workshop, and relay support for guardian routes.",
                List.of(new ItemStack(ModItems.MACHINE_UPGRADE_SPEED.get(), 1), new ItemStack(ModItems.ENERGY_CELL.get(), 8)),
                player -> hasBlockNearPlayer(player, "relay_station"),
                List.of(new ItemStack(ModItems.MACHINE_CASING.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 2), new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4)),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.NORMAL,
                List.of("build_workshop"),
                true,
                "relay_station",
                List.of(new Mission.BlockRequirement("relay_station", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase4.add(bossMission(
                "neutralize_plains_warlord",
                "Plains Warlord",
                "echoashfallprotocol:plains_warlord",
                "Radwarden Outpost",
                "Ruined Plains",
                "activate_relay_station",
                Mission.Difficulty.HARD,
                List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 4))
        ));
        phase4.add(bossMission(
                "neutralize_city_ruin_stalker",
                "City Ruin Stalker",
                "echoashfallprotocol:city_ruin_stalker",
                "Data Center",
                "Ruined Cityscape",
                "neutralize_plains_warlord",
                Mission.Difficulty.HARD,
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT_MACHINES.get(), 1), new ItemStack(ModItems.POWER_CELL.get(), 2))
        ));
        phase4.add(bossMission(
                "neutralize_industrial_juggernaut",
                "Industrial Juggernaut",
                "echoashfallprotocol:industrial_juggernaut",
                "Data Center",
                "Industrial Ruins",
                "neutralize_city_ruin_stalker",
                Mission.Difficulty.HARD,
                List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 3), new ItemStack(ModItems.SCHEMATIC_FRAGMENT_WEAPONS.get(), 1))
        ));
        phase4.add(bossMission(
                "neutralize_toxic_hive_matriarch",
                "Toxic Hive Matriarch",
                "echoashfallprotocol:toxic_hive_matriarch",
                "Sporebound Sanctum",
                "Toxic Swamp",
                "neutralize_industrial_juggernaut",
                Mission.Difficulty.EXTREME,
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1), new ItemStack(ModItems.RAD_AWAY.get(), 2))
        ));
        phase4.add(bossMission(
                "neutralize_crash_zone_colossus",
                "Crash Zone Colossus",
                "echoashfallprotocol:crash_zone_colossus",
                "Military Vault",
                "Crash Zone Wasteland",
                "neutralize_toxic_hive_matriarch",
                Mission.Difficulty.EXTREME,
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT_ARMOR.get(), 1), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4))
        ));
        phase4.add(bossMission(
                "neutralize_radiation_behemoth",
                "Radiation Behemoth",
                "echoashfallprotocol:radiation_behemoth",
                "Reactor Ruin",
                "Radiation Zone",
                "neutralize_crash_zone_colossus",
                Mission.Difficulty.EXTREME,
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1), new ItemStack(ModItems.RAD_AWAY.get(), 2))
        ));
        phase4.add(new Mission(
                "enter_cryogenic_ruins",
                "[ECHO-7] Cryogenic sites are temperature hazards first, loot sites second. Enter or scan Cryogenic Ruins before challenging the overseer.",
                "Enter Cryogenic Ruins",
                "[ECHO-7] Cryogenic route archived. Warmth is now mission equipment.",
                List.of(new ItemStack(ModItems.HAND_WARMER.get(), 2), new ItemStack(ModItems.EMERGENCY_RATION.get(), 2)),
                player -> hasDiscoveredPOI(player, "cryogenic_ruins")
                        || QuestData.get(player).hasVisitedLocation("biome", "cryogenic_ruins")
                        || QuestData.get(player).hasVisitedLocation("biome", "echoashfallprotocol:cryogenic_ruins"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("neutralize_radiation_behemoth"),
                false,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("biome", "cryogenic_ruins", "Cryogenic Ruins"))
        ));
        phase4.add(new Mission(
                "recover_cryo_sample",
                "[ECHO-7] Recover a cryogenic substrate sample before deeper entry. Fractured cryo stone holds preserved tech traces.",
                "Recover Cryo Sample + Turn In",
                "[ECHO-7] Cryo sample sealed. Expect brittle materials and cold-shock failures in this route.",
                List.of(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 4), new ItemStack(ModItems.HAND_WARMER.get(), 2)),
                player -> player.getInventory().contains(new ItemStack(ModBlocks.CRYOGENIC_FRACTURED_STONE_ITEM.get())),
                List.of(new ItemStack(ModBlocks.CRYOGENIC_FRACTURED_STONE_ITEM.get(), 1)),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.HARD,
                List.of("enter_cryogenic_ruins"),
                true,
                "cryogenic_fractured_stone"
        ));
        phase4.add(new Mission(
                "warm_up_after_exposure",
                "[ECHO-7] Do not let cold stack quietly. Use a Hand Warmer after cryogenic exposure and confirm body temperature recovery.",
                "Warm Up After Exposure",
                "[ECHO-7] Temperature recovery logged. Retreat-and-warm is the cryogenic equivalent of RadAway.",
                List.of(new ItemStack(ModItems.THERMAL_LINER.get(), 1)),
                player -> hasSpecialMarker(player, "cold:warmed_up"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.HARD,
                List.of("recover_cryo_sample"),
                true,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("special", "cold:warmed_up", "Warmed Up"))
        ));
        phase4.add(new Mission(
                "craft_cold_route_supplies",
                "[ECHO-7] Assemble cold-route supplies: Thermal Liner and Hand Warmers before engaging cryogenic command entities.",
                "Prepare Cold Supplies + Turn In",
                "[ECHO-7] Cold-route kit confirmed. You may proceed without becoming part of the facility.",
                List.of(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                player -> player.getInventory().contains(new ItemStack(ModItems.THERMAL_LINER.get()))
                        && player.getInventory().contains(new ItemStack(ModItems.HAND_WARMER.get())),
                List.of(new ItemStack(ModItems.THERMAL_LINER.get(), 1), new ItemStack(ModItems.HAND_WARMER.get(), 1)),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.HARD,
                List.of("warm_up_after_exposure"),
                true,
                "thermal_liner"
        ));
        phase4.add(bossMission(
                "neutralize_cryogenic_overseer",
                "Cryogenic Overseer",
                "echoashfallprotocol:cryogenic_overseer",
                "Cryogenic Ruins",
                "Cryogenic Ruins",
                "craft_cold_route_supplies",
                Mission.Difficulty.EXTREME,
                List.of(new ItemStack(ModItems.THERMAL_LINER.get(), 1), new ItemStack(ModItems.HAND_WARMER.get(), 4))
        ));
        phase4.add(bossMission(
                "neutralize_nexus_scar_avatar",
                "Nexus Scar Avatar",
                "echoashfallprotocol:nexus_scar_avatar",
                "Reactor Ruin",
                "Nexus Scar",
                "neutralize_cryogenic_overseer",
                Mission.Difficulty.EXTREME,
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4))
        ));
        PHASES.put(6, phase4); // Phase 6: Grid Restoration

        // === PHASE 7: NEXUS INTEGRATION (Endgame Decision) ===
        List<Mission> phase5 = new ArrayList<>();
        phase5.add(new Mission(
                "find_nexus_core",
                "[ECHO-7] I see it. The Nexus Core. Stand near the unresolved Core structure and hold position; the grid is listening back.",
                "Locate the Nexus Core",
                "[ECHO-7] We have arrived. The system is still running after years of Gridfall. It is waiting for a command, and commands have consequences.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2)),
                player -> hasBlockNearPlayer(player, "nexus_core"),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("neutralize_nexus_scar_avatar"),
                false, // Auto-completes when found
                "",
                List.of(new Mission.BlockRequirement("nexus_core", 1)),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        phase5.add(new Mission(
                "awaken_nexus_core",
                "[ECHO-7] The Core is not a door. It is a sleeping command system. Wake it from range and let the instability meter start before we touch the relay network.",
                "Awaken the Nexus Core",
                "[ECHO-7] Core wake confirmed. Instability is now a live endgame pressure source.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2), new ItemStack(ModItems.RAD_AWAY.get(), 1)),
                MissionRegistry::isNexusCampaignAwakened,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("find_nexus_core"),
                false,
                ""
        ));
        phase5.add(new Mission(
                "scan_prime_relays",
                "[ECHO-7] Six Prime Relay signatures are buried in the old network: Reactor, Cryo, Bio, Transit, Industrial, and Scar. Run the relay scan before choosing which three to resolve.",
                "Scan 6 Prime Relays",
                "[ECHO-7] Relay map indexed. Each site can be stabilized, severed, or overridden before the final choice.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 4), new ItemStack(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get(), 1)),
                MissionRegistry::hasScannedPrimeRelays,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.EXTREME,
                List.of("awaken_nexus_core"),
                false,
                ""
        ));
        phase5.add(new Mission(
                "resolve_prime_relays",
                "[ECHO-7] Resolve any three Prime Relays. Stabilize favors RESTORE readiness, sever favors DESTROY readiness, and override favors CONTROL readiness. The final choice remains free.",
                "Resolve 3 Prime Relays",
                "[ECHO-7] Prime Relay readiness established. The Core will resist, but it can be cornered.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1), new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 3)),
                MissionRegistry::hasResolvedPrimeRelays,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("scan_prime_relays"),
                false,
                ""
        ));
        phase5.add(new Mission(
                "stabilize_nexus_grid",
                "[ECHO-7] The relay network is reacting. Stabilize five Power Nodes near the Nexus Core before provoking the countermeasure.",
                "Stabilize the Nexus Grid",
                "[ECHO-7] Grid lock achieved. The Core can be forced into a countermeasure window.",
                List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 4)),
                MissionRegistry::hasActivatedNexusGrid,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.EXTREME,
                List.of("resolve_prime_relays"),
                false,
                ""
        ));
        phase5.add(new Mission(
                "survive_core_countermeasure",
                "[ECHO-7] The Core is cornered. Hold the active grid through one countermeasure siege, then the final command interface can open.",
                "Survive Core Countermeasure",
                "[ECHO-7] Countermeasure survived. The Nexus choice is ready, and it is still irreversible.",
                List.of(new ItemStack(ModItems.ELITE_BATTERY.get(), 1), new ItemStack(ModItems.RAD_AWAY.get(), 2)),
                MissionRegistry::hasSurvivedCoreCountermeasure,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EXTREME,
                List.of("stabilize_nexus_grid"),
                false,
                ""
        ));
        phase5.add(new Mission(
                "reach_decision",
                "[ECHO-7] Warfront complete. Open NEXUS, choose RESTORE, DESTROY, or CONTROL, then confirm. This choice is permanent.",
                "Reach the Final Decision Point",
                "[ECHO-7] Connection... severed. History will remember this day. Whatever you chose, the world is changed.",
                Collections.emptyList(),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).hasMadeChoice(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("survive_core_countermeasure"),
                false, // Manual completion
                ""
        ));
        PHASES.put(7, phase5); // Phase 7: Nexus Integration

        // === PHASE 8: POST-NEXUS ENDGAME ===
        List<Mission> phase8 = new ArrayList<>();
        
        // RESTORE Path Missions
        phase8.add(new Mission(
                "restore_repair_nodes",
                "[ECHO-7] The grid requires stabilization. Activate 3 Power Nodes; crafted replacements work if old nodes are lost.",
                "Activate 3 Power Nodes",
                "[ECHO-7] Grid stabilization progressing. The Core's influence spreads with each node restored.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2), new ItemStack(ModItems.ENERGY_CELL.get(), 8)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).getNodesRepaired() >= 3,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("reach_decision"),
                false,
                "",
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_purge_corruption",
                "[ECHO-7] Corrupted entities threaten the restored grid. Eliminate 20 irradiated hostiles to purify the system.",
                "Defeat 20 Corrupted Mobs",
                "[ECHO-7] The corruption recedes. Order takes hold where chaos once reigned.",
                List.of(new ItemStack(ModItems.PREFALL_ARCHIVES_KEY.get(), 1), new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 3), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 4)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).getCorruptedMobsKilled() >= 20,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EXTREME,
                List.of("restore_repair_nodes"),
                false,
                "",
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_enter_archives",
                "[ECHO-7] The restored grid opened the Pre-Fall Archives. Right-click the Archives Key on overworld ground; I will issue a Return Keystone inside.",
                "Enter the Pre-Fall Archives",
                "[ECHO-7] Archive gate confirmed. Return Keystone fallback active. The final guardian is awake.",
                List.of(new ItemStack(ModItems.RETURN_KEYSTONE.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).hasEnteredArchives(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.EXTREME,
                List.of("restore_purge_corruption"),
                false,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("dimension", "echoashfallprotocol:prefall_archives", "Pre-Fall Archives")),
                Collections.emptyList(),
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_guardian",
                "[ECHO-7] A final guardian bars total integration. The Warden runs defender lockdowns and pulse phases; enter with medicine, top-tier weapons, and a Return Keystone.",
                "Defeat The Warden in Pre-Fall Archives",
                "[ECHO-7] The Warden falls. Return to the mission channel and confirm the Restore epilogue.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 8)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isWardenDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("restore_enter_archives"),
                false,
                "",
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_world_lattice",
                "[ECHO-7] The Warden was midpoint, not closure. Rebuild the world lattice by purifying relay routes from the terminal operation channel.",
                "Rebuild World Lattice",
                "[ECHO-7] Relay purification accepted. The Corruption Bloom is exposed.",
                List.of(new ItemStack(ModItems.RAD_AWAY.get(), 4), new ItemStack(ModItems.ENERGY_CELL.get(), 8)),
                MissionRegistry::hasPathOperationComplete,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.EXTREME,
                List.of("restore_guardian"),
                false,
                "",
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_finale",
                "[ECHO-7] The Corruption Bloom is feeding on restored relay flow. Defeat the path finale, then seal the Restore epilogue.",
                "Defeat Corruption Bloom",
                "[ECHO-7] Corruption Bloom collapsed. The Restore epilogue can now be sealed.",
                List.of(new ItemStack(ModItems.NEXUS_HELMET.get(), 1)),
                MissionRegistry::hasFinalBossDefeated,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EXTREME,
                List.of("restore_world_lattice"),
                false,
                "",
                PostNexusData.NexusPath.RESTORE
        ));
        phase8.add(new Mission(
                "restore_epilogue",
                "[ECHO-7] Restoration has a cost. Turn in this terminal protocol to seal the repaired grid and close the branch.",
                "Complete Restore Epilogue",
                "[ECHO-7] The grid breathes again. Settlements reconnect, water clears, and the old world becomes a scaffold for the new.",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isFinalBossDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("restore_finale"),
                true,
                "",
                PostNexusData.NexusPath.RESTORE
        ));

        // DESTROY Path Missions
        phase8.add(new Mission(
                "destroy_scorched_earth",
                "[ECHO-7] Freedom demands sacrifice. Break 5 Power Nodes to sever the grid; rebuilt nodes still count if the route stalls.",
                "Destroy 5 Power Nodes",
                "[ECHO-7] The grid falters. Each destroyed node is another link broken in the chain of control.",
                List.of(new ItemStack(ModItems.NEXUS_ANNIHILATOR.get(), 1), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 6)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).getNodesDestroyed() >= 5,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("reach_decision"),
                false,
                "",
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_survive_storms",
                "[ECHO-7] The storms grow wild without the Core's moderation. Survive a radiation storm, ash storm, Nexus surge, or thunder event from cover, then recover.",
                "Survive a Severe Storm",
                "[ECHO-7] Storm survival logged. Cover held, route recovered, Archives key released.",
                List.of(new ItemStack(ModItems.PREFALL_ARCHIVES_KEY.get(), 1), new ItemStack(ModItems.MUTAGEN_VIAL.get(), 2)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).getStormsSurvived() >= 1,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.SURVIVAL,
                Mission.Difficulty.EXTREME,
                List.of("destroy_scorched_earth"),
                false,
                "",
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_enter_archives",
                "[ECHO-7] The severed grid exposed the Pre-Fall Archives. Right-click the Archives Key on overworld ground; I will issue a Return Keystone inside.",
                "Enter the Pre-Fall Archives",
                "[ECHO-7] Archive gate breached. Return Keystone fallback active. The final guardian is awake.",
                List.of(new ItemStack(ModItems.RETURN_KEYSTONE.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).hasEnteredArchives(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.EXTREME,
                List.of("destroy_survive_storms"),
                false,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("dimension", "echoashfallprotocol:prefall_archives", "Pre-Fall Archives")),
                Collections.emptyList(),
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_guardian",
                "[ECHO-7] Even chaos has its guardians. The Warden runs defender lockdowns and pulse phases; enter with medicine, top-tier weapons, and a Return Keystone.",
                "Defeat The Warden in Pre-Fall Archives",
                "[ECHO-7] The last chain is broken. Return to the mission channel and confirm the Destroy epilogue.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 8)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isWardenDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("destroy_enter_archives"),
                false,
                "",
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_dead_signal",
                "[ECHO-7] The Warden exposed a dead command carrier below the Archives. Collapse the remaining signal infrastructure before it reboots.",
                "Collapse Dead Signal",
                "[ECHO-7] Dead signal collapse confirmed. The Severance Engine is exposed.",
                List.of(new ItemStack(ModItems.MUTAGEN_VIAL.get(), 2), new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 8)),
                MissionRegistry::hasPathOperationComplete,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.EXTREME,
                List.of("destroy_guardian"),
                false,
                "",
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_finale",
                "[ECHO-7] The Severance Engine is trying to preserve the old kill-switch. Destroy it before the epilogue.",
                "Defeat Severance Engine",
                "[ECHO-7] Severance Engine destroyed. The Destroy epilogue can now be sealed.",
                List.of(new ItemStack(ModItems.NEXUS_ANNIHILATOR.get(), 1)),
                MissionRegistry::hasFinalBossDefeated,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EXTREME,
                List.of("destroy_dead_signal"),
                false,
                "",
                PostNexusData.NexusPath.DESTROY
        ));
        phase8.add(new Mission(
                "destroy_epilogue",
                "[ECHO-7] The grid is dead. Turn in this terminal protocol and let the wasteland choose its own shape.",
                "Complete Destroy Epilogue",
                "[ECHO-7] No master signal remains. The world is harsh, free, and finally unowned.",
                List.of(new ItemStack(ModItems.NEXUS_ANNIHILATOR.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isFinalBossDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("destroy_finale"),
                true,
                "",
                PostNexusData.NexusPath.DESTROY
        ));

        // CONTROL Path Missions
        phase8.add(new Mission(
                "control_signal_expansion",
                "[ECHO-7] Your reach must extend. Place 3 Signal Scanner or Relay Station blocks as beacons in the Overworld.",
                "Place 3 Signal Beacons",
                "[ECHO-7] The network spreads. Your voice carries farther with each booster deployed.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2), new ItemStack(ModItems.CIRCUIT_BOARD.get(), 8)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).getSignalBoostersPlaced() >= 3,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("reach_decision"),
                false,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_resource_dominance",
                "[ECHO-7] Power requires resources. Collect or carry 50 Dense Alloy Chunks, 50 Nexus Crystals, and 50 Energy Cells.",
                "Collect 50 of Each Rare Resource",
                "[ECHO-7] You have gathered what the old world could not hold. Your dominion is secured by wealth.",
                List.of(new ItemStack(ModItems.PREFALL_ARCHIVES_KEY.get(), 1), new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 5)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isResourceDominanceComplete()
                        || hasControlRareResources(player),
                List.of(
                        new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), PostNexusData.CONTROL_DENSE_ALLOY_REQUIRED),
                        new ItemStack(ModItems.NEXUS_CRYSTAL.get(), PostNexusData.CONTROL_NEXUS_CRYSTALS_REQUIRED),
                        new ItemStack(ModItems.ENERGY_CELL.get(), PostNexusData.CONTROL_ENERGY_CELLS_REQUIRED)
                ),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.EXTREME,
                List.of("control_signal_expansion"),
                false,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_enter_archives",
                "[ECHO-7] The Archives respond to your command lattice. Right-click the Archives Key on overworld ground; I will issue a Return Keystone inside.",
                "Enter the Pre-Fall Archives",
                "[ECHO-7] Archive gate claimed. Return Keystone fallback active. The final guardian stands between you and total control.",
                List.of(new ItemStack(ModItems.RETURN_KEYSTONE.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).hasEnteredArchives(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.EXPLORATION,
                Mission.Difficulty.EXTREME,
                List.of("control_resource_dominance"),
                false,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new Mission.LocationRequirement("dimension", "echoashfallprotocol:prefall_archives", "Pre-Fall Archives")),
                Collections.emptyList(),
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_guardian",
                "[ECHO-7] A rival power stirs in the depths. The Warden runs defender lockdowns and pulse phases; enter with medicine, top-tier weapons, and a Return Keystone.",
                "Defeat The Warden in Pre-Fall Archives",
                "[ECHO-7] The Warden yields. The command lattice is exposed, but it is not yet bound.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 8)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isWardenDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("control_enter_archives"),
                false,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_command_lattice",
                "[ECHO-7] The Warden was midpoint, not closure. Bind the relay network into a command lattice from the terminal operation channel.",
                "Bind Command Lattice",
                "[ECHO-7] Command lattice bound. Mirror Command is exposed.",
                List.of(new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 4), new ItemStack(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get(), 2)),
                MissionRegistry::hasPathOperationComplete,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.TECH,
                Mission.Difficulty.EXTREME,
                List.of("control_guardian"),
                false,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_finale",
                "[ECHO-7] Mirror Command is reflecting your signal back through every relay. Defeat the path finale, then seal the Control epilogue.",
                "Defeat Mirror Command",
                "[ECHO-7] Mirror Command bound. The Control epilogue can now be sealed.",
                List.of(new ItemStack(ModItems.NEXUS_BLADE.get(), 1)),
                MissionRegistry::hasFinalBossDefeated,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                Mission.Difficulty.EXTREME,
                List.of("control_command_lattice"),
                false,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        phase8.add(new Mission(
                "control_epilogue",
                "[ECHO-7] The guardian has fallen. Turn in this terminal protocol and bind the wasteland network to your signal.",
                "Complete Control Epilogue",
                "[ECHO-7] Every beacon answers. The wasteland has a new Core, and it knows your name.",
                List.of(new ItemStack(ModItems.NEXUS_BLADE.get(), 1)),
                player -> com.knoxhack.echoashfallprotocol.endgame.PostNexusData.get(player).isFinalBossDefeated(),
                Collections.emptyList(),
                null,
                Mission.MissionCategory.STORY,
                Mission.Difficulty.EXTREME,
                List.of("control_finale"),
                true,
                "",
                PostNexusData.NexusPath.CONTROL
        ));
        
        PHASES.put(8, phase8); // Phase 8: Post-Nexus Endgame
        canonicalizeAshfallRoute();
    }

    private static void canonicalizeAshfallRoute() {
        Map<String, Mission> byId = new LinkedHashMap<>();
        for (List<Mission> phaseMissions : PHASES.values()) {
            for (Mission mission : phaseMissions) {
                byId.putIfAbsent(mission.id(), AshfallMissionRoute.adjust(mission));
            }
        }

        Set<String> routed = new LinkedHashSet<>();
        Map<Integer, List<Mission>> canonical = new LinkedHashMap<>();
        for (int phase = 0; phase < AshfallMissionRoute.phaseCount(); phase++) {
            List<Mission> missions = new ArrayList<>();
            for (String missionId : AshfallMissionRoute.phaseRoute(phase)) {
                Mission mission = byId.get(missionId);
                if (mission != null) {
                    missions.add(mission);
                    routed.add(missionId);
                }
            }
            canonical.put(phase, missions);
        }

        for (Mission mission : byId.values()) {
            if (!routed.contains(mission.id()) && !AshfallMissionRoute.isDeprecated(mission.id())) {
                canonical.computeIfAbsent(AshfallMissionRoute.phaseCount() - 1, ignored -> new ArrayList<>()).add(mission);
            }
        }

        PHASES.clear();
        for (int phase = 0; phase < AshfallMissionRoute.phaseCount(); phase++) {
            PHASES.put(phase, List.copyOf(canonical.getOrDefault(phase, Collections.emptyList())));
        }
    }

    public static List<Mission> getMissionsForPhase(int phase) {
        return PHASES.getOrDefault(phase, Collections.emptyList());
    }

    public static Mission getMission(int phase, int index) {
        List<Mission> missions = getMissionsForPhase(phase);
        if (index >= 0 && index < missions.size()) {
            return missions.get(index);
        }
        return null;
    }

    public static int getPhaseCount() {
        return PHASES.size();
    }

    public static String getPhaseTitle(int phase) {
        return AshfallMissionRoute.phaseTitle(phase);
    }

    public static int getMissionCount(int phase) {
        return getMissionsForPhase(phase).size();
    }
    
    /**
     * Look up a mission by its unique ID across all phases
     */
    public static Mission getMissionById(String missionId) {
        if (missionId == null || missionId.isEmpty()) return null;
        for (int phase = 0; phase < PHASES.size(); phase++) {
            for (Mission mission : getMissionsForPhase(phase)) {
                if (mission.id().equals(missionId)) {
                    return mission;
                }
            }
        }
        return null;
    }
    
    /**
     * Get all missions as a flat list
     */
    public static List<Mission> getAllMissions() {
        List<Mission> all = new ArrayList<>();
        for (int phase = 0; phase < PHASES.size(); phase++) {
            all.addAll(getMissionsForPhase(phase));
        }
        return all;
    }

    /**
     * Public method to check if a player has placed a specific block near them.
     * Used by terminal/HUD providers for displaying block requirements.
     */
    public static boolean hasBlockNearPlayer(Player player, String blockName) {
        Block target = switch (blockName) {
            case "hand_recycler" -> ModBlocks.HAND_RECYCLER.get();
            case "ash_campfire" -> ModBlocks.ASH_CAMPFIRE.get();
            case "micro_generator" -> ModBlocks.MICRO_GENERATOR.get();
            case "thermal_burner" -> ModBlocks.THERMAL_BURNER.get();
            case "thermal_array" -> ModBlocks.THERMAL_ARRAY.get();
            case "water_purifier" -> ModBlocks.WATER_PURIFIER.get();
            case "ore_grinder" -> ModBlocks.ORE_GRINDER.get();
            case "isotope_refiner" -> ModBlocks.ISOTOPE_REFINER.get();
            case "field_med_bay" -> ModBlocks.FIELD_MED_BAY.get();
            case "signal_scanner" -> ModBlocks.SIGNAL_SCANNER.get();
            case "power_node" -> ModBlocks.POWER_NODE.get();
            case "nexus_core" -> ModBlocks.NEXUS_CORE.get();
            case "research_lab" -> ModBlocks.RESEARCH_LAB.get();
            case "relay_station" -> ModBlocks.RELAY_STATION.get();
            case "workshop_block" -> ModBlocks.WORKSHOP_BLOCK.get();
            case "deep_core_miner" -> ModBlocks.DEEP_CORE_MINER.get();
            case "radiation_cleanser" -> ModBlocks.RADIATION_CLEANSER.get();
            case "atmospheric_scrubber" -> ModBlocks.ATMOSPHERIC_SCRUBBER.get();
            case "autofeed_hopper" -> ModBlocks.AUTOFEED_HOPPER.get();
            case "contaminant_condenser" -> ModBlocks.CONTAMINANT_CONDENSER.get();
            case "battery_bank" -> ModBlocks.BATTERY_BANK.get();
            case "scrap_dynamo" -> ModBlocks.SCRAP_DYNAMO.get();
            case "filter_workbench" -> ModBlocks.FILTER_WORKBENCH.get();
            case "scrap_press" -> ModBlocks.SCRAP_PRESS.get();
            case "power_cable" -> ModBlocks.POWER_CABLE.get();
            case "reinforced_power_cable" -> ModBlocks.REINFORCED_POWER_CABLE.get();
            case "high_voltage_power_cable" -> ModBlocks.HIGH_VOLTAGE_POWER_CABLE.get();
            case "energy_meter" -> ModBlocks.ENERGY_METER.get();
            case "load_distributor" -> ModBlocks.LOAD_DISTRIBUTOR.get();
            case "nexus_capacitor" -> ModBlocks.NEXUS_CAPACITOR.get();
            case "item_pipe" -> ModBlocks.ITEM_PIPE.get();
            case "factory_controller" -> ModBlocks.FACTORY_CONTROLLER.get();
            case "rain_collector" -> ModBlocks.RAIN_COLLECTOR.get();
            case "mutated_sapling" -> ModBlocks.MUTATED_SAPLING.get();
            case "chest" -> Blocks.CHEST;
            default -> null;
        };

        if (target == null) return false;

        BlockPos center = player.blockPosition();
        long gameTime = player.level().getGameTime();
        BlockProbeKey cacheKey = new BlockProbeKey(
                player.getUUID(),
                player.level().dimension().toString(),
                blockName,
                center.getX(),
                center.getY(),
                center.getZ());
        BlockProbeResult cached = BLOCK_PROBE_CACHE.get(cacheKey);
        if (cached != null && gameTime - cached.gameTime() <= BLOCK_PROBE_CACHE_TICKS) {
            return cached.found();
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean found = false;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -20; dz <= 20; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (player.level().getBlockState(cursor).is(target)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (found) break;
        }
        BLOCK_PROBE_CACHE.put(cacheKey, new BlockProbeResult(gameTime, found));
        pruneBlockProbeCache(gameTime);
        return found;
    }

    public static void invalidateBlockProbeCache(Player player) {
        if (player == null) return;
        UUID playerId = player.getUUID();
        String dimension = player.level().dimension().toString();
        BLOCK_PROBE_CACHE.keySet().removeIf(key ->
                key.playerId().equals(playerId) && key.dimension().equals(dimension));
    }

    private static void pruneBlockProbeCache(long gameTime) {
        if (BLOCK_PROBE_CACHE.size() <= BLOCK_PROBE_CACHE_MAX) {
            return;
        }
        BLOCK_PROBE_CACHE.entrySet().removeIf(entry -> gameTime - entry.getValue().gameTime() > BLOCK_PROBE_CACHE_TICKS);
        if (BLOCK_PROBE_CACHE.size() <= BLOCK_PROBE_CACHE_MAX) {
            return;
        }
        Iterator<BlockProbeKey> iterator = BLOCK_PROBE_CACHE.keySet().iterator();
        while (BLOCK_PROBE_CACHE.size() > BLOCK_PROBE_CACHE_MAX && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record BlockProbeKey(UUID playerId, String dimension, String blockName, int x, int y, int z) {}

    private record BlockProbeResult(long gameTime, boolean found) {}

    private static boolean hasActivatedNexusGrid(Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;

        BlockPos center = player.blockPosition();
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-24, -8, -24), center.offset(24, 8, 24))) {
            if (!level.getBlockState(cursor).is(ModBlocks.NEXUS_CORE.get())) continue;
            if (level.getBlockEntity(cursor) instanceof com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity core
                    && core.getActivatedNodeCount(level, cursor) >= com.knoxhack.echoashfallprotocol.block.NexusCoreBlock.REQUIRED_NODES) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasControlRareResources(Player player) {
        return hasItemCount(player, new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), PostNexusData.CONTROL_DENSE_ALLOY_REQUIRED))
                && hasItemCount(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), PostNexusData.CONTROL_NEXUS_CRYSTALS_REQUIRED))
                && hasItemCount(player, new ItemStack(ModItems.ENERGY_CELL.get(), PostNexusData.CONTROL_ENERGY_CELLS_REQUIRED));
    }

    private static boolean hasItemCount(Player player, ItemStack required) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == required.getItem()) {
                count += stack.getCount();
                if (count >= required.getCount()) return true;
            }
        }
        return false;
    }

    private static boolean hasChargedBattery(Player player, int minimumEnergy) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BatteryItem
                    && BatteryItem.getStoredEnergy(stack) >= minimumEnergy) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyAlloyWeapon(Player player) {
        return player.getInventory().contains(new ItemStack(ModItems.ALLOY_BLADE.get()))
                || player.getInventory().contains(new ItemStack(ModItems.ALLOY_HAMMER.get()));
    }

    private static boolean hasEquippedAlloyKit(Player player) {
        return hasAnyAlloyWeapon(player)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ALLOY_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ALLOY_CHESTPLATE.get());
    }

    private static boolean hasMidgameRouteSupplies(Player player) {
        boolean foodReady = hasItemCount(player, new ItemStack(ModItems.EMERGENCY_RATION.get(), 3))
                || hasItemCount(player, new ItemStack(ModItems.WILD_BERRY.get(), 12));
        boolean medicineReady = hasItemCount(player, new ItemStack(ModItems.BANDAGE.get(), 3))
                || player.getInventory().contains(new ItemStack(ModItems.STIM_PACK.get()));
        boolean filterReady = player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()))
                || player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get()));
        boolean portablePowerReady = hasChargedBattery(player, 1_000)
                || hasItemCount(player, new ItemStack(ModItems.ENERGY_CELL.get(), 2));

        return hasItemCount(player, new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 3))
                && foodReady
                && medicineReady
                && hasItemCount(player, new ItemStack(ModItems.RAD_AWAY.get(), 2))
                && filterReady
                && portablePowerReady;
    }

    private static boolean hasMidgameGridReady(Player player) {
        boolean upgradedCable = hasBlockNearPlayer(player, "reinforced_power_cable")
                || hasBlockNearPlayer(player, "high_voltage_power_cable");
        boolean bufferedStorage = hasBlockNearPlayer(player, "battery_bank")
                || hasBlockNearPlayer(player, "nexus_capacitor");

        return hasBlockNearPlayer(player, "thermal_array")
                && hasBlockNearPlayer(player, "factory_controller")
                && hasBlockNearPlayer(player, "energy_meter")
                && upgradedCable
                && bufferedStorage;
    }
    
    /**
     * Check if player has discovered a specific POI
     */
    private static boolean hasDiscoveredPOI(Player player, String poiId) {
        return com.knoxhack.echoashfallprotocol.echo.QuestData.get(player).hasDiscoveredPOI(poiId);
    }

    private static boolean hasAnyFactionContact(Player player) {
        QuestData quest = QuestData.get(player);
        return quest.hasVisitedLocation("special", "faction_contact:any")
                || EchoCoreServices.factionProfiles(player).stream()
                        .anyMatch(profile -> AshfallFactionMap.isAshfall(profile.definition().id()) && profile.contacted());
    }

    private static boolean hasFactionContact(Player player, String factionId) {
        Identifier id = AshfallFactionMap.resolveFactionId(factionId);
        return EchoCoreServices.factionProfile(player, id).map(profile -> profile.contacted() || profile.contactCount() > 0)
                .orElse(false)
                || QuestData.get(player).hasVisitedLocation("special", "faction_contact:" + id.getPath());
    }

    private static boolean hasSpecialMarker(Player player, String marker) {
        return QuestData.get(player).hasVisitedLocation("special", marker);
    }

    private static boolean hasRadiationZoneScout(Player player) {
        QuestData quest = QuestData.get(player);
        return quest.hasVisitedLocation("biome", "radiation_zone")
                || quest.hasVisitedLocation("special", "hazard:radiation_zone");
    }

    private static boolean hasWastelandFood(Player player) {
        return player.getInventory().contains(new ItemStack(ModItems.WILD_BERRY.get()))
                || player.getInventory().contains(new ItemStack(ModItems.EMERGENCY_RATION.get()));
    }

    private static boolean hasRationStockpile(Player player) {
        return hasItemCount(player, new ItemStack(ModItems.EMERGENCY_RATION.get(), 4))
                || hasItemCount(player, new ItemStack(ModItems.WILD_BERRY.get(), 12));
    }

    private static boolean hasAnyBed(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.getItem() instanceof BedItem || stack.is(ModBlocks.EMERGENCY_BUNK_ITEM.get()))) return true;
        }
        return false;
    }

    private static boolean hasAnyDataLog(Player player) {
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation("special", "data_log:archived")) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof DataLogItem) return true;
        }
        return false;
    }

    private static boolean hasEchoDroneRepairAtLeast(Player player, int repairPercent) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        return !level.getEntitiesOfClass(
                com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone.class,
                player.getBoundingBox().inflate(128.0),
                drone -> player.getUUID().equals(drone.getOwnerUUID()) && drone.getRepairLevel() >= repairPercent
        ).isEmpty();
    }

    private static boolean hasEchoDroneMode(Player player, com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone.DroneMode mode) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        return !level.getEntitiesOfClass(
                com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone.class,
                player.getBoundingBox().inflate(128.0),
                drone -> player.getUUID().equals(drone.getOwnerUUID()) && drone.getCurrentMode() == mode
        ).isEmpty();
    }

    private static boolean hasScoutDroneSupport(Player player) {
        if (player.getInventory().contains(new ItemStack(ModItems.SCOUT_DRONE_ITEM.get()))) return true;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        return !level.getEntitiesOfClass(
                com.knoxhack.echoashfallprotocol.entity.ScoutDrone.class,
                player.getBoundingBox().inflate(128.0),
                drone -> player.getUUID().equals(drone.getOwnerUUID())
        ).isEmpty();
    }

    private static boolean isNexusCampaignAwakened(Player player) {
        NexusCampaignData campaign = nexusCampaignData(player);
        return campaign != null && campaign.isAwakened();
    }

    private static boolean hasScannedPrimeRelays(Player player) {
        NexusCampaignData campaign = nexusCampaignData(player);
        return campaign != null && campaign.getScannedRelayCount() >= NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT;
    }

    private static boolean hasResolvedPrimeRelays(Player player) {
        NexusCampaignData campaign = nexusCampaignData(player);
        return campaign != null && campaign.getResolvedRelayCount() >= NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT;
    }

    private static boolean hasSurvivedCoreCountermeasure(Player player) {
        NexusCampaignData campaign = nexusCampaignData(player);
        return campaign != null && campaign.isSiegeComplete();
    }

    private static boolean hasPathOperationComplete(Player player) {
        return PostNexusData.get(player).getPathOperationsComplete() >= 1;
    }

    private static boolean hasFinalBossDefeated(Player player) {
        return PostNexusData.get(player).isFinalBossDefeated();
    }

    private static NexusCampaignData nexusCampaignData(Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return null;
        }
        return NexusCampaignData.get(level.getServer().overworld());
    }

    private static Mission bossMission(
            String id,
            String bossName,
            String entityId,
            String structureName,
            String biomeName,
            String prerequisite,
            Mission.Difficulty difficulty,
            List<ItemStack> rewards
    ) {
        return new Mission(
                id,
                "[ECHO-7] Guardian signal locked below " + biomeName + ". Scan for the visible " + structureName + " entrance, descend into the buried node, read the room before it reads you, then neutralize " + bossName + ".",
                "Defeat " + bossName,
                "[ECHO-7] " + bossName + " neutralized. Underground Gridfall node archived; another old instruction has gone quiet.",
                rewards,
                player -> QuestData.get(player).getEntityKills(entityId) >= 1,
                Collections.emptyList(),
                null,
                Mission.MissionCategory.COMBAT,
                difficulty,
                List.of(prerequisite),
                false,
                "",
                Collections.emptyList(),
                List.of(new Mission.EntityKillRequirement(entityId, 1, bossName)),
                Collections.emptyList()
        );
    }
}



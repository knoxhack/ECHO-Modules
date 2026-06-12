package com.knoxhack.echoashfallprotocol.echo;

import java.util.Map;

/**
 * Short practical field guides for the mission detail card.
 * These explain how the main mission item, block, enemy, location, or system works.
 */
public final class MissionGuideRegistry {
    private static final Guide FALLBACK = new Guide(
            "ECHO FIELD NOTE",
            "ECHO has no recovered field note for this protocol yet. Follow the visible objective and keep a retreat path open.");

    private static final Map<String, Guide> GUIDES = Map.ofEntries(
            Map.entry("fix_mask_filter", guide("Basic Filter Cartridge", "Basic Filter Cartridges are expedition supplies for toxic-air pockets. They drain only while a mask is actively filtering hazard air.")),
            Map.entry("equip_gas_mask", guide("Gas Mask", "The Gas Mask is for marked toxic zones, not normal travel. Equip it in the helmet slot before entering hazard pockets.")),
            Map.entry("drink_clean_water", guide("Clean Water", "Clean Water is the safe first drink and the first proof that the pod route is still survivable. Confirm a starter bottle before scouting, then turn dirty-water collection into a planned loop.")),
            Map.entry("secure_emergency_water_loop", guide("Emergency Water Loop", "Collect dirty water first, then craft one Clean Water Bottle with Dirty Water, Ash, and a Basic Filter. This proves a manual fallback; the powered purifier is still the real solution.")),
            Map.entry("get_dirty_water", guide("Dirty Water", "Dirty Water is a failure-state resource, not a lifestyle. Fill bottles from water sources, store them at shelter, and feed them into purification before long routes.")),
            Map.entry("emergency_filter_water", guide("Emergency Clean Water", "Dirty Water + Basic Filter + Ash makes one Clean Water Bottle by hand. It burns the whole filter, so treat it as a survival override until the Water Purifier takes over.")),
            Map.entry("craft_scrap_knife", guide("Scrap Knife", "The Scrap Knife is a metal salvage tool, not the final weapon plan. Use it to cut wire, open loose wreckage, and recover scrap near the pod before machines need that metal.")),
            Map.entry("secure_crash_outpost", guide("Ash Campfire", "The Ash Campfire marks the pod as a recoverable route instead of a crash crater. Place it near the pod, then keep storage, light, and early rewards inside that safety radius.")),
            Map.entry("forage_wasteland_food", guide("Food Buffer", "Wild berries and emergency rations buy time while machines come online. The wasteland does not feed you kindly, but a counted reserve keeps panic out of the first expedition.")),
            Map.entry("plant_mutated_sapling", guide("Mutated Sapling", "Mutated saplings give renewable wood where healthy forests failed. Plant one near shelter, then use the reward bridge to start rain collection before water pressure returns.")),
            Map.entry("build_rain_collector", guide("Rain Collector", "Rain Collectors turn bad weather into dirty water. Craft it from the rewarded cauldron and scrap plastic, then purify the output before trusting it on routes.")),
            Map.entry("stockpile_rations", guide("Ration Buffer", "Food reserves keep exploration from becoming a panic spiral. Keep 4 Emergency Rations or 12 Wild Berries at shelter before the next push.")),
            Map.entry("secure_sleep_shelter", guide("Sleep Shelter", "A bed and lit shelter give you a retreat point and a reason not to fight the dark. Claim this reward before assembling the non-metal field kit.")),
            Map.entry("assemble_wasteland_field_kit", guide("Wasteland Field Kit", "Bone and hide tools keep you alive without burning scrap that belongs in the machine chain. The kit gives close work, reach, and dust protection in one checkpoint.")),
            Map.entry("craft_bone_knife", guide("Ashbone Shiv", "The Ashbone Shiv keeps you armed when metal is scarce. It is a primitive backup blade for early scavenging and survival.")),
            Map.entry("craft_crude_spear", guide("Scavenger Spear", "The Scavenger Spear gives safer reach against early threats. Use spacing, backpedal, and avoid long fights until armor improves.")),
            Map.entry("craft_hide_wrap", guide("Hide Wrap", "The Hide Wrap is crude breathing protection. Claim its schematic reward before starting the machine chain.")),
            Map.entry("find_schematic_fragment", guide("Schematic Fragment", "Schematic Fragments are compressed process memory: recipes, tolerances, and repairs that ECHO-7 cannot fully infer from wreckage alone. This early chain keeps the first recycler reliable.")),
            Map.entry("build_hand_recycler", guide("Hand Recycler", "The Hand Recycler is the first machine bridge. Use the guaranteed schematic casing, 4 Scrap Metal, and 4 Scrap Wire; ECHO issues a 1000 FE starter battery when the recycler comes online.")),
            Map.entry("make_machine_casing", guide("Machine Casing", "Machine Casings are the frame for most technology blocks. After proving one casing, claim the generator bridge parts and build power beside the recycler.")),
            Map.entry("build_micro_generator", guide("Micro Generator", "The Micro Generator is early power. The first-hour recipe needs one casing, scrap wire, an energy cell, a circuit board, and scrap metal.")),
            Map.entry("build_water_purifier", guide("Water Purifier", "The Water Purifier recipe needs scrap plastic, a filtration membrane, three machine casings, and a circuit board. Keep it beside power and stocked with dirty bottles and filters.")),
            Map.entry("stockpile_clean_water", guide("Clean Water Reserve", "A purifier is only useful if you keep reserves. Stockpile clean bottles at base and carry one before leaving shelter.")),
            Map.entry("build_filter_workbench", guide("Filter Workbench", "The Filter Workbench turns toxic-zone filtration from emergency scavenging into planned expedition supply. Build it after water and power are stable.")),
            Map.entry("craft_advanced_filter", guide("Advanced Filter Cartridge", "Advanced Filters last longer inside toxic-air pockets. Keep one installed and one spare only when the route calls for filtration.")),
            Map.entry("build_battery_bank", guide("Battery Bank", "Battery Banks store and distribute power so machines do not stall whenever generation dips. Place one near your generator and early machines.")),
            Map.entry("build_scrap_dynamo", guide("Scrap Dynamo", "The Scrap Dynamo is the first steady workshop generator. Feed it scrap or coal, then route output through banks and cables instead of relying on nearby-power luck.")),
            Map.entry("charge_basic_battery", guide("Portable Batteries", "Batteries store FE on the item. Insert one into a generator or Battery Bank to charge it, insert it into machines to power them, or sneak-right-click energy blocks to move charge directly.")),
            Map.entry("route_power_cable", guide("Power Cable", "Power Cable extends the base power network beyond direct adjacency. Use it to route energy from generators and battery banks to machines.")),
            Map.entry("upgrade_power_cable", guide("Cable Tiers", "Starter cable is intentionally limited. Reinforced and high-voltage cable move more FE per tick, so use better cable on high-demand branches.")),
            Map.entry("install_energy_meter", guide("Energy Meter", "Right-click an Energy Meter to inspect stored FE, demand, brownouts, priority pauses, and the weakest cable bottleneck in the connected grid.")),
            Map.entry("set_power_priority", guide("Load Distributor", "Load Distributors mark the grid priority mode. Use Survival First when water and medical machines matter more than factory throughput.")),
            Map.entry("build_scrap_press", guide("Scrap Press", "The Scrap Press is a powered machine for compressing scrap materials. It is your first step from hand salvage into workshop-scale production.")),
            Map.entry("overclock_machine", guide("Machine Upgrades", "Overclock modules make machines faster but hungrier. Efficiency modules reduce FE demand. Watch the Energy Meter before stacking upgrades.")),
            Map.entry("install_item_pipe", guide("Item Pipe", "Item Pipes move outputs between nearby machines and storage. Use them to reduce manual hauling as your base grows.")),
            Map.entry("build_thermal_burner", guide("Thermal Burner", "The Thermal Burner provides heat for advanced processing. Build it after water security so higher-tier crafting can begin.")),
            Map.entry("base_stability_check", guide("Base Stability", "A stable outpost has storage, water purification, buffered power, and a small recovery kit. Confirm this before the map turns from survival problem into expedition problem.")),
            Map.entry("first_faction_contact", guide("Faction Contacts", "Faction channels open through Echo Core contacts, POI affinity, and route work. One verified contact is enough to put the crossband network on the map.")),
            Map.entry("contact_radwarden_compact", guide("Radwarden Contact", "Find a Radwarden route contact near radiation, containment, warning, or hot-zone POIs. They trade in decon support and route safety.")),
            Map.entry("contact_crashbreak_salvage", guide("Crashbreak Contact", "Find a Crashbreak contact around wreckage, crash fields, and salvage routes. They turn risk into maps, parts, and beacon repairs.")),
            Map.entry("contact_sporebound_sanctum", guide("Sporebound Contact", "Find a Sporebound contact near toxic growth, biological labs, or living work sites. They treat adaptation as medicine and field science.")),
            Map.entry("complete_first_faction_task", guide("Faction Contracts", "Contact opens a channel; completing a field contract earns trust. Start with any available Echo Core faction contract.")),
            Map.entry("complete_radwarden_contract", guide("Radwarden Contract", "Complete one Radwarden contract to prove containment discipline and unlock better hazard support.")),
            Map.entry("complete_crashbreak_contract", guide("Crashbreak Contract", "Complete one Crashbreak contract to prove salvage reliability and unlock route material support.")),
            Map.entry("complete_sporebound_contract", guide("Sporebound Contract", "Complete one Sporebound contract to prove biological field control and unlock better medical support.")),
            Map.entry("build_research_lab", guide("Research Lab", "The Research Lab processes fragments and unlocks advanced recipes. Build it near your base so research and crafting stay connected.")),
            Map.entry("first_schematic", guide("Schematic Unlocks", "Schematics convert fragments into usable recipe knowledge. Use the Research Lab to turn salvage finds into permanent progression.")),
            Map.entry("build_factory_controller", guide("Factory Controller", "The Factory Controller scans connected machines through cables and pipes. Use it as the oversight block for a larger powered workshop.")),
            Map.entry("craft_portable_scanner", guide("Portable Signal Scanner", "The Portable Signal Scanner searches for nearby exploration sites and reports route, hazard, prep, supplies, and objective state. The terminal Route Map POI Atlas turns those hits into a full template catalog.")),
            Map.entry("expedition_readiness", guide("Expedition Kit", "A scanner finds danger; supplies let you survive it. Pack clean water, bandages or medicine, and route-specific extras such as RadAway or spare filters.")),
            Map.entry("scan_first_poi", guide("First POI Scan", "Run the portable scanner until ECHO records a structure. Each site has a route, hazard profile, prep hint, reward track, and field-log state; after the first scan, check Route Map -> POI Atlas for the concrete template variants behind that profile.")),
            Map.entry("loot_survivor_cache", guide("Survivor Cache", "Small shelters and survivor logs teach safe scavenging: take water, food, medicine, and information before pushing onward.")),
            Map.entry("enter_bio_lab", guide("Bio Lab", "Bio labs and mutant sanctuaries contain medical supplies, tissue samples, toxic pockets, and mutation threats. Bring filters only for the hazard air inside.")),
            Map.entry("recover_data_log", guide("Data Logs", "Data Logs explain why a location still matters. Read them like hazard gear: context will not stop a blade, but it can keep you from walking into one.")),
            Map.entry("clear_military_vault", guide("Military Vault", "Military vaults and Radwarden sites hold dense materials and combat-grade supplies. Prepare armor, medicine, and an exit route.")),
            Map.entry("survey_reactor_ruin", guide("Reactor Ruin", "Reactor sites are radiation lessons. Carry RadAway, monitor exposure, use scrubber pockets, and leave before severe exposure is sustained.")),
            Map.entry("find_dense_alloy", guide("Dense Alloy", "Dense Alloy is a high-grade material found through vaults, deep mining, or advanced salvage. You need it for serious machinery.")),
            Map.entry("repair_echo_drone", guide("Drone Recon", "Repair the companion drone if it is available, or deploy a Scout Drone as backup support. The drone is utility, witness, and warning layer in one damaged shell.")),
            Map.entry("upgrade_drone_support", guide("Drone Support", "Stable drone support means a repaired companion or an active Scout Drone. Use terminal controls before long expeditions, then let the machine risk the first look.")),
            Map.entry("set_drone_scout_mode", guide("Scout Mode", "Scout mode sends the drone ahead as a warning layer. Use it before entering vaults, labs, or unfamiliar POIs.")),
            Map.entry("recover_drone_intel", guide("Drone Intel", "Drone intel represents a successful recon pass from the repaired ECHO drone. Let it mark risk before you commit supplies, armor, and blood.")),
            Map.entry("deploy_scout_drone", guide("Scout Drone", "A Scout Drone is a backup recon unit when the companion is unavailable or you need extra support on routes.")),
            Map.entry("faction_reputation", guide("Reputation", "Reputation measures faction trust. Complete trades, tasks, and exploration goals to unlock better stock and faction support.")),
            Map.entry("first_perk", guide("Research Perks", "Research perks are permanent upgrades bought with research progress. Choose perks that support your current survival bottleneck.")),
            Map.entry("poi_explorer", guide("POI Exploration", "Points of interest are expedition loops: scan, prepare for the listed hazard, enter, recover the objective cache or log, then return before supplies collapse. The POI Atlas is recognition support; scanner profiles remain the actual mission and save identity.")),
            Map.entry("build_field_med_bay", guide("Field Med Bay", "The Field Med Bay supports healing and medical crafting. Keep it stocked so radiation, wounds, and mutations do not snowball.")),
            Map.entry("scan_mutation_status", guide("Mutation Status", "Use the Field Med Bay as a biological checkpoint. Know your mutation state before using catalysts or entering hot zones.")),
            Map.entry("use_field_med_bay", guide("Med Bay Treatment", "A powered Field Med Bay is both treatment and mutation triage. Stand close long enough for recovery, then trust its scan before entering hotter biology sites.")),
            Map.entry("craft_radaway", guide("RadAway", "RadAway lowers radiation after exposure. Make it from Clean Water + Ash, or use the dirty-water emergency recipe with a Basic Filter. Retreat first, treat second.")),
            Map.entry("stabilize_mutation_effects", guide("Mutation Stabilization", "Carry RadAway, bandages, and med bay access before experimenting. Mutation benefits have survival costs.")),
            Map.entry("scout_radiation_zone", guide("Radiation Zone Scout", "Radiation zones are for short controlled trips until shielding improves. Carry RadAway, watch the RAD meter, and leave before severe exposure is sustained.")),
            Map.entry("build_atmospheric_scrubber", guide("Atmospheric Scrubber", "Atmospheric Scrubbers create expedition recovery pockets: toxic air is suppressed, filter drain stops, and radiation decays faster nearby.")),
            Map.entry("build_radiation_cleanser", guide("Radiation Cleanser", "The Radiation Cleanser handles contaminated salvage and keeps production safe. It complements RadAway, scrubber recovery zones, and Hazmat gear.")),
            Map.entry("collect_mutated_tissue", guide("Mutated Tissue", "Mutated Tissue is the first biological research input. Recover it from biological threats, bio-sites, or faction support.")),
            Map.entry("craft_mutagen_vial", guide("Mutagen Vial", "Mutagen Vials are controlled mutation catalysts. Craft them only after cleanup, medicine, and stabilization are available.")),
            Map.entry("acquire_mutagen", guide("Mutagen Handling", "Mutagen proof comes from the vial you already verified. This checkpoint closes biology prep without asking for a second vial.")),
            Map.entry("build_thermal_array", guide("Thermal Array", "The Thermal Array is the midgame power bridge. Use it after Dense Alloy, route it through storage and reinforced cable, then check the Energy Meter before extraction machines.")),
            Map.entry("build_ore_grinder", guide("Substrate Grinder", "The Substrate Grinder processes raw biome substrate, deepslate, wasteland stone, slag, shale, cryo stone, and trace fragments into resource streams. Keep output and byproduct slots clear; rare substrate takes more power.")),
            Map.entry("build_isotope_refiner", guide("Isotope Refiner", "The Isotope Refiner processes radioactive and advanced materials. Keep filtration and medicine ready before handling its inputs.")),
            Map.entry("forge_alloy_weapon", guide("Alloy Weapon", "Forge an Alloy Blade or Alloy Hammer before guardian routes. Primitive weapons can clear scavengers, but buried nodes need dense-alloy force.")),
            Map.entry("equip_alloy_kit", guide("Alloy Route Kit", "Equip Alloy Helmet and Alloy Chestplate while carrying an alloy weapon. The full set is better, but this confirms you have real midgame protection.")),
            Map.entry("stockpile_route_supplies", guide("Route Supplies", "Pack clean water, food, medicine, RadAway, advanced filtration, and portable FE before guardian routes. Scan first, enter prepared, and retreat early.")),
            Map.entry("calibrate_midgame_grid", guide("Midgame Grid", "A guardian-ready workshop has Thermal Array power, buffered storage, reinforced cable, an Energy Meter, and Factory Controller visibility.")),
            Map.entry("craft_alloy_blade", guide("Alloy Blade", "The Alloy Blade is a stronger combat tool for midgame threats. Craft it before pushing deeper ruins and guardian sites.")),
            Map.entry("deploy_stationary_scanner", guide("Signal Scanner", "The stationary Signal Scanner anchors long-range detection. Place it at a base or outpost to reveal larger exploration targets.")),
            Map.entry("neutralize_plains_warlord", guide("Plains Warlord", "Radwarden flags this bunker as a failed command node. Clear reserve lanes before engaging so the rally does not turn into a long bandit swarm.")),
            Map.entry("neutralize_city_ruin_stalker", guide("City Ruin Stalker", "Crashbreak wants the data-vault lanes readable again. Bring lighting, check corners, and avoid chasing the Stalker into dark corridors.")),
            Map.entry("neutralize_industrial_juggernaut", guide("Industrial Juggernaut", "Crashbreak treats this as machine salvage under fire. Keep moving around heat vents, wait through heavy slams, and bring sustained healing.")),
            Map.entry("neutralize_toxic_hive_matriarch", guide("Toxic Hive Matriarch", "Sporebound reads the hive as adaptation gone territorial. Clear brood pressure quickly, carry elite filters, and avoid camping in hive-pod lanes.")),
            Map.entry("neutralize_crash_zone_colossus", guide("Crash Zone Colossus", "Crashbreak needs the crash-vault telemetry freed from wreckage law. Repair armor first, watch debris fields, and keep space for artillery pulses.")),
            Map.entry("neutralize_radiation_behemoth", guide("Radiation Behemoth", "Radwarden marks this reactor sink as containment failure. Bring RadAway, hazmat support, and a scrubber recovery pocket; treat after pulses.")),
            Map.entry("enter_cryogenic_ruins", guide("Cryogenic Ruins", "Cryogenic ruins threaten temperature before combat. Bring food, hand warmers, and thermal protection.")),
            Map.entry("recover_cryo_sample", guide("Cryo Sample", "Cryogenic fractured stone preserves old technology traces. Recover a sample before committing to deeper cold routes.")),
            Map.entry("warm_up_after_exposure", guide("Warm-Up Protocol", "Use Hand Warmers after cold exposure. Warm up early instead of waiting for movement and health penalties to stack.")),
            Map.entry("craft_cold_route_supplies", guide("Cold Route Kit", "Thermal Liners and Hand Warmers are the cold equivalent of filters and RadAway. Pack both before cryogenic bosses.")),
            Map.entry("neutralize_cryogenic_overseer", guide("Cryogenic Overseer", "Radwarden classifies the frozen facility as containment still issuing orders. Bring thermal liners, hand warmers, food, and a fast weapon.")),
            Map.entry("neutralize_nexus_scar_avatar", guide("Nexus Scar Avatar", "Sporebound treats the Avatar as the last readable anomaly before the Core. Bring your best kit for recursion, drones, weakness, and unstable lanes.")),
            Map.entry("build_scout_drone", guide("Scout Drone", "The Scout Drone extends your awareness and support options. Repair and deploy it before long expeditions or boss hunts.")),
            Map.entry("activate_power_node", guide("Power Node", "Power Nodes are grid anchors. Activating them restores parts of the old network and prepares the path toward the Nexus.")),
            Map.entry("build_nexus_capacitor", guide("Nexus Capacitor", "The Nexus Capacitor is late-grid storage. Use it behind high-voltage cable to absorb guardian-route demand spikes and keep critical machines alive.")),
            Map.entry("build_workshop", guide("Workshop", "The Workshop centralizes higher crafting. Place it near power and storage so late machines and upgrades are easier to manage.")),
            Map.entry("activate_relay_station", guide("Relay Station", "Relay Stations extend signal coverage. Use them to connect distant outposts and support wider grid operations.")),
            Map.entry("find_nexus_core", guide("Nexus Core", "The Nexus Core is locked behind all eight active buried guardian nodes. Stand near the unresolved Core after the Nexus Scar Avatar falls; the grid will know you are there.")),
            Map.entry("awaken_nexus_core", guide("Nexus Awakening", "Awakening the Core starts Nexus Instability. Use the NEXUS terminal tab near the unresolved Core, then watch the pressure meter.")),
            Map.entry("scan_prime_relays", guide("Prime Relay Scan", "The Warfront has six relay identities: Reactor, Cryo, Bio, Transit, Industrial, and Scar. Scan them through the terminal before choosing which three to resolve.")),
            Map.entry("resolve_prime_relays", guide("Relay Resolution", "Each Prime Relay can be stabilized, severed, or overridden. These actions bias Restore, Destroy, or Control readiness, but the final permanent choice stays free.")),
            Map.entry("stabilize_nexus_grid", guide("Nexus Grid", "Stabilizing the grid links five active Power Nodes to the Core. Power Nodes can be crafted or rebuilt if a route stalls.")),
            Map.entry("survive_core_countermeasure", guide("Core Countermeasure", "Once three relays and five Power Nodes are secured, provoke and survive one Core siege. Treat it like a guardian-route holdout with filters, medicine, batteries, and an exit line.")),
            Map.entry("reach_decision", guide("Nexus Decision", "The Nexus choice commits your endgame direction. Use the NEXUS tab near the unresolved Core, select RESTORE, DESTROY, or CONTROL once, then confirm it. The choice is permanent.")),
            Map.entry("restore_repair_nodes", guide("Restore Path", "Restoration repairs the grid instead of breaking it. Activate three Power Nodes; crafted replacements count when the old world has already spent its anchors.")),
            Map.entry("restore_purge_corruption", guide("Corruption Purge", "Purging corruption removes unstable grid residue. Bring cleansing supplies and expect resistance around damaged infrastructure.")),
            Map.entry("restore_enter_archives", guide("Pre-Fall Archives", "Right-click the Archives Key on overworld ground to enter. Lost Archives Keys are craftable, and entry issues a Return Keystone failsafe.")),
            Map.entry("restore_guardian", guide("The Warden", "The Warden runs defender lockdowns and pulse phases. Bring medicine, top-tier weapons, recovery items, and a Return Keystone.")),
            Map.entry("restore_world_lattice", guide("World Lattice", "Restore's final act purifies relay routes after The Warden. Complete the path operation through the terminal to expose Corruption Bloom.")),
            Map.entry("restore_finale", guide("Corruption Bloom", "Corruption Bloom is Restore's path finale. Defeat it before the epilogue can seal the rebuilt grid.")),
            Map.entry("restore_epilogue", guide("Restore Epilogue", "The final Restore protocol resolves the repaired grid after Corruption Bloom falls. Return the epilogue through the mission channel and let the branch close.")),
            Map.entry("destroy_scorched_earth", guide("Destroy Path", "Destroying Power Nodes severs grid control. Crafted or rebuilt nodes can still count if too many old anchors are gone.")),
            Map.entry("destroy_survive_storms", guide("Severe Storm", "Radiation storms, ash storms, Nexus surges, and thunder can satisfy this route. Shelter, RadAway, and clean water are the survival loop.")),
            Map.entry("destroy_enter_archives", guide("Archives Breach", "Right-click the Archives Key on overworld ground to enter. Lost Archives Keys are craftable, and entry issues a Return Keystone failsafe.")),
            Map.entry("destroy_guardian", guide("The Warden", "The Warden guards the final chain of control with lockdowns and pulse phases. Bring your strongest kit and a Return Keystone.")),
            Map.entry("destroy_dead_signal", guide("Dead Signal", "Destroy's final act collapses command infrastructure after The Warden. Complete the path operation through the terminal to expose the Severance Engine.")),
            Map.entry("destroy_finale", guide("Severance Engine", "The Severance Engine preserves the old kill-switch. Defeat it before the epilogue can leave the signal unowned.")),
            Map.entry("destroy_epilogue", guide("Destroy Epilogue", "The final Destroy protocol ends grid authority after the Severance Engine falls. Return the epilogue through the mission channel and leave the signal unowned.")),
            Map.entry("control_signal_expansion", guide("Control Path", "Control expands your command signal through beacons. Place Signal Scanners or Relay Stations in the Overworld.")),
            Map.entry("control_resource_dominance", guide("Resource Dominance", "Control resources count tracked pickups or held inventory. Dense Alloy, Nexus Crystals, and Energy Cells each need fifty.")),
            Map.entry("control_enter_archives", guide("Command Lattice", "Right-click the Archives Key on overworld ground to enter. Lost Archives Keys are craftable, and entry issues a Return Keystone failsafe.")),
            Map.entry("control_guardian", guide("The Warden", "The Warden is the last rival command. Expect lockdowns and pulse phases before the network's deepest layer gives way.")),
            Map.entry("control_command_lattice", guide("Command Lattice", "Control's final act binds relay routes into your command lattice after The Warden. Complete the path operation through the terminal to expose Mirror Command.")),
            Map.entry("control_finale", guide("Mirror Command", "Mirror Command reflects your own signal back through the lattice. Defeat it before the epilogue can bind the wasteland network.")),
            Map.entry("control_epilogue", guide("Control Epilogue", "The final Control protocol binds the wasteland network to your signal after Mirror Command falls. Return the epilogue through the mission channel and accept the cost of command."))
    );

    private MissionGuideRegistry() {
    }

    public static Guide get(String missionId) {
        return GUIDES.getOrDefault(missionId, FALLBACK);
    }

    public static boolean hasGuide(String missionId) {
        return GUIDES.containsKey(missionId);
    }

    private static Guide guide(String title, String body) {
        return new Guide(title, body);
    }

    public record Guide(String title, String body) {}
}

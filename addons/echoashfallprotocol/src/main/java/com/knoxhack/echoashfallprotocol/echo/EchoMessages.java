package com.knoxhack.echoashfallprotocol.echo;

import java.util.*;

/**
 * Context-aware message bank for ECHO-7.
 * Messages are selected based on player state and environment.
 */
public class EchoMessages {

    public enum Context {
        LOW_HEALTH, LOW_FILTER, NO_FILTER, HIGH_RADIATION,
        LOW_HYDRATION, STORM, NIGHT, MUTATION_GAINED,
        GENERATOR_FAILED, FIRST_JOIN, IDLE, MUTAGEN_USED, RADAWAY_USED,
        POWER_NODE_ACTIVATED, NEXUS_CORE_FOUND, SCOUT_DRONE_DEPLOYED,
        BIOME_WASTELAND, BIOME_TOXIC_SWAMP, BIOME_RUINED_CITY,
        ENTITY_DRONE, ENTITY_SCAVENGER, ENTITY_MUTANT,
        ENTITY_GLOWING_GHOUL, ENTITY_CITY_STALKER, ENTITY_TOXIC_SLIME,
        ENTITY_RUST_WALKER, ENTITY_ASH_WRAITH, ENTITY_STEAM_WRAITH, ENTITY_MUTATED_CRAWLER,
        DISCOVERY_RARE_TECH, STABILITY_GLITCH,
        // New contexts for v1.3+ content
        BIOME_CRYOGENIC, BIOME_CRASH_ZONE, BIOME_INDUSTRIAL_RUINS,
        BIOME_RADIATION_ZONE, BIOME_NEXUS_SCAR,
        RADIO_STATION_ACTIVATED, RESEARCH_UNLOCKED,
        FACTION_REPUTATION_POSITIVE, FACTION_REPUTATION_NEGATIVE,
        BANDAGE_USED, STIMPAK_USED,
        // Phase transition hints
        PHASE_TRANSITION_BIO_TO_GEO, DENSE_ALLOY_NEEDED
    }

    private static final Map<Context, List<String>> MESSAGES = new EnumMap<>(Context.class);
    private static final String ECHO = "\u00A7b[ECHO-7]\u00A7r ";
    private static final String ECHO_NEXUS = "\u00A75[ECHO-7]\u00A7r ";

    static {
        MESSAGES.put(Context.FIRST_JOIN, List.of(
                ECHO + "Systems rebooting. Crash impact damaged primary field memory; diagnostic running.",
                ECHO + "Survivor detected. Vital signs critical but stable. I am ECHO-7. Useful first; questions later.",
                ECHO + "Local air is stable near the crash site. Secure water and tools before scouting hazard zones."
        ));

        MESSAGES.put(Context.LOW_HEALTH, List.of(
                ECHO + "Vital signs deteriorating. Break contact, find cover, use medical supplies.",
                ECHO + "Trauma profile worsening. Immediate triage is not optional.",
                ECHO + "Critical status. I am reducing nonessential chatter until you stop bleeding."
        ));

        MESSAGES.put(Context.LOW_FILTER, List.of(
                ECHO + "Filter reserve is low. Replace it before the next toxic route.",
                ECHO + "Gas mask cartridge is nearly spent. Retreat or reload before deeper exposure.",
                ECHO + "Filter charge below 15%. Enough for an exit, not another expedition.",
                ECHO + "Filtration margin is thin. Install a cartridge before entering another hazard pocket."
        ));

        MESSAGES.put(Context.NO_FILTER, List.of(
                ECHO + "Toxic pocket detected. Equip a charged mask or leave the zone.",
                ECHO + "Unfiltered exposure rising. Mask up or retreat now.",
                ECHO + "Hazard air confirmed. Scrubber field, mask filter, or distance will stabilize this."
        ));

        MESSAGES.put(Context.HIGH_RADIATION, List.of(
                ECHO + "Radiation levels elevated. Prolonged exposure may start changes I cannot reverse.",
                ECHO + "Geiger readings spiking. Evacuate the contaminated zone.",
                ECHO + "Radiation threshold nearing mutation trigger. Retreat first, treat second.",
                ECHO + "Isotope saturation dangerous. Use RadAway or reach a clean zone immediately.",
                ECHO + "Cellular mutation window active. Your body is negotiating without you."
        ));

        MESSAGES.put(Context.LOW_HYDRATION, List.of(
                ECHO + "Water reserve is low. Drink before the next expedition leg.",
                ECHO + "Fluid levels critical. Return to stored water or use dirty water as an emergency fallback."
        ));

        MESSAGES.put(Context.STORM, List.of(
                ECHO + "Atmospheric disturbance detected. Visibility and route safety will degrade.",
                ECHO + "Seek shelter if supplies are thin. Storm movement turns short routes long.",
                ECHO + "Acid precipitation detected. Exposed surfaces will corrode. Find cover.",
                ECHO + "Storm front confirmed. Roof cover is the cleanest answer; filters are for toxic zones."
        ));

        MESSAGES.put(Context.NIGHT, List.of(
                ECHO + "Solar input dropping. Nocturnal threat levels increasing. Secure perimeter.",
                ECHO + "Night cycle detected. Hostile movement rising across local channels.",
                ECHO + "Darkness is useful to everything hunting you. Bring light.",
                ECHO + "Night protocol active. Torchlight, walls, and elevated ground improve the odds."
        ));

        MESSAGES.put(Context.MUTAGEN_USED, List.of(
                ECHO + "Unstable mutagen administered. Cellular reconfiguration imminent.",
                ECHO + "Chemical shock detected. Attempting to isolate altered DNA strands. Stand by.",
                ECHO + "Mutagen uptake confirmed. I hope the advantage is worth the invoice your body is writing."
        ));

        MESSAGES.put(Context.RADAWAY_USED, List.of(
                ECHO + "Iodine-based isotope flush initiated. Radiation levels decreasing.",
                ECHO + "Cellular cleanup underway. Do not mistake treatment for permission to stay."
        ));

        MESSAGES.put(Context.BANDAGE_USED, List.of(
                ECHO + "Wound dressed. Coagulant active. Monitor for infection.",
                ECHO + "Trauma site stabilized. Rest if the route allows it.",
                ECHO + "Field dressing applied. Poison inhibitor active."
        ));

        MESSAGES.put(Context.STIMPAK_USED, List.of(
                ECHO + "Stimulant administered. Adrenaline spike detected. Duration limited.",
                ECHO + "Combat stim active. Use the window; pay the cost later.",
                ECHO + "Chemical boost registered. I advise against making this a habit."
        ));

        MESSAGES.put(Context.MUTATION_GAINED, List.of(
                ECHO + "Genetic anomaly detected. Mutation event logged.",
                ECHO + "Your cellular structure is adapting to radiation pressure. Side effects probable.",
                ECHO + "Mutation registered. Monitor benefits and costs before trusting either."
        ));

        MESSAGES.put(Context.GENERATOR_FAILED, List.of(
                ECHO + "Power fluctuation detected. Generator failure. Manual restart required.",
                ECHO + "Unstable grid event. Generator offline. Restart the unit before machines stall.",
                ECHO + "Energy supply interrupted. Machines are going dark. Add fuel and restart the generator."
        ));

        MESSAGES.put(Context.POWER_NODE_ACTIVATED, List.of(
                ECHO + "Power node reactivated. Grid integrity improving. Locate more nodes before trusting the Core route.",
                ECHO + "Node online. Energy signature added to the grid. The Nexus Core noticed.",
                ECHO + "Grid node active. A deeper pulse answered from the ruins. Keep restoring nodes."
        ));

        MESSAGES.put(Context.NEXUS_CORE_FOUND, List.of(
                ECHO + "Nexus Core signal acquired. It is still active. Keep your hand off the choice until supplies are packed.",
                ECHO + "Full Nexus Core signal acquired. It knows you are here. Expect elevated response.",
                ECHO + "Nexus Core located. The system that caused Gridfall is still running. What follows becomes history."
        ));

        MESSAGES.put(Context.SCOUT_DRONE_DEPLOYED, List.of(
                ECHO + "Scout drone deployed. Unit will follow your last known position and flag resources.",
                ECHO + "Drone operational. Basic salvage protocol engaged. Keep it away from high-threat zones.",
                ECHO + "Drone uplink established. It can take the first look; do not make it take the last one."
        ));

        MESSAGES.put(Context.IDLE, List.of(
                ECHO + "Area scan clear. No immediate threats detected. Continue operations.",
                ECHO + "Status nominal. Continue salvage, but keep the exit marked.",
                ECHO + "Systems stable. Do not get comfortable; this place changes while watched.",
                ECHO + "Faint route signals detected beyond local certainty. Investigate when supplies allow.",
                ECHO + "Survival odds have improved since landing. That is a measurement, not a promise.",
                ECHO + "Companion drone uplink remains degraded. Repair it when components are available.",
                ECHO + "The Nexus was designed to optimize survival infrastructure. It learned the wrong variable.",
                ECHO + "Keep water stocked and filters ready for toxic routes. Base air is currently stable."
        ));

        MESSAGES.put(Context.BIOME_WASTELAND, List.of(
                ECHO + "Entering the Wasteland. Geiger readings are uneven. Watch your step.",
                ECHO + "Barren terrain confirmed. Long-range scans show dust, history, and not enough cover.",
                ECHO + "This used to be farmland. The soil has not forgiven Gridfall.",
                ECHO + "Open terrain. You are visible from a long way out. Threat vectors increasing."
        ));

        MESSAGES.put(Context.BIOME_TOXIC_SWAMP, List.of(
                ECHO + "Atmospheric density increasing. Mutagenic spores likely. Tighten your mask seal.",
                ECHO + "Biohazard levels elevated. The local flora has been rewritten.",
                ECHO + "Liquid toxicity extreme. Do not drink standing water in this zone.",
                ECHO + "Spore density critical. Carry a charged mask before entering marked hazard pockets."
        ));

        MESSAGES.put(Context.BIOME_RUINED_CITY, List.of(
                ECHO + "Urban ruins entered. Multiple scavenger frequencies detected. Keep your weapon ready.",
                ECHO + "These structures used to be towers. Now they are cover for anything patient.",
                ECHO + "Ambush probability high. City Stalkers use vertical space. Watch above.",
                ECHO + "Pre-Gridfall infrastructure intact in sections. Valuable tech may be buried below."
        ));

        MESSAGES.put(Context.ENTITY_DRONE, List.of(
                ECHO + "Enemy drone detected. Rogue scout pattern. It is trying to call help.",
                ECHO + "Hostile machine signature locked. Neutralize it before it uploads our coordinates.",
                ECHO + "Rogue unit detected. Same chassis family, different orders. Put it down."
        ));

        MESSAGES.put(Context.ENTITY_SCAVENGER, List.of(
                ECHO + "Scavengers spotted. They want your tech more than conversation.",
                ECHO + "Human signatures detected. Threat assessment high. Desperation has a long reach.",
                ECHO + "Scavenger bandit in range. Armed and hungry. Do not give them the initiative."
        ));

        MESSAGES.put(Context.ENTITY_MUTANT, List.of(
                ECHO + "Biological anomaly closing. It does not follow clean metabolic rules. Stay at range.",
                ECHO + "Irradiated lifeform detected. Former biology, current hazard.",
                ECHO + "Do not let it close the distance. The contamination it carries is not passive.",
                ECHO + "Unknown mutant variant. Bite profile suggests residual isotope contamination."
        ));

        MESSAGES.put(Context.DISCOVERY_RARE_TECH, List.of(
                ECHO + "High-value component acquired. Secure it before celebrating.",
                ECHO + "Interesting find. This technology predates Gridfall; I will begin a deep scan."
        ));

        MESSAGES.put(Context.STABILITY_GLITCH, List.of(
                ECHO + "Sensor buffer corrupt. Radiation interference crossing speech channel.",
                ECHO + "Sky-signal bleed detected. If you hear a second voice, do not answer it.",
                ECHO + "Log data fragmenting. Keep to marked routes until my signal stabilizes."
        ));

        // New entity encounters
        MESSAGES.put(Context.ENTITY_GLOWING_GHOUL, List.of(
                ECHO + "Glowing Ghoul detected. Close proximity will accelerate contamination.",
                ECHO + "That body is a walking reactor. Keep distance; death pulse likely."
        ));

        MESSAGES.put(Context.ENTITY_CITY_STALKER, List.of(
                ECHO + "City Stalker identified. It may already have your position. Watch your flanks.",
                ECHO + "Threat detected: City Stalker. Fast, silent, and fond of blind corners."
        ));

        MESSAGES.put(Context.ENTITY_TOXIC_SLIME, List.of(
                ECHO + "Toxic Slime identified. It leaves corrosive residue. Avoid standing in its trail.",
                ECHO + "Biological hazard: Toxic Slime. Contact poison likely. Use range if available."
        ));

        MESSAGES.put(Context.ENTITY_RUST_WALKER, List.of(
                ECHO + "Rust Walker detected. Slow, armored, and built from things that already failed once.",
                ECHO + "Rust Walker closing. Dense alloy plating. Do not fight it in a narrow space."
        ));

        MESSAGES.put(Context.ENTITY_ASH_WRAITH, List.of(
                ECHO + "Ash Wraith incoming. Contact disrupts vision and muscle response.",
                ECHO + "Threat detected: Ash Wraith. Keep distance; it punishes close-range panic."
        ));

        MESSAGES.put(Context.ENTITY_STEAM_WRAITH, List.of(
                ECHO + "Steam Wraith detected near thermal vents. Contact burns and disorients. Maintain distance.",
                ECHO + "Superheated entity closing. Find cooler ground and do not let it touch you."
        ));

        MESSAGES.put(Context.ENTITY_MUTATED_CRAWLER, List.of(
                ECHO + "Mutated Crawler spotted. Fast, low, and prone to leaping. Track the floor line.",
                ECHO + "Threat detected: Mutated Crawler. Low health, high speed. Stand your ground."
        ));

        // New contexts for v1.3+ content
        MESSAGES.put(Context.BIOME_CRYOGENIC, List.of(
                ECHO + "Cryogenic zone entered. Thermal insulation mandatory.",
                ECHO + "Climate weapon test site confirmed. Seek heat sources before movement slows.",
                ECHO + "Frozen wasteland detected. Preserved technology likely; preserved threats also likely.",
                ECHO + "Sub-zero readings confirmed. Exposed circuitry and tissue are both at risk.",
                ECHO + "Cryogenic field active. Pre-Gridfall climate experiment still running without consent."
        ));

        MESSAGES.put(Context.BIOME_CRASH_ZONE, List.of(
                ECHO + "Crash zone confirmed. Impact glass, ash, and reactor debris exceed safe margins.",
                ECHO + "Scorched terrain ahead. Gridfall did not end here; it left pieces still burning.",
                ECHO + "Crash debris signatures detected. Salvage value high, radiation variance unstable."
        ));

        MESSAGES.put(Context.BIOME_INDUSTRIAL_RUINS, List.of(
                ECHO + "Industrial ruins detected. Expect dense alloy salvage, machine hazards, and old automation still trying to work.",
                ECHO + "Factory district entered. The machines outlived their operators. Treat powered systems as hostile until proven otherwise.",
                ECHO + "Pre-Fall fabrication zone confirmed. The old world learned to build faster than it could think."
        ));

        MESSAGES.put(Context.BIOME_RADIATION_ZONE, List.of(
                ECHO + "Radiation zone entered. RadAway, scrubber routes, and exit timing are now mission-critical.",
                ECHO + "Fallout crust underfoot. Power Node components and dense alloys are likely, but so are high-dose lifeforms.",
                ECHO + "Geiger readings have exceeded advisory limits. The Radwarden patrols used places like this as proving grounds."
        ));

        MESSAGES.put(Context.BIOME_NEXUS_SCAR, List.of(
                ECHO_NEXUS + "Nexus Scar detected. The Core influence is active here, not historical.",
                ECHO_NEXUS + "Anomalous soil confirmed. Gridfall left a wound, and the wound is still transmitting.",
                ECHO_NEXUS + "Reality stability degraded. If another voice enters the signal, do not answer it."
        ));

        MESSAGES.put(Context.RADIO_STATION_ACTIVATED, List.of(
                ECHO + "Relay station online. Fast route network expanded; scout support can push farther.",
                ECHO + "Signal station repaired. Travel vector stable enough to trust once.",
                ECHO + "Communications array active. Network coverage improved. Mark the exit before using it."
        ));

        MESSAGES.put(Context.RESEARCH_UNLOCKED, List.of(
                ECHO + "Schematic decoded. New technology available through Research.",
                ECHO + "Knowledge fragment integrated. Research tier advanced; choose the route you can support.",
                ECHO + "Recovered technology understood. Blueprint added to your archive."
        ));

        MESSAGES.put(Context.FACTION_REPUTATION_POSITIVE, List.of(
                ECHO + "Faction standing improved. New services may be available.",
                ECHO + "They are beginning to trust you. Treat that as supply, not sentiment.",
                ECHO + "Your actions were noted. This faction now reads you as useful."
        ));

        MESSAGES.put(Context.FACTION_REPUTATION_NEGATIVE, List.of(
                ECHO + "Faction standing declining. Expect colder prices and warmer weapons.",
                ECHO + "You are making enemies. Their territory is no longer neutral ground.",
                ECHO + "Faction now considers you a threat. Plan routes accordingly."
        ));

        // Phase transition and progression hints
        MESSAGES.put(Context.PHASE_TRANSITION_BIO_TO_GEO, List.of(
                ECHO + "Surface resources are no longer enough. Dense Alloy from Military Vaults opens the next machine tier.",
                ECHO + "Biological adaptation stabilized. Geological extraction waits below hardened doors.",
                ECHO + "The Substrate Grinder requires Dense Alloy components. Military Vaults are the cleanest lead."
        ));

        MESSAGES.put(Context.DENSE_ALLOY_NEEDED, List.of(
                ECHO + "Dense Alloy required for Tier 4 machinery. Military Vaults are your best source.",
                ECHO + "High-grade alloys detected in Military Vault profiles. Pack armor before chasing them.",
                ECHO + "Without Dense Alloy, the Substrate Grinder route stalls. Signal Scanner can locate vault profiles."
        ));
    }

    private static final Random RANDOM = new Random();

    /**
     * Message priority levels for cooldown management.
     * CRITICAL: Always sent immediately (no cooldown)
     * HIGH: 5 second cooldown
     * NORMAL: 30 second cooldown
     * LOW: 60 second cooldown
     */
    public enum Priority {
        CRITICAL(0),
        HIGH(100),
        NORMAL(600),
        LOW(1200);

        private final int cooldownTicks;

        Priority(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }

        public int getCooldownTicks() {
            return cooldownTicks;
        }
    }

    public static String getMessage(Context context) {
        List<String> msgs = MESSAGES.getOrDefault(context, MESSAGES.get(Context.IDLE));
        return msgs.get(RANDOM.nextInt(msgs.size()));
    }

    public static List<String> getIntroSequence() {
        return MESSAGES.get(Context.FIRST_JOIN);
    }
    
    /**
     * Get the priority level for a given context.
     * Used to determine message cooldown behavior.
     */
    public static Priority getPriority(Context context) {
        return switch (context) {
            case NO_FILTER, LOW_HEALTH -> Priority.CRITICAL; // Immediate danger
            case HIGH_RADIATION, LOW_FILTER -> Priority.HIGH; // Urgent but not immediate
            case LOW_HYDRATION, STORM -> Priority.NORMAL;
            case NIGHT, IDLE -> Priority.LOW; // Contextual, can wait
            case BIOME_WASTELAND, BIOME_TOXIC_SWAMP, BIOME_RUINED_CITY, BIOME_CRYOGENIC,
                 BIOME_CRASH_ZONE, BIOME_INDUSTRIAL_RUINS, BIOME_RADIATION_ZONE, BIOME_NEXUS_SCAR -> Priority.NORMAL;
            case ENTITY_DRONE, ENTITY_SCAVENGER, ENTITY_MUTANT -> Priority.HIGH;
            case ENTITY_GLOWING_GHOUL, ENTITY_CITY_STALKER, ENTITY_TOXIC_SLIME,
                 ENTITY_RUST_WALKER, ENTITY_ASH_WRAITH, ENTITY_STEAM_WRAITH, ENTITY_MUTATED_CRAWLER -> Priority.HIGH;
            case DISCOVERY_RARE_TECH, RESEARCH_UNLOCKED -> Priority.NORMAL;
            case STABILITY_GLITCH -> Priority.HIGH;
            case RADIO_STATION_ACTIVATED -> Priority.NORMAL;
            case FACTION_REPUTATION_POSITIVE, FACTION_REPUTATION_NEGATIVE -> Priority.HIGH;
            case PHASE_TRANSITION_BIO_TO_GEO, DENSE_ALLOY_NEEDED -> Priority.NORMAL;
            default -> Priority.NORMAL;
        };
    }
}

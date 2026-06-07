package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class NexusRelayProfiles {
    private static final Map<NexusRelayType, NexusRelayProfile> BY_TYPE = new EnumMap<>(NexusRelayType.class);

    static {
        register(new NexusRelayProfile(
                NexusRelayType.REACTOR, StructureType.REACTOR_RUIN,
                "Reactor Relay", "Radiation routing",
                "Carry RadAway, elite filters, and spare Power Node components.",
                "Clear four Gridbound Husks and defeat the Relay Warden.",
                "Instability reducers, energy cells, radiation support",
                "uranium traces, power conduits, reactor salvage",
                4, ModEntities.GRIDBOUND_HUSK::get, 4, null, 0, ModEntities.RELAY_WARDEN::get));
        register(new NexusRelayProfile(
                NexusRelayType.CRYO, StructureType.CRYOGENIC_RUINS,
                "Cryo Relay", "Cold timing",
                "Bring heat support, food buffer, and a sealed route out.",
                "Survive the cold timing rooms and clear four Signal Leeches.",
                "scanner tuning, thermal supplies, clean route data",
                "cryo glass, thermal coils, sealed coolant",
                4, ModEntities.SIGNAL_LEECH::get, 4, null, 0, null));
        register(new NexusRelayProfile(
                NexusRelayType.BIO, StructureType.BIO_LAB,
                "Bio Relay", "Mutation pressure",
                "Carry purifier supplies, mutagen countermeasures, and medicine.",
                "Purge corrupted growth rooms and clear six Gridbound Husks.",
                "purification records, filters, bio-lab salvage",
                "mutated tissue, sterile kits, lab caches",
                6, ModEntities.GRIDBOUND_HUSK::get, 6, null, 0, null));
        register(new NexusRelayProfile(
                NexusRelayType.TRANSIT, StructureType.SUBWAY_STATION,
                "Transit Relay", "Ambush lanes",
                "Mark exits, bring sustained damage, and expect navigation loops.",
                "Break the navigation loop by clearing five pressure mobs.",
                "route return data, scanner lens tuning, travel cache",
                "rails, route keys, transit map data",
                5, ModEntities.GRIDBOUND_HUSK::get, 3, ModEntities.SIGNAL_LEECH::get, 2, null));
        register(new NexusRelayProfile(
                NexusRelayType.INDUSTRIAL, StructureType.DERELICT_WORKSHOP,
                "Industrial Relay", "Toxic repair",
                "Bring filters, repair parts, and heavy armor.",
                "Repair the command shell by clearing three Gridbound Husks and a Relay Warden.",
                "machine upgrades, elite filtration, high-tier cells",
                "alloy scrap, toxic barrels, machine casings",
                3, ModEntities.GRIDBOUND_HUSK::get, 3, null, 0, ModEntities.RELAY_WARDEN::get));
        register(new NexusRelayProfile(
                NexusRelayType.SCAR, StructureType.REACTOR_RUIN,
                "Scar Relay", "Nexus anomaly",
                "Expect scanner interference, null fields, and late Nexus pressure.",
                "Contain the anomaly by clearing four Gridbound Husks and a Nexus Nullifier.",
                "return beacon data, Nexus crystals, command records",
                "crystalline residue, scar glass, anomaly cache",
                4, ModEntities.GRIDBOUND_HUSK::get, 4, null, 0, ModEntities.NEXUS_NULLIFIER::get));
    }

    private NexusRelayProfiles() {
    }

    public static Collection<NexusRelayProfile> all() {
        return BY_TYPE.values();
    }

    public static Optional<NexusRelayProfile> byType(NexusRelayType type) {
        return Optional.ofNullable(BY_TYPE.get(type));
    }

    public static boolean hasCoverage() {
        for (NexusRelayType type : NexusRelayType.values()) {
            if (!BY_TYPE.containsKey(type)) {
                return false;
            }
        }
        return BY_TYPE.size() == NexusRelayType.values().length;
    }

    private static void register(NexusRelayProfile profile) {
        BY_TYPE.put(profile.type(), profile);
    }
}

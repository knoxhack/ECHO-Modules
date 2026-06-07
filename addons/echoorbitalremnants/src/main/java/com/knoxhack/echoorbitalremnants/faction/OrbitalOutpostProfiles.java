package com.knoxhack.echoorbitalremnants.faction;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class OrbitalOutpostProfiles {
    private OrbitalOutpostProfiles() {
    }

    public static FactionPledgeItem.Faction factionForDimension(ResourceKey<Level> dimension) {
        if (dimension == ModDimensions.SATURN_RING_GRAVEYARD) {
            return FactionPledgeItem.Faction.VOID_SALVAGERS;
        }
        if (dimension == ModDimensions.TITAN_METHANE_SHELF) {
            return FactionPledgeItem.Faction.ORBITAL_REMNANT;
        }
        if (dimension == ModDimensions.NEXUS_ANOMALY_BELT) {
            return FactionPledgeItem.Faction.NEXUS_CHOIR;
        }
        return null;
    }

    public static ResourceKey<Level> requiredDimension(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> ModDimensions.SATURN_RING_GRAVEYARD;
            case ORBITAL_REMNANT -> ModDimensions.TITAN_METHANE_SHELF;
            case NEXUS_CHOIR -> ModDimensions.NEXUS_ANOMALY_BELT;
        };
    }

    public static String factionId(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "crashbreak";
            case ORBITAL_REMNANT -> "radwarden";
            case NEXUS_CHOIR -> "sporebound";
        };
    }

    public static FactionPledgeItem.Faction factionFromId(String value) {
        String id = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "crashbreak", "void_salvagers", "void_salvager_manifest", "crashbreak_saturn_salvage_charter" ->
                    FactionPledgeItem.Faction.VOID_SALVAGERS;
            case "radwarden", "orbital_remnant", "orbital_remnant_relay", "radwarden_titan_containment_charter" ->
                    FactionPledgeItem.Faction.ORBITAL_REMNANT;
            case "sporebound", "nexus_choir", "nexus_choir_anchor", "sporebound_nexus_anchor_charter" ->
                    FactionPledgeItem.Faction.NEXUS_CHOIR;
            default -> {
                try {
                    yield FactionPledgeItem.Faction.valueOf(id.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }

    public static Identifier echoCoreFactionId(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> Identifier.fromNamespaceAndPath("echoashfallprotocol", "crashbreak_salvage");
            case ORBITAL_REMNANT -> Identifier.fromNamespaceAndPath("echoashfallprotocol", "radwarden_compact");
            case NEXUS_CHOIR -> Identifier.fromNamespaceAndPath("echoashfallprotocol", "sporebound_sanctum");
        };
    }

    public static String roleId(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "salvage_broker";
            case ORBITAL_REMNANT -> "containment_marshal";
            case NEXUS_CHOIR -> "anchor_guide";
        };
    }

    public static String roleName(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Salvage Broker";
            case ORBITAL_REMNANT -> "Containment Marshal";
            case NEXUS_CHOIR -> "Anchor Guide";
        };
    }

    public static String shortName(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Crashbreak";
            case ORBITAL_REMNANT -> "Radwarden";
            case NEXUS_CHOIR -> "Sporebound";
        };
    }

    public static String contractId(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "crashbreak_saturn_salvage_charter";
            case ORBITAL_REMNANT -> "radwarden_titan_containment_charter";
            case NEXUS_CHOIR -> "sporebound_nexus_anchor_charter";
        };
    }

    public static String contractTitle(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Crashbreak Saturn Salvage Charter";
            case ORBITAL_REMNANT -> "Radwarden Titan Containment Charter";
            case NEXUS_CHOIR -> "Sporebound Nexus Anchor Charter";
        };
    }

    public static String objective(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Scan a Saturn Ring Relay at the outpost or deliver 1 Saturn Ring Fragment and 1 Vacuum Circuit.";
            case ORBITAL_REMNANT -> "Scan a Titan Methane Pump at the outpost or deliver 1 Titan Methane Cell and 1 Suit Sealant Patch.";
            case NEXUS_CHOIR -> "After ECHO-0, scan Nexus Anchor/Growth at the outpost or deliver 1 Nexus Stabilizer Shard.";
        };
    }

    public static String reward(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Navigation chip, orbital alloy, and hull-repair support.";
            case ORBITAL_REMNANT -> "Oxygen cells, sealant, and pressure-support gear.";
            case NEXUS_CHOIR -> "Anomaly supplies, cryo support, and stabilization material.";
        };
    }

    public static String route(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Saturn Ring Graveyard";
            case ORBITAL_REMNANT -> "Titan Methane Shelf";
            case NEXUS_CHOIR -> "Nexus Anomaly Belt";
        };
    }

    public static String greeting(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Crashbreak has a ring-yard rule: salvage proves itself when it gets you home.";
            case ORBITAL_REMNANT -> "Radwarden containment recognizes your route signal. Keep your seals checked and your proof clean.";
            case NEXUS_CHOIR -> "Sporebound hears the anchor strain. The route can be interpreted, but only after ECHO-0 stops shouting over it.";
        };
    }

    public static String localContext(FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case VOID_SALVAGERS -> "Saturn relays drift through wreckage here. Ring fragments, circuits, and hull alloy barter best.";
            case ORBITAL_REMNANT -> "Titan pressure work is unforgiving. Methane cells, sealant, and oxygen support keep the shelf survivable.";
            case NEXUS_CHOIR -> "Nexus proof is unstable. Anchor growth, stabilizer shards, and cryo support matter more than pledges.";
        };
    }

    public static String outpostEntityKey(FactionPledgeItem.Faction faction) {
        return EchoOrbitalRemnants.MODID + ":" + factionId(faction) + "_outpost";
    }
}

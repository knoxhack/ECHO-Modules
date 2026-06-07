package com.knoxhack.echoashfallprotocol.research;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all research perks in the game.
 * Three branches with 3 tiers each.
 */
public class PerkRegistry {

    private static final Map<String, Perk> PERKS = new HashMap<>();

    // === RADWARDEN TECH BRANCH (Blue) ===
    public static final Perk WEAPON_DAMAGE_1 = register(new Perk(
        "radwarden.weapon_damage.1", "Ballistics Training I",
        "+10% weapon damage", Perk.Branch.RADWARDEN_TECH, 1, 50, null,
        (player, tier) -> {
            // Effect applied via damage calculation in weapon handlers
        }
    ));

    public static final Perk WEAPON_DAMAGE_2 = register(new Perk(
        "radwarden.weapon_damage.2", "Ballistics Training II",
        "+20% weapon damage", Perk.Branch.RADWARDEN_TECH, 2, 150,
        new String[]{"radwarden.weapon_damage.1"},
        (player, tier) -> {}
    ));

    public static final Perk WEAPON_DAMAGE_3 = register(new Perk(
        "radwarden.weapon_damage.3", "Ballistics Mastery",
        "+30% weapon damage", Perk.Branch.RADWARDEN_TECH, 3, 300,
        new String[]{"radwarden.weapon_damage.2"},
        (player, tier) -> {}
    ));

    public static final Perk ARMOR_DURABILITY_1 = register(new Perk(
        "radwarden.armor_durability.1", "Field Maintenance I",
        "Armor durability loss reduced by 15%", Perk.Branch.RADWARDEN_TECH, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk ARMOR_DURABILITY_2 = register(new Perk(
        "radwarden.armor_durability.2", "Field Maintenance II",
        "Armor durability loss reduced by 30%", Perk.Branch.RADWARDEN_TECH, 2, 150,
        new String[]{"radwarden.armor_durability.1"},
        (player, tier) -> {}
    ));

    public static final Perk MACHINE_EFFICIENCY_1 = register(new Perk(
        "radwarden.machine_eff.1", "Power Optimization I",
        "Machines work 10% faster", Perk.Branch.RADWARDEN_TECH, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk MACHINE_EFFICIENCY_2 = register(new Perk(
        "radwarden.machine_eff.2", "Power Optimization II",
        "Machines work 20% faster, 10% less power", Perk.Branch.RADWARDEN_TECH, 2, 150,
        new String[]{"radwarden.machine_eff.1"},
        (player, tier) -> {}
    ));

    // === CRASHBREAK SALVAGE BRANCH (Yellow) ===
    public static final Perk BETTER_LOOT_1 = register(new Perk(
        "crashbreak.loot.1", "Scavenger's Eye I",
        "+15% chance for extra loot from containers", Perk.Branch.CRASHBREAK_SALVAGE, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk BETTER_LOOT_2 = register(new Perk(
        "crashbreak.loot.2", "Scavenger's Eye II",
        "+30% chance for extra loot, rare items more common", Perk.Branch.CRASHBREAK_SALVAGE, 2, 150,
        new String[]{"crashbreak.loot.1"},
        (player, tier) -> {}
    ));

    public static final Perk CHEAPER_TRADES_1 = register(new Perk(
        "crashbreak.trade.1", "Negotiation I",
        "10% better prices with traders", Perk.Branch.CRASHBREAK_SALVAGE, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk CHEAPER_TRADES_2 = register(new Perk(
        "crashbreak.trade.2", "Negotiation II",
        "20% better prices, faction discounts stack", Perk.Branch.CRASHBREAK_SALVAGE, 2, 150,
        new String[]{"crashbreak.trade.1"},
        (player, tier) -> {}
    ));

    public static final Perk FASTER_SCAVENGING_1 = register(new Perk(
        "crashbreak.speed.1", "Quick Hands I",
        "20% faster looting/container opening", Perk.Branch.CRASHBREAK_SALVAGE, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk FASTER_SCAVENGING_2 = register(new Perk(
        "crashbreak.speed.2", "Quick Hands II",
        "40% faster looting, instant scrap collection", Perk.Branch.CRASHBREAK_SALVAGE, 2, 150,
        new String[]{"crashbreak.speed.1"},
        (player, tier) -> {}
    ));

    // === SPOREBOUND BIO BRANCH (Green) ===
    public static final Perk RADIATION_RESIST_1 = register(new Perk(
        "sporebound.rad_resist.1", "Bio-Adaptation I",
        "+20% radiation resistance", Perk.Branch.SPOREBOUND_BIO, 1, 50, null,
        (player, tier) -> {
            // Effect applied via radiation system
        }
    ));

    public static final Perk RADIATION_RESIST_2 = register(new Perk(
        "sporebound.rad_resist.2", "Bio-Adaptation II",
        "+40% radiation resistance", Perk.Branch.SPOREBOUND_BIO, 2, 150,
        new String[]{"sporebound.rad_resist.1"},
        (player, tier) -> {}
    ));

    public static final Perk RADIATION_RESIST_3 = register(new Perk(
        "sporebound.rad_resist.3", "Rad-Immunity",
        "+60% radiation resistance, immunity to low-rad zones", Perk.Branch.SPOREBOUND_BIO, 3, 300,
        new String[]{"sporebound.rad_resist.2"},
        (player, tier) -> {}
    ));

    public static final Perk HEALTH_REGEN_1 = register(new Perk(
        "sporebound.regen.1", "Rapid Recovery I",
        "Slow health regeneration when not in combat", Perk.Branch.SPOREBOUND_BIO, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk HEALTH_REGEN_2 = register(new Perk(
        "sporebound.regen.2", "Rapid Recovery II",
        "Faster regeneration, works in combat at reduced rate", Perk.Branch.SPOREBOUND_BIO, 2, 150,
        new String[]{"sporebound.regen.1"},
        (player, tier) -> {}
    ));

    public static final Perk MUTATION_SYNERGY_1 = register(new Perk(
        "sporebound.synergy.1", "Mutation Control",
        "Mutation effects last 50% longer, negatives reduced", Perk.Branch.SPOREBOUND_BIO, 1, 50, null,
        (player, tier) -> {}
    ));

    public static final Perk MUTATION_SYNERGY_2 = register(new Perk(
        "sporebound.synergy.2", "Genetic Mastery",
        "Mutations have positive-only variants, longer duration", Perk.Branch.SPOREBOUND_BIO, 2, 150,
        new String[]{"sporebound.synergy.1"},
        (player, tier) -> {}
    ));

    private static Perk register(Perk perk) {
        PERKS.put(perk.getId(), perk);
        return perk;
    }

    public static Perk get(String id) {
        return PERKS.get(id);
    }

    public static Map<String, Perk> getAll() {
        return new HashMap<>(PERKS);
    }

    public static Map<String, Perk> getByBranch(Perk.Branch branch) {
        Map<String, Perk> result = new HashMap<>();
        for (Perk perk : PERKS.values()) {
            if (perk.getBranch() == branch) {
                result.put(perk.getId(), perk);
            }
        }
        return result;
    }

    public static void init() {
        // Static initialization happens above
    }
}

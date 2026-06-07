package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.FactionStanding;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Orbital Remnants standing mirror for Echo Core. Orbital keeps its save and item lanes, but player-facing
 * reputation now resolves into the three Ashfall factions.
 */
public final class OrbitalFactions {
    public static final Identifier ORBITAL_REMNANTS = ashfall("radwarden_compact");
    public static final Identifier VOID_SALVAGERS = ashfall("crashbreak_salvage");
    public static final Identifier NEXUS_CHOIR = ashfall("sporebound_sanctum");

    private OrbitalFactions() {
    }

    public static void register() {
        // Ashfall owns the active Echo Core faction atlas.
    }

    public static void sync(Player player, EchoTerminalProgress progress) {
        if (player == null || progress == null) {
            return;
        }
        mirror(player, ORBITAL_REMNANTS, progress.orbitalRemnantStanding());
        mirror(player, VOID_SALVAGERS, progress.voidSalvagerStanding());
        mirror(player, NEXUS_CHOIR, progress.nexusChoirStanding());
    }

    private static void mirror(Player player, Identifier factionId, FactionStanding standing) {
        switch (standing == null ? FactionStanding.UNKNOWN : standing) {
            case HOSTILE -> EchoCoreServices.setFactionReputation(player, factionId, -75);
            case CONTACTED -> {
                EchoCoreServices.markFactionContacted(player, factionId);
                EchoCoreServices.setFactionReputation(player, factionId, 10);
            }
            case TRUSTED -> EchoCoreServices.setFactionReputation(player, factionId, 55);
            case ALIGNED -> EchoCoreServices.setFactionReputation(player, factionId, 100);
            case UNKNOWN -> {
                if (EchoCoreServices.factionProfile(player, factionId).isEmpty()) {
                    EchoCoreServices.setFactionReputation(player, factionId, 0);
                }
            }
        }
    }

    private static Identifier ashfall(String path) {
        return Identifier.fromNamespaceAndPath("echoashfallprotocol", path);
    }
}

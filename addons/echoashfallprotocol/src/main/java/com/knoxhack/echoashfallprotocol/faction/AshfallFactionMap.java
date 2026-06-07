package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

/**
 * Identifier-only faction lookup for Ashfall runtime systems.
 */
public final class AshfallFactionMap {
    private static final List<Identifier> ALL = List.of(
            AshfallBiomeFactions.RADWARDEN_COMPACT,
            AshfallBiomeFactions.CRASHBREAK_SALVAGE,
            AshfallBiomeFactions.SPOREBOUND_SANCTUM);

    private AshfallFactionMap() {
    }

    public static List<Identifier> all() {
        return ALL;
    }

    public static boolean isAshfall(Identifier factionId) {
        return canonicalFaction(factionId) != null;
    }

    public static Identifier resolveFactionId(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return AshfallBiomeFactions.RADWARDEN_COMPACT;
        }

        if (normalized.contains(":")) {
            try {
                Identifier parsed = Identifier.parse(normalized);
                return canonicalOrDefault(parsed);
            } catch (RuntimeException ignored) {
                return AshfallBiomeFactions.RADWARDEN_COMPACT;
            }
        }

        return canonicalOrDefault(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, normalized));
    }

    public static Identifier canonicalOrDefault(Identifier factionId) {
        Identifier canonical = canonicalFaction(factionId);
        return canonical == null ? AshfallBiomeFactions.RADWARDEN_COMPACT : canonical;
    }

    public static Identifier canonicalFaction(Identifier factionId) {
        if (factionId == null) {
            return null;
        }
        if (ALL.contains(factionId)) {
            return factionId;
        }
        return switch (factionId.toString()) {
            case "echoashfallprotocol:survivor_network",
                    "echoashfallprotocol:ashland_rangers",
                    "echoashfallprotocol:thawbound_collective",
                    "echoashfallprotocol:remnant_collective",
                    "echoorbitalremnants:orbital_remnants",
                    "echoarmory:remnant_collective" -> AshfallBiomeFactions.RADWARDEN_COMPACT;
            case "echoashfallprotocol:dustline_freeholds",
                    "echoashfallprotocol:metro_archivists",
                    "echoashfallprotocol:rustworks_union",
                    "echoashfallprotocol:salvager_guild",
                    "echocore:survivors",
                    "echoorbitalremnants:void_salvagers",
                    "echoarmory:salvager_guild",
                    "echoarmory:construct_foundry" -> AshfallBiomeFactions.CRASHBREAK_SALVAGE;
            case "echoashfallprotocol:scarbound_conclave",
                    "echoashfallprotocol:mutant_front",
                    "echoorbitalremnants:nexus_choir" -> AshfallBiomeFactions.SPOREBOUND_SANCTUM;
            default -> null;
        };
    }

    public static Identifier forPoi(String poiId) {
        String value = normalize(poiId);
        if (value.contains("nexus") || value.contains("scar") || value.contains("anomaly")) {
            return AshfallBiomeFactions.SPOREBOUND_SANCTUM;
        }
        if (value.contains("radiation") || value.contains("reactor") || value.contains("military")
                || value.contains("containment") || value.contains("warning")
                || value.contains("cryo") || value.contains("frozen") || value.contains("thermal")
                || value.contains("ash") || value.contains("wasteland") || value.contains("checkpoint")
                || value.contains("survivor") || value.contains("shelter")) {
            return AshfallBiomeFactions.RADWARDEN_COMPACT;
        }
        if (value.contains("crash") || value.contains("drop_pod") || value.contains("wreck")
                || value.contains("airframe")
                || value.contains("industrial") || value.contains("factory") || value.contains("train")
                || value.contains("refinery") || value.contains("pipe") || value.contains("gantry")
                || value.contains("city") || value.contains("metro") || value.contains("subway")
                || value.contains("archive") || value.contains("data_center")
                || value.contains("plains") || value.contains("freehold") || value.contains("watchtower")
                || value.contains("roadside") || value.contains("camp")) {
            return AshfallBiomeFactions.CRASHBREAK_SALVAGE;
        }
        if (value.contains("toxic") || value.contains("spore") || value.contains("mutant")
                || value.contains("bio") || value.contains("grove")) {
            return AshfallBiomeFactions.SPOREBOUND_SANCTUM;
        }
        return AshfallBiomeFactions.RADWARDEN_COMPACT;
    }

    public static Identifier forEntity(String entityId) {
        String value = normalize(entityId);
        if (value.contains("nexus") || value.contains("scar")) {
            return AshfallBiomeFactions.SPOREBOUND_SANCTUM;
        }
        if (value.contains("radiation") || value.contains("rad_zombie") || value.contains("behemoth")
                || value.contains("cryo") || value.contains("frozen") || value.contains("overseer")
                || value.contains("wasteland")) {
            return AshfallBiomeFactions.RADWARDEN_COMPACT;
        }
        if (value.contains("crash") || value.contains("scavenger") || value.contains("bandit")
                || value.contains("rust") || value.contains("steam") || value.contains("industrial")
                || value.contains("city") || value.contains("metro")
                || value.contains("plains") || value.contains("warlord") || value.contains("wild_dog")) {
            return AshfallBiomeFactions.CRASHBREAK_SALVAGE;
        }
        if (value.contains("toxic") || value.contains("spore") || value.contains("feral")
                || value.contains("crawler") || value.contains("ghoul")) {
            return AshfallBiomeFactions.SPOREBOUND_SANCTUM;
        }
        return AshfallBiomeFactions.RADWARDEN_COMPACT;
    }

    public static String displayName(Identifier factionId) {
        Identifier canonical = canonicalOrDefault(factionId);
        return EchoCoreServices.factionDefinition(canonical)
                .map(EchoFactionDefinition::displayName)
                .orElseGet(() -> readable(canonical));
    }

    public static String shortName(Identifier factionId) {
        Identifier canonical = canonicalOrDefault(factionId);
        return EchoCoreServices.factionDefinition(canonical)
                .map(EchoFactionDefinition::shortName)
                .orElseGet(() -> readable(canonical));
    }

    public static String readable(Identifier factionId) {
        if (factionId == null) {
            return "Ashfall";
        }
        String path = factionId.getPath();
        String[] words = path.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.isEmpty() ? factionId.toString() : builder.toString();
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}

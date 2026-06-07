package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echocore.api.EchoDiscoveryCategory;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoDiscoveryProvider;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventProfile;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventProfiles;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class AshfallDiscoveryProvider implements EchoDiscoveryProvider {
    private static final Identifier CHAPTER =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_protocol");
    private static final Identifier ICON_STRUCTURE = terminalIcon("page_route_map");
    private static final Identifier ICON_BIOME = terminalIcon("state_active");
    private static final Identifier ICON_GUARDIAN = terminalIcon("mission_combat");
    private static final Identifier ICON_EVENT = terminalIcon("mission_hazard");
    private static final Identifier HERO_STRUCTURE = terminalCard("panel_route_map");
    private static final Identifier HERO_FIELD = terminalCard("signal_detail_header");
    private static final List<String> BIOMES = List.of(
            "the_wasteland",
            "crash_zone_wasteland",
            "ruined_plains",
            "ruined_cityscape",
            "industrial_ruins",
            "toxic_swamp",
            "radiation_zone",
            "cryogenic_ruins",
            "nexus_scar");

    @Override
    public List<EchoDiscoveryEntry> entries(Player player) {
        List<EchoDiscoveryEntry> entries = new ArrayList<>();
        int sort = 100;
        for (ExplorationSiteRegistry.SiteProfile profile : ExplorationSiteRegistry.allSorted()) {
            entries.add(new EchoDiscoveryEntry(
                    structureId(profile.id()),
                    CHAPTER,
                    EchoDiscoveryCategory.STRUCTURE,
                    profile.displayName(),
                    "Unmapped " + vagueKind(profile.kind()),
                    profile.prepHint(),
                    profile.description(),
                    ICON_STRUCTURE,
                    HERO_STRUCTURE,
                    accentFor(profile.dangerLevel().getDisplayName()),
                    null,
                    sort++));
        }

        sort = 2_000;
        for (String biome : BIOMES) {
            entries.add(new EchoDiscoveryEntry(
                    biomeId(biome),
                    CHAPTER,
                    EchoDiscoveryCategory.BIOME,
                    readable(biome),
                    "Unknown Wasteland Region",
                    "A hostile biome signature is present. Enter the region to catalog its field profile.",
                    "Ashfall biome telemetry has been confirmed from direct field traversal.",
                    ICON_BIOME,
                    HERO_FIELD,
                    0xFF66E8FF,
                    null,
                    sort++));
        }

        sort = 3_000;
        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            entries.add(new EchoDiscoveryEntry(
                    guardianId(profile.bossPath()),
                    CHAPTER,
                    EchoDiscoveryCategory.GUARDIAN,
                    profile.title(),
                    "Buried Guardian Signature",
                    "A hostile guardian route is sealed beneath a matching biome. Follow missions or scanner leads to reveal it.",
                    profile.lore() + " Faction thread: " + AshfallFactionMap.displayName(profile.ownerFaction())
                            + ". Counterplay: " + profile.mechanicHint(),
                    ICON_GUARDIAN,
                    HERO_FIELD,
                    profile.color(),
                    Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, profile.missionId()),
                    sort++));
        }

        sort = 4_000;
        for (EnvironmentalEventProfile profile : EnvironmentalEventProfiles.activeProfiles()) {
            entries.add(new EchoDiscoveryEntry(
                    eventId(profile.commandAlias()),
                    CHAPTER,
                    EchoDiscoveryCategory.EVENT,
                    profile.type().getDisplayName(),
                    "Unstable Weather Pattern",
                    "A custom Ashfall event can occur under the right world pressure.",
                    profile.type().getWarning(),
                    ICON_EVENT,
                    HERO_FIELD,
                    profile.particleColor(),
                    null,
                    sort++));
        }

        return entries;
    }

    @Override
    public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
        if (player == null || entry == null) {
            return EchoDiscoveryState.LOCKED;
        }
        String path = entry.id().getPath();
        QuestData quest = safeQuest(player);
        if (path.startsWith("structure/")) {
            String siteId = path.substring("structure/".length());
            if (quest == null) {
                return EchoDiscoveryState.LOCKED;
            }
            if (quest.hasPOIState(siteId, QuestData.POIObjectiveState.REWARD_CLAIMED)
                    || quest.hasPOIState(siteId, QuestData.POIObjectiveState.CLEARED)
                    || quest.hasPOIState(siteId, QuestData.POIObjectiveState.BOSS_DEFEATED)
                    || quest.hasPOIState(siteId, QuestData.POIObjectiveState.DATA_RECOVERED)
                    || quest.hasPOIState(siteId, QuestData.POIObjectiveState.SAMPLE_RECOVERED)) {
                return EchoDiscoveryState.CHECKED;
            }
            return quest.isPOIDiscovered(siteId) ? EchoDiscoveryState.DISCOVERED : EchoDiscoveryState.LOCKED;
        }
        if (path.startsWith("biome/")) {
            String biome = path.substring("biome/".length());
            return quest != null && visitedBiome(quest, biome) ? EchoDiscoveryState.CHECKED : EchoDiscoveryState.LOCKED;
        }
        if (path.startsWith("guardian/")) {
            String boss = path.substring("guardian/".length());
            if (quest == null) {
                return EchoDiscoveryState.LOCKED;
            }
            String site = "guardian_" + boss;
            if (quest.hasPOIState(site, QuestData.POIObjectiveState.BOSS_DEFEATED)
                    || quest.getEntityKills("echoashfallprotocol:" + boss) > 0) {
                return EchoDiscoveryState.CHECKED;
            }
            return quest.isPOIDiscovered(site) || quest.hasVisitedLocation("guardian", boss)
                    ? EchoDiscoveryState.DISCOVERED
                    : EchoDiscoveryState.LOCKED;
        }
        if (path.startsWith("event/")) {
            String event = path.substring("event/".length());
            return eventChecked(player, event) ? EchoDiscoveryState.CHECKED : EchoDiscoveryState.LOCKED;
        }
        return EchoDiscoveryState.LOCKED;
    }

    public static Identifier structureId(String siteId) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "structure/" + cleanPath(siteId));
    }

    public static Identifier biomeId(String biomePath) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biome/" + cleanPath(biomePath));
    }

    public static Identifier guardianId(String bossPath) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "guardian/" + cleanPath(bossPath));
    }

    public static Identifier eventId(String alias) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "event/" + cleanPath(alias));
    }

    private static QuestData safeQuest(Player player) {
        try {
            return QuestData.get(player);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean visitedBiome(QuestData quest, String biome) {
        return quest.getVisitedBiomes().contains(biome)
                || quest.getVisitedBiomes().contains(EchoAshfallProtocol.MODID + ":" + biome);
    }

    private static boolean eventChecked(Player player, String alias) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return EnvironmentalEventProfiles.byAlias(alias)
                .map(type -> EnvironmentalEventData.get(level.getServer().overworld()).getEventsSurvived(type) > 0)
                .orElse(false);
    }

    private static String vagueKind(ExplorationSiteRegistry.SiteKind kind) {
        return switch (kind) {
            case FACTION_HUB -> "Local Channel";
            case MAIN_SITE -> "Major Site";
            case SURVIVAL_CACHE -> "Survival Cache";
            case HAZARD_SITE -> "Hazard Site";
            case RELAY -> "Relay Signal";
            case RESOURCE_SITE -> "Resource Site";
            case LANDMARK -> "Field Landmark";
        };
    }

    private static int accentFor(String danger) {
        String key = danger == null ? "" : danger.toLowerCase(Locale.ROOT);
        if (key.contains("safe")) {
            return 0xFF92F7A6;
        }
        if (key.contains("high") || key.contains("deadly")) {
            return 0xFFFF8FA3;
        }
        return 0xFFFFD166;
    }

    private static String cleanPath(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_/.-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    private static String readable(String value) {
        String[] parts = cleanPath(value).replace('/', '_').split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Unknown Region" : builder.toString();
    }

    private static Identifier terminalIcon(String name) {
        return Identifier.fromNamespaceAndPath("echoterminal", "textures/gui/icons/terminal/" + name + ".png");
    }

    private static Identifier terminalCard(String name) {
        return Identifier.fromNamespaceAndPath("echoterminal", "textures/gui/terminal/cards/" + name + ".png");
    }
}

package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelaySiteService;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.integration.AshfallDiscoveryProvider;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared structure scan logic for block and portable scanners.
 */
public final class POIScannerService {

    public static final int BASE_SCAN_CHUNK_RADIUS = 31;
    public static final int DISCOVERY_RADIUS = 96;

    private static final TagKey<Structure> POI_STRUCTURES_TAG = TagKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath("echoashfallprotocol", "poi_structures")
    );

    private POIScannerService() {
    }

    @Nullable
    public static ScanHit scan(ServerPlayer player) {
        return scan((ServerLevel) player.level(), player.blockPosition(), player);
    }

    @Nullable
    public static ScanHit scan(ServerLevel level, BlockPos origin, @Nullable ServerPlayer player) {
        ScanHit guardianHit = scanActiveGuardianSite(level, origin, player);
        if (guardianHit != null) {
            return guardianHit;
        }
        ScanHit relayHit = NexusRelaySiteService.scanActiveRelaySite(level, origin, player).orElse(null);
        if (relayHit != null) {
            return relayHit;
        }

        HolderSet.Named<Structure> poiSet;
        try {
            poiSet = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(POI_STRUCTURES_TAG);
        } catch (Exception e) {
            return null;
        }

        Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource()
            .getGenerator()
            .findNearestMapStructure(level, poiSet, origin, getChunkRadius(player), false);

        if (hit == null || hit.getFirst() == null) {
            return null;
        }

        String structureId = hit.getSecond().unwrapKey()
                .map(POIScannerService::structurePathFromKey)
                .orElse("unknown");
        ExplorationSiteRegistry.SiteProfile profile = ExplorationSiteRegistry.getByStructureOrFallback(structureId);
        double distance = Math.sqrt(origin.distSqr(hit.getFirst()));
        QuestData quest = player != null ? QuestData.get(player) : null;
        boolean discovered = quest != null && quest.isPOIDiscovered(profile.id());
        String objectiveStatus = quest != null ? quest.getPOIStateSummary(profile.id()) : "Unknown";

        return new ScanHit(
            hit.getFirst(),
            profile.id(),
            structureId,
            profile.displayName(),
            profile.route(),
            profile.description(),
            profile.objective(),
            profile.rewardTrack(),
            profile.dangerLevel().getDisplayName(),
            profile.hazardName(),
            profile.prepHint(),
            profile.resourceProfile(),
            distance,
            getDirection(hit.getFirst(), origin),
            discovered,
            objectiveStatus
        );
    }

    @Nullable
    private static ScanHit scanActiveGuardianSite(ServerLevel level, BlockPos origin, @Nullable ServerPlayer player) {
        if (player == null) {
            return null;
        }
        QuestData quest = QuestData.get(player);
        Mission mission = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (mission == null) {
            return null;
        }
        BiomeGuardianProfile profile = BiomeGuardianProfiles.byMissionId(mission.id()).orElse(null);
        if (profile == null) {
            return null;
        }
        return BiomeGuardianSiteData.get(level).nearestActiveForMission(origin, mission.id())
                .map(site -> {
                    BlockPos entrance = site.entrance();
                    double distance = Math.sqrt(origin.distSqr(entrance));
                    String siteId = "guardian_" + profile.bossPath();
                    boolean discovered = quest.isPOIDiscovered(siteId);
                    return new ScanHit(
                            entrance,
                            siteId,
                            profile.mainStructure().name().toLowerCase(java.util.Locale.ROOT),
                            profile.title() + " Guardian Entrance",
                            "Guardian Entrance",
                            profile.surfaceEntrance() + " descends to a marked route and dedicated boss chamber in "
                                    + profile.undergroundSite()
                                    + ". " + profile.lore() + " Arena mechanic: " + profile.mechanicHint(),
                            "Enter the " + profile.surfaceEntrance() + ", follow the marked route, counter "
                                    + profile.ability().displayName() + ", and neutralize " + profile.title(),
                            "Guardian Dossier",
                            mission.difficulty().name(),
                            profile.arenaHazard().displayName(),
                            profile.prepHint(),
                            "guardian datacore, profile rewards, data log, Nexus route stability",
                            distance,
                            getDirection(entrance, origin),
                            discovered,
                            quest.getPOIStateSummary(siteId)
                    );
                })
                .orElse(null);
    }

    private static String structurePathFromKey(ResourceKey<Structure> key) {
        return key.identifier().getPath();
    }

    public static boolean discover(ServerPlayer player, ScanHit hit) {
        QuestData quest = QuestData.get(player);
        String siteId = ExplorationSiteRegistry.normalize(hit.id());
        boolean alreadyDiscovered = quest.isPOIDiscovered(siteId);

        quest.discoverPOI(siteId);
        quest.recordPOIState(siteId, QuestData.POIObjectiveState.SCANNED);
        if (hit.distance() <= DISCOVERY_RADIUS * 0.5D) {
            quest.recordPOIState(siteId, QuestData.POIObjectiveState.ENTERED);
            quest.visitLocation("poi", siteId);
        }
        if (siteId.startsWith("guardian_")) {
            quest.visitLocation("guardian", siteId.substring("guardian_".length()));
            if (!alreadyDiscovered) {
                quest.addToArchive("[GUARDIAN] " + hit.displayName()
                        + " surface entrance archived. " + hit.intelLine());
            }
        } else if (siteId.startsWith("nexus_relay_")) {
            quest.visitLocation("special", "nexus:relay_site:" + siteId.substring("nexus_relay_".length()));
            if (!alreadyDiscovered) {
                quest.addToArchive("[NEXUS RELAY] " + hit.displayName()
                        + " site archived. Objective: " + hit.objective());
            }
        }

        ExplorationSiteRegistry.SiteProfile profile = ExplorationSiteRegistry.getOrFallback(siteId);
        if (isFactionHub(profile)) {
            quest.discoverPOI("faction_hub");
            quest.visitLocation("poi", "faction_hub");
        }

        if (!alreadyDiscovered && !siteId.startsWith("guardian_") && !siteId.startsWith("nexus_relay_")) {
            quest.addToArchive("[SCAN] " + hit.displayName() + " archived. Hazard: "
                    + hit.hazardProfile() + ". Prep: " + hit.prepHint() + ".");
        }
        QuestData.saveAndSync(player, quest);
        if (siteId.startsWith("guardian_")) {
            EchoCoreServices.discoverFeature(player,
                    AshfallDiscoveryProvider.guardianId(siteId.substring("guardian_".length())));
        } else if (!siteId.startsWith("nexus_relay_")) {
            EchoCoreServices.discoverFeature(player, AshfallDiscoveryProvider.structureId(siteId));
        }
        EchoCoreServices.structureDiscoveryService().recordStructureScan(
                player,
                Identifier.fromNamespaceAndPath("echoashfallprotocol", siteId),
                hit.position(),
                hit.displayName(),
                hit.intelLine());

        if (!alreadyDiscovered && profile.faction() != null) {
            var territory = player.getData(ModAttachments.FACTION_TERRITORY.get());
            territory.addVillage(hit.position(), profile.faction(), profile.displayName());
            territory.modifyBiomeInfluence(profile.faction(), player.level().getBiome(hit.position()).toString(), 20);
            com.knoxhack.echoashfallprotocol.faction.FactionTerritory.saveAndSync(player, territory);
        }
        return !alreadyDiscovered;
    }

    private static boolean isFactionHub(ExplorationSiteRegistry.SiteProfile profile) {
        return profile.kind() == ExplorationSiteRegistry.SiteKind.FACTION_HUB
                || "faction_hub".equals(profile.id());
    }

    public static boolean shouldAutoDiscover(ScanHit hit) {
        return hit.distance() <= DISCOVERY_RADIUS;
    }

    public static int getChunkRadius(@Nullable ServerPlayer player) {
        int range = BASE_SCAN_CHUNK_RADIUS;
        if (player != null && PostNexusData.get(player).isPath(PostNexusData.NexusPath.CONTROL)) {
            range += 12;
        }
        if (player != null && NexusRelaySiteService.hasRelayScannerLens(player)) {
            range += 12;
        }
        return range;
    }

    public static Component createSummary(ScanHit hit) {
        String status = hit.discovered() ? "Catalogued" : "New signal";
        return Component.literal("[ECHO-7] ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(hit.displayName() + " | " + status + " | " + hit.objectiveStatus())
                        .withStyle(ChatFormatting.WHITE));
    }

    public static List<Component> createReadout(ScanHit hit) {
        return List.of(
                createSummary(hit),
                Component.literal("Route: " + hit.route()
                        + " | Distance: " + (int) hit.distance()
                        + " blocks | Direction: " + hit.direction()).withStyle(ChatFormatting.GRAY),
                Component.literal("Risk: " + hit.riskProfile()
                        + " | Hazard: " + hit.hazardProfile()).withStyle(ChatFormatting.GRAY),
                Component.literal("Prep: " + hit.prepHint()).withStyle(ChatFormatting.YELLOW),
                Component.literal("Rewards: " + hit.rewardTrack()
                        + " | Supplies: " + hit.resourceProfile()).withStyle(ChatFormatting.GRAY),
                Component.literal("Objective: " + hit.objective()
                        + " | State: " + hit.objectiveStatus()).withStyle(ChatFormatting.DARK_AQUA),
                Component.literal("Intel: " + hit.intelLine()).withStyle(ChatFormatting.DARK_AQUA)
        );
    }

    private static String getDirection(BlockPos target, BlockPos source) {
        int dx = target.getX() - source.getX();
        int dz = target.getZ() - source.getZ();
        double angle = Math.toDegrees(Math.atan2(-dz, dx));
        if (angle < 0) {
            angle += 360;
        }

        if (angle >= 337.5 || angle < 22.5) return "East";
        if (angle < 67.5) return "Southeast";
        if (angle < 112.5) return "South";
        if (angle < 157.5) return "Southwest";
        if (angle < 202.5) return "West";
        if (angle < 247.5) return "Northwest";
        if (angle < 292.5) return "North";
        return "Northeast";
    }

    public record ScanHit(
        BlockPos position,
        String id,
        String structureId,
        String displayName,
        String route,
        String intelLine,
        String objective,
        String rewardTrack,
        String riskProfile,
        String hazardProfile,
        String prepHint,
        String resourceProfile,
        double distance,
        String direction,
        boolean discovered,
        String objectiveStatus
    ) {
    }
}

package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Ashfall-internal owner for Prime Relay sites, encounters, and finale boss routing.
 */
public final class NexusRelaySiteService {
    private static final int SITE_SCAN_RADIUS_BASE = POIScannerService.BASE_SCAN_CHUNK_RADIUS * 16;
    private static final int SITE_SCAN_RADIUS_LENS_BONUS = 12 * 16;
    private static final double ENCOUNTER_RECORD_RADIUS_SQR = 128.0D * 128.0D;
    private static final double LIVE_BOSS_RADIUS = 256.0D;

    private NexusRelaySiteService() {
    }

    public static void ensureSitesAssignedAndGenerated(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerLevel level = ((ServerLevel) player.level()).getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(level);
        ensureSitesAssignedAndGenerated(level, campaign, player.blockPosition());
    }

    public static void ensureSitesAssignedAndGenerated(ServerLevel level, NexusCampaignData campaign, BlockPos fallbackAnchor) {
        if (level == null || campaign == null) {
            return;
        }
        BlockPos anchor = campaign.getNexusPos();
        if (anchor == null || anchor.equals(BlockPos.ZERO)) {
            anchor = fallbackAnchor == null ? level.getLevelData().getRespawnData().pos() : fallbackAnchor;
        }
        for (NexusRelayProfile profile : NexusRelayProfiles.all()) {
            if (!campaign.hasRelaySite(profile.type())) {
                campaign.assignRelaySite(profile.type(), deterministicSite(level, anchor, profile.type()));
            }
            if (!campaign.isRelayGenerated(profile.type())) {
                generateRelaySite(level, campaign, profile);
            }
        }
    }

    public static boolean startOrRecoverEncounter(ServerPlayer player, NexusRelayType type) {
        if (player == null || type == null) {
            return false;
        }
        ServerLevel level = ((ServerLevel) player.level()).getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(level);
        NexusRelayProfile profile = NexusRelayProfiles.byType(type).orElse(null);
        if (profile == null) {
            return false;
        }
        if (!campaign.isAwakened() || campaign.getScannedRelayCount() < NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Scan the Prime Relay network before spawning relay encounters."));
            return false;
        }
        ensureSitesAssignedAndGenerated(level, campaign, player.blockPosition());
        if (campaign.getRelayState(type).isResolved()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + type.displayName() + " is already resolved."));
            return false;
        }
        if (campaign.isRelayEncounterComplete(type)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + type.displayName() + " encounter objective is already complete."));
            return false;
        }
        BlockPos site = campaign.getRelaySite(type);
        int live = livePressureMobs(level, site, profile);
        if (live <= 0) {
            spawnEncounterMobs(level, player, campaign, profile);
        } else if (campaign.isRelayEncounterStarted(type)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + type.displayName()
                    + " still has " + live + " live pressure signature" + (live == 1 ? "" : "s")
                    + " near the site. Recovery spawn skipped."));
        }
        campaign.markRelayEncounterStarted(type);
        campaign.activateRelay(type);
        AshfallAdapterCoreLateRuntime.relayActivated(
                player,
                siteId(type),
                type.name(),
                campaign.getRelaySite(type),
                "prime_relay_encounter");
        campaign.setDirty();
        QuestData quest = QuestData.get(player);
        quest.discoverPOI(siteId(type));
        quest.recordPOIState(siteId(type), QuestData.POIObjectiveState.ENTERED);
        quest.addToArchive("[NEXUS RELAY] " + type.displayName() + " encounter active at " + formatPos(site)
                + ". Objective: " + profile.objective());
        QuestData.saveAndSync(player, quest);
        NexusCampaignActions.syncCampaignState(level);
        player.sendSystemMessage(Component.literal("[NEXUS] " + type.displayName()
                + " encounter active at " + formatPos(site) + ". " + profile.objective()));
        return true;
    }

    public static boolean startNextEncounter(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ServerLevel level = ((ServerLevel) player.level()).getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (!campaign.isAwakened() || campaign.getScannedRelayCount() < NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Scan the Prime Relay network before spawning relay encounters."));
            return false;
        }
        ensureSitesAssignedAndGenerated(level, campaign, player.blockPosition());
        NexusRelayType type = campaign.firstEncounterPendingRelay();
        if (type == null) {
            player.sendSystemMessage(Component.literal("[ECHO-7] No unresolved Prime Relay encounter is waiting."));
            return false;
        }
        return startOrRecoverEncounter(player, type);
    }

    public static boolean recordPressureMobKill(ServerPlayer player, LivingEntity killed) {
        if (player == null || killed == null || !(killed.level() instanceof ServerLevel killedLevel)) {
            return false;
        }
        if (!NexusPressureMobProfiles.isPressureMob(killed.getType())) {
            return false;
        }
        ServerLevel level = killedLevel.getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(level);
        Optional<NexusRelayProfile> profileOptional = nearestActiveRelay(campaign, killed.blockPosition(), killed.getType());
        if (profileOptional.isEmpty()) {
            return false;
        }
        NexusRelayProfile profile = profileOptional.get();
        NexusRelayType type = profile.type();
        if (profile.isCommander(killed.getType())) {
            campaign.markRelayCommanderDefeated(type);
            AshfallAdapterCoreLateRuntime.bossDefeated(
                    player,
                    "prime_relay_" + type.name().toLowerCase(Locale.ROOT) + "_commander",
                    type.name(),
                    killed.blockPosition(),
                    "prime_relay_commander_defeat");
        } else if (profile.countsPressureKill(killed.getType())
                && campaign.getRelayPressureKills(type) < profile.requiredPressureKills()) {
            campaign.incrementRelayPressureKill(type);
        }
        if (campaign.isRelayObjectiveSatisfied(type, profile)) {
            campaign.markRelayEncounterComplete(type);
            QuestData quest = QuestData.get(player);
            quest.recordPOIState(siteId(type), QuestData.POIObjectiveState.CLEARED);
            quest.visitLocation("special", "nexus:encounter:" + type.name().toLowerCase(Locale.ROOT));
            quest.addToArchive("[NEXUS RELAY] " + type.displayName()
                    + " encounter complete. Outcome command now accepts stabilize, sever, or override.");
            QuestData.saveAndSync(player, quest);
            player.sendSystemMessage(Component.literal("[NEXUS] " + type.displayName()
                    + " objective complete. Resolve it with /nexus relay "
                    + type.name().toLowerCase(Locale.ROOT) + " <stabilize|sever|override>."));
        }
        NexusCampaignActions.syncCampaignState(level);
        return true;
    }

    public static Optional<POIScannerService.ScanHit> scanActiveRelaySite(ServerLevel level, BlockPos origin, ServerPlayer player) {
        if (level == null || origin == null || player == null) {
            return Optional.empty();
        }
        NexusCampaignData campaign = NexusCampaignData.get(level.getServer().overworld());
        if (!campaign.isAwakened() || campaign.getScannedRelayCount() < NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT) {
            return Optional.empty();
        }
        ensureSitesAssignedAndGenerated(level.getServer().overworld(), campaign, player.blockPosition());
        int scanRadius = SITE_SCAN_RADIUS_BASE + (hasRelayScannerLens(player) ? SITE_SCAN_RADIUS_LENS_BONUS : 0);
        QuestData quest = QuestData.get(player);
        return NexusRelayProfiles.all().stream()
                .filter(profile -> campaign.hasRelaySite(profile.type()))
                .filter(profile -> !campaign.getRelayState(profile.type()).isResolved())
                .map(profile -> scanHit(campaign, quest, profile, origin))
                .filter(hit -> hit.distance() <= scanRadius)
                .min(Comparator.comparingDouble(POIScannerService.ScanHit::distance));
    }

    public static boolean spawnFinalBoss(ServerPlayer player, PostNexusData.NexusPath path) {
        if (player == null || path == null || path == PostNexusData.NexusPath.NONE) {
            return false;
        }
        ServerLevel level = ((ServerLevel) player.level()).getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(level);
        NexusFinalBossProfile profile = NexusFinalBossProfiles.byPath(path).orElse(null);
        if (profile == null) {
            return false;
        }
        if (campaign.isFinaleComplete() || PostNexusData.get(player).isFinalBossDefeated()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + profile.title()
                    + " is already defeated for this path."));
            return false;
        }
        if (hasLiveFinalBoss(level, campaign, path)) {
            campaign.markFinalBossSummoned(path);
            NexusCampaignActions.syncCampaignState(level);
            player.sendSystemMessage(Component.literal("[ECHO-7] " + profile.title() + " is already live near the Core."));
            return true;
        }
        BlockPos anchor = campaign.getNexusPos();
        if (anchor == null || anchor.equals(BlockPos.ZERO)) {
            anchor = player.blockPosition();
        }
        BlockPos spawn = surfaceNear(level, anchor.offset(12, 0, 12));
        NexusFinalBossEntity boss = profile.entityType().get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            return false;
        }
        boss.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        campaign.markFinalBossSummoned(path);
        NexusCampaignActions.syncCampaignState(level);
        player.sendSystemMessage(Component.literal("[NEXUS] " + profile.title()
                + " summoned at " + formatPos(spawn) + ". Defeat it to seal the path finale."));
        return true;
    }

    public static boolean hasLiveFinalBoss(ServerLevel level, NexusCampaignData campaign, PostNexusData.NexusPath path) {
        if (level == null || campaign == null || path == null || path == PostNexusData.NexusPath.NONE) {
            return false;
        }
        BlockPos center = campaign.getNexusPos();
        if (center == null || center.equals(BlockPos.ZERO)) {
            center = level.getLevelData().getRespawnData().pos();
        }
        AABB box = new AABB(center).inflate(LIVE_BOSS_RADIUS);
        return !level.getEntitiesOfClass(NexusFinalBossEntity.class, box,
                boss -> boss.isAlive() && boss.path() == path).isEmpty();
    }

    public static String siteId(NexusRelayType type) {
        return "nexus_relay_" + type.name().toLowerCase(Locale.ROOT);
    }

    public static boolean hasRelayScannerLens(Player player) {
        return player != null && player.getInventory().contains(new ItemStack(ModItems.RELAY_SCANNER_LENS.get()));
    }

    public static boolean objectiveShellExists(ServerLevel level, NexusCampaignData campaign, NexusRelayType type) {
        if (level == null || campaign == null || !campaign.hasRelaySite(type)) {
            return false;
        }
        BlockPos site = campaign.getRelaySite(type);
        for (BlockPos pos : BlockPos.betweenClosed(site.offset(-4, -1, -4), site.offset(4, 5, 4))) {
            if (!level.getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static void generateRelaySite(ServerLevel level, NexusCampaignData campaign, NexusRelayProfile profile) {
        BlockPos site = campaign.getRelaySite(profile.type());
        if (site == null || site.equals(BlockPos.ZERO)) {
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ site.asLong() ^ profile.type().ordinal() * 341873128712L);
        ProceduralStructureGenerator.generateStructure(level, site, profile.structureType(), random);
        decorateRelaySite(level, site, profile);
        campaign.markRelayGenerated(profile.type());
    }

    private static void decorateRelaySite(ServerLevel level, BlockPos site, NexusRelayProfile profile) {
        var primary = switch (profile.type()) {
            case REACTOR -> Blocks.YELLOW_STAINED_GLASS;
            case CRYO -> Blocks.LIGHT_BLUE_STAINED_GLASS;
            case BIO -> Blocks.LIME_STAINED_GLASS;
            case TRANSIT -> Blocks.RAIL;
            case INDUSTRIAL -> Blocks.CUT_COPPER;
            case SCAR -> Blocks.PURPLE_STAINED_GLASS;
        };
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) == 4 || Math.abs(dz) == 4 || (dx == 0 && dz == 0)) {
                    level.setBlock(site.offset(dx, 0, dz), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(site.above(), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        level.setBlock(site.above(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
        level.setBlock(site.above(3), primary.defaultBlockState(), 2);
        level.setBlock(site.above(4), Blocks.BEACON.defaultBlockState(), 2);
        level.setBlock(site.offset(2, 1, 0), Blocks.BARREL.defaultBlockState(), 2);
        level.setBlock(site.offset(-2, 1, 0), Blocks.LECTERN.defaultBlockState(), 2);
        level.setBlock(site.offset(0, 1, 2), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
        level.setBlock(site.offset(0, 1, -2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
    }

    private static void spawnEncounterMobs(ServerLevel level, ServerPlayer player, NexusCampaignData campaign, NexusRelayProfile profile) {
        BlockPos site = campaign.getRelaySite(profile.type());
        int remainingPressure = Math.max(0, profile.requiredPressureKills() - campaign.getRelayPressureKills(profile.type()));
        int primary = Math.min(profile.primarySpawnCount(), remainingPressure);
        int secondary = Math.min(profile.secondarySpawnCount(), Math.max(0, remainingPressure - primary));
        spawnMobPack(level, player, site, profile.primaryPressureType(), primary);
        spawnMobPack(level, player, site, profile.secondaryPressureType(), secondary);
        if (profile.commanderType() != null && !campaign.isRelayCommanderDefeated(profile.type())) {
            spawnMobPack(level, player, site, profile.commanderType(), 1);
        }
    }

    private static void spawnMobPack(ServerLevel level, ServerPlayer player, BlockPos site,
            java.util.function.Supplier<EntityType<NexusPressureMobEntity>> typeSupplier, int count) {
        if (typeSupplier == null || count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            NexusPressureMobEntity mob = typeSupplier.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (mob == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / Math.max(1, count)) * i + level.getRandom().nextDouble() * 0.5D;
            double distance = 5.0D + level.getRandom().nextDouble() * 8.0D;
            BlockPos spawn = surfaceNear(level, new BlockPos(
                    site.getX() + (int) Math.round(Math.cos(angle) * distance),
                    site.getY(),
                    site.getZ() + (int) Math.round(Math.sin(angle) * distance)));
            mob.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            mob.setTarget(player);
            level.addFreshEntity(mob);
        }
    }

    private static Optional<NexusRelayProfile> nearestActiveRelay(NexusCampaignData campaign, BlockPos killPos, EntityType<?> killedType) {
        return NexusRelayProfiles.all().stream()
                .filter(profile -> campaign.isRelayEncounterStarted(profile.type()))
                .filter(profile -> !campaign.isRelayEncounterComplete(profile.type()))
                .filter(profile -> profile.countsPressureKill(killedType) || profile.isCommander(killedType))
                .filter(profile -> campaign.hasRelaySite(profile.type()))
                .filter(profile -> campaign.getRelaySite(profile.type()).distSqr(killPos) <= ENCOUNTER_RECORD_RADIUS_SQR)
                .min(Comparator.comparingDouble(profile -> campaign.getRelaySite(profile.type()).distSqr(killPos)));
    }

    private static int livePressureMobs(ServerLevel level, BlockPos site, NexusRelayProfile profile) {
        if (site == null || site.equals(BlockPos.ZERO)) {
            return 0;
        }
        return level.getEntitiesOfClass(NexusPressureMobEntity.class, new AABB(site).inflate(96.0D),
                mob -> mob.isAlive()
                        && profile != null
                        && (profile.countsPressureKill(mob.getType()) || profile.isCommander(mob.getType()))).size();
    }

    private static POIScannerService.ScanHit scanHit(NexusCampaignData campaign, QuestData quest,
            NexusRelayProfile profile, BlockPos origin) {
        BlockPos site = campaign.getRelaySite(profile.type());
        double distance = Math.sqrt(origin.distSqr(site));
        return new POIScannerService.ScanHit(
                site,
                siteId(profile.type()),
                profile.structureType().getName(),
                profile.type().displayName(),
                "Prime Relay",
                profile.routeTitle() + ": " + profile.type().routeIdentity(),
                profile.objective(),
                profile.rewardTrack(),
                campaign.getRelayState(profile.type()).name(),
                profile.hazardName(),
                profile.prepHint(),
                profile.resourceProfile(),
                distance,
                direction(site, origin),
                quest.isPOIDiscovered(siteId(profile.type())),
                campaign.relayEncounterStatus(profile.type())
        );
    }

    private static BlockPos deterministicSite(ServerLevel level, BlockPos anchor, NexusRelayType type) {
        int[][] offsets = {
                {920, 120},
                {-720, -620},
                {120, 880},
                {-960, 80},
                {680, 660},
                {80, -1080}
        };
        int[] offset = offsets[type.ordinal()];
        long mixed = level.getSeed() ^ anchor.asLong() ^ (long) type.ordinal() * 91815541L;
        RandomSource random = RandomSource.create(mixed);
        int x = anchor.getX() + offset[0] + random.nextInt(121) - 60;
        int z = anchor.getZ() + offset[1] + random.nextInt(121) - 60;
        return surfaceNear(level, new BlockPos(x, anchor.getY(), z));
    }

    private static BlockPos surfaceNear(ServerLevel level, BlockPos pos) {
        int y = Math.max(level.getMinY() + 4,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) + 1);
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private static String direction(BlockPos target, BlockPos source) {
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

    private static String formatPos(BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return "unassigned";
        }
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}

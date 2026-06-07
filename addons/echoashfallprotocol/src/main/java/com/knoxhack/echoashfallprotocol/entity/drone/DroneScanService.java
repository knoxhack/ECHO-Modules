package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMarker;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanCategory;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanResult;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.network.DroneMarkersPacket;
import com.knoxhack.echoashfallprotocol.registry.DroneTags;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echonetcore.api.EchoNetSend;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DroneScanService {
    public static final Identifier MAP_PROVIDER_ID =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "companion_drone_scans");
    public static final Identifier MAP_LAYER_ID =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone_scan");

    private static final TagKey<net.minecraft.world.level.block.Block> COMMON_ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final Map<java.util.UUID, List<EchoDroneMarker>> RECENT_MARKERS = new LinkedHashMap<>();
    private static boolean mapProviderRegistered;

    private DroneScanService() {
    }

    public static void registerMapProvider() {
        if (mapProviderRegistered) {
            return;
        }
        mapProviderRegistered = true;
        EchoCoreServices.registerMapDataProvider(new IMapDataProvider() {
            @Override
            public Identifier providerId() {
                return MAP_PROVIDER_ID;
            }

            @Override
            public List<IMapMarker> markers(net.minecraft.world.entity.player.Player player) {
                if (player == null) {
                    return List.of();
                }
                long now = player.level().getGameTime();
                List<EchoDroneMarker> markers = RECENT_MARKERS.getOrDefault(player.getUUID(), List.of()).stream()
                        .filter(marker -> marker.expiresAt() > now)
                        .toList();
                List<IMapMarker> out = new ArrayList<>();
                int index = 0;
                for (EchoDroneMarker marker : markers) {
                    out.add(new EchoMapMarker(
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID,
                                    "drone_scan/" + player.getUUID() + "/" + index++),
                            MAP_LAYER_ID,
                            MAP_PROVIDER_ID,
                            IMapMarker.MarkerKind.DRONE_SCAN,
                            IMapMarker.MarkerState.DISCOVERED,
                            marker.label(),
                            marker.detail(),
                            marker.dimension(),
                            marker.pos().getX() + 0.5D,
                            marker.pos().getY() + 0.5D,
                            marker.pos().getZ() + 0.5D,
                            marker.category() == EchoDroneScanCategory.HAZARD ? 4.0F : 1.5F,
                            null,
                            null,
                            index,
                            marker.precise()));
                }
                return out;
            }
        });
    }

    public static List<EchoDroneScanResult> scanArea(ServerPlayer owner, EchoCompanionDrone drone, BlockPos center,
            boolean scoutScan) {
        if (owner == null || drone == null || !(owner.level() instanceof ServerLevel level)) {
            return List.of();
        }
        CompanionDroneData data = CompanionDroneStateStore.get(owner);
        if (!Config.ENABLE_COMPANION_DRONE_UTILITY.get()) {
            send(owner, "Companion Drone utility is disabled by config.", ChatFormatting.YELLOW);
            return List.of();
        }

        long now = level.getGameTime();
        int cooldown = scoutScan ? Config.DRONE_SCOUT_COOLDOWN_TICKS.get() : Config.DRONE_SCAN_COOLDOWN_TICKS.get();
        long lastScan = scoutScan ? data.getLastScoutTime() : data.getLastScanTime();
        if (lastScan != Long.MIN_VALUE && now - lastScan < cooldown) {
            long remaining = Math.max(1L, (cooldown - (now - lastScan) + 19L) / 20L);
            send(owner, "Scan array cooling down: " + remaining + "s.", ChatFormatting.YELLOW);
            return List.of();
        }

        int radius = scoutScan ? Config.DRONE_SCOUT_SCAN_RADIUS.get()
                : data.hasUpgrade(EchoDroneUpgrade.SENSOR_LENS)
                        ? Config.DRONE_SCAN_RADIUS_SENSOR_LENS.get()
                        : Config.DRONE_SCAN_RADIUS_BASE.get();
        radius = Math.max(4, Math.min(48, signalAdjustedRadius(data, radius)));

        List<EchoDroneScanResult> results = new ArrayList<>();
        BlockPos scanCenter = center == null ? drone.blockPosition() : center.immutable();
        addMissionHint(owner, data, scanCenter, results);
        scanEntities(level, owner, scanCenter, radius, results);
        scanDroppedItems(level, scanCenter, radius, results);
        scanBlocks(level, scanCenter, radius, results);

        List<EchoDroneScanResult> sorted = results.stream()
                .sorted(Comparator.comparingInt((EchoDroneScanResult result) -> result.category().priority())
                        .thenComparingDouble(EchoDroneScanResult::distanceSqr))
                .toList();
        List<EchoDroneScanResult> limited = prioritizeResultCoverage(sorted, Math.max(1, Config.DRONE_SCAN_MAX_RESULTS.get()));

        List<EchoDroneMarker> markers = limited.stream()
                .limit(Math.max(0, Config.DRONE_SCAN_MAX_MARKERS.get()))
                .map(result -> new EchoDroneMarker(
                        result.category(),
                        result.label(),
                        result.detail(),
                        level.dimension(),
                        result.pos(),
                        now + Math.max(20, Config.DRONE_MARKER_DURATION_TICKS.get()),
                        result.precise()))
                .toList();
        publishTemporaryMarkers(owner, markers);

        String summary = summary(owner, limited, scoutScan);
        if (scoutScan) {
            data.setLastScoutTime(now);
        } else {
            data.setLastScanTime(now);
        }
        data.setLastScanSummary(summary);
        data.setTaskLabel(scoutScan ? "Scout scan complete" : "Scan complete");
        data.setMode(scoutScan ? EchoDroneMode.SCOUT : data.getMode());
        data.setBatteryPercent(data.getBatteryPercent() - (scoutScan ? 4 : 2));
        CompanionDroneStateStore.save(owner, data);
        drone.speak(summary, EchoCompanionDrone.MOOD_PROFESSIONAL, 45, 6);
        send(owner, summary, ChatFormatting.GREEN);
        recordScanProgress(owner);
        AshfallAdapterCoreExplorationRuntime.droneState(
                owner,
                scoutScan ? "scout_scan" : "scan_area",
                data.getMode().name(),
                !limited.isEmpty(),
                Map.of(
                        "resultCount", limited.size(),
                        "scoutScan", scoutScan,
                        "markerCount", markers.size()));
        return limited;
    }

    public static void publishTemporaryMarkers(ServerPlayer player, List<EchoDroneMarker> markers) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        List<EchoDroneMarker> active = new ArrayList<>();
        active.addAll(RECENT_MARKERS.getOrDefault(player.getUUID(), List.of()).stream()
                .filter(marker -> marker.expiresAt() > now)
                .toList());
        if (markers != null) {
            active.addAll(markers.stream()
                    .filter(marker -> marker != null && marker.expiresAt() > now)
                    .toList());
        }
        active = active.stream()
                .sorted(Comparator.comparingInt((EchoDroneMarker marker) -> marker.category().priority()))
                .limit(Math.max(0, Config.DRONE_SCAN_MAX_MARKERS.get()))
                .toList();
        RECENT_MARKERS.put(player.getUUID(), active);
        EchoNetSend.toPlayer(player, DroneMarkersPacket.of(active, now));
    }

    public static List<EchoDroneMarker> recentMarkers(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        long now = player.level().getGameTime();
        return RECENT_MARKERS.getOrDefault(player.getUUID(), List.of()).stream()
                .filter(marker -> marker.expiresAt() > now)
                .toList();
    }

    private static int signalAdjustedRadius(CompanionDroneData data, int radius) {
        if (!Config.ENABLE_DRONE_SIGNAL.get()) {
            return radius;
        }
        if (data.getSignalQuality() >= 70 || data.hasUpgrade(EchoDroneUpgrade.SIGNAL_ANTENNA)) {
            return radius;
        }
        if (data.getSignalQuality() >= 35) {
            return Math.max(6, (int) Math.round(radius * 0.75D));
        }
        return Math.max(4, radius / 2);
    }

    private static List<EchoDroneScanResult> prioritizeResultCoverage(List<EchoDroneScanResult> sorted, int maxResults) {
        if (sorted == null || sorted.isEmpty()) {
            return List.of();
        }
        List<EchoDroneScanResult> out = new ArrayList<>();
        for (EchoDroneScanCategory category : EchoDroneScanCategory.values()) {
            if (out.size() >= maxResults) {
                break;
            }
            sorted.stream()
                    .filter(result -> result.category() == category)
                    .findFirst()
                    .ifPresent(result -> {
                        if (!out.contains(result)) {
                            out.add(result);
                        }
                    });
        }
        for (EchoDroneScanResult result : sorted) {
            if (out.size() >= maxResults) {
                break;
            }
            if (!out.contains(result)) {
                out.add(result);
            }
        }
        return List.copyOf(out);
    }

    private static void addMissionHint(ServerPlayer owner, CompanionDroneData data, BlockPos center,
            List<EchoDroneScanResult> results) {
        if (!Config.ENABLE_DRONE_MISSION_HINTS.get()) {
            return;
        }
        QuestData quest = QuestData.get(owner);
        MissionUxSummary summary = MissionUxSummary.current(owner, quest);
        if (summary.missionId().isBlank()) {
            if (data.hasUpgrade(EchoDroneUpgrade.MISSION_DECODER)) {
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.MISSION, center,
                        "No mission signal detected", "Mission decoder has no active objective lock.", null, 0.0D, false));
            }
            return;
        }
        String detail = summary.nextStep();
        if (detail == null || detail.isBlank()) {
            detail = summary.objectiveSummary();
        }
        results.add(new EchoDroneScanResult(EchoDroneScanCategory.MISSION, center,
                "Objective: " + summary.shortTitle(), detail, null, 0.0D, false));
    }

    private static void scanEntities(ServerLevel level, ServerPlayer owner, BlockPos center, int radius,
            List<EchoDroneScanResult> results) {
        double r = radius;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(r),
                entity -> entity.isAlive()
                        && entity != owner
                        && !entity.getType().builtInRegistryHolder().is(DroneTags.IGNORE_ENTITIES))) {
            if (isHostile(entity, owner)) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String label = entity.getType().builtInRegistryHolder().is(DroneTags.HOSTILE_PRIORITY)
                        ? "Priority hostile" : "Hostile movement";
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.HOSTILE,
                        entity.blockPosition(), label, entity.getDisplayName().getString(), id,
                        entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D), true));
            } else if (entity.getType().builtInRegistryHolder().is(DroneTags.SCAN_INTEREST)) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.LOOT,
                        entity.blockPosition(), "Field signal", entity.getDisplayName().getString(), id,
                        entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D), false));
            }
        }
    }

    private static void scanDroppedItems(ServerLevel level, BlockPos center, int radius, List<EchoDroneScanResult> results) {
        double r = radius;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(center).inflate(r),
                item -> item.isAlive() && !item.getItem().isEmpty() && !item.getItem().is(DroneTags.IGNORE_ITEMS))) {
            boolean salvage = item.getItem().is(DroneTags.SALVAGE_ITEMS) || Config.DRONE_ALLOW_NON_SCRAP_PICKUP.get();
            if (!salvage) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item.getItem().getItem());
            results.add(new EchoDroneScanResult(EchoDroneScanCategory.LOOT, item.blockPosition(),
                    item.getItem().is(DroneTags.SALVAGE_ITEMS) ? "Dropped salvage" : "Dropped item",
                    item.getItem().getHoverName().getString(), id,
                    item.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D), true));
        }
    }

    private static void scanBlocks(ServerLevel level, BlockPos center, int radius, List<EchoDroneScanResult> results) {
        int checked = 0;
        int maxChecks = Math.max(128, Config.DRONE_SCAN_MAX_BLOCK_CHECKS.get());
        List<BlockPos> candidates = new ArrayList<>();
        int verticalRadius = Math.min(8, radius);
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-radius, -verticalRadius, -radius),
                center.offset(radius, verticalRadius, radius))) {
            candidates.add(cursor.immutable());
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        for (BlockPos pos : candidates) {
            if (++checked > maxChecks || results.size() >= Config.DRONE_SCAN_MAX_RESULTS.get() * 3) {
                return;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(DroneTags.IGNORE_BLOCKS)) {
                continue;
            }
            double dist = pos.distSqr(center);
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            BlockEntity be = level.getBlockEntity(pos);
            if (state.is(DroneTags.SCAN_OBJECTIVES) || state.is(ModBlocks.STRUCTURE_CACHE.get())
                    || state.is(ModBlocks.ECHO_CACHE.get())) {
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.MISSION, pos, "Objective signal",
                        blockName(state), id, dist, false));
                continue;
            }
            if (isHazardBlock(state)) {
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.HAZARD, pos, "Hazard signature",
                        blockName(state), id, dist, false));
                continue;
            }
            if (state.is(DroneTags.SCAN_RESOURCES) || state.is(COMMON_ORES) || id.getPath().contains("ore")) {
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.RESOURCE, pos, "Resource node",
                        blockName(state), id, dist, false));
                continue;
            }
            if (be instanceof Container || state.is(DroneTags.SCAN_CONTAINERS)
                    || state.is(Blocks.CHEST) || state.is(Blocks.BARREL) || state.is(ModBlocks.SUPPLY_CRATE.get())) {
                results.add(new EchoDroneScanResult(EchoDroneScanCategory.CONTAINER, pos, "Container",
                        blockName(state), id, dist, false));
            }
        }
    }

    private static boolean isHazardBlock(BlockState state) {
        return state.is(DroneTags.SCAN_HAZARDS)
                || state.is(HazardZoneManager.TOXIC_AIR_SOURCES)
                || state.is(HazardZoneManager.RADIATION_SOURCES)
                || state.is(HazardZoneManager.ACID_SOURCES)
                || state.is(HazardZoneManager.CRYO_SOURCES)
                || state.is(HazardZoneManager.NEXUS_ANOMALY_SOURCES)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.LAVA);
    }

    public static boolean isHostile(Entity entity, ServerPlayer owner) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (entity instanceof Mob mob && (mob.getTarget() == owner || mob.getTarget() instanceof EchoCompanionDrone)) {
            return true;
        }
        return entity instanceof Monster || entity.getType().builtInRegistryHolder().is(DroneTags.HOSTILE_PRIORITY);
    }

    private static String blockName(BlockState state) {
        return state.getBlock().getName().getString();
    }

    private static String summary(ServerPlayer owner, List<EchoDroneScanResult> results, boolean scoutScan) {
        String prefix = scoutScan ? "Scout scan complete: " : "Scan complete: ";
        if (results == null || results.isEmpty()) {
            return prefix + "no notable signals.";
        }
        Map<EchoDroneScanCategory, Integer> counts = new EnumMap<>(EchoDroneScanCategory.class);
        for (EchoDroneScanResult result : results) {
            counts.merge(result.category(), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        addPart(parts, counts, EchoDroneScanCategory.LOOT);
        addPart(parts, counts, EchoDroneScanCategory.HOSTILE);
        addPart(parts, counts, EchoDroneScanCategory.HAZARD);
        addPart(parts, counts, EchoDroneScanCategory.MISSION);
        addPart(parts, counts, EchoDroneScanCategory.RESOURCE);
        addPart(parts, counts, EchoDroneScanCategory.CONTAINER);
        EchoDroneScanResult nearest = results.stream()
                .min(Comparator.comparingInt((EchoDroneScanResult result) -> result.category().priority())
                        .thenComparingDouble(EchoDroneScanResult::distanceSqr))
                .orElse(null);
        String nearestLine = nearest == null || owner == null ? "" : " Nearest: " + nearest.label()
                + " " + Math.max(1, (int)Math.round(Math.sqrt(nearest.distanceSqr()))) + "m "
                + directionFrom(owner, nearest.pos()) + ".";
        return prefix + String.join(", ", parts) + "." + nearestLine;
    }

    private static String directionFrom(ServerPlayer owner, BlockPos pos) {
        double dx = pos.getX() + 0.5D - owner.getX();
        double dz = pos.getZ() + 0.5D - owner.getZ();
        if (Math.abs(dx) < 1.0D && Math.abs(dz) < 1.0D) {
            return "nearby";
        }
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0D ? "east" : "west";
        }
        return dz >= 0.0D ? "south" : "north";
    }

    private static void addPart(List<String> parts, Map<EchoDroneScanCategory, Integer> counts, EchoDroneScanCategory category) {
        int count = counts.getOrDefault(category, 0);
        if (count <= 0) {
            return;
        }
        parts.add(count + " " + category.summaryName() + (count == 1 ? "" : "s"));
    }

    private static void send(ServerPlayer owner, String message, ChatFormatting color) {
        owner.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] " + message).withStyle(color), true);
    }

    private static void recordScanProgress(ServerPlayer owner) {
        try {
            QuestData quest = QuestData.get(owner);
            quest.visitLocation("special", "drone:field_scan");
            quest.visitLocation("special", "drone:intel_recovered");
            QuestData.saveAndSync(owner, quest);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.debug("Unable to record Companion Drone scan progress.", exception);
        }
    }
}

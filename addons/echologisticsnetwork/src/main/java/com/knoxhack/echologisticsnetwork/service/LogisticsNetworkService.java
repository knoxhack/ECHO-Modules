package com.knoxhack.echologisticsnetwork.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echologisticsnetwork.Config;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointProvider;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointRole;
import com.knoxhack.echologisticsnetwork.api.LogisticsNetworkStatus;
import com.knoxhack.echologisticsnetwork.content.FactionDepotOffer;
import com.knoxhack.echologisticsnetwork.content.FactoryRestockPolicy;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.LoadoutRequirement;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.content.SupplyCategory;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionHooks;
import com.knoxhack.echologisticsnetwork.registry.ModEntities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class LogisticsNetworkService {
   private static final int BASE_RADIUS = 24;
   private static final int INDUSTRIAL_RADIUS = 36;
   private static final int INDUSTRIAL_DUCT_LIMIT = 96;
   private static final int Y_RADIUS = 8;
   private static final int DELIVERY_ROW_LIMIT = 5;
   private static final int SNAPSHOT_SCAN_CACHE_LIMIT = 128;
   private static final List<LogisticsKind> REQUEST_TARGETS = List.of(
      LogisticsKind.LOADOUT_LOCKER,
      LogisticsKind.ROUTE_REQUESTER,
      LogisticsKind.AUTO_RESTOCK_STATION
   );
   private static final List<LogisticsExternalEndpointProvider> EXTERNAL_ENDPOINT_PROVIDERS = new CopyOnWriteArrayList<>();
   private static final Map<SnapshotScanKey, CachedSnapshotScan> SNAPSHOT_SCAN_CACHE = new LinkedHashMap<>(64, 0.75F, true);

   private LogisticsNetworkService() {
   }

   public static void registerExternalEndpointProvider(LogisticsExternalEndpointProvider provider) {
      if (provider == null || provider.providerId() == null) {
         return;
      }
      for (LogisticsExternalEndpointProvider existing : EXTERNAL_ENDPOINT_PROVIDERS) {
         if (provider.providerId().equals(existing.providerId())) {
            return;
         }
      }
      EXTERNAL_ENDPOINT_PROVIDERS.add(provider);
   }

   public static List<Identifier> externalEndpointProviderIds() {
      return EXTERNAL_ENDPOINT_PROVIDERS.stream()
         .map(LogisticsExternalEndpointProvider::providerId)
         .filter(id -> id != null)
         .toList();
   }

   public static List<LogisticsExternalEndpoint> externalEndpointsFromProvider(Level level, BlockPos origin, String networkId, Identifier providerId) {
      if (providerId == null || level == null || origin == null) {
         return List.of();
      }
      for (LogisticsExternalEndpointProvider provider : EXTERNAL_ENDPOINT_PROVIDERS) {
         if (providerId.equals(provider.providerId())) {
            return safeExternalEndpoints(provider, level, origin, networkId);
         }
      }
      return List.of();
   }

   public static LogisticsSnapshot snapshot(Level level, BlockPos origin, String networkId, Player player) {
      return snapshot(level, origin, networkId, player, false);
   }

   public static LogisticsNetworkStatus networkStatus(Level level, BlockPos origin, String networkId) {
      return networkStatus(level, origin, networkId, null);
   }

   public static LogisticsNetworkStatus networkStatus(Level level, BlockPos origin, String networkId, Player player) {
      return LogisticsNetworkStatus.from(snapshot(level, origin, networkId, player));
   }

   public static LogisticsSnapshot snapshot(Level level, BlockPos origin, String networkId, Player player, boolean forceRefresh) {
      if (level == null || origin == null) {
         return LogisticsSnapshot.empty(networkId);
      }
      String resolvedNetworkId = normalizeNetworkId(networkId);
      CachedSnapshotScan scan = cachedSnapshotScan(level, origin, resolvedNetworkId, forceRefresh);
      List<LogisticsBlockEntity> logisticsBlocks = scan.logisticsBlocks();
      List<StorageNode> nodes = scan.storageNodes();
      List<StockRow> stockRows = new ArrayList<>();
      List<MissingRow> missingRows = new ArrayList<>();
      for (SupplyCategory category : LogisticsContent.categories()) {
         int count = countCategory(nodes, category);
         stockRows.add(new StockRow(category.id(), category.title(), count, category.lowStockTarget(), category.accentColor()));
         if (count < category.lowStockTarget()) {
            missingRows.add(new MissingRow(category.id(), category.title(), category.lowStockTarget() - count, category.accentColor()));
         }
      }
      List<LoadoutReadiness> readiness = LogisticsContent.loadouts().stream()
         .map(loadout -> readiness(nodes, loadout))
         .toList();
      List<DeliveryJob> deliveryJobs = scan.deliveryJobs();
      FactoryRestockStatus factoryRestock = factoryRestockStatus(level, origin, resolvedNetworkId,
         readiness.stream()
            .filter(LoadoutReadiness::ready)
            .map(row -> row.presetId().toString())
            .findFirst()
            .orElse(selectedLoadoutIdForSnapshot(readiness)));
      boolean dockOnline = logisticsBlocks.stream().anyMatch(block -> block.kind() == LogisticsKind.DRONE_DELIVERY_DOCK);
      Optional<LogisticsBlockEntity> relay = nearestKind(logisticsBlocks, origin, LogisticsKind.REMOTE_REWARD_RELAY);
      Optional<LogisticsBlockEntity> depot = nearestKind(logisticsBlocks, origin, LogisticsKind.FACTION_TRADE_DEPOT);
      int endpointCount = (int)logisticsBlocks.stream().filter(LogisticsNetworkService::isRequestTarget).count()
         + (int)scan.externalEndpoints().stream()
            .filter(endpoint -> endpoint.hasRole(LogisticsExternalEndpointRole.REQUEST_TARGET)
               || endpoint.hasRole(LogisticsExternalEndpointRole.DELIVERY_TARGET))
            .count();
      LogisticsBlockEntity selectedEndpoint = chooseEndpoint(level, origin, logisticsBlocks, "");
      String selectedLoadoutId = selectedEndpoint == null ? firstReadyLoadout(readiness) : selectedEndpoint.loadoutId();
      Optional<LoadoutReadiness> selectedReadiness = readiness(readiness, selectedLoadoutId);
      String selectedTitle = LogisticsContent.loadout(selectedLoadoutId)
         .map(LoadoutPreset::title)
         .orElse(selectedLoadoutId == null || selectedLoadoutId.isBlank() ? "None" : selectedLoadoutId);
      EndpointRef endpointRef = selectedEndpoint == null
         ? null
         : new EndpointRef(selectedEndpoint.kind(), selectedEndpoint.getBlockPos(), selectedEndpoint.loadoutId());
      return new LogisticsSnapshot(
         resolvedNetworkId,
         stockRows,
         missingRows,
         readiness,
         deliveryJobs.size(),
         deliveryJobs.stream().limit(DELIVERY_ROW_LIMIT).toList(),
         LogisticsContent.offers(),
         logisticsBlocks.size(),
         endpointCount,
         dockOnline,
         relay.isPresent(),
         depot.isPresent(),
         depot.map(LogisticsBlockEntity::cooldownTicks).orElse(0),
         endpointRef,
         selectedLoadoutId,
         selectedTitle,
         selectedReadiness.map(LoadoutReadiness::ready).orElse(false),
         selectedReadiness.map(LoadoutReadiness::missingCount).orElse(0),
         resolvedNetworkId + "|" + (selectedLoadoutId == null ? "" : selectedLoadoutId),
         factoryRestock
      );
   }

   public static void invalidateSnapshots() {
      synchronized (SNAPSHOT_SCAN_CACHE) {
         SNAPSHOT_SCAN_CACHE.clear();
      }
   }

   private static CachedSnapshotScan cachedSnapshotScan(Level level, BlockPos origin, String networkId, boolean forceRefresh) {
      int cacheTicks = Config.snapshotCacheTicks();
      long now = level.getGameTime();
      SnapshotScanKey key = SnapshotScanKey.of(level, origin, networkId);
      if (!forceRefresh && cacheTicks > 0) {
         synchronized (SNAPSHOT_SCAN_CACHE) {
            CachedSnapshotScan cached = SNAPSHOT_SCAN_CACHE.get(key);
            if (cached != null && now - cached.createdTick() <= cacheTicks) {
               return cached;
            }
         }
      }
      CachedSnapshotScan scanned = buildSnapshotScan(level, origin, networkId, now);
      if (cacheTicks > 0) {
         synchronized (SNAPSHOT_SCAN_CACHE) {
            SNAPSHOT_SCAN_CACHE.put(key, scanned);
            while (SNAPSHOT_SCAN_CACHE.size() > SNAPSHOT_SCAN_CACHE_LIMIT) {
               SNAPSHOT_SCAN_CACHE.remove(SNAPSHOT_SCAN_CACHE.keySet().iterator().next());
            }
         }
      }
      return scanned;
   }

   private static CachedSnapshotScan buildSnapshotScan(Level level, BlockPos origin, String networkId, long now) {
      List<LogisticsBlockEntity> blocks = logisticsBlocks(level, origin, networkId);
      List<LogisticsExternalEndpoint> endpoints = externalEndpoints(level, origin, networkId);
      List<StorageNode> nodes = storageNodes(level, origin, networkId, blocks, endpoints);
      List<DeliveryJob> deliveryJobs = activeDeliveryJobs(level, origin, networkId);
      return new CachedSnapshotScan(now, blocks, nodes, endpoints, deliveryJobs);
   }

   public static boolean requestDashboardLoadout(Player player, BlockPos origin, String networkId) {
      if (player == null || !(player.level() instanceof ServerLevel level) || origin == null) {
         return false;
      }
      String resolvedNetworkId = normalizeNetworkId(networkId);
      LogisticsBlockEntity endpoint = chooseEndpoint(level, origin, resolvedNetworkId, "");
      if (endpoint == null) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // No eligible Loadout Locker, Route Requester, or Auto-Restock Station in network " + resolvedNetworkId + "."));
         return false;
      }
      return requestLoadout(player, endpoint.getBlockPos(), endpoint.getBlockPos(), endpoint.loadoutId());
   }

   public static boolean requestLoadout(Player player, BlockPos origin, BlockPos targetPos, String presetId) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      return requestLoadout(level, player.getUUID(), player, origin, targetPos, presetId, null);
   }

   public static boolean requestLoadout(ServerLevel level, UUID owner, BlockPos origin, BlockPos targetPos, String presetId) {
      return requestLoadout(level, owner, origin, targetPos, presetId, null);
   }

   public static boolean requestLoadout(ServerLevel level, UUID owner, BlockPos origin, BlockPos targetPos, String presetId, String networkId) {
      return requestLoadout(level, owner, null, origin, targetPos, presetId, networkId);
   }

   public static FactoryRestockStatus factoryRestockStatus(Level level, BlockPos origin, String networkId, String presetId) {
      return evaluateFactoryRestock(level, null, null, origin, networkId, presetId, false);
   }

   public static FactoryRestockStatus requestFactoryAutoRestock(ServerPlayer player, BlockPos origin, String presetId) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return FactoryRestockStatus.blocked("No server player context available.", "global", presetId, BlockPos.ZERO);
      }
      return evaluateFactoryRestock(level, player.getUUID(), player, origin, nearestNetworkId(level, origin), presetId, true);
   }

   public static FactoryRestockStatus requestFactoryAutoRestock(ServerLevel level, UUID owner, Player player, BlockPos origin, String networkId, String presetId) {
      return evaluateFactoryRestock(level, owner, player, origin, networkId, presetId, true);
   }

   public static FactoryDispatchResult requestFactoryLoadout(ServerPlayer player, BlockPos origin, String presetId) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return new FactoryDispatchResult(false, "No server player context available.", "global", presetId == null ? "" : presetId, BlockPos.ZERO);
      }
      if (origin == null) {
         return new FactoryDispatchResult(false, "No factory controller origin supplied.", "global", presetId == null ? "" : presetId, BlockPos.ZERO);
      }
      LoadoutPreset preset = LogisticsContent.loadout(presetId).orElse(null);
      if (preset == null) {
         return new FactoryDispatchResult(false, "Unknown factory loadout " + presetId + ".", "global", presetId == null ? "" : presetId, BlockPos.ZERO);
      }
      String networkId = nearestNetworkId(level, origin);
      List<StorageNode> nodes = storageNodes(level, origin, networkId);
      ExtractionPlan plan = planLoadout(nodes, preset);
      if (!plan.ready()) {
         return new FactoryDispatchResult(false, "Missing " + plan.missingRequired() + " required supply item(s).", networkId, preset.id().toString(), BlockPos.ZERO);
      }
      List<StorageNode> depots = nodes.stream()
         .filter(node -> isIndustrialInputDepot(level, node.pos()))
         .filter(node -> targetBlockTypeAllowed(level, node.pos(), preset))
         .sorted(Comparator.comparingDouble(node -> node.pos().distSqr(origin)))
         .toList();
      if (depots.isEmpty()) {
         boolean anyInputDepot = nodes.stream().anyMatch(node -> isIndustrialInputDepot(level, node.pos()));
         return new FactoryDispatchResult(false,
            anyInputDepot ? "Nearest Industrial input depot is disallowed by this loadout." : "No connected Industrial input depot was found.",
            networkId,
            preset.id().toString(),
            BlockPos.ZERO);
      }
      StorageNode target = depots.stream()
         .filter(node -> canAcceptPayload(level, node.pos(), plan.payload()))
         .findFirst()
         .orElse(null);
      if (target == null) {
         return new FactoryDispatchResult(false, "Connected Industrial input depot is full or cannot accept the full payload.", networkId, preset.id().toString(), depots.get(0).pos());
      }
      LogisticsBlockEntity dock = nearestDock(level, origin, networkId);
      if (dock == null) {
         return new FactoryDispatchResult(false, "No Drone Delivery Dock online for network " + networkId + ".", networkId, preset.id().toString(), target.pos());
      }
      boolean dispatched = requestLoadout(level, player.getUUID(), player, origin, target.pos(), preset.id().toString(), networkId);
      return new FactoryDispatchResult(
         dispatched,
         dispatched ? "Courier dispatched to Industrial input depot " + target.pos().toShortString() + "." : "Dispatch blocked. Check stock, dock, depot capacity, and route access.",
         networkId,
         preset.id().toString(),
         target.pos()
      );
   }

   private static FactoryRestockStatus evaluateFactoryRestock(Level rawLevel, UUID owner, Player player, BlockPos origin,
                                                              String networkId, String presetId, boolean dispatch) {
      if (!(rawLevel instanceof ServerLevel serverLevel)) {
         return FactoryRestockStatus.blocked("Factory restock requires a server level.", networkId, presetId, BlockPos.ZERO);
      }
      if (rawLevel == null || origin == null) {
         return FactoryRestockStatus.blocked("No factory restock origin supplied.", networkId, presetId, BlockPos.ZERO);
      }
      LoadoutPreset preset = LogisticsContent.loadout(presetId).orElse(null);
      if (preset == null) {
         return FactoryRestockStatus.blocked("Unknown factory loadout " + (presetId == null ? "" : presetId) + ".", networkId, presetId, BlockPos.ZERO);
      }
      FactoryRestockPolicy policy = preset.restockPolicy();
      if (!policy.enabled()) {
         return new FactoryRestockStatus(false, false, "Loadout is not configured for factory auto-restock.",
            normalizeNetworkId(networkId), preset.id().toString(), BlockPos.ZERO, 0, 0, 0, 0, policy.cooldownTicks());
      }
      if (!EchoRuntimeModules.isLoaded("echoindustrialnexus")) {
         return new FactoryRestockStatus(false, false, "Industrial Nexus is not loaded; no factory depot target is available.",
            normalizeNetworkId(networkId), preset.id().toString(), BlockPos.ZERO, 0, policy.targetRuns(), policy.minRuns(), 0, policy.cooldownTicks());
      }
      String resolvedNetworkId = networkId == null || networkId.isBlank() ? nearestNetworkId(rawLevel, origin) : normalizeNetworkId(networkId);
      CachedSnapshotScan scan = dispatch ? null : cachedSnapshotScan(rawLevel, origin, resolvedNetworkId, false);
      List<StorageNode> nodes = scan == null ? storageNodes(rawLevel, origin, resolvedNetworkId) : scan.storageNodes();
      List<FactoryDepotCandidate> candidates = nodes.stream()
         .filter(node -> isIndustrialInputDepot(rawLevel, node.pos()))
         .filter(node -> targetBlockTypeAllowed(rawLevel, node.pos(), preset))
         .filter(node -> industrialControllerAllowsRestock(rawLevel, node.pos(), preset))
         .map(node -> new FactoryDepotCandidate(node, currentRuns(node.container(), preset),
            inFlightRuns(rawLevel, origin, resolvedNetworkId, node.pos(), preset.id(), scan)))
         .sorted(Comparator.comparingInt((FactoryDepotCandidate candidate) -> candidate.currentRuns() + candidate.inFlightRuns())
            .thenComparingDouble(candidate -> candidate.node().pos().distSqr(origin)))
         .toList();
      if (candidates.isEmpty()) {
         boolean anyDepot = nodes.stream().anyMatch(node -> isIndustrialInputDepot(rawLevel, node.pos()));
         return new FactoryRestockStatus(false, false,
            anyDepot ? "No enabled Industrial input depot accepts this auto-restock loadout." : "No connected Industrial input depot was found.",
            resolvedNetworkId, preset.id().toString(), BlockPos.ZERO, 0, policy.targetRuns(), policy.minRuns(), 0, policy.cooldownTicks());
      }
      FactoryDepotCandidate target = candidates.stream()
         .filter(candidate -> candidate.currentRuns() + candidate.inFlightRuns() < policy.minRuns())
         .findFirst()
         .orElse(null);
      if (target == null) {
         FactoryDepotCandidate best = candidates.get(0);
         return new FactoryRestockStatus(true, false, "Factory depot already meets auto-restock target.",
            resolvedNetworkId, preset.id().toString(), best.node().pos(), best.currentRuns(), policy.targetRuns(),
            policy.minRuns(), best.inFlightRuns(), policy.cooldownTicks());
      }
      if (target.inFlightRuns() >= policy.maxInFlight()) {
         return new FactoryRestockStatus(true, false, "Factory auto-restock is waiting for in-flight courier capacity.",
            resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
            policy.minRuns(), target.inFlightRuns(), policy.cooldownTicks());
      }
      List<StorageNode> sourceNodes = nodes.stream()
         .filter(node -> !node.pos().equals(target.node().pos()))
         .filter(node -> !isIndustrialInputDepot(rawLevel, node.pos()))
         .toList();
      ExtractionPlan plan = planLoadout(sourceNodes, preset);
      if (!plan.ready()) {
         return new FactoryRestockStatus(true, false, "Missing " + plan.missingRequired() + " required supply item(s).",
            resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
            policy.minRuns(), target.inFlightRuns(), policy.cooldownTicks());
      }
      if (!canAcceptPayload(rawLevel, target.node().pos(), plan.payload())) {
         return new FactoryRestockStatus(true, false, "Industrial input depot is full or cannot accept the full payload.",
            resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
            policy.minRuns(), target.inFlightRuns(), policy.cooldownTicks());
      }
      LogisticsBlockEntity dock = scan == null
         ? nearestDock(serverLevel, origin, resolvedNetworkId)
         : nearestKind(scan.logisticsBlocks(), origin, LogisticsKind.DRONE_DELIVERY_DOCK).orElse(null);
      if (dock == null) {
         return new FactoryRestockStatus(true, false, "No Drone Delivery Dock online for network " + resolvedNetworkId + ".",
            resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
            policy.minRuns(), target.inFlightRuns(), policy.cooldownTicks());
      }
      if (!dispatch) {
         return new FactoryRestockStatus(true, false, "Factory auto-restock is ready.",
            resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
            policy.minRuns(), target.inFlightRuns(), policy.cooldownTicks());
      }
      boolean dispatched = requestLoadout(serverLevel, owner, player, origin, target.node().pos(),
         preset.id().toString(), resolvedNetworkId, sourceNodes);
      if (dispatched) {
         LogisticsMissionHooks.recordIndustrialAutoRestock(player, preset.id().toString());
      }
      return new FactoryRestockStatus(true, dispatched,
         dispatched ? "Auto-restock courier dispatched to Industrial input depot " + target.node().pos().toShortString() + "."
            : "Factory auto-restock dispatch failed during final reservation.",
         resolvedNetworkId, preset.id().toString(), target.node().pos(), target.currentRuns(), policy.targetRuns(),
         policy.minRuns(), target.inFlightRuns() + (dispatched ? 1 : 0), policy.cooldownTicks());
   }

   private static boolean requestLoadout(ServerLevel level, UUID owner, Player player, BlockPos origin, BlockPos targetPos, String presetId, String explicitNetworkId) {
      return requestLoadout(level, owner, player, origin, targetPos, presetId, explicitNetworkId, null);
   }

   private static boolean requestLoadout(ServerLevel level, UUID owner, Player player, BlockPos origin, BlockPos targetPos,
                                         String presetId, String explicitNetworkId, List<StorageNode> explicitNodes) {
      if (level == null) {
         return false;
      }
      LoadoutPreset preset = LogisticsContent.loadout(presetId).orElse(null);
      if (preset == null) {
         message(player, "ECHO LOGISTICS // Unknown loadout preset: " + presetId + ". Rebind the Loadout Card or reload datapacks.");
         return false;
      }
      BlockPos requestOrigin = origin == null ? BlockPos.ZERO : origin;
      BlockPos deliveryTarget = targetPos == null ? requestOrigin : targetPos;
      if (!targetBlockTypeAllowed(level, deliveryTarget, preset)) {
         message(player, "ECHO LOGISTICS // Loadout target rejected by preset rules. Allowed targets: " + targetBlockList(preset) + ".");
         return false;
      }
      String networkId = explicitNetworkId == null || explicitNetworkId.isBlank() ? networkIdAt(level, requestOrigin) : normalizeNetworkId(explicitNetworkId);
      List<StorageNode> nodes = explicitNodes == null ? storageNodes(level, requestOrigin, networkId) : explicitNodes;
      ExtractionPlan plan = planLoadout(nodes, preset);
      if (!plan.ready()) {
         message(player, "ECHO LOGISTICS // Loadout blocked. Missing " + plan.missingRequired() + " required supply item(s). Scan Logistics for low-stock rows.");
         return false;
      }
      LogisticsBlockEntity dock = nearestDock(level, requestOrigin, networkId);
      if (dock == null) {
         message(player, "ECHO LOGISTICS // No Drone Delivery Dock online for network " + networkId + ". Place or manifest a dock on the same network.");
         return false;
      }
      if (!canAcceptPayload(level, deliveryTarget, plan.payload())) {
         message(player, "ECHO LOGISTICS // Target inventory cannot accept the full sealed payload. Clear space before dispatch.");
         return false;
      }
      List<ItemStack> payload = extractPlannedLoadout(plan);
      if (payload.isEmpty()) {
         message(player, "ECHO LOGISTICS // Payload extraction failed; source storage was left unchanged.");
         return false;
      }
      CourierDroneEntity drone = ModEntities.COURIER_DRONE.get().create(level, EntitySpawnReason.EVENT);
      if (drone == null) {
         recoverPayload(level, dock.getBlockPos(), payload);
         return false;
      }
      UUID jobId = UUID.randomUUID();
      BlockPos dockPos = dock.getBlockPos();
      drone.configureDelivery(jobId, owner, networkId, dockPos, deliveryTarget, preset.id(), payload, preset.deliveryTicks());
      drone.setPos(dockPos.getX() + 0.5D, dockPos.getY() + 1.35D, dockPos.getZ() + 0.5D);
      if (!level.addFreshEntity(drone)) {
         recoverPayload(level, dockPos, payload);
         return false;
      }
      recordDeliveryStatus(level, dockPos, deliveryTarget, "Delivery " + shortJob(jobId) + " dispatched: " + preset.title());
      message(player, "ECHO LOGISTICS // Courier dispatched: " + preset.title()
         + " [" + shortJob(jobId) + "] | ETA " + preset.deliveryTicks() + "t.");
      if (player instanceof ServerPlayer serverPlayer) {
         EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
         LogisticsMissionHooks.recordRequestLoadout(serverPlayer, preset.id().toString());
      }
      invalidateSnapshots();
      return true;
   }

   public static int pendingRelayRewards(Player player) {
      return player == null ? 0 : EchoCoreServices.pendingTerminalRewardCount(player);
   }

   public static boolean claimRelayRewards(ServerPlayer player, LogisticsBlockEntity relay) {
      if (player == null) {
         return false;
      }
      int pending = pendingRelayRewards(player);
      if (pending <= 0) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // No pending terminal rewards are waiting at the relay."));
         if (relay != null) {
            relay.refreshSnapshot(player);
         }
         return false;
      }
      if (relay == null || relay.kind() != LogisticsKind.REMOTE_REWARD_RELAY) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // Remote Reward Relay offline. Place a relay in this logistics network before claiming " + pending + " pending reward item(s)."));
         if (relay != null) {
            relay.refreshSnapshot(player);
         }
         return false;
      }
      boolean claimed = EchoCoreServices.claimTerminalRewards(player);
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal(claimed
         ? "ECHO LOGISTICS // Remote Reward Relay transferred pending rewards into your inventory."
         : "ECHO LOGISTICS // No pending terminal rewards are waiting at the relay."));
      if (relay != null) {
         relay.refreshSnapshot(player);
      }
      if (claimed) {
         EchoCoreServices.discoverVisibleRouteRecords(player);
      }
      return claimed;
   }

   public static boolean performDepotExchange(Player player, LogisticsBlockEntity depot) {
      if (player == null || depot == null) {
         return false;
      }
      if (depot.kind() != LogisticsKind.FACTION_TRADE_DEPOT) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // Depot exchange requires a Faction Trade Depot."));
         return false;
      }
      if (depot.cooldownTicks() > 0) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // Depot offer cooling down for " + ticks(depot.cooldownTicks()) + "."));
         return false;
      }
      List<FactionDepotOffer> offers = selectedDepotOffers(depot);
      if (offers.isEmpty()) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // Selected depot offer is no longer loaded. Cycle offers or reload datapacks."));
         return false;
      }
      for (FactionDepotOffer offer : offers) {
         int reputation = EchoCoreServices.factionProfile(player, offer.factionId()).map(profile -> profile.reputation()).orElse(0);
         if (reputation < offer.minReputation()) {
            continue;
         }
         if (!removeStack(depot, offer.input())) {
            continue;
         }
         if (!insertStack(depot, offer.output().copy())) {
            player.drop(offer.output().copy(), false);
         }
         EchoCoreServices.addFactionReputation(player, offer.factionId(), offer.reputationDelta());
         depot.setDepotOfferId(offer.id().toString());
         depot.setCooldownTicks(offer.cooldownTicks());
         depot.recordManifest("Depot offer completed: " + offer.id().getPath());
         LogisticsMissionHooks.recordDepotExchange(player, offer.id().toString());
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // Depot offer completed: "
            + offer.id().getPath().replace('_', ' ')
            + " | reputation " + signed(offer.reputationDelta())
            + " | cooldown " + ticks(offer.cooldownTicks()) + "."));
         depot.refreshSnapshot(player);
         return true;
      }
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO LOGISTICS // No available depot offer matched the stored goods and reputation."));
      return false;
   }

   public static boolean deliverPayload(Level level, BlockPos targetPos, List<ItemStack> payload) {
      if (level == null || targetPos == null || payload == null || payload.isEmpty()) {
         return false;
      }
      BlockEntity blockEntity = level.getBlockEntity(targetPos);
      if (blockEntity instanceof Container container) {
         return insertPayloadIntoContainer(container, payload);
      }
      return false;
   }

   public static boolean canAcceptPayload(Level level, BlockPos targetPos, List<ItemStack> payload) {
      if (level == null || targetPos == null || payload == null || payload.isEmpty()) {
         return false;
      }
      return level.getBlockEntity(targetPos) instanceof Container container && canInsertAll(container, payload);
   }

   public static boolean depositPayload(Level level, BlockPos origin, String networkId, List<ItemStack> payload) {
      if (level == null || origin == null || payload == null || payload.isEmpty()) {
         return false;
      }
      for (StorageNode node : externalDeliveryNodes(level, origin, networkId)) {
         if (canInsertAll(node.container(), payload)) {
            return insertPayloadIntoContainer(node.container(), payload);
         }
      }
      for (StorageNode node : storageNodes(level, origin, networkId)) {
         if (canInsertAll(node.container(), payload)) {
            return insertPayloadIntoContainer(node.container(), payload);
         }
      }
      return false;
   }

   public static boolean canAcceptNetworkPayload(Level level, BlockPos origin, String networkId, List<ItemStack> payload) {
      if (level == null || origin == null || payload == null || payload.isEmpty()) {
         return false;
      }
      return externalDeliveryNodes(level, origin, networkId).stream().anyMatch(node -> canInsertAll(node.container(), payload))
         || storageNodes(level, origin, networkId).stream().anyMatch(node -> canInsertAll(node.container(), payload));
   }

   public static int cancelActiveDeliveries(Player player, BlockPos origin, String networkId) {
      if (player == null || origin == null || !(player.level() instanceof ServerLevel level)) {
         return 0;
      }
      return cancelActiveDeliveries(level, player.getUUID(), origin, networkId);
   }

   public static int cancelActiveDeliveries(ServerLevel level, UUID owner, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return 0;
      }
      int cancelled = 0;
      for (CourierDroneEntity drone : level.getEntitiesOfClass(CourierDroneEntity.class, new AABB(origin).inflate(64.0D))) {
         DeliveryJob job = drone.deliveryJob();
         if (!networkMatches(networkId, drone.networkId())) {
            continue;
         }
         if (owner != null && job.owner() != null && !job.owner().equals(owner)) {
            continue;
         }
         if (drone.cancelToDock("cancelled by terminal")) {
            cancelled++;
         }
      }
      if (cancelled > 0) {
         invalidateSnapshots();
      }
      return cancelled;
   }

   private static boolean targetBlockTypeAllowed(Level level, BlockPos targetPos, LoadoutPreset preset) {
      if (preset.targetBlockTypes().isEmpty()) {
         return true;
      }
      Identifier targetId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock());
      return preset.targetBlockTypes().contains(targetId);
   }

   private static boolean isIndustrialInputDepot(Level level, BlockPos pos) {
      if (level == null || pos == null) {
         return false;
      }
      Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
      return "echoindustrialnexus".equals(id.getNamespace()) && "input_depot_crate".equals(id.getPath())
         && level.getBlockEntity(pos) instanceof Container;
   }

   private static boolean industrialControllerAllowsRestock(Level level, BlockPos depotPos, LoadoutPreset preset) {
      if (level == null || depotPos == null || preset == null) {
         return false;
      }
      FactoryRestockPolicy policy = preset.restockPolicy();
      boolean sawController = false;
      for (BlockPos pos : BlockPos.betweenClosed(depotPos.offset(-12, -6, -12), depotPos.offset(12, 6, 12))) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity == null || !"IndustrialMultiblockControllerBlockEntity".equals(blockEntity.getClass().getSimpleName())) {
            continue;
         }
         sawController = true;
         try {
            Object enabled = blockEntity.getClass().getMethod("logisticsAutoRestockEnabled").invoke(blockEntity);
            if (!Boolean.TRUE.equals(enabled)) {
               continue;
            }
            Object loadout = blockEntity.getClass()
               .getMethod("logisticsLoadoutIdForRecipe", Identifier.class)
               .invoke(blockEntity, policy.factoryTaskId());
            if (preset.id().toString().equals(String.valueOf(loadout))) {
               return true;
            }
         } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
         }
      }
      return !sawController;
   }

   private static int currentRuns(Container container, LoadoutPreset preset) {
      int runs = Integer.MAX_VALUE;
      boolean required = false;
      for (LoadoutRequirement requirement : preset.requirements()) {
         if (requirement.optional()) {
            continue;
         }
         required = true;
         int count = 0;
         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && requirement.matches(stack)) {
               count += stack.getCount();
            }
         }
         runs = Math.min(runs, count / Math.max(1, requirement.count()));
      }
      return required ? Math.max(0, runs == Integer.MAX_VALUE ? 0 : runs) : 0;
   }

   private static int inFlightRuns(Level level, BlockPos origin, String networkId, BlockPos targetPos, Identifier presetId) {
      return inFlightRuns(level, origin, networkId, targetPos, presetId, null);
   }

   private static int inFlightRuns(Level level, BlockPos origin, String networkId, BlockPos targetPos, Identifier presetId,
                                   CachedSnapshotScan scan) {
      if (level == null || origin == null || targetPos == null || presetId == null) {
         return 0;
      }
      List<DeliveryJob> jobs = scan == null ? activeDeliveryJobs(level, origin, networkId) : scan.deliveryJobs();
      return (int)jobs.stream()
         .filter(job -> targetPos.equals(job.targetPos()) && presetId.equals(job.presetId()))
         .count();
   }

   private static String targetBlockList(LoadoutPreset preset) {
      return preset.targetBlockTypes().isEmpty()
         ? "any inventory"
         : String.join(", ", preset.targetBlockTypes().stream().map(Identifier::toString).toList());
   }

   public static boolean insertPayloadIntoContainer(Container container, List<ItemStack> payload) {
      if (container == null || payload == null || payload.isEmpty()) {
         return false;
      }
      Optional<List<ItemStack>> simulated = simulateInserted(container, payload);
      if (simulated.isEmpty()) {
         return false;
      }
      List<ItemStack> slots = simulated.get();
      for (int slot = 0; slot < Math.min(container.getContainerSize(), slots.size()); slot++) {
         container.setItem(slot, slots.get(slot).copy());
      }
      container.setChanged();
      invalidateSnapshots();
      return true;
   }

   public static void recordDeliveryStatus(Level level, BlockPos sourceDock, BlockPos targetPos, String manifest) {
      if (level == null || manifest == null || manifest.isBlank()) {
         return;
      }
      if (sourceDock != null && level.getBlockEntity(sourceDock) instanceof LogisticsBlockEntity dock) {
         dock.recordManifest(manifest);
      }
      if (targetPos != null && !targetPos.equals(sourceDock) && level.getBlockEntity(targetPos) instanceof LogisticsBlockEntity target) {
         target.recordManifest(manifest);
      }
   }

   public static void returnPayload(Level level, BlockPos pos, List<ItemStack> payload) {
      if (level == null || pos == null || payload == null) {
         return;
      }
      for (ItemStack stack : payload) {
         if (!stack.isEmpty()) {
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack.copy());
            level.addFreshEntity(item);
         }
      }
   }

   public static void recoverPayload(Level level, BlockPos sourceDock, List<ItemStack> payload) {
      if (!deliverPayload(level, sourceDock, payload)) {
         returnPayload(level, sourceDock, payload);
      }
   }

   private static List<DeliveryJob> activeDeliveryJobs(Level level, BlockPos origin, String networkId) {
      return level.getEntitiesOfClass(CourierDroneEntity.class, new AABB(origin).inflate(64.0D)).stream()
         .filter(drone -> networkMatches(networkId, drone.networkId()))
         .map(CourierDroneEntity::deliveryJob)
         .sorted(Comparator.comparing(DeliveryJob::createdTick).thenComparing(job -> job.id().toString()))
         .toList();
   }

   private static List<LogisticsBlockEntity> logisticsBlocks(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return List.of();
      }
      int radius = EchoRuntimeModules.isLoaded("echoindustrialnexus") ? INDUSTRIAL_RADIUS : BASE_RADIUS;
      List<LogisticsBlockEntity> blocks = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -Y_RADIUS, -radius), origin.offset(radius, Y_RADIUS, radius))) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof LogisticsBlockEntity logistics && networkMatches(networkId, logistics.networkId())) {
            blocks.add(logistics);
         }
      }
      return blocks.stream().sorted(Comparator.comparing(block -> block.getBlockPos().toShortString())).toList();
   }

   private static Optional<LogisticsBlockEntity> nearestKind(List<LogisticsBlockEntity> blocks, BlockPos origin, LogisticsKind kind) {
      return blocks.stream()
         .filter(block -> block.kind() == kind)
         .min(Comparator.comparingDouble(block -> block.getBlockPos().distSqr(origin)));
   }

   private static boolean isRequestTarget(LogisticsBlockEntity block) {
      return block != null && REQUEST_TARGETS.contains(block.kind());
   }

   private static LogisticsBlockEntity chooseEndpoint(Level level, BlockPos origin, String networkId, String requestedLoadoutId) {
      return chooseEndpoint(level, origin, logisticsBlocks(level, origin, networkId), requestedLoadoutId);
   }

   private static LogisticsBlockEntity chooseEndpoint(Level level, BlockPos origin, List<LogisticsBlockEntity> logisticsBlocks, String requestedLoadoutId) {
      return logisticsBlocks.stream()
         .filter(LogisticsNetworkService::isRequestTarget)
         .filter(endpoint -> {
            String loadoutId = requestedLoadoutId == null || requestedLoadoutId.isBlank() ? endpoint.loadoutId() : requestedLoadoutId;
            return LogisticsContent.loadout(loadoutId).filter(preset -> targetBlockTypeAllowed(level, endpoint.getBlockPos(), preset)).isPresent();
         })
         .min(Comparator.comparingDouble(endpoint -> endpoint.getBlockPos().distSqr(origin)))
         .orElse(null);
   }

   private static String firstReadyLoadout(List<LoadoutReadiness> readiness) {
      return readiness.stream()
         .filter(LoadoutReadiness::ready)
         .map(row -> row.presetId().toString())
         .findFirst()
         .orElse(LogisticsContent.firstLoadoutId());
   }

   private static String selectedLoadoutIdForSnapshot(List<LoadoutReadiness> readiness) {
      return firstReadyLoadout(readiness);
   }

   private static Optional<LoadoutReadiness> readiness(List<LoadoutReadiness> readiness, String loadoutId) {
      if (loadoutId == null || loadoutId.isBlank()) {
         return Optional.empty();
      }
      return readiness.stream()
         .filter(row -> row.presetId().toString().equals(loadoutId))
         .findFirst();
   }

   private static boolean canInsertAll(Container container, List<ItemStack> payload) {
      return simulateInserted(container, payload).isPresent();
   }

   private static Optional<List<ItemStack>> simulateInserted(Container container, List<ItemStack> payload) {
      List<ItemStack> slots = new ArrayList<>();
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
         slots.add(container.getItem(slot).copy());
      }
      for (ItemStack stack : payload) {
         ItemStack remainder = stack.copy();
         for (int slot = 0; slot < slots.size() && !remainder.isEmpty(); slot++) {
            ItemStack existing = slots.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remainder) && container.canPlaceItem(slot, remainder)) {
               int limit = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
               int moved = existing.getCount() >= limit ? 0 : Math.min(remainder.getCount(), limit - existing.getCount());
               if (moved > 0) {
                  existing.grow(moved);
                  remainder.shrink(moved);
               }
            }
         }
         for (int slot = 0; slot < slots.size() && !remainder.isEmpty(); slot++) {
            if (!slots.get(slot).isEmpty() || !container.canPlaceItem(slot, remainder)) {
               continue;
            }
            int moved = Math.min(remainder.getCount(), Math.min(remainder.getMaxStackSize(), container.getMaxStackSize()));
            slots.set(slot, remainder.copyWithCount(moved));
            remainder.shrink(moved);
         }
         if (!remainder.isEmpty()) {
            return Optional.empty();
         }
      }
      return Optional.of(slots);
   }

   private static List<StorageNode> storageNodes(Level level, BlockPos origin, String networkId) {
      return storageNodes(level, origin, networkId, null, null);
   }

   private static List<StorageNode> storageNodes(Level level, BlockPos origin, String networkId,
                                                 List<LogisticsBlockEntity> scannedBlocks,
                                                 List<LogisticsExternalEndpoint> scannedEndpoints) {
      int radius = EchoRuntimeModules.isLoaded("echoindustrialnexus") ? INDUSTRIAL_RADIUS : BASE_RADIUS;
      Map<BlockPos, StorageNode> nodes = new LinkedHashMap<>();
      List<LogisticsBlockEntity> logisticsBlocks = new ArrayList<>();
      if (scannedBlocks == null) {
         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -8, -radius), origin.offset(radius, 8, radius))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LogisticsBlockEntity logistics && networkMatches(networkId, logistics.networkId())) {
               logisticsBlocks.add(logistics);
               addStorageNode(level, nodes, logistics);
            }
         }
      } else {
         for (LogisticsBlockEntity logistics : scannedBlocks) {
            if (logistics != null && networkMatches(networkId, logistics.networkId())) {
               logisticsBlocks.add(logistics);
               addStorageNode(level, nodes, logistics);
            }
         }
      }
      if (EchoRuntimeModules.isLoaded("echoindustrialnexus")) {
         addIndustrialDuctNodes(level, logisticsBlocks, nodes);
      }
      addExternalStorageNodes(level, origin, networkId, nodes, scannedEndpoints);
      return nodes.values().stream().sorted(Comparator.comparing(node -> node.pos().toShortString())).toList();
   }

   private static void addExternalStorageNodes(Level level, BlockPos origin, String networkId, Map<BlockPos, StorageNode> nodes) {
      addExternalStorageNodes(level, origin, networkId, nodes, null);
   }

   private static void addStorageNode(Level level, Map<BlockPos, StorageNode> nodes, LogisticsBlockEntity logistics) {
      BlockPos pos = logistics.getBlockPos();
      if (logistics.kind() == LogisticsKind.SMART_STORAGE_LABEL) {
         for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (level.getBlockEntity(adjacent) instanceof Container container && !(container instanceof LogisticsBlockEntity)) {
               nodes.putIfAbsent(adjacent, new StorageNode(container, adjacent, logistics.categoryId()));
            }
         }
      } else if (logistics.storageNode()) {
         nodes.putIfAbsent(pos, new StorageNode(logistics, pos, logistics.categoryId()));
      }
   }

   private static void addExternalStorageNodes(Level level, BlockPos origin, String networkId,
                                               Map<BlockPos, StorageNode> nodes,
                                               List<LogisticsExternalEndpoint> scannedEndpoints) {
      List<LogisticsExternalEndpoint> endpoints = scannedEndpoints == null
         ? externalEndpoints(level, origin, networkId)
         : scannedEndpoints;
      for (LogisticsExternalEndpoint endpoint : endpoints) {
         if (!endpoint.hasRole(LogisticsExternalEndpointRole.STORAGE) || !networkMatches(networkId, endpoint.networkId())) {
            continue;
         }
         if (level.getBlockEntity(endpoint.pos()) instanceof Container container && !(container instanceof LogisticsBlockEntity)) {
            String category = endpoint.categoryId() == null ? "" : endpoint.categoryId().toString();
            nodes.putIfAbsent(endpoint.pos(), new StorageNode(container, endpoint.pos(), category));
         }
      }
   }

   private static List<StorageNode> externalDeliveryNodes(Level level, BlockPos origin, String networkId) {
      return externalEndpoints(level, origin, networkId).stream()
         .filter(endpoint -> endpoint.hasRole(LogisticsExternalEndpointRole.DELIVERY_TARGET)
            && networkMatches(networkId, endpoint.networkId()))
         .map(endpoint -> externalStorageNode(level, endpoint))
         .flatMap(Optional::stream)
         .sorted(Comparator.comparing(node -> node.pos().toShortString()))
         .toList();
   }

   private static Optional<StorageNode> externalStorageNode(Level level, LogisticsExternalEndpoint endpoint) {
      if (level == null || endpoint == null) {
         return Optional.empty();
      }
      if (level.getBlockEntity(endpoint.pos()) instanceof Container container && !(container instanceof LogisticsBlockEntity)) {
         String category = endpoint.categoryId() == null ? "" : endpoint.categoryId().toString();
         return Optional.of(new StorageNode(container, endpoint.pos(), category));
      }
      return Optional.empty();
   }

   private static List<LogisticsExternalEndpoint> externalEndpoints(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null || EXTERNAL_ENDPOINT_PROVIDERS.isEmpty()) {
         return List.of();
      }
      Map<ExternalEndpointKey, LogisticsExternalEndpoint> endpoints = new LinkedHashMap<>();
      for (LogisticsExternalEndpointProvider provider : EXTERNAL_ENDPOINT_PROVIDERS) {
         for (LogisticsExternalEndpoint endpoint : safeExternalEndpoints(provider, level, origin, networkId)) {
            endpoints.putIfAbsent(ExternalEndpointKey.from(endpoint), endpoint);
         }
      }
      return List.copyOf(endpoints.values());
   }

   private static List<LogisticsExternalEndpoint> safeExternalEndpoints(LogisticsExternalEndpointProvider provider, Level level, BlockPos origin, String networkId) {
      try {
         List<LogisticsExternalEndpoint> provided = provider.endpoints(level, origin, normalizeNetworkId(networkId));
         if (provided == null) {
            return List.of();
         }
         return provided.stream()
            .filter(endpoint -> endpoint != null && networkMatches(networkId, endpoint.networkId()))
            .toList();
      } catch (RuntimeException exception) {
         com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork.LOGGER.warn(
            "Logistics external endpoint provider {} failed near {}.",
            provider.providerId(),
            origin,
            exception
         );
         return List.of();
      }
   }

   private static void addIndustrialDuctNodes(Level level, List<LogisticsBlockEntity> logisticsBlocks, Map<BlockPos, StorageNode> nodes) {
      if (level == null || logisticsBlocks.isEmpty()) {
         return;
      }
      Set<BlockPos> seedDucts = new LinkedHashSet<>();
      for (LogisticsBlockEntity logistics : logisticsBlocks) {
         collectAdjacentIndustrialItemDucts(level, logistics.getBlockPos(), seedDucts);
      }
      for (BlockPos storagePos : List.copyOf(nodes.keySet())) {
         collectAdjacentIndustrialItemDucts(level, storagePos, seedDucts);
      }
      if (seedDucts.isEmpty()) {
         return;
      }
      Set<BlockPos> visited = new LinkedHashSet<>();
      Queue<BlockPos> queue = new ArrayDeque<>(seedDucts);
      while (!queue.isEmpty() && visited.size() < INDUSTRIAL_DUCT_LIMIT) {
         BlockPos ductPos = queue.remove().immutable();
         if (!visited.add(ductPos) || !isIndustrialItemDuct(level, ductPos)) {
            continue;
         }
         for (Direction direction : Direction.values()) {
            BlockPos neighbor = ductPos.relative(direction);
            if (isIndustrialItemDuct(level, neighbor)) {
               if (!visited.contains(neighbor)) {
                  queue.add(neighbor.immutable());
               }
               continue;
            }
            if (level.getBlockEntity(neighbor) instanceof Container container && !(container instanceof LogisticsBlockEntity)) {
               nodes.putIfAbsent(neighbor.immutable(), new StorageNode(container, neighbor.immutable(), ""));
            }
         }
      }
   }

   private static void collectAdjacentIndustrialItemDucts(Level level, BlockPos pos, Set<BlockPos> ductPositions) {
      if (pos == null || ductPositions == null) {
         return;
      }
      for (Direction direction : Direction.values()) {
         BlockPos adjacent = pos.relative(direction);
         if (isIndustrialItemDuct(level, adjacent)) {
            ductPositions.add(adjacent.immutable());
         }
      }
   }

   private static boolean isIndustrialItemDuct(Level level, BlockPos pos) {
      if (level == null || pos == null) {
         return false;
      }
      Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
      return "echoindustrialnexus".equals(id.getNamespace()) && id.getPath().endsWith("_duct") && !id.getPath().contains("flux");
   }

   private static LogisticsBlockEntity nearestDock(ServerLevel level, BlockPos origin, String networkId) {
      LogisticsBlockEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      int radius = EchoRuntimeModules.isLoaded("echoindustrialnexus") ? INDUSTRIAL_RADIUS : BASE_RADIUS;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -8, -radius), origin.offset(radius, 8, radius))) {
         if (level.getBlockEntity(pos) instanceof LogisticsBlockEntity logistics
            && logistics.kind() == LogisticsKind.DRONE_DELIVERY_DOCK
            && networkMatches(networkId, logistics.networkId())) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
               best = logistics;
               bestDistance = distance;
            }
         }
      }
      return best;
   }

   private static List<FactionDepotOffer> selectedDepotOffers(LogisticsBlockEntity depot) {
      String selected = depot.depotOfferId();
      List<FactionDepotOffer> offers = LogisticsContent.offers();
      if (selected.isBlank()) {
         return offers;
      }
      return offers.stream().filter(offer -> offer.id().toString().equals(selected)).toList();
   }

   private static String networkIdAt(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof LogisticsBlockEntity logistics ? logistics.networkId() : "global";
   }

   private static String nearestNetworkId(Level level, BlockPos origin) {
      if (level == null || origin == null) {
         return "global";
      }
      if (level.getBlockEntity(origin) instanceof LogisticsBlockEntity logistics) {
         return normalizeNetworkId(logistics.networkId());
      }
      int radius = EchoRuntimeModules.isLoaded("echoindustrialnexus") ? INDUSTRIAL_RADIUS : BASE_RADIUS;
      LogisticsBlockEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -Y_RADIUS, -radius), origin.offset(radius, Y_RADIUS, radius))) {
         if (level.getBlockEntity(pos) instanceof LogisticsBlockEntity logistics) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
               best = logistics;
               bestDistance = distance;
            }
         }
      }
      return best == null ? "global" : normalizeNetworkId(best.networkId());
   }

   private static boolean networkMatches(String expected, String actual) {
      String a = normalizeNetworkId(expected);
      String b = normalizeNetworkId(actual);
      return a.equals(b);
   }

   private static String normalizeNetworkId(String networkId) {
      return networkId == null || networkId.isBlank() ? "global" : networkId;
   }

   private static void message(Player player, String line) {
      if (player != null && line != null && !line.isBlank()) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line));
      }
   }

   private static int countCategory(List<StorageNode> nodes, SupplyCategory category) {
      int count = 0;
      for (StorageNode node : nodes) {
         for (int slot = 0; slot < node.container().getContainerSize(); slot++) {
            ItemStack stack = node.container().getItem(slot);
            if (category.matches(stack) && nodeAllows(node, category.id())) {
               count += stack.getCount();
            }
         }
      }
      return count;
   }

   private static LoadoutReadiness readiness(List<StorageNode> nodes, LoadoutPreset preset) {
      int required = 0;
      for (LoadoutRequirement requirement : preset.requirements()) {
         if (!requirement.optional()) {
            required += requirement.count();
         }
      }
      int missing = planLoadout(nodes, preset).missingRequired();
      return new LoadoutReadiness(preset.id(), preset.title(), missing <= 0, required, missing);
   }

   private static ExtractionPlan planLoadout(List<StorageNode> nodes, LoadoutPreset preset) {
      List<ItemStack> payload = new ArrayList<>();
      List<ExtractionMove> moves = new ArrayList<>();
      Map<SlotKey, ItemStack> simulated = new LinkedHashMap<>();
      int missingRequired = 0;
      for (LoadoutRequirement requirement : preset.requirements()) {
         int remaining = requirement.count();
         for (StorageNode node : nodes) {
            for (int slot = 0; slot < node.container().getContainerSize() && remaining > 0; slot++) {
               SlotKey key = new SlotKey(node, slot);
               ItemStack stack = simulated.get(key);
               if (stack == null) {
                  stack = node.container().getItem(slot).copy();
                  simulated.put(key, stack);
               }
               if (stack.isEmpty() || !requirement.matches(stack) || !nodeAllows(node, requirement)) {
                  continue;
               }
               int moved = Math.min(remaining, stack.getCount());
               ItemStack extracted = stack.copyWithCount(moved);
               mergePayload(payload, extracted.copy());
               moves.add(new ExtractionMove(node, slot, extracted.copy(), moved));
               stack.shrink(moved);
               if (stack.isEmpty()) {
                  simulated.put(key, ItemStack.EMPTY);
               }
               remaining -= moved;
            }
         }
         if (remaining > 0 && !requirement.optional()) {
            missingRequired += remaining;
         }
      }
      if (missingRequired > 0) {
         return new ExtractionPlan(List.of(), List.of(), missingRequired);
      }
      return new ExtractionPlan(payload, moves, 0);
   }

   private static List<ItemStack> extractPlannedLoadout(ExtractionPlan plan) {
      if (!plan.ready() || plan.moves().isEmpty()) {
         return List.of();
      }
      List<ExtractionMove> applied = new ArrayList<>();
      for (ExtractionMove move : plan.moves()) {
         Container container = move.node().container();
         ItemStack current = container.getItem(move.slot());
         if (!ItemStack.isSameItemSameComponents(current, move.stack()) || current.getCount() < move.count()) {
            rollback(applied);
            return List.of();
         }
         current.shrink(move.count());
         if (current.isEmpty()) {
            container.setItem(move.slot(), ItemStack.EMPTY);
         }
         container.setChanged();
         applied.add(move);
      }
      return plan.payload().stream().map(ItemStack::copy).toList();
   }

   private static void rollback(List<ExtractionMove> applied) {
      for (int i = applied.size() - 1; i >= 0; i--) {
         ExtractionMove move = applied.get(i);
         Container container = move.node().container();
         ItemStack current = container.getItem(move.slot());
         if (current.isEmpty()) {
            container.setItem(move.slot(), move.stack().copyWithCount(move.count()));
         } else if (ItemStack.isSameItemSameComponents(current, move.stack())) {
            current.grow(move.count());
         } else {
            insertPayloadIntoContainer(container, List.of(move.stack().copyWithCount(move.count())));
         }
         container.setChanged();
      }
   }

   private static boolean nodeAllows(StorageNode node, LoadoutRequirement requirement) {
      if (node.categoryId().isBlank()) {
         return true;
      }
      return requirement.kind() != LoadoutRequirement.Kind.CATEGORY || requirement.target().toString().equals(node.categoryId());
   }

   private static boolean nodeAllows(StorageNode node, Identifier categoryId) {
      return node.categoryId().isBlank() || node.categoryId().equals(categoryId.toString());
   }

   private static void mergePayload(List<ItemStack> payload, ItemStack addition) {
      for (ItemStack existing : payload) {
         if (ItemStack.isSameItemSameComponents(existing, addition) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(addition.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            addition.shrink(moved);
         }
      }
      if (!addition.isEmpty()) {
         payload.add(addition.copy());
      }
   }

   private static boolean removeStack(Container container, ItemStack wanted) {
      if (wanted.isEmpty()) {
         return true;
      }
      int available = 0;
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
         ItemStack stack = container.getItem(slot);
         if (ItemStack.isSameItemSameComponents(stack, wanted)) {
            available += stack.getCount();
         }
      }
      if (available < wanted.getCount()) {
         return false;
      }
      int remaining = wanted.getCount();
      for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
         ItemStack stack = container.getItem(slot);
         if (!ItemStack.isSameItemSameComponents(stack, wanted)) {
            continue;
         }
         int removed = Math.min(remaining, stack.getCount());
         stack.shrink(removed);
         if (stack.isEmpty()) {
            container.setItem(slot, ItemStack.EMPTY);
         }
         remaining -= removed;
      }
      container.setChanged();
      return true;
   }

   private static boolean insertStack(Container container, ItemStack stack) {
      return insertPayloadIntoContainer(container, List.of(stack));
   }

   private static String shortJob(UUID jobId) {
      return jobId.toString().substring(0, 8);
   }

   private static String ticks(int ticks) {
      int safeTicks = Math.max(0, ticks);
      if (safeTicks < 20) {
         return safeTicks + "t";
      }
      int seconds = Math.round(safeTicks / 20.0F);
      return safeTicks + "t (~" + seconds + "s)";
   }

   private static String signed(int value) {
      return value > 0 ? "+" + value : String.valueOf(value);
   }

   private record SnapshotScanKey(int levelIdentity, String dimension, int regionX, int regionY, int regionZ,
                                  String networkId, boolean industrialLoaded) {
      private static SnapshotScanKey of(Level level, BlockPos origin, String networkId) {
         return new SnapshotScanKey(
            System.identityHashCode(level),
            level.dimension().identifier().toString(),
            origin.getX() >> 4,
            origin.getY() >> 4,
            origin.getZ() >> 4,
            normalizeNetworkId(networkId),
            EchoRuntimeModules.isLoaded("echoindustrialnexus")
         );
      }
   }

   private record CachedSnapshotScan(long createdTick,
                                     List<LogisticsBlockEntity> logisticsBlocks,
                                     List<StorageNode> storageNodes,
                                     List<LogisticsExternalEndpoint> externalEndpoints,
                                     List<DeliveryJob> deliveryJobs) {
      private CachedSnapshotScan {
         logisticsBlocks = List.copyOf(logisticsBlocks == null ? List.of() : logisticsBlocks);
         storageNodes = List.copyOf(storageNodes == null ? List.of() : storageNodes);
         externalEndpoints = List.copyOf(externalEndpoints == null ? List.of() : externalEndpoints);
         deliveryJobs = List.copyOf(deliveryJobs == null ? List.of() : deliveryJobs);
      }
   }

   private record StorageNode(Container container, BlockPos pos, String categoryId) {
      private StorageNode {
         categoryId = categoryId == null ? "" : categoryId;
      }
   }

   private record ExternalEndpointKey(BlockPos pos,
                                      String networkId,
                                      Identifier categoryId,
                                      Identifier loadoutId,
                                      Set<LogisticsExternalEndpointRole> roles) {
      private static ExternalEndpointKey from(LogisticsExternalEndpoint endpoint) {
         return new ExternalEndpointKey(
            endpoint.pos(),
            normalizeNetworkId(endpoint.networkId()),
            endpoint.categoryId(),
            endpoint.loadoutId(),
            endpoint.roles()
         );
      }
   }

   private record FactoryDepotCandidate(StorageNode node, int currentRuns, int inFlightRuns) {
   }

   public record FactoryDispatchResult(boolean dispatched, String message, String networkId, String loadoutId, BlockPos targetPos) {
      public FactoryDispatchResult {
         message = message == null || message.isBlank() ? "No dispatch status available." : message.strip();
         networkId = normalizeNetworkId(networkId);
         loadoutId = loadoutId == null ? "" : loadoutId;
         targetPos = targetPos == null ? BlockPos.ZERO : targetPos.immutable();
      }
   }

   public record FactoryRestockStatus(boolean eligible, boolean dispatched, String message, String networkId,
                                      String loadoutId, BlockPos targetPos, int currentRuns, int targetRuns,
                                      int minRuns, int inFlightRuns, int cooldownTicks) {
      public FactoryRestockStatus {
         message = message == null || message.isBlank() ? "No factory restock status available." : message.strip();
         networkId = normalizeNetworkId(networkId);
         loadoutId = loadoutId == null ? "" : loadoutId;
         targetPos = targetPos == null ? BlockPos.ZERO : targetPos.immutable();
         currentRuns = Math.max(0, currentRuns);
         targetRuns = Math.max(0, targetRuns);
         minRuns = Math.max(0, minRuns);
         inFlightRuns = Math.max(0, inFlightRuns);
         cooldownTicks = Math.max(0, cooldownTicks);
      }

      public static FactoryRestockStatus blocked(String message, String networkId, String loadoutId, BlockPos targetPos) {
         return new FactoryRestockStatus(false, false, message, networkId, loadoutId, targetPos, 0, 0, 0, 0, 0);
      }
   }

   private record SlotKey(StorageNode node, int slot) {
   }

   private record ExtractionMove(StorageNode node, int slot, ItemStack stack, int count) {
      private ExtractionMove {
         stack = stack.copyWithCount(count);
      }
   }

   private record ExtractionPlan(List<ItemStack> payload, List<ExtractionMove> moves, int missingRequired) {
      private ExtractionPlan {
         payload = payload == null ? List.of() : payload.stream().map(ItemStack::copy).toList();
         moves = List.copyOf(moves == null ? List.of() : moves);
         missingRequired = Math.max(0, missingRequired);
      }

      private boolean ready() {
         return missingRequired <= 0 && !payload.isEmpty();
      }
   }

   public record StockRow(Identifier categoryId, String title, int count, int lowStockTarget, int accentColor) {
   }

   public record MissingRow(Identifier categoryId, String title, int missing, int accentColor) {
   }

   public record LoadoutReadiness(Identifier presetId, String title, boolean ready, int requiredCount, int missingCount) {
   }

   public record DeliveryJob(UUID id, UUID owner, BlockPos sourceDock, BlockPos targetPos, Identifier presetId, List<ItemStack> payload, String status, long createdTick, long etaTick) {
      public DeliveryJob {
         payload = payload == null ? List.of() : payload.stream().map(ItemStack::copy).toList();
      }
   }

   public record EndpointRef(LogisticsKind kind, BlockPos pos, String loadoutId) {
      public EndpointRef {
         kind = kind == null ? LogisticsKind.LOGISTICS_TERMINAL : kind;
         pos = pos == null ? BlockPos.ZERO : pos.immutable();
         loadoutId = loadoutId == null ? "" : loadoutId;
      }
   }

   public record LogisticsSnapshot(
      String networkId,
      List<StockRow> stockRows,
      List<MissingRow> missingRows,
      List<LoadoutReadiness> loadoutReadiness,
      int activeDeliveries,
      List<DeliveryJob> deliveryJobs,
      List<FactionDepotOffer> depotOffers,
      int blockCount,
      int endpointCount,
      boolean dockOnline,
      boolean relayOnline,
      boolean depotOnline,
      int depotCooldown,
      EndpointRef selectedEndpoint,
      String selectedLoadoutId,
      String selectedLoadoutTitle,
      boolean selectedReady,
      int selectedMissing,
      String requestPayload,
      FactoryRestockStatus factoryRestock
   ) {
      public LogisticsSnapshot(String networkId, List<StockRow> stockRows, List<MissingRow> missingRows, List<LoadoutReadiness> loadoutReadiness, int activeDeliveries, List<FactionDepotOffer> depotOffers) {
         this(networkId, stockRows, missingRows, loadoutReadiness, activeDeliveries, List.of(), depotOffers);
      }

      public LogisticsSnapshot(String networkId, List<StockRow> stockRows, List<MissingRow> missingRows, List<LoadoutReadiness> loadoutReadiness, int activeDeliveries, List<DeliveryJob> deliveryJobs, List<FactionDepotOffer> depotOffers) {
         this(networkId, stockRows, missingRows, loadoutReadiness, activeDeliveries, deliveryJobs, depotOffers,
            0, 0, false, false, false, 0, null, "", "None", false, 0, "", FactoryRestockStatus.blocked("No factory restock status.", networkId, "", BlockPos.ZERO));
      }

      public LogisticsSnapshot {
         networkId = networkId == null || networkId.isBlank() ? "global" : networkId;
         stockRows = List.copyOf(stockRows == null ? List.of() : stockRows);
         missingRows = List.copyOf(missingRows == null ? List.of() : missingRows);
         loadoutReadiness = List.copyOf(loadoutReadiness == null ? List.of() : loadoutReadiness);
         deliveryJobs = List.copyOf(deliveryJobs == null ? List.of() : deliveryJobs);
         depotOffers = List.copyOf(depotOffers == null ? List.of() : depotOffers);
         blockCount = Math.max(0, blockCount);
         endpointCount = Math.max(0, endpointCount);
         depotCooldown = Math.max(0, depotCooldown);
         selectedLoadoutId = selectedLoadoutId == null ? "" : selectedLoadoutId;
         selectedLoadoutTitle = selectedLoadoutTitle == null || selectedLoadoutTitle.isBlank() ? "None" : selectedLoadoutTitle;
         selectedMissing = Math.max(0, selectedMissing);
         requestPayload = requestPayload == null ? "" : requestPayload;
         factoryRestock = factoryRestock == null
            ? FactoryRestockStatus.blocked("No factory restock status.", networkId, selectedLoadoutId, BlockPos.ZERO)
            : factoryRestock;
      }

      public static LogisticsSnapshot empty(String networkId) {
         return new LogisticsSnapshot(networkId, List.of(), List.of(), List.of(), 0, List.of(), List.of());
      }

      public boolean canRequest() {
         return selectedEndpoint != null && !selectedLoadoutId.isBlank();
      }

      public boolean canDispatch() {
         return canRequest() && dockOnline && selectedReady;
      }

      public String requestBlockedReason() {
         if (selectedEndpoint == null) {
            return "No eligible Loadout Locker, Route Requester, or Auto-Restock Station is in range.";
         }
         if (selectedLoadoutId.isBlank()) {
            return "No loadout preset is selected.";
         }
         return "";
      }

      public String dispatchBlockedReason() {
         String requestReason = requestBlockedReason();
         if (!requestReason.isBlank()) {
            return requestReason;
         }
         if (!dockOnline) {
            return "No Drone Delivery Dock is online for this network.";
         }
         if (!selectedReady) {
            return selectedMissing > 0
               ? "Selected loadout is missing " + selectedMissing + " required item(s)."
               : "Selected loadout is not ready.";
         }
         return "";
      }
   }
}

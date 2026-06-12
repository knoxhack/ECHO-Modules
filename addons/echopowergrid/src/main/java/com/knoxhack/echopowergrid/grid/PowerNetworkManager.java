package com.knoxhack.echopowergrid.grid;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.ISoundService;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.api.EchoGridState;
import com.knoxhack.echopowergrid.api.EchoPowerNetwork;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.api.EchoPowerQuality;
import com.knoxhack.echopowergrid.api.PowerGridAlert;
import com.knoxhack.echopowergrid.api.PowerGridAlertLevel;
import com.knoxhack.echopowergrid.api.PowerGridDrawResult;
import com.knoxhack.echopowergrid.api.PowerGridNetworkSummary;
import com.knoxhack.echopowergrid.api.PowerGridNodeSummary;
import com.knoxhack.echopowergrid.api.PowerGridRouteSummary;
import com.knoxhack.echopowergrid.block.BreakerBlock;
import com.knoxhack.echopowergrid.block.CableBlock;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.SubstationBlockEntity;
import com.knoxhack.echopowergrid.config.PowerGridConfig;
import com.knoxhack.echopowergrid.integration.runtimeguard.PowerGridRuntimeGuardIntegration;
import com.knoxhack.echopowergrid.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PowerNetworkManager {
    private static final Map<ResourceKey<Level>, PowerNetworkManager> MANAGERS = new HashMap<>();
    private static final Identifier SOUND_BROWNOUT = Identifier.fromNamespaceAndPath("echosoundcore", "powergrid.brownout");
    private static final Identifier SOUND_OVERLOAD = Identifier.fromNamespaceAndPath("echosoundcore", "powergrid.overload.warning");
    private static final Identifier SOUND_BREAKER_TRIP = Identifier.fromNamespaceAndPath("echosoundcore", "powergrid.breaker.trip");
    private static final Identifier SOUND_POWER_RESTORED = Identifier.fromNamespaceAndPath("echosoundcore", "powergrid.power_restored");

    private final ServerLevel level;
    private final Map<BlockPos, EchoPowerNetwork> posToNetwork = new HashMap<>();
    private final Map<UUID, EchoPowerNetwork> networks = new HashMap<>();
    private final Set<BlockPos> dirtyPositions = new HashSet<>();
    private final Deque<EchoPowerNetwork> rebuildQueue = new ArrayDeque<>();
    private int tickCounter = 0;
    private int networkUpdateCursor = 0;

    private PowerNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static PowerNetworkManager get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return MANAGERS.computeIfAbsent(serverLevel.dimension(), k -> new PowerNetworkManager(serverLevel));
        }
        // Client fallback - return a no-op manager
        return new PowerNetworkManager(null) {
            @Override
            public void tick() {}
        };
    }

    public static void tickAll(MinecraftServer server) {
        if (!PowerGridConfig.ENABLED.get()) return;
        for (ServerLevel level : server.getAllLevels()) {
            get(level).tick();
        }
    }

    public static void clearAll() {
        MANAGERS.clear();
    }

    public void tick() {
        if (level == null) return;
        tickCounter++;

        // Rebuild dirty networks in batches
        int batchSize = PowerGridConfig.NETWORK_REBUILD_BATCH_SIZE.get();
        if (!dirtyPositions.isEmpty() && canRunRuntimeGuardWork("dirty_rebuild", batchSize)) {
            profileRuntimeGuardWork("dirty_rebuild", () -> rebuildDirtyNetworks(batchSize));
        }

        // Process rebuild queue
        int processed = 0;
        while (!rebuildQueue.isEmpty() && processed < batchSize) {
            EchoPowerNetwork net = rebuildQueue.poll();
            if (net != null && networks.containsKey(net.networkId)) {
                if (!canRunRuntimeGuardWork("queued_rebuild", net.size())) {
                    rebuildQueue.addFirst(net);
                    break;
                }
                EchoPowerNetwork network = net;
                profileRuntimeGuardWork("queued_rebuild", () -> rebuildNetwork(network));
                processed++;
            }
        }

        // Update networks on interval
        int interval = PowerGridConfig.NETWORK_UPDATE_INTERVAL_TICKS.get();
        if (tickCounter % interval == 0 && canRunRuntimeGuardWork("network_update", Math.max(1, networks.size()))) {
            profileRuntimeGuardWork("network_update", this::updateNetworks);
        }

        // Increment overload grace every tick for overloaded networks
        for (EchoPowerNetwork network : networks.values()) {
            if (network.overloaded) {
                network.overloadGraceTicks++;
            }
        }
    }

    public EchoPowerNetwork getNetworkAt(BlockPos pos) {
        return posToNetwork.get(pos);
    }


    // Debug helper for tests
    public String debugNetworkState(BlockPos pos) {
        EchoPowerNetwork net = posToNetwork.get(pos);
        if (net == null) return "no network at " + pos;
        return "net=" + net.networkId + " nodes=" + net.size() + " dirty=" + net.dirty
                + " gen=" + net.totalGeneration + " demand=" + net.totalDemand
                + " stored=" + net.totalStored + " state=" + net.state;
    }
    public int getNetworkCount() {
        return networks.size();
    }

    private static boolean canRunRuntimeGuardWork(String workId, int cost) {
        return !EchoRuntimeModules.isLoaded("echoruntimeguard")
                || PowerGridRuntimeGuardIntegration.canRunGridWork(workId, cost);
    }

    private static void profileRuntimeGuardWork(String workId, Runnable task) {
        if (EchoRuntimeModules.isLoaded("echoruntimeguard")) {
            PowerGridRuntimeGuardIntegration.profile(workId, task);
        } else if (task != null) {
            task.run();
        }
    }

    public Optional<EchoEnergyStorage> getEnergyStorageAt(BlockPos pos) {
        if (level == null) return Optional.empty();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EchoEnergyStorage storage) {
            return Optional.of(storage);
        }
        return Optional.empty();
    }

    public boolean requestPower(BlockPos pos, long epPerTick) {
        return drawPower(pos, epPerTick, true).satisfied();
    }

    public PowerGridDrawResult drawPower(BlockPos pos, long ep, boolean simulate) {
        if (level == null || pos == null || ep <= 0L) {
            return PowerGridDrawResult.empty(ep, simulate);
        }
        EchoPowerNetwork network = ensureNetworkReadyForDraw(pos);
        if (network == null || network.isEmpty()) {
            return PowerGridDrawResult.empty(ep, simulate);
        }
        return drawPowerFromNetwork(network, pos, ep, simulate);
    }

    private EchoPowerNetwork ensureNetworkReadyForDraw(BlockPos pos) {
        EchoPowerNetwork network = posToNetwork.get(pos);
        if (network != null && !network.dirty) {
            return network;
        }
        if (!dirtyPositions.contains(pos) && network == null) {
            return null;
        }
        if (!canRunRuntimeGuardWork("draw_rebuild", network == null ? 1 : network.size())) {
            return network;
        }
        profileRuntimeGuardWork("draw_rebuild", () -> rebuildDirtyPosition(pos));
        network = posToNetwork.get(pos);
        if (network != null && network.dirty && canRunRuntimeGuardWork("draw_rebuild_network", network.size())) {
            EchoPowerNetwork dirtyNetwork = network;
            profileRuntimeGuardWork("draw_rebuild_network", () -> rebuildNetwork(dirtyNetwork));
            network = posToNetwork.get(pos);
        }
        return network;
    }

    public List<PowerGridNetworkSummary> loadedNetworkSummaries() {
        if (level == null) {
            return List.of();
        }
        return networks.values().stream()
                .filter(network -> network != null && !network.isEmpty())
                .sorted(Comparator
                        .comparing((EchoPowerNetwork network) -> network.dimension.identifier().toString())
                        .thenComparing(network -> network.networkId.toString()))
                .map(network -> new PowerGridNetworkSummary(
                        network.networkId,
                        network.dimension,
                        anchorFor(network),
                        network.state,
                        network.quality,
                        network.totalGeneration,
                        network.totalDemand,
                        drawPowerFromNetwork(network, anchorFor(network), Long.MAX_VALUE / 8L, true).drawn(),
                        network.totalStored,
                        network.totalCapacity,
                        network.size(),
                        network.transferLimit))
                .toList();
    }

    public List<PowerGridNodeSummary> loadedNodeSummaries() {
        if (level == null) {
            return List.of();
        }
        List<PowerGridNodeSummary> summaries = new ArrayList<>();
        for (EchoPowerNetwork network : networks.values()) {
            if (network == null || network.isEmpty()) {
                continue;
            }
            for (BlockPos pos : network.getNodes()) {
                PowerGridNodeSummary summary = nodeSummary(pos);
                if (summary != null) {
                    summaries.add(summary);
                }
            }
        }
        summaries.sort(Comparator
                .comparing((PowerGridNodeSummary summary) -> summary.dimension().identifier().toString())
                .thenComparing(PowerGridNodeSummary::pos));
        return List.copyOf(summaries);
    }

    public Optional<PowerGridNetworkSummary> networkSummaryAt(BlockPos pos) {
        EchoPowerNetwork network = posToNetwork.get(pos);
        if (network == null || network.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PowerGridNetworkSummary(
                network.networkId,
                network.dimension,
                anchorFor(network),
                network.state,
                network.quality,
                network.totalGeneration,
                network.totalDemand,
                drawPowerFromNetwork(network, pos, Long.MAX_VALUE / 8L, true).drawn(),
                network.totalStored,
                network.totalCapacity,
                network.size(),
                network.transferLimit));
    }

    public List<PowerGridAlert> alerts() {
        if (level == null) {
            return List.of();
        }
        List<PowerGridAlert> result = new ArrayList<>();
        for (EchoPowerNetwork network : networks.values()) {
            if (network == null || network.isEmpty()) {
                continue;
            }
            BlockPos anchor = anchorFor(network);
            if (network.state == EchoGridState.BROWNOUT) {
                result.add(new PowerGridAlert(network.networkId, network.dimension, anchor, PowerGridAlertLevel.WARNING,
                        "brownout", "Demand exceeds delivered EP; lower-priority consumers may slow or pause."));
            } else if (network.state == EchoGridState.OVERLOADED) {
                result.add(new PowerGridAlert(network.networkId, network.dimension, anchor, PowerGridAlertLevel.DANGER,
                        "overload", "Demand exceeds path transfer limits; breakers may trip."));
            } else if (network.state == EchoGridState.TRIPPED) {
                result.add(new PowerGridAlert(network.networkId, network.dimension, anchor, PowerGridAlertLevel.DANGER,
                        "tripped", "A breaker isolated this network."));
            }
            if (network.quality != EchoPowerQuality.STABLE) {
                result.add(new PowerGridAlert(network.networkId, network.dimension, anchor, PowerGridAlertLevel.WARNING,
                        "quality_" + network.quality.name().toLowerCase(java.util.Locale.ROOT),
                        "Network quality is " + network.quality.name() + "."));
            }
            for (BlockPos pos : network.getNodes()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BreakerBlock breaker && breaker.isTripped(state)) {
                    result.add(new PowerGridAlert(network.networkId, network.dimension, pos, PowerGridAlertLevel.DANGER,
                            "breaker_tripped", "Emergency breaker is tripped and blocks traversal."));
                }
            }
        }
        return List.copyOf(result);
    }

    public PowerGridRouteSummary routeSummary(BlockPos from, BlockPos to, long requestedEp) {
        if (level == null || from == null || to == null) {
            return PowerGridRouteSummary.blocked(from, to, requestedEp, "Missing route endpoint.");
        }
        RouteInfo route = findRoute(from, to, Math.max(0L, requestedEp));
        if (route.blocked()) {
            return PowerGridRouteSummary.blocked(from, to, requestedEp, route.blockedReason());
        }
        long limited = Math.min(Math.max(0L, requestedEp), route.transferLimit());
        long loss = lossFor(limited, route.distance());
        long deliverable = Math.max(0L, limited - loss);
        return new PowerGridRouteSummary(from, to, route.distance(), route.transferLimit(), lossPercent(route.distance()),
                requestedEp, loss, deliverable, false, "");
    }

    public void markDirty(BlockPos pos) {
        dirtyPositions.add(pos);
        EchoPowerNetwork net = posToNetwork.get(pos);
        if (net != null) {
            net.dirty = true;
        }
    }

    public void onBlockPlaced(BlockPos pos) {
        markDirty(pos);
        for (BlockPos neighbor : getNeighbors(pos)) {
            markDirty(neighbor);
        }
    }

    public void onBlockRemoved(BlockPos pos) {
        EchoPowerNetwork net = posToNetwork.remove(pos);
        if (net != null) {
            net.removeNode(pos);
            net.dirty = true;
            rebuildQueue.add(net);
        }
        for (BlockPos neighbor : getNeighbors(pos)) {
            markDirty(neighbor);
        }
    }

    private void rebuildDirtyNetworks(int batchSize) {
        int processed = 0;
        Iterator<BlockPos> it = dirtyPositions.iterator();
        while (it.hasNext() && processed < batchSize * 4) {
            BlockPos pos = it.next();
            it.remove();
            processed++;

            rebuildDirtyPosition(pos);
        }
    }

    private void rebuildDirtyPosition(BlockPos pos) {
        if (pos == null) {
            return;
        }
        dirtyPositions.remove(pos);
        EchoPowerNetwork existing = posToNetwork.get(pos);
        if (existing != null) {
            mergeNeighborNetworks(pos, existing);
            existing.dirty = true;
            rebuildQueue.add(existing);
        } else if (isPowerNode(pos)) {
            // New node - find or create network
            EchoPowerNetwork neighborNet = findNeighborNetwork(pos);
            if (neighborNet != null) {
                mergeNeighborNetworks(pos, neighborNet);
                neighborNet.addNode(pos);
                posToNetwork.put(pos, neighborNet);
                neighborNet.dirty = true;
                rebuildQueue.add(neighborNet);
            } else {
                EchoPowerNetwork net = new EchoPowerNetwork(UUID.randomUUID(), level.dimension());
                net.addNode(pos);
                networks.put(net.networkId, net);
                posToNetwork.put(pos, net);
                rebuildQueue.add(net);
            }
        }
    }

    private void rebuildNetwork(EchoPowerNetwork network) {
        if (network.isEmpty()) {
            networks.remove(network.networkId);
            return;
        }

        int maxSize = PowerGridConfig.MAX_NETWORK_SIZE.get();
        int scanLimit = PowerGridConfig.MAX_CABLE_SCAN_PER_TICK.get();

        Set<BlockPos> candidates = new HashSet<>();
        for (BlockPos pos : network.getNodes()) {
            if (isPowerNode(pos)) {
                candidates.add(pos.immutable());
            } else {
                posToNetwork.remove(pos);
            }
        }
        if (candidates.isEmpty()) {
            networks.remove(network.networkId);
            network.clearNodes();
            return;
        }

        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> unvisited = new HashSet<>(candidates);
        int scanned = 0;
        while (!unvisited.isEmpty() && scanned < scanLimit) {
            BlockPos seed = unvisited.iterator().next();
            Set<BlockPos> component = floodComponent(seed, maxSize, scanLimit - scanned);
            scanned += component.size();
            component.retainAll(candidates);
            unvisited.removeAll(component);
            if (!component.isEmpty()) {
                components.add(component);
            } else {
                unvisited.remove(seed);
            }
        }
        if (!unvisited.isEmpty()) {
            components.add(new HashSet<>(unvisited));
        }

        components.sort(Comparator.comparing(component -> component.stream().min(BlockPos::compareTo).orElse(BlockPos.ZERO)));
        network.clearNodes();
        network.addAllNodes(components.get(0));
        network.dirty = false;
        for (BlockPos pos : network.getNodes()) {
            posToNetwork.put(pos, network);
        }
        primeNetworkAfterRebuild(network);

        for (int i = 1; i < components.size(); i++) {
            EchoPowerNetwork split = new EchoPowerNetwork(UUID.randomUUID(), level.dimension());
            split.addAllNodes(components.get(i));
            split.dirty = false;
            networks.put(split.networkId, split);
            for (BlockPos pos : split.getNodes()) {
                posToNetwork.put(pos, split);
            }
            primeNetworkAfterRebuild(split);
        }
    }

    private void primeNetworkAfterRebuild(EchoPowerNetwork network) {
        if (network != null && !network.isEmpty() && !network.dirty) {
            updateNetwork(network, 1, false);
        }
    }

    private void updateNetworks() {
        List<EchoPowerNetwork> toUpdate = new ArrayList<>(networks.values());
        if (toUpdate.isEmpty()) {
            return;
        }
        toUpdate.sort(Comparator.comparing(network -> network.networkId));
        int maxUpdates = Math.min(PowerGridConfig.GRID_UPDATES_PER_TICK.get(), toUpdate.size());
        int ticksCovered = Math.max(1, PowerGridConfig.NETWORK_UPDATE_INTERVAL_TICKS.get())
                * Math.max(1, (toUpdate.size() + maxUpdates - 1) / maxUpdates);
        int updated = 0;
        int scanned = 0;
        int start = Math.floorMod(networkUpdateCursor, toUpdate.size());

        while (scanned < toUpdate.size() && updated < maxUpdates) {
            EchoPowerNetwork network = toUpdate.get((start + scanned) % toUpdate.size());
            scanned++;
            if (network.isEmpty() || network.dirty) continue;
            updated++;
            updateNetwork(network, ticksCovered, true);
        }
        networkUpdateCursor = (start + Math.max(scanned, 1)) % toUpdate.size();
    }

    private void updateNetwork(EchoPowerNetwork network, int ticksCovered) {
        updateNetwork(network, ticksCovered, true);
    }

    private void updateNetwork(EchoPowerNetwork network, int ticksCovered, boolean allowStorageCharging) {
        if (network == null || network.isEmpty() || network.dirty) {
            return;
        }
        int coveredTicks = Math.max(1, ticksCovered);
        long totalGen = 0;
        long totalDemand = 0;
        long totalStored = 0;
        long totalCapacity = 0;
        long minTransfer = Long.MAX_VALUE;
        EchoPowerQuality quality = EchoPowerQuality.STABLE;

        List<BatteryBlockEntity> batteries = new ArrayList<>();
        List<PowerConsumerBlockEntity> consumers = new ArrayList<>();
        List<GeneratorBlockEntity> generators = new ArrayList<>();

        for (BlockPos pos : network.getNodes()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GeneratorBlockEntity gen) {
                totalGen += gen.getGenerationPerTick();
                if (gen.getPowerQuality() == EchoPowerQuality.DIRTY) quality = EchoPowerQuality.DIRTY;
                generators.add(gen);
            } else if (be instanceof BatteryBlockEntity bat) {
                totalStored += bat.getEnergyStored();
                totalCapacity += bat.getMaxEnergyStored();
                batteries.add(bat);
            } else if (be instanceof PowerConsumerBlockEntity con) {
                totalDemand += con.getDemandPerTick();
                consumers.add(con);
            }

            BlockState state = level.getBlockState(pos);
            long transfer = ModBlocks.getTransferLimit(state);
            if (transfer > 0 && transfer < minTransfer) {
                minTransfer = transfer;
            }
        }

        network.totalGeneration = totalGen;
        network.totalDemand = totalDemand;
        network.totalStored = totalStored;
        network.totalCapacity = totalCapacity;
        network.transferLimit = minTransfer == Long.MAX_VALUE ? Long.MAX_VALUE : minTransfer;
        network.quality = quality;

        long demandBudget = saturatedMultiply(totalDemand, coveredTicks);
        long storageWindow = storageReceiveWindow(batteries, coveredTicks);
        long generatorWindow = generatorAvailableWindow(generators, coveredTicks);
        long requestedStorageWindow = allowStorageCharging ? storageWindow : 0L;
        long requestedFromGenerators = Math.min(generatorWindow, saturatedAdd(demandBudget, requestedStorageWindow));
        long generatedBudget = extractGeneratorBudget(generators, requestedFromGenerators, coveredTicks);
        long suppliedBudget = generatedBudget;

        if (suppliedBudget < demandBudget && !batteries.isEmpty()) {
            long deficit = demandBudget - suppliedBudget;
            long extracted = extractBatteryBudget(batteries, deficit, coveredTicks);
            suppliedBudget = saturatedAdd(suppliedBudget, extracted);
        }

        boolean overload = PowerGridConfig.ENABLE_OVERLOAD.get() && totalDemand > network.transferLimit && network.transferLimit > 0;
        network.overloaded = overload;
        boolean brownout = PowerGridConfig.ENABLE_BROWNOUT.get() && demandBudget > 0 && suppliedBudget < demandBudget;

        if (!overload) {
            network.overloadGraceTicks = 0;
        }
        boolean shouldTriggerOverload = overload && network.overloadGraceTicks >= PowerGridConfig.OVERLOAD_GRACE_TICKS.get();
        EchoGridState previousState = network.state;

        if (shouldTriggerOverload) {
            network.state = EchoGridState.OVERLOADED;
            handleOverloadEffects(network);
        } else if (brownout) {
            network.state = EchoGridState.BROWNOUT;
        } else if (generatedBudget > demandBudget && storageWindow > 0) {
            network.state = EchoGridState.CHARGING;
        } else if (generatedBudget < demandBudget && suppliedBudget >= demandBudget && totalStored > 0) {
            network.state = EchoGridState.DISCHARGING;
        } else {
            network.state = EchoGridState.STABLE;
        }
        playStateFeedback(network, previousState);

        if (!consumers.isEmpty()) {
            double ratio = brownout && demandBudget > 0 ? (double) suppliedBudget / (double) demandBudget : 1.0;
            for (PowerConsumerBlockEntity con : consumers) {
                long demand = con.getDemandPerTick();
                long supplied = (long) Math.floor(demand * ratio);
                con.setPowerReceived(supplied, coveredTicks);
            }
        }

        long excess = allowStorageCharging && generatedBudget > demandBudget ? generatedBudget - demandBudget : 0;
        if (excess > 0 && !batteries.isEmpty()) {
            receiveBatteryBudget(batteries, excess, coveredTicks);
        }
    }

    private void playStateFeedback(EchoPowerNetwork network, EchoGridState previousState) {
        if (level == null || network == null || previousState == null || previousState == network.state) {
            return;
        }
        Identifier eventId = soundForStateTransition(previousState, network.state);
        if (eventId == null) {
            return;
        }
        ISoundService soundService = EchoCoreServices.soundService();
        if (!soundService.available()) {
            return;
        }
        BlockPos anchor = anchorFor(network);
        double x = anchor.getX() + 0.5D;
        double y = anchor.getY() + 0.5D;
        double z = anchor.getZ() + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= 2304.0D) {
                soundService.playEvent(player, eventId, 0.45F, 1.0F);
            }
        }
    }

    private static Identifier soundForStateTransition(EchoGridState previousState, EchoGridState currentState) {
        return switch (currentState) {
            case BROWNOUT -> SOUND_BROWNOUT;
            case OVERLOADED -> SOUND_OVERLOAD;
            case TRIPPED, EMERGENCY -> SOUND_BREAKER_TRIP;
            case STABLE, CHARGING, DISCHARGING -> wasStressed(previousState) ? SOUND_POWER_RESTORED : null;
            default -> null;
        };
    }

    private static boolean wasStressed(EchoGridState state) {
        return state == EchoGridState.BROWNOUT
                || state == EchoGridState.OVERLOADED
                || state == EchoGridState.TRIPPED
                || state == EchoGridState.EMERGENCY;
    }

    private static long generatorAvailableWindow(List<GeneratorBlockEntity> generators, int ticks) {
        long available = 0;
        for (GeneratorBlockEntity gen : generators) {
            available = saturatedAdd(available, gen.getAvailableEnergyForNetwork(ticks));
        }
        return available;
    }

    private static long extractGeneratorBudget(List<GeneratorBlockEntity> generators, long requested, int ticks) {
        long remaining = requested;
        long extractedTotal = 0;
        for (GeneratorBlockEntity gen : generators) {
            if (remaining <= 0) break;
            long available = gen.getAvailableEnergyForNetwork(ticks);
            long extracted = gen.extractEnergyForNetwork(Math.min(remaining, available));
            remaining -= extracted;
            extractedTotal = saturatedAdd(extractedTotal, extracted);
        }
        return extractedTotal;
    }

    private static long storageReceiveWindow(List<BatteryBlockEntity> batteries, int ticks) {
        long capacity = 0;
        for (BatteryBlockEntity bat : batteries) {
            long free = Math.max(0, bat.getMaxEnergyStored() - bat.getEnergyStored());
            capacity = saturatedAdd(capacity, Math.min(free, saturatedMultiply(bat.getMaxInput(), ticks)));
        }
        return capacity;
    }

    private static long receiveBatteryBudget(List<BatteryBlockEntity> batteries, long amount, int ticks) {
        long remaining = amount;
        long receivedTotal = 0;
        for (BatteryBlockEntity bat : batteries) {
            for (int tick = 0; tick < ticks && remaining > 0; tick++) {
                long received = bat.receiveEnergy(Math.min(remaining, bat.getMaxInput()), false);
                if (received <= 0) break;
                remaining -= received;
                receivedTotal = saturatedAdd(receivedTotal, received);
            }
            if (remaining <= 0) break;
        }
        return receivedTotal;
    }

    private static long extractBatteryBudget(List<BatteryBlockEntity> batteries, long amount, int ticks) {
        long remaining = amount;
        long extractedTotal = 0;
        for (BatteryBlockEntity bat : batteries) {
            for (int tick = 0; tick < ticks && remaining > 0; tick++) {
                long extracted = bat.extractEnergy(Math.min(remaining, bat.getMaxOutput()), false);
                if (extracted <= 0) break;
                remaining -= extracted;
                extractedTotal = saturatedAdd(extractedTotal, extracted);
            }
            if (remaining <= 0) break;
        }
        return extractedTotal;
    }

    private PowerGridDrawResult drawPowerFromNetwork(EchoPowerNetwork network, BlockPos targetPos, long ep, boolean simulate) {
        long requested = Math.max(0L, ep);
        if (requested <= 0L) {
            return new PowerGridDrawResult(0L, 0L, simulate, network == null ? EchoGridState.OFFLINE : network.state);
        }
        List<GeneratorBlockEntity> generators = new ArrayList<>();
        List<BatteryBlockEntity> batteries = new ArrayList<>();
        collectDrawableStorage(network, generators, batteries);

        long remaining = requested;
        long drawn = 0L;
        for (GeneratorBlockEntity generator : generators) {
            if (remaining <= 0L) {
                break;
            }
            GrossDraw gross = grossDrawFor(targetPos, generator.getBlockPos(), remaining,
                    generator.getAvailableEnergyForNetwork(1));
            if (gross.deliverable() <= 0L || gross.gross() <= 0L) {
                continue;
            }
            long extracted = simulate ? gross.gross() : generator.extractEnergyForNetwork(gross.gross());
            if (extracted <= 0L) {
                continue;
            }
            long delivered = Math.min(remaining, applyRouteLoss(extracted, gross.distance()));
            remaining -= delivered;
            drawn = saturatedAdd(drawn, delivered);
        }
        for (BatteryBlockEntity battery : batteries) {
            if (remaining <= 0L) {
                break;
            }
            GrossDraw gross = grossDrawFor(targetPos, battery.getBlockPos(), remaining,
                    Math.min(battery.getEnergyStored(), battery.getMaxOutput()));
            if (gross.deliverable() <= 0L || gross.gross() <= 0L) {
                continue;
            }
            long extracted = battery.extractEnergy(gross.gross(), simulate);
            if (extracted <= 0L) {
                continue;
            }
            long delivered = Math.min(remaining, applyRouteLoss(extracted, gross.distance()));
            remaining -= delivered;
            drawn = saturatedAdd(drawn, delivered);
        }
        if (!simulate && drawn > 0L) {
            network.totalStored = totalStored(batteries);
        }
        return new PowerGridDrawResult(requested, drawn, simulate, network.state);
    }

    private void collectDrawableStorage(EchoPowerNetwork network, List<GeneratorBlockEntity> generators,
            List<BatteryBlockEntity> batteries) {
        if (network == null) {
            return;
        }
        for (BlockPos node : network.getNodes()) {
            BlockEntity blockEntity = level.getBlockEntity(node);
            if (blockEntity instanceof GeneratorBlockEntity generator
                    && generator.canExtract()
                    && generator.getMaxEnergyStored() > 0L) {
                generators.add(generator);
            } else if (blockEntity instanceof BatteryBlockEntity battery && battery.canExtract()) {
                batteries.add(battery);
            }
        }
    }

    private long totalStored(List<BatteryBlockEntity> batteries) {
        long total = 0L;
        for (BatteryBlockEntity battery : batteries) {
            total = saturatedAdd(total, battery.getEnergyStored());
        }
        return total;
    }

    private void handleOverloadEffects(EchoPowerNetwork network) {
        if (level == null) return;

        // Trip breakers
        if (PowerGridConfig.TRIP_BREAKERS.get()) {
            for (BlockPos pos : network.getNodes()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BreakerBlock && !state.getValue(BreakerBlock.TRIPPED)) {
                    BreakerBlock.tryTrip(level, pos, network);
                }
            }
        }

        // Damage cables
        if (PowerGridConfig.DAMAGE_CABLES.get()) {
            for (BlockPos pos : network.getNodes()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CableBlock) {
                    level.destroyBlock(pos, true);
                }
            }
        }

        // Extreme overload explosions (disabled on servers if configured)
        if (PowerGridConfig.EXPLODE_ON_EXTREME_OVERLOAD.get() && !isExplosionDisabled()) {
            for (BlockPos pos : network.getNodes()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CableBlock || state.getBlock() instanceof BreakerBlock) {
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
                    }
                    break; // Only one explosion per overload event
                }
            }
        }
    }

    private boolean isExplosionDisabled() {
        if (!PowerGridConfig.DISABLE_EXPLOSIONS_ON_SERVERS.get()) return false;
        return level != null && !level.isClientSide() && level.getServer() != null && level.getServer().isDedicatedServer();
    }

    private PowerGridNodeSummary nodeSummary(BlockPos pos) {
        if (level == null || pos == null || !isPowerNode(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        EchoPowerNodeType type = nodeType(state, blockEntity);
        long generation = 0L;
        long demand = 0L;
        long stored = 0L;
        long capacity = 0L;
        boolean online = true;
        EchoPowerQuality quality = EchoPowerQuality.STABLE;
        if (blockEntity instanceof GeneratorBlockEntity generator) {
            generation = generator.getGenerationPerTick();
            stored = generator.getEnergyStored();
            capacity = generator.getMaxEnergyStored();
            online = generator.isOnline();
            quality = generator.getPowerQuality();
        } else if (blockEntity instanceof BatteryBlockEntity battery) {
            stored = battery.getEnergyStored();
            capacity = battery.getMaxEnergyStored();
            online = battery.isOnline();
        } else if (blockEntity instanceof PowerConsumerBlockEntity consumer) {
            demand = consumer.getDemandPerTick();
            online = consumer.isOnline();
        }
        boolean tripped = state.getBlock() instanceof BreakerBlock breaker && breaker.isTripped(state);
        return new PowerGridNodeSummary(pos, level.dimension(), type, generation, demand, stored, capacity,
                online && !tripped, ModBlocks.getTransferLimit(state), quality, tripped, tripped);
    }

    private EchoPowerNodeType nodeType(BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof GeneratorBlockEntity generator) {
            return generator.getNodeType();
        }
        if (blockEntity instanceof BatteryBlockEntity) {
            return EchoPowerNodeType.STORAGE;
        }
        if (blockEntity instanceof PowerConsumerBlockEntity consumer) {
            return consumer.getNodeType();
        }
        Block block = state.getBlock();
        if (block instanceof CableBlock) return EchoPowerNodeType.CABLE;
        if (block instanceof BreakerBlock) return EchoPowerNodeType.BREAKER;
        if (block instanceof com.knoxhack.echopowergrid.block.MeterBlock) return EchoPowerNodeType.METER;
        if (block instanceof com.knoxhack.echopowergrid.block.SubstationBlock) return EchoPowerNodeType.SUBSTATION;
        return EchoPowerNodeType.CABLE;
    }

    private Set<BlockPos> floodComponent(BlockPos seed, int maxSize, int scanLimit) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        if (seed == null || !isPowerNode(seed)) {
            return visited;
        }
        visited.add(seed.immutable());
        queue.add(seed.immutable());
        int scanned = 0;
        while (!queue.isEmpty() && visited.size() < maxSize && scanned < scanLimit) {
            BlockPos current = queue.poll();
            scanned++;
            if (!canConduct(current)) {
                continue;
            }
            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor) || !isPowerNode(neighbor)) {
                    continue;
                }
                visited.add(neighbor.immutable());
                queue.add(neighbor.immutable());
                if (visited.size() >= maxSize) {
                    break;
                }
            }
        }
        return visited;
    }

    private RouteInfo findRoute(BlockPos from, BlockPos to, long requestedEp) {
        EchoPowerNetwork fromNetwork = posToNetwork.get(from);
        EchoPowerNetwork toNetwork = posToNetwork.get(to);
        if (fromNetwork == null || toNetwork == null || fromNetwork != toNetwork) {
            return RouteInfo.blocked("Endpoints are not on the same loaded network.");
        }
        if (!isPowerNode(from) || !isPowerNode(to)) {
            return RouteInfo.blocked("One or both endpoints are not power nodes.");
        }
        if (from.equals(to)) {
            long limit = Math.max(0L, ModBlocks.getTransferLimit(level.getBlockState(from)));
            return new RouteInfo(0, limit <= 0L ? Long.MAX_VALUE : limit, false, "");
        }
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(from.immutable());
        queue.add(from.immutable());
        int scanned = 0;
        int scanLimit = PowerGridConfig.MAX_CABLE_SCAN_PER_TICK.get();
        while (!queue.isEmpty() && scanned < scanLimit) {
            BlockPos current = queue.poll();
            scanned++;
            if (!canConduct(current) && !current.equals(from) && !current.equals(to)) {
                continue;
            }
            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor) || !isPowerNode(neighbor)) {
                    continue;
                }
                visited.add(neighbor.immutable());
                parent.put(neighbor.immutable(), current.immutable());
                if (neighbor.equals(to)) {
                    return routeFromParents(from, to, parent, requestedEp);
                }
                queue.add(neighbor.immutable());
            }
        }
        return RouteInfo.blocked(scanned >= scanLimit ? "Route scan budget exhausted." : "No conducting route found.");
    }

    private RouteInfo routeFromParents(BlockPos from, BlockPos to, Map<BlockPos, BlockPos> parent, long requestedEp) {
        long transferLimit = Long.MAX_VALUE;
        int distance = 0;
        BlockPos current = to.immutable();
        while (!current.equals(from)) {
            BlockState state = level.getBlockState(current);
            long transfer = ModBlocks.getTransferLimit(state);
            if (transfer > 0L && transfer < transferLimit) {
                transferLimit = transfer;
            }
            current = parent.get(current);
            distance++;
            if (current == null) {
                return RouteInfo.blocked("Route parent chain was incomplete.");
            }
        }
        long firstTransfer = ModBlocks.getTransferLimit(level.getBlockState(from));
        if (firstTransfer > 0L && firstTransfer < transferLimit) {
            transferLimit = firstTransfer;
        }
        if (transferLimit == Long.MAX_VALUE && requestedEp > 0L) {
            transferLimit = requestedEp;
        }
        return new RouteInfo(distance, transferLimit, false, "");
    }

    private GrossDraw grossDrawFor(BlockPos target, BlockPos source, long desiredDelivery, long sourceAvailable) {
        if (desiredDelivery <= 0L || sourceAvailable <= 0L) {
            return new GrossDraw(0L, 0L, 0);
        }
        RouteInfo route = findRoute(target, source, desiredDelivery);
        if (route.blocked()) {
            return new GrossDraw(0L, 0L, 0);
        }
        long grossLimit = Math.min(sourceAvailable, route.transferLimit());
        if (grossLimit <= 0L) {
            return new GrossDraw(0L, 0L, route.distance());
        }
        long gross = grossNeededForDelivery(desiredDelivery, route.distance(), grossLimit);
        long deliverable = Math.min(desiredDelivery, applyRouteLoss(gross, route.distance()));
        return new GrossDraw(gross, deliverable, route.distance());
    }

    private long grossNeededForDelivery(long desiredDelivery, int distance, long grossLimit) {
        long gross = Math.min(grossLimit, desiredDelivery);
        for (int i = 0; i < 8 && gross < grossLimit; i++) {
            long delivered = applyRouteLoss(gross, distance);
            if (delivered >= desiredDelivery) {
                return gross;
            }
            long missing = desiredDelivery - delivered;
            gross = Math.min(grossLimit, saturatedAdd(gross, Math.max(1L, missing)));
        }
        return gross;
    }

    private long applyRouteLoss(long gross, int distance) {
        return Math.max(0L, gross - lossFor(gross, distance));
    }

    private long lossFor(long gross, int distance) {
        if (gross <= 0L || distance <= 0 || !PowerGridConfig.ENABLE_POWER_LOSS.get()) {
            return 0L;
        }
        double percent = lossPercent(distance) / 100.0D;
        return Math.min(gross, (long) Math.ceil(gross * percent));
    }

    private double lossPercent(int distance) {
        if (distance <= 0 || !PowerGridConfig.ENABLE_POWER_LOSS.get()) {
            return 0.0D;
        }
        return Math.min(95.0D, PowerGridConfig.BASE_LOSS_PERCENT_PER_16_BLOCKS.get() * distance / 16.0D);
    }

    private BlockPos anchorFor(EchoPowerNetwork network) {
        BlockPos generator = null;
        BlockPos storage = null;
        BlockPos first = null;
        for (BlockPos node : network.getNodes()) {
            BlockPos immutable = node.immutable();
            if (first == null || immutable.compareTo(first) < 0) {
                first = immutable;
            }
            BlockEntity blockEntity = level.getBlockEntity(node);
            if (blockEntity instanceof SubstationBlockEntity) {
                return immutable;
            }
            if (generator == null && blockEntity instanceof GeneratorBlockEntity) {
                generator = immutable;
            } else if (storage == null && blockEntity instanceof BatteryBlockEntity) {
                storage = immutable;
            }
        }
        if (generator != null) {
            return generator;
        }
        return storage == null ? (first == null ? BlockPos.ZERO : first) : storage;
    }

    private static long saturatedMultiply(long value, int multiplier) {
        if (value <= 0 || multiplier <= 0) return 0;
        if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE;
        return value * multiplier;
    }

    private static long saturatedAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private EchoPowerNetwork findNeighborNetwork(BlockPos pos) {
        for (BlockPos neighbor : getNeighbors(pos)) {
            EchoPowerNetwork net = posToNetwork.get(neighbor);
            if (net != null) return net;
        }
        return null;
    }

    private void mergeNeighborNetworks(BlockPos pos, EchoPowerNetwork target) {
        if (pos == null || target == null) {
            return;
        }
        for (BlockPos neighbor : getNeighbors(pos)) {
            EchoPowerNetwork other = posToNetwork.get(neighbor);
            if (other == null || other == target || other.dimension != target.dimension) {
                continue;
            }
            for (BlockPos node : other.getNodes()) {
                target.addNode(node);
                posToNetwork.put(node, target);
            }
            networks.remove(other.networkId);
            other.clearNodes();
            target.dirty = true;
        }
    }

    private boolean isPowerNode(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return ModBlocks.isPowerNode(state);
    }

    private boolean canConduct(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BreakerBlock breaker) {
            return !breaker.isTripped(state);
        }
        return true;
    }

    private static BlockPos[] getNeighbors(BlockPos pos) {
        return new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below()
        };
    }

    private record RouteInfo(int distance, long transferLimit, boolean blocked, String blockedReason) {
        private static RouteInfo blocked(String reason) {
            return new RouteInfo(0, 0L, true, reason == null ? "Route blocked." : reason);
        }
    }

    private record GrossDraw(long gross, long deliverable, int distance) {
    }
}

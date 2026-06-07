package com.knoxhack.echo.machinecore.integration;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;
import com.knoxhack.echo.machinecore.EchoMachineConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;






import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;

/**
 * Live runtime host for EchoPowerGrid machines.
 */
public final class MachineCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = EchoMachineConstants.MOD_ID + ":machine_runtime";

    private static final String ACTION_TICK = "block_entities.tick";
    private static final String ACTION_SNAPSHOT = "block_entities.snapshot";
    private static final String ACTION_APPLY_SNAPSHOT = "block_entities.apply_snapshot";
    private static final String ACTION_INSERT_ITEM = "capabilities.insert_item";
    private static final String ACTION_EXTRACT_ITEM = "capabilities.extract_item";
    private static final String ACTION_RECEIVE_ENERGY = "capabilities.receive_energy";
    private static final String ACTION_EXTRACT_ENERGY = "capabilities.extract_energy";
    private static final String ACTION_READ_CAPABILITY = "capabilities.read_capability";

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final MachineCoreRuntimeHost HOST = new MachineCoreRuntimeHost();
    private static volatile MinecraftServer currentServer;

    private final BlockEntities blockEntities = new PowerGridBlockEntities();
    private final Capabilities capabilities = new PowerGridCapabilities();

    private MachineCoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void bindServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.BlockEntities",
                        "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                        ACTION_TICK,
                        ACTION_SNAPSHOT,
                        ACTION_APPLY_SNAPSHOT,
                        ACTION_INSERT_ITEM,
                        ACTION_EXTRACT_ITEM,
                        ACTION_RECEIVE_ENERGY,
                        ACTION_EXTRACT_ENERGY,
                        ACTION_READ_CAPABILITY),
                Set.of(
                        "echopowergrid:hand_crank_generator",
                        "echopowergrid:scrap_burner_generator",
                        "echopowergrid:solar_panel",
                        "echopowergrid:reinforced_solar_panel",
                        "echopowergrid:biofuel_generator",
                        "echopowergrid:creative_power_source",
                        "echopowergrid:small_battery_bank",
                        "echopowergrid:medium_battery_bank",
                        "echopowergrid:field_battery_bank",
                        "echopowergrid:industrial_battery_bank",
                        "echopowergrid:low_voltage_cable",
                        "echopowergrid:industrial_cable",
                        "echopowergrid:high_voltage_cable",
                        "echopowergrid:outpost_substation",
                        "echopowergrid:relay_substation",
                        "echopowergrid:factory_substation",
                        "echopowergrid:nexus_stabilizer_coupler",
                        "echopowergrid:emergency_breaker",
                        "echopowergrid:power_meter",
                        "echopowergrid:creative_power_sink",
                        "echopowergrid:test_power_consumer"),
                false,
                false,
                false));
        registerActions(EchoRuntimeActionDispatcher.global());
        // Logger removed("Registered PowerGrid machine runtime host {}", RUNTIME_HOST_ID);
    }

    public static NativeResult dispatchReceiveEnergy(net.minecraft.server.level.ServerPlayer player,
                                                      net.minecraft.world.level.Level level,
                                                      BlockPos pos,
                                                      String machineId,
                                                      int amount) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Power grid energy receive action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "receive_energy");
        payload.put("capabilityId", "energy");
        payload.put("amount", Math.max(1, amount));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "powergrid-energy-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", ACTION_RECEIVE_ENERGY,
                "nativeInterface", "EchoNativeRuntimeHost.Capabilities",
                "nativeMethod", "receiveEnergy"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_RECEIVE_ENERGY,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    public static NativeResult dispatchExtractEnergy(net.minecraft.server.level.ServerPlayer player,
                                                      net.minecraft.world.level.Level level,
                                                      BlockPos pos,
                                                      String machineId,
                                                      int amount) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Power grid energy extract action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "extract_energy");
        payload.put("capabilityId", "energy");
        payload.put("amount", Math.max(1, amount));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "powergrid-energy-extract-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", ACTION_EXTRACT_ENERGY,
                "nativeInterface", "EchoNativeRuntimeHost.Capabilities",
                "nativeMethod", "extractEnergy"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_EXTRACT_ENERGY,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    public static NativeResult dispatchTick(net.minecraft.server.level.ServerPlayer player,
                                             net.minecraft.world.level.Level level,
                                             BlockPos pos,
                                             String machineId) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Power grid tick action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "tick");
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "powergrid-tick-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", ACTION_TICK,
                "nativeInterface", "EchoNativeRuntimeHost.BlockEntities",
                "nativeMethod", "tick"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_TICK,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    @Override
    public BlockEntities blockEntities() {
        return blockEntities;
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    private static void registerActions(EchoRuntimeActionDispatcher dispatcher) {
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_TICK, MachineCoreRuntimeHost::dispatchTickAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.tick", MachineCoreRuntimeHost::dispatchTickAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_SNAPSHOT, MachineCoreRuntimeHost::dispatchSnapshotAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.snapshot", MachineCoreRuntimeHost::dispatchSnapshotAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_APPLY_SNAPSHOT, MachineCoreRuntimeHost::dispatchApplySnapshotAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.applySnapshot", MachineCoreRuntimeHost::dispatchApplySnapshotAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_INSERT_ITEM, MachineCoreRuntimeHost::dispatchInsertItemAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.insertItem", MachineCoreRuntimeHost::dispatchInsertItemAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ITEM, MachineCoreRuntimeHost::dispatchExtractItemAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.extractItem", MachineCoreRuntimeHost::dispatchExtractItemAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_RECEIVE_ENERGY, MachineCoreRuntimeHost::dispatchReceiveEnergyAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.receiveEnergy", MachineCoreRuntimeHost::dispatchReceiveEnergyAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ENERGY, MachineCoreRuntimeHost::dispatchExtractEnergyAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.extractEnergy", MachineCoreRuntimeHost::dispatchExtractEnergyAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_READ_CAPABILITY, MachineCoreRuntimeHost::dispatchReadCapabilityAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.readCapability", MachineCoreRuntimeHost::dispatchReadCapabilityAction);
    }

    private static EchoRuntimeActionOutcome dispatchTickAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        Map<String, Object> before = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult result = runtimeHost.blockEntities().tick(action.targetBlock(), action.context());
        Map<String, Object> after = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchSnapshotAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeBlockEntitySnapshot snapshot = runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context());
        NativeResult result = NativeResult.noop("Captured live power grid block entity snapshot.", snapshot.state());
        return EchoRuntimeActionOutcome.of(Map.of(), result, summary(snapshot), false, false);
    }

    private static EchoRuntimeActionOutcome dispatchApplySnapshotAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeBlockEntitySnapshot beforeSnapshot = runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context());
        String blockEntityId = firstNonBlank(stringValue(action.inputPayload(), "blockEntityId"), beforeSnapshot.blockEntityId());
        Map<String, Object> state = mapValue(action.inputPayload(), "state");
        if (state.isEmpty()) {
            state = mapValue(action.inputPayload(), "snapshotState");
        }
        if (state.isEmpty()) {
            state = action.inputPayload();
        }
        NativeResult result = runtimeHost.blockEntities().applySnapshot(
                new NativeBlockEntitySnapshot(blockEntityId, action.targetBlock(), state),
                action.context());
        NativeBlockEntitySnapshot afterSnapshot = runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context());
        return EchoRuntimeActionOutcome.of(summary(beforeSnapshot), result, summary(afterSnapshot), result.mutated(), false);
    }

    private static EchoRuntimeActionOutcome dispatchInsertItemAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "item");
        NativeItemStack stack = new NativeItemStack(
                firstNonBlank(stringValue(action.inputPayload(), "itemId"), stringValue(action.inputPayload(), "item")),
                Math.max(1, intValue(action.inputPayload(), "count", 1)),
                mapValue(action.inputPayload(), "components"));
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().insertItem(request, stack, action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), false);
    }

    private static EchoRuntimeActionOutcome dispatchExtractItemAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "item");
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().extractItem(
                request,
                firstNonBlank(stringValue(action.inputPayload(), "itemId"), stringValue(action.inputPayload(), "item")),
                Math.max(1, intValue(action.inputPayload(), "count", 1)),
                action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), false);
    }

    private static EchoRuntimeActionOutcome dispatchReceiveEnergyAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "energy");
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().receiveEnergy(
                request,
                Math.max(1, intValue(action.inputPayload(), "amount", 1)),
                action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchExtractEnergyAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "energy");
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().extractEnergy(
                request,
                Math.max(1, intValue(action.inputPayload(), "amount", 1)),
                action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchReadCapabilityAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, firstNonBlank(stringValue(action.inputPayload(), "capabilityId"), "energy"));
        Map<String, Object> snapshot = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = NativeResult.noop("Read live power grid capability.", snapshot);
        return EchoRuntimeActionOutcome.of(snapshot, result, snapshot, false, false);
    }

    private final class PowerGridBlockEntities implements BlockEntities {
        @Override
        public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(block);
            if (resolved.entity() == null) {
                return NativeResult.unsupported("No live power grid block entity is loaded at target.", Map.of(
                        "block", blockSnapshot(block),
                        "loaded", resolved.loaded()));
            }
            NativeBlockEntitySnapshot beforeSnapshot = snapshot(block, context);
            BlockEntity entity = resolved.entity();
            if (!invokePowerGridTick(entity, resolved.level(), resolved.pos(), resolved.state())) {
                return NativeResult.unsupported("Block entity is not a registered power grid machine.", beforeSnapshot.state());
            }
            NativeBlockEntitySnapshot afterSnapshot = snapshot(block, context);
            boolean changed = !beforeSnapshot.state().equals(afterSnapshot.state());
            return changed
                    ? NativeResult.mutated("Ticked live power grid block entity and mutated state.", afterSnapshot.state())
                    : NativeResult.noop("Ticked live power grid block entity with no state change.", afterSnapshot.state());
        }

        @Override
        public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(block);
            BlockEntity entity = resolved.entity();
            if (entity == null) {
                return new NativeBlockEntitySnapshot("minecraft:missing", block, Map.of(
                        "loaded", resolved.loaded(),
                        "block", blockSnapshot(block)));
            }
            Map<String, Object> state = new LinkedHashMap<>();
            String machineId = machineId(entity);
            state.put("machineId", machineId);
            state.put("blockId", BuiltInRegistries.BLOCK.getKey(resolved.state().getBlock()).toString());
            state.put("blockEntityId", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
            state.put("position", positionSnapshot(resolved.level(), resolved.pos()));
            if (isEnergyStorage(entity)) {
                state.put("energyStored", energyLong(entity, "getEnergyStored", 0L));
                state.put("energyCapacity", energyLong(entity, "getMaxEnergyStored", 0L));
                state.put("canReceiveEnergy", energyBoolean(entity, "canReceive"));
                state.put("canExtractEnergy", energyBoolean(entity, "canExtract"));
            }
            return new NativeBlockEntitySnapshot(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString(),
                    block,
                    state);
        }

        @Override
        public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(snapshot.block());
            BlockEntity entity = resolved.entity();
            if (entity == null) {
                return NativeResult.unsupported("No live power grid block entity is loaded for snapshot application.", Map.of(
                        "block", blockSnapshot(snapshot.block())));
            }
            Map<String, Object> before = mapValue(snapshot.state(), "state");
            if (before.isEmpty()) {
                before = snapshot.state();
            }
            boolean changed = false;
            if (isEnergyStorage(entity) && snapshot.state().containsKey("energyStored")) {
                long stored = longValue(snapshot.state(), "energyStored", 0);
                if (stored >= 0 && stored <= energyLong(entity, "getMaxEnergyStored", 0L)
                        && setEnergyStored(entity, stored)) {
                    changed = true;
                }
            }
            NativeBlockEntitySnapshot afterSnapshot = snapshot(snapshot.block(), context);
            return changed
                    ? NativeResult.mutated("Applied snapshot to live power grid block entity.", afterSnapshot.state())
                    : NativeResult.noop("Snapshot did not change power grid block entity state.", afterSnapshot.state());
        }
    }

    private final class PowerGridCapabilities implements Capabilities {
        @Override
        public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
            return NativeResult.unsupported("Power grid machines do not support live item insertion through this host.", Map.of(
                    "capabilityId", request.capabilityId(),
                    "block", blockSnapshot(request.block())));
        }

        @Override
        public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
            return NativeResult.unsupported("Power grid machines do not support live item extraction through this host.", Map.of(
                    "capabilityId", request.capabilityId(),
                    "block", blockSnapshot(request.block())));
        }

        @Override
        public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            if (!isEnergyStorage(entity)) {
                return NativeResult.unsupported("Target block entity does not expose EchoEnergyStorage.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = readCapability(request, context);
            int received = saturatedInt(energyTransfer(entity, "receiveEnergy", Math.max(1, amount), false));
            if (received > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = readCapability(request, context);
            return received > 0
                    ? NativeResult.mutated("Received energy into live power grid storage.", copyPayload(after, Map.of("received", received)))
                    : NativeResult.noop("Power grid storage did not accept energy.", copyPayload(before, Map.of("received", 0)));
        }

        @Override
        public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            if (!isEnergyStorage(entity)) {
                return NativeResult.unsupported("Target block entity does not expose EchoEnergyStorage.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = readCapability(request, context);
            int extracted = saturatedInt(energyTransfer(entity, "extractEnergy", Math.max(1, amount), false));
            if (extracted > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = readCapability(request, context);
            return extracted > 0
                    ? NativeResult.mutated("Extracted energy from live power grid storage.", copyPayload(after, Map.of("extracted", extracted)))
                    : NativeResult.noop("Power grid storage did not provide energy.", copyPayload(before, Map.of("extracted", 0)));
        }

        @Override
        public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("capabilityId", request.capabilityId());
            snapshot.put("block", blockSnapshot(request.block()));
            if (isEnergyStorage(entity)) {
                snapshot.put("energy", Map.of(
                        "present", true,
                        "stored", energyLong(entity, "getEnergyStored", 0L),
                        "capacity", energyLong(entity, "getMaxEnergyStored", 0L),
                        "canReceive", energyBoolean(entity, "canReceive"),
                        "canExtract", energyBoolean(entity, "canExtract")));
            } else {
                snapshot.put("energy", Map.of("present", false));
            }
            return Map.copyOf(snapshot);
        }
    }

    // Helper types and methods

    private record ResolvedBlock(ServerLevel level, BlockPos pos, BlockState state, BlockEntity entity, boolean loaded) {}

    private static ResolvedBlock resolve(NativeBlockRef block) {
        MinecraftServer server = currentServer;
        if (server == null || block == null) {
            return new ResolvedBlock(null, BlockPos.ZERO, null, null, false);
        }
        Identifier dimensionId = Identifier.tryParse(block.dimensionId());
        if (dimensionId == null) {
            return new ResolvedBlock(null, new BlockPos(block.x(), block.y(), block.z()), null, null, false);
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        BlockPos pos = new BlockPos(block.x(), block.y(), block.z());
        if (level == null || !level.hasChunkAt(pos)) {
            return new ResolvedBlock(level, pos, null, null, false);
        }
        BlockState state = level.getBlockState(pos);
        return new ResolvedBlock(level, pos, state, level.getBlockEntity(pos), true);
    }

    private static NativeCapabilityRequest request(EchoRuntimeAction action, String defaultCapability) {
        return new NativeCapabilityRequest(
                firstNonBlank(stringValue(action.inputPayload(), "capabilityId"), defaultCapability),
                action.targetBlock(),
                stringValue(action.inputPayload(), "side"),
                mapValue(action.inputPayload(), "query"));
    }

    private static NativeMutationContext context(net.minecraft.world.level.Level level, BlockPos pos, String key, Map<String, Object> metadata) {
        return new NativeMutationContext(
                EchoMachineConstants.MOD_ID,
                dimensionId(level),
                key,
                "server",
                level.getGameTime(),
                metadata);
    }

    private static NativeBlockRef blockRef(net.minecraft.world.level.Level level, BlockPos pos) {
        return new NativeBlockRef(dimensionId(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static EchoNativeRuntimeHost.NativePlayerRef playerRef(net.minecraft.server.level.ServerPlayer player) {
        return new EchoNativeRuntimeHost.NativePlayerRef(player.getUUID().toString());
    }

    private static String dimensionId(net.minecraft.world.level.Level level) {
        return level.dimension().identifier().toString();
    }

    private static String machineId(BlockEntity entity) {
        if (entity == null) {
            return "";
        }
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String machineIdAt(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return "";
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        }
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static Map<String, Object> positionSnapshot(net.minecraft.world.level.Level level, BlockPos pos) {
        return Map.of(
                "dimensionId", dimensionId(level),
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static Map<String, Object> blockSnapshot(NativeBlockRef block) {
        if (block == null) {
            return Map.of();
        }
        return Map.of(
                "dimensionId", block.dimensionId(),
                "x", block.x(),
                "y", block.y(),
                "z", block.z());
    }

    private static Map<String, Object> summary(NativeBlockEntitySnapshot snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("blockEntityId", snapshot.blockEntityId());
        summary.putAll(snapshot.state());
        return Map.copyOf(summary);
    }

    private static boolean eventPublished(NativeResult result) {
        return Boolean.TRUE.equals(result.snapshot().get("eventPublished"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Map<?, ?> typed) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : typed.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Map<String, Object> map, String key, int fallback) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longValue(Map<String, Object> map, String key, long fallback) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean invokePowerGridTick(BlockEntity entity, ServerLevel level, BlockPos pos, BlockState state) {
        if (entity == null || level == null || pos == null || state == null) {
            return false;
        }
        for (String className : Set.of(
                "com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity",
                "com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity",
                "com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity",
                "com.knoxhack.echopowergrid.block.entity.SubstationBlockEntity")) {
            try {
                Class<?> type = Class.forName(className);
                if (!type.isInstance(entity)) {
                    continue;
                }
                Method tick = type.getMethod(
                        "tick",
                        net.minecraft.world.level.Level.class,
                        BlockPos.class,
                        BlockState.class,
                        type);
                tick.invoke(null, level, pos, state, entity);
                return true;
            } catch (ClassNotFoundException ignored) {
                // PowerGrid remains optional at MachineCore compile time.
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isEnergyStorage(Object value) {
        return value != null
                && hasPublicMethod(value, "getEnergyStored")
                && hasPublicMethod(value, "getMaxEnergyStored")
                && hasPublicMethod(value, "canReceive")
                && hasPublicMethod(value, "canExtract")
                && hasPublicMethod(value, "receiveEnergy", long.class, boolean.class)
                && hasPublicMethod(value, "extractEnergy", long.class, boolean.class);
    }

    private static boolean hasPublicMethod(Object target, String name, Class<?>... parameterTypes) {
        try {
            target.getClass().getMethod(name, parameterTypes);
            return true;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    private static long energyLong(Object target, String method, long fallback) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value instanceof Number number ? number.longValue() : fallback;
        } catch (ReflectiveOperationException exception) {
            return fallback;
        }
    }

    private static boolean energyBoolean(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static long energyTransfer(Object target, String method, long amount, boolean simulate) {
        try {
            Object value = target.getClass().getMethod(method, long.class, boolean.class)
                    .invoke(target, amount, simulate);
            return value instanceof Number number ? number.longValue() : 0L;
        } catch (ReflectiveOperationException exception) {
            return 0L;
        }
    }

    private static boolean setEnergyStored(Object target, long stored) {
        try {
            target.getClass().getMethod("setEnergyStored", long.class).invoke(target, stored);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static int saturatedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value;
    }

    private static Map<String, Object> copyPayload(Map<String, Object> base, Map<String, Object> extras) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (base != null) {
            copy.putAll(base);
        }
        if (extras != null) {
            copy.putAll(extras);
        }
        return Map.copyOf(copy);
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second == null ? "" : second : first;
    }
}

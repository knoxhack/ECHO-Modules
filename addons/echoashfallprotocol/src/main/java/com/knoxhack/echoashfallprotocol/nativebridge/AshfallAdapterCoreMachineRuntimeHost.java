package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock;
import com.knoxhack.echoashfallprotocol.block.HandRecyclerBlock;
import com.knoxhack.echoashfallprotocol.block.MicroGeneratorBlock;
import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.RelayStationBlock;
import com.knoxhack.echoashfallprotocol.block.ResearchLabBlock;
import com.knoxhack.echoashfallprotocol.block.StructureCacheBlock;
import com.knoxhack.echoashfallprotocol.block.WaterPurifierBlock;
import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.DeepCoreMinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FieldMedBayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FilterWorkbenchBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HopperHandler;
import com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.MachineInventory;
import com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RainCollectorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapDynamoBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapPressBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.StructureCacheBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalBurnerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreMissionTriggerRuntime;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Live AdapterCore machine runtime for the Ashfall starter machine path.
 */
public final class AshfallAdapterCoreMachineRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = EchoAshfallProtocol.MODID + ":machine_runtime";
    public static final String EVENT_OUTPUT_CREATED = "machine.output_created";

    private static final String ACTION_TICK = "block_entities.tick";
    private static final String ACTION_SNAPSHOT = "block_entities.snapshot";
    private static final String ACTION_APPLY_SNAPSHOT = "block_entities.apply_snapshot";
    private static final String ACTION_INSERT_ITEM = "capabilities.insert_item";
    private static final String ACTION_EXTRACT_ITEM = "capabilities.extract_item";
    private static final String ACTION_RECEIVE_ENERGY = "capabilities.receive_energy";
    private static final String ACTION_EXTRACT_ENERGY = "capabilities.extract_energy";
    private static final String ACTION_READ_CAPABILITY = "capabilities.read_capability";
    private static final String ACTION_USE_BLOCK = "machine.use_block";
    private static final String ACTION_STATE_CHANGED = "machine.state_changed";
    private static final String SAVE_ROOT = "echoashfallprotocol.adaptercore.machines";

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final AshfallAdapterCoreMachineRuntimeHost HOST = new AshfallAdapterCoreMachineRuntimeHost();
    private static volatile MinecraftServer activeServer;

    private final BlockEntities blockEntities = new AshfallBlockEntities();
    private final Capabilities capabilities = new AshfallCapabilities();
    private final Events events = new AshfallEvents();
    private final Hud hud = new AshfallHud();
    private final SaveData saveData = new AshfallSaveData();

    private AshfallAdapterCoreMachineRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.BlockEntities",
                        "EchoNativeRuntimeHost.Capabilities",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Hud",
                        "EchoNativeRuntimeHost.SaveData"),
                Set.of(
                        ACTION_TICK,
                        ACTION_SNAPSHOT,
                        ACTION_APPLY_SNAPSHOT,
                        ACTION_INSERT_ITEM,
                        ACTION_EXTRACT_ITEM,
                        ACTION_RECEIVE_ENERGY,
                        ACTION_EXTRACT_ENERGY,
                        ACTION_READ_CAPABILITY,
                        ACTION_USE_BLOCK,
                        ACTION_STATE_CHANGED,
                        EVENT_OUTPUT_CREATED),
                Set.of(
                        EchoAshfallProtocol.MODID + ":water_purifier",
                        EchoAshfallProtocol.MODID + ":rain_collector",
                        EchoAshfallProtocol.MODID + ":hand_recycler",
                        EchoAshfallProtocol.MODID + ":micro_generator",
                        EchoAshfallProtocol.MODID + ":scrap_dynamo",
                        EchoAshfallProtocol.MODID + ":thermal_burner",
                        EchoAshfallProtocol.MODID + ":thermal_array",
                        EchoAshfallProtocol.MODID + ":power_node",
                        EchoAshfallProtocol.MODID + ":power_cable",
                        EchoAshfallProtocol.MODID + ":battery_bank",
                        EchoAshfallProtocol.MODID + ":nexus_capacitor",
                        EchoAshfallProtocol.MODID + ":nexus_core",
                        EchoAshfallProtocol.MODID + ":load_distributor",
                        EchoAshfallProtocol.MODID + ":factory_controller",
                        EchoAshfallProtocol.MODID + ":research_lab",
                        EchoAshfallProtocol.MODID + ":field_med_bay",
                        EchoAshfallProtocol.MODID + ":atmospheric_scrubber",
                        EchoAshfallProtocol.MODID + ":radiation_cleanser",
                        EchoAshfallProtocol.MODID + ":structure_cache",
                        EchoAshfallProtocol.MODID + ":recovery_cache",
                        EchoAshfallProtocol.MODID + ":emergency_bunk",
                        EchoAshfallProtocol.MODID + ":relay_station",
                        "echorecovery:recovery_cache",
                        "signalos:terminal",
                        "echoterminal:terminal"),
                true,
                true,
                true));
        registerActions(EchoRuntimeActionDispatcher.global());
        EchoAshfallProtocol.LOGGER.info("Registered AdapterCore machine runtime host {}", RUNTIME_HOST_ID);
    }

    public static void bindServer(MinecraftServer server) {
        if (server != null) {
            activeServer = server;
        }
    }

    public static NativeResult dispatchUseBlock(ServerPlayer player, Level level, BlockPos pos, String machineId) {
        return dispatchUseBlock(player, level, pos, machineId, true);
    }

    public static NativeResult dispatchUseBlock(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            boolean applyLiveUse) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine use action was not on a live server side.", Map.of());
        }
        bindServer(level);
        register();
        NativeBlockRef blockRef = blockRef(level, pos);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "use_block");
        payload.put("position", positionSnapshot(pos));
        payload.put("applyLiveUse", applyLiveUse);
        NativeMutationContext context = context(level, pos, "machine-use-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", ACTION_USE_BLOCK,
                "nativeInterface", "EchoNativeRuntimeHost.Events",
                "nativeMethod", "publish"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_USE_BLOCK,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef,
                context));
    }

    public static NativeResult dispatchNativeMachineState(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            Map<String, Object> state,
            String source) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine state action was not on a live server side.", Map.of());
        }
        bindServer(level);
        register();
        NativeBlockRef blockRef = blockRef(level, pos);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "state_changed");
        payload.put("source", firstNonBlank(source, ACTION_STATE_CHANGED));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("state", state == null ? Map.of() : Map.copyOf(state));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "machine-state-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", ACTION_STATE_CHANGED,
                "nativeInterface", "EchoNativeRuntimeHost.Events",
                "nativeMethod", "publish"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_STATE_CHANGED,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef,
                context));
    }

    public static NativeResult outputCreated(Level level, BlockPos pos, BlockEntity entity, ItemStack output) {
        if (level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine output was not produced on a live server side.", Map.of());
        }
        bindServer(level);
        register();
        ServerPlayer player = nearestPlayer(level, pos, 18.0D);
        NativePlayerRef playerRef = player == null ? null : playerRef(player);
        String machineId = entity == null ? machineIdAt(level, pos) : machineId(entity);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", machineId);
        payload.put("canonicalMachineId", canonicalMachineId(machineId, entity));
        payload.put("output", stackSnapshot(output));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("block", blockSnapshot(blockRef(level, pos)));
        payload.put("blockId", machineIdAt(level, pos));
        if (player != null) {
            payload.put("playerId", player.getUUID().toString());
            payload.put("playerName", player.getName().getString());
        }
        NativeMutationContext context = context(level, pos, "machine-output-" + pos.asLong() + "-" + level.getGameTime(), Map.of(
                "playerId", player == null ? "" : player.getUUID().toString(),
                "machineId", machineId,
                "source", EVENT_OUTPUT_CREATED,
                "nativeInterface", "EchoNativeRuntimeHost.Events",
                "nativeMethod", "publish"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                EVENT_OUTPUT_CREATED,
                RUNTIME_HOST_ID,
                payload,
                playerRef,
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    public static NativeResult dispatchNativeMachineTick(ServerPlayer player, Level level, BlockPos pos, String machineId) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine tick action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "tick");
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "machine-tick-" + player.getUUID() + "-" + level.getGameTime(), Map.of(
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

    public static NativeResult dispatchNativeMachineInsertItem(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            String itemId,
            int count) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine item insertion action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "insert_item");
        payload.put("capabilityId", "item");
        payload.put("itemId", firstNonBlank(itemId, ""));
        payload.put("count", Math.max(1, count));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "machine-insert-" + player.getUUID() + "-"
                + compactActionKey(firstNonBlank(itemId, "item")) + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "itemId", payload.get("itemId"),
                "source", ACTION_INSERT_ITEM,
                "nativeInterface", "EchoNativeRuntimeHost.Capabilities",
                "nativeMethod", "insertItem"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_INSERT_ITEM,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    public static NativeResult dispatchNativeMachineExtractItem(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            String itemId,
            int count) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine item extraction action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", "extract_item");
        payload.put("capabilityId", "item");
        payload.put("itemId", firstNonBlank(itemId, ""));
        payload.put("count", Math.max(1, count));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "machine-extract-" + player.getUUID() + "-"
                + compactActionKey(firstNonBlank(itemId, "item")) + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "itemId", payload.get("itemId"),
                "source", ACTION_EXTRACT_ITEM,
                "nativeInterface", "EchoNativeRuntimeHost.Capabilities",
                "nativeMethod", "extractItem"));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                ACTION_EXTRACT_ITEM,
                RUNTIME_HOST_ID,
                payload,
                playerRef(player),
                dimensionId(level),
                null,
                blockRef(level, pos),
                context));
    }

    public static NativeResult dispatchNativeMachineReceiveEnergy(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            int amount) {
        return dispatchNativeMachineEnergy(player, level, pos, machineId, ACTION_RECEIVE_ENERGY, "receive_energy", amount);
    }

    public static NativeResult dispatchNativeMachineExtractEnergy(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            int amount) {
        return dispatchNativeMachineEnergy(player, level, pos, machineId, ACTION_EXTRACT_ENERGY, "extract_energy", amount);
    }

    private static NativeResult dispatchNativeMachineEnergy(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            String machineId,
            String actionId,
            String actionName,
            int amount) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return NativeResult.noop("Machine energy action was not on a live server side.", Map.of());
        }
        register();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("machineId", firstNonBlank(machineId, machineIdAt(level, pos)));
        payload.put("action", actionName);
        payload.put("capabilityId", "energy");
        payload.put("amount", Math.max(1, amount));
        payload.put("position", positionSnapshot(level, pos));
        payload.put("playerId", player.getUUID().toString());
        payload.put("playerName", player.getName().getString());
        NativeMutationContext context = context(level, pos, "machine-energy-" + player.getUUID() + "-"
                + actionName + "-" + level.getGameTime(), Map.of(
                "playerId", player.getUUID().toString(),
                "playerName", player.getName().getString(),
                "machineId", payload.get("machineId"),
                "source", actionId,
                "nativeInterface", "EchoNativeRuntimeHost.Capabilities",
                "nativeMethod", actionName));
        return EchoRuntimeActionDispatcher.global().dispatch(new EchoRuntimeAction(
                actionId,
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

    @Override
    public Events events() {
        return events;
    }

    @Override
    public Hud hud() {
        return hud;
    }

    @Override
    public SaveData saveData() {
        return saveData;
    }

    private static void registerActions(EchoRuntimeActionDispatcher dispatcher) {
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_TICK, AshfallAdapterCoreMachineRuntimeHost::dispatchTick);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.tick", AshfallAdapterCoreMachineRuntimeHost::dispatchTick);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_SNAPSHOT, AshfallAdapterCoreMachineRuntimeHost::dispatchSnapshot);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.snapshot", AshfallAdapterCoreMachineRuntimeHost::dispatchSnapshot);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_APPLY_SNAPSHOT, AshfallAdapterCoreMachineRuntimeHost::dispatchApplySnapshot);
        dispatcher.registerAction(RUNTIME_HOST_ID, "BlockEntities.applySnapshot", AshfallAdapterCoreMachineRuntimeHost::dispatchApplySnapshot);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_INSERT_ITEM, AshfallAdapterCoreMachineRuntimeHost::dispatchInsertItem);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.insertItem", AshfallAdapterCoreMachineRuntimeHost::dispatchInsertItem);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ITEM, AshfallAdapterCoreMachineRuntimeHost::dispatchExtractItem);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.extractItem", AshfallAdapterCoreMachineRuntimeHost::dispatchExtractItem);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_RECEIVE_ENERGY, AshfallAdapterCoreMachineRuntimeHost::dispatchReceiveEnergy);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.receiveEnergy", AshfallAdapterCoreMachineRuntimeHost::dispatchReceiveEnergy);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ENERGY, AshfallAdapterCoreMachineRuntimeHost::dispatchExtractEnergy);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.extractEnergy", AshfallAdapterCoreMachineRuntimeHost::dispatchExtractEnergy);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_READ_CAPABILITY, AshfallAdapterCoreMachineRuntimeHost::dispatchReadCapability);
        dispatcher.registerAction(RUNTIME_HOST_ID, "Capabilities.readCapability", AshfallAdapterCoreMachineRuntimeHost::dispatchReadCapability);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_USE_BLOCK, AshfallAdapterCoreMachineRuntimeHost::dispatchUseBlockAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_STATE_CHANGED, AshfallAdapterCoreMachineRuntimeHost::dispatchStateChangedAction);
        dispatcher.registerAction(RUNTIME_HOST_ID, EVENT_OUTPUT_CREATED, AshfallAdapterCoreMachineRuntimeHost::dispatchOutputCreated);
    }

    private static EchoRuntimeActionOutcome dispatchTick(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        Map<String, Object> before = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult result = runtimeHost.blockEntities().tick(action.targetBlock(), action.context());
        Map<String, Object> after = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchSnapshot(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeBlockEntitySnapshot snapshot = runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context());
        NativeResult result = NativeResult.noop("Captured live machine block entity snapshot.", snapshot.state());
        return EchoRuntimeActionOutcome.of(Map.of(), result, summary(snapshot), false, false);
    }

    private static EchoRuntimeActionOutcome dispatchApplySnapshot(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
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

    private static EchoRuntimeActionOutcome dispatchInsertItem(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
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

    private static EchoRuntimeActionOutcome dispatchExtractItem(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
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

    private static EchoRuntimeActionOutcome dispatchReceiveEnergy(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "energy");
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().receiveEnergy(
                request,
                Math.max(1, intValue(action.inputPayload(), "amount", 1)),
                action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchExtractEnergy(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, "energy");
        Map<String, Object> before = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = runtimeHost.capabilities().extractEnergy(
                request,
                Math.max(1, intValue(action.inputPayload(), "amount", 1)),
                action.context());
        Map<String, Object> after = runtimeHost.capabilities().readCapability(request, action.context());
        return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), eventPublished(result));
    }

    private static EchoRuntimeActionOutcome dispatchReadCapability(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        NativeCapabilityRequest request = request(action, firstNonBlank(stringValue(action.inputPayload(), "capabilityId"), "machine"));
        Map<String, Object> snapshot = runtimeHost.capabilities().readCapability(request, action.context());
        NativeResult result = NativeResult.noop("Read live machine capability.", snapshot);
        return EchoRuntimeActionOutcome.of(snapshot, result, snapshot, false, false);
    }

    private static EchoRuntimeActionOutcome dispatchUseBlockAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        Map<String, Object> before = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult liveUse = boolValue(action.inputPayload(), "applyLiveUse", true)
                ? applyLiveUseBlock(action)
                : NativeResult.noop("Live machine use was already applied by the source runtime caller.", Map.of(
                        "liveUseSkipped", true,
                        "liveUseSupported", true));
        NativeResult save = runtimeHost.saveData().write(new NativeSaveData(
                "machine_interactions",
                machineSaveKey(action.targetBlock()),
                copyPayload(action.inputPayload(), Map.of(
                        "eventId", ACTION_USE_BLOCK,
                        "gameTime", action.context().gameTime(),
                        "liveUseMutated", liveUse.mutated(),
                        "liveUseStatus", liveUse.status(),
                        "playerId", action.targetPlayer() == null ? "" : action.targetPlayer().playerId()))), action.context());
        NativeResult event = runtimeHost.events().publish(new NativeEvent(
                ACTION_USE_BLOCK,
                action.targetPlayer(),
                copyPayload(action.inputPayload(), Map.of(
                        "block", blockSnapshot(action.targetBlock()),
                        "liveUseMutated", liveUse.mutated(),
                        "liveUseStatus", liveUse.status()))), action.context());
        Map<String, Object> after = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult result = liveUse.mutated()
                ? NativeResult.mutated("Used live machine block through AdapterCore player action path.", copyPayload(
                        liveUse.snapshot(), Map.of(
                                "eventPublished", eventPublished(event),
                                "saveUpdated", save.mutated(),
                                "saveStatus", save.status(),
                                "eventStatus", event.status())))
                : save.mutated() ? save : event;
        return EchoRuntimeActionOutcome.of(before, result, after, save.mutated(), true);
    }

    private static NativeResult applyLiveUseBlock(EchoRuntimeAction action) {
        ResolvedBlock resolved = resolve(action.targetBlock());
        ServerPlayer player = action.targetPlayer() == null ? playerFromContext(action.context()) : resolvePlayer(action.targetPlayer());
        if (resolved.state() != null && resolved.state().getBlock() instanceof EmergencyBunkBlock) {
            if (player == null) {
                return NativeResult.noop("No online player was available for live emergency bunk use.", Map.of(
                        "liveUseSupported", false,
                        "machineId", EchoAshfallProtocol.MODID + ":emergency_bunk",
                        "block", blockSnapshot(action.targetBlock())));
            }
            InteractionResult interaction = EmergencyBunkBlock.useEmergencyBunk(
                    resolved.state(),
                    resolved.level(),
                    resolved.pos(),
                    player,
                    "native_client_machine.use_block");
            boolean anchored = interaction == InteractionResult.SUCCESS_SERVER;
            return anchored
                    ? NativeResult.mutated("Used live EmergencyBunkBlock.useEmergencyBunk through AdapterCore.", Map.of(
                            "liveUseSupported", true,
                            "realUseMethod", "EmergencyBunkBlock.useEmergencyBunk",
                            "machineId", EchoAshfallProtocol.MODID + ":emergency_bunk",
                            "block", blockSnapshot(action.targetBlock()),
                            "respawnAnchored", true,
                            "missionMarker", "shelter:slept"))
                    : NativeResult.noop("Emergency bunk did not anchor recovery through live block use.", Map.of(
                            "liveUseSupported", true,
                            "realUseMethod", "EmergencyBunkBlock.useEmergencyBunk",
                            "machineId", EchoAshfallProtocol.MODID + ":emergency_bunk",
                            "block", blockSnapshot(action.targetBlock()),
                            "respawnAnchored", false));
        }
        if (resolved.state() != null && resolved.state().getBlock() instanceof RelayStationBlock relay) {
            if (player == null) {
                return NativeResult.noop("No online player was available for live relay station use.", Map.of(
                        "liveUseSupported", false,
                        "machineId", EchoAshfallProtocol.MODID + ":relay_station",
                        "block", blockSnapshot(action.targetBlock())));
            }
            BlockState beforeState = resolved.state();
            BlockPos beforePlayerPos = player.blockPosition();
            InteractionResult interaction = relay.useRelayStation(beforeState, resolved.level(), resolved.pos(), player);
            BlockState afterState = resolved.level().getBlockState(resolved.pos());
            BlockPos afterPlayerPos = player.blockPosition();
            boolean blockStateChanged = !afterState.equals(beforeState);
            boolean playerMoved = !afterPlayerPos.equals(beforePlayerPos);
            Map<String, Object> snapshot = Map.of(
                    "liveUseSupported", true,
                    "realUseMethod", "RelayStationBlock.useRelayStation",
                    "machineId", EchoAshfallProtocol.MODID + ":relay_station",
                    "block", blockSnapshot(action.targetBlock()),
                    "interactionResult", interaction.toString(),
                    "blockStateChanged", blockStateChanged,
                    "playerMoved", playerMoved,
                    "repaired", afterState.hasProperty(RelayStationBlock.REPAIRED) && afterState.getValue(RelayStationBlock.REPAIRED),
                    "active", afterState.hasProperty(RelayStationBlock.ACTIVE) && afterState.getValue(RelayStationBlock.ACTIVE));
            return blockStateChanged || playerMoved
                    ? NativeResult.mutated("Used live RelayStationBlock.useRelayStation through AdapterCore.", snapshot)
                    : NativeResult.noop("Relay station use completed without live state mutation.", snapshot);
        }
        if (resolved.state() != null && resolved.state().getBlock() instanceof StructureCacheBlock) {
            if (player == null) {
                return NativeResult.noop("No online player was available for live structure cache use.", Map.of(
                        "liveUseSupported", false,
                        "machineId", EchoAshfallProtocol.MODID + ":recovery_cache",
                        "block", blockSnapshot(action.targetBlock())));
            }
            NativeResult cacheUse = StructureCacheBlock.useStructureCache(
                    resolved.level(),
                    resolved.pos(),
                    player,
                    "native_client_machine.use_block",
                    false);
            return cacheUse.mutated()
                    ? NativeResult.mutated("Used live StructureCacheBlock.useStructureCache through AdapterCore.", copyPayload(
                            cacheUse.snapshot(), Map.of(
                                    "liveUseSupported", true,
                                    "realUseMethod", "StructureCacheBlock.useStructureCache",
                                    "machineId", EchoAshfallProtocol.MODID + ":recovery_cache",
                                    "block", blockSnapshot(action.targetBlock()))))
                    : NativeResult.noop("Structure cache use completed without live state mutation.", copyPayload(
                            cacheUse.snapshot(), Map.of(
                                    "liveUseSupported", true,
                                    "realUseMethod", "StructureCacheBlock.useStructureCache",
                                    "machineId", EchoAshfallProtocol.MODID + ":recovery_cache",
                                    "block", blockSnapshot(action.targetBlock()))));
        }
        if (resolved.state() != null && resolved.state().getBlock() instanceof NexusCoreBlock) {
            if (player == null) {
                return NativeResult.noop("No online player was available for live Nexus Core use.", Map.of(
                        "liveUseSupported", false,
                        "machineId", EchoAshfallProtocol.MODID + ":nexus_core",
                        "block", blockSnapshot(action.targetBlock())));
            }
            boolean discoveredBefore = resolved.entity() instanceof NexusCoreBlockEntity core && core.isDiscovered();
            InteractionResult interaction = NexusCoreBlock.useNexusCore(resolved.level(), resolved.pos(), player);
            BlockEntity afterEntity = resolved.level().getBlockEntity(resolved.pos());
            boolean discoveredAfter = afterEntity instanceof NexusCoreBlockEntity core && core.isDiscovered();
            Map<String, Object> snapshot = Map.of(
                    "liveUseSupported", true,
                    "realUseMethod", "NexusCoreBlock.useNexusCore",
                    "machineId", EchoAshfallProtocol.MODID + ":nexus_core",
                    "block", blockSnapshot(action.targetBlock()),
                    "interactionResult", interaction.toString(),
                    "discoveredBefore", discoveredBefore,
                    "discoveredAfter", discoveredAfter,
                    "visibleFeedback", true);
            return discoveredAfter && !discoveredBefore
                    ? NativeResult.mutated("Used live NexusCoreBlock.useNexusCore through AdapterCore.", snapshot)
                    : NativeResult.noop("Nexus Core use completed through live block path.", snapshot);
        }
        if (resolved.state() != null && resolved.state().getBlock() instanceof ResearchLabBlock) {
            if (player == null) {
                return NativeResult.noop("No online player was available for live Research Lab use.", Map.of(
                        "liveUseSupported", false,
                        "machineId", EchoAshfallProtocol.MODID + ":research_lab",
                        "block", blockSnapshot(action.targetBlock())));
            }
            InteractionResult interaction = ResearchLabBlock.useResearchLab(resolved.level(), resolved.pos(), player);
            Map<String, Object> snapshot = Map.of(
                    "liveUseSupported", true,
                    "realUseMethod", "ResearchLabBlock.useResearchLab",
                    "machineId", EchoAshfallProtocol.MODID + ":research_lab",
                    "block", blockSnapshot(action.targetBlock()),
                    "interactionResult", interaction.toString(),
                    "visibleFeedback", true);
            return NativeResult.mutated("Used live ResearchLabBlock.useResearchLab through AdapterCore.", snapshot);
        }
        if (resolved.entity() == null) {
            return NativeResult.noop("No live machine block entity handled AdapterCore use.", Map.of(
                    "liveUseSupported", false,
                    "loaded", resolved.loaded(),
                    "block", blockSnapshot(action.targetBlock())));
        }
        if (player == null) {
            return NativeResult.noop("No online player was available for live machine use.", Map.of(
                    "liveUseSupported", false,
                    "machineId", machineId(resolved.entity()),
                    "block", blockSnapshot(action.targetBlock())));
        }
        if (resolved.entity() instanceof RainCollectorBlockEntity collector) {
            boolean filled = fillRainCollectorBottle(resolved.level(), player, collector);
            Map<String, Object> snapshot = copyPayload(
                    machineSnapshot(resolved.level(), resolved.pos(), resolved.state(), collector, action.context()),
                    Map.of(
                            "liveUseSupported", true,
                            "realUseMethod", "RainCollectorBlockEntity.fillBottle",
                            "dirtyWaterCollected", filled));
            return filled
                    ? NativeResult.mutated("Filled dirty water bottle through live RainCollectorBlockEntity.fillBottle.", snapshot)
                    : NativeResult.noop("Rain collector did not fill a bottle through live block use.", snapshot);
        }
        return NativeResult.noop("Machine use has no specialized live block interaction.", Map.of(
                "liveUseSupported", false,
                "machineId", machineId(resolved.entity()),
                "blockEntityId", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(resolved.entity().getType()).toString()));
    }

    private static boolean fillRainCollectorBottle(Level level, ServerPlayer player, RainCollectorBlockEntity collector) {
        if (collector.getStoredBottles() <= 0) {
            return collector.fillBottle(level, player, InteractionHand.MAIN_HAND);
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.GLASS_BOTTLE)) {
            return collector.fillBottle(level, player, InteractionHand.MAIN_HAND);
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).is(Items.GLASS_BOTTLE)) {
            return collector.fillBottle(level, player, InteractionHand.OFF_HAND);
        }
        InteractionHand emptyHand = player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                ? InteractionHand.MAIN_HAND
                : player.getItemInHand(InteractionHand.OFF_HAND).isEmpty() ? InteractionHand.OFF_HAND : null;
        if (emptyHand == null) {
            return false;
        }
        Container inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(Items.GLASS_BOTTLE)) {
                continue;
            }
            ItemStack stagedBottle = stack.copy();
            stagedBottle.setCount(1);
            stack.shrink(1);
            inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            player.setItemInHand(emptyHand, stagedBottle);
            boolean filled = collector.fillBottle(level, player, emptyHand);
            if (!filled) {
                ItemStack remainingHand = player.getItemInHand(emptyHand);
                player.setItemInHand(emptyHand, ItemStack.EMPTY);
                ItemStack restoredSlot = inventory.getItem(slot);
                if (restoredSlot.isEmpty()) {
                    inventory.setItem(slot, remainingHand.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : remainingHand);
                } else if (restoredSlot.is(Items.GLASS_BOTTLE)) {
                    restoredSlot.grow(1);
                    inventory.setItem(slot, restoredSlot);
                } else if (!remainingHand.isEmpty()) {
                    player.drop(remainingHand, false);
                }
            }
            return filled;
        }
        return false;
    }

    private static EchoRuntimeActionOutcome dispatchStateChangedAction(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        Map<String, Object> before = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult save = runtimeHost.saveData().write(new NativeSaveData(
                "machine_state",
                machineSaveKey(action.targetBlock()),
                copyPayload(action.inputPayload(), Map.of(
                        "eventId", ACTION_STATE_CHANGED,
                        "gameTime", action.context().gameTime(),
                        "playerId", action.targetPlayer() == null ? "" : action.targetPlayer().playerId()))), action.context());
        NativeResult event = runtimeHost.events().publish(new NativeEvent(
                ACTION_STATE_CHANGED,
                action.targetPlayer(),
                copyPayload(action.inputPayload(), Map.of("block", blockSnapshot(action.targetBlock())))), action.context());
        Map<String, Object> after = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult result = save.mutated() ? save : event;
        return EchoRuntimeActionOutcome.of(before, result, after, save.mutated(), true);
    }

    private static EchoRuntimeActionOutcome dispatchOutputCreated(EchoNativeRuntimeHost runtimeHost, EchoRuntimeAction action) {
        Map<String, Object> before = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult save = runtimeHost.saveData().write(new NativeSaveData(
                "machine_outputs",
                machineSaveKey(action.targetBlock()),
                copyPayload(action.inputPayload(), Map.of(
                        "eventId", EVENT_OUTPUT_CREATED,
                        "gameTime", action.context().gameTime()))), action.context());
        NativeResult event = runtimeHost.events().publish(new NativeEvent(
                EVENT_OUTPUT_CREATED,
                action.targetPlayer(),
                action.inputPayload()), action.context());
        Map<String, Object> after = summary(runtimeHost.blockEntities().snapshot(action.targetBlock(), action.context()));
        NativeResult result = event.mutated() ? event : save;
        return EchoRuntimeActionOutcome.of(before, result, after, save.mutated(), true);
    }

    private final class AshfallBlockEntities implements BlockEntities {
        @Override
        public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(block);
            if (resolved.entity() == null) {
                return NativeResult.unsupported("No live machine block entity is loaded at AdapterCore target.", Map.of(
                        "block", blockSnapshot(block),
                        "loaded", resolved.loaded()));
            }
            NativeBlockEntitySnapshot beforeSnapshot = snapshot(block, context);
            BlockEntity entity = resolved.entity();
            if (entity instanceof WaterPurifierBlockEntity purifier) {
                WaterPurifierBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), purifier);
            } else if (entity instanceof RainCollectorBlockEntity collector) {
                RainCollectorBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), collector);
            } else if (entity instanceof HandRecyclerBlockEntity recycler) {
                HandRecyclerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), recycler);
            } else if (entity instanceof MicroGeneratorBlockEntity generator) {
                MicroGeneratorBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), generator);
            } else if (entity instanceof ThermalBurnerBlockEntity burner) {
                ThermalBurnerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), burner);
            } else if (entity instanceof ThermalArrayBlockEntity array) {
                ThermalArrayBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), array);
            } else if (entity instanceof ScrapDynamoBlockEntity dynamo) {
                ScrapDynamoBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), dynamo);
            } else if (entity instanceof PowerNodeBlockEntity node) {
                PowerNodeBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), node);
            } else if (entity instanceof PowerCableBlockEntity cable) {
                PowerCableBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), cable);
            } else if (entity instanceof BatteryBankBlockEntity battery) {
                BatteryBankBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), battery);
            } else if (entity instanceof NexusCapacitorBlockEntity capacitor) {
                NexusCapacitorBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), capacitor);
            } else if (entity instanceof LoadDistributorBlockEntity distributor) {
                LoadDistributorBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), distributor);
            } else if (entity instanceof FactoryControllerBlockEntity controller) {
                FactoryControllerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), controller);
            } else if (entity instanceof FieldMedBayBlockEntity medBay) {
                FieldMedBayBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), medBay);
            } else if (entity instanceof AtmosphericScrubberBlockEntity scrubber) {
                AtmosphericScrubberBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), scrubber);
            } else if (entity instanceof RadiationCleanserBlockEntity cleanser) {
                RadiationCleanserBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), cleanser);
            } else if (entity instanceof ScrapPressBlockEntity press) {
                ScrapPressBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), press);
            } else if (entity instanceof OreGrinderBlockEntity grinder) {
                OreGrinderBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), grinder);
            } else if (entity instanceof FilterWorkbenchBlockEntity workbench) {
                FilterWorkbenchBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), workbench);
            } else if (entity instanceof IsotopeRefinerBlockEntity refiner) {
                IsotopeRefinerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), refiner);
            } else if (entity instanceof CrystallineSynthesizerBlockEntity synthesizer) {
                CrystallineSynthesizerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), synthesizer);
            } else if (entity instanceof DeepCoreMinerBlockEntity miner) {
                DeepCoreMinerBlockEntity.serverTick(resolved.level(), resolved.pos(), resolved.state(), miner);
            } else if (entity instanceof StructureCacheBlockEntity) {
                return NativeResult.noop("Recovery cache has no server tick, but snapshot/save capability is live.", beforeSnapshot.state());
            } else if (isTerminal(entity)) {
                entity.setChanged();
                return NativeResult.noop("Terminal has no machine tick, but inventory/save capability is live.", beforeSnapshot.state());
            } else {
                return NativeResult.unsupported("Block entity is not one of the wired AdapterCore machines.", beforeSnapshot.state());
            }

            NativeBlockEntitySnapshot afterSnapshot = snapshot(block, context);
            boolean changed = !beforeSnapshot.state().equals(afterSnapshot.state());
            return changed
                    ? NativeResult.mutated("Ticked live machine block entity and mutated persisted machine state.", afterSnapshot.state())
                    : NativeResult.noop("Ticked live machine block entity with no state change.", afterSnapshot.state());
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
            Map<String, Object> state = machineSnapshot(resolved.level(), resolved.pos(), resolved.state(), entity, context);
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
                return NativeResult.unsupported("No live machine block entity is loaded for snapshot application.", Map.of(
                        "block", blockSnapshot(snapshot.block())));
            }
            Map<String, Object> before = machineSnapshot(resolved.level(), resolved.pos(), resolved.state(), entity, context);
            boolean changed = false;

            Container inventory = inventory(entity);
            if (inventory != null && snapshot.state().containsKey("inventorySlots")) {
                applyInventory(inventory, listValue(snapshot.state(), "inventorySlots"));
                changed = true;
            }
            if (entity instanceof IEnergyStorage energy && snapshot.state().containsKey("energyStored")) {
                energy.setEnergyStored(intValue(snapshot.state(), "energyStored", energy.getEnergyStored()));
                changed = true;
            }
            changed |= applyProgress(entity, snapshot.state());
            if (snapshot.state().containsKey("active")) {
                changed |= setActive(resolved.level(), resolved.pos(), resolved.state(), boolValue(snapshot.state(), "active", false));
            }
            if (changed) {
                entity.setChanged();
            }
            Map<String, Object> after = machineSnapshot(resolved.level(), resolved.pos(), resolved.level().getBlockState(resolved.pos()), entity, context);
            return changed
                    ? NativeResult.mutated("Applied AdapterCore machine snapshot to live block entity state.", copyPayload(after, Map.of("before", before)))
                    : NativeResult.noop("AdapterCore machine snapshot matched live state.", after);
        }
    }

    private final class AshfallCapabilities implements Capabilities {
        @Override
        public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            ItemStack toInsert = nativeStack(stack);
            if (toInsert.isEmpty()) {
                return NativeResult.failed("AdapterCore item insertion received an unknown or empty item stack.", Map.of(
                        "itemId", stack.itemId(),
                        "count", stack.count()));
            }
            if (entity instanceof ScrapDynamoBlockEntity dynamo) {
                Map<String, Object> before = capabilitySnapshot(request, context);
                int requested = toInsert.getCount();
                if (!dynamo.isFuel(toInsert)) {
                    return NativeResult.noop("Scrap dynamo rejected a non-fuel AdapterCore item stack.", copyPayload(before, Map.of(
                            "inserted", 0,
                            "requested", stack.count(),
                            "itemId", stack.itemId())));
                }
                dynamo.addFuel(toInsert);
                int inserted = requested - toInsert.getCount();
                Map<String, Object> after = capabilitySnapshot(request, context);
                return inserted > 0
                        ? NativeResult.mutated("Inserted fuel into live scrap dynamo burn state.", copyPayload(after, Map.of(
                                "inserted", inserted,
                                "requested", stack.count(),
                                "itemId", stack.itemId())))
                        : NativeResult.noop("Scrap dynamo did not accept the AdapterCore item stack.", copyPayload(before, Map.of(
                                "inserted", 0,
                                "requested", stack.count(),
                                "itemId", stack.itemId())));
            }
            Container inventory = inventory(entity);
            if (entity == null || inventory == null) {
                return NativeResult.unsupported("Target block entity does not expose a live inventory capability.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = capabilitySnapshot(request, context);
            int inserted = insertIntoInventory(entity, inventory, toInsert, side(request.side()));
            if (inserted > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilitySnapshot(request, context);
            return inserted > 0
                    ? NativeResult.mutated("Inserted item stack into live native machine inventory.", copyPayload(after, Map.of(
                            "inserted", inserted,
                            "requested", stack.count(),
                            "itemId", stack.itemId())))
                    : NativeResult.noop("No machine slot accepted the AdapterCore item stack.", copyPayload(before, Map.of(
                            "inserted", 0,
                            "requested", stack.count(),
                            "itemId", stack.itemId())));
        }

        @Override
        public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            Container inventory = inventory(entity);
            if (entity == null || inventory == null) {
                return NativeResult.unsupported("Target block entity does not expose a live inventory capability.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = capabilitySnapshot(request, context);
            ItemStack extracted = extractFromInventory(entity, inventory, firstNonBlank(itemId, ""), Math.max(1, count), side(request.side()));
            if (!extracted.isEmpty()) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilitySnapshot(request, context);
            return !extracted.isEmpty()
                    ? NativeResult.mutated("Extracted item stack from live native machine inventory.", copyPayload(after, Map.of(
                            "extracted", extracted.getCount(),
                            "itemId", itemId(extracted))))
                    : NativeResult.noop("No matching machine output stack was available for AdapterCore extraction.", copyPayload(before, Map.of(
                            "extracted", 0,
                            "itemId", firstNonBlank(itemId, ""))));
        }

        @Override
        public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            if (!(entity instanceof IEnergyStorage energy)) {
                return NativeResult.unsupported("Target block entity does not expose a live energy receive capability.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = capabilitySnapshot(request, context);
            int received = energy.receiveEnergy(Math.max(1, amount), false);
            if (received > 0) {
                if (entity instanceof PowerNodeBlockEntity node) {
                    node.activate();
                    setActive(resolved.level(), resolved.pos(), resolved.state(), true);
                }
                entity.setChanged();
            }
            Map<String, Object> after = capabilitySnapshot(request, context);
            NativeResult event = received > 0
                    ? publishEnergyStateChanged(request, context, entity, after, "energy_received", received)
                    : NativeResult.noop("No energy state event was published because the machine did not receive energy.", after);
            return received > 0
                    ? NativeResult.mutated("Received energy into live native machine storage.", copyPayload(after, Map.of(
                            "received", received,
                            "eventPublished", eventPublished(event),
                            "stateEvent", event.snapshot())))
                    : NativeResult.noop("Machine energy storage did not accept AdapterCore energy.", copyPayload(before, Map.of("received", 0)));
        }

        @Override
        public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
            ResolvedBlock resolved = resolve(request.block());
            BlockEntity entity = resolved.entity();
            if (!(entity instanceof IEnergyStorage energy)) {
                return NativeResult.unsupported("Target block entity does not expose a live energy extract capability.", Map.of(
                        "capabilityId", request.capabilityId(),
                        "block", blockSnapshot(request.block())));
            }
            Map<String, Object> before = capabilitySnapshot(request, context);
            int extracted = energy.extractEnergy(Math.max(1, amount), false);
            if (extracted > 0) {
                entity.setChanged();
            }
            Map<String, Object> after = capabilitySnapshot(request, context);
            NativeResult event = extracted > 0
                    ? publishEnergyStateChanged(request, context, entity, after, "energy_extracted", extracted)
                    : NativeResult.noop("No energy state event was published because the machine did not extract energy.", after);
            return extracted > 0
                    ? NativeResult.mutated("Extracted energy from live native machine storage.", copyPayload(after, Map.of(
                            "extracted", extracted,
                            "eventPublished", eventPublished(event),
                            "stateEvent", event.snapshot())))
                    : NativeResult.noop("Machine energy storage did not provide AdapterCore energy.", copyPayload(before, Map.of("extracted", 0)));
        }

        @Override
        public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
            return capabilitySnapshot(request, context);
        }

        private NativeResult publishEnergyStateChanged(
                NativeCapabilityRequest request,
                NativeMutationContext context,
                BlockEntity entity,
                Map<String, Object> capabilitySnapshot,
                String source,
                int amount) {
            String machineId = machineId(entity);
            Map<String, Object> energySnapshot = mapValue(capabilitySnapshot, "energy");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("machineId", machineId);
            payload.put("canonicalMachineId", canonicalMachineId(machineId, entity));
            payload.put("action", "state_changed");
            payload.put("source", source);
            payload.put("position", blockSnapshot(request.block()));
            payload.put("state", Map.of(
                    "energyStored", intValue(energySnapshot, "stored", 0),
                    "energyCapacity", intValue(energySnapshot, "capacity", 0),
                    "amount", Math.max(0, amount),
                    "source", source));
            return events.publish(new NativeEvent(
                    ACTION_STATE_CHANGED,
                    playerFromContext(context) == null ? null : playerRef(playerFromContext(context)),
                    payload), context);
        }
    }

    private final class AshfallEvents implements Events {
        @Override
        public NativeResult publish(NativeEvent event, NativeMutationContext context) {
            if (event == null) {
                return NativeResult.failed("AdapterCore event was missing.", Map.of("failureReason", "missing event"));
            }
            ServerPlayer player = event.player() == null ? playerFromContext(context) : resolvePlayer(event.player());
            Map<String, Object> payload = event.payload();
            String machineId = firstNonBlank(stringValue(payload, "machineId"), stringValue(context.metadata(), "machineId"));
            if (EVENT_OUTPUT_CREATED.equals(event.eventId())) {
                String outputItemId = stringValue(mapValue(payload, "output"), "item");
                boolean powerMissionAdvanced = player != null
                        && AshfallAdapterCoreMissionTriggerRuntime.machinePowered(player, machineId);
                boolean outputMissionAdvanced = player != null
                        && !outputItemId.isBlank()
                        && AshfallAdapterCoreMissionTriggerRuntime.machineOutputCreated(player, outputItemId);
                boolean missionAdvanced = powerMissionAdvanced || outputMissionAdvanced;
                if (player != null) {
                    player.sendSystemMessage(Component.literal("[ECHO-7] Machine output created: "
                            + firstNonBlank(itemPath(outputItemId), "output")
                            + "."));
                    writePlayerMachineSave(player, "last_output_machine", machineId);
                    writePlayerMachineSave(player, "last_output_item", outputItemId);
                    writePlayerMachineSave(player, "last_output_event", EVENT_OUTPUT_CREATED);
                }
                boolean blockSaved = markBlockEntityChanged(payload);
                boolean liveEffect = missionAdvanced || player != null || blockSaved;
                Map<String, Object> snapshot = copyPayload(payload, Map.of(
                        "eventPublished", true,
                        "missionAdvanced", missionAdvanced,
                        "powerMissionAdvanced", powerMissionAdvanced,
                        "outputMissionAdvanced", outputMissionAdvanced,
                        "hudVisible", player != null,
                        "saveUpdated", player != null || blockSaved,
                        "blockSaveUpdated", blockSaved));
                return liveEffect
                        ? NativeResult.mutated("Published machine.output_created through AdapterCore and live mission/HUD/save hooks.", snapshot)
                        : NativeResult.noop("Machine output event had no live mission, HUD, player save, or block save effect.", snapshot);
            }
            if (ACTION_USE_BLOCK.equals(event.eventId())) {
                if (player != null) {
                    writePlayerMachineSave(player, "last_used_machine", machineId);
                    writePlayerMachineSave(player, "last_used_at", Long.toString(context.gameTime()));
                }
                Map<String, Object> snapshot = copyPayload(payload, Map.of(
                        "eventPublished", player != null,
                        "hudVisible", player != null,
                        "saveUpdated", player != null));
                return player != null
                        ? NativeResult.mutated("Published machine use through AdapterCore player action path.", snapshot)
                        : NativeResult.noop("Machine use event had no online player save or HUD effect.", snapshot);
            }
            if (ACTION_STATE_CHANGED.equals(event.eventId())) {
                boolean powerMissionAdvanced = player != null
                        && AshfallAdapterCoreMissionTriggerRuntime.machinePowered(player, machineId);
                if (player != null) {
                    writePlayerMachineSave(player, "last_state_machine", machineId);
                    writePlayerMachineSave(player, "last_state_event", ACTION_STATE_CHANGED);
                    writePlayerMachineSave(player, "last_state_at", Long.toString(context.gameTime()));
                }
                boolean blockSaved = markBlockEntityChanged(payload);
                boolean liveEffect = powerMissionAdvanced || player != null || blockSaved;
                Map<String, Object> snapshot = copyPayload(payload, Map.of(
                        "eventPublished", true,
                        "missionAdvanced", powerMissionAdvanced,
                        "powerMissionAdvanced", powerMissionAdvanced,
                        "hudVisible", player != null,
                        "saveUpdated", player != null || blockSaved,
                        "blockSaveUpdated", blockSaved));
                return liveEffect
                        ? NativeResult.mutated("Published machine.state_changed through AdapterCore and live save hooks.", snapshot)
                        : NativeResult.noop("Machine state event had no live mission, HUD, player save, or block save effect.", snapshot);
            }
            return NativeResult.unsupported("AdapterCore machine event id is not wired by this host.", Map.of(
                    "eventId", event.eventId(),
                    "payload", payload));
        }
    }

    private final class AshfallHud implements Hud {
        @Override
        public NativeResult publishNotification(NativePlayerRef playerRef, Map<String, Object> payload, NativeMutationContext context) {
            ServerPlayer player = resolvePlayer(playerRef);
            if (player == null) {
                return NativeResult.failed("AdapterCore HUD player is not online.", Map.of(
                        "playerId", playerRef == null ? "" : playerRef.playerId()));
            }
            player.sendSystemMessage(Component.literal(firstNonBlank(stringValue(payload, "message"), "[ECHO-7] Machine state updated.")));
            return NativeResult.mutated("Published AdapterCore machine HUD notification.", copyPayload(payload, Map.of("hudVisible", true)));
        }
    }

    private final class AshfallSaveData implements SaveData {
        @Override
        public NativeResult write(NativeSaveData data, NativeMutationContext context) {
            if (data == null) {
                return NativeResult.failed("AdapterCore save data was missing.", Map.of("failureReason", "missing save data"));
            }
            ServerPlayer player = playerFromPayload(data.payload());
            if (player == null) {
                player = playerFromContext(context);
            }
            boolean saved = false;
            if (player != null) {
                CompoundTag root = player.getPersistentData().getCompoundOrEmpty(SAVE_ROOT).copy();
                CompoundTag scope = root.getCompoundOrEmpty(data.scope()).copy();
                CompoundTag entry = compoundFromMap(data.payload());
                entry.putLong("writtenAtGameTime", context.gameTime());
                scope.put(safeTagKey(data.key()), entry);
                root.put(safeTagKey(data.scope()), scope);
                player.getPersistentData().put(SAVE_ROOT, root);
                saved = true;
            }
            boolean blockSaved = markBlockEntityChanged(data.payload());
            Map<String, Object> snapshot = Map.of(
                    "scope", data.scope(),
                    "key", data.key(),
                    "playerSaveUpdated", saved,
                    "blockSaveUpdated", blockSaved);
            return saved || blockSaved
                    ? NativeResult.mutated("Wrote AdapterCore machine save data into live player/block persistent state.", snapshot)
                    : NativeResult.noop("AdapterCore machine save data had no live player or block persistence target.", snapshot);
        }

        @Override
        public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
            ServerPlayer player = playerFromContext(context);
            if (player == null) {
                return Map.of("scope", scope, "key", key, "present", false);
            }
            CompoundTag root = player.getPersistentData().getCompoundOrEmpty(SAVE_ROOT);
            CompoundTag scopeTag = root.getCompoundOrEmpty(safeTagKey(scope));
            CompoundTag entry = scopeTag.getCompoundOrEmpty(safeTagKey(key));
            return Map.of(
                    "scope", scope,
                    "key", key,
                    "present", !entry.isEmpty(),
                    "snapshot", entry.toString());
        }

        @Override
        public NativeResult delete(String scope, String key, NativeMutationContext context) {
            ServerPlayer player = playerFromContext(context);
            if (player == null) {
                return NativeResult.noop("No online player save scope matched AdapterCore machine delete.", Map.of(
                        "scope", scope,
                        "key", key));
            }
            CompoundTag root = player.getPersistentData().getCompoundOrEmpty(SAVE_ROOT).copy();
            CompoundTag scopeTag = root.getCompoundOrEmpty(safeTagKey(scope)).copy();
            boolean present = !scopeTag.getCompoundOrEmpty(safeTagKey(key)).isEmpty();
            scopeTag.remove(safeTagKey(key));
            root.put(safeTagKey(scope), scopeTag);
            player.getPersistentData().put(SAVE_ROOT, root);
            return present
                    ? NativeResult.mutated("Deleted AdapterCore machine save data.", Map.of("scope", scope, "key", key))
                    : NativeResult.noop("AdapterCore machine save data key was already absent.", Map.of("scope", scope, "key", key));
        }
    }

    private static Map<String, Object> machineSnapshot(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockEntity entity,
            NativeMutationContext context) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        String machineId = machineId(entity);
        snapshot.put("machineId", machineId);
        snapshot.put("canonicalMachineId", canonicalMachineId(machineId, entity));
        snapshot.put("blockId", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        snapshot.put("blockEntityId", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
        snapshot.put("position", positionSnapshot(level, pos));
        snapshot.put("ownerPlayerId", ownerPlayerId(entity, context));

        Container inventory = inventory(entity);
        List<Map<String, Object>> inventorySlots = inventorySlots(inventory);
        snapshot.put("inventorySlots", inventorySlots);
        snapshot.put("inputSlots", slots(inventory, inputSlots(entity)));
        snapshot.put("outputSlots", slots(inventory, outputSlots(entity)));

        if (entity instanceof IEnergyStorage energy) {
            snapshot.put("energyStored", energy.getEnergyStored());
            snapshot.put("energyCapacity", energy.getMaxEnergyStored());
            snapshot.put("canReceiveEnergy", energy.canReceive());
            snapshot.put("canExtractEnergy", energy.canExtract());
        } else {
            snapshot.put("energyStored", 0);
            snapshot.put("energyCapacity", 0);
            snapshot.put("canReceiveEnergy", false);
            snapshot.put("canExtractEnergy", false);
        }

        ProgressState progress = progress(entity, state);
        snapshot.put("progress", progress.progress());
        snapshot.put("maxProgress", progress.maxProgress());
        snapshot.put("active", progress.active());
        snapshot.put("lastRecipeId", lastRecipeId(entity, inventory));
        snapshot.put("saveTag", entity.saveWithFullMetadata(level.registryAccess()).toString());
        return Map.copyOf(snapshot);
    }

    private static Map<String, Object> capabilitySnapshot(NativeCapabilityRequest request, NativeMutationContext context) {
        ResolvedBlock resolved = resolve(request.block());
        BlockEntity entity = resolved.entity();
        if (entity == null) {
            return Map.of(
                    "capabilityId", request.capabilityId(),
                    "loaded", resolved.loaded(),
                    "block", blockSnapshot(request.block()),
                    "inventory", Map.of("present", false),
                    "energy", Map.of("present", false));
        }
        Container inventory = inventory(entity);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("capabilityId", request.capabilityId());
        snapshot.put("side", request.side());
        snapshot.put("machineId", machineId(entity));
        snapshot.put("canonicalMachineId", canonicalMachineId(machineId(entity), entity));
        snapshot.put("block", blockSnapshot(request.block()));
        snapshot.put("inventory", Map.of(
                "present", inventory != null,
                "slots", inventorySlots(inventory),
                "inputSlots", slots(inventory, inputSlots(entity)),
                "outputSlots", slots(inventory, outputSlots(entity))));
        if (entity instanceof IEnergyStorage energy) {
            snapshot.put("energy", Map.of(
                    "present", true,
                    "stored", energy.getEnergyStored(),
                    "capacity", energy.getMaxEnergyStored(),
                    "canReceive", energy.canReceive(),
                    "canExtract", energy.canExtract()));
        } else {
            snapshot.put("energy", Map.of("present", false, "stored", 0, "capacity", 0));
        }
        return Map.copyOf(snapshot);
    }

    private static ResolvedBlock resolve(NativeBlockRef block) {
        MinecraftServer server = currentServer();
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

    private static NativeMutationContext context(Level level, BlockPos pos, String key, Map<String, Object> metadata) {
        bindServer(level);
        return new NativeMutationContext(
                EchoAshfallProtocol.MODID,
                dimensionId(level),
                key,
                "server",
                level.getGameTime(),
                metadata == null ? Map.of() : metadata);
    }

    private static NativeBlockRef blockRef(Level level, BlockPos pos) {
        bindServer(level);
        return new NativeBlockRef(dimensionId(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static NativePlayerRef playerRef(ServerPlayer player) {
        return new NativePlayerRef(player.getUUID().toString());
    }

    private static ServerPlayer resolvePlayer(NativePlayerRef ref) {
        if (ref == null) {
            return null;
        }
        MinecraftServer server = currentServer();
        if (server == null) {
            return null;
        }
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(ref.playerId()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ServerPlayer playerFromContext(NativeMutationContext context) {
        if (context == null) {
            return null;
        }
        String playerId = stringValue(context.metadata(), "playerId");
        if (playerId.isBlank()) {
            return null;
        }
        return resolvePlayer(new NativePlayerRef(playerId));
    }

    private static ServerPlayer playerFromPayload(Map<String, Object> payload) {
        String playerId = stringValue(payload, "playerId");
        if (playerId.isBlank()) {
            return null;
        }
        return resolvePlayer(new NativePlayerRef(playerId));
    }

    private static void bindServer(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            bindServer(serverLevel.getServer());
        }
    }

    private static MinecraftServer currentServer() {
        return activeServer;
    }

    private static ServerPlayer nearestPlayer(Level level, BlockPos pos, double radius) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerPlayer nearest = null;
        double best = radius * radius;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            double distance = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }

    private static String machineId(BlockEntity entity) {
        if (entity == null) {
            return "";
        }
        return BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).toString();
    }

    private static String machineIdAt(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return "";
        }
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private static String canonicalMachineId(String machineId, BlockEntity entity) {
        String id = machineId == null ? "" : machineId.toLowerCase(Locale.ROOT);
        if (id.equals(EchoAshfallProtocol.MODID + ":structure_cache")) {
            return EchoAshfallProtocol.MODID + ":recovery_cache";
        }
        if (id.equals("signalos:workstation")) {
            return "signalos:terminal";
        }
        if (id.isBlank() && isTerminal(entity)) {
            return "signalos:terminal";
        }
        return id;
    }

    private static String ownerPlayerId(BlockEntity entity, NativeMutationContext context) {
        String contextPlayer = context == null ? "" : stringValue(context.metadata(), "playerId");
        if (!contextPlayer.isBlank()) {
            return contextPlayer;
        }
        Object reflected = invokeNoArg(entity, "ownerUUID");
        if (reflected != null) {
            return reflected.toString();
        }
        reflected = invokeNoArg(entity, "ownerId");
        return reflected == null ? "" : reflected.toString();
    }

    private static Container inventory(BlockEntity entity) {
        if (entity instanceof HopperHandler hopperHandler) {
            return hopperHandler.getInventory();
        }
        if (entity instanceof MicroGeneratorBlockEntity generator) {
            return generator.getInventory();
        }
        if (entity instanceof ThermalArrayBlockEntity array) {
            return array.getInventory();
        }
        if (entity instanceof BatteryBankBlockEntity battery) {
            return battery.getInventory();
        }
        if (entity instanceof RadiationCleanserBlockEntity cleanser) {
            return cleanser.getInventory();
        }
        if (entity instanceof Container container) {
            return container;
        }
        Object bootDrive = invokeNoArg(entity, "bootDrive");
        if (bootDrive instanceof Container container) {
            return container;
        }
        return null;
    }

    private static int[] inputSlots(BlockEntity entity) {
        if (entity instanceof HopperHandler hopperHandler) {
            return uniqueSlots(Direction.values(), hopperHandler::getInputSlots);
        }
        Container inventory = inventory(entity);
        if (entity instanceof MicroGeneratorBlockEntity && inventory != null) {
            return new int[]{0, MicroGeneratorBlockEntity.BATTERY_SLOT};
        }
        if (entity instanceof ThermalArrayBlockEntity && inventory != null) {
            return new int[]{0, 1, 2, ThermalArrayBlockEntity.BATTERY_SLOT};
        }
        if (entity instanceof BatteryBankBlockEntity && inventory != null) {
            return new int[]{BatteryBankBlockEntity.BATTERY_SLOT};
        }
        if (entity instanceof RadiationCleanserBlockEntity && inventory != null) {
            return new int[]{
                    RadiationCleanserBlockEntity.INPUT_SLOT,
                    RadiationCleanserBlockEntity.FILTER_SLOT,
                    RadiationCleanserBlockEntity.BATTERY_SLOT};
        }
        return allSlots(inventory);
    }

    private static int[] outputSlots(BlockEntity entity) {
        if (entity instanceof HopperHandler hopperHandler) {
            return uniqueSlots(Direction.values(), hopperHandler::getOutputSlots);
        }
        Container inventory = inventory(entity);
        if (entity instanceof RadiationCleanserBlockEntity && inventory != null) {
            return new int[]{RadiationCleanserBlockEntity.OUTPUT_SLOT};
        }
        return entity instanceof MicroGeneratorBlockEntity || entity instanceof ThermalArrayBlockEntity ? new int[]{} : allSlots(inventory);
    }

    private static int[] uniqueSlots(Direction[] directions, DirectionSlots lookup) {
        Set<Integer> slots = new LinkedHashSet<>();
        for (Direction direction : directions) {
            for (int slot : lookup.slots(direction)) {
                slots.add(slot);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] allSlots(Container inventory) {
        if (inventory == null) {
            return new int[]{};
        }
        int[] slots = new int[inventory.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private static List<Map<String, Object>> inventorySlots(Container inventory) {
        if (inventory == null) {
            return List.of();
        }
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            slots.add(slotSnapshot(slot, inventory.getItem(slot)));
        }
        return List.copyOf(slots);
    }

    private static List<Map<String, Object>> slots(Container inventory, int[] slotIds) {
        if (inventory == null || slotIds.length == 0) {
            return List.of();
        }
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int slot : slotIds) {
            if (slot >= 0 && slot < inventory.getContainerSize()) {
                slots.add(slotSnapshot(slot, inventory.getItem(slot)));
            }
        }
        return List.copyOf(slots);
    }

    private static Map<String, Object> slotSnapshot(int slot, ItemStack stack) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("slot", slot);
        snapshot.putAll(stackSnapshot(stack));
        return Map.copyOf(snapshot);
    }

    private static Map<String, Object> stackSnapshot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Map.of("item", "", "count", 0, "empty", true);
        }
        return Map.of(
                "item", itemId(stack),
                "count", stack.getCount(),
                "maxStackSize", stack.getMaxStackSize(),
                "empty", false);
    }

    private static ItemStack nativeStack(NativeItemStack stack) {
        Identifier id = Identifier.tryParse(stack.itemId());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, stack.count()));
    }

    private static int insertIntoInventory(BlockEntity entity, Container inventory, ItemStack stack, Direction side) {
        int remaining = stack.getCount();
        int[] slots = inputSlots(entity);
        for (int slot : slots) {
            if (remaining <= 0) {
                break;
            }
            if (!canInsert(entity, slot, stack, side)) {
                continue;
            }
            ItemStack existing = inventory.getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int limit = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
            int moved = Math.min(remaining, existing.isEmpty() ? limit : limit - existing.getCount());
            if (moved <= 0) {
                continue;
            }
            if (existing.isEmpty()) {
                ItemStack inserted = stack.copy();
                inserted.setCount(moved);
                setInventorySlot(inventory, slot, inserted);
            } else {
                existing.grow(moved);
                inventory.setChanged();
            }
            remaining -= moved;
        }
        return stack.getCount() - remaining;
    }

    private static ItemStack extractFromInventory(
            BlockEntity entity,
            Container inventory,
            String itemId,
            int count,
            Direction side) {
        ItemStack extracted = ItemStack.EMPTY;
        Predicate<ItemStack> matcher = stack -> itemId.isBlank() || itemId(stack).equals(itemId);
        for (int slot : outputSlots(entity)) {
            if (count <= 0) {
                break;
            }
            if (!canExtract(entity, slot, side)) {
                continue;
            }
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || !matcher.test(existing)) {
                continue;
            }
            int moved = Math.min(count, existing.getCount());
            ItemStack taken = existing.copy();
            taken.setCount(moved);
            existing.shrink(moved);
            if (existing.isEmpty()) {
                setInventorySlot(inventory, slot, ItemStack.EMPTY);
            } else {
                inventory.setChanged();
            }
            if (extracted.isEmpty()) {
                extracted = taken;
            } else if (ItemStack.isSameItemSameComponents(extracted, taken)) {
                extracted.grow(taken.getCount());
            }
            count -= moved;
        }
        return extracted;
    }

    private static boolean canInsert(BlockEntity entity, int slot, ItemStack stack, Direction side) {
        if (entity instanceof HopperHandler hopperHandler) {
            return Arrays.stream(hopperHandler.getInputSlots(side == null ? Direction.UP : side)).anyMatch(candidate -> candidate == slot)
                    && hopperHandler.canInsertItem(slot, stack);
        }
        if (entity instanceof MicroGeneratorBlockEntity) {
            if (slot == 0) {
                return isCoalOrSimpleWoodFuel(stack);
            }
            return slot == MicroGeneratorBlockEntity.BATTERY_SLOT && isBatteryLike(stack);
        }
        if (entity instanceof ThermalArrayBlockEntity) {
            if (slot >= 0 && slot <= 2) {
                return isCoalOrSimpleWoodFuel(stack);
            }
            return slot == ThermalArrayBlockEntity.BATTERY_SLOT && isBatteryLike(stack);
        }
        if (entity instanceof BatteryBankBlockEntity) {
            return slot == BatteryBankBlockEntity.BATTERY_SLOT && isBatteryLike(stack);
        }
        return true;
    }

    private static boolean canExtract(BlockEntity entity, int slot, Direction side) {
        if (entity instanceof HopperHandler hopperHandler) {
            return Arrays.stream(hopperHandler.getOutputSlots(side == null ? Direction.DOWN : side)).anyMatch(candidate -> candidate == slot)
                    && hopperHandler.canExtractItem(slot);
        }
        return true;
    }

    private static void setInventorySlot(Container inventory, int slot, ItemStack stack) {
        if (inventory instanceof MachineInventory machineInventory) {
            machineInventory.setStackInSlot(slot, stack);
        } else {
            inventory.setItem(slot, stack);
        }
        inventory.setChanged();
    }

    private static void applyInventory(Container inventory, List<Map<String, Object>> slots) {
        inventory.clearContent();
        for (Map<String, Object> slotSnapshot : slots) {
            int slot = intValue(slotSnapshot, "slot", -1);
            if (slot < 0 || slot >= inventory.getContainerSize()) {
                continue;
            }
            String itemId = firstNonBlank(stringValue(slotSnapshot, "item"), stringValue(slotSnapshot, "itemId"));
            int count = intValue(slotSnapshot, "count", 0);
            if (itemId.isBlank() || count <= 0) {
                continue;
            }
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item != Items.AIR) {
                setInventorySlot(inventory, slot, new ItemStack(item, count));
            }
        }
        inventory.setChanged();
    }

    private static boolean applyProgress(BlockEntity entity, Map<String, Object> state) {
        boolean changed = false;
        if (entity instanceof WaterPurifierBlockEntity purifier) {
            if (state.containsKey("progress")) {
                purifier.data.set(0, intValue(state, "progress", purifier.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                purifier.data.set(1, intValue(state, "maxProgress", purifier.data.get(1)));
                changed = true;
            }
        } else if (entity instanceof HandRecyclerBlockEntity recycler) {
            if (state.containsKey("progress")) {
                recycler.data.set(0, intValue(state, "progress", recycler.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                recycler.data.set(1, intValue(state, "maxProgress", recycler.data.get(1)));
                changed = true;
            }
        } else if (entity instanceof MicroGeneratorBlockEntity generator) {
            if (state.containsKey("energyStored")) {
                generator.data.set(0, intValue(state, "energyStored", generator.data.get(0)));
                changed = true;
            }
            if (state.containsKey("progress")) {
                generator.data.set(2, intValue(state, "progress", generator.data.get(2)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                generator.data.set(3, intValue(state, "maxProgress", generator.data.get(3)));
                changed = true;
            }
        } else if (entity instanceof ThermalBurnerBlockEntity burner) {
            if (state.containsKey("energyStored")) {
                burner.data.set(2, intValue(state, "energyStored", burner.data.get(2)));
                changed = true;
            }
            if (state.containsKey("progress")) {
                burner.data.set(0, intValue(state, "progress", burner.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                burner.data.set(1, intValue(state, "maxProgress", burner.data.get(1)));
                changed = true;
            }
        } else if (entity instanceof ThermalArrayBlockEntity array) {
            if (state.containsKey("energyStored")) {
                array.data.set(0, intValue(state, "energyStored", array.data.get(0)));
                changed = true;
            }
            if (state.containsKey("progress")) {
                array.data.set(2, intValue(state, "progress", array.data.get(2)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                array.data.set(3, intValue(state, "maxProgress", array.data.get(3)));
                changed = true;
            }
        } else if (entity instanceof ScrapPressBlockEntity press) {
            if (state.containsKey("progress")) {
                press.data.set(0, intValue(state, "progress", press.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                press.data.set(1, intValue(state, "maxProgress", press.data.get(1)));
                changed = true;
            }
            if (state.containsKey("active")) {
                press.data.set(3, boolValue(state, "active", press.data.get(3) != 0) ? 1 : 0);
                changed = true;
            }
        } else if (entity instanceof OreGrinderBlockEntity grinder) {
            if (state.containsKey("progress")) {
                grinder.data.set(0, intValue(state, "progress", grinder.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                grinder.data.set(1, intValue(state, "maxProgress", grinder.data.get(1)));
                changed = true;
            }
        } else if (entity instanceof FilterWorkbenchBlockEntity workbench) {
            if (state.containsKey("progress")) {
                workbench.data.set(0, intValue(state, "progress", workbench.data.get(0)));
                changed = true;
            }
        } else if (entity instanceof IsotopeRefinerBlockEntity refiner) {
            if (state.containsKey("progress")) {
                refiner.data.set(0, intValue(state, "progress", refiner.data.get(0)));
                changed = true;
            }
            if (state.containsKey("maxProgress")) {
                refiner.data.set(1, intValue(state, "maxProgress", refiner.data.get(1)));
                changed = true;
            }
        } else if (entity instanceof CrystallineSynthesizerBlockEntity synthesizer) {
            if (state.containsKey("progress")) {
                synthesizer.data.set(0, intValue(state, "progress", synthesizer.data.get(0)));
                changed = true;
            }
            if (state.containsKey("phase")) {
                synthesizer.data.set(2, intValue(state, "phase", synthesizer.data.get(2)));
                changed = true;
            }
        } else if (entity instanceof DeepCoreMinerBlockEntity miner) {
            if (state.containsKey("progress")) {
                miner.data.set(0, intValue(state, "progress", miner.data.get(0)));
                changed = true;
            }
        } else if (entity instanceof RadiationCleanserBlockEntity cleanser) {
            if (state.containsKey("progress")) {
                cleanser.data.set(0, intValue(state, "progress", cleanser.data.get(0)));
                changed = true;
            }
            if (state.containsKey("energyStored")) {
                cleanser.data.set(3, intValue(state, "energyStored", cleanser.data.get(3)));
                changed = true;
            }
        }
        return changed;
    }

    private static ProgressState progress(BlockEntity entity, BlockState state) {
        if (entity instanceof WaterPurifierBlockEntity purifier) {
            return new ProgressState(purifier.data.get(0), purifier.data.get(1), state.hasProperty(WaterPurifierBlock.ACTIVE)
                    && state.getValue(WaterPurifierBlock.ACTIVE));
        }
        if (entity instanceof HandRecyclerBlockEntity recycler) {
            return new ProgressState(recycler.data.get(0), recycler.data.get(1), state.hasProperty(HandRecyclerBlock.ACTIVE)
                    && state.getValue(HandRecyclerBlock.ACTIVE));
        }
        if (entity instanceof MicroGeneratorBlockEntity generator) {
            return new ProgressState(generator.data.get(2), generator.data.get(3), state.hasProperty(MicroGeneratorBlock.ACTIVE)
                    && state.getValue(MicroGeneratorBlock.ACTIVE));
        }
        if (entity instanceof ThermalBurnerBlockEntity burner) {
            return new ProgressState(burner.data.get(0), burner.data.get(1), hasActiveProperty(state) && activeValue(state));
        }
        if (entity instanceof ThermalArrayBlockEntity array) {
            return new ProgressState(array.data.get(2), array.data.get(3), hasActiveProperty(state) && activeValue(state));
        }
        if (entity instanceof ScrapDynamoBlockEntity dynamo) {
            return new ProgressState(dynamo.getBurnTimeRemaining(), dynamo.getMaxBurnTime(), hasActiveProperty(state) && activeValue(state));
        }
        if (entity instanceof PowerNodeBlockEntity node) {
            return new ProgressState(node.getEnergyStored(), node.getMaxEnergyStored(),
                    node.isActivated() || (hasActiveProperty(state) && activeValue(state)));
        }
        if (entity instanceof ScrapPressBlockEntity press) {
            return new ProgressState(press.data.get(0), press.data.get(1), press.data.get(3) != 0);
        }
        if (entity instanceof OreGrinderBlockEntity grinder) {
            return new ProgressState(grinder.data.get(0), grinder.data.get(1), hasActiveProperty(state) && activeValue(state));
        }
        if (entity instanceof FilterWorkbenchBlockEntity workbench) {
            return new ProgressState(workbench.data.get(0), workbench.data.get(1), workbench.data.get(0) > 0);
        }
        if (entity instanceof IsotopeRefinerBlockEntity refiner) {
            return new ProgressState(refiner.data.get(0), refiner.data.get(1), hasActiveProperty(state) && activeValue(state));
        }
        if (entity instanceof CrystallineSynthesizerBlockEntity synthesizer) {
            return new ProgressState(synthesizer.data.get(0), synthesizer.data.get(1), synthesizer.data.get(2) > 0);
        }
        if (entity instanceof DeepCoreMinerBlockEntity miner) {
            return new ProgressState(miner.data.get(0), miner.data.get(1), miner.data.get(0) > 0 && miner.data.get(3) == 0);
        }
        if (entity instanceof RadiationCleanserBlockEntity cleanser) {
            return new ProgressState(cleanser.data.get(0), cleanser.data.get(1), cleanser.data.get(0) > 0);
        }
        if (entity instanceof IEnergyStorage energy) {
            return new ProgressState(energy.getEnergyStored(), energy.getMaxEnergyStored(), hasActiveProperty(state) && activeValue(state));
        }
        return new ProgressState(0, 0, hasActiveProperty(state) && activeValue(state));
    }

    private static boolean setActive(ServerLevel level, BlockPos pos, BlockState state, boolean active) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && property.getName().equals("active")) {
                boolean before = state.getValue(booleanProperty);
                if (before != active) {
                    level.setBlockAndUpdate(pos, state.setValue(booleanProperty, active));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasActiveProperty(BlockState state) {
        return state.getProperties().stream().anyMatch(property -> property instanceof BooleanProperty
                && property.getName().equals("active"));
    }

    private static boolean activeValue(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && property.getName().equals("active")) {
                return state.getValue(booleanProperty);
            }
        }
        return false;
    }

    private static String lastRecipeId(BlockEntity entity, Container inventory) {
        if (inventory == null) {
            return "";
        }
        if (entity instanceof WaterPurifierBlockEntity && inventory.getContainerSize() > 2) {
            ItemStack output = inventory.getItem(2);
            return output.isEmpty() ? "" : EchoAshfallProtocol.MODID + ":water_purifier/" + itemPath(itemId(output));
        }
        if (entity instanceof HandRecyclerBlockEntity && inventory.getContainerSize() > 1) {
            ItemStack output = inventory.getItem(1);
            return output.isEmpty() ? "" : EchoAshfallProtocol.MODID + ":hand_recycler/" + itemPath(itemId(output));
        }
        if (entity instanceof MicroGeneratorBlockEntity generator && generator.data.get(3) > 0) {
            return EchoAshfallProtocol.MODID + ":micro_generator/fuel_burn";
        }
        if (entity instanceof ThermalBurnerBlockEntity burner && burner.data.get(1) > 0) {
            return EchoAshfallProtocol.MODID + ":thermal_burner/fuel_burn";
        }
        if (entity instanceof ThermalArrayBlockEntity array && array.data.get(3) > 0) {
            return EchoAshfallProtocol.MODID + ":thermal_array/fuel_burn";
        }
        if (entity instanceof ScrapDynamoBlockEntity dynamo && dynamo.getMaxBurnTime() > 0) {
            return EchoAshfallProtocol.MODID + ":scrap_dynamo/fuel_burn";
        }
        if (entity instanceof RadiationCleanserBlockEntity && inventory.getContainerSize() > RadiationCleanserBlockEntity.OUTPUT_SLOT) {
            ItemStack output = inventory.getItem(RadiationCleanserBlockEntity.OUTPUT_SLOT);
            return output.isEmpty() ? "" : EchoAshfallProtocol.MODID + ":radiation_cleanser/" + itemPath(itemId(output));
        }
        return "";
    }

    private static boolean isCoalOrSimpleWoodFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL)
                || stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS)
                || stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS)
                || stack.is(Items.STICK);
    }

    private static boolean isBatteryLike(ItemStack stack) {
        String path = itemPath(itemId(stack));
        return path.contains("battery") || path.contains("cell");
    }

    private static boolean isTerminal(BlockEntity entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName().toLowerCase(Locale.ROOT);
        String machineId = machineId(entity);
        return className.contains("terminal") || machineId.contains(":terminal") || machineId.contains(":workstation");
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Direction side(String side) {
        if (side == null || side.isBlank()) {
            return null;
        }
        return Direction.byName(side.toLowerCase(Locale.ROOT));
    }

    private static String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String itemPath(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        int split = itemId.indexOf(':');
        return split >= 0 ? itemId.substring(split + 1) : itemId;
    }

    private static Map<String, Object> summary(NativeBlockEntitySnapshot snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("blockEntityId", snapshot.blockEntityId());
        summary.put("block", blockSnapshot(snapshot.block()));
        summary.put("machineId", snapshot.state().getOrDefault("machineId", ""));
        summary.put("canonicalMachineId", snapshot.state().getOrDefault("canonicalMachineId", ""));
        summary.put("energyStored", snapshot.state().getOrDefault("energyStored", 0));
        summary.put("energyCapacity", snapshot.state().getOrDefault("energyCapacity", 0));
        summary.put("progress", snapshot.state().getOrDefault("progress", 0));
        summary.put("maxProgress", snapshot.state().getOrDefault("maxProgress", 0));
        summary.put("active", snapshot.state().getOrDefault("active", false));
        summary.put("inputSlots", snapshot.state().getOrDefault("inputSlots", List.of()));
        summary.put("outputSlots", snapshot.state().getOrDefault("outputSlots", List.of()));
        return Map.copyOf(summary);
    }

    private static boolean eventPublished(NativeResult result) {
        return Boolean.TRUE.equals(result.snapshot().get("eventPublished"));
    }

    private static boolean markBlockEntityChanged(Map<String, Object> payload) {
        Map<String, Object> block = mapValue(payload, "block");
        if (block.isEmpty() && payload.containsKey("position")) {
            block = mapValue(payload, "position");
        }
        if (block.isEmpty()) {
            return false;
        }
        String dimension = firstNonBlank(stringValue(block, "dimensionId"), stringValue(payload, "dimensionId"));
        int x = intValue(block, "x", 0);
        int y = intValue(block, "y", 0);
        int z = intValue(block, "z", 0);
        if (dimension.isBlank()) {
            return false;
        }
        ResolvedBlock resolved = resolve(new NativeBlockRef(dimension, x, y, z));
        if (resolved.entity() != null) {
            resolved.entity().setChanged();
            return true;
        }
        return false;
    }

    private static void writePlayerMachineSave(ServerPlayer player, String key, String value) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(SAVE_ROOT).copy();
        root.putString(key, value == null ? "" : value);
        player.getPersistentData().put(SAVE_ROOT, root);
    }

    private static CompoundTag compoundFromMap(Map<String, Object> values) {
        CompoundTag tag = new CompoundTag();
        if (values == null) {
            return tag;
        }
        values.forEach((key, value) -> writeTagValue(tag, safeTagKey(key), value));
        return tag;
    }

    @SuppressWarnings("unchecked")
    private static void writeTagValue(CompoundTag tag, String key, Object value) {
        if (value instanceof Boolean bool) {
            tag.putBoolean(key, bool);
        } else if (value instanceof Number number) {
            if (value instanceof Float || value instanceof Double) {
                tag.putDouble(key, number.doubleValue());
            } else {
                tag.putLong(key, number.longValue());
            }
        } else if (value instanceof Map<?, ?> map) {
            CompoundTag child = new CompoundTag();
            ((Map<String, Object>) map).forEach((childKey, childValue) -> writeTagValue(child, safeTagKey(childKey), childValue));
            tag.put(key, child);
        } else if (value instanceof Iterable<?> iterable) {
            int index = 0;
            CompoundTag child = new CompoundTag();
            for (Object childValue : iterable) {
                writeTagValue(child, "value_" + index, childValue);
                index++;
            }
            child.putInt("count", index);
            tag.put(key, child);
        } else {
            tag.putString(key, value == null ? "" : String.valueOf(value));
        }
    }

    private static String safeTagKey(String key) {
        return key == null || key.isBlank() ? "value" : key.replace('.', '_').replace(':', '_');
    }

    private static String compactActionKey(String value) {
        if (value == null || value.isBlank()) {
            return "item";
        }
        return value.replaceAll("[^A-Za-z0-9_.:-]", "_");
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

    private static Map<String, Object> positionSnapshot(Level level, BlockPos pos) {
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Map<?, ?> typed) {
            Map<String, Object> copy = new LinkedHashMap<>();
            typed.forEach((childKey, childValue) -> copy.put(String.valueOf(childKey), childValue));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> typed) {
                Map<String, Object> copy = new LinkedHashMap<>();
                typed.forEach((childKey, childValue) -> copy.put(String.valueOf(childKey), childValue));
                entries.add(Map.copyOf(copy));
            }
        }
        return List.copyOf(entries);
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
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean boolValue(Map<String, Object> map, String key, boolean fallback) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second == null ? "" : second : first;
    }

    private static String machineSaveKey(NativeBlockRef block) {
        if (block == null) {
            return "missing_block";
        }
        return block.dimensionId().replace(':', '_') + "_" + block.x() + "_" + block.y() + "_" + block.z();
    }

    private record ResolvedBlock(ServerLevel level, BlockPos pos, BlockState state, BlockEntity entity, boolean loaded) {
    }

    private record ProgressState(int progress, int maxProgress, boolean active) {
    }

    @FunctionalInterface
    private interface DirectionSlots {
        int[] slots(Direction direction);
    }
}

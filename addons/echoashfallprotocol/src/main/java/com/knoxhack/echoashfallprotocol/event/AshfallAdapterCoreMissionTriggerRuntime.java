package com.knoxhack.echoashfallprotocol.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.AshfallBetaRouteContract;
import com.knoxhack.echoashfallprotocol.echo.AshfallBetaRouteContract.EchoObjective;
import com.knoxhack.echoashfallprotocol.echo.AshfallBetaRouteContract.EchoObjectiveTrigger;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Native AdapterCore backend for the Agent 6 beta-route trigger contract.
 */
public final class AshfallAdapterCoreMissionTriggerRuntime {
    private AshfallAdapterCoreMissionTriggerRuntime() {
    }

    public static void onPlayerLoggedIn(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            playerSpawned(player);
        }
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime % 100L == 0L) {
            worldTick(player);
        }
    }

    public static void onItemObtained(Object event) {
        if (eventValue(event, "getPlayer") instanceof ServerPlayer player) {
            ItemStack stack = itemStackValue(event, "getOriginalStack");
            if (!stack.isEmpty()) {
                AshfallAdapterCoreEarlyEventRuntime.itemObtained(player, stack, "pickup");
            }
        }
    }

    public static void onItemUsed(Object event) {
        ItemStack stack = itemStackValue(event, "getItem");
        if (eventValue(event, "getEntity") instanceof ServerPlayer player && !stack.isEmpty()) {
            AshfallAdapterCoreEarlyEventRuntime.itemConsumed(player, stack);
        }
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerAshfallMissionCommands(commandDispatcher);
        }
    }

    @SuppressWarnings("unchecked")
    public static void registerAshfallMissionCommands(CommandDispatcher<?> dispatcher) {
        CommandDispatcher<CommandSourceStack> commandDispatcher =
                (CommandDispatcher<CommandSourceStack>) dispatcher;
        commandDispatcher.register(ashfallMissionCommand("ashfall"));
        commandDispatcher.register(ashfallMissionCommand("echoashfall"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ashfallMissionCommand(String root) {
        return Commands.literal(root)
                .then(Commands.literal("mission")
                        .executes(context -> executeMissionCommand(context.getSource().getPlayerOrException(), "mission status"))
                        .then(Commands.literal("status")
                                .executes(context -> executeMissionCommand(context.getSource().getPlayerOrException(), "mission status")))
                        .then(Commands.literal("open")
                                .executes(context -> executeMissionCommand(context.getSource().getPlayerOrException(), "mission"))));
    }

    private static int executeMissionCommand(ServerPlayer player, String commandLine) {
        terminalMissionCommand(player, commandLine);
        player.sendSystemMessage(Component.literal("[ECHO-7] Mission command routed through AdapterCore."));
        return 1;
    }

    public static boolean playerSpawned(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        AshfallAdapterCoreFirstSpawnRuntime.FirstSpawnRuntimeResult result =
                AshfallAdapterCoreFirstSpawnRuntime.startMissionCoreFirstMission(player);
        boolean recorded = record(player, EchoObjectiveTrigger.PLAYER_SPAWNED, "echoashfallprotocol:drop_pod");
        return result.realNativeStateMutated() || recorded;
    }

    public static boolean worldTick(ServerPlayer player) {
        return record(player, EchoObjectiveTrigger.REGION_ENTERED, "echoashfallprotocol:crash_zone_wasteland");
    }

    public static boolean itemUsed(ServerPlayer player, String itemId) {
        return record(player, EchoObjectiveTrigger.ITEM_USED, itemId);
    }

    public static boolean itemCollected(ServerPlayer player, String itemId) {
        return record(player, EchoObjectiveTrigger.ITEM_COLLECTED, itemId);
    }

    public static boolean recipeCrafted(ServerPlayer player, String itemId) {
        return record(player, EchoObjectiveTrigger.RECIPE_CRAFTED, itemId);
    }

    public static boolean blockPlaced(ServerPlayer player, String blockId) {
        return record(player, EchoObjectiveTrigger.BLOCK_PLACED, blockId);
    }

    public static boolean blockBroken(ServerPlayer player, String blockId) {
        return record(player, EchoObjectiveTrigger.BLOCK_BROKEN, blockId);
    }

    public static boolean terminalOpened(ServerPlayer player, String pageId) {
        NativeResult routePage = AshfallAdapterCoreMajorRouteRuntime.terminalPageOpened(player, pageId);
        NativeResult routeCache = AshfallAdapterCoreMajorRouteRuntime.relayCacheClaimed(player, pageId);
        return routePage.mutated()
                || routeCache.mutated()
                || record(player, EchoObjectiveTrigger.TERMINAL_OPENED, pageId);
    }

    public static boolean terminalMissionCommand(ServerPlayer player, String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return false;
        }
        String command = commandLine.strip().toLowerCase(java.util.Locale.ROOT);
        if (!command.equals("mission") && !command.equals("mission status") && !command.equals("ashfall mission")) {
            return false;
        }
        boolean advanced = terminalOpened(player, "echoterminal:ashfall_first_steps");
        publishHudObjective(player, "terminal_command");
        return advanced;
    }

    public static boolean lensScanned(ServerPlayer player, String scanId) {
        NativeResult routeScan = AshfallAdapterCoreMajorRouteRuntime.relayConsoleScanned(player, scanId);
        return routeScan.mutated() || record(player, EchoObjectiveTrigger.LENS_SCANNED, scanId);
    }

    public static boolean scannerUsed(ServerPlayer player, String scannerId) {
        NativeResult routeScan = AshfallAdapterCoreMajorRouteRuntime.relayConsoleScanned(player, scannerId);
        return routeScan.mutated() || record(player, EchoObjectiveTrigger.SCANNER_USED, scannerId);
    }

    public static boolean regionEntered(ServerPlayer player, String regionId) {
        return record(player, EchoObjectiveTrigger.REGION_ENTERED, regionId);
    }

    public static boolean hazardSurvived(ServerPlayer player, String hazardId) {
        return record(player, EchoObjectiveTrigger.HAZARD_SURVIVED, hazardId);
    }

    public static boolean entityDefeated(ServerPlayer player, String entityId) {
        return record(player, EchoObjectiveTrigger.ENTITY_DEFEATED, entityId);
    }

    public static boolean machinePowered(ServerPlayer player, String machineId) {
        NativeResult routeRepair = AshfallAdapterCoreMajorRouteRuntime.relayPowerCouplerRepaired(player, machineId);
        return routeRepair.mutated() || record(player, EchoObjectiveTrigger.MACHINE_POWERED, machineId);
    }

    public static boolean machineOutputCreated(ServerPlayer player, String itemId) {
        return record(player, EchoObjectiveTrigger.MACHINE_OUTPUT_CREATED, itemId);
    }

    public static boolean missionCompleted(ServerPlayer player, String missionId) {
        return record(player, EchoObjectiveTrigger.MISSION_COMPLETED, missionId);
    }

    public static boolean missionObjectiveCompleted(ServerPlayer player, String missionId) {
        return record(player, EchoObjectiveTrigger.MISSION_OBJECTIVE_COMPLETED, missionId);
    }

    public static boolean saveRestored(ServerPlayer player, String contractId) {
        return record(player, EchoObjectiveTrigger.SAVE_RESTORED, contractId);
    }

    public static boolean relayWeatherWindowChecked(ServerPlayer player, String weatherWindowId) {
        return AshfallAdapterCoreMajorRouteRuntime.relayWeatherWindowChecked(player, weatherWindowId).mutated();
    }

    public static boolean holomapMarkerSelected(ServerPlayer player, String markerId) {
        return AshfallAdapterCoreMajorRouteRuntime.holomapMarkerSelected(player, markerId).mutated();
    }

    public static boolean relayCacheClaimed(ServerPlayer player, String cacheId) {
        return AshfallAdapterCoreMajorRouteRuntime.relayCacheClaimed(player, cacheId).mutated();
    }

    public static boolean terminalRouteRecordUpdated(ServerPlayer player, String routeRecordTarget) {
        return AshfallAdapterCoreMajorRouteRuntime.terminalRouteRecordUpdated(player, routeRecordTarget).mutated();
    }

    public static boolean record(ServerPlayer player, EchoObjectiveTrigger trigger, String target) {
        if (player == null || trigger == null || target == null || target.isBlank()) {
            return false;
        }
        Optional<EchoObjective> objective = AshfallBetaRouteContract.betaObjectives().stream()
                .filter(candidate -> candidate.trigger() == trigger)
                .filter(candidate -> candidate.target().equals(target))
                .findFirst();
        if (objective.isEmpty() || !EchoCoreServices.missionCoreAvailable()) {
            return false;
        }
        Identifier objectiveTarget = targetId(target);
        MissionObjectiveType type = objectiveType(trigger);
        boolean advanced = EchoCoreServices.recordMissionObjective(
                player,
                type,
                objectiveTarget,
                objective.get().requiredProgress(),
                Map.of(
                        "source", EchoAshfallProtocol.MODID,
                        "adapterCoreContract", AshfallBetaRouteContract.CONTRACT_ID,
                        "adapterCoreTrigger", trigger.id(),
                        "adapterCoreObjective", objective.get().id()));
        if (trigger == EchoObjectiveTrigger.MISSION_COMPLETED) {
            EchoCoreServices.completeMission(player, targetId(AshfallBetaRouteContract.FIRST_MISSION_ID));
            EchoCoreServices.startMission(player, targetId(AshfallBetaRouteContract.NEXT_MISSION_ID));
        }
        writeMissionSnapshot(player, objective.get(), trigger, target, advanced);
        publishHudObjective(player, trigger.id());
        return advanced;
    }

    private static void writeMissionSnapshot(
            ServerPlayer player,
            EchoObjective objective,
            EchoObjectiveTrigger trigger,
            String target,
            boolean advanced) {
        CompoundTag data = player.getPersistentData();
        data.putString("echoashfallprotocol.agent6.contract", AshfallBetaRouteContract.CONTRACT_ID);
        data.putString("echoashfallprotocol.agent6.mission", AshfallBetaRouteContract.FIRST_MISSION_ID);
        data.putString("echoashfallprotocol.agent6.last_objective", objective.id());
        data.putString("echoashfallprotocol.agent6.last_trigger", trigger.id());
        data.putString("echoashfallprotocol.agent6.last_target", target);
        data.putBoolean("echoashfallprotocol.agent6.last_advanced", advanced);
        data.putBoolean("echoashfallprotocol.agent6.save_persists_mission_state", true);
        data.putBoolean("echoashfallprotocol.agent6.hud_objective_visible", true);
    }

    private static void publishHudObjective(ServerPlayer player, String source) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(
                "[ECHO-7] Objective: secure the crash outpost. Open Terminal, scan the area, salvage, survive, recover."));
        player.getPersistentData().putString("echoashfallprotocol.agent6.hud_source", source);
    }

    private static MissionObjectiveType objectiveType(EchoObjectiveTrigger trigger) {
        return switch (trigger) {
            case ITEM_COLLECTED -> MissionObjectiveType.OBTAIN_ITEM;
            case RECIPE_CRAFTED -> MissionObjectiveType.CRAFT_ITEM;
            case BLOCK_PLACED -> MissionObjectiveType.PLACE_BLOCK;
            case MACHINE_POWERED -> MissionObjectiveType.REPAIR_MACHINE;
            case MACHINE_OUTPUT_CREATED -> MissionObjectiveType.OBTAIN_ITEM;
            case BLOCK_BROKEN, ITEM_USED, TERMINAL_OPENED, HAZARD_SURVIVED,
                 MISSION_OBJECTIVE_COMPLETED, MISSION_COMPLETED, SAVE_RESTORED ->
                    MissionObjectiveType.CUSTOM;
            case LENS_SCANNED, SCANNER_USED -> MissionObjectiveType.SCAN_BLOCK;
            case REGION_ENTERED -> MissionObjectiveType.ENTER_REGION;
            case ENTITY_DEFEATED -> MissionObjectiveType.KILL_ENTITY;
            case PLAYER_SPAWNED -> MissionObjectiveType.CUSTOM;
            default -> MissionObjectiveType.CUSTOM;
        };
    }

    private static Identifier targetId(String target) {
        Identifier parsed = Identifier.tryParse(target);
        if (parsed != null) {
            return parsed;
        }
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, target.replace(':', '/').replace(' ', '_'));
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}

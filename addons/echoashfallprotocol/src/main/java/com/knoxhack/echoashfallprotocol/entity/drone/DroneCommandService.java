package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneCommand;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.network.DroneCommandPacket;
import com.knoxhack.echoashfallprotocol.registry.ModSounds;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DroneCommandService {
    private DroneCommandService() {
    }

    public static void handle(DroneCommandPacket packet, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        execute(serverPlayer, packet == null ? "" : packet.command());
    }

    public static int execute(ServerPlayer player, String rawCommand) {
        String raw = rawCommand == null ? "" : rawCommand.trim();
        EchoDroneCommand command = EchoDroneCommand.parse(raw);
        if (command == EchoDroneCommand.UNKNOWN) {
            EchoDroneMode legacyMode = modeFromRaw(raw);
            if (legacyMode != null) {
                return setMode(player, legacyMode, raw);
            }
            return rejectOrFallback(player, raw, "Unknown command.");
        }
        return switch (command) {
            case RECALL -> recall(player);
            case STATUS -> status(player);
            case SCAN_AREA -> scanArea(player);
            case SCOUT_AHEAD -> scoutAhead(player);
            case COLLECT_SCRAP -> setMode(player, EchoDroneMode.SALVAGE, raw);
            case GUARD_HERE -> setMode(player, EchoDroneMode.GUARD, raw);
            case TOGGLE_ASSIST -> toggleAssist(player);
            case TOGGLE_LIGHT -> toggleLight(player);
            case SET_MODE -> setMode(player, modeFromRaw(raw), raw);
            case UNKNOWN -> rejectOrFallback(player, raw, "Unknown command.");
        };
    }

    public static int setBattery(ServerPlayer player, int value) {
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        data.setBatteryPercent(value);
        CompanionDroneStateStore.save(player, data);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "battery", data.getMode().name(), true, Map.of("battery", data.getBatteryPercent()));
        send(player, "Battery set to " + data.getBatteryPercent() + "%.", ChatFormatting.GREEN, false);
        return 1;
    }

    public static int addUpgrade(ServerPlayer player, EchoDroneUpgrade upgrade) {
        if (upgrade == null) {
            send(player, "Unknown upgrade.", ChatFormatting.RED, false);
            return 0;
        }
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        data.addUpgrade(upgrade);
        CompanionDroneStateStore.save(player, data);
        EchoCompanionDrone drone = CompanionDroneStateStore.nearestOwned(player);
        if (drone != null) {
            drone.applyFieldAssistantState(data);
        }
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "upgrade", data.getMode().name(), true, Map.of("upgrade", upgrade.name()));
        send(player, "Upgrade installed: " + upgrade.displayName() + ".", ChatFormatting.GREEN, false);
        return 1;
    }

    public static int listUpgrades(ServerPlayer player) {
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        send(player, "Installed upgrades: " + data.upgradesDisplay() + ".", ChatFormatting.AQUA, false);
        return data.getUpgrades().size();
    }

    public static int reset(ServerPlayer player) {
        EchoCompanionDrone drone = CompanionDroneStateStore.nearestOwned(player);
        if (drone != null) {
            drone.discard();
        }
        CompanionDroneData data = new CompanionDroneData();
        data.setOwnerUuid(player.getUUID());
        CompanionDroneStateStore.save(player, data);
        AshfallAdapterCoreExplorationRuntime.droneState(player, "reset", data.getMode().name(), true, Map.of());
        send(player, "Drone state reset. Use recall to reconstruct the local link.", ChatFormatting.YELLOW, false);
        return 1;
    }

    public static int debugMarkers(ServerPlayer player) {
        int markers = DroneScanService.recentMarkers(player).size();
        send(player, "Recent scan markers: " + markers + ".", ChatFormatting.AQUA, false);
        return markers;
    }

    private static int recall(ServerPlayer player) {
        if (!Config.ENABLE_COMPANION_DRONE_UTILITY.get()) {
            return rejectOrFallback(player, "recall", "Companion Drone utility is disabled by config.");
        }
        send(player, "Companion Drone recalling...", ChatFormatting.AQUA, true);
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, true, true);
        if (drone == null) {
            return fallbackScout(player, "RECALL");
        }
        if (!CompanionDroneStateStore.sameDimension(player, drone)) {
            drone.discard();
            CompanionDroneData data = CompanionDroneStateStore.get(player);
            data.setDroneUuid(null);
            drone = CompanionDroneStateStore.spawnRecoveredDrone(player, data);
            if (drone == null) {
                return rejectOrFallback(player, "recall", "Recall failed. Owner link unavailable.");
            }
        }
        boolean returned = drone.recallTo(player);
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        data.setMode(EchoDroneMode.FOLLOW);
        data.setTaskLabel(EchoDroneMode.FOLLOW.taskLabel());
        data.setReturningToOwner(false);
        data.setPathingStuck(false);
        data.clearTarget();
        CompanionDroneStateStore.link(player, drone, data);
        CompanionDroneStateStore.save(player, data);
        play(player, returned ? ModSounds.ECHO_COMPLETE.get() : ModSounds.ECHO_MESSAGE.get(), returned ? 1.15F : 0.7F);
        send(player, returned ? "Companion Drone returned." : "Drone link restored.", returned ? ChatFormatting.GREEN : ChatFormatting.YELLOW, true);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "recall", data.getMode().name(), true, Map.of("returned", returned));
        return returned ? 1 : 0;
    }

    private static int status(ServerPlayer player) {
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        if (drone != null) {
            CompanionDroneStateStore.link(player, drone, data);
            CompanionDroneStateStore.save(player, data);
        }
        send(player, data.statusLine(), data.isDeployed() ? ChatFormatting.AQUA : ChatFormatting.GRAY, true);
        if (!data.getLastWarning().isBlank()) {
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Last warning: " + data.getLastWarning())
                    .withStyle(ChatFormatting.YELLOW));
        }
        player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Last scan: " + data.getLastScanSummary())
                .withStyle(ChatFormatting.GRAY));
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "status", data.getMode().name(), true, Map.of("deployed", data.isDeployed()));
        return data.isDeployed() ? 1 : 0;
    }

    private static int scanArea(ServerPlayer player) {
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        if (drone == null) {
            return fallbackScout(player, "SCOUT");
        }
        if (!canCommand(player, drone, 72.0D)) {
            return reject(player, "Drone is out of command range. Use recall.");
        }
        return DroneScanService.scanArea(player, drone, player.blockPosition(), false).size();
    }

    private static int scoutAhead(ServerPlayer player) {
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        if (drone == null) {
            return fallbackScout(player, "SCOUT");
        }
        if (!canCommand(player, drone, 96.0D)) {
            return reject(player, "Scout failed: signal blocked.");
        }
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        if (Config.ENABLE_DRONE_SIGNAL.get() && data.getSignalQuality() < 20) {
            return reject(player, "Scout failed: signal blocked.");
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            long now = serverLevel.getGameTime();
            int cooldown = Math.max(20, Config.DRONE_SCOUT_COOLDOWN_TICKS.get());
            if (data.getLastScoutTime() != Long.MIN_VALUE && now - data.getLastScoutTime() < cooldown) {
                long remaining = Math.max(1L, (cooldown - (now - data.getLastScoutTime()) + 19L) / 20L);
                return reject(player, "Scout array cooling down: " + remaining + "s.");
            }
        }

        int max = Math.max(4, Config.DRONE_SCOUT_MAX_DISTANCE.get());
        Vec3 look = player.getLookAngle();
        BlockPos target = BlockPos.containing(player.position().add(look.scale(max)).add(0.0D, 1.0D, 0.0D));
        if (!(player.level() instanceof ServerLevel level) || !level.isLoaded(target)) {
            target = player.blockPosition().relative(player.getDirection(), Math.min(8, max)).above();
        }
        data.setMode(EchoDroneMode.SCOUT);
        data.setTaskLabel("Scouting ahead");
        data.setTarget(player.level().dimension(), target);
        CompanionDroneStateStore.save(player, data);
        drone.applyFieldAssistantState(data);
        drone.beginScoutAhead(target);
        drone.speak("Drone scouting ahead...", EchoCompanionDrone.MOOD_PROFESSIONAL, 35, 4);
        send(player, "Drone scouting ahead...", ChatFormatting.AQUA, true);
        play(player, ModSounds.ECHO_MESSAGE.get(), 1.25F);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "scout_ahead", data.getMode().name(), true, Map.of(
                        "targetX", target.getX(),
                        "targetY", target.getY(),
                        "targetZ", target.getZ()));
        return 1;
    }

    private static int toggleAssist(ServerPlayer player) {
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        EchoDroneMode next = data.getMode() == EchoDroneMode.ASSIST ? EchoDroneMode.FOLLOW : EchoDroneMode.ASSIST;
        return setMode(player, next, next.name());
    }

    private static int toggleLight(ServerPlayer player) {
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        if (drone == null) {
            return fallbackScout(player, "TOGGLE_LIGHT");
        }
        if (!canCommand(player, drone, 96.0D)) {
            return reject(player, "Drone is out of command range. Use recall.");
        }
        drone.toggleLight();
        drone.speak(drone.isLightEnabled() ? "Light enabled." : "Light disabled.",
                EchoCompanionDrone.MOOD_PROFESSIONAL, 30, 0);
        play(player, ModSounds.ECHO_MESSAGE.get(), 1.35F);
        send(player, "Light " + (drone.isLightEnabled() ? "enabled." : "disabled.") + ".", ChatFormatting.AQUA, true);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "toggle_light", CompanionDroneStateStore.get(player).getMode().name(), true,
                Map.of("lightEnabled", drone.isLightEnabled()));
        return 1;
    }

    private static int setMode(ServerPlayer player, EchoDroneMode mode, String raw) {
        if (mode == null) {
            return rejectOrFallback(player, raw, "Unknown command.");
        }
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        if (drone == null) {
            return fallbackScout(player, raw);
        }
        if (!canCommand(player, drone, 96.0D)) {
            return reject(player, "Drone is out of command range. Use recall.");
        }
        EchoCompanionDrone.DroneMode legacyMode = legacyModeFor(mode, raw);
        if (legacyMode != null && !drone.canSwitchToMode(legacyMode)) {
            drone.speak(legacyMode.getDisplayName() + " locked. Repair required.",
                    EchoCompanionDrone.MOOD_CONCERNED, 45, 6);
            send(player, legacyMode.getDisplayName() + " requires higher repair integrity.", ChatFormatting.YELLOW, true);
            play(player, ModSounds.ECHO_MESSAGE.get(), 0.8F);
            return 0;
        }

        CompanionDroneData data = CompanionDroneStateStore.get(player);
        data.setMode(mode);
        data.setTaskLabel(mode.taskLabel());
        data.setReturningToOwner(false);
        if (mode != EchoDroneMode.SCOUT) {
            data.clearTarget();
        }
        CompanionDroneStateStore.link(player, drone, data);
        if (legacyMode != null) {
            drone.setCurrentMode(legacyMode);
        } else {
            drone.applyFieldAssistantState(data);
        }
        CompanionDroneStateStore.save(player, data);
        drone.speak("Mode set: " + mode.displayName() + ".", EchoCompanionDrone.MOOD_PROFESSIONAL, 35, 0);
        play(player, ModSounds.ECHO_COMPLETE.get(), 1.05F);
        send(player, "Mode: " + mode.displayName(), ChatFormatting.GREEN, true);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "set_mode", mode.name(), true, Map.of("rawCommand", raw == null ? "" : raw));
        return 1;
    }

    private static boolean canCommand(ServerPlayer player, EchoCompanionDrone drone, double range) {
        return drone != null
                && player.getUUID().equals(drone.getOwnerUUID())
                && CompanionDroneStateStore.sameDimension(player, drone)
                && drone.distanceToSqr(player) <= range * range;
    }

    private static EchoDroneMode modeFromRaw(String raw) {
        String key = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "FOLLOW" -> EchoDroneMode.FOLLOW;
            case "ASSIST" -> EchoDroneMode.ASSIST;
            case "SCOUT", "SCOUT_MODE" -> EchoDroneMode.SCOUT;
            case "SALVAGE", "SALVAGE_MODE", "SCAVENGE", "COLLECT_SCRAP" -> EchoDroneMode.SALVAGE;
            case "GUARD", "GUARD_MODE", "PATROL" -> EchoDroneMode.GUARD;
            case "DOCK" -> EchoDroneMode.DOCK;
            case "RECALL" -> EchoDroneMode.RECALL;
            case "COMBAT" -> EchoDroneMode.GUARD;
            default -> EchoDroneMode.parse(key, null);
        };
    }

    private static EchoCompanionDrone.DroneMode legacyModeFor(EchoDroneMode mode, String raw) {
        String key = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if ("COMBAT".equals(key)) {
            return EchoCompanionDrone.DroneMode.COMBAT;
        }
        if ("PATROL".equals(key)) {
            return EchoCompanionDrone.DroneMode.PATROL;
        }
        return switch (mode) {
            case FOLLOW, ASSIST, DOCK, RECALL -> EchoCompanionDrone.DroneMode.FOLLOW;
            case SCOUT -> EchoCompanionDrone.DroneMode.SCOUT;
            case SALVAGE -> EchoCompanionDrone.DroneMode.SCAVENGE;
            case GUARD -> null;
        };
    }

    private static int fallbackScout(ServerPlayer player, String raw) {
        ScoutDrone scout = findOwnedScoutDrone(player);
        if (scout == null) {
            send(player, "No linked companion or Scout Drone found.", ChatFormatting.RED, true);
            play(player, ModSounds.ECHO_MESSAGE.get(), 0.7F);
            return 0;
        }
        String command = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if ("RECALL".equals(command) || "RETURN".equals(command)) {
            scout.setMode(ScoutDrone.DroneMode.FOLLOW);
            Vec3 target = player.position().add(0.0D, 1.5D, 0.0D);
            scout.teleportTo(target.x, target.y, target.z);
            scout.setDeltaMovement(Vec3.ZERO);
            play(player, ModSounds.ECHO_COMPLETE.get(), 1.1F);
            send(player, "Scout Drone recalled.", ChatFormatting.GREEN, true);
            AshfallAdapterCoreExplorationRuntime.droneState(
                    player, "scout_recall", ScoutDrone.DroneMode.FOLLOW.name(), true, Map.of());
            return 1;
        }
        ScoutDrone.DroneMode mode = switch (command) {
            case "FOLLOW", "ASSIST" -> ScoutDrone.DroneMode.FOLLOW;
            case "SCOUT", "SCOUT_AHEAD", "SCAN", "SCAN_AREA", "SCAVENGE", "COLLECT_SCRAP" -> ScoutDrone.DroneMode.SCAVENGE;
            case "COMBAT", "PATROL", "GUARD", "GUARD_HERE" -> ScoutDrone.DroneMode.DEFENSE;
            default -> null;
        };
        if (mode == null) {
            send(player, "Unknown Scout Drone command.", ChatFormatting.RED, true);
            play(player, ModSounds.ECHO_MESSAGE.get(), 0.7F);
            return 0;
        }
        scout.setMode(mode);
        play(player, ModSounds.ECHO_COMPLETE.get(), 1.0F);
        send(player, "Scout mode: " + mode.getDisplayName(), ChatFormatting.GREEN, true);
        AshfallAdapterCoreExplorationRuntime.droneState(
                player, "scout_set_mode", mode.name(), true, Map.of());
        return 1;
    }

    private static ScoutDrone findOwnedScoutDrone(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(128.0D);
        ScoutDrone nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        for (ScoutDrone drone : level.getEntitiesOfClass(ScoutDrone.class, area,
                drone -> !drone.isRemoved() && drone.isAlive() && player.getUUID().equals(drone.getOwnerUUID()))) {
            double distance = drone.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = drone;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int rejectOrFallback(ServerPlayer player, String raw, String reason) {
        if (CompanionDroneStateStore.nearestOwned(player) == null) {
            return fallbackScout(player, raw);
        }
        return reject(player, reason);
    }

    private static int reject(ServerPlayer player, String reason) {
        send(player, reason, ChatFormatting.RED, true);
        play(player, ModSounds.ECHO_MESSAGE.get(), 0.7F);
        if (Config.LOG_DRONE_STATE_CHANGES.get()) {
            EchoAshfallProtocol.LOGGER.info("Companion Drone command rejected for {}: {}",
                    player.getScoreboardName(), reason);
        }
        return 0;
    }

    private static void send(ServerPlayer player, String message, ChatFormatting color, boolean actionbar) {
        player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] " + message).withStyle(color), actionbar);
    }

    private static void play(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.55F, pitch);
    }

}

package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public final class DroneWarningService {
    private DroneWarningService() {
    }

    public static void tickWarnings(EchoCompanionDrone drone, ServerLevel level, ServerPlayer owner) {
        if (drone == null || owner == null || !Config.ENABLE_HAZARD_WARNINGS.get()) {
            return;
        }
        CompanionDroneData data = CompanionDroneStateStore.get(owner);
        EchoDroneMode mode = data.getMode();
        if (mode != EchoDroneMode.ASSIST && mode != EchoDroneMode.FOLLOW && mode != EchoDroneMode.GUARD) {
            return;
        }
        if ("SILENT".equalsIgnoreCase(Config.DRONE_MESSAGE_VERBOSITY.get())) {
            return;
        }
        Warning warning = firstWarning(drone, level, owner, data, level.getGameTime());
        if (warning == null) {
            return;
        }
        long now = level.getGameTime();
        data.setLastWarningTime(now);
        data.setWarningTime(warning.category().key(), now);
        data.setLastWarning(warning.message());
        CompanionDroneStateStore.save(owner, data);
        drone.speak(warning.message(), warning.urgent() ? EchoCompanionDrone.MOOD_URGENT : EchoCompanionDrone.MOOD_CONCERNED,
                warning.urgent() ? 55 : 38, warning.urgent() ? 12 : 5);
        owner.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] " + warning.message()).withStyle(warning.color()), true);
    }

    private static Warning firstWarning(EchoCompanionDrone drone, ServerLevel level, ServerPlayer owner,
            CompanionDroneData data, long now) {
        if (owner.getHealth() <= Math.max(4.0F, owner.getMaxHealth() * 0.25F)) {
            Warning warning = new Warning(WarningCategory.OPERATOR_HEALTH, "Operator vitals low.",
                    ChatFormatting.RED, true);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (Config.ENABLE_HOSTILE_WARNINGS.get() && hostileNearby(level, owner, drone)) {
            Warning warning = new Warning(WarningCategory.HOSTILE, "Hostile movement detected.",
                    ChatFormatting.RED, false);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        HazardZoneManager.HazardSnapshot snapshot = HazardZoneManager.scan(owner);
        if (snapshot.toxicAir()) {
            Warning warning = new Warning(WarningCategory.HAZARD, "Toxic air detected ahead.",
                    ChatFormatting.YELLOW, false);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (snapshot.radiationZone() || snapshot.radiationStorm()) {
            Warning warning = new Warning(WarningCategory.HAZARD,
                    snapshot.radiationStorm() ? "Signal interference rising." : "Radiation spike nearby.",
                    ChatFormatting.YELLOW, snapshot.severity().ordinal() >= HazardZoneManager.HazardSeverity.HIGH.ordinal());
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (snapshot.acidContact()) {
            Warning warning = new Warning(WarningCategory.HAZARD, "Caustic surface detected.",
                    ChatFormatting.YELLOW, true);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (snapshot.nexusAnomaly()) {
            Warning warning = new Warning(WarningCategory.HAZARD, "Anomalous signal pressure rising.",
                    ChatFormatting.LIGHT_PURPLE, true);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (data.hasUpgrade(EchoDroneUpgrade.HAZARD_SENSOR)) {
            EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(owner);
            if (telemetry != null && telemetry.warning()) {
                Warning warning = new Warning(WarningCategory.HAZARD, telemetry.statusLine(), ChatFormatting.YELLOW,
                        telemetry.radiation() >= 70 || telemetry.toxicAir() >= 70 || telemetry.exposure() >= 70);
                if (canEmit(warning, data, now)) {
                    return warning;
                }
            }
        }
        if (Config.ENABLE_DRONE_SIGNAL.get() && data.getSignalQuality() <= 25) {
            Warning warning = new Warning(WarningCategory.SIGNAL, "Signal interference rising.",
                    ChatFormatting.LIGHT_PURPLE, true);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (Config.ENABLE_DRONE_BATTERY.get() && data.getBatteryPercent() <= 15) {
            Warning warning = new Warning(WarningCategory.BATTERY, "Battery low. Recall recommended.",
                    ChatFormatting.YELLOW, false);
            if (canEmit(warning, data, now)) {
                return warning;
            }
        }
        if (data.hasUpgrade(EchoDroneUpgrade.MISSION_DECODER) && Config.ENABLE_DRONE_MISSION_HINTS.get()) {
            String summary = data.getLastScanSummary();
            if (summary != null && summary.toLowerCase(java.util.Locale.ROOT).contains("objective")) {
                Warning warning = new Warning(WarningCategory.MISSION, "Mission target nearby.",
                        ChatFormatting.AQUA, false);
                if (canEmit(warning, data, now)) {
                    return warning;
                }
            }
        }
        return null;
    }

    private static boolean canEmit(Warning warning, CompanionDroneData data, long now) {
        if (warning == null || data == null) {
            return false;
        }
        String verbosity = Config.DRONE_MESSAGE_VERBOSITY.get();
        if ("SILENT".equalsIgnoreCase(verbosity)) {
            return false;
        }
        if ("MINIMAL".equalsIgnoreCase(verbosity) && !warning.urgent()) {
            return false;
        }
        if (!"TACTICAL".equalsIgnoreCase(verbosity) && warning.category() == WarningCategory.MISSION) {
            return false;
        }
        long last = data.warningTime(warning.category().key());
        return last == Long.MIN_VALUE || now - last >= Config.DRONE_WARNING_COOLDOWN_TICKS.get();
    }

    private static boolean hostileNearby(ServerLevel level, ServerPlayer owner, EchoCompanionDrone drone) {
        AABB area = owner.getBoundingBox().inflate(12.0D, 6.0D, 12.0D);
        return !level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive()
                        && entity != owner
                        && entity != drone
                        && (entity instanceof Monster || DroneScanService.isHostile(entity, owner))).isEmpty();
    }

    private enum WarningCategory {
        BATTERY("battery"),
        SIGNAL("signal"),
        OPERATOR_HEALTH("operator_health"),
        HOSTILE("hostile"),
        HAZARD("hazard"),
        MISSION("mission");

        private final String key;

        WarningCategory(String key) {
            this.key = key;
        }

        private String key() {
            return key;
        }
    }

    private record Warning(WarningCategory category, String message, ChatFormatting color, boolean urgent) {
    }
}

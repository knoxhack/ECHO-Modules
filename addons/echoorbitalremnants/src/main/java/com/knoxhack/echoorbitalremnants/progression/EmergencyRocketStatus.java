package com.knoxhack.echoorbitalremnants.progression;

import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public record EmergencyRocketStatus(
        boolean staged,
        boolean occupied,
        boolean riderIsPlayer,
        EmergencyRocketEntity.LaunchState launchState,
        int countdownTicks,
        int launchTicks,
        double distanceSqr
) {
    private static final double SEARCH_RADIUS = 16.0D;
    private static final double SEARCH_HEIGHT = 8.0D;
    public static final EmergencyRocketStatus NONE = new EmergencyRocketStatus(
            false, false, false, EmergencyRocketEntity.LaunchState.PLACED, 0, 0, Double.MAX_VALUE);

    public static EmergencyRocketStatus near(Player player) {
        AABB area = new AABB(
                player.getX() - SEARCH_RADIUS,
                player.getY() - SEARCH_HEIGHT,
                player.getZ() - SEARCH_RADIUS,
                player.getX() + SEARCH_RADIUS,
                player.getY() + SEARCH_HEIGHT,
                player.getZ() + SEARCH_RADIUS);
        return player.level().getEntities((Entity) null, area,
                        entity -> entity.getType() == ModEntities.EMERGENCY_ROCKET_VEHICLE.get())
                .stream()
                .filter(EmergencyRocketEntity.class::isInstance)
                .map(EmergencyRocketEntity.class::cast)
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .map(rocket -> new EmergencyRocketStatus(
                        true,
                        rocket.isVehicle(),
                        player.getVehicle() == rocket,
                        rocket.launchState(),
                        rocket.countdownTicks(),
                        rocket.launchTicks(),
                        player.distanceToSqr(rocket)))
                .orElse(NONE);
    }

    public boolean countingDown() {
        return launchState == EmergencyRocketEntity.LaunchState.COUNTDOWN;
    }

    public boolean launching() {
        return launchState == EmergencyRocketEntity.LaunchState.LAUNCHING;
    }

    public int countdownSeconds() {
        if (!countingDown()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(countdownTicks / 20.0D));
    }

    public float ascentProgress() {
        if (!launching()) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, launchTicks / (float) EmergencyRocketEntity.ASCENT_TICKS));
    }

    public String label(boolean lowOrbitReached, LaunchReadiness launch, LaunchReadiness assembly) {
        if (lowOrbitReached) {
            return "ORBIT CONFIRMED";
        }
        if (staged) {
            return switch (launchState) {
                case COUNTDOWN -> "T-MINUS " + countdownSeconds();
                case LAUNCHING -> "ASCENT COMMITTED";
                case PLACED -> occupied ? "CABIN OCCUPIED" : "VEHICLE STAGED";
            };
        }
        if (!launch.ready()) {
            return "LAUNCH HOLD";
        }
        if (!assembly.ready()) {
            return "ASSEMBLY HOLD";
        }
        return "READY TO STAGE";
    }

    public String detail(boolean lowOrbitReached, LaunchReadiness launch, LaunchReadiness assembly) {
        if (lowOrbitReached) {
            return "Low Earth Orbit reached. Earth return vector saved.";
        }
        if (staged) {
            if (!launch.ready() && launchState != EmergencyRocketEntity.LaunchState.LAUNCHING) {
                return "Vehicle is staged, but launch systems are missing: " + missingSummary(launch) + ".";
            }
            return switch (launchState) {
                case COUNTDOWN -> "Stay seated until ignition. Dismount before ignition aborts the countdown.";
                case LAUNCHING -> "Ignition is committed. Orbit handoff is in progress.";
                case PLACED -> occupied
                        ? "Cabin is sealed. The rider right-clicks the rocket again to start countdown."
                        : "Vehicle is staged. Right-click the rocket to board the cabin.";
            };
        }
        if (!launch.ready()) {
            return "Missing launch systems: " + missingSummary(launch) + ".";
        }
        if (!assembly.ready()) {
            return "Missing assembly parts: " + missingSummary(assembly) + ".";
        }
        return "Use the Emergency Rocket on a complete 5x5 Launch Platform to stage the vehicle.";
    }

    public String nextObjective(boolean lowOrbitReached, LaunchReadiness launch, LaunchReadiness assembly) {
        String label = label(lowOrbitReached, launch, assembly);
        String detail = detail(lowOrbitReached, launch, assembly);
        return "Next Step: " + label + ". " + detail;
    }

    private static String missingSummary(LaunchReadiness readiness) {
        List<String> missing = readiness.missing().stream()
                .limit(3)
                .map(component -> component.getString().replaceFirst("^- ", ""))
                .toList();
        if (missing.isEmpty()) {
            return "checklist incomplete";
        }
        String summary = String.join(", ", missing);
        if (readiness.missing().size() > missing.size()) {
            summary += ", +" + (readiness.missing().size() - missing.size()) + " more";
        }
        return summary;
    }
}

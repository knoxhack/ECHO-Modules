package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class BossEncounterSupport {
    static final OrbitalBossProfile DOCKING_AI = new OrbitalBossProfile(
            "echoorbitalremnants:corrupted_docking_ai",
            "Corrupted Docking AI",
            "ECHO-7 // Docking AI rerouting reserve drones through emergency ports. Station ECHO still thinks arrivals are contamination.",
            "ECHO-7 // Docking AI hard-locking airlock pressure. End this before the room becomes an instruction.",
            "ECHO-7 // Corrupted Docking AI cycling hostile airlock logic.",
            "ECHO-7 // Docking AI reserve ports open. Seal pressure and clear drones.",
            "ECHO-7 // Docking AI final airlock cycle. Pressure locks are hostile.",
            SuitPressure.PRESSURE,
            true,
            "Docking AI black box recovered; station cache authority restored and quarantine docking logic archived.");
    static final OrbitalBossProfile CAPTAIN = new OrbitalBossProfile(
            "echoorbitalremnants:abandoned_captain",
            "The Abandoned Captain",
            "ECHO-7 // Captain distress channel open. Broken crew signatures converging.",
            "ECHO-7 // Captain command chain corrupted by Nexus quarantine code. Crew loyalty survived the crew.",
            "ECHO-7 // Captain telemetry matches Station ECHO pod records.",
            "ECHO-7 // Captain command loop waking failed crew telemetry.",
            "ECHO-7 // Captain oxygen siphon at crisis load. Clear the crew loop.",
            SuitPressure.OXYGEN,
            true,
            "Captain black box recovered; pod records, fall path, and Mars transfer logs archived.");
    static final OrbitalBossProfile EUROPA_WARDEN = new OrbitalBossProfile(
            "echoorbitalremnants:europa_cryo_warden",
            "Europa Cryo Warden",
            "ECHO-7 // Europa Warden rerouting vents through the thermal arrays. Cold is being used as command language.",
            "ECHO-7 // Europa Warden entering hard-freeze protocol. Thermal cover matters now.",
            "ECHO-7 // Cryo Warden vent pulse destabilizing suit pressure.",
            "ECHO-7 // Cryo Warden vent chain active. Use thermal arrays between pulses.",
            "ECHO-7 // Cryo Warden final vent cycle. Find thermal cover or mobility will fail.",
            SuitPressure.THERMAL,
            true,
            "Europa black box recovered; thermal array data secured before quarantine cold could reseal it.");
    static final OrbitalBossProfile ECHO_ZERO = new OrbitalBossProfile(
            "echoorbitalremnants:echo_zero",
            "ECHO-0",
            "ECHO-0 // Phase 2. Pressure loops inverted. Quarantine motive remains absolute.",
            "ECHO-0 // Final protocol. Earth must remain silent or the signal returns.",
            "ECHO-0 // Earth remains contained. Living routes feed the signal.",
            "ECHO-0 // Quarantine pulse. Pressure and radiation rising.",
            "ECHO-0 // Final protocol pulse. Oxygen, pressure, and radiation protections are under attack.",
            SuitPressure.QUARANTINE,
            true,
            "ECHO-0 black box recovered; quarantine motive archived for post-Nexus stabilization.");

    private BossEncounterSupport() {
    }

    static ServerBossEvent bossBar(LivingEntity entity, String name, BossEvent.BossBarColor color) {
        return new ServerBossEvent(entity.getUUID(), Component.literal(name), color, BossEvent.BossBarOverlay.PROGRESS);
    }

    static void update(ServerBossEvent event, LivingEntity entity) {
        event.setProgress(healthPercent(entity));
    }

    static void clear(ServerBossEvent event) {
        event.removeAllPlayers();
    }

    static void report(Player player, String message) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.setLastTerminalReport(player, message);
        player.sendSystemMessage(Component.literal("ECHO-7 // " + message));
    }

    static void reportPhase(LivingEntity entity, String message) {
        if (entity instanceof Mob mob && mob.getTarget() instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    static float healthPercent(LivingEntity entity) {
        return Math.max(0.0F, entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()));
    }

    static int phaseFor(LivingEntity entity, int currentPhase, float phaseTwoThreshold, float phaseThreeThreshold) {
        float health = healthPercent(entity);
        if (currentPhase < 3 && health <= phaseThreeThreshold) {
            return 3;
        }
        if (currentPhase < 2 && health <= phaseTwoThreshold) {
            return 2;
        }
        return currentPhase;
    }

    static int updatePhase(LivingEntity entity, int currentPhase, OrbitalBossProfile profile) {
        int next = phaseFor(entity, currentPhase, 0.65F, 0.35F);
        if (next != currentPhase) {
            reportPhase(entity, next >= 3 ? profile.phaseThreeLine() : profile.phaseTwoLine());
        }
        return next;
    }

    static void reportSuitPressure(Player player, OrbitalBossProfile profile, int phase) {
        player.sendSystemMessage(Component.literal(profile.pressureLine(phase)));
    }

    static void giveBlackBox(Player player, OrbitalBossProfile profile) {
        if (!profile.blackBoxReward()) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.ORBITAL_BLACK_BOX.get());
        if (!player.getInventory().contains(stack)) {
            give(player, stack);
        }
    }

    static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    enum SuitPressure {
        PRESSURE,
        OXYGEN,
        THERMAL,
        QUARANTINE
    }

    record OrbitalBossProfile(
            String bossId,
            String title,
            String phaseTwoLine,
            String phaseThreeLine,
            String phaseOnePressureLine,
            String phaseTwoPressureLine,
            String phaseThreePressureLine,
            SuitPressure pressureType,
            boolean blackBoxReward,
            String terminalArchiveCopy
    ) {
        String pressureLine(int phase) {
            if (phase >= 3) {
                return phaseThreePressureLine;
            }
            if (phase >= 2) {
                return phaseTwoPressureLine;
            }
            return phaseOnePressureLine;
        }
    }
}

package com.knoxhack.echostationfall.progression;

import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallStationState;
import com.knoxhack.echostationfall.integration.StationfallSuitState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class StationfallRouteTracker {
    private StationfallRouteTracker() {
    }

    public static String status(Player player) {
        StationfallProgress progress = StationfallProgress.get(player);
        StationfallSuitState suit = StationfallSuitState.get(player);
        SignalPanicState panic = SignalPanicState.get(player);
        StationSection section = StationfallDimensions.isStation(player.level())
                ? StationSection.fromPosition(player.blockPosition())
                : null;
        StationfallStationState station = player.level() instanceof ServerLevel level
                ? StationfallStationState.get(level)
                : null;

        String sectionName = section == null ? "not aboard" : section.displayName();
        String next = nextStep(progress, station, section);
        String boss = station != null && station.bossActive()
                ? "active"
                : progress.bossDefeated() || (station != null && station.bossDefeated()) ? "defeated" : "locked";
        String blackbox = progress.blackboxRetrieved() || (station != null && station.blackboxRewarded()) ? "recovered" : "pending";

        return "Section: " + sectionName
                + " | Next: " + next
                + " | oxygen " + suit.oxygen() + "%"
                + " | pressure " + suit.pressure() + "%"
                + " | panic " + panic.value() + "%"
                + " | power " + progress.poweredSectionCount() + "/9"
                + " | logs " + progress.decodedLogCount() + "/9"
                + " | objectives " + progress.objectiveCount() + "/" + StationfallObjective.values().length
                + " | override " + yesNo(progress.aiOverrideObtained())
                + " | mother " + boss
                + " | blackbox " + blackbox;
    }

    public static String actionbarHint(Player player) {
        StationfallProgress progress = StationfallProgress.get(player);
        StationSection section = StationfallDimensions.isStation(player.level())
                ? StationSection.fromPosition(player.blockPosition())
                : null;
        StationfallStationState station = player.level() instanceof ServerLevel level
                ? StationfallStationState.get(level)
                : null;
        return "ECHO-7 // " + nextStep(progress, station, section);
    }

    private static String nextStep(
            StationfallProgress progress,
            StationfallStationState station,
            StationSection section
    ) {
        if (!progress.boarded()) {
            return "Board Stationfall from the Terminal or Station Access Card.";
        }
        if (section != null) {
            StationPowerState power = station != null ? station.powerState(section) : progress.powerState(section);
            if (!power.stableOrBetter()) {
                return "Restore " + section.displayName() + " power.";
            }
            if (!progress.logDecoded(section)) {
                return "Decode " + section.displayName() + " crew log.";
            }
            StationfallObjective objective = StationfallObjective.bySection(section);
            if (objective != null && !progress.objectiveComplete(objective)) {
                return objective.hint() + " (" + progress.objectiveStepCount(objective) + "/" + objective.targetSteps() + ")";
            }
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            if (!progress.objectiveComplete(objective)) {
                return objective.title() + " in " + objective.section().displayName()
                        + " (" + progress.objectiveStepCount(objective) + "/" + objective.targetSteps() + ").";
            }
        }
        if (progress.poweredSectionCount() < StationSection.values().length) {
            return "Bring remaining section power online.";
        }
        if (progress.decodedLogCount() < StationSection.values().length) {
            return "Decode remaining crew logs.";
        }
        if (!progress.aiOverrideObtained()) {
            return "Recover the AI Override in the Data Core.";
        }
        if (!progress.bossDefeated()) {
            return "Use the Command Console and defeat Station Mother.";
        }
        if (!progress.blackboxRetrieved()) {
            return "Claim the Stationfall Blackbox.";
        }
        return "Return from Stationfall with the blackbox.";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}

package com.knoxhack.echostationfall.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallStationState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class StationfallCoreIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static final EchoAddonChapter CHAPTER = new EchoAddonChapter() {
        @Override
        public String id() {
            return "stationfall";
        }

        @Override
        public String modId() {
            return EchoStationfall.MODID;
        }

        @Override
        public String displayName() {
            return "ECHO: Stationfall";
        }

        @Override
        public String summary() {
            return "Board the dead orbital station, restore nine sections, stabilize major sections, survive Signal Panic, and recover the Stationfall Blackbox.";
        }

        @Override
        public boolean isAvailable(Player player) {
            return player != null && StationfallProgress.get(player).canBoard(player);
        }

        @Override
        public String statusLine(Player player) {
            if (player == null) {
                return "Stationfall waits for player telemetry.";
            }
            StationfallProgress progress = StationfallProgress.get(player);
            if (!progress.canBoard(player)) {
                return "Locked behind Stationfall coordinates or Station Network restoration.";
            }
            if (progress.blackboxRetrieved()) {
                return "Stationfall Blackbox recovered. Blackbox Protocol handoff exposed.";
            }
            return progress.poweredSectionCount() + "/9 sections stable, "
                    + progress.decodedLogCount() + "/9 crew logs decoded, "
                    + progress.objectiveCount() + "/" + StationfallObjective.values().length
                    + " objectives stabilized.";
        }
    };

    private StationfallCoreIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoAddonRegistry.register(CHAPTER);
        EchoCoreServices.registerRouteRecordService(StationfallCoreIntegration::routes);
        EchoCoreServices.registerDiagnosticService(StationfallCoreIntegration::diagnostics);
        EchoCoreServices.registerHazardTelemetryService(StationfallCoreIntegration::telemetry);
    }

    private static List<EchoRouteRecord> routes(Player player) {
        if (player == null) {
            return List.of(new EchoRouteRecord(
                    id("stationfall_route"),
                    "stationfall",
                    "Stationfall Boarding Route",
                    "Dungeon",
                    "echostationfall:stationfall_station",
                    "LOCKED",
                    "Stationfall waits for player telemetry.",
                    false));
        }
        StationfallProgress progress = StationfallProgress.get(player);
        boolean complete = progress.blackboxRetrieved();
        return List.of(new EchoRouteRecord(
                id("stationfall_route"),
                "stationfall",
                "Stationfall Boarding Route",
                "Dungeon",
                "echostationfall:stationfall_station",
                !progress.canBoard(player) ? "LOCKED" : complete ? "BLACKBOX RECOVERED" : progress.boarded() ? "ACTIVE" : "READY",
                summary(player),
                complete));
    }

    private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        if (player == null) {
            return List.of();
        }
        StationfallProgress progress = StationfallProgress.get(player);
        List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
        if (!progress.canBoard(player)) {
            blockers.add(new EchoDiagnosticBlocker(
                    id("stationfall_orbital_gate"),
                    "stationfall",
                    EchoDiagnosticBlocker.Severity.BLOCKED,
                    "Stationfall route locked",
                    "Stationfall coordinates or Station Network restoration are missing.",
                    "Recover Stationfall coordinates, or complete the optional Orbital Remnants route."));
        }

        StationfallSuitState suit = StationfallSuitState.get(player);
        if (StationfallDimensions.isStation(player.level()) && (suit.oxygen() <= 25 || suit.pressure() <= 25)) {
            blockers.add(new EchoDiagnosticBlocker(
                    id("stationfall_suit_critical"),
                    "stationfall",
                    EchoDiagnosticBlocker.Severity.CRITICAL,
                    "Suit reserves critical",
                    "Oxygen " + suit.oxygen() + "%, pressure " + suit.pressure() + "%.",
                    "Use Emergency Oxygen Pack, Pressure Seal Kit, or restore local power."));
        }

        SignalPanicState panic = SignalPanicState.get(player);
        if (panic.critical()) {
            blockers.add(new EchoDiagnosticBlocker(
                    id("stationfall_signal_panic"),
                    "stationfall",
                    EchoDiagnosticBlocker.Severity.WARNING,
                    "Signal Panic critical",
                    "False ECHO events and door hallucinations are active.",
                    "Move to stable power or use a Signal Panic Dampener."));
        }
        return List.copyOf(blockers);
    }

    private static EchoHazardTelemetry telemetry(Player player) {
        if (player == null) {
            return EchoHazardTelemetry.nominal();
        }
        if (!StationfallDimensions.isStation(player.level())) {
            return EchoHazardTelemetry.nominal();
        }
        StationfallSuitState suit = StationfallSuitState.get(player);
        SignalPanicState panic = SignalPanicState.get(player);
        StationSection section = StationSection.fromPosition(player.blockPosition());
        StationPowerState power = StationPowerState.EMERGENCY;
        if (player.level() instanceof ServerLevel serverLevel) {
            power = StationfallStationState.get(serverLevel).powerState(section);
        }
        int exposure = Math.max(panic.value(), power.hostile() ? 55 : 15);
        return new EchoHazardTelemetry(
                100,
                0,
                0,
                suit.oxygen(),
                suit.pressure(),
                0,
                power == StationPowerState.OVERLOADED ? 45 : 0,
                exposure,
                "Stationfall: " + section.displayName() + " / " + power.displayName()
                        + " / Signal Panic " + panic.value() + "%");
    }

    private static String summary(Player player) {
        if (player == null) {
            return "Stationfall waits for player telemetry.";
        }
        StationfallProgress progress = StationfallProgress.get(player);
        if (!progress.canBoard(player)) {
            return "Dead station route is sealed until Stationfall coordinates or Station Network access are restored.";
        }
        if (StationfallDimensions.isStation(player.level())) {
            StationSection section = StationSection.fromPosition(player.blockPosition());
            return "Current section: " + section.displayName() + ". "
                    + progress.poweredSectionCount() + "/9 stable, "
                    + progress.decodedLogCount() + "/9 logs, "
                    + progress.objectiveCount() + "/" + StationfallObjective.values().length
                    + " objectives.";
        }
        return "Board with the Station Access Card, the Stationfall command path, or Terminal BOARD STATION action.";
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path);
    }
}

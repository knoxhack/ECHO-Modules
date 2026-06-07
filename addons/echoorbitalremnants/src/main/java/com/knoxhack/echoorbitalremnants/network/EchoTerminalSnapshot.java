package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.integration.AshfallCompat;
import com.knoxhack.echoorbitalremnants.progression.EmergencyRocketStatus;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record EchoTerminalSnapshot(
        String activeTab,
        String location,
        String nextObjective,
        String missionStep,
        String scanRequirement,
        String scanReport,
        String surveyStatus,
        String localHazard,
        List<String> surveyLines,
        List<String> groundSiteLines,
        boolean orbitalExposure,
        boolean launchReady,
        List<String> launchMissing,
        boolean assemblyReady,
        List<String> assemblyMissing,
        String rocketLaunchStatus,
        String rocketLaunchDetail,
        int rocketCountdownSeconds,
        float rocketAscentProgress,
        boolean rocketStaged,
        boolean rocketOccupied,
        boolean rocketCountingDown,
        boolean rocketLaunching,
        int oxygen,
        int pressure,
        int radiation,
        boolean sealSecure,
        boolean suitLeak,
        String gravity,
        int stationPower,
        boolean lunarOpen,
        boolean marsOpen,
        boolean europaOpen,
        boolean saturnOpen,
        boolean titanOpen,
        boolean nexusOpen,
        boolean lowOrbitReached,
        boolean stationCoordinates,
        boolean lunarInvestigated,
        boolean marsVisited,
        boolean europaVisited,
        boolean saturnVisited,
        boolean titanVisited,
        boolean anomalyEntered,
        boolean echoZero,
        boolean finalComplete,
        int echoMemory,
        boolean earthReturnSaved,
        boolean routeReturnSaved,
        String orbitalRemnantStanding,
        String voidSalvagerStanding,
        String nexusChoirStanding,
        String factionContract,
        String missionHelp
) {
    private static final int MAX_STRING = 512;
    private static final int MAX_LIST = 12;

    public static EchoTerminalSnapshot from(Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        SuitState suit = SuitState.get(player);
        LaunchReadiness launch = LaunchReadiness.evaluateForLaunch(player);
        LaunchReadiness assembly = LaunchReadiness.evaluateForAssembly(player);
        EmergencyRocketStatus rocket = EmergencyRocketStatus.near(player);
        boolean ashesLocked = !progress.launchSiteTracked() && AshfallCompat.isOrbitalCalibrationLocked(player);
        return new EchoTerminalSnapshot(
                progress.activeTab(),
                player.level().dimension().identifier().toString(),
                ashesLocked ? "Next Step: Resolve an ECHO: Ashfall Protocol Nexus path to unlock Orbital Remnants."
                        : launchObjective(progress, launch, assembly, rocket),
                progress.missionStep(),
                ashesLocked ? "Nexus decision required before orbital calibration can begin." : progress.scanRequirement(),
                progress.lastTerminalReport(),
                progress.surveyStatus(),
                progress.localHazardText(player),
                progress.surveyLines(),
                progress.groundSiteLines(player),
                SuitEvents.isOrbitalExposure(player),
                launch.ready(),
                missingText(launch),
                assembly.ready(),
                missingText(assembly),
                rocket.label(progress.lowOrbitReached(), launch, assembly),
                rocket.detail(progress.lowOrbitReached(), launch, assembly),
                rocket.countdownSeconds(),
                rocket.ascentProgress(),
                rocket.staged(),
                rocket.occupied(),
                rocket.countingDown(),
                rocket.launching(),
                suit.oxygen(),
                suit.pressure(),
                suit.radiation(),
                suit.helmetSealSecure(),
                suit.suitLeak(),
                String.format(Locale.ROOT, "%.2fG", suit.gravity()),
                suit.stationPower(),
                progress.lunarSignalUnlocked(),
                progress.marsRouteUnlocked(),
                progress.europaRouteUnlocked(),
                progress.saturnRouteUnlocked(),
                progress.titanRouteUnlocked(),
                progress.deepSpaceProtocolUnlocked(),
                progress.lowOrbitReached(),
                progress.stationCoordinatesRecovered(),
                progress.lunarSignalInvestigated(),
                progress.marsAshBasinVisited(),
                progress.europaCryoOceanVisited(),
                progress.saturnRingGraveyardVisited(),
                progress.titanMethaneShelfVisited(),
                progress.anomalyBeltEntered(),
                progress.echoZeroEncountered(),
                progress.finalNetworkSealed(),
                progress.echoMemoryFragments(),
                progress.hasEarthReturnPoint(),
                progress.hasReturnPoint(),
                progress.orbitalRemnantStanding().name(),
                progress.voidSalvagerStanding().name(),
                progress.nexusChoirStanding().name(),
                progress.factionContractStatus(),
                progress.missionHelpReport());
    }

    public static EchoTerminalSnapshot read(RegistryFriendlyByteBuf buffer) {
        return new EchoTerminalSnapshot(
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                readList(buffer),
                readList(buffer),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readList(buffer),
                buffer.readBoolean(),
                readList(buffer),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_STRING),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING));
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(activeTab, MAX_STRING);
        buffer.writeUtf(location, MAX_STRING);
        buffer.writeUtf(nextObjective, MAX_STRING);
        buffer.writeUtf(missionStep, MAX_STRING);
        buffer.writeUtf(scanRequirement, MAX_STRING);
        buffer.writeUtf(scanReport, MAX_STRING);
        buffer.writeUtf(surveyStatus, MAX_STRING);
        buffer.writeUtf(localHazard, MAX_STRING);
        writeList(buffer, surveyLines);
        writeList(buffer, groundSiteLines);
        buffer.writeBoolean(orbitalExposure);
        buffer.writeBoolean(launchReady);
        writeList(buffer, launchMissing);
        buffer.writeBoolean(assemblyReady);
        writeList(buffer, assemblyMissing);
        buffer.writeUtf(rocketLaunchStatus, MAX_STRING);
        buffer.writeUtf(rocketLaunchDetail, MAX_STRING);
        buffer.writeInt(rocketCountdownSeconds);
        buffer.writeFloat(rocketAscentProgress);
        buffer.writeBoolean(rocketStaged);
        buffer.writeBoolean(rocketOccupied);
        buffer.writeBoolean(rocketCountingDown);
        buffer.writeBoolean(rocketLaunching);
        buffer.writeInt(oxygen);
        buffer.writeInt(pressure);
        buffer.writeInt(radiation);
        buffer.writeBoolean(sealSecure);
        buffer.writeBoolean(suitLeak);
        buffer.writeUtf(gravity, MAX_STRING);
        buffer.writeInt(stationPower);
        buffer.writeBoolean(lunarOpen);
        buffer.writeBoolean(marsOpen);
        buffer.writeBoolean(europaOpen);
        buffer.writeBoolean(saturnOpen);
        buffer.writeBoolean(titanOpen);
        buffer.writeBoolean(nexusOpen);
        buffer.writeBoolean(lowOrbitReached);
        buffer.writeBoolean(stationCoordinates);
        buffer.writeBoolean(lunarInvestigated);
        buffer.writeBoolean(marsVisited);
        buffer.writeBoolean(europaVisited);
        buffer.writeBoolean(saturnVisited);
        buffer.writeBoolean(titanVisited);
        buffer.writeBoolean(anomalyEntered);
        buffer.writeBoolean(echoZero);
        buffer.writeBoolean(finalComplete);
        buffer.writeInt(echoMemory);
        buffer.writeBoolean(earthReturnSaved);
        buffer.writeBoolean(routeReturnSaved);
        buffer.writeUtf(orbitalRemnantStanding, MAX_STRING);
        buffer.writeUtf(voidSalvagerStanding, MAX_STRING);
        buffer.writeUtf(nexusChoirStanding, MAX_STRING);
        buffer.writeUtf(factionContract, MAX_STRING);
        buffer.writeUtf(missionHelp, MAX_STRING);
    }

    private static List<String> missingText(LaunchReadiness readiness) {
        return readiness.missing().stream()
                .limit(MAX_LIST)
                .map(component -> component.getString().replaceFirst("^- ", ""))
                .toList();
    }

    private static String launchObjective(EchoTerminalProgress progress, LaunchReadiness launch,
            LaunchReadiness assembly, EmergencyRocketStatus rocket) {
        if (progress.launchSiteTracked() && !progress.lowOrbitReached() && launch.ready() && assembly.ready()) {
            return rocket.nextObjective(false, launch, assembly);
        }
        return progress.nextObjective(launch, assembly);
    }

    private static List<String> readList(RegistryFriendlyByteBuf buffer) {
        int size = Math.min(MAX_LIST, buffer.readInt());
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buffer.readUtf(MAX_STRING));
        }
        return List.copyOf(values);
    }

    private static void writeList(RegistryFriendlyByteBuf buffer, List<String> values) {
        int size = Math.min(MAX_LIST, values.size());
        buffer.writeInt(size);
        for (int i = 0; i < size; i++) {
            buffer.writeUtf(values.get(i), MAX_STRING);
        }
    }
}

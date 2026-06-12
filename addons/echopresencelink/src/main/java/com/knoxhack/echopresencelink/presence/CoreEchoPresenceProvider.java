package com.knoxhack.echopresencelink.presence;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionProfile;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoPackMode;
import com.echoplatform.echocore.api.WorldContextSnapshot;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.mission.IMissionProgressView;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.api.EchoPresenceContext;
import com.knoxhack.echopresencelink.api.EchoPresenceProvider;
import com.knoxhack.echopresencelink.api.EchoPresenceSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class CoreEchoPresenceProvider implements EchoPresenceProvider {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, "core");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public EchoPresenceSnapshot snapshot(EchoPresenceContext context) {
        Player player = context == null ? null : context.player();
        long start = context == null ? 0L : context.sessionStartEpochSeconds();
        if (player == null) {
            return EchoPresenceSnapshot.of(ID, 5, "ECHO Presence Link", "Awaiting field telemetry",
                    "echo_terminal", start);
        }

        EchoPackMode packMode = EchoCoreServices.packMode(player);
        String details = packMode.displayName();

        String nexus = EchoCoreServices.nexusCampaignStatusLine(player);
        if (EchoCoreServices.nexusInstability(player) > 0 || !EchoCoreServices.nexusCampaignPathId(player).isBlank()) {
            return new EchoPresenceSnapshot(ID, 65, "Nexus Campaign", nexus, "nexus_core",
                    "Nexus Campaign", "echo_ashfall", "ECHO", start, List.of(), false);
        }

        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player);
        if (telemetry.warning()) {
            return new EchoPresenceSnapshot(ID, 55, details, telemetry.statusLine(), hazardAsset(telemetry),
                    "Hazard Telemetry", "echo_ashfall", "ECHO", start, List.of(), false);
        }

        WorldContextSnapshot world = EchoCoreServices.worldContext(player);
        WorldHazardSnapshot worldHazard = world.hazard();
        if (!worldHazard.safeZone()) {
            return new EchoPresenceSnapshot(ID, 50, details, worldHazard.summary(), assetForHazard(worldHazard),
                    "World Hazard", "echo_ashfall", "ECHO", start, List.of(), false);
        }

        Optional<IMissionProgressView> mission = activeMission(player);
        if (mission.isPresent()) {
            MissionDefinition definition = mission.get().definition();
            String missionDetails = details + " | P" + (definition.phaseOrder() + 1) + " " + definition.phaseTitle();
            String state = mission.get().actionHint().isBlank() ? definition.title() : mission.get().actionHint();
            return new EchoPresenceSnapshot(ID, 35, missionDetails, state, "echo_ashfall",
                    details, "echo_terminal", "Mission Feed", start, List.of(), false);
        }

        Optional<EchoFactionProfile> faction = EchoCoreServices.factionProfiles(player).stream()
                .filter(EchoFactionProfile::contacted)
                .max(Comparator.comparingInt(EchoFactionProfile::reputation));
        if (faction.isPresent()) {
            EchoFactionProfile profile = faction.get();
            return new EchoPresenceSnapshot(ID, 25, details,
                    "Tracking " + profile.definition().shortName() + " field standing",
                    assetForFaction(profile.definition().id()), profile.definition().displayName(),
                    "echo_ashfall", "ECHO", start, List.of(), false);
        }

        Optional<WorldRegionInstance> region = world.currentRegionOptional();
        if (region.isPresent() && region.get().discovered()) {
            return new EchoPresenceSnapshot(ID, 20, details, "Surveying " + region.get().displayName(),
                    "echo_ashfall", region.get().displayName(), "", "", start, List.of(), false);
        }

        return EchoPresenceSnapshot.of(ID, 10, details, packMode.statusLine(), "echo_ashfall", start);
    }

    @Override
    public int order() {
        return 0;
    }

    private static Optional<IMissionProgressView> activeMission(Player player) {
        return EchoCoreServices.missionService().missions(player).stream()
                .filter(view -> view != null && view.definition() != null)
                .filter(view -> view.status() == MissionStatus.ACTIVE || view.status() == MissionStatus.CLAIMABLE
                        || view.status() == MissionStatus.UNLOCKED)
                .sorted(Comparator
                        .comparingInt((IMissionProgressView view) -> statusRank(view.status()))
                        .thenComparingInt(view -> view.definition().phaseOrder())
                        .thenComparingInt(view -> view.definition().missionOrder()))
                .findFirst();
    }

    private static int statusRank(MissionStatus status) {
        return switch (status == null ? MissionStatus.LOCKED : status) {
            case ACTIVE -> 0;
            case CLAIMABLE -> 1;
            case UNLOCKED -> 2;
            default -> 9;
        };
    }

    private static String hazardAsset(EchoHazardTelemetry telemetry) {
        if (telemetry.radiation() >= 50) {
            return "hazard_radiation";
        }
        if (telemetry.toxicAir() >= 50 || telemetry.oxygen() <= 35) {
            return "hazard_toxic";
        }
        if (telemetry.cold() >= 50) {
            return "hazard_cold";
        }
        return "hazard_mutation";
    }

    private static String assetForHazard(WorldHazardSnapshot hazard) {
        String joined = hazard.hazardIds().toString().toLowerCase(java.util.Locale.ROOT);
        if (joined.contains("cold") || joined.contains("cryo")) {
            return "hazard_cold";
        }
        if (joined.contains("toxic") || joined.contains("acid")) {
            return "hazard_toxic";
        }
        if (joined.contains("mutation") || joined.contains("nexus")) {
            return "hazard_mutation";
        }
        return "hazard_radiation";
    }

    private static String assetForFaction(Identifier factionId) {
        String path = factionId == null ? "" : factionId.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("crash")) {
            return "faction_crashbreak";
        }
        if (path.contains("spore")) {
            return "faction_sporebound";
        }
        return "faction_radwarden";
    }
}

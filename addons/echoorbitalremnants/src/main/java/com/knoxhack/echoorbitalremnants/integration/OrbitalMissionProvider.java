package com.knoxhack.echoorbitalremnants.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.EmergencyRocketStatus;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class OrbitalMissionProvider implements TerminalMissionProvider {
    public static final OrbitalMissionProvider INSTANCE = new OrbitalMissionProvider();
    private static final String ACTION_SCAN = "scan";
    private static final String ACTION_CLAIM_CACHE = "claim_cache";
    private static final int ACCENT = 0xFF82E9FF;

    private OrbitalMissionProvider() {
    }

    @Override
    public TerminalMissionChapter chapter() {
        return new TerminalMissionChapter(
                OrbitalTerminalIds.CHAPTER_ID,
                "ECHO-0 ROUTE CHAIN",
                "Integrated ECHO-7 route records for Ashfall recovery, Station ECHO debris, ECHO-0 quarantine, surveys, and the final network seal.",
                300,
                ACCENT,
                true);
    }

    @Override
    public List<TerminalMissionDefinition> missions(Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        LaunchReadiness launch = LaunchReadiness.evaluateForLaunch(player);
        LaunchReadiness assembly = LaunchReadiness.evaluateForAssembly(player);
        return java.util.Arrays.stream(OrbitalMission.values())
                .map(mission -> definition(player, progress, launch, assembly, mission))
                .toList();
    }

    @Override
    public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
        OrbitalMission mission = mission(missionId);
        if (mission == null) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "UNKNOWN", "Orbital route signal missing from the current ECHO-0 index.",
                    "No clean Orbital Remnants field record is available for this signal.", List.of());
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        boolean available = isAvailable(player, progress, mission);
        boolean complete = isComplete(progress, mission);
        boolean claimed = progress.hasTerminalMissionCacheClaimed(mission.path());
        TerminalMissionStatus status = !available
                ? TerminalMissionStatus.LOCKED
                : complete
                        ? claimed ? TerminalMissionStatus.CLAIMED : TerminalMissionStatus.CLAIMABLE
                        : TerminalMissionStatus.UNLOCKED;
        List<TerminalMissionAction> actions = actions(player, progress, mission, available, complete, claimed);
        return new TerminalMissionSnapshot(
                mission.id(),
                status,
                progress(player, progress, mission),
                statusLabel(status),
                available ? "" : lockedReason(player, progress, mission),
                actionHint(player, progress, mission, available, complete, claimed),
                actions);
    }

    @Override
    public TerminalMissionPresentation presentation(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        return new TerminalMissionPresentation(
                definition.title(),
                definition.briefing(),
                snapshot.actionHint(),
                definition.phaseTitle(),
                switch (snapshot.status()) {
                    case CLAIMABLE, CLAIMED, COMPLETED -> "success";
                    case UNLOCKED -> "active";
                    case LOCKED, VIEW_ONLY -> "muted";
                },
                List.of(definition.category(), definition.difficulty(), snapshot.statusLabel()),
                "echoorbitalremnants:" + definition.id().getPath());
    }

    @Override
    public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        return TerminalMissionRole.MAIN;
    }

    @Override
    public Optional<TerminalMissionRoutePlacement> routePlacement(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        OrbitalMission mission = mission(definition == null ? null : definition.id());
        if (mission == null) {
            return Optional.empty();
        }
        int phase = switch (mission) {
            case EARTH_CALIBRATION, LAUNCH_CHAIN, LOW_ORBIT, STATION_NETWORK -> 10;
            case LUNAR_SIGNAL, MARS_ROUTE, EUROPA_ROUTE, SATURN_ROUTE, TITAN_ROUTE, DEEP_SPACE_PROTOCOL -> 13;
            case ECHO_ZERO -> 14;
            case SURVEY_NETWORK, FACTION_CONTRACT, FINAL_SEAL -> 15;
        };
        return Optional.of(TerminalMissionRoutePlacement.main(
                phase, mission.phaseOrder() * 100 + mission.order()));
    }

    @Override
    public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        OrbitalMission mission = mission(missionId);
        if (mission == null) {
            return false;
        }
        if (ACTION_SCAN.equals(actionId)) {
            return OrbitalTerminalActions.scan(player);
        }
        if (!ACTION_CLAIM_CACHE.equals(actionId)) {
            return false;
        }
        if (player == null) {
            return false;
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        if (!isAvailable(player, progress, mission) || !isComplete(progress, mission)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Support cache locked. Complete the route record first."), true);
            return true;
        }
        if (!progress.markTerminalMissionCacheClaimed(player, mission.path())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Support cache already claimed."), true);
            return true;
        }
        List<ItemStack> rewards = rewards(mission);
        if (!EchoCoreServices.storeTerminalRewards(player, mission.id().toString(), rewards)) {
            awardDirectly(player, rewards);
            player.sendSystemMessage(Component.literal("[ECHO-7] Support cache delivered to your route inventory."), true);
        }
        return true;
    }

    private static TerminalMissionDefinition definition(Player player, EchoTerminalProgress progress,
            LaunchReadiness launch, LaunchReadiness assembly, OrbitalMission mission) {
        return new TerminalMissionDefinition(
                mission.id(),
                OrbitalTerminalIds.CHAPTER_ID,
                mission.phaseId(),
                mission.phaseTitle(),
                mission.phaseOrder(),
                mission.order(),
                mission.title(),
                briefing(player, mission),
                guide(player, mission),
                mission.category(),
                mission.difficulty(),
                icon(mission),
                prerequisites(mission),
                requirements(player, progress, launch, assembly, mission),
                rewards(mission).stream().map(TerminalMissionReward::of).toList());
    }

    private static List<TerminalMissionAction> actions(Player player, EchoTerminalProgress progress,
            OrbitalMission mission, boolean available, boolean complete, boolean claimed) {
        if (!available) {
            return List.of(TerminalMissionAction.disabled(ACTION_SCAN, "SCAN", lockedReason(player, progress, mission)));
        }
        if (complete) {
            return List.of(claimed
                    ? TerminalMissionAction.disabled(ACTION_CLAIM_CACHE, "CLAIM CACHE", "Support cache already claimed.")
                    : TerminalMissionAction.enabled(ACTION_CLAIM_CACHE, "CLAIM CACHE"));
        }
        if (!OrbitalTerminalActions.canScan(player)) {
            return List.of(TerminalMissionAction.disabled(ACTION_SCAN, "SCAN ROUTE",
                    OrbitalTerminalActions.MISSING_TERMINAL_REASON));
        }
        return List.of(TerminalMissionAction.enabled(ACTION_SCAN, "SCAN ROUTE"));
    }

    private static List<TerminalMissionRequirement> requirements(Player player, EchoTerminalProgress progress,
            LaunchReadiness launch, LaunchReadiness assembly, OrbitalMission mission) {
        EmergencyRocketStatus rocket = EmergencyRocketStatus.near(player);
        return switch (mission) {
            case EARTH_CALIBRATION -> List.of(requirement(
                    "Earth calibration",
                    progress.launchSiteTracked()
                            ? "Orbital contact calibrated from ruined Earth."
                            : earthCalibrationInstruction(player),
                    ModItems.ECHO_TERMINAL.get(), progress.launchSiteTracked() ? 1 : 0, 1));
            case LAUNCH_CHAIN -> List.of(
                    requirement("Launch systems", missingOrReady(launch, "Launch systems ready."),
                            ModBlocks.LAUNCH_PLATFORM.get(), launch.ready() ? 1 : 0, 1),
                    requirement("Rocket assembly",
                            assembly.ready() || rocket.staged() || progress.launchPrepared()
                                    ? rocket.detail(progress.lowOrbitReached(), launch, assembly)
                                    : missingOrReady(assembly, "Rocket assembly ready."),
                            ModItems.EMERGENCY_ROCKET.get(), assembly.ready() || rocket.staged() || progress.launchPrepared() ? 1 : 0, 1));
            case LOW_ORBIT -> List.of(requirement(
                    "Low Earth Orbit",
                    progress.lowOrbitReached() ? "Emergency Rocket vector confirmed." : rocket.detail(false, launch, assembly),
                    ModItems.EMERGENCY_ROCKET.get(), progress.lowOrbitReached() ? 1 : 0, 1));
            case STATION_NETWORK -> List.of(requirement(
                    "Station relay network",
                    progress.stationNetworkGateOpen() ? "Station ECHO relay gate open." : "Repair unique Station Relay Nodes.",
                    ModItems.STATION_RELAY_FUSE.get(), progress.stationNetworkGateOpen() ? 3 : progress.stationRelayRepairs(), 3));
            case LUNAR_SIGNAL -> List.of(requirement(
                    "Lunar signal",
                    progress.lunarSignalInvestigated() ? "Lunar scar telemetry logged." : "Use the Orbital Shuttle from staging.",
                    ModItems.ORBITAL_SHUTTLE.get(), progress.lunarSignalInvestigated() ? 1 : 0, 1));
            case MARS_ROUTE -> List.of(requirement(
                    "Mars transfer",
                    progress.marsRouteUnlocked() ? "Mars transfer route unlocked." : "Resolve Helium-3 telemetry from the Moon.",
                    ModItems.MARS_TRANSFER_WINDOW.get(), progress.marsRouteUnlocked() ? 1 : 0, 1));
            case EUROPA_ROUTE -> List.of(requirement(
                    "Europa transfer",
                    progress.europaRouteUnlocked() ? "Europa cryo route unlocked." : "Resolve Martian Silica pressure telemetry.",
                    ModItems.EUROPA_TRANSFER_WINDOW.get(), progress.europaRouteUnlocked() ? 1 : 0, 1));
            case SATURN_ROUTE -> List.of(requirement(
                    "Saturn transfer",
                    progress.saturnRouteUnlocked() ? "Saturn ring route unlocked." : "Resolve Europa thermal telemetry.",
                    ModItems.SATURN_TRANSFER_WINDOW.get(), progress.saturnRouteUnlocked() ? 1 : 0, 1));
            case TITAN_ROUTE -> List.of(requirement(
                    "Titan transfer",
                    progress.titanRouteUnlocked() ? "Titan methane route unlocked." : "Restore Saturn Ring Relays and recover ring fragments.",
                    ModItems.TITAN_TRANSFER_WINDOW.get(), progress.titanRouteUnlocked() ? 1 : 0, 1));
            case DEEP_SPACE_PROTOCOL -> List.of(requirement(
                    "Deep Space Protocol",
                    progress.deepSpaceProtocolUnlocked() ? "Deep Space Protocol unlocked." : "Resolve Titan methane telemetry or Nexus drive evidence.",
                    ModItems.NEXUS_DRIVE_CORE.get(), progress.deepSpaceProtocolUnlocked() ? 1 : 0, 1));
            case ECHO_ZERO -> List.of(requirement(
                    "ECHO-0",
                    progress.echoZeroEncountered() ? "ECHO-0 quarantine authority resolved." : "Confront ECHO-0 in the Nexus Anomaly Belt.",
                    ModItems.NEXUS_DRIVE_VESSEL.get(), progress.echoZeroEncountered() ? 1 : 0, 1));
            case SURVEY_NETWORK -> List.of(requirement(
                    "Route survey network",
                    progress.allSurveysComplete() ? "Every route survey is mapped and stable." : progress.surveyStatus(),
                    ModItems.ORBIT_SURVEY_DATA.get(), progress.totalSurveyCount(), 21));
            case FACTION_CONTRACT -> List.of(requirement(
                    "Faction outpost charters",
                    progress.allOutpostChartersComplete() ? "Crashbreak, Radwarden, and Sporebound Tier I outpost support secured." : progress.factionContractRequirement(),
                    ModItems.ORBITAL_REMNANT_BADGE.get(), progress.completedOutpostCharterCount(), 3));
            case FINAL_SEAL -> List.of(requirement(
                    "Final network seal",
                    progress.finalNetworkSealed() ? "Orbital Remnants arc complete. Earth no longer answers to quarantine." : progress.scanRequirement(),
                    ModItems.STABILIZED_ECHO_CORE.get(), progress.finalNetworkSealed() ? 1 : 0, 1));
        };
    }

    private static TerminalMissionRequirement requirement(String label, String detail, ItemLike icon, int have, int need) {
        int safeNeed = Math.max(1, need);
        int safeHave = Math.max(0, Math.min(safeNeed, have));
        return TerminalMissionRequirement.custom(label, detail, new ItemStack(icon), safeHave, safeNeed, safeHave >= safeNeed);
    }

    private static String briefing(Player player, OrbitalMission mission) {
        if (mission != OrbitalMission.EARTH_CALIBRATION) {
            return mission.briefing();
        }
        return standaloneOrbital(player)
                ? "Recover an orbital handoff file and reopen ECHO-7 contact from ruined Earth."
                : mission.briefing();
    }

    private static String guide(Player player, OrbitalMission mission) {
        if (mission != OrbitalMission.EARTH_CALIBRATION) {
            return mission.guide();
        }
        return standaloneOrbital(player)
                ? "Sneak-use the ECHO-7 Terminal on Earth. The standalone pack treats calibration as a recovered handoff file: practical salvage first, missing sky records later."
                : mission.guide();
    }

    private static String earthCalibrationInstruction(Player player) {
        return standaloneOrbital(player)
                ? "Sneak-use ECHO-7 on Earth to recover the orbital handoff file."
                : "Sneak-use ECHO-7 on Earth after Ashfall recovery to calibrate the Orbital route.";
    }

    private static boolean standaloneOrbital(Player player) {
        return EchoCoreServices.packMode(player) == com.echoplatform.echocore.api.EchoPackMode.ORBITAL_STANDALONE;
    }

    private static boolean isAvailable(Player player, EchoTerminalProgress progress, OrbitalMission mission) {
        if (mission == OrbitalMission.EARTH_CALIBRATION) {
            return !AshfallCompat.isOrbitalCalibrationLocked(player);
        }
        OrbitalMission previous = mission.previous();
        return previous != null && isAvailable(player, progress, previous) && isComplete(progress, previous);
    }

    private static boolean isComplete(EchoTerminalProgress progress, OrbitalMission mission) {
        return switch (mission) {
            case EARTH_CALIBRATION -> progress.launchSiteTracked();
            case LAUNCH_CHAIN -> progress.launchPrepared() || progress.lowOrbitReached();
            case LOW_ORBIT -> progress.lowOrbitReached();
            case STATION_NETWORK -> progress.stationNetworkGateOpen();
            case LUNAR_SIGNAL -> progress.lunarSignalInvestigated();
            case MARS_ROUTE -> progress.marsRouteUnlocked();
            case EUROPA_ROUTE -> progress.europaRouteUnlocked();
            case SATURN_ROUTE -> progress.saturnRouteUnlocked();
            case TITAN_ROUTE -> progress.titanRouteUnlocked();
            case DEEP_SPACE_PROTOCOL -> progress.deepSpaceProtocolUnlocked();
            case ECHO_ZERO -> progress.echoZeroEncountered();
            case SURVEY_NETWORK -> progress.allSurveysComplete();
            case FACTION_CONTRACT -> progress.allOutpostChartersComplete();
            case FINAL_SEAL -> progress.finalNetworkSealed();
            default -> false;
        };
    }

    private static float progress(Player player, EchoTerminalProgress progress, OrbitalMission mission) {
        if (!isAvailable(player, progress, mission)) {
            return 0.0F;
        }
        if (isComplete(progress, mission)) {
            return 1.0F;
        }
        return switch (mission) {
            case EARTH_CALIBRATION, LOW_ORBIT, LUNAR_SIGNAL, MARS_ROUTE, EUROPA_ROUTE, SATURN_ROUTE, TITAN_ROUTE, DEEP_SPACE_PROTOCOL,
                    ECHO_ZERO, FINAL_SEAL -> 0.0F;
            case FACTION_CONTRACT -> progress.completedOutpostCharterCount() / 3.0F;
            case LAUNCH_CHAIN -> {
                LaunchReadiness launch = LaunchReadiness.evaluateForLaunch(player);
                LaunchReadiness assembly = LaunchReadiness.evaluateForAssembly(player);
                yield ((launch.ready() ? 1.0F : 0.0F) + (assembly.ready() ? 1.0F : 0.0F)) / 2.0F;
            }
            case STATION_NETWORK -> progress.stationRelayRepairs() / 3.0F;
            case SURVEY_NETWORK -> progress.totalSurveyCount() / 21.0F;
            default -> 0.0F;
        };
    }

    private static String lockedReason(Player player, EchoTerminalProgress progress, OrbitalMission mission) {
        if (mission == OrbitalMission.EARTH_CALIBRATION && AshfallCompat.isOrbitalCalibrationLocked(player)) {
            return "Resolve an ECHO: Ashfall Protocol Nexus path before ECHO-0 allows orbital calibration.";
        }
        OrbitalMission previous = mission.previous();
        if (previous == null) {
            return "Orbital route record sealed by ECHO-0 quarantine state.";
        }
        return "Complete " + previous.title() + " first, then reopen this route record.";
    }

    private static String actionHint(Player player, EchoTerminalProgress progress, OrbitalMission mission,
            boolean available, boolean complete, boolean claimed) {
        if (!available) {
            return lockedReason(player, progress, mission);
        }
        if (complete) {
            return claimed
                    ? "Continue from the next active orbital record."
                    : "Claim the optional support cache before the next vacuum push.";
        }
        if (mission == OrbitalMission.SURVEY_NETWORK) {
            return "Use the Survey tab for route counts. " + progress.missionHelpReport();
        }
        if (mission == OrbitalMission.LAUNCH_CHAIN || mission == OrbitalMission.LOW_ORBIT) {
            LaunchReadiness launch = LaunchReadiness.evaluateForLaunch(player);
            LaunchReadiness assembly = LaunchReadiness.evaluateForAssembly(player);
            return EmergencyRocketStatus.near(player).detail(progress.lowOrbitReached(), launch, assembly);
        }
        if (mission == OrbitalMission.FACTION_CONTRACT) {
            return progress.factionContractRequirement();
        }
        return progress.scanRequirement();
    }

    private static List<String> prerequisites(OrbitalMission mission) {
        OrbitalMission previous = mission.previous();
        return previous == null ? List.of() : List.of(previous.title());
    }

    private static String missingOrReady(LaunchReadiness readiness, String readyText) {
        if (readiness.ready()) {
            return readyText;
        }
        List<String> missing = readiness.missing().stream()
                .limit(3)
                .map(component -> component.getString().replaceFirst("^- ", ""))
                .toList();
        if (missing.isEmpty()) {
            return "Checklist incomplete.";
        }
        String detail = "Missing: " + String.join(", ", missing);
        if (readiness.missing().size() > missing.size()) {
            detail += ", +" + (readiness.missing().size() - missing.size()) + " more";
        }
        return detail;
    }

    private static String statusLabel(TerminalMissionStatus status) {
        return switch (status) {
            case LOCKED -> "LOCKED";
            case UNLOCKED -> "ACTIVE";
            case COMPLETED -> "COMPLETE";
            case CLAIMABLE -> "CACHE READY";
            case CLAIMED -> "CLAIMED";
            case VIEW_ONLY -> "VIEW";
        };
    }

    private static List<ItemStack> rewards(OrbitalMission mission) {
        return switch (mission) {
            case EARTH_CALIBRATION -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 1),
                    stack(ModItems.VACUUM_CIRCUIT.get(), 1));
            case LAUNCH_CHAIN -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 3),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                    stack(ModItems.HEAT_SHIELD_PLATE.get(), 1));
            case LOW_ORBIT -> stacks(
                    stack(ModItems.OXYGEN_CANISTER.get(), 1),
                    stack(ModItems.VACUUM_CIRCUIT.get(), 2));
            case STATION_NETWORK -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 4),
                    stack(ModItems.ORBITAL_ALLOY.get(), 1));
            case LUNAR_SIGNAL -> stacks(
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 3),
                    stack(ModItems.HEAT_SHIELD_PLATE.get(), 2));
            case MARS_ROUTE -> stacks(
                    stack(ModItems.OXYGEN_CANISTER.get(), 2),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 2));
            case EUROPA_ROUTE -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 4),
                    stack(ModItems.FROZEN_WIRING.get(), 2));
            case SATURN_ROUTE -> stacks(
                    stack(ModItems.SATURN_RELAY_LENS.get(), 1),
                    stack(ModItems.OXYGEN_CANISTER.get(), 2));
            case TITAN_ROUTE -> stacks(
                    stack(ModItems.TITAN_METHANE_CELL.get(), 1),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 3));
            case DEEP_SPACE_PROTOCOL -> stacks(
                    stack(ModItems.OXYGEN_CANISTER.get(), 2),
                    stack(ModItems.ORBITAL_ALLOY.get(), 2));
            case ECHO_ZERO -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 6),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 4),
                    stack(ModItems.HEAT_SHIELD_PLATE.get(), 2));
            case SURVEY_NETWORK -> stacks(
                    stack(ModItems.OXYGEN_CANISTER.get(), 3),
                    stack(ModItems.VACUUM_CIRCUIT.get(), 3));
            case FACTION_CONTRACT -> stacks(
                    stack(ModItems.ORBITAL_ALLOY.get(), 2),
                    stack(ModItems.HEAT_SHIELD_PLATE.get(), 2));
            case FINAL_SEAL -> stacks(
                    stack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 8),
                    stack(ModItems.SUIT_SEALANT_PATCH.get(), 6),
                    stack(ModItems.ORBITAL_ALLOY.get(), 2));
        };
    }

    private static List<ItemStack> stacks(ItemStack... stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return List.copyOf(result);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static void awardDirectly(ServerPlayer player, List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            ItemStack copy = reward.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    private static ItemStack icon(OrbitalMission mission) {
        return switch (mission) {
            case EARTH_CALIBRATION -> stack(ModItems.ECHO_TERMINAL.get(), 1);
            case LAUNCH_CHAIN -> stack(ModItems.EMERGENCY_ROCKET.get(), 1);
            case LOW_ORBIT -> stack(ModBlocks.STATION_LIFE_SUPPORT_CORE.get(), 1);
            case STATION_NETWORK -> stack(ModItems.STATION_RELAY_FUSE.get(), 1);
            case LUNAR_SIGNAL -> stack(ModItems.ORBITAL_SHUTTLE.get(), 1);
            case MARS_ROUTE -> stack(ModItems.MARS_TRANSFER_WINDOW.get(), 1);
            case EUROPA_ROUTE -> stack(ModItems.EUROPA_TRANSFER_WINDOW.get(), 1);
            case SATURN_ROUTE -> stack(ModItems.SATURN_TRANSFER_WINDOW.get(), 1);
            case TITAN_ROUTE -> stack(ModItems.TITAN_TRANSFER_WINDOW.get(), 1);
            case DEEP_SPACE_PROTOCOL -> stack(ModItems.NEXUS_DRIVE_CORE.get(), 1);
            case ECHO_ZERO -> stack(ModItems.NEXUS_DRIVE_VESSEL.get(), 1);
            case SURVEY_NETWORK -> stack(ModItems.ORBIT_SURVEY_DATA.get(), 1);
            case FACTION_CONTRACT -> stack(ModItems.ORBITAL_REMNANT_BADGE.get(), 1);
            case FINAL_SEAL -> stack(ModItems.STABILIZED_ECHO_CORE.get(), 1);
        };
    }

    private static OrbitalMission mission(Identifier id) {
        if (id == null) {
            return null;
        }
        for (OrbitalMission mission : OrbitalMission.values()) {
            if (mission.id().equals(id)) {
                return mission;
            }
        }
        return null;
    }

    private enum OrbitalMission {
        EARTH_CALIBRATION("earth_calibration", "EARTH RECONTACT", 0, 0,
                "Earth Calibration",
                "Reopen ECHO-7 orbital contact from ruined Earth after the Nexus decision gives the sky something to answer.",
                "Sneak-use the ECHO-7 Terminal on Earth. When Ashfall is installed, complete any Nexus path first so the quarantine handoff has a real field trigger.",
                "Calibration", "Guide"),
        LAUNCH_CHAIN("launch_chain", "EARTH RECONTACT", 0, 1,
                "Launch Chain",
                "Build the launch pad, assembly frame, fuel, oxygen, suit, and rocket parts without trusting orbit to be kind.",
                "Use Earth recovery sites and machine recipes to complete launch readiness, then stage the Emergency Rocket vehicle on the 5x5 pad.",
                "Crafting", "Route"),
        LOW_ORBIT("low_orbit", "ORBITAL CALIBRATION", 1, 0,
                "Low Earth Orbit",
                "Board the staged Emergency Rocket and establish the first orbital vector through debris and quarantine static.",
                "Launch from the pad after countdown, recover the return vector, and scan for Station ECHO systems before oxygen becomes the whole plan.",
                "Route", "Hazard"),
        STATION_NETWORK("station_network", "ORBITAL CALIBRATION", 1, 1,
                "Station Network",
                "Restore station life support and repair the Low Orbit relay network before the route goes blind again.",
                "Scan Station Relay Nodes with Station Relay Fuses. Some recovered saves may already have a bypassed relay gate.",
                "Repair", "Route"),
        LUNAR_SIGNAL("lunar_signal", "Moon", 2, 0,
                "Lunar Signal",
                "Follow Station ECHO debris toward the Lunar Scar Zone and the first clear quarantine wound.",
                "Use the Orbital Shuttle from orbital staging, then stabilize lunar telemetry.",
                "Route", "Hazard"),
        MARS_ROUTE("mars_route", "Mars", 3, 0,
                "Mars Route",
                "Resolve Helium-3 telemetry into a Mars transfer route through dust, pressure loss, and old colony silence.",
                "Repair lunar extractors when required, then scan with Helium-3 support.",
                "Route", "Hazard"),
        EUROPA_ROUTE("europa_route", "Europa", 4, 0,
                "Europa Route",
                "Resolve Martian pressure telemetry into the Europa cryo route, where cold treats suit seals as suggestions.",
                "Repair Mars pressure consoles when required, then scan with Martian Silica support.",
                "Route", "Hazard"),
        SATURN_ROUTE("saturn_route", "Saturn", 5, 0,
                "Saturn Route",
                "Follow Europa thermal telemetry into a graveyard of ring relays and broken faction hubs.",
                "Calibrate Europa thermal arrays when required, then scan with Cryo Crystal support and use the Saturn Transfer Window.",
                "Route", "Hazard"),
        TITAN_ROUTE("titan_route", "Titan", 6, 0,
                "Titan Route",
                "Use Saturn relay fragments to descend onto the methane shelf before Deep Space Protocol can hold.",
                "Restore Saturn Ring Relays when required, then scan with ring fragments and use the Titan Transfer Window.",
                "Route", "Hazard"),
        DEEP_SPACE_PROTOCOL("deep_space_protocol", "Nexus Anomaly", 7, 0,
                "Deep Space Protocol",
                "Use Titan or Nexus-drive telemetry to reveal the anomaly belt and the authority hiding beyond it.",
                "Pressurize Titan Methane Pumps when required, then scan with Titan Survey Core or Nexus Drive support. The route opens only after the evidence holds.",
                "Route", "Endgame"),
        ECHO_ZERO("echo_zero", "Nexus Anomaly", 7, 1,
                "ECHO-0",
                "Confront the quarantine intelligence that believes Earth must stay silent to starve the Nexus.",
                "Enter the Nexus Anomaly Belt, survive oxygen, pressure, and radiation load, then resolve ECHO-0.",
                "Story", "Endgame"),
        SURVEY_NETWORK("survey_network", "ROUTE SURVEY", 8, 0,
                "Survey Network",
                "Map the route worlds and stabilize the post-ECHO-0 Nexus anchors before the old quarantine reinterprets silence.",
                "Use the Survey tab to finish each route's three unique logs. Evidence is the countermeasure now.",
                "Survey", "Endgame"),
        FACTION_CONTRACT("faction_contract", "ROUTE SURVEY", 8, 1,
                "Faction Outposts",
                "Secure Tier I charters from Crashbreak, Radwarden, and Sporebound outposts after the survey network is stable enough for people to argue over it.",
                "Visit the Saturn, Titan, and Nexus NPC outposts, then complete each faction's Tier I charter proof.",
                "Faction", "Endgame"),
        FINAL_SEAL("final_seal", "FINAL SEAL", 9, 0,
                "Final Network Seal",
                "Close the Orbital Remnants arc after ECHO-0, surveys, and Radwarden, Crashbreak, and Sporebound support prove the route belongs to the living.",
                "Press SCAN once all final prerequisites are complete.",
                "Story", "Complete");

        private final String path;
        private final String phaseTitle;
        private final int phaseOrder;
        private final int order;
        private final String title;
        private final String briefing;
        private final String guide;
        private final String category;
        private final String difficulty;

        OrbitalMission(String path, String phaseTitle, int phaseOrder, int order, String title,
                String briefing, String guide, String category, String difficulty) {
            this.path = path;
            this.phaseTitle = phaseTitle;
            this.phaseOrder = phaseOrder;
            this.order = order;
            this.title = title;
            this.briefing = briefing;
            this.guide = guide;
            this.category = category;
            this.difficulty = difficulty;
        }

        Identifier id() {
            return OrbitalTerminalIds.id(path);
        }

        String path() {
            return path;
        }

        String phaseId() {
            return phaseTitle.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        }

        String phaseTitle() {
            return phaseTitle;
        }

        int phaseOrder() {
            return phaseOrder;
        }

        int order() {
            return order;
        }

        String title() {
            return title;
        }

        String briefing() {
            return briefing;
        }

        String guide() {
            return guide;
        }

        String category() {
            return category;
        }

        String difficulty() {
            return difficulty;
        }

        OrbitalMission previous() {
            int index = ordinal() - 1;
            return index < 0 ? null : values()[index];
        }
    }
}

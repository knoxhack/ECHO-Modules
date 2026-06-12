package com.knoxhack.echostationfall.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationLore;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.progression.StationfallRouteTracker;
import com.knoxhack.echostationfall.registry.ModItems;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallRouteService;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class StationfallTerminalCommonIntegration {
    public static final Identifier CHAPTER_ID = id("stationfall");
    public static final Identifier STATION_MOTHER_ARCHIVE_ID = id("station_mother_record");
    public static final Identifier BLACKBOX_ARCHIVE_ID = id("stationfall_blackbox_record");
    public static final String ACTION_BOARD = "board_station";
    public static final String ACTION_RETURN = "return_station";
    public static final String ACTION_TRACK = "track_section";
    public static final String ACTION_CLAIM = "claim_cache";

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private StationfallTerminalCommonIntegration() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path);
    }

    public static Identifier archiveId(StationSection section) {
        return id("stationfall_" + section.key() + "_crew_log");
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
        if (missionCoreLoaded) {
            StationfallMissionCoreIntegration.register();
        } else {
            TerminalMissionRegistry.register(Provider.INSTANCE);
        }
        TerminalMissionActions.registerForTab(CHAPTER_ID);
        archives();
    }

    private static void archives() {
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("stationfall_route_manual"),
                "Stationfall",
                "Stationfall Route Manual",
                "OPEN",
                List.of(
                        "BOARD STATION uses the same route service as Station Access Card.",
                        "TRACK SECTION reports section, life support, route objective, boss, and blackbox state.",
                        "Power, pressure, logs, Signal Panic, and boss state are mirrored through ECHO Core."
                ),
                false
        ));
        for (StationSection section : StationSection.values()) {
            TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                    archiveId(section),
                    "Stationfall Crew Logs",
                    section.displayName() + " Crew Log",
                    "LOCKED",
                    StationLore.crewLog(section),
                    true
            ));
        }
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                STATION_MOTHER_ARCHIVE_ID,
                "Stationfall",
                "Station Mother Record",
                "LOCKED",
                StationLore.stationMotherRecord(),
                true
        ));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                BLACKBOX_ARCHIVE_ID,
                "Stationfall",
                "Stationfall Blackbox",
                "LOCKED",
                StationLore.blackboxRecord(),
                true
        ));
    }

    public static final class Provider implements TerminalMissionProvider {
        public static final Provider INSTANCE = new Provider();

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(
                    CHAPTER_ID,
                    "ECHO: STATIONFALL",
                    "Station route, live telemetry, crew logs, boss state, and blackbox handoff.",
                    330,
                    0xFFFF536A,
                    true
            );
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return Arrays.stream(Mission.values()).map(mission -> definition(player, mission)).toList();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier id) {
            Mission mission = Mission.by(id);
            if (mission == null) {
                return snap(id, TerminalMissionStatus.LOCKED, 0.0F, "UNKNOWN", "No Stationfall record.", "No clean route.", List.of());
            }
            StationfallProgress progress = StationfallProgress.get(player);
            boolean available = available(player, progress, mission);
            boolean complete = complete(progress, mission);
            boolean claimed = progress.terminalRewardClaimed(mission.path);
            TerminalMissionStatus status = !available
                    ? TerminalMissionStatus.LOCKED
                    : complete ? (claimed ? TerminalMissionStatus.CLAIMED : TerminalMissionStatus.CLAIMABLE) : TerminalMissionStatus.UNLOCKED;
            return snap(
                    mission.id,
                    status,
                    progress(player, progress, mission),
                    label(status),
                    available ? "" : locked(mission),
                    hint(player, status),
                    actions(player, mission, available, complete, claimed)
            );
        }

        @Override
        public TerminalMissionPresentation presentation(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot
        ) {
            Mission mission = Mission.by(definition.id());
            return new TerminalMissionPresentation(
                    definition.title(),
                    definition.briefing(),
                    snapshot.actionHint(),
                    mission == null ? definition.phaseTitle() : mission.phaseTitle,
                    statusTone(snapshot.status()),
                    List.of(definition.category(), definition.difficulty(), snapshot.statusLabel()),
                    "echostationfall:" + definition.id().getPath()
            );
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
            Mission mission = Mission.by(definition == null ? null : definition.id());
            if (mission == null) {
                return Optional.empty();
            }
            int phase = switch (mission) {
                case BOARD -> 10;
                case POWER, LOGS, STABILIZE -> 11;
                case OVERRIDE, MOTHER -> 13;
                case BLACKBOX -> 14;
            };
            return Optional.of(TerminalMissionRoutePlacement.main(
                    phase, mission.phaseOrder * 100 + mission.order));
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String action) {
            Mission mission = Mission.by(missionId);
            if (mission == null) {
                return false;
            }
            if (ACTION_BOARD.equals(action)) {
                return StationfallRouteService.board(player, "terminal");
            }
            if (ACTION_RETURN.equals(action)) {
                return StationfallRouteService.returnFromStation(player);
            }
            if (ACTION_TRACK.equals(action)) {
                player.sendSystemMessage(Component.literal("ECHO-7 // " + StationfallRouteTracker.status(player)));
                return true;
            }
            if (!ACTION_CLAIM.equals(action)) {
                return false;
            }

            StationfallProgress progress = StationfallProgress.get(player);
            if (!available(player, progress, mission) || !complete(progress, mission)) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Stationfall cache locked."));
                return true;
            }
            if (!progress.markTerminalRewardClaimed(player, mission.path)) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Stationfall cache already claimed."));
                return true;
            }
            List<ItemStack> rewards = rewards(mission);
            if (!EchoCoreServices.storeTerminalRewards(player, mission.id.toString(), rewards)) {
                award(player, rewards);
            }
            return true;
        }
    }

    private static TerminalMissionDefinition definition(Player player, Mission mission) {
        StationfallProgress progress = StationfallProgress.get(player);
        return new TerminalMissionDefinition(
                mission.id,
                CHAPTER_ID,
                mission.phaseId,
                mission.phaseTitle,
                mission.phaseOrder,
                mission.order,
                mission.title,
                mission.brief,
                mission.guide,
                mission.category,
                mission.difficulty,
                icon(mission),
                mission.previous() == null ? List.of() : List.of(mission.previous().title),
                requirements(player, progress, mission),
                rewards(mission).stream().map(TerminalMissionReward::of).toList()
        );
    }

    private static TerminalMissionSnapshot snap(
            Identifier id,
            TerminalMissionStatus status,
            float progress,
            String label,
            String lockReason,
            String hint,
            List<TerminalMissionAction> actions
    ) {
        return new TerminalMissionSnapshot(id, status, progress, label, lockReason, hint, actions);
    }

    private static boolean available(Player player, StationfallProgress progress, Mission mission) {
        return mission == Mission.BOARD
                || mission.previous() != null
                && available(player, progress, mission.previous())
                && complete(progress, mission.previous());
    }

    private static boolean complete(StationfallProgress progress, Mission mission) {
        return switch (mission) {
            case BOARD -> progress.boarded();
            case POWER -> progress.poweredSectionCount() >= StationSection.values().length;
            case LOGS -> progress.decodedLogCount() >= StationSection.values().length;
            case STABILIZE -> progress.allObjectivesComplete();
            case OVERRIDE -> progress.aiOverrideObtained();
            case MOTHER -> progress.bossDefeated();
            case BLACKBOX -> progress.blackboxRetrieved();
        };
    }

    private static float progress(Player player, StationfallProgress progress, Mission mission) {
        if (!available(player, progress, mission)) {
            return 0.0F;
        }
        if (complete(progress, mission)) {
            return 1.0F;
        }
        return switch (mission) {
            case POWER -> progress.poweredSectionCount() / (float) StationSection.values().length;
            case LOGS -> progress.decodedLogCount() / (float) StationSection.values().length;
            case STABILIZE -> progress.objectiveCount() / (float) StationfallObjective.values().length;
            default -> 0.0F;
        };
    }

    private static String label(TerminalMissionStatus status) {
        return switch (status) {
            case LOCKED -> "LOCKED";
            case UNLOCKED -> "ACTIVE";
            case CLAIMABLE -> "CACHE READY";
            case CLAIMED -> "CLAIMED";
            case COMPLETED -> "COMPLETE";
            case VIEW_ONLY -> "VIEW";
        };
    }

    private static String statusTone(TerminalMissionStatus status) {
        return switch (status) {
            case CLAIMABLE, CLAIMED, COMPLETED -> "success";
            case UNLOCKED -> "active";
            case LOCKED, VIEW_ONLY -> "muted";
        };
    }

    private static String locked(Mission mission) {
        return mission == Mission.BOARD
                ? "Restore Orbital station coordinates or Station Network first."
                : "Complete " + mission.previous().title + " first.";
    }

    private static String hint(Player player, TerminalMissionStatus status) {
        return status == TerminalMissionStatus.CLAIMABLE
                ? "Claim the optional support cache."
                : StationfallRouteTracker.status(player);
    }

    private static List<TerminalMissionAction> actions(
            Player player,
            Mission mission,
            boolean available,
            boolean complete,
            boolean claimed
    ) {
        List<TerminalMissionAction> actions = new ArrayList<>();
        if (!available) {
            actions.add(TerminalMissionAction.disabled(ACTION_TRACK, "TRACK SECTION", locked(mission)));
            return actions;
        }
        if (!StationfallDimensions.isStation(player.level())) {
            actions.add(TerminalMissionAction.enabled(ACTION_BOARD, "BOARD STATION"));
        } else {
            actions.add(TerminalMissionAction.enabled(ACTION_RETURN, "RETURN"));
        }
        actions.add(TerminalMissionAction.enabled(ACTION_TRACK, "TRACK SECTION"));
        if (complete) {
            actions.add(claimed
                    ? TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Support cache already claimed.")
                    : TerminalMissionAction.enabled(ACTION_CLAIM, "CLAIM CACHE"));
        }
        return List.copyOf(actions);
    }

    private static List<TerminalMissionRequirement> requirements(Player player, StationfallProgress progress, Mission mission) {
        return switch (mission) {
            case BOARD -> List.of(req("Station route", progress.canBoard(player)
                    ? "Stationfall route available."
                    : "Need Stationfall coordinates/network.", ModItems.STATION_ACCESS_CARD.get(), progress.canBoard(player) ? 1 : 0, 1));
            case POWER -> List.of(req("Section power", progress.poweredSectionCount() + "/9 sections stable.",
                    ModItems.STATION_BATTERY.get(), progress.poweredSectionCount(), 9));
            case LOGS -> List.of(req("Crew logs", progress.decodedLogCount() + "/9 decoded.",
                    ModItems.CREW_LOG_TABLET.get(), progress.decodedLogCount(), 9));
            case STABILIZE -> stabilizeRequirements(player, progress);
            case OVERRIDE -> List.of(req("AI Override", progress.aiOverrideObtained()
                    ? "Override chip synchronized."
                    : "Use Data Core Terminal.", ModItems.AI_OVERRIDE_CHIP.get(), progress.aiOverrideObtained() ? 1 : 0, 1));
            case MOTHER -> List.of(req("Station Mother", progress.bossDefeated()
                    ? "Boss defeated."
                    : "Use Command Console with override.", ModItems.AI_OVERRIDE_CORE.get(), progress.bossDefeated() ? 1 : 0, 1));
            case BLACKBOX -> List.of(req("Blackbox", progress.blackboxRetrieved()
                    ? "Blackbox recovered."
                    : "Defeat Station Mother.", ModItems.STATIONFALL_BLACKBOX.get(), progress.blackboxRetrieved() ? 1 : 0, 1));
        };
    }

    private static List<TerminalMissionRequirement> stabilizeRequirements(Player player, StationfallProgress progress) {
        List<TerminalMissionRequirement> requirements = new ArrayList<>();
        requirements.add(req("Section objectives",
                progress.objectiveCount() + "/" + StationfallObjective.values().length + " stabilized.",
                ModItems.PRESSURE_SEAL_KIT.get(), progress.objectiveCount(), StationfallObjective.values().length));
        StationfallSuitState suit = player == null ? null : StationfallSuitState.get(player);
        SignalPanicState panic = player == null ? null : SignalPanicState.get(player);
        int pressure = suit == null ? 0 : suit.pressure();
        int oxygen = suit == null ? 0 : suit.oxygen();
        int panicSafety = panic == null ? 0 : Math.max(0, 100 - panic.value());
        requirements.add(req("Pressure seal", pressure + "% suit pressure restored.",
                ModItems.PRESSURE_SEAL_KIT.get(), pressure, 70));
        requirements.add(req("Oxygen reserve", oxygen + "% suit oxygen restored.",
                ModItems.EMERGENCY_OXYGEN_PACK.get(), oxygen, 70));
        requirements.add(req("Signal Panic dampened", (panic == null ? 0 : panic.value()) + "% panic exposure.",
                ModItems.SIGNAL_PANIC_DAMPENER.get(), panicSafety, 70));
        for (StationfallObjective objective : StationfallObjective.values()) {
            requirements.add(req(objective.title(),
                    objective.section().displayName() + ": " + objectiveStepLine(progress, objective),
                    ModItems.CREW_LOG_TABLET.get(),
                    progress.objectiveStepCount(objective),
                    objective.targetSteps()));
        }
        return List.copyOf(requirements);
    }

    private static String objectiveStepLine(StationfallProgress progress, StationfallObjective objective) {
        return progress.objectiveStepCount(objective) + "/" + objective.targetSteps() + " - " + objective.hint();
    }

    private static TerminalMissionRequirement req(String label, String description, ItemLike icon, int have, int need) {
        int needed = Math.max(1, need);
        int held = Math.max(0, Math.min(needed, have));
        return TerminalMissionRequirement.custom(label, description, new ItemStack(icon), held, needed, held >= needed);
    }

    private static List<ItemStack> rewards(Mission mission) {
        return switch (mission) {
            case BOARD -> List.of(new ItemStack(ModItems.EMERGENCY_OXYGEN_PACK.get(), 2), new ItemStack(ModItems.PRESSURE_SEAL_KIT.get(), 2));
            case POWER -> List.of(new ItemStack(ModItems.STATION_BATTERY.get(), 2), new ItemStack(ModItems.SIGNAL_PANIC_DAMPENER.get()));
            case LOGS -> List.of(new ItemStack(ModItems.CREW_LOG_TABLET.get(), 3));
            case STABILIZE -> List.of(new ItemStack(ModItems.PRESSURE_SEAL_KIT.get(), 3), new ItemStack(ModItems.EMERGENCY_OXYGEN_PACK.get(), 2));
            case OVERRIDE -> List.of(new ItemStack(ModItems.PRESSURE_SEAL_KIT.get(), 4));
            case MOTHER -> List.of(new ItemStack(ModItems.EMERGENCY_OXYGEN_PACK.get(), 4));
            case BLACKBOX -> List.of(new ItemStack(ModItems.ORBITAL_MEMORY_FRAGMENT.get(), 2));
        };
    }

    private static ItemStack icon(Mission mission) {
        return switch (mission) {
            case BOARD -> new ItemStack(ModItems.STATION_ACCESS_CARD.get());
            case POWER -> new ItemStack(ModItems.STATION_BATTERY.get());
            case LOGS -> new ItemStack(ModItems.CREW_LOG_TABLET.get());
            case STABILIZE -> new ItemStack(ModItems.PRESSURE_SEAL_KIT.get());
            case OVERRIDE -> new ItemStack(ModItems.AI_OVERRIDE_CHIP.get());
            case MOTHER -> new ItemStack(ModItems.AI_OVERRIDE_CORE.get());
            case BLACKBOX -> new ItemStack(ModItems.STATIONFALL_BLACKBOX.get());
        };
    }

    private static void award(ServerPlayer player, List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            ItemStack copy = reward.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    private enum Mission {
        BOARD("board_station", "BOARDING", 0, 0, "Board Station",
                "Use the Orbital gate or Station Access Card to dock at the dead station.",
                "Reach orbital staging, then BOARD STATION or use the access card.", "Route", "Horror"),
        POWER("restore_power", "STATION LOOP", 1, 0, "Restore Power",
                "Bring all nine sections to stable or overloaded power.",
                "Use Station Batteries at power nodes. Sneak-use to overload.", "Power", "Hazard"),
        LOGS("decode_logs", "STATION LOOP", 1, 1, "Decode Crew Logs",
                "Decode each section log and unlock archive entries.",
                "Interact with Crew Log Terminals in every section.", "Lore", "Horror"),
        STABILIZE("stabilize_sections", "STATION LOOP", 1, 2, "Stabilize Sections",
                "Complete each major section objective before trusting the Command Module.",
                "Purge growth, recover manifest, restart Engineering, query pods, and align Observation.", "Objectives", "Hazard"),
        OVERRIDE("ai_override", "DATA CORE", 2, 0, "AI Override",
                "Recover an AI Override Chip from the Data Core.",
                "Use the Data Core Terminal before the final console.", "Progression", "Danger"),
        MOTHER("station_mother", "COMMAND", 3, 0, "The Station Mother",
                "Defeat the AI horror entity in Command Module.",
                "Use the Command Console with override and survive four phases.", "Boss", "Endgame"),
        BLACKBOX("blackbox", "EXTRACTION", 4, 0, "Stationfall Blackbox",
                "Recover the Blackbox and expose Blackbox Protocol handoff.",
                "Boss rewards are duplicate-safe and mirrored through ECHO Core.", "Story", "Endgame");

        final String path;
        final String phaseTitle;
        final String title;
        final String brief;
        final String guide;
        final String category;
        final String difficulty;
        final String phaseId;
        final int phaseOrder;
        final int order;
        final Identifier id;

        Mission(String path, String phaseTitle, int phaseOrder, int order, String title, String brief, String guide,
                String category, String difficulty) {
            this.path = path;
            this.phaseTitle = phaseTitle;
            this.phaseOrder = phaseOrder;
            this.order = order;
            this.title = title;
            this.brief = brief;
            this.guide = guide;
            this.category = category;
            this.difficulty = difficulty;
            this.phaseId = phaseTitle.toLowerCase(Locale.ROOT).replace(' ', '_');
            this.id = id(path);
        }

        Mission previous() {
            int index = ordinal() - 1;
            return index < 0 ? null : values()[index];
        }

        static Mission by(Identifier id) {
            for (Mission mission : values()) {
                if (mission.id.equals(id)) {
                    return mission;
                }
            }
            return null;
        }
    }
}

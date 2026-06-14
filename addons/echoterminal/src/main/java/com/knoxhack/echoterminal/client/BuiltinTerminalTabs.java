package com.knoxhack.echoterminal.client;

import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.DataKeyMetadata;
import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.DataServiceDiagnostics;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.echoplatform.echocore.api.EchoFactionDefinition;
import com.echoplatform.echocore.api.EchoFactionProfile;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoModuleInfo;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.IDataKey;
import com.echoplatform.echocore.api.IDataService;
import com.echoplatform.echocore.api.config.EchoConfigApplyResult;
import com.echoplatform.echocore.api.config.EchoConfigCategorySnapshot;
import com.echoplatform.echocore.api.config.EchoConfigEntrySnapshot;
import com.echoplatform.echocore.api.config.EchoConfigModuleSnapshot;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import com.echoplatform.echocore.api.config.EchoConfigValueKind;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.TerminalIcon;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.api.theme.TerminalIconKey;
import com.knoxhack.echoterminal.api.theme.TerminalTheme;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.client.discovery.DiscoveryGridTab;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import com.knoxhack.echoterminal.client.recipe.TerminalRecipeIndexTab;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.network.TerminalConfigActionPacket;
import com.knoxhack.echoterminal.network.TerminalConfigClientState;
import com.knoxhack.echoterminal.player.TerminalPlayerData;
import com.knoxhack.echoterminal.player.TerminalPlayerData.TrackedMission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class BuiltinTerminalTabs {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier MISSION_GRAPH = id("mission_graph");
    private static final Identifier ROUTE_RECORDS = id("route_records");
    private static final Identifier VITALS = id("vitals");
    private static final Identifier DATA_CORE = id("data_core");
    private static final Identifier REWARD_INBOX = BuiltinTerminalCommonIntegration.REWARD_INBOX;
    private static final Identifier ADDONS = id("addons");
    private static final Identifier ARCHIVES = id("archives");
    private static final Identifier SETTINGS = id("settings");
    private static final Identifier CLAIM_REWARDS = BuiltinTerminalCommonIntegration.CLAIM_REWARDS;
    private static final Map<String, TerminalAddonGuide> FALLBACK_GUIDES = Map.ofEntries(
            Map.entry("ashfall", TerminalAddonGuide.mainline(1, 10, "Start here",
                    "Begin with Ashfall to learn survival basics, repair the terminal, stabilize camp, and open the first route signals.",
                    List.of(
                            "Secure water, shelter, food, and filters before long trips.",
                            "Open Ashfall Command or Protocol Roadmap for the next mission.",
                            "Use Survival Route when you want the complete roadmap."))),
            Map.entry("ashfall_protocol", TerminalAddonGuide.mainline(1, 10, "Start here",
                    "Begin with Ashfall to learn survival basics, repair the terminal, stabilize camp, and open the first route signals.",
                    List.of(
                            "Secure water, shelter, food, and filters before long trips.",
                            "Open Ashfall Command or Protocol Roadmap for the next mission.",
                            "Use Survival Route when you want the complete roadmap."))),
            Map.entry("orbital_remnants", TerminalAddonGuide.mainline(2, 20, "After Ashfall",
                    "Start Orbital Remnants after Ashfall gives you enough supplies and route confidence to leave the ground network.",
                    List.of(
                            "Open Orbital Command and review launch readiness.",
                            "Scan the launch site and fill missing launch systems.",
                            "Use Route Survey to track route worlds and ECHO-0 records."))),
            Map.entry("stationfall", TerminalAddonGuide.mainline(3, 30, "After Orbital",
                    "Start Stationfall once Orbital route progress exposes the dead station and you can support suit hazards.",
                    List.of(
                            "Board only after station coordinates or network restoration are ready.",
                            "Restore station sections before pushing deep objectives.",
                            "Recover crew logs and the Stationfall Blackbox handoff."))),
            Map.entry("nexus", TerminalAddonGuide.mainline(4, 40, "Late story",
                    "Start Nexus Protocol after Stationfall has exposed the blackbox handoff and the world can support field instability.",
                    List.of(
                            "Open Nexus Protocol to review research chain readiness.",
                            "Watch Nexus Field before running risky charge systems.",
                            "Use Field Map to avoid collapsed or critical chunks."))),
            Map.entry("nexus_protocol", TerminalAddonGuide.mainline(4, 40, "Late story",
                    "Start Nexus Protocol after Stationfall has exposed the blackbox handoff and the world can support field instability.",
                    List.of(
                            "Open Nexus Protocol to review research chain readiness.",
                            "Watch Nexus Field before running risky charge systems.",
                            "Use Field Map to avoid collapsed or critical chunks."))),
            Map.entry("blackbox", TerminalAddonGuide.mainline(5, 50, "Final chapter",
                    "Start Blackbox Protocol after late-story handoffs are complete and you are ready to resolve memory archives and endings.",
                    List.of(
                            "Open Blackbox Access to confirm route availability.",
                            "Decode memory archives before committing to final proof.",
                            "Use Truth Engine only when ending readiness is clear."))),
            Map.entry("blackbox_protocol", TerminalAddonGuide.mainline(5, 50, "Final chapter",
                    "Start Blackbox Protocol after late-story handoffs are complete and you are ready to resolve memory archives and endings.",
                    List.of(
                            "Open Blackbox Access to confirm route availability.",
                            "Decode memory archives before committing to final proof.",
                            "Use Truth Engine only when ending readiness is clear."))),
            Map.entry("industrial", TerminalAddonGuide.optional(600, "Side route",
                    "Industrial Nexus is optional factory progression; start it when your base can support heat, power, and machine recovery.",
                    List.of(
                            "Open Industrial Nexus and scan factory route telemetry.",
                            "Build toward scrubbers and safe-zone support.",
                            "Treat Furnace Warden progress as production survival, not a main saga gate."))),
            Map.entry("industrial_nexus", TerminalAddonGuide.optional(600, "Side route",
                    "Industrial Nexus is optional factory progression; start it when your base can support heat, power, and machine recovery.",
                    List.of(
                            "Open Industrial Nexus and scan factory route telemetry.",
                            "Build toward scrubbers and safe-zone support.",
                            "Treat Furnace Warden progress as production survival, not a main saga gate."))));

    private BuiltinTerminalTabs() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        for (BuiltinTabRegistration registration : builtinTabs()) {
            registerTab(registration.tab(), registration.profile());
        }
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("field_manual"),
                "Protocol Flow",
                "Terminal Field Interface",
                "OPEN",
                List.of(
                        "Use this terminal as the command surface for installed ECHO chapters and survival routes.",
                        "Views collect missions, field records, drone controls, route state, and chapter status when those systems are present.",
                        "Progression stays sealed until the field route proves it; the terminal shows the clearest safe command view without opening records early."),
                false));
    }

    public static Map<Identifier, TerminalNavigationProfile> builtinNavigationProfilesForTests() {
        Map<Identifier, TerminalNavigationProfile> profiles = new LinkedHashMap<>();
        for (BuiltinTabRegistration registration : builtinTabs()) {
            profiles.put(registration.tab().descriptor().id(), registration.profile());
        }
        return Map.copyOf(profiles);
    }

    private static List<BuiltinTabRegistration> builtinTabs() {
        return List.of(
                new BuiltinTabRegistration(new OverviewTab(), TerminalNavigationProfile.command(0)),
                new BuiltinTabRegistration(new MainSurvivalRouteTab(), TerminalNavigationProfile.progress(0)),
                new BuiltinTabRegistration(new BaselineTab(), TerminalNavigationProfile.progress(50)),
                new BuiltinTabRegistration(new MissionGraphTab(), TerminalNavigationProfile.holomap(10)),
                new BuiltinTabRegistration(new AddonsTab(), TerminalNavigationProfile.progress(150)),
                new BuiltinTabRegistration(new RouteRecordsTab(), TerminalNavigationProfile.holomap(20)),
                new BuiltinTabRegistration(new DiscoveryGridTab(), TerminalNavigationProfile.holomap(30)),
                new BuiltinTabRegistration(new FactionAtlasTab(), TerminalNavigationProfile.intel(128)),
                new BuiltinTabRegistration(new TerminalRecipeIndexTab(), TerminalNavigationProfile.index(0)),
                new BuiltinTabRegistration(new ArchivesTab(), TerminalNavigationProfile.intel(950)),
                new BuiltinTabRegistration(new VitalsTab(), TerminalNavigationProfile.system(130)),
                new BuiltinTabRegistration(new RewardInboxTab(), TerminalNavigationProfile.system(140)),
                new BuiltinTabRegistration(new DataCoreStatusTab(), TerminalNavigationProfile.system(145)),
                new BuiltinTabRegistration(new SettingsTab(), TerminalNavigationProfile.system(175)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static void registerTab(TerminalTab tab, TerminalNavigationProfile profile) {
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), profile);
    }

    private record BuiltinTabRegistration(TerminalTab tab, TerminalNavigationProfile profile) {
    }

    public static Identifier commandDeckPriorityTabForTests(
            boolean hasBlockers, int pendingRewards, boolean hasIncompleteRoute, int chapterCount) {
        return commandDeckPriorityTab(false, pendingRewards, hasBlockers, false, hasIncompleteRoute,
                true, chapterCount);
    }

    public static Identifier commandDeckPriorityTabForTests(
            boolean hasBlockers, int pendingRewards, boolean hasIncompleteRoute,
            boolean survivalRouteAvailable, int chapterCount) {
        return commandDeckPriorityTab(false, pendingRewards, hasBlockers, false, hasIncompleteRoute,
                survivalRouteAvailable, chapterCount);
    }

    public static Identifier commandDeckPriorityTabForTests(
            boolean vitalsWarning, boolean hasBlockers, int pendingRewards,
            boolean hasActiveSurvivalObjective, boolean hasIncompleteRoute,
            boolean survivalRouteAvailable, int chapterCount) {
        return commandDeckPriorityTab(vitalsWarning, pendingRewards, hasBlockers, hasActiveSurvivalObjective,
                hasIncompleteRoute, survivalRouteAvailable, chapterCount);
    }

    public static Identifier commandDeckRewardActionForTests() {
        return CLAIM_REWARDS;
    }

    public static List<String> recipeIndexFailureDiagnosticIdsForTests() {
        Map<Identifier, EchoDiagnosticBlocker> diagnostics = new LinkedHashMap<>();
        DiagnosticSupport.addRecipeDiagnostics(null, diagnostics,
                () -> {
                    throw new NoClassDefFoundError("test missing recipe bridge");
                });
        return diagnostics.keySet().stream()
                .map(Identifier::toString)
                .toList();
    }

    public static List<String> addonGuideOrderForTests(List<EchoAddonChapter> chapters) {
        return addonChapterEntries(chapters, null).stream()
                .map(entry -> entry.guide().label() + "|" + chapterId(entry.chapter()))
                .toList();
    }

    public static List<String> addonConfigAwareGuideOrderForTests(List<EchoAddonChapter> chapters) {
        return addonChapterEntries(configAwareChapters(chapters, null), null).stream()
                .map(entry -> chapterId(entry.chapter()) + "|" + chapterModId(entry.chapter()))
                .toList();
    }

    public static boolean addonConfigControlsStackForTests(int width, EchoConfigValueKind kind) {
        return configControlsStack(width, kind);
    }

    public static int addonConfigRowHeightForTests(int width, EchoConfigValueKind kind, boolean hasBadges) {
        return configEntryMinimumHeight(width, kind, hasBadges);
    }

    public static List<String> addonConfigSideTitlesForTests(String moduleId) {
        return configSnapshotsFor(moduleId).stream()
                .map(BuiltinTerminalTabs::configSideTitle)
                .toList();
    }

    public static TerminalAddonGuide addonGuideForTests(String chapterId) {
        return fallbackGuide(chapterId, "ECHO Chapter");
    }

    private static List<EchoConfigModuleSnapshot> configSnapshotsFor(String moduleId) {
        List<EchoConfigModuleSnapshot> modules = new ArrayList<>();
        TerminalConfigClientState.commonModule(moduleId).ifPresent(modules::add);
        EchoConfigRegistry.snapshot(moduleId, EchoConfigSide.CLIENT)
                .filter(EchoConfigModuleSnapshot::hasEntries)
                .ifPresent(modules::add);
        return modules;
    }

    private static String configSideTitle(EchoConfigModuleSnapshot module) {
        return module.categories().stream()
                .flatMap(category -> category.entries().stream())
                .findFirst()
                .map(entry -> entry.side() == EchoConfigSide.CLIENT ? "Client Local" : "Server/Common")
                .orElse("Config");
    }

    private static boolean textConfigKind(EchoConfigValueKind kind) {
        return kind != EchoConfigValueKind.BOOLEAN && kind != EchoConfigValueKind.ENUM;
    }

    private static boolean configControlsStack(int width, EchoConfigValueKind kind) {
        return TerminalUi.shouldStackControls(width, textConfigKind(kind));
    }

    private static int configEntryMinimumHeight(int width, EchoConfigValueKind kind, boolean hasBadges) {
        return TerminalUi.responsiveControlRowHeight(width, textConfigKind(kind), hasBadges);
    }

    private static int configControlsWidth(int width, EchoConfigValueKind kind) {
        return TerminalUi.responsiveControlWidth(width, textConfigKind(kind));
    }

    private static Identifier commandDeckPriorityTab(
            boolean vitalsWarning, int pendingRewards, boolean hasBlockers,
            boolean hasActiveSurvivalObjective, boolean hasIncompleteRoute,
            boolean survivalRouteAvailable, int chapterCount) {
        if (vitalsWarning) {
            return VITALS;
        }
        if (pendingRewards > 0) {
            return REWARD_INBOX;
        }
        if (hasActiveSurvivalObjective && survivalRouteAvailable) {
            return MainSurvivalQuestProvider.TAB_ID;
        }
        if (hasIncompleteRoute) {
            return ROUTE_RECORDS;
        }
        if (survivalRouteAvailable) {
            return MainSurvivalQuestProvider.TAB_ID;
        }
        return ADDONS;
    }

    private static List<EchoAddonChapter> addonChapters() {
        try {
            return EchoAddonRegistry.chapters();
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal addon chapter list failed; rendering empty chapter list.", exception);
            return List.of();
        }
    }

    private static List<AddonChapterEntry> addonChapterEntries(TerminalRenderContext context) {
        return addonChapterEntries(configAwareChapters(addonChapters(), context), context);
    }

    private static List<EchoAddonChapter> configAwareChapters(
            List<EchoAddonChapter> chapters, TerminalRenderContext context) {
        List<EchoAddonChapter> entries = new ArrayList<>(chapters == null ? List.of() : chapters);
        Set<String> knownKeys = new HashSet<>();
        for (EchoAddonChapter chapter : entries) {
            knownKeys.add(cleanKey(chapterId(chapter)));
            knownKeys.add(cleanKey(chapterModId(chapter)));
        }
        Map<String, EchoConfigModuleSnapshot> configModules = new LinkedHashMap<>();
        TerminalConfigClientState.commonModules().forEach(module -> configModules.putIfAbsent(module.moduleId(), module));
        EchoConfigRegistry.snapshots(EchoConfigSide.COMMON).forEach(module -> configModules.putIfAbsent(module.moduleId(), module));
        EchoConfigRegistry.snapshots(EchoConfigSide.CLIENT).forEach(module -> configModules.putIfAbsent(module.moduleId(), module));
        Set<String> configModuleIds = new HashSet<>(configModules.keySet());
        if (configModuleIds.isEmpty()) {
            return entries;
        }
        try {
            for (EchoModuleInfo module : EchoCoreServices.moduleReport()) {
                if (module == null || !module.loaded() || !configModuleIds.contains(module.modId())) {
                    continue;
                }
                if (knownKeys.contains(cleanKey(module.modId()))) {
                    continue;
                }
                entries.add(new ModuleAddonChapter(module));
                knownKeys.add(cleanKey(module.modId()));
            }
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal config module list failed; using registered addon chapters only.", exception);
        }
        for (EchoConfigModuleSnapshot module : configModules.values()) {
            String moduleId = module.moduleId();
            if (moduleId.isBlank() || knownKeys.contains(cleanKey(moduleId))) {
                continue;
            }
            entries.add(new ConfigSnapshotAddonChapter(module));
            knownKeys.add(cleanKey(moduleId));
        }
        return entries;
    }

    private static List<AddonChapterEntry> addonChapterEntries(
            List<EchoAddonChapter> chapters, TerminalRenderContext context) {
        List<AddonChapterEntry> entries = new ArrayList<>();
        for (EchoAddonChapter chapter : chapters == null ? List.<EchoAddonChapter>of() : chapters) {
            TerminalAddonInfo info = addonInfo(chapter, context);
            entries.add(new AddonChapterEntry(chapter, info, guideFor(chapter, info)));
        }
        return entries.stream()
                .sorted(java.util.Comparator
                        .comparingInt((AddonChapterEntry entry) -> entry.guide().sortOrder())
                        .thenComparing(entry -> entry.guide().mainline() ? 0 : 1)
                        .thenComparing(entry -> chapterGuideTitle(entry.chapter()))
                        .thenComparing(entry -> chapterId(entry.chapter())))
                .toList();
    }

    private static TerminalAddonGuide guideFor(EchoAddonChapter chapter, TerminalAddonInfo info) {
        TerminalAddonGuide provided = info == null ? TerminalAddonGuide.empty() : info.guide();
        if (provided != null && !provided.isEmpty()) {
            return provided;
        }
        return fallbackGuide(chapterId(chapter), chapterGuideTitle(chapter));
    }

    private static TerminalAddonGuide fallbackGuide(String chapterId, String title) {
        String key = cleanKey(chapterId);
        TerminalAddonGuide guide = FALLBACK_GUIDES.get(key);
        if (guide == null && key.contains(":")) {
            guide = FALLBACK_GUIDES.get(key.substring(key.indexOf(':') + 1));
        }
        if (guide != null) {
            return guide;
        }
        return TerminalAddonGuide.optional(9000, "Addon content",
                "This addon is installed, but it has not published mod route metadata yet.",
                List.of(
                        "Check its status and availability below.",
                        "Open linked terminal pages if they are registered.",
                        "Use the owning addon's records for first mission details."));
    }

    private static String chapterGuideTitle(EchoAddonChapter chapter) {
        String title = chapterDisplayName(chapter);
        if (title.regionMatches(true, 0, "ECHO:", 0, 5)) {
            title = title.substring(5).strip();
        }
        title = title.replaceFirst("(?i)^optional\\s*:\\s*", "");
        title = title.replaceFirst("(?i)^chapter\\s+\\d+\\s*:\\s*", "");
        return title.isBlank() ? "ECHO Mod" : title;
    }

    private static String chapterGuideHeading(EchoAddonChapter chapter, TerminalAddonGuide guide) {
        return chapterGuideTitle(chapter);
    }

    private static String cleanKey(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static TerminalAddonInfo addonInfo(EchoAddonChapter chapter, TerminalRenderContext context) {
        try {
            return TerminalAddonInfoRegistry.info(chapterId(chapter), context == null ? null : context.player());
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal addon info lookup failed for {}.", chapterId(chapter), exception);
            return TerminalAddonInfo.empty();
        }
    }

    private static String chapterId(EchoAddonChapter chapter) {
        try {
            String id = chapter == null ? "" : chapter.id();
            return id == null ? "" : id;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter id failed; using fallback id.", exception);
            return "";
        }
    }

    private static String chapterDisplayName(EchoAddonChapter chapter) {
        try {
            String displayName = chapter == null ? "" : chapter.displayName();
            return displayName == null || displayName.isBlank() ? "ECHO Mod" : displayName;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter display name failed; using fallback title.", exception);
            return "ECHO Mod";
        }
    }

    private static String chapterModId(EchoAddonChapter chapter) {
        try {
            String modId = chapter == null ? "" : chapter.modId();
            return modId == null || modId.isBlank() ? "unknown" : modId;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter mod id failed; using fallback mod id.", exception);
            return "unknown";
        }
    }

    private static String chapterSummary(EchoAddonChapter chapter) {
        try {
            String summary = chapter == null ? "" : chapter.summary();
            return summary == null || summary.isBlank() ? "No chapter briefing available." : summary;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter summary failed; using fallback summary.", exception);
            return "No chapter briefing available.";
        }
    }

    private static String chapterStatusLine(EchoAddonChapter chapter, TerminalRenderContext context) {
        try {
            String status = chapter == null ? "" : chapter.statusLine(context == null ? null : context.player());
            return status == null || status.isBlank() ? "Status signal unavailable." : status;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter status failed; using fallback status.", exception);
            return "Status signal unavailable.";
        }
    }

    private static boolean chapterAvailable(EchoAddonChapter chapter, TerminalRenderContext context) {
        try {
            return chapter != null && chapter.isAvailable(context == null ? null : context.player());
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal chapter availability failed; treating chapter as locked.", exception);
            return false;
        }
    }

    private static TerminalMissionChapter missionChapter(TerminalMissionProvider provider) {
        try {
            TerminalMissionChapter chapter = provider == null ? null : provider.chapter();
            return chapter == null
                    ? new TerminalMissionChapter(id("mission_provider"), "Unlisted Chapter", "", Integer.MAX_VALUE,
                            0xFF92F7A6, true)
                    : chapter;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal mission provider chapter failed; using fallback chapter.", exception);
            return new TerminalMissionChapter(id("mission_provider"), "Unlisted Chapter", "", Integer.MAX_VALUE,
                    0xFF92F7A6, true);
        }
    }

    private record AddonChapterEntry(
            EchoAddonChapter chapter,
            TerminalAddonInfo info,
            TerminalAddonGuide guide) {
    }

    private record ModuleAddonChapter(EchoModuleInfo module) implements EchoAddonChapter {
        @Override
        public String id() {
            return module.modId();
        }

        @Override
        public String modId() {
            return module.modId();
        }

        @Override
        public String displayName() {
            return module.displayName();
        }

        @Override
        public String summary() {
            return module.ownership().isBlank()
                    ? "Runtime module exposes editable ECHO config."
                    : module.ownership();
        }

        @Override
        public String statusLine(net.minecraft.world.entity.player.Player player) {
            return module.statusLine();
        }
    }

    private record ConfigSnapshotAddonChapter(EchoConfigModuleSnapshot module) implements EchoAddonChapter {
        @Override
        public String id() {
            return module.moduleId();
        }

        @Override
        public String modId() {
            return module.moduleId();
        }

        @Override
        public String displayName() {
            return module.displayName();
        }

        @Override
        public String summary() {
            return "Runtime module exposes editable ECHO config.";
        }
    }

    private static final class MainSurvivalRouteTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(MainSurvivalQuestProvider.TAB_ID, "SURVIVAL ROUTE", 170, 0xFF92F7A6);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Survival Route", TerminalTabChrome.GROUP_FIELD, "SR",
                        "Main survival quest line", 170);
        private final TerminalMissionBrowser browser =
                new TerminalMissionBrowser(MainSurvivalQuestProvider.INSTANCE, descriptor.id(), true, 40,
                        TerminalMissionBrowser.ActionMode.FULL_ACTIONS,
                        TerminalMissionBrowser.InitialTreeFocus.TOP,
                        TerminalMissionBrowser.InitialSelection.FIRST_RECORD,
                        TerminalMissionBrowser.DefaultPhaseExpansion.FIRST_ONLY);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            browser.onSelected(context);
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            browser.render(context, graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            return browser.mouseClicked(context, mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
                double dragX, double dragY) {
            return browser.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            return browser.mouseReleased(context, mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
            return browser.mouseScrolled(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
            return browser.keyPressed(context, event);
        }

        @Override
        public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
            return browser.charTyped(context, event);
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return browser.contentHeight(context);
        }
    }

    private static final class BaselineTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(VanillaJourneyProvider.TAB_ID, "BASELINE", 50, 0xFF92F7A6);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Baseline", TerminalTabChrome.GROUP_FIELD, "BL",
                        "Minecraft advancement route", 50);
        private final TerminalMissionBrowser browser =
                new TerminalMissionBrowser(VanillaJourneyProvider.INSTANCE, descriptor.id(), true, 10);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            browser.onSelected(context);
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            browser.render(context, graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            return browser.mouseClicked(context, mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
                double dragX, double dragY) {
            return browser.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            return browser.mouseReleased(context, mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
            return browser.mouseScrolled(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
            return browser.keyPressed(context, event);
        }

        @Override
        public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
            return browser.charTyped(context, event);
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return browser.contentHeight(context);
        }
    }

    private static final class OverviewTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("overview"), "COMMAND DECK", 0, 0xFF66D9FF);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Command Deck", TerminalTabChrome.GROUP_PROTOCOL, "CD",
                        "Active protocol dashboard", 0);
        private final List<DeckHitbox> hitboxes = new ArrayList<>();
        private List<DeckAction> lastActions = List.of();
        private int selectedActionIndex = -1;
        private int actionRenderIndex;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            hitboxes.clear();
            lastActions = List.of();
            selectedActionIndex = -1;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            hitboxes.clear();
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            List<EchoDiagnosticBlocker> diagnostics = EchoCoreServices.diagnostics(context.player());
            List<EchoRouteRecord> routes = EchoCoreServices.routeRecords(context.player());
            EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(context.player());
            List<EchoAddonChapter> chapters = addonChapters();
            int pendingRewards = EchoCoreServices.pendingTerminalRewardCount(context.player());
            EchoRouteRecord route = firstIncompleteRoute(routes);
            SurvivalObjective objective = survivalObjective(context);
            TrackedMission tracked = TerminalPlayerData.get(context.player()).trackedMission();
            List<DeckAction> tasks = survivalTasks(context, telemetry, diagnostics, objective, route, pendingRewards, chapters.size());
            DeckAction next = tracked == null
                    ? null
                    : trackedMissionAction(context, tracked);
            next = next != null
                    ? next
                    : tasks.isEmpty()
                    ? navigateAction(context, "Open Survival Route", "Guide", "Open the route and choose the next field objective.",
                            TerminalUi.GREEN, MainSurvivalQuestProvider.TAB_ID)
                    : tasks.get(0);
            List<DeckAction> followups = tasks.stream().skip(1).limit(3).toList();
            List<DeckAction> commands = commandActions(context, diagnostics, routes, pendingRewards, chapters.size());
            List<DeckAction> themeActions = themeActions();
            lastActions = deckActions(next, followups, commands, themeActions);
            actionRenderIndex = 0;
            if (selectedActionIndex >= lastActions.size()) {
                selectedActionIndex = -1;
            }

            int cy = TerminalUi.sectionHeader(context, graphics,
                    "SURVIVAL COMMAND", survivalHeaderStatus(telemetry, pendingRewards, diagnostics), x, y, w,
                    descriptor.accentColor());
            boolean wide = w >= 680;
            int gap = wide ? 12 : 8;
            if (wide) {
                int nextW = Math.max(360, w * 58 / 100);
                drawNextStepPanel(context, graphics, next, x, cy, nextW, 108, mouseX, mouseY);
                drawVitalsPanel(context, graphics, telemetry, x + nextW + gap, cy,
                        Math.max(220, w - nextW - gap), 108);
                cy += 120;
                int tasksW = Math.max(360, w * 58 / 100);
                int lowerY = cy;
                drawTaskList(context, graphics, followups, x, cy, tasksW, mouseX, mouseY);
                drawCommandGrid(context, graphics, commands, x + tasksW + gap, cy,
                        Math.max(220, w - tasksW - gap), mouseX, mouseY);
                int lowerH = Math.max(followups.isEmpty() ? 74 : 18 + followups.size() * 54,
                        18 + ((commands.size() + commandColumns(Math.max(220, w - tasksW - gap)) - 1)
                                / commandColumns(Math.max(220, w - tasksW - gap))) * 38);
                drawThemeSelector(context, graphics, themeActions, x, lowerY + lowerH + 10, w, mouseX, mouseY);
            } else {
                drawNextStepPanel(context, graphics, next, x, cy, w, 104, mouseX, mouseY);
                cy += 112;
                drawVitalsPanel(context, graphics, telemetry, x, cy, w, 96);
                cy += 104;
                cy = drawTaskList(context, graphics, followups, x, cy, w, mouseX, mouseY) + 10;
                drawCommandGrid(context, graphics, commands, x, cy, w, mouseX, mouseY);
                cy += 18 + ((commands.size() + commandColumns(w) - 1) / commandColumns(w)) * 38 + 10;
                drawThemeSelector(context, graphics, themeActions, x, cy, w, mouseX, mouseY);
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            int w = context.contentWidth();
            int commandRows = w >= 680 ? 2 : (4 + commandColumns(w) - 1) / commandColumns(w);
            int lower = Math.max(18 + 3 * 54, 18 + commandRows * 38);
            int base = w >= 680
                    ? 20 + 120 + lower + 86
                    : 20 + 112 + 104 + 18 + 3 * 54 + 10 + commandRows * 38 + 86;
            return Math.max(context.contentHeight(), base + 20);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (DeckHitbox hitbox : List.copyOf(hitboxes)) {
                if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                    selectedActionIndex = hitbox.index();
                    lastActions.get(hitbox.index()).execute(context);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                if (selectedActionIndex >= 0 && selectedActionIndex < lastActions.size()) {
                    lastActions.get(selectedActionIndex).execute(context);
                    return true;
                }
                if (!lastActions.isEmpty()) {
                    lastActions.get(0).execute(context);
                    return true;
                }
            }
            if (!lastActions.isEmpty() && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP)) {
                selectedActionIndex = selectedActionIndex < 0
                        ? 0
                        : Math.floorMod(selectedActionIndex - 1, lastActions.size());
                context.playCommandSound();
                return true;
            }
            if (!lastActions.isEmpty() && (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN)) {
                selectedActionIndex = selectedActionIndex < 0
                        ? 0
                        : Math.floorMod(selectedActionIndex + 1, lastActions.size());
                context.playCommandSound();
                return true;
            }
            return false;
        }

        private void drawNextStepPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                DeckAction action, int x, int y, int w, int h, int mouseX, int mouseY) {
            int actionIndex = registerHitbox(x, y, w, h, mouseX, mouseY);
            boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, w, h);
            boolean selected = actionIndex == selectedActionIndex;
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h,
                    hovered || selected ? action.color() : descriptor.accentColor());
            TerminalUi.hybridIconBadge(context, graphics, action.icon(context), TerminalIcon.DEFAULT,
                    x + 12, y + 30, 34, action.color(), hovered || selected);
            TerminalUi.line(context, graphics, "NEXT STEP", x + 12, y + 10, w - 24, action.color());
            TerminalUi.line(context, graphics, action.label(), x + 54, y + 32, Math.max(70, w - 166), TerminalUi.TEXT);
            int pillW = Math.min(104, Math.max(76, w / 4));
            TerminalUi.miniStatusPill(context, graphics, action.value(), x + w - pillW - 12,
                    y + 30, pillW, action.color(), hovered || selected);
            TerminalUi.wrap(context, graphics, action.detail(), x + 54, y + 56, w - 66,
                    action.enabled() ? TerminalUi.MUTED : TerminalUi.RED);
        }

        private static void drawVitalsPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                EchoHazardTelemetry telemetry, int x, int y, int w, int h) {
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h,
                    telemetry.warning() ? TerminalUi.AMBER : TerminalUi.GREEN);
            TerminalUi.line(context, graphics, "SURVIVAL STATUS", x + 12, y + 10, w - 24,
                    telemetry.warning() ? TerminalUi.AMBER : TerminalUi.GREEN);
            TerminalUi.wrap(context, graphics, survivalWarning(telemetry), x + 12, y + 28, w - 24,
                    telemetry.warning() ? TerminalUi.AMBER : TerminalUi.MUTED);
            int chipY = y + h - 24;
            int chipGap = 6;
            int chipW = Math.max(56, (w - 24 - chipGap * 2) / 3);
            drawVitalChip(context, graphics, "Water", telemetry.hydration(), true, x + 12, chipY, chipW);
            drawVitalChip(context, graphics, "Air", telemetry.oxygen(), true, x + 12 + chipW + chipGap, chipY, chipW);
            drawVitalChip(context, graphics, "Hazard", highestHazard(telemetry), false,
                    x + 12 + (chipW + chipGap) * 2, chipY, Math.max(44, w - 24 - (chipW + chipGap) * 2));
        }

        private static void drawVitalChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String label, int value, boolean highGood, int x, int y, int w) {
            int danger = highGood ? 100 - value : value;
            int color = danger >= 70 ? TerminalUi.RED : danger >= 40 ? TerminalUi.AMBER : TerminalUi.GREEN;
            TerminalUi.dataSurfaceRow(context, graphics, x, y, w, 16, color, false, false, true);
            TerminalUi.line(context, graphics, label, x + 5, y + 4, Math.max(20, w - 34), TerminalUi.MUTED);
            TerminalUi.line(context, graphics, value + "%", x + w - 30, y + 4, 28, color);
        }

        private int drawTaskList(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<DeckAction> actions, int x, int y, int w, int mouseX, int mouseY) {
            TerminalUi.section(context, graphics, "FIELD CHECKLIST", x, y, descriptor.accentColor());
            int cy = y + 18;
            if (actions.isEmpty()) {
                TerminalUi.emptyState(context, graphics, x, cy, w,
                        "No Extra Field Tasks", "Open the Survival Route when you want the full roadmap.",
                        TerminalUi.GREEN);
                return cy + 56;
            }
            for (DeckAction action : actions) {
                drawTaskCard(context, graphics, action, x, cy, w, 46, mouseX, mouseY);
                cy += 54;
            }
            return cy;
        }

        private void drawCommandGrid(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<DeckAction> commands, int x, int y, int width, int mouseX, int mouseY) {
            TerminalUi.section(context, graphics, "USEFUL COMMANDS", x, y, descriptor.accentColor());
            int columns = commandColumns(width);
            int gap = 6;
            int cardW = Math.max(96, (width - gap * (columns - 1)) / columns);
            int cy = y + 18;
            for (int i = 0; i < commands.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int cx = x + col * (cardW + gap);
                int cardY = cy + row * 38;
                int cw = col == columns - 1 ? Math.max(96, width - col * (cardW + gap)) : cardW;
                drawActionButton(context, graphics, commands.get(i), cx, cardY, cw, 30, mouseX, mouseY);
            }
        }

        private void drawActionButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                DeckAction action, int x, int y, int w, int h, int mouseX, int mouseY) {
            int actionIndex = registerHitbox(x, y, w, h, mouseX, mouseY);
            boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, w, h);
            boolean selected = actionIndex == selectedActionIndex;
            if (action.enabled()) {
                TerminalUi.primaryCommandButton(context, graphics, x, y, w, h, action.label(), action.icon(context),
                        action.color(), hovered || selected);
            } else {
                TerminalUi.disabledCommandButton(context, graphics, x, y, w, h, action.label(), action.icon(context));
            }
        }

        private void drawThemeSelector(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<DeckAction> themes, int x, int y, int width, int mouseX, int mouseY) {
            TerminalUi.section(context, graphics, "APPEARANCE", x, y, descriptor.accentColor());
            int columns = Math.max(1, Math.min(2, themes.size()));
            int gap = 8;
            int cardW = Math.max(128, (width - gap * Math.max(0, columns - 1)) / columns);
            int cy = y + 18;
            for (int i = 0; i < themes.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int cx = x + col * (cardW + gap);
                int cardY = cy + row * 36;
                int cw = col == columns - 1 ? Math.max(128, width - col * (cardW + gap)) : cardW;
                drawActionButton(context, graphics, themes.get(i), cx, cardY, cw, 28, mouseX, mouseY);
            }
        }

        private void drawTaskCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                DeckAction action, int x, int y, int w, int h, int mouseX, int mouseY) {
            int actionIndex = registerHitbox(x, y, w, h, mouseX, mouseY);
            boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, w, h);
            boolean selected = actionIndex == selectedActionIndex;
            TerminalUi.dataSurfaceRow(context, graphics, x, y, w, h,
                    hovered || selected ? action.color() : TerminalUi.CYAN_DIM,
                    hovered, selected, action.enabled());
            TerminalUi.hybridIcon(graphics, action.icon(context), TerminalIcon.DEFAULT, x + 9, y + 8, 18,
                    action.color(), hovered || selected);
            TerminalUi.line(context, graphics, action.label(), x + 34, y + 7, Math.max(80, w - 136), TerminalUi.TEXT);
            TerminalUi.line(context, graphics, action.value(), x + w - 92, y + 7, 82, action.color());
            TerminalUi.line(context, graphics, action.detail(), x + 34, y + 24, w - 44,
                    action.enabled() ? TerminalUi.MUTED : TerminalUi.RED);
        }

        private int registerHitbox(int x, int y, int w, int h, int mouseX, int mouseY) {
            int index = actionRenderIndex++;
            if (index < lastActions.size()) {
                hitboxes.add(new DeckHitbox(x, y, w, h, index));
                if (TerminalUi.inside(mouseX, mouseY, x, y, w, h)) {
                    selectedActionIndex = index;
                }
                return index;
            }
            return -1;
        }

        private static List<DeckAction> deckActions(DeckAction priority, List<DeckAction> followups,
                List<DeckAction> commands, List<DeckAction> themes) {
            List<DeckAction> actions = new ArrayList<>();
            actions.add(priority);
            actions.addAll(followups);
            actions.addAll(commands);
            actions.addAll(themes);
            return List.copyOf(actions);
        }

        private static List<DeckAction> themeActions() {
            Identifier selected = TerminalClientOptions.selectedThemeId();
            return TerminalThemeRegistry.all().stream()
                    .map(theme -> DeckAction.theme(theme, selected.equals(theme.id())))
                    .toList();
        }

        private static List<DeckAction> survivalTasks(TerminalRenderContext context, EchoHazardTelemetry telemetry,
                List<EchoDiagnosticBlocker> diagnostics, SurvivalObjective objective, EchoRouteRecord route,
                int pendingRewards, int chapterCount) {
            List<DeckAction> actions = new ArrayList<>();
            if (telemetry.warning()) {
                addUnique(actions, vitalAction(context, telemetry));
            }
            if (pendingRewards > 0) {
                addUnique(actions, DeckAction.reward("Claim Rewards", pendingRewards + " ready",
                        "Move support caches into your inventory before heading out.", TerminalUi.AMBER, true));
            }
            if (objective != null) {
                addUnique(actions, survivalObjectiveAction(context, objective));
            }
            if (route != null) {
                addUnique(actions, routeAction(context, route));
            }
            if (!diagnostics.isEmpty()) {
                addUnique(actions, blockerAction(context, diagnostics.get(0)));
            }
            addUnique(actions, navigateAction(context, "Open Survival Route", "Guide",
                    "Review the full survival roadmap and pick the next mission.", TerminalUi.GREEN,
                    MainSurvivalQuestProvider.TAB_ID));
            if (chapterCount > 0) {
                addUnique(actions, navigateAction(context, "Mods", "Linked",
                        "Check installed mod routes, status, and linked terminal pages.", TerminalUi.CYAN, ADDONS));
            }
            return actions.stream().limit(4).toList();
        }

        private static List<DeckAction> commandActions(TerminalRenderContext context, List<EchoDiagnosticBlocker> diagnostics,
                List<EchoRouteRecord> routes, int pendingRewards, int chapterCount) {
            List<DeckAction> commands = new ArrayList<>();
            addUnique(commands, navigateAction(context, "Active Route", chapterCount > 0 ? chapterCount + " routes" : "Route",
                    "Open the next field objective and mod route map.", TerminalUi.GREEN, MainSurvivalQuestProvider.TAB_ID));
            addUnique(commands, navigateAction(context, "Intel", routeSummary(routes),
                    "Open shared route records and world intel.", TerminalUi.CYAN, ROUTE_RECORDS));
            addUnique(commands, navigateAction(context, diagnostics.isEmpty() ? "Vitals" : "Blocker Status",
                    diagnostics.isEmpty() ? "Health" : String.valueOf(diagnostics.size()),
                    diagnostics.isEmpty()
                            ? "Review hydration, oxygen, and hazard telemetry."
                            : "Review the blocker summary on this Command Deck.",
                    diagnostics.isEmpty() ? TerminalUi.CYAN : DiagnosticSupport.severityColor(diagnostics.get(0).severity()),
                    VITALS));
            if (pendingRewards > 0) {
                addUnique(commands, DeckAction.reward("Rewards", pendingRewards + " ready",
                        "Collect support caches now.", TerminalUi.AMBER, true));
            } else {
                addUnique(commands, navigateAction(context, "Settings", "System",
                        "Tune navigation, presentation, and accessibility.", TerminalUi.CYAN, SETTINGS));
            }
            return commands.stream().limit(4).toList();
        }

        private static String routeSummary(List<EchoRouteRecord> routes) {
            return routes == null || routes.isEmpty() ? "Records" : completedRoutes(routes) + "/" + routes.size();
        }

        private static DeckAction blockerAction(TerminalRenderContext context, EchoDiagnosticBlocker blocker) {
            String action = blocker.nextAction().isBlank() ? blocker.detail() : blocker.nextAction();
            return navigateAction(context, "Review Blocker", blocker.severity().name(),
                    blocker.title() + ". " + action, DiagnosticSupport.severityColor(blocker.severity()),
                    MainSurvivalQuestProvider.TAB_ID);
        }

        private static DeckAction routeAction(TerminalRenderContext context, EchoRouteRecord route) {
            return navigateAction(context, "Continue Route", route.title(),
                    route.status() + ". " + route.summary(), TerminalUi.GREEN, ROUTE_RECORDS);
        }

        private static DeckAction navigateAction(TerminalRenderContext context,
                String label, String value, String detail, int color, Identifier tabId) {
            boolean enabled = context != null && context.canNavigateToTab(tabId);
            return new DeckAction(label, value, detail, color, tabId, false, null, "open", enabled);
        }

        private static DeckAction vitalAction(TerminalRenderContext context, EchoHazardTelemetry telemetry) {
            return navigateAction(context, "Stabilize Vitals", "WARNING", survivalWarning(telemetry),
                    TerminalUi.AMBER, VITALS);
        }

        private static DeckAction survivalObjectiveAction(TerminalRenderContext context, SurvivalObjective objective) {
            return navigateAction(context, "Continue Survival", objective.statusLabel(),
                    objective.title() + ". " + objective.nextStep(), objective.color(), MainSurvivalQuestProvider.TAB_ID);
        }

        private static DeckAction trackedMissionAction(TerminalRenderContext context, TrackedMission tracked) {
            boolean enabled = context != null && context.canNavigateToTab(tracked.tabId());
            int color = tracked.color() == 0 ? TerminalUi.AMBER : tracked.color();
            String title = tracked.title() == null || tracked.title().isBlank()
                    ? tracked.missionId().getPath()
                    : tracked.title();
            String nextStep = tracked.nextStep() == null || tracked.nextStep().isBlank()
                    ? "Open the tracked chapter to review the next move."
                    : tracked.nextStep();
            return new DeckAction("Tracked Mission", title, nextStep, color, tracked.tabId(),
                    false, null, "track", enabled);
        }

        private static void addUnique(List<DeckAction> actions, DeckAction action) {
            for (DeckAction existing : actions) {
                if (existing.sameDestination(action)) {
                    return;
                }
            }
            actions.add(action);
        }

        private static EchoRouteRecord firstIncompleteRoute(List<EchoRouteRecord> routes) {
            return routes.stream().filter(route -> !route.complete()).findFirst().orElse(null);
        }

        private static int completedRoutes(List<EchoRouteRecord> routes) {
            return (int) routes.stream().filter(EchoRouteRecord::complete).count();
        }

        private static int commandColumns(int width) {
            if (width >= 620) {
                return 2;
            }
            return width >= 360 ? 2 : 1;
        }

        private static String survivalHeaderStatus(EchoHazardTelemetry telemetry, int pendingRewards,
                List<EchoDiagnosticBlocker> diagnostics) {
            if (telemetry.warning()) {
                return "Vitals need attention";
            }
            if (pendingRewards > 0) {
                return pendingRewards + " reward(s) ready";
            }
            if (!diagnostics.isEmpty()) {
                return diagnostics.size() + " blocker(s)";
            }
            return "Ready";
        }

        private static String survivalWarning(EchoHazardTelemetry telemetry) {
            if (telemetry.hydration() <= 35) {
                return "Drink water soon. Hydration is " + telemetry.hydration() + "%.";
            }
            if (telemetry.oxygen() <= 35) {
                return "Find breathable air. Oxygen is " + telemetry.oxygen() + "%.";
            }
            if (telemetry.pressure() <= 35) {
                return "Pressure is unstable. Check suit or shelter before pushing ahead.";
            }
            if (telemetry.radiation() >= 50) {
                return "Radiation is elevated. Reduce exposure or leave the hot zone.";
            }
            if (telemetry.toxicAir() >= 50) {
                return "Toxic air detected. Use protection or relocate.";
            }
            if (telemetry.cold() >= 50) {
                return "Cold exposure is climbing. Warm up before traveling.";
            }
            if (telemetry.heat() >= 50) {
                return "Heat exposure is climbing. Cool down before traveling.";
            }
            if (telemetry.exposure() >= 50) {
                return "Environmental exposure is high. Find cover or stabilize gear.";
            }
            return telemetry.statusLine();
        }

        private static int highestHazard(EchoHazardTelemetry telemetry) {
            return Math.max(Math.max(telemetry.radiation(), telemetry.toxicAir()),
                    Math.max(Math.max(telemetry.cold(), telemetry.heat()), telemetry.exposure()));
        }

        private static SurvivalObjective survivalObjective(TerminalRenderContext context) {
            SurvivalObjective best = null;
            for (TerminalMissionDefinition definition : safeSurvivalMissions(context)) {
                TerminalMissionSnapshot snapshot = safeSurvivalSnapshot(context, definition);
                if (isClaimed(snapshot.status())) {
                    continue;
                }
                TerminalMissionPresentation presentation = safeSurvivalPresentation(context, definition, snapshot);
                SurvivalObjective candidate = new SurvivalObjective(
                        presentation.shortTitle(),
                        snapshot.statusLabel(),
                        emptyFallback(presentation.nextStep(), presentation.objectiveSummary()),
                        statusColor(snapshot.status()),
                        survivalScore(snapshot.status()));
                if (best == null || candidate.score() < best.score()) {
                    best = candidate;
                }
            }
            return best;
        }

        private static List<TerminalMissionDefinition> safeSurvivalMissions(TerminalRenderContext context) {
            try {
                List<TerminalMissionDefinition> missions = MainSurvivalQuestProvider.INSTANCE
                        .missions(context == null ? null : context.player());
                return missions == null ? List.of() : missions;
            } catch (RuntimeException exception) {
                return List.of();
            }
        }

        private static TerminalMissionSnapshot safeSurvivalSnapshot(
                TerminalRenderContext context, TerminalMissionDefinition definition) {
            try {
                TerminalMissionSnapshot snapshot = MainSurvivalQuestProvider.INSTANCE
                        .snapshot(context == null ? null : context.player(), definition.id());
                return snapshot == null
                        ? new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED, 0.0F,
                                "LOCKED", "", "Open Survival Route for details.", List.of())
                        : snapshot;
            } catch (RuntimeException exception) {
                return new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED, 0.0F,
                        "LOCKED", "", "Open Survival Route for details.", List.of());
            }
        }

        private static TerminalMissionPresentation safeSurvivalPresentation(
                TerminalRenderContext context, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            try {
                TerminalMissionPresentation presentation = MainSurvivalQuestProvider.INSTANCE
                        .presentation(context == null ? null : context.player(), definition, snapshot);
                return presentation == null
                        ? TerminalMissionPresentation.fallback(definition, snapshot)
                        : presentation;
            } catch (RuntimeException exception) {
                return TerminalMissionPresentation.fallback(definition, snapshot);
            }
        }

        private static boolean isClaimed(TerminalMissionStatus status) {
            return status == TerminalMissionStatus.CLAIMED || status == TerminalMissionStatus.COMPLETED;
        }

        private static int survivalScore(TerminalMissionStatus status) {
            return switch (status) {
                case CLAIMABLE -> 0;
                case UNLOCKED -> 1;
                case LOCKED, VIEW_ONLY -> 2;
                case COMPLETED, CLAIMED -> 3;
            };
        }

        private static int statusColor(TerminalMissionStatus status) {
            return switch (status) {
                case CLAIMABLE -> TerminalUi.AMBER;
                case UNLOCKED -> TerminalUi.GREEN;
                case LOCKED, VIEW_ONLY -> TerminalUi.MUTED;
                case COMPLETED, CLAIMED -> TerminalUi.CYAN;
            };
        }

        private static String emptyFallback(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }

        private record DeckHitbox(int x, int y, int w, int h, int index) {
        }

        private record SurvivalObjective(String title, String statusLabel, String nextStep, int color, int score) {
        }

        private record DeckAction(String label, String value, String detail, int color, Identifier tabId,
                boolean rewardClaim, Identifier themeId, String iconKey, boolean enabled) {
            static DeckAction reward(String label, String value, String detail, int color, boolean enabled) {
                return new DeckAction(label, value, detail, color, REWARD_INBOX, true, null, "claim", enabled);
            }

            static DeckAction theme(TerminalTheme theme, boolean selected) {
                return new DeckAction(theme.displayName(), selected ? "ACTIVE" : "AVAILABLE",
                        selected ? "Current terminal skin and icon set." : "Switch terminal skin and icon set.",
                        selected ? TerminalUi.GREEN : theme.tokens().colors().accent(),
                        null, false, theme.id(), "settings", true);
            }

            static DeckAction themeCycle() {
                List<TerminalTheme> themes = TerminalThemeRegistry.all();
                if (themes.isEmpty()) {
                    return new DeckAction("Theme", "DEFAULT", "Theme registry is using the fallback console skin.",
                            TerminalUi.CYAN, null, false, TerminalClientOptions.selectedThemeId(), "cycle", true);
                }
                Identifier current = TerminalClientOptions.selectedThemeId();
                int index = 0;
                for (int i = 0; i < themes.size(); i++) {
                    if (themes.get(i).id().equals(current)) {
                        index = i;
                        break;
                    }
                }
                TerminalTheme next = themes.get(Math.floorMod(index + 1, themes.size()));
                return new DeckAction("Theme", next.displayName(), "Cycle terminal skin and icon set.",
                        next.tokens().colors().accent(), null, false, next.id(), "cycle", true);
            }

            boolean sameDestination(DeckAction other) {
                return other != null && rewardClaim == other.rewardClaim
                        && java.util.Objects.equals(tabId, other.tabId)
                        && java.util.Objects.equals(themeId, other.themeId);
            }

            Identifier icon(TerminalRenderContext context) {
                if (themeId != null) {
                    return TerminalUi.themedIcon(context, TerminalIconKey.theme(iconKey), TerminalVisualAssets.ICON_GROUP_SYSTEMS);
                }
                Identifier fallback = rewardClaim ? TerminalVisualAssets.ICON_ACTION_CLAIM : TerminalVisualAssets.ICON_ACTION_OPEN_ROADMAP;
                return TerminalUi.themedActionIcon(context, iconKey, fallback);
            }

            void execute(TerminalRenderContext context) {
                if (!enabled) {
                    context.playRejectedSound();
                    return;
                }
                if (themeId != null) {
                    TerminalClientOptions.selectTheme(themeId);
                    TerminalUi.applyThemeGlobals(TerminalClientOptions.currentTheme());
                    context.playCommandSound();
                    return;
                }
                if (rewardClaim) {
                    context.sendAction(REWARD_INBOX, CLAIM_REWARDS, "");
                    return;
                }
                context.navigateToTab(tabId);
            }
        }
    }

    private static final class SettingsTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(SETTINGS, "INTERFACE SETTINGS", 175, 0xFF9FD1FF);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Interface Settings", TerminalTabChrome.GROUP_SYSTEMS, "IS",
                        "Presentation and accessibility", 175);
        private final List<SettingsHitbox> hitboxes = new ArrayList<>();

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            hitboxes.clear();
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int cy = TerminalUi.sectionHeader(context, graphics,
                    "INTERFACE SETTINGS", "Client-only", x, y, w, descriptor.accentColor());
            cy = TerminalUi.sectionHeader(context, graphics, "Appearance", "", x, cy + 4, w,
                    descriptor.accentColor());
            cy = drawInterfaceDensityOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = drawTerminalZoomOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = drawVisualOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = TerminalUi.sectionHeader(context, graphics, "Accessibility", "", x, cy + 4, w,
                    TerminalUi.GREEN);
            cy = drawAccessibilityOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = TerminalUi.sectionHeader(context, graphics, "Controls", "", x, cy + 4, w,
                    descriptor.accentColor());
            cy = drawNavigationOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = drawHudNoticeOptions(context, graphics, x, cy, w, mouseX, mouseY) + 10;
            cy = TerminalUi.sectionHeader(context, graphics, "Debug / Advanced", "", x, cy + 4, w,
                    TerminalUi.AMBER);
            drawDebugOptions(context, graphics, x, cy, w, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (SettingsHitbox hitbox : List.copyOf(hitboxes)) {
                if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                    hitbox.action().run();
                    context.playCommandSound();
                    return true;
                }
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), context.contentWidth() < 420 ? 560 : 470);
        }

        private int drawNavigationOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            List<SettingsOption> options = new ArrayList<>();
            for (TerminalClientOptions.NavigationStyle style : TerminalClientOptions.NavigationStyle.values()) {
                options.add(new SettingsOption(label(style.name()),
                        TerminalClientOptions.navigationStyle == style,
                        () -> TerminalClientOptions.selectNavigationStyle(style)));
            }
            return drawOptionSection(context, graphics, "NAVIGATION", x, y, w, mouseX, mouseY, options);
        }

        private int drawInterfaceDensityOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            List<SettingsOption> options = new ArrayList<>();
            for (TerminalClientOptions.InterfaceDensity density : TerminalClientOptions.InterfaceDensity.values()) {
                options.add(new SettingsOption(label(density.name()),
                        TerminalClientOptions.interfaceDensity == density,
                        () -> TerminalClientOptions.selectInterfaceDensity(density)));
            }
            return drawOptionSection(context, graphics, "INTERFACE DENSITY", x, y, w, mouseX, mouseY, options);
        }

        private int drawTerminalZoomOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            List<SettingsOption> options = new ArrayList<>();
            for (TerminalClientOptions.TerminalZoom zoom : TerminalClientOptions.TerminalZoom.values()) {
                options.add(new SettingsOption(zoom.label(),
                        TerminalClientOptions.terminalZoom == zoom,
                        () -> TerminalClientOptions.selectTerminalZoom(zoom)));
            }
            return drawOptionSection(context, graphics, "TERMINAL ZOOM", x, y, w, mouseX, mouseY, options);
        }

        private int drawHudNoticeOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            return drawOptionSection(context, graphics, "HUD NOTICES", x, y, w, mouseX, mouseY,
                    List.of(new SettingsOption("mission hud", TerminalClientOptions.missionHudNotifications,
                            () -> TerminalClientOptions.setMissionHudNotifications(
                                    !TerminalClientOptions.missionHudNotifications))));
        }

        private int drawVisualOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            List<SettingsOption> options = new ArrayList<>();
            for (TerminalClientOptions.VisualLevel level : TerminalClientOptions.VisualLevel.values()) {
                options.add(new SettingsOption(label(level.name()),
                        TerminalClientOptions.visualLevel == level,
                        () -> TerminalClientOptions.selectVisualLevel(level)));
            }
            return drawOptionSection(context, graphics, "VISUAL TREATMENTS", x, y, w, mouseX, mouseY, options);
        }

        private int drawAccessibilityOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            return drawOptionSection(context, graphics, "READABILITY MODES", x, y, w, mouseX, mouseY, List.of(
                    new SettingsOption("high contrast", TerminalClientOptions.highContrastMode(),
                            () -> TerminalClientOptions.setHighContrastMode(!TerminalClientOptions.highContrastMode())),
                    new SettingsOption("reduced clutter", TerminalClientOptions.reducedClutterMode(),
                            () -> TerminalClientOptions.setReducedClutterMode(!TerminalClientOptions.reducedClutterMode())),
                    new SettingsOption("large text", TerminalClientOptions.largeTextMode(),
                            () -> TerminalClientOptions.setLargeTextMode(!TerminalClientOptions.largeTextMode())),
                    new SettingsOption("simplified terminal", TerminalClientOptions.simplifiedTerminalMode(),
                            () -> TerminalClientOptions.setSimplifiedTerminalMode(!TerminalClientOptions.simplifiedTerminalMode())),
                    new SettingsOption("reduced motion", TerminalClientOptions.reduceMotion(),
                            () -> TerminalClientOptions.setReducedMotion(!TerminalClientOptions.reduceMotion())),
                    new SettingsOption("reduce glow", TerminalClientOptions.reduceGlow(),
                            () -> TerminalClientOptions.setReduceGlow(!TerminalClientOptions.reduceGlow())),
                    new SettingsOption("reduce grid noise", TerminalClientOptions.reduceGridNoise(),
                            () -> TerminalClientOptions.setReduceGridNoise(!TerminalClientOptions.reduceGridNoise()))));
        }

        private int drawDebugOptions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            return drawOptionSection(context, graphics, "DEBUG / ADVANCED", x, y, w, mouseX, mouseY, List.of(
                    new SettingsOption("hide debug info", TerminalClientOptions.hideDebugInfo(),
                            () -> TerminalClientOptions.setHideDebugInfo(!TerminalClientOptions.hideDebugInfo())),
                    new SettingsOption("use screencore", TerminalClientOptions.useScreenCore(),
                            () -> TerminalClientOptions.setUseScreenCore(!TerminalClientOptions.useScreenCore())),
                    new SettingsOption("match terminal layout", TerminalClientOptions.screenCoreMatchExistingLayout(),
                            () -> TerminalClientOptions.setScreenCoreMatchExistingLayout(
                                    !TerminalClientOptions.screenCoreMatchExistingLayout())),
                    new SettingsOption("screencore debug", TerminalClientOptions.screenCoreDebug(),
                            () -> TerminalClientOptions.setScreenCoreDebug(!TerminalClientOptions.screenCoreDebug())),
                    new SettingsOption("screencore shell", TerminalClientOptions.screenCoreExperimentalTabs(),
                            () -> TerminalClientOptions.setScreenCoreExperimentalTabs(
                                    !TerminalClientOptions.screenCoreExperimentalTabs()))));
        }

        private int drawOptionSection(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String title, int x, int y, int w, int mouseX, int mouseY, List<SettingsOption> options) {
            TerminalUi.section(context, graphics, title, x, y, descriptor.accentColor());
            int chipX = x;
            int chipY = y + 18;
            int rowBottom = chipY + 16;
            int gap = 6;
            int estimatedRows = Math.max(1, (options.size() + Math.max(1, w / 96) - 1) / Math.max(1, w / 96));
            TerminalUi.filterToolbarPanel(context, graphics, x, chipY - 4, w,
                    Math.max(26, estimatedRows * 21 + 8), descriptor.accentColor());
            for (SettingsOption option : options) {
                int chipW = Math.max(74, Math.min(148, context.minecraft().font.width(option.label()) + 26));
                chipW = Math.min(chipW, Math.max(52, w));
                if (chipX > x && chipX + chipW > x + w) {
                    chipX = x;
                    chipY += 21;
                }
                drawOptionChip(context, graphics, chipX, chipY, chipW, option.label(),
                        option.selected(), mouseX, mouseY, option.action());
                rowBottom = Math.max(rowBottom, chipY + 16);
                chipX += chipW + gap;
            }
            return rowBottom + 4;
        }

        private void drawOptionChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, String label, boolean selected, int mouseX, int mouseY, Runnable action) {
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, y, w, 16);
            TerminalUi.filterChip(context, graphics, x, y, w, label, selected, true,
                    descriptor.accentColor(), hover);
            hitboxes.add(new SettingsHitbox(x, y, w, 16, action));
        }

        private static String label(String value) {
            return value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        }

        private record SettingsOption(String label, boolean selected, Runnable action) {
        }

        private record SettingsHitbox(int x, int y, int w, int h, Runnable action) {
        }
    }

    private static final class AddonsTab implements ClientTerminalTab {
        private static final int ROW_HEIGHT = 52;
        private static final int LINK_HEIGHT = 34;
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(ADDONS, "MODS", 150, 0xFFFFD166);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Mods", TerminalTabChrome.GROUP_CORE, "MD",
                        "Installed mod order", 150);
        private final List<AddonLinkHitbox> linkHitboxes = new ArrayList<>();
        private final List<ConfigHitbox> configHitboxes = new ArrayList<>();
        private String selectedChapterId = "";
        private String editingConfigKey = "";
        private String editDraft = "";
        private int lastConfigRequestTick = -1000;
        private int lastListX;
        private int lastListY;
        private int lastListW;
        private int lastListH;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            normalizeSelection(addonChapterEntries(context));
            linkHitboxes.clear();
            configHitboxes.clear();
            lastConfigRequestTick = -1000;
            requestCommonConfig(context);
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            linkHitboxes.clear();
            configHitboxes.clear();
            maybeRequestCommonConfig(context);
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            List<AddonChapterEntry> entries = addonChapterEntries(context);
            if (entries.isEmpty()) {
                TerminalUi.flatDataPanel(context, graphics,
                        x, y, w, Math.min(150, Math.max(96, h / 4)), "MODS", "",
                        descriptor.accentColor());
                TerminalUi.wrap(context, graphics,
                        "No mod routes detected. Installed ECHO mod guidance appears here after route metadata registers.",
                        x + 14, y + 44, w - 28, TerminalUi.MUTED);
                return;
            }
            normalizeSelection(entries);
            boolean wide = w >= 700;
            int listW = wide ? Math.max(280, Math.min(400, w * 34 / 100)) : w;
            int detailX = wide ? x + listW + 14 : x;
            int desiredBoardH = Math.max(250, entries.size() * ROW_HEIGHT + 66);
            int boardH = wide
                    ? Math.max(260, Math.min(Math.max(260, h), desiredBoardH))
                    : Math.max(168, entries.size() * ROW_HEIGHT + 52);
            int detailY = wide ? y : y + boardH + 12;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;
            long mainlineCount = entries.stream().filter(entry -> entry.guide().mainline()).count();
            long optionalCount = entries.size() - mainlineCount;

            int cy = TerminalUi.flatDataPanel(context, graphics,
                    x, y, listW - 8, boardH, "MODS",
                    mainlineCount + " story / " + optionalCount + " side", descriptor.accentColor()) + 6;
            lastListX = x;
            lastListY = cy;
            lastListW = listW - 8;
            lastListH = Math.max(1, boardH - (cy - y) - 12);
            for (AddonChapterEntry entry : entries) {
                EchoAddonChapter chapter = entry.chapter();
                TerminalAddonGuide guide = entry.guide();
                boolean available = chapterAvailable(chapter, context);
                boolean selected = chapterId(chapter).equals(selectedChapterId);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x, cy, listW - 8, ROW_HEIGHT - 8);
                TerminalRenderContext chapterContext = context.withChapterTheme(chapterModId(chapter),
                        chapterGuideTitle(chapter), chapterModId(chapter));
                int rowAccent = available
                        ? TerminalUi.chapterAccent(chapterContext, guide.mainline() ? TerminalUi.CYAN : TerminalUi.AMBER)
                        : TerminalUi.MUTED;
                int statusColor = available ? TerminalUi.GREEN : TerminalUi.MUTED;
                TerminalUi.dataListRow(chapterContext, graphics, x + 10, cy, listW - 28, ROW_HEIGHT - 8,
                        chapterGuideHeading(chapter, guide), guide.stageLabel(),
                        available ? "AVAILABLE" : "LOCKED", selected, hovered, rowAccent, statusColor);
                cy += ROW_HEIGHT;
            }

            AddonChapterEntry selectedEntry = selectedEntry(entries);
            if (selectedEntry == null) {
                return;
            }
            EchoAddonChapter selected = selectedEntry.chapter();
            TerminalAddonInfo info = selectedEntry.info();
            TerminalAddonGuide guide = selectedEntry.guide();
            boolean available = chapterAvailable(selected, context);
            AddonSignals signals = addonSignals(selected, context);
            List<TerminalAddonMetric> metrics = addonMetrics(info, signals, available);
            List<TerminalAddonSection> sections = addonSections(info, selected, context, signals, available);
            List<TerminalAddonLink> links = addonLinks(info, selected);
            TerminalRenderContext selectedContext = context.withChapterTheme(chapterModId(selected),
                    chapterGuideTitle(selected), chapterModId(selected));
            int color = available
                    ? TerminalUi.chapterAccent(selectedContext, guide.mainline() ? TerminalUi.GREEN : TerminalUi.AMBER)
                    : TerminalUi.MUTED;
            int detailPanelH = wide
                    ? Math.max(boardH,
                            addonDetailHeight(context, selected, info, guide, metrics, sections, links, detailW - 8))
                    : addonDetailHeight(context, selected, info, guide, metrics, sections, links, detailW - 8);
            int dy = TerminalUi.flatDataPanel(selectedContext, graphics,
                    detailX, detailY, detailW - 8, detailPanelH, "MOD DETAIL", "",
                    color) + 2;
            Identifier banner = TerminalUi.chapterBanner(selectedContext);
            if (TerminalClientOptions.useVisualAssets() && banner != null && detailW > 340) {
                TerminalUi.imagePanel(selectedContext, graphics, banner, detailX + 12, dy + 2,
                        detailW - 32, Math.min(70, Math.max(46, detailPanelH / 6)), color, 0.70F, true,
                        TerminalUi.ImageFit.COVER);
            }
            TerminalUi.hybridIconBadge(selectedContext, graphics,
                    TerminalUi.themedIcon(selectedContext, TerminalIconKey.chapter(chapterModId(selected)),
                            TerminalVisualAssets.ICON_PAGE_CHAPTERS),
                    TerminalIcon.ADDONS,
                    detailX + 14, dy + 2, 42, color, true);
            TerminalUi.line(selectedContext, graphics, chapterGuideHeading(selected, guide), detailX + 66, dy + 8,
                    detailW - 178, available ? TerminalUi.GREEN : TerminalUi.MUTED);
            TerminalUi.miniStatusPill(selectedContext, graphics, available ? "AVAILABLE" : "LOCKED",
                    detailX + detailW - 102, dy + 7, 82, color, available);
            dy += 56;
            dy = TerminalUi.keyValue(context, graphics, detailX + 14, dy, detailW - 32,
                    "Stage", guide.stageLabel(), guide.mainline() ? TerminalUi.GREEN : TerminalUi.AMBER);
            if (!guide.startHint().isBlank()) {
                dy = TerminalUi.wrap(context, graphics, guide.startHint(), detailX + 14, dy + 4,
                        detailW - 32, TerminalUi.TEXT) + 8;
            }
            dy = drawGuideSteps(context, graphics, guide, detailX + 14, dy, detailW - 32);
            TerminalUi.divider(graphics, detailX + 14, dy, detailW - 32, descriptor.accentColor());
            dy += 10;
            dy = TerminalUi.keyValue(context, graphics, detailX + 14, dy, detailW - 32,
                    "Signal", chapterModId(selected), TerminalUi.TEXT);
            dy = TerminalUi.keyValue(context, graphics, detailX + 14, dy, detailW - 32,
                    "Status", chapterStatusLine(selected, context), color);
            String summary = chapterSummary(selected);
            dy = TerminalUi.wrap(context, graphics, summary, detailX + 14, dy + 4,
                    detailW - 32, TerminalUi.TEXT) + 8;
            if (!info.summary().isBlank() && !info.summary().equals(summary)) {
                dy = TerminalUi.wrap(context, graphics, info.summary(), detailX + 14, dy,
                        detailW - 32, TerminalUi.MUTED) + 8;
            }
            TerminalUi.divider(graphics, detailX + 14, dy, detailW - 32, descriptor.accentColor());
            dy = drawMetrics(context, graphics, metrics, detailX + 14, dy + 10, detailW - 32);
            dy = drawSections(context, graphics, sections, detailX + 14, dy + 6, detailW - 32);
            dy = drawLinks(context, graphics, guide, links, detailX + 14, dy + 8, detailW - 32, mouseX, mouseY);
            drawConfigSections(context, graphics, chapterModId(selected), detailX + 14, dy + 10,
                    detailW - 32, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            if (TerminalUi.inside(mouseX, mouseY, lastListX, lastListY, lastListW, lastListH)) {
                List<AddonChapterEntry> entries = addonChapterEntries(context);
                int index = (int) ((mouseY - lastListY) / ROW_HEIGHT);
                if (index >= 0 && index < entries.size()) {
                    selectedChapterId = chapterId(entries.get(index).chapter());
                    context.playCommandSound();
                    return true;
                }
            }
            for (AddonLinkHitbox hitbox : List.copyOf(linkHitboxes)) {
                if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                    if (context.canNavigateToTab(hitbox.link().targetTabId())) {
                        context.navigateToTab(hitbox.link().targetTabId());
                    } else {
                        context.playRejectedSound();
                    }
                    return true;
                }
            }
            for (ConfigHitbox hitbox : List.copyOf(configHitboxes)) {
                if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                    hitbox.action().run();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
            if (editingConfigKey.isBlank() || event == null) {
                return false;
            }
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editingConfigKey = "";
                editDraft = "";
                context.playRejectedSound();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editDraft.isEmpty()) {
                    editDraft = editDraft.substring(0, editDraft.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                findEditingEntry().ifPresent(entry -> applyConfigValue(context, entry, editDraft));
                return true;
            }
            return false;
        }

        @Override
        public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
            if (editingConfigKey.isBlank() || event == null || !event.isAllowedChatCharacter()
                    || editDraft.length() >= 96) {
                return false;
            }
            String typed = event.codepointAsString();
            if (typed == null || typed.isBlank()) {
                return false;
            }
            editDraft += typed;
            return true;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            int w = context.contentWidth();
            boolean wide = w >= 700;
            int listW = wide ? Math.max(280, Math.min(400, w * 34 / 100)) : w;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;
            List<AddonChapterEntry> entries = addonChapterEntries(context);
            int boardH = wide ? Math.max(260, Math.min(Math.max(260, context.contentHeight()),
                    Math.max(250, entries.size() * ROW_HEIGHT + 66)))
                    : Math.max(180, entries.size() * ROW_HEIGHT + 46);
            AddonChapterEntry selectedEntry = selectedEntry(entries);
            EchoAddonChapter selected = selectedEntry == null ? null : selectedEntry.chapter();
            TerminalAddonInfo info = selectedEntry == null ? TerminalAddonInfo.empty() : selectedEntry.info();
            TerminalAddonGuide guide = selectedEntry == null ? TerminalAddonGuide.empty() : selectedEntry.guide();
            AddonSignals signals = selected == null ? AddonSignals.empty() : addonSignals(selected, context);
            boolean available = selected != null && chapterAvailable(selected, context);
            List<TerminalAddonMetric> metrics = selected == null ? List.of() : addonMetrics(info, signals, available);
            List<TerminalAddonSection> sections = selected == null ? List.of()
                    : addonSections(info, selected, context, signals, available);
            List<TerminalAddonLink> links = selected == null ? List.of() : addonLinks(info, selected);
            int detailHeight = selected == null ? 120
                    : addonDetailHeight(context, selected, info, guide, metrics, sections, links, detailW - 8);
            if (wide) {
                return Math.max(context.contentHeight(), Math.max(boardH, detailHeight) + 32);
            }
            return Math.max(context.contentHeight(), boardH + detailHeight + 32);
        }

        private void normalizeSelection(List<AddonChapterEntry> entries) {
            if (entries.stream().anyMatch(entry -> chapterId(entry.chapter()).equals(selectedChapterId))) {
                return;
            }
            selectedChapterId = entries.isEmpty() ? "" : chapterId(entries.get(0).chapter());
        }

        private AddonChapterEntry selectedEntry(List<AddonChapterEntry> entries) {
            normalizeSelection(entries);
            return entries.stream()
                    .filter(entry -> chapterId(entry.chapter()).equals(selectedChapterId))
                    .findFirst()
                    .orElse(null);
        }

        private static AddonSignals addonSignals(EchoAddonChapter chapter, TerminalRenderContext context) {
            String chapterId = chapterId(chapter);
            String modId = chapterModId(chapter);
            int missions = 0;
            for (TerminalMissionProvider provider : TerminalMissionRegistry.providers()) {
                TerminalMissionChapter missionChapter = missionChapter(provider);
                if (matchesChapterKey(missionChapter.id().toString(), chapterId, modId)) {
                    missions += safeMissionCount(provider, context);
                }
            }
            int routes = 0;
            for (EchoRouteRecord record : EchoCoreServices.routeRecords(context == null ? null : context.player())) {
                if (record != null && matchesChapterKey(record.chapterId(), chapterId, modId)) {
                    routes++;
                }
            }
            int diagnostics = 0;
            for (EchoDiagnosticBlocker blocker : EchoCoreServices.diagnostics(context == null ? null : context.player())) {
                if (blocker != null && matchesChapterKey(blocker.chapterId(), chapterId, modId)) {
                    diagnostics++;
                }
            }
            int archives = 0;
            for (TerminalArchiveEntry entry : TerminalArchiveRegistry.entries()) {
                if (entry != null && (entry.id().getNamespace().equals(modId)
                        || matchesLooseText(entry.group(), chapterId, modId))) {
                    archives++;
                }
            }
            return new AddonSignals(missions, routes, diagnostics, archives);
        }

        private static int safeMissionCount(TerminalMissionProvider provider, TerminalRenderContext context) {
            try {
                List<TerminalMissionDefinition> missions = provider.missions(context == null ? null : context.player());
                return missions == null ? 0 : (int) missions.stream().filter(mission -> mission != null).count();
            } catch (RuntimeException exception) {
                EchoTerminal.LOGGER.warn("Terminal addon mission count failed; treating provider as empty.", exception);
                return 0;
            }
        }

        private static List<TerminalAddonMetric> addonMetrics(
                TerminalAddonInfo info, AddonSignals signals, boolean available) {
            List<TerminalAddonMetric> metrics = new ArrayList<>();
            metrics.add(new TerminalAddonMetric("State", available ? "ONLINE" : "LOCKED",
                    available ? "chapter commands can expose their own checks" : "chapter preview remains read-only",
                    available ? TerminalUi.GREEN : TerminalUi.AMBER));
            metrics.add(new TerminalAddonMetric("Missions", String.valueOf(signals.missions()),
                    "registered terminal objectives", TerminalUi.CYAN));
            metrics.add(new TerminalAddonMetric("Routes", String.valueOf(signals.routes()),
                    "core route records", TerminalUi.GREEN));
            metrics.add(new TerminalAddonMetric("Diagnostics", String.valueOf(signals.diagnostics()),
                    "active blockers", signals.diagnostics() > 0 ? TerminalUi.AMBER : TerminalUi.MUTED));
            metrics.add(new TerminalAddonMetric("Archives", String.valueOf(signals.archives()),
                    "shared records", TerminalUi.AMBER));
            metrics.addAll(info.metrics());
            return List.copyOf(metrics);
        }

        private static List<TerminalAddonSection> addonSections(
                TerminalAddonInfo info, EchoAddonChapter chapter, TerminalRenderContext context,
                AddonSignals signals, boolean available) {
            List<TerminalAddonSection> sections = new ArrayList<>();
            sections.add(new TerminalAddonSection("Live Signal", List.of(
                    chapterStatusLine(chapter, context),
                    available
                            ? "Linked terminal pages can be opened from this info page."
                            : "Route preview only; the owning addon decides when commands become available.",
                    signals.routes() + " route record(s), " + signals.diagnostics() + " diagnostic blocker(s).")));
            sections.addAll(info.sections());
            return List.copyOf(sections);
        }

        private static List<TerminalAddonLink> addonLinks(TerminalAddonInfo info, EchoAddonChapter chapter) {
            Map<String, TerminalAddonLink> links = new LinkedHashMap<>();
            for (TerminalAddonLink link : info.links()) {
                links.putIfAbsent(link.targetTabId().toString(), link);
            }
            for (TerminalAddonLink link : inferredLinks(chapter)) {
                links.putIfAbsent(link.targetTabId().toString(), link);
            }
            return List.copyOf(links.values());
        }

        private static List<TerminalAddonLink> inferredLinks(EchoAddonChapter chapter) {
            String chapterId = chapterId(chapter);
            String modId = chapterModId(chapter);
            List<TerminalAddonLink> links = new ArrayList<>();
            for (TerminalTab tab : TerminalTabRegistry.tabs()) {
                if (tab == null || tab.descriptor() == null || tab.descriptor().id().equals(ADDONS)) {
                    continue;
                }
                TerminalNavigationProfile profile = TerminalNavigationProfiles.profileFor(tab);
                boolean related = matchesChapterKey(profile.chapterId(), chapterId, modId)
                        || tab.descriptor().id().getNamespace().equals(modId);
                if (related) {
                    TerminalTabChrome chrome = tab.chrome();
                    String label = chrome == null ? tab.descriptor().title() : chrome.shortTitle();
                    String detail = chrome == null ? "" : chrome.summary();
                    links.add(new TerminalAddonLink(tab.descriptor().id(), label, detail, tab.descriptor().accentColor()));
                }
            }
            return links.stream().limit(6).toList();
        }

        private static int drawGuideSteps(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                TerminalAddonGuide guide, int x, int y, int width) {
            if (guide == null || guide.starterSteps().isEmpty()) {
                return y;
            }
            TerminalUi.line(context, graphics, "Start Steps", x, y, width, TerminalUi.CYAN);
            TerminalUi.divider(graphics, x, y + 14, width, TerminalUi.CYAN);
            int cy = y + 22;
            for (String step : guide.starterSteps()) {
                cy = TerminalUi.wrap(context, graphics, "- " + step, x + 6, cy,
                        Math.max(40, width - 6), TerminalUi.TEXT) + 4;
            }
            return cy + 6;
        }

        private static int drawMetrics(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<TerminalAddonMetric> metrics, int x, int y, int width) {
            if (metrics.isEmpty()) {
                return y;
            }
            TerminalUi.line(context, graphics, "Metrics", x, y, width, TerminalUi.CYAN);
            int cy = y + 16;
            int columns = width >= 420 ? 2 : 1;
            int gap = 8;
            int cardW = columns == 1 ? width : Math.max(120, (width - gap) / 2);
            int rowH = 0;
            for (int i = 0; i < metrics.size(); i++) {
                TerminalAddonMetric metric = metrics.get(i);
                int column = i % columns;
                int cx = x + column * (cardW + gap);
                int cardH = TerminalUi.denseDataCard(context, graphics, cx, cy, cardW,
                        metric.label(), metric.value(), metric.detail(), metric.color());
                rowH = Math.max(rowH, cardH);
                if (column == columns - 1 || i == metrics.size() - 1) {
                    cy += rowH + gap;
                    rowH = 0;
                }
            }
            return cy;
        }

        private static int drawSections(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<TerminalAddonSection> sections, int x, int y, int width) {
            int cy = y;
            for (TerminalAddonSection section : sections) {
                if (section.lines().isEmpty()) {
                    continue;
                }
                TerminalUi.line(context, graphics, section.title(), x, cy, width, TerminalUi.CYAN);
                TerminalUi.divider(graphics, x, cy + 14, width, TerminalUi.CYAN);
                cy += 22;
                for (String line : section.lines()) {
                    cy = TerminalUi.wrap(context, graphics, "- " + line, x + 6, cy,
                            Math.max(40, width - 6), TerminalUi.TEXT) + 4;
                }
                cy += 6;
            }
            return cy;
        }

        private int drawLinks(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                TerminalAddonGuide guide, List<TerminalAddonLink> links, int x, int y, int width,
                int mouseX, int mouseY) {
            TerminalUi.line(context, graphics, "Linked Pages", x, y, width, TerminalUi.CYAN);
            TerminalUi.divider(graphics, x, y + 14, width, TerminalUi.CYAN);
            int cy = y + 22;
            if (links.isEmpty()) {
                return TerminalUi.wrap(context, graphics,
                        "No addon terminal pages are registered. Shared intel and diagnostics still appear above.",
                        x, cy, width, TerminalUi.MUTED) + 8;
            }
            for (TerminalAddonLink link : links) {
                boolean enabled = context.canNavigateToTab(link.targetTabId());
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x, cy, width, LINK_HEIGHT - 4);
                int color = enabled ? link.color() : TerminalUi.MUTED;
                TerminalUi.dataListRow(context, graphics, x, cy, width, LINK_HEIGHT - 4,
                        linkLabel(guide, link), link.detail(), enabled ? "OPEN" : "MISSING",
                        false, hovered, descriptor.accentColor(), color);
                linkHitboxes.add(new AddonLinkHitbox(x, cy, width, LINK_HEIGHT - 4, link));
                cy += LINK_HEIGHT;
            }
            return cy;
        }

        private int drawConfigSections(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String moduleId, int x, int y, int width, int mouseX, int mouseY) {
            List<EchoConfigModuleSnapshot> modules = configSnapshotsFor(moduleId);
            TerminalUi.line(context, graphics, "Addon Config", x, y, width, TerminalUi.CYAN);
            TerminalUi.divider(graphics, x, y + 14, width, TerminalUi.CYAN);
            int cy = y + 22;
            String status = TerminalConfigClientState.status();
            if (!status.isBlank()) {
                cy = TerminalUi.wrap(context, graphics, status, x, cy, width, TerminalUi.AMBER) + 6;
            }
            if (modules.isEmpty()) {
                return TerminalUi.wrap(context, graphics,
                        "No editable config published for this addon.",
                        x, cy, width, TerminalUi.MUTED) + 8;
            }
            for (EchoConfigModuleSnapshot module : modules) {
                String sideTitle = configSideTitle(module);
                TerminalUi.section(context, graphics, sideTitle, x, cy,
                        "Client Local".equals(sideTitle) ? TerminalUi.CYAN : TerminalUi.GREEN);
                cy += 18;
                for (EchoConfigCategorySnapshot category : module.categories()) {
                    TerminalUi.line(context, graphics, category.title(), x + 6, cy, width - 6, TerminalUi.MUTED);
                    cy += 14;
                    for (EchoConfigEntrySnapshot entry : category.entries()) {
                        cy = drawConfigEntry(context, graphics, entry, x + 6, cy, width - 6, mouseX, mouseY) + 5;
                    }
                    cy += 4;
                }
            }
            return cy;
        }

        private int drawConfigEntry(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                EchoConfigEntrySnapshot entry, int x, int y, int width, int mouseX, int mouseY) {
            int rowH = configEntryHeight(context, entry, width);
            boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, width, rowH);
            int color = entryColor(entry);
            boolean textEntry = textConfigKind(entry.kind());
            boolean stacked = configControlsStack(width, entry.kind());
            int controlsW = configControlsWidth(width, entry.kind());
            int buttonGap = 6;
            int buttonW = Math.max(30, (controlsW - buttonGap) / 2);
            int controlsX = stacked ? x + 8 : x + width - controlsW - 8;
            int copyW = stacked ? width - 16 : Math.max(72, controlsX - x - 18);
            int copyX = x + 8;
            List<String> badges = configBadges(entry);

            TerminalUi.dataSurfaceRow(context, graphics, x, y, width, rowH, color, hovered, false, entry.editable());
            TerminalUi.line(context, graphics, entry.label(), copyX, y + 6, copyW, entry.editable()
                    ? TerminalUi.TEXT : TerminalUi.MUTED);
            int detailY = y + 20;
            if (!badges.isEmpty()) {
                detailY = TerminalUi.statusBadgeRow(context, graphics, badges, copyX, detailY, copyW, color) + 2;
            }
            TerminalUi.wrap(context, graphics, configDescription(entry), copyX, detailY, copyW, TerminalUi.MUTED);

            if (entry.kind() == EchoConfigValueKind.BOOLEAN) {
                int buttonY = stacked ? y + rowH - 24 : y + Math.max(7, (rowH - 18) / 2);
                drawConfigButton(context, graphics, entry.value(), controlsX, buttonY, buttonW, 18, entry.editable(),
                        hovered, entryColor(entry), () -> toggleConfig(context, entry));
                drawConfigButton(context, graphics, "RESET", controlsX + buttonW + buttonGap, buttonY, buttonW, 18,
                        entry.editable(),
                        hovered, TerminalUi.AMBER, () -> resetConfig(context, entry));
            } else if (entry.kind() == EchoConfigValueKind.ENUM) {
                int buttonY = stacked ? y + rowH - 24 : y + Math.max(7, (rowH - 18) / 2);
                drawConfigButton(context, graphics, entry.value(), controlsX, buttonY, buttonW, 18, entry.editable(),
                        hovered, entryColor(entry), () -> cycleConfig(context, entry));
                drawConfigButton(context, graphics, "RESET", controlsX + buttonW + buttonGap, buttonY, buttonW, 18,
                        entry.editable(),
                        hovered, TerminalUi.AMBER, () -> resetConfig(context, entry));
            } else {
                boolean editing = configKey(entry).equals(editingConfigKey);
                String fieldText = editing ? editDraft + "_" : entry.value();
                int fieldY = stacked ? y + rowH - 48 : y + 7;
                int buttonY = stacked ? y + rowH - 24 : y + 31;
                TerminalUi.inlineValueField(context, graphics, controlsX, fieldY, controlsW, 18, fieldText,
                        color, editing, entry.editable());
                configHitboxes.add(new ConfigHitbox(controlsX, fieldY, controlsW, 18,
                        () -> beginConfigEdit(context, entry)));
                drawConfigButton(context, graphics, editing ? "SAVE" : "EDIT", controlsX, buttonY, buttonW, 18,
                        entry.editable(),
                        hovered, TerminalUi.CYAN,
                        () -> {
                            if (editing) {
                                applyConfigValue(context, entry, editDraft);
                            } else {
                                beginConfigEdit(context, entry);
                            }
                        });
                drawConfigButton(context, graphics, "RESET", controlsX + buttonW + buttonGap, buttonY, buttonW, 18,
                        entry.editable(),
                        hovered, TerminalUi.AMBER, () -> resetConfig(context, entry));
            }
            return y + rowH;
        }

        private void drawConfigButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String label, int x, int y, int width, int height, boolean enabled, boolean hovered,
                int color, Runnable action) {
            TerminalUi.compactButton(context, graphics, x, y, width, height,
                    label == null || label.isBlank() ? "-" : label.toLowerCase(java.util.Locale.ROOT),
                    color, enabled, hovered && enabled);
            configHitboxes.add(new ConfigHitbox(x, y, width, height, () -> {
                if (!enabled) {
                    context.playRejectedSound();
                    return;
                }
                action.run();
            }));
        }

        private static List<String> configBadges(EchoConfigEntrySnapshot entry) {
            List<String> badges = new ArrayList<>();
            if (!entry.editable()) {
                badges.add("OP required");
            }
            if (entry.restartRequired()) {
                badges.add("restart");
            }
            if (entry.newWorldOnly()) {
                badges.add("new chunks");
            }
            return badges;
        }

        private String configDescription(EchoConfigEntrySnapshot entry) {
            String range = !entry.minValue().isBlank() || !entry.maxValue().isBlank()
                    ? " [" + entry.minValue() + ".." + entry.maxValue() + "]"
                    : "";
            return entry.description().isBlank() ? "Default " + entry.defaultValue() + range : entry.description();
        }

        private int entryColor(EchoConfigEntrySnapshot entry) {
            return entry.side() == EchoConfigSide.CLIENT ? TerminalUi.CYAN : TerminalUi.GREEN;
        }

        private void maybeRequestCommonConfig(TerminalRenderContext context) {
            int tick = context == null || context.themeContext() == null ? 0 : context.themeContext().tick();
            if (tick - lastConfigRequestTick >= 80) {
                requestCommonConfig(context);
            }
        }

        private void requestCommonConfig(TerminalRenderContext context) {
            lastConfigRequestTick = context == null || context.themeContext() == null ? 0 : context.themeContext().tick();
            EchoNetClientActions.sendServerboundAction(new TerminalConfigActionPacket(
                    TerminalConfigActionPacket.Action.REQUEST, EchoConfigSide.COMMON, "", "", ""));
        }

        private static int addonConfigHeight(TerminalRenderContext context, String moduleId, int width) {
            List<EchoConfigModuleSnapshot> modules = configSnapshotsFor(moduleId);
            if (modules.isEmpty()) {
                return 52;
            }
            int rows = 0;
            int categories = 0;
            for (EchoConfigModuleSnapshot module : modules) {
                categories += module.categories().size() + 1;
                for (EchoConfigCategorySnapshot category : module.categories()) {
                    rows += category.entries().stream()
                            .mapToInt(entry -> configEntryHeight(context, entry, width - 6) + 5)
                            .sum();
                }
            }
            return 34 + categories * 22 + rows + (width < 320 ? 30 : 0);
        }

        private static int configEntryHeight(TerminalRenderContext context, EchoConfigEntrySnapshot entry, int width) {
            List<String> badges = configBadges(entry);
            int minimum = configEntryMinimumHeight(width, entry.kind(), !badges.isEmpty());
            boolean stacked = configControlsStack(width, entry.kind());
            int controlsW = configControlsWidth(width, entry.kind());
            int copyW = stacked ? Math.max(48, width - 16) : Math.max(72, width - controlsW - 26);
            String range = !entry.minValue().isBlank() || !entry.maxValue().isBlank()
                    ? " [" + entry.minValue() + ".." + entry.maxValue() + "]"
                    : "";
            String detail = entry.description().isBlank() ? "Default " + entry.defaultValue() + range : entry.description();
            int detailH = TerminalUi.wrappedHeight(context, detail, copyW);
            int badgeH = badges.isEmpty() ? 0 : TerminalUi.statusBadgeRowsHeight(context, badges, copyW) + 2;
            int controlsH = stacked ? (textConfigKind(entry.kind()) ? 48 : 24) : 0;
            return Math.max(minimum, 24 + badgeH + detailH + controlsH + 10);
        }

        private void beginConfigEdit(TerminalRenderContext context, EchoConfigEntrySnapshot entry) {
            if (!entry.editable()) {
                context.playRejectedSound();
                return;
            }
            editingConfigKey = configKey(entry);
            editDraft = entry.value();
            context.playCommandSound();
        }

        private void toggleConfig(TerminalRenderContext context, EchoConfigEntrySnapshot entry) {
            applyConfigValue(context, entry, String.valueOf(!Boolean.parseBoolean(entry.value())));
        }

        private void cycleConfig(TerminalRenderContext context, EchoConfigEntrySnapshot entry) {
            if (entry.options().isEmpty()) {
                context.playRejectedSound();
                return;
            }
            int index = Math.max(0, entry.options().indexOf(entry.value()));
            String next = entry.options().get((index + 1) % entry.options().size());
            applyConfigValue(context, entry, next);
        }

        private void applyConfigValue(TerminalRenderContext context, EchoConfigEntrySnapshot entry, String value) {
            if (entry.side() == EchoConfigSide.COMMON) {
                EchoNetClientActions.sendServerboundAction(new TerminalConfigActionPacket(
                        TerminalConfigActionPacket.Action.SET, EchoConfigSide.COMMON,
                        entry.moduleId(), entry.entryId(), value));
                lastConfigRequestTick = context.themeContext().tick();
            } else {
                EchoConfigApplyResult result = EchoConfigRegistry.apply(EchoConfigSide.CLIENT,
                        entry.moduleId(), entry.entryId(), value);
                if (!result.success()) {
                    context.playRejectedSound();
                    return;
                }
            }
            editingConfigKey = "";
            editDraft = "";
            context.playCommandSound();
        }

        private void resetConfig(TerminalRenderContext context, EchoConfigEntrySnapshot entry) {
            if (entry.side() == EchoConfigSide.COMMON) {
                EchoNetClientActions.sendServerboundAction(new TerminalConfigActionPacket(
                        TerminalConfigActionPacket.Action.RESET, EchoConfigSide.COMMON,
                        entry.moduleId(), entry.entryId(), ""));
                lastConfigRequestTick = context.themeContext().tick();
            } else {
                EchoConfigApplyResult result = EchoConfigRegistry.reset(EchoConfigSide.CLIENT,
                        entry.moduleId(), entry.entryId());
                if (!result.success()) {
                    context.playRejectedSound();
                    return;
                }
            }
            editingConfigKey = "";
            editDraft = "";
            context.playCommandSound();
        }

        private java.util.Optional<EchoConfigEntrySnapshot> findEditingEntry() {
            if (editingConfigKey.isBlank()) {
                return java.util.Optional.empty();
            }
            String[] parts = editingConfigKey.split("\\|", 3);
            String moduleId = parts.length >= 2 ? parts[1] : selectedChapterId;
            return configSnapshotsFor(moduleId).stream()
                    .flatMap(module -> module.categories().stream())
                    .flatMap(category -> category.entries().stream())
                    .filter(entry -> configKey(entry).equals(editingConfigKey))
                    .findFirst();
        }

        private static String configKey(EchoConfigEntrySnapshot entry) {
            return entry.side().name() + "|" + entry.moduleId() + "|" + entry.entryId();
        }

        private static String linkLabel(TerminalAddonGuide guide, TerminalAddonLink link) {
            String label = link.label();
            if (guide == null || guide.label().isBlank() || label.startsWith(guide.label())) {
                return label;
            }
            return guide.label() + " / " + label;
        }

        private static int addonDetailHeight(TerminalRenderContext context, EchoAddonChapter chapter,
                TerminalAddonInfo info, TerminalAddonGuide guide, List<TerminalAddonMetric> metrics,
                List<TerminalAddonSection> sections, List<TerminalAddonLink> links, int width) {
            if (chapter == null) {
                return 120;
            }
            int wrapWidth = Math.max(80, width - 32);
            String summary = chapterSummary(chapter);
            int columns = wrapWidth >= 420 ? 2 : 1;
            int metricRows = (metrics.size() + columns - 1) / columns;
            int guideHeight = 20;
            if (guide != null) {
                if (!guide.startHint().isBlank()) {
                    guideHeight += TerminalUi.wrappedHeight(context, guide.startHint(), wrapWidth) + 8;
                }
                if (!guide.starterSteps().isEmpty()) {
                    guideHeight += 28;
                    for (String step : guide.starterSteps()) {
                        guideHeight += TerminalUi.wrappedHeight(context, "- " + step, wrapWidth - 6) + 4;
                    }
                    guideHeight += 6;
                }
            }
            int sectionHeight = 0;
            for (TerminalAddonSection section : sections) {
                if (section.lines().isEmpty()) {
                    continue;
                }
                sectionHeight += 28;
                for (String line : section.lines()) {
                    sectionHeight += TerminalUi.wrappedHeight(context, "- " + line, wrapWidth - 6) + 4;
                }
                sectionHeight += 6;
            }
            int providerSummary = info.summary().isBlank() || info.summary().equals(summary)
                    ? 0
                    : TerminalUi.wrappedHeight(context, info.summary(), wrapWidth) + 8;
            int linkHeight = 26 + Math.max(1, links.size()) * LINK_HEIGHT;
            int configHeight = addonConfigHeight(context, chapterModId(chapter), width);
            return Math.max(220,
                    98
                            + guideHeight
                            + TerminalUi.wrappedHeight(context, summary, wrapWidth)
                            + providerSummary
                            + metricRows * 76
                            + sectionHeight
                            + linkHeight
                            + configHeight
                            + 24);
        }

        private static boolean matchesChapterKey(String value, String chapterId, String modId) {
            String normalized = cleanKey(value);
            String chapter = cleanKey(chapterId);
            String mod = cleanKey(modId);
            return !chapter.isBlank() && (normalized.equals(chapter)
                    || normalized.endsWith(":" + chapter)
                    || (!mod.isBlank() && normalized.equals(mod + ":" + chapter))
                    || (!mod.isBlank() && normalized.equals(mod)));
        }

        private static boolean matchesLooseText(String value, String chapterId, String modId) {
            String normalized = cleanKey(value).replace(' ', '_');
            String chapter = cleanKey(chapterId).replace(' ', '_');
            String mod = cleanKey(modId).replace(' ', '_');
            return (!chapter.isBlank() && normalized.contains(chapter))
                    || (!mod.isBlank() && normalized.contains(mod));
        }

        private static String cleanKey(String value) {
            return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        }

        private record AddonSignals(int missions, int routes, int diagnostics, int archives) {
            static AddonSignals empty() {
                return new AddonSignals(0, 0, 0, 0);
            }
        }

        private record AddonLinkHitbox(int x, int y, int w, int h, TerminalAddonLink link) {
        }

        private record ConfigHitbox(int x, int y, int w, int h, Runnable action) {
        }
    }

    private static final class DiagnosticSupport {
        private static List<EchoDiagnosticBlocker> diagnostics(TerminalRenderContext context) {
            Map<Identifier, EchoDiagnosticBlocker> diagnostics = new LinkedHashMap<>();
            for (EchoDiagnosticBlocker blocker : EchoCoreServices.diagnostics(context.player())) {
                diagnostics.putIfAbsent(blocker.id(), blocker);
            }
            List<TerminalMissionProvider> providers = TerminalMissionRegistry.providers();
            if (providers.isEmpty()) {
                EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                        id("diagnostic/no_mission_providers"),
                        EchoTerminal.MODID,
                        EchoDiagnosticBlocker.Severity.INFO,
                        "No mission providers linked",
                        "Terminal has no registered mission providers in the current runtime.",
                        "Install or enable an ECHO chapter that owns mission content.");
                diagnostics.putIfAbsent(blocker.id(), blocker);
            }
            for (TerminalMissionProvider provider : providers) {
                TerminalMissionChapter chapter = missionChapter(provider);
                MissionProviderRead read = readMissions(provider, context, chapter);
                if (read.failure() != null) {
                    EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                            id("diagnostic/mission_provider_failed_" + safePath(chapter.id().toString())),
                            chapter.id().toString(),
                            EchoDiagnosticBlocker.Severity.BLOCKED,
                            chapter.title() + " mission provider failed",
                            "The mission provider threw while Terminal was collecting route state.",
                            "Reload the world or check the owning chapter log for " + read.failure().getClass().getSimpleName() + ".");
                    diagnostics.putIfAbsent(blocker.id(), blocker);
                    continue;
                }
                if (read.missions().isEmpty()) {
                    EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                            id("diagnostic/mission_provider_empty_" + safePath(chapter.id().toString())),
                            chapter.id().toString(),
                            EchoDiagnosticBlocker.Severity.INFO,
                            chapter.title() + " has no missions",
                            "The provider is registered but did not publish route rows for this player context.",
                            "Open the chapter page or verify that its JSON/Java mission content is enabled.");
                    diagnostics.putIfAbsent(blocker.id(), blocker);
                }
                for (TerminalMissionDefinition mission : read.missions()) {
                    TerminalMissionSnapshot snapshot = safeSnapshot(provider, context, mission);
                    if (snapshot.status() == TerminalMissionStatus.LOCKED && !snapshot.unlockReason().isBlank()) {
                        EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                                mission.id(),
                                chapter.id().toString(),
                                EchoDiagnosticBlocker.Severity.BLOCKED,
                                mission.title(),
                                snapshot.unlockReason(),
                                snapshot.actionHint());
                        diagnostics.putIfAbsent(blocker.id(), blocker);
                    }
                }
            }
            addArchiveAndRecipeDiagnostics(context, diagnostics);
            return diagnostics.values().stream()
                    .sorted(java.util.Comparator
                            .comparingInt((EchoDiagnosticBlocker blocker) -> severityRank(blocker.severity()))
                            .thenComparing(EchoDiagnosticBlocker::chapterId)
                            .thenComparing(EchoDiagnosticBlocker::title)
                            .thenComparing(blocker -> blocker.id().toString()))
                    .toList();
        }

        private static MissionProviderRead readMissions(TerminalMissionProvider provider,
                TerminalRenderContext context, TerminalMissionChapter chapter) {
            try {
                List<TerminalMissionDefinition> missions = provider.missions(context == null ? null : context.player());
                List<TerminalMissionDefinition> safeMissions = missions == null ? List.of() : missions.stream()
                        .filter(mission -> mission != null)
                        .toList();
                return new MissionProviderRead(safeMissions, null);
            } catch (RuntimeException ignored) {
                EchoTerminal.LOGGER.warn("Terminal mission provider {} failed while collecting diagnostics.",
                        chapter.id(), ignored);
                return new MissionProviderRead(List.of(), ignored);
            }
        }

        private static void addArchiveAndRecipeDiagnostics(TerminalRenderContext context,
                Map<Identifier, EchoDiagnosticBlocker> diagnostics) {
            if (TerminalArchiveRegistry.entries().isEmpty()) {
                EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                        id("diagnostic/no_archive_entries"),
                        EchoTerminal.MODID,
                        EchoDiagnosticBlocker.Severity.INFO,
                        "No archive records linked",
                        "Terminal has no shared archive entries in the current runtime.",
                        "Enable SignalOS or an ECHO chapter that publishes archive records.");
                diagnostics.putIfAbsent(blocker.id(), blocker);
            }
            addRecipeDiagnostics(context, diagnostics, () -> com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry
                    .snapshot(context == null ? null : context.player()));
        }

        private static void addRecipeDiagnostics(TerminalRenderContext context,
                Map<Identifier, EchoDiagnosticBlocker> diagnostics,
                Supplier<com.knoxhack.echoterminal.api.recipe.TerminalRecipeSnapshot> snapshotSupplier) {
            try {
                var recipeSnapshot = snapshotSupplier.get();
                if (recipeSnapshot.providerCount() <= 0) {
                    EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                            id("diagnostic/no_recipe_providers"),
                            EchoTerminal.MODID,
                            EchoDiagnosticBlocker.Severity.INFO,
                            "No recipe providers linked",
                            "The ECHO Recipe Index has no provider-backed recipe sources.",
                            "Enable Ashfall, Industrial Nexus, Armory, or another recipe-aware ECHO addon.");
                    diagnostics.putIfAbsent(blocker.id(), blocker);
                } else if (recipeSnapshot.recipes().isEmpty()) {
                    EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                            id("diagnostic/empty_recipe_index"),
                            EchoTerminal.MODID,
                            EchoDiagnosticBlocker.Severity.WARNING,
                            "Recipe index empty",
                            recipeSnapshot.providerCount() + " provider(s) are registered, but no recipe rows were published.",
                            "Check recipe provider data loading and gated recipe visibility.");
                    diagnostics.putIfAbsent(blocker.id(), blocker);
                }
            } catch (RuntimeException | LinkageError exception) {
                EchoTerminal.LOGGER.warn("Terminal recipe index failed while collecting diagnostics.", exception);
                EchoDiagnosticBlocker blocker = new EchoDiagnosticBlocker(
                        id("diagnostic/recipe_index_failed"),
                        EchoTerminal.MODID,
                        EchoDiagnosticBlocker.Severity.BLOCKED,
                        "Recipe index failed",
                        "Terminal could not collect provider-backed recipe data.",
                        "Check the owning recipe provider logs and reload the world.");
                diagnostics.putIfAbsent(blocker.id(), blocker);
            }
        }

        private static TerminalMissionSnapshot safeSnapshot(TerminalMissionProvider provider,
                TerminalRenderContext context, TerminalMissionDefinition mission) {
            try {
                TerminalMissionSnapshot snapshot = provider.snapshot(context == null ? null : context.player(), mission.id());
                return snapshot == null ? fallbackSnapshot(mission.id(),
                        "Mission signal did not return state.", "Open the chapter tab that owns this route.") : snapshot;
            } catch (RuntimeException ignored) {
                return fallbackSnapshot(mission.id(),
                        "Mission signal failed to report state.", "Open the chapter tab that owns this route or reload the world.");
            }
        }

        private static TerminalMissionSnapshot fallbackSnapshot(Identifier missionId, String reason, String actionHint) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", reason, actionHint, List.of());
        }

        private static int severityColor(EchoDiagnosticBlocker.Severity severity) {
            return switch (severity) {
                case CRITICAL -> TerminalUi.RED;
                case BLOCKED -> TerminalUi.AMBER;
                case WARNING -> 0xFFFFE08A;
                case INFO -> TerminalUi.CYAN;
            };
        }

        private static int severityRank(EchoDiagnosticBlocker.Severity severity) {
            return switch (severity == null ? EchoDiagnosticBlocker.Severity.INFO : severity) {
                case CRITICAL -> 0;
                case BLOCKED -> 1;
                case WARNING -> 2;
                case INFO -> 3;
            };
        }

        private static String safePath(String value) {
            String cleaned = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9_./-]", "_")
                    .replace('/', '_')
                    .replace('.', '_')
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            return cleaned.isBlank() ? "provider" : cleaned;
        }

        private record MissionProviderRead(List<TerminalMissionDefinition> missions, RuntimeException failure) {
            private MissionProviderRead {
                missions = missions == null ? List.of() : List.copyOf(missions);
            }
        }
    }

    private static final class MissionGraphTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("mission_graph"), "ROUTE SOURCES", 120, 0xFF92F7A6);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Route Sources", TerminalTabChrome.GROUP_PROTOCOL, "RS",
                        "Shared route diagnostics", 120);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            List<TerminalMissionProvider> providers = graphProviders();
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w,
                    Math.max(130, Math.min(context.contentHeight(), 78
                            + Math.max(1, providers.size()) * 66)), "MISSION GRAPH",
                    providers.size() + " source(s)", descriptor.accentColor()) + 8;
            if (providers.isEmpty()) {
                TerminalUi.wrap(context, graphics, "No mission routes are linked yet.",
                        x + 14, cy, w - 28, TerminalUi.MUTED);
                return;
            }
            for (TerminalMissionProvider provider : providers) {
                List<TerminalMissionDefinition> missions = safeMissions(provider, context);
                int locked = 0;
                int active = 0;
                int complete = 0;
                for (TerminalMissionDefinition mission : missions) {
                    TerminalMissionStatus status = safeStatus(provider, context, mission);
                    if (status == TerminalMissionStatus.LOCKED || status == TerminalMissionStatus.VIEW_ONLY) {
                        locked++;
                    } else if (status == TerminalMissionStatus.CLAIMED || status == TerminalMissionStatus.COMPLETED
                            || status == TerminalMissionStatus.CLAIMABLE) {
                        complete++;
                    } else {
                        active++;
                    }
                }
                TerminalUi.flatHudPanel(context, graphics, x + 10, cy, w - 20, 58, descriptor.accentColor());
                TerminalMissionChapter chapter = missionChapter(provider);
                TerminalUi.line(context, graphics, chapter.title(), x + 22, cy + 9, w - 44, TerminalUi.TEXT);
                TerminalUi.line(context, graphics, chapter.summary(), x + 22, cy + 25, w - 44, TerminalUi.MUTED);
                TerminalUi.line(context, graphics,
                        complete + " complete / " + active + " active / " + locked + " locked",
                        x + 22, cy + 41, w - 44, TerminalUi.GREEN);
                cy += 66;
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 70 + graphProviders().size() * 66);
        }

        private static List<TerminalMissionProvider> graphProviders() {
            return TerminalMissionRegistry.providers().stream()
                    .filter(provider -> provider != null && provider != MainSurvivalQuestProvider.INSTANCE)
                    .toList();
        }

        private static List<TerminalMissionDefinition> safeMissions(TerminalMissionProvider provider,
                TerminalRenderContext context) {
            try {
                List<TerminalMissionDefinition> missions = provider.missions(context == null ? null : context.player());
                return missions == null ? List.of() : missions.stream()
                        .filter(mission -> mission != null)
                        .toList();
            } catch (RuntimeException ignored) {
                return List.of();
            }
        }

        private static TerminalMissionStatus safeStatus(TerminalMissionProvider provider,
                TerminalRenderContext context, TerminalMissionDefinition mission) {
            try {
                TerminalMissionSnapshot snapshot = provider.snapshot(context == null ? null : context.player(), mission.id());
                return snapshot == null ? TerminalMissionStatus.LOCKED : snapshot.status();
            } catch (RuntimeException ignored) {
                return TerminalMissionStatus.LOCKED;
            }
        }
    }

    private static final class VitalsTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("vitals"), "VITALS", 130, 0xFF66E8FF);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Vitals", TerminalTabChrome.GROUP_SYSTEMS, "VT",
                        "Shared hazard telemetry", 130);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(context.player());
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w,
                    Math.max(190, Math.min(context.contentHeight(), 232)),
                    "VITALS TELEMETRY", telemetry.warning() ? "WARNING" : "NOMINAL", descriptor.accentColor()) + 8;
            cy = meter(context, graphics, x + 14, cy, w - 28, "HYDRATION", telemetry.hydration(), true, TerminalUi.CYAN);
            cy = meter(context, graphics, x + 14, cy, w - 28, "RADIATION", telemetry.radiation(), false, TerminalUi.AMBER);
            cy = meter(context, graphics, x + 14, cy, w - 28, "TOXIC AIR", telemetry.toxicAir(), false, TerminalUi.RED);
            cy = meter(context, graphics, x + 14, cy, w - 28, "OXYGEN", telemetry.oxygen(), true, TerminalUi.GREEN);
            cy = meter(context, graphics, x + 14, cy, w - 28, "PRESSURE", telemetry.pressure(), true, TerminalUi.CYAN);
            cy = meter(context, graphics, x + 14, cy, w - 28, "COLD", telemetry.cold(), false, 0xFF9FD1FF);
            cy = meter(context, graphics, x + 14, cy, w - 28, "HEAT", telemetry.heat(), false, TerminalUi.AMBER);
            cy = meter(context, graphics, x + 14, cy, w - 28, "EXPOSURE", telemetry.exposure(), false, TerminalUi.RED);
            TerminalUi.wrap(context, graphics, telemetry.statusLine(), x + 14, cy + 6, w - 28,
                    telemetry.warning() ? TerminalUi.AMBER : TerminalUi.GREEN);
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 244);
        }

        private static int meter(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, String label, int value, boolean highGood, int color) {
            int danger = highGood ? 100 - value : value;
            int meterColor = danger >= 70 ? TerminalUi.RED : danger >= 40 ? TerminalUi.AMBER : color;
            TerminalUi.line(context, graphics, label, x, y, Math.max(90, w / 3), TerminalUi.MUTED);
            TerminalUi.progress(graphics, x + Math.max(96, w / 3), y + 3,
                    Math.max(70, w - Math.max(108, w / 3) - 48), 7, value / 100.0F, meterColor);
            TerminalUi.line(context, graphics, value + "%", x + w - 42, y, 42, meterColor);
            return y + 20;
        }
    }

    private static final class DataCoreStatusTab implements ClientTerminalTab {
        private static final IDataKey<String> TERMINAL_PROBE = IDataKey.string(
                Identifier.fromNamespaceAndPath("echodatacore", "system/terminal_probe"),
                DataScope.PLAYER, "offline", true);
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(DATA_CORE, "DATA CORE", 145, 0xFF7DE0A8);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Data Core", TerminalTabChrome.GROUP_SYSTEMS, "DC",
                        "Shared data service", 145);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            EchoCoreServices.registerDataKey(TERMINAL_PROBE);
            IDataService service = EchoCoreServices.dataService();
            DataServiceDiagnostics diagnostics = service.diagnostics();
            String probe = EchoCoreServices.playerData(context.player()).get(TERMINAL_PROBE);
            boolean online = diagnostics.available() && probe != null && !"offline".equalsIgnoreCase(probe);
            String backendState = diagnostics.available() ? (online ? "ONLINE" : "WAITING") : "NO-OP";
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int panelH = Math.max(240, Math.min(context.contentHeight(), 420));
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, panelH,
                    "DATA CORE", backendState, descriptor.accentColor()) + 8;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Service", service.getClass().getSimpleName(), online ? TerminalUi.GREEN : TerminalUi.MUTED) + 2;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Probe", probe == null ? "offline" : probe, online ? TerminalUi.GREEN : TerminalUi.MUTED) + 2;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Revision", Long.toString(diagnostics.revision()), descriptor.accentColor()) + 2;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Registered keys", diagnostics.registeredKeyCount() + " / synced " + diagnostics.syncedKeyCount(),
                    descriptor.accentColor()) + 2;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Metadata keys", Integer.toString(diagnostics.metadataKeyCount()), descriptor.accentColor()) + 2;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Dirty owners", Integer.toString(diagnostics.dirtyOwnerCount()), TerminalUi.AMBER) + 8;
            Map<String, Long> metadataOwners = new LinkedHashMap<>();
            for (DataKeyMetadata meta : service.allKeyMetadata().values()) {
                String owner = meta.owner().isBlank() ? meta.id().getNamespace() : meta.owner();
                metadataOwners.merge(owner + "/" + meta.scope(), 1L, Long::sum);
            }
            String ownerSummary = metadataOwners.entrySet().stream()
                    .limit(4)
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("  "));
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28,
                    "Owners", ownerSummary.isBlank() ? "No synced metadata yet." : ownerSummary,
                    descriptor.accentColor()) + 8;

            if (TerminalClientOptions.hideDebugInfo()) {
                TerminalUi.emptyState(context, graphics, x + 14, cy + 4, w - 28,
                        "Debug Data Hidden",
                        "Raw data keys and identifiers are hidden in normal mode. Disable Hide Debug Info in System > Debug / Advanced to inspect them.",
                        descriptor.accentColor());
                return;
            }

            Map<Identifier, String> playerValues = EchoCoreServices.playerData(context.player()).debugSnapshot();
            Map<Identifier, String> worldValues = context.player() == null
                    ? Map.of()
                    : EchoCoreServices.worldData(context.player().level()).debugSnapshot();
            TerminalUi.line(context, graphics, "PLAYER VALUES " + playerValues.size(),
                    x + 14, cy, w - 28, TerminalUi.GREEN);
            cy += 14;
            cy = renderDataRows(context, graphics, playerValues, x + 14, cy, w - 28, 4);
            cy += 2;
            TerminalUi.line(context, graphics, "WORLD VALUES " + worldValues.size(),
                    x + 14, cy, w - 28, TerminalUi.GREEN);
            cy += 14;
            cy = renderDataRows(context, graphics, worldValues, x + 14, cy, w - 28, 4);
            cy += 2;
            long teamMetadata = service.allKeyMetadata().values().stream()
                    .filter(meta -> meta.scope() == DataScope.TEAM)
                    .count();
            TerminalUi.line(context, graphics, "TEAM KEYS " + teamMetadata,
                    x + 14, cy, w - 28, TerminalUi.GREEN);
            cy += 14;
            TerminalUi.line(context, graphics,
                    teamMetadata == 0 ? "No team-scoped public keys registered." : "Team values appear when a synced team snapshot is present.",
                    x + 14, cy, w - 28, TerminalUi.MUTED);
            cy += 14;

            List<DataKeyMetadata> metadata = service.allKeyMetadata().values().stream()
                    .filter(meta -> meta.synced() || meta.id().getNamespace().startsWith("echo"))
                    .limit(5)
                    .toList();
            if (metadata.isEmpty()) {
                TerminalUi.wrap(context, graphics,
                        "Shared progression is waiting for DataCore or another addon to register keys.",
                        x + 14, cy, w - 28, TerminalUi.MUTED);
                return;
            }
            cy += 2;
            TerminalUi.line(context, graphics, "KEY CONTRACTS", x + 14, cy, w - 28, descriptor.accentColor());
            cy += 14;
            for (DataKeyMetadata meta : metadata) {
                TerminalUi.line(context, graphics,
                        meta.scope() + " / " + meta.kind() + " / " + meta.id()
                                + (meta.title().isBlank() ? "" : " / " + meta.title()),
                        x + 14, cy, w - 28, TerminalUi.MUTED);
                cy += 14;
            }
            if (!diagnostics.recentChanges().isEmpty()) {
                cy += 4;
                TerminalUi.line(context, graphics, "RECENT CHANGES", x + 14, cy, w - 28, descriptor.accentColor());
                cy += 14;
                for (String change : diagnostics.recentChanges().stream().limit(4).toList()) {
                    TerminalUi.line(context, graphics, change, x + 14, cy, w - 28, TerminalUi.MUTED);
                    cy += 14;
                }
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 420);
        }

        private int renderDataRows(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                Map<Identifier, String> values, int x, int y, int w, int maxRows) {
            if (values.isEmpty()) {
                TerminalUi.line(context, graphics, "No public values visible yet.", x, y, w, TerminalUi.MUTED);
                return y + 14;
            }
            int cy = y;
            for (Map.Entry<Identifier, String> entry : values.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Identifier::toString)))
                    .limit(maxRows)
                    .toList()) {
                TerminalUi.line(context, graphics, entry.getKey() + " = " + entry.getValue(),
                        x, cy, w, TerminalUi.MUTED);
                cy += 14;
            }
            if (values.size() > maxRows) {
                TerminalUi.line(context, graphics, "... " + (values.size() - maxRows) + " more",
                        x, cy, w, TerminalUi.MUTED);
                cy += 14;
            }
            return cy;
        }
    }

    private static final class RouteRecordsTab implements ClientTerminalTab {
        private static final int ROW_HEIGHT = 50;
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("route_records"), "ROUTE RECORDS", 125, 0xFF9FD1FF);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Route Records", TerminalTabChrome.GROUP_FIELD, "RR",
                        "Shared route records", 125);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            List<EchoRouteRecord> records = EchoCoreServices.routeRecords(context.player());
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w,
                    Math.max(118, Math.min(context.contentHeight(), 68 + Math.max(1, records.size()) * ROW_HEIGHT)),
                    "ROUTE RECORDS", records.size() + " route(s)", descriptor.accentColor()) + 8;
            if (records.isEmpty()) {
                TerminalUi.wrap(context, graphics,
                        "No route records are online yet. Scanner routes, recovery sites, surveys, and orbital paths appear here when chapters publish them.",
                        x + 14, cy, w - 28, TerminalUi.MUTED);
                return;
            }
            for (EchoRouteRecord record : records) {
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x + 10, cy, w - 20, ROW_HEIGHT - 6);
                int color = record.complete() ? TerminalUi.GREEN : TerminalUi.AMBER;
                TerminalUi.dataListRow(context, graphics, x + 10, cy, w - 20, ROW_HEIGHT - 6,
                        record.title(), record.dimensionHint(), record.status(),
                        false, hovered, descriptor.accentColor(), color);
                int summaryW = w < 420 ? w - 40 : w - 210;
                TerminalUi.line(context, graphics, record.category() + " / " + record.summary(),
                        x + 20, cy + 32, Math.max(80, summaryW), TerminalUi.MUTED);
                cy += ROW_HEIGHT;
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 82 + Math.max(1, EchoCoreServices.routeRecords(context.player()).size()) * ROW_HEIGHT);
        }
    }

    private static final class FactionAtlasTab implements ClientTerminalTab {
        private static final int ROW_HEIGHT = 60;
        private static final int FILTER_HEIGHT = 20;
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("faction_atlas"), "FACTIONS", 128, 0xFFB889F5);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Faction Atlas", TerminalTabChrome.GROUP_FIELD, "FX",
                        "Shared faction atlas", 128);
        private String selectedFactionId = "";
        private String filterNamespace = "all";
        private int lastListX;
        private int lastListY;
        private int lastListW;
        private int lastListH;
        private int lastFilterX;
        private int lastFilterY;
        private int lastFilterW;
        private int lastFilterH;
        private List<String> lastFilters = List.of();

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            normalizeSelection(filteredProfiles(context));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            List<EchoFactionProfile> allProfiles = EchoCoreServices.factionProfiles(context.player());
            if (allProfiles.isEmpty()) {
                TerminalUi.flatDataPanel(context, graphics, x, y, w, Math.min(140, Math.max(90, h / 4)),
                        "FACTION ATLAS", "0 linked", descriptor.accentColor());
                TerminalUi.line(context, graphics, "No faction signals are online.",
                        x + 14, y + 44, w - 28, TerminalUi.MUTED);
                return;
            }

            lastFilters = filters(allProfiles);
            int fy = drawFilters(context, graphics, mouseX, mouseY, x, y, w, lastFilters);
            List<EchoFactionProfile> profiles = filteredProfiles(allProfiles);
            normalizeSelection(profiles);

            boolean wide = w >= 650;
            int listW = wide ? Math.max(260, Math.min(410, w * 42 / 100)) : w;
            int detailX = wide ? x + listW + 14 : x;
            int boardY = fy + 8;
            int boardH = wide ? Math.max(280, Math.min(Math.max(280, h - (boardY - y) - 12),
                    Math.max(280, profiles.size() * ROW_HEIGHT + 70)))
                    : Math.max(180, profiles.size() * ROW_HEIGHT + 58);
            int detailY = wide ? boardY : boardY + boardH + 12;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;

            int cy = TerminalUi.flatDataPanel(context, graphics, x, boardY, listW - 8, boardH,
                    "FACTION ATLAS", profiles.size() + " visible / " + allProfiles.size() + " total",
                    descriptor.accentColor()) + 6;
            lastListX = x;
            lastListY = cy;
            lastListW = listW - 8;
            lastListH = Math.max(1, boardH - (cy - boardY) - 10);
            for (EchoFactionProfile profile : profiles) {
                EchoFactionDefinition definition = profile.definition();
                boolean selected = definition.id().toString().equals(selectedFactionId);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x + 10, cy, listW - 28, ROW_HEIGHT - 8);
                int color = profile.standing().accentColor();
                TerminalUi.dataListRow(context, graphics, x + 10, cy, listW - 28, ROW_HEIGHT - 8,
                        definition.displayName(), definition.route(), profile.standing().displayName(),
                        selected, hovered, descriptor.accentColor(), color);
                TerminalUi.line(context, graphics,
                        contactSummary(profile) + " / " + definition.serviceSummary(),
                        x + 20, cy + 38, listW - 42, selected ? TerminalUi.TEXT : TerminalUi.MUTED);
                cy += ROW_HEIGHT;
            }

            EchoFactionProfile selected = selectedProfile(profiles);
            if (selected != null) {
                renderDetail(context, graphics, detailX, detailY, detailW - 8,
                        wide ? boardH : detailHeight(context, selected, detailW - 8), selected);
            }
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            if (TerminalUi.inside(mouseX, mouseY, lastFilterX, lastFilterY, lastFilterW, lastFilterH)
                    && !lastFilters.isEmpty()) {
                int filterW = Math.max(72, lastFilterW / Math.max(1, lastFilters.size()));
                int index = (int) ((mouseX - lastFilterX) / filterW);
                if (index >= 0 && index < lastFilters.size()) {
                    filterNamespace = lastFilters.get(index);
                    selectedFactionId = "";
                    normalizeSelection(filteredProfiles(context));
                    return true;
                }
            }
            if (TerminalUi.inside(mouseX, mouseY, lastListX, lastListY, lastListW, lastListH)) {
                List<EchoFactionProfile> profiles = filteredProfiles(context);
                int index = (int) ((mouseY - lastListY) / ROW_HEIGHT);
                if (index >= 0 && index < profiles.size()) {
                    selectedFactionId = profiles.get(index).definition().id().toString();
                    return true;
                }
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            List<EchoFactionProfile> profiles = filteredProfiles(context);
            int w = context.contentWidth();
            boolean wide = w >= 650;
            int listW = wide ? Math.max(260, Math.min(410, w * 42 / 100)) : w;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;
            EchoFactionProfile selected = selectedProfile(profiles);
            int detail = selected == null ? 120 : detailHeight(context, selected, detailW - 8);
            int list = Math.max(190, profiles.size() * ROW_HEIGHT + 88);
            if (wide) {
                return Math.max(context.contentHeight(), FILTER_HEIGHT + 18 + Math.max(list, detail));
            }
            return Math.max(context.contentHeight(), FILTER_HEIGHT + 24 + list + detail + 16);
        }

        private int drawFilters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, int x, int y, int w, List<String> filters) {
            lastFilterX = x;
            lastFilterY = y;
            lastFilterW = w - 8;
            lastFilterH = FILTER_HEIGHT;
            int filterW = Math.max(72, (w - 8) / Math.max(1, filters.size()));
            int fx = x;
            for (String filter : filters) {
                boolean selected = filter.equals(filterNamespace);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, fx, y, filterW - 6, FILTER_HEIGHT);
                int color = selected ? descriptor.accentColor() : hovered ? TerminalUi.CYAN : TerminalUi.MUTED;
                TerminalUi.miniStatusPill(context, graphics, label(filter), fx, y + 3, filterW - 6, color, selected);
                fx += filterW;
            }
            return y + FILTER_HEIGHT;
        }

        private void renderDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, EchoFactionProfile profile) {
            EchoFactionDefinition definition = profile.definition();
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, h,
                    "FACTION DETAIL", definition.modId(), descriptor.accentColor()) + 4;
            TerminalUi.hybridIconBadge(graphics,
                    TerminalUi.themedIcon(context, TerminalIconKey.chapter(definition.modId()),
                            TerminalVisualAssets.ICON_PAGE_ROUTE_MAP),
                    TerminalIcon.WORLD,
                    x + 14, cy + 2, 42, definition.accentColor(), true);
            TerminalUi.line(context, graphics, definition.displayName(), x + 66, cy + 8,
                    w - 170, TerminalUi.TEXT);
            TerminalUi.miniStatusPill(context, graphics, profile.standing().displayName(),
                    x + w - 104, cy + 7, 84, profile.standing().accentColor(),
                    profile.standing().ordinal() >= 2);
            cy += 56;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Standing", profile.reputation() + " / " + profile.completedContracts() + " contract(s)",
                    profile.standing().accentColor());
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Contact", contactSummary(profile), profile.contacted() ? TerminalUi.GREEN : TerminalUi.MUTED);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Last Contact", lastContact(profile), profile.lastInteractionTick() > 0L ? TerminalUi.TEXT : TerminalUi.MUTED);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Route", blank(definition.route(), "Unassigned"), TerminalUi.TEXT);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Hazard", blank(definition.hazard(), "None listed"), TerminalUi.AMBER);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Prep", blank(definition.prepHint(), "Field kit"), TerminalUi.CYAN);
            cy = TerminalUi.wrap(context, graphics, definition.summary(), x + 14, cy + 4, w - 32,
                    TerminalUi.TEXT) + 8;
            TerminalUi.divider(graphics, x + 14, cy, w - 32, definition.accentColor());
            cy += 9;
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Services", blank(definition.serviceSummary(), "Dialogue only"), TerminalUi.GREEN);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Service State", serviceState(definition, profile), TerminalUi.AMBER);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "NPC Roles", definition.roles().isEmpty() ? "None"
                            : String.join(", ", definition.roles().stream().map(role -> role.displayName()).toList()),
                    TerminalUi.TEXT);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Commands", definition.actions().size() + " known"
                            + (definition.actions().stream().anyMatch(action -> action.service()) ? " / field service" : ""),
                    TerminalUi.MUTED);
            cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 32,
                    "Active Contract", profile.activeContractId().map(Identifier::toString).orElse("none"),
                    profile.activeContractId().isPresent() ? TerminalUi.AMBER : TerminalUi.MUTED);
            if (!profile.npcMemory().isBlank()) {
                cy = TerminalUi.wrap(context, graphics, "Memory: " + profile.npcMemory(),
                        x + 14, cy + 2, w - 32, TerminalUi.MUTED) + 6;
            }
            if (!definition.contracts().isEmpty()) {
                TerminalUi.line(context, graphics, "Contracts", x + 14, cy + 4, w - 32, descriptor.accentColor());
                cy += 20;
                for (var contract : definition.contracts()) {
                    String status = profile.completedContractIds().contains(contract.id()) ? "DONE"
                            : profile.activeContractId().filter(contract.id()::equals).isPresent() ? "ACTIVE"
                            : profile.reputation() >= contract.requiredReputation() ? "AVAILABLE" : "LOCKED";
                    TerminalUi.line(context, graphics, status + " / " + contract.title() + " / " + contract.route(),
                            x + 22, cy, w - 44, TerminalUi.TEXT);
                    cy = TerminalUi.wrap(context, graphics, contract.objective(), x + 22, cy + 15,
                            w - 44, TerminalUi.MUTED) + 6;
                }
            }
            if (!definition.poiAffinities().isEmpty()) {
                TerminalUi.line(context, graphics,
                        "POI Affinity: " + String.join(", ", definition.poiAffinities().stream()
                                .map(affinity -> affinity.profileId()).limit(4).toList()),
                        x + 14, cy + 4, w - 32, TerminalUi.MUTED);
            }
        }

        private List<EchoFactionProfile> filteredProfiles(TerminalRenderContext context) {
            return filteredProfiles(EchoCoreServices.factionProfiles(context.player()));
        }

        private List<EchoFactionProfile> filteredProfiles(List<EchoFactionProfile> profiles) {
            if ("all".equals(filterNamespace)) {
                return profiles;
            }
            return profiles.stream()
                    .filter(profile -> profile.definition().modId().equals(filterNamespace))
                    .toList();
        }

        private List<String> filters(List<EchoFactionProfile> profiles) {
            List<String> namespaces = new ArrayList<>();
            namespaces.add("all");
            for (String namespace : profiles.stream()
                    .map(profile -> profile.definition().modId())
                    .distinct()
                    .sorted()
                    .toList()) {
                namespaces.add(namespace);
            }
            if (!namespaces.contains(filterNamespace)) {
                filterNamespace = "all";
            }
            return List.copyOf(namespaces);
        }

        private void normalizeSelection(List<EchoFactionProfile> profiles) {
            if (profiles.stream().anyMatch(profile -> profile.definition().id().toString().equals(selectedFactionId))) {
                return;
            }
            selectedFactionId = profiles.isEmpty() ? "" : profiles.get(0).definition().id().toString();
        }

        private EchoFactionProfile selectedProfile(List<EchoFactionProfile> profiles) {
            normalizeSelection(profiles);
            return profiles.stream()
                    .filter(profile -> profile.definition().id().toString().equals(selectedFactionId))
                    .findFirst()
                    .orElse(null);
        }

        private int detailHeight(TerminalRenderContext context, EchoFactionProfile profile, int width) {
            int wrapWidth = Math.max(80, width - 32);
            int contractHeight = profile.definition().contracts().stream()
                    .mapToInt(contract -> 26 + TerminalUi.wrappedHeight(context, contract.objective(), wrapWidth - 12))
                    .sum();
            return Math.max(260,
                    210
                            + TerminalUi.wrappedHeight(context, profile.definition().summary(), wrapWidth)
                            + (profile.npcMemory().isBlank() ? 0
                                    : TerminalUi.wrappedHeight(context, "Memory: " + profile.npcMemory(), wrapWidth))
                            + contractHeight
                            + profile.definition().poiAffinities().size() * 4);
        }

        private static String label(String namespace) {
            return "all".equals(namespace) ? "ALL" : namespace.replace("echo", "").replace("_", " ").toUpperCase(java.util.Locale.ROOT);
        }

        private static String blank(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }

        private static String contactSummary(EchoFactionProfile profile) {
            if (!profile.contacted()) {
                return "UNCONTACTED";
            }
            return "CONTACTED x" + profile.contactCount();
        }

        private static String lastContact(EchoFactionProfile profile) {
            if (profile.lastInteractionTick() <= 0L) {
                return "none";
            }
            String role = profile.lastRoleId().isBlank() ? "unidentified contact" : profile.lastRoleId();
            return role + " at tick " + profile.lastInteractionTick();
        }

        private static String serviceState(EchoFactionDefinition definition, EchoFactionProfile profile) {
            return definition.actions().stream()
                    .filter(action -> action.service())
                    .findFirst()
                    .map(action -> profile.reputation() >= action.requiredReputation()
                            ? "Unlocked: " + action.label()
                            : "Locked: " + action.label() + " requires standing " + action.requiredReputation())
                    .orElse("Dialogue and contracts only");
        }
    }

    private static final class RewardInboxTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(REWARD_INBOX, "REWARD INBOX", 140, 0xFFFFD166);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Reward Inbox", TerminalTabChrome.GROUP_SYSTEMS, "RI",
                        "Shared support caches", 140);
        private int claimX;
        private int claimY;
        private int claimW;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int pending = EchoCoreServices.pendingTerminalRewardCount(context.player());
            int panelH = Math.max(132, Math.min(190, context.contentHeight() - 12));
            int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, panelH,
                    "REWARD INBOX", pending + " item(s)", descriptor.accentColor()) + 8;
            TerminalUi.wrap(context, graphics,
                    "Ashfall rewards, Orbital support caches, faction payouts, and future ECHO chapter caches can use this shared terminal inbox.",
                    x + 14, cy, w - 28, TerminalUi.TEXT);
            claimX = x + 14;
            claimY = cy + 54;
            claimW = Math.min(180, Math.max(130, w / 3));
            boolean enabled = pending > 0;
            boolean hovered = TerminalUi.inside(mouseX, mouseY, claimX, claimY, claimW, 24);
            if (enabled) {
                TerminalUi.primaryCommandButton(context, graphics, claimX, claimY, claimW, 24,
                        "CLAIM ALL", TerminalUi.themedActionIcon(context, "claim", TerminalVisualAssets.ICON_ACTION_CLAIM),
                        descriptor.accentColor(), hovered);
            } else {
                TerminalUi.miniStatusPill(context, graphics, "INBOX EMPTY", claimX, claimY + 4, claimW,
                        TerminalUi.MUTED, false);
            }
            String hint = enabled
                    ? "Support caches are stored in the nearest owned ECHO Terminal."
                    : "Complete chapter objectives to fill the inbox.";
            if (w < 420) {
                TerminalUi.wrap(context, graphics, hint, claimX, claimY + 32, w - 28, TerminalUi.MUTED);
            } else {
                TerminalUi.line(context, graphics, hint,
                        claimX + claimW + 12, claimY + 8, Math.max(80, w - claimW - 40), TerminalUi.MUTED);
            }
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button == 0 && EchoCoreServices.pendingTerminalRewardCount(context.player()) > 0
                    && TerminalUi.inside(mouseX, mouseY, claimX, claimY, claimW, 24)) {
                context.sendAction(REWARD_INBOX, CLAIM_REWARDS, "");
                return true;
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 180);
        }
    }

    private static final class ArchivesTab implements ClientTerminalTab {
        private static final int ROW_HEIGHT = 30;
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(id("archives"), "FIELD ARCHIVE", 950, 0xFF9FD1FF);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Field Archive", TerminalTabChrome.GROUP_FIELD, "FA", "Shared archive records",
                        950);
        private String selectedEntryId = "";
        private int lastListX;
        private int lastListY;
        private int lastListW;
        private int lastListH;
        private final List<ArchiveHitbox> archiveHitboxes = new ArrayList<>();
        private ArchiveVisibility visibility = ArchiveVisibility.ALL;
        private String selectedGroup = "";

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            normalizeSelection(TerminalArchiveRegistry.entries());
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            archiveHitboxes.clear();
            List<TerminalArchiveEntry> allEntries = TerminalArchiveRegistry.entries();
            TerminalUi.section(context, graphics, "SHARED ARCHIVES", x, y, descriptor.accentColor());
            if (allEntries.isEmpty()) {
                TerminalUi.line(context, graphics, "No shared archive records are available.", x, y + 18, w, TerminalUi.MUTED);
                return;
            }
            int filtersBottom = drawArchiveFilters(context, graphics, allEntries, x, y + 18, w, mouseX, mouseY);
            List<TerminalArchiveEntry> entries = filteredEntries(context, allEntries);
            if (entries.isEmpty()) {
                TerminalUi.emptyState(context, graphics, x, filtersBottom + 6, w,
                        "No Records", "No archive records match the current filter.", descriptor.accentColor());
                return;
            }
            normalizeSelection(entries);
            boolean wide = w >= 640;
            int listW = wide ? Math.max(250, Math.min(390, w * 40 / 100)) : w;
            int detailX = wide ? x + listW + 14 : x;
            int detailY = wide ? filtersBottom + 4 : filtersBottom + 10 + entries.size() * ROW_HEIGHT;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;

            int cy = filtersBottom + 4;
            lastListX = x;
            lastListY = cy;
            lastListW = listW - 8;
            lastListH = entries.size() * ROW_HEIGHT;
            for (TerminalArchiveEntry entry : entries) {
                boolean selected = entry.id().toString().equals(selectedEntryId);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x, cy, listW - 8, ROW_HEIGHT - 4);
                boolean locked = locked(context, entry);
                boolean unread = !locked && !TerminalPlayerData.get(context.player()).isArchiveRead(entry.id());
                int color = locked ? TerminalUi.MUTED : TerminalUi.TEXT;
                TerminalUi.selectableRow(context, graphics, x, cy, listW - 8, ROW_HEIGHT - 4,
                        selected, hovered, descriptor.accentColor());
                TerminalUi.line(context, graphics, (unread ? "* " : "") + entry.title(), x + 6, cy + 4, listW - 90, color);
                TerminalUi.miniStatusPill(context, graphics, entry.status(), x + listW - 78, cy + 3, 64, color, selected);
                TerminalUi.line(context, graphics, entry.group(), x + 6, cy + 17, listW - 16, TerminalUi.MUTED);
                cy += ROW_HEIGHT;
            }

            TerminalArchiveEntry selected = selectedEntry(entries);
            if (selected == null) {
                return;
            }
            int detailPanelH = archiveDetailHeight(context, selected, detailW - 8);
            if (wide) {
                detailPanelH = Math.max(detailPanelH, Math.max(150, h - (detailY - y) - 8));
            }
            boolean locked = locked(context, selected);
            int detailColor = locked ? TerminalUi.MUTED : TerminalUi.TEXT;
            int dy = TerminalUi.flatDataPanel(context, graphics,
                    detailX, detailY, detailW - 8, detailPanelH, "RECORD DETAIL", "",
                    descriptor.accentColor());
            TerminalUi.line(context, graphics, selected.title(), detailX + 8, dy,
                    detailW - 92, detailColor);
            TerminalUi.miniStatusPill(context, graphics, selected.status(), detailX + detailW - 92, dy - 2, 78,
                    selected.locked() ? TerminalUi.MUTED : TerminalUi.GREEN, false);
            dy = TerminalUi.keyValue(context, graphics, detailX + 8, dy + 22,
                    detailW - 24, "Group", selected.group(), TerminalUi.AMBER) + 4;
            TerminalUi.divider(graphics, detailX + 8, dy, detailW - 24, descriptor.accentColor());
            dy += 9;
            if (locked) {
                TerminalUi.wrap(context, graphics, "Record locked. Discover the route proof through its owning chapter.",
                        detailX + 8, dy, detailW - 24, TerminalUi.MUTED);
            } else {
                for (String line : selected.lines()) {
                    dy = TerminalUi.wrap(context, graphics, line, detailX + 8, dy, detailW - 24, TerminalUi.TEXT) + 6;
                }
            }
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (ArchiveHitbox hitbox : List.copyOf(archiveHitboxes)) {
                if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                    hitbox.action().run();
                    return true;
                }
            }
            if (!TerminalUi.inside(mouseX, mouseY, lastListX, lastListY, lastListW, lastListH)) {
                return false;
            }
            List<TerminalArchiveEntry> entries = filteredEntries(context, TerminalArchiveRegistry.entries());
            int index = (int) ((mouseY - lastListY) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                TerminalArchiveEntry entry = entries.get(index);
                selectedEntryId = entry.id().toString();
                if (!locked(context, entry)) {
                    context.sendAction(ARCHIVES, BuiltinTerminalCommonIntegration.MARK_ARCHIVE_READ,
                            entry.id().toString());
                }
                return true;
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            List<TerminalArchiveEntry> entries = filteredEntries(context, TerminalArchiveRegistry.entries());
            int w = context.contentWidth();
            boolean wide = w >= 640;
            int listW = wide ? Math.max(250, Math.min(390, w * 40 / 100)) : w;
            int detailW = wide ? Math.max(220, w - listW - 18) : w;
            TerminalArchiveEntry selected = selectedEntry(entries);
            int rows = entries.size() * ROW_HEIGHT;
            int detailHeight = selected == null ? 40 : archiveDetailHeight(context, selected, detailW - 8) + 28;
            if (wide) {
                return Math.max(context.contentHeight(), Math.max(84 + rows, detailHeight + 42));
            }
            return Math.max(context.contentHeight(), rows + detailHeight + 68);
        }

        private int drawArchiveFilters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<TerminalArchiveEntry> entries, int x, int y, int w, int mouseX, int mouseY) {
            int cy = y;
            TerminalUi.filterToolbarPanel(context, graphics, x, y - 4, w, 44, descriptor.accentColor());
            TerminalUi.line(context, graphics, "STATE", x, cy + 4, 42, TerminalUi.MUTED);
            int chipX = x + 46;
            for (ArchiveVisibility mode : ArchiveVisibility.values()) {
                int chipW = mode == ArchiveVisibility.UNREAD ? 62 : 56;
                boolean selected = visibility == mode;
                boolean hover = TerminalUi.inside(mouseX, mouseY, chipX, cy, chipW, 15);
                TerminalUi.filterChip(context, graphics, chipX, cy, chipW, mode.label(), selected, true,
                        descriptor.accentColor(), hover);
                archiveHitboxes.add(new ArchiveHitbox(chipX, cy, chipW, 15, () -> visibility = mode));
                chipX += chipW + 4;
            }
            cy += 20;
            TerminalUi.line(context, graphics, "GROUP", x, cy + 4, 42, TerminalUi.MUTED);
            chipX = x + 46;
            drawGroupChip(graphics, context, "ALL", "", chipX, cy, 50, mouseX, mouseY);
            chipX += 54;
            for (String group : archiveGroups(entries)) {
                int chipW = Math.min(112, Math.max(58, TerminalUi.wrappedHeight(context, group, 120) + group.length() * 5));
                if (chipX + chipW > x + w) {
                    break;
                }
                drawGroupChip(graphics, context, group, group, chipX, cy, chipW, mouseX, mouseY);
                chipX += chipW + 4;
            }
            return cy + 18;
        }

        private void drawGroupChip(GuiGraphicsExtractor graphics, TerminalRenderContext context, String label, String group,
                int x, int y, int w, int mouseX, int mouseY) {
            boolean selected = selectedGroup.equals(group);
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, y, w, 15);
            TerminalUi.filterChip(context, graphics, x, y, w, label, selected, true,
                    descriptor.accentColor(), hover);
            archiveHitboxes.add(new ArchiveHitbox(x, y, w, 15, () -> selectedGroup = group));
        }

        private List<TerminalArchiveEntry> filteredEntries(TerminalRenderContext context, List<TerminalArchiveEntry> entries) {
            TerminalPlayerData data = TerminalPlayerData.get(context.player());
            return entries.stream()
                    .filter(entry -> selectedGroup.isBlank() || selectedGroup.equals(entry.group()))
                    .filter(entry -> visibility.matches(context, data, entry))
                    .toList();
        }

        private static List<String> archiveGroups(List<TerminalArchiveEntry> entries) {
            return entries.stream().map(TerminalArchiveEntry::group).distinct().sorted().toList();
        }

        private void normalizeSelection(List<TerminalArchiveEntry> entries) {
            if (entries.stream().anyMatch(entry -> entry.id().toString().equals(selectedEntryId))) {
                return;
            }
            selectedEntryId = entries.isEmpty() ? "" : entries.get(0).id().toString();
        }

        private TerminalArchiveEntry selectedEntry(List<TerminalArchiveEntry> entries) {
            normalizeSelection(entries);
            return entries.stream()
                    .filter(entry -> entry.id().toString().equals(selectedEntryId))
                    .findFirst()
                    .orElse(null);
        }

        private static int archiveDetailHeight(TerminalRenderContext context, TerminalArchiveEntry entry, int width) {
            if (entry == null) {
                return 80;
            }
            int bodyHeight = 0;
            if (locked(context, entry)) {
                bodyHeight = TerminalUi.wrappedHeight(context,
                    "Record locked. Discover the route proof through its owning chapter.", width - 16);
            } else {
                for (String line : entry.lines()) {
                    bodyHeight += TerminalUi.wrappedHeight(context, line, width - 16) + 6;
                }
            }
            return Math.max(100, 58 + bodyHeight);
        }

        private static boolean locked(TerminalRenderContext context, TerminalArchiveEntry entry) {
            return entry.locked() && !EchoCoreServices.isArchiveUnlocked(context.player(), entry.id().toString());
        }

        private record ArchiveHitbox(int x, int y, int w, int h, Runnable action) {
        }

        private enum ArchiveVisibility {
            ALL("ALL"),
            OPEN("OPEN"),
            LOCKED("LOCKED"),
            UNREAD("UNREAD");

            private final String label;

            ArchiveVisibility(String label) {
                this.label = label;
            }

            String label() {
                return label;
            }

            boolean matches(TerminalRenderContext context, TerminalPlayerData data, TerminalArchiveEntry entry) {
                boolean locked = ArchivesTab.locked(context, entry);
                return switch (this) {
                    case ALL -> true;
                    case OPEN -> !locked;
                    case LOCKED -> locked;
                    case UNREAD -> !locked && !data.isArchiveRead(entry.id());
                };
            }
        }
    }
}

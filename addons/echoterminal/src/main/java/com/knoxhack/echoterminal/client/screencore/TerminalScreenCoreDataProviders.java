package com.knoxhack.echoterminal.client.screencore;

import com.knoxhack.echocore.api.DataServiceDiagnostics;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.EchoResolvedDiscoveryEntry;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionRegistry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSnapshot;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.player.TerminalPlayerData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TerminalScreenCoreDataProviders {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final String ASHFALL_STARTER_MISSION_ID = "echoashfallprotocol:secure_crash_outpost";
    private static final int SCREENCORE_RECIPE_ROW_LIMIT = 100;
    private static final int RECIPE_SNAPSHOT_CACHE_TICKS = 20;
    private static final long OVERVIEW_ROUTE_WARN_NANOS = 35_000_000L;
    private static final long OVERVIEW_ROUTE_WARN_COOLDOWN_TICKS = 200L;
    private static final int OVERVIEW_ROUTE_CACHE_TICKS = 40;
    private static final String REWARD_FALLBACK_ICON = "minecraft:chest";
    private static final String REQUIREMENT_ITEM_FALLBACK_ICON = "minecraft:chest";
    private static final String REQUIREMENT_EQUIPMENT_FALLBACK_ICON = "minecraft:shield";
    private static final String REQUIREMENT_ENTITY_FALLBACK_ICON = "minecraft:iron_sword";
    private static final String REQUIREMENT_LOCATION_FALLBACK_ICON = "minecraft:compass";
    private static final String REQUIREMENT_CUSTOM_FALLBACK_ICON = "minecraft:paper";
    private static TerminalStatusSnapshot statusSnapshot;
    private static TerminalOverviewRouteSnapshot overviewRouteSnapshot;
    private static TerminalRecipeUiSnapshot recipeUiSnapshot;
    private static long recipeUiBuildCount;
    private static long lastOverviewRouteWarnTick = Long.MIN_VALUE;

    private TerminalScreenCoreDataProviders() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoScreenRegistry.registerDataProvider("terminal", TerminalScreenCoreDataProviders::resolve);
        }
    }

    static List<TerminalTabPageCandidate> pageCandidates() {
        return TerminalScreenCoreBridge.tabs().stream()
                .map(tab -> new TerminalTabPageCandidate(tab.descriptor().id(),
                        TerminalScreenCoreBridge.pageForTab(tab.descriptor().id())))
                .toList();
    }

    public static Object resolveForTests(EchoDataContext context, String path) {
        return resolve(context == null ? EchoDataContext.empty() : context, EchoDataContext.splitPath(path));
    }

    public static void resetStateForTests() {
        state().resetForTests();
        statusSnapshot = null;
        overviewRouteSnapshot = null;
        recipeUiSnapshot = null;
        recipeUiBuildCount = 0L;
        lastOverviewRouteWarnTick = Long.MIN_VALUE;
    }

    public static long recipeUiBuildCountForTests() {
        return recipeUiBuildCount;
    }

    public static String preferredMissionActionIdForTests(List<TerminalMissionAction> actions) {
        TerminalMissionAction action = firstDisplayAction(actions);
        return action == null ? "" : action.id();
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        if (path == null || path.isEmpty()) {
            return terminalSummary(context);
        }
        return switch (path.get(0)) {
            case "activeTabId" -> activeTabId(context).toString();
            case "activeTheme" -> TerminalClientOptions.selectedThemeId().toString();
            case "activePageId" -> TerminalScreenCoreBridge.pageForTab(activeTabId(context)).toString();
            case "activeTab" -> resolveNested(activeTab(context), path, 1);
            case "navigation" -> navigation(context, path);
            case "shell" -> shell(context, path);
            case "overview" -> overview(path);
            case "diagnostics" -> diagnostics(path);
            case "missionGraph" -> missionGraph(path);
            case "missionBrowser" -> missionBrowser(context, path);
            case "addons" -> addons(path);
            case "recipeIndex" -> recipeIndex(path);
            case "routeRecords" -> routeRecords(path);
            case "discoveryGrid" -> discoveryGrid(path);
            case "factions" -> factions(path);
            case "archives" -> archives(path);
            case "vitals" -> vitals(path);
            case "rewardInbox" -> rewardInbox(path);
            case "dataCore" -> dataCore(path);
            case "scriptCore" -> scriptCore(path);
            case "settings" -> settings(path);
            case "screenCore" -> screenCore(path);
            case "fallback" -> fallback(context, path);
            default -> null;
        };
    }

    private static Map<String, Object> terminalSummary(EchoDataContext context) {
        return row("activeTabId", activeTabId(context).toString(),
                "tabCount", TerminalScreenCoreBridge.tabs().size(),
                "migrationState", TerminalScreenCoreBridge.migrationState(activeTabId(context)));
    }

    private static Identifier activeTabId(EchoDataContext context) {
        String raw = context == null ? "" : context.resolveToString("terminal.activeTabId");
        Identifier parsed = Identifier.tryParse(raw);
        return TerminalScreenCoreBridge.normalizeTab(parsed);
    }

    private static Map<String, Object> activeTab(EchoDataContext context) {
        Identifier active = activeTabId(context);
        return TerminalScreenCoreBridge.tab(active)
                .map(tab -> tabRow(tab, true))
                .orElseGet(() -> row("id", active.toString(), "title", "Terminal", "summary", "Fallback renderer"));
    }

    private static Object navigation(EchoDataContext context, List<String> path) {
        if (path.size() > 1 && ("items".equals(path.get(1)) || "sections".equals(path.get(1)))) {
            return sectionRows(context);
        }
        if (path.size() > 1 && "activeSection".equals(path.get(1))) {
            return resolveNested(activeSectionRow(context), path, 2);
        }
        if (path.size() > 1 && "activeTabs".equals(path.get(1))) {
            return activeSectionTabs(context);
        }
        if (path.size() > 1 && "groups".equals(path.get(1))) {
            return sectionRows(context);
        }
        return row("count", TerminalScreenCoreBridge.tabs().size(),
                "sectionCount", sectionRows(context).size(),
                "activeSectionId", activeSectionKey(context));
    }

    private static List<Map<String, Object>> sectionRows(EchoDataContext context) {
        Map<String, List<TerminalTab>> tabsBySection = tabsBySection();
        String activeSection = activeSectionKey(context);
        Identifier activeTab = activeTabId(context);
        return TerminalNavigationSection.storyFirstOrder().stream()
                .filter(section -> tabsBySection.containsKey(section.key()))
                .map(section -> sectionRow(section, tabsBySection.get(section.key()), activeSection, activeTab))
                .toList();
    }

    private static Map<String, Object> activeSectionRow(EchoDataContext context) {
        Map<String, List<TerminalTab>> tabsBySection = tabsBySection();
        String activeSection = activeSectionKey(context);
        TerminalNavigationSection section = TerminalNavigationSection.fromKey(activeSection);
        return sectionRow(section, tabsBySection.getOrDefault(section.key(), List.of()),
                activeSection, activeTabId(context));
    }

    private static List<Map<String, Object>> activeSectionTabs(EchoDataContext context) {
        Identifier active = activeTabId(context);
        String activeSection = activeSectionKey(context);
        return tabsBySection().getOrDefault(activeSection, List.of()).stream()
                .map(tab -> tabRow(tab, tab.descriptor().id().equals(active)))
                .toList();
    }

    private static Map<String, List<TerminalTab>> tabsBySection() {
        Map<String, List<TerminalTab>> sections = new LinkedHashMap<>();
        for (TerminalTab tab : TerminalScreenCoreBridge.tabs()) {
            TerminalNavigationProfile profile = TerminalNavigationProfiles.profileFor(tab);
            sections.computeIfAbsent(profile.section().key(), ignored -> new ArrayList<>()).add(tab);
        }
        sections.replaceAll((ignored, tabs) -> tabs.stream()
                .sorted(Comparator
                        .comparingInt((TerminalTab tab) -> TerminalNavigationProfiles.profileFor(tab).order())
                        .thenComparingInt(tab -> tab.descriptor().order())
                        .thenComparing(tab -> tab.descriptor().id().toString()))
                .toList());
        return sections;
    }

    private static Map<String, Object> sectionRow(TerminalNavigationSection section, List<TerminalTab> tabs,
            String activeSection, Identifier activeTab) {
        List<TerminalTab> safeTabs = tabs == null ? List.of() : tabs;
        boolean active = section.key().equals(activeSection);
        Identifier defaultTab = safeTabs.isEmpty() ? TerminalScreenCoreBridge.normalizeTab(null)
                : safeTabs.get(0).descriptor().id();
        return row("id", section.key(),
                "label", sectionLabel(section),
                "compactLabel", sectionCompactLabel(section),
                "icon", sectionIcon(section),
                "status", active ? "active" : "ready",
                "active", active,
                "count", safeTabs.size(),
                "countLabel", safeTabs.size() + " pages",
                "defaultTabId", defaultTab.toString(),
                "tabs", safeTabs.stream()
                        .map(tab -> tabRow(tab, tab.descriptor().id().equals(activeTab)))
                        .toList());
    }

    private static String activeSectionKey(EchoDataContext context) {
        Identifier active = activeTabId(context);
        return TerminalScreenCoreBridge.tab(active)
                .map(TerminalNavigationProfiles::profileFor)
                .map(profile -> profile.section().key())
                .orElse(TerminalNavigationSection.COMMAND.key());
    }

    private static String sectionLabel(TerminalNavigationSection section) {
        return switch (section) {
            case INDEX -> "Index";
            case HOLOMAP -> "Map";
            default -> section.label();
        };
    }

    private static String sectionCompactLabel(TerminalNavigationSection section) {
        return switch (section) {
            case COMMAND -> "Command";
            case CHAPTERS -> "Progress";
            case INTEL -> "Intel";
            case INDEX -> "Index";
            case HOLOMAP -> "Map";
            case SYSTEM -> "System";
            default -> section.label();
        };
    }

    private static String sectionIcon(TerminalNavigationSection section) {
        return switch (section) {
            case COMMAND -> "CMD";
            case CHAPTERS -> "RT";
            case INTEL -> "INT";
            case INDEX -> "IDX";
            case HOLOMAP -> "MAP";
            case SYSTEM -> "SYS";
            default -> "E7";
        };
    }

    private static Map<String, Object> tabRow(TerminalTab tab, boolean active) {
        TerminalTabChrome chrome = tab.chrome();
        TerminalNavigationProfile profile = TerminalNavigationProfiles.profileFor(tab);
        String shortTitle = chrome == null ? tab.descriptor().title() : chrome.shortTitle();
        String summary = chrome == null ? "" : chrome.summary();
        return row(
                "id", tab.descriptor().id().toString(),
                "pageId", TerminalScreenCoreBridge.pageForTab(tab.descriptor().id()).toString(),
                "title", tab.descriptor().title(),
                "shortTitle", shortTitle,
                "compactShortTitle", compactTitle(shortTitle),
                "summary", summary,
                "group", profile.section().key(),
                "groupLabel", profile.section().label(),
                "chapterId", profile.chapterId(),
                "chapterTitle", profile.chapterTitle(),
                "navSubtitle", navSubtitle(profile, summary),
                "order", tab.descriptor().order(),
                "accent", color(tab.descriptor().accentColor()),
                "active", active,
                "migrationState", TerminalScreenCoreBridge.migrationState(tab.descriptor().id()),
                "rendererLabel", rendererLabel(tab.descriptor().id()),
                "rendererStatus", rendererStatus(tab.descriptor().id()),
                "navBadge", active ? "OPEN" : rendererLabel(tab.descriptor().id()),
                "navStatus", active ? "active" : rendererStatus(tab.descriptor().id()));
    }

    private static String compactTitle(String title) {
        String clean = title == null ? "" : title.strip();
        if (clean.length() <= 10) {
            return clean.isBlank() ? "Terminal" : clean;
        }
        return switch (normalize(clean)) {
            case "command deck" -> "Command";
            case "survival route" -> "Survival";
            case "mission graph", "route sources" -> "Sources";
            case "mission browser" -> "Missions";
            case "recipe index" -> "Recipes";
            case "field archive" -> "Archive";
            case "route records" -> "Records";
            case "reward inbox" -> "Rewards";
            case "interface settings" -> "Settings";
            case "discovery grid" -> "Discover";
            case "faction atlas" -> "Factions";
            case "data core" -> "Data";
            default -> compactWords(clean);
        };
    }

    private static String compactWords(String value) {
        String[] words = value.split("\\s+");
        if (words.length >= 2) {
            String first = words[0];
            String second = words[1];
            String compact = clip(first, 8) + " " + second.charAt(0);
            if (compact.length() <= 10) {
                return compact;
            }
        }
        return clip(value, 9) + ".";
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max));
    }

    private static String navSubtitle(TerminalNavigationProfile profile, String summary) {
        if (profile.chapterTitle() != null && !profile.chapterTitle().isBlank()) {
            return profile.chapterTitle();
        }
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        return profile.section().label();
    }

    private static Object shell(EchoDataContext context, List<String> path) {
        Map<String, Object> status = shellStatus(context);
        if (path.size() > 1 && "status".equals(path.get(1))) {
            return resolveNested(status, path, 2);
        }
        return resolveNested(row("status", status), path, 1);
    }

    private static Map<String, Object> shellStatus(EchoDataContext context) {
        TerminalStatusSnapshot snapshot = statusSnapshot();
        EchoHazardTelemetry telemetry = snapshot.telemetry();
        int pending = snapshot.pendingRewards();
        int diagnosticCount = snapshot.diagnosticCount();
        int routeCount = snapshot.routeCount();
        int chapterCount = snapshot.chapterCount();
        Identifier active = activeTabId(context);
        boolean debug = TerminalClientOptions.screenCoreDebug();
        Map<String, Object> routeMission = overviewRouteSnapshot().activeMission();
        String buildFingerprint = "SC " + modVersion("echoscreencore") + " / T " + modVersion(EchoTerminal.MODID);
        return row(
                "primary", telemetry.warning() ? telemetry.statusLine() : "Field systems nominal.",
                "routeBreadcrumb", overviewMissionRouteLine(routeMission),
                "hullPercent", telemetry.warning() ? 78 : 92,
                "powerPercent", telemetry.warning() ? 63 : 86,
                "oxygenPercent", telemetry.warning() ? 42 : 88,
                "terminalId", "ECHO-7A-13",
                "sessionUptime", "03:44:27",
                "vitalsLabel", telemetry.warning() ? "VITALS WARN" : "NOMINAL",
                "vitalsStatusKey", telemetry.warning() ? "warning" : "ready",
                "rewardStatusKey", pending > 0 ? "warning" : "ready",
                "rewardsLabel", pending + " rewards",
                "diagnosticStatusKey", diagnosticCount > 0 ? "warning" : "ready",
                "diagnosticsLabel", diagnosticCount + " checks",
                "routeStatusKey", routeCount > 0 ? "ready" : "info",
                "routesLabel", routeCount + " routes",
                "chapterStatusKey", chapterCount > 0 ? "ready" : "info",
                "chaptersLabel", chapterCount + " modules",
                "rendererBuildLabel", buildFingerprint,
                "buildFingerprint", buildFingerprint,
                "rendererLine", rendererLabel(active) + " shell active; legacy controls remain available.",
                "footerLine", "ScreenCore command shell online / " + rendererLabel(active) + " / "
                        + routeCount + " route records / " + chapterCount + " modules",
                "debugLabel", debug ? "DEBUG ON" : "DEBUG OFF",
                "debugStatusKey", debug ? "warning" : "info");
    }

    private static Object overview(List<String> path) {
        TerminalStatusSnapshot snapshot = statusSnapshot();
        EchoHazardTelemetry telemetry = snapshot.telemetry();
        int pending = snapshot.pendingRewards();
        int diagnosticCount = snapshot.diagnosticCount();
        int routeCount = snapshot.routeCount();
        int chapterCount = snapshot.chapterCount();
        TerminalOverviewRouteSnapshot routeSnapshot = overviewRouteSnapshot();
        if (path.size() > 1 && "quickLinks".equals(path.get(1))) {
            return List.of(
                    quickLink("Survival Route", "Open active route guidance.", MainSurvivalQuestProvider.TAB_ID),
                    quickLink("Rewards", pending + " pending cache(s).", id("reward_inbox")),
                    quickLink("Route Records", routeCount + " route record(s).", id("route_records")),
                    quickLink("Vitals", telemetry.statusLine(), id("vitals")));
        }
        if (path.size() > 1 && "priorityCards".equals(path.get(1))) {
            return List.of(
                    quickLink("Survival Route", "Open the active route spine and current mission handoff.",
                            MainSurvivalQuestProvider.TAB_ID, "active", "ROUTE"),
                    quickLink(telemetry.warning() ? "Stabilize Vitals" : "Vitals Nominal",
                            telemetry.statusLine(), id("vitals"),
                            telemetry.warning() ? "warning" : "ready",
                            telemetry.warning() ? "WARN" : "OK"),
                    quickLink(pending > 0 ? "Claim Reward Cache" : "Reward Inbox",
                            pending > 0 ? pending + " terminal reward(s) ready." : "No pending reward caches.",
                            id("reward_inbox"), pending > 0 ? "warning" : "ready",
                            pending > 0 ? "CLAIM" : "CLEAR"),
                    quickLink("Route Records", routeCount + " shared route record(s).",
                            id("route_records"), routeCount > 0 ? "ready" : "info", "SYNC"));
        }
        if (path.size() > 1 && ("blockerCards".equals(path.get(1)) || "diagnosticCards".equals(path.get(1)))) {
            return snapshot.diagnosticRows().stream()
                    .limit(3)
                    .map(row -> quickLink(
                            String.valueOf(row.get("title")),
                            diagnosticSummary(row),
                            MainSurvivalQuestProvider.TAB_ID,
                            String.valueOf(row.get("severityKey")),
                            String.valueOf(row.get("severity"))))
                    .toList();
        }
        if (path.size() > 1 && "commandRows".equals(path.get(1))) {
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(overviewRouteCommandRow(routeSnapshot.activeMission()));
            snapshot.diagnosticRows().stream()
                    .limit(2)
                    .map(row -> quickLink(
                            String.valueOf(row.get("title")),
                            diagnosticSummary(row),
                            MainSurvivalQuestProvider.TAB_ID,
                            String.valueOf(row.get("severityKey")),
                            String.valueOf(row.get("severity"))))
                    .forEach(rows::add);
            rows.add(quickLink(telemetry.warning() ? "Stabilize Vitals" : "Vitals Nominal",
                    telemetry.statusLine(), id("vitals"),
                    telemetry.warning() ? "warning" : "ready",
                    telemetry.warning() ? "WARN" : "OK"));
            rows.add(quickLink(pending > 0 ? "Claim Reward Cache" : "Reward Inbox",
                    pending > 0 ? pending + " terminal reward(s) ready." : "No pending reward caches.",
                    id("reward_inbox"), pending > 0 ? "warning" : "ready",
                    pending > 0 ? "CLAIM" : "CLEAR"));
            rows.add(quickLink("Route Records", routeCount + " shared route record(s).",
                    id("route_records"), routeCount > 0 ? "ready" : "info", "SYNC"));
            return rows;
        }
        if (path.size() > 1 && "signalFeed".equals(path.get(1))) {
            return List.of(
                    signal("Vitals", telemetry.statusLine(),
                            telemetry.warning() ? "warning" : "ready",
                            telemetry.warning() ? "WARN" : "OK"),
                    signal("Rewards", pending + " terminal reward cache(s).",
                            pending > 0 ? "warning" : "ready",
                            pending > 0 ? "READY" : "CLEAR"),
                    signal("Diagnostics", diagnosticCount + " active diagnostic check(s).",
                            diagnosticCount > 0 ? "warning" : "ready",
                            diagnosticCount > 0 ? "REVIEW" : "CLEAR"),
                    signal("Routes", routeCount + " shared route record(s).", "info", "SYNC"),
                    signal("Modules", chapterCount + " chapter module(s) registered.", "info", "STACK"));
        }
        if (path.size() > 1 && "recentIntel".equals(path.get(1))) {
            return overviewRecentIntel(snapshot);
        }
        if (path.size() > 1 && "hazardWarnings".equals(path.get(1))) {
            return overviewHazardWarnings(snapshot);
        }
        if (path.size() > 1 && "sideOps".equals(path.get(1))) {
            return overviewSideOps(routeSnapshot.routeRows());
        }
        if (path.size() > 1 && "routeStatus".equals(path.get(1))) {
            Map<String, Object> routeStatus = overviewRouteStatus(routeSnapshot.visibleRouteRows(), snapshot);
            return resolveNested(routeStatus, path, 2);
        }
        if (path.size() > 1 && "themeOptions".equals(path.get(1))) {
            return com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.all().stream()
                    .map(theme -> row("id", theme.id().toString(), "title", theme.displayName(),
                            "selected", theme.id().equals(TerminalClientOptions.selectedThemeId())))
                    .toList();
        }
        if (path.size() > 1 && "bestNextAction".equals(path.get(1))) {
            return resolveNested(routeSnapshot.activeMission(), path, 2);
        }
        if (path.size() > 1 && simpleOverviewKey(path.get(1))) {
            return resolveNested(overviewSummary(snapshot, null), path, 1);
        }
        Map<String, Object> routeStatus = overviewRouteStatus(routeSnapshot.visibleRouteRows(), snapshot);
        Map<String, Object> summary = overviewSummary(snapshot, routeStatus);
        if (routeSnapshot.degraded()) {
            summary.put("routeSyncStatus", "SYNC LIMITED");
            summary.put("routeSyncStatusKey", "warning");
        }
        return resolveNested(summary, path, 1);
    }

    private static boolean simpleOverviewKey(String key) {
        return switch (key) {
            case "pendingRewards", "diagnostics", "routeRecords", "installedChapters",
                    "vitalsStatus", "vitalsStatusKey", "diagnosticStatus", "diagnosticStatusKey",
                    "topDiagnosticTitle", "topDiagnosticSummary", "vitalsLine" -> true;
            default -> false;
        };
    }

    private static Map<String, Object> overviewSummary(
            TerminalStatusSnapshot snapshot,
            Map<String, Object> routeStatus) {
        EchoHazardTelemetry telemetry = snapshot.telemetry();
        int pending = snapshot.pendingRewards();
        int diagnosticCount = snapshot.diagnosticCount();
        int routeCount = snapshot.routeCount();
        int chapterCount = snapshot.chapterCount();
        Map<String, Object> route = routeStatus == null
                ? row("progressPercent", 0, "routeLine", "Route 01 > Ashfall C45 > Podfall")
                : routeStatus;
        return row("pendingRewards", pending,
                "diagnostics", diagnosticCount,
                "routeRecords", routeCount,
                "installedChapters", chapterCount,
                "routeProgress", route.get("progressPercent"),
                "routeLine", route.get("routeLine"),
                "vitalsStatus", telemetry.warning() ? "WARNING" : "NOMINAL",
                "vitalsStatusKey", telemetry.warning() ? "warning" : "ready",
                "diagnosticStatus", diagnosticCount > 0 ? "REVIEW" : "CLEAR",
                "diagnosticStatusKey", diagnosticCount > 0 ? "warning" : "ready",
                "topDiagnosticTitle", snapshot.topDiagnosticTitle(),
                "topDiagnosticSummary", snapshot.topDiagnosticSummary(),
                "vitalsLine", telemetry.statusLine(),
                "routeSyncStatus", "SYNCED",
                "routeSyncStatusKey", "ready");
    }

    private static TerminalOverviewRouteSnapshot overviewRouteSnapshot() {
        Player player = player();
        int playerKey = player == null ? 0 : System.identityHashCode(player);
        int tick = player == null ? -1 : player.tickCount;
        int bucket = tick < 0 ? -1 : tick / OVERVIEW_ROUTE_CACHE_TICKS;
        String selectedId = state().selectedMissionId() == null ? "" : state().selectedMissionId().toString();
        TerminalOverviewRouteSnapshot cached = overviewRouteSnapshot;
        if (cached != null && cached.matches(playerKey, bucket, selectedId)) {
            return cached;
        }

        long start = System.nanoTime();
        try {
            List<Map<String, Object>> routeRows = survivalRouteRows();
            TerminalOverviewRouteSnapshot next = routeSnapshotFromRows(playerKey, bucket, selectedId, routeRows, false);
            long elapsed = System.nanoTime() - start;
            if (elapsed > OVERVIEW_ROUTE_WARN_NANOS) {
                logSlowOverviewRouteSnapshot(tick, elapsed);
                if (cached != null && cached.playerKey() == playerKey && cached.selectedMissionId().equals(selectedId)) {
                    return cached.withScope(bucket, true);
                }
                next = routeSnapshotFromRows(playerKey, bucket, selectedId, routeRows, true);
            }
            overviewRouteSnapshot = next;
            return next;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Command Deck route snapshot failed; using cached or fallback route data.", exception);
            if (cached != null && cached.playerKey() == playerKey && cached.selectedMissionId().equals(selectedId)) {
                TerminalOverviewRouteSnapshot fallback = cached.withScope(bucket, true);
                overviewRouteSnapshot = fallback;
                return fallback;
            }
            TerminalOverviewRouteSnapshot fallback =
                    routeSnapshotFromRows(playerKey, bucket, selectedId, List.of(), true);
            overviewRouteSnapshot = fallback;
            return fallback;
        }
    }

    private static TerminalOverviewRouteSnapshot routeSnapshotFromRows(
            int playerKey,
            int bucket,
            String selectedId,
            List<Map<String, Object>> routeRows,
            boolean degraded) {
        List<Map<String, Object>> rows = List.copyOf(routeRows == null ? List.of() : routeRows);
        List<Map<String, Object>> visibleRows = rows.stream()
                .filter(row -> !Boolean.TRUE.equals(row.get("sideCard")))
                .toList();
        return new TerminalOverviewRouteSnapshot(playerKey, bucket, selectedId, rows, visibleRows,
                overviewActiveMission(rows, visibleRows), degraded);
    }

    private static void logSlowOverviewRouteSnapshot(int tick, long elapsed) {
        long now = tick < 0 ? 0L : tick;
        if (lastOverviewRouteWarnTick != Long.MIN_VALUE
                && now - lastOverviewRouteWarnTick < OVERVIEW_ROUTE_WARN_COOLDOWN_TICKS) {
            return;
        }
        lastOverviewRouteWarnTick = now;
        EchoTerminal.LOGGER.warn("Command Deck route snapshot took {} ms; cached route data will be reused when possible.",
                elapsed / 1_000_000L);
    }

    private static List<Map<String, Object>> survivalRouteRows() {
        TerminalMissionProvider provider = MainSurvivalQuestProvider.INSTANCE;
        return safeMissions(provider).stream()
                .map(mission -> missionRow(provider, mission, MainSurvivalQuestProvider.TAB_ID))
                .toList();
    }

    private static Map<String, Object> overviewActiveMission(
            List<Map<String, Object>> allRows,
            List<Map<String, Object>> visibleRows) {
        Map<String, Object> stats = missionStats(visibleRows);
        Map<String, Object> mission = visibleRows.stream()
                .filter(TerminalScreenCoreDataProviders::ashfallMainSpineRouteRow)
                .filter(row -> !Boolean.TRUE.equals(row.get("completed")))
                .findFirst()
                .or(() -> visibleRows.stream()
                        .filter(TerminalScreenCoreDataProviders::mainRouteRow)
                        .filter(row -> !Boolean.TRUE.equals(row.get("completed")))
                        .findFirst())
                .or(() -> visibleRows.stream()
                        .filter(TerminalScreenCoreDataProviders::mainRouteRow)
                        .findFirst())
                .or(() -> visibleRows.stream().findFirst())
                .orElse(null);
        if (mission == null) {
            return row("title", "Open Survival Route",
                    "summary", "Review route objectives and mission handoff.",
                    "missionId", "",
                    "tabId", MainSurvivalQuestProvider.TAB_ID.toString(),
                    "statusKey", "active",
                    "badge", "NEXT",
                    "compactStatusLabel", "READY",
                    "statusCompactLabel", "READY",
                    "location", "Podfall",
                    "routeLine", "Route 01 > Ashfall C45 > Podfall",
                    "eta", "00:37:21",
                    "distance", "2.6 km",
                    "threat", "HIGH",
                    "threatStatusKey", "warning",
                    "progressPercent", 67,
                    "routeProgressLabel", homeRouteProgressLabel(67),
                    "rewardCountLabel", "No rewards",
                    "rewardCompactLabel", "RWD 0",
                    "rewardState", "info",
                    "rewardStateLabel", "NONE",
                    "rewardSummary", "No listed rewards",
                    "compactActionLabel", "Route",
                    "actionLabel", "Open Survival Route");
        }
        String id = String.valueOf(mission.getOrDefault("id", ""));
        String status = String.valueOf(mission.getOrDefault("status", "active"));
        String compactStatus = String.valueOf(mission.getOrDefault("statusCompactLabel", compactStatusLabel(status)));
        return row("title", overviewMissionTitle(mission),
                "summary", overviewMissionSummary(mission),
                "missionId", id,
                "tabId", MainSurvivalQuestProvider.TAB_ID.toString(),
                "statusKey", status,
                "badge", compactStatus,
                "compactStatusLabel", compactStatus,
                "statusCompactLabel", compactStatus,
                "location", overviewMissionLocation(mission),
                "routeLine", overviewMissionRouteLine(mission),
                "eta", overviewMissionEta(mission),
                "distance", overviewMissionDistance(mission),
                "threat", overviewMissionThreat(mission),
                "threatStatusKey", overviewMissionThreatStatus(mission),
                "progressPercent", mission.getOrDefault("progressPercent", 0),
                "routeProgressLabel", homeRouteProgressLabel(stats.get("progress")),
                "rewardCountLabel", mission.getOrDefault("rewardCountLabel", "No rewards"),
                "rewardCompactLabel", mission.getOrDefault("rewardCompactLabel", "RWD 0"),
                "rewardState", mission.getOrDefault("rewardState", "info"),
                "rewardStateLabel", mission.getOrDefault("rewardStateLabel", "NONE"),
                "rewardSummary", mission.getOrDefault("rewardSummary", "No listed rewards"),
                "compactActionLabel", mission.getOrDefault("primaryCommandLabel", "Open"),
                "actionLabel", "Open Survival Route");
    }

    private static boolean ashfallMainSpineRouteRow(Map<String, Object> row) {
        return mainRouteRow(row)
                && String.valueOf(row.getOrDefault("id", "")).startsWith("echoashfallprotocol:");
    }

    private static boolean mainRouteRow(Map<String, Object> row) {
        if (row == null || Boolean.TRUE.equals(row.get("sideCard"))) {
            return false;
        }
        String role = normalize(String.valueOf(row.getOrDefault("role", "main")));
        return role.isBlank() || "main".equals(role);
    }

    private static Map<String, Object> overviewRouteCommandRow(Map<String, Object> activeMission) {
        String title = String.valueOf(activeMission.getOrDefault("title", "Open Survival Route"));
        String summary = String.valueOf(activeMission.getOrDefault("summary", "Review route objectives and mission handoff."));
        String status = String.valueOf(activeMission.getOrDefault("statusKey", "active"));
        String badge = String.valueOf(activeMission.getOrDefault("statusCompactLabel",
                activeMission.getOrDefault("compactStatusLabel", "ROUTE")));
        Map<String, Object> row = quickLink("Open Survival Route", title + " - " + summary,
                MainSurvivalQuestProvider.TAB_ID, status, badge);
        String missionId = String.valueOf(activeMission.getOrDefault("missionId", ""));
        if (!missionId.isBlank()) {
            row.put("missionId", missionId);
            row.put("actionValue", missionId);
        }
        return row;
    }

    private static Map<String, Object> overviewRouteStatus(
            List<Map<String, Object>> visibleRows,
            TerminalStatusSnapshot snapshot) {
        Map<String, Object> stats = missionStats(visibleRows);
        int progress = percentNumber(stats.get("progress"));
        Map<String, Object> mission = overviewActiveMission(List.of(), visibleRows);
        return row("routeLine", overviewMissionRouteLine(mission),
                "progressPercent", progress,
                "distance", "2.6 km",
                "travel", "00:37",
                "routes", snapshot.routeCount(),
                "modules", snapshot.chapterCount(),
                "conditions", snapshot.telemetry().warning() ? "HAZARDOUS" : "STABLE",
                "conditionsStatusKey", snapshot.telemetry().warning() ? "warning" : "ready",
                "visibility", snapshot.telemetry().warning() ? "POOR" : "CLEAR",
                "visibilityStatusKey", snapshot.telemetry().warning() ? "warning" : "ready");
    }

    private static List<Map<String, Object>> overviewRecentIntel(TerminalStatusSnapshot snapshot) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        snapshot.diagnosticRows().stream()
                .limit(2)
                .map(row -> overviewLink(String.valueOf(row.get("title")),
                        diagnosticSummary(row),
                        id("data_core"),
                        String.valueOf(row.get("severityKey")),
                        String.valueOf(row.get("severity")),
                        "04:32"))
                .forEach(rows::add);
        int pending = snapshot.pendingRewards();
        rows.add(overviewLink(
                pending > 0 ? "Reward cache ready" : "Reward queue clear",
                pending > 0 ? pending + " terminal cache(s) waiting in the inbox." : "No pending reward cache.",
                id("reward_inbox"),
                pending > 0 ? "warning" : "ready",
                pending > 0 ? "CLAIM" : "CLEAR",
                "04:11"));
        rows.add(overviewLink("Route records synchronized",
                snapshot.routeCount() + " shared route record(s) available.",
                id("route_records"),
                snapshot.routeCount() > 0 ? "ready" : "info",
                "SYNC",
                "03:58"));
        rows.add(overviewLink("Podfall route signal detected",
                "Signal fragment routed through the Ashfall route bridge.",
                id("archives"),
                "warning",
                "REVIEW",
                "03:42"));
        return rows.stream().limit(5).toList();
    }

    private static List<Map<String, Object>> overviewHazardWarnings(TerminalStatusSnapshot snapshot) {
        return List.of(
                overviewLink("Ashfall storm activity increased",
                        snapshot.telemetry().statusLine(),
                        id("vitals"),
                        snapshot.telemetry().warning() ? "warning" : "ready",
                        snapshot.telemetry().warning() ? "SEVERE" : "CLEAR",
                        ""),
                overviewLink("Radiation spikes detected ahead",
                        "Route sensors recommend limited exposure near the outpost approach.",
                        id("vitals"),
                        "warning",
                        "HIGH",
                        ""),
                overviewLink("Structural instability reported",
                        "Ruined decks around crash-site salvage lanes may collapse under load.",
                        id("route_records"),
                        "warning",
                        "MEDIUM",
                        ""));
    }

    private static List<Map<String, Object>> overviewSideOps(List<Map<String, Object>> routeRows) {
        List<Map<String, Object>> sideOps = routeRows.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("sideCard")))
                .map(TerminalScreenCoreDataProviders::overviewSideOp)
                .toList();
        if (!sideOps.isEmpty()) {
            return sideOps.stream().limit(12).toList();
        }
        return List.of(overviewLink("Open Survival Route",
                "No optional side operations are currently attached to the route.",
                MainSurvivalQuestProvider.TAB_ID,
                "info",
                "ROUTE",
                ""));
    }

    private static Map<String, Object> overviewSideOp(Map<String, Object> mission) {
        String id = String.valueOf(mission.getOrDefault("id", ""));
        String compactStatus = String.valueOf(mission.getOrDefault("statusCompactLabel",
                compactStatusLabel(String.valueOf(mission.getOrDefault("status", "info")))));
        return row("id", id,
                "title", overviewMissionTitle(mission),
                "summary", overviewMissionSummary(mission),
                "missionId", id,
                "routeAnchor", mission.getOrDefault("routeAnchor", ""),
                "statusKey", mission.getOrDefault("status", "info"),
                "badge", compactStatus,
                "compactStatusLabel", compactStatus,
                "distance", overviewMissionDistance(mission),
                "reward", overviewMissionReward(mission));
    }

    private static Map<String, Object> overviewLink(
            String title,
            String summary,
            Identifier tabId,
            String statusKey,
            String badge,
            String time) {
        return row("title", title,
                "summary", summary,
                "tabId", tabId.toString(),
                "statusKey", statusKey,
                "badge", badge,
                "compactStatusLabel", compactStatusLabel(badge),
                "time", time);
    }

    private static String overviewMissionTitle(Map<String, Object> mission) {
        String id = String.valueOf(mission.getOrDefault("id", ""));
        return switch (id) {
            case ASHFALL_STARTER_MISSION_ID -> "Anchor Pod Outpost";
            case "echorelictech:arcana_relictech/find_unknown_relic",
                    "echorelictech:arcana_relictech/scan_unknown_relic",
                    "echorelictech:arcana_relictech/decode_first_relic" -> "RelicTech Data Reclamation";
            case "echoindustrialnexus:mission/reclaim_power" -> "Power Grid Node Restoration";
            case "echoashfallprotocol:first_faction_contact" -> "Industrial Nexus Supply Run";
            case "echoashfallprotocol:recover_data_log" -> "Arcana Division Field Research";
            case "echoblackboxprotocol:mission/decode_memories" -> "Blackbox Protocol: Data Purge";
            case "echoashfallprotocol:scan_first_poi" -> "RelicTech Outpost Security";
            case "echoashfallprotocol:scout_radiation_zone" -> "Ashfall Storm Watch";
            default -> String.valueOf(mission.getOrDefault("title", "Mission"));
        };
    }

    private static String overviewMissionSummary(Map<String, Object> mission) {
        String id = String.valueOf(mission.getOrDefault("id", ""));
        return switch (id) {
            case ASHFALL_STARTER_MISSION_ID ->
                    "Craft and place an Ash Campfire near the pod, then keep storage, light, and first objectives anchored at the crash site.";
            case "echorelictech:arcana_relictech/find_unknown_relic",
                    "echorelictech:arcana_relictech/scan_unknown_relic",
                    "echorelictech:arcana_relictech/decode_first_relic" ->
                    "Recover encrypted RelicTech archives and identify the first stable relic signal.";
            case "echoindustrialnexus:mission/reclaim_power" ->
                    "Bring offline grid nodes back online before the outpost route destabilizes.";
            case "echoashfallprotocol:first_faction_contact" ->
                    "Deliver critical parts to the industrial support lane and confirm live contacts.";
            case "echoashfallprotocol:recover_data_log" ->
                    "Collect etheric residue samples and cross-check the archive fragment.";
            case "echoblackboxprotocol:mission/decode_memories" ->
                    "Eliminate corrupted data clusters before they poison the route archive.";
            case "echoashfallprotocol:scan_first_poi" ->
                    "Secure perimeter scans and disable intrusions around the outpost approach.";
            case "echoashfallprotocol:scout_radiation_zone" ->
                    "Monitor storm formation in Sector 7 and keep the hazard map current.";
            default -> firstNonBlank(String.valueOf(mission.getOrDefault("nextStep", "")),
                    String.valueOf(mission.getOrDefault("subtitle", "")),
                    String.valueOf(mission.getOrDefault("summary", "")));
        };
    }

    private static String overviewMissionLocation(Map<String, Object> mission) {
        String id = String.valueOf(mission.getOrDefault("id", ""));
        if (ASHFALL_STARTER_MISSION_ID.equals(id)) {
            return "Podfall";
        }
        String title = overviewMissionTitle(mission).toLowerCase(Locale.ROOT);
        if (title.contains("relictech")) {
            return "RelicTech Outpost";
        }
        if (title.contains("power")) {
            return "Grid Node S-71";
        }
        if (title.contains("arcana")) {
            return "Arcana Sector";
        }
        if (title.contains("blackbox")) {
            return "Blackbox Cluster";
        }
        return "Ashfall C45";
    }

    private static String overviewMissionRouteLine(Map<String, Object> mission) {
        if (mission == null || mission.isEmpty()) {
            return "Route 01 > Ashfall C45 > Podfall";
        }
        String missionId = String.valueOf(mission.getOrDefault("missionId", mission.getOrDefault("id", ""))).strip();
        if (ASHFALL_STARTER_MISSION_ID.equals(missionId)) {
            return "Route 01 > Ashfall C45 > Podfall";
        }
        String existing = String.valueOf(mission.getOrDefault("routeLine", "")).strip();
        if (!existing.isBlank()) {
            return existing;
        }
        String index = String.valueOf(mission.getOrDefault("indexLabel", "")).strip();
        if (index.isBlank() || ">".equals(index)) {
            int order = Math.max(1, number(mission.getOrDefault("displayOrder", 1)));
            index = String.format(Locale.ROOT, "%02d", order);
        }
        String phase = firstNonBlank(
                String.valueOf(mission.getOrDefault("phase", "")),
                overviewMissionLocation(mission));
        if (phase.isBlank()) {
            phase = "Route";
        }
        return "Route " + index + " > Ashfall C45 > " + phase;
    }

    private static String overviewMissionEta(Map<String, Object> mission) {
        int progress = number(mission.get("progressPercent"));
        if (progress >= 80) {
            return "00:12:40";
        }
        if (progress >= 40) {
            return "00:18:42";
        }
        return "00:37:21";
    }

    private static String overviewMissionDistance(Map<String, Object> mission) {
        int order = number(mission.get("indexLabel"));
        if (order <= 0) {
            order = Math.max(1, Math.abs(String.valueOf(mission.getOrDefault("id", "")).hashCode()) % 5 + 1);
        }
        return switch (order % 5) {
            case 0 -> "1.1 km";
            case 1 -> "1.6 km";
            case 2 -> "1.8 km";
            case 3 -> "2.0 km";
            default -> "2.6 km";
        };
    }

    private static String overviewMissionThreat(Map<String, Object> mission) {
        String status = String.valueOf(mission.getOrDefault("status", ""));
        if ("locked".equals(status)) {
            return "LOCKED";
        }
        int progress = number(mission.get("progressPercent"));
        return progress < 50 ? "HIGH" : "MEDIUM";
    }

    private static String overviewMissionThreatStatus(Map<String, Object> mission) {
        return switch (overviewMissionThreat(mission)) {
            case "LOCKED" -> "locked";
            case "HIGH" -> "warning";
            default -> "info";
        };
    }

    private static String overviewMissionReward(Map<String, Object> mission) {
        String compact = String.valueOf(mission.getOrDefault("rewardCompactLabel", "")).strip();
        if (!compact.isBlank()) {
            return compact;
        }
        return "RWD " + Math.max(0, number(mission.getOrDefault("rewardCount", 0)));
    }

    private static int percentNumber(Object value) {
        String raw = String.valueOf(value == null ? "" : value).replace("%", "").strip();
        return number(raw);
    }

    private static Object diagnostics(List<String> path) {
        List<Map<String, Object>> rows = statusSnapshot().diagnosticRows().stream()
                .filter(blocker -> "all".equals(state().diagnosticsChapterFilter())
                        || state().diagnosticsChapterFilter().equals(String.valueOf(blocker.get("chapterId"))))
                .toList();
        if (path.size() > 1 && ("all".equals(path.get(1)) || "echoCoreServices".equals(path.get(1)))) {
            return rows;
        }
        return resolveNested(row("count", rows.size(), "status", rows.isEmpty() ? "CLEAR" : "REVIEW",
                "statusKey", rows.isEmpty() ? "ready" : "warning",
                "chapterFilter", state().diagnosticsChapterFilter()), path, 1);
    }

    private static TerminalStatusSnapshot statusSnapshot() {
        Player player = player();
        int playerKey = player == null ? 0 : System.identityHashCode(player);
        int tick = player == null ? -1 : player.tickCount;
        TerminalStatusSnapshot cached = statusSnapshot;
        if (cached != null && cached.playerKey() == playerKey && cached.tick() == tick) {
            return cached;
        }
        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player);
        int pendingRewards = EchoCoreServices.pendingTerminalRewardCount(player);
        List<EchoDiagnosticBlocker> diagnostics = safeList(EchoCoreServices.diagnostics(player));
        List<Map<String, Object>> diagnosticRows = diagnostics.stream()
                .map(TerminalScreenCoreDataProviders::diagnosticRow)
                .toList();
        List<EchoRouteRecord> routes = safeList(EchoCoreServices.routeRecords(player));
        int chapterCount = EchoAddonRegistry.chapters().size();
        TerminalStatusSnapshot next = new TerminalStatusSnapshot(
                playerKey, tick, telemetry, pendingRewards, diagnostics, diagnosticRows, routes, chapterCount);
        statusSnapshot = next;
        return next;
    }

    private static Map<String, Object> diagnosticRow(EchoDiagnosticBlocker blocker) {
        return row("id", blocker.id().toString(),
                "chapterId", blocker.chapterId(),
                "severity", blocker.severity().name(),
                "severityKey", statusKey(blocker.severity().name()),
                "title", blocker.title(),
                "detail", blocker.detail(),
                "nextAction", blocker.nextAction());
    }

    private static String diagnosticSummary(Map<String, Object> row) {
        String action = String.valueOf(row.getOrDefault("nextAction", ""));
        if (!action.isBlank()) {
            return action;
        }
        return String.valueOf(row.getOrDefault("detail", ""));
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Object missionGraph(List<String> path) {
        List<Map<String, Object>> providers = TerminalMissionRegistry.providers().stream()
                .filter(provider -> provider != MainSurvivalQuestProvider.INSTANCE)
                .map(TerminalScreenCoreDataProviders::providerRow)
                .toList();
        if (path.size() > 1 && ("providers".equals(path.get(1)) || "visibleProviders".equals(path.get(1)))) {
            return providers;
        }
        if (path.size() > 1 && "selectedProvider".equals(path.get(1))) {
            return resolveNested(selectedRow(providers, state().selectedMissionProviderId())
                    .orElseGet(() -> providers.isEmpty()
                            ? row("title", "No mission provider selected", "id", "",
                                    "routeUnavailable", true,
                                    "routeUnavailableReason", "No route provider is publishing objectives.",
                                    "diagnosticsUnavailable", true,
                                    "diagnosticsUnavailableReason", "No provider diagnostics are available.")
                            : providers.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedProviderId".equals(path.get(1))) {
            return state().selectedMissionProviderId();
        }
        return resolveNested(row("count", providers.size(),
                "selectedProviderId", state().selectedMissionProviderId()), path, 1);
    }

    private static Map<String, Object> providerRow(TerminalMissionProvider provider) {
        TerminalMissionChapter chapter = safeChapter(provider);
        List<TerminalMissionDefinition> missions = safeMissions(provider);
        int locked = 0;
        int active = 0;
        int complete = 0;
        for (TerminalMissionDefinition mission : missions) {
            TerminalMissionStatus status = safeSnapshot(provider, mission.id()).status();
            if (status == TerminalMissionStatus.LOCKED || status == TerminalMissionStatus.VIEW_ONLY) {
                locked++;
            } else if (status == TerminalMissionStatus.CLAIMED || status == TerminalMissionStatus.COMPLETED
                    || status == TerminalMissionStatus.CLAIMABLE) {
                complete++;
            } else {
                active++;
            }
        }
        Identifier targetTab = providerRouteTarget(chapter.id());
        return row("id", chapter.id().toString(), "title", chapter.title(), "summary", chapter.summary(),
                "targetTabId", targetTab.toString(),
                "targetPageId", TerminalScreenCoreBridge.pageForTab(targetTab).toString(),
                "routeLabel", VanillaJourneyProvider.TAB_ID.equals(targetTab) ? "Open Baseline" : "Open Survival Route",
                "diagnosticsLabel", "System Diagnostics",
                "routeUnavailable", false,
                "routeUnavailableReason", "This provider has no route page.",
                "diagnosticsUnavailable", false,
                "diagnosticsUnavailableReason", "No diagnostics are published for this provider.",
                "selected", chapter.id().toString().equals(state().selectedMissionProviderId()),
                "statusKey", active > 0 ? "active" : complete > 0 ? "ready" : "info",
                "badge", active > 0 ? active + " active" : complete + " done",
                "complete", complete, "active", active, "locked", locked,
                "statusLine", complete + " complete / " + active + " active / " + locked + " locked");
    }

    private static Object missionBrowser(EchoDataContext context, List<String> path) {
        Identifier activeTabId = activeTabId(context);
        TerminalMissionProvider provider = missionProviderFor(activeTabId);
        List<Map<String, Object>> allMissions = safeMissions(provider).stream()
                .map(mission -> missionRow(provider, mission, activeTabId))
                .toList();
        String query = normalize(state().missionSearch());
        String providerFilter = state().missionProviderFilter();
        List<Map<String, Object>> baseMissions = allMissions.stream()
                .filter(row -> !Boolean.TRUE.equals(row.get("sideCard")))
                .filter(row -> "all".equals(providerFilter)
                        || providerFilter.equals(String.valueOf(row.get("providerId")))
                        || providerFilter.equals(String.valueOf(row.get("sourceChapterId"))))
                .filter(row -> query.isBlank()
                        || normalize(String.valueOf(row.get("id"))).contains(query)
                        || normalize(String.valueOf(row.get("title"))).contains(query)
                        || normalize(String.valueOf(row.get("providerTitle"))).contains(query)
                        || normalize(String.valueOf(row.get("phase"))).contains(query)
                        || normalize(String.valueOf(row.get("statusLabel"))).contains(query))
                .toList();
        String selectedPhase = selectedPhase(baseMissions, allMissions);
        List<Map<String, Object>> visibleMissions = withDisplayOrderLabels(baseMissions.stream()
                .filter(row -> selectedPhase.isBlank() || selectedPhase.equals(phaseKey(row)))
                .toList());
        Map<String, Object> selected = selectedMissionRow(visibleMissions, allMissions);
        String selectedId = String.valueOf(selected.getOrDefault("id", ""));
        Map<String, Object> stats = missionStats(visibleMissions);
        List<Map<String, Object>> roadmapRows = roadmapRows(baseMissions, selected);
        if (path.size() > 1 && ("visibleMissions".equals(path.get(1)) || "missionTree".equals(path.get(1)))) {
            return visibleMissions;
        }
        if (path.size() > 1 && "roadmapRows".equals(path.get(1))) {
            return roadmapRows;
        }
        if (path.size() > 1 && "currentProvider".equals(path.get(1))) {
            return resolveNested(providerRow(provider), path, 2);
        }
        if (path.size() > 1 && "selectedPhase".equals(path.get(1))) {
            return resolveNested(selectedPhaseRow(roadmapRows), path, 2);
        }
        if (path.size() > 1 && "selectedMission".equals(path.get(1))) {
            return resolveNested(selected, path, 2);
        }
        if (path.size() > 1 && "selectedMissionId".equals(path.get(1))) {
            return selectedId;
        }
        long sideCardCount = allMissions.stream().filter(row -> Boolean.TRUE.equals(row.get("sideCard"))).count();
        return resolveNested(row("count", visibleMissions.size(),
                "sideCardCount", sideCardCount,
                "activeCount", stats.get("active"),
                "activeLabel", countLabel(stats.get("active"), "ACTIVE"),
                "activeCompactLabel", countLabelCompact(stats.get("active"), "Active"),
                "readyCount", stats.get("ready"),
                "readyLabel", countLabel(stats.get("ready"), "READY"),
                "readyCompactLabel", countLabelCompact(stats.get("ready"), "Ready"),
                "lockedCount", stats.get("locked"),
                "lockedLabel", countLabel(stats.get("locked"), "LOCKED"),
                "lockedCompactLabel", countLabelCompact(stats.get("locked"), "Locked"),
                "doneCount", stats.get("done"),
                "doneLabel", countLabel(stats.get("done"), "DONE"),
                "doneCompactLabel", countLabelCompact(stats.get("done"), "Done"),
                "routeProgress", stats.get("progress"),
                "routeProgressLabel", progressCompactLabel(stats.get("progress")),
                "phaseCount", roadmapRows.size(),
                "query", state().missionSearch(),
                "providerFilter", state().missionProviderFilter(),
                "legacyAvailable", true), path, 1);
    }

    private static Map<String, Object> selectedMissionRow(
            List<Map<String, Object>> visibleMissions,
            List<Map<String, Object>> allMissions) {
        String selectedId = state().selectedMissionId() == null ? "" : state().selectedMissionId().toString();
        Map<String, Object> selected = selectedRow(visibleMissions, selectedId)
                .or(() -> selectedSideCardRow(visibleMissions, allMissions, selectedId))
                .orElseGet(() -> visibleMissions.isEmpty()
                        ? row("id", "", "title", "No mission selected", "sideCards", List.of(), "requirements", List.of(),
                                "rewardRows", List.of(),
                                "rewardCount", 0,
                                "rewardCountLabel", "No rewards",
                                "rewardCompactLabel", "RWD 0",
                                "rewardState", "info",
                                "rewardStateLabel", "NONE",
                                "rewardSummary", "No listed rewards",
                                "status", "locked", "statusLabel", "SELECT", "statusCompactLabel", "SELECT",
                                "typeLabel", "MAIN MISSION",
                                "routeChip", "ROUTE",
                                "heroTexture", defaultMissionHeroTexture(),
                                "briefingTitle", "No mission selected",
                                "briefingBody", "Select a route objective to view its briefing packet.",
                                "guidanceTitle", "Next step",
                                "guidanceBody", "Choose a phase or mission row to inspect what the field team needs next.",
                                "contextBody", "No route context is selected.",
                                "nextStep", "Select a mission record.",
                                "actionReason", "No mission selected.",
                                "primaryActionId", "",
                                "primaryCommandLabel", "Map",
                                "primaryCommandMode", "map",
                                "primaryCommandEnabled", true,
                                "primaryCommandDisabled", false,
                                "primaryCommandDisabledReason", "Select a route objective first.",
                                "primaryActionDisabled", false,
                                "completeActionId", "",
                                "completeCommandDisabled", true,
                                "completeCommandDisabledReason", "Select a route objective first.",
                                "claimActionId", "",
                                "claimCommandDisabled", true,
                                "claimCommandDisabledReason", "Select a route objective first.",
                                "trackLabel", "Track",
                                "trackClear", false,
                                "trackDisabled", true,
                                "trackDisabledReason", "Select a route objective first.")
                        : visibleMissions.get(0));
        if (!selected.containsKey("sideCards")) {
            selected = withSideCards(selected, allMissions);
        }
        return selected;
    }

    private static Optional<Map<String, Object>> selectedSideCardRow(
            List<Map<String, Object>> visibleMissions,
            List<Map<String, Object>> allMissions,
            String selectedId) {
        return selectedRow(allMissions, selectedId)
                .filter(row -> Boolean.TRUE.equals(row.get("sideCard")))
                .filter(row -> {
                    String routeAnchor = String.valueOf(row.getOrDefault("routeAnchor", ""));
                    return visibleMissions.stream()
                            .anyMatch(mission -> routeAnchor.equals(String.valueOf(mission.getOrDefault("id", ""))));
                });
    }

    private static String selectedPhase(
            List<Map<String, Object>> baseMissions,
            List<Map<String, Object>> allMissions) {
        String selectedId = state().selectedMissionId() == null ? "" : state().selectedMissionId().toString();
        if (!selectedId.isBlank()) {
            Optional<Map<String, Object>> selectedBase = selectedRow(baseMissions, selectedId);
            if (selectedBase.isPresent()) {
                return phaseKey(selectedBase.get());
            }
            Optional<Map<String, Object>> selectedSideCard = selectedRow(allMissions, selectedId)
                    .filter(row -> Boolean.TRUE.equals(row.get("sideCard")));
            if (selectedSideCard.isPresent()) {
                String routeAnchor = String.valueOf(selectedSideCard.get().getOrDefault("routeAnchor", ""));
                Optional<Map<String, Object>> anchoredMission = selectedRow(baseMissions, routeAnchor);
                if (anchoredMission.isPresent()) {
                    return phaseKey(anchoredMission.get());
                }
            }
        }
        return baseMissions.isEmpty() ? "" : phaseKey(baseMissions.get(0));
    }

    private static String phaseKey(Map<String, Object> mission) {
        return String.valueOf(mission.getOrDefault("phase", "Route"));
    }

    private static Map<String, Object> missionStats(List<Map<String, Object>> visibleMissions) {
        int active = 0;
        int ready = 0;
        int locked = 0;
        int done = 0;
        int progressSum = 0;
        for (Map<String, Object> row : visibleMissions) {
            progressSum += number(row.get("progressPercent"));
            if (Boolean.TRUE.equals(row.get("completed"))) {
                done++;
            } else if (Boolean.TRUE.equals(row.get("ready"))) {
                ready++;
            } else if (Boolean.TRUE.equals(row.get("active"))) {
                active++;
            } else if (Boolean.TRUE.equals(row.get("locked"))) {
                locked++;
            }
        }
        int progress = visibleMissions.isEmpty() ? 0 : Math.round(progressSum / (float) visibleMissions.size());
        return row("active", active, "ready", ready, "locked", locked, "done", done, "progress", progress + "%");
    }

    private static String countLabel(Object value, String label) {
        return String.format(Locale.ROOT, "%02d %s", number(value), label);
    }

    private static String countLabelCompact(Object value, String label) {
        return label + " " + number(value);
    }

    private static String progressCompactLabel(Object value) {
        return "Progress " + percentNumber(value) + "%";
    }

    private static String homeRouteProgressLabel(Object value) {
        return "Route " + percentNumber(value) + "%";
    }

    private static List<Map<String, Object>> withDisplayOrderLabels(List<Map<String, Object>> missions) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>(missions.size());
        for (int index = 0; index < missions.size(); index++) {
            Map<String, Object> mission = missions.get(index);
            Map<String, Object> copy = new LinkedHashMap<>(mission);
            String displayLabel = String.format(Locale.ROOT, "%02d", index + 1);
            copy.put("sourceIndexLabel", String.valueOf(mission.getOrDefault("indexLabel", "")));
            copy.put("displayOrder", index + 1);
            copy.put("displayOrderLabel", displayLabel);
            copy.put("indexLabel", displayLabel);
            copy.put("targetMissionId", String.valueOf(mission.getOrDefault("id", "")));
            rows.add(copy);
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> roadmapRows(List<Map<String, Object>> visibleMissions, Map<String, Object> selected) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        LinkedHashMap<String, List<Map<String, Object>>> phases = new LinkedHashMap<>();
        String selectedRouteId = selectedRouteMissionId(selected);
        for (Map<String, Object> mission : visibleMissions) {
            String phase = phaseKey(mission);
            phases.computeIfAbsent(phase, ignored -> new ArrayList<>()).add(mission);
        }
        int phaseIndex = 1;
        for (Map.Entry<String, List<Map<String, Object>>> entry : phases.entrySet()) {
            List<Map<String, Object>> missions = entry.getValue();
            int complete = (int) missions.stream().filter(row -> Boolean.TRUE.equals(row.get("completed"))).count();
            int total = missions.size();
            String phaseId = "phase:" + phaseIndex + ":" + normalize(entry.getKey()).replace(' ', '_');
            String phaseStatus = complete >= total && total > 0 ? "done" : phaseHasActive(missions) ? "active" : "locked";
            Map<String, Object> targetMission = phaseTargetMission(missions);
            String targetMissionId = String.valueOf(targetMission.getOrDefault("id", ""));
            String targetTitle = String.valueOf(targetMission.getOrDefault("title", "Route step"));
            String targetStatus = String.valueOf(targetMission.getOrDefault("status", phaseStatus));
            String targetStatusLabel = String.valueOf(targetMission.getOrDefault("statusCompactLabel",
                    complete >= total ? "DONE" : phaseStatus.toUpperCase(Locale.ROOT)));
            boolean selectedPhase = phaseContainsSelected(missions, selectedRouteId);
            rows.add(row("id", phaseId,
                    "targetMissionId", targetMissionId,
                    "class", "mission-phase-row",
                    "indexLabel", String.format(Locale.ROOT, "P%02d", phaseIndex),
                    "title", entry.getKey(),
                    "subtitle", "Next: " + targetTitle,
                    "detail", complete + "/" + total + " route steps ready in this phase.",
                    "nextTitle", targetTitle,
                    "nextStatus", targetStatus,
                    "nextStatusLabel", targetStatusLabel,
                    "status", phaseStatus,
                    "statusLabel", complete + "/" + total,
                    "statusCompactLabel", complete + "/" + total,
                    "progressPercent", total == 0 ? 0 : Math.round(complete * 100.0F / total),
                    "selected", selectedPhase,
                    "tooltip", "Route phase " + entry.getKey()));
            phaseIndex++;
        }
        return List.copyOf(rows);
    }

    private static String selectedRouteMissionId(Map<String, Object> selected) {
        if (selected == null || selected.isEmpty()) {
            return "";
        }
        String routeAnchor = String.valueOf(selected.getOrDefault("routeAnchor", ""));
        if (Boolean.TRUE.equals(selected.get("sideCard")) && !routeAnchor.isBlank()) {
            return routeAnchor;
        }
        return String.valueOf(selected.getOrDefault("id", ""));
    }

    private static Map<String, Object> selectedPhaseRow(List<Map<String, Object>> roadmapRows) {
        return roadmapRows.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("selected")))
                .findFirst()
                .or(() -> roadmapRows.stream().findFirst())
                .orElseGet(() -> row("title", "Route",
                        "subtitle", "Select a route phase.",
                        "detail", "No route phases are available.",
                        "status", "info",
                        "statusCompactLabel", "ROUTE",
                        "nextStatus", "info",
                        "nextStatusLabel", "OPEN",
                        "progressPercent", 0,
                        "targetMissionId", ""));
    }

    private static boolean phaseHasActive(List<Map<String, Object>> missions) {
        return missions.stream().anyMatch(row -> Boolean.TRUE.equals(row.get("active")) || Boolean.TRUE.equals(row.get("ready")));
    }

    private static boolean phaseContainsSelected(List<Map<String, Object>> missions, String selectedId) {
        return selectedId != null && !selectedId.isBlank()
                && missions.stream().anyMatch(row -> selectedId.equals(String.valueOf(row.get("id"))));
    }

    private static Map<String, Object> phaseTargetMission(List<Map<String, Object>> missions) {
        return missions.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("ready")) || Boolean.TRUE.equals(row.get("active")))
                .findFirst()
                .or(() -> missions.stream().filter(row -> !Boolean.TRUE.equals(row.get("locked"))).findFirst())
                .orElseGet(() -> missions.isEmpty() ? Map.of() : missions.get(0));
    }

    private static Map<String, Object> missionRow(
            TerminalMissionProvider provider,
            TerminalMissionDefinition mission,
            Identifier activeTabId) {
        TerminalMissionSnapshot snapshot = safeSnapshot(provider, mission.id());
        TerminalMissionRole role = safeRole(provider, mission, snapshot);
        Identifier routeAnchor = safeRouteAnchor(provider, mission, snapshot, role);
        List<TerminalMissionIntelUnlock> intelUnlocks = safeIntelUnlocks(provider, mission, snapshot, role);
        TerminalMissionAction primaryAction = firstDisplayAction(snapshot.actions());
        TerminalMissionAction completeAction = firstEnabledActionMatching(snapshot.actions(),
                TerminalScreenCoreDataProviders::isCompleteAction);
        TerminalMissionAction claimAction = firstEnabledActionMatching(snapshot.actions(),
                TerminalScreenCoreDataProviders::isClaimAction);
        boolean sideCard = routeAnchor != null;
        TerminalMissionChapter providerChapter = safeChapter(provider);
        TerminalMissionChapter sourceChapter = sourceChapterForMission(provider, mission).orElse(providerChapter);
        boolean tracked = TerminalPlayerData.get(player()).isTracking(activeTabId, mission.id());
        String sourceLine = sourceLine(providerChapter, sourceChapter);
        String typeLabel = missionTypeLabel(role, sideCard);
        String routeChip = mission.phaseTitle().isBlank() ? "ROUTE" : mission.phaseTitle().toUpperCase(Locale.ROOT);
        String actionReason = actionReason(snapshot, primaryAction);
        boolean completed = snapshot.status() == TerminalMissionStatus.COMPLETED
                || snapshot.status() == TerminalMissionStatus.CLAIMED;
        String displayTitle = routeMissionTitle(mission.id(), mission.title());
        String displayBriefing = routeMissionBriefing(mission.id(), mission.briefing());
        String statusCompactLabel = statusCompactLabel(snapshot.status());
        String primaryCommandMode = primaryCommandMode(snapshot, primaryAction);
        List<Map<String, Object>> rewardRows = missionRewardRows(mission.rewards(), snapshot);
        String requirementSummary = mission.requirements().isEmpty()
                ? "No explicit checklist"
                : mission.requirements().size() + " requirement(s)";
        String rewardSummary = missionRewardSummary(mission.rewards(), snapshot);
        int progressPercent = Math.round(snapshot.progress() * 100.0F);
        return row("id", mission.id().toString(),
                "indexLabel", mission.missionOrder() <= 0 ? ">" : String.format(Locale.ROOT, "%02d", mission.missionOrder()),
                "title", displayTitle,
                "subtitle", displayBriefing,
                "sourceLine", sourceLine,
                "providerId", providerChapter.id().toString(),
                "providerTitle", providerChapter.title(),
                "sourceChapterId", sourceChapter.id().toString(),
                "sourceTitle", sourceChapter.title(),
                "chapter", mission.chapterId().toString(),
                "phase", mission.phaseTitle(),
                "routeChip", routeChip,
                "heroTexture", missionHeroTexture(mission),
                "typeLabel", typeLabel,
                "role", role.name().toLowerCase(Locale.ROOT),
                "routeAnchor", routeAnchor == null ? "" : routeAnchor.toString(),
                "sideCard", sideCard,
                "sideCardStatus", sideCard ? sideCardStatus(snapshot) : "",
                "statusCompactLabel", statusCompactLabel,
                "sideCardProgress", sideCard ? sideCardProgress(mission, snapshot) : "",
                "sideCardActionLabel", primaryAction == null ? "VIEW" : primaryAction.label(),
                "sideCardActionEnabled", sideCard && primaryAction != null,
                "primaryActionId", primaryAction == null ? "" : primaryAction.id(),
                "primaryActionLabel", primaryAction == null ? primaryCommandLabel(snapshot, null) : primaryAction.label(),
                "primaryActionEnabled", primaryAction != null && primaryAction.enabled(),
                "primaryActionDisabled", false,
                "primaryCommandLabel", primaryCommandLabel(snapshot, primaryAction),
                "primaryCommandMode", primaryCommandMode,
                "primaryCommandEnabled", true,
                "primaryCommandDisabled", false,
                "primaryCommandDisabledReason", actionReason,
                "completeActionId", completeAction == null ? "" : completeAction.id(),
                "completeCommandDisabled", completeAction == null,
                "completeCommandDisabledReason", completeCommandDisabledReason(snapshot, completeAction),
                "claimActionId", claimAction == null ? "" : claimAction.id(),
                "claimCommandDisabled", claimAction == null,
                "claimCommandDisabledReason", claimCommandDisabledReason(snapshot, claimAction, mission.rewards()),
                "actionReason", actionReason,
                "intelUnlockCount", intelUnlocks.size(),
                "intelUnlocks", intelUnlocks.stream().map(TerminalScreenCoreDataProviders::intelUnlockRow).toList(),
                "status", statusKey(snapshot.status().name()),
                "statusLabel", snapshot.statusLabel(),
                "progressPercent", progressPercent,
                "tracked", tracked,
                "trackLabel", tracked ? "Untrack" : "Track",
                "trackClear", tracked,
                "trackDisabled", false,
                "trackDisabledReason", tracked ? "This mission is already tracked." : "Mission tracking is unavailable.",
                "active", snapshot.status() == TerminalMissionStatus.UNLOCKED,
                "ready", snapshot.status() == TerminalMissionStatus.CLAIMABLE,
                "locked", snapshot.status() == TerminalMissionStatus.LOCKED,
                "completed", completed,
                "unlockHint", snapshot.unlockReason(),
                "nextStep", nextStep(snapshot, primaryAction),
                "briefingTitle", displayTitle,
                "briefingBody", missionBriefingBody(displayBriefing),
                "guidanceTitle", guidanceTitle(snapshot.status()),
                "guidanceBody", guidanceBody(snapshot, primaryAction, actionReason, requirementSummary),
                "contextBody", missionContextBody(mission, providerChapter, sourceChapter, typeLabel, progressPercent,
                        rewardSummary),
                "requirements", mission.requirements().stream()
                        .map(TerminalScreenCoreDataProviders::requirementRow)
                        .toList(),
                "rewardRows", rewardRows,
                "rewardCount", mission.rewards().size(),
                "rewardCountLabel", rewardCountLabel(mission.rewards()),
                "rewardCompactLabel", rewardCompactLabel(mission.rewards()),
                "rewardState", rewardStateKey(mission.rewards(), snapshot),
                "rewardStateLabel", rewardStateLabel(mission.rewards(), snapshot),
                "rewardSummary", rewardSummary,
                "requirementSummary", requirementSummary);
    }

    private static String missionBriefingBody(String briefing) {
        if (briefing == null || briefing.isBlank()) {
            return "No briefing packet is available for this objective.";
        }
        return briefing;
    }

    private static String guidanceTitle(TerminalMissionStatus status) {
        return switch (status == null ? TerminalMissionStatus.LOCKED : status) {
            case LOCKED, VIEW_ONLY -> "Locked";
            case CLAIMABLE -> "Ready";
            case COMPLETED, CLAIMED -> "Complete";
            case UNLOCKED -> "Next step";
        };
    }

    private static String guidanceBody(
            TerminalMissionSnapshot snapshot,
            TerminalMissionAction action,
            String actionReason,
            String requirementSummary) {
        if (snapshot != null && !snapshot.actionHint().isBlank()) {
            return snapshot.actionHint();
        }
        if (snapshot != null && !snapshot.unlockReason().isBlank()) {
            return snapshot.unlockReason();
        }
        if (action != null && action.enabled() && !action.label().isBlank()) {
            return "Use " + action.label() + " when the field objective is ready.";
        }
        if (actionReason != null && !actionReason.isBlank()) {
            return actionReason;
        }
        if (requirementSummary != null && !requirementSummary.isBlank()) {
            return "Requirement status: " + requirementSummary + ".";
        }
        return "Track this objective to pin it on the Command Deck.";
    }

    private static String missionContextBody(
            TerminalMissionDefinition mission,
            TerminalMissionChapter providerChapter,
            TerminalMissionChapter sourceChapter,
            String typeLabel,
            int progressPercent,
            String rewardSummary) {
        String phase = mission == null || mission.phaseTitle().isBlank() ? "Route" : mission.phaseTitle();
        String source = sourceLine(providerChapter, sourceChapter);
        String type = typeLabel == null || typeLabel.isBlank() ? "Mission" : typeLabel;
        String rewards = rewardSummary == null || rewardSummary.isBlank() ? "No listed rewards" : rewardSummary;
        return phase + " - " + type + " - " + source + " - " + progressPercent + "% complete - " + rewards + ".";
    }

    private static String routeMissionTitle(Identifier missionId, String fallback) {
        return routeMissionTitle(missionId == null ? "" : missionId.toString(), fallback);
    }

    private static String routeMissionTitle(String missionId, String fallback) {
        return switch (missionId) {
            case ASHFALL_STARTER_MISSION_ID -> "Anchor Pod Outpost";
            case "echorelictech:arcana_relictech/find_unknown_relic",
                    "echorelictech:arcana_relictech/scan_unknown_relic",
                    "echorelictech:arcana_relictech/decode_first_relic" -> "RelicTech Data Reclamation";
            case "echoindustrialnexus:mission/reclaim_power" -> "Power Grid Node Restoration";
            case "echoashfallprotocol:first_faction_contact" -> "Industrial Nexus Supply Run";
            case "echoashfallprotocol:recover_data_log" -> "Arcana Division Field Research";
            case "echoblackboxprotocol:mission/decode_memories" -> "Blackbox Protocol: Data Purge";
            case "echoashfallprotocol:scan_first_poi" -> "RelicTech Outpost Security";
            case "echoashfallprotocol:scout_radiation_zone" -> "Ashfall Storm Watch";
            default -> fallback == null || fallback.isBlank() ? "Mission" : fallback;
        };
    }

    private static String routeMissionBriefing(Identifier missionId, String fallback) {
        String id = missionId == null ? "" : missionId.toString();
        return switch (id) {
            case ASHFALL_STARTER_MISSION_ID ->
                    "Craft and place an Ash Campfire near the pod, then keep storage, light, and first objectives anchored at the crash site.";
            case "echorelictech:arcana_relictech/find_unknown_relic",
                    "echorelictech:arcana_relictech/scan_unknown_relic",
                    "echorelictech:arcana_relictech/decode_first_relic" ->
                    "Recover encrypted RelicTech archives and stabilize the first relic signal.";
            case "echoindustrialnexus:mission/reclaim_power" ->
                    "Bring offline grid nodes back online before the outpost route destabilizes.";
            case "echoashfallprotocol:first_faction_contact" ->
                    "Deliver critical parts to the industrial support lane and confirm live contacts.";
            case "echoashfallprotocol:recover_data_log" ->
                    "Collect etheric residue samples and cross-check the archive fragment.";
            case "echoblackboxprotocol:mission/decode_memories" ->
                    "Purge corrupted data clusters before they poison the route archive.";
            case "echoashfallprotocol:scan_first_poi" ->
                    "Secure perimeter scans and disable intrusions around the outpost approach.";
            case "echoashfallprotocol:scout_radiation_zone" ->
                    "Monitor storm formation in Sector 7 and keep the hazard map current.";
            default -> fallback == null ? "" : fallback;
        };
    }

    private static String missionHeroTexture(TerminalMissionDefinition mission) {
        return defaultMissionHeroTexture();
    }

    private static String defaultMissionHeroTexture() {
        return "echoterminal:textures/gui/cyberglass/hero_podfall.png";
    }

    private static Map<String, Object> withSideCards(
            Map<String, Object> selected,
            List<Map<String, Object>> allMissions) {
        Map<String, Object> copy = new LinkedHashMap<>(selected);
        String selectedId = String.valueOf(selected.getOrDefault("id", ""));
        List<Map<String, Object>> sideCards = allMissions.stream()
                .filter(row -> selectedId.equals(row.get("routeAnchor")))
                .toList();
        copy.put("sideCards", sideCards);
        copy.put("sideCardCount", sideCards.size());
        return copy;
    }

    private static Map<String, Object> intelUnlockRow(TerminalMissionIntelUnlock unlock) {
        return row("kind", unlock.kind().name().toLowerCase(Locale.ROOT),
                "id", unlock.id().toString(),
                "title", unlock.title(),
                "summary", unlock.summary());
    }

    private static String sourceLine(TerminalMissionChapter providerChapter, TerminalMissionChapter sourceChapter) {
        String provider = providerChapter == null ? "MissionCore" : providerChapter.title();
        String source = sourceChapter == null ? "" : sourceChapter.title();
        if (source.isBlank() || provider.equals(source)) {
            return provider;
        }
        return provider + " / " + source;
    }

    private static String missionTypeLabel(TerminalMissionRole role, boolean sideCard) {
        if (sideCard) {
            return "SIDE OPS";
        }
        return switch (role == null ? TerminalMissionRole.MAIN : role) {
            case OPTIONAL -> "OPTIONAL INTEL";
            case REFERENCE -> "REFERENCE";
            default -> "MAIN MISSION";
        };
    }

    private static String nextStep(TerminalMissionSnapshot snapshot, TerminalMissionAction action) {
        if (snapshot != null && !snapshot.actionHint().isBlank()) {
            return snapshot.actionHint();
        }
        if (action != null && action.enabled()) {
            return action.label();
        }
        if (action != null && !action.disabledReason().isBlank()) {
            return action.disabledReason();
        }
        if (snapshot != null && !snapshot.unlockReason().isBlank()) {
            return snapshot.unlockReason();
        }
        return "Track this objective.";
    }

    private static String actionReason(TerminalMissionSnapshot snapshot, TerminalMissionAction action) {
        if (action != null && action.enabled()) {
            return "Command channel ready.";
        }
        if (action != null && !action.disabledReason().isBlank()) {
            return action.disabledReason();
        }
        if (snapshot != null && !snapshot.unlockReason().isBlank()) {
            return snapshot.unlockReason();
        }
        if (snapshot != null && !snapshot.actionHint().isBlank()) {
            return snapshot.actionHint();
        }
        return "No mission command is available; tracking still pins this record to the Command Deck.";
    }

    private static String statusCompactLabel(TerminalMissionStatus status) {
        return switch (status == null ? TerminalMissionStatus.LOCKED : status) {
            case CLAIMABLE -> "CLAIM";
            case UNLOCKED -> "READY";
            case COMPLETED, CLAIMED -> "DONE";
            case VIEW_ONLY -> "INFO";
            case LOCKED -> "LOCKED";
        };
    }

    private static String compactStatusLabel(String statusKey) {
        String normalized = normalize(statusKey);
        if (normalized.contains("claim")) {
            return "CLAIM";
        }
        if (normalized.contains("ready") || normalized.contains("unlocked")) {
            return "READY";
        }
        if (normalized.contains("done") || normalized.contains("complete") || normalized.contains("claimed")) {
            return "DONE";
        }
        if (normalized.contains("locked")) {
            return "LOCKED";
        }
        if (normalized.contains("active")) {
            return "ACTIVE";
        }
        if (normalized.contains("warning")) {
            return "WARN";
        }
        return compactStatusLabel((Object) statusKey);
    }

    private static String compactStatusLabel(Object label) {
        String clean = String.valueOf(label == null ? "" : label).strip();
        if (clean.isBlank()) {
            return "INFO";
        }
        String normalized = normalize(clean);
        if (normalized.contains("reward") || normalized.contains("claim")) {
            return "CLAIM";
        }
        if (normalized.contains("available") || normalized.contains("ready")) {
            return "READY";
        }
        if (normalized.contains("complete") || normalized.contains("clear") || normalized.contains("done")) {
            return "DONE";
        }
        if (normalized.contains("locked")) {
            return "LOCKED";
        }
        String upper = clean.toUpperCase(Locale.ROOT);
        return upper.length() <= 6 ? upper : upper.substring(0, 6);
    }

    private static String primaryCommandLabel(TerminalMissionSnapshot snapshot, TerminalMissionAction action) {
        if (action != null && action.enabled()) {
            return compactActionLabel(action.label());
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case COMPLETED, CLAIMED -> "Next";
            case LOCKED -> "Unlock";
            default -> "Map";
        };
    }

    private static String primaryCommandMode(TerminalMissionSnapshot snapshot, TerminalMissionAction action) {
        if (action != null && action.enabled()) {
            return "action";
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case COMPLETED, CLAIMED -> "next";
            case LOCKED -> "requirements";
            default -> "map";
        };
    }

    private static String compactActionLabel(String label) {
        String clean = label == null || label.isBlank() ? "Open" : label.strip();
        String normalized = normalize(clean);
        if (normalized.contains("claim")) {
            return "Claim";
        }
        if (normalized.contains("reward")) {
            return "Claim";
        }
        if (normalized.contains("scan")) {
            return "Scan";
        }
        if (normalized.contains("open")) {
            return "Open";
        }
        if (normalized.contains("archive")) {
            return "Archive";
        }
        if (normalized.contains("sync")) {
            return "Sync";
        }
        if (clean.length() <= 10) {
            return clean;
        }
        String first = clean.split("\\s+", 2)[0];
        return first.length() <= 10 ? first : first.substring(0, 10);
    }

    private static String completeCommandDisabledReason(
            TerminalMissionSnapshot snapshot,
            TerminalMissionAction completeAction) {
        if (completeAction != null) {
            return "";
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case LOCKED, VIEW_ONLY -> "Complete unlocks after the mission requirements are available.";
            case CLAIMABLE, COMPLETED, CLAIMED -> "This objective is already complete.";
            case UNLOCKED -> "No complete command is available yet; finish the checklist first.";
        };
    }

    private static String claimCommandDisabledReason(
            TerminalMissionSnapshot snapshot,
            TerminalMissionAction claimAction,
            List<TerminalMissionReward> rewards) {
        if (claimAction != null) {
            return "";
        }
        if (rewards == null || rewards.isEmpty()) {
            return "No reward cache is listed for this objective.";
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case LOCKED, VIEW_ONLY -> "Rewards are hidden until the objective unlocks.";
            case UNLOCKED -> "Claim rewards after the objective is ready to turn in.";
            case COMPLETED, CLAIMED -> "Rewards for this objective are already archived.";
            case CLAIMABLE -> "No claim command is available for this reward cache.";
        };
    }

    private static Map<String, Object> requirementRow(TerminalMissionRequirement requirement) {
        boolean done = requirement.satisfied();
        String progress = requirement.need() > 0
                ? Math.min(requirement.have(), requirement.need()) + "/" + requirement.need()
                : "";
        String detail = requirement.detail();
        if (!progress.isBlank() && !detail.contains(progress)) {
            detail = detail.isBlank() ? progress : detail + " / " + progress;
        }
        String iconItemId = iconItemId(requirement.icon(), requirementFallbackIcon(requirement.kind()));
        return row("title", requirement.label(),
                "detail", detail,
                "kind", requirement.kind().name().toLowerCase(Locale.ROOT),
                "iconItemId", iconItemId,
                "iconCount", iconCount(requirement.icon()),
                "status", done ? "done" : "missing",
                "statusLabel", done ? "DONE" : "MISSING",
                "have", requirement.have(),
                "need", requirement.need());
    }

    private static List<Map<String, Object>> missionRewardRows(
            List<TerminalMissionReward> rewards,
            TerminalMissionSnapshot snapshot) {
        List<TerminalMissionReward> safeRewards = rewards == null ? List.of() : rewards;
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        int limit = Math.min(4, safeRewards.size());
        for (int index = 0; index < limit; index++) {
            rows.add(rewardRow(safeRewards.get(index), index, snapshot));
        }
        if (safeRewards.size() > limit) {
            int remaining = safeRewards.size() - limit;
            rows.add(row("id", "reward-more",
                    "title", "+" + remaining + " more reward entr" + (remaining == 1 ? "y" : "ies"),
                    "detail", "Open the reward inbox after completion for the full cache manifest.",
                    "itemId", "",
                    "iconItemId", REWARD_FALLBACK_ICON,
                    "iconCount", 1,
                    "count", remaining,
                    "countLabel", "+" + remaining,
                    "status", "info",
                    "stateLabel", "MORE"));
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> rewardRow(
            TerminalMissionReward reward,
            int index,
            TerminalMissionSnapshot snapshot) {
        ItemStack stack = reward == null ? ItemStack.EMPTY : reward.stack();
        String itemId = itemId(stack);
        String detail = reward == null ? "" : reward.detail();
        if (detail.isBlank()) {
            detail = itemId.isBlank() ? "Terminal reward cache entry." : itemId;
        }
        return row("id", "reward-" + index,
                "title", reward == null ? "Reward" : reward.label(),
                "detail", detail,
                "itemId", itemId,
                "iconItemId", iconItemId(stack, REWARD_FALLBACK_ICON),
                "iconCount", iconCount(stack),
                "count", stack.isEmpty() ? 0 : stack.getCount(),
                "countLabel", stack.isEmpty() ? "INFO" : "x" + stack.getCount(),
                "status", rewardStateKey(List.of(reward == null ? TerminalMissionReward.text("Reward", "") : reward), snapshot),
                "stateLabel", rewardStateLabel(List.of(reward == null ? TerminalMissionReward.text("Reward", "") : reward), snapshot));
    }

    private static String missionRewardSummary(
            List<TerminalMissionReward> rewards,
            TerminalMissionSnapshot snapshot) {
        int count = rewards == null ? 0 : rewards.size();
        if (count <= 0) {
            return "No listed rewards";
        }
        String state = rewardStateLabel(rewards, snapshot);
        return count + " listed reward" + (count == 1 ? "" : "s") + " / " + state;
    }

    private static String rewardCountLabel(List<TerminalMissionReward> rewards) {
        int count = rewards == null ? 0 : rewards.size();
        return count <= 0 ? "No rewards" : count + " reward" + (count == 1 ? "" : "s");
    }

    private static String rewardCompactLabel(List<TerminalMissionReward> rewards) {
        int count = rewards == null ? 0 : rewards.size();
        return "RWD " + count;
    }

    private static String rewardStateLabel(
            List<TerminalMissionReward> rewards,
            TerminalMissionSnapshot snapshot) {
        if (rewards == null || rewards.isEmpty()) {
            return "NONE";
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case CLAIMABLE -> "CLAIM";
            case COMPLETED, CLAIMED -> "CLAIMED";
            case LOCKED -> "LOCKED";
            case VIEW_ONLY -> "INFO";
            case UNLOCKED -> "REWARD";
        };
    }

    private static String rewardStateKey(
            List<TerminalMissionReward> rewards,
            TerminalMissionSnapshot snapshot) {
        if (rewards == null || rewards.isEmpty()) {
            return "info";
        }
        TerminalMissionStatus status = snapshot == null ? TerminalMissionStatus.VIEW_ONLY : snapshot.status();
        return switch (status) {
            case CLAIMABLE -> "warning";
            case COMPLETED, CLAIMED -> "done";
            case LOCKED -> "locked";
            case VIEW_ONLY -> "info";
            case UNLOCKED -> "ready";
        };
    }

    private static TerminalMissionAction firstDisplayAction(List<TerminalMissionAction> actions) {
        TerminalMissionAction bestEnabled = null;
        int bestEnabledPriority = Integer.MAX_VALUE;
        TerminalMissionAction bestDisabled = null;
        int bestDisabledPriority = Integer.MAX_VALUE;
        for (TerminalMissionAction action : actions == null ? List.<TerminalMissionAction>of() : actions) {
            int priority = missionActionPriority(action);
            if (action.enabled()) {
                if (bestEnabled == null || priority < bestEnabledPriority) {
                    bestEnabled = action;
                    bestEnabledPriority = priority;
                }
                continue;
            }
            if (bestDisabled == null || priority < bestDisabledPriority) {
                bestDisabled = action;
                bestDisabledPriority = priority;
            }
        }
        return bestEnabled == null ? bestDisabled : bestEnabled;
    }

    private static TerminalMissionAction firstEnabledAction(List<TerminalMissionAction> actions) {
        for (TerminalMissionAction action : actions == null ? List.<TerminalMissionAction>of() : actions) {
            if (action.enabled()) {
                return action;
            }
        }
        return null;
    }

    private static TerminalMissionAction firstEnabledActionMatching(
            List<TerminalMissionAction> actions,
            Predicate<TerminalMissionAction> predicate) {
        for (TerminalMissionAction action : actions == null ? List.<TerminalMissionAction>of() : actions) {
            if (action.enabled() && predicate.test(action)) {
                return action;
            }
        }
        return null;
    }

    private static boolean isCompleteAction(TerminalMissionAction action) {
        String signal = missionActionSignal(action);
        return signal.contains("complete") || signal.contains("turnin");
    }

    private static boolean isClaimAction(TerminalMissionAction action) {
        String signal = missionActionSignal(action);
        return signal.contains("claim") || signal.contains("reward");
    }

    private static int missionActionPriority(TerminalMissionAction action) {
        String signal = missionActionSignal(action);
        if (signal.contains("turnin") || signal.contains("complete")) {
            return 0;
        }
        if (signal.contains("claim")) {
            return 1;
        }
        return 10;
    }

    private static String missionActionSignal(TerminalMissionAction action) {
        if (action == null) {
            return "";
        }
        return normalize(action.id() + " " + action.label())
                .replaceAll("[^a-z0-9]+", "");
    }

    private static String sideCardStatus(TerminalMissionSnapshot snapshot) {
        return switch (snapshot.status()) {
            case CLAIMABLE -> "READY";
            case COMPLETED, CLAIMED -> "ARCHIVED";
            case UNLOCKED -> "ACTIVE";
            case VIEW_ONLY, LOCKED -> "LOCKED";
        };
    }

    private static String sideCardProgress(TerminalMissionDefinition mission, TerminalMissionSnapshot snapshot) {
        if (snapshot.status() == TerminalMissionStatus.LOCKED || snapshot.status() == TerminalMissionStatus.VIEW_ONLY) {
            return "Locked";
        }
        if (snapshot.status() == TerminalMissionStatus.CLAIMABLE) {
            return "Ready to archive";
        }
        if (snapshot.status() == TerminalMissionStatus.COMPLETED || snapshot.status() == TerminalMissionStatus.CLAIMED) {
            return "Archived";
        }
        int have = 0;
        int need = 0;
        for (var requirement : mission.requirements()) {
            if (requirement.need() > 0) {
                have += Math.min(requirement.have(), requirement.need());
                need += requirement.need();
            }
        }
        return need > 0 ? have + "/" + need + " complete" : Math.round(snapshot.progress() * 100.0F) + "% complete";
    }

    private static Object addons(List<String> path) {
        TerminalStatusSnapshot snapshot = statusSnapshot();
        List<Map<String, Object>> chapters = EchoAddonRegistry.chapters().stream()
                .map(chapter -> addonRow(chapter, snapshot))
                .toList();
        if (path.size() > 1 && ("chapters".equals(path.get(1)) || "visibleChapters".equals(path.get(1)))) {
            return chapters;
        }
        if (path.size() > 1 && "selectedChapter".equals(path.get(1))) {
            return resolveNested(selectedRow(chapters, state().selectedAddonId())
                    .orElseGet(() -> chapters.isEmpty() ? row("title", "No mod routes detected") : chapters.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedChapterId".equals(path.get(1))) {
            return state().selectedAddonId();
        }
        return resolveNested(row("count", chapters.size()), path, 1);
    }

    private static String addonIcon(EchoAddonChapter chapter) {
        String id = chapter == null ? "" : normalize(chapter.id());
        if (id.contains("ashfall")) {
            return "ASH";
        }
        if (id.contains("relic")) {
            return "REL";
        }
        if (id.contains("power")) {
            return "PWR";
        }
        if (id.contains("index")) {
            return "IDX";
        }
        if (id.contains("holo") || id.contains("map")) {
            return "MAP";
        }
        return "E7";
    }

    private static Map<String, Object> addonRow(EchoAddonChapter chapter, TerminalStatusSnapshot snapshot) {
        Identifier routeTarget = addonRouteTarget(chapter);
        Identifier archivesTarget = id("archives");
        Identifier diagnosticsTarget = id("data_core");
        Identifier configTarget = id("settings");
        boolean available = chapter.isAvailable(player());
        return row("id", chapter.id(), "modId", chapter.modId(), "title", chapter.displayName(),
                "summary", chapter.summary(), "status", chapter.statusLine(player()),
                "available", available,
                "selected", chapter.id().equals(state().selectedAddonId()),
                "icon", addonIcon(chapter),
                "statusLabel", available ? "ACTIVE" : "STANDBY",
                "routeCount", chapterRouteCount(chapter),
                "recordCount", snapshot.routeCount(),
                "statusKey", available ? "ready" : "locked",
                "routeTargetTabId", routeTarget.toString(),
                "routeTargetPageId", TerminalScreenCoreBridge.pageForTab(routeTarget).toString(),
                "archiveTargetTabId", archivesTarget.toString(),
                "archiveTargetPageId", TerminalScreenCoreBridge.pageForTab(archivesTarget).toString(),
                "diagnosticsTargetTabId", diagnosticsTarget.toString(),
                "diagnosticsTargetPageId", TerminalScreenCoreBridge.pageForTab(diagnosticsTarget).toString(),
                "configTargetTabId", configTarget.toString(),
                "configTargetPageId", TerminalScreenCoreBridge.pageForTab(configTarget).toString());
    }

    private static Identifier addonRouteTarget(EchoAddonChapter chapter) {
        String normalizedId = normalize(chapter.id());
        String normalizedModId = normalize(chapter.modId());
        for (TerminalTab tab : TerminalScreenCoreBridge.tabs()) {
            Identifier tabId = tab.descriptor().id();
            if (id("addons").equals(tabId)) {
                continue;
            }
            TerminalNavigationProfile profile = TerminalNavigationProfiles.profileFor(tab);
            String tabKey = normalize(tabId.toString());
            if ((!normalizedId.isBlank() && (normalize(profile.chapterId()).equals(normalizedId)
                    || tabKey.contains(normalizedId)
                    || normalize(tab.descriptor().title()).contains(normalizedId)))
                    || (!normalizedModId.isBlank() && (normalize(tabId.getNamespace()).equals(normalizedModId)
                            || tabKey.contains(normalizedModId)))) {
                return tabId;
            }
        }
        return MainSurvivalQuestProvider.TAB_ID;
    }

    private static int chapterRouteCount(EchoAddonChapter chapter) {
        if (chapter == null) {
            return 0;
        }
        String chapterId = normalize(chapter.id());
        int count = 0;
        for (TerminalMissionProvider provider : TerminalMissionRegistry.providers()) {
            TerminalMissionChapter missionChapter = safeChapter(provider);
            if (chapterId.equals(normalize(missionChapter.id().toString()))
                    || chapterId.equals(normalize(missionChapter.title()))) {
                count += safeMissions(provider).size();
            }
        }
        return count;
    }

    private static Object recipeIndex(List<String> path) {
        TerminalRecipeUiSnapshot view = recipeUiSnapshot();
        TerminalRecipeSnapshot snapshot = view.snapshot();
        List<Map<String, Object>> recipes = view.recipes();
        if (path.size() > 1 && "categories".equals(path.get(1))) {
            return view.categories();
        }
        if (path.size() > 1 && ("recipes".equals(path.get(1)) || "visibleItems".equals(path.get(1)))) {
            return recipes;
        }
        if (path.size() > 1 && "selectedRecipe".equals(path.get(1))) {
            return resolveNested(selectedRow(recipes, state().selectedRecipeId())
                    .orElseGet(() -> recipes.isEmpty()
                            ? row("title", "No recipe selected", "summary", "Use search or mode controls to narrow recipes.")
                            : recipes.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedRecipeId".equals(path.get(1))) {
            return state().selectedRecipeId() == null ? "" : state().selectedRecipeId().toString();
        }
        return resolveNested(row("providerCount", snapshot.providerCount(), "recipeCount", snapshot.recipes().size(),
                        "visibleCount", view.visibleCount(), "mode", view.mode(), "query", view.queryText(),
                        "category", view.category(),
                        "modeLabel", view.mode().toUpperCase(Locale.ROOT),
                        "modes", List.of(
                                row("id", "recipes", "label", "Recipes", "selected", "recipes".equals(view.mode())),
                                row("id", "uses", "label", "Uses", "selected", "uses".equals(view.mode())),
                                row("id", "sources", "label", "Sources", "selected", "sources".equals(view.mode())),
                                row("id", "info", "label", "Info", "selected", "info".equals(view.mode()))),
                        "legacyAvailable", true),
                path, 1);
    }

    private static TerminalRecipeUiSnapshot recipeUiSnapshot() {
        Player player = player();
        int playerKey = player == null ? 0 : System.identityHashCode(player);
        int tickBucket = player == null ? -1 : player.tickCount / RECIPE_SNAPSHOT_CACHE_TICKS;
        long registryRevision = TerminalRecipeRegistry.revision();
        String queryText = state().recipeSearch();
        String query = normalize(queryText);
        String mode = state().recipeMode();
        String categoryFilter = state().recipeCategory();
        TerminalRecipeUiSnapshot cached = recipeUiSnapshot;
        if (cached != null && cached.matches(registryRevision, playerKey, tickBucket, query, mode, categoryFilter)) {
            return cached;
        }
        TerminalRecipeSnapshot snapshot = TerminalRecipeRegistry.snapshot(player);
        Map<Identifier, String> searchTextByRecipe = new LinkedHashMap<>();
        ArrayList<Map<String, Object>> recipes = new ArrayList<>();
        for (TerminalRecipeEntry recipe : snapshot.recipes()) {
            if (!"all".equals(categoryFilter) && !recipe.categoryId().toString().equals(categoryFilter)) {
                continue;
            }
            if (!matchesRecipeQuery(recipe, query, searchTextByRecipe)) {
                continue;
            }
            recipes.add(recipeRow(snapshot, recipe));
            if (recipes.size() >= SCREENCORE_RECIPE_ROW_LIMIT) {
                break;
            }
        }
        ArrayList<Map<String, Object>> categories = new ArrayList<>();
        categories.add(row("id", "all", "title", "All", "compactTitle", "All", "accent", color(0xFF00EAFF),
                "selected", "all".equals(categoryFilter)));
        snapshot.categories().stream()
                .map(category -> recipeCategoryRow(category, categoryFilter))
                .forEach(categories::add);
        TerminalRecipeUiSnapshot next = new TerminalRecipeUiSnapshot(
                registryRevision,
                playerKey,
                tickBucket,
                query,
                queryText,
                mode,
                categoryFilter,
                snapshot,
                List.copyOf(categories),
                List.copyOf(recipes),
                recipes.size());
        recipeUiSnapshot = next;
        recipeUiBuildCount++;
        return next;
    }

    private static Map<String, Object> recipeCategoryRow(TerminalRecipeCategory category, String selectedCategory) {
        String id = category.id().toString();
        return row("id", id, "title", category.title(), "compactTitle", compactRecipeCategoryTitle(category.title()),
                "accent", color(category.accentColor()),
                "selected", id.equals(selectedCategory));
    }

    private static String compactRecipeCategoryTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Recipe";
        }
        String compact = title
                .replace(" Collection", "")
                .replace(" Collector", "")
                .replace(" Operations", " Ops")
                .replace(" Operation", " Ops")
                .replace(" Purifier", " Purify")
                .replace(" Assembly", "Assemble")
                .replace("Thermal Burn", "Thermal")
                .strip();
        if (compact.length() <= 14) {
            return compact;
        }
        return compact.substring(0, 13).stripTrailing() + ".";
    }

    private static Map<String, Object> recipeRow(TerminalRecipeSnapshot snapshot, TerminalRecipeEntry recipe) {
        TerminalRecipeCategory category = snapshot.categoryMap().get(recipe.categoryId());
        return row("id", recipe.id().toString(), "title", recipe.title(),
                "category", recipe.categoryId().toString(),
                "categoryTitle", category == null ? recipe.categoryId().getPath() : category.title(),
                "machine", itemId(recipe.machine()),
                "machineLabel", itemLabel(recipe.machine()),
                "summary", recipeSummary(recipe),
                "inputSummary", slotSummary(recipe, TerminalRecipeSlot.Role.INPUT),
                "outputSummary", slotSummary(recipe, TerminalRecipeSlot.Role.OUTPUT),
                "catalystSummary", slotSummary(recipe, TerminalRecipeSlot.Role.CATALYST),
                "noteSummary", recipe.notes().isEmpty() ? "No provider notes." : recipe.notes().get(0).text().getString(),
                "process", recipe.processTicks() <= 0 ? "Instant" : recipe.processTicks() + " ticks",
                "locked", recipe.locked(),
                "status", recipe.locked() ? "locked" : "ready");
    }

    private static Object routeRecords(List<String> path) {
        List<Map<String, Object>> routes = EchoCoreServices.routeRecords(player()).stream()
                .map(route -> row("id", route.id().toString(), "chapterId", route.chapterId(),
                        "title", route.title(), "category", route.category(), "dimensionHint", route.dimensionHint(),
                        "status", route.status(), "statusKey", route.complete() ? "done" : "active",
                        "summary", route.summary(), "complete", route.complete()))
                .filter(route -> "all".equals(state().routeFilter())
                        || state().routeFilter().equalsIgnoreCase(String.valueOf(route.get("category"))))
                .toList();
        if (path.size() > 1 && ("all".equals(path.get(1)) || "visible".equals(path.get(1)))) {
            return routes;
        }
        if (path.size() > 1 && "selected".equals(path.get(1))) {
            return resolveNested(selectedRow(routes, state().selectedRouteRecordId())
                    .orElseGet(() -> routes.isEmpty() ? row("title", "No route selected") : routes.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedId".equals(path.get(1))) {
            return state().selectedRouteRecordId() == null ? "" : state().selectedRouteRecordId().toString();
        }
        return resolveNested(row("count", routes.size(), "filter", state().routeFilter()), path, 1);
    }

    private static Object discoveryGrid(List<String> path) {
        List<Map<String, Object>> cards = EchoCoreServices.resolvedDiscoveryEntries(player()).stream()
                .map(TerminalScreenCoreDataProviders::discoveryRow)
                .filter(card -> "all".equals(state().discoveryCategory())
                        || state().discoveryCategory().equalsIgnoreCase(String.valueOf(card.get("category"))))
                .filter(card -> "all".equals(state().discoveryState())
                        || state().discoveryState().equalsIgnoreCase(String.valueOf(card.get("state"))))
                .toList();
        if (path.size() > 1 && "visibleCards".equals(path.get(1))) {
            return cards;
        }
        if (path.size() > 1 && "categories".equals(path.get(1))) {
            List<Map<String, Object>> categories = new ArrayList<>();
            categories.add(row("id", "all", "label", "ALL"));
            java.util.Arrays.stream(com.knoxhack.echocore.api.EchoDiscoveryCategory.values())
                    .map(category -> row("id", category.name(), "label", category.displayName()))
                    .forEach(categories::add);
            return categories;
        }
        if (path.size() > 1 && "states".equals(path.get(1))) {
            return List.of("all", "discovered", "checked", "locked").stream()
                    .map(state -> row("id", state, "label", state.toUpperCase(Locale.ROOT)))
                    .toList();
        }
        if (path.size() > 1 && "selected".equals(path.get(1))) {
            return resolveNested(selectedRow(cards, state().selectedDiscoveryId())
                    .orElseGet(() -> cards.isEmpty() ? row("title", "No discoveries") : cards.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedId".equals(path.get(1))) {
            return state().selectedDiscoveryId() == null ? "" : state().selectedDiscoveryId().toString();
        }
        return resolveNested(row("count", cards.size(),
                "category", state().discoveryCategory(),
                "state", state().discoveryState()), path, 1);
    }

    private static Map<String, Object> discoveryRow(EchoResolvedDiscoveryEntry resolved) {
        var entry = resolved.entry();
        EchoDiscoveryState state = resolved.state();
        boolean revealed = state != EchoDiscoveryState.LOCKED;
        return row("id", entry.id().toString(),
                "chapterId", entry.chapterId().toString(),
                "category", entry.category().displayName(),
                "state", state.name(),
                "statusKey", revealed ? "ready" : "locked",
                "title", revealed ? entry.revealedTitle() : entry.lockedHintTitle(),
                "summary", revealed ? entry.revealedSummary() : entry.hintText(),
                "revealed", revealed);
    }

    private static Object factions(List<String> path) {
        List<Map<String, Object>> profiles = EchoCoreServices.factionProfiles(player()).stream()
                .map(TerminalScreenCoreDataProviders::factionRow)
                .filter(profile -> "all".equals(state().factionNamespace())
                        || state().factionNamespace().equals(String.valueOf(profile.get("namespace"))))
                .toList();
        if (path.size() > 1 && "visible".equals(path.get(1))) {
            return profiles;
        }
        if (path.size() > 1 && "selected".equals(path.get(1))) {
            return resolveNested(selectedRow(profiles, state().selectedFactionId())
                    .orElseGet(() -> profiles.isEmpty() ? row("title", "No faction signals") : profiles.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedId".equals(path.get(1))) {
            return state().selectedFactionId() == null ? "" : state().selectedFactionId().toString();
        }
        if (path.size() > 1 && "namespaces".equals(path.get(1))) {
            List<Map<String, Object>> namespaces = new ArrayList<>();
            namespaces.add(row("id", "all", "label", "ALL"));
            profiles.stream()
                    .map(profile -> String.valueOf(profile.get("id")).split(":", 2)[0])
                    .distinct()
                    .map(namespace -> row("id", namespace, "label", namespace))
                    .forEach(namespaces::add);
            return namespaces;
        }
        if (path.size() > 1 && ("contracts".equals(path.get(1))
                || "services".equals(path.get(1)) || "poiAffinity".equals(path.get(1)))) {
            return List.of();
        }
        return resolveNested(row("count", profiles.size(), "namespace", state().factionNamespace()), path, 1);
    }

    private static Map<String, Object> factionRow(EchoFactionProfile profile) {
        return row("id", profile.definition().id().toString(),
                "namespace", profile.definition().id().getNamespace(),
                "title", profile.definition().displayName(),
                "route", profile.definition().route(),
                "summary", profile.definition().summary(),
                "standing", profile.standing().displayName(),
                "statusKey", profile.contacted() ? "ready" : "locked",
                "reputation", profile.reputation(),
                "contacted", profile.contacted(),
                "contracts", profile.definition().contracts().size(),
                "activeContract", profile.activeContractId().map(Identifier::toString).orElse("none"),
                "memory", profile.npcMemory());
    }

    private static Object archives(List<String> path) {
        List<TerminalArchiveEntry> entries;
        try {
            entries = TerminalArchiveRegistry.entries();
        } catch (LinkageError exception) {
            entries = List.of();
        }
        List<Map<String, Object>> records = entries.stream()
                .map(TerminalScreenCoreDataProviders::archiveRow)
                .filter(record -> "all".equals(state().archiveState())
                        || state().archiveState().equalsIgnoreCase(String.valueOf(record.get("state"))))
                .filter(record -> "all".equals(state().archiveGroup())
                        || state().archiveGroup().equalsIgnoreCase(String.valueOf(record.get("group"))))
                .toList();
        if (path.size() > 1 && "visibleRecords".equals(path.get(1))) {
            return records;
        }
        if (path.size() > 1 && "selectedRecord".equals(path.get(1))) {
            return resolveNested(selectedRow(records, state().selectedArchiveId())
                    .orElseGet(() -> records.isEmpty()
                            ? row("title", "No shared archive records",
                                    "group", "Archive",
                                    "body", "No provider-backed archive rows are available.",
                                    "markDisabled", true,
                                    "markDisabledReason", "No archive record is selected.")
                            : records.get(0)), path, 2);
        }
        if (path.size() > 1 && "selectedRecordId".equals(path.get(1))) {
            return state().selectedArchiveId() == null ? "" : state().selectedArchiveId().toString();
        }
        if (path.size() > 1 && "states".equals(path.get(1))) {
            return List.of("all", "open", "locked", "unread").stream()
                    .map(state -> row("id", state, "label", state.toUpperCase(Locale.ROOT)))
                    .toList();
        }
        if (path.size() > 1 && "groups".equals(path.get(1))) {
            List<Map<String, Object>> groups = new ArrayList<>();
            groups.add(row("id", "all", "label", "ALL"));
            records.stream()
                    .map(record -> String.valueOf(record.get("group")))
                    .distinct()
                    .map(group -> row("id", group, "label", group))
                    .forEach(groups::add);
            return groups;
        }
        return resolveNested(row("count", records.size(),
                "state", state().archiveState(),
                "group", state().archiveGroup()), path, 1);
    }

    private static Map<String, Object> archiveRow(TerminalArchiveEntry entry) {
        boolean locked = entry.locked() && !EchoCoreServices.isArchiveUnlocked(player(), entry.id().toString());
        return row("id", entry.id().toString(), "group", entry.group(), "title", entry.title(),
                "status", entry.status(), "state", locked ? "locked" : "open",
                "statusKey", locked ? "locked" : "ready", "locked", locked,
                "markDisabled", locked,
                "markDisabledReason", locked ? "Unlock this record before flagging it read." : "Archive record is already available.",
                "body", locked ? "Record locked. Discover the route proof through its owning chapter."
                        : String.join(" ", entry.lines()));
    }

    private static Object vitals(List<String> path) {
        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player());
        List<Map<String, Object>> vitals = List.of(
                vital("Hydration", telemetry.hydration(), true),
                vital("Radiation", telemetry.radiation(), false),
                vital("Toxic Air", telemetry.toxicAir(), false),
                vital("Oxygen", telemetry.oxygen(), true),
                vital("Pressure", telemetry.pressure(), true),
                vital("Cold", telemetry.cold(), false),
                vital("Heat", telemetry.heat(), false),
                vital("Exposure", telemetry.exposure(), false));
        if (path.size() > 1 && "all".equals(path.get(1))) {
            return vitals;
        }
        if (path.size() > 1 && "warnings".equals(path.get(1))) {
            return vitals.stream().filter(vital -> !"ready".equals(vital.get("status"))).toList();
        }
        if (path.size() > 1 && "nominal".equals(path.get(1))) {
            return vitals.stream().filter(vital -> "ready".equals(vital.get("status"))).toList();
        }
        return resolveNested(row("status", telemetry.warning() ? "WARNING" : "NOMINAL",
                "statusKey", telemetry.warning() ? "warning" : "ready",
                "statusLine", telemetry.statusLine()), path, 1);
    }

    private static Map<String, Object> vital(String label, int value, boolean highGood) {
        int danger = highGood ? 100 - value : value;
        return row("label", label, "value", value, "status",
                danger >= 70 ? "critical" : danger >= 40 ? "warning" : "ready");
    }

    private static Object rewardInbox(List<String> path) {
        int pending = EchoCoreServices.pendingTerminalRewardCount(player());
        List<Map<String, Object>> incoming = rewardRows(pending);
        List<Map<String, Object>> crates = crateRows(pending);
        List<Map<String, Object>> claimables = claimableRows(pending);
        ArrayList<Map<String, Object>> rewardOptions = new ArrayList<>(incoming);
        rewardOptions.addAll(crates);
        if (path.size() > 1 && "incomingRewards".equals(path.get(1))) {
            return incoming;
        }
        if (path.size() > 1 && "crates".equals(path.get(1))) {
            return crates;
        }
        if (path.size() > 1 && "claimables".equals(path.get(1))) {
            return claimables;
        }
        if (path.size() > 1 && "selected".equals(path.get(1))) {
            return resolveNested(selectedRow(rewardOptions, state().selectedRewardId())
                    .orElseGet(() -> incoming.isEmpty()
                            ? row("title", "Reward Inbox", "summary", "No rewards waiting.")
                            : incoming.get(0)), path, 2);
        }
        String statusLabel = state().rewardDeferred() ? "DEFERRED" : state().rewardViewed() ? "VIEWED" : pending > 0 ? "READY" : "CLEAR";
        String statusKey = state().rewardDeferred() || state().rewardViewed() ? "info" : pending > 0 ? "warning" : "ready";
        return resolveNested(row("pending", pending,
                        "summary", pending > 0 ? pending + " support cache item(s) ready." : "No terminal rewards are waiting.",
                        "canClaimAll", pending > 0,
                        "claimDisabled", pending <= 0,
                        "selectedRewardId", state().selectedRewardId(),
                        "deferred", state().rewardDeferred(),
                        "viewed", state().rewardViewed(),
                        "statusKey", statusKey,
                        "statusLabel", statusLabel),
                path, 1);
    }

    private static List<Map<String, Object>> rewardRows(int pending) {
        if (pending <= 0) {
            return List.of(
                    row("id", "empty", "title", "No Pending Rewards",
                            "summary", "Complete route objectives to route caches into this inbox.",
                            "badge", "OK", "statusLabel", "CLEAR", "statusKey", "ready", "disabled", true,
                            "disabledReason", "No reward cache is queued yet."),
                    row("id", "support", "title", "Support Channel Online",
                            "summary", "Shared reward service is ready for mission payouts and support deliveries.",
                            "badge", "SYNC", "statusLabel", "ONLINE", "statusKey", "ready", "disabled", true,
                            "disabledReason", "This status row has no claimable payload."));
        }
        return List.of(
                row("id", "terminal-cache", "title", "Terminal Support Cache",
                        "summary", pending + " cache item(s) ready to claim.", "badge", "CR",
                        "statusLabel", "READY", "statusKey", "warning"),
                row("id", "route-payout", "title", "Route Objective Payout",
                        "summary", "Mission reward delivery queued by shared ECHO services.", "badge", "XP",
                        "statusLabel", "READY", "statusKey", "ready"),
                row("id", "field-supply", "title", "Field Supply Drop",
                        "summary", "Supplies are bound to the nearest owned ECHO terminal.", "badge", "KIT",
                        "statusLabel", "READY", "statusKey", "active"));
    }

    private static List<Map<String, Object>> crateRows(int pending) {
        return List.of(
                row("id", "relictech-cache", "title", "RelicTech Data Cache",
                        "summary", pending > 0 ? "High-value intel and component data." : "No cache sealed yet.",
                        "statusLabel", pending > 0 ? "READY TO CLAIM" : "LOCKED",
                        "statusKey", pending > 0 ? "warning" : "locked",
                        "disabled", pending <= 0,
                        "disabledReason", pending > 0 ? "" : "No sealed cache is available to claim."),
                row("id", "s71-crate", "title", "S-71 Backhaul Crate",
                        "summary", "Logistics package recovered from route 71 backhaul.",
                        "statusLabel", pending > 0 ? "READY" : "STANDBY",
                        "statusKey", pending > 0 ? "ready" : "info",
                        "disabled", pending <= 0,
                        "disabledReason", pending > 0 ? "" : "Backhaul crate unlocks when rewards are pending."));
    }

    private static List<Map<String, Object>> claimableRows(int pending) {
        int multiplier = Math.max(1, pending);
        return List.of(
                row("title", "Experience", "value", String.valueOf(1250 * multiplier)),
                row("title", "ECHO Credits", "value", String.valueOf(4500 * multiplier)),
                row("title", "Route Data", "value", String.valueOf(Math.max(1, pending))),
                row("title", "Support Tokens", "value", String.valueOf(3 * multiplier)));
    }

    private static Object dataCore(List<String> path) {
        DataServiceDiagnostics diagnostics = EchoCoreServices.dataService().diagnostics();
        return resolveNested(row("serviceState", diagnostics.available() ? "ONLINE" : "NO-OP",
                "statusKey", diagnostics.available() ? "ready" : "warning",
                "filter", state().diagnosticsChapterFilter(),
                "summary", diagnostics.available()
                        ? "Shared data service online; registered records can sync through the Terminal."
                        : "Shared data service is unavailable; providers are using safe no-op state.",
                "revision", diagnostics.revision(),
                "registeredKeys", diagnostics.registeredKeyCount(),
                "syncedKeys", diagnostics.syncedKeyCount(),
                "metadataOwners", diagnostics.metadataKeyCount(),
                "dirtyOwners", diagnostics.dirtyOwnerCount(),
                "dirtyStatusKey", diagnostics.dirtyOwnerCount() > 0 ? "warning" : "ready",
                "rawVisible", !TerminalClientOptions.hideDebugInfo()), path, 1);
    }

    private static Object scriptCore(List<String> path) {
        Map<String, Object> view = scriptCoreView();
        if (path.size() > 1 && "definitions".equals(path.get(1))) {
            return view.get("definitions");
        }
        if (path.size() > 1 && "executableDefinitions".equals(path.get(1))) {
            return view.get("executableDefinitions");
        }
        if (path.size() > 1 && "executableSlots".equals(path.get(1))) {
            return view.get("executableSlots");
        }
        if (path.size() > 1 && "diagnostics".equals(path.get(1))) {
            return view.get("diagnostics");
        }
        return resolveNested(view, path, 1);
    }

    private static Map<String, Object> scriptCoreView() {
        boolean actionRegistered = EchoActionRegistry.action("scriptcore.execute").isPresent();
        boolean previewRegistered = EchoActionRegistry.action("scriptcore.preview").isPresent();
        try {
            Class<?> apiClass = Class.forName("com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi");
            Object api = apiClass.getMethod("get").invoke(null);
            Object summary = apiClass.getMethod("diagnosticsSummary").invoke(api);
            Object registry = apiClass.getMethod("registry").invoke(api);
            Object lastResult = apiClass.getMethod("lastResult").invoke(api);
            @SuppressWarnings("unchecked")
            List<Object> definitions = (List<Object>) registry.getClass().getMethod("all").invoke(registry);
            @SuppressWarnings("unchecked")
            List<Object> diagnostics = (List<Object>) lastResult.getClass().getMethod("diagnostics").invoke(lastResult);
            List<Map<String, Object>> definitionRows = definitions.stream()
                    .map(TerminalScreenCoreDataProviders::scriptDefinitionRow)
                    .toList();
            List<Map<String, Object>> executableRows = definitions.stream()
                    .flatMap(definition -> scriptExecutableRows(definition).stream())
                    .limit(48)
                    .toList();
            boolean configAllowed = scriptCoreUiActionsAllowed();
            return row("present", true,
                    "bridgeRegistered", actionRegistered,
                    "bridgeStatus", actionRegistered ? "ready" : "warning",
                    "bridgeLabel", actionRegistered ? "EXEC BRIDGE READY" : "EXEC BRIDGE PENDING",
                    "previewRegistered", previewRegistered,
                    "previewStatus", previewRegistered ? "ready" : "warning",
                    "previewLabel", previewRegistered ? "PREVIEW READY" : "PREVIEW PENDING",
                    "configAllowed", configAllowed,
                    "configStatus", configAllowed ? "ready" : "warning",
                    "configLabel", configAllowed ? "OPT-IN EXEC ON" : "OPT-IN EXEC OFF",
                    "definitionCount", intMethod(summary, "definitionCount"),
                    "errors", longMethod(summary, "errors"),
                    "warnings", longMethod(summary, "warnings"),
                    "runtimeBackend", stringMethod(summary, "runtimeStorageBackend", "unknown"),
                    "statusKey", longMethod(summary, "errors") > 0 ? "danger" : longMethod(summary, "warnings") > 0 ? "warning" : "ready",
                    "summary", executableRows.size() + " executable definition(s) exposed as trusted ScreenCore intents.",
                    "definitions", definitionRows,
                    "executableDefinitions", executableRows,
                    "executableSlots", executableRows,
                    "diagnostics", diagnostics.stream().map(TerminalScreenCoreDataProviders::scriptDiagnosticRow).limit(40).toList());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return row("present", false,
                    "bridgeRegistered", actionRegistered,
                    "bridgeStatus", "locked",
                    "bridgeLabel", "SCRIPTCORE OFFLINE",
                    "previewRegistered", previewRegistered,
                    "previewStatus", "locked",
                    "previewLabel", "PREVIEW OFFLINE",
                    "configAllowed", false,
                    "configStatus", "locked",
                    "configLabel", "OPT-IN EXEC OFF",
                    "definitionCount", 0,
                    "errors", 0,
                    "warnings", 0,
                    "runtimeBackend", "missing",
                    "statusKey", "locked",
                    "summary", "ScriptCore is not loaded or its API is unavailable.",
                    "definitions", List.of(),
                    "executableDefinitions", List.of(),
                    "executableSlots", List.of(),
                    "diagnostics", List.of());
        }
    }

    private static Map<String, Object> scriptDefinitionRow(Object definition) {
        List<?> actions = listMethod(definition, "actions");
        String id = stringMethod(definition, "id", "");
        String type = stringMethod(definition, "type", "generic");
        String pack = stringMethod(definition, "pack", "unknown");
        String title = optionalString(method(definition, "title"), id);
        String description = optionalString(method(definition, "description"), "No description supplied.");
        long customActions = actions.stream()
                .filter(action -> "custom".equalsIgnoreCase(stringMethod(action, "type", "")))
                .count();
        return row("id", id,
                "title", title,
                "description", description,
                "type", type,
                "pack", pack,
                "status", actions.isEmpty() ? "locked" : customActions > 0 ? "warning" : "ready",
                "statusLabel", actions.isEmpty() ? "READ ONLY" : customActions > 0 ? "CUSTOM" : "EXECUTE",
                "actionCount", actions.size(),
                "customActionCount", customActions,
                "slot", "actions",
                "executeDisabled", actions.isEmpty(),
                "executeReason", actions.isEmpty()
                        ? "Definition has no actions slot."
                        : "Sends a trusted ScriptCore execute intent; server config still owns authority.");
    }

    private static List<Map<String, Object>> scriptExecutableRows(Object definition) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        String id = stringMethod(definition, "id", "");
        String type = stringMethod(definition, "type", "generic");
        String pack = stringMethod(definition, "pack", "unknown");
        String title = optionalString(method(definition, "title"), id);
        addExecutableSlot(rows, id, title, pack, type, "actions", listMethod(definition, "actions"));
        if ("mission".equals(type)) {
            addExecutableSlot(rows, id, title, pack, type, "on_start", listMethod(definition, "onStart"));
            addExecutableSlot(rows, id, title, pack, type, "on_complete", listMethod(definition, "onComplete"));
            addExecutableSlot(rows, id, title, pack, type, "on_fail", listMethod(definition, "onFail"));
        }
        if ("world_state".equals(type) || "weather_event".equals(type)) {
            addExecutableSlot(rows, id, title, pack, type, "effects", listMethod(definition, "effects"));
        }
        if ("dialogue".equals(type)) {
            for (Object choice : listMethod(definition, "choices")) {
                String choiceId = stringMethod(choice, "id", "");
                if (!choiceId.isBlank()) {
                    addExecutableSlot(rows, id, title + " / " + choiceId, pack, type,
                            "choice:" + choiceId, listMethod(choice, "actions"));
                }
            }
        }
        return rows;
    }

    private static void addExecutableSlot(
            List<Map<String, Object>> rows,
            String id,
            String title,
            String pack,
            String type,
            String slot,
            List<?> actions) {
        if (actions.isEmpty()) {
            return;
        }
        long customActions = actions.stream()
                .filter(action -> "custom".equalsIgnoreCase(stringMethod(action, "type", "")))
                .count();
        boolean executeRegistered = EchoActionRegistry.action("scriptcore.execute").isPresent();
        boolean previewRegistered = EchoActionRegistry.action("scriptcore.preview").isPresent();
        boolean configAllowed = scriptCoreUiActionsAllowed();
        boolean customBlocked = customActions > 0;
        boolean executeDisabled = customBlocked || !executeRegistered || !configAllowed;
        rows.add(row("id", id,
                "title", title,
                "description", "Trusted ScreenCore intent -> ScriptCore slot " + slot,
                "type", type,
                "pack", pack,
                "slot", slot,
                "slotLabel", slot.toUpperCase(Locale.ROOT),
                "actionCount", actions.size(),
                "customActionCount", customActions,
                "status", customBlocked || !configAllowed ? "warning" : "ready",
                "statusLabel", customBlocked ? "CUSTOM" : !configAllowed ? "OPT-IN OFF" : "EXECUTE",
                "executeDisabled", executeDisabled,
                "previewDisabled", customBlocked || !previewRegistered,
                "previewReason", !previewRegistered
                        ? "Preview bridge is not registered yet."
                        : customBlocked
                        ? "Custom ScriptCore actions are rejected from UI preview."
                        : "Runs server preflight without mutating state.",
                "executeReason", customBlocked
                        ? "Custom ScriptCore actions are rejected from UI execution."
                        : !executeRegistered
                        ? "Execute bridge is not registered yet."
                        : !configAllowed
                        ? "Enable scriptcore.allow_screencore_ui_actions to execute from Terminal."
                        : "Sends a trusted ScriptCore execute intent for this slot."));
    }

    private static Map<String, Object> scriptDiagnosticRow(Object diagnostic) {
        String severity = stringMethod(diagnostic, "severity", "INFO").toLowerCase(Locale.ROOT);
        return row("severity", severity,
                "status", statusKey(severity),
                "code", stringMethod(diagnostic, "code", "SCRIPTCORE_INFO"),
                "message", stringMethod(diagnostic, "message", ""),
                "definitionId", optionalString(method(diagnostic, "definitionId"), ""));
    }

    private static boolean scriptCoreUiActionsAllowed() {
        try {
            Class<?> config = Class.forName("com.knoxhack.echo.scriptcore.config.ScriptCoreConfig");
            Method method = config.getMethod("screenCoreUiActionsAllowed");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static Object settings(List<String> path) {
        if (path.size() > 1 && "options".equals(path.get(1))) {
            return List.of(
                    setting("Interface Density", TerminalClientOptions.interfaceDensity().name()),
                    setting("Terminal Zoom", TerminalClientOptions.terminalZoom().label()),
                    setting("Navigation Style", TerminalClientOptions.navigationStyle().name()),
                    setting("Visual Level", TerminalClientOptions.visualLevel.name()),
                    setting("Large Text", TerminalClientOptions.largeTextMode() ? "enabled" : "off"),
                    setting("High Contrast", TerminalClientOptions.highContrastMode() ? "enabled" : "off"),
                    setting("Reduced Clutter", TerminalClientOptions.reducedClutterMode() ? "enabled" : "off"),
                    setting("HUD Notices", TerminalClientOptions.missionHudNotifications ? "enabled" : "off"),
                    setting("ScreenCore", TerminalClientOptions.useScreenCore() ? "allowed" : "disabled"),
                    setting("ScreenCore Shell", TerminalClientOptions.screenCoreExperimentalTabs() ? "enabled" : "legacy"));
        }
        return resolveNested(row("useScreenCore", TerminalClientOptions.useScreenCore(),
                "experimentalTabs", TerminalClientOptions.screenCoreExperimentalTabs(),
                "debug", TerminalClientOptions.screenCoreDebug(),
                "missionHudNotifications", TerminalClientOptions.missionHudNotifications,
                "screenCoreLabel", TerminalClientOptions.screenCoreExperimentalTabs() ? "SCREENCORE" : "LEGACY",
                "screenCoreStatusKey", TerminalClientOptions.screenCoreExperimentalTabs() ? "ready" : "warning"),
                path, 1);
    }

    private static Map<String, Object> setting(String label, String value) {
        return row("label", label, "value", value);
    }

    private static Object screenCore(List<String> path) {
        return resolveNested(row("present", TerminalScreenCoreBridge.screenCorePresent(),
                "useScreenCore", TerminalClientOptions.useScreenCore(),
                "matchExistingLayout", TerminalClientOptions.screenCoreMatchExistingLayout(),
                "experimentalTabs", TerminalClientOptions.screenCoreExperimentalTabs(),
                "debug", TerminalClientOptions.screenCoreDebug()), path, 1);
    }

    private static Object fallback(EchoDataContext context, List<String> path) {
        Identifier active = activeTabId(context);
        Map<String, Object> tab = activeTab(context);
        TerminalStatusSnapshot snapshot = statusSnapshot();
        String state = TerminalScreenCoreBridge.migrationState(active);
        return resolveNested(row("title", tab.getOrDefault("title", "Terminal Module"),
                "shortTitle", tab.getOrDefault("shortTitle", "Module"),
                "summary", tab.getOrDefault("summary", "Module data is available through shared Terminal services."),
                "subtitle", tab.getOrDefault("navSubtitle", tab.getOrDefault("groupLabel", "Terminal")),
                "section", tab.getOrDefault("groupLabel", "Terminal"),
                "state", state,
                "stateLabel", rendererLabel(active),
                "stateStatus", rendererStatus(active),
                "routeCount", snapshot.routeCount(),
                "chapterCount", snapshot.chapterCount(),
                "diagnosticCount", snapshot.diagnosticCount(),
                "pendingRewards", snapshot.pendingRewards(),
                "reason", fallbackReason(active),
                "actions", fallbackActions(active, snapshot)), path, 1);
    }

    private static String fallbackReason(Identifier active) {
        if (active != null && "echorelictech".equals(active.getNamespace())) {
            return "RelicTech is linked through mission, archive, recipe, and discovery services. Use the tactical links below to open the live route surfaces.";
        }
        if (active != null && active.getPath().contains("guide")) {
            return "ECHO Guide records are folded into the active command deck, route browser, field archive, and terminal settings.";
        }
        return "Connected through shared route, diagnostic, reward, and chapter services. Use the native actions below for operational navigation.";
    }

    private static List<Map<String, Object>> fallbackActions(Identifier active, TerminalStatusSnapshot snapshot) {
        if (active != null && "echorelictech".equals(active.getNamespace())) {
            return List.of(
                    quickLink("RelicTech Mission Line", "Open Survival Route with RelicTech and route-linked salvage records.",
                            MainSurvivalQuestProvider.TAB_ID, "active", "ROUTE"),
                    quickLink("Field Archive", "Review RelicTech field notes, cache signals, and recovered fragments.",
                            id("archives"), "warning", "INTEL"),
                    quickLink("Recipe Index", "Inspect RelicTech parts, machines, and crafting dependencies.",
                            id("recipe_index"), "info", "INDEX"),
                    quickLink("Addon Modules", "Open installed ECHO modules and RelicTech chapter status.",
                            id("addons"), "ready", "MODULE"));
        }
        return List.of(
                quickLink("Open Survival Route", "Jump to native route objectives and mission handoff.",
                        MainSurvivalQuestProvider.TAB_ID, "active", "ROUTE"),
                quickLink("Command Deck", "Return to the tactical overview and priority queue.",
                        id("overview"), "ready", "DECK"),
                quickLink("Route Records", snapshot.routeCount() + " shared route record(s).",
                        id("route_records"), snapshot.routeCount() > 0 ? "ready" : "info", "SYNC"),
                quickLink("Settings", "Adjust ScreenCore, cyberglass, and accessibility options.",
                        id("settings"), "info", "CONFIG"));
    }

    private static Map<String, Object> quickLink(String title, String summary, Identifier tabId) {
        return quickLink(title, summary, tabId, "ready", "OPEN");
    }

    private static Map<String, Object> quickLink(
            String title, String summary, Identifier tabId, String statusKey, String badge) {
        return row("title", title, "summary", summary, "tabId", tabId.toString(),
                "actionValue", tabId.toString(),
                "pageId", TerminalScreenCoreBridge.pageForTab(tabId).toString(),
                "status", "OPEN",
                "statusKey", statusKey,
                "badge", badge);
    }

    private static Map<String, Object> signal(String title, String summary, String statusKey, String badge) {
        return row("title", title, "summary", summary, "statusKey", statusKey, "badge", badge);
    }

    private static TerminalMissionProvider missionProviderFor(Identifier tabId) {
        if (VanillaJourneyProvider.TAB_ID.equals(tabId)) {
            return VanillaJourneyProvider.INSTANCE;
        }
        return MainSurvivalQuestProvider.INSTANCE;
    }

    private static Identifier providerRouteTarget(Identifier providerId) {
        return VanillaJourneyProvider.CHAPTER_ID.equals(providerId)
                ? VanillaJourneyProvider.TAB_ID
                : MainSurvivalQuestProvider.TAB_ID;
    }

    private static Optional<TerminalMissionChapter> sourceChapterForMission(
            TerminalMissionProvider provider,
            TerminalMissionDefinition mission) {
        if (provider != MainSurvivalQuestProvider.INSTANCE || mission == null) {
            return Optional.empty();
        }
        return MainSurvivalQuestProvider.INSTANCE.sourceChapter(player(), mission.id());
    }

    private static TerminalMissionChapter safeChapter(TerminalMissionProvider provider) {
        try {
            TerminalMissionChapter chapter = provider.chapter();
            return chapter == null ? MainSurvivalQuestProvider.INSTANCE.chapter() : chapter;
        } catch (RuntimeException exception) {
            return MainSurvivalQuestProvider.INSTANCE.chapter();
        }
    }

    private static List<TerminalMissionDefinition> safeMissions(TerminalMissionProvider provider) {
        try {
            List<TerminalMissionDefinition> missions = provider.missions(player());
            return missions == null ? List.of() : missions.stream().filter(Objects::nonNull).toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static TerminalMissionSnapshot safeSnapshot(TerminalMissionProvider provider, Identifier missionId) {
        try {
            TerminalMissionSnapshot snapshot = provider.snapshot(player(), missionId);
            return snapshot == null
                    ? new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                            "LOCKED", "Mission state unavailable.", "Use the fallback Terminal renderer.", List.of())
                    : snapshot;
        } catch (RuntimeException exception) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", "Mission state failed to resolve.", "Use the fallback Terminal renderer.", List.of());
        }
    }

    private static TerminalMissionRole safeRole(
            TerminalMissionProvider provider,
            TerminalMissionDefinition mission,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionRole role = provider.role(player(), mission, snapshot);
            return role == null ? TerminalMissionRole.fallback(mission, snapshot) : role;
        } catch (RuntimeException exception) {
            return TerminalMissionRole.fallback(mission, snapshot);
        }
    }

    private static Identifier safeRouteAnchor(
            TerminalMissionProvider provider,
            TerminalMissionDefinition mission,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            return provider.routeAnchor(player(), mission, snapshot, role).orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static List<TerminalMissionIntelUnlock> safeIntelUnlocks(
            TerminalMissionProvider provider,
            TerminalMissionDefinition mission,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            List<TerminalMissionIntelUnlock> unlocks = provider.intelUnlocks(player(), mission, snapshot, role);
            return unlocks == null ? List.of() : unlocks.stream().filter(Objects::nonNull).distinct().toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Player player() {
        try {
            return Minecraft.getInstance().player;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static TerminalScreenCoreUiState state() {
        return TerminalScreenCoreUiState.current();
    }

    private static Optional<Map<String, Object>> selectedRow(List<Map<String, Object>> rows, Identifier id) {
        return selectedRow(rows, id == null ? "" : id.toString());
    }

    private static Optional<Map<String, Object>> selectedRow(List<Map<String, Object>> rows, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return rows.stream()
                .filter(row -> id.equals(String.valueOf(row.get("id"))))
                .findFirst();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String statusKey(String status) {
        return switch (normalize(status)) {
            case "claimable", "completed", "claimed" -> "success";
            case "unlocked", "active" -> "active";
            case "warning", "blocked" -> "warning";
            case "critical", "error" -> "danger";
            case "locked", "view_only", "view-only" -> "locked";
            default -> "info";
        };
    }

    private static String rendererLabel(Identifier tabId) {
        String state = TerminalScreenCoreBridge.migrationState(tabId);
        return switch (state) {
            case "screencore", "external-screencore" -> "ScreenCore";
            case "fallback-default", "external-fallback-default", "external-fallback" -> "Dossier";
            default -> "Module";
        };
    }

    private static String modVersion(String modId) {
        String version = EchoRuntimeModules.metadata(modId, modId).version();
        return version == null || version.isBlank() ? "dev" : version;
    }

    private static String rendererStatus(Identifier tabId) {
        String state = TerminalScreenCoreBridge.migrationState(tabId);
        return switch (state) {
            case "screencore", "external-screencore" -> "ready";
            case "fallback-default", "external-fallback-default", "external-fallback" -> "ready";
            default -> "info";
        };
    }

    private static boolean matchesRecipeQuery(TerminalRecipeEntry recipe, String query) {
        if (query.isBlank()) {
            return true;
        }
        return recipeSearchText(recipe).contains(query);
    }

    private static boolean matchesRecipeQuery(
            TerminalRecipeEntry recipe,
            String query,
            Map<Identifier, String> searchTextByRecipe) {
        if (query.isBlank()) {
            return true;
        }
        return searchTextByRecipe.computeIfAbsent(recipe.id(), ignored -> recipeSearchText(recipe)).contains(query);
    }

    private static String recipeSearchText(TerminalRecipeEntry recipe) {
        StringBuilder text = new StringBuilder()
                .append(recipe.id()).append(' ')
                .append(recipe.title()).append(' ')
                .append(recipe.categoryId()).append(' ')
                .append(itemId(recipe.machine())).append(' ')
                .append(itemLabel(recipe.machine())).append(' ')
                .append(recipeSummary(recipe));
        for (TerminalRecipeSlot slot : recipe.slots()) {
            text.append(' ').append(slot.label());
            for (ItemStack stack : slot.stacks()) {
                text.append(' ').append(itemId(stack)).append(' ').append(itemLabel(stack));
            }
        }
        return normalize(text.toString());
    }

    private static String recipeSummary(TerminalRecipeEntry recipe) {
        String outputs = slotSummary(recipe, TerminalRecipeSlot.Role.OUTPUT);
        String inputs = slotSummary(recipe, TerminalRecipeSlot.Role.INPUT);
        if (!outputs.isBlank() && !inputs.isBlank()) {
            return inputs + " -> " + outputs;
        }
        if (!outputs.isBlank()) {
            return "Produces " + outputs;
        }
        if (!inputs.isBlank()) {
            return "Uses " + inputs;
        }
        return recipe.locked() ? "Locked provider recipe." : "Provider recipe entry.";
    }

    private static String slotSummary(TerminalRecipeEntry recipe, TerminalRecipeSlot.Role role) {
        List<String> labels = new ArrayList<>();
        for (TerminalRecipeSlot slot : recipe.slots()) {
            if (slot.role() != role) {
                continue;
            }
            if (!slot.label().isBlank()) {
                labels.add(slot.label());
            }
            for (ItemStack stack : slot.stacks()) {
                String label = itemLabel(stack);
                if (!label.isBlank()) {
                    labels.add(label);
                }
            }
        }
        if (role == TerminalRecipeSlot.Role.MACHINE && !recipe.machine().isEmpty()) {
            labels.add(itemLabel(recipe.machine()));
        }
        return labels.stream().filter(value -> !value.isBlank()).distinct().limit(4).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String itemLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return stack.getHoverName().getString();
    }

    private static Object method(Object target, String name) {
        if (target == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            if (method.getParameterCount() == 0) {
                return method.invoke(target);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static String stringMethod(Object target, String name, String fallback) {
        Object value = method(target, name);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int intMethod(Object target, String name) {
        return number(method(target, name));
    }

    private static long longMethod(Object target, String name) {
        Object value = method(target, name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static List<?> listMethod(Object target, String name) {
        Object value = method(target, name);
        return value instanceof List<?> list ? list : List.of();
    }

    private static String optionalString(Object value, String fallback) {
        if (value instanceof Optional<?> optional) {
            return optional.map(String::valueOf).orElse(fallback);
        }
        return value == null ? fallback : String.valueOf(value);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static String iconItemId(ItemStack stack, String fallback) {
        String id = itemId(stack);
        if (!id.isBlank()) {
            return id;
        }
        return fallback == null || fallback.isBlank() ? REQUIREMENT_CUSTOM_FALLBACK_ICON : fallback;
    }

    private static int iconCount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 1 : Math.max(1, stack.getCount());
    }

    private static String requirementFallbackIcon(TerminalMissionRequirement.Kind kind) {
        return switch (kind == null ? TerminalMissionRequirement.Kind.CUSTOM : kind) {
            case ITEM, BLOCK -> REQUIREMENT_ITEM_FALLBACK_ICON;
            case EQUIPMENT -> REQUIREMENT_EQUIPMENT_FALLBACK_ICON;
            case ENTITY_KILL -> REQUIREMENT_ENTITY_FALLBACK_ICON;
            case LOCATION -> REQUIREMENT_LOCATION_FALLBACK_ICON;
            case CUSTOM -> REQUIREMENT_CUSTOM_FALLBACK_ICON;
        };
    }

    private static String color(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static Object resolveNested(Object value, List<String> path, int start) {
        Object current = value;
        for (int index = start; index < path.size(); index++) {
            if (current == null) {
                return null;
            }
            String key = path.get(index);
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else if (current instanceof List<?> list) {
                try {
                    int listIndex = Integer.parseInt(key);
                    current = listIndex >= 0 && listIndex < list.size() ? list.get(listIndex) : null;
                } catch (NumberFormatException exception) {
                    return null;
                }
            } else {
                return current;
            }
        }
        return current;
    }

    private static Map<String, Object> row(Object... entries) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            Object key = entries[i];
            if (key != null) {
                row.put(String.valueOf(key), entries[i + 1] == null ? "" : entries[i + 1]);
            }
        }
        return row;
    }

    private record TerminalStatusSnapshot(int playerKey, int tick, EchoHazardTelemetry telemetry,
            int pendingRewards, List<EchoDiagnosticBlocker> diagnostics, List<Map<String, Object>> diagnosticRows,
            List<EchoRouteRecord> routes, int chapterCount) {
        private int diagnosticCount() {
            return diagnostics.size();
        }

        private int routeCount() {
            return routes.size();
        }

        private String topDiagnosticTitle() {
            return diagnosticRows.isEmpty() ? "No active blockers" : String.valueOf(diagnosticRows.get(0).get("title"));
        }

        private String topDiagnosticSummary() {
            return diagnosticRows.isEmpty()
                    ? "Command Deck will keep blocker status here when a route stalls."
                    : diagnosticSummary(diagnosticRows.get(0));
        }
    }

    private record TerminalOverviewRouteSnapshot(
            int playerKey,
            int bucket,
            String selectedMissionId,
            List<Map<String, Object>> routeRows,
            List<Map<String, Object>> visibleRouteRows,
            Map<String, Object> activeMission,
            boolean degraded) {
        private boolean matches(int playerKey, int bucket, String selectedMissionId) {
            return this.playerKey == playerKey
                    && this.bucket == bucket
                    && this.selectedMissionId.equals(selectedMissionId == null ? "" : selectedMissionId);
        }

        private TerminalOverviewRouteSnapshot withScope(int bucket, boolean degraded) {
            return new TerminalOverviewRouteSnapshot(playerKey, bucket, selectedMissionId,
                    routeRows, visibleRouteRows, activeMission, degraded);
        }
    }

    private record TerminalRecipeUiSnapshot(
            long registryRevision,
            int playerKey,
            int tickBucket,
            String query,
            String queryText,
            String mode,
            String category,
            TerminalRecipeSnapshot snapshot,
            List<Map<String, Object>> categories,
            List<Map<String, Object>> recipes,
            int visibleCount) {
        private boolean matches(long registryRevision, int playerKey, int tickBucket, String query, String mode,
                String category) {
            return this.registryRevision == registryRevision
                    && this.playerKey == playerKey
                    && this.tickBucket == tickBucket
                    && this.query.equals(query == null ? "" : query)
                    && this.mode.equals(mode == null ? "" : mode)
                    && this.category.equals(category == null ? "" : category);
        }
    }
}

record TerminalTabPageCandidate(Identifier tabId, Identifier pageId) {
}

package com.knoxhack.signalos.content;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoChapterCapability;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoModuleInfo;
import com.knoxhack.echocore.api.EchoProfile;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsApi;
import com.knoxhack.signalos.api.SignalOsApp;
import com.knoxhack.signalos.api.SignalOsDataProvider;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsProviderStatus;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalDiagnosticProvider;
import com.knoxhack.signalos.api.TerminalPage;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class SignalOsBuiltinContent {
    private static final Identifier CORE_CHAPTER = id("signalos");

    private SignalOsBuiltinContent() {
    }

    public static void register() {
        registerDesktopApps();
        registerCorePages();
        registerCoreArchive();
        SignalOsApi.registerDiagnostics(new CoreDiagnosticsProvider());
        SignalOsApi.registerDataProvider(new EchoLinkDataProvider());
    }

    private static void registerDesktopApps() {
        app("home", "Home", "home", "System overview and launch surface.", 0, 0xFF66E8FF);
        app("files", "Files", "files", "Browse records available on the current SignalOS network.", 10, 0xFF8BD7FF);
        app("notes", "Notes", "notes", "Create and review local operator notes.", 20, 0xFFFFD166);
        app("logs", "Logs", "logs", "Read system logs, archive lines, and Echo-linked reports.", 30, 0xFF91F7A5);
        app("network_monitor", "Network Monitor", "network", "Inspect linked terminals, workstations, racks, and relays.", 40, 0xFF66E8FF);
        app("settings", "Settings", "settings", "Operator preferences and SignalOS shell state.", 50, 0xFFB6A7FF);
        app("data_vault", "Data Vault", "data_vault", "Archive and inspect discovered data records.", 60, 0xFF9FD1FF);
        app("echo_link", "Echo Link", "echo_link", "Optional bridge into Echo Core module, route, and diagnostic state.", 70, 0xFFFF8FA3);
        app("signalnet", "SignalNet", "signalnet", "Browse the curated in-world network.", 80, 0xFF66E8FF);
        app("missions", "Missions", "missions", "Legacy SignalOS mission surface.", 100, 0xFFFFD166);
        app("archives", "Archives", "archives", "Legacy SignalOS archive surface.", 110, 0xFF9FD1FF);
        app("rewards", "Rewards", "rewards", "Shared terminal reward inbox.", 120, 0xFF91F7A5);
        app("diagnostics", "Diagnostics", "diagnostics", "SignalOS provider diagnostics.", 130, 0xFFFFB454);
    }

    private static void app(String path, String title, String type, String summary, int order, int accent) {
        SignalOsApi.registerApp(SignalOsApp.builder(id(path))
                .title(title)
                .type(type)
                .summary(summary)
                .order(order)
                .accentColor(accent)
                .build());
    }

    private static void registerCorePages() {
        SignalOsApi.registerPage(page("signalos_missions", "Missions", "missions", 0));
        SignalOsApi.registerPage(page("signalos_archives", "Archives", "archives", 10));
        SignalOsApi.registerPage(page("signalos_rewards", "Rewards", "rewards", 20));
        SignalOsApi.registerPage(page("signalos_diagnostics", "Diagnostics", "diagnostics", 30));
    }

    private static void registerCoreArchive() {
        SignalOsApi.registerArchive(TerminalArchiveRecord.builder(id("runtime_interface"))
                .chapter(CORE_CHAPTER.toString())
                .title("Runtime Interface")
                .group("SignalOS")
                .status("OPEN")
                .order(10)
                .line("SignalOS core content is registered through the Java API and merged with JSON content at reload time.")
                .line("The desktop shell resolves built-in apps first, then Java renderers, JSON record views, and finally unsupported metadata views.")
                .build());
    }

    private static TerminalPage page(String path, String title, String type, int order) {
        return TerminalPage.builder(id(path).toString())
                .chapter(CORE_CHAPTER.toString())
                .title(title)
                .type(type)
                .order(order)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }

    private static final class CoreDiagnosticsProvider implements TerminalDiagnosticProvider {
        @Override
        public Identifier id() {
            return SignalOsBuiltinContent.id("core_diagnostics");
        }

        @Override
        public List<Diagnostic> diagnostics(Player player) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            int chapterCount = SignalOsContentRegistry.chapters().size();
            int missionCount = SignalOsContentRegistry.missions().size();
            int archiveCount = SignalOsContentRegistry.archives().size();
            diagnostics.add(new Diagnostic(
                    SignalOsBuiltinContent.id("core_diagnostics/content_index"),
                    "Content Index",
                    chapterCount + " chapter(s), " + missionCount + " mission(s), " + archiveCount + " archive record(s).",
                    chapterCount == 0 ? Severity.WARNING : Severity.INFO));

            diagnostics.add(new Diagnostic(
                    SignalOsBuiltinContent.id("core_diagnostics/operator_link"),
                    "Operator Link",
                    player == null ? "No local player context." : "Operator context available.",
                    player == null ? Severity.WARNING : Severity.INFO));

            int pendingRewards = player == null ? 0 : SignalOsTerminalServices.pendingRewardCount(player);
            diagnostics.add(new Diagnostic(
                    SignalOsBuiltinContent.id("core_diagnostics/reward_relay"),
                    "Reward Relay",
                    pendingRewards + " cached reward item(s) visible from the linked terminal.",
                    Severity.INFO));
            for (SignalOsProviderStatus status : SignalOsContentRegistry.providerStatuses(player)) {
                diagnostics.add(new Diagnostic(
                        SignalOsBuiltinContent.id("core_diagnostics/provider/" + EchoLinkDataProvider.safePath(status.id())),
                        status.label() + " Provider",
                        status.status() + (status.detail().isBlank() ? "" : " | " + status.detail()),
                        status.severity()));
            }
            return diagnostics;
        }

        @Override
        public int order() {
            return 0;
        }
    }

    private static final class EchoLinkDataProvider implements SignalOsDataProvider {
        @Override
        public Identifier id() {
            return SignalOsBuiltinContent.id("echo_link_data");
        }

        @Override
        public List<SignalOsDataRecord> records(Player player) {
            List<SignalOsDataRecord> records = new ArrayList<>();
            records.add(new SignalOsDataRecord(
                    SignalOsBuiltinContent.id("record/platform_summary"),
                    "Echo Platform Summary",
                    "echo",
                    "Echo Core",
                    EchoCoreServices.platformProviderSummary(),
                    10,
                    false));
            EchoProfile profile = EchoCoreServices.profile(player);
            records.add(new SignalOsDataRecord(
                    SignalOsBuiltinContent.id("record/profile"),
                    "Operator Profile",
                    "profile",
                    "Echo Core",
                    "Callsign: " + profile.callsign()
                            + " | Difficulty: " + profile.difficulty()
                            + " | Completed arcs: " + profile.completedArcs().size()
                            + " | Discovered records: " + profile.discoveredRecords().size(),
                    20,
                    false));
            EchoHazardTelemetry hazards = EchoCoreServices.hazardTelemetry(player);
            records.add(new SignalOsDataRecord(
                    SignalOsBuiltinContent.id("record/hazards"),
                    "Hazard Telemetry",
                    "hazard",
                    "Echo Core",
                    hazards.statusLine()
                            + " | hydration=" + hazards.hydration()
                            + " oxygen=" + hazards.oxygen()
                            + " radiation=" + hazards.radiation()
                            + " toxicAir=" + hazards.toxicAir()
                            + " exposure=" + hazards.exposure(),
                    30,
                    !hazards.warning()));
            records.add(new SignalOsDataRecord(
                    SignalOsBuiltinContent.id("record/theme_sound"),
                    "Theme And Sound Links",
                    "system",
                    "Echo Core",
                    "ThemeCore: " + (EchoCoreServices.themeCoreAvailable()
                            ? EchoCoreServices.themeService().currentThemeName() : "fallback")
                            + " | SoundCore: " + (EchoCoreServices.soundCoreAvailable()
                            ? "available, active events=" + EchoCoreServices.soundService().activeEvents().size()
                            : "fallback"),
                    40,
                    false));
            int capabilityIndex = 0;
            for (EchoChapterCapability capability : EchoCoreServices.chapterCapabilities(player)) {
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/chapter/" + safePath(capability.id())),
                        capability.displayName(),
                        "chapter",
                        "Echo Chapter Capabilities",
                        (capability.installed() ? "Installed" : "Missing")
                                + " | " + (capability.available() ? "Available" : "Unavailable")
                                + (capability.statusLine().isBlank() ? "" : " | " + capability.statusLine()),
                        50 + capabilityIndex++,
                        !capability.available()));
            }
            int index = 0;
            for (EchoModuleInfo module : EchoCoreServices.moduleReport()) {
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/module/" + module.modId()),
                        module.displayName(),
                        "module",
                        "Echo Module Catalog",
                        (module.loaded() ? "Loaded" : "Missing") + " | " + module.modId()
                                + (module.version().isBlank() ? "" : " | " + module.version())
                                + " | " + module.projectPath(),
                        100 + index++,
                        !module.loaded()));
            }
            index = 0;
            for (EchoDiagnosticBlocker blocker : EchoCoreServices.diagnostics(player)) {
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/diagnostic/" + safePath(blocker.id())),
                        blocker.title(),
                        "diagnostic",
                        "Echo Diagnostics",
                        blocker.severity().name() + " | " + blocker.detail(),
                        300 + index++,
                        false));
            }
            index = 0;
            for (EchoRouteRecord route : EchoCoreServices.routeRecords(player)) {
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/route/" + safePath(route.id())),
                        route.title(),
                        "route",
                        "Echo Route Records",
                        route.status() + " | " + route.summary(),
                        500 + index++,
                        route.complete()));
            }
            index = 0;
            for (EchoDiscoveryEntry entry : EchoCoreServices.discoveryEntries(player)) {
                EchoDiscoveryState state = EchoCoreServices.discoveryState(player, entry);
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/discovery/" + safePath(entry.id())),
                        state == EchoDiscoveryState.LOCKED ? entry.lockedHintTitle() : entry.revealedTitle(),
                        "discovery",
                        "Echo Discovery",
                        state.displayName() + " | "
                                + (state == EchoDiscoveryState.LOCKED ? entry.hintText() : entry.revealedSummary()),
                        700 + index++,
                        state == EchoDiscoveryState.CHECKED));
            }
            index = 0;
            for (EchoFactionProfile profileEntry : EchoCoreServices.factionProfiles(player)) {
                records.add(new SignalOsDataRecord(
                        SignalOsBuiltinContent.id("record/faction/" + safePath(profileEntry.definition().id())),
                        profileEntry.definition().displayName(),
                        "faction",
                        "Echo Factions",
                        profileEntry.standingLine()
                                + " | contacted=" + profileEntry.contacted()
                                + " | completed contracts=" + profileEntry.completedContracts()
                                + profileEntry.activeContractId().map(id -> " | active=" + id).orElse(""),
                        900 + index++,
                        !profileEntry.contacted()));
            }
            return records;
        }

        @Override
        public int order() {
            return 0;
        }

        @Override
        public SignalOsProviderStatus providerStatus(Player player) {
            return new SignalOsProviderStatus(id(), "Echo Link Data", "ONLINE",
                    TerminalDiagnosticProvider.Severity.INFO, EchoCoreServices.platformProviderSummary());
        }

        private static String safePath(Identifier id) {
            if (id == null) {
                return "unknown";
            }
            return safePath(id.getNamespace() + "/" + id.getPath());
        }

        private static String safePath(String value) {
            if (value == null || value.isBlank()) {
                return "unknown";
            }
            return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        }
    }
}

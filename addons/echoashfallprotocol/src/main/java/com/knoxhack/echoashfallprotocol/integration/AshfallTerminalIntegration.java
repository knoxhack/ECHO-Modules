package com.knoxhack.echoashfallprotocol.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionProfile;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.client.hud.HudState;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionActions;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionRoute;
import com.knoxhack.echoashfallprotocol.echo.EchoGuideManager;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.EndgameMissionProgress;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.NexusAccessRules;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.NexusChoiceService;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventStatus;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.network.ArchiveIntelReadPacket;
import com.knoxhack.echoashfallprotocol.network.DroneCommandPacket;
import com.knoxhack.echoashfallprotocol.network.ModNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.world.ExplorationPoiCatalog;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalIcon;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelKind;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionVisuals;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Optional bridge that contributes Ashfall content to the modular ECHO Terminal.
 */
public final class AshfallTerminalIntegration {
    private static final AtomicBoolean CLIENT_REGISTERED = new AtomicBoolean(false);
    private static final boolean LEGACY_NEXUS_TERMINAL_ENABLED = false;
    private static final String ASHFALL_CHAPTER_ID = "ashfall_protocol";

    private static final Identifier OVERVIEW = id("overview");
    private static final Identifier MISSIONS = id("missions");
    private static final Identifier SIDE_OPS = id("side_ops");
    private static final Identifier ARCHIVES = Identifier.fromNamespaceAndPath("echoterminal", "archives");
    private static final Identifier STATUS = id("status");
    private static final Identifier DRONE = id("drone");
    private static final Identifier CODEX = id("codex");
    private static final Identifier WORLD = id("world");
    private static final Identifier NEXUS = id("nexus");
    private static final Identifier TURN_IN = id("turn_in_mission");
    private static final Identifier CLAIM_REWARDS = id("claim_terminal_rewards");
    private static final Identifier DRONE_COMMAND = id("drone_command");
    private static final Identifier NEXUS_CHOICE = id("nexus_choice");
    private static final Identifier NEXUS_WARFRONT = id("nexus_warfront");
    private static final AshfallMissionProvider ASHFALL_MISSION_PROVIDER = new AshfallMissionProvider();
    private static final AshfallSideOpsProvider ASHFALL_SIDE_OPS_PROVIDER = new AshfallSideOpsProvider();

    private AshfallTerminalIntegration() {
    }

    public static void register() {
        registerClient();
    }

    public static void registerClient() {
        AshfallTerminalCommonIntegration.register();
        if (!CLIENT_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        registerTab(new OverviewTab(), ashfallProfile(90));
        registerTab(new MissionBrowserTab(ASHFALL_MISSION_PROVIDER), ashfallProfile(100));
        registerTab(MissionBrowserTab.sideOps(ASHFALL_SIDE_OPS_PROVIDER), ashfallProfile(110));
        registerTab(new StatusTab(), ashfallProfile(180));
        registerTab(new ArchivesTab(), TerminalNavigationProfile.terminal(140));
        registerTab(new DroneTab(), ashfallProfile(190));
        registerTab(new CodexTab(), ashfallProfile(150));
        registerTab(new WorldTab(), ashfallProfile(130));
        if (LEGACY_NEXUS_TERMINAL_ENABLED && !nexusProtocolLoaded()) {
            TerminalTabRegistry.register(new NexusTab());
            TerminalNavigationProfiles.register(NEXUS, ashfallProfile(220));
        }
        TerminalRecipeRegistry.register(AshfallTerminalRecipeProvider.INSTANCE);
    }

    private static void registerTab(TerminalTab tab, TerminalNavigationProfile profile) {
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), profile);
    }

    private static TerminalNavigationProfile ashfallProfile(int order) {
        return TerminalNavigationProfile.chapter("ashfall", "Chapter 1: Ashfall Protocol", "C1", order);
    }

    private static final class AshfallAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return ASHFALL_CHAPTER_ID;
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            if (player == null) {
                return new TerminalAddonInfo(
                        "Chapter 1 survival route, terminal repair, field safety, drone support, and early route intel.",
                        List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", 0xFF66D9FF)),
                        List.of(new TerminalAddonSection("Start Feed",
                                List.of("Open Ashfall Command after player telemetry is available."))),
                        links(),
                        guide());
            }
            QuestData quest = QuestData.get(player);
            SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
            MissionUxSummary current = MissionUxSummary.current(player, quest);
            return new TerminalAddonInfo(
                    "Chapter 1 survival route, terminal repair, field safety, drone support, and early route intel.",
                    List.of(
                            new TerminalAddonMetric("Phase", String.valueOf(quest.getCurrentPhase() + 1),
                                    "active Ashfall route phase", TerminalUi.GREEN),
                            new TerminalAddonMetric("Mission", String.valueOf(quest.getCurrentMissionIndex() + 1),
                                    current.shortTitle(), TerminalUi.CYAN),
                            new TerminalAddonMetric("Hydration", survival.getHydration() + "%",
                                    "field survival reserve", survival.getHydration() <= 30 ? TerminalUi.AMBER : TerminalUi.GREEN),
                            new TerminalAddonMetric("Filter", Math.round(survival.getFilterPercent() * 100.0F) + "%",
                                    survival.hasMask() ? "mask equipped" : "mask not confirmed",
                                    survival.getFilterPercent() <= 0.25F ? TerminalUi.AMBER : TerminalUi.CYAN)),
                    List.of(new TerminalAddonSection("Start Feed", List.of(
                            current.objectiveSummary(),
                            current.nextStep(),
                            survival.isSafeZone() ? "Safe zone detected." : "No safe zone detected yet."))),
                    links(),
                    guide());
        }

        private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.mainline(1, 10, "Start here",
                    "Begin with Ashfall to learn survival basics, repair the terminal, stabilize camp, and open the first route signals.",
                    List.of(
                            "Secure water, shelter, food, and filters before long trips.",
                            "Open Ashfall Command or Protocol Roadmap for the next mission.",
                            "Use Survival Route when you want the complete roadmap."));
        }

        private static List<TerminalAddonLink> links() {
            return List.of(
                    new TerminalAddonLink(OVERVIEW, "Ashfall Command", "Chapter 1 field dashboard", 0xFF66D9FF),
                    new TerminalAddonLink(MISSIONS, "Protocol Roadmap", "Active Ashfall mission chain", 0xFF92F7A6),
                    new TerminalAddonLink(WORLD, "Route Map", "POIs, routes, and field map", 0xFFC09BFF),
                    new TerminalAddonLink(CODEX, "Survival Index", "Intel and recipe planning", 0xFFFFD166));
        }
    }

    private static boolean nexusProtocolLoaded() {
        try {
            return EchoRuntimeModules.isLoaded("echonexusprotocol");
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall terminal Nexus ownership check failed; keeping legacy Nexus terminal available.", exception);
            return false;
        }
    }

    private static void registerFieldManualEntries() {
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_survival_manual"),
                "Outpost Survival",
                "Ashfall Survival Manual",
                "OPEN",
                List.of(
                        "Gridfall did not leave a normal wilderness behind. The safe route is water, shelter, food, filters, and a powered outpost before distance.",
                        "Mission records are tactical briefings, not shortcuts. ECHO-7 can show the road ahead, but field validation still confirms every turn-in and reward.",
                        "Hazards stack quickly: toxic air, radiation, cold, acid contact, storms, and Nexus anomalies each need a different countermeasure."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_weather_protocols"),
                "Outpost Survival",
                "Weather Event Protocols",
                "OPEN",
                List.of(
                        "ECHO-7 treats weather as a route condition. Radiation storms, acid rain, blackouts, ash storms, cryo fronts, and Nexus surges each change what a safe expedition looks like.",
                        "Counters are practical: cover, filters, reserve power, clean water, heat, RadAway, scrubber pockets, or distance from unstable sources.",
                        "The Vitals Scan tab shows the active event, weather override, survival impact, counter guidance, and survived-event counts for route planning."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_systems_manual"),
                "Machine Systems",
                "Field Systems Primer",
                "OPEN",
                List.of(
                        "Recycler, generator, purifier, grinder, refiner, research, cable, and power blocks are the spine of recovery. They turn ruin into repeatable survival.",
                        "Research points unlock perks and schematic categories, while rare schematics let recovered knowledge catch up to field pressure.",
                        "Machine mission turn-ins verify field progress but do not bypass ECHO validation."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_progression_manual"),
                "Protocol Flow",
                "Protocol Roadmap Rules",
                "OPEN",
                List.of(
                        "Locked missions are visible for planning, but completion, rewards, and phase advancement remain gated by QuestData.",
                        "Public beta route guidance ends at Orbital handoff. Legacy Nexus save state remains readable, but Ashfall no longer owns the finale terminal."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_threat_manual"),
                "Threat Dossier",
                "Guardian Threat Dossier",
                "OPEN",
                List.of(
                        "Eight active biome guardian signals hold the old grid in place. Each one is tied to Radwarden containment, Crashbreak salvage, or Sporebound anomaly interpretation.",
                        "Each guardian has a unique threat profile, owner faction thread, surface entrance, arena route, defender set, and reward bundle. Scan, prepare, descend, and leave nothing unresolved."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_drone_manual"),
                "Machine Systems",
                "Companion Drone Protocols",
                "OPEN",
                List.of(
                        "Drone commands are tactical orders routed through the terminal and confirmed against live field state.",
                        "Follow unlocks immediately. Scout and light require partial repairs, combat and scavenge require operational integrity, and patrol requires enhanced integrity.",
                        "The drone is not only gear. It is ECHO-7's moving witness, and the Scout Drone backup keeps that witness active when the companion shell is gone."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_nexus_manual"),
                "Recovered Lore",
                "Nexus Path Interface",
                "OPEN",
                List.of(
                        "The Nexus Core can commit RESTORE, DESTROY, or CONTROL once the guardian chain, Warfront relays, countermeasure siege, and five Power Nodes are resolved.",
                        "RESTORE repairs what remains, DESTROY breaks the machine that broke the world, and CONTROL binds the grid to your signal. None of these routes are innocent.",
                        "The chosen path is mirrored through ECHO Core services so addon chapters can react without owning Ashfall's route state."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_world_regions"),
                "Recovered Lore",
                "Wasteland Region Field Notes",
                "OPEN",
                List.of(
                        "The Wasteland is open ash-dirt and low cover: sparse survival pressure with early grass tufts as vegetation, not a healed living surface.",
                        "Crash zones carry slag, cables, twisted metal, and scorched debris. City and industrial belts become denser, sharper, and more useful for salvage.",
                        "Toxic swamps, radiation flats, cryogenic ridges, and Nexus scars each announce their danger through terrain language before the terminal confirms the hazard. Learn the terrain before it becomes a meter."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_poi_atlas"),
                "Signal Logs",
                "POI Field Atlas",
                "OPEN",
                List.of(
                        "The Route Map POI Atlas groups every surface template signal under the scanner profile that owns its gameplay identity.",
                        "Individual ruins are not separate save objectives. ECHO tracks the route profile, then lists template variants so you can recognize wreckage, camps, labs, vaults, and faction hubs in the field.",
                        "Muted template rows mean the parent route has not been scanned yet; discovered rows inherit the route hazard, prep kit, and objective state from the scanner profile."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_faction_threads"),
                "Recovered Lore",
                "Faction Signal Threads",
                "OPEN",
                List.of(
                        "Three Ashfall factions now report through Echo Core: Radwarden Compact containment crews, Crashbreak Salvage route builders, and Sporebound Sanctum anomaly interpreters.",
                        "Faction work is not separate from the main route: contacts, contracts, services, and standing all feed the same synced Echo Core record.",
                        "Orbital lanes mirror those same three pressures after the Nexus choice reaches orbit: Radwarden containment, Crashbreak salvage, and Sporebound anomaly reading."),
                false));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static List<EchoFactionProfile> ashfallFactionProfiles(Player player) {
        if (player == null) {
            return List.of();
        }
        return AshfallFactionMap.all().stream()
                .map(factionId -> EchoCoreServices.factionProfile(player, factionId))
                .flatMap(Optional::stream)
                .toList();
    }

    private static String activeFactionContractLine(List<EchoFactionProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return "Active faction contract: Echo Core faction data unavailable.";
        }
        for (EchoFactionProfile profile : profiles) {
            if (profile.activeContractId().isPresent()) {
                return "Active faction contract: " + profile.definition().shortName()
                        + " / " + profile.activeContractId().get().getPath();
            }
        }
        long contacted = profiles.stream().filter(EchoFactionProfile::contacted).count();
        return "Active faction contract: none / contacts " + contacted + "/" + profiles.size();
    }

    private static void turnInCurrentMission(ServerPlayer player, String payload) {
        QuestData quest = QuestData.get(player);
        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        }

        Mission target = AshfallMissionActions.resolveTarget(quest, payload);
        String rejection = AshfallMissionActions.turnInRejection(player, quest, target);
        if (!rejection.isBlank()) {
            AshfallMissionActions.sendTurnInRejection(player, rejection);
            QuestData.syncToClient(player);
            return;
        }

        EchoGuideManager.turnInMission(player, quest, target);
        QuestData.syncToClient(player);
    }

    private static void chooseNexusPath(ServerPlayer player, String payload) {
        NexusChoiceService.applyChoice(player, payload);
    }

    private static void handleNexusWarfront(ServerPlayer player, String payload) {
        NexusCampaignActions.handleTerminalAction(player, payload);
    }

    private abstract static class AshfallTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor;
        private final TerminalTabChrome chrome;
        private final List<Hitbox> hitboxes = new ArrayList<>();

        private AshfallTab(Identifier id, String title, int order, int accentColor) {
            this(id, title, order, accentColor, TerminalTabChrome.fromDescriptor(
                    new TerminalTabDescriptor(id, title, order, accentColor)));
        }

        private AshfallTab(Identifier id, String title, int order, int accentColor, TerminalTabChrome chrome) {
            this.descriptor = new TerminalTabDescriptor(id, title, order, accentColor);
            this.chrome = chrome == null ? TerminalTabChrome.fromDescriptor(descriptor) : chrome;
        }

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (Hitbox hitbox : List.copyOf(hitboxes)) {
                if (hitbox.contains(mouseX, mouseY)) {
                    if (hitbox.enabled()) {
                        hitbox.action().run();
                    } else {
                        context.playRejectedSound();
                    }
                    return true;
                }
            }
            return false;
        }

        protected void beginHitboxes() {
            hitboxes.clear();
        }

        protected void addHitbox(int x, int y, int width, int height, Runnable action) {
            hitboxes.add(new Hitbox(x, y, width, height, true, action));
        }

        protected void addHitbox(int x, int y, int width, int height, boolean enabled, Runnable action) {
            hitboxes.add(new Hitbox(x, y, width, height, enabled, action));
        }

        protected void section(TerminalRenderContext context, GuiGraphicsExtractor graphics, String title, int x, int y) {
            TerminalUi.sectionHeader(context, graphics, title, "", x, y,
                    Math.max(80, context.contentX() + context.contentWidth() - x), descriptor.accentColor());
        }

        protected int wrap(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String text, int x, int y, int width, int color) {
            return TerminalUi.wrap(context, graphics, text, x, y, width, color);
        }

        protected void line(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String text, int x, int y, int width, int color) {
            TerminalUi.line(context, graphics, text, x, y, width, color);
        }

        protected void chip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String label, int x, int y, int width, int color) {
            TerminalUi.statusPill(context, graphics, label, x, y, width, color, false);
        }

        protected void button(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, String label, boolean enabled, double mouseX, double mouseY, Runnable action) {
            boolean effectiveEnabled = enabled && !referenceOnly(context);
            boolean hovered = effectiveEnabled && TerminalUi.inside(mouseX, mouseY, x, y, w, 18);
            TerminalUi.button(context, graphics, x, y, w, label, descriptor.accentColor(), effectiveEnabled, hovered);
            hitboxes.add(new Hitbox(x, y, w, 18, effectiveEnabled, action));
        }

        protected int dataPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                Identifier texture, int x, int y, int w, int h, String title, String detail, float darken) {
            return TerminalUi.dataPanel(context, graphics, texture, x, y, w, h, title, detail,
                    descriptor.accentColor(), darken, TerminalUi.ImageFit.COVER);
        }

        protected int flatDataPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, String title, String detail) {
            return TerminalUi.flatDataPanel(context, graphics, x, y, w, h, title, detail,
                    descriptor.accentColor());
        }

        protected void texturedPanel(GuiGraphicsExtractor graphics,
                Identifier texture, int x, int y, int w, int h, float darken) {
            TerminalUi.hdBackplatePanel(graphics, texture, x, y, w, h, descriptor.accentColor(), darken,
                    TerminalUi.ImageFit.COVER);
        }

        protected void dataRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, String title, String detail, String status,
                boolean selected, boolean hovered, int statusColor) {
            TerminalUi.dataListRow(context, graphics, x, y, w, h, title, detail, status,
                    selected, hovered, descriptor.accentColor(), statusColor);
        }

        protected void texturedButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, String label, Identifier icon, boolean enabled,
                double mouseX, double mouseY, Runnable action) {
            boolean effectiveEnabled = enabled && !referenceOnly(context);
            boolean hovered = effectiveEnabled && TerminalUi.inside(mouseX, mouseY, x, y, w, h);
            if (effectiveEnabled) {
                TerminalUi.primaryCommandButton(context, graphics, x, y, w, h, label, icon,
                        descriptor.accentColor(), hovered);
            } else {
                TerminalUi.disabledCommandButton(context, graphics, x, y, w, h, label, icon);
            }
            addHitbox(x, y, w, h, effectiveEnabled, action);
        }

        protected boolean referenceOnly(TerminalRenderContext context) {
            return TerminalNavigationProfiles.referenceOnlyChapterContext(context);
        }

        protected int drawItems(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<ItemStack> stacks, int x, int y, int width, String emptyLine) {
            if (stacks == null || stacks.isEmpty()) {
                line(context, graphics, emptyLine, x, y, width, TerminalUi.MUTED);
                return y + 12;
            }
            int cy = y;
            for (ItemStack stack : stacks) {
                String count = stack.getCount() > 1 ? stack.getCount() + "x " : "";
                line(context, graphics, "- " + count + stack.getHoverName().getString(), x, cy, width, TerminalUi.TEXT);
                cy += 12;
            }
            return cy;
        }

        protected static int statusColor(QuestData.MissionStatus status) {
            return switch (status) {
                case COMPLETED -> TerminalUi.GREEN;
                case UNLOCKED -> TerminalUi.AMBER;
                case LOCKED -> TerminalUi.MUTED;
            };
        }

        protected static int progressColor(float value) {
            if (value >= 1.0F) {
                return TerminalUi.GREEN;
            }
            if (value > 0.0F) {
                return TerminalUi.AMBER;
            }
            return TerminalUi.MUTED;
        }

        protected static int summaryColor(MissionUxSummary summary) {
            return switch (summary.statusTone()) {
                case "success" -> TerminalUi.GREEN;
                case "active" -> TerminalUi.AMBER;
                default -> TerminalUi.MUTED;
            };
        }

        protected static String phaseTitle(int phase) {
            return MissionRegistry.getPhaseTitle(phase);
        }

        protected static boolean safeComplete(Mission mission, Player player) {
            try {
                return mission.isComplete(player);
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        protected static boolean visible(int y, int height, int viewportY, int viewportH) {
            return y + height >= viewportY && y <= viewportY + viewportH;
        }

        private record Hitbox(int x, int y, int width, int height, boolean enabled, Runnable action) {
            boolean contains(double mouseX, double mouseY) {
                return TerminalUi.inside(mouseX, mouseY, x, y, width, height);
            }
        }
    }

    private static final class OverviewTab extends AshfallTab {
        private OverviewTab() {
            super(OVERVIEW, "ASHFALL COMMAND", 90, 0xFF66D9FF,
                    TerminalTabChrome.of("Ashfall Command", TerminalTabChrome.GROUP_PROTOCOL, "AC",
                            "Active protocol dashboard", 90));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            QuestData quest = QuestData.get(context.player());
            SurvivalData survival = context.player().getData(ModAttachments.SURVIVAL_DATA.get());
            ResearchData research = ResearchData.get(context.player());
            EchoIntel intel = EchoIntel.get(context.player());
            PostNexusData post = PostNexusData.get(context.player());
            Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
            MissionUxSummary currentSummary = MissionUxSummary.current(context.player(), quest);

            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            boolean wide = w >= 760;
            int gap = 14;
            int metricsH = wide ? Math.max(72, Math.min(86, h / 5)) : 86;
            int topH = wide ? Math.max(210, h - metricsH - gap) : Math.max(230, h * 48 / 100);
            if (wide && topH + gap + metricsH > h) {
                topH = Math.max(120, h - metricsH - gap);
            }
            int leftW = wide ? Math.max(390, Math.min(620, w * 58 / 100)) : w;
            int rightX = wide ? x + leftW + gap : x;
            int rightY = wide ? y : y + topH + gap;
            int rightW = wide ? Math.max(260, w - leftW - gap) : w;

            TerminalUi.hdBackplatePanel(graphics, TerminalVisualAssets.CARD_ACTIVE_PROTOCOL_HERO,
                    x, y, leftW, topH, descriptor().accentColor(), 0.56F, TerminalUi.ImageFit.COVER);
            TerminalUi.line(context, graphics, "ACTIVE PROTOCOL", x + 16, y + 15, leftW - 32, descriptor().accentColor());
            TerminalUi.divider(graphics, x + 16, y + 31, leftW - 32, descriptor().accentColor());
            if (current == null) {
                wrap(context, graphics,
                        "Protocol registry sync in progress. Field commands will update when the server link refreshes.",
                        x + 16, y + 40, leftW - 32, TerminalUi.MUTED);
            } else {
                int color = summaryColor(currentSummary);
                Identifier missionIcon = TerminalVisualAssets.missionIconArt(id(current.id()),
                        current.category() == null ? "" : current.category().getDisplayName());
                TerminalUi.iconTextureBadge(graphics, missionIcon, x + 22, y + 56, 58, color, true);
                line(context, graphics, currentSummary.shortTitle(), x + 94, y + 60, leftW - 220, TerminalUi.TEXT);
                chip(context, graphics, currentSummary.statusLabel(), x + leftW - 130, y + 58, 104, color);
                wrap(context, graphics, currentSummary.objectiveSummary(), x + 94, y + 80, leftW - 122, TerminalUi.TEXT);

                int cardH = 70;
                int cardY = y + topH - cardH - 14;
                int progressY = Math.max(y + 112, cardY - 38);
                line(context, graphics, "PROTOCOL PROGRESS", x + 16, progressY, leftW - 32, descriptor().accentColor());
                TerminalUi.progress(graphics, x + 16, progressY + 18, leftW - 88, 8, current.getProgress(context.player()),
                        progressColor(current.getProgress(context.player())));
                String percent = Math.round(current.getProgress(context.player()) * 100.0F) + "%";
                line(context, graphics, percent, x + leftW - 60, progressY + 16, 44, color);

                int halfW = Math.max(120, (leftW - 44) / 2);
                TerminalUi.flatHudPanel(graphics, x + 16, cardY, halfW, cardH, TerminalUi.AMBER);
                line(context, graphics, "NEXT STEP", x + 26, cardY + 10, halfW - 20, TerminalUi.AMBER);
                wrap(context, graphics, currentSummary.nextStep(), x + 26, cardY + 27, halfW - 20, TerminalUi.TEXT);
                TerminalUi.flatHudPanel(graphics, x + 28 + halfW, cardY, leftW - halfW - 44, cardH,
                        descriptor().accentColor());
                line(context, graphics, "REQUIREMENT", x + 38 + halfW, cardY + 10, leftW - halfW - 64, descriptor().accentColor());
                drawCurrentRequirement(context, graphics, current, quest, x + 38 + halfW, cardY + 27,
                        leftW - halfW - 64);
            }

            TerminalUi.hdBackplatePanel(graphics, TerminalVisualAssets.CARD_ROUTE_STATUS_PANEL,
                    rightX, rightY, rightW, topH, descriptor().accentColor(), 0.60F, TerminalUi.ImageFit.COVER);
            line(context, graphics, "ROUTE STATUS", rightX + 16, rightY + 15, rightW - 32, descriptor().accentColor());
            TerminalUi.divider(graphics, rightX + 16, rightY + 31, rightW - 32, descriptor().accentColor());
            int actionH = 50;
            int actionY = rightY + topH - actionH - 14;
            int ry = rightY + 48;
            ry = TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.OVERVIEW,
                    "PHASE PROGRESS", (quest.getCurrentPhase() + 1) + " / " + MissionRegistry.getPhaseCount(),
                    descriptor().accentColor());
            ry = TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.MISSIONS,
                    "UNLOCKED / COMPLETED", quest.getUnlockedMissionIds().size() + " / " + quest.getCompletedMissionIds().size(),
                    TerminalUi.GREEN);
            ry = TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.WORLD,
                    "POIS / MARKERS / POWER", quest.getDiscoveredPOICount() + " / "
                            + quest.getVisitedSpecialLocations().size() + " / "
                            + quest.getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES,
                    TerminalUi.TEXT);
            ry += 10;
            NexusWorldData.WorldState worldState = HudState.getNexusState();
            ry = TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.NEXUS,
                    "NEXUS LOCK STATUS",
                    worldState == NexusWorldData.WorldState.NORMAL ? "LOCKED" : NexusAccessRules.stateLabel(worldState),
                    worldState == NexusWorldData.WorldState.NORMAL ? TerminalUi.RED : TerminalUi.GREEN);
            ry = TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.ORBITAL,
                    "ORBITAL LOCK STATUS", post.hasMadeChoice() ? "READY" : "LOCKED",
                    post.hasMadeChoice() ? TerminalUi.GREEN : TerminalUi.RED);
            if (ry + 24 < actionY - 6) {
                TerminalUi.statusLineRow(context, graphics, rightX + 20, ry, rightW - 40, TerminalIcon.ARCHIVES,
                        "INTEL RECORDS", intel.getAllIntel().size() + " RECORDS / " + intel.getUnreadCount() + " UNREAD",
                        intel.getUnreadCount() > 0 ? TerminalUi.AMBER : TerminalUi.GREEN);
            }
            TerminalUi.flatHudPanel(graphics, rightX + 16, actionY, rightW - 32, actionH, TerminalUi.CYAN);
            line(context, graphics, "NEXT ACTION", rightX + 26, actionY + 8, rightW - 52, TerminalUi.CYAN);
            wrap(context, graphics, nextAction(context.player(), quest, survival, current, post),
                    rightX + 26, actionY + 23, rightW - 52, TerminalUi.TEXT);

            int metricsY = wide ? y + topH + gap : rightY + topH + gap;
            int metricCount = wide ? 4 : 2;
            int metricW = Math.max(130, (w - gap * (metricCount - 1)) / metricCount);
            drawOverviewMetric(context, graphics, x, metricsY, metricW, metricsH, TerminalIcon.STATUS,
                    "SURVIVAL", survival.isSafeZone() ? "NOMINAL" : survival.getPrimaryHazard(),
                    "Hydration " + survival.getHydration() + "% / Radiation " + Math.round(survival.getRadiationLevel()) + "%",
                    survival.isSafeZone() ? TerminalUi.GREEN : TerminalUi.AMBER);
            drawOverviewMetric(context, graphics, x + (metricW + gap), metricsY, metricW, metricsH, TerminalIcon.DRONE,
                    "DRONE", quest.getDroneStage().name(),
                    "Integrity " + quest.getDroneHealth() + "% / Light " + (quest.isDroneLightEnabled() ? "online" : "locked"),
                    quest.getDroneHealth() >= 50 ? TerminalUi.GREEN : TerminalUi.AMBER);
            if (wide) {
                drawOverviewMetric(context, graphics, x + (metricW + gap) * 2, metricsY, metricW, metricsH, TerminalIcon.CODEX,
                        "RESEARCH", research.getPoints() + " RP",
                        "Tier " + research.getCurrentTier() + " / Perks " + research.getUnlockedPerks().size(),
                        research.getPoints() > 0 ? TerminalUi.GREEN : TerminalUi.MUTED);
                drawOverviewMetric(context, graphics, x + (metricW + gap) * 3, metricsY,
                        Math.max(130, w - (metricW + gap) * 3), metricsH, TerminalIcon.WORLD,
                        "OUTPOST", "GRID " + (post.hasMadeChoice() ? "SEALED" : "UNSTABLE"),
                        "Power nodes " + quest.getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES
                                + " / Guardians tracked",
                        post.hasMadeChoice() ? TerminalUi.GREEN : TerminalUi.RED);
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), overviewHeight(context));
        }

        private static int overviewCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, String label, String value, String detail, int color) {
            return TerminalUi.denseDataCard(context, graphics, x, y, width, label, value, detail, color);
        }

        private static void drawOverviewMetric(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, int height, TerminalIcon icon, String label, String value, String detail, int color) {
            TerminalUi.flatHudPanel(graphics, x, y, width, height, color);
            int iconSize = height < 82 ? 30 : 38;
            int iconY = y + Math.max(9, (height - iconSize) / 2);
            int textX = x + (height < 82 ? 52 : 62);
            int textW = Math.max(40, width - (textX - x) - 12);
            TerminalUi.hybridIconBadge(graphics, overviewMetricTexture(icon), icon, x + 12, iconY, iconSize, color, true);
            TerminalUi.line(context, graphics, label, textX, y + 10, textW, color);
            TerminalUi.line(context, graphics, value, textX, y + 26, textW, color);
            TerminalUi.wrap(context, graphics, detail, textX, y + 42, textW, TerminalUi.MUTED);
        }

        private static Identifier overviewMetricTexture(TerminalIcon icon) {
            return switch (icon) {
                case STATUS -> TerminalVisualAssets.ICON_GROUP_FIELD;
                case DRONE -> TerminalVisualAssets.ICON_GROUP_PROTOCOL;
                case CODEX -> TerminalVisualAssets.ICON_GROUP_SYSTEMS;
                case WORLD -> TerminalVisualAssets.ICON_STATE_NEEDED;
                default -> null;
            };
        }

        private static void drawCurrentRequirement(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                Mission mission, QuestData quest, int x, int y, int width) {
            List<Mission.ItemProgress> itemProgress = mission.getItemProgress(context.player());
            if (!itemProgress.isEmpty()) {
                Mission.ItemProgress progress = itemProgress.get(0);
                TerminalUi.line(context, graphics, progress.item().getHoverName().getString(),
                        x, y, width, progress.satisfied() ? TerminalUi.GREEN : TerminalUi.AMBER);
                TerminalUi.line(context, graphics, progress.have() + " / " + progress.need() + " held",
                        x, y + 13, width, TerminalUi.MUTED);
                return;
            }
            if (!mission.requiredBlocks().isEmpty()) {
                Mission.BlockRequirement requirement = mission.requiredBlocks().get(0);
                int have = quest.getBlockPlaceCount(requirement.blockId());
                TerminalUi.line(context, graphics, requirement.displayName(), x, y, width,
                        have >= requirement.count() ? TerminalUi.GREEN : TerminalUi.AMBER);
                TerminalUi.line(context, graphics, have + " / " + requirement.count() + " placed",
                        x, y + 13, width, TerminalUi.MUTED);
                return;
            }
            TerminalUi.line(context, graphics, "Server validation", x, y, width, TerminalUi.AMBER);
            TerminalUi.line(context, graphics, "Follow the next step.", x, y + 13, width, TerminalUi.MUTED);
        }

        private static int overviewHeight(TerminalRenderContext context) {
            int w = context.contentWidth();
            boolean wide = w >= 760;
            return Math.max(context.contentHeight(), wide ? 300 : 740);
        }

        private static String nextAction(Player player, QuestData quest, SurvivalData survival, Mission current, PostNexusData post) {
            if (!survival.isSafeZone() && !"NONE".equalsIgnoreCase(survival.getPrimaryHazard())) {
                return "Stabilize the current hazard before extending the route: " + survival.getPrimaryHazard() + ".";
            }
            if (current != null && !safeComplete(current, player)) {
                return "Open PROTOCOL ROADMAP: " + MissionUxSummary.of(player, quest, current).nextStep();
            }
            if (!quest.getAllPendingRewards().isEmpty()) {
                return "Claim pending mission rewards from Protocol Roadmap.";
            }
            if (!post.hasMadeChoice()) {
                return "Continue Ashfall protocols toward guardian resolution and the Nexus Core.";
            }
            return "Review Orbital Command and Route Map for the next post-Nexus route.";
        }
    }

    private static final class MissionBrowserTab extends AshfallTab {
        private final TerminalMissionBrowser browser;

        private MissionBrowserTab(TerminalMissionProvider provider) {
            this(provider, MISSIONS, "PROTOCOL ROADMAP", 100, 0xFF66D9FF,
                    TerminalTabChrome.of("Protocol Roadmap", TerminalTabChrome.GROUP_PROTOCOL, "PR",
                            "ECHO-7 route objectives", 100),
                    true);
        }

        private MissionBrowserTab(TerminalMissionProvider provider, Identifier tabId, String title, int order,
                int accentColor, TerminalTabChrome chrome, boolean showExpandControls) {
            super(tabId, title, order, accentColor, chrome);
            this.browser = new TerminalMissionBrowser(provider, tabId, showExpandControls,
                    TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
                    TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);
        }

        private static MissionBrowserTab sideOps(TerminalMissionProvider provider) {
            return new MissionBrowserTab(provider, SIDE_OPS, "SIGNAL LEADS", 110, 0xFFFFD166,
                    TerminalTabChrome.of("Signal Leads", TerminalTabChrome.GROUP_PROTOCOL, "SL",
                            "Optional recon signals", 110),
                    true);
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

    private static final class AshfallMissionProvider implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(
                    id("ashfall_protocol"),
                    "ECHO-7 PROTOCOL CHAIN",
                    "Required ECHO-7 field protocols for crash survival, route recovery, drone support, buried nodes, and the Nexus decision.",
                    10,
                    0xFF66D9FF,
                    true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            QuestData quest = QuestData.get(player);
            List<TerminalMissionDefinition> definitions = new ArrayList<>();
            for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
                List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
                for (int i = 0; i < missions.size(); i++) {
                    Mission mission = missions.get(i);
                    definitions.add(definition(player, quest, mission, phase, i + 1));
                }
            }
            return definitions;
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            String ashfallId = missionId.getPath();
            Mission mission = MissionRegistry.getMissionById(ashfallId);
            if (mission == null) {
                return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                        "LOCKED", "Ashfall protocol signal missing.",
                        "ECHO-7 has no clean field record for this identifier. Reopen the protocol chain from a known objective.", List.of());
            }
            QuestData quest = QuestData.get(player);
            QuestData.MissionStatus status = quest.getMissionStatus(mission.id());
            boolean preview = mission.isPathPreview(player);
            boolean pendingRewards = AshfallMissionActions.hasClaimableRewards(player, quest, mission);
            boolean completeNow = AshfallTab.safeComplete(mission, player);
            TerminalMissionStatus terminalStatus = preview
                    ? TerminalMissionStatus.VIEW_ONLY
                    : switch (status) {
                        case COMPLETED -> pendingRewards ? TerminalMissionStatus.CLAIMABLE : TerminalMissionStatus.COMPLETED;
                        case UNLOCKED -> TerminalMissionStatus.UNLOCKED;
                        case LOCKED -> TerminalMissionStatus.LOCKED;
                    };
            boolean current = isCurrentMission(quest, mission);
            boolean turnInReady = AshfallMissionActions.canTurnIn(player, quest, mission);
            MissionUxSummary summary = MissionUxSummary.of(player, quest, mission);
            String claimReason = AshfallMissionActions.claimReason(player, quest, mission);
            List<TerminalMissionAction> actions = List.of(
                    turnInReady
                            ? TerminalMissionAction.enabled(TURN_IN.getPath(), "TURN IN")
                            : TerminalMissionAction.disabled(TURN_IN.getPath(), "TURN IN",
                                    turnInReason(player, quest, mission, status, current, completeNow, preview)),
                    pendingRewards
                            ? TerminalMissionAction.enabled(CLAIM_REWARDS.getPath(), "CLAIM REWARDS")
                            : TerminalMissionAction.disabled(CLAIM_REWARDS.getPath(), "CLAIM REWARDS",
                                    claimReason.isBlank() ? "No sealed support cache is waiting for this protocol." : claimReason));
            String reason = terminalStatus == TerminalMissionStatus.LOCKED || terminalStatus == TerminalMissionStatus.VIEW_ONLY
                    ? unlockReason(player, quest, mission)
                    : "";
            return new TerminalMissionSnapshot(
                    id(mission.id()),
                    terminalStatus,
                    EndgameMissionProgress.forMission(player, quest, mission)
                            .map(EndgameMissionProgress.Snapshot::progress)
                            .orElseGet(() -> mission.getProgress(player)),
                    summary.statusLabel(),
                    reason,
                    summary.nextStep(),
                    actions);
        }

        @Override
        public TerminalMissionPresentation presentation(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot) {
            Mission mission = MissionRegistry.getMissionById(definition.id().getPath());
            if (mission == null) {
                return TerminalMissionPresentation.fallback(definition, snapshot);
            }
            MissionUxSummary summary = MissionUxSummary.of(player, QuestData.get(player), mission);
            return new TerminalMissionPresentation(
                    summary.shortTitle(),
                    summary.objectiveSummary(),
                    summary.nextStep(),
                    summary.routeHint(),
                    summary.statusTone(),
                    summary.tags(),
                    summary.relatedIntelKey());
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            Mission mission = MissionRegistry.getMissionById(definition.id().getPath());
            if (mission != null && mission.isPathPreview(player)) {
                return TerminalMissionRole.REFERENCE;
            }
            return mission != null && !AshfallMissionRoute.blocksPhase(mission)
                    ? TerminalMissionRole.OPTIONAL
                    : TerminalMissionRole.MAIN;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            if (definition == null) {
                return Optional.empty();
            }
            Mission mission = MissionRegistry.getMissionById(definition.id().getPath());
            TerminalMissionRole safeRole = mission != null && !AshfallMissionRoute.blocksPhase(mission)
                    ? TerminalMissionRole.OPTIONAL
                    : (role == null ? TerminalMissionRole.MAIN : role);
            return Optional.of(new TerminalMissionRoutePlacement(
                    mission == null
                            ? Math.max(0, Math.min(15, definition.phaseOrder()))
                            : AshfallMissionRoute.routePhase(mission.id(), definition.phaseOrder()),
                    mission == null
                            ? definition.missionOrder()
                            : AshfallMissionRoute.routeOrder(mission.id(), definition.missionOrder()),
                    safeRole,
                    true));
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            Mission mission = definition == null ? null : MissionRegistry.getMissionById(definition.id().getPath());
            if (mission == null || AshfallMissionRoute.blocksPhase(mission)) {
                return Optional.empty();
            }
            String anchor = AshfallMissionRoute.routeAnchor(mission.id());
            return anchor.isBlank() ? Optional.empty() : Optional.of(id(anchor));
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            if (TURN_IN.getPath().equals(actionId)) {
                turnInCurrentMission(player, missionId == null ? "" : missionId.getPath());
                return true;
            }
            if (CLAIM_REWARDS.getPath().equals(actionId)) {
                EchoGuideManager.claimRewards(player, missionId == null ? "" : missionId.getPath());
                QuestData.syncToClient(player);
                return true;
            }
            return false;
        }

        private static TerminalMissionDefinition definition(Player player, QuestData quest,
                Mission mission, int phase, int missionOrder) {
            List<TerminalMissionRequirement> requirements = requirements(player, quest, mission);
            List<TerminalMissionReward> rewards = mission.rewards().stream()
                    .map(TerminalMissionReward::of)
                    .toList();
            return new TerminalMissionDefinition(
                    id(mission.id()),
                    id("ashfall_protocol"),
                    "phase_" + (phase + 1),
                    "P" + (phase + 1) + " " + AshfallTab.phaseTitle(phase),
                    phase,
                    missionOrder,
                    mission.objectiveText(),
                    mission.echoMessage(),
                    mission.completionMessage(),
                    mission.category().getDisplayName(),
                    mission.difficulty().name(),
                    missionIcon(mission),
                    prerequisiteLabels(mission),
                    requirements,
                    rewards);
        }

        private static List<TerminalMissionRequirement> requirements(Player player, QuestData quest, Mission mission) {
            List<TerminalMissionRequirement> endgameRequirements = EndgameMissionProgress.forMission(player, quest, mission)
                    .map(snapshot -> snapshot.entries().stream()
                            .map(entry -> TerminalMissionRequirement.custom(
                                    entry.label(),
                                    entry.detail(),
                                    entry.icon(),
                                    entry.have(),
                                    entry.need(),
                                    entry.satisfied()))
                            .toList())
                    .orElse(List.of());
            if (!endgameRequirements.isEmpty()) {
                return endgameRequirements;
            }

            List<TerminalMissionRequirement> requirements = new ArrayList<>();
            for (Mission.ItemProgress progress : mission.getItemProgress(player)) {
                requirements.add(TerminalMissionRequirement.item(
                        progress.item(), progress.have(), progress.need(), progress.satisfied()));
            }
            for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
                int have = quest.getBlockPlaceCount(requirement.blockId());
                boolean satisfied = have >= requirement.count();
                requirements.add(TerminalMissionRequirement.block(
                        requirement.displayName(),
                        have + "/" + requirement.count() + " placed",
                        blockIcon(requirement.blockId(), requirement.displayName()),
                        have,
                        requirement.count(),
                        satisfied));
            }
            for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
                int have = quest.getEntityKills(requirement.entityType());
                boolean satisfied = have >= requirement.count();
                requirements.add(TerminalMissionRequirement.entity(
                        requirement.displayName(),
                        have + "/" + requirement.count() + " neutralized",
                        have,
                        requirement.count(),
                        satisfied));
            }
            for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
                boolean satisfied = quest.hasVisitedLocation(requirement.locationType(), requirement.locationId());
                requirements.add(TerminalMissionRequirement.location(
                        requirement.displayName(),
                        requirement.locationType().toUpperCase(Locale.ROOT) + ": " + requirement.locationId(),
                        satisfied));
            }
            for (Mission.EquipmentRequirement requirement : mission.requiredEquipment()) {
                ItemStack equipped = player.getItemBySlot(requirement.slot());
                boolean satisfied = !equipped.isEmpty() && equipped.getItem() == requirement.item().getItem();
                requirements.add(TerminalMissionRequirement.equipment(
                        requirement.displayName(),
                        requirement.slot().getName() + " equipped",
                        requirement.item(),
                        satisfied));
            }
            if (requirements.isEmpty()) {
                boolean complete = AshfallTab.safeComplete(mission, player);
                requirements.add(TerminalMissionRequirement.custom(
                        mission.objectiveText(),
                        complete ? "Objective complete" : "Progress tracked by server state",
                        missionIcon(mission),
                        complete ? 1 : 0,
                        1,
                        complete));
            }
            return requirements;
        }

        private static List<String> prerequisiteLabels(Mission mission) {
            List<String> labels = new ArrayList<>();
            for (String prerequisite : mission.getPrerequisites()) {
                Mission prereq = MissionRegistry.getMissionById(prerequisite);
                labels.add(prereq == null ? prerequisite : prereq.objectiveText());
            }
            if (mission.isPathRestricted()) {
                labels.add("Nexus path: " + mission.requiredPath().name());
            }
            return labels;
        }

        private static ItemStack missionIcon(Mission mission) {
            ItemStack objective = mission.getObjectiveItem();
            if (!objective.isEmpty()) {
                return objective.copy();
            }
            if (mission.objectiveIcon() != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(mission.objectiveIcon()).orElse(Items.AIR);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
            return switch (mission.category()) {
                case SURVIVAL -> new ItemStack(Items.CAMPFIRE);
                case CRAFTING -> new ItemStack(Items.CRAFTING_TABLE);
                case EXPLORATION -> new ItemStack(Items.COMPASS);
                case COMBAT -> new ItemStack(Items.IRON_SWORD);
                case TECH -> new ItemStack(Items.REDSTONE);
                case STORY -> new ItemStack(Items.WRITABLE_BOOK);
            };
        }

        private static ItemStack blockIcon(String blockId, String displayName) {
            Identifier registryId = blockId.contains(":")
                    ? Identifier.tryParse(blockId)
                    : Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, blockId);
            if (registryId != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(registryId).orElse(Items.AIR);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
            String fallback = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
            if (fallback.contains("campfire")) return new ItemStack(Items.CAMPFIRE);
            if (fallback.contains("collector")) return new ItemStack(Items.CAULDRON);
            if (fallback.contains("generator")) return new ItemStack(Items.FURNACE);
            if (fallback.contains("lab")) return new ItemStack(Items.LECTERN);
            if (fallback.contains("node")) return new ItemStack(Items.REDSTONE_BLOCK);
            return new ItemStack(Items.STONE);
        }

        private static boolean isCurrentMission(QuestData quest, Mission mission) {
            Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
            return current != null && current.id().equals(mission.id());
        }

        private static String unlockReason(Player player, QuestData quest, Mission mission) {
            return MissionUxSummary.unlockReason(player, quest, mission);
        }

        private static String turnInReason(Player player, QuestData quest, Mission mission, QuestData.MissionStatus status,
                boolean current, boolean completeNow, boolean preview) {
            return MissionUxSummary.turnInReason(player, quest, mission, status, current, completeNow, preview);
        }
    }

    private static final class AshfallSideOpsProvider implements TerminalMissionProvider {
        private static final List<SideOp> OPS = List.of(
                new SideOp(
                        "crash_blackbox_signal",
                        "Crash Blackbox Signal",
                        "PERIMETER SIGNALS",
                        0,
                        1,
                        0,
                        1,
                        "secure_crash_outpost",
                        "",
                        "Recover the first telemetry thread from the drop-pod perimeter.",
                        "ECHO-7 fell with the pod, but the pod did not fall cleanly. The first shelter loop gives the terminal enough signal clarity to reconstruct what survived impact and what was already missing.",
                        "Secure the crash outpost so ECHO-7 can stitch together the pod's damaged landing record.",
                        "Crash outpost anchored. Blackbox telemetry confirms the descent vector originated above Earth, not from any ground launch.",
                        "secure_crash_outpost",
                        SideOpCheck.MISSION_COMPLETE,
                        stack(Items.RECOVERY_COMPASS),
                        "Outpost anchor",
                        "Secure Crash Outpost"),
                new SideOp(
                        "wasteland_surface_report",
                        "Wasteland Surface Report",
                        "PERIMETER SIGNALS",
                        0,
                        2,
                        1,
                        2,
                        "secure_crash_outpost",
                        "",
                        "Classify the ash-dirt surface and low vegetation around the starter basin.",
                        "The Wasteland is not a grassland. Its living layer appears as sparse tufts, reeds, and mutated shrubs over dead soil: dirt first, forage second, shelter always.",
                        "Travel the Wasteland or secure the first outpost to confirm the local terrain language.",
                        "Surface report archived. ECHO marks wasteland grass as vegetation, not a stable topsoil layer.",
                        "secure_crash_outpost",
                        SideOpCheck.WASTELAND_SURVEY,
                        modBlockStack(ModBlocks.WASTELAND_DIRT::get),
                        "Terrain language",
                        "Confirm Wasteland surface"),
                new SideOp(
                        "first_ruin_signature",
                        "First Ruin Signature",
                        "Route Records",
                        1,
                        1,
                        7,
                        10,
                        "craft_portable_scanner",
                        "craft_portable_scanner",
                        "Use scanner-led exploration to prove the first ruin route exists.",
                        "A ruin without a route is only wreckage. Once ECHO records a POI, it can attach hazard profile, prep kit, likely supplies, and the reason the place still matters.",
                        "Craft the Portable Signal Scanner and log one point of interest.",
                        "First ruin signature archived. The wasteland route map now has a real recovery thread.",
                        "craft_portable_scanner",
                        SideOpCheck.FIRST_POI,
                        stack(Items.SPYGLASS),
                        "Route signal",
                        "Discover 1 POI"),
                new SideOp(
                        "poi_field_atlas",
                        "POI Field Atlas",
                        "Route Records",
                        1,
                        2,
                        7,
                        20,
                        "scan_first_poi",
                        "scan_first_poi",
                        "Open the Route Map atlas and start turning isolated scanner hits into a readable field catalog.",
                        "ECHO can identify a route profile from the scanner, then list every concrete template signal that belongs to that route: camps, wrecks, labs, vaults, hubs, and landmarks without turning each ruin into a separate objective.",
                        "Use the Route Map POI Atlas after your first scan, then catalogue at least three POI profiles.",
                        "POI atlas synchronized. ECHO now treats template variants as field recognition, while profile discovery remains the actual route state.",
                        "scan_first_poi",
                        SideOpCheck.POI_ATLAS,
                        stack(Items.MAP),
                        "POI Atlas",
                        "Discover 3 POI profiles"),
                new SideOp(
                        "faction_crossband",
                        "Faction Crossband",
                        "Human Signals",
                        2,
                        1,
                        8,
                        10,
                        "first_faction_contact",
                        "first_faction_contact",
                        "Identify the first living social signal in the wasteland.",
                        "Ashfall factions read each route differently. Contacting any faction proves the ruins are not empty; they are organized, territorial, and listening.",
                        "Reach any faction job site and open first contact.",
                        "Crossband stable. ECHO can now separate military order, salvage economy, and biological adaptation threads.",
                        "first_faction_contact",
                        SideOpCheck.FACTION_CONTACT,
                        stack(Items.EMERALD),
                        "Faction contact",
                        "Contact any faction"),
                new SideOp(
                        "radwarden_containment_thread",
                        "Radwarden Containment Thread",
                        "Human Signals",
                        2,
                        2,
                        8,
                        20,
                        "first_faction_contact",
                        "first_faction_contact",
                        "Open the Radwarden dossier as optional context, not required route pressure.",
                        "Radwarden signals frame the wasteland as containment work: warnings first, decon second, courage only after the meter agrees.",
                        "Contact a Radwarden source or complete one Radwarden job when the route naturally offers it.",
                        "Radwarden thread archived. ECHO marks containment discipline as a survival language, not a mandatory errand chain.",
                        "first_faction_contact",
                        SideOpCheck.RADWARDEN_THREAD,
                        stack(Items.SHIELD),
                        "Radwarden dossier",
                        "Contact or contract"),
                new SideOp(
                        "crashbreak_salvage_thread",
                        "Crashbreak Salvage Thread",
                        "Human Signals",
                        2,
                        3,
                        8,
                        30,
                        "first_faction_contact",
                        "first_faction_contact",
                        "Open the Crashbreak dossier as optional context, not required route pressure.",
                        "Crashbreak signals frame the wasteland as an economy of wrecks: every route has a price, every lost machine has a second use.",
                        "Contact a Crashbreak source or complete one Crashbreak job when the route naturally offers it.",
                        "Crashbreak thread archived. ECHO marks salvage value as field intelligence, not a mandatory contract ladder.",
                        "first_faction_contact",
                        SideOpCheck.CRASHBREAK_THREAD,
                        stack(Items.IRON_PICKAXE),
                        "Crashbreak dossier",
                        "Contact or contract"),
                new SideOp(
                        "sporebound_adaptation_thread",
                        "Sporebound Adaptation Thread",
                        "Human Signals",
                        2,
                        4,
                        8,
                        40,
                        "first_faction_contact",
                        "first_faction_contact",
                        "Open the Sporebound dossier as optional context, not required route pressure.",
                        "Sporebound signals frame mutation as medicine with teeth: useful when measured, lethal when worshipped.",
                        "Contact a Sporebound source or complete one Sporebound job when the route naturally offers it.",
                        "Sporebound thread archived. ECHO marks adaptation work as biological context, not a required side chain.",
                        "first_faction_contact",
                        SideOpCheck.SPOREBOUND_THREAD,
                        stack(Items.RED_MUSHROOM),
                        "Sporebound dossier",
                        "Contact or contract"),
                new SideOp(
                        "drone_memory_sweep",
                        "Drone Memory Sweep",
                        "Machine Echoes",
                        3,
                        1,
                        8,
                        50,
                        "repair_echo_drone",
                        "repair_echo_drone",
                        "Recover a drone-linked field memory without changing drone progression.",
                        "The drone is not just equipment. It is ECHO-7's mobile witness: a broken extension of the emergency system that survived the fall and kept looking.",
                        "Repair drone support and recover drone intel.",
                        "Drone memory archived. Companion and Scout links now read as field witnesses, not just utility commands.",
                        "repair_echo_drone",
                        SideOpCheck.DRONE_INTEL,
                        stack(Items.OBSERVER),
                        "Drone witness",
                        "Recover drone intel"),
                new SideOp(
                        "guardian_signal_lattice",
                        "Guardian Signal Lattice",
                        "Anomaly Chain",
                        4,
                        1,
                        12,
                        10,
                        "deploy_stationary_scanner",
                        "deploy_stationary_scanner",
                        "Resolve the first buried guardian signal and classify the node lattice.",
                        "Every guardian is a local scar claimed by one of the three faction threads: Radwarden containment, Crashbreak salvage, or Sporebound anomaly reading. The first defeat proves the chain is physical, not just signal noise.",
                        "Neutralize any biome guardian through the main Ashfall route.",
                        "Guardian lattice archived. The Nexus Core is defended by places, not just weapons.",
                        "deploy_stationary_scanner",
                        SideOpCheck.ANY_GUARDIAN,
                        stack(Items.BEACON),
                        "Guardian node",
                        "Neutralize any guardian"),
                new SideOp(
                        "nexus_choice_record",
                        "Nexus Choice Record",
                        "Nexus",
                        5,
                        1,
                        14,
                        10,
                        "reach_decision",
                        "reach_decision",
                        "Archive the final path commitment once RESTORE, DESTROY, or CONTROL is chosen.",
                        "The Nexus choice is not a menu branch. It is a moral operating mode for the broken grid: repair, sever, or command, each with a different kind of blood on it.",
                        "Reach the Nexus decision and commit one path.",
                        "Nexus choice archived. Addon chapters can now react through core services without owning Ashfall progression.",
                        "reach_decision",
                        SideOpCheck.NEXUS_CHOICE,
                        modBlockStack(ModBlocks.NEXUS_CORE::get),
                        "Path commitment",
                        "Choose a Nexus path"),
                new SideOp(
                        "orbital_quarantine_echo",
                        "Orbital Quarantine Echo",
                        "ECHO-0",
                        6,
                        1,
                        15,
                        10,
                        "reach_decision",
                        "reach_decision",
                        "Preview the post-Nexus orbital thread that explains why the fall began above Earth.",
                        "Orbital Remnants turns the question upward: ECHO-7 is a rescue fragment, ECHO-0 is the quarantine authority, and the sky is still enforcing a decision made during Gridfall.",
                        "Make any Nexus choice to unlock orbital calibration when the optional addon is installed.",
                        "Orbital quarantine thread ready. If Orbital Remnants is installed, Earth calibration can begin from its own terminal path.",
                        "reach_decision",
                        SideOpCheck.NEXUS_CHOICE,
                        stack(Items.END_CRYSTAL),
                        "Orbital preview",
                        "Make a Nexus choice"));

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(
                    id("ashfall_side_ops"),
                    "ECHO-7 SIGNAL LEADS",
                    "Optional lore, recon, and world-context objectives: useful route context first, deeper Gridfall echoes underneath.",
                    20,
                    0xFFFFD166,
                    true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return OPS.stream().map(op -> definition(player, op)).toList();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            SideOp op = sideOp(missionId);
            if (op == null) {
                return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                        "LOCKED", "Signal lead not found in the current archive index.", "No optional record is available for this signal.", List.of());
            }
            QuestData quest = QuestData.get(player);
            boolean unlocked = isUnlocked(quest, op);
            boolean complete = unlocked && isComplete(player, quest, op);
            boolean archived = complete && intelArchived(player, op);
            TerminalMissionStatus status = archived
                    ? TerminalMissionStatus.COMPLETED
                    : complete ? TerminalMissionStatus.CLAIMABLE
                    : unlocked ? TerminalMissionStatus.UNLOCKED : TerminalMissionStatus.VIEW_ONLY;
            String label = archived ? "ARCHIVED" : complete ? "READY" : unlocked ? "OPTIONAL" : "VIEW";
            String unlockReason = unlocked ? "" : unlockReason(quest, op);
            String hint = archived ? op.completeHint()
                    : complete ? "Intel recovered. Archive the side record to unlock contextual discoveries."
                    : unlocked ? op.activeHint() : unlockReason;
            return new TerminalMissionSnapshot(
                    id(op.path()),
                    status,
                    complete ? 1.0F : 0.0F,
                    label,
                    unlockReason,
                    hint,
                    complete && !archived
                            ? List.of(TerminalMissionAction.enabled("archive_intel", "Archive Intel"))
                            : List.of());
        }

        @Override
        public TerminalMissionPresentation presentation(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot) {
            SideOp op = sideOp(definition.id());
            if (op == null) {
                return TerminalMissionPresentation.fallback(definition, snapshot);
            }
            return new TerminalMissionPresentation(
                    op.title(),
                    op.briefing(),
                    snapshot.actionHint(),
                    op.phaseTitle() + " / field recon",
                    snapshot.status() == TerminalMissionStatus.COMPLETED ? "success"
                            : snapshot.status() == TerminalMissionStatus.UNLOCKED ? "active" : "muted",
                    List.of("Optional", op.phaseTitle(), op.requirementLabel()),
                    "poi_field_atlas".equals(op.path()) ? "ashfall_poi_atlas" : "ashfall_faction_threads");
        }

        @Override
        public TerminalMissionVisuals visuals(Player player, TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot) {
            return new TerminalMissionVisuals(
                    TerminalVisualAssets.missionHeroArt(definition == null ? null : definition.id(), "Field Recon"),
                    "side_ops",
                    "lore",
                    snapshot.status() == TerminalMissionStatus.COMPLETED ? "success"
                            : snapshot.status() == TerminalMissionStatus.UNLOCKED ? "active" : "locked");
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return TerminalMissionRole.OPTIONAL;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            SideOp op = definition == null ? null : sideOp(definition.id());
            if (op == null) {
                int order = definition == null ? 0 : definition.missionOrder();
                return Optional.of(TerminalMissionRoutePlacement.optional(15, order));
            }
            return Optional.of(TerminalMissionRoutePlacement.optional(op.routePhaseOrder(), op.routeOrder()));
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            SideOp op = definition == null ? null : sideOp(definition.id());
            if (op == null || op.routeGateMissionId().isBlank()) {
                return List.of();
            }
            return List.of(id(op.routeGateMissionId()));
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            SideOp op = definition == null ? null : sideOp(definition.id());
            if (op == null || op.routeAnchorMissionId().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(id(op.routeAnchorMissionId()));
        }

        @Override
        public List<TerminalMissionIntelUnlock> intelUnlocks(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            SideOp op = definition == null ? null : sideOp(definition.id());
            return op == null ? List.of() : sideOpIntel(op);
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            if (!"archive_intel".equals(actionId)) {
                return false;
            }
            SideOp op = sideOp(missionId);
            if (op == null) {
                return false;
            }
            QuestData quest = QuestData.get(player);
            if (!isUnlocked(quest, op) || !isComplete(player, quest, op)) {
                return false;
            }
            return applyIntelUnlocks(player, op);
        }

        private TerminalMissionDefinition definition(Player player, SideOp op) {
            QuestData quest = QuestData.get(player);
            boolean complete = isUnlocked(quest, op) && isComplete(player, quest, op);
            return new TerminalMissionDefinition(
                    id(op.path()),
                    id("ashfall_side_ops"),
                    op.phaseTitle().toLowerCase(Locale.ROOT).replace(' ', '_'),
                    op.phaseTitle(),
                    op.phaseOrder(),
                    op.missionOrder(),
                    op.title(),
                    op.briefing(),
                    op.fieldGuide(),
                    "Field Recon",
                    "Recon",
                    op.iconStack(),
                    List.of(op.routeGateMissionId().isBlank()
                            ? "No prerequisite"
                            : objectiveName(op.routeGateMissionId())),
                    List.of(TerminalMissionRequirement.custom(
                            op.requirementLabel(),
                            op.requirementDetail(),
                            op.iconStack(),
                            complete ? 1 : 0,
                            1,
                            complete)),
                    List.of(TerminalMissionReward.text("Archive Context",
                            "Adds tactical field context only; required route progress and caches stay unchanged.")));
        }

        private static SideOp sideOp(Identifier id) {
            if (id == null) {
                return null;
            }
            return OPS.stream()
                    .filter(op -> op.path().equals(id.getPath()))
                    .findFirst()
                    .orElse(null);
        }

        private static boolean isUnlocked(QuestData quest, SideOp op) {
            return op.routeGateMissionId().isBlank() || quest.isMissionCompleted(op.routeGateMissionId());
        }

        private static boolean isComplete(Player player, QuestData quest, SideOp op) {
            return switch (op.check()) {
                case MISSION_COMPLETE -> quest.isMissionCompleted(op.unlockMissionId());
                case WASTELAND_SURVEY -> quest.isMissionCompleted("secure_crash_outpost")
                        || quest.hasVisitedLocation("biome", "the_wasteland")
                        || quest.hasVisitedLocation("biome", "echoashfallprotocol:the_wasteland");
                case FIRST_POI -> quest.getDiscoveredPOICount() >= 1;
                case POI_ATLAS -> quest.getDiscoveredPOICount() >= 3;
                case FACTION_CONTACT -> quest.hasVisitedLocation("special", "faction_contact:any")
                        || quest.isMissionCompleted("first_faction_contact");
                case RADWARDEN_THREAD -> quest.hasVisitedLocation("special", "faction_contact:radwarden_compact")
                        || quest.isMissionCompleted("contact_radwarden_compact")
                        || quest.isMissionCompleted("complete_radwarden_contract");
                case CRASHBREAK_THREAD -> quest.hasVisitedLocation("special", "faction_contact:crashbreak_salvage")
                        || quest.isMissionCompleted("contact_crashbreak_salvage")
                        || quest.isMissionCompleted("complete_crashbreak_contract");
                case SPOREBOUND_THREAD -> quest.hasVisitedLocation("special", "faction_contact:sporebound_sanctum")
                        || quest.isMissionCompleted("contact_sporebound_sanctum")
                        || quest.isMissionCompleted("complete_sporebound_contract");
                case DRONE_INTEL -> quest.hasVisitedLocation("special", "drone:intel_recovered")
                        || quest.isMissionCompleted("recover_drone_intel");
                case ANY_GUARDIAN -> quest.getCompletedMissionIds().stream()
                        .anyMatch(id -> id.startsWith("neutralize_"));
                case NEXUS_CHOICE -> PostNexusData.get(player).hasMadeChoice();
            };
        }

        private static String unlockReason(QuestData quest, SideOp op) {
            if (op.routeGateMissionId().isBlank() || quest.isMissionCompleted(op.routeGateMissionId())) {
                return "";
            }
            return "Complete " + objectiveName(op.routeGateMissionId()) + " first.";
        }

        private static String objectiveName(String missionId) {
            Mission mission = MissionRegistry.getMissionById(missionId);
            return mission == null ? missionId : mission.objectiveText();
        }

        private static List<TerminalMissionIntelUnlock> sideOpIntel(SideOp op) {
            return switch (op.path()) {
                case "crash_blackbox_signal" -> List.of(
                        archive("ashfall_progression_manual", "Protocol Roadmap Rules",
                                "Crash telemetry explains why the route begins with a protected outpost."),
                        route("ashfall_active_protocol", "Ashfall Active Protocol",
                                "Adds the first route record signal beside the survival spine."));
                case "wasteland_surface_report" -> List.of(
                        archive("ashfall_survival_manual", "Ashfall Survival Manual",
                                "Clarifies wasteland surface rules, field shelter, and route pressure."),
                        discovery(AshfallDiscoveryProvider.biomeId("the_wasteland"), "Wasteland Biome",
                                "Reveals the starter basin as a hostile field region."));
                case "first_ruin_signature" -> List.of(
                        archive("ashfall_poi_atlas", "POI Field Atlas",
                                "Turns the first scanner hit into route-map context."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_camp"), "Survivor Camp",
                                "Adds a concrete recovery-site lead to the discovery grid."));
                case "poi_field_atlas" -> List.of(
                        archive("ashfall_poi_atlas", "POI Field Atlas",
                                "Explains route profiles, template variants, and scanner cataloging."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_cache"), "Survivor Cache",
                                "Adds a practical cache lead for route planning."));
                case "faction_crossband" -> List.of(
                        archive("ashfall_faction_threads", "Faction Threads",
                                "Frames Ashfall factions as optional context instead of mandatory standing."),
                        faction("radwarden_compact", "Radwarden Compact",
                                "Reveals the containment faction signal without changing reputation."),
                        faction("crashbreak_salvage", "Crashbreak Salvage",
                                "Reveals the salvage faction signal without changing reputation."),
                        faction("sporebound_sanctum", "Sporebound Sanctum",
                                "Reveals the adaptation faction signal without changing reputation."));
                case "radwarden_containment_thread" -> List.of(
                        archive("ashfall_faction_threads", "Radwarden Dossier",
                                "Adds containment doctrine to the field archive."),
                        faction("radwarden_compact", "Radwarden Compact",
                                "Reveals containment intel without touching contracts or standing."));
                case "crashbreak_salvage_thread" -> List.of(
                        archive("ashfall_faction_threads", "Crashbreak Dossier",
                                "Adds salvage doctrine to the field archive."),
                        faction("crashbreak_salvage", "Crashbreak Salvage",
                                "Reveals salvage intel without touching contracts or standing."));
                case "sporebound_adaptation_thread" -> List.of(
                        archive("ashfall_faction_threads", "Sporebound Dossier",
                                "Adds adaptation doctrine to the field archive."),
                        faction("sporebound_sanctum", "Sporebound Sanctum",
                                "Reveals biological intel without touching contracts or standing."));
                case "drone_memory_sweep" -> List.of(
                        archive("ashfall_drone_manual", "Drone Command Primer",
                                "Recasts drone support as recovered witness telemetry."));
                case "guardian_signal_lattice" -> List.of(
                        archive("ashfall_threat_manual", "Biome Guardian Dossier",
                                "Connects guardian nodes to the Nexus route lattice."),
                        discovery(AshfallDiscoveryProvider.guardianId("plains_warlord"), "Guardian Lattice",
                                "Adds the first buried guardian signal to contextual discoveries."));
                case "nexus_choice_record" -> List.of(
                        archive("ashfall_nexus_manual", "Nexus Path Protocols",
                                "Archives the selected RESTORE, DESTROY, or CONTROL path as route context."));
                case "orbital_quarantine_echo" -> List.of(
                        archive("ashfall_nexus_manual", "Orbital Handshake",
                                "Previews ECHO-0 quarantine context for optional post-Nexus routing."));
                default -> List.of();
            };
        }

        private static TerminalMissionIntelUnlock archive(String path, String title, String summary) {
            return TerminalMissionIntelUnlock.archive(id(path), title, summary);
        }

        private static TerminalMissionIntelUnlock route(String path, String title, String summary) {
            return TerminalMissionIntelUnlock.route(id(path), title, summary);
        }

        private static TerminalMissionIntelUnlock discovery(Identifier target, String title, String summary) {
            return TerminalMissionIntelUnlock.discovery(target, title, summary);
        }

        private static TerminalMissionIntelUnlock faction(String factionPath, String title, String summary) {
            return TerminalMissionIntelUnlock.faction(id("faction/" + factionPath), title, summary);
        }

        private static TerminalMissionIntelUnlock poi(Identifier target, String title, String summary) {
            return TerminalMissionIntelUnlock.poi(target, title, summary);
        }

        private static boolean intelArchived(Player player, SideOp op) {
            if (player == null || op == null) {
                return false;
            }
            for (TerminalMissionIntelUnlock unlock : sideOpIntel(op)) {
                if (!intelUnlocked(player, unlock)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean intelUnlocked(Player player, TerminalMissionIntelUnlock unlock) {
            if (unlock.kind() == TerminalMissionIntelKind.ARCHIVE) {
                return EchoCoreServices.isArchiveUnlocked(player, unlock.id().toString());
            }
            Identifier target = unlock.kind() == TerminalMissionIntelKind.ROUTE
                    ? EchoCoreServices.routeDiscoveryId(unlock.id())
                    : unlock.id();
            return EchoCoreServices.hasDiscoveredFeature(player, target);
        }

        private static boolean applyIntelUnlocks(ServerPlayer player, SideOp op) {
            boolean changed = false;
            for (TerminalMissionIntelUnlock unlock : sideOpIntel(op)) {
                if (unlock.kind() == TerminalMissionIntelKind.ARCHIVE) {
                    if (!EchoCoreServices.isArchiveUnlocked(player, unlock.id().toString())) {
                        EchoCoreServices.unlockArchive(player, unlock.id().toString());
                        changed = true;
                    }
                    continue;
                }
                Identifier target = unlock.kind() == TerminalMissionIntelKind.ROUTE
                        ? EchoCoreServices.routeDiscoveryId(unlock.id())
                        : unlock.id();
                changed |= EchoCoreServices.discoverFeature(player, target);
            }
            return changed;
        }

        private record SideOp(
                String path,
                String title,
                String phaseTitle,
                int phaseOrder,
                int missionOrder,
                int routePhaseOrder,
                int routeOrder,
                String routeAnchorMissionId,
                String routeGateMissionId,
                String briefing,
                String fieldGuide,
                String activeHint,
                String completeHint,
                String unlockMissionId,
                SideOpCheck check,
                Supplier<ItemStack> icon,
                String requirementLabel,
                String requirementDetail) {
            private ItemStack iconStack() {
                return icon.get().copy();
            }
        }

        private static Supplier<ItemStack> stack(Item item) {
            return () -> new ItemStack(item);
        }

        private static Supplier<ItemStack> modBlockStack(Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> new ItemStack(block.get().asItem());
        }

        private enum SideOpCheck {
            MISSION_COMPLETE,
            WASTELAND_SURVEY,
            FIRST_POI,
            POI_ATLAS,
            FACTION_CONTACT,
            RADWARDEN_THREAD,
            CRASHBREAK_THREAD,
            SPOREBOUND_THREAD,
            DRONE_INTEL,
            ANY_GUARDIAN,
            NEXUS_CHOICE
        }
    }

    private static final class ArchivesTab extends AshfallTab {
        private static final List<String> GROUPS = List.of(
                "Outpost Survival", "Machine Systems", "Protocol Flow", "Threat Dossier", "Recovered Lore",
                "Signal Logs", "ECHO-0");
        private String selectedGroup = "Outpost Survival";
        private String selectedEntryId = "";
        private int recordScroll;
        private int detailScroll;
        private int lastRecordX;
        private int lastRecordY;
        private int lastRecordW;
        private int lastRecordH;
        private int lastRecordContentH;
        private int lastArchiveDetailX;
        private int lastArchiveDetailY;
        private int lastArchiveDetailW;
        private int lastArchiveDetailH;
        private int lastArchiveDetailContentH;

        private ArchivesTab() {
            super(ARCHIVES, "FIELD ARCHIVE", 140, 0xFF9FD1FF,
                    TerminalTabChrome.of("Field Archive", TerminalTabChrome.GROUP_FIELD, "FA",
                            "Recovered field records", 140));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            List<ArchiveEntry> entries = buildEntries(context.player());
            normalizeSelection(entries);

            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            boolean wide = w >= 620;
            int groupW = wide ? 116 : w;
            int listW = wide ? 190 : w;
            int listX = wide ? x + groupW + 10 : x;
            int detailX = wide ? listX + listW + 10 : x;
            int detailW = wide ? w - groupW - listW - 26 : w;
            int groupPanelH = 34 + GROUPS.size() * 20;
            int listY = wide ? y : y + groupPanelH + 12;
            List<ArchiveEntry> groupEntries = entriesForGroup(entries, selectedGroup);
            int detailY = wide ? y : listY + listHeight(groupEntries) + 18;
            int viewportH = context.contentHeight();

            int gy = flatDataPanel(context, graphics, x, y, groupW, groupPanelH, "FIELD ARCHIVE", "");
            for (String group : GROUPS) {
                GroupCount count = countGroup(entries, group);
                boolean selected = group.equals(selectedGroup);
                int color = selected ? descriptor().accentColor() : (count.total() > 0 ? TerminalUi.TEXT : TerminalUi.MUTED);
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 10, gy, groupW - 20, 18);
                TerminalUi.selectableRow(graphics, x + 10, gy, groupW - 20, 18, selected, hover, descriptor().accentColor());
                line(context, graphics, group, x + 16, gy + 5, groupW - 58, color);
                line(context, graphics, count.open() + "/" + count.total(), x + groupW - 50, gy + 5, 36, TerminalUi.MUTED);
                addHitbox(x + 10, gy, groupW - 20, 18, () -> {
                    selectedGroup = group;
                    selectedEntryId = "";
                    recordScroll = 0;
                    detailScroll = 0;
                });
                gy += 20;
            }

            int recordsPanelH = wide ? viewportH : Math.max(80, 34 + listHeight(groupEntries));
            flatDataPanel(context, graphics, listX, listY, listW, recordsPanelH, "RECORDS", "");
            lastRecordX = listX;
            lastRecordY = listY + 34;
            lastRecordW = listW - 8;
            lastRecordH = Math.max(60, recordsPanelH - 44);
            lastRecordContentH = groupEntries.size() * 30;
            recordScroll = TerminalUi.clampScroll(recordScroll, lastRecordContentH, lastRecordH);
            if (wide) {
                graphics.enableScissor(lastRecordX, lastRecordY, lastRecordX + lastRecordW, lastRecordY + lastRecordH);
            }
            int ey = lastRecordY - (wide ? recordScroll : 0);
            for (ArchiveEntry entry : groupEntries) {
                boolean selected = entry.id().equals(selectedEntryId);
                int color = entry.locked() ? TerminalUi.MUTED : statusArchiveColor(entry.status());
                boolean hover = TerminalUi.inside(mouseX, mouseY, listX + 10, ey - 2, listW - 24, 28);
                dataRow(context, graphics, listX + 10, ey - 2, listW - 24, 28,
                        entry.title(), entry.category(), entry.status(), selected, hover, color);
                if (visible(ey - 2, 26, lastRecordY, lastRecordH)) {
                    addHitbox(listX + 10, ey - 2, listW - 24, 28, () -> {
                        selectArchiveEntry(context.player(), entry);
                        detailScroll = 0;
                    });
                }
                ey += 30;
            }
            if (wide) {
                graphics.disableScissor();
                TerminalUi.scrollbar(graphics, listX + listW - 6, lastRecordY, lastRecordH,
                        recordScroll, lastRecordContentH - lastRecordH, descriptor().accentColor());
            }

            ArchiveEntry selected = selectedEntry(entries);
            lastArchiveDetailX = detailX;
            lastArchiveDetailY = detailY;
            lastArchiveDetailW = detailW;
            lastArchiveDetailH = wide ? viewportH : Math.max(120, viewportH - (detailY - y));
            lastArchiveDetailContentH = archiveDetailHeight(context, selected, detailW);
            detailScroll = TerminalUi.clampScroll(detailScroll, lastArchiveDetailContentH, lastArchiveDetailH);
            if (wide) {
                graphics.enableScissor(detailX, detailY, detailX + detailW, detailY + lastArchiveDetailH);
            }
            drawArchiveDetail(context, graphics, selected, detailX, detailY - (wide ? detailScroll : 0), detailW);
            if (wide) {
                graphics.disableScissor();
                TerminalUi.scrollbar(graphics, detailX + detailW - 5, detailY, lastArchiveDetailH,
                        detailScroll, lastArchiveDetailContentH - lastArchiveDetailH, descriptor().accentColor());
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            if (context.contentWidth() < 620) {
                List<ArchiveEntry> entries = buildEntries(context.player());
                List<ArchiveEntry> groupEntries = entriesForGroup(entries, selectedGroup);
                ArchiveEntry selected = selectedEntry(entries);
                return Math.max(context.contentHeight(), GROUPS.size() * 18 + listHeight(groupEntries)
                        + archiveDetailHeight(context, selected, context.contentWidth()) + 70);
            }
            return context.contentHeight();
        }

        @Override
        public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
            int amount = (int) Math.round(delta * 18.0D);
            if (TerminalUi.inside(mouseX, mouseY, lastRecordX, lastRecordY, lastRecordW, lastRecordH)) {
                recordScroll = TerminalUi.clampScroll(recordScroll - amount, lastRecordContentH, lastRecordH);
                return true;
            }
            if (TerminalUi.inside(mouseX, mouseY, lastArchiveDetailX, lastArchiveDetailY,
                    lastArchiveDetailW, lastArchiveDetailH)) {
                detailScroll = TerminalUi.clampScroll(detailScroll - amount, lastArchiveDetailContentH, lastArchiveDetailH);
                return true;
            }
            return false;
        }

        private void drawArchiveDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                ArchiveEntry entry, int x, int y, int width) {
            int panelH = Math.max(130, archiveDetailHeight(context, entry, width));
            int cy = flatDataPanel(context, graphics, x, y, width, panelH, "DETAIL", "");
            if (entry == null) {
                wrap(context, graphics, "No archive record is available for this group.", x + 12, cy, width - 24, TerminalUi.MUTED);
                return;
            }
            TerminalUi.hybridIconBadge(graphics, TerminalVisualAssets.ICON_PAGE_FIELD_ARCHIVE, TerminalIcon.ARCHIVES,
                    x + 12, cy, 40, descriptor().accentColor(), !entry.locked());
            line(context, graphics, entry.title(), x + 62, cy + 4, width - 150, TerminalUi.TEXT);
            chip(context, graphics, entry.status(), x + Math.max(0, width - 84), cy + 2, 74,
                    entry.locked() ? TerminalUi.MUTED : statusArchiveColor(entry.status()));
            cy += 52;
            line(context, graphics, "CATEGORY: " + entry.category() + " / GROUP: " + entry.group(),
                    x + 12, cy, width - 24, TerminalUi.AMBER);
            cy += 16;
            if (entry.locked()) {
                cy = wrap(context, graphics, entry.hint(), x + 12, cy, width - 24, TerminalUi.MUTED) + 5;
                line(context, graphics, "Content remains gated until discovered.", x + 12, cy, width - 24, TerminalUi.MUTED);
                return;
            }
            if (entry.lines().isEmpty()) {
                wrap(context, graphics,
                        "Record detail is sealed until its owning route proof is recovered. Continue the active Ashfall mission chain or check Protocol Roadmap for the next unlock.",
                        x + 12, cy, width - 24, TerminalUi.MUTED);
                return;
            }
            for (String line : entry.lines()) {
                cy = wrap(context, graphics, line, x + 12, cy, width - 24, TerminalUi.TEXT) + 7;
            }
        }

        private int archiveDetailHeight(TerminalRenderContext context, ArchiveEntry entry, int width) {
            if (entry == null) {
                return 70;
            }
            int height = 56;
            if (entry.locked()) {
                return height + TerminalUi.wrappedHeight(context, entry.hint(), width) + 28;
            }
            if (entry.lines().isEmpty()) {
                return height + 28;
            }
            for (String line : entry.lines()) {
                height += TerminalUi.wrappedHeight(context, line, width) + 7;
            }
            return height;
        }

        private void selectArchiveEntry(Player player, ArchiveEntry entry) {
            selectedEntryId = entry.id();
            if (entry.dataLog() && "NEW".equals(entry.status())) {
                EchoIntel intel = EchoIntel.get(player);
                intel.markAsRead(entry.sourceIntelId());
                EchoNetClientActions.sendServerboundAction(new ArchiveIntelReadPacket(entry.sourceIntelId()));
            }
        }

        private void normalizeSelection(List<ArchiveEntry> entries) {
            if (!GROUPS.contains(selectedGroup)) {
                selectedGroup = GROUPS.get(0);
            }
            List<ArchiveEntry> groupEntries = entriesForGroup(entries, selectedGroup);
            if (groupEntries.stream().noneMatch(entry -> entry.id().equals(selectedEntryId))) {
                selectedEntryId = groupEntries.isEmpty() ? "" : groupEntries.get(0).id();
            }
        }

        private ArchiveEntry selectedEntry(List<ArchiveEntry> entries) {
            return entries.stream()
                    .filter(entry -> entry.id().equals(selectedEntryId))
                    .findFirst()
                    .orElse(null);
        }

        private List<ArchiveEntry> buildEntries(Player player) {
            List<ArchiveEntry> entries = new ArrayList<>();
            for (TerminalArchiveEntry entry : TerminalArchiveRegistry.entries()) {
                String group = normalizeGroup(entry.group());
                entries.add(new ArchiveEntry(
                        entry.id().toString(),
                        group,
                        entry.title(),
                        group,
                        entry.locked() ? "LOCKED" : entry.status(),
                        entry.locked(),
                        entry.locked() ? "Acquire the related field record to unlock full content." : "",
                        entry.lines(),
                        false,
                        ""));
            }

            EchoIntel intel = EchoIntel.get(player);
            for (EchoIntel.IntelEntry entry : intel.getAllIntel()) {
                entries.add(new ArchiveEntry(
                        "intel:" + entry.id,
                        "Signal Logs",
                        entry.title,
                        entry.type.getDisplayName(),
                        entry.isRead ? "READ" : "NEW",
                        false,
                        "",
                        List.of(entry.content, "Priority: " + entry.priority.getLabel()
                                + (entry.relatedFaction == null ? "" : " / Faction: "
                                        + AshfallFactionMap.displayName(entry.relatedFaction))),
                        true,
                        entry.id));
            }

            entries.add(new ArchiveEntry(
                    "locked:blackbox_protocol",
                    "Signal Logs",
                    "Crash Blackbox Record",
                    "Historical Record",
                    "LOCKED",
                    true,
                    "Recover data logs from ruins, drones, or faction routes.",
                    List.of(),
                    false,
                    ""));
            if (entries.stream().noneMatch(entry -> entry.group().equals("ECHO-0"))) {
                entries.add(new ArchiveEntry(
                        "locked:orbital_module",
                        "ECHO-0",
                        "ECHO-0 Route Module",
                        "Addon Path",
                        "MODULE",
                        true,
                        "Install Echo: Orbital Remnants to populate orbital records in this terminal.",
                        List.of(),
                        false,
                        ""));
            }
            entries.sort(Comparator.comparing(ArchiveEntry::group).thenComparing(ArchiveEntry::title));
            return List.copyOf(entries);
        }

        private static String normalizeGroup(String group) {
            if (group == null || group.isBlank()) {
                return "Recovered Lore";
            }
            String normalized = switch (group) {
                case "Survival" -> "Outpost Survival";
                case "Systems" -> "Machine Systems";
                case "Progression" -> "Protocol Flow";
                case "Threats" -> "Threat Dossier";
                case "Lore" -> "Recovered Lore";
                case "Data Logs" -> "Signal Logs";
                case "Orbital" -> "ECHO-0";
                default -> group;
            };
            for (String known : GROUPS) {
                if (known.equalsIgnoreCase(normalized)) {
                    return known;
                }
            }
            return "Recovered Lore";
        }

        private static List<ArchiveEntry> entriesForGroup(List<ArchiveEntry> entries, String group) {
            return entries.stream().filter(entry -> entry.group().equals(group)).toList();
        }

        private static GroupCount countGroup(List<ArchiveEntry> entries, String group) {
            int total = 0;
            int open = 0;
            for (ArchiveEntry entry : entries) {
                if (!entry.group().equals(group)) {
                    continue;
                }
                total++;
                if (!entry.locked()) {
                    open++;
                }
            }
            return new GroupCount(total, open);
        }

        private static int listHeight(List<ArchiveEntry> entries) {
            return 22 + entries.size() * 30;
        }

        private static int statusArchiveColor(String status) {
            return switch (status) {
                case "OPEN", "READ" -> TerminalUi.GREEN;
                case "NEW" -> TerminalUi.AMBER;
                case "LOCKED", "MODULE" -> TerminalUi.MUTED;
                default -> TerminalUi.TEXT;
            };
        }

        private record GroupCount(int total, int open) {
        }

        private record ArchiveEntry(
                String id,
                String group,
                String title,
                String category,
                String status,
                boolean locked,
                String hint,
                List<String> lines,
                boolean dataLog,
                String sourceIntelId) {
        }
    }

    private static final class StatusTab extends AshfallTab {
        private StatusTab() {
            super(STATUS, "VITALS SCAN", 180, 0xFF92F7A6,
                    TerminalTabChrome.of("Vitals Scan", TerminalTabChrome.GROUP_SYSTEMS, "VS",
                            "Systems and hazard scan", 180));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            QuestData quest = QuestData.get(context.player());
            ResearchData research = ResearchData.get(context.player());
            SurvivalData survival = context.player().getData(ModAttachments.SURVIVAL_DATA.get());
            EchoIntel intel = EchoIntel.get(context.player());
            List<EchoFactionProfile> factionProfiles = ashfallFactionProfiles(context.player());

            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            boolean wide = w >= 620;
            int leftW = wide ? Math.max(230, w / 2 - 8) : w;
            int rightX = wide ? x + leftW + 16 : x;
            int rightW = wide ? Math.max(180, w - leftW - 18) : w;

            int hazardReasonH = survival.getHazardReason().isBlank()
                    ? 0
                    : TerminalUi.wrappedHeight(context, survival.getHazardReason(), leftW - 16) + 4;
            int healthPanelH = Math.max(190, 170 + hazardReasonH);
            int cy = flatDataPanel(context, graphics, x, y, leftW, healthPanelH, "TERMINAL HEALTH", "") + 2;
            line(context, graphics, quest.isTerminalOnline() ? "LINK ONLINE" : "LINK DEGRADED", x + 12, cy, leftW - 24,
                    quest.isTerminalOnline() ? TerminalUi.GREEN : TerminalUi.RED);
            cy += 18;
            TerminalUi.meter(context, graphics, x + 12, cy, leftW - 24, "Terminal", quest.getTerminalHealth(), TerminalUi.GREEN);
            cy += 16;
            TerminalUi.meter(context, graphics, x + 12, cy, leftW - 24, "Drone", quest.getDroneHealth(), TerminalUi.AMBER);
            cy += 16;
            TerminalUi.meter(context, graphics, x + 12, cy, leftW - 24, "Hydration", survival.getHydration(), TerminalUi.CYAN);
            cy += 16;
            TerminalUi.meter(context, graphics, x + 12, cy, leftW - 24, "Radiation", Math.round(survival.getRadiationLevel()), TerminalUi.RED);
            cy += 19;
            line(context, graphics, "Air filter: " + Math.round(survival.getFilterPercent() * 100.0F) + "% / tier "
                    + survival.getFilterTier(), x + 12, cy, leftW - 24, TerminalUi.TEXT);
            cy += 14;
            line(context, graphics, "Hazard: " + survival.getPrimaryHazard() + " / " + survival.getHazardSeverity(),
                    x + 12, cy, leftW - 24, survival.isSafeZone() ? TerminalUi.GREEN : TerminalUi.AMBER);
            cy += 16;
            if (!survival.getHazardReason().isBlank()) {
                cy = wrap(context, graphics, survival.getHazardReason(), x + 12, cy, leftW - 24, TerminalUi.MUTED) + 4;
            }
            cy = Math.max(cy, y + healthPanelH + 10);
            cy = renderWeatherEventPanel(context, graphics, x, cy, leftW) + 4;

            int factionY = cy + 20;
            String activeContract = activeFactionContractLine(factionProfiles);
            int factionH = Math.max(112, 28 + factionProfiles.size() * 13
                    + TerminalUi.wrappedHeight(context, activeContract, leftW - 28));
            cy = flatDataPanel(context, graphics, x, factionY, leftW, factionH, "FACTION SUMMARY", "");
            for (EchoFactionProfile profile : factionProfiles) {
                int color = profile.contacted() ? profile.definition().accentColor() : TerminalUi.MUTED;
                line(context, graphics, profile.definition().shortName() + ": " + profile.reputation()
                        + " / " + profile.standing().displayName()
                        + (profile.contacted() ? " / contacted" : " / no contact"),
                        x + 12, cy, leftW - 24, color);
                cy += 13;
            }
            cy = wrap(context, graphics, activeContract, x + 12, cy + 4, leftW - 24, TerminalUi.MUTED);
            int leftBottom = cy;

            int rightY = wide ? y : leftBottom + 22;
            MissionUxSummary missionSummary = MissionUxSummary.current(context.player(), quest);
            int syncPanelH = Math.max(190, 154
                    + TerminalUi.wrappedHeight(context, "Next: " + missionSummary.nextStep(), rightW - 16));
            int ry = flatDataPanel(context, graphics, rightX, rightY, rightW, syncPanelH, "PROTOCOL SYNC", "");
            line(context, graphics, "Current: " + missionSummary.shortTitle(), rightX + 12, ry, rightW - 24,
                    summaryColor(missionSummary));
            ry += 14;
            line(context, graphics, missionSummary.statusLabel() + " / " + missionSummary.routeHint(),
                    rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            ry = wrap(context, graphics, "Next: " + missionSummary.nextStep(), rightX + 12, ry, rightW - 24,
                    summaryColor(missionSummary)) + 4;
            line(context, graphics, "Current phase: " + (quest.getCurrentPhase() + 1) + "/" + MissionRegistry.getPhaseCount(),
                    rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            line(context, graphics, "Unlocked missions: " + quest.getUnlockedMissionIds().size(),
                    rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            line(context, graphics, "Completed missions: " + quest.getCompletedMissionIds().size()
                    + "/" + MissionRegistry.getAllMissions().size(), rightX + 12, ry, rightW - 24, TerminalUi.GREEN);
            ry += 14;
            line(context, graphics, "Pending reward caches: " + quest.getAllPendingRewards().size(),
                    rightX + 12, ry, rightW - 24, quest.getAllPendingRewards().isEmpty() ? TerminalUi.MUTED : TerminalUi.AMBER);
            ry += 14;
            line(context, graphics, "ECHO relationship: " + quest.getEchoRelationship(), rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            line(context, graphics, "Research points: " + research.getPoints() + "/" + ResearchData.MAX_POINTS,
                    rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            line(context, graphics, "Research tier: " + research.getCurrentTier()
                    + " / perks " + research.getUnlockedPerks().size()
                    + " / schematics " + research.getUnlockedSchematics().size(),
                    rightX + 12, ry, rightW - 24, TerminalUi.TEXT);
            ry += 14;
            line(context, graphics, "Intel: " + intel.getAllIntel().size() + " records / "
                    + intel.getUnreadCount() + " unread", rightX + 12, ry, rightW - 24,
                    intel.getUnreadCount() > 0 ? TerminalUi.AMBER : TerminalUi.GREEN);
            ry = rightY + syncPanelH + 10;

            TerminalUi.compactCommandStrip(context, graphics, rightX, ry, rightW, "SYNC STATE",
                    "Server sync active. Mission, research, and intel commands remain validated by linked world state.",
                    TerminalUi.MUTED);
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), statusHeight(context));
        }

        private static int statusHeight(TerminalRenderContext context) {
            SurvivalData survival = context.player().getData(ModAttachments.SURVIVAL_DATA.get());
            List<EchoFactionProfile> factionProfiles = ashfallFactionProfiles(context.player());
            int w = context.contentWidth();
            boolean wide = w >= 620;
            int leftW = wide ? Math.max(230, w / 2 - 8) : w;
            int rightW = wide ? Math.max(180, w - leftW - 18) : w;
            String activeContract = activeFactionContractLine(factionProfiles);
            int hazardReasonH = survival.getHazardReason().isBlank()
                    ? 0
                    : TerminalUi.wrappedHeight(context, survival.getHazardReason(), leftW - 16) + 4;
            int healthPanelH = Math.max(190, 170 + hazardReasonH);
            int factionH = Math.max(112, 28 + factionProfiles.size() * 13
                    + TerminalUi.wrappedHeight(context, activeContract, leftW - 28));
            int leftH = healthPanelH + 10
                    + weatherEventPanelHeight(context, leftW) + 4
                    + 20 + factionH;
            String sync = "Server sync active. Mission, research, and intel commands remain validated by linked world state.";
            MissionUxSummary missionSummary = MissionUxSummary.current(context.player(), QuestData.get(context.player()));
            int missionSummaryH = 14 + 14
                    + TerminalUi.wrappedHeight(context, "Next: " + missionSummary.nextStep(), rightW) + 4;
            int rightH = missionSummaryH + 14 * 8 + 34 + TerminalUi.wrappedHeight(context, sync, rightW);
            return wide ? Math.max(leftH, rightH) : leftH + 22 + rightH;
        }

        private static int renderWeatherEventPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                                                   int x, int y, int width) {
            EnvironmentalEventStatus status = terminalEventStatus(context.player());
            int panelH = Math.max(112, weatherEventPanelHeight(context, width));
            int cy = TerminalUi.flatDataPanel(context, graphics,
                    x, y, width, panelH, "WEATHER EVENT", "", 0xFF92F7A6);
            TerminalUi.line(context, graphics, "Active: " + status.shortStatusText(), x + 12, cy, width - 24,
                    status.active() ? status.hudColor() : TerminalUi.GREEN);
            cy += 14;
            TerminalUi.line(context, graphics, "Weather override: " + status.weatherLabel()
                    + " / phase " + status.phasePercent() + "%", x + 12, cy, width - 24, TerminalUi.TEXT);
            cy += 14;
            TerminalUi.line(context, graphics, "Counts: " + weatherCountsLineOne(), x + 12, cy, width - 24, TerminalUi.MUTED);
            cy += 14;
            TerminalUi.line(context, graphics, "Counts: " + weatherCountsLineTwo(), x + 12, cy, width - 24, TerminalUi.MUTED);
            cy += 14;
            cy = TerminalUi.wrap(context, graphics, "Counter: " + status.counterGuidance(), x + 12, cy, width - 24,
                    status.active() ? TerminalUi.AMBER : TerminalUi.MUTED) + 3;
            cy = TerminalUi.wrap(context, graphics, "Impact: " + status.survivalImpact(), x + 12, cy, width - 24,
                    status.active() ? TerminalUi.TEXT : TerminalUi.MUTED);
            return Math.max(cy + 8, y + panelH + 2);
        }

        private static int weatherEventPanelHeight(TerminalRenderContext context, int width) {
            EnvironmentalEventStatus status = terminalEventStatus(context.player());
            int wrapW = Math.max(40, width - 16);
            return 17 + 14 * 4
                    + TerminalUi.wrappedHeight(context, "Counter: " + status.counterGuidance(), wrapW) + 3
                    + TerminalUi.wrappedHeight(context, "Impact: " + status.survivalImpact(), wrapW) + 16;
        }

        private static EnvironmentalEventStatus terminalEventStatus(Player player) {
            return HudState.getEnvironmentalEventStatus(player.level().getGameTime());
        }

        private static String weatherCountsLineOne() {
            return "Rad " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.RADIATION_STORM)
                    + " / Acid " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.TOXIC_STORM)
                    + " / Blackout " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.BLACKOUT);
        }

        private static String weatherCountsLineTwo() {
            return "Ash " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.ASH_STORM)
                    + " / Cryo " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.CRYO_FRONT)
                    + " / Surge " + HudState.getEnvEventSurvivalCount(EnvironmentalEventType.NEXUS_SURGE);
        }
    }

    private static final class DroneTab extends AshfallTab {
        private DroneTab() {
            super(DRONE, "COMPANION LINK", 190, 0xFFFFD166,
                    TerminalTabChrome.of("Companion Link", TerminalTabChrome.GROUP_SYSTEMS, "CL",
                            "Drone command channel", 190));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            QuestData quest = QuestData.get(context.player());
            CompanionDroneData droneData = context.player().getData(ModAttachments.COMPANION_DRONE_DATA.get());
            DroneSnapshot snapshot = DroneSnapshot.scan(context.player());
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            boolean wide = w >= 620;
            int leftW = wide ? Math.max(330, Math.min(520, w * 55 / 100)) : w;
            int rightX = wide ? x + leftW + 14 : x;
            int rightW = wide ? Math.max(220, w - leftW - 18) : w;

            int panelY = y;
            int panelH = Math.max(194, Math.min(236, context.contentHeight() / 3));
            int cy = flatDataPanel(context, graphics, x, panelY, leftW, panelH, "DRONE COMMAND LINK", "");
            TerminalUi.hybridIconBadge(graphics, TerminalVisualAssets.ICON_PAGE_COMPANION_LINK, TerminalIcon.DRONE,
                    x + 14, cy + 2, 38, descriptor().accentColor(), true);
            line(context, graphics, "Companion", x + 62, cy + 2, 78, TerminalUi.MUTED);
            line(context, graphics, snapshot.companionStatus(), x + 148, cy + 2, leftW - 160, snapshot.companionColor());
            cy += 12;
            line(context, graphics, "Scout", x + 62, cy + 2, 78, TerminalUi.MUTED);
            line(context, graphics, snapshot.scoutStatus(), x + 148, cy + 2, leftW - 160, snapshot.scoutColor());
            cy += 12;
            line(context, graphics, "Repair", x + 62, cy + 2, 78, TerminalUi.MUTED);
            line(context, graphics, quest.getDroneStage().name() + " / integrity " + quest.getDroneHealth() + "%",
                    x + 148, cy + 2, leftW - 160, TerminalUi.TEXT);
            cy += 14;
            TerminalUi.progress(graphics, x + 62, cy + 2, Math.min(300, leftW - 76), 8, quest.getDroneHealth() / 100.0F,
                    quest.getDroneHealth() >= 50 ? TerminalUi.GREEN : TerminalUi.AMBER);
            cy += 13;
            line(context, graphics, "Battery", x + 62, cy + 2, 78, TerminalUi.MUTED);
            int batteryColor = droneData.getBatteryPercent() <= 15 ? TerminalUi.AMBER : TerminalUi.GREEN;
            line(context, graphics, droneData.getBatteryPercent() + "% / signal " + droneData.signalLabel(),
                    x + 148, cy + 2, leftW - 160, batteryColor);
            cy += 12;
            line(context, graphics, "Task", x + 62, cy + 2, 78, TerminalUi.MUTED);
            line(context, graphics, droneData.getMode().displayName() + " / " + droneData.getTaskLabel(),
                    x + 148, cy + 2, leftW - 160, TerminalUi.TEXT);
            cy += 12;
            line(context, graphics, "Last scan", x + 62, cy + 2, 78, TerminalUi.MUTED);
            line(context, graphics, droneData.getLastScanSummary(), x + 148, cy + 2, leftW - 160, TerminalUi.MUTED);

            int gridY = panelY + panelH - 78;
            int buttonW = Math.max(70, Math.min(116, (leftW - 28) / 3));
            drawCommandButton(context, graphics, x + 8, gridY, buttonW, "RECALL", true, "recall", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 14 + buttonW, gridY, buttonW, "SCAN", true, "scan_area", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 20 + buttonW * 2, gridY, buttonW, "SCOUT",
                    quest.canUseDroneMode("SCOUT"), "scout_ahead", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 8, gridY + 22, buttonW, "ASSIST",
                    true, "toggle_assist", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 14 + buttonW, gridY + 22, buttonW, "SALVAGE",
                    quest.canUseDroneMode("SCAVENGE"), "collect_scrap", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 20 + buttonW * 2, gridY + 22, buttonW, "GUARD",
                    true, "guard_here", mouseX, mouseY);
            drawCommandButton(context, graphics, x + 8, gridY + 44, Math.min(leftW - 16, buttonW * 2 + 6), "TOGGLE LIGHT",
                    quest.isDroneLightEnabled(), "toggle_light", mouseX, mouseY);

            int infoY = wide ? y : panelY + panelH + 12;
            int lockPanelY = infoY;
            int lockH = wide ? panelH : 156;
            cy = flatDataPanel(context, graphics, rightX, lockPanelY, rightW, lockH, "MODE LOCKS", "");
            cy = lockLine(context, graphics, quest, rightX + 12, cy, rightW - 24, "SCOUT", "Partial repair required.");
            cy = lockLine(context, graphics, quest, rightX + 12, cy, rightW - 24, "COMBAT", "Operational repair required.");
            cy = lockLine(context, graphics, quest, rightX + 12, cy, rightW - 24, "SCAVENGE", "Operational repair required.");
            cy = lockLine(context, graphics, quest, rightX + 12, cy, rightW - 24, "PATROL", "Enhanced repair required.");
            TerminalUi.wrap(context, graphics,
                    snapshot.hasAnyDrone()
                            ? "Commands are ready. If a mode is unavailable, repair integrity until the lock reason clears."
                            : "No linked drone is currently in command range. Recall and follow will reconnect when a valid owned drone is nearby.",
                    rightX + 12, cy + 8, rightW - 24, TerminalUi.MUTED);
            if (wide) {
                int telemetryY = panelY + panelH + 14;
                int telemetryH = Math.min(132, Math.max(96, context.contentHeight() - (telemetryY - y)));
                if (telemetryH >= 96) {
                    int ty = flatDataPanel(context, graphics, x, telemetryY, w, telemetryH, "DRONE TELEMETRY", "");
                    int tileGap = 10;
                    int tileW = Math.max(120, (w - tileGap * 2) / 3);
                    TerminalUi.denseDataCard(context, graphics, x + 12, ty + 2, tileW, "REPAIR STATE",
                            droneData.getMode().displayName(), "Integrity " + quest.getDroneHealth() + "%", TerminalUi.AMBER);
                    TerminalUi.denseDataCard(context, graphics, x + 12 + tileW + tileGap, ty + 2, tileW, "COMMAND RANGE",
                            snapshot.hasAnyDrone() ? "LINKED" : "NO CONTACT",
                            snapshot.hasAnyDrone() ? "Nearby owned drone detected" : "Recall/follow will reconnect when valid",
                            snapshot.hasAnyDrone() ? TerminalUi.GREEN : TerminalUi.MUTED);
                    TerminalUi.denseDataCard(context, graphics, x + 12 + (tileW + tileGap) * 2, ty + 2,
                            Math.max(120, w - 24 - (tileW + tileGap) * 2), "FIELD STATUS",
                            "Battery " + droneData.getBatteryPercent() + "%",
                            "Signal " + droneData.signalLabel() + " / " + droneData.getTaskLabel(),
                            droneData.getSignalQuality() <= 25 ? TerminalUi.AMBER : TerminalUi.CYAN);
                }
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), droneHeight(context));
        }

        private static int droneHeight(TerminalRenderContext context) {
            QuestData quest = QuestData.get(context.player());
            DroneSnapshot snapshot = DroneSnapshot.scan(context.player());
            String lightLine = "Light module: "
                    + (quest.isDroneLightEnabled() ? "AVAILABLE" : "LOCKED UNTIL PARTIAL REPAIR");
            String summary = snapshot.hasAnyDrone()
                    ? "Commands are ready. If a mode is unavailable, repair integrity until the lock reason clears."
                    : "No linked drone is currently in command range. Recall and follow will reconnect when a valid owned drone is nearby.";
            int w = context.contentWidth();
            boolean wide = w >= 620;
            int rightW = wide ? Math.max(220, w - Math.max(330, Math.min(520, w * 55 / 100)) - 18) : w;
            int locksH = 194;
            int leftH = 194;
            int stackedH = leftH + 12 + locksH + TerminalUi.wrappedHeight(context, summary, rightW);
            return wide ? Math.max(context.contentHeight(), Math.max(leftH, locksH) + 128) : Math.max(context.contentHeight(), stackedH);
        }

        private void drawCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, String label, boolean enabled, String command, double mouseX, double mouseY) {
            boolean effectiveEnabled = enabled && !referenceOnly(context);
            boolean hovered = effectiveEnabled && TerminalUi.inside(mouseX, mouseY, x, y, width, 16);
            if (effectiveEnabled) {
                TerminalUi.primaryCommandButton(context, graphics, x, y, width, 18, label,
                        TerminalVisualAssets.ICON_ACTION_SCAN, descriptor().accentColor(), hovered);
            } else {
                TerminalUi.disabledCommandButton(context, graphics, x, y, width, 18, label,
                        TerminalVisualAssets.ICON_STATE_LOCKED);
            }
            addHitbox(x, y, width, 16, effectiveEnabled, () -> context.sendAction(DRONE, DRONE_COMMAND, command));
        }

        private int lockLine(TerminalRenderContext context, GuiGraphicsExtractor graphics, QuestData quest,
                int x, int y, int width, String mode, String reason) {
            boolean available = quest.canUseDroneMode(mode);
            line(context, graphics, mode + ": " + (available ? "available" : reason), x, y, width,
                    available ? TerminalUi.GREEN : TerminalUi.MUTED);
            return y + 12;
        }

        private record DroneSnapshot(String companionStatus, int companionColor, String scoutStatus, int scoutColor,
                                     boolean hasAnyDrone) {
            static DroneSnapshot scan(Player player) {
                List<EchoCompanionDrone> companions = player.level().getEntitiesOfClass(EchoCompanionDrone.class,
                        player.getBoundingBox().inflate(128.0D),
                        drone -> !drone.isRemoved() && drone.isAlive() && player.getUUID().equals(drone.getOwnerUUID()));
                List<ScoutDrone> scouts = player.level().getEntitiesOfClass(ScoutDrone.class,
                        player.getBoundingBox().inflate(128.0D),
                        drone -> !drone.isRemoved() && drone.isAlive() && player.getUUID().equals(drone.getOwnerUUID()));
                String companion = companions.isEmpty()
                        ? "no linked companion in range"
                        : companions.get(0).getCurrentMode().getDisplayName() + " / "
                                + companions.get(0).getRepairLevel() + "% / light "
                                + (companions.get(0).isLightEnabled() ? "on" : "off");
                String scout = scouts.isEmpty()
                        ? "no Scout Drone in range"
                        : scouts.get(0).getMode().getDisplayName();
                return new DroneSnapshot(companion, companions.isEmpty() ? TerminalUi.MUTED : TerminalUi.GREEN,
                        scout, scouts.isEmpty() ? TerminalUi.MUTED : TerminalUi.GREEN,
                        !companions.isEmpty() || !scouts.isEmpty());
            }
        }
    }

    private static final class CodexTab extends AshfallTab {
        private CodexCategory selectedCategory = CodexCategory.START;
        private String selectedEntryId = "";

        private CodexTab() {
            super(CODEX, "SURVIVAL INDEX", 150, 0xFFB7D8FF,
                    TerminalTabChrome.of("Survival Index", TerminalTabChrome.GROUP_FIELD, "SI",
                            "Intel and recipes index", 150));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            QuestData quest = QuestData.get(context.player());
            EchoIntel intel = EchoIntel.get(context.player());
            ResearchData research = ResearchData.get(context.player());
            PostNexusData post = PostNexusData.get(context.player());
            CodexState state = new CodexState(context.player(), quest, intel, research, post);
            List<CodexEntry> entries = buildCodexEntries(state);
            normalizeCodexSelection(entries);
            List<CodexEntry> visibleEntries = entries.stream()
                    .filter(entry -> entry.category() == selectedCategory)
                    .toList();
            CodexEntry selected = selectedCodexEntry(visibleEntries);
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();

            boolean wide = w >= 680;
            boolean medium = w >= 500;
            if (wide) {
                int railW = 122;
                int listW = 214;
                int detailX = x + railW + listW + 24;
                int detailW = Math.max(180, w - railW - listW - 30);
                renderCodexRail(context, graphics, entries, x, y, railW, mouseX, mouseY);
                renderCodexEntryList(context, graphics, visibleEntries, x + railW + 10, y, listW, mouseX, mouseY);
                renderCodexDetail(context, graphics, selected, detailX, y, detailW, context.contentHeight());
            } else {
                int railH = renderCodexRail(context, graphics, entries, x, y, w, mouseX, mouseY);
                int listY = y + railH + 10;
                int listW = medium ? Math.min(230, w * 44 / 100) : w;
                int detailX = medium ? x + listW + 12 : x;
                int detailY = medium ? listY : listY + codexListHeight(visibleEntries) + 12;
                int detailW = medium ? Math.max(160, w - listW - 12) : w;
                renderCodexEntryList(context, graphics, visibleEntries, x, listY, listW, mouseX, mouseY);
                int detailMinH = medium ? Math.max(150, context.contentHeight() - (detailY - y)) : 150;
                renderCodexDetail(context, graphics, selected, detailX, detailY, detailW, detailMinH);
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            int w = context.contentWidth();
            CodexState state = new CodexState(context.player(), QuestData.get(context.player()),
                    EchoIntel.get(context.player()), ResearchData.get(context.player()), PostNexusData.get(context.player()));
            List<CodexEntry> entries = buildCodexEntries(state);
            List<CodexEntry> visibleEntries = entries.stream()
                    .filter(entry -> entry.category() == selectedCategory)
                    .toList();
            CodexEntry selected = selectedCodexEntry(visibleEntries);
            boolean wide = w >= 680;
            boolean medium = w >= 500;
            int railH = 18 + CodexCategory.values().length * 20;
            int listH = 18 + codexListHeight(visibleEntries);
            int detailW = wide ? Math.max(180, w - 122 - 214 - 30) : medium ? Math.max(160, w - Math.min(230, w * 44 / 100) - 12) : w;
            int detailH = codexDetailHeight(context, selected, detailW);
            int measured = wide ? Math.max(railH, Math.max(listH, detailH))
                    : medium ? railH + 10 + Math.max(listH, detailH)
                    : railH + 10 + listH + 12 + detailH;
            return Math.max(context.contentHeight(), measured);
        }

        private void normalizeCodexSelection(List<CodexEntry> entries) {
            if (selectedCategory == null) {
                selectedCategory = CodexCategory.START;
            }
            boolean hasCategory = entries.stream().anyMatch(entry -> entry.category() == selectedCategory);
            if (!hasCategory) {
                selectedCategory = CodexCategory.START;
            }
            List<CodexEntry> categoryEntries = entries.stream()
                    .filter(entry -> entry.category() == selectedCategory)
                    .toList();
            boolean hasEntry = categoryEntries.stream().anyMatch(entry -> entry.id().equals(selectedEntryId));
            if (!hasEntry) {
                selectedEntryId = categoryEntries.isEmpty() ? "" : categoryEntries.get(0).id();
            }
        }

        private CodexEntry selectedCodexEntry(List<CodexEntry> entries) {
            return entries.stream()
                    .filter(entry -> entry.id().equals(selectedEntryId))
                    .findFirst()
                    .orElse(entries.isEmpty() ? null : entries.get(0));
        }

        private int renderCodexRail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<CodexEntry> entries, int x, int y, int w, int mouseX, int mouseY) {
            int panelH = 34 + CodexCategory.values().length * 20;
            int cy = flatDataPanel(context, graphics, x, y, w, panelH, "SURVIVAL INDEX", "");
            for (CodexCategory category : CodexCategory.values()) {
                long total = entries.stream().filter(entry -> entry.category() == category).count();
                long unlocked = entries.stream().filter(entry -> entry.category() == category && entry.unlocked()).count();
                boolean selected = category == selectedCategory;
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 10, cy, w - 20, 17);
                TerminalUi.selectableRow(graphics, x + 10, cy, w - 20, 17, selected, hover, descriptor().accentColor());
                String count = unlocked + "/" + total;
                line(context, graphics, category.label(), x + 16, cy + 5, Math.max(38, w - 58),
                        selected ? TerminalUi.TEXT : TerminalUi.MUTED);
                line(context, graphics, count, x + w - 50, cy + 5, 36,
                        unlocked == total ? TerminalUi.GREEN : TerminalUi.AMBER);
                addHitbox(x + 10, cy, w - 20, 17, () -> {
                    selectedCategory = category;
                    selectedEntryId = "";
                });
                cy += 20;
            }
            return Math.max(panelH, cy - y + 8);
        }

        private void renderCodexEntryList(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<CodexEntry> entries, int x, int y, int w, int mouseX, int mouseY) {
            int panelH = 44 + (entries.isEmpty() ? 18 : entries.size() * 38);
            int cy = flatDataPanel(context, graphics, x, y, w, panelH, selectedCategory.label() + " ENTRIES", "");
            line(context, graphics, selectedCategory.summary(), x + 12, cy, w - 24, TerminalUi.MUTED);
            cy += 16;
            if (entries.isEmpty()) {
                line(context, graphics, "No entries in this category.", x + 12, cy, w - 24, TerminalUi.MUTED);
                return;
            }
            for (CodexEntry entry : entries) {
                boolean selected = entry.id().equals(selectedEntryId);
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 10, cy, w - 20, 34);
                int color = entry.unlocked() ? typeColor(entry.type()) : TerminalUi.MUTED;
                String status = (entry.unlocked() ? "OPEN / " : "LOCKED / ") + entry.type().label();
                dataRow(context, graphics, x + 10, cy, w - 20, 34, entry.title(), status,
                        entry.unlocked() ? "OPEN" : "LOCKED", selected, hover,
                        entry.unlocked() ? TerminalUi.GREEN : TerminalUi.AMBER);
                if (!selected && color != TerminalUi.MUTED) {
                    graphics.fill(x + 13, cy + 4, x + 16, cy + 30, color);
                }
                addHitbox(x + 10, cy, w - 20, 34, () -> selectedEntryId = entry.id());
                cy += 38;
            }
        }

        private void renderCodexDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                CodexEntry entry, int x, int y, int w, int minHeight) {
            int panelH = Math.max(Math.max(150, minHeight), codexDetailHeight(context, entry, w));
            int cy = flatDataPanel(context, graphics, x, y, w, panelH, "FIELD DETAIL", "");
            if (entry == null) {
                line(context, graphics, "No Codex entry selected.", x + 12, cy, w - 24, TerminalUi.MUTED);
                return;
            }
            TerminalUi.hybridIconBadge(graphics, TerminalVisualAssets.ICON_PAGE_SURVIVAL_INDEX, TerminalIcon.CODEX,
                    x + 12, cy, 40, descriptor().accentColor(), entry.unlocked());
            line(context, graphics, entry.title(), x + 62, cy + 4, w - 74,
                    entry.unlocked() ? TerminalUi.TEXT : TerminalUi.MUTED);
            line(context, graphics, entry.type().label() + " / " + (entry.unlocked() ? entry.status() : "LOCKED"),
                    x + 62, cy + 20, w - 74, entry.unlocked() ? typeColor(entry.type()) : TerminalUi.AMBER);
            cy += 52;
            if (!entry.unlocked()) {
                cy = drawCodexBlock(context, graphics, "LOCK", entry.lockReason(), x + 12, cy, w - 24, TerminalUi.AMBER);
                drawCodexBlock(context, graphics, "NEXT", entry.nextHint(), x + 12, cy + 4, w - 24, TerminalUi.MUTED);
                return;
            }
            cy = drawCodexBlock(context, graphics, "SUMMARY", entry.description(), x + 12, cy, w - 24, TerminalUi.TEXT);
            cy = drawCodexBlock(context, graphics, "USE", entry.practicalUse(), x + 12, cy + 4, w - 24, TerminalUi.CYAN);
            cy = drawCodexBlock(context, graphics, "PREP", entry.prep(), x + 12, cy + 4, w - 24, TerminalUi.AMBER);
            cy = drawCodexBlock(context, graphics, "PROGRESS", entry.progress(), x + 12, cy + 4, w - 24, TerminalUi.GREEN);
            cy = drawCodexList(context, graphics, "REWARDS/OUTPUTS", entry.rewards(), x + 12, cy + 4, w - 24, TerminalUi.TEXT);
            drawCodexList(context, graphics, "NOTES", entry.notes(), x + 12, cy + 4, w - 24, TerminalUi.MUTED);
        }

        private int drawCodexBlock(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String label, String text, int x, int y, int w, int color) {
            if (text == null || text.isBlank()) {
                return y;
            }
            line(context, graphics, label, x, y, w, color);
            return wrap(context, graphics, text, x + 8, y + 12, w - 8, TerminalUi.TEXT) + 4;
        }

        private int drawCodexList(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                String label, List<String> lines, int x, int y, int w, int color) {
            if (lines == null || lines.isEmpty()) {
                return y;
            }
            line(context, graphics, label, x, y, w, color);
            int cy = y + 12;
            for (String line : lines) {
                cy = wrap(context, graphics, "- " + line, x + 8, cy, w - 8, TerminalUi.TEXT) + 3;
            }
            return cy;
        }

        private int codexListHeight(List<CodexEntry> entries) {
            return 12 + (entries.isEmpty() ? 14 : entries.size() * 36);
        }

        private int codexDetailHeight(TerminalRenderContext context, CodexEntry entry, int width) {
            if (entry == null) {
                return 88;
            }
            int bodyW = Math.max(80, width - 8);
            int height = 88;
            if (!entry.unlocked()) {
                return height
                        + TerminalUi.wrappedHeight(context, entry.lockReason(), bodyW)
                        + TerminalUi.wrappedHeight(context, entry.nextHint(), bodyW)
                        + 34;
            }
            height += TerminalUi.wrappedHeight(context, entry.description(), bodyW) + 18;
            height += TerminalUi.wrappedHeight(context, entry.practicalUse(), bodyW) + 18;
            height += TerminalUi.wrappedHeight(context, entry.prep(), bodyW) + 18;
            height += TerminalUi.wrappedHeight(context, entry.progress(), bodyW) + 18;
            for (String line : entry.rewards()) {
                height += TerminalUi.wrappedHeight(context, "- " + line, bodyW) + 3;
            }
            for (String line : entry.notes()) {
                height += TerminalUi.wrappedHeight(context, "- " + line, bodyW) + 3;
            }
            return height + 34;
        }

        private List<CodexEntry> buildCodexEntries(CodexState state) {
            List<CodexEntry> entries = new ArrayList<>();
            entries.add(entry(state, "start_survival", CodexCategory.START, CodexEntryType.PROTOCOL,
                    "Ashfall Survival Loop", "OPEN", CodexUnlockRule.ALWAYS, "",
                    "Survive the opening crash by stabilizing water, food, shelter, filters, and the damaged ECHO terminal.",
                    "Use this as the baseline checklist whenever a route pushes into a harsher biome or a louder part of the old signal.",
                    "Keep clean water, cooked food, spare filters, and a safe return point.",
                    "Active route: " + MissionUxSummary.current(state.player(), state.quest()).shortTitle(),
                    List.of("Stable early base", "Terminal reward flow", "Drone repair path"),
                    List.of("ECHO validates completion from field state before releasing sealed caches."),
                    "This protocol is always available.", "Open the terminal after the crash."));
            entries.add(entry(state, "start_missions", CodexCategory.START, CodexEntryType.PROTOCOL,
                    "Mission Route Flow", "OPEN", CodexUnlockRule.ALWAYS, "",
                    "ECHO missions unlock in order, verify progress against world state, and store many rewards in the terminal.",
                    "Use the mission browser for active objectives, then return here for context when the next order feels too quiet.",
                    "Claim pending rewards before crafting late-step machines or route items.",
                    state.quest().getCompletedMissionIds().size() + "/" + MissionRegistry.getAllMissions().size() + " missions complete.",
                    List.of("Pending rewards: " + state.quest().getAllPendingRewards().size()),
                    List.of("Locked Codex entries reveal hints without spoiling full mechanics."),
                    "This protocol is always available.", "Complete ECHO's first field checks."));
            entries.add(entry(state, "items_radaway", CodexCategory.ITEMS, CodexEntryType.ITEM,
                    "RadAway And Exposure Medicine", "FIELD READY", CodexUnlockRule.PROGRESS_PHASE, "2",
                    "RadAway and medical supplies counter radiation-zone mistakes, reactor guardian pressure, and the slow confidence that gets survivors killed.",
                    "Carry it before entering radiation routes, reactor ruins, and Behemoth-linked sites.",
                    "Pair medicine with armor repairs and a planned retreat path.",
                    "Radiation biome visits: " + countContains(state.quest().getVisitedBiomes(), "radiation"),
                    List.of("Radiation cleanup", "Reactor guardian prep"),
                    List.of("The Radiation Behemoth expects cleanup between pulses."),
                    "Medical protocols are still locked.", "Advance into resource stability or discover a radiation route."));
            entries.add(entry(state, "items_alloy", CodexCategory.ITEMS, CodexEntryType.ITEM,
                    "Dense Alloy And Schematics", "RECOVERED TECH", CodexUnlockRule.PROGRESS_PHASE, "3",
                    "Dense alloy chunks and schematic fragments are the backbone of mid-game machines, armor, weapons, and the first real argument with the Gridfall.",
                    "Use guardian rewards and structure caches to close gaps before harsher biomes.",
                    "Do not spend rare fragments until the matching machine or gear route is clear.",
                    "Schematics unlocked: " + state.research().getUnlockedSchematics().size(),
                    List.of("Machine schematics", "Weapon/armor fragments", "Upgrade materials"),
                    List.of("Guardian rewards intentionally point toward their counterplay theme."),
                    "Advanced salvage references are locked.", "Complete more early machine and faction bridge missions."));
            entries.add(entry(state, "items_nexus_crystal", CodexCategory.ITEMS, CodexEntryType.ITEM,
                    "Nexus Crystals", "ANOMALY MATERIAL", CodexUnlockRule.ANY_GUARDIAN, "",
                    "Nexus crystals are guardian-tier materials used by late progression and Nexus-route systems. They behave less like ore than frozen command residue.",
                    "Treat them as strategic materials rather than common crafting stock.",
                    "Defeat biome guardians and keep the drops secured before pushing the Core route.",
                    "Guardian kills recorded: " + guardianKillCount(state.quest()),
                    List.of("Nexus route materials", "Late energy progression"),
                    List.of("The Nexus Scar Avatar drops the largest guardian crystal bundle."),
                    "Nexus materials are still unidentified.", "Neutralize any biome guardian."));
            entries.add(entry(state, "machines_power", CodexCategory.MACHINES, CodexEntryType.MACHINE,
                    "Power Network", "UTILITY", CodexUnlockRule.PROGRESS_PHASE, "2",
                    "Generators, cables, power nodes, and load-routing machines turn survival bases into route platforms that can survive a bad night and a worse signal.",
                    "Use power stability before committing to Nexus, cryo, or orbital preparation.",
                    "Keep a manual fallback for water, food, and defense if the grid is damaged.",
                    "Power nodes collected: " + state.quest().getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES,
                    List.of("Base automation", "Nexus Core requirements", "Machine throughput"),
                    List.of("Power Nodes matter twice: exploration utility and Nexus gate progress."),
                    "Power routing references are locked.", "Stabilize basic resources and start grid missions."));
            entries.add(entry(state, "machines_research", CodexCategory.MACHINES, CodexEntryType.MACHINE,
                    "Research And Fabrication", "SCHEMATIC", CodexUnlockRule.PROGRESS_PHASE, "3",
                    "Research points, schematic fragments, and rare tech schematics provide the controlled tech climb: recover a memory, test it, then trust it only when it works.",
                    "Use research to convert exploration wins into reliable crafting options.",
                    "Spend points on the route you are actively building, not every tempting branch.",
                    "Research points: " + state.research().getPoints() + "/" + ResearchData.MAX_POINTS,
                    List.of("Perks: " + state.research().getUnlockedPerks().size(), "Schematics: " + state.research().getUnlockedSchematics().size()),
                    List.of("Guardian and route rewards are meant to supplement research, not replace it."),
                    "Research references are locked.", "Recover enough infrastructure to begin schematic work."));
            entries.add(entry(state, "hazards_radiation", CodexCategory.HAZARDS, CodexEntryType.HAZARD,
                    "Radiation And Reactor Hot Zones", "HAZARD", CodexUnlockRule.VISITED_BIOME, "radiation",
                    "Radiation pressure punishes long fights, poor cleanup discipline, and the belief that an invisible threat is not currently working.",
                    "Use RadAway, hazmat gear, scrubber pockets, and short exposure windows.",
                    "Carry medicine before entering reactor basements or red-zone routes.",
                    "Visited radiation routes: " + countContains(state.quest().getVisitedBiomes(), "radiation"),
                    List.of("RadAway value", "Reactor cleanup rewards"),
                    List.of("Radiation Behemoth turns this hazard into an encounter rule."),
                    "Radiation records are locked.", "Enter or scan a radiation route."));
            entries.add(entry(state, "hazards_cold", CodexCategory.HAZARDS, CodexEntryType.HAZARD,
                    "Cryo Cold And Freeze Control", "HAZARD", CodexUnlockRule.VISITED_BIOME, "cryo",
                    "Cryo zones pressure movement, warmth, and long-duration planning. Cold is quiet enough to feel fair until your hands stop answering.",
                    "Use thermal liners, hand warmers, and warm-pocket counterplay.",
                    "Avoid overlapping slow windows during cold fights.",
                    "Visited cold routes: " + countContains(state.quest().getVisitedBiomes(), "cryo"),
                    List.of("Thermal prep", "Cryogenic guardian rewards"),
                    List.of("Cryogenic Overseer and Europa Cryo Warden both test thermal discipline."),
                    "Cryo records are locked.", "Enter or scan a cryogenic route."));
            entries.add(entry(state, "hazards_toxic", CodexCategory.HAZARDS, CodexEntryType.HAZARD,
                    "Toxic Air And Bio Pressure", "HAZARD", CodexUnlockRule.VISITED_BIOME, "toxic",
                    "Toxic routes become dangerous when poison, filters, and add pressure overlap. If the mask starts to feel routine, leave before it becomes an autopsy note.",
                    "Use filters, clean water, and fast add-clear tools.",
                    "Refresh filters before entering hive or swamp structures.",
                    "Visited toxic routes: " + countContains(state.quest().getVisitedBiomes(), "toxic"),
                    List.of("Filter cartridges", "Mutated tissue", "Bio-hive rewards"),
                    List.of("Toxic Hive Matriarch rewards directly support filtration."),
                    "Toxic records are locked.", "Enter or scan a toxic route."));
            entries.add(entry(state, "exploration_sites", CodexCategory.EXPLORATION, CodexEntryType.SITE,
                    "POIs And Route Records", "FIELD MAP", CodexUnlockRule.ALWAYS, "",
                    "Exploration sites log route markers, POI objective states, power nodes, and special discoveries. Every marked ruin is a question the old world failed to answer.",
                    "Use WORLD for raw records and CODEX for what those records mean.",
                    "Bring repair supplies when investigating unknown structures.",
                    "Discovered POIs: " + state.quest().getDiscoveredPOICount(),
                    List.of("Route markers", "Objective state records", "Power-node discoveries"),
                    List.of("Guardian entrances are tracked separately once a guardian mission is active."),
                    "Exploration records are locked.", "Move beyond the crash site."));
            entries.add(entry(state, "exploration_guardian_sites", CodexCategory.EXPLORATION, CodexEntryType.SITE,
                    "Guardian Entrances", "GUARDIAN ROUTE", CodexUnlockRule.ANY_GUARDIAN_MISSION, "",
                    "Each biome guardian has a surface entrance and an underground threat site. The entrance is the warning; the arena is the argument.",
                    "Follow the compass before combat; once the guardian is live, the HUD points to the active threat instead.",
                    "Clear side rooms, mark exits, and prepare counterplay before entering the arena.",
                    "Active guardian missions unlocked: " + unlockedGuardianMissionCount(state.quest()),
                    List.of("Mission-smart compass", "Guardian arena route", "Guardian reward bundle"),
                    List.of("Defeated sites stop providing compass targets."),
                    "Guardian site records are locked.", "Progress until ECHO identifies a biome guardian route."));
            for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
                entries.add(guardianEntry(state, profile));
            }
            entries.add(entry(state, "nexus_core", CodexCategory.NEXUS, CodexEntryType.PROTOCOL,
                    "Nexus Core Route", "ENDGAME", CodexUnlockRule.ALL_GUARDIANS, "",
                    "The Nexus Core accepts a final route only after all biome guardians are resolved and power is stable. It does not ask because it needs permission; it asks because orders bind history.",
                    "Use it to choose the world's post-Nexus direction: restore, destroy, or control.",
                    "Resolve guardians first, then confirm power-node readiness.",
                    "Guardians defeated: " + guardianKillCount(state.quest()) + "/" + BiomeGuardianProfiles.all().size(),
                    List.of("Post-Nexus choice", "Path-specific missions", "World-state transition"),
                    List.of("The chosen path is mirrored through ECHO Core services for addons."),
                    "Nexus Core records are locked.", "Neutralize all biome guardians."));
            entries.add(entry(state, "nexus_warden", CodexCategory.NEXUS, CodexEntryType.BOSS,
                    "The Warden", "ARCHIVE THREAT", CodexUnlockRule.POST_NEXUS_CHOICE, "",
                    "The Warden guards the Pre-Fall Archives after a Nexus choice is made. It protects history from decay, theft, and anyone trying to write the ending alone.",
                    "Break defender lockdowns, survive archive pulses, and claim the final protocol route.",
                    "Bring endgame armor, medicine, sustained damage, and room to clear defenders.",
                    state.post().isWardenDefeated() ? "Warden defeated." : "Warden unresolved.",
                    List.of("Warden Archive Cipher", "Nexus crystals", "Dense alloy", "Energy cells", "Final protocol readiness"),
                    List.of("The encounter HUD uses Archive Lockdown as its warning line."),
                    "Archive threat records are locked.", "Make a Nexus choice first."));
            addOrbitalEntries(entries, state);
            return entries;
        }

        private CodexEntry guardianEntry(CodexState state, BiomeGuardianProfile profile) {
            BiomeGuardianProfile.CinematicCue cue = profile.cinematicCue();
            BiomeGuardianProfile.PolishData polish = profile.polish();
            boolean defeated = state.quest().getEntityKills(profile.entityId()) > 0
                    || state.quest().isMissionCompleted(profile.missionId());
            return entry(state, "guardian_" + profile.bossPath(), CodexCategory.GUARDIANS, CodexEntryType.BOSS,
                    profile.title(), defeated ? "DEFEATED" : "GUARDIAN SIGNAL",
                    CodexUnlockRule.MISSION_UNLOCKED, profile.missionId(),
                    polish.codexSummary() + " " + profile.lore() + " Faction owner: "
                            + AshfallFactionMap.displayName(profile.ownerFaction()) + ".",
                    profile.mechanicHint(),
                    profile.prepHint(),
                    (defeated ? "Defeated. " : "Mission state: ")
                            + (state.quest().isMissionCompleted(profile.missionId()) ? "complete" : state.quest().isMissionUnlocked(profile.missionId()) ? "unlocked" : "locked"),
                    rewardLines(profile),
                    List.of("Entrance: " + profile.surfaceEntrance(),
                            "Arena: " + profile.undergroundSite(),
                            "Faction thread: " + AshfallFactionMap.displayName(profile.ownerFaction()),
                            "Arena feature: " + polish.arenaSetPiece(),
                            "Counterplay: " + polish.counterplayObject(),
                            "Add pressure: " + polish.addPressurePattern(),
                            "Reward category: " + polish.rewardCategory(),
                            "HUD cue: " + cue.phaseWarningLabel() + " / " + cue.counterplayLabel()),
                    "Guardian mechanics are classified until ECHO identifies the route.",
                    "Progress missions until ECHO marks the " + profile.surfaceEntrance() + ".");
        }

        private void addOrbitalEntries(List<CodexEntry> entries, CodexState state) {
            entries.add(entry(state, "orbital_route", CodexCategory.ORBITAL, CodexEntryType.ROUTE,
                    "Orbital Route Systems", "ADDON ROUTE", CodexUnlockRule.POST_NEXUS_CHOICE, "",
                    "Orbital Remnants opens a post-Nexus route chain through launch, Station ECHO, route worlds, and deep space. The fall began above you; the return route does not forgive that.",
                    "Use the ORBITAL tab for live scan actions and this Codex page for reference.",
                    "Finish the Ashfall Nexus choice before expecting orbital calibration.",
                    orbitalProgressLine(state),
                    List.of("Launch readiness", "Route vessels", "Return vectors", "Nexus Anomaly Belt"),
                    List.of("The standalone Orbital terminal remains unchanged."),
                    "Orbital records are locked.", "Make an Ashfall Nexus choice to unlock orbital calibration."));
            entries.add(entry(state, "orbital_suit", CodexCategory.ORBITAL, CodexEntryType.PROTOCOL,
                    "Suit Systems", "LIFE SUPPORT", CodexUnlockRule.POST_NEXUS_CHOICE, "",
                    "Major orbital encounters pressure oxygen, pressure seals, radiation, gravity, and thermal systems. In orbit, survival is not a health bar; it is a checklist with teeth.",
                    "Treat suit health as encounter-critical, not travel flavor.",
                    "Carry oxygen cells, sealant patches, thermal support, and route-specific parts.",
                    orbitalProgressLine(state),
                    List.of("Oxygen", "Pressure", "Radiation", "Thermal recovery"),
                    List.of("Encounter HUD subtitles name the suit system under pressure."),
                    "Suit telemetry is locked.", "Calibrate orbital contact or open the Orbital tab after Nexus."));
            entries.add(orbitalBossEntry(state, "orbital_docking_ai", "Corrupted Docking AI", "Docking AI",
                    "Airlock pressure and reserve drones turn Station ECHO safety systems hostile. The station is still trying to protect itself from the living.",
                    "Keep pressure sealed and clear emergency-port drones before the lock cycle stacks.",
                    "station_coordinates", "Navigation chip and station cache authority."));
            entries.add(orbitalBossEntry(state, "orbital_captain", "The Abandoned Captain", "Captain",
                    "A failed command loop drains oxygen and wakes broken crew telemetry. Command survived here as a distress signal with a weapon in its hand.",
                    "Protect oxygen, clear reinforcements, and use the fight to read station lore.",
                    "station_life_support", "Martian silica and Mars transfer records."));
            entries.add(orbitalBossEntry(state, "orbital_cryo_warden", "Europa Cryo Warden", "Cryo Warden",
                    "Europa vent pulses punish players away from thermal arrays. The moon keeps its dead cold and its warnings colder.",
                    "Fight near thermal cover and recover pressure during vent windows.",
                    "europa_cryo_ocean", "Thermal stabilizer and Europa probe array."));
            entries.add(orbitalBossEntry(state, "orbital_echo_zero", "ECHO-0", "ECHO-0",
                    "The final orbital quarantine fight attacks oxygen, pressure, radiation, and Nexus stabilization. ECHO-0 is not malfunctioning; it is obeying the worst order it ever received.",
                    "Stabilize suit systems, survive quarantine pulses, and finish the post-ECHO-0 network.",
                    "nexus_anomaly_belt", "Nexus Drive Core and faction-sensitive final rewards."));
        }

        private CodexEntry orbitalBossEntry(CodexState state, String id, String title, String label,
                String description, String prep, String milestone, String rewards) {
            return entry(state, id, CodexCategory.ORBITAL, CodexEntryType.BOSS,
                    title, "ORBITAL THREAT", CodexUnlockRule.ORBITAL_INTEL, milestone,
                    description,
                    "The encounter HUD tracks this threat as " + label + " during live combat.",
                    prep,
                    orbitalProgressLine(state),
                    List.of(rewards, "Orbital Black Box archive proof."),
                    List.of("Orbital encounter rewards and progress flags remain owned by the addon."),
                    "Orbital encounter records are locked.", "Advance Orbital Remnants until this route milestone is mirrored.");
        }

        private CodexEntry entry(CodexState state, String id, CodexCategory category, CodexEntryType type,
                String title, String status, CodexUnlockRule unlockRule, String unlockKey,
                String description, String practicalUse, String prep, String progress,
                List<String> rewards, List<String> notes, String lockReason, String nextHint) {
            boolean unlocked = unlockRule.unlocked(state, unlockKey);
            return new CodexEntry(id, category, type, title, status, unlocked, description, practicalUse,
                    prep, progress, rewards, notes, lockReason, nextHint);
        }

        private static List<String> rewardLines(BiomeGuardianProfile profile) {
            return profile.rewardBundle().entries().stream()
                    .map(entry -> {
                        ItemStack stack = entry.stack(RandomSource.create(1L));
                        String count = stack.getCount() > 1 ? stack.getCount() + "x " : "";
                        return count + stack.getHoverName().getString();
                    })
                    .toList();
        }

        private static int guardianKillCount(QuestData quest) {
            int count = 0;
            for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
                if (quest.getEntityKills(profile.entityId()) > 0 || quest.isMissionCompleted(profile.missionId())) {
                    count++;
                }
            }
            return count;
        }

        private static int unlockedGuardianMissionCount(QuestData quest) {
            int count = 0;
            for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
                if (quest.isMissionUnlocked(profile.missionId())) {
                    count++;
                }
            }
            return count;
        }

        private static int countContains(Set<String> values, String needle) {
            int count = 0;
            String normalized = needle.toLowerCase(Locale.ROOT);
            for (String value : values) {
                if (value != null && value.toLowerCase(Locale.ROOT).contains(normalized)) {
                    count++;
                }
            }
            return count;
        }

        private static boolean hasAnyGuardianMission(QuestData quest) {
            return unlockedGuardianMissionCount(quest) > 0 || guardianKillCount(quest) > 0;
        }

        private static boolean allGuardiansResolved(QuestData quest) {
            return guardianKillCount(quest) >= BiomeGuardianProfiles.all().size();
        }

        private static boolean hasOrbitalIntel(CodexState state, String milestone) {
            return state.intel().hasDiscoveredLore("echoorbitalremnants_orbital_" + milestone)
                    || state.quest().getArchive().stream().anyMatch(line -> line != null
                    && line.toLowerCase(Locale.ROOT).contains(milestone.toLowerCase(Locale.ROOT).replace('_', ' ')));
        }

        private static String orbitalProgressLine(CodexState state) {
            if (hasOrbitalIntel(state, "orbital_remnants_complete")) {
                return "Orbital Remnants arc complete.";
            }
            if (hasOrbitalIntel(state, "echo_zero_resolved")) {
                return "ECHO-0 resolved; Nexus stabilization available.";
            }
            if (hasOrbitalIntel(state, "nexus_anomaly_belt")) {
                return "Nexus Anomaly Belt reached.";
            }
            if (state.post().hasMadeChoice()) {
                return "Nexus choice confirmed; orbital calibration may be available.";
            }
            return "Locked until an Ashfall Nexus choice or mirrored orbital intel exists.";
        }

        private static int typeColor(CodexEntryType type) {
            return switch (type) {
                case BOSS -> TerminalUi.RED;
                case HAZARD -> TerminalUi.AMBER;
                case MACHINE -> TerminalUi.CYAN;
                case ITEM -> TerminalUi.GREEN;
                case ROUTE, SITE -> 0xFFB7D8FF;
                case LORE -> 0xFFE0B6FF;
                case PROTOCOL -> TerminalUi.TEXT;
            };
        }

        private enum CodexCategory {
            START("STARTUP", "Crash survival, mission flow, and baseline ECHO protocol."),
            ITEMS("SALVAGE", "Materials, medicine, schematics, and route-defining rewards."),
            MACHINES("MACHINES", "Power, research, fabrication, and grid infrastructure."),
            HAZARDS("HAZARDS", "Environmental pressure and the counterplay that keeps routes survivable."),
            EXPLORATION("FIELD ROUTES", "POIs, entrances, route records, and field mapping."),
            GUARDIANS("GUARDIANS", "Biome guardian dossiers generated from threat profiles."),
            NEXUS("NEXUS", "Core choice, Archive Warden, and endgame protocol state."),
            ORBITAL("ECHO-0", "Mirrored addon route systems, suit pressure, and orbital threat intel.");

            private final String label;
            private final String summary;

            CodexCategory(String label, String summary) {
                this.label = label;
                this.summary = summary;
            }

            String label() {
                return label;
            }

            String summary() {
                return summary;
            }
        }

        private enum CodexEntryType {
            PROTOCOL("PROTOCOL"),
            ITEM("ITEM"),
            MACHINE("MACHINE"),
            HAZARD("HAZARD"),
            SITE("SITE"),
            BOSS("THREAT"),
            ROUTE("ROUTE"),
            LORE("LORE");

            private final String label;

            CodexEntryType(String label) {
                this.label = label;
            }

            String label() {
                return label;
            }
        }

        private enum CodexUnlockRule {
            ALWAYS {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return true;
                }
            },
            PROGRESS_PHASE {
                @Override
                boolean unlocked(CodexState state, String key) {
                    try {
                        return state.quest().getCurrentPhase() >= Integer.parseInt(key);
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                }
            },
            VISITED_BIOME {
                @Override
                boolean unlocked(CodexState state, String key) {
                    String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
                    return state.quest().getVisitedBiomes().stream().anyMatch(biome -> biome.toLowerCase(Locale.ROOT).contains(normalized))
                            || state.quest().getLastBiomeId().toLowerCase(Locale.ROOT).contains(normalized);
                }
            },
            MISSION_UNLOCKED {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return state.quest().isMissionUnlocked(key) || state.quest().isMissionCompleted(key);
                }
            },
            ANY_GUARDIAN {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return guardianKillCount(state.quest()) > 0;
                }
            },
            ANY_GUARDIAN_MISSION {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return hasAnyGuardianMission(state.quest());
                }
            },
            ALL_GUARDIANS {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return allGuardiansResolved(state.quest());
                }
            },
            POST_NEXUS_CHOICE {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return state.post().hasMadeChoice();
                }
            },
            ORBITAL_INTEL {
                @Override
                boolean unlocked(CodexState state, String key) {
                    return hasOrbitalIntel(state, key);
                }
            };

            abstract boolean unlocked(CodexState state, String key);
        }

        private record CodexState(Player player, QuestData quest, EchoIntel intel,
                                  ResearchData research, PostNexusData post) {
        }

        private record CodexEntry(
                String id,
                CodexCategory category,
                CodexEntryType type,
                String title,
                String status,
                boolean unlocked,
                String description,
                String practicalUse,
                String prep,
                String progress,
                List<String> rewards,
                List<String> notes,
                String lockReason,
                String nextHint
        ) {
        }

        private int codexDetailHeight(TerminalRenderContext context, int width) {
            return context.contentHeight();
        }

        private void renderNotes(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, EchoIntel intel, int x, int y, int w) {
            line(context, graphics, "Archive notes: " + quest.getArchive().size()
                    + " / Intel records: " + intel.getAllIntel().size()
                    + " / Unread: " + intel.getUnreadCount(), x, y, w, TerminalUi.TEXT);
            MissionUxSummary summary = MissionUxSummary.current(context.player(), quest);
            int cy = y + 22;
            line(context, graphics, "Active route: " + summary.shortTitle(), x, cy, w,
                    summaryColor(summary));
            cy = wrap(context, graphics, "Next: " + summary.nextStep(), x, cy + 14, w,
                    summaryColor(summary)) + 6;
            if (!summary.relatedIntelKey().isBlank()) {
                line(context, graphics, "Relevant protocol: " + summary.relatedIntelKey(),
                        x, cy, w, TerminalUi.MUTED);
                cy += 16;
            }
            List<String> notes = recentUniqueNotes(quest);
            if (notes.isEmpty()) {
                line(context, graphics, "No ECHO archive notes are unlocked for this category yet.", x, cy, w,
                        TerminalUi.MUTED);
                return;
            }
            for (String note : notes) {
                int color = note.startsWith("[GOAL]") ? TerminalUi.CYAN : TerminalUi.TEXT;
                cy = wrap(context, graphics, "- " + compactNote(note), x, cy, w, color) + 6;
            }
        }

        private void renderDossiers(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                EchoIntel intel, int x, int y, int w) {
            line(context, graphics, "Records: " + intel.getAllIntel().size()
                    + " / unread " + intel.getUnreadCount(), x, y, w,
                    intel.getUnreadCount() > 0 ? TerminalUi.AMBER : TerminalUi.GREEN);
            int cy = y + 24;
            for (Identifier faction : AshfallFactionMap.all()) {
                int complete = intel.getDossierCompletion(faction);
                String name = EchoCoreServices.factionDefinition(faction)
                        .map(definition -> definition.shortName())
                        .orElse(AshfallFactionMap.displayName(faction));
                line(context, graphics, name + " dossier " + complete + "%", x, cy, w, TerminalUi.TEXT);
                TerminalUi.progress(graphics, x + Math.min(150, w / 2), cy + 2,
                        Math.max(80, w - Math.min(155, w / 2 + 5)), 8, complete / 100.0F, TerminalUi.CYAN);
                cy += 16;
            }
        }

        private void renderSystems(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, ResearchData research, int x, int y, int w) {
            int cy;
            if (w < 330) {
                int cardW = Math.max(120, w - 8);
                cy = y;
                cy += overviewCard(context, graphics, x, cy, cardW, "TERMINAL",
                        quest.getTerminalHealth() + "%", quest.isTerminalOnline() ? "Link online" : "Link degraded",
                        quest.isTerminalOnline() ? TerminalUi.GREEN : TerminalUi.RED) + 8;
                cy += overviewCard(context, graphics, x, cy, cardW, "DRONE",
                        quest.getDroneHealth() + "%", quest.getDroneStage().name(), TerminalUi.AMBER) + 8;
                cy += overviewCard(context, graphics, x, cy, cardW, "RESEARCH",
                        research.getPoints() + "/" + ResearchData.MAX_POINTS, "Tier " + research.getCurrentTier(),
                        TerminalUi.CYAN) + 14;
            } else {
                int cardW = Math.max(96, Math.min(160, (w - 16) / 3));
                int cardH = 0;
                cardH = Math.max(cardH, overviewCard(context, graphics, x, y, cardW, "TERMINAL",
                        quest.getTerminalHealth() + "%", quest.isTerminalOnline() ? "Link online" : "Link degraded",
                        quest.isTerminalOnline() ? TerminalUi.GREEN : TerminalUi.RED));
                cardH = Math.max(cardH, overviewCard(context, graphics, x + cardW + 8, y, cardW, "DRONE",
                        quest.getDroneHealth() + "%", quest.getDroneStage().name(), TerminalUi.AMBER));
                cardH = Math.max(cardH, overviewCard(context, graphics, x + (cardW + 8) * 2, y, cardW, "RESEARCH",
                        research.getPoints() + "/" + ResearchData.MAX_POINTS, "Tier " + research.getCurrentTier(),
                        TerminalUi.CYAN));
                cy = y + cardH + 14;
            }
            line(context, graphics, "Mission registry: " + quest.getCompletedMissionIds().size()
                    + "/" + MissionRegistry.getAllMissions().size() + " complete", x, cy, w, TerminalUi.TEXT);
            line(context, graphics, "Pending rewards: " + quest.getAllPendingRewards().size(), x, cy + 14, w,
                    quest.getAllPendingRewards().isEmpty() ? TerminalUi.MUTED : TerminalUi.AMBER);
            line(context, graphics, "Schemas: " + research.getUnlockedSchematics().size()
                    + " / perks " + research.getUnlockedPerks().size(), x, cy + 28, w, TerminalUi.TEXT);
        }

        private void renderFactions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                EchoIntel intel, int x, int y, int w) {
            String insight = intel.synthesizeInsight();
            int cy = y;
            if (insight != null && !insight.isBlank()) {
                cy = wrap(context, graphics, insight, x, cy, w, TerminalUi.AMBER) + 8;
            } else {
                line(context, graphics, "Pattern analysis: no elevated faction pattern detected.", x, cy, w, TerminalUi.MUTED);
                cy += 22;
            }
            for (EchoFactionProfile profile : ashfallFactionProfiles(context.player())) {
                int color = profile.contacted() ? profile.definition().accentColor() : TerminalUi.MUTED;
                line(context, graphics, profile.definition().displayName() + ": " + profile.reputation()
                        + " / " + profile.standing().displayName()
                        + " / contracts " + profile.completedContracts(),
                        x, cy, w, color);
                cy += 14;
            }
        }

        private static int overviewCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, String label, String value, String detail, int color) {
            return TerminalUi.statusCard(context, graphics, x, y, width, label, value, detail, color);
        }

        private static int systemsHeight(TerminalRenderContext context, QuestData quest, ResearchData research, int width) {
            int terminalH = TerminalUi.cardHeight(context,
                    quest.isTerminalOnline() ? "Link online" : "Link degraded", Math.max(96, Math.min(160, (width - 16) / 3)));
            int droneH = TerminalUi.cardHeight(context, quest.getDroneStage().name(),
                    Math.max(96, Math.min(160, (width - 16) / 3)));
            int researchH = TerminalUi.cardHeight(context, "Tier " + research.getCurrentTier(),
                    Math.max(96, Math.min(160, (width - 16) / 3)));
            if (width < 330) {
                int cardW = Math.max(120, width - 8);
                return TerminalUi.cardHeight(context,
                        quest.isTerminalOnline() ? "Link online" : "Link degraded", cardW)
                        + TerminalUi.cardHeight(context, quest.getDroneStage().name(), cardW)
                        + TerminalUi.cardHeight(context, "Tier " + research.getCurrentTier(), cardW)
                        + 8 + 8 + 14 + 42;
            }
            return Math.max(terminalH, Math.max(droneH, researchH)) + 14 + 42;
        }

        private static List<String> recentUniqueNotes(QuestData quest) {
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String note : quest.getArchive()) {
                if (note == null || note.isBlank()) {
                    continue;
                }
                unique.add(note.trim());
                if (unique.size() >= 8) {
                    break;
                }
            }
            return List.copyOf(unique);
        }

        private static String compactNote(String note) {
            return note.replace("Secure Crash Outpost | Open Terminal -> [TURN IN]",
                    "Secure Crash Outpost: terminal turn-in ready");
        }
    }

    private static final class WorldTab extends AshfallTab {
        private static final List<String> SECTIONS = List.of(
                "Current Fix", "Route Marks", "POI Signals", "POI Atlas", "Nexus Grid");
        private String selectedSection = "Current Fix";

        private WorldTab() {
            super(WORLD, "ROUTE MAP", 130, 0xFFFF8FA3,
                    TerminalTabChrome.of("Route Map", TerminalTabChrome.GROUP_FIELD, "RM",
                            "Routes, POIs, signal map", 130));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            QuestData quest = QuestData.get(context.player());
            PostNexusData post = PostNexusData.get(context.player());
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            boolean wide = w >= 560;
            int navW = wide ? 150 : w;
            int detailX = wide ? x + navW + 12 : x;
            int navPanelH = 38 + SECTIONS.size() * 26;
            int detailY = wide ? y : y + navPanelH + 12;
            int detailW = wide ? w - navW - 18 : w;

            if (!SECTIONS.contains(selectedSection)) {
                selectedSection = SECTIONS.get(0);
            }
            int sy = flatDataPanel(context, graphics, x, y, navW - 4, navPanelH, "ROUTE MAP", "");
            sy += 3;
            for (String section : SECTIONS) {
                boolean selected = section.equals(selectedSection);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, x + 8, sy, navW - 20, 20);
                dataRow(context, graphics, x + 8, sy, navW - 20, 20, section, "", "",
                        selected, hovered, selected ? descriptor().accentColor() : TerminalUi.MUTED);
                addHitbox(x + 8, sy, navW - 20, 20, () -> selectedSection = section);
                sy += 26;
            }

            String detailTitle = switch (selectedSection) {
                case "Route Marks" -> "ROUTE MARKERS";
                case "POI Signals" -> "POI SIGNAL STATES";
                case "POI Atlas" -> "POI FIELD ATLAS";
                case "Nexus Grid" -> "NEXUS ROUTE STATE";
                default -> "CURRENT ROUTE FIX";
            };
            int detailPanelY = detailY;
            int measuredDetailH = Math.max(138, worldDetailHeight(context, quest, detailW - 84) + 78);
            boolean atlas = "POI Atlas".equals(selectedSection);
            int detailPanelH = wide && !atlas ? Math.max(220, h - (detailY - y) - 6) : measuredDetailH;
            TerminalUi.hdBackplatePanel(graphics, TerminalVisualAssets.CARD_PANEL_ROUTE_MAP,
                    detailX, detailPanelY, detailW, detailPanelH, descriptor().accentColor(), 0.76F,
                    TerminalUi.ImageFit.COVER);
            TerminalUi.sectionHeader(context, graphics, detailTitle, "", detailX + 14, detailPanelY + 14,
                    detailW - 28, descriptor().accentColor());
            TerminalUi.hybridIconBadge(graphics, TerminalVisualAssets.ICON_PAGE_ROUTE_MAP, TerminalIcon.WORLD,
                    detailX + 16, detailPanelY + 36, 42, descriptor().accentColor(), true);
            BlockPos pos = context.player().blockPosition();
            switch (selectedSection) {
                case "Route Marks" -> renderRouteMarkers(context, graphics, quest, detailX + 72, detailPanelY + 38, detailW - 92);
                case "POI Signals" -> renderPoiStates(context, graphics, quest, detailX + 72, detailPanelY + 38, detailW - 92);
                case "POI Atlas" -> renderPoiAtlas(context, graphics, quest, detailX + 72, detailPanelY + 38, detailW - 92);
                case "Nexus Grid" -> renderWorldNexus(context, graphics, quest, post, detailX + 72, detailPanelY + 38, detailW - 92);
                default -> renderLocation(context, graphics, quest, pos, detailX + 72, detailPanelY + 38, detailW - 92);
            }
            boolean summarySafe = switch (selectedSection) {
                case "Route Marks" -> quest.getVisitedSpecialLocations().size() <= 4;
                case "POI Signals" -> quest.getPOIObjectiveStates().size() <= 4;
                case "POI Atlas" -> false;
                default -> true;
            };
            if (wide && summarySafe && detailPanelH >= 310) {
                renderWorldSummaryTiles(context, graphics, quest, post,
                        detailX + 18, detailPanelY + detailPanelH - 82, detailW - 36);
            }
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            int w = context.contentWidth();
            boolean wide = w >= 560;
            int navW = wide ? 150 : w;
            int detailW = wide ? w - navW - 18 : w;
            int navH = 38 + SECTIONS.size() * 26;
            int detailH = Math.max(138, worldDetailHeight(context, QuestData.get(context.player()), detailW - 84) + 78);
            if (wide && "POI Atlas".equals(selectedSection)) {
                return Math.max(context.contentHeight(), Math.max(navH, detailH));
            }
            return Math.max(context.contentHeight(), wide ? Math.max(navH, context.contentHeight()) : navH + 12 + detailH);
        }

        private void renderLocation(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, BlockPos pos, int x, int y, int w) {
            line(context, graphics, "Dimension: " + context.player().level().dimension().identifier(), x, y, w, TerminalUi.TEXT);
            line(context, graphics, "Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), x, y + 14, w, TerminalUi.TEXT);
            line(context, graphics, "Last biome: " + (quest.getLastBiomeId().isBlank() ? "unknown" : quest.getLastBiomeId()),
                    x, y + 28, w, TerminalUi.MUTED);
            line(context, graphics, "Visited biomes: " + quest.getVisitedBiomes().size()
                    + " / dimensions: " + quest.getVisitedDimensions().size(), x, y + 42, w, TerminalUi.TEXT);
            line(context, graphics, "Discovered POIs: " + quest.getDiscoveredPOICount()
                    + " / Power nodes: " + quest.getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES,
                    x, y + 56, w, TerminalUi.AMBER);
            line(context, graphics, "POI Atlas: " + ExplorationPoiCatalog.totalTemplateCount()
                    + " template signals grouped by scanner profile", x, y + 70, w, TerminalUi.MUTED);
        }

        private void renderWorldSummaryTiles(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, PostNexusData post, int x, int y, int w) {
            int gap = 8;
            int tileW = Math.max(96, (w - gap * 2) / 3);
            TerminalUi.denseDataCard(context, graphics, x, y, tileW, "ROUTE MEMORY",
                    quest.getVisitedBiomes().size() + " biomes",
                    quest.getVisitedDimensions().size() + " dimensions tracked", descriptor().accentColor());
            TerminalUi.denseDataCard(context, graphics, x + tileW + gap, y, tileW, "SIGNALS",
                    quest.getDiscoveredPOICount() + " POIs",
                    quest.getPOIObjectiveStates().size() + " objective states", TerminalUi.AMBER);
            TerminalUi.denseDataCard(context, graphics, x + (tileW + gap) * 2, y,
                    Math.max(96, w - (tileW + gap) * 2), "NEXUS GRID",
                    quest.getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES + " nodes",
                    post.hasMadeChoice() ? post.getSelectedPath().name() + " path selected" : "path unresolved",
                    post.hasMadeChoice() ? TerminalUi.GREEN : TerminalUi.MUTED);
        }

        private void renderRouteMarkers(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, int x, int y, int w) {
            List<String> markers = quest.getVisitedSpecialLocations().stream().sorted().toList();
            if (markers.isEmpty()) {
                line(context, graphics, "No special route markers logged.", x, y, w, TerminalUi.MUTED);
                return;
            }
            int cy = y;
            for (String marker : markers) {
                cy = wrap(context, graphics, "- " + marker, x, cy, w, TerminalUi.TEXT) + 3;
            }
        }

        private void renderPoiStates(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, int x, int y, int w) {
            List<String> poiStates = quest.getPOIObjectiveStates().stream().sorted().toList();
            if (poiStates.isEmpty()) {
                line(context, graphics, "No POI objective states recorded.", x, y, w, TerminalUi.MUTED);
                return;
            }
            int cy = y;
            for (String stateLine : poiStates) {
                cy = wrap(context, graphics, "- " + stateLine, x, cy, w, TerminalUi.TEXT) + 3;
            }
        }

        private void renderPoiAtlas(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, int x, int y, int w) {
            List<ExplorationSiteRegistry.SiteProfile> profiles = catalogProfiles();
            int discovered = 0;
            for (ExplorationSiteRegistry.SiteProfile profile : profiles) {
                if (quest.isPOIDiscovered(profile.id())) {
                    discovered++;
                }
            }
            int cy = y;
            line(context, graphics, discovered + "/" + profiles.size() + " route profiles catalogued | "
                    + ExplorationPoiCatalog.totalTemplateCount() + " template signals indexed",
                    x, cy, w, TerminalUi.AMBER);
            cy += 18;
            for (ExplorationSiteRegistry.SiteProfile profile : profiles) {
                List<ExplorationPoiCatalog.Entry> entries = ExplorationPoiCatalog.forProfile(profile.id());
                boolean profileDiscovered = quest.isPOIDiscovered(profile.id());
                String status = profileDiscovered ? quest.getPOIStateSummary(profile.id()) : "UNSCANNED";
                int profileColor = profileDiscovered ? profile.dangerLevel().getColor() : TerminalUi.MUTED;
                cy = wrap(context, graphics, profile.displayName() + " | " + status + " | "
                        + entries.size() + " templates", x, cy, w, profileColor) + 2;
                cy = wrap(context, graphics, profile.route() + " | " + profile.hazardName()
                        + " | Prep: " + profile.prepHint(), x + 6, cy, Math.max(60, w - 6),
                        profileDiscovered ? TerminalUi.TEXT : TerminalUi.MUTED) + 2;
                for (ExplorationPoiCatalog.Entry entry : entries) {
                    int rowColor = profileDiscovered ? TerminalUi.TEXT : TerminalUi.MUTED;
                    String marker = entry.landmark() ? "landmark" : entry.faction() ? "faction" : entry.category();
                    cy = wrap(context, graphics, "- " + entry.displayName() + " [" + marker + "] "
                            + entry.poolSummary(), x + 12, cy, Math.max(60, w - 12), rowColor) + 2;
                }
                cy += 5;
            }
        }

        private void renderWorldNexus(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                QuestData quest, PostNexusData post, int x, int y, int w) {
            NexusWorldData.WorldState state = HudState.getNexusState();
            line(context, graphics, "World state: " + NexusAccessRules.stateLabel(state), x, y, w,
                    state == NexusWorldData.WorldState.NORMAL ? TerminalUi.MUTED : TerminalUi.GREEN);
            line(context, graphics, "Player path: " + post.getSelectedPath().name(), x, y + 14, w,
                    post.hasMadeChoice() ? TerminalUi.GREEN : TerminalUi.AMBER);
            line(context, graphics, "Power nodes: " + quest.getCollectedPowerNodes() + "/" + NexusCoreBlock.REQUIRED_NODES,
                    x, y + 28, w, quest.getCollectedPowerNodes() >= NexusCoreBlock.REQUIRED_NODES ? TerminalUi.GREEN : TerminalUi.AMBER);
            line(context, graphics, "Guardian routes are listed in NEXUS when unresolved.", x, y + 42, w, TerminalUi.MUTED);
        }

        private int worldDetailHeight(TerminalRenderContext context, QuestData quest, int width) {
            return switch (selectedSection) {
                case "Route Marks" -> measuredRows(context, quest.getVisitedSpecialLocations().stream().sorted().toList(), width);
                case "POI Signals" -> measuredRows(context, quest.getPOIObjectiveStates().stream().sorted().toList(), width);
                case "POI Atlas" -> poiAtlasHeight(context, quest, width);
                case "Nexus Grid" -> 56;
                default -> 84;
            };
        }

        private static int measuredRows(TerminalRenderContext context, List<String> rows, int width) {
            if (rows.isEmpty()) {
                return 12;
            }
            int height = 0;
            for (String row : rows) {
                height += TerminalUi.wrappedHeight(context, "- " + row, width) + 3;
            }
            return height;
        }

        private static List<ExplorationSiteRegistry.SiteProfile> catalogProfiles() {
            return ExplorationSiteRegistry.allSorted().stream()
                    .toList();
        }

        private static int poiAtlasHeight(TerminalRenderContext context, QuestData quest, int width) {
            int safeWidth = Math.max(80, width);
            int height = 18;
            for (ExplorationSiteRegistry.SiteProfile profile : catalogProfiles()) {
                List<ExplorationPoiCatalog.Entry> entries = ExplorationPoiCatalog.forProfile(profile.id());
                height += TerminalUi.wrappedHeight(context, profile.displayName() + " | "
                        + (quest.isPOIDiscovered(profile.id()) ? quest.getPOIStateSummary(profile.id()) : "UNSCANNED")
                        + " | " + entries.size() + " templates", safeWidth) + 2;
                height += TerminalUi.wrappedHeight(context, profile.route() + " | " + profile.hazardName()
                        + " | Prep: " + profile.prepHint(), Math.max(60, safeWidth - 6)) + 2;
                for (ExplorationPoiCatalog.Entry entry : entries) {
                    height += TerminalUi.wrappedHeight(context, "- " + entry.displayName() + " ["
                            + entry.markerLabel() + "] " + entry.poolSummary(), Math.max(60, safeWidth - 12)) + 2;
                }
                height += 5;
            }
            return height;
        }
    }

    private static final class NexusTab extends AshfallTab {
        private static final int CORE_SCAN_HORIZONTAL = 16;
        private static final int CORE_SCAN_VERTICAL = 8;
        private static final long CONFIRM_TIMEOUT_TICKS = 200L;
        private static long lastScanTick = Long.MIN_VALUE;
        private static ClientCoreStatus lastCoreStatus = new ClientCoreStatus(false, 0);
        private static String pendingChoicePayload = "";
        private static long pendingChoiceTick = Long.MIN_VALUE;

        private NexusTab() {
            super(NEXUS, "NEXUS CORE", 220, 0xFFC77DFF,
                    TerminalTabChrome.of("Nexus Core", TerminalTabChrome.GROUP_NEXUS, "NX", "Final path control", 220));
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            beginHitboxes();
            Player player = context.player();
            QuestData quest = QuestData.get(player);
            PostNexusData post = PostNexusData.get(player);
            NexusWorldData.WorldState state = clientWorldState(post);
            ClientCoreStatus core = scanNearbyCore(player);
            ClientGuardianStatus guardians = guardianStatus(quest);
            boolean sealed = state != NexusWorldData.WorldState.NORMAL;
            boolean ready = choiceReady(player, quest, post, core, guardians);
            if (!ready || sealed || pendingChoiceExpired(player)) {
                clearPendingChoice();
            }
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int h = context.contentHeight();
            boolean wide = w >= 760;
            int gap = 14;
            int commandH = wide ? 58 : 60;
            int topH = wide ? Math.max(286, h - commandH - gap) : 332;
            if (wide && topH + commandH + gap > h) {
                topH = Math.max(220, h - commandH - gap);
            }
            int panelW = wide ? Math.max(430, Math.min(620, w * 58 / 100)) : w;
            int briefX = wide ? x + panelW + gap : x;
            int briefY = wide ? y : y + topH + gap;
            int briefW = wide ? Math.max(250, w - panelW - gap) : w;

            int corePanelH = wide ? Math.min(238, Math.max(196, topH * 58 / 100)) : 214;
            int summaryY = y + corePanelH + gap;
            int summaryH = Math.max(74, topH - corePanelH - gap);
            texturedPanel(graphics, TerminalVisualAssets.CARD_PANEL_NEXUS_PATH, x, y, panelW, corePanelH, 0.68F);
            TerminalUi.hybridIconBadge(graphics, TerminalVisualAssets.ICON_PAGE_NEXUS_CORE, TerminalIcon.NEXUS,
                    x + 18, y + 28, 44, descriptor().accentColor(), true);
            TerminalUi.line(context, graphics, "NEXUS CORE INTERFACE", x + 74, y + 24, panelW - 100, descriptor().accentColor());
            TerminalUi.divider(graphics, x + 74, y + 42, Math.max(80, panelW - 108), descriptor().accentColor());
            int cy = y + 76;
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Core range",
                    core.nearbyCore(), core.nearbyCore() ? "Nearby" : "Stand near unresolved Nexus Core");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Power nodes",
                    core.nearbyCore() && core.activatedNodes() >= NexusCoreBlock.REQUIRED_NODES,
                    core.nearbyCore()
                            ? core.activatedNodes() + "/" + NexusCoreBlock.REQUIRED_NODES + " active"
                            : "--/" + NexusCoreBlock.REQUIRED_NODES + " active");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Guardians",
                    guardians.missingCount() == 0,
                    guardians.resolvedCount() + "/" + guardians.totalCount() + " resolved");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Core status",
                    HudState.isNexusCampaignAwakened(),
                    HudState.isNexusCampaignAwakened()
                            ? "Instability " + HudState.getNexusInstability() + "%"
                            : "Dormant");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Prime relays",
                    HudState.getNexusRelaysResolved() >= 3,
                    HudState.getNexusRelaysScanned() + "/6 scanned, "
                            + HudState.getNexusRelaysResolved() + "/3 resolved");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Path readiness",
                    HudState.getNexusRelaysResolved() >= 3,
                    "R" + HudState.getNexusReadinessRestore()
                            + " / D" + HudState.getNexusReadinessDestroy()
                            + " / C" + HudState.getNexusReadinessControl());
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "Siege status",
                    HudState.isNexusSiegeComplete(),
                    HudState.isNexusSiegeComplete() ? "Countermeasure survived" : "Core siege pending");
            cy = TerminalUi.checklistRow(context, graphics, x + 18, cy, panelW - 36, "World seal",
                    state == NexusWorldData.WorldState.NORMAL, sealed ? NexusAccessRules.stateLabel(state) : "Ready for first commitment");

            int summaryTextY = TerminalUi.flatDataPanel(context, graphics, x, summaryY, panelW, summaryH,
                    sealed ? "SEALED PATH" : "PROGRESS SUMMARY", "",
                    ready ? TerminalUi.GREEN : descriptor().accentColor());
            summaryTextY += 4;
            if (sealed) {
                String chooser = HudState.getNexusPlayer();
                wrap(context, graphics,
                        NexusAccessRules.stateLabel(state)
                                + (chooser == null || chooser.isBlank() ? "" : " / " + chooser)
                                + ". Route systems and addon chapters can now read this sealed path.",
                        x + 18, summaryTextY, panelW - 36, TerminalUi.GREEN);
            } else {
                String pendingChoice = pendingChoiceLabel(player);
                wrap(context, graphics, ready
                                ? (pendingChoice.isBlank()
                                        ? "Status: READY. Select a path once, then confirm; this choice is permanent."
                                        : "Confirm " + pendingChoice + " to seal this path. No second prompt follows.")
                                : choiceBlockedReason(state, post, core, guardians),
                        x + 18, summaryTextY, panelW - 36, ready ? TerminalUi.GREEN : TerminalUi.AMBER);
                drawWarfrontCommands(context, graphics, x + 14, summaryY + summaryH - 48, panelW - 28,
                        sealed, post, mouseX, mouseY);
            }

            drawPathBrief(context, graphics, briefX, briefY, briefW, topH, state, ready, core, guardians);

            int buttonY = wide ? y + topH + gap : briefY + topH + gap;
            int buttonW = Math.max(120, (w - gap * 2) / 3);
            drawNexusCommand(context, graphics, x, buttonY, buttonW, commandH, "RESTORE GRID", "restore", ready, mouseX, mouseY);
            drawNexusCommand(context, graphics, x + buttonW + gap, buttonY, buttonW, commandH, "DESTROY CORE", "destroy", ready, mouseX, mouseY);
            drawNexusCommand(context, graphics, x + (buttonW + gap) * 2, buttonY,
                    Math.max(120, w - (buttonW + gap) * 2), commandH, "CONTROL SIGNAL", "control", ready, mouseX, mouseY);
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            Player player = context.player();
            QuestData quest = QuestData.get(player);
            PostNexusData post = PostNexusData.get(player);
            NexusWorldData.WorldState state = clientWorldState(post);
            ClientCoreStatus core = scanNearbyCore(player);
            ClientGuardianStatus guardians = guardianStatus(quest);
            boolean wide = context.contentWidth() >= 760;
            return Math.max(context.contentHeight(), wide ? 326 : 704);
        }

        private static void drawPathBrief(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, int height, NexusWorldData.WorldState state, boolean ready,
                ClientCoreStatus core, ClientGuardianStatus guardians) {
            int cy = TerminalUi.flatDataPanel(context, graphics,
                    x, y, width, height, "PATH BRIEF", "", 0xFFC77DFF);
            cy += 7;
            List<String> relayLines = HudState.getNexusRelaySummaryLines();
            int rowH = relayLines.isEmpty()
                    ? Math.max(46, (height - (cy - y) - 24) / 3)
                    : Math.max(36, (height - (cy - y) - 88) / 3);
            pathRow(context, graphics, x + 18, cy, width - 36, rowH, TerminalIcon.STATUS,
                    "RESTORE", "Warfront, Archives/Warden, Corruption Bloom, purification seal. Permanent route.", ready, TerminalUi.GREEN);
            cy += rowH + 8;
            pathRow(context, graphics, x + 18, cy, width - 36, rowH, TerminalIcon.NEXUS,
                    "DESTROY", "Warfront, Archives/Warden, Severance Engine, collapse seal. Permanent route.", ready, TerminalUi.RED);
            cy += rowH + 8;
            pathRow(context, graphics, x + 18, cy, width - 36, rowH, TerminalIcon.ENDGAME,
                    "CONTROL", "Warfront, Archives/Warden, Mirror Command, command seal. Permanent route.", ready, 0xFFC77DFF);
            if (!relayLines.isEmpty()) {
                int relayY = cy + rowH + 9;
                TerminalUi.line(context, graphics, "RELAY NETWORK", x + 18, relayY, width - 36, TerminalUi.AMBER);
                relayY += 13;
                int maxLines = Math.min(6, Math.max(1, (y + height - relayY - 4) / 11));
                for (int i = 0; i < Math.min(maxLines, relayLines.size()); i++) {
                    TerminalUi.line(context, graphics, relayLines.get(i), x + 20, relayY, width - 40, TerminalUi.MUTED);
                    relayY += 11;
                }
            }
        }

        private static void pathRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, int height, TerminalIcon icon, String title, String detail, boolean ready, int color) {
            graphics.fill(x, y, x + width, y + height, 0x77071117);
            graphics.fill(x, y + height - 1, x + width, y + height, 0x33244352);
            graphics.outline(x, y, width, height, ready ? (0x33000000 | (color & 0x00FFFFFF)) : 0x33244352);
            int iconSize = height < 52 ? 20 : 24;
            TerminalUi.hybridIcon(graphics, TerminalVisualAssets.ICON_PAGE_NEXUS_CORE, icon,
                    x + 10, y + Math.max(7, (height - iconSize) / 2), iconSize, color, ready);
            TerminalUi.line(context, graphics, title, x + 44, y + 8, width - 126, TerminalUi.TEXT);
            TerminalUi.wrap(context, graphics, detail, x + 44, y + (height < 52 ? 21 : 24), width - 126, TerminalUi.MUTED);
            TerminalUi.missionStatusPill(context, graphics, ready ? "READY" : "LOCKED",
                    x + width - 86, y + Math.max(10, height / 2 - 7), 76);
        }

        private void drawNexusCommand(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, int height, String label, String payload, boolean ready, int mouseX, int mouseY) {
            boolean effectiveReady = ready && !referenceOnly(context);
            boolean hover = effectiveReady && TerminalUi.inside(mouseX, mouseY, x, y, width, height);
            boolean armed = effectiveReady && isPendingChoice(context.player(), payload);
            String visibleLabel = armed ? confirmLabel(payload) : label;
            if (effectiveReady) {
                TerminalUi.primaryCommandButton(context, graphics, x, y, width, height, visibleLabel,
                        TerminalVisualAssets.ICON_STATE_ACTIVE, armed ? TerminalUi.AMBER : descriptor().accentColor(), hover);
            } else {
                TerminalUi.disabledCommandButton(context, graphics, x, y, width, height,
                        label + (ready ? " / READ ONLY" : " / LOCKED"), TerminalVisualAssets.ICON_STATE_LOCKED);
            }
            addHitbox(x, y, width, height, effectiveReady, () -> {
                if (isPendingChoice(context.player(), payload)) {
                    context.sendAction(NEXUS, NEXUS_CHOICE, payload);
                    clearPendingChoice();
                } else {
                    armChoice(context.player(), payload);
                }
            });
        }

        private static int nexusPanelHeight(TerminalRenderContext context, int panelW, boolean sealed,
                boolean ready, NexusWorldData.WorldState state, ClientCoreStatus core, ClientGuardianStatus guardians) {
            if (sealed) {
                String sealedText = "Route systems and addon chapters can now read this sealed path.";
                return Math.max(88, 10 + 14 * 3 + 2
                        + TerminalUi.wrappedHeight(context, sealedText, panelW - 16) + 10);
            }
            String reason = ready
                    ? "Status: READY. Select a path once, then confirm; this choice is permanent."
                    : choiceBlockedReason(state, PostNexusData.get(context.player()), core, guardians);
            int checklistWidth = panelW - 16;
            int height = 10 + 14 + 4;
            height += checklistHeight(context, checklistWidth, core.nearbyCore() ? "Nearby" : "Stand near unresolved Nexus Core");
            height += checklistHeight(context, checklistWidth,
                    core.nearbyCore()
                            ? core.activatedNodes() + "/" + NexusCoreBlock.REQUIRED_NODES + " active"
                            : "--/" + NexusCoreBlock.REQUIRED_NODES + " active");
            height += checklistHeight(context, checklistWidth, guardians.resolvedCount() + "/" + guardians.totalCount() + " resolved");
            height += checklistHeight(context, checklistWidth, HudState.getNexusRelaysResolved() + "/3 resolved");
            height += checklistHeight(context, checklistWidth, HudState.isNexusSiegeComplete() ? "Complete" : "Pending");
            height += checklistHeight(context, checklistWidth, "Ready for first commitment");
            height += 6;
            height += Math.max(23, TerminalUi.wrappedHeight(context, reason, panelW - 14) + 14) + 8;
            height += 18 + 8;
            return Math.max(118, height);
        }

        private static int checklistHeight(TerminalRenderContext context, int width, String detail) {
            int split = Math.min(160, Math.max(96, width / 3));
            return TerminalUi.wrappedHeight(context, detail, Math.max(24, width - split)) + 3;
        }

        private void drawWarfrontCommands(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, boolean sealed, PostNexusData post, int mouseX, int mouseY) {
            int gap = 6;
            int buttonH = 18;
            int rowW = Math.max(1, width);
            int smallW = Math.max(62, (rowW - gap * 2) / 3);
            boolean canPrep = !sealed && !HudState.isNexusWarfrontComplete();
            boolean needsEncounter = canPrep
                    && HudState.isNexusCampaignAwakened()
                    && HudState.getNexusRelaysScanned() >= 6
                    && HudState.getNexusRelaysResolved() < 3;
            drawWarfrontCommand(context, graphics, x, y, smallW, buttonH, "WAKE", "awaken",
                    canPrep && !HudState.isNexusCampaignAwakened(), mouseX, mouseY);
            drawWarfrontCommand(context, graphics, x + smallW + gap, y, smallW, buttonH, "SCAN", "scan",
                    canPrep && HudState.isNexusCampaignAwakened() && HudState.getNexusRelaysScanned() < 6, mouseX, mouseY);
            drawWarfrontCommand(context, graphics, x + (smallW + gap) * 2, y,
                    Math.max(62, rowW - (smallW + gap) * 2), buttonH,
                    needsEncounter ? "FIGHT" : "SIEGE",
                    needsEncounter ? "encounter" : "siege",
                    needsEncounter || (canPrep && HudState.getNexusRelaysResolved() >= 3 && !HudState.isNexusSiegeComplete()),
                    mouseX, mouseY);

            int rowY = y + buttonH + 5;
            drawWarfrontCommand(context, graphics, x, rowY, smallW, buttonH, "STABILIZE", "relay:stabilize",
                    canPrep && HudState.getNexusRelaysResolved() < 3 && HudState.getNexusRelaysScanned() >= 6, mouseX, mouseY);
            drawWarfrontCommand(context, graphics, x + smallW + gap, rowY, smallW, buttonH, "SEVER", "relay:sever",
                    canPrep && HudState.getNexusRelaysResolved() < 3 && HudState.getNexusRelaysScanned() >= 6, mouseX, mouseY);
            boolean finalAct = sealed && post.hasMadeChoice() && post.isWardenDefeated();
            if (finalAct) {
                boolean operationComplete = post.getPathOperationsComplete() >= 1;
                drawWarfrontCommand(context, graphics, x + (smallW + gap) * 2, rowY,
                        Math.max(62, rowW - (smallW + gap) * 2), buttonH,
                        operationComplete ? "FINALE" : "OPERATION",
                        operationComplete ? "finale" : "operation",
                        !post.isFinalBossDefeated(), mouseX, mouseY);
            } else {
                drawWarfrontCommand(context, graphics, x + (smallW + gap) * 2, rowY,
                        Math.max(62, rowW - (smallW + gap) * 2), buttonH, "OVERRIDE", "relay:override",
                        canPrep && HudState.getNexusRelaysResolved() < 3 && HudState.getNexusRelaysScanned() >= 6,
                        mouseX, mouseY);
            }
        }

        private void drawWarfrontCommand(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int width, int height, String label, String payload,
                boolean enabled, int mouseX, int mouseY) {
            boolean effectiveEnabled = enabled && !referenceOnly(context);
            boolean hover = effectiveEnabled && TerminalUi.inside(mouseX, mouseY, x, y, width, height);
            if (effectiveEnabled) {
                TerminalUi.button(context, graphics, x, y, width, label, descriptor().accentColor(), true, hover);
            } else {
                TerminalUi.button(context, graphics, x, y, width, label, TerminalUi.MUTED, false, false);
            }
            addHitbox(x, y, width, height, effectiveEnabled,
                    () -> context.sendAction(NEXUS, NEXUS_WARFRONT, payload));
        }

        private static String choiceBlockedReason(NexusWorldData.WorldState state, PostNexusData post,
                ClientCoreStatus core, ClientGuardianStatus guardians) {
            if (state != NexusWorldData.WorldState.NORMAL) {
                return "Choice already committed for this world.";
            }
            if (guardians.missingCount() > 0) {
                return guardians.missingCount() + " guardian signal"
                        + (guardians.missingCount() == 1 ? "" : "s")
                        + " unresolved. Next: " + guardians.firstMissingTitle() + ".";
            }
            if (!core.nearbyCore()) {
                return "Stand near the unresolved Nexus Core to commit a final path.";
            }
            if (core.activatedNodes() < NexusCoreBlock.REQUIRED_NODES) {
                return "Restore " + (NexusCoreBlock.REQUIRED_NODES - core.activatedNodes())
                        + " more Power Nodes near the Core.";
            }
            if (!warfrontReady(post)) {
                return "Resolve three Prime Relays and survive the Core countermeasure siege.";
            }
            return "Server validation required.";
        }

        private static boolean choiceReady(Player player, QuestData quest, PostNexusData post,
                                           ClientCoreStatus core, ClientGuardianStatus guardians) {
            return clientWorldState(post) == NexusWorldData.WorldState.NORMAL
                    && core.nearbyCore()
                    && core.activatedNodes() >= NexusCoreBlock.REQUIRED_NODES
                    && guardians.missingCount() == 0
                    && warfrontReady(post);
        }

        private static boolean warfrontReady(PostNexusData post) {
            return HudState.isNexusWarfrontComplete()
                    || (HudState.isNexusCampaignAwakened()
                            && HudState.getNexusRelaysResolved() >= 3
                            && HudState.isNexusSiegeComplete())
                    || (post.getRelaysResolved() >= 3 && post.getSiegesSurvived() >= 1);
        }

        private static boolean pendingChoiceExpired(Player player) {
            return !pendingChoicePayload.isBlank()
                    && pendingChoiceTick != Long.MIN_VALUE
                    && player.level().getGameTime() - pendingChoiceTick > CONFIRM_TIMEOUT_TICKS;
        }

        private static void armChoice(Player player, String payload) {
            pendingChoicePayload = payload == null ? "" : payload;
            pendingChoiceTick = player.level().getGameTime();
        }

        private static boolean isPendingChoice(Player player, String payload) {
            if (pendingChoiceExpired(player)) {
                clearPendingChoice();
                return false;
            }
            return !pendingChoicePayload.isBlank() && pendingChoicePayload.equals(payload);
        }

        private static String pendingChoiceLabel(Player player) {
            if (pendingChoiceExpired(player)) {
                clearPendingChoice();
                return "";
            }
            return switch (pendingChoicePayload) {
                case "restore" -> "RESTORE";
                case "destroy" -> "DESTROY";
                case "control" -> "CONTROL";
                default -> "";
            };
        }

        private static String confirmLabel(String payload) {
            return switch (payload) {
                case "restore" -> "CONFIRM RESTORE";
                case "destroy" -> "CONFIRM DESTROY";
                case "control" -> "CONFIRM CONTROL";
                default -> "CONFIRM";
            };
        }

        private static void clearPendingChoice() {
            pendingChoicePayload = "";
            pendingChoiceTick = Long.MIN_VALUE;
        }

        private static NexusWorldData.WorldState clientWorldState(PostNexusData post) {
            NexusWorldData.WorldState state = HudState.getNexusState();
            if (state != NexusWorldData.WorldState.NORMAL || !post.hasMadeChoice()) {
                return state;
            }
            return switch (post.getSelectedPath()) {
                case RESTORE -> NexusWorldData.WorldState.RESTORED;
                case DESTROY -> NexusWorldData.WorldState.DESTROYED;
                case CONTROL -> NexusWorldData.WorldState.CONTROLLED;
                case NONE -> NexusWorldData.WorldState.NORMAL;
            };
        }

        private static ClientCoreStatus scanNearbyCore(Player player) {
            Level level = player.level();
            long gameTime = level.getGameTime();
            if (gameTime - lastScanTick < 20L) {
                return lastCoreStatus;
            }
            lastScanTick = gameTime;

            BlockPos playerPos = player.blockPosition();
            for (BlockPos cursor : BlockPos.betweenClosed(
                    playerPos.offset(-CORE_SCAN_HORIZONTAL, -CORE_SCAN_VERTICAL, -CORE_SCAN_HORIZONTAL),
                    playerPos.offset(CORE_SCAN_HORIZONTAL, CORE_SCAN_VERTICAL, CORE_SCAN_HORIZONTAL))) {
                if (!level.getBlockState(cursor).is(ModBlocks.NEXUS_CORE.get())) {
                    continue;
                }
                BlockEntity blockEntity = level.getBlockEntity(cursor);
                if (blockEntity instanceof NexusCoreBlockEntity core && !core.hasChoiceBeenMade()) {
                    lastCoreStatus = new ClientCoreStatus(true,
                            core.getActivatedNodeCount(level, core.getBlockPos()));
                    return lastCoreStatus;
                }
            }

            lastCoreStatus = new ClientCoreStatus(false, 0);
            return lastCoreStatus;
        }

        private static ClientGuardianStatus guardianStatus(QuestData quest) {
            int total = BiomeGuardianProfiles.all().size();
            int missing = 0;
            String firstMissing = "";
            for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
                if (quest.isMissionCompleted(profile.missionId()) || quest.getEntityKills(profile.entityId()) >= 1) {
                    continue;
                }
                missing++;
                if (firstMissing.isEmpty()) {
                    firstMissing = profile.title();
                }
            }
            return new ClientGuardianStatus(total, total - missing, missing, firstMissing);
        }

        private record ClientCoreStatus(boolean nearbyCore, int activatedNodes) {
        }

        private record ClientGuardianStatus(int totalCount, int resolvedCount, int missingCount,
                                            String firstMissingTitle) {
        }
    }
}

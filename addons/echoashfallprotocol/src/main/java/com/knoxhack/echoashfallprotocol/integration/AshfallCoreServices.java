package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoProfile;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.NexusCampaignService;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionInteractionHandler;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Ashfall-owned service implementations exposed through ECHO Core.
 */
public final class AshfallCoreServices {
    private static final String CHAPTER_ID = "ashfall_protocol";

    private AshfallCoreServices() {
    }

    public static void register() {
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return EchoAshfallProtocol.MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: Ashfall Protocol";
            }

            @Override
            public String summary() {
                return "Survival progression, missions, drones, Nexus path data, and wasteland field intel.";
            }

            @Override
            public String statusLine(net.minecraft.world.entity.player.Player player) {
                if (player == null) {
                    return "Ashfall systems available.";
                }
                try {
                    QuestData quest = QuestData.get(player);
                    return "Phase " + (quest.getCurrentPhase() + 1)
                            + " / Mission " + (quest.getCurrentMissionIndex() + 1);
                } catch (RuntimeException exception) {
                    EchoAshfallProtocol.LOGGER.warn("Ashfall chapter status failed; using fallback status.", exception);
                    return "Ashfall systems available.";
                }
            }
        });

        if (!nexusProtocolLoaded()) {
            EchoCoreServices.registerNexusPathService(AshfallCoreServices::hasPostNexusChoice);
            EchoCoreServices.registerNexusCampaignService(new AshfallNexusCampaignService());
        }
        EchoCoreServices.registerIntelMirrorService(AshfallCoreServices::mirrorIntel);
        EchoCoreServices.registerHazardTelemetryService(AshfallCoreServices::hazardTelemetry);
        EchoCoreServices.registerDiagnosticService(AshfallCoreServices::diagnostics);
        EchoCoreServices.registerRouteRecordService(AshfallCoreServices::routeRecords);
        EchoCoreServices.registerDiscoveryProvider(new AshfallDiscoveryProvider());
        EchoCoreServices.registerFactionActionHandler(AshfallFactionInteractionHandler.INSTANCE);
        AshfallBiomeFactions.register();
        EchoAshfallProtocol.LOGGER.info("ECHO platform providers after Ashfall setup: {}",
                EchoCoreServices.platformProviderSummary());
    }

    private static boolean nexusProtocolLoaded() {
        try {
            return EchoRuntimeModules.isLoaded("echonexusprotocol");
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus ownership check failed; keeping legacy Nexus services available.", exception);
            return false;
        }
    }

    private static boolean hasPostNexusChoice(Player player) {
        if (player == null) {
            return false;
        }
        try {
            PostNexusData postNexus = PostNexusData.get(player);
            boolean hasChoice = postNexus.hasMadeChoice();
            if (hasChoice && player instanceof ServerPlayer serverPlayer) {
                String path = postNexus.getSelectedPath().name().toLowerCase(Locale.ROOT);
                EchoProfile profile = EchoCoreServices.profile(player).withNexusPath(path).completeArc("ashfall:nexus");
                EchoCoreServices.saveProfile(serverPlayer, profile);
                EchoCoreServices.recordMilestone(serverPlayer, "ashfall:nexus:" + path);
            }
            return hasChoice;
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus path provider failed; treating path as unresolved.", exception);
            return false;
        }
    }

    private static NexusWorldData campaignData(Player player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        try {
            return NexusWorldData.get(level.getServer().overworld());
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus campaign data unavailable; using fallback campaign state.", exception);
            return null;
        }
    }

    private static EchoHazardTelemetry hazardTelemetry(Player player) {
        if (player == null) {
            return EchoHazardTelemetry.nominal();
        }
        try {
            SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
            int hydration = Math.round(survival.getHydrationPercent() * 100.0F);
            int radiation = Math.round(survival.getRadiationLevel());
            int toxicAir = survival.isToxicAirActive() ? 80 : 0;
            int cold = survival.isCryoZone() ? Math.max(45, Math.round(survival.getHazardIntensity() * 100.0F)) : 0;
            int heat = survival.isAcidContact() ? 45 : 0;
            int exposure = survival.isNexusAnomaly() ? 75 : survival.isRadiationStorm() ? 60 : 0;
            String status = survival.getHazardReason().isBlank()
                    ? "Ashfall survival telemetry online."
                    : survival.getHazardReason();
            return new EchoHazardTelemetry(hydration, radiation, toxicAir, 100, 100, cold, heat, exposure, status);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall hazard telemetry failed; using nominal telemetry.", exception);
            return EchoHazardTelemetry.nominal();
        }
    }

    private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        if (player == null) {
            return List.of();
        }
        try {
            List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
            SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
            QuestData quest = QuestData.get(player);
            if (quest.getCurrentPhase() <= 0 && !quest.isDropPodInitialized()) {
                blockers.add(blocker("ashfall_drop_pod_uninitialized", EchoDiagnosticBlocker.Severity.WARNING,
                        "Drop pod start unconfirmed", "The Ashfall drop pod initialization flag is not set.",
                        "Stay near the pod start, secure the lockers, and let the first recovery objective initialize."));
            }
            if (quest.getCurrentPhase() <= 0 && !quest.isEchoIntroPlayed()) {
                blockers.add(blocker("ashfall_intro_pending", EchoDiagnosticBlocker.Severity.INFO,
                        "First contact pending", "ECHO-7 onboarding has not completed for this operator.",
                        "Open the ECHO interface or follow the first podfall prompt before leaving the crash site."));
            }
            if (!quest.isTerminalOnline()) {
                blockers.add(blocker("ashfall_terminal_offline", EchoDiagnosticBlocker.Severity.BLOCKED,
                        "Terminal offline", "The Ashfall terminal state is offline or critically damaged.",
                        "Repair or re-open the ECHO Terminal before relying on route guidance."));
            } else if (quest.getLastTerminalInteraction() <= 0L && quest.getCurrentPhase() <= 1) {
                blockers.add(blocker("ashfall_terminal_unopened", EchoDiagnosticBlocker.Severity.INFO,
                        "Terminal not yet opened", "The shared terminal has not recorded an early Ashfall interaction.",
                        "Open the ECHO Terminal and review Command Deck, What Now, and Route Sources."));
            }
            if (survival.getHydration() <= 25) {
                blockers.add(blocker("ashfall_low_hydration", EchoDiagnosticBlocker.Severity.WARNING,
                        "Hydration low", "Water is below safe field range.",
                        "Purify dirty water or return to shelter before pushing the route."));
            }
            if (survival.isToxicAirActive() && !survival.hasMask()) {
                blockers.add(blocker("ashfall_toxic_air_no_mask", EchoDiagnosticBlocker.Severity.BLOCKED,
                        "Toxic air active", "The local atmosphere is unsafe and no mask is active.",
                        "Equip or craft a mask before long exposure."));
            }
            if (!quest.getAllPendingRewards().isEmpty()) {
                blockers.add(blocker("ashfall_pending_rewards", EchoDiagnosticBlocker.Severity.INFO,
                        "Mission rewards pending", "Ashfall has unclaimed mission reward caches.",
                        "Open the Ashfall mission channel or shared Reward Inbox to claim support items."));
            }
            if (quest.getCurrentPhase() >= 6) {
                List<BiomeGuardianProfile> missingGuardians = missingGuardians(quest);
                if (!missingGuardians.isEmpty()) {
                    BiomeGuardianProfile next = missingGuardians.get(0);
                    blockers.add(blocker("ashfall_guardians_unresolved", EchoDiagnosticBlocker.Severity.BLOCKED,
                            "Guardian signals unresolved",
                            missingGuardians.size() + " active biome guardian signal(s) still block the Nexus route.",
                            "Track the next guardian route: " + next.title() + "."));
                }
            }
            if (quest.getCurrentPhase() >= 7 && quest.getCollectedPowerNodes() < 5) {
                blockers.add(blocker("ashfall_power_nodes_incomplete", EchoDiagnosticBlocker.Severity.BLOCKED,
                        "Power node grid incomplete",
                        "Only " + quest.getCollectedPowerNodes() + "/5 Ashfall Power Nodes are recorded.",
                        "Restore five Power Nodes near the Nexus route before committing the final protocol."));
            }
            NexusCampaignData campaign = campaignWarfrontData(player);
            if (campaign != null && campaign.isAwakened() && !campaign.isWarfrontComplete()) {
                if (campaign.getScannedRelayCount() < NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT) {
                    blockers.add(blocker("ashfall_prime_relays_unscanned", EchoDiagnosticBlocker.Severity.BLOCKED,
                            "Prime Relays unscanned",
                            "Prime Relay scan progress is " + campaign.getScannedRelayCount() + "/"
                                    + NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT + ".",
                            "Scan the full Prime Relay network before resolving relay encounters."));
                } else if (campaign.getResolvedRelayCount() < NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT) {
                    blockers.add(blocker("ashfall_prime_relays_unresolved", EchoDiagnosticBlocker.Severity.BLOCKED,
                            "Prime Relays unresolved",
                            "Prime Relay resolution progress is " + campaign.getResolvedRelayCount() + "/"
                                    + NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT + ".",
                            "Complete relay encounters and resolve at least three Prime Relays."));
                } else if (!campaign.isSiegeComplete()) {
                    blockers.add(blocker("ashfall_countermeasure_siege_pending", EchoDiagnosticBlocker.Severity.BLOCKED,
                            "Countermeasure siege pending",
                            "Prime Relays are ready, but the Core countermeasure siege is not complete.",
                            "Survive the Core countermeasure event before opening the Nexus final protocol."));
                }
            }
            if (!nexusProtocolLoaded()) {
                PostNexusData postNexus = PostNexusData.get(player);
                if (quest.getCurrentPhase() >= 7 && !postNexus.hasMadeChoice()) {
                    blockers.add(blocker("ashfall_nexus_unresolved", EchoDiagnosticBlocker.Severity.BLOCKED,
                            "Nexus path unresolved", "The endgame path has not been committed.",
                            "Use the Nexus Core terminal near the resolved grid and choose Restore, Destroy, or Control."));
                }
            }
            return List.copyOf(blockers);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall diagnostic provider failed; returning no blockers.", exception);
            return List.of();
        }
    }

    private static NexusCampaignData campaignWarfrontData(Player player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        try {
            return NexusCampaignData.get(level.getServer().overworld());
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus Warfront diagnostics unavailable.", exception);
            return null;
        }
    }

    private static List<BiomeGuardianProfile> missingGuardians(QuestData quest) {
        if (quest == null) {
            return List.of();
        }
        return BiomeGuardianProfiles.all().stream()
                .filter(profile -> !quest.isMissionCompleted(profile.missionId())
                        && quest.getEntityKills(profile.entityId()) <= 0)
                .toList();
    }

    private static List<EchoRouteRecord> routeRecords(Player player) {
        if (player == null) {
            return List.of();
        }
        try {
            QuestData quest = QuestData.get(player);
            List<EchoRouteRecord> routes = new ArrayList<>();
            routes.add(new EchoRouteRecord(
                    id("ashfall_active_protocol"),
                    CHAPTER_ID,
                    "Ashfall Active Protocol",
                    "Mission",
                    player.level().dimension().identifier().toString(),
                    "PHASE " + (quest.getCurrentPhase() + 1),
                    "Current Ashfall mission route index " + (quest.getCurrentMissionIndex() + 1) + ".",
                    false));
            if (!nexusProtocolLoaded()) {
                PostNexusData postNexus = PostNexusData.get(player);
                routes.add(new EchoRouteRecord(
                        id("ashfall_nexus_handoff"),
                        CHAPTER_ID,
                        "Nexus Handoff",
                        "Legacy",
                        "Overworld Nexus Core",
                        postNexus.hasMadeChoice() ? postNexus.getSelectedPath().name() : "LOCKED",
                        postNexus.hasMadeChoice()
                                ? "Nexus legacy is ready for Orbital Remnants."
                                : "Resolve the Ashfall Nexus path to create a full-saga handoff.",
                        postNexus.hasMadeChoice()));
            }
            return List.copyOf(routes);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall route provider failed; returning no routes.", exception);
            return List.of();
        }
    }

    private static EchoDiagnosticBlocker blocker(String path, EchoDiagnosticBlocker.Severity severity,
            String title, String detail, String nextAction) {
        return new EchoDiagnosticBlocker(id(path), CHAPTER_ID, severity, title, detail, nextAction);
    }

    private static void mirrorIntel(ServerPlayer player, String sourceModId, String id, String title, String content) {
        if (player == null) {
            return;
        }
        String safeSource = sanitize(sourceModId, "addon");
        String safeId = sanitize(id, "entry");
        String safeTitle = title == null || title.isBlank() ? safeId : title;
        String safeContent = content == null ? "" : content;

        try {
            QuestData quest = QuestData.get(player);
            quest.addToArchive("[" + safeSource.toUpperCase(Locale.ROOT) + "] " + safeTitle);
            QuestData.saveAndSync(player, quest);

            EchoIntel intel = EchoIntel.get(player);
            intel.discoverLore(safeSource + "_" + safeId, safeTitle, safeContent);
            EchoIntel.saveAndSync(player, intel);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall intel mirror failed for {}:{}.", safeSource, safeId, exception);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private static final class AshfallNexusCampaignService implements NexusCampaignService {
        @Override
        public String pathId(Player player) {
            if (player == null) {
                return "";
            }
            try {
                PostNexusData postNexus = PostNexusData.get(player);
                return postNexus.hasMadeChoice()
                        ? postNexus.getSelectedPath().name().toLowerCase(Locale.ROOT)
                        : "";
            } catch (RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus campaign path lookup failed.", exception);
                return "";
            }
        }

        @Override
        public int instability(Player player) {
            NexusWorldData campaign = campaignData(player);
            if (campaign == null) {
                return 0;
            }
            if (campaign.isDestroyed()) {
                return 85;
            }
            if (campaign.isControlled()) {
                return 45;
            }
            return campaign.isRestored() ? 15 : 0;
        }

        @Override
        public boolean isWarfrontComplete(Player player) {
            NexusWorldData campaign = campaignData(player);
            return campaign != null && campaign.hasChoiceBeenMade();
        }

        @Override
        public boolean isFinalProtocolComplete(Player player) {
            if (player == null) {
                return false;
            }
            try {
                return PostNexusData.get(player).isFinalProtocolComplete();
            } catch (RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus final protocol lookup failed.", exception);
                return false;
            }
        }

        @Override
        public List<String> relaySummary(Player player) {
            NexusWorldData campaign = campaignData(player);
            if (campaign == null) {
                return List.of();
            }
            return List.of(
                    "Nexus state: " + campaign.getState().name().toLowerCase(Locale.ROOT),
                    "Tracked power nodes: " + campaign.getActiveNodePositions().size());
        }

        @Override
        public boolean isFinalBossDefeated(Player player) {
            if (player == null) {
                return false;
            }
            try {
                return PostNexusData.get(player).isWardenDefeated();
            } catch (RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("Ashfall Nexus final boss lookup failed.", exception);
                return false;
            }
        }

        @Override
        public String statusLine(Player player) {
            NexusWorldData campaign = campaignData(player);
            return campaign == null ? "Nexus campaign sync pending."
                    : "Nexus state " + campaign.getState().name().toLowerCase(Locale.ROOT) + ".";
        }
    }
}

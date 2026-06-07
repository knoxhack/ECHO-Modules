package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.integration.AshfallCompat;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.progression.ModAdvancements;
import com.knoxhack.echoorbitalremnants.network.EchoTerminalSnapshot;
import com.knoxhack.echoorbitalremnants.network.OpenEchoTerminalPayload;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.world.GroundRecoverySite;
import com.knoxhack.echoorbitalremnants.world.GroundRecoverySites;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;

public class EchoTerminalItem extends Item {
    public EchoTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                performScan(player);
            }
            openTerminal(player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void performScan(Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        if (progress.echoZeroEncountered() && player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.grantEchoZeroResolved(serverPlayer);
        }
        if (progress.sealFinalNetwork(player)) {
            EchoTerminalProgress current = EchoTerminalProgress.get(player);
            report(player, current, current.lastTerminalReport());
            return;
        }
        Level level = player.level();
        if (SuitEvents.isOrbitalProgressionScan(player)) {
            progress.scanOrbit(player);
            String midGameReport = tryMidGameRepair(player, progress);
            progress = EchoTerminalProgress.get(player);
            String surveyReport = trySurveyScan(player, progress);
            String vendorReport = tryFactionVendorScan(player, EchoTerminalProgress.get(player));
            String contractReport = tryFactionContractScan(player, EchoTerminalProgress.get(player), false);
            if (midGameReport != null || surveyReport != null || vendorReport != null || contractReport != null) {
                String combined = combineReports(midGameReport, surveyReport, vendorReport, contractReport);
                report(player, EchoTerminalProgress.get(player), combined);
                return;
            }
            if (level.dimension() == ModDimensions.LUNAR_SCAR_ZONE && has(player, ModItems.HELIUM_3_CELL.get())) {
                if (midGameObjectivesEnabled() && !progress.lunarExtractorGateOpen() && !player.hasInfiniteMaterials()) {
                    report(player, progress, "Mars telemetry unstable. Restore three Helium Extractor Nodes with Helium Extractor Cores before the transfer window can hold.");
                    return;
                }
                progress.unlockMarsRoute(player);
                report(player, progress, "Mars transfer window resolved from Helium-3 telemetry. The route is thin, but real.");
            } else if (level.dimension() == ModDimensions.MARS_ASH_BASIN && has(player, ModItems.MARTIAN_SILICA.get())) {
                if (midGameObjectivesEnabled() && !progress.marsHabitatGateOpen() && !player.hasInfiniteMaterials()) {
                    report(player, progress, "Europa prep unstable. Repair three Mars Pressure Consoles with Pressure Regulators before the suit route can hold.");
                    return;
                }
                progress.unlockEuropaRoute(player);
                report(player, progress, "Europa cryo route triangulated through Martian terraformer dust.");
            } else if (level.dimension() == ModDimensions.EUROPA_CRYO_OCEAN && has(player, ModItems.CRYO_CRYSTAL.get())) {
                if (midGameObjectivesEnabled() && !progress.europaArrayGateOpen() && !player.hasInfiniteMaterials()) {
                    report(player, progress, "Saturn transfer unstable. Calibrate three Europa Thermal Arrays with Europa Probe Arrays before the ring route opens.");
                    return;
                }
                progress.unlockSaturnRoute(player);
                report(player, progress, "Saturn Transfer Window resolved. Europa thermal signal points into the ring graveyard.");
            } else if (level.dimension() == ModDimensions.SATURN_RING_GRAVEYARD && has(player, ModItems.SATURN_RING_FRAGMENT.get())) {
                if (midGameObjectivesEnabled() && !progress.saturnRelayGateOpen() && !player.hasInfiniteMaterials()) {
                    report(player, progress, "Titan descent unstable. Restore three Saturn Ring Relays with Saturn Relay Lenses before the methane shelf can hold.");
                    return;
                }
                progress.unlockTitanRoute(player);
                report(player, progress, "Titan Transfer Window resolved from Saturn ring telemetry.");
            } else if (level.dimension() == ModDimensions.TITAN_METHANE_SHELF && has(player, ModItems.TITAN_SURVEY_CORE.get())) {
                if (midGameObjectivesEnabled() && !progress.titanPumpGateOpen() && !player.hasInfiniteMaterials()) {
                    report(player, progress, "Deep Space Protocol unstable. Pressurize three Titan Methane Pumps before the anomaly route opens.");
                    return;
                }
                progress.unlockDeepSpaceProtocol(player);
                report(player, progress, "Deep Space Protocol unlocked. Titan methane telemetry confirms the anomaly belt.");
            } else if (has(player, ModItems.NEXUS_DRIVE_CORE.get())) {
                progress.unlockDeepSpaceProtocol(player);
                report(player, progress, "Deep Space Protocol unlocked. Nexus Drive telemetry is no longer silent.");
            } else if (hasStationLifeSupportCore(player)) {
                progress.restoreStationLifeSupport(player);
                report(player, progress, "Station life support handshake restored. Lunar Signal unlocked.");
            } else {
                report(player, progress, missingOrbitalScanReport(player, progress));
            }
        } else {
            if (!progress.launchSiteTracked() && AshfallCompat.isOrbitalCalibrationLocked(player)) {
                report(player, progress, "Orbital link sealed. Resolve an ECHO: Ashfall Protocol Nexus path before ECHO-7 can challenge quarantine beyond Earth.");
                return;
            }
            boolean seedSites = !progress.launchSiteTracked();
            if (seedSites && level instanceof ServerLevel serverLevel) {
                progress.markOrbitalContact(player);
                List<GroundRecoverySite> sites = GroundRecoverySites.seedStarterSites(serverLevel, player.blockPosition());
                progress.setGroundRecoverySites(player, sites);
                report(player, progress, "Orbital signal calibrated. Five Earth recovery sites tracked. Scan each landmark to secure launch salvage and prove the sky is answering.");
                return;
            }
            if (progress.hasGroundRecoverySites() && !progress.allGroundRecoverySitesComplete()) {
                EchoTerminalProgress.GroundSiteScanResult result = progress.scanGroundRecoverySite(player);
                report(player, EchoTerminalProgress.get(player), result.report());
                return;
            }
            if (seedSites) {
                progress.markOrbitalContact(player);
                report(player, progress, "Orbital signal calibrated. Recovery sites could not be seeded here; use launch recipes and local salvage as the fallback route.");
                return;
            }
            report(player, progress, progress.allGroundRecoverySitesComplete()
                    ? "Earth recovery complete. Build the launch chain, assemble the Emergency Rocket, then stage it on the pad."
                    : "Orbital signal calibrated. Recovery sites tracked.");
        }
    }

    private static String tryMidGameRepair(Player player, EchoTerminalProgress progress) {
        if (!midGameObjectivesEnabled()) {
            return null;
        }
        Level level = player.level();
        if (level.dimension() == ModDimensions.LOW_EARTH_ORBIT) {
            String siteId = nearbyBlockSiteId(player, "station_network", ModBlocks.STATION_RELAY_NODE.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.STATION_RELAY_FUSE.get()) && !player.hasInfiniteMaterials() && !progress.stationNetworkGateOpen()) {
                return "Station relay waiting for one Station Relay Fuse at this node.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairStationRelay(player, siteId);
            consumeRepairItem(player, ModItems.STATION_RELAY_FUSE.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.STATION_POWER_MATRIX.get(), 1);
                give(player, ModItems.OXYGEN_CANISTER.get(), 2);
                give(player, ModItems.VACUUM_CIRCUIT.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.ELECTRIC_SPARK, 1.5F);
            return result.report("Station Network restored. Lunar prep is stronger and the Orbital Shuttle route is cleared.");
        }
        if (level.dimension() == ModDimensions.LUNAR_SCAR_ZONE) {
            String siteId = nearbyBlockSiteId(player, "lunar_extractors", ModBlocks.HELIUM_EXTRACTOR_NODE.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.HELIUM_EXTRACTOR_CORE.get()) && !player.hasInfiniteMaterials() && !progress.lunarExtractorGateOpen()) {
                return "Helium extractor waiting for one Helium Extractor Core at this node.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairLunarExtractor(player, siteId);
            consumeRepairItem(player, ModItems.HELIUM_EXTRACTOR_CORE.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.LUNAR_PRESSURE_MAP.get(), 1);
                give(player, ModItems.HELIUM_3_CELL.get(), 2);
                give(player, ModItems.SUIT_SEALANT_PATCH.get(), 2);
            }
            playObjectiveFeedback(player, ParticleTypes.HAPPY_VILLAGER, 1.35F);
            return result.report("Helium Extractor Network restored. Mars route reliability is online.");
        }
        if (level.dimension() == ModDimensions.MARS_ASH_BASIN) {
            String siteId = nearbyBlockSiteId(player, "mars_habitats", ModBlocks.MARS_PRESSURE_CONSOLE.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.PRESSURE_REGULATOR.get()) && !player.hasInfiniteMaterials() && !progress.marsHabitatGateOpen()) {
                return "Mars console waiting for one Pressure Regulator at this habitat console.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairMarsPressureConsole(player, siteId);
            consumeRepairItem(player, ModItems.PRESSURE_REGULATOR.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.MARTIAN_HABITAT_KEY.get(), 1);
                give(player, ModItems.MARTIAN_SILICA.get(), 3);
                give(player, ModItems.OXYGEN_BOOSTER.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.CLOUD, 0.9F);
            return result.report("Mars habitats pressurized. Europa prep can hold through dust hazard zones.");
        }
        if (level.dimension() == ModDimensions.EUROPA_CRYO_OCEAN) {
            String siteId = nearbyBlockSiteId(player, "europa_arrays", ModBlocks.EUROPA_THERMAL_ARRAY.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.EUROPA_PROBE_ARRAY.get()) && !player.hasInfiniteMaterials() && !progress.europaArrayGateOpen()) {
                return "Europa array waiting for one Europa Probe Array at this thermal array.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairEuropaThermalArray(player, siteId);
            consumeRepairItem(player, ModItems.EUROPA_PROBE_ARRAY.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.THERMAL_STABILIZER.get(), 1);
                give(player, ModItems.CRYO_BATTERY.get(), 1);
                give(player, ModItems.NEXUS_STABILIZER_SHARD.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.SNOWFLAKE, 1.65F);
            return result.report("Europa Thermal Array calibrated. Saturn Transfer Window is unlocked.");
        }
        if (level.dimension() == ModDimensions.SATURN_RING_GRAVEYARD) {
            String siteId = nearbyBlockSiteId(player, "saturn_relays", ModBlocks.SATURN_RING_RELAY.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.SATURN_RELAY_LENS.get()) && !player.hasInfiniteMaterials() && !progress.saturnRelayGateOpen()) {
                return "Saturn relay waiting for one Saturn Relay Lens at this ring relay.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairSaturnRingRelay(player, siteId);
            consumeRepairItem(player, ModItems.SATURN_RELAY_LENS.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.TITAN_TRANSFER_WINDOW.get(), 1);
                give(player, ModItems.SATURN_RING_FRAGMENT.get(), 2);
                give(player, ModItems.OXYGEN_CANISTER.get(), 2);
            }
            playObjectiveFeedback(player, ParticleTypes.ELECTRIC_SPARK, 1.85F);
            return result.report("Saturn Ring Relays restored. Titan descent vector is stable.");
        }
        if (level.dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            String siteId = nearbyBlockSiteId(player, "titan_pumps", ModBlocks.TITAN_METHANE_PUMP.get());
            if (siteId == null) {
                return null;
            }
            if (!has(player, ModItems.TITAN_METHANE_CELL.get()) && !player.hasInfiniteMaterials() && !progress.titanPumpGateOpen()) {
                return "Titan pump waiting for one Titan Methane Cell at this pressure station.";
            }
            EchoTerminalProgress.RouteObjectiveResult result = progress.repairTitanMethanePump(player, siteId);
            consumeRepairItem(player, ModItems.TITAN_METHANE_CELL.get(), result);
            if (result.newlyComplete()) {
                give(player, ModItems.NEXUS_DRIVE_CORE.get(), 1);
                give(player, ModItems.TITAN_SURVEY_CORE.get(), 1);
                give(player, ModItems.SUIT_SEALANT_PATCH.get(), 2);
            }
            playObjectiveFeedback(player, ParticleTypes.LARGE_SMOKE, 0.7F);
            return result.report("Titan Methane Pumps pressurized. Deep Space Protocol is unlocked.");
        }
        return null;
    }

    private static String trySurveyScan(Player player, EchoTerminalProgress progress) {
        Level level = player.level();
        if (level.dimension() == ModDimensions.LOW_EARTH_ORBIT) {
            String siteId = nearbyBlockSiteId(player, "orbit", ModBlocks.SIGNAL_RELAY.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.ORBIT_SURVEY_DATA.get())) {
                siteId = itemSiteId(player, "orbit");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordOrbitSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.ORBIT_SURVEY_DATA.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.OXYGEN_CANISTER.get(), 2);
                give(player, ModItems.VACUUM_CIRCUIT.get(), 2);
            }
            playObjectiveFeedback(player, ParticleTypes.ELECTRIC_SPARK, 1.5F);
            return result.report("Orbit survey complete. Station power routing and salvage maps improved. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.LUNAR_SCAR_ZONE) {
            String siteId = nearbyBlockSiteId(player, "moon", ModBlocks.SURVEY_MARKER.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.LUNAR_CORE_SAMPLE.get())) {
                siteId = itemSiteId(player, "moon");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordMoonSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.LUNAR_CORE_SAMPLE.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.HELIUM_3_CELL.get(), 2);
                give(player, ModItems.MARTIAN_PRESSURE_VALVE.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.HAPPY_VILLAGER, 1.35F);
            return result.report("Lunar survey complete. Helium telemetry and Mars transfer reliability improved. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.MARS_ASH_BASIN) {
            String siteId = nearbyBlockSiteId(player, "mars", ModBlocks.SIGNAL_RELAY.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.MARTIAN_PRESSURE_VALVE.get())) {
                siteId = itemSiteId(player, "mars");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordMarsSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.MARTIAN_PRESSURE_VALVE.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.MARTIAN_SILICA.get(), 3);
                give(player, ModItems.EUROPA_THERMAL_PROBE.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.CLOUD, 0.9F);
            return result.report("Mars survey complete. Pressure valves restored and Europa prep materials recovered. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.EUROPA_CRYO_OCEAN) {
            String siteId = nearbyBlockSiteId(player, "europa", ModBlocks.THERMAL_VENT.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.EUROPA_THERMAL_PROBE.get())) {
                siteId = itemSiteId(player, "europa");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordEuropaSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.EUROPA_THERMAL_PROBE.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.CRYO_BATTERY.get(), 1);
                give(player, ModItems.NEXUS_STABILIZER_SHARD.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.SNOWFLAKE, 1.65F);
            return result.report("Europa survey complete. Thermal vents mapped and Saturn transfer recipes unlocked. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.SATURN_RING_GRAVEYARD) {
            String siteId = nearbyBlockSiteId(player, "saturn", ModBlocks.SATURN_RING_RELAY.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.SATURN_RING_FRAGMENT.get())) {
                siteId = itemSiteId(player, "saturn");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordSaturnSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.SATURN_RING_FRAGMENT.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.SATURN_RELAY_LENS.get(), 1);
                give(player, ModItems.TITAN_TRANSFER_WINDOW.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.ELECTRIC_SPARK, 1.85F);
            return result.report("Saturn survey complete. Ring relay drift mapped and Titan transfer reliability improved. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            String siteId = nearbyBlockSiteId(player, "titan", ModBlocks.TITAN_METHANE_PUMP.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.TITAN_SURVEY_CORE.get())) {
                siteId = itemSiteId(player, "titan");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordTitanSurvey(player, siteId);
            consumeSurveyItem(player, ModItems.TITAN_SURVEY_CORE.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.TITAN_METHANE_CELL.get(), 2);
                give(player, ModItems.NEXUS_DRIVE_CORE.get(), 1);
            }
            playObjectiveFeedback(player, ParticleTypes.LARGE_SMOKE, 0.7F);
            return result.report("Titan survey complete. Methane shelf telemetry stabilized Nexus prep. Cache role confirmed: route proof, crafting support, and survival recovery.");
        }
        if (level.dimension() == ModDimensions.NEXUS_ANOMALY_BELT) {
            String siteId = nearbyBlockSiteId(player, "nexus", ModBlocks.NEXUS_ANCHOR.get(), ModBlocks.NEXUS_GROWTH.get());
            boolean itemScan = false;
            if (siteId == null && has(player, ModItems.NEXUS_STABILIZER_SHARD.get())) {
                siteId = itemSiteId(player, "nexus");
                itemScan = true;
            }
            if (siteId == null) {
                return null;
            }
            EchoTerminalProgress.SurveyResult result = progress.recordNexusStabilization(player, siteId);
            if (!progress.echoZeroEncountered()) {
                return "Nexus stabilization locked. Resolve ECHO-0 first.";
            }
            consumeSurveyItem(player, ModItems.NEXUS_STABILIZER_SHARD.get(), itemScan, result);
            if (result.newlyComplete()) {
                give(player, ModItems.STABILIZED_ECHO_CORE.get(), 1);
                give(player, ModItems.NEXUS_DUST.get(), 8);
            }
            playObjectiveFeedback(player, ParticleTypes.REVERSE_PORTAL, 1.95F);
            String report = result.report("Nexus anchors stabilized. Post-ECHO-0 survey network is complete. Cache role confirmed: route proof, crafting support, and survival recovery.");
            if (result.newlyComplete() && EchoTerminalProgress.get(player).finalNetworkSealed()) {
                report = EchoTerminalProgress.get(player).lastTerminalReport();
            }
            return report;
        }
        return null;
    }

    private static String tryFactionContractScan(Player player, EchoTerminalProgress progress, boolean reportBlocked) {
        FactionPledgeItem.Faction faction = progress.activeContractFaction();
        if (faction == null) {
            return reportBlocked ? progress.legacyFactionContractRequirement() : null;
        }
        boolean completed = switch (faction) {
            case ORBITAL_REMNANT -> completeOrbitalRemnantContract(player);
            case VOID_SALVAGERS -> completeVoidSalvagerContract(player);
            case NEXUS_CHOIR -> completeNexusChoirContract(player, progress);
        };
        if (!completed) {
            return reportBlocked ? factionBlockedReport(player, progress, faction) : null;
        }
        EchoTerminalProgress.ContractResult result = progress.completeFactionContract(player);
        grantFactionContractReward(player, faction);
        String report = result.name() + " complete. Contract cache delivered; faction relay count " + result.completedCount() + ".";
        if (EchoTerminalProgress.get(player).finalNetworkSealed()) {
            report = EchoTerminalProgress.get(player).lastTerminalReport();
        }
        return report;
    }

    private static String tryFactionVendorScan(Player player, EchoTerminalProgress progress) {
        if (!nearbyAnyBlock(player, ModBlocks.FACTION_VENDOR_KIOSK.get(), ModBlocks.FACTION_RELAY_HUB.get())) {
            return null;
        }
        FactionPledgeItem.Faction faction = alignedFaction(progress);
        if (progress.activeContractFaction() != null) {
            return "Faction support hub reserved for active " + progress.activeContractFaction().displayName()
                    + " contract service. Vendor cache is paused until this requirement clears: "
                    + progress.legacyFactionContractRequirement();
        }
        if (faction == null) {
            return "Faction support hub online. No pledge detected; use a Radwarden Badge, Crashbreak Marker, or Sporebound Sigil to authorize barter caches.";
        }
        String vendorId = "vendor:" + faction.name().toLowerCase(java.util.Locale.ROOT)
                + ":" + player.level().dimension().identifier().getPath()
                + ":" + (player.blockPosition().getX() >> 4)
                + ":" + (player.blockPosition().getZ() >> 4);
        if (!progress.markTerminalMissionCacheClaimed(player, vendorId)) {
            return "Faction support cache already serviced at this hub. Find another relay hub, or use the NPC outpost contact for services and charters.";
        }
        grantFactionVendorReward(player, faction);
        playObjectiveFeedback(player, ParticleTypes.HAPPY_VILLAGER, 1.25F);
        return faction.vendorCacheReport();
    }

    private static FactionPledgeItem.Faction alignedFaction(EchoTerminalProgress progress) {
        if (progress.orbitalRemnantStanding() == com.knoxhack.echoorbitalremnants.progression.FactionStanding.ALIGNED) {
            return FactionPledgeItem.Faction.ORBITAL_REMNANT;
        }
        if (progress.voidSalvagerStanding() == com.knoxhack.echoorbitalremnants.progression.FactionStanding.ALIGNED) {
            return FactionPledgeItem.Faction.VOID_SALVAGERS;
        }
        if (progress.nexusChoirStanding() == com.knoxhack.echoorbitalremnants.progression.FactionStanding.ALIGNED) {
            return FactionPledgeItem.Faction.NEXUS_CHOIR;
        }
        return null;
    }

    private static void grantFactionVendorReward(Player player, FactionPledgeItem.Faction faction) {
        switch (faction) {
            case ORBITAL_REMNANT -> {
                give(player, ModItems.EMERGENCY_OXYGEN_CELL.get(), 3);
                give(player, ModItems.OXYGEN_CANISTER.get(), 1);
                give(player, ModItems.SATURN_RELAY_LENS.get(), 1);
            }
            case VOID_SALVAGERS -> {
                give(player, ModItems.ORBITAL_ALLOY.get(), 2);
                give(player, ModItems.VACUUM_CIRCUIT.get(), 2);
                give(player, ModItems.TITAN_METHANE_CELL.get(), 1);
            }
            case NEXUS_CHOIR -> {
                give(player, ModItems.NEXUS_STABILIZER_SHARD.get(), 2);
                give(player, ModItems.NEXUS_DUST.get(), 4);
                give(player, ModItems.TITAN_SURVEY_CORE.get(), 1);
            }
        }
    }

    private static boolean completeOrbitalRemnantContract(Player player) {
        if (has(player, ModItems.ORBIT_SURVEY_DATA.get())) {
            if (!player.hasInfiniteMaterials()) {
                consumeOne(player, ModItems.ORBIT_SURVEY_DATA.get());
            }
            return true;
        }
        if (player.level().dimension() != ModDimensions.LOW_EARTH_ORBIT) {
            return false;
        }
        if (nearbyAnyBlock(player, ModBlocks.SIGNAL_RELAY.get(), ModBlocks.DOCKING_BEACON.get())) {
            return true;
        }
        return false;
    }

    private static boolean completeVoidSalvagerContract(Player player) {
        if (nearbyAnyBlock(player, ModBlocks.BROKEN_SOLAR_PANEL.get(), ModBlocks.SATELLITE_PLATING.get(),
                ModBlocks.ORBITAL_PLATING.get())) {
            return true;
        }
        if (count(player, ModItems.ORBITAL_ALLOY.get()) >= 1 && count(player, ModItems.VACUUM_CIRCUIT.get()) >= 1) {
            if (!player.hasInfiniteMaterials()) {
                consumeOne(player, ModItems.ORBITAL_ALLOY.get());
                consumeOne(player, ModItems.VACUUM_CIRCUIT.get());
            }
            return true;
        }
        return false;
    }

    private static String factionBlockedReport(Player player, EchoTerminalProgress progress, FactionPledgeItem.Faction faction) {
        return switch (faction) {
            case ORBITAL_REMNANT -> player.level().dimension() != ModDimensions.LOW_EARTH_ORBIT
                    ? "Faction contract wrong dimension: waiting for Low Earth Orbit, or 1 Orbit Survey Data as proof."
                    : "Faction contract waiting for a Signal Relay/Docking Beacon scan, or 1 Orbit Survey Data.";
            case VOID_SALVAGERS -> "Faction contract waiting for orbital salvage, or 1 Orbital Alloy plus 1 Vacuum Circuit.";
            case NEXUS_CHOIR -> !progress.echoZeroEncountered()
                    ? "Faction contract sealed: Sporebound anomaly readings unlock only after ECHO-0 is resolved."
                    : player.level().dimension() != ModDimensions.NEXUS_ANOMALY_BELT
                    ? "Faction contract waiting for the Nexus Anomaly Belt, or 1 Nexus Stabilizer Shard."
                    : "Faction contract waiting for a Nexus Anchor/Growth scan, or 1 Nexus Stabilizer Shard.";
        };
    }

    private static boolean completeNexusChoirContract(Player player, EchoTerminalProgress progress) {
        if (!progress.echoZeroEncountered()) {
            return false;
        }
        if (player.level().dimension() == ModDimensions.NEXUS_ANOMALY_BELT
                && nearbyAnyBlock(player, ModBlocks.NEXUS_ANCHOR.get(), ModBlocks.NEXUS_GROWTH.get())) {
            return true;
        }
        if (has(player, ModItems.NEXUS_STABILIZER_SHARD.get())) {
            if (!player.hasInfiniteMaterials()) {
                consumeOne(player, ModItems.NEXUS_STABILIZER_SHARD.get());
            }
            return true;
        }
        return false;
    }

    private static void grantFactionContractReward(Player player, FactionPledgeItem.Faction faction) {
        switch (faction) {
            case ORBITAL_REMNANT -> {
                give(player, ModItems.EMERGENCY_OXYGEN_CELL.get(), 4);
                give(player, ModItems.OXYGEN_CANISTER.get(), 2);
                give(player, ModItems.SUIT_SEALANT_PATCH.get(), 2);
            }
            case VOID_SALVAGERS -> {
                give(player, ModItems.VACUUM_CIRCUIT.get(), 2);
                give(player, ModItems.HEAT_SHIELD_PLATE.get(), 1);
                give(player, ModItems.NAVIGATION_CHIP.get(), 1);
            }
            case NEXUS_CHOIR -> {
                give(player, ModItems.NEXUS_DUST.get(), 4);
                give(player, ModItems.CRYO_BATTERY.get(), 1);
                give(player, ModItems.EMERGENCY_OXYGEN_CELL.get(), 2);
            }
        }
    }

    private static String missingOrbitalScanReport(Player player, EchoTerminalProgress progress) {
        Level level = player.level();
        if (progress.finalNetworkSealed()) {
            return progress.scanRequirement();
        }
        if (progress.activeContractFaction() != null) {
            return factionBlockedReport(player, progress, progress.activeContractFaction());
        }
        if (progress.factionContractCoolingDown()) {
            return "Faction relay is cooling down. Wait for the sync counter to clear, then press SCAN again.";
        }
        if (progress.allSurveysComplete()) {
            return !progress.allOutpostChartersComplete()
                    ? "Survey network complete. " + progress.factionContractRequirement()
                    + " Complete Tier I outpost charters before the final seal (" + progress.completedOutpostCharterCount() + "/3)."
                    : "Survey network complete. Press SCAN to seal the final network.";
        }
        if (!progress.stationLifeSupportRestored()) {
            return "Scan incomplete. Carry or stand near a Station Life Support Core to unlock the Lunar Signal.";
        }
        if (level.dimension() == ModDimensions.LOW_EARTH_ORBIT) {
            if (midGameObjectivesEnabled() && !progress.stationNetworkGateOpen()) {
                return "Orbit objective needs a Station Relay Node nearby and one Station Relay Fuse.";
            }
            return "Orbit scan found no new log. Stand near a Signal Relay or carry Orbit Survey Data.";
        }
        if (level.dimension() == ModDimensions.LUNAR_SCAR_ZONE) {
            if (midGameObjectivesEnabled() && !progress.lunarExtractorGateOpen()) {
                return "Lunar objective needs a Helium Extractor Node nearby and one Helium Extractor Core.";
            }
            if (!progress.marsRouteUnlocked()) {
                return "Lunar scan incomplete. Carry a Helium-3 Cell to resolve the Mars Transfer Window.";
            }
            return "Lunar survey needs a Survey Marker nearby or one Lunar Core Sample in inventory.";
        }
        if (level.dimension() == ModDimensions.MARS_ASH_BASIN) {
            if (midGameObjectivesEnabled() && !progress.marsHabitatGateOpen()) {
                return "Mars objective needs a Pressure Console nearby and one Pressure Regulator.";
            }
            if (!progress.europaRouteUnlocked()) {
                return "Mars scan incomplete. Carry Martian Silica to resolve the Europa Transfer Window.";
            }
            return "Mars survey needs a Signal Relay nearby or one Martian Pressure Valve in inventory.";
        }
        if (level.dimension() == ModDimensions.EUROPA_CRYO_OCEAN) {
            if (midGameObjectivesEnabled() && !progress.europaArrayGateOpen()) {
                return "Europa objective needs a Thermal Array nearby and one Europa Probe Array.";
            }
            if (!progress.saturnRouteUnlocked()) {
                return "Europa scan incomplete. Carry a Cryo Crystal to resolve the Saturn Transfer Window.";
            }
            return "Europa survey needs a Thermal Vent nearby or one Europa Thermal Probe in inventory.";
        }
        if (level.dimension() == ModDimensions.SATURN_RING_GRAVEYARD) {
            if (midGameObjectivesEnabled() && !progress.saturnRelayGateOpen()) {
                return "Saturn objective needs a Ring Relay nearby and one Saturn Relay Lens.";
            }
            if (!progress.titanRouteUnlocked()) {
                return "Saturn scan incomplete. Carry a Saturn Ring Fragment to resolve the Titan Transfer Window.";
            }
            return "Saturn survey needs a Ring Relay nearby or one Saturn Ring Fragment in inventory.";
        }
        if (level.dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            if (midGameObjectivesEnabled() && !progress.titanPumpGateOpen()) {
                return "Titan objective needs a Methane Pump nearby and one Titan Methane Cell.";
            }
            if (!progress.deepSpaceProtocolUnlocked()) {
                return "Titan scan incomplete. Carry a Titan Survey Core or Nexus Drive Core to unlock Deep Space Protocol.";
            }
            return "Titan survey needs a Methane Pump nearby or one Titan Survey Core in inventory.";
        }
        if (level.dimension() == ModDimensions.NEXUS_ANOMALY_BELT) {
            if (!progress.echoZeroEncountered()) {
                return "Nexus stabilization locked. Defeat ECHO-0 before Anchor/Growth scans or shards can count.";
            }
            return "Nexus stabilization needs a new Nexus Anchor/Growth site or one Nexus Stabilizer Shard. Signal Analyzer can process Nexus Dust into shards.";
        }
        if (!progress.allOutpostChartersComplete()) {
            return "Outpost charter incomplete. Visit the Saturn, Titan, and Nexus faction NPC outposts.";
        }
        return "Scan found no route hook. Use ROUTES or SURVEY for the next required location and item.";
    }

    private static void report(Player player, EchoTerminalProgress progress, String message) {
        progress.setLastTerminalReport(player, message);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
        }
        player.sendSystemMessage(Component.literal("ECHO-7 // " + message));
    }

    private static void playObjectiveFeedback(Player player, net.minecraft.core.particles.ParticleOptions particle, float pitch) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = player.blockPosition();
        serverLevel.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.45F, pitch);
        serverLevel.sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(), 14, 0.45D, 0.35D, 0.45D, 0.02D);
    }

    private static String combineReports(String... reports) {
        StringBuilder builder = new StringBuilder();
        for (String report : reports) {
            if (report == null || report.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(report);
        }
        return builder.toString();
    }

    public static void openTerminal(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            EchoNetSend.toPlayer(serverPlayer, new OpenEchoTerminalPayload(EchoTerminalSnapshot.from(player)),
                    EchoPacketKind.CLIENTBOUND_SYNC);
        }
    }

    public static boolean hasTerminal(Player player) {
        return has(player, ModItems.ECHO_TERMINAL.get())
                || player.getMainHandItem().is(ModItems.ECHO_TERMINAL.get())
                || player.getOffhandItem().is(ModItems.ECHO_TERMINAL.get());
    }

    private static boolean has(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearbyAnyBlock(Player player, Block... blocks) {
        BlockPos center = player.blockPosition();
        int radius = 12;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            Block block = player.level().getBlockState(pos).getBlock();
            for (Block candidate : blocks) {
                if (block == candidate) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void give(Player player, ItemLike item, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static boolean hasStationLifeSupportCore(Player player) {
        return has(player, ModBlocks.STATION_LIFE_SUPPORT_CORE.get().asItem())
                || hasNearbyBlock(player, ModBlocks.STATION_LIFE_SUPPORT_CORE.get());
    }

    private static boolean hasNearbyBlock(Player player, Block block) {
        BlockPos center = player.blockPosition();
        int radius = 12;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (player.level().getBlockState(pos).getBlock() == block) {
                return true;
            }
        }
        return false;
    }

    private static String nearbyBlockSiteId(Player player, String route, Block... blocks) {
        BlockPos center = player.blockPosition();
        int radius = 12;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            Block found = player.level().getBlockState(pos).getBlock();
            for (Block block : blocks) {
                if (found != block) {
                    continue;
                }
                return route + ":" + player.level().dimension().identifier().getPath() + ":" + (pos.getX() >> 4) + ":" + (pos.getZ() >> 4);
            }
        }
        return null;
    }

    private static String itemSiteId(Player player, String route) {
        return "item:" + route + ":" + player.getStringUUID() + ":" + player.tickCount + ":" + player.level().getGameTime();
    }

    private static void consumeSurveyItem(Player player, Item item, boolean itemScan, EchoTerminalProgress.SurveyResult result) {
        if (itemScan && result.counted() && !player.hasInfiniteMaterials()) {
            consumeOne(player, item);
        }
    }

    private static void consumeRepairItem(Player player, Item item, EchoTerminalProgress.RouteObjectiveResult result) {
        if (result.counted() && !player.hasInfiniteMaterials()) {
            consumeOne(player, item);
        }
    }

    private static boolean midGameObjectivesEnabled() {
        try {
            return Config.MID_GAME_OBJECTIVES_ENABLED.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static boolean consumeOne(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

package com.knoxhack.echoarcanacore.integration.veilbound;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoarcanacore.EchoArcanaCore;
import com.knoxhack.echoarcanacore.api.VeilboundRuntimeSnapshot;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class ArcanaVeilboundLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private ArcanaVeilboundLensIntegration() {
    }

    public static void register() {
        if (!EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
            return;
        }
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(Provider.INSTANCE);
            EchoArcanaCore.LOGGER.info("ECHO Lens bridge loaded for ARCANA: Veilbound Studies.");
        }
    }

    private enum Provider implements ServerLensProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return EchoArcanaCore.id("veilbound_lens_bridge");
        }

        @Override
        public int priority() {
            return 180;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.INTEGRATION;
        }

        @Override
        public boolean supports(LensContext context) {
            return target(context).isPresent();
        }

        @Override
        public List<LensInfoSection> inspect(LensContext context) {
            Optional<VeilboundBridgeCatalog.Entry> match = target(context);
            if (match.isEmpty()) {
                return List.of();
            }
            VeilboundBridgeCatalog.Entry entry = match.get();
            recordDiscovery(context, entry);
            VeilboundRuntimeSnapshot snapshot = VeilboundRuntimeBridge.snapshot(context.player());
            List<LensInfoRow> rows = new ArrayList<>();
            rows.add(row("ARCANA Target", entry.title(), "A", LensTone.ECHO, LensVisibility.COMPACT));
            rows.add(row("Registry", entry.id().toString(), "ID", LensTone.INFO, LensVisibility.EXPANDED));
            rows.add(row("Primary Resonance", VeilboundBridgeCatalog.primaryResonance(stripKind(entry.id())), "R",
                    LensTone.GOOD, LensVisibility.COMPACT));
            rows.add(row("Field Journal", snapshot.available() ? "Saved scans: " + snapshot.scanCount() : "Observation mirror ready",
                    "J", LensTone.INFO, LensVisibility.EXPANDED));
            if (snapshot.available()) {
                rows.add(row("Known Resonance", Integer.toString(snapshot.knownResonances().size()), "R",
                        LensTone.GOOD, LensVisibility.EXPANDED));
                rows.add(row("Active Research", snapshot.activeResearch().isBlank() ? "none" : snapshot.activeResearch(), "T",
                        LensTone.INFO, LensVisibility.EXPANDED));
                rows.add(row("Local Pressure", snapshot.pressureSummary(), "P", LensTone.WARNING,
                        LensVisibility.EXPANDED));
            }
            rows.add(row("Arcane Index", "echoarcaneindex:" + VeilboundBridgeCatalog.indexPagePath(entry), "I",
                    LensTone.INFO, LensVisibility.EXPANDED));
            if (entry.kind() == VeilboundBridgeCatalog.Kind.BLOCK) {
                VeilboundBridgeCatalog.landmarkForBlock(stripKind(entry.id())).ifPresent(landmark ->
                        rows.add(row("HoloMap", "Landmark bridge: " + landmark.title(), "M", LensTone.WARNING,
                                LensVisibility.EXPANDED)));
            }
            return List.of(LensInfoSection.of(
                    EchoArcanaCore.id("veilbound_scan/" + VeilboundBridgeCatalog.entryPath(entry)),
                    LensDataCategory.INTEGRATION,
                    "ARCANA: Veilbound Studies",
                    "V",
                    LensTone.ECHO,
                    LensVisibility.COMPACT,
                    rows));
        }

        @Override
        public List<LensInfoRow> deepScanSignals(LensContext context) {
            Optional<VeilboundBridgeCatalog.Entry> match = target(context);
            if (match.isEmpty()) {
                return List.of();
            }
            VeilboundBridgeCatalog.Entry entry = match.get();
            VeilboundRuntimeSnapshot snapshot = VeilboundRuntimeBridge.snapshot(context.player());
            return List.of(
                    row("Veilbound", entry.title(), "V", LensTone.ECHO, LensVisibility.COMPACT),
                    row("Observation", snapshot.available()
                                    ? "Journal scans: " + snapshot.scanCount() + "; pressure: " + snapshot.pressureSummary()
                                    : "Use Veil Lens for ARCANA-owned journal data",
                            "J", LensTone.INFO,
                            LensVisibility.EXPANDED));
        }

        private static Optional<VeilboundBridgeCatalog.Entry> target(LensContext context) {
            if (context == null || !EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
                return Optional.empty();
            }
            Identifier targetId = null;
            if (context.hasBlock()) {
                targetId = BuiltInRegistries.BLOCK.getKey(context.blockState().getBlock());
            } else if (context.hasEntity()) {
                targetId = BuiltInRegistries.ENTITY_TYPE.getKey(context.entity().getType());
            }
            return VeilboundBridgeCatalog.target(targetId);
        }

        private static void recordDiscovery(LensContext context, VeilboundBridgeCatalog.Entry entry) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Identifier discoveryId = VeilboundBridgeCatalog.landmarkForBlock(stripKind(entry.id()))
                    .map(VeilboundBridgeCatalog::discoveryId)
                    .orElseGet(() -> VeilboundBridgeCatalog.discoveryId(entry));
            VeilboundRuntimeBridge.rememberScanLocation(player, discoveryId, context.blockPos());
            EchoCoreServices.discoverFeature(player, discoveryId);
            VeilboundRuntimeBridge.syncServerProgress(player, VeilboundRuntimeBridge.snapshot(player));
            Identifier firstSignal = EchoArcanaCore.id("first_signal");
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.UNLOCK_RESEARCH,
                    MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, firstSignal, "first_arcane_scan"),
                    1,
                    MissionHookTargets.context(EchoArcanaCore.MODID, firstSignal, "target", entry.id().toString()));
            Identifier firstScan = EchoArcanaCore.id("arcana_veilbound/complete_first_field_scan");
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.SCAN_BLOCK,
                    MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, firstScan, "first_field_scan"),
                    1,
                    MissionHookTargets.context(EchoArcanaCore.MODID, firstScan, "target", entry.id().toString()));
        }

        private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
            return LensInfoRow.of(label, value, icon, tone, visibility);
        }

        private static Identifier stripKind(Identifier id) {
            if (id == null) {
                return VeilboundBridgeCatalog.contentId("unknown");
            }
            String path = id.getPath();
            int slash = path.indexOf('/');
            if (slash >= 0 && slash + 1 < path.length()) {
                return VeilboundBridgeCatalog.contentId(path.substring(slash + 1));
            }
            return id;
        }
    }
}

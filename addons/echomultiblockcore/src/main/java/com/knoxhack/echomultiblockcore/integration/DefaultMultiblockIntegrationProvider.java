package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockDataCoreProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockRole;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockScanProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTerminalProvider;
import com.knoxhack.echomultiblockcore.api.RobotState;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echomultiblockcore.runtime.MultiblockSavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DefaultMultiblockIntegrationProvider {
    public static final MultiblockTerminalProvider TERMINAL = new TerminalProvider();
    public static final MultiblockScanProvider SCAN = new ScanProvider();
    public static final MultiblockDataCoreProvider DATA_CORE = new DataCoreProvider();
    public static final MultiblockMapMarkerProvider MAP_MARKERS = new MapMarkerProvider();

    private DefaultMultiblockIntegrationProvider() {
    }

    public static List<MultiblockStatusSnapshot> statusSnapshots(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        List<MultiblockStatusSnapshot> snapshots = new ArrayList<>();
        for (MultiblockSavedData.Entry entry : MultiblockSavedData.get(level).entries()) {
            if (level.isLoaded(entry.controllerPos())
                    && level.getBlockEntity(entry.controllerPos()) instanceof MultiblockControllerBlockEntity controller) {
                snapshots.add(controller.statusSnapshot());
            } else {
                snapshots.add(statusFromEntry(entry));
            }
        }
        return List.copyOf(snapshots);
    }

    public static List<MultiblockRuntimeSnapshot> runtimeSnapshots(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        List<MultiblockRuntimeSnapshot> snapshots = new ArrayList<>();
        for (MultiblockSavedData.Entry entry : MultiblockSavedData.get(level).entries()) {
            if (level.isLoaded(entry.controllerPos())
                    && level.getBlockEntity(entry.controllerPos()) instanceof MultiblockControllerBlockEntity controller) {
                snapshots.add(controller.runtimeSnapshot());
            } else {
                snapshots.add(runtimeFromEntry(level, entry));
            }
        }
        return List.copyOf(snapshots);
    }

    public static List<MultiblockMapMarkerSnapshot> markerSnapshots(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return runtimeSnapshots(level).stream()
                .map(DefaultMultiblockIntegrationProvider::markerFromRuntime)
                .toList();
    }

    private static MultiblockStatusSnapshot statusFromEntry(MultiblockSavedData.Entry entry) {
        Optional<MultiblockDefinition> definition = MultiblockContent.definition(entry.definitionId());
        Optional<MultiblockProgressionDefinition> progression = MultiblockProgressionRegistry.byFacility(entry.definitionId());
        MultiblockState state = state(entry.state());
        return new MultiblockStatusSnapshot(
                entry.definitionId(),
                definition.map(MultiblockDefinition::displayName).orElse(entry.definitionId().getPath()),
                state,
                entry.integrity(),
                state == MultiblockState.OFFLINE || state == MultiblockState.INCOMPLETE ? 0.0D : 1.0D,
                entry.controllerPos(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Saved runtime snapshot; controller chunk is not currently loaded."),
                progression.map(MultiblockProgressionDefinition::title).orElse(""),
                progression.map(value -> "T" + value.tier() + " // " + value.featuredRecipeSummary()).orElse(""),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                definition.map(MultiblockDefinition::facilityStage).orElse(""),
                definition.map(MultiblockDefinition::facilityRoute).orElse(""),
                definition.map(MultiblockDefinition::primaryUse).orElse(""),
                state == MultiblockState.OFFLINE || state == MultiblockState.INCOMPLETE ? "structure" : "",
                null,
                "",
                integrationHints(definition.orElse(null), progression));
    }

    private static MultiblockRuntimeSnapshot runtimeFromEntry(ServerLevel level, MultiblockSavedData.Entry entry) {
        Optional<MultiblockDefinition> definition = MultiblockContent.definition(entry.definitionId());
        MultiblockDefinition def = definition.orElse(null);
        Optional<MultiblockProgressionDefinition> progression = MultiblockProgressionRegistry.byFacility(entry.definitionId());
        MultiblockState state = state(entry.state());
        List<String> warnings = List.of("Saved runtime snapshot; controller chunk is not currently loaded.");
        return new MultiblockRuntimeSnapshot(
                entry.definitionId(),
                entry.controllerPos(),
                state,
                entry.integrity(),
                state == MultiblockState.OFFLINE || state == MultiblockState.INCOMPLETE ? 0.0D : 1.0D,
                def == null ? 0 : def.volume(),
                0,
                List.of(),
                warnings,
                level.getGameTime(),
                level.dimension(),
                def == null ? entry.definitionId().getPath() : def.displayName(),
                def == null ? "general" : def.category(),
                def == null ? MultiblockRole.INFRASTRUCTURE : def.role(),
                def == null ? 0xFF00D8FF : def.previewColor(),
                0,
                warnings.size(),
                MultiblockCapabilityRuntime.EMPTY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                progression.map(MultiblockProgressionDefinition::id).orElse(null),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                progression.map(MultiblockProgressionDefinition::title).orElse(""),
                progression.map(MultiblockProgressionDefinition::featuredRecipeSummary).orElse(""),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                def == null ? "" : def.facilityStage(),
                def == null ? "" : def.facilityRoute(),
                def == null ? "" : def.primaryUse(),
                state == MultiblockState.OFFLINE || state == MultiblockState.INCOMPLETE ? "structure" : "",
                null,
                "",
                integrationHints(def, progression));
    }

    private static MultiblockMapMarkerSnapshot markerFromRuntime(MultiblockRuntimeSnapshot snapshot) {
        Identifier markerId = EchoMultiblockCore.id("marker/" + snapshot.definitionId().getNamespace()
                + "/" + snapshot.definitionId().getPath() + "/" + Long.toUnsignedString(snapshot.controllerPos().asLong()));
        String summary = snapshot.role().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " / " + snapshot.state()
                + " / integrity " + Math.round(snapshot.integrity()) + "%"
                + (snapshot.progressionTier() > 0 ? " / tier " + snapshot.progressionTier() : "")
                + (snapshot.warningCount() > 0 ? " / warnings " + snapshot.warningCount() : "");
        return new MultiblockMapMarkerSnapshot(
                markerId,
                snapshot.definitionId(),
                snapshot.controllerPos(),
                snapshot.dimension(),
                snapshot.role(),
                snapshot.state(),
                snapshot.markerColor(),
                snapshot.progressionTitle().isBlank() ? snapshot.displayName() : snapshot.progressionTitle(),
                summary
                        + (snapshot.facilityRoute().isBlank() ? "" : " / route " + snapshot.facilityRoute())
                        + (snapshot.blockedReasonCode().isBlank() ? "" : " / blocked " + snapshot.blockedReasonCode()));
    }

    private static List<String> integrationHints(MultiblockDefinition definition, Optional<MultiblockProgressionDefinition> progression) {
        List<String> hints = new ArrayList<>();
        if (definition != null) {
            hints.addAll(definition.integrationHints());
            if (!definition.facilityRoute().isBlank()) {
                hints.add("Route: " + definition.facilityRoute());
            }
            if (!definition.primaryUse().isBlank()) {
                hints.add("Use: " + definition.primaryUse());
            }
        }
        progression.ifPresent(value -> hints.add("Progression: T" + value.tier() + " " + value.title()));
        return List.copyOf(hints);
    }

    private static Optional<LensMultiblockScan> scan(Level level, BlockPos pos) {
        if (level == null || pos == null || level.isClientSide() || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
            return Optional.of(controller.scanSnapshot());
        }
        if (blockEntity instanceof RoboticArmBlockEntity arm) {
            return Optional.of(robotScan(arm));
        }
        return Optional.empty();
    }

    private static LensMultiblockScan robotScan(RoboticArmBlockEntity arm) {
        RobotState robotState = arm.getRobotState();
        MultiblockState state = switch (robotState) {
            case MOVING, WORKING -> MultiblockState.ACTIVE;
            case COOLING -> MultiblockState.PAUSED;
            case JAMMED -> MultiblockState.JAMMED;
            case DAMAGED -> MultiblockState.DAMAGED;
            case OFFLINE -> MultiblockState.OFFLINE;
            case IDLE -> MultiblockState.FORMED;
        };
        return new LensMultiblockScan(
                arm.getRobotId(),
                "Robotic Arm",
                state,
                arm.installedTool().isEmpty() ? 0.5D : 1.0D,
                arm.getBlockPos(),
                List.of(),
                List.of(arm.statusComponent().getString()),
                List.of());
    }

    private static MultiblockState state(String raw) {
        try {
            return MultiblockState.valueOf(raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return MultiblockState.FORMED;
        }
    }

    private static final class TerminalProvider implements MultiblockTerminalProvider {
        @Override
        public Identifier providerId() {
            return EchoMultiblockCore.id("default_terminal_provider");
        }

        @Override
        public List<MultiblockStatusSnapshot> snapshots(Player player) {
            return player instanceof ServerPlayer serverPlayer
                    ? statusSnapshots((ServerLevel) serverPlayer.level())
                    : List.of();
        }
    }

    private static final class ScanProvider implements MultiblockScanProvider {
        @Override
        public Identifier providerId() {
            return EchoMultiblockCore.id("default_scan_provider");
        }

        @Override
        public Optional<LensMultiblockScan> scan(Player player, Level level, BlockPos pos) {
            return DefaultMultiblockIntegrationProvider.scan(level, pos);
        }
    }

    private static final class DataCoreProvider implements MultiblockDataCoreProvider {
        @Override
        public Identifier providerId() {
            return EchoMultiblockCore.id("default_data_core_provider");
        }

        @Override
        public List<MultiblockRuntimeSnapshot> snapshots(Player player) {
            return player instanceof ServerPlayer serverPlayer
                    ? runtimeSnapshots((ServerLevel) serverPlayer.level())
                    : List.of();
        }
    }

    private static final class MapMarkerProvider implements MultiblockMapMarkerProvider {
        @Override
        public Identifier providerId() {
            return EchoMultiblockCore.id("default_map_marker_provider");
        }

        @Override
        public List<MultiblockMapMarkerSnapshot> markers(Player player) {
            return player instanceof ServerPlayer serverPlayer
                    ? markerSnapshots((ServerLevel) serverPlayer.level())
                    : List.of();
        }

        @Override
        public boolean refresh(ServerPlayer player, String reason) {
            return player != null;
        }
    }
}

package com.knoxhack.echolens.registry;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.LensTargetKind;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.integration.LensMissionHooks;
import com.knoxhack.echolens.integration.LensWorldCoreScanBridge;
import com.knoxhack.echolens.integration.runtimeguard.LensRuntimeGuardHooks;
import com.knoxhack.echolens.network.LensScanRequestPacket;
import com.knoxhack.echolens.network.LensScanResponsePacket;
import com.knoxhack.echolens.network.LensServerScanStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LensServerScanService {
    private LensServerScanService() {
    }

    public static LensScanResponsePacket scan(ServerPlayer player, LensScanRequestPacket request) {
        if (player == null || request == null) {
            return unavailable(0, "", "No player or request.");
        }
        String signature = targetSignature(request);
        if (!LensConfig.bool(LensConfig.SERVER_DEEP_SCAN_ENABLED, true)) {
            return unavailable(request.requestId(), signature, "Server Deep Scan disabled.");
        }
        if (request.scanMode() != LensScanMode.DEEP) {
            return unavailable(request.requestId(), signature, "Server scan only supports Deep Scan.");
        }
        if (!LensRuntimeGuardHooks.canRunDeepScan(player)) {
            return unavailable(request.requestId(), signature, "RuntimeGuard throttled Deep Scan; retry shortly.");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return unavailable(request.requestId(), signature, "No server level.");
        }

        Validation validation = validate(player, level, request);
        if (!validation.accepted()) {
            return LensScanResponsePacket.of(request.requestId(), validation.status(), signature, validation.sections(),
                    validation.message());
        }

        LensContext context = validation.context();
        List<LensInfoSection> sections = new ArrayList<>(validation.sections());
        sections.addAll(collectProviderSections(context,
                LensRuntimeGuardHooks.deepScanBudget(player, LensProviderRegistry.serverProviders().size())));
        LensServerScanStatus status = validation.redacted()
                ? LensServerScanStatus.REDACTED
                : LensServerScanStatus.VERIFIED;
        LensRuntimeGuardHooks.recordDeepScan(player,
                request.targetKind() == LensTargetKind.ENTITY ? 0 : 1,
                request.targetKind() == LensTargetKind.ENTITY ? 1 : 0);
        LensWorldCoreScanBridge.recordVerifiedScan(player, context, status);
        LensMissionHooks.recordVerifiedDeepScan(player, request.targetKind().name());
        if (request.targetKind() == LensTargetKind.BLOCK) {
            LensMissionHooks.recordMachineDiagnostic(player, request.targetId() == null ? "block" : request.targetId().toString());
            LensMissionHooks.recordActualBlockScan(player, request.targetId());
        }
        return LensScanResponsePacket.of(request.requestId(), status, signature, sections,
                validation.redacted() ? "Private target data redacted." : "Server scan verified.");
    }

    private static List<LensInfoSection> collectProviderSections(LensContext context, int providerBudget) {
        List<LensInfoSection> sections = new ArrayList<>();
        List<LensInfoRow> signalRows = new ArrayList<>();
        List<LensInfoSection> providerSections = new ArrayList<>();
        int inspectedProviders = 0;
        for (ServerLensProvider provider : LensProviderRegistry.serverProviders()) {
            if (inspectedProviders >= providerBudget) {
                break;
            }
            inspectedProviders++;
            try {
                if (!provider.supports(context)) {
                    continue;
                }
                List<LensInfoRow> signals = provider.deepScanSignals(context);
                if (signals != null) {
                    for (LensInfoRow row : signals) {
                        if (row != null && row.visibleIn(LensScanMode.DEEP)) {
                            signalRows.add(row);
                        }
                    }
                }
                List<LensInfoSection> provided = provider.inspect(context);
                if (provided == null) {
                    continue;
                }
                LensProviderRegistry.recordProviderSuccess(provider);
                for (LensInfoSection section : LensReportSanitizer.normalizeServer(provided)) {
                    if (section != null && section.visibleIn(LensScanMode.DEEP)) {
                        providerSections.add(serverVisible(section));
                    }
                }
            } catch (RuntimeException exception) {
                LensProviderRegistry.recordProviderFailure(provider, exception);
                EchoLens.LOGGER.warn("Lens server provider {} failed; continuing without its output.",
                        provider.id(), exception);
            }
        }
        if (!signalRows.isEmpty()) {
            sections.add(signalSection(signalRows));
        }
        sections.addAll(providerSections);
        return LensReportSanitizer.normalizeServer(sections);
    }

    private static Validation validate(ServerPlayer player, ServerLevel level, LensScanRequestPacket request) {
        double maxDistance = LensConfig.decimal(LensConfig.SERVER_SCAN_DISTANCE, 24.0D);
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        if (request.targetKind() == LensTargetKind.BLOCK || request.targetKind() == LensTargetKind.FLUID) {
            BlockPos pos = request.blockPos();
            if (pos == null || !level.isLoaded(pos)) {
                return Validation.rejected("Target block is unavailable.", LensServerScanStatus.UNAVAILABLE);
            }
            if (Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition()) > maxDistance * maxDistance) {
                return Validation.rejected("Target is out of scan range.", LensServerScanStatus.UNAVAILABLE);
            }
            if (!canSeeBlock(player, pos, maxDistance)) {
                return Validation.rejected("Target is not in clear scan view.", LensServerScanStatus.UNAVAILABLE);
            }
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState().isEmpty() ? level.getFluidState(pos) : state.getFluidState();
            Identifier actualId = request.targetKind() == LensTargetKind.FLUID
                    ? BuiltInRegistries.FLUID.getKey(fluid.getType())
                    : BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (request.targetKind() == LensTargetKind.BLOCK && state.isAir()) {
                return Validation.rejected("Target block no longer exists.", LensServerScanStatus.UNAVAILABLE);
            }
            if (request.targetKind() == LensTargetKind.FLUID && fluid.isEmpty()) {
                return Validation.rejected("Target fluid no longer exists.", LensServerScanStatus.UNAVAILABLE);
            }
            if (request.targetId() != null && actualId != null && !request.targetId().equals(actualId)) {
                return Validation.rejected("Target changed before server scan completed.", LensServerScanStatus.STALE);
            }
            LensContext context = request.targetKind() == LensTargetKind.FLUID
                    ? new LensContext(player, level, LensTargetKind.FLUID, pos, state, fluid, null,
                            LensScanMode.DEEP, policy)
                    : LensContext.block(player, level, pos, state, fluid, LensScanMode.DEEP, policy);
            boolean redacted = redactsPrivateTarget(context);
            return Validation.accepted(context, redacted);
        }
        if (request.targetKind() == LensTargetKind.ENTITY) {
            Entity entity = level.getEntity(request.entityId());
            if (entity == null || !entity.isAlive()) {
                return Validation.rejected("Target entity is unavailable.", LensServerScanStatus.UNAVAILABLE);
            }
            if (entity.distanceToSqr(player) > maxDistance * maxDistance) {
                return Validation.rejected("Target is out of scan range.", LensServerScanStatus.UNAVAILABLE);
            }
            Identifier actualId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (request.targetId() != null && actualId != null && !request.targetId().equals(actualId)) {
                return Validation.rejected("Target changed before server scan completed.", LensServerScanStatus.STALE);
            }
            if (!player.hasLineOfSight(entity)) {
                return Validation.rejected("Target is not in clear scan view.", LensServerScanStatus.UNAVAILABLE);
            }
            LensContext context = LensContext.entity(player, level, entity, LensScanMode.DEEP, policy);
            return Validation.accepted(context, redactsPrivateTarget(context));
        }
        return Validation.rejected("No valid target.", LensServerScanStatus.UNAVAILABLE);
    }

    private static boolean redactsPrivateTarget(LensContext context) {
        if (!LensConfig.bool(LensConfig.SERVER_REDACT_PROTECTED_TARGETS, true)) {
            return false;
        }
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        if (policy != LensAccessPolicy.PUBLIC_ONLY) {
            return false;
        }
        if (context.hasBlock()) {
            BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
            return blockEntity instanceof Container;
        }
        return context.hasEntity() && context.entity() instanceof Container;
    }

    private static boolean canSeeBlock(ServerPlayer player, BlockPos pos, double maxDistance) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        if (center.subtract(eye).normalize().dot(player.getLookAngle().normalize()) < 0.15D) {
            return false;
        }
        BlockHitResult result = player.level().clip(new ClipContext(
                eye,
                eye.add(player.getLookAngle().normalize().scale(maxDistance)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        return result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(pos);
    }

    private static LensInfoSection serverVisible(LensInfoSection section) {
        List<LensInfoRow> rows = LensInspectionService.visibleRows(section, LensScanMode.DEEP);
        return new LensInfoSection(section.id(), section.category(), section.title(), section.icon(), section.tone(),
                LensVisibility.DEEP, rows);
    }

    private static LensInfoSection signalSection(List<LensInfoRow> rows) {
        return LensInfoSection.of(EchoLens.id("section/server_scan_signals"), LensDataCategory.INTEGRATION,
                "Scan Signals", "S", LensTone.ECHO, LensVisibility.DEEP, rows);
    }

    private static LensScanResponsePacket unavailable(int requestId, String signature, String message) {
        return LensScanResponsePacket.of(requestId, LensServerScanStatus.UNAVAILABLE, signature,
                List.of(statusSection(LensServerScanStatus.UNAVAILABLE, message)), message);
    }

    private static LensInfoSection statusSection(LensServerScanStatus status, String message) {
        LensTone tone = switch (status) {
            case VERIFIED -> LensTone.GOOD;
            case REDACTED -> LensTone.WARNING;
            case STALE, UNAVAILABLE -> LensTone.MUTED;
            case LOCAL, QUERYING -> LensTone.INFO;
        };
        return LensInfoSection.of(EchoLens.id("section/server_scan_status"), LensDataCategory.INTEGRATION,
                "Server Scan", "S", tone, LensVisibility.DEEP,
                List.of(LensInfoRow.of("Status", message == null || message.isBlank() ? status.name() : message,
                        "S", tone, LensVisibility.DEEP)));
    }

    public static String targetSignature(LensScanRequestPacket request) {
        if (request == null) {
            return "";
        }
        return switch (request.targetKind()) {
            case BLOCK, FLUID -> request.targetKind() + ":" + request.blockPos() + ":" + request.targetId();
            case ENTITY -> request.targetKind() + ":" + request.entityId() + ":" + request.targetId();
            case MISS -> "MISS";
        };
    }

    private record Validation(
            boolean accepted,
            boolean redacted,
            LensServerScanStatus status,
            LensContext context,
            List<LensInfoSection> sections,
            String message) {
        static Validation accepted(LensContext context, boolean redacted) {
            List<LensInfoSection> sections = redacted
                    ? List.of(statusSection(LensServerScanStatus.REDACTED, "Private target data redacted."))
                    : List.of(statusSection(LensServerScanStatus.VERIFIED, "Server target verified."));
            return new Validation(true, redacted,
                    redacted ? LensServerScanStatus.REDACTED : LensServerScanStatus.VERIFIED,
                    context, sections, redacted ? "Private target data redacted." : "Server target verified.");
        }

        static Validation rejected(String message, LensServerScanStatus status) {
            return new Validation(false, false, status, null, List.of(statusSection(status, message)), message);
        }
    }
}

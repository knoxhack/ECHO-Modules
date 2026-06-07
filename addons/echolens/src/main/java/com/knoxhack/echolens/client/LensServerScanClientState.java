package com.knoxhack.echolens.client;

import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.LensTargetKind;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.integration.LensSoundFeedback;
import com.knoxhack.echolens.network.LensScanRequestPacket;
import com.knoxhack.echolens.network.LensScanResponsePacket;
import com.knoxhack.echolens.network.LensServerScanStatus;
import com.knoxhack.echolens.registry.LensServerScanService;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

public final class LensServerScanClientState {
    private static int nextRequestId = 1;
    private static int activeRequestId;
    private static String activeSignature = "";
    private static long requestTick;
    private static long responseTick;
    private static LensServerScanStatus status = LensServerScanStatus.LOCAL;
    private static String message = "";
    private static List<LensInfoSection> sections = List.of();

    private LensServerScanClientState() {
    }

    public static void update(LensContext context) {
        long now = gameTime();
        if (context == null || context.scanMode() != LensScanMode.DEEP) {
            status = LensServerScanStatus.LOCAL;
            sections = List.of();
            message = "";
            return;
        }
        if (!LensConfig.bool(LensConfig.SERVER_DEEP_SCAN_ENABLED, true)) {
            status = LensServerScanStatus.LOCAL;
            sections = List.of();
            message = "Server Deep Scan disabled.";
            return;
        }
        LensScanRequestPacket request = request(context);
        String signature = LensServerScanService.targetSignature(request);
        int cacheTicks = LensConfig.integer(LensConfig.SERVER_DEEP_SCAN_CACHE_TICKS, 20);
        int timeoutTicks = LensConfig.integer(LensConfig.SERVER_DEEP_SCAN_TIMEOUT_TICKS, 40);
        if (signature.equals(activeSignature)) {
            if ((status == LensServerScanStatus.VERIFIED || status == LensServerScanStatus.REDACTED)
                    && now - responseTick <= cacheTicks) {
                return;
            }
            if (status == LensServerScanStatus.QUERYING && now - requestTick <= timeoutTicks) {
                return;
            }
        }
        activeSignature = signature;
        activeRequestId = nextRequestId++;
        requestTick = now;
        status = LensServerScanStatus.QUERYING;
        sections = List.of(statusSection("Querying server...", LensTone.INFO));
        message = "Querying server...";
        LensSoundFeedback.play(LensSoundFeedback.SCAN_START);
        LensScanRequestPacket outbound = new LensScanRequestPacket(activeRequestId, request.scanMode(),
                request.targetKind(), request.blockPos(), request.entityId(), request.targetId());
        if (!EchoNetClientActions.trySendServerboundAction(outbound)) {
            status = LensServerScanStatus.UNAVAILABLE;
            sections = List.of(statusSection("Server scan unavailable.", LensTone.MUTED));
            message = "Server scan unavailable.";
        }
    }

    public static void apply(LensScanResponsePacket packet) {
        if (packet == null || packet.requestId() != activeRequestId) {
            return;
        }
        if (!packet.targetSignature().isBlank() && !packet.targetSignature().equals(activeSignature)) {
            return;
        }
        status = packet.status();
        sections = packet.toSections();
        message = packet.message();
        responseTick = gameTime();
        LensSoundFeedback.playStatus(status);
        if (sections.isEmpty()) {
            sections = List.of(statusSection(message.isBlank() ? status.name() : message, tone(status)));
        }
    }

    public static List<LensInfoSection> sections() {
        if (!LensConfig.bool(LensConfig.SHOW_SERVER_SCAN_STATUS, true)) {
            return status == LensServerScanStatus.VERIFIED || status == LensServerScanStatus.REDACTED
                    ? sections
                    : List.of();
        }
        return sections;
    }

    public static LensServerScanStatus status() {
        return status;
    }

    public static String statusLabel() {
        return switch (status) {
            case LOCAL -> "Local";
            case QUERYING -> "Querying";
            case VERIFIED -> "Verified";
            case REDACTED -> "Redacted";
            case UNAVAILABLE -> "Unavailable";
            case STALE -> "Stale";
        };
    }

    private static LensScanRequestPacket request(LensContext context) {
        Identifier targetId = targetId(context);
        int entityId = context.hasEntity() ? context.entity().getId() : -1;
        return new LensScanRequestPacket(0, context.scanMode(), context.targetKind(), context.blockPos(),
                entityId, targetId);
    }

    private static Identifier targetId(LensContext context) {
        if (context.hasEntity()) {
            Entity entity = context.entity();
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }
        if (context.hasBlock() && context.targetKind() != LensTargetKind.FLUID) {
            return context.blockState().getBlock() == Blocks.AIR
                    ? null
                    : BuiltInRegistries.BLOCK.getKey(context.blockState().getBlock());
        }
        if (context.hasFluid()) {
            return BuiltInRegistries.FLUID.getKey(context.fluidState().getType());
        }
        return null;
    }

    private static LensInfoSection statusSection(String value, LensTone tone) {
        return LensInfoSection.of(EchoLens.id("section/client_server_scan"), LensDataCategory.INTEGRATION,
                "Server Scan", "S", tone, LensVisibility.DEEP,
                List.of(LensInfoRow.of("Status", value, "S", tone, LensVisibility.DEEP)));
    }

    private static LensTone tone(LensServerScanStatus status) {
        return switch (status) {
            case VERIFIED -> LensTone.GOOD;
            case REDACTED -> LensTone.WARNING;
            case STALE, UNAVAILABLE -> LensTone.MUTED;
            case LOCAL, QUERYING -> LensTone.INFO;
        };
    }

    private static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
}

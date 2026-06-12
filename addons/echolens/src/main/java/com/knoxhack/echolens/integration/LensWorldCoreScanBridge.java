package com.knoxhack.echolens.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensTargetKind;
import com.knoxhack.echolens.network.LensServerScanStatus;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Routes verified Lens scans into WorldCore marker/discovery state.
 */
public final class LensWorldCoreScanBridge {
    private LensWorldCoreScanBridge() {
    }

    public static boolean recordVerifiedScan(ServerPlayer player, LensContext context, LensServerScanStatus status) {
        if (player == null || context == null || context.blockPos() == null
                || (status != LensServerScanStatus.VERIFIED && status != LensServerScanStatus.REDACTED)) {
            return false;
        }
        Identifier targetId = targetId(context);
        if (targetId == null) {
            return false;
        }
        Identifier scanId = scanId(context.targetKind(), targetId);
        return EchoCoreServices.structureDiscoveryService().recordStructureScan(
                player,
                scanId,
                context.blockPos(),
                displayName(context.targetKind(), targetId),
                summary(context.targetKind(), targetId, status));
    }

    private static Identifier targetId(LensContext context) {
        if (context.targetKind() == LensTargetKind.FLUID && context.hasFluid()) {
            return BuiltInRegistries.FLUID.getKey(context.fluidState().getType());
        }
        if (context.targetKind() == LensTargetKind.BLOCK && context.hasBlock() && !context.blockState().isAir()) {
            return BuiltInRegistries.BLOCK.getKey(context.blockState().getBlock());
        }
        return null;
    }

    private static Identifier scanId(LensTargetKind kind, Identifier targetId) {
        String type = kind == LensTargetKind.FLUID ? "fluid" : "block";
        return EchoLens.id("scan/" + type + "/" + targetId.getNamespace() + "/" + sanitize(targetId.getPath()));
    }

    private static String displayName(LensTargetKind kind, Identifier targetId) {
        String prefix = kind == LensTargetKind.FLUID ? "Fluid Scan" : "Block Scan";
        return prefix + ": " + readable(targetId.getPath());
    }

    private static String summary(LensTargetKind kind, Identifier targetId, LensServerScanStatus status) {
        String targetType = kind == LensTargetKind.FLUID ? "fluid" : "block";
        if (status == LensServerScanStatus.REDACTED) {
            return "Lens Deep Scan verified this " + targetType
                    + " location, but private target details were redacted.";
        }
        return "Lens Deep Scan verified " + targetId + " and published the location to WorldCore/HoloMap.";
    }

    private static String readable(String value) {
        String clean = value == null ? "target" : value.replace('/', '_').replace('-', '_');
        StringBuilder label = new StringBuilder();
        for (String part : clean.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                label.append(part.substring(1));
            }
        }
        return label.isEmpty() ? "Target" : label.toString();
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}

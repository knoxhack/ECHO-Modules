package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.config.LensConfig;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum ServerPrivacyProvider implements ServerLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("server_privacy");
    }

    @Override
    public int priority() {
        return 265;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.INVENTORY;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && (context.hasBlock() || context.hasEntity());
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        boolean privateTarget = privateContainer(context);
        LensTone tone = privateTarget && policy == LensAccessPolicy.PUBLIC_ONLY ? LensTone.WARNING : LensTone.GOOD;
        return List.of(LensInfoSection.of(EchoLens.id("section/server_privacy"), LensDataCategory.INVENTORY,
                "Server Privacy", "P", tone, LensVisibility.DEEP, List.of(
                        LensInfoRow.of("Policy", policy.name(), "P", tone, LensVisibility.DEEP),
                        LensInfoRow.of("Contents", privateTarget ? "Hidden by server" : "No private contents exposed",
                                "C", privateTarget ? LensTone.WARNING : LensTone.GOOD, LensVisibility.DEEP),
                        LensInfoRow.of("Protection", LensConfig.bool(LensConfig.SERVER_REDACT_PROTECTED_TARGETS, true)
                                        ? "Redaction enabled" : "Redaction disabled",
                                "R", LensTone.INFO, LensVisibility.DEEP))));
    }

    @Override
    public List<LensInfoRow> deepScanSignals(LensContext context) {
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        boolean privateTarget = privateContainer(context);
        if (!privateTarget) {
            return List.of(LensInfoRow.of("Privacy", "Public target", "P", LensTone.GOOD, LensVisibility.DEEP));
        }
        boolean redacted = policy == LensAccessPolicy.PUBLIC_ONLY
                && LensConfig.bool(LensConfig.SERVER_REDACT_PROTECTED_TARGETS, true);
        return List.of(LensInfoRow.of("Privacy", redacted ? "Redacted" : "Protected",
                "P", redacted ? LensTone.WARNING : LensTone.INFO, LensVisibility.DEEP));
    }

    private static boolean privateContainer(LensContext context) {
        if (context.hasBlock()) {
            BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
            if (blockEntity instanceof Container) {
                return true;
            }
        }
        return context.hasEntity() && context.entity() instanceof Container;
    }
}

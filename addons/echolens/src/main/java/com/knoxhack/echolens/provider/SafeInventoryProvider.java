package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.InventoryLensProvider;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.config.LensConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum SafeInventoryProvider implements InventoryLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("safe_inventory");
    }

    @Override
    public int priority() {
        return 240;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && ((context.hasBlock()
                && context.level().getBlockEntity(context.blockPos()) instanceof Container)
                || (context.hasEntity() && context.entity() instanceof Container));
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        Container container = container(context);
        if (container == null) {
            return List.of();
        }
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(LensInfoRow.of("Privacy", privacyText(policy), "P", privacyTone(policy), LensVisibility.COMPACT));
        if (policy != LensAccessPolicy.PUBLIC_ONLY) {
            rows.add(LensInfoRow.of("Capacity", container.getContainerSize() + " slots", "S",
                    LensTone.INFO, LensVisibility.EXPANDED));
        }
        rows.add(LensInfoRow.of("Contents", "Redacted by Lens", "R", LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/inventory"), LensDataCategory.INVENTORY,
                "Inventory", "I", LensTone.MUTED, LensVisibility.COMPACT, rows));
    }

    private static Container container(LensContext context) {
        if (context.hasBlock()) {
            BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
            if (blockEntity instanceof Container container) {
                return container;
            }
        }
        Entity entity = context.entity();
        return entity instanceof Container container ? container : null;
    }

    private static String privacyText(LensAccessPolicy policy) {
        return switch (policy) {
            case PUBLIC_ONLY -> "Protected";
            case PROTECTED_SUMMARY -> "Public summary";
            case ALLOW_DETAILED -> "Detailed status allowed";
        };
    }

    private static LensTone privacyTone(LensAccessPolicy policy) {
        return switch (policy) {
            case PUBLIC_ONLY -> LensTone.WARNING;
            case PROTECTED_SUMMARY -> LensTone.INFO;
            case ALLOW_DETAILED -> LensTone.GOOD;
        };
    }
}

package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.MachineLensProvider;
import com.knoxhack.echolens.config.LensConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum MachineStatusProvider implements MachineLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("machine_status");
    }

    @Override
    public int priority() {
        return 220;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.hasBlock()
                && LensConfig.bool(LensConfig.MACHINE_STATUS_VISIBILITY, true)
                && context.level().getBlockEntity(context.blockPos()) != null;
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
        if (blockEntity == null) {
            return List.of();
        }
        List<LensInfoRow> rows = new ArrayList<>();
        Identifier typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        rows.add(LensInfoRow.of("Machine", typeId == null ? "unknown" : typeId.toString(), "▤",
                LensTone.INFO, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Interface", blockEntity instanceof MenuProvider ? "Accessible" : "No menu",
                "◫", blockEntity instanceof MenuProvider ? LensTone.GOOD : LensTone.MUTED, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Status", "Public diagnostics only", "⎋", LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/machine"), LensDataCategory.MACHINE, "Machine",
                "▤", LensTone.INFO, LensVisibility.EXPANDED, rows));
    }
}

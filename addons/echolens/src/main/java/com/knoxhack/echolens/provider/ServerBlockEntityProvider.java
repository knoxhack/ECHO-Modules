package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum ServerBlockEntityProvider implements ServerLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("server_block_entity");
    }

    @Override
    public int priority() {
        return 260;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.MACHINE;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.hasBlock();
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
        if (blockEntity == null) {
            return List.of();
        }
        Identifier typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(LensInfoRow.of("Verified", "Server block entity", "S", LensTone.GOOD, LensVisibility.DEEP));
        rows.add(LensInfoRow.of("Type", typeId == null ? "unknown" : typeId.toString(), "T",
                LensTone.INFO, LensVisibility.DEEP));
        rows.add(LensInfoRow.of("Position", blockEntity.getBlockPos().toShortString(), "P",
                LensTone.MUTED, LensVisibility.DEEP));
        rows.add(LensInfoRow.of("Interface", blockEntity instanceof MenuProvider ? "Menu available" : "No menu",
                "I", blockEntity instanceof MenuProvider ? LensTone.GOOD : LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/server_block_entity"), LensDataCategory.MACHINE,
                "Server Block Entity", "S", LensTone.INFO, LensVisibility.DEEP, rows));
    }

    @Override
    public List<LensInfoRow> deepScanSignals(LensContext context) {
        BlockEntity blockEntity = context.level().getBlockEntity(context.blockPos());
        return List.of(LensInfoRow.of("Block Entity",
                blockEntity == null ? "None detected" : "Verified",
                "S", blockEntity == null ? LensTone.MUTED : LensTone.GOOD, LensVisibility.DEEP));
    }
}

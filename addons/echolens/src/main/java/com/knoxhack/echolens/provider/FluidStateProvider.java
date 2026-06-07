package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.FluidLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;

public enum FluidStateProvider implements FluidLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("fluid_state");
    }

    @Override
    public int priority() {
        return 140;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.FLUID;
    }

    @Override
    public List<LensInfoSection> inspectFluid(LensContext context, FluidState state) {
        Identifier id = BuiltInRegistries.FLUID.getKey(state.getType());
        boolean bucket = state.getType().getBucket() != Items.AIR;
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(LensInfoRow.of("Fluid", id == null ? "unknown" : id.toString(), "~", LensTone.INFO,
                LensVisibility.COMPACT));
        rows.add(LensInfoRow.of("Source", state.isSource() ? "Yes" : "Flowing", "O",
                state.isSource() ? LensTone.GOOD : LensTone.NEUTRAL, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Level", Integer.toString(state.getAmount()), "L", LensTone.NEUTRAL,
                LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Bucket", bucket ? "Available" : "Unavailable", "B",
                bucket ? LensTone.GOOD : LensTone.MUTED, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Public Scan", "Fluid state only", "P", LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/fluid"), LensDataCategory.FLUID, "Fluid",
                "~", LensTone.INFO, LensVisibility.COMPACT, rows));
    }
}

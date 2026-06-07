package com.knoxhack.echolens.provider;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.WorldContextSnapshot;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.IntegrationLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public enum WorldContextProvider implements IntegrationLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("world_context");
    }

    @Override
    public int priority() {
        return 680;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.player() != null;
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        List<LensInfoRow> rows = new ArrayList<>();
        WorldContextSnapshot snapshot = EchoCoreServices.worldContext(context.player());
        snapshot.currentRegionOptional().ifPresent(region -> {
            rows.add(row("Region", region.displayName(), "R", LensTone.ECHO, LensVisibility.DEEP));
            rows.add(row("Region Type", region.type().name(), "T", LensTone.INFO, LensVisibility.DEEP));
        });
        var hazard = snapshot.hazard();
        rows.add(row("Hazard", hazard.safeZone() ? "Safe" : hazard.summary(), "H",
                hazard.safeZone() ? LensTone.GOOD : LensTone.WARNING, LensVisibility.DEEP));
        rows.add(row("Severity", Integer.toString(hazard.severity()), "!",
                hazard.severity() > 0 ? LensTone.WARNING : LensTone.MUTED, LensVisibility.DEEP));
        int markers = snapshot.nearbyMarkers().size();
        rows.add(row("Map Markers", Integer.toString(markers), "M",
                markers > 0 ? LensTone.INFO : LensTone.MUTED, LensVisibility.DEEP));
        rows.add(row("Regions", Integer.toString(snapshot.activeRegions().size()), "A",
                snapshot.activeRegions().isEmpty() ? LensTone.MUTED : LensTone.INFO, LensVisibility.DEEP));
        int routes = EchoCoreServices.routeRecords(context.player()).size();
        rows.add(row("Routes", Integer.toString(routes), ">", routes > 0 ? LensTone.INFO : LensTone.MUTED,
                LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/world_context"), LensDataCategory.INTEGRATION,
                "World Context", "W", LensTone.ECHO, LensVisibility.DEEP, rows));
    }

    private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return LensInfoRow.of(label, value == null || value.isBlank() ? "-" : value, icon, tone, visibility);
    }
}

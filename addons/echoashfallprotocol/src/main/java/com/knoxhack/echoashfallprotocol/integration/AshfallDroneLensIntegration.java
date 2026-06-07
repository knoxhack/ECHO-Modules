package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echolens.api.EntityLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public enum AshfallDroneLensIntegration implements EntityLensProvider {
    INSTANCE;

    public static void register() {
        LensProviderRegistry.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "companion_drone_diagnostics");
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.ENTITY;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.entity() instanceof EchoCompanionDrone;
    }

    @Override
    public List<LensInfoSection> inspectEntity(LensContext context, Entity entity) {
        if (!(entity instanceof EchoCompanionDrone drone)) {
            return List.of();
        }
        Player viewer = context.player();
        CompanionDroneData data = viewer == null ? new CompanionDroneData() : CompanionDroneStateStore.get(viewer);
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(row("Role", "Field Assistant", "R", LensTone.ECHO, LensVisibility.COMPACT));
        rows.add(row("Owner", ownerName(viewer, drone), "O", LensTone.INFO, LensVisibility.COMPACT));
        rows.add(row("Mode", data.getMode().displayName(), "M", LensTone.INFO, LensVisibility.COMPACT));
        rows.add(row("Task", data.getTaskLabel(), "T", LensTone.NEUTRAL, LensVisibility.COMPACT));
        rows.add(row("Battery", data.getBatteryPercent() + "%", "B",
                data.getBatteryPercent() <= 15 ? LensTone.WARNING : LensTone.GOOD, LensVisibility.COMPACT));
        rows.add(row("Signal", data.signalLabel(), "S",
                data.getSignalQuality() <= 25 ? LensTone.WARNING : LensTone.GOOD, LensVisibility.COMPACT));
        rows.add(row("Health", drone.getRepairLevel() + "%", "HP",
                drone.getRepairLevel() <= 30 ? LensTone.WARNING : LensTone.GOOD, LensVisibility.EXPANDED));
        rows.add(row("Upgrades", data.upgradesDisplay(), "U", LensTone.INFO, LensVisibility.EXPANDED));
        if (!data.getLastWarning().isBlank()) {
            rows.add(row("Warning", data.getLastWarning(), "!", LensTone.WARNING, LensVisibility.EXPANDED));
        }
        rows.add(row("Last Scan", data.getLastScanSummary(), "SC", LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(id(), LensDataCategory.ENTITY, "Companion Drone",
                "DR", LensTone.ECHO, LensVisibility.COMPACT, rows));
    }

    private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return LensInfoRow.of(label, value == null || value.isBlank() ? "Unknown" : value, icon, tone, visibility);
    }

    private static String ownerName(Player viewer, EchoCompanionDrone drone) {
        if (viewer != null && viewer.getUUID().equals(drone.getOwnerUUID())) {
            return viewer.getScoreboardName();
        }
        return drone.getOwnerUUID() == null ? "Unlinked" : "Linked operator";
    }
}

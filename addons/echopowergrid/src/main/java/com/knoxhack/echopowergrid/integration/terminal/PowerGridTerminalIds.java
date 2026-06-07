package com.knoxhack.echopowergrid.integration.terminal;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import net.minecraft.resources.Identifier;

public final class PowerGridTerminalIds {
    public static final Identifier TAB = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "power_grid");
    public static final Identifier CHAPTER = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "power_grid");
    public static final Identifier STATUS_ACTION = id("status");
    public static final String STATUS_SYNC = "powergrid_status_sync";

    private PowerGridTerminalIds() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, path);
    }
}

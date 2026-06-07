package com.knoxhack.echomultiblockcore.api;

import net.minecraft.resources.Identifier;

public record MultiblockCapability(Identifier id) {
    public static final MultiblockCapability POWER_INPUT = echo("power_input");
    public static final MultiblockCapability ITEM_INPUT = echo("item_input");
    public static final MultiblockCapability ITEM_OUTPUT = echo("item_output");
    public static final MultiblockCapability FLUID_INPUT = echo("fluid_input");
    public static final MultiblockCapability DATA = echo("data");
    public static final MultiblockCapability MISSION_PROVIDER = echo("mission_provider");
    public static final MultiblockCapability TERMINAL_TAB = echo("terminal_tab");
    public static final MultiblockCapability MAP_MARKER = echo("map_marker");
    public static final MultiblockCapability SCANNER_TARGET = echo("scanner_target");
    public static final MultiblockCapability HAZARD_SUPPRESSION = echo("hazard_suppression");
    public static final MultiblockCapability CONVOY_DISPATCH = echo("convoy_dispatch");
    public static final MultiblockCapability ORBITAL_LAUNCH = echo("orbital_launch");
    public static final MultiblockCapability NEXUS_GATE = echo("nexus_gate");
    public static final MultiblockCapability RESEARCH_STATION = echo("research_station");
    public static final MultiblockCapability ROBOTICS = echo("robotics");
    public static final MultiblockCapability WORKCELL = echo("workcell");
    public static final MultiblockCapability AUTO_BUILDER = echo("auto_builder");

    public MultiblockCapability {
        if (id == null) {
            id = Identifier.fromNamespaceAndPath("echo", "unknown");
        }
    }

    public static MultiblockCapability of(Identifier id) {
        return new MultiblockCapability(id);
    }

    private static MultiblockCapability echo(String path) {
        return new MultiblockCapability(Identifier.fromNamespaceAndPath("echo", path));
    }
}

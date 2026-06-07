package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.resources.Identifier;

public final class OrbitalTerminalIds {
    public static final Identifier COMMAND_TAB = id("orbital");
    public static final Identifier SURVEY_TAB = id("orbital_survey");
    public static final Identifier ECHO_TAB = id("orbital_echo");
    public static final Identifier SCAN_ACTION = id("scan");
    public static final Identifier CHAPTER_ID = id("orbital_remnants");

    private OrbitalTerminalIds() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, path);
    }
}

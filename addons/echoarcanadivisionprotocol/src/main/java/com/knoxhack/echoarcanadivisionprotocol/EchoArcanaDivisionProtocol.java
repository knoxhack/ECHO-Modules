package com.knoxhack.echoarcanadivisionprotocol;

import java.util.List;

/**
 * Data-first pack root for Arcana Division ownership contracts.
 */
public final class EchoArcanaDivisionProtocol {
    public static final String MODID = "echoarcanadivisionprotocol";
    public static final List<String> FOUNDATION_MODULES = List.of(
            "echofoundationcore",
            "echomaterialcore",
            "echotoolcore",
            "echostationcore",
            "echoworldstarter",
            "echocommonloot",
            "echocreatureroles"
    );
    public static final List<String> ARCANA_MODULES = List.of(
            "echoarcanacore",
            "echoaetherworks",
            "echocursecore",
            "echofamiliarcore",
            "echogrimoire",
            "echoriftworlds",
            "echoritualcore",
            "echospellcore"
    );

    public EchoArcanaDivisionProtocol() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO: Arcana Division online with " + ARCANA_MODULES.size() + " Arcana modules.");
    }
}

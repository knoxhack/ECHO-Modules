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
    public static final List<String> LAUNCHER_SUPPORT_MODULES = List.of(
            "echoholomap",
            "echoindex",
            "echolens",
            "echoterminal",
            "echothemecore",
            "echomissioncore"
    );
    public static final List<String> BETA_RUNTIME_MODULES = List.of(
            "echocore",
            "echoadaptercore",
            "echonetcore",
            "echofoundationcore",
            "echomaterialcore",
            "echotoolcore",
            "echostationcore",
            "echoworldstarter",
            "echocommonloot",
            "echocreatureroles",
            "echoarcanacore",
            "echoaetherworks",
            "echocursecore",
            "echofamiliarcore",
            "echogrimoire",
            "echoriftworlds",
            "echoritualcore",
            "echospellcore",
            "echoholomap",
            "echoindex",
            "echolens",
            "echoterminal",
            "echothemecore",
            "echomissioncore"
    );

    public EchoArcanaDivisionProtocol() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO: Arcana Division beta online with " + BETA_RUNTIME_MODULES.size() + " runtime modules.");
    }
}

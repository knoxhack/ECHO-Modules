package com.knoxhack.echoscreencore.api.contract;

import net.minecraft.resources.Identifier;

public record EchoScreenComponentContract(
    Identifier id,
    EchoScreenComponentKind kind,
    String purpose,
    int minWidth,
    int minHeight,
    boolean focusable,
    boolean scrollOwner,
    boolean controllerReady,
    boolean supportsThemeTokens,
    String degradedMode
) {
    public EchoScreenComponentContract {
        if (id == null) {
            throw new IllegalArgumentException("Screen component contract id is required.");
        }
        kind = kind == null ? EchoScreenComponentKind.CARD : kind;
        purpose = purpose == null ? "" : purpose;
        minWidth = Math.max(0, minWidth);
        minHeight = Math.max(0, minHeight);
        degradedMode = degradedMode == null ? "" : degradedMode;
    }
}

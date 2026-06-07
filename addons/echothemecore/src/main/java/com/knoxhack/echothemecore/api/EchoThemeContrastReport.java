package com.knoxhack.echothemecore.api;

public record EchoThemeContrastReport(
    float textOnPanel,
    float mutedTextOnPanel,
    float accentOnBackground,
    float successOnPanel,
    float warningOnPanel,
    float errorOnPanel,
    boolean readable,
    String recommendation
) {
}

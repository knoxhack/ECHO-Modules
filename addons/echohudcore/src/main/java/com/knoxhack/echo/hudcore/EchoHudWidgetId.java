package com.knoxhack.echo.hudcore;

public record EchoHudWidgetId(String value) {
    public EchoHudWidgetId {
        value = HudContractGuards.id(value, "hud widget id");
    }

    public static EchoHudWidgetId of(String value) {
        return new EchoHudWidgetId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.knoxhack.echo.lorecore;

public record EchoLoreFragmentId(String value) {
    public EchoLoreFragmentId {
        value = LoreContractGuards.id(value, "lore fragment id");
    }

    public static EchoLoreFragmentId of(String value) {
        return new EchoLoreFragmentId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

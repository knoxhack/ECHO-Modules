package com.knoxhack.echoscreencore.client.style;

public record EchoStyleState(
        boolean hovered,
        boolean focused,
        boolean disabled,
        boolean selected,
        boolean active) {
    public static final EchoStyleState NONE = new EchoStyleState(false, false, false, false, false);
}

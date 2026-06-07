package com.knoxhack.echopresencelink.api;

import net.minecraft.resources.Identifier;

public interface EchoPresenceProvider {
    Identifier id();

    EchoPresenceSnapshot snapshot(EchoPresenceContext context);

    default int order() {
        return 0;
    }
}

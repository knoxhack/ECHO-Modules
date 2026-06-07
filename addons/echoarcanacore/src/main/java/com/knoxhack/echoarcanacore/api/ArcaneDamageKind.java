package com.knoxhack.echoarcanacore.api;

import com.knoxhack.echoarcanacore.EchoArcanaCore;
import net.minecraft.resources.Identifier;

public enum ArcaneDamageKind {
    AETHER,
    VOID,
    SOUL,
    BLOOD,
    FRACTURE,
    VEIL,
    DECAY,
    CRYSTAL,
    STORM,
    CURSED,
    RITUAL_BACKLASH,
    RELIC_BACKLASH;

    public Identifier id() {
        return EchoArcanaCore.id("damage/" + name().toLowerCase(java.util.Locale.ROOT));
    }
}

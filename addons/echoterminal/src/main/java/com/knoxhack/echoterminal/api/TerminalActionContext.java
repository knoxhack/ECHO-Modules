package com.knoxhack.echoterminal.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record TerminalActionContext(
        ServerPlayer player,
        Identifier tabId,
        Identifier actionId,
        String payload) {
    public TerminalActionContext {
        if (tabId != null) {
            TerminalApiIds.requireLowercase(tabId, "Terminal action tab");
        }
        if (actionId != null) {
            TerminalApiIds.requireLowercase(actionId, "Terminal action");
        }
        payload = payload == null ? "" : payload;
    }
}

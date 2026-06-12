package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public interface TerminalService {
    default void registerDashboardCard(Identifier cardId) {
    }
}

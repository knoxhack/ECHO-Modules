package com.knoxhack.echoterminal.api;

import net.minecraft.resources.Identifier;

/**
 * Optional client-side metadata for Terminal tabs that can render through ScreenCore.
 */
public interface TerminalScreenCorePageMetadata {
    Identifier screenCorePageId();
}

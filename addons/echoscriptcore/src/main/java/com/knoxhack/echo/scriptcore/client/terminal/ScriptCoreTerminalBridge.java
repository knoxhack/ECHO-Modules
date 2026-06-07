package com.knoxhack.echo.scriptcore.client.terminal;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;

public final class ScriptCoreTerminalBridge {
    private static boolean registered;

    private ScriptCoreTerminalBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ScriptCoreTerminalTab tab = new ScriptCoreTerminalTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.system(215));
        EchoScriptCore.LOGGER.info("ScriptCore Terminal browser registered.");
    }
}

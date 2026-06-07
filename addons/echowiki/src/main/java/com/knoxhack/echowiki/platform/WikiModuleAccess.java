package com.knoxhack.echowiki.platform;

import java.util.Map;

public final class WikiModuleAccess {
    private static final Map<String, String> MODULE_CLASSES = Map.ofEntries(
            Map.entry("echoashfallprotocol", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocol"),
            Map.entry("echoindex", "com.knoxhack.echoindex.EchoIndex"),
            Map.entry("echomissioncore", "com.knoxhack.echomissioncore.EchoMissionCore"),
            Map.entry("echoterminal", "com.knoxhack.echoterminal.EchoTerminal"),
            Map.entry("echotutorialcore", "com.knoxhack.echotutorialcore.EchoTutorialCore"),
            Map.entry("echoworldcore", "com.knoxhack.echoworldcore.EchoWorldCore")
    );

    private WikiModuleAccess() {
    }

    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        if ("minecraft".equals(modId) || "echowiki".equals(modId)) {
            return true;
        }
        String className = MODULE_CLASSES.get(modId);
        if (className == null) {
            return false;
        }
        try {
            Class.forName(className, false, WikiModuleAccess.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}

package com.knoxhack.signalos.platform;

import java.util.Map;

public final class SignalOsModuleAccess {
    private static final Map<String, String> MODULE_CLASSES = Map.of(
            "echorendercore", "com.knoxhack.echorendercore.EchoRenderCore",
            "echoscreencore", "com.knoxhack.echoscreencore.EchoScreenCore",
            "echoterminal", "com.knoxhack.echoterminal.EchoTerminal"
    );

    private SignalOsModuleAccess() {
    }

    public static boolean isLoaded(String modId) {
        String className = MODULE_CLASSES.get(modId);
        if (className == null) {
            return "minecraft".equals(modId);
        }
        try {
            Class.forName(className, false, SignalOsModuleAccess.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}

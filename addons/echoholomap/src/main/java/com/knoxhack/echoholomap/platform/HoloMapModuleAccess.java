package com.knoxhack.echoholomap.platform;

import java.util.Map;

public final class HoloMapModuleAccess {
    private static final Map<String, String> MODULE_CLASSES = Map.of(
            "echoruntimeguard", "com.knoxhack.echoruntimeguard.EchoRuntimeGuard",
            "echoscreencore", "com.knoxhack.echoscreencore.EchoScreenCore",
            "echoterminal", "com.knoxhack.echoterminal.EchoTerminal",
            "echoworldcore", "com.knoxhack.echoworldcore.EchoWorldCore"
    );

    private HoloMapModuleAccess() {
    }

    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        if ("minecraft".equals(modId)) {
            return true;
        }
        String className = MODULE_CLASSES.get(modId);
        if (className == null) {
            return false;
        }
        try {
            Class.forName(className, false, HoloMapModuleAccess.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}

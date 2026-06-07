package com.knoxhack.echolens.platform;

import java.util.Map;

public final class LensModuleAccess {
    private static final Map<String, String> MODULE_CLASSES = Map.ofEntries(
            Map.entry("echoashfallprotocol", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocol"),
            Map.entry("echodatacore", "com.knoxhack.echodata.EchoDataCore"),
            Map.entry("echoholomap", "com.knoxhack.echoholomap.EchoHoloMap"),
            Map.entry("echoindex", "com.knoxhack.echoindex.EchoIndex"),
            Map.entry("echomissioncore", "com.knoxhack.echomissioncore.EchoMissionCore"),
            Map.entry("echomultiblockcore", "com.knoxhack.echomultiblockcore.EchoMultiblockCore"),
            Map.entry("echorendercore", "com.knoxhack.echorendercore.EchoRenderCore"),
            Map.entry("echoruntimeguard", "com.knoxhack.echoruntimeguard.EchoRuntimeGuard"),
            Map.entry("echosoundcore", "com.knoxhack.echosoundcore.EchoSoundCore"),
            Map.entry("echoterminal", "com.knoxhack.echoterminal.EchoTerminal"),
            Map.entry("echothemecore", "com.knoxhack.echothemecore.EchoThemeCore"),
            Map.entry("echotutorialcore", "com.knoxhack.echotutorialcore.EchoTutorialCore"),
            Map.entry("echoworldcore", "com.knoxhack.echoworldcore.EchoWorldCore")
    );
    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry("minecraft", "Minecraft"),
            Map.entry("echoashfallprotocol", "ECHO: Ashfall Protocol"),
            Map.entry("echodatacore", "ECHO: DataCore"),
            Map.entry("echoindex", "ECHO: Index"),
            Map.entry("echomissioncore", "ECHO: MissionCore"),
            Map.entry("echorendercore", "ECHO: RenderCore"),
            Map.entry("echoruntimeguard", "ECHO: RuntimeGuard"),
            Map.entry("echosoundcore", "ECHO: SoundCore"),
            Map.entry("echoterminal", "ECHO: Terminal")
    );

    private LensModuleAccess() {
    }

    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        if ("minecraft".equals(modId)) {
            return true;
        }
        String className = MODULE_CLASSES.get(modId);
        return className != null && classPresent(className);
    }

    public static String displayName(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "Unknown";
        }
        return DISPLAY_NAMES.getOrDefault(namespace, namespace);
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, LensModuleAccess.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}

package com.echoplatform.echocore.api;

/**
 * Stable facade for optional ECHO module presence checks.
 */
public final class EchoIntegrations {
    public static final String DATA_CORE = "echodatacore";
    public static final String HOLO_MAP = "echoholomap";
    public static final String INDEX = "echoindex";
    public static final String LENS = "echolens";
    public static final String MISSION_CORE = "echomissioncore";
    public static final String SOUND_CORE = "echosoundcore";
    public static final String TERMINAL = "echoterminal";
    public static final String WORLD_CORE = "echoworldcore";

    private EchoIntegrations() {
    }

    public static boolean has(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        return EchoRuntimeModules.isLoaded(moduleId);
    }

    public static boolean hasDataCore() {
        return has(DATA_CORE);
    }

    public static boolean hasHoloMap() {
        return has(HOLO_MAP);
    }

    public static boolean hasIndex() {
        return has(INDEX);
    }

    public static boolean hasLens() {
        return has(LENS);
    }

    public static boolean hasMissionCore() {
        return has(MISSION_CORE);
    }

    public static boolean hasSoundCore() {
        return has(SOUND_CORE);
    }

    public static boolean hasTerminal() {
        return has(TERMINAL);
    }

    public static boolean hasWorldCore() {
        return has(WORLD_CORE);
    }
}

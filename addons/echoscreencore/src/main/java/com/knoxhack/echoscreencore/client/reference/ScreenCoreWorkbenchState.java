package com.knoxhack.echoscreencore.client.reference;

import java.util.Map;
import net.minecraft.resources.Identifier;

public final class ScreenCoreWorkbenchState {
    public enum PreviewMode {
        FIT,
        SMALL,
        DEFAULT,
        LARGE,
        CURRENT
    }

    private static Identifier selectedPage = Identifier.fromNamespaceAndPath("echoscreencore", "reference_list_detail");
    private static PreviewMode previewMode = PreviewMode.DEFAULT;
    private static boolean debug;

    private ScreenCoreWorkbenchState() {
    }

    public static Identifier selectedPage() {
        return selectedPage;
    }

    public static void selectPage(Identifier pageId) {
        if (pageId != null) {
            selectedPage = pageId;
        }
    }

    public static PreviewMode previewMode() {
        return previewMode;
    }

    public static void setPreviewMode(PreviewMode mode) {
        previewMode = mode == null ? PreviewMode.DEFAULT : mode;
    }

    public static boolean debug() {
        return debug;
    }

    public static void toggleDebug() {
        debug = !debug;
    }

    public static int viewportWidth(int currentWidth) {
        return switch (previewMode) {
            case SMALL -> 360;
            case DEFAULT -> 854;
            case LARGE -> 1280;
            case CURRENT, FIT -> Math.max(1, currentWidth);
        };
    }

    public static int viewportHeight(int currentHeight) {
        return switch (previewMode) {
            case SMALL -> 240;
            case DEFAULT -> 480;
            case LARGE -> 720;
            case CURRENT, FIT -> Math.max(1, currentHeight);
        };
    }

    public static Map<String, Object> data() {
        return Map.of(
                "selectedPage", selectedPage.toString(),
                "previewMode", previewMode.name().toLowerCase(java.util.Locale.ROOT),
                "debug", debug);
    }
}

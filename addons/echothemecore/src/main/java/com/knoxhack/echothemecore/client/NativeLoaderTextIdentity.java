package com.knoxhack.echothemecore.client;

import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;

public final class NativeLoaderTextIdentity {
    private static final String LABEL_PROPERTY = "echo.native.loader.label";
    private static final String PRODUCT_LABEL_PROPERTY = "echo.native.loader.productLabel";
    private static final String LEGACY_TESTER_LABEL_PROPERTY = "echo.native.loader.testerLabel";
    private static final String WINDOW_TITLE_PROPERTY = "echo.native.loader.windowTitle";
    private static final String DEFAULT_LABEL = "ECHO Native Loader";
    private static final String DEFAULT_PRODUCT_LABEL = "ECHO NATIVE LOADER - PRODUCT CLIENT";
    private static final String DEFAULT_WINDOW_TITLE = "ECHO Native Loader - Product Client";

    private NativeLoaderTextIdentity() {
    }

    public static boolean active() {
        return EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    public static String label() {
        return propertyOrDefault(LABEL_PROPERTY, DEFAULT_LABEL);
    }

    public static String productLabel() {
        String productLabel = System.getProperty(PRODUCT_LABEL_PROPERTY, "").trim();
        if (!productLabel.isBlank()) {
            return productLabel;
        }
        return propertyOrDefault(LEGACY_TESTER_LABEL_PROPERTY, DEFAULT_PRODUCT_LABEL);
    }

    /**
     * Compatibility alias for older launcher properties. New UI should use productLabel().
     */
    public static String testerLabel() {
        return productLabel();
    }

    public static String windowTitle() {
        return propertyOrDefault(WINDOW_TITLE_PROPERTY, DEFAULT_WINDOW_TITLE);
    }

    public static String badgeLabel() {
        return label() + " // PRODUCT";
    }

    private static String propertyOrDefault(String key, String fallback) {
        String value = System.getProperty(key, "").trim();
        return value.isBlank() ? fallback : value;
    }
}

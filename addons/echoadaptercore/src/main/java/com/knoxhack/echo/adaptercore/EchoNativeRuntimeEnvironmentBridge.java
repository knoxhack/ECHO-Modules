package com.knoxhack.echo.adaptercore;

public final class EchoNativeRuntimeEnvironmentBridge {
    private static final String ENVIRONMENT_CLASS =
            "dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment";

    private EchoNativeRuntimeEnvironmentBridge() {
    }

    public static boolean isNativeLoaderActive() {
        return invokeBoolean("isNativeLoaderActive");
    }

    public static boolean isWindowedNativeClient() {
        return invokeBoolean("isWindowedNativeClient");
    }

    private static boolean invokeBoolean(String methodName) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = EchoNativeRuntimeEnvironmentBridge.class.getClassLoader();
            }
            Class<?> environment = Class.forName(ENVIRONMENT_CLASS, false, loader);
            Object value = environment.getMethod(methodName).invoke(null);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}

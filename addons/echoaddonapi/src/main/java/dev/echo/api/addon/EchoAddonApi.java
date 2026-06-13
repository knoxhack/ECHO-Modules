package dev.echo.api.addon;

/**
 * Minimal common entrypoint for the public ECHO Add-on API artifact.
 */
public final class EchoAddonApi {
    public static final String MODID = "echoaddonapi";
    public static final String VERSION = "0.1.0";

    public EchoAddonApi() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public String version() {
        return VERSION;
    }
}

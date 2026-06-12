package com.knoxhack.echogalacticcore;

import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

public final class EchoGalacticCoreNativeModule implements EchoNativeModuleEntrypoint {
    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("label", "Unofficial ECHO Platform port/fork of Galacticraft Legacy");
        context.attribute("license", "MIT");
        context.attribute("derivedFrom", "Galacticraft Legacy by TeamGalacticraft");
        GalacticCoreServices.phase(context, "discover");
    }

    @Override
    public void resolve(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.resolveDependencies(context);
        GalacticCoreServices.phase(context, "resolve");
    }

    @Override
    public void loadClasses(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.phase(context, "load_classes");
    }

    @Override
    public void construct(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.phase(context, "construct");
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService(
                "module." + GalacticCoreIds.MOD_ID + ".native.surface.module.entrypoint",
                this,
                "lifecycle",
                "diagnostics",
                "native_entrypoint"
        );
        GalacticCoreServices.registerModuleServices(context);
        GalacticCoreServices.phase(context, "register_services");
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.registerContent(context);
        GalacticCoreServices.phase(context, "register_content");
    }

    @Override
    public void commonSetup(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.registerCommonRuntime(context);
        GalacticCoreServices.phase(context, "common_setup");
    }

    @Override
    public void clientSetup(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.registerClientRuntime(context);
        GalacticCoreServices.phase(context, "client_setup");
    }

    @Override
    public void serverSetup(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.registerServerRuntime(context);
        GalacticCoreServices.phase(context, "server_setup");
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.phase(context, "ready");
    }

    @Override
    public void shutdown(EchoNativeModuleLoadContext context) {
        GalacticCoreServices.phase(context, "shutdown");
        GalacticCoreServices.shutdown(context);
    }
}

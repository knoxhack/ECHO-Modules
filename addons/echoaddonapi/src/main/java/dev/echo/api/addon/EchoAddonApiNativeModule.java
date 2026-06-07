package dev.echo.api.addon;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

public final class EchoAddonApiNativeModule implements EchoNativeModuleEntrypoint {
    private static final String MODULE_ID = "echoaddonapi";

    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_native_module_entrypoint");
        context.attribute("nativeEntrypointClass", getClass().getName());
        context.attribute("nativeModuleEntrypoint", true);
        context.attribute("apiSpine", true);
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService(
                "service.echoaddonapi.public_api",
                this,
                "public_api",
                "sdk_spine",
                "addon_contracts",
                "registry_contracts",
                "lifecycle_contracts"
        );
        context.recordMutation(
                "service",
                "public_api_contract_registered",
                MODULE_ID + ":public_api",
                EchoNativeLoadStatus.MUTATED
        );
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        context.attribute("echoAddonApiNativeReady", true);
    }
}

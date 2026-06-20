package com.knoxhack.echodatacore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echodatacore.integration.DataCoreDiagnostics;
import com.knoxhack.echodatacore.integration.DataCoreIndexProvider;
import com.knoxhack.echodatacore.integration.DataCoreRuntimeSpineConsumer;
import com.knoxhack.echodatacore.integration.DataCoreWorldCoreConsumer;
import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoDataCore.MODID)
public class EchoDataCore {
    public static final String MODID = "echodatacore";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);

    EchoDataCore() {
        this(null);
    }

    public EchoDataCore(IEventBus modEventBus) {
        Config.registerEchoConfig();
        commonSetup();
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                "com.knoxhack.echodatacore.test.ModGameTests");
    }

    public void commonSetup() {
        registerCommonServices("neoforge_common_setup");
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        return registerCommonServices("native_loader_module_ready");
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        EchoCoreServices.registerDataService(DataCoreDataService.INSTANCE);
        DataCoreBuiltinKeys.register();
        DataCoreDiagnostics.register();
        DataCoreIndexProvider.register();
        DataCoreRuntimeSpineConsumer.register();
        DataCoreWorldCoreConsumer.register();
        LOGGER.info("ECHO: DataCore registered [{}]. {}", source, EchoCoreServices.platformProviderSummary());
        return true;
    }
}

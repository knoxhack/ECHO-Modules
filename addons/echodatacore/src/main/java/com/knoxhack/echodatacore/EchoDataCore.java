package com.knoxhack.echodatacore;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echodatacore.integration.DataCoreDiagnostics;
import com.knoxhack.echodatacore.integration.DataCoreIndexProvider;
import com.knoxhack.echodatacore.integration.DataCoreRuntimeSpineConsumer;
import com.knoxhack.echodatacore.integration.DataCoreWorldCoreConsumer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoDataCore {
    public static final String MODID = "echodatacore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoDataCore() {
        Config.registerEchoConfig();
        commonSetup();
    }

    public void commonSetup() {
        EchoCoreServices.registerDataService(DataCoreDataService.INSTANCE);
        DataCoreBuiltinKeys.register();
        DataCoreDiagnostics.register();
        DataCoreIndexProvider.register();
        DataCoreRuntimeSpineConsumer.register();
        DataCoreWorldCoreConsumer.register();
        LOGGER.info("ECHO: DataCore registered. {}", EchoCoreServices.platformProviderSummary());
    }
}

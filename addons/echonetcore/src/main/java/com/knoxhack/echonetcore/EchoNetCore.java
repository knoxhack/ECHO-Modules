package com.knoxhack.echonetcore;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import com.knoxhack.echonetcore.service.NetCoreNetworkService;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoNetCore {
    public static final String MODID = "echonetcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoNetCore() {
        EchoNetCoreConfig.registerEchoConfig();
        EchoCoreServices.registerNetworkService(NetCoreNetworkService.INSTANCE);
        LOGGER.info("ECHO: NetCore native network bridge online.");
    }
}

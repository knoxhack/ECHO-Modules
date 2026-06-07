package com.knoxhack.echo.vehiclecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoVehicleCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoVehicleCore(Object modEventBus) {
        LOGGER.info("ECHO: VehicleCore loaded shared vehicle contracts.");
        var runtime = Agent9VehicleCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: VehicleCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}

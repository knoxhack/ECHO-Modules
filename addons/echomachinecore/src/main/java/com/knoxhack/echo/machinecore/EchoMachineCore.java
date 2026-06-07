package com.knoxhack.echo.machinecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoMachineCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoMachineCore() {
        LOGGER.info("ECHO: MachineCore loaded shared machine contracts.");
        var runtime = Agent9MachineCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: MachineCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}

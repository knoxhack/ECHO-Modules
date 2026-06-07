package com.knoxhack.echo.reportcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoReportCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoReportCore(Object modEventBus) {
        LOGGER.info("ECHO: ReportCore loaded deterministic tooling report contracts.");
    }
}

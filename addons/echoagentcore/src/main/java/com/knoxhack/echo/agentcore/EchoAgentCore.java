package com.knoxhack.echo.agentcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoAgentCore {
    public static final String MODID = EchoAgentConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoAgentCore(Object ignoredModEventBus, Object ignoredModContainer) {
        LOGGER.info(
                "ECHO: AgentCore loaded {} agent lanes and {} safe command risk levels.",
                EchoAgentConstants.AGENT_LANES.size(),
                EchoAgentConstants.SAFE_COMMAND_RISKS.size()
        );
    }
}

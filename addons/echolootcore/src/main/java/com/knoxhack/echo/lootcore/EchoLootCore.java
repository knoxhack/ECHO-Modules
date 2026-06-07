package com.knoxhack.echo.lootcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoLootCore {
    public static final String MODID = "echolootcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoLootCore() {
        LOGGER.info("ECHO: LootCore online with rarity, pool, reward, and anti-duplication contracts.");
        var runtime = Agent9LootCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: LootCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}

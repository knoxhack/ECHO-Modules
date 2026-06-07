package com.knoxhack.echo.recipecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoRecipeCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoRecipeCore() {
        LOGGER.info("ECHO: RecipeCore contracts initialized.");
        var runtime = Agent9RecipeCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: RecipeCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}

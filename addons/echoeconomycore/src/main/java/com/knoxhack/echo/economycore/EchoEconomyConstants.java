package com.knoxhack.echo.economycore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoEconomyConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of(EchoEconomyCore.MODID);
    public static final EchoFeatureId FEATURE_CURRENCIES = EchoFeatureId.of("economy.currencies");
    public static final EchoFeatureId FEATURE_BARTER = EchoFeatureId.of("economy.barter");
    public static final EchoFeatureId FEATURE_FACTION_SHOPS = EchoFeatureId.of("economy.faction_shops");
    public static final EchoFeatureId FEATURE_TRADE_VALIDATION = EchoFeatureId.of("economy.trade_validation");

    private EchoEconomyConstants() {
    }
}

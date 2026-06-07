package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public interface MultiblockPowerProvider {
    default Identifier providerId() {
        return MultiblockIntegrationServices.generatedProviderId(this, "power");
    }

    long availablePower(Level level, BlockPos controllerPos);

    long drawPower(Level level, BlockPos controllerPos, long ep, boolean simulate);
}

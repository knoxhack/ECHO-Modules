package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public interface EchoRoboticComponent {
    Identifier getRobotId();

    default Identifier getRobotRuntimeId(BlockPos controllerPos) {
        String controller = controllerPos == null ? "unlinked" : Long.toUnsignedString(controllerPos.asLong());
        return Identifier.fromNamespaceAndPath(getRobotId().getNamespace(), controller + "/" + getRobotId().getPath());
    }

    RobotState getRobotState();

    List<RobotToolType> getInstalledTools();

    boolean canPerform(MultiblockAutomationRecipe recipe);

    void assignTask(MultiblockAutomationRecipe recipe);

    void clearTask();

    int getReach();

    int getPrecision();

    int getStrength();

    int getHeat();

    int getMaxHeat();
}

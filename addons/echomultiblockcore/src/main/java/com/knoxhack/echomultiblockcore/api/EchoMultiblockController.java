package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface EchoMultiblockController {
    Identifier getMultiblockId();

    MultiblockState getState();

    ValidationResult validateStructure();

    void onStructureFormed();

    void onStructureBroken();

    void tickFormedStructure();

    float getIntegrity();

    void setIntegrity(float value);

    List<MultiblockCapability> getCapabilities();

    Optional<MultiblockRuntime> getRuntime();
}

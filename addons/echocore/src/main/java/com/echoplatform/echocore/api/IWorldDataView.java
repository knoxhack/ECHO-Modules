package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public interface IWorldDataView extends IDataView {
    default Identifier dimensionId() {
        return Identifier.withDefaultNamespace("overworld");
    }
}

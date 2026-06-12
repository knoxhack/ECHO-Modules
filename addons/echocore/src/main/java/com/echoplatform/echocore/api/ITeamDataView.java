package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public interface ITeamDataView extends IDataView {
    default Identifier teamId() {
        return Identifier.withDefaultNamespace("team");
    }
}

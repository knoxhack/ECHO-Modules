package com.echoplatform.echocore.api;

import java.util.UUID;

public interface IPlayerDataView extends IDataView {
    default UUID playerId() {
        return new UUID(0L, 0L);
    }
}

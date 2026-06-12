package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record DataChangeMessage(
        DataScope scope,
        String ownerId,
        Identifier keyId,
        DataValueKind kind,
        long revision,
        boolean fullSnapshot,
        DataChangeKind changeKind) {
    public DataChangeMessage {
        scope = scope == null ? DataScope.PLAYER : scope;
        ownerId = ownerId == null ? "" : ownerId;
        kind = kind == null ? DataValueKind.RECORD : kind;
        changeKind = changeKind == null ? DataChangeKind.SET : changeKind;
    }
}

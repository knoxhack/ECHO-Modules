package com.echoplatform.echocore.api.network;

public enum EchoPacketKind {
    COMMAND,
    EVENT,
    SYNC,
    DEBUG,
    CUSTOM,
    CLIENTBOUND_SYNC,
    SERVERBOUND_ACTION,
    OPTIONAL_ADDON,
    DEBUG_DEV
}

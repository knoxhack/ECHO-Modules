package com.knoxhack.signalos.api;

public enum SignalOsDriveResultCode {
    OK,
    NO_ACTIVE_DRIVE,
    UNSUPPORTED_DRIVE,
    INVALID_PATH,
    NOT_FOUND,
    ALREADY_EXISTS,
    CAPACITY_FULL,
    READ_ONLY,
    INVALID_PAYLOAD,
    UNKNOWN_ACTION,
    ERROR
}

package com.knoxhack.signalos.api;

public record SignalOsActionResult(
        boolean handled,
        SignalOsDriveResultCode code,
        String message) {
    public SignalOsActionResult {
        code = code == null ? SignalOsDriveResultCode.ERROR : code;
        message = message == null ? "" : message.strip();
    }

    public boolean success() {
        return handled && code == SignalOsDriveResultCode.OK;
    }

    public static SignalOsActionResult success(String message) {
        return new SignalOsActionResult(true, SignalOsDriveResultCode.OK, message);
    }

    public static SignalOsActionResult failure(SignalOsDriveResultCode code, String message) {
        return new SignalOsActionResult(true, code, message);
    }

    public static SignalOsActionResult unknown() {
        return new SignalOsActionResult(false, SignalOsDriveResultCode.UNKNOWN_ACTION, "");
    }

    public static SignalOsActionResult fromDriveResult(SignalOsDriveWriteResult result) {
        SignalOsDriveWriteResult safe = result == null
                ? SignalOsDriveWriteResult.failure(SignalOsDriveResultCode.ERROR, SignalOsDriveData.EMPTY,
                        "[SignalOS] Drive action failed.")
                : result;
        return new SignalOsActionResult(true, safe.code(), safe.message());
    }
}

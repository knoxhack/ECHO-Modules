package com.knoxhack.signalos.api;

public record SignalOsDriveWriteResult(
        SignalOsDriveResultCode code,
        SignalOsDriveData drive,
        String message) {
    public SignalOsDriveWriteResult {
        code = code == null ? SignalOsDriveResultCode.ERROR : code;
        drive = drive == null ? SignalOsDriveData.EMPTY : drive;
        message = message == null ? "" : message.strip();
    }

    public boolean success() {
        return code == SignalOsDriveResultCode.OK;
    }

    public static SignalOsDriveWriteResult success(SignalOsDriveData drive, String message) {
        return new SignalOsDriveWriteResult(SignalOsDriveResultCode.OK, drive, message);
    }

    public static SignalOsDriveWriteResult failure(SignalOsDriveResultCode code, SignalOsDriveData drive,
            String message) {
        return new SignalOsDriveWriteResult(code, drive, message);
    }
}

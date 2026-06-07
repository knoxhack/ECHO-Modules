package com.knoxhack.echo.packcore;

import java.nio.file.Path;

public interface EchoLockfileVerificationReportWriter {
    Path write(EchoLockfileVerificationResult result, Path reportsRoot);
}

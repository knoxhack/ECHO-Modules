package com.knoxhack.echo.packcore;

public interface EchoLockfileVerifier {
    EchoLockfileVerificationResult verify(
            EchoPackProfile profile,
            EchoPackLockfile lockfile,
            EchoInstallState installState
    );
}

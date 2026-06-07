package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoLockfileDriftDetector(
        List<String> driftCodes,
        List<String> repairableCodes
) {
    public EchoLockfileDriftDetector {
        driftCodes = PackContractGuards.immutableList(driftCodes);
        repairableCodes = PackContractGuards.immutableList(repairableCodes);
    }
}

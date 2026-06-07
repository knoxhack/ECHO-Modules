package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoLockfileChecksum(
        String algorithm,
        EchoLockfileChecksumMode mode,
        String value,
        List<String> inputs
) {
    public EchoLockfileChecksum {
        algorithm = PackContractGuards.optionalText(algorithm);
        mode = mode == null ? EchoLockfileChecksumMode.UNKNOWN : mode;
        value = PackContractGuards.optionalText(value);
        inputs = PackContractGuards.immutableList(inputs);
    }
}

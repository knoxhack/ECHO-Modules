package com.knoxhack.echo.packcore;

public record EchoManagedTarget(
        boolean configured,
        String path,
        boolean localOnly,
        boolean exists,
        boolean readable,
        boolean scanned,
        int jarCount
) {
    public EchoManagedTarget {
        path = PackContractGuards.optionalText(path);
        jarCount = Math.max(0, jarCount);
    }
}

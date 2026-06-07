package com.knoxhack.echo.packcore;

public record EchoManagedTargetResolver(
        String propertyName,
        String redactedPath,
        boolean localOnly
) {
    public static final String DEFAULT_PROPERTY = "echoManagedTarget";
    public static final String PROVIDED_PATH = "provided_by_echoManagedTarget";

    public EchoManagedTargetResolver {
        propertyName = PackContractGuards.requireText(propertyName, "managed target property name");
        redactedPath = PackContractGuards.requireText(redactedPath, "managed target redacted path");
    }

    public static EchoManagedTargetResolver defaultResolver() {
        return new EchoManagedTargetResolver(DEFAULT_PROPERTY, PROVIDED_PATH, true);
    }

    public EchoManagedTarget unresolved() {
        return new EchoManagedTarget(false, "", false, false, false, false, 0);
    }

    public EchoManagedTarget resolved(boolean exists, boolean readable, boolean scanned, int jarCount) {
        return new EchoManagedTarget(true, redactedPath, localOnly, exists, readable, scanned, jarCount);
    }
}

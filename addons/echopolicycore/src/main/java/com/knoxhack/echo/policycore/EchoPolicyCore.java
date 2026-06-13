package com.knoxhack.echo.policycore;

import java.util.List;

public final class EchoPolicyCore {
    public static final String MODID = "echopolicycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echovalidationcore",
            "echoreportcore",
            "echometadatacore"
        );
    public static final List<String> PROVIDES = List.of(
            "policy.manifest",
            "policy.validation",
            "policy.runtime_hooks",
            "policy.trust_metadata"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "policy_manifest",
            "launcher_validation",
            "studio_validation",
            "runtime_enforcement_hooks",
            "blocked_module_rules"
        );

    public EchoPolicyCore() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}

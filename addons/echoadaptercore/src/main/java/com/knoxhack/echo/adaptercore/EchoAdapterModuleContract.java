package com.knoxhack.echo.adaptercore;

import java.util.Set;

public record EchoAdapterModuleContract(
        String moduleId,
        Set<EchoAdapterDomain> domains,
        boolean metadataDeclared,
        boolean nativeEntrypointDeclared,
        String rendererTarget
) {
    public EchoAdapterModuleContract {
        moduleId = AdapterContractGuards.requireText(moduleId, "moduleId");
        domains = AdapterContractGuards.immutableSet(domains);
        rendererTarget = AdapterContractGuards.optionalText(rendererTarget);
    }

    public boolean supports(EchoAdapterDomain domain) {
        return domains.contains(domain);
    }
}

package com.echoplatform.echocore.api;

import java.util.List;

public record EchoFactionInteractionSnapshot(
        EchoFactionProfile profile,
        String roleId,
        List<EchoFactionAction> actions,
        List<EchoFactionContract> contracts,
        String localContext) {
    public EchoFactionInteractionSnapshot {
        roleId = roleId == null ? "" : roleId;
        actions = actions == null ? List.of() : List.copyOf(actions);
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
        localContext = localContext == null ? "" : localContext;
    }

    public String roleName() {
        if (profile == null || profile.definition() == null) {
            return roleId;
        }
        return profile.definition().roles().stream()
                .filter(role -> role.id().equals(roleId))
                .findFirst()
                .map(EchoNpcRole::displayName)
                .orElse(roleId);
    }

    public String greeting() {
        if (profile == null || profile.definition() == null) {
            return "";
        }
        return profile.definition().dialogue().greeting();
    }
}

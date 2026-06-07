package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleRole;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class EchoRoleConflictDetector {
    private static final Set<EchoModuleRole> EXCLUSIVE_ROLES = EnumSet.of(
            EchoModuleRole.RUNTIME_CORE,
            EchoModuleRole.NETWORK_CORE,
            EchoModuleRole.DATA_CORE,
            EchoModuleRole.PLATFORM_CORE,
            EchoModuleRole.ADAPTER_CORE,
            EchoModuleRole.VALIDATION_CORE,
            EchoModuleRole.SCHEMA_CORE,
            EchoModuleRole.METADATA_CORE,
            EchoModuleRole.MODULE_GRAPH,
            EchoModuleRole.HEALTH_CORE,
            EchoModuleRole.RECOVERY_CORE,
            EchoModuleRole.AGENT_CORE,
            EchoModuleRole.BRIDGE_CORE,
            EchoModuleRole.REPORT_CORE,
            EchoModuleRole.PACK_CORE,
            EchoModuleRole.GAME_ROOT,
            EchoModuleRole.QUEST_DIRECTOR
    );

    public List<EchoRoleConflict> detect(Collection<EchoScannedModule> modules) {
        EchoModuleRoleIndex index = EchoModuleRoleIndex.fromModules(modules);
        return EXCLUSIVE_ROLES.stream()
                .sorted(java.util.Comparator.comparing(EchoModuleRole::serializedName))
                .map(role -> conflictFor(role, index.modulesFor(role)))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    public Set<EchoModuleRole> exclusiveRoles() {
        return Set.copyOf(EXCLUSIVE_ROLES);
    }

    private java.util.Optional<EchoRoleConflict> conflictFor(EchoModuleRole role, List<EchoModuleId> modules) {
        if (modules.size() <= 1) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new EchoRoleConflict(
                role,
                modules,
                "Exclusive platform role " + role.serializedName() + " has multiple providers.",
                true
        ));
    }
}

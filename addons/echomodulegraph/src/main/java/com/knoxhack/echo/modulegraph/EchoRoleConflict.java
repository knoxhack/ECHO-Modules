package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleRole;

import java.util.List;
import java.util.Objects;

public record EchoRoleConflict(
        EchoModuleRole role,
        List<EchoModuleId> modules,
        String reason,
        boolean blocking
) {
    public EchoRoleConflict {
        Objects.requireNonNull(role, "role");
        modules = ModuleGraphContractGuards.immutableList(modules);
        reason = ModuleGraphContractGuards.optionalText(reason);
    }

    public EchoModuleGraphIssue toIssue() {
        return new EchoModuleGraphIssue(
                EchoModuleGraphIssueKind.ROLE_CONFLICT,
                null,
                modules.stream().findFirst().orElse(null),
                modules.size() > 1 ? modules.get(1) : null,
                null,
                null,
                "",
                reason.isEmpty() ? "Multiple modules declare exclusive role " + role.serializedName() + "." : reason,
                "",
                "Review module roles and pack composition rules.",
                false,
                List.of("docs/echo/validation/ECHO_MODULE_GRAPH.md")
        );
    }
}

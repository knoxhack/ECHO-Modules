package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EchoFeatureGraphBuilder {
    private final EchoFeaturePolicy policy;
    private final boolean officialPack;

    public EchoFeatureGraphBuilder(EchoFeaturePolicy policy, boolean officialPack) {
        this.policy = policy == null ? EchoExclusiveFeaturePolicy.defaults() : policy;
        this.officialPack = officialPack;
    }

    public static EchoFeatureGraph build(Collection<EchoScannedModule> modules) {
        return new EchoFeatureGraphBuilder(EchoExclusiveFeaturePolicy.defaults(), false).buildGraph(modules);
    }

    public EchoFeatureGraph buildGraph(Collection<EchoScannedModule> modules) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        EchoFeatureProviderIndex providers = EchoFeatureProviderIndex.fromModules(safeModules);
        EchoFeatureConsumerIndex consumers = EchoFeatureConsumerIndex.fromModules(safeModules);
        return EchoFeatureGraph.fromIndexes(safeModules, providers, consumers, policy, officialPack);
    }

    static EchoFeatureGraphNode nodeFor(
            EchoFeatureId feature,
            Collection<EchoScannedModule> modules,
            EchoFeatureProviderIndex providers,
            EchoFeatureConsumerIndex consumers,
            EchoFeaturePolicy policy,
            boolean officialPack
    ) {
        List<EchoFeatureProviderNode> providerNodes = new ArrayList<>();
        List<EchoFeatureConsumerNode> consumerNodes = new ArrayList<>();
        Set<EchoRuntimeSide> sides = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        Set<String> capabilities = new HashSet<>();
        List<EchoFeatureGraphIssue> issues = new ArrayList<>();

        for (EchoScannedModule module : modules) {
            if (providers.providersFor(feature).contains(module.moduleId())) {
                EchoApiStability stability = parseStability(module.attributes().get("apiStability"));
                providerNodes.add(new EchoFeatureProviderNode(
                        module.moduleId(),
                        module.supportedSides(),
                        stability,
                        module.attributes().getOrDefault("trustLevel", ""),
                        Boolean.parseBoolean(module.attributes().getOrDefault("official", "false")),
                        !Boolean.parseBoolean(module.attributes().getOrDefault("disabledByPack", "false"))
                ));
                sides.addAll(module.supportedSides());
                permissions.addAll(splitAttribute(module.attributes().get("permissions")));
                capabilities.add(feature.value());
            }
            for (EchoFeatureRequirement requirement : module.consumedFeatures()) {
                if (requirement.featureId().equals(feature)) {
                    consumerNodes.add(new EchoFeatureConsumerNode(
                            module.moduleId(),
                            requirement.required(),
                            requirement.versionRange(),
                            requirement.reason(),
                            module.supportedSides(),
                            !Boolean.parseBoolean(module.attributes().getOrDefault("disabledByPack", "false"))
                    ));
                    sides.addAll(module.supportedSides());
                }
            }
        }

        List<EchoFeatureConsumerNode> requiredBy = consumerNodes.stream()
                .filter(EchoFeatureConsumerNode::required)
                .sorted(Comparator.comparing(node -> node.moduleId().value()))
                .toList();
        List<EchoFeatureConsumerNode> optionalFor = consumerNodes.stream()
                .filter(node -> !node.required())
                .sorted(Comparator.comparing(node -> node.moduleId().value()))
                .toList();
        providerNodes.sort(Comparator.comparing(node -> node.moduleId().value()));
        consumerNodes.sort(Comparator.comparing(node -> node.moduleId().value()));

        EchoFeatureStatus status = statusFor(feature, providerNodes, requiredBy, optionalFor, policy, officialPack, issues);
        return new EchoFeatureGraphNode(
                feature,
                providerNodes,
                consumerNodes,
                requiredBy,
                optionalFor,
                Set.copyOf(sides),
                providerNodes.stream().findFirst().map(EchoFeatureProviderNode::apiStability).orElse(EchoApiStability.EXPERIMENTAL),
                status,
                "",
                null,
                Set.of(),
                Set.copyOf(permissions),
                Set.copyOf(capabilities),
                issues
        );
    }

    private static EchoFeatureStatus statusFor(
            EchoFeatureId feature,
            List<EchoFeatureProviderNode> providers,
            List<EchoFeatureConsumerNode> requiredBy,
            List<EchoFeatureConsumerNode> optionalFor,
            EchoFeaturePolicy policy,
            boolean officialPack,
            List<EchoFeatureGraphIssue> issues
    ) {
        if (providers.isEmpty() && !requiredBy.isEmpty()) {
            issues.add(issue("ECHO-FEATURE-MISSING-REQUIRED", EchoDiagnosticSeverity.ERROR, feature, requiredBy.getFirst().moduleId(),
                    "Required feature has no provider.", "Enable or implement a module that provides this feature.", true));
            return EchoFeatureStatus.MISSING_REQUIRED;
        }
        if (providers.isEmpty() && !optionalFor.isEmpty()) {
            issues.add(issue("ECHO-FEATURE-MISSING-OPTIONAL", EchoDiagnosticSeverity.WARNING, feature, optionalFor.getFirst().moduleId(),
                    "Optional feature has no provider.", "Install an optional provider or accept degraded integration.", false));
            return EchoFeatureStatus.MISSING_OPTIONAL;
        }
        if (providers.size() > 1 && policy.exclusive(feature)) {
            issues.add(issue("ECHO-FEATURE-EXCLUSIVE-PROVIDER-CONFLICT", EchoDiagnosticSeverity.ERROR, feature, providers.getFirst().moduleId(),
                    "Exclusive feature has multiple providers.", "Choose one provider in pack policy.", true));
            return EchoFeatureStatus.CONFLICTED;
        }
        for (EchoFeatureProviderNode provider : providers) {
            if (policy.trustBlocked(provider.trustLevel(), officialPack)) {
                issues.add(issue("ECHO-FEATURE-TRUST-BLOCKED", EchoDiagnosticSeverity.ERROR, feature, provider.moduleId(),
                        "Feature provider is blocked by pack trust policy.", "Use an official or trusted provider.", true));
                return EchoFeatureStatus.TRUST_BLOCKED;
            }
        }
        if (!providers.isEmpty() && requiredBy.isEmpty() && optionalFor.isEmpty()) {
            return EchoFeatureStatus.UNUSED;
        }
        return providers.isEmpty() ? EchoFeatureStatus.UNKNOWN : EchoFeatureStatus.PROVIDED;
    }

    private static EchoFeatureGraphIssue issue(
            String code,
            EchoDiagnosticSeverity severity,
            EchoFeatureId feature,
            EchoModuleId module,
            String summary,
            String suggestedFix,
            boolean blocking
    ) {
        return new EchoFeatureGraphIssue(code, severity, feature, module, summary, suggestedFix, blocking, List.of("docs/echo/validation/ECHO_FEATURE_GRAPH.md"));
    }

    private static EchoApiStability parseStability(String value) {
        if (value == null || value.isBlank()) {
            return EchoApiStability.EXPERIMENTAL;
        }
        for (EchoApiStability stability : EchoApiStability.values()) {
            if (stability.serializedName().equalsIgnoreCase(value) || stability.name().equalsIgnoreCase(value)) {
                return stability;
            }
        }
        return EchoApiStability.EXPERIMENTAL;
    }

    private static Set<String> splitAttribute(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        return values;
    }
}

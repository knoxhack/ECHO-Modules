package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoFeatureGraph(
        Map<EchoFeatureId, EchoFeatureNode> nodes,
        List<EchoFeatureGraphNode> graphNodes,
        List<EchoFeatureRequirementEdge> requirementEdges,
        List<EchoFeatureGraphIssue> featureIssues,
        List<EchoFeatureEdge> edges,
        List<EchoModuleGraphIssue> issues
) {
    public EchoFeatureGraph {
        nodes = ModuleGraphContractGuards.immutableMap(nodes);
        graphNodes = ModuleGraphContractGuards.immutableList(graphNodes);
        requirementEdges = ModuleGraphContractGuards.immutableList(requirementEdges);
        featureIssues = ModuleGraphContractGuards.immutableList(featureIssues);
        edges = ModuleGraphContractGuards.immutableList(edges);
        issues = ModuleGraphContractGuards.immutableList(issues);
    }

    public static EchoFeatureGraph fromModules(Collection<EchoScannedModule> modules) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        EchoFeatureProviderIndex providers = EchoFeatureProviderIndex.fromModules(safeModules);
        EchoFeatureConsumerIndex consumers = EchoFeatureConsumerIndex.fromModules(safeModules);
        return fromIndexes(safeModules, providers, consumers, EchoExclusiveFeaturePolicy.defaults(), false);
    }

    static EchoFeatureGraph fromIndexes(
            Collection<EchoScannedModule> modules,
            EchoFeatureProviderIndex providers,
            EchoFeatureConsumerIndex consumers,
            EchoFeaturePolicy policy,
            boolean officialPack
    ) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        Map<EchoFeatureId, EchoFeatureNode> nodes = new HashMap<>();
        List<EchoFeatureGraphNode> graphNodes = new ArrayList<>();
        List<EchoFeatureRequirementEdge> requirementEdges = new ArrayList<>();
        List<EchoFeatureGraphIssue> featureIssues = new ArrayList<>();
        List<EchoFeatureEdge> edges = new ArrayList<>();
        List<EchoModuleGraphIssue> issues = new ArrayList<>();
        Set<EchoFeatureId> allFeatures = new HashSet<>();
        allFeatures.addAll(providers.providersByFeature().keySet());
        allFeatures.addAll(consumers.consumersByFeature().keySet());
        for (EchoFeatureId feature : allFeatures.stream().sorted(Comparator.comparing(EchoFeatureId::value)).toList()) {
            boolean required = !consumers.requiredConsumersFor(feature).isEmpty();
            EchoFeatureGraphNode graphNode = EchoFeatureGraphBuilder.nodeFor(
                    feature,
                    safeModules,
                    providers,
                    consumers,
                    policy,
                    officialPack
            );
            graphNodes.add(graphNode);
            featureIssues.addAll(graphNode.issues());
            EchoFeatureNode node = new EchoFeatureNode(
                    feature,
                    feature.value(),
                    providers.providersFor(feature),
                    consumers.consumersFor(feature),
                    consumers.requiredConsumersFor(feature),
                    consumers.optionalConsumersFor(feature),
                    required,
                    graphNode.runtimeSides(),
                    graphNode.apiStability().serializedName(),
                    graphNode.status(),
                    graphNode.deprecationInfo(),
                    graphNode.replacementFeature(),
                    graphNode.conflicts(),
                    graphNode.permissions(),
                    graphNode.capabilities(),
                    List.of()
            );
            nodes.put(feature, node);
            if (node.missingProvider()) {
                issues.add(new EchoModuleGraphIssue(
                        EchoModuleGraphIssueKind.FEATURE_PROVIDER_MISSING,
                        null,
                        consumers.requiredConsumersFor(feature).stream().findFirst().orElse(null),
                        null,
                        null,
                        feature,
                        "",
                        "Required feature has no provider: " + feature.value(),
                        "",
                        "Install or enable a module that provides " + feature.value() + ".",
                        false,
                        List.of("docs/echo/validation/ECHO_FEATURE_GRAPH.md")
                ));
            }
            if (providers.providersFor(feature).size() > 1) {
                issues.add(new EchoModuleGraphIssue(
                        EchoModuleGraphIssueKind.FEATURE_PROVIDER_CONFLICT,
                        null,
                        providers.providersFor(feature).stream().findFirst().orElse(null),
                        null,
                        null,
                        feature,
                        "",
                        "Multiple modules provide feature: " + feature.value(),
                        "",
                        "Check pack composition rules or mark one provider as preferred.",
                        false,
                        List.of("docs/echo/validation/ECHO_FEATURE_GRAPH.md")
                ));
            }
        }
        for (EchoScannedModule module : safeModules) {
            for (EchoFeatureId feature : module.providedFeatures()) {
                edges.add(new EchoFeatureEdge(feature, module.moduleId(), null, EchoFeatureEdgeKind.PROVIDES, false, ""));
            }
            for (EchoFeatureRequirement requirement : module.consumedFeatures()) {
                EchoModuleId provider = providers.providersFor(requirement.featureId()).stream().findFirst().orElse(null);
                requirementEdges.add(new EchoFeatureRequirementEdge(
                        requirement.featureId(),
                        module.moduleId(),
                        provider,
                        requirement.required(),
                        requirement.versionRange(),
                        requirement.reason()
                ));
                edges.add(new EchoFeatureEdge(
                        requirement.featureId(),
                        module.moduleId(),
                        provider,
                        requirement.required() ? EchoFeatureEdgeKind.REQUIRES : EchoFeatureEdgeKind.OPTIONAL_CONSUMES,
                        requirement.required(),
                        requirement.reason()
                ));
            }
        }
        requirementEdges.sort(Comparator
                .comparing((EchoFeatureRequirementEdge edge) -> edge.featureId().value())
                .thenComparing(edge -> edge.consumerModuleId().value()));
        edges.sort(Comparator
                .comparing((EchoFeatureEdge edge) -> edge.featureId().value())
                .thenComparing(edge -> edge.fromModule().value())
                .thenComparing(edge -> edge.kind().serializedName()));
        return new EchoFeatureGraph(nodes, graphNodes, requirementEdges, featureIssues, edges, issues);
    }

    public List<EchoFeatureNode> missingProviderNodes() {
        return nodes.values().stream().filter(EchoFeatureNode::missingProvider).toList();
    }

    public Set<EchoRuntimeSide> runtimeSidesFor(EchoFeatureId featureId) {
        EchoFeatureNode node = nodes.get(featureId);
        return node == null ? Set.of() : node.runtimeSides();
    }
}

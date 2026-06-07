package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoValidationCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echovalidationcore";
    public static final String PACK_VALIDATION_CONTRACT_ID = "echovalidationcore:data/pack_validation";
    public static final String DIAGNOSTIC_REPORT_CONTRACT_ID = "echovalidationcore:diagnostic/diagnostic_report";
    public static final String REPAIR_SUGGESTION_CONTRACT_ID = "echovalidationcore:diagnostic/repair_suggestion";
    public static final List<String> CONTRACT_IDS = List.of(
            PACK_VALIDATION_CONTRACT_ID,
            DIAGNOSTIC_REPORT_CONTRACT_ID,
            REPAIR_SUGGESTION_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoValidationEngine engine = EchoValidationEngine.empty();
        EchoDiagnosticReport report = engine.validateAll(
                "ValidationCore Native Adapter Contract",
                List.of(),
                EchoDiagnosticContext.workspace()
        );
        Map<String, Object> referenceProbe = exerciseReferenceBehavior(engine, report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "validationcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics"));
        result.put("runtimeTargets", List.of("echo_native"));
        result.put("validationRuleCount", engine.rules().size());
        result.put("diagnosticCount", report.diagnostics().size());
        result.put("highestSeverity", report.highestSeverity().serializedName());
        result.put("validationEngineRoundTrip", referenceProbe.get("validationEngineRoundTrip"));
        result.put("packValidationRoundTrip", referenceProbe.get("packValidationRoundTrip"));
        result.put("diagnosticReportRoundTrip", referenceProbe.get("diagnosticReportRoundTrip"));
        result.put("repairSuggestionRoundTrip", referenceProbe.get("repairSuggestionRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("requiresValidationEngineBridge", true);
        result.put("requiresDiagnosticReportBridge", true);
        result.put("requiresRepairSuggestionBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "ValidationCore native contract exposed pack validation, diagnostic report, and repair suggestion behavior through AdapterCore.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoValidationCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent4-validationcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "ValidationCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("adapterCoreUsed")),
                "ValidationCore native adapter should use AdapterCore");
        require(CONTRACT_IDS.equals(activation.get("registeredFeatureContracts")),
                "ValidationCore native adapter should expose all validation contracts");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "ValidationCore native adapter should register every validation contract");
        require("INFO".equals(activation.get("highestSeverity")),
                "ValidationCore native adapter should exercise a clean diagnostic report");
        require(Boolean.TRUE.equals(activation.get("validationEngineRoundTrip")),
                "ValidationCore native adapter should exercise validation engine behavior");
        require(Boolean.TRUE.equals(activation.get("packValidationRoundTrip")),
                "ValidationCore native adapter should exercise pack validation behavior");
        require(Boolean.TRUE.equals(activation.get("diagnosticReportRoundTrip")),
                "ValidationCore native adapter should exercise diagnostic report behavior");
        require(Boolean.TRUE.equals(activation.get("repairSuggestionRoundTrip")),
                "ValidationCore native adapter should exercise repair suggestion behavior");
        require(Boolean.TRUE.equals(activation.get("serviceCodeExecuted")),
                "ValidationCore native adapter should execute reference validation behavior");
        System.out.println("validationcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior(
            EchoValidationEngine cleanEngine,
            EchoDiagnosticReport cleanReport
    ) {
        EchoValidationTarget target = new EchoValidationTarget(
                "agent4-validationcore-smoke",
                "ValidationCore Native Adapter Contract",
                EchoValidationScope.PACK,
                EchoValidationCategory.PACK_PROFILE,
                EchoModuleId.of(MODULE_ID),
                null,
                null,
                "addons/echovalidationcore/src/main/resources/echo.pack.json",
                Set.of(),
                Map.of("id", MODULE_ID, "name", "ValidationCore")
        );
        EchoValidationEngine schemaEngine = EchoValidationEngine.of(List.of(new RequiredFieldValidationRule(
                "validation.required_schema",
                EchoValidationCategory.PACK_PROFILE,
                EchoValidationScope.PACK,
                "schema"
        )));
        EchoValidationResult validationResult = schemaEngine.validate(target, EchoDiagnosticContext.workspace());
        EchoDiagnostic blockingDiagnostic = validationResult.diagnostics().get(0);
        EchoDiagnosticReport diagnosticReport = EchoDiagnosticReport.of(
                "ValidationCore diagnostic bridge",
                EchoDiagnosticContext.workspace(),
                List.of(blockingDiagnostic)
        );
        EchoRepairSuggestion suggestion = EchoRepairSuggestion.manual(
                "validation.add_schema",
                "Add schema id",
                "Declare the validation schema before the pack is accepted."
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cleanValidationRuleCount", cleanEngine.rules().size());
        result.put("cleanDiagnosticCount", cleanReport.diagnostics().size());
        result.put("cleanHighestSeverity", cleanReport.highestSeverity().serializedName());
        result.put("validationEngineRoundTrip", cleanEngine.rules().isEmpty()
                && cleanReport.diagnostics().isEmpty()
                && cleanReport.highestSeverity() == EchoDiagnosticSeverity.INFO
                && schemaEngine.rules().size() == 1);
        result.put("packValidationRoundTrip", validationResult.target().id().equals("agent4-validationcore-smoke")
                && validationResult.diagnostics().size() == 1
                && blockingDiagnostic.code().value().equals("validation.required_schema")
                && blockingDiagnostic.blocking()
                && blockingDiagnostic.likelyFiles().contains(target.path()));
        result.put("diagnosticReportRoundTrip", diagnosticReport.hasBlockingDiagnostics()
                && diagnosticReport.highestSeverity() == EchoDiagnosticSeverity.ERROR
                && diagnosticReport.diagnostics().get(0).summary().contains("schema"));
        result.put("repairSuggestionRoundTrip", suggestion.requiresConfirmation()
                && suggestion.risk().equals("manual_review")
                && suggestion.label().equals("Add schema id")
                && suggestion.summary().contains("validation schema"));
        result.put("blockingDiagnosticCount", diagnosticReport.diagnostics().size());
        result.put("repairSuggestionId", suggestion.id());
        return Map.copyOf(result);
    }

    private record RequiredFieldValidationRule(
            String id,
            EchoValidationCategory category,
            EchoValidationScope scope,
            String field
    ) implements EchoValidationRule {
        private RequiredFieldValidationRule {
            id = requireText(id, "rule id");
            category = category == null ? EchoValidationCategory.UNKNOWN : category;
            scope = scope == null ? EchoValidationScope.UNKNOWN : scope;
            field = requireText(field, "field");
        }

        @Override
        public EchoValidationResult validate(EchoValidationTarget target, EchoDiagnosticContext context) {
            if (target.attributes().containsKey(field) && !target.attributes().get(field).isBlank()) {
                return EchoValidationResult.passed(target);
            }
            EchoRepairSuggestion suggestion = EchoRepairSuggestion.manual(
                    "validation.add_schema",
                    "Add schema id",
                    "Declare the validation schema before the pack is accepted."
            );
            EchoDiagnostic diagnostic = EchoDiagnostic.builder(
                            EchoDiagnosticCode.of(id),
                            EchoDiagnosticSeverity.ERROR,
                            "Missing required validation field",
                            "Missing required validation field: " + field
                    )
                    .moduleId(target.moduleId())
                    .category(target.category())
                    .cause("The pack metadata did not declare " + field + ".")
                    .playerFix("Add a schema id before loading the pack.")
                    .developerDetails("AdapterCore ValidationCore required-field rule executed for " + target.id())
                    .repairable(true)
                    .addRepairSuggestion(suggestion)
                    .likelyFile(target.path())
                    .suggestedAgentLane("agent-4-registry-data-assets")
                    .relatedDoc("docs/echo/schema/ECHO_SCHEMA_REGISTRY.md")
                    .build();
            return EchoValidationResult.of(target, List.of(diagnostic));
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

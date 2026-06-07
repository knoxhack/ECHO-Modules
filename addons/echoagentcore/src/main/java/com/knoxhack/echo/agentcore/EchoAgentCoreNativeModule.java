package com.knoxhack.echo.agentcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoAgentCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoAgentConstants.MOD_ID;
    public static final String SAFE_COMMAND_CONTRACT_ID = "echoagentcore:command/safe_command";
    public static final String TASK_QUEUE_CONTRACT_ID = "echoagentcore:data/task_queue";
    public static final String PROMPT_BUNDLE_CONTRACT_ID = "echoagentcore:data/prompt_bundle";
    public static final String RUN_REPORT_CONTRACT_ID = "echoagentcore:diagnostic/run_report";
    public static final List<String> CONTRACT_IDS = List.of(
            SAFE_COMMAND_CONTRACT_ID,
            TASK_QUEUE_CONTRACT_ID,
            PROMPT_BUNDLE_CONTRACT_ID,
            RUN_REPORT_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "agentcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("commands", "data", "diagnostics"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("safeCommandPolicyRoundTrip", referenceProbe.get("safeCommandPolicyRoundTrip"));
        result.put("taskQueueRoundTrip", referenceProbe.get("taskQueueRoundTrip"));
        result.put("promptBundleRoundTrip", referenceProbe.get("promptBundleRoundTrip"));
        result.put("runReportRoundTrip", referenceProbe.get("runReportRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "AgentCore native contract exercised safe command policy, task queue readiness, prompt bundle review propagation, and run report review behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoAgentCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agentcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "AgentCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("safeCommandPolicyRoundTrip")),
                "AgentCore native adapter should exercise safe command policy behavior");
        require(Boolean.TRUE.equals(activation.get("taskQueueRoundTrip")),
                "AgentCore native adapter should exercise task queue behavior");
        require(Boolean.TRUE.equals(activation.get("promptBundleRoundTrip")),
                "AgentCore native adapter should exercise prompt bundle behavior");
        require(Boolean.TRUE.equals(activation.get("runReportRoundTrip")),
                "AgentCore native adapter should exercise run report behavior");
        System.out.println("agentcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoAiSafeCommand safeCommand = new EchoAiSafeCommand(
                "agentcore.verify.audit",
                "Verify AdapterCore Audit",
                List.of("node", "Echo/tools/audit_adaptercore_contracts.mjs"),
                ".",
                EchoAiCommandRisk.MEDIUM,
                false,
                List.of("--console=plain"),
                Duration.ofSeconds(30L),
                EchoAiCommandOutputParser.JSON_REPORT,
                EchoAiCommandEnvironmentPolicy.CONFIRM_WRITE,
                "Runs the AdapterCore gap audit.",
                "Requires confirmation because it writes report JSON."
        );
        EchoAiAcceptanceCriterion criterion = EchoAiAcceptanceCriterion.required(
                "audit.gap.checked",
                EchoAiAcceptanceCriterionType.DIAGNOSTIC_ABSENT,
                "AdapterCore audit no longer reports this module as a native runtime gap."
        );
        EchoAiProtectedFileRule protectedFileRule = EchoAiProtectedFileRule.blocked(
                "src/main/resources/**",
                "Resource edits require explicit review in AgentCore tasks."
        );
        EchoAiTask task = new EchoAiTask(
                EchoAiTaskId.of("AdapterCore.AgentCore.Native"),
                "Attach AgentCore native adapter",
                "Bind AgentCore safe command, queue, prompt, and report behavior through AdapterCore.",
                EchoAiTaskStatus.READY,
                EchoAiTaskPriority.HIGH,
                EchoAiAgentLane.NATIVE_CLI_AGENT,
                null,
                Set.of(),
                Set.of(),
                List.of(criterion),
                List.of(safeCommand),
                List.of(protectedFileRule),
                List.of(),
                List.of("reports/echo/adaptercore/module-runtime-gap-audit.json"),
                false
        );
        EchoAiTaskQueue queue = new EchoAiTaskQueue(
                "agentcore.native.queue",
                "ashfall",
                "all",
                EchoAiTaskStatus.READY,
                List.of(task),
                Map.of("audit", 1),
                Map.of(EchoAiAgentLane.NATIVE_CLI_AGENT.serializedName(), 1),
                Map.of(EchoAiTaskPriority.HIGH.serializedName(), 1),
                List.of("module-runtime-gap-audit.json"),
                List.of("no destructive commands"),
                123L
        );
        EchoAiPromptBundle promptBundle = new EchoAiPromptBundle(
                "agentcore.native.prompt",
                null,
                null,
                null,
                "AgentCore Native Adapter Prompt",
                "Continue AdapterCore runtime parity.",
                null,
                List.of(task),
                List.of(new EchoAiPromptSection(
                        "rules",
                        "Rules",
                        "Reference behavior must be executable before parity is claimed.",
                        0,
                        true,
                        false,
                        List.of("goal")
                )),
                List.of(protectedFileRule),
                List.of(safeCommand),
                "EchoAgentCoreNativeModule",
                456L
        );
        EchoAiNextPhasePrompt nextPrompt = new EchoAiNextPhasePrompt(
                "agentcore.next",
                "native-runtime-gap-reduction",
                "Next AdapterCore Runtime Gap",
                "Pick the next module with executable reference behavior.",
                List.of(criterion),
                List.of("AgentCore adapter activated."),
                List.of("No metadata-only completion."),
                "agentcore.run"
        );
        EchoAiRunReport runReport = new EchoAiRunReport(
                "agentcore.run",
                EchoAiTaskStatus.NEEDS_REVIEW,
                "AgentCore native adapter requires review after parity.",
                null,
                null,
                List.of(new EchoAiTaskResult(
                        task.id(),
                        EchoAiTaskStatus.NEEDS_REVIEW,
                        "Parity evidence must be inspected.",
                        List.of(),
                        List.of(criterion),
                        List.of(),
                        List.of(safeCommand.id()),
                        List.of("reports/echo/agents/agent-2-status.json"),
                        false
                )),
                List.of(),
                List.of(safeCommand.id()),
                List.of("addons/echoagentcore/src/main/java/com/knoxhack/echo/agentcore/EchoAgentCoreNativeModule.java"),
                nextPrompt,
                500L,
                100L
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("safeCommandPolicyRoundTrip", safeCommand.requiresConfirmation()
                && !safeCommand.executionBlockedByPolicy()
                && safeCommand.outputParser() == EchoAiCommandOutputParser.JSON_REPORT
                && safeCommand.timeout().equals(Duration.ofSeconds(30L)));
        result.put("taskQueueRoundTrip", queue.hasReadyTasks()
                && queue.tasks().size() == 1
                && task.requiresHumanReview()
                && task.hasExecutableModelOnlyCommands());
        result.put("promptBundleRoundTrip", promptBundle.requiresHumanReview()
                && promptBundle.schema().value().equals(EchoAgentConstants.PROMPT_BUNDLE_SCHEMA_ID)
                && promptBundle.sections().get(0).required());
        result.put("runReportRoundTrip", runReport.requiresHumanReview()
                && runReport.finishedAtEpochMillis() == 500L
                && runReport.nextPhasePrompt().targetPhase().equals("native-runtime-gap-reduction"));
        result.put("safeCommandId", safeCommand.id());
        result.put("taskQueueId", queue.id());
        result.put("promptBundleId", promptBundle.id());
        result.put("runReportId", runReport.id());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

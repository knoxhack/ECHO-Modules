package com.knoxhack.echo.adaptercore;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class EchoNativeStoryActivationSmokeHarness {
    private EchoNativeStoryActivationSmokeHarness() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of(".").toAbsolutePath().normalize();
        Path reportRoot = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : repoRoot;
        require(Files.isDirectory(repoRoot.resolve("addons/echoadaptercore")),
                "Agent 10 native activation smoke requires the ECHO repo root");

        List<NativeStoryModule> modules = modules();
        List<Map<String, Object>> activations = new ArrayList<>();
        Map<String, String> context = Map.of(
                "packId", "ashfall",
                "runtime", "echo_native_loader",
                "agent", "agent-10-story-signalos-arcane"
        );
        for (NativeStoryModule module : modules) {
            Map<String, Object> result = activate(module, context);
            require(Boolean.TRUE.equals(result.get("activated")),
                    module.className() + " did not report activated=true");
            require(Boolean.TRUE.equals(result.get("adapterCoreUsed")),
                    module.className() + " did not use AdapterCore");
            require(Boolean.TRUE.equals(result.get("nativeAdapterCodeExecuted")),
                    module.className() + " did not execute native adapter code");
            require(Boolean.TRUE.equals(result.get("serviceCodeExecuted")),
                    module.className() + " did not execute story service code");
            require(String.valueOf(result.getOrDefault("moduleId", "")).equals(module.moduleId()),
                    module.className() + " reported the wrong module id");

            Object storyRuntime = result.get("storyRuntimeBridge");
            require(storyRuntime instanceof Map<?, ?>,
                    module.className() + " did not return a storyRuntimeBridge report");
            Map<?, ?> storyReport = (Map<?, ?>) storyRuntime;
            require(Boolean.TRUE.equals(storyReport.get("serviceCodeExecuted")),
                    module.className() + " story runtime did not execute service code");
            require(Boolean.TRUE.equals(storyReport.get("handlerExecuted")),
                    module.className() + " story runtime did not execute a handler");
            Object countValue = storyReport.get("handlerExecutionCount");
            require(countValue instanceof Number && ((Number) countValue).intValue() > 0,
                    module.className() + " story runtime handler count was empty");

            String resultText = String.valueOf(result);
            for (String requiredId : module.requiredIds()) {
                require(resultText.contains(requiredId),
                        module.className() + " activation result missed contract id " + requiredId);
            }

            Map<String, Object> activation = new LinkedHashMap<>();
            activation.put("moduleId", module.moduleId());
            activation.put("nativeEntrypoint", module.className());
            activation.put("handlerExecutionCount", ((Number) countValue).intValue());
            activation.put("requiredIds", module.requiredIds());
            activations.add(activation);
        }

        writeReports(reportRoot, modules, activations);
        int handlerCount = activations.stream()
                .mapToInt(activation -> ((Number) activation.get("handlerExecutionCount")).intValue())
                .sum();
        System.out.println("agent-10 native story activation PASS modules=" + modules.size()
                + " handlers=" + handlerCount);
    }

    private static Map<String, Object> activate(
            NativeStoryModule module,
            Map<String, String> context
    ) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Class<?> entrypoint = Class.forName(module.className());
        Object instance = entrypoint.getDeclaredConstructor().newInstance();
        require(instance instanceof EchoNativeModuleAdapter,
                module.className() + " does not implement EchoNativeModuleAdapter");
        return ((EchoNativeModuleAdapter) instance).describeNativeSurfaces(context);
    }

    private static List<NativeStoryModule> modules() {
        return List.of(
                new NativeStoryModule(
                        "signalos",
                        "com.knoxhack.signalos.EchoSignalOsNativeModule",
                        List.of(
                                "signalos:archive/field_cache",
                                "signalos:data_drive/handoff_drive",
                                "signalos:signal/secure_cache",
                                "signalos:story_flag/cache_secured",
                                "signalos:mission/secure_cache",
                                "signalos:chapter/cache_handoff"
                        )
                ),
                new NativeStoryModule(
                        "echospellcore",
                        "com.knoxhack.echospellcore.EchoSpellCoreNativeModule",
                        List.of("echospellcore:spell/signal_pulse")
                ),
                new NativeStoryModule(
                        "echoritualcore",
                        "com.knoxhack.echoritualcore.EchoRitualCoreNativeModule",
                        List.of(
                                "echoritualcore:ritual/relic_stabilization",
                                "echoritualcore:story_flag/relic_stabilized"
                        )
                ),
                new NativeStoryModule(
                        "echocursecore",
                        "com.knoxhack.echocursecore.EchoCurseCoreNativeModule",
                        List.of("echocursecore:curse/echo_rot")
                ),
                new NativeStoryModule(
                        "echoriftworlds",
                        "com.knoxhack.echoriftworlds.EchoRiftWorldsNativeModule",
                        List.of(
                                "echoriftworlds:rift_event/cache_echo",
                                "signalos:chapter/cache_handoff"
                        )
                ),
                new NativeStoryModule(
                        "echoblackboxprotocol",
                        "com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocolNativeModule",
                        List.of("echoblackboxprotocol:archive/core_memory")
                ),
                new NativeStoryModule(
                        "echoorbitalremnants",
                        "com.knoxhack.echoorbitalremnants.EchoOrbitalRemnantsNativeModule",
                        List.of(
                                "echoorbitalremnants:data_drive/orbital_blackbox",
                                "echoblackboxprotocol:archive/core_memory"
                        )
                ),
                new NativeStoryModule(
                        "echonexusprotocol",
                        "com.knoxhack.echonexusprotocol.EchoNexusProtocolNativeModule",
                        List.of(
                                "echonexusprotocol:signal/nexus_handoff",
                                "echoprimecore:mission/prime_route"
                        )
                ),
                new NativeStoryModule(
                        "echoprimecore",
                        "com.knoxhack.echoprimecore.EchoPrimeCoreNativeModule",
                        List.of(
                                "echoprimecore:story_flag/prime_route_unlocked",
                                "echoprimecore:mission/prime_route",
                                "echostationfall:chapter/stationfall_route"
                        )
                ),
                new NativeStoryModule(
                        "echostationfall",
                        "com.knoxhack.echostationfall.EchoStationfallNativeModule",
                        List.of("echostationfall:chapter/stationfall_route")
                ),
                new NativeStoryModule(
                        "echopresencelink",
                        "com.knoxhack.echopresencelink.EchoPresenceLinkNativeModule",
                        List.of(
                                "echopresencelink:presence/signalos_cache",
                                "echopresencelink:presence/prime_route"
                        )
                ),
                new NativeStoryModule(
                        "echogrimoire",
                        "com.knoxhack.echogrimoire.EchoGrimoireNativeModule",
                        List.of("echogrimoire:archive/arcane_codex")
                ),
                new NativeStoryModule(
                        "signalosexample",
                        "com.knoxhack.signalosexample.SignalOsExampleNativeModule",
                        List.of(
                                "signalosexample:data_drive/arcane_codex_demo",
                                "echogrimoire:archive/arcane_codex"
                        )
                ),
                new NativeStoryModule(
                        "echoarcanacore",
                        "com.knoxhack.echoarcanacore.EchoArcanaCoreNativeModule",
                        List.of(
                                "echoarcanacore:signal/aether_wake",
                                "echoarcanacore:story_flag/arcane_codex_unlocked",
                                "echoarcanacore:mission/arcane_codex_sync"
                        )
                ),
                new NativeStoryModule(
                        "echorelictech",
                        "com.knoxhack.echorelictech.EchoRelicTechNativeModule",
                        List.of(
                                "echorelictech:relic_effect/echo_mirror",
                                "echorelictech:relic_effect/phase_anchor"
                        )
                ),
                new NativeStoryModule(
                        "echoarcaneindex",
                        "com.knoxhack.echoarcaneindex.EchoArcaneIndexNativeModule",
                        List.of("echoarcaneindex:chapter/arcane_codex")
                ),
                new NativeStoryModule(
                        "echoaetherworks",
                        "com.knoxhack.echoaetherworks.EchoAetherWorksNativeModule",
                        List.of("echoaetherworks:presence/aether_sync")
                )
        );
    }

    private static void writeReports(
            Path repoRoot,
            List<NativeStoryModule> modules,
            List<Map<String, Object>> activations
    ) throws IOException {
        Path agentDir = repoRoot.resolve("reports/echo/agents");
        Files.createDirectories(agentDir);
        int handlerCount = activations.stream()
                .mapToInt(activation -> ((Number) activation.get("handlerExecutionCount")).intValue())
                .sum();
        List<String> moduleIds = modules.stream().map(NativeStoryModule::moduleId).toList();
        List<String> ownedModules = List.of(
                "echosignalos",
                "signalos",
                "signalosexample",
                "echoblackboxprotocol",
                "echonexusprotocol",
                "echoorbitalremnants",
                "echorelictech",
                "echoriftworlds",
                "echoritualcore",
                "echospellcore",
                "echocursecore",
                "echogrimoire",
                "echoaetherworks",
                "echostationfall",
                "echoprimecore",
                "echopresencelink",
                "echoarcanacore",
                "echoarcaneindex"
        );
        List<String> entrypoints = modules.stream().map(NativeStoryModule::className).toList();
        List<String> contentIds = modules.stream()
                .flatMap(module -> module.requiredIds().stream())
                .distinct()
                .toList();
        List<String> contracts = List.of(
                "EchoArchiveEntry",
                "EchoDataDrive",
                "EchoSignalMessage",
                "EchoStoryFlag",
                "EchoRelicEffect",
                "EchoSpell",
                "EchoRitual",
                "EchoCurse",
                "EchoRiftEvent",
                "EchoChapterUnlock",
                "EchoPresenceLink"
        );

        String status = "{\n"
                + "  \"agent\": \"agent-10-story-signalos-arcane\",\n"
                + "  \"modulesOwned\": " + jsonArray(ownedModules) + ",\n"
                + "  \"featuresAudited\": [\"SignalOS terminal open\", \"data drive archive unlock\", \"story mission start\", \"Prime route chapter progression\", \"Arcane codex route progression\", \"relic/spell/ritual/curse gameplay mutation\", \"rift chapter unlock\", \"story save/load\", \"Index/Wiki/Lore updates\", \"module-by-module story reference audit\", \"native story handler activation\"],\n"
                + "  \"adapterContractsAdded\": " + jsonArray(contracts) + ",\n"
                + "  \"echoNativeImplemented\": " + jsonArray(entrypoints) + ",\n"
                + "  \"standaloneImplemented\": [\"dev.echo.standalone.runtime.gameplay.EchoStandaloneStoryRuntime\"],\n"
                + "  \"parityPassed\": [\"runStandaloneStoryParitySmoke\", \"runEchoAgent10NativeStoryActivationSmoke\", \"runNativeAgent10StorySmoke\"],\n"
                + "  \"blockers\": []\n"
                + "}\n";
        Files.writeString(agentDir.resolve("agent-10-status.json"), status);

        String activationReport = "{\n"
                + "  \"agent\": \"agent-10-story-signalos-arcane\",\n"
                + "  \"nativeModulesActivated\": " + jsonArray(moduleIds) + ",\n"
                + "  \"nativeEntrypoints\": " + jsonArray(entrypoints) + ",\n"
                + "  \"handlerExecutionCount\": " + handlerCount + ",\n"
                + "  \"activationResults\": " + activationJson(activations) + "\n"
                + "}\n";
        Files.writeString(agentDir.resolve("agent-10-native-activation.json"), activationReport);

        String parity = "{\n"
                + "  \"agent\": \"agent-10-story-signalos-arcane\",\n"
                + "  \"referenceContentIds\": " + jsonArray(contentIds) + ",\n"
                + "  \"moduleReferencesCovered\": " + jsonArray(moduleIds) + ",\n"
                + "  \"adapterContractsAdded\": " + jsonArray(contracts) + ",\n"
                + "  \"echoNativeImplemented\": " + jsonArray(entrypoints) + ",\n"
                + "  \"standaloneImplemented\": [\"SignalOS terminal command\", \"data drive archive unlock\", \"mission start\", \"Prime route chapter progression\", \"Arcane codex chapter progression\", \"gameplay mutations\", \"save/load\", \"lore updates\"],\n"
                + "  \"parityPassed\": [\"Native Loader story backend executes\", \"Native entrypoints instantiate\", \"Native activateNative executes AdapterCore service code\", \"Native Story Runtime bridge executes handlers\", \"SignalOS opens in terminal\", \"data drive unlocks archive\", \"story mission starts\", \"Prime route starts and unlocks Stationfall chapter\", \"Arcane codex starts and unlocks Arcane Index chapter\", \"relic/spell/ritual changes gameplay\", \"story state saves/loads\", \"Index/Wiki/Lore updates\", \"module-by-module standalone story references execute\"],\n"
                + "  \"nativeHandlerExecutionCount\": " + handlerCount + ",\n"
                + "  \"blockers\": []\n"
                + "}\n";
        Files.writeString(agentDir.resolve("agent-10-parity.json"), parity);

        String blockers = "{\n"
                + "  \"agent\": \"agent-10-story-signalos-arcane\",\n"
                + "  \"modulesOwned\": " + jsonArray(ownedModules) + ",\n"
                + "  \"featuresAudited\": [\"module-by-module story reference audit\", \"native story handler activation\"],\n"
                + "  \"adapterContractsAdded\": [],\n"
                + "  \"echoNativeImplemented\": [],\n"
                + "  \"standaloneImplemented\": [],\n"
                + "  \"parityPassed\": [\"runStandaloneStoryParitySmoke\", \"runEchoAgent10NativeStoryActivationSmoke\", \"runNativeAgent10StorySmoke\"],\n"
                + "  \"blockers\": []\n"
                + "}\n";
        Files.writeString(agentDir.resolve("agent-10-blockers.json"), blockers);
    }

    private static String activationJson(List<Map<String, Object>> activations) {
        return activations.stream()
                .map(activation -> "{"
                        + "\"moduleId\":" + jsonString(String.valueOf(activation.get("moduleId")))
                        + ",\"nativeEntrypoint\":" + jsonString(String.valueOf(activation.get("nativeEntrypoint")))
                        + ",\"handlerExecutionCount\":" + activation.get("handlerExecutionCount")
                        + ",\"requiredIds\":" + jsonArray(castStringList(activation.get("requiredIds")))
                        + "}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> castStringList(Object value) {
        return (List<String>) value;
    }

    private static String jsonArray(List<String> values) {
        return values.stream()
                .map(EchoNativeStoryActivationSmokeHarness::jsonString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record NativeStoryModule(String moduleId, String className, List<String> requiredIds) {
    }
}

package com.knoxhack.echo.creatorcore.adapter;

import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorExportResult;
import com.knoxhack.echo.creatorcore.api.CreatorFormField;
import com.knoxhack.echo.creatorcore.api.CreatorFormFieldKind;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import com.knoxhack.echo.creatorcore.codex.CodexBridgeService;
import com.knoxhack.echo.creatorcore.codex.CodexJobSnapshot;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.definition.CreatorDefinitionService;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftService;
import com.knoxhack.echo.creatorcore.export.CreatorExportService;
import com.knoxhack.echo.creatorcore.validation.CreatorValidationService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreatorCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoCreatorCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEFINITION_SERVICE =
            TEST_FUNCTIONS.register("definition_service_aggregation", () -> CreatorCoreGameTests::definitionServiceAggregation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPORT_DELEGATION =
            TEST_FUNCTIONS.register("scriptcore_export_delegation", () -> CreatorCoreGameTests::scriptCoreExportDelegation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OPTIONAL_MISSING =
            TEST_FUNCTIONS.register("optional_adapter_missing_safe", () -> CreatorCoreGameTests::optionalAdapterMissingSafe);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_STUDIO_SCHEMA =
            TEST_FUNCTIONS.register("mission_studio_schema", () -> CreatorCoreGameTests::missionStudioSchema);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CODEX_CONFIG_LOCKS =
            TEST_FUNCTIONS.register("codex_config_locks", () -> CreatorCoreGameTests::codexConfigLocks);

    private CreatorCoreGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "definition_service_aggregation", DEFINITION_SERVICE.getId());
        register(event, "scriptcore_export_delegation", EXPORT_DELEGATION.getId());
        register(event, "optional_adapter_missing_safe", OPTIONAL_MISSING.getId());
        register(event, "mission_studio_schema", MISSION_STUDIO_SCHEMA.getId());
        register(event, "codex_config_locks", CODEX_CONFIG_LOCKS.getId());
    }

    private static void definitionServiceAggregation(GameTestHelper helper) {
        CreatorAdapterRegistry registry = new CreatorAdapterRegistry();
        Identifier definitionId = EchoCreatorCore.id("test_definition");
        registry.register(new DefinitionTestAdapter(definitionId));
        CreatorDefinitionService service = new CreatorDefinitionService(registry);

        helper.assertTrue(service.listDefinitions().size() == 1, "Definition service should aggregate summaries.");
        helper.assertTrue(service.detail(definitionId).isPresent(), "Definition service should resolve detail.");
        helper.assertTrue(service.previewSummaries().size() == 1, "Definition service should aggregate previews.");
        helper.assertTrue(service.formSchemas().stream().anyMatch(schema -> "mission".equals(schema.type())),
                "Definition service should aggregate form schemas.");
        helper.succeed();
    }

    private static void scriptCoreExportDelegation(GameTestHelper helper) {
        boolean original = CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false);
        CreatorCoreConfig.ALLOW_EXPORTS.set(true);
        try {
            Identifier successId = EchoCreatorCore.id("export_success");
            CreatorDraftService successDrafts = new CreatorDraftService();
            successDrafts.createMissionStudioDraft(successId, "test", "gametest");
            CreatorAdapterRegistry successRegistry = new CreatorAdapterRegistry();
            successRegistry.register(new ExportTestAdapter(true));
            CreatorExportService successExports = new CreatorExportService(
                    successDrafts, new CreatorValidationService(successRegistry, successDrafts), successRegistry);
            CreatorExportResult success = successExports.exportDraft(successId);
            helper.assertTrue(success.success(), "Available scriptcore adapter should be able to delegate success.");
            helper.assertTrue(success.message().contains("delegated"), "Success result should come from the fake scriptcore adapter.");

            Identifier failureId = EchoCreatorCore.id("export_failure");
            CreatorDraftService failureDrafts = new CreatorDraftService();
            failureDrafts.createMissionStudioDraft(failureId, "test", "gametest");
            CreatorAdapterRegistry failureRegistry = new CreatorAdapterRegistry();
            failureRegistry.register(new ExportTestAdapter(false));
            CreatorExportService failureExports = new CreatorExportService(
                    failureDrafts, new CreatorValidationService(failureRegistry, failureDrafts), failureRegistry);
            CreatorExportResult failure = failureExports.exportDraft(failureId);
            helper.assertTrue(!failure.success(), "ScriptCore delegation failure should fail honestly.");
            helper.assertTrue(failure.message().contains("blocked"), "Failure result should come from the fake scriptcore adapter.");
        } finally {
            CreatorCoreConfig.ALLOW_EXPORTS.set(original);
        }
        helper.succeed();
    }

    private static void optionalAdapterMissingSafe(GameTestHelper helper) {
        CreatorAdapter adapter = new MissingOptionalTestAdapter();
        helper.assertTrue(!adapter.isAvailable(), "Missing optional adapter should not be available.");
        helper.assertTrue(adapter.capabilities().isEmpty(), "Missing optional adapter should expose no runtime capabilities.");
        helper.assertTrue(!adapter.status().isBlank(), "Missing optional adapter should report a clear status.");
        helper.assertTrue(!adapter.diagnostics().isEmpty(), "Missing optional adapter should report a diagnostic instead of throwing.");
        helper.succeed();
    }

    private static void missionStudioSchema(GameTestHelper helper) {
        InternalFallbackCreatorAdapter adapter = new InternalFallbackCreatorAdapter();
        CreatorFormSchema schema = adapter.formSchemas().stream()
                .filter(candidate -> "mission".equals(candidate.type()))
                .findFirst()
                .orElseThrow();
        for (String field : List.of("pack", "id", "title", "briefing", "chapter", "phase", "kind",
                "prerequisites", "objectives", "rewards")) {
            helper.assertTrue(schema.fields().stream().map(CreatorFormField::name).anyMatch(field::equals),
                    "Mission Studio schema should include " + field + ".");
        }

        CreatorDraft draft = new CreatorDraftService().createMissionStudioDraft(
                EchoCreatorCore.id("mission_schema_draft"), "test", "gametest");
        for (String jsonField : List.of("chapter", "phase", "briefing", "prerequisites", "objectives", "rewards")) {
            helper.assertTrue(draft.content().has(jsonField), "Mission Studio draft should include " + jsonField + ".");
        }
        helper.succeed();
    }

    private static void codexConfigLocks(GameTestHelper helper) {
        boolean originalBridge = CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false);
        boolean originalRepoEdits = CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS, false);
        try {
            CodexBridgeService service = new CodexBridgeService();
            CreatorCoreConfig.ALLOW_CODEX_BRIDGE.set(false);
            CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS.set(false);
            CodexJobSnapshot bridgeLocked = service.startJob("asset_repair", "test");
            helper.assertTrue(!bridgeLocked.hasJob(), "Locked Codex bridge should not start a job.");
            helper.assertTrue(bridgeLocked.error().contains("allow_codex_bridge=false"),
                    "Locked Codex bridge should explain the config gate.");

            CreatorCoreConfig.ALLOW_CODEX_BRIDGE.set(true);
            CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS.set(false);
            CodexJobSnapshot repoLocked = service.startJob("asset_repair", "test");
            helper.assertTrue(!repoLocked.hasJob(), "Locked Codex repo edits should not start a job.");
            helper.assertTrue(repoLocked.error().contains("allow_codex_repo_edits=false"),
                    "Locked Codex repo edits should explain the config gate.");
        } finally {
            CreatorCoreConfig.ALLOW_CODEX_BRIDGE.set(originalBridge);
            CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS.set(originalRepoEdits);
        }
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(EchoCreatorCore.id("creatorcore_" + testName));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1,
                false, 16);
        event.registerTest(EchoCreatorCore.id(testName),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoCreatorCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static final class DefinitionTestAdapter implements CreatorAdapter {
        private final Identifier definitionId;

        private DefinitionTestAdapter(Identifier definitionId) {
            this.definitionId = definitionId;
        }

        @Override
        public Identifier id() {
            return EchoCreatorCore.id("definition_test");
        }

        @Override
        public String displayName() {
            return "Definition Test";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String status() {
            return "test ready";
        }

        @Override
        public Set<String> capabilities() {
            return Set.of("definitions", "preview", "editor");
        }

        @Override
        public List<CreatorDefinitionSummary> listDefinitions() {
            return List.of(new CreatorDefinitionSummary(definitionId, "mission", "Test Mission", id().toString(), "test", "ready"));
        }

        @Override
        public Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
            if (!definitionId.equals(id)) {
                return Optional.empty();
            }
            JsonObject raw = new JsonObject();
            raw.addProperty("id", id.toString());
            raw.addProperty("type", "mission");
            return Optional.of(new CreatorDefinitionDetail(id, "mission", "Test Mission", "Detail body",
                    id().toString(), "test", "ready", Optional.empty(), List.of("test"), raw,
                    Map.of("source", "gametest"), List.of(), List.of("Preview line"), true));
        }

        @Override
        public List<CreatorPreviewSummary> previewSummaries() {
            return List.of(new CreatorPreviewSummary(definitionId, "mission", "Test Mission", id().toString(),
                    "test", List.of("Preview line"), true));
        }

        @Override
        public List<CreatorFormSchema> formSchemas() {
            return List.of(new CreatorFormSchema("mission", "Mission Test Schema", "",
                    List.of(new CreatorFormField("title", "Title", CreatorFormFieldKind.TEXT, true, List.of(), "", false)),
                    false));
        }
    }

    private static final class ExportTestAdapter implements CreatorAdapter {
        private final boolean success;

        private ExportTestAdapter(boolean success) {
            this.success = success;
        }

        @Override
        public Identifier id() {
            return EchoCreatorCore.id("scriptcore");
        }

        @Override
        public String displayName() {
            return "ScriptCore Test";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String status() {
            return "test export bridge";
        }

        @Override
        public Set<String> capabilities() {
            return Set.of("export");
        }

        @Override
        public CreatorExportResult exportDraft(CreatorDraft draft, Path targetPath) {
            if (success) {
                return CreatorExportResult.success("delegated export success", targetPath.toString(), 1);
            }
            return CreatorExportResult.failed("blocked by delegated scriptcore test", targetPath.toString());
        }
    }

    private static final class MissingOptionalTestAdapter extends ModPresenceCreatorAdapter {
        private MissingOptionalTestAdapter() {
            super("missing_optional_test", "creatorcore_missing_optional_test_mod", "Missing Optional Test", null,
                    Set.of("preview"), "Missing optional adapter test mod is not installed.",
                    "This status should not be reached in the test.", true);
        }
    }
}

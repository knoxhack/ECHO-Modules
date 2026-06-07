package com.knoxhack.echo.scriptcore.api;

import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreRuntimeMigrationService;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreRuntimeStateService;
import com.knoxhack.echo.scriptcore.validation.EchoScriptValidator;
import java.util.List;

public final class EchoScriptCoreApi {
    private static final EchoScriptCoreApi INSTANCE = new EchoScriptCoreApi();

    private EchoScriptCoreApi() {
    }

    public static EchoScriptCoreApi get() {
        return INSTANCE;
    }

    public EchoScriptRegistryView registry() {
        return EchoScriptRegistry.INSTANCE;
    }

    public EchoScriptLoadResult reloadAll() {
        return EchoScriptReloader.INSTANCE.reloadAll();
    }

    public EchoScriptLoadResult reloadPack(String pack) {
        return EchoScriptReloader.INSTANCE.reloadPack(pack);
    }

    public EchoScriptLoadResult reloadType(String type) {
        return EchoScriptReloader.INSTANCE.reloadType(type);
    }

    public EchoScriptLoadResult lastResult() {
        return EchoScriptReloader.INSTANCE.lastResult();
    }

    public EchoScriptDiagnosticsSummary diagnosticsSummary() {
        List<EchoScriptDiagnostic> diagnostics = lastResult().diagnostics().isEmpty()
                ? EchoScriptValidator.INSTANCE.validate(EchoScriptRegistry.INSTANCE.all())
                : lastResult().diagnostics();
        EchoScriptRuntimeState runtime = runtimeState();
        return new EchoScriptDiagnosticsSummary(
                EchoScriptRegistry.INSTANCE.countByPack().keySet().stream().sorted().toList(),
                EchoScriptRegistry.INSTANCE.all().size(),
                diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR).count(),
                diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.WARNING).count(),
                diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.INFO).count(),
                EchoScriptAdapterRegistry.INSTANCE.adapters().stream()
                        .filter(adapter -> !adapter.isAvailable())
                        .map(adapter -> adapter.id().getPath())
                        .sorted()
                        .toList(),
                diagnostics.stream().filter(d -> d.code().contains("BROKEN")).count(),
                diagnostics.stream().filter(d -> "SCRIPTCORE_CIRCULAR_PREREQUISITE".equals(d.code())).count(),
                diagnostics.stream().filter(d -> "SCRIPTCORE_INVALID_OBJECTIVE".equals(d.code())).count(),
                diagnostics.stream().filter(d -> "SCRIPTCORE_UNKNOWN_ACTION".equals(d.code())).count(),
                diagnostics.stream().filter(d -> "SCRIPTCORE_UNKNOWN_CONDITION".equals(d.code())).count(),
                diagnostics.stream().filter(d -> d.message().contains("HoloMap marker")).count(),
                diagnostics.stream().filter(d -> d.message().contains("no obvious unlock path")).count(),
                diagnostics.stream().filter(d -> d.message().contains("may never trigger")).count(),
                runtime.available(),
                runtime.backendName());
    }

    public EchoScriptRuntimeState runtimeState() {
        return ScriptCoreRuntimeStateService.INSTANCE;
    }

    public EchoScriptRuntimeMigrationService runtimeMigrations() {
        return ScriptCoreRuntimeMigrationService.INSTANCE;
    }

    public List<EchoScriptDiagnostic> validateAll() {
        return EchoScriptValidator.INSTANCE.validate(EchoScriptRegistry.INSTANCE.all());
    }

    public EchoScriptAdapterRegistry adapters() {
        return EchoScriptAdapterRegistry.INSTANCE;
    }

    public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        return EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(condition, context);
    }

    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        return EchoScriptAdapterRegistry.INSTANCE.executeAction(action, context);
    }

    public void registerAdapter(EchoScriptAdapter adapter) {
        EchoScriptAdapterRegistry.INSTANCE.register(adapter);
    }
}

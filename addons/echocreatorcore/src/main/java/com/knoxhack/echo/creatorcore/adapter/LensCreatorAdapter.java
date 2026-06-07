package com.knoxhack.echo.creatorcore.adapter;

import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class LensCreatorAdapter extends ModPresenceCreatorAdapter {
    private static final String REGISTRY_CLASS = "com.knoxhack.echolens.registry.LensProviderRegistry";
    private static final String SCRIPT_API_CLASS = "com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi";

    public LensCreatorAdapter() {
        super("lens", "echolens", "ECHO: Lens", null,
                Set.of("definitions", "diagnostics", "preview"),
                "Lens not installed; lens_scan drafts remain generic JSON templates.",
                "Lens detected; provider diagnostics and read-only scan previews are available.",
                true);
    }

    @Override
    public String status() {
        if (!detectedMod()) {
            return super.status();
        }
        return "Lens preview ready: " + providerDiagnostics().size() + " provider(s), "
                + scriptDefinitions().size() + " ScriptCore lens_scan definition(s).";
    }

    @Override
    public List<CreatorDefinitionSummary> listDefinitions() {
        if (!isAvailable()) {
            return List.of();
        }
        List<CreatorDefinitionSummary> summaries = new ArrayList<>();
        for (Object diagnostic : providerDiagnostics()) {
            Identifier id = AdapterReflection.value(diagnostic, "id", Identifier.class).orElse(null);
            if (id != null) {
                boolean enabled = AdapterReflection.value(diagnostic, "enabled", Boolean.class).orElse(false);
                summaries.add(new CreatorDefinitionSummary(id, "lens_provider", id.toString(), id().toString(),
                        id.getNamespace(), enabled ? "enabled" : "disabled"));
            }
        }
        for (Object definition : scriptDefinitions()) {
            Identifier id = AdapterReflection.value(definition, "id", Identifier.class).orElse(null);
            if (id != null) {
                summaries.add(new CreatorDefinitionSummary(id, "lens_scan",
                        AdapterReflection.optionalString(AdapterReflection.invoke(definition, "title")).orElse(id.toString()),
                        id().toString(), AdapterReflection.value(definition, "pack", String.class).orElse(id.getNamespace()),
                        "scriptcore"));
            }
        }
        return List.copyOf(summaries);
    }

    @Override
    public Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
        if (!isAvailable() || id == null) {
            return Optional.empty();
        }
        for (Object diagnostic : providerDiagnostics()) {
            Identifier providerId = AdapterReflection.value(diagnostic, "id", Identifier.class).orElse(null);
            if (id.equals(providerId)) {
                String category = String.valueOf(AdapterReflection.invoke(diagnostic, "category"));
                String providerClass = AdapterReflection.value(diagnostic, "providerClass", String.class).orElse("unknown");
                boolean enabled = AdapterReflection.value(diagnostic, "enabled", Boolean.class).orElse(false);
                JsonObject raw = new JsonObject();
                raw.addProperty("id", id.toString());
                raw.addProperty("type", "lens_provider");
                raw.addProperty("provider_class", providerClass);
                raw.addProperty("category", category);
                raw.addProperty("enabled", enabled);
                List<String> preview = List.of("Category: " + category, "Provider: " + providerClass,
                        "Enabled: " + enabled);
                return Optional.of(new CreatorDefinitionDetail(id, "lens_provider", id.toString(),
                        "Registered Lens provider diagnostic.", id().toString(), id.getNamespace(),
                        enabled ? "enabled" : "disabled", Optional.empty(), List.of(category), raw,
                        java.util.Map.of(), List.of(), preview, true));
            }
        }
        for (Object definition : scriptDefinitions()) {
            Identifier definitionId = AdapterReflection.value(definition, "id", Identifier.class).orElse(null);
            if (id.equals(definitionId)) {
                return Optional.of(scriptDetail(definition));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CreatorPreviewSummary> previewSummaries() {
        if (!isAvailable()) {
            return List.of();
        }
        return listDefinitions().stream()
                .limit(40)
                .map(summary -> new CreatorPreviewSummary(summary.id(), summary.type(), summary.title(),
                        id().toString(), "Lens", List.of("Status: " + summary.status(), "Pack: " + summary.pack()), true))
                .toList();
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        List<CreatorDiagnostic> base = new ArrayList<>(super.diagnostics());
        if (!isAvailable()) {
            return List.copyOf(base);
        }
        for (Object diagnostic : providerDiagnostics()) {
            boolean enabled = AdapterReflection.value(diagnostic, "enabled", Boolean.class).orElse(true);
            if (!enabled) {
                Identifier id = AdapterReflection.value(diagnostic, "id", Identifier.class).orElse(id());
                base.add(new CreatorDiagnostic(CreatorDiagnostic.Severity.INFO,
                        "creatorcore.lens.provider_disabled",
                        "Lens provider is registered but disabled: " + id,
                        displayName(), Optional.of(id), Optional.empty(), Optional.empty(),
                        Optional.of("Enable the Lens category in Lens config if this preview is expected."),
                        false, Optional.of(id())));
            }
        }
        return List.copyOf(base);
    }

    private CreatorDefinitionDetail scriptDetail(Object definition) {
        Identifier definitionId = AdapterReflection.value(definition, "id", Identifier.class)
                .orElse(Identifier.fromNamespaceAndPath("lens", "unknown_scan"));
        JsonObject raw = AdapterReflection.value(AdapterReflection.invoke(definition, "rawJson"), JsonObject.class)
                .map(JsonObject::deepCopy)
                .orElseGet(JsonObject::new);
        String title = AdapterReflection.optionalString(AdapterReflection.invoke(definition, "title")).orElse(definitionId.toString());
        String description = AdapterReflection.optionalString(AdapterReflection.invoke(definition, "description")).orElse("");
        List<String> preview = List.of("ScriptCore lens_scan", "Raw JSON fields: " + raw.entrySet().size());
        return new CreatorDefinitionDetail(definitionId, "lens_scan", title, description, id().toString(),
                AdapterReflection.value(definition, "pack", String.class).orElse(definitionId.getNamespace()),
                "scriptcore", Optional.empty(), List.of("lens_scan"), raw,
                AdapterReflection.stringMap(AdapterReflection.invoke(definition, "metadata")),
                List.of(), preview, true);
    }

    private static List<Object> providerDiagnostics() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(AdapterReflection.load(REGISTRY_CLASS), "diagnostics"));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Object> scriptDefinitions() {
        try {
            Object api = AdapterReflection.invoke(AdapterReflection.load(SCRIPT_API_CLASS), "get");
            Object registry = AdapterReflection.invoke(api, "registry");
            return AdapterReflection.iterable(AdapterReflection.invoke(registry, "getByType", "lens_scan"));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }
}

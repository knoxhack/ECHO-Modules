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

public final class HoloMapCreatorAdapter extends ModPresenceCreatorAdapter {
    private static final String SERVICE_CLASS = "com.knoxhack.echoholomap.map.HoloMapService";
    private static final String SCRIPT_API_CLASS = "com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi";

    public HoloMapCreatorAdapter() {
        super("holomap", "echoholomap", "ECHO: HoloMap", null,
                Set.of("definitions", "diagnostics", "preview"),
                "HoloMap not installed; marker drafts remain generic JSON templates.",
                "HoloMap detected; read-only layer, marker, route, and ScriptCore HoloMap previews are available.",
                true);
    }

    @Override
    public String status() {
        if (!detectedMod()) {
            return super.status();
        }
        return "HoloMap preview ready: " + layers().size() + " layer(s), " + markers().size()
                + " marker(s), " + routes().size() + " route(s).";
    }

    @Override
    public List<CreatorDefinitionSummary> listDefinitions() {
        if (!isAvailable()) {
            return List.of();
        }
        List<CreatorDefinitionSummary> summaries = new ArrayList<>();
        for (Object layer : layers()) {
            Identifier id = AdapterReflection.value(layer, "id", Identifier.class).orElse(null);
            if (id != null) {
                summaries.add(new CreatorDefinitionSummary(id, "holomap_layer",
                        AdapterReflection.value(layer, "title", String.class).orElse(id.toString()),
                        id().toString(), id.getNamespace(), "runtime"));
            }
        }
        for (Object marker : markers()) {
            Identifier id = AdapterReflection.value(marker, "id", Identifier.class).orElse(null);
            if (id != null) {
                summaries.add(new CreatorDefinitionSummary(id, "holomap_marker",
                        AdapterReflection.value(marker, "title", String.class).orElse(id.toString()),
                        id().toString(), id.getNamespace(), "runtime"));
            }
        }
        for (Object definition : scriptDefinitions()) {
            Identifier id = AdapterReflection.value(definition, "id", Identifier.class).orElse(null);
            if (id != null) {
                summaries.add(new CreatorDefinitionSummary(id,
                        AdapterReflection.value(definition, "type", String.class).orElse("holomap_marker"),
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
        for (Object layer : layers()) {
            Identifier layerId = AdapterReflection.value(layer, "id", Identifier.class).orElse(null);
            if (id.equals(layerId)) {
                return Optional.of(layerDetail(layer));
            }
        }
        for (Object marker : markers()) {
            Identifier markerId = AdapterReflection.value(marker, "id", Identifier.class).orElse(null);
            if (id.equals(markerId)) {
                return Optional.of(markerDetail(marker));
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
        List<CreatorPreviewSummary> previews = new ArrayList<>();
        listDefinitions().stream().limit(60).forEach(summary -> previews.add(new CreatorPreviewSummary(
                summary.id(), summary.type(), summary.title(), id().toString(), "HoloMap",
                List.of("Status: " + summary.status(), "Pack: " + summary.pack()), true)));
        return List.copyOf(previews);
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        List<CreatorDiagnostic> diagnostics = new ArrayList<>(super.diagnostics());
        if (!isAvailable()) {
            return List.copyOf(diagnostics);
        }
        for (Object diagnostic : providerDiagnostics()) {
            boolean healthy = AdapterReflection.value(diagnostic, "healthy", Boolean.class).orElse(true);
            if (!healthy) {
                Identifier providerId = AdapterReflection.value(diagnostic, "providerId", Identifier.class).orElse(id());
                diagnostics.add(new CreatorDiagnostic(CreatorDiagnostic.Severity.WARNING,
                        "creatorcore.holomap.provider_unhealthy",
                        AdapterReflection.value(diagnostic, "message", String.class)
                                .orElse("HoloMap provider is reporting unhealthy status: " + providerId),
                        displayName(), Optional.of(providerId), Optional.empty(), Optional.empty(),
                        Optional.of("Check HoloMap provider diagnostics before relying on previews."),
                        false, Optional.of(id())));
            }
        }
        return List.copyOf(diagnostics);
    }

    private CreatorDefinitionDetail layerDetail(Object layer) {
        Identifier layerId = AdapterReflection.value(layer, "id", Identifier.class)
                .orElse(Identifier.fromNamespaceAndPath("holomap", "unknown_layer"));
        String title = AdapterReflection.value(layer, "title", String.class).orElse(layerId.toString());
        JsonObject raw = new JsonObject();
        raw.addProperty("id", layerId.toString());
        raw.addProperty("type", "holomap_layer");
        raw.addProperty("title", title);
        raw.addProperty("sort_order", String.valueOf(AdapterReflection.invoke(layer, "sortOrder")));
        raw.addProperty("visible_by_default", String.valueOf(AdapterReflection.invoke(layer, "visibleByDefault")));
        return new CreatorDefinitionDetail(layerId, "holomap_layer", title, "Runtime HoloMap layer.",
                id().toString(), layerId.getNamespace(), "runtime", Optional.empty(), List.of("layer"), raw,
                java.util.Map.of(), List.of(), List.of("Layer sort: " + AdapterReflection.invoke(layer, "sortOrder"),
                        "Visible by default: " + AdapterReflection.invoke(layer, "visibleByDefault")), true);
    }

    private CreatorDefinitionDetail markerDetail(Object marker) {
        Identifier markerId = AdapterReflection.value(marker, "id", Identifier.class)
                .orElse(Identifier.fromNamespaceAndPath("holomap", "unknown_marker"));
        String title = AdapterReflection.value(marker, "title", String.class).orElse(markerId.toString());
        String summary = AdapterReflection.value(marker, "summary", String.class).orElse("");
        JsonObject raw = new JsonObject();
        raw.addProperty("id", markerId.toString());
        raw.addProperty("type", "holomap_marker");
        raw.addProperty("title", title);
        raw.addProperty("summary", summary);
        raw.addProperty("layer", String.valueOf(AdapterReflection.invoke(marker, "layerId")));
        raw.addProperty("state", String.valueOf(AdapterReflection.invoke(marker, "state")));
        raw.addProperty("kind", String.valueOf(AdapterReflection.invoke(marker, "kind")));
        raw.addProperty("x", String.valueOf(AdapterReflection.invoke(marker, "x")));
        raw.addProperty("y", String.valueOf(AdapterReflection.invoke(marker, "y")));
        raw.addProperty("z", String.valueOf(AdapterReflection.invoke(marker, "z")));
        return new CreatorDefinitionDetail(markerId, "holomap_marker", title, summary, id().toString(),
                markerId.getNamespace(), "runtime", Optional.empty(), List.of("marker"), raw,
                java.util.Map.of(), List.of(), List.of("Layer: " + AdapterReflection.invoke(marker, "layerId"),
                        "State: " + AdapterReflection.invoke(marker, "state"),
                        "Position: " + AdapterReflection.invoke(marker, "x") + ", "
                                + AdapterReflection.invoke(marker, "y") + ", " + AdapterReflection.invoke(marker, "z")), true);
    }

    private CreatorDefinitionDetail scriptDetail(Object definition) {
        Identifier definitionId = AdapterReflection.value(definition, "id", Identifier.class)
                .orElse(Identifier.fromNamespaceAndPath("holomap", "unknown_script_definition"));
        String type = AdapterReflection.value(definition, "type", String.class).orElse("holomap_marker");
        JsonObject raw = AdapterReflection.value(AdapterReflection.invoke(definition, "rawJson"), JsonObject.class)
                .map(JsonObject::deepCopy)
                .orElseGet(JsonObject::new);
        return new CreatorDefinitionDetail(definitionId, type,
                AdapterReflection.optionalString(AdapterReflection.invoke(definition, "title")).orElse(definitionId.toString()),
                AdapterReflection.optionalString(AdapterReflection.invoke(definition, "description")).orElse(""),
                id().toString(), AdapterReflection.value(definition, "pack", String.class).orElse(definitionId.getNamespace()),
                "scriptcore", Optional.empty(), List.of(type), raw,
                AdapterReflection.stringMap(AdapterReflection.invoke(definition, "metadata")), List.of(),
                List.of("ScriptCore HoloMap definition", "Raw JSON fields: " + raw.entrySet().size()), true);
    }

    private static Object service() {
        return AdapterReflection.staticField(SERVICE_CLASS, "INSTANCE");
    }

    private static List<Object> layers() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(service(), "richLayers", new Object[] {null}));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Object> markers() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(service(), "richMarkers", new Object[] {null}));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Object> routes() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(service(), "richRoutes", new Object[] {null}));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Object> providerDiagnostics() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(service(), "diagnostics", new Object[] {null}));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Object> scriptDefinitions() {
        try {
            Object api = AdapterReflection.invoke(AdapterReflection.load(SCRIPT_API_CLASS), "get");
            Object registry = AdapterReflection.invoke(api, "registry");
            List<Object> definitions = new ArrayList<>();
            definitions.addAll(AdapterReflection.iterable(AdapterReflection.invoke(registry, "getByType", "holomap_layer")));
            definitions.addAll(AdapterReflection.iterable(AdapterReflection.invoke(registry, "getByType", "holomap_marker")));
            return List.copyOf(definitions);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }
}

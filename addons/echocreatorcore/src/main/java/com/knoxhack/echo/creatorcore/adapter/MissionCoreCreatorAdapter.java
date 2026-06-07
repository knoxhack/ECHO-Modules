package com.knoxhack.echo.creatorcore.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class MissionCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    private static final String SERVICE_CLASS = "com.knoxhack.echomissioncore.service.MissionCoreService";

    public MissionCoreCreatorAdapter() {
        super("missioncore", "echomissioncore", "ECHO: MissionCore", null,
                Set.of("definitions", "diagnostics", "preview"),
                "MissionCore not installed; mission drafts remain generic JSON templates.",
                "MissionCore detected; read-only mission previews are wired through MissionCoreService.",
                true);
    }

    @Override
    public String status() {
        if (!detectedMod()) {
            return super.status();
        }
        return "MissionCore preview ready: " + listDefinitions().size() + " mission definition(s).";
    }

    @Override
    public List<CreatorDefinitionSummary> listDefinitions() {
        if (!isAvailable()) {
            return List.of();
        }
        List<CreatorDefinitionSummary> summaries = new ArrayList<>();
        for (Object mission : missions()) {
            Identifier id = AdapterReflection.value(mission, "id", Identifier.class).orElse(null);
            if (id == null) {
                continue;
            }
            String title = AdapterReflection.value(mission, "title", String.class).orElse(id.toString());
            summaries.add(new CreatorDefinitionSummary(id, "mission", title, id().toString(), id.getNamespace(), "missioncore"));
        }
        return List.copyOf(summaries);
    }

    @Override
    public Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
        if (!isAvailable() || id == null) {
            return Optional.empty();
        }
        for (Object mission : missions()) {
            Identifier missionId = AdapterReflection.value(mission, "id", Identifier.class).orElse(null);
            if (id.equals(missionId)) {
                return Optional.of(toDetail(mission));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CreatorPreviewSummary> previewSummaries() {
        if (!isAvailable()) {
            return List.of();
        }
        return missions().stream()
                .limit(40)
                .map(mission -> {
                    Identifier id = AdapterReflection.value(mission, "id", Identifier.class)
                            .orElse(Identifier.fromNamespaceAndPath("missioncore", "unknown"));
                    String title = AdapterReflection.value(mission, "title", String.class).orElse(id.toString());
                    return new CreatorPreviewSummary(id, "mission", title, id().toString(), "MissionCore",
                            previewLines(mission), true);
                })
                .toList();
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        List<CreatorDiagnostic> base = super.diagnostics();
        if (!isAvailable()) {
            return base;
        }
        List<CreatorDiagnostic> diagnostics = new ArrayList<>(base);
        try {
            for (Object warning : AdapterReflection.iterable(AdapterReflection.invoke(service(), "validateContent"))) {
                diagnostics.add(CreatorDiagnostic.warning("creatorcore.missioncore.validation",
                        String.valueOf(warning), displayName(), "Review MissionCore content registration."));
            }
        } catch (RuntimeException exception) {
            diagnostics.add(CreatorDiagnostic.warning("creatorcore.missioncore.validation_unavailable",
                    "MissionCore validation warnings could not be read: " + exception.getMessage(),
                    displayName(), "Check the MissionCore log."));
        }
        return List.copyOf(diagnostics);
    }

    private CreatorDefinitionDetail toDetail(Object mission) {
        Identifier missionId = AdapterReflection.value(mission, "id", Identifier.class)
                .orElse(Identifier.fromNamespaceAndPath("missioncore", "unknown"));
        String title = AdapterReflection.value(mission, "title", String.class).orElse(missionId.toString());
        String briefing = AdapterReflection.value(mission, "briefing", String.class).orElse("");
        String category = AdapterReflection.value(mission, "category", String.class).orElse("");
        String difficulty = AdapterReflection.value(mission, "difficulty", String.class).orElse("");
        Map<String, String> metadata = AdapterReflection.stringMap(AdapterReflection.invoke(mission, "metadata"));
        JsonObject raw = new JsonObject();
        raw.addProperty("id", missionId.toString());
        raw.addProperty("type", "mission");
        raw.addProperty("title", title);
        raw.addProperty("briefing", briefing);
        raw.addProperty("category", category);
        raw.addProperty("difficulty", difficulty);
        raw.addProperty("chapter", String.valueOf(AdapterReflection.invoke(mission, "chapterId")));
        raw.addProperty("phase", String.valueOf(AdapterReflection.invoke(mission, "phaseTitle")));
        raw.add("objectives", rows(AdapterReflection.invoke(mission, "objectives"), "label", "detail"));
        raw.add("rewards", rows(AdapterReflection.invoke(mission, "rewards"), "label", "detail"));
        return new CreatorDefinitionDetail(missionId, "mission", title, briefing, id().toString(),
                missionId.getNamespace(), "missioncore", Optional.empty(), List.of(category, difficulty).stream()
                        .filter(value -> value != null && !value.isBlank()).toList(),
                raw, metadata, List.of(), previewLines(mission), true);
    }

    private static JsonArray rows(Object values, String titleMethod, String detailMethod) {
        JsonArray array = new JsonArray();
        for (Object value : AdapterReflection.iterable(values)) {
            JsonObject object = new JsonObject();
            object.addProperty("id", String.valueOf(AdapterReflection.invoke(value, "id")));
            object.addProperty("title", AdapterReflection.value(value, titleMethod, String.class).orElse(""));
            object.addProperty("detail", AdapterReflection.value(value, detailMethod, String.class).orElse(""));
            array.add(object);
        }
        return array;
    }

    private static List<String> previewLines(Object mission) {
        List<String> lines = new ArrayList<>();
        lines.add("Chapter: " + String.valueOf(AdapterReflection.invoke(mission, "chapterId")));
        lines.add("Phase: " + AdapterReflection.value(mission, "phaseTitle", String.class).orElse("unknown"));
        lines.add("Kind: " + String.valueOf(AdapterReflection.invoke(mission, "kind")));
        lines.add("Objectives: " + AdapterReflection.iterable(AdapterReflection.invoke(mission, "objectives")).size());
        lines.add("Rewards: " + AdapterReflection.iterable(AdapterReflection.invoke(mission, "rewards")).size());
        return List.copyOf(lines);
    }

    private static List<Object> missions() {
        try {
            return AdapterReflection.iterable(AdapterReflection.invoke(service(), "missionDefinitions"));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Object service() {
        return AdapterReflection.staticField(SERVICE_CLASS, "INSTANCE");
    }
}

package com.knoxhack.echo.creatorcore.draft;

import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorExportResult;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorDraftService {
    private final CreatorDraftStore store = new CreatorDraftStore();
    private final Map<Identifier, CreatorDraft> memoryDrafts = new LinkedHashMap<>();

    public CreatorDraftService() {
        CreatorDraft example = CreatorDraftTemplateFactory.exampleMission();
        memoryDrafts.put(example.id(), example);
    }

    public CreatorDraftStore store() {
        return store;
    }

    public synchronized CreatorDraft createDraft(String type, Identifier id, String pack) {
        CreatorDraft draft = CreatorDraftTemplateFactory.create(type, id, pack, "command");
        memoryDrafts.put(id, draft);
        return draft;
    }

    public synchronized CreatorDraft createFromTemplate(String type, Identifier id, String pack) {
        return createDraft(type, id, pack);
    }

    public synchronized CreatorDraft createMissionStudioDraft(Identifier id, String pack, String createdBy) {
        CreatorDraft draft = CreatorDraftTemplateFactory.create("mission", id, pack, createdBy);
        memoryDrafts.put(id, draft);
        return draft;
    }

    public synchronized Optional<CreatorDraft> getDraft(Identifier id) {
        if (memoryDrafts.containsKey(id)) {
            return Optional.of(memoryDrafts.get(id));
        }
        return listDrafts().stream().filter(draft -> draft.id().equals(id)).findFirst();
    }

    public synchronized List<CreatorDraft> listDrafts() {
        Map<Identifier, CreatorDraft> drafts = new LinkedHashMap<>();
        if (CreatorCoreConfig.bool(CreatorCoreConfig.GENERATE_EXAMPLE_DRAFTS, true)) {
            drafts.putAll(memoryDrafts);
        }
        for (CreatorDraft draft : store.loadDrafts()) {
            drafts.put(draft.id(), draft);
        }
        return drafts.values().stream()
                .sorted(Comparator.comparing(CreatorDraft::type).thenComparing(draft -> draft.id().toString()))
                .limit(CreatorCoreConfig.integer(CreatorCoreConfig.MAX_DRAFTS, 500))
                .toList();
    }

    public synchronized List<CreatorDiagnostic> validateDraft(Identifier id) {
        Optional<CreatorDraft> draft = getDraft(id);
        if (draft.isEmpty()) {
            return List.of(CreatorDiagnostic.error("creatorcore.draft_missing",
                    "Draft not found: " + id, "Draft Service", "Create the draft or check the id."));
        }
        return validate(draft.get());
    }

    public synchronized Optional<CreatorDraft> addTemplateSection(Identifier id, String section) {
        Optional<CreatorDraft> draft = getDraft(id);
        if (draft.isEmpty()) {
            return Optional.empty();
        }
        CreatorDraft existing = draft.get();
        if (!CreatorDraftTemplateFactory.applyTemplateSection(existing.content(), existing.type(), section)) {
            return Optional.empty();
        }
        CreatorDraft updated = new CreatorDraft(existing.id(), existing.type(), existing.pack(), existing.title(),
                existing.content(), existing.sourceAdapter(), existing.createdAt(), java.time.Instant.now(),
                existing.createdBy(), existing.diagnostics(), existing.status());
        memoryDrafts.put(id, updated);
        return Optional.of(updated);
    }

    public synchronized List<CreatorDiagnostic> validate(CreatorDraft draft) {
        List<CreatorDiagnostic> diagnostics = new ArrayList<>();
        if (draft.id() == null) {
            diagnostics.add(CreatorDiagnostic.error("creatorcore.draft.id_missing",
                    "Draft id is missing.", "Draft Service", "Use a namespaced id such as example:repair_radio."));
        }
        if (draft.type().isBlank() || "unknown".equals(draft.type())) {
            diagnostics.add(CreatorDiagnostic.error("creatorcore.draft.type_missing",
                    "Draft type is missing.", "Draft Service", "Pick a type such as mission or tutorial_hint."));
        }
        if (draft.title().isBlank()) {
            diagnostics.add(CreatorDiagnostic.warning("creatorcore.draft.title_missing",
                    "Draft title is blank.", "Draft Service", "Add a title before exporting."));
        }
        if (draft.content().entrySet().isEmpty()) {
            diagnostics.add(CreatorDiagnostic.error("creatorcore.draft.empty",
                    "Draft JSON content is empty.", "Draft Service", "Create the draft from a template."));
        }
        return List.copyOf(diagnostics);
    }

    public synchronized Path saveDraft(Identifier id) throws IOException {
        CreatorDraft draft = getDraft(id).orElseThrow(() -> new IOException("Draft not found: " + id));
        return store.save(draft);
    }

    public synchronized boolean deleteDraft(Identifier id) throws IOException {
        CreatorDraft draft = getDraft(id).orElseThrow(() -> new IOException("Draft not found: " + id));
        memoryDrafts.remove(id);
        return store.delete(draft);
    }

    public synchronized Optional<CreatorDraft> importDraft(Path path) {
        Optional<CreatorDraft> draft = store.read(path);
        draft.ifPresent(value -> memoryDrafts.put(value.id(), value));
        return draft;
    }

    public CreatorExportResult exportDraft(Identifier id) {
        return CreatorExportResult.failed("Use CreatorExportService for permission-checked exports.", "");
    }
}

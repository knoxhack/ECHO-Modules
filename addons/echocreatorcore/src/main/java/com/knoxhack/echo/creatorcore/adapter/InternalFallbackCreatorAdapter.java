package com.knoxhack.echo.creatorcore.adapter;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorFormField;
import com.knoxhack.echo.creatorcore.api.CreatorFormFieldKind;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftTemplateFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class InternalFallbackCreatorAdapter implements CreatorAdapter {
    private static final Set<String> TEMPLATE_TYPES = Set.of(
            "mission", "archive_entry", "lens_scan", "holomap_marker",
            "weather_event", "faction", "world_state", "tutorial_hint");

    @Override
    public Identifier id() {
        return EchoCreatorCore.id("internal");
    }

    @Override
    public String displayName() {
        return "CreatorCore Internal";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String status() {
        return "CreatorCore dashboard, diagnostics, drafts, and roadmap panels are online.";
    }

    @Override
    public Set<String> capabilities() {
        return Set.of("definitions", "diagnostics", "drafts", "preview", "editor");
    }

    @Override
    public List<CreatorDefinitionSummary> listDefinitions() {
        return List.of(
                CreatorDefinitionSummary.of(EchoCreatorCore.id("dashboard"), "creator_panel", "Creator dashboard", "internal"),
                CreatorDefinitionSummary.of(EchoCreatorCore.id("validation_center"), "creator_panel", "Validation center", "internal"),
                CreatorDefinitionSummary.of(EchoCreatorCore.id("draft_templates"), "template_set", "Generic creator draft templates", "internal"),
                CreatorDefinitionSummary.of(EchoCreatorCore.id("roadmap"), "creator_panel", "CreatorCore roadmap", "internal"));
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        return List.of(CreatorDiagnostic.info("creatorcore.internal.ready",
                "CreatorCore internal fallback adapter is active.", "CreatorCore Internal"));
    }

    @Override
    public List<CreatorPreviewSummary> previewSummaries() {
        return List.of(new CreatorPreviewSummary(EchoCreatorCore.id("mission_studio"), "mission_studio",
                "Mission Studio draft form", id().toString(), "CreatorCore",
                List.of("Draft-only mission editing surface.", "Writes remain locked until allow_draft_writes=true."), true));
    }

    @Override
    public List<CreatorFormSchema> formSchemas() {
        return List.of(new CreatorFormSchema("mission", "Mission Studio Draft",
                "First-pass mission form fields for draft-only authoring.",
                List.of(
                        new CreatorFormField("pack", "Pack", CreatorFormFieldKind.TEXT, true, List.of(), "example", false),
                        new CreatorFormField("id", "Mission Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, List.of(), "example:repair_radio", false),
                        CreatorFormField.text("title", "Title", true, "Repair the Radio"),
                        CreatorFormField.textArea("briefing", "Briefing", false, "What the player sees before starting."),
                        CreatorFormField.text("chapter", "Chapter", true, "example:chapter_one"),
                        CreatorFormField.text("phase", "Phase", false, "Opening Signal"),
                        CreatorFormField.select("kind", "Kind", false, List.of("MAIN", "SIDE", "REPEATABLE")),
                        new CreatorFormField("prerequisites", "Prerequisites", CreatorFormFieldKind.LIST, false, List.of(), "example:first_signal", false),
                        new CreatorFormField("objectives", "Objectives", CreatorFormFieldKind.JSON, true, List.of(), "[{\"type\":\"collect_item\"}]", false),
                        new CreatorFormField("rewards", "Rewards", CreatorFormFieldKind.JSON, false, List.of(), "[{\"type\":\"grant_xp\"}]", false)),
                false));
    }

    @Override
    public boolean supportsDraftType(String type) {
        return type != null && TEMPLATE_TYPES.contains(type);
    }

    @Override
    public Optional<CreatorDraft> createDraft(String type, Identifier id) {
        if (!supportsDraftType(type) || id == null) {
            return Optional.empty();
        }
        return Optional.of(CreatorDraftTemplateFactory.create(type, id, "default", "internal"));
    }
}

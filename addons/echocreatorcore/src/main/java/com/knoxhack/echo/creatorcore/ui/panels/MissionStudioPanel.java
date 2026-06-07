package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorFormField;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class MissionStudioPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("mission_studio");
    }

    @Override
    public String title() {
        return "Mission Studio";
    }

    @Override
    public String summary() {
        return "Guided ScriptCore draft and template workflow.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("Mode: guided draft editing. Live ScriptCore/MissionCore definitions are read-only.");
        lines.add("Draft writes: " + (model.writeLocked() ? "LOCKED" : "ALLOWED"));
        lines.add("Exports: " + (model.exportsLocked() ? "LOCKED" : "ALLOWED"));
        lines.add("Create base mission draft: /echo creatorcore drafts create mission <pack> <id>");
        lines.add("Validate/export: /echo creatorcore drafts validate <id> | drafts export <id>");
        lines.add("Template sections: objectives, rewards, conditions, unlock_conditions, actions, on_start, on_complete");
        lines.add("Other ScriptCore draft types: archive_entry, lens_scan, holomap_layer, holomap_marker, weather_event, faction, world_state, tutorial_hint, dialogue, ending, recipe_unlock, loot_profile, generic");
        long draftErrors = model.drafts().stream()
                .flatMap(draft -> CreatorCoreApi.get().drafts().validateDraft(draft.id()).stream())
                .filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR)
                .count();
        lines.add("Draft diagnostics: " + draftErrors + " blocking error(s) across " + model.drafts().size() + " draft(s).");
        lines.add("");
        CreatorFormSchema schema = model.formSchemas().stream()
                .filter(candidate -> "mission".equals(candidate.type()))
                .findFirst()
                .orElse(null);
        if (schema == null) {
            lines.add("Mission form schema is not registered.");
            return lines;
        }
        lines.add(schema.title() + " | readOnly=" + schema.readOnly());
        if (!schema.description().isBlank()) {
            lines.add(schema.description());
        }
        lines.add("");
        lines.add("Fields:");
        for (CreatorFormField field : schema.fields()) {
            lines.add("  " + field.name() + " | " + field.kind()
                    + " | required=" + field.required()
                    + " | " + field.label());
            if (!field.options().isEmpty()) {
                lines.add("    options=" + String.join(", ", field.options()));
            }
            if (!field.placeholder().isBlank()) {
                lines.add("    placeholder=" + field.placeholder());
            }
        }
        return lines;
    }
}

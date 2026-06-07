package com.knoxhack.echo.creatorcore.definition;

import com.knoxhack.echo.creatorcore.adapter.CreatorAdapterRegistry;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorDefinitionService {
    private final CreatorAdapterRegistry adapters;

    public CreatorDefinitionService(CreatorAdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public List<CreatorDefinitionSummary> listDefinitions() {
        return adapters.listDefinitions();
    }

    public Optional<CreatorDefinitionDetail> detail(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        for (CreatorAdapter adapter : adapters.adapters()) {
            Optional<CreatorDefinitionDetail> detail = adapter.definitionDetail(id);
            if (detail.isPresent()) {
                return detail;
            }
        }
        return listDefinitions().stream()
                .filter(summary -> id.equals(summary.id()))
                .findFirst()
                .map(summary -> CreatorDefinitionDetail.fromSummary(summary, List.of("No detail provider is wired for this definition.")));
    }

    public List<CreatorDefinitionDetail> details(int limit) {
        int max = Math.max(1, limit);
        List<CreatorDefinitionDetail> details = new ArrayList<>();
        for (CreatorDefinitionSummary summary : listDefinitions()) {
            detail(summary.id()).ifPresent(details::add);
            if (details.size() >= max) {
                break;
            }
        }
        return List.copyOf(details);
    }

    public List<CreatorPreviewSummary> previewSummaries() {
        List<CreatorPreviewSummary> previews = new ArrayList<>();
        for (CreatorAdapter adapter : adapters.adapters()) {
            previews.addAll(adapter.previewSummaries());
        }
        return List.copyOf(previews);
    }

    public List<CreatorFormSchema> formSchemas() {
        List<CreatorFormSchema> schemas = new ArrayList<>();
        for (CreatorAdapter adapter : adapters.adapters()) {
            schemas.addAll(adapter.formSchemas());
        }
        return List.copyOf(schemas);
    }
}

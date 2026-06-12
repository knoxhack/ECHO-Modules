package com.knoxhack.echoopenlandsprotocol.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OpenlandsContractResource(
        String id,
        OpenlandsContractKind kind,
        String resourcePath,
        int minimumEntries,
        List<String> domains,
        List<String> requiredFields,
        String description
) {
    public OpenlandsContractResource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(description, "description");
        domains = List.copyOf(domains == null ? List.of() : domains);
        requiredFields = List.copyOf(requiredFields == null ? List.of() : requiredFields);
        if (!resourcePath.startsWith("data/echoopenlandsprotocol/openlands/")) {
            throw new IllegalArgumentException("Openlands contract resource must live under the Openlands data root: " + resourcePath);
        }
        if (minimumEntries < 0) {
            throw new IllegalArgumentException("minimumEntries must be >= 0");
        }
    }

    public String namespacedId() {
        return OpenlandsRuntimeContracts.MODULE_ID + ":" + id;
    }

    public Map<String, Object> asAdapterRecord() {
        return Map.of(
                "id", id,
                "namespacedId", namespacedId(),
                "kind", kind.name().toLowerCase(java.util.Locale.ROOT),
                "resourcePath", resourcePath,
                "minimumEntries", minimumEntries,
                "domains", domains,
                "requiredFields", requiredFields,
                "description", description
        );
    }
}

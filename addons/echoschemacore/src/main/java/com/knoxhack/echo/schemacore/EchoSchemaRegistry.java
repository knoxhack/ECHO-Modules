package com.knoxhack.echo.schemacore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EchoSchemaRegistry {
    private final ConcurrentMap<SchemaKey, EchoSchemaDescriptor> descriptorsByKey = new ConcurrentHashMap<>();

    public EchoSchemaValidationResult register(EchoSchemaDescriptor descriptor) {
        SchemaKey key = SchemaKey.from(descriptor);
        EchoSchemaDescriptor previous = descriptorsByKey.putIfAbsent(key, descriptor);
        if (previous != null && !previous.equals(descriptor)) {
            return EchoSchemaValidationResult.issue(new EchoSchemaIssue(
                    EchoSchemaIssueSeverity.ERROR,
                    "ECHO-SCHEMA-DUPLICATE",
                    "Duplicate schema descriptor",
                    "A schema descriptor with the same id, version, and document kind is already registered.",
                    descriptor.id(),
                    descriptor.version(),
                    descriptor.kind(),
                    "",
                    "Choose a new schema version or remove the duplicate provider."
            ));
        }
        return EchoSchemaValidationResult.valid(descriptor);
    }

    public List<EchoSchemaValidationResult> registerAll(EchoSchemaProvider provider) {
        return provider.schemaDescriptors().stream()
                .map(this::register)
                .toList();
    }

    public Optional<EchoSchemaDescriptor> findByIdAndVersion(EchoSchemaId id, EchoSchemaVersion version) {
        return descriptorsByKey.values().stream()
                .filter(descriptor -> descriptor.id().equals(id) && descriptor.version().equals(version))
                .sorted(descriptorComparator())
                .findFirst();
    }

    public List<EchoSchemaDescriptor> findById(EchoSchemaId id) {
        return descriptorsByKey.values().stream()
                .filter(descriptor -> descriptor.id().equals(id))
                .sorted(descriptorComparator())
                .toList();
    }

    public List<EchoSchemaDescriptor> findByKind(EchoSchemaDocumentKind kind) {
        return descriptorsByKey.values().stream()
                .filter(descriptor -> descriptor.kind() == kind)
                .sorted(descriptorComparator())
                .toList();
    }

    public List<EchoSchemaDescriptor> findByKindAndVersion(EchoSchemaDocumentKind kind, EchoSchemaVersion version) {
        return descriptorsByKey.values().stream()
                .filter(descriptor -> descriptor.kind() == kind && descriptor.version().equals(version))
                .sorted(descriptorComparator())
                .toList();
    }

    public Optional<EchoSchemaDescriptor> find(EchoSchemaId id, EchoSchemaDocumentKind kind, EchoSchemaVersion version) {
        return Optional.ofNullable(descriptorsByKey.get(new SchemaKey(id, version, kind)));
    }

    public List<EchoSchemaDescriptor> descriptors() {
        return descriptorsByKey.values().stream()
                .sorted(descriptorComparator())
                .toList();
    }

    public boolean contains(EchoSchemaId id, EchoSchemaDocumentKind kind, EchoSchemaVersion version) {
        return descriptorsByKey.containsKey(new SchemaKey(id, version, kind));
    }

    public void clear() {
        descriptorsByKey.clear();
    }

    private static Comparator<EchoSchemaDescriptor> descriptorComparator() {
        return Comparator.comparing((EchoSchemaDescriptor descriptor) -> descriptor.kind().serializedName())
                .thenComparing(descriptor -> descriptor.id().value())
                .thenComparing(EchoSchemaDescriptor::version);
    }

    private record SchemaKey(EchoSchemaId id, EchoSchemaVersion version, EchoSchemaDocumentKind kind) {
        static SchemaKey from(EchoSchemaDescriptor descriptor) {
            return new SchemaKey(descriptor.id(), descriptor.version(), descriptor.kind());
        }
    }
}

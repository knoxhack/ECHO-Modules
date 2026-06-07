package com.knoxhack.echotextureforge.api.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TextureSpecRegistry {
    private final Map<String, TextureSpec> specs = new LinkedHashMap<>();

    public void register(TextureSpec spec) {
        if (spec != null) {
            specs.put(spec.key(), spec);
        }
    }

    public void registerAll(Collection<TextureSpec> values) {
        if (values != null) {
            values.forEach(this::register);
        }
    }

    public Optional<TextureSpec> find(String namespace, String assetId, TextureKind kind) {
        return Optional.ofNullable(specs.get(TextureSpec.key(namespace, assetId, kind)));
    }

    public List<TextureSpec> all() {
        List<TextureSpec> list = new ArrayList<>(specs.values());
        list.sort(Comparator.comparing(TextureSpec::namespace)
                .thenComparing(spec -> spec.assetKind().id())
                .thenComparing(TextureSpec::assetId));
        return List.copyOf(list);
    }

    public List<TextureSpec> byNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return all();
        }
        return all().stream().filter(spec -> namespace.equals(spec.namespace())).toList();
    }

    public int size() {
        return specs.size();
    }
}

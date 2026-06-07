package com.knoxhack.echotextureforge.common.scan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ResourceScanResult {
    private final Path workspaceRoot;
    private final List<Path> scannedModuleRoots = new ArrayList<>();
    private final Map<String, NamespaceAssets> namespaces = new LinkedHashMap<>();

    public ResourceScanResult(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public void addScannedModuleRoot(Path moduleRoot) {
        if (moduleRoot != null && !scannedModuleRoots.contains(moduleRoot.normalize())) {
            scannedModuleRoots.add(moduleRoot.normalize());
        }
    }

    public List<Path> scannedModuleRoots() {
        return List.copyOf(scannedModuleRoots);
    }

    public NamespaceAssets namespace(String namespace) {
        return namespaces.computeIfAbsent(namespace, NamespaceAssets::new);
    }

    public Map<String, NamespaceAssets> namespaces() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(namespaces));
    }

    public Optional<NamespaceAssets> assets(String namespace) {
        return Optional.ofNullable(namespaces.get(namespace));
    }

    public boolean hasAsset(String namespace, String relativePath) {
        return assets(namespace).map(assets -> assets.has(relativePath)).orElse(false);
    }

    public Optional<Path> firstPath(String namespace, String relativePath) {
        return assets(namespace).flatMap(assets -> assets.firstPath(relativePath));
    }

    public int textureCount() {
        return namespaces.values().stream().mapToInt(assets -> assets.textureFiles().size()).sum();
    }

    public int itemModelCount() {
        return namespaces.values().stream().mapToInt(assets -> assets.itemModels().size()).sum();
    }

    public int blockModelCount() {
        return namespaces.values().stream().mapToInt(assets -> assets.blockModels().size()).sum();
    }

    public int blockstateCount() {
        return namespaces.values().stream().mapToInt(assets -> assets.blockstates().size()).sum();
    }

    public static final class NamespaceAssets {
        private final String namespace;
        private final Map<String, List<Path>> files = new LinkedHashMap<>();
        private final Set<String> textures = new LinkedHashSet<>();
        private final Set<String> itemTextures = new LinkedHashSet<>();
        private final Set<String> blockTextures = new LinkedHashSet<>();
        private final Set<String> entityTextures = new LinkedHashSet<>();
        private final Set<String> guiTextures = new LinkedHashSet<>();
        private final Set<String> itemModels = new LinkedHashSet<>();
        private final Set<String> blockModels = new LinkedHashSet<>();
        private final Set<String> blockstates = new LinkedHashSet<>();
        private final Set<String> langKeys = new LinkedHashSet<>();
        private final Set<String> specFiles = new LinkedHashSet<>();

        private NamespaceAssets(String namespace) {
            this.namespace = namespace;
        }

        public String namespace() {
            return namespace;
        }

        public void addFile(String relativePath, Path physicalPath) {
            String normalized = normalize(relativePath);
            files.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(physicalPath.normalize());
            if (normalized.startsWith("textures/") && normalized.endsWith(".png")) {
                textures.add(normalized);
                if (normalized.startsWith("textures/item/")) {
                    itemTextures.add(normalized);
                } else if (normalized.startsWith("textures/block/")) {
                    blockTextures.add(normalized);
                } else if (normalized.startsWith("textures/entity/")) {
                    entityTextures.add(normalized);
                } else if (normalized.startsWith("textures/gui/")) {
                    guiTextures.add(normalized);
                }
            } else if (normalized.startsWith("models/item/") && normalized.endsWith(".json")) {
                itemModels.add(normalized);
            } else if (normalized.startsWith("models/block/") && normalized.endsWith(".json")) {
                blockModels.add(normalized);
            } else if (normalized.startsWith("blockstates/") && normalized.endsWith(".json")) {
                blockstates.add(normalized);
            } else if (normalized.startsWith("textureforge/specs/") && normalized.endsWith(".json")) {
                specFiles.add(normalized);
            }
        }

        public void addLangKey(String key) {
            if (key != null && !key.isBlank()) {
                langKeys.add(key.strip());
            }
        }

        public boolean has(String relativePath) {
            return files.containsKey(normalize(relativePath));
        }

        public Optional<Path> firstPath(String relativePath) {
            List<Path> paths = files.get(normalize(relativePath));
            if (paths == null || paths.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(paths.getFirst());
        }

        public Map<String, List<Path>> files() {
            Map<String, List<Path>> copy = new LinkedHashMap<>();
            files.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            return copy;
        }

        public Map<String, List<Path>> duplicateFiles() {
            Map<String, List<Path>> duplicates = new LinkedHashMap<>();
            files.forEach((key, value) -> {
                if (value.size() > 1) {
                    duplicates.put(key, List.copyOf(value));
                }
            });
            return duplicates;
        }

        public Set<String> textureFiles() {
            return sorted(textures);
        }

        public Set<String> itemTextures() {
            return sorted(itemTextures);
        }

        public Set<String> blockTextures() {
            return sorted(blockTextures);
        }

        public Set<String> entityTextures() {
            return sorted(entityTextures);
        }

        public Set<String> guiTextures() {
            return sorted(guiTextures);
        }

        public Set<String> itemModels() {
            return sorted(itemModels);
        }

        public Set<String> blockModels() {
            return sorted(blockModels);
        }

        public Set<String> blockstates() {
            return sorted(blockstates);
        }

        public Set<String> langKeys() {
            return sorted(langKeys);
        }

        public Set<String> specFiles() {
            return sorted(specFiles);
        }

        private static String normalize(String path) {
            return path == null ? "" : path.replace('\\', '/');
        }

        private static Set<String> sorted(Set<String> input) {
            List<String> values = new ArrayList<>(input);
            values.sort(Comparator.naturalOrder());
            return new LinkedHashSet<>(values);
        }
    }
}

package com.knoxhack.echotextureforge.common.spec;

import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureType;
import com.knoxhack.echotextureforge.common.scan.ResourceScanResult;
import com.knoxhack.echotextureforge.common.style.TextureStyleFamilies;
import java.util.ArrayList;
import java.util.List;

public final class TextureSpecGenerator {
    private TextureSpecGenerator() {
    }

    public static List<TextureSpec> fromResources(ResourceScanResult resources, String namespaceFilter) {
        List<TextureSpec> specs = new ArrayList<>();
        resources.namespaces().forEach((namespace, assets) -> {
            if (namespaceFilter != null && !namespaceFilter.isBlank() && !namespaceFilter.equals(namespace)) {
                return;
            }
            assets.itemModels().forEach(model -> {
                String id = strip(model, "models/item/", ".json");
                if (assets.blockstates().contains("blockstates/" + id + ".json")) {
                    return;
                }
                specs.add(TextureSpec.builder(namespace, id, TextureKind.ITEM)
                        .styleFamily(TextureStyleFamilies.defaultForNamespace(namespace))
                        .sourceRegistryId("resource:" + namespace + ":" + model)
                        .build());
            });
            assets.blockstates().forEach(blockstate -> {
                String id = strip(blockstate, "blockstates/", ".json");
                boolean machine = machineLike(id) || assets.blockModels().stream().anyMatch(model ->
                        model.contains("/machine/") || model.contains("/machines/") || model.endsWith("/" + id + ".json"));
                specs.add(TextureSpec.builder(namespace, id, machine ? TextureKind.MACHINE : TextureKind.BLOCK)
                        .textureType(machine ? TextureType.MACHINE_FRONT_SIDE_TOP : TextureType.CUBE_ALL)
                        .styleFamily(TextureStyleFamilies.defaultForNamespace(namespace))
                        .transparencyRequired(false)
                        .machineFacesRequired(machine ? List.of("front", "side", "top") : List.of())
                        .sourceRegistryId("resource:" + namespace + ":" + blockstate)
                        .build());
            });
            assets.guiTextures().forEach(texture -> specs.add(TextureSpec.builder(namespace, strip(texture, "textures/gui/", ".png"), TextureKind.UI)
                    .textureType(TextureType.ICON)
                    .styleFamily(TextureStyleFamilies.defaultForNamespace(namespace))
                    .sourceRegistryId("resource:" + namespace + ":" + texture)
                    .build()));
            assets.entityTextures().forEach(texture -> specs.add(TextureSpec.builder(namespace, strip(texture, "textures/entity/", ".png"), TextureKind.ENTITY)
                    .textureType(TextureType.MOB_BASE)
                    .styleFamily(TextureStyleFamilies.defaultForNamespace(namespace))
                    .sourceRegistryId("resource:" + namespace + ":" + texture)
                    .build()));
        });
        return List.copyOf(specs);
    }

    private static String strip(String value, String prefix, String suffix) {
        String stripped = value;
        if (stripped.startsWith(prefix)) {
            stripped = stripped.substring(prefix.length());
        }
        if (stripped.endsWith(suffix)) {
            stripped = stripped.substring(0, stripped.length() - suffix.length());
        }
        return stripped;
    }

    private static boolean machineLike(String path) {
        String value = path == null ? "" : path.toLowerCase(java.util.Locale.ROOT);
        return value.contains("machine") || value.contains("generator") || value.contains("press")
                || value.contains("assembler") || value.contains("refinery") || value.contains("extractor")
                || value.contains("compressor") || value.contains("charger") || value.contains("terminal")
                || value.contains("station") || value.contains("nexus") || value.contains("relay")
                || value.contains("fabricator") || value.contains("reactor") || value.contains("controller");
    }
}

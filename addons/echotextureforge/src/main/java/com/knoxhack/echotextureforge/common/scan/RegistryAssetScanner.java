package com.knoxhack.echotextureforge.common.scan;

import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import com.knoxhack.echotextureforge.api.spec.TextureType;
import com.knoxhack.echotextureforge.common.style.TextureStyleFamilies;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class RegistryAssetScanner {
    private RegistryAssetScanner() {
    }

    public static RegistrySpecs scan(String namespaceFilter) {
        List<TextureSpec> specs = new ArrayList<>();
        int registeredItems = 0;
        int registeredBlocks = 0;

        for (var item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!include(id, namespaceFilter)) {
                continue;
            }
            if (item instanceof BlockItem) {
                continue;
            }
            registeredItems++;
            TextureStyleFamily style = TextureStyleFamilies.defaultForNamespace(id.getNamespace());
            TextureKind kind = isArmorLike(id.getPath()) ? TextureKind.ARMOR : TextureKind.ITEM;
            TextureType type = inferItemType(id.getPath());
            specs.add(TextureSpec.builder(id.getNamespace(), id.getPath(), kind)
                    .textureType(type)
                    .styleFamily(style)
                    .sourceRegistryId(id.toString())
                    .registryId(id.toString())
                    .sourceAddon(id.getNamespace())
                    .build());
        }

        for (var block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!include(id, namespaceFilter)) {
                continue;
            }
            registeredBlocks++;
            TextureStyleFamily style = TextureStyleFamilies.defaultForNamespace(id.getNamespace());
            boolean machine = machineLike(id.getPath());
            specs.add(TextureSpec.builder(id.getNamespace(), id.getPath(), machine ? TextureKind.MACHINE : TextureKind.BLOCK)
                    .textureType(machine ? TextureType.MACHINE_FRONT_SIDE_TOP : inferBlockType(id.getPath()))
                    .styleFamily(style)
                    .transparencyRequired(false)
                    .machineFacesRequired(machine ? List.of("front", "side", "top") : List.of())
                    .sourceRegistryId(id.toString())
                    .registryId(id.toString())
                    .sourceAddon(id.getNamespace())
                    .build());
        }

        specs.sort(Comparator.comparing(TextureSpec::namespace)
                .thenComparing(spec -> spec.assetKind().id())
                .thenComparing(TextureSpec::assetId));
        return new RegistrySpecs(List.copyOf(specs), registeredItems, registeredBlocks);
    }

    private static boolean include(Identifier id, String namespaceFilter) {
        if (id == null) {
            return false;
        }
        if (namespaceFilter != null && !namespaceFilter.isBlank() && !namespaceFilter.equals(id.getNamespace())) {
            return false;
        }
        return ResourceAssetScanner.includeNamespace(id.getNamespace());
    }

    private static TextureType inferItemType(String path) {
        if (path.contains("sword") || path.contains("blade") || path.contains("rifle") || path.contains("pistol")) {
            return TextureType.WEAPON;
        }
        if (path.contains("pickaxe") || path.contains("axe") || path.contains("shovel") || path.contains("wrench")
                || path.contains("cutter") || path.contains("tool")) {
            return TextureType.TOOL;
        }
        if (path.contains("chip") || path.contains("upgrade")) {
            return TextureType.UPGRADE_CHIP;
        }
        if (path.contains("battery") || path.contains("cell")) {
            return TextureType.BATTERY;
        }
        if (path.contains("blueprint") || path.contains("plan")) {
            return TextureType.BLUEPRINT;
        }
        if (path.contains("data") || path.contains("drive") || path.contains("wafer")) {
            return TextureType.DATA_DRIVE;
        }
        if (path.contains("relic") || path.contains("ancient") || path.contains("artifact")) {
            return TextureType.RELIC;
        }
        if (path.contains("module")) {
            return TextureType.MODULE;
        }
        if (path.contains("plate") || path.contains("ingot") || path.contains("dust") || path.contains("fiber")) {
            return TextureType.MATERIAL;
        }
        return TextureType.COMPONENT;
    }

    private static TextureType inferBlockType(String path) {
        if (path.contains("ore")) {
            return TextureType.ORE;
        }
        if (path.contains("glass") || path.contains("grate") || path.contains("window")) {
            return TextureType.TRANSPARENT_CUTOUT;
        }
        if (path.contains("column") || path.contains("pillar")) {
            return TextureType.CUBE_COLUMN;
        }
        if (path.contains("panel") || path.contains("plate")) {
            return TextureType.DECORATIVE_PANEL;
        }
        if (path.contains("ruin") || path.contains("damaged")) {
            return TextureType.RUINED_VARIANT;
        }
        if (path.contains("hazard") || path.contains("warning")) {
            return TextureType.HAZARD_VARIANT;
        }
        return TextureType.CUBE_ALL;
    }

    private static boolean machineLike(String path) {
        return path.contains("machine") || path.contains("generator") || path.contains("terminal")
                || path.contains("press") || path.contains("core") || path.contains("controller")
                || path.contains("reactor") || path.contains("fabricator") || path.contains("grinder")
                || path.contains("assembler") || path.contains("refinery") || path.contains("extractor")
                || path.contains("compressor") || path.contains("charger") || path.contains("station")
                || path.contains("nexus") || path.contains("relay");
    }

    private static boolean isArmorLike(String path) {
        return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings")
                || path.contains("boots") || path.contains("armor");
    }

    public record RegistrySpecs(List<TextureSpec> specs, int registeredItems, int registeredBlocks) {
    }
}

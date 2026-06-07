package com.knoxhack.echomultiblockcore.content;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class MultiblockContent {
    private static volatile Map<Identifier, MultiblockDefinition> definitions = Map.of();
    private static final Map<Identifier, Identifier> ALIASES = java.util.Map.of(
            EchoMultiblockCore.id("industrial_assembly_line_demo"), EchoMultiblockCore.id("industrial_assembly_line"));

    private MultiblockContent() {
    }

    public static void replaceDefinitions(Map<Identifier, MultiblockDefinition> loaded) {
        definitions = Map.copyOf(loaded == null ? Map.of() : loaded);
        EchoMultiblockCore.LOGGER.info("ECHO MultiblockCore loaded {} multiblock definition(s).", definitions.size());
    }

    public static List<MultiblockDefinition> definitions() {
        return definitions.values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .toList();
    }

    public static Optional<MultiblockDefinition> definition(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        MultiblockDefinition def = definitions.get(id);
        if (def == null) {
            Identifier alias = ALIASES.get(id);
            if (alias != null) {
                def = definitions.get(alias);
            }
        }
        return Optional.ofNullable(def);
    }

    public static Map<Identifier, MultiblockDefinition> snapshot() {
        return new LinkedHashMap<>(definitions);
    }
}

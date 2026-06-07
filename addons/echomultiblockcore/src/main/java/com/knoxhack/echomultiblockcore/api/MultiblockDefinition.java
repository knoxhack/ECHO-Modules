package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record MultiblockDefinition(
        Identifier id,
        String displayName,
        String translationKey,
        String category,
        MultiblockRole role,
        int width,
        int height,
        int depth,
        Identifier controllerBlockId,
        Map<Character, StructureBlockRequirement> palette,
        List<List<String>> layers,
        boolean allowedRotations,
        boolean mirrorable,
        boolean requiresFoundation,
        List<MultiblockCapability> capabilities,
        List<Identifier> optionalTags,
        List<String> upgradeRules,
        int previewColor,
        IntegrityRules integrityRules,
        List<WorkcellDefinition> workcells,
        List<RoboticsRequirement> roboticsRequirements,
        List<CapabilityRequirement> capabilityRequirements,
        List<UpgradeSlotRule> upgradeSlotRules,
        String facilityStage,
        String facilityRoute,
        String primaryUse,
        List<String> integrationHints) {
    public MultiblockDefinition(
            Identifier id,
            String displayName,
            String translationKey,
            String category,
            MultiblockRole role,
            int width,
            int height,
            int depth,
            Identifier controllerBlockId,
            Map<Character, StructureBlockRequirement> palette,
            List<List<String>> layers,
            boolean allowedRotations,
            boolean mirrorable,
            boolean requiresFoundation,
            List<MultiblockCapability> capabilities,
            List<Identifier> optionalTags,
            List<String> upgradeRules,
            int previewColor,
            IntegrityRules integrityRules,
            List<WorkcellDefinition> workcells,
            List<RoboticsRequirement> roboticsRequirements,
            List<CapabilityRequirement> capabilityRequirements,
            List<UpgradeSlotRule> upgradeSlotRules) {
        this(id, displayName, translationKey, category, role, width, height, depth, controllerBlockId,
                palette, layers, allowedRotations, mirrorable, requiresFoundation, capabilities, optionalTags,
                upgradeRules, previewColor, integrityRules, workcells, roboticsRequirements,
                capabilityRequirements, upgradeSlotRules, "", "", "", List.of());
    }

    public MultiblockDefinition(
            Identifier id,
            String displayName,
            String translationKey,
            String category,
            MultiblockRole role,
            int width,
            int height,
            int depth,
            Identifier controllerBlockId,
            Map<Character, StructureBlockRequirement> palette,
            List<List<String>> layers,
            boolean allowedRotations,
            boolean mirrorable,
            boolean requiresFoundation,
            List<MultiblockCapability> capabilities,
            List<Identifier> optionalTags,
            List<String> upgradeRules,
            int previewColor,
            IntegrityRules integrityRules,
            List<WorkcellDefinition> workcells,
            List<RoboticsRequirement> roboticsRequirements) {
        this(id, displayName, translationKey, category, role, width, height, depth, controllerBlockId,
                palette, layers, allowedRotations, mirrorable, requiresFoundation, capabilities, optionalTags,
                upgradeRules, previewColor, integrityRules, workcells, roboticsRequirements, List.of(), List.of(),
                "", "", "", List.of());
    }

    public MultiblockDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Multiblock definition id is required.");
        }
        displayName = displayName == null || displayName.isBlank() ? id.getPath() : displayName.strip();
        translationKey = translationKey == null || translationKey.isBlank()
                ? "multiblock." + id.getNamespace() + "." + id.getPath().replace('/', '.')
                : translationKey.strip();
        category = category == null || category.isBlank() ? "general" : category.strip();
        role = role == null ? MultiblockRole.INFRASTRUCTURE : role;
        width = Math.max(1, width);
        height = Math.max(1, height);
        depth = Math.max(1, depth);
        palette = Map.copyOf(palette == null ? Map.of() : palette);
        layers = copyLayers(layers);
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        optionalTags = List.copyOf(optionalTags == null ? List.of() : optionalTags);
        upgradeRules = List.copyOf(upgradeRules == null ? List.of() : upgradeRules);
        previewColor = previewColor == 0 ? 0xFF00D8FF : previewColor;
        integrityRules = integrityRules == null ? IntegrityRules.DEFAULT : integrityRules;
        workcells = List.copyOf(workcells == null ? List.of() : workcells);
        roboticsRequirements = List.copyOf(roboticsRequirements == null ? List.of() : roboticsRequirements);
        capabilityRequirements = List.copyOf(capabilityRequirements == null ? List.of() : capabilityRequirements);
        upgradeSlotRules = List.copyOf(upgradeSlotRules == null ? List.of() : upgradeSlotRules);
        facilityStage = facilityStage == null ? "" : facilityStage.strip();
        facilityRoute = facilityRoute == null ? "" : facilityRoute.strip();
        primaryUse = primaryUse == null ? "" : primaryUse.strip();
        integrationHints = List.copyOf(integrationHints == null ? List.of() : integrationHints);
    }

    public int volume() {
        return width * height * depth;
    }

    public Optional<BlockPos> controllerLocalPosition() {
        for (int y = 0; y < layers.size(); y++) {
            List<String> rows = layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    StructureBlockRequirement requirement = palette.get(row.charAt(x));
                    if (requirement != null && requirement.kind() == StructureBlockRequirement.SlotKind.CONTROLLER) {
                        return Optional.of(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public StructureBlockRequirement requirementAt(int x, int y, int z) {
        if (y < 0 || y >= layers.size()) {
            return StructureBlockRequirement.wildcard();
        }
        List<String> rows = layers.get(y);
        if (z < 0 || z >= rows.size()) {
            return StructureBlockRequirement.wildcard();
        }
        String row = rows.get(z);
        if (x < 0 || x >= row.length()) {
            return StructureBlockRequirement.wildcard();
        }
        return palette.getOrDefault(row.charAt(x), StructureBlockRequirement.wildcard());
    }

    private static List<List<String>> copyLayers(List<List<String>> layers) {
        if (layers == null || layers.isEmpty()) {
            return List.of(List.of("C"));
        }
        return layers.stream().map(layer -> List.copyOf(layer == null ? List.of() : layer)).toList();
    }

    public record IntegrityRules(int max, boolean damageable, int damagedThreshold, int offlineThreshold) {
        public static final IntegrityRules DEFAULT = new IntegrityRules(100, true, 70, 25);

        public IntegrityRules {
            max = Math.max(1, max);
            damagedThreshold = Math.max(0, Math.min(max, damagedThreshold <= 0 ? 70 : damagedThreshold));
            offlineThreshold = Math.max(0, Math.min(max, offlineThreshold <= 0 ? 25 : offlineThreshold));
        }
    }

    public record RoboticsRequirement(int minArms, List<RobotToolType> requiredTools) {
        public RoboticsRequirement {
            minArms = Math.max(0, minArms);
            requiredTools = List.copyOf(requiredTools == null ? List.of() : requiredTools);
        }
    }
}

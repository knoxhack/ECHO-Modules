package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OpenlandsBuilderUxRuntime {
    private static final Set<String> HAMMER_COMMANDS = Set.of("wooden_hammer", "shape_cycle", "rotate_block", "copy_variant");
    private static final Set<String> STORAGE_COMMANDS = Set.of("quick_stack", "quick_deposit", "craft_from_nearby_storage");
    private static final Set<String> METADATA_COMMANDS = Set.of("named_chests");
    private static final Set<String> LOCAL_COMMANDS = Set.of("sort_inventory");

    private OpenlandsBuilderUxRuntime() {
    }

    public static List<String> commandIds() {
        return List.of(
                "wooden_hammer",
                "quick_stack",
                "quick_deposit",
                "sort_inventory",
                "craft_from_nearby_storage",
                "named_chests"
        );
    }

    public static OpenlandsBuilderActionResult validateBuilderAction(OpenlandsBuilderActionSnapshot snapshot) {
        String commandId = snapshot.commandId();
        if (!commandIds().contains(commandId) && !HAMMER_COMMANDS.contains(commandId)) {
            return OpenlandsBuilderActionResult.rejected(commandId, "unknown_command", true);
        }
        if (HAMMER_COMMANDS.contains(commandId)) {
            return validateHammerAction(snapshot);
        }
        if (STORAGE_COMMANDS.contains(commandId)) {
            return validateStorageAction(snapshot);
        }
        if (METADATA_COMMANDS.contains(commandId)) {
            if (!snapshot.containerPermission()) {
                return OpenlandsBuilderActionResult.rejected(commandId, "container_permission_required", false);
            }
            if (!snapshot.serverAuthoritative()) {
                return OpenlandsBuilderActionResult.rejected(commandId, "server_authoritative_transfer_required", false);
            }
            return OpenlandsBuilderActionResult.accepted(commandId, false);
        }
        if (LOCAL_COMMANDS.contains(commandId)) {
            return OpenlandsBuilderActionResult.accepted(commandId, true);
        }
        return OpenlandsBuilderActionResult.rejected(commandId, "unsupported_command", true);
    }

    public static Map<String, Object> adapterRecord() {
        return Map.of(
                "commandIds", commandIds(),
                "hammerCommands", HAMMER_COMMANDS.stream().sorted().toList(),
                "storageCommands", STORAGE_COMMANDS.stream().sorted().toList(),
                "metadataCommands", METADATA_COMMANDS.stream().sorted().toList(),
                "serverAuthoritativeRequired", true,
                "craftFromStorageMustReserveBeforeConsume", true,
                "namedChestMaxCharacters", 32
        );
    }

    private static OpenlandsBuilderActionResult validateHammerAction(OpenlandsBuilderActionSnapshot snapshot) {
        if (!snapshot.playerCanEdit()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "player_cannot_edit_block", true);
        }
        if (!snapshot.targetSupported()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "target_block_not_supported", true);
        }
        if (!snapshot.variantExists()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "variant_missing_for_runtime", true);
        }
        if (!snapshot.hasRequiredItemOrCreative()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "required_item_or_creative_missing", true);
        }
        if (!snapshot.serverAuthoritative()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "server_validation_required", true);
        }
        return OpenlandsBuilderActionResult.accepted(snapshot.commandId(), true);
    }

    private static OpenlandsBuilderActionResult validateStorageAction(OpenlandsBuilderActionSnapshot snapshot) {
        if (!snapshot.containerPermission()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "container_permission_required", true);
        }
        if (!snapshot.chunkLoaded()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "chunk_loaded_required", true);
        }
        if (!snapshot.serverAuthoritative()) {
            return OpenlandsBuilderActionResult.rejected(snapshot.commandId(), "server_authoritative_transfer_required", true);
        }
        return OpenlandsBuilderActionResult.accepted(snapshot.commandId(), true);
    }
}

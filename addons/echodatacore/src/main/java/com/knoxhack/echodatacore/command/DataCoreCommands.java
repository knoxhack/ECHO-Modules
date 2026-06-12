package com.knoxhack.echodatacore.command;

import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.DataKeyMetadata;
import com.echoplatform.echocore.api.DataServiceDiagnostics;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import com.echoplatform.echocore.api.IDataService;
import com.knoxhack.echodatacore.Config;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.legacy.DataCoreLegacyAdapters;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class DataCoreCommands {
    private static final int MAX_LINES = 24;

    private DataCoreCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("echodata")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
                        .then(Commands.literal("key")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .executes(context -> keyDetails(context.getSource(),
                                                StringArgumentType.getString(context, "key")))))
                        .then(Commands.literal("keys")
                                .executes(context -> dumpKeys(context.getSource(), "", 1))
                                .then(Commands.literal("page")
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> dumpKeys(context.getSource(), "",
                                                        IntegerArgumentType.getInteger(context, "page")))))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(context -> dumpKeys(context.getSource(),
                                                StringArgumentType.getString(context, "namespace"), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> dumpKeys(context.getSource(),
                                                        StringArgumentType.getString(context, "namespace"),
                                                        IntegerArgumentType.getInteger(context, "page"))))))
                        .then(Commands.literal("metadata")
                                .executes(context -> dumpMetadata(context.getSource(), "", 1))
                                .then(Commands.literal("sync")
                                        .executes(context -> metadataSync(context.getSource(),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> metadataSync(context.getSource(),
                                                        EntityArgument.getPlayer(context, "target")))))
                                .then(Commands.literal("page")
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> dumpMetadata(context.getSource(), "",
                                                        IntegerArgumentType.getInteger(context, "page")))))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(context -> dumpMetadata(context.getSource(),
                                                StringArgumentType.getString(context, "namespace"), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> dumpMetadata(context.getSource(),
                                                        StringArgumentType.getString(context, "namespace"),
                                                        IntegerArgumentType.getInteger(context, "page"))))))
                        .then(Commands.literal("dirty")
                                .executes(context -> dumpDirty(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.literal("player")
                                        .executes(context -> inspectPlayer(context.getSource(),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> inspectPlayer(context.getSource(),
                                                        EntityArgument.getPlayer(context, "target")))))
                                .then(Commands.literal("world")
                                        .executes(context -> inspectWorld(context.getSource())))
                                .then(Commands.literal("team")
                                        .then(Commands.argument("teamId", StringArgumentType.string())
                                                .executes(context -> inspectTeam(context.getSource(),
                                                        parseKey(StringArgumentType.getString(context, "teamId")))))))
                        .then(Commands.literal("legacy")
                                .then(Commands.literal("player")
                                        .executes(context -> legacyPlayer(context.getSource(),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> legacyPlayer(context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"))))))
                        .then(Commands.literal("migrate")
                                .then(Commands.literal("preview")
                                        .then(Commands.literal("world")
                                                .executes(context -> migrateScopedPreview(context.getSource(),
                                                        DataScope.WORLD, "")))
                                        .then(Commands.literal("team")
                                                .then(Commands.argument("teamId", StringArgumentType.string())
                                                        .executes(context -> migrateScopedPreview(context.getSource(),
                                                                DataScope.TEAM,
                                                                StringArgumentType.getString(context, "teamId")))))
                                        .then(Commands.literal("player")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(context -> migratePlayer(context.getSource(),
                                                                EntityArgument.getPlayer(context, "target"), "", false))
                                                        .then(Commands.argument("namespace", StringArgumentType.word())
                                                                .executes(context -> migratePlayer(context.getSource(),
                                                                        EntityArgument.getPlayer(context, "target"),
                                                                        StringArgumentType.getString(context, "namespace"), false))))))
                                .then(Commands.literal("apply")
                                        .then(Commands.literal("player")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(context -> migratePlayer(context.getSource(),
                                                                EntityArgument.getPlayer(context, "target"), "", true))
                                                        .then(Commands.argument("namespace", StringArgumentType.word())
                                                                .executes(context -> migratePlayer(context.getSource(),
                                                                        EntityArgument.getPlayer(context, "target"),
                                                                        StringArgumentType.getString(context, "namespace"), true)))))))
                        .then(Commands.literal("sync")
                                .then(Commands.literal("full")
                                        .executes(context -> fullSync(context.getSource(),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> fullSync(context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"))))))
                        .then(Commands.literal("flag")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("key", StringArgumentType.string())
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setFlag(context.getSource(),
                                                                StringArgumentType.getString(context, "key"),
                                                                BoolArgumentType.getBool(context, "value"))))))
                                .then(Commands.literal("unset")
                                        .then(Commands.argument("key", StringArgumentType.string())
                                                .executes(context -> unsetFlag(context.getSource(),
                                                        StringArgumentType.getString(context, "key")))))));
    }

    private static int status(CommandSourceStack source) {
        IDataService service = EchoCoreServices.dataService();
        DataServiceDiagnostics diagnostics = service.diagnostics();
        tell(source, "Data service: " + service.getClass().getName()
                + (diagnostics.available() ? " ONLINE" : " NO-OP"), ChatFormatting.AQUA);
        tell(source, "Revision=" + diagnostics.revision()
                + ", keys=" + diagnostics.registeredKeyCount()
                + ", synced=" + diagnostics.syncedKeyCount()
                + ", metadata=" + diagnostics.metadataKeyCount()
                + ", dirtyOwners=" + diagnostics.dirtyOwnerCount(), ChatFormatting.GRAY);
        if (service instanceof DataCoreDataService dataCore) {
            tell(source, "Datapack keys=" + dataCore.datapackRegisteredKeyCount()
                    + ", clientMetadata=" + dataCore.clientMetadataCount()
                    + ", duplicateConflicts=" + dataCore.duplicateKeyConflictCount()
                    + ", metadataConflicts=" + dataCore.metadataConflictCount()
                    + ", dirtyKeys=" + dataCore.dirtyKeyCount(), ChatFormatting.GRAY);
        }
        diagnostics.recentChanges().stream().limit(8)
                .forEach(change -> tell(source, change, ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int keyDetails(CommandSourceStack source, String rawKey) {
        Identifier keyId = parseKey(rawKey);
        IDataService service = EchoCoreServices.dataService();
        tell(source, "Key " + keyId, ChatFormatting.AQUA);
        service.key(keyId).ifPresentOrElse(key -> tell(source,
                        "Contract: " + key.scope() + " " + key.kind()
                                + (key.synced() ? " synced" : " server-only")
                                + " default=" + key.defaultValue(),
                        ChatFormatting.GRAY),
                () -> tell(source, "No Java/simple key contract is registered.", ChatFormatting.DARK_GRAY));
        service.keyMetadata(keyId).ifPresentOrElse(meta -> {
            tell(source, "Metadata: owner=" + meta.owner()
                    + ", title=\"" + meta.title() + "\""
                    + ", source=" + meta.source(), ChatFormatting.GRAY);
            if (!meta.description().isBlank()) {
                tell(source, "Description: " + meta.description(), ChatFormatting.DARK_GRAY);
            }
            if (!meta.legacyRoot().isBlank()) {
                tell(source, "Legacy: root=" + meta.legacyRoot()
                        + (meta.legacyField().isBlank() ? "" : ", field=" + meta.legacyField()),
                        ChatFormatting.GRAY);
            }
        }, () -> tell(source, "No metadata is registered.", ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int dumpKeys(CommandSourceStack source, String namespace, int page) {
        IDataService service = EchoCoreServices.dataService();
        DataServiceDiagnostics diagnostics = service.diagnostics();
        List<IDataKey<?>> keys = service.registeredKeys().stream()
                .filter(key -> namespace == null || namespace.isBlank() || namespace.equals(key.id().getNamespace()))
                .sorted(Comparator.comparing(key -> key.id().toString()))
                .toList();
        int safePage = Math.max(1, page);
        int from = Math.min(keys.size(), (safePage - 1) * MAX_LINES);
        int totalPages = Math.max(1, (int) Math.ceil(keys.size() / (double) MAX_LINES));
        tell(source, "Registered keys: " + keys.size()
                + ", synced=" + diagnostics.syncedKeyCount()
                + ", metadata=" + diagnostics.metadataKeyCount()
                + ", revision=" + diagnostics.revision()
                + ", page=" + safePage + "/" + totalPages, ChatFormatting.AQUA);
        keys.stream()
                .skip(from)
                .limit(MAX_LINES)
                .forEach(key -> tell(source, key.scope() + " " + key.kind() + " " + key.id()
                        + (key.synced() ? " synced" : " server"), ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int dumpMetadata(CommandSourceStack source, String namespace, int page) {
        IDataService service = EchoCoreServices.dataService();
        List<DataKeyMetadata> metadata = service.allKeyMetadata().values().stream()
                .filter(meta -> namespace == null || namespace.isBlank() || namespace.equals(meta.id().getNamespace()))
                .sorted(Comparator.comparing(meta -> meta.id().toString()))
                .toList();
        int safePage = Math.max(1, page);
        int from = Math.min(metadata.size(), (safePage - 1) * MAX_LINES);
        int totalPages = Math.max(1, (int) Math.ceil(metadata.size() / (double) MAX_LINES));
        tell(source, "Metadata entries: " + metadata.size()
                + ", page=" + safePage + "/" + totalPages, ChatFormatting.AQUA);
        metadata.stream()
                .skip(from)
                .limit(MAX_LINES)
                .forEach(meta -> tell(source, meta.scope() + " " + meta.kind() + " " + meta.id()
                        + " owner=" + meta.owner()
                        + " source=" + meta.source()
                        + (meta.title().isBlank() ? "" : " title=\"" + meta.title() + "\""),
                        ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int metadataSync(CommandSourceStack source, ServerPlayer player) {
        DataCoreDataService.INSTANCE.sendMetadataSync(player);
        tell(source, "Requested DataCore metadata sync for " + player.getScoreboardName(),
                ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int dumpDirty(CommandSourceStack source) {
        IDataService service = EchoCoreServices.dataService();
        DataServiceDiagnostics diagnostics = service.diagnostics();
        tell(source, "Dirty owners: " + diagnostics.dirtyOwnerCount()
                + ", revision=" + diagnostics.revision(), ChatFormatting.AQUA);
        if (service instanceof DataCoreDataService dataCore) {
            dataCore.dirtyOwnerCounts().forEach((name, count) ->
                    tell(source, name + "=" + count, ChatFormatting.GRAY));
        }
        diagnostics.recentChanges().stream().limit(MAX_LINES)
                .forEach(change -> tell(source, change, ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int inspectPlayer(CommandSourceStack source, ServerPlayer player) {
        Map<Identifier, String> values = EchoCoreServices.playerData(player).debugSnapshot();
        tell(source, "Player data for " + player.getScoreboardName() + ": " + values.size() + " value(s)",
                ChatFormatting.AQUA);
        dumpValues(source, values);
        return Command.SINGLE_SUCCESS;
    }

    private static int inspectWorld(CommandSourceStack source) {
        Map<Identifier, String> values = EchoCoreServices.worldData(source.getLevel()).debugSnapshot();
        tell(source, "World data for " + source.getLevel().dimension().identifier() + ": "
                + values.size() + " value(s)", ChatFormatting.AQUA);
        dumpValues(source, values);
        return Command.SINGLE_SUCCESS;
    }

    private static int inspectTeam(CommandSourceStack source, Identifier teamId) {
        Map<Identifier, String> values = EchoCoreServices.teamData(source.getLevel(), teamId).debugSnapshot();
        tell(source, "Team data for " + teamId + ": " + values.size() + " value(s)", ChatFormatting.AQUA);
        dumpValues(source, values);
        return Command.SINGLE_SUCCESS;
    }

    private static int legacyPlayer(CommandSourceStack source, ServerPlayer player) {
        Map<Identifier, String> values = DataCoreLegacyAdapters.snapshot(player);
        tell(source, "Legacy data for " + player.getScoreboardName() + ": " + values.size() + " value(s)",
                ChatFormatting.AQUA);
        dumpValues(source, values);
        return Command.SINGLE_SUCCESS;
    }

    private static int migratePlayer(CommandSourceStack source, ServerPlayer player, String namespace, boolean apply) {
        if (apply && !debugMutationsAllowed(source)) {
            tell(source, "Migration apply is disabled. Enable echodatacore debug commands for this world.",
                    ChatFormatting.RED);
            return 0;
        }
        DataCoreLegacyAdapters.MigrationReport report = apply
                ? DataCoreLegacyAdapters.apply(player, namespace)
                : DataCoreLegacyAdapters.preview(player, namespace);
        tell(source, (apply ? "Applied" : "Previewed") + " legacy migration for "
                + player.getScoreboardName()
                + (namespace == null || namespace.isBlank() ? "" : " namespace=" + namespace)
                + ": candidates=" + report.candidates()
                + ", already_mirrored=" + report.alreadyMirrored()
                + ", applied=" + report.applied()
                + ", failed_decode=" + report.failedDecode(), apply ? ChatFormatting.YELLOW : ChatFormatting.AQUA);
        report.values().entrySet().stream().limit(MAX_LINES)
                .forEach(entry -> {
                    DataCoreLegacyAdapters.MigrationCandidate detail = report.details().get(entry.getKey());
                    String legacy = detail == null ? "" : " [" + detail.scope() + " "
                            + detail.legacyRoot()
                            + (detail.legacyField().isBlank() ? "" : "." + detail.legacyField())
                            + (detail.source().isBlank() ? "" : " source=" + detail.source()) + "]";
                    tell(source, entry.getKey() + " <= " + entry.getValue() + legacy, ChatFormatting.GRAY);
                });
        return Command.SINGLE_SUCCESS;
    }

    private static int migrateScopedPreview(CommandSourceStack source, DataScope scope, String ownerId) {
        IDataService service = EchoCoreServices.dataService();
        List<IDataKey<?>> keys = service.registeredKeys().stream()
                .filter(key -> key.scope() == scope)
                .filter(key -> service.keyMetadata(key.id())
                        .map(meta -> !meta.legacyRoot().isBlank())
                        .orElse(false))
                .sorted(Comparator.comparing(key -> key.id().toString()))
                .toList();
        tell(source, "Previewed " + scope + " legacy metadata"
                + (ownerId == null || ownerId.isBlank() ? "" : " owner=" + ownerId)
                + ": candidates=" + keys.size()
                + ". World/team legacy reads are metadata-only in 1.3.0 and never mutate old roots.",
                ChatFormatting.AQUA);
        keys.stream().limit(MAX_LINES).forEach(key -> {
            DataCoreLegacyAdapters.MigrationCandidate detail = DataCoreLegacyAdapters.candidate(key);
            tell(source, key.id() + " <= " + detail.legacyRoot()
                    + (detail.legacyField().isBlank() ? "" : "." + detail.legacyField())
                    + (detail.source().isBlank() ? "" : " source=" + detail.source()),
                    ChatFormatting.GRAY);
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int fullSync(CommandSourceStack source, ServerPlayer player) {
        EchoCoreServices.dataSyncBridge().requestFullSync(player);
        tell(source, "Requested full DataCore sync for " + player.getScoreboardName(), ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int setFlag(CommandSourceStack source, String rawKey, boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!debugMutationsAllowed(source)) {
            tell(source, "Flag mutation is disabled. Enable echodatacore debug commands for this world.",
                    ChatFormatting.RED);
            return 0;
        }
        Identifier key = parseKey(rawKey);
        ServerPlayer player = source.getPlayerOrException();
        boolean changed = EchoCoreServices.playerData(player).set(IDataKey.flag(key, DataScope.PLAYER, false, true), value);
        tell(source, "Set " + key + "=" + value + (changed ? "" : " (unchanged)"), ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int unsetFlag(CommandSourceStack source, String rawKey) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!debugMutationsAllowed(source)) {
            tell(source, "Flag mutation is disabled. Enable echodatacore debug commands for this world.",
                    ChatFormatting.RED);
            return 0;
        }
        Identifier key = parseKey(rawKey);
        ServerPlayer player = source.getPlayerOrException();
        boolean changed = EchoCoreServices.playerData(player).clear(IDataKey.flag(key, DataScope.PLAYER, false, true));
        tell(source, "Unset " + key + (changed ? "" : " (not present)"), ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static void dumpValues(CommandSourceStack source, Map<Identifier, String> values) {
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .limit(MAX_LINES)
                .forEach(entry -> tell(source, entry.getKey() + " = " + entry.getValue(), ChatFormatting.GRAY));
        if (values.size() > MAX_LINES) {
            tell(source, "... " + (values.size() - MAX_LINES) + " more value(s)", ChatFormatting.DARK_GRAY);
        }
    }

    private static Identifier parseKey(String rawKey) {
        String raw = rawKey == null ? "" : rawKey.strip();
        Identifier key = Identifier.tryParse(raw);
        if (key == null) {
            String path = raw.isBlank() ? "debug/flag" : raw
                    .toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9_./-]", "_");
            key = Identifier.fromNamespaceAndPath("echodatacore", path);
        }
        return key;
    }

    private static boolean debugMutationsAllowed(CommandSourceStack source) {
        return Config.DEBUG_COMMANDS.get() || !source.getServer().isDedicatedServer();
    }

    private static void tell(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal("[ECHO DATA] " + message).withStyle(color), false);
    }
}

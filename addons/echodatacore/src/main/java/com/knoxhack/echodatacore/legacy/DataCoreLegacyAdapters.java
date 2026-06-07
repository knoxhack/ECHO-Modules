package com.knoxhack.echodatacore.legacy;

import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.IDataKey;
import com.knoxhack.echocore.api.IDataService;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.EchoDataCore;
import com.mojang.serialization.Codec;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class DataCoreLegacyAdapters {
    private static final Map<String, String> ROOTS_BY_NAMESPACE = Map.ofEntries(
            Map.entry("echocore", "echocore_progress_ledger"),
            Map.entry("echocore_profile", "echocore_profile"),
            Map.entry("echocore_factions", "echocore_factions"),
            Map.entry("echoorbitalremnants", "echoorbitalremnants_progress"),
            Map.entry("echoconvoyprotocol", "echoconvoyprotocol"),
            Map.entry("echoagriculturereclamation", "echoagriculturereclamation_progress"),
            Map.entry("echoindustrialnexus", "echoindustrialnexus_progress"),
            Map.entry("echostationfall", "echostationfall_progress"),
            Map.entry("echoblackboxprotocol", "echoblackboxprotocol_progress"),
            Map.entry("echonexusprotocol", "echonexusprotocol_progress"),
            Map.entry("echologisticsnetwork", "echologisticsnetwork_progress"),
            Map.entry("echoarmory", "echoarmory_progress"),
            Map.entry("signalos", "signalos")
    );

    private DataCoreLegacyAdapters() {
    }

    public static Optional<CompoundTag> read(Player player, IDataKey<?> key) {
        if (player == null || key == null) {
            return Optional.empty();
        }
        DataKeyMetadata metadata = EchoCoreServices.dataService().keyMetadata(key.id()).orElse(null);
        String rootName = rootName(key, metadata);
        if (rootName.isBlank()) {
            return Optional.empty();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(rootName);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        String field = fieldName(key, metadata);
        if (field.isBlank() || !root.contains(field)) {
            return Optional.empty();
        }
        return legacyEntry(key, root, field);
    }

    public static Map<Identifier, String> snapshot(Player player) {
        Map<Identifier, String> snapshot = new LinkedHashMap<>();
        if (player == null) {
            return snapshot;
        }
        for (Map.Entry<String, String> rootEntry : ROOTS_BY_NAMESPACE.entrySet()) {
            CompoundTag root = player.getPersistentData().getCompoundOrEmpty(rootEntry.getValue());
            if (root.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Tag> value : root.entrySet()) {
                Identifier id = Identifier.tryParse(rootEntry.getKey() + ":legacy/" + rootEntry.getValue() + "/" + cleanPath(value.getKey()));
                if (id != null) {
                    snapshot.put(id, value.getValue().toString());
                }
            }
        }
        addAttachmentSnapshot(snapshot, player, "echoashfallprotocol", "quest_data",
                "com.knoxhack.echoashfallprotocol.echo.QuestData",
                Map.of(
                        "completed_missions", "getCompletedMissionIds",
                        "unlocked_missions", "getUnlockedMissionIds",
                        "discovered_pois", "getDiscoveredPOIs",
                        "collected_power_nodes", "getCollectedPowerNodes"));
        addAttachmentSnapshot(snapshot, player, "echoterminal", "terminal_player_data",
                "com.knoxhack.echoterminal.player.TerminalPlayerData",
                Map.of(
                        "read_archives", "readArchiveIds",
                        "tracked_mission", "trackedMission"));
        addAttachmentSnapshot(snapshot, player, "echonexusprotocol", "nexus_player_data",
                "com.knoxhack.echonexusprotocol.data.NexusPlayerData",
                Map.of(
                        "research_unlocks", "researchUnlocks",
                        "scanned_ids", "scannedIds",
                        "blackbox_fragments", "blackboxFragments",
                        "ending_path", "endingPath",
                        "final_choice_state", "finalChoiceState"));
        return snapshot;
    }

    public static MigrationReport preview(Player player, String namespace) {
        return migrate(player, namespace, false, EchoCoreServices.dataService());
    }

    public static MigrationReport apply(Player player, String namespace) {
        return migrate(player, namespace, true, EchoCoreServices.dataService());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MigrationReport migrate(Player player, String namespace, boolean apply, IDataService service) {
        if (player == null || service == null) {
            return new MigrationReport(0, 0, 0, 0, Map.of(), Map.of());
        }
        Map<Identifier, String> candidates = new LinkedHashMap<>();
        Map<Identifier, MigrationCandidate> details = new LinkedHashMap<>();
        int alreadyMirrored = 0;
        int applied = 0;
        int failedDecode = 0;
        for (IDataKey key : service.registeredKeys()) {
            if (namespace != null && !namespace.isBlank() && !namespace.equals(key.id().getNamespace())) {
                continue;
            }
            Optional<CompoundTag> legacy = read(player, key);
            if (legacy.isEmpty()) {
                continue;
            }
            candidates.put(key.id(), legacy.get().toString());
            details.put(key.id(), candidate(key));
            if (service instanceof DataCoreDataService dataCore && dataCore.hasStoredPlayerValue(player, key.id())) {
                alreadyMirrored++;
                continue;
            }
            Optional<Object> decoded = decodeLegacy(key, legacy.get());
            if (decoded.isEmpty()) {
                failedDecode++;
                continue;
            }
            if (apply && service.player(player).set(key, decoded.get())) {
                applied++;
                if (service instanceof DataCoreDataService dataCore) {
                    dataCore.recordLegacyMirror(player, key);
                }
            }
        }
        return new MigrationReport(candidates.size(), alreadyMirrored, applied, failedDecode, candidates, details);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<Object> decodeLegacy(IDataKey key, CompoundTag entry) {
        try {
            return entry.read("value", key.codec(), NbtOps.INSTANCE).map(value -> (Object) value);
        } catch (RuntimeException exception) {
            EchoDataCore.LOGGER.debug("Legacy DataCore adapter could not decode {} for migration.", key.id(), exception);
            return Optional.empty();
        }
    }

    public static MigrationCandidate candidate(IDataKey<?> key) {
        if (key == null) {
            return new MigrationCandidate("", "", "", "");
        }
        DataKeyMetadata metadata = EchoCoreServices.dataService().keyMetadata(key.id()).orElse(null);
        return new MigrationCandidate(rootName(key, metadata), fieldName(key, metadata),
                metadata == null ? "" : metadata.source(), key.scope().name());
    }

    private static String rootName(IDataKey<?> key, DataKeyMetadata metadata) {
        if (metadata != null && !metadata.legacyRoot().isBlank()) {
            return metadata.legacyRoot();
        }
        String path = key.id().getPath();
        if (path.startsWith("legacy/")) {
            String[] parts = path.split("/", 3);
            return parts.length >= 2 ? parts[1] : "";
        }
        if ("echocore".equals(key.id().getNamespace()) && path.startsWith("profile/")) {
            return "echocore_profile";
        }
        if ("echocore".equals(key.id().getNamespace()) && path.startsWith("faction/")) {
            return "echocore_factions";
        }
        return ROOTS_BY_NAMESPACE.getOrDefault(key.id().getNamespace(), "");
    }

    private static String fieldName(IDataKey<?> key, DataKeyMetadata metadata) {
        if (metadata != null && !metadata.legacyField().isBlank()) {
            return metadata.legacyField();
        }
        String path = key.id().getPath();
        if (path.startsWith("legacy/")) {
            String[] parts = path.split("/", 3);
            return parts.length >= 3 ? parts[2] : "";
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static Optional<CompoundTag> legacyEntry(IDataKey<?> key, CompoundTag root, String field) {
        Object value = switch (key.kind()) {
            case FLAG -> root.getBooleanOr(field, false);
            case COUNTER -> root.getLongOr(field, root.getIntOr(field, 0));
            case STRING, ENUM -> root.getStringOr(field, "");
            case RECORD -> root.getCompoundOrEmpty(field);
        };
        CompoundTag entry = new CompoundTag();
        entry.putString("kind", key.kind().name());
        try {
            store(entry, key, value);
            return Optional.of(entry);
        } catch (RuntimeException exception) {
            EchoDataCore.LOGGER.debug("Legacy DataCore adapter could not read {} from field {}.", key.id(), field, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void store(CompoundTag entry, IDataKey key, Object value) {
        Codec codec = key.codec();
        entry.store("value", codec, NbtOps.INSTANCE, value);
    }

    private static void addAttachmentSnapshot(
            Map<Identifier, String> snapshot,
            Player player,
            String namespace,
            String attachmentName,
            String className,
            Map<String, String> methods) {
        try {
            Class<?> type = Class.forName(className);
            Method get = type.getMethod("get", Player.class);
            Object data = get.invoke(null, player);
            if (data == null) {
                return;
            }
            for (Map.Entry<String, String> methodEntry : methods.entrySet()) {
                Method method = type.getMethod(methodEntry.getValue());
                Object value = method.invoke(data);
                Identifier id = Identifier.tryParse(namespace + ":legacy/" + attachmentName + "/" + methodEntry.getKey());
                if (id != null) {
                    snapshot.put(id, summarize(value));
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Optional attachment owner is not loaded.
        }
    }

    private static String summarize(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            return "count=" + collection.size() + " " + collection.stream().limit(12).toList();
        }
        return value.toString();
    }

    private static String cleanPath(String value) {
        return value == null ? "unknown" : value.replace(':', '_').replace('|', '_').replace(' ', '_');
    }

    public record MigrationReport(
            int candidates,
            int alreadyMirrored,
            int applied,
            int failedDecode,
            Map<Identifier, String> values,
            Map<Identifier, MigrationCandidate> details) {
        public MigrationReport {
            values = values == null ? Map.of() : Map.copyOf(values);
            details = details == null ? Map.of() : Map.copyOf(details);
        }

        public MigrationReport(int candidates, int alreadyMirrored, int applied, int failedDecode,
                Map<Identifier, String> values) {
            this(candidates, alreadyMirrored, applied, failedDecode, values, Map.of());
        }
    }

    public record MigrationCandidate(String legacyRoot, String legacyField, String source, String scope) {
        public MigrationCandidate {
            legacyRoot = legacyRoot == null ? "" : legacyRoot;
            legacyField = legacyField == null ? "" : legacyField;
            source = source == null ? "" : source;
            scope = scope == null ? "" : scope;
        }
    }
}

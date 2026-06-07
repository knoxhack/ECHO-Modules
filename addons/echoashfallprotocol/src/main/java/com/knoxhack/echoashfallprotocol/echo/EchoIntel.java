package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Tracks intelligence gathered by ECHO-7 through drone reconnaissance.
 */
public class EchoIntel implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoIntel> STREAM_CODEC = StreamCodec.of(
            EchoIntel::writeSync,
            EchoIntel::readSync
    );

    private static final int MAX_INTEL_ENTRIES = 100;

    public enum IntelType {
        RECON("Field Reconnaissance", "Visual observations from drone patrols"),
        INTERCEPT("Intercepted Transmission", "Radio and communication monitoring"),
        DOSSIER("Faction Dossier", "Compiled intelligence reports"),
        TACTICAL("Tactical Data", "Troop movements and base locations"),
        DIPLOMATIC("Diplomatic Intel", "Faction relations and negotiations"),
        HISTORICAL("Historical Record", "Archived world events");

        private final String displayName;
        private final String description;

        IntelType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum IntelPriority {
        LOW(0xFF8A9BB0, "Low"),
        MEDIUM(0xFFFFA94D, "Medium"),
        HIGH(0xFFFF8C42, "High"),
        CRITICAL(0xFFFF3333, "Critical");

        private final int color;
        private final String label;

        IntelPriority(int color, String label) {
            this.color = color;
            this.label = label;
        }

        public int getColor() {
            return color;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class IntelEntry {
        public final String id;
        public final IntelType type;
        public final String title;
        public String content;
        public final long timestamp;
        public final IntelPriority priority;
        public final Identifier relatedFaction;
        public boolean isRead = false;

        public IntelEntry(String id, IntelType type, String title, String content,
                IntelPriority priority, Identifier faction) {
            this(id, type, title, content, System.currentTimeMillis(), priority, faction, false);
        }

        private IntelEntry(String id, IntelType type, String title, String content,
                long timestamp, IntelPriority priority, Identifier faction, boolean isRead) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
            this.priority = priority;
            this.relatedFaction = faction;
            this.isRead = isRead;
        }
    }

    private final List<IntelEntry> allIntel = new ArrayList<>();
    private final Map<String, IntelEntry> intelById = new HashMap<>();
    private final Map<Identifier, Integer> dossierProgress = new HashMap<>();
    private final Set<String> discoveredLore = new HashSet<>();
    private final List<String> activityLog = new ArrayList<>();

    public EchoIntel() {
        for (Identifier factionId : AshfallFactionMap.all()) {
            dossierProgress.put(factionId, 0);
        }
    }

    public void addIntel(IntelEntry entry) {
        if (entry == null || entry.id == null || entry.id.isBlank() || intelById.containsKey(entry.id)) {
            return;
        }
        if (allIntel.size() >= MAX_INTEL_ENTRIES) {
            IntelEntry oldest = allIntel.remove(0);
            intelById.remove(oldest.id);
        }
        allIntel.add(entry);
        intelById.put(entry.id, entry);
        activityLog.add(entry.title + " " + entry.content);
    }

    public void addReconIntel(String title, String content, Identifier faction, IntelPriority priority) {
        addIntel(new IntelEntry(makeId("recon", title), IntelType.RECON, title, content,
                priority == null ? IntelPriority.LOW : priority, faction));
    }

    public void addTacticalIntel(String title, String content, Identifier faction, IntelPriority priority) {
        addIntel(new IntelEntry(makeId("tactical", title), IntelType.TACTICAL, title, content,
                priority == null ? IntelPriority.MEDIUM : priority, faction));
    }

    public void addInterceptedTransmission(String title, String transmission, Identifier faction) {
        addIntel(new IntelEntry(makeId("intercept", title), IntelType.INTERCEPT, title, transmission,
                IntelPriority.HIGH, faction));
    }

    public void updateDossier(Identifier faction, String category, String info) {
        if (faction == null) {
            return;
        }
        dossierProgress.merge(faction, 1, Integer::sum);
        addIntel(new IntelEntry(makeId("dossier", faction.getPath() + "_" + category), IntelType.DOSSIER,
                AshfallFactionMap.displayName(faction) + ": " + category, info,
                IntelPriority.MEDIUM, faction));
    }

    public List<IntelEntry> getAllIntel() {
        return List.copyOf(allIntel);
    }

    public List<IntelEntry> getIntelByType(IntelType type) {
        return allIntel.stream()
                .filter(entry -> entry.type == type)
                .toList();
    }

    public List<IntelEntry> getFactionIntel(Identifier faction) {
        return allIntel.stream()
                .filter(entry -> faction != null && faction.equals(entry.relatedFaction))
                .toList();
    }

    public int getUnreadCount() {
        int count = 0;
        for (IntelEntry entry : allIntel) {
            if (!entry.isRead) {
                count++;
            }
        }
        return count;
    }

    public void markAsRead(String intelId) {
        IntelEntry entry = intelById.get(intelId);
        if (entry != null) {
            entry.isRead = true;
        }
    }

    public void markAllAsRead() {
        for (IntelEntry entry : allIntel) {
            entry.isRead = true;
        }
    }

    public int getDossierCompletion(Identifier faction) {
        return Math.min(100, dossierProgress.getOrDefault(faction, 0) * 10);
    }

    public boolean hasDiscoveredLore(String loreId) {
        return discoveredLore.contains(loreId);
    }

    public void discoverLore(String loreId, String title, String content) {
        if (loreId == null || loreId.isBlank() || !discoveredLore.add(loreId)) {
            return;
        }
        addIntel(new IntelEntry("lore_" + loreId, IntelType.HISTORICAL, title, content,
                IntelPriority.LOW, null));
    }

    public List<String> detectPatterns() {
        Map<Identifier, Integer> mentionCount = new HashMap<>();
        for (String activity : activityLog) {
            String normalized = AshfallFactionMap.normalize(activity);
            for (Identifier factionId : AshfallFactionMap.all()) {
                if (normalized.contains(factionId.getPath())
                        || normalized.contains(AshfallFactionMap.displayName(factionId).toLowerCase(java.util.Locale.ROOT))) {
                    mentionCount.merge(factionId, 1, Integer::sum);
                }
            }
        }
        List<String> patterns = new ArrayList<>();
        for (Map.Entry<Identifier, Integer> entry : mentionCount.entrySet()) {
            if (entry.getValue() >= 5) {
                patterns.add(AshfallFactionMap.displayName(entry.getKey())
                        + " activity detected - possible operation in planning");
            }
        }
        return patterns;
    }

    public String synthesizeInsight() {
        List<String> patterns = detectPatterns();
        if (!patterns.isEmpty()) {
            return patterns.get(0);
        }
        if (getUnreadCount() > 0) {
            return "Unread field intel is available for review.";
        }
        return "No unusual faction traffic detected.";
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("IntelCount", allIntel.size());
        for (int i = 0; i < allIntel.size(); i++) {
            IntelEntry entry = allIntel.get(i);
            String prefix = "Intel_" + i + "_";
            output.putString(prefix + "id", entry.id);
            output.putString(prefix + "type", entry.type.name());
            output.putString(prefix + "title", entry.title);
            output.putString(prefix + "content", entry.content);
            output.putLong(prefix + "time", entry.timestamp);
            output.putString(prefix + "priority", entry.priority.name());
            output.putString(prefix + "faction", entry.relatedFaction == null ? "" : entry.relatedFaction.toString());
            output.putBoolean(prefix + "read", entry.isRead);
        }
        output.putInt("DossierCount", dossierProgress.size());
        int index = 0;
        for (Map.Entry<Identifier, Integer> entry : dossierProgress.entrySet()) {
            output.putString("Dossier_" + index + "_faction", entry.getKey().toString());
            output.putInt("Dossier_" + index + "_progress", entry.getValue());
            index++;
        }
        output.putInt("LoreCount", discoveredLore.size());
        index = 0;
        for (String lore : discoveredLore) {
            output.putString("Lore_" + index++, lore);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        allIntel.clear();
        intelById.clear();
        int count = input.getIntOr("IntelCount", 0);
        for (int i = 0; i < count; i++) {
            String prefix = "Intel_" + i + "_";
            String id = input.getStringOr(prefix + "id", "");
            if (id.isBlank()) {
                continue;
            }
            IntelEntry entry = new IntelEntry(
                    id,
                    safeEnum(IntelType.class, input.getStringOr(prefix + "type", ""), IntelType.RECON),
                    input.getStringOr(prefix + "title", "Intel"),
                    input.getStringOr(prefix + "content", ""),
                    input.getLongOr(prefix + "time", System.currentTimeMillis()),
                    safeEnum(IntelPriority.class, input.getStringOr(prefix + "priority", ""), IntelPriority.LOW),
                    parseFaction(input.getStringOr(prefix + "faction", "")),
                    input.getBooleanOr(prefix + "read", false));
            allIntel.add(entry);
            intelById.put(entry.id, entry);
        }

        dossierProgress.clear();
        int dossierCount = input.getIntOr("DossierCount", -1);
        if (dossierCount >= 0) {
            for (int i = 0; i < dossierCount; i++) {
                Identifier faction = parseFaction(input.getStringOr("Dossier_" + i + "_faction", ""));
                if (faction != null) {
                    dossierProgress.put(faction, input.getIntOr("Dossier_" + i + "_progress", 0));
                }
            }
        } else {
            for (Identifier faction : AshfallFactionMap.all()) {
                int progress = input.getIntOr("Dossier_" + faction.getPath(), 0);
                if (progress == 0) {
                    progress = input.getIntOr("Dossier_" + faction.getPath().toUpperCase(java.util.Locale.ROOT), 0);
                }
                dossierProgress.put(faction, progress);
            }
        }

        discoveredLore.clear();
        int loreCount = input.getIntOr("LoreCount", 0);
        for (int i = 0; i < loreCount; i++) {
            String lore = input.getStringOr("Lore_" + i, "");
            if (!lore.isBlank()) {
                discoveredLore.add(lore);
            }
        }
    }

    public static EchoIntel get(net.minecraft.world.entity.player.Player player) {
        return player.getData(ModAttachments.ECHO_INTEL.get());
    }

    public static void saveAndSync(ServerPlayer player, EchoIntel intel) {
        player.setData(ModAttachments.ECHO_INTEL.get(), intel);
        player.syncData(ModAttachments.ECHO_INTEL.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.ECHO_INTEL.get());
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, EchoIntel data) {
        buf.writeVarInt(data.allIntel.size());
        for (IntelEntry entry : data.allIntel) {
            buf.writeUtf(entry.id);
            buf.writeUtf(entry.type.name());
            buf.writeUtf(entry.title);
            buf.writeUtf(entry.content);
            buf.writeLong(entry.timestamp);
            buf.writeUtf(entry.priority.name());
            buf.writeBoolean(entry.relatedFaction != null);
            if (entry.relatedFaction != null) {
                buf.writeUtf(entry.relatedFaction.toString());
            }
            buf.writeBoolean(entry.isRead);
        }

        buf.writeVarInt(data.dossierProgress.size());
        for (Map.Entry<Identifier, Integer> entry : data.dossierProgress.entrySet()) {
            buf.writeUtf(entry.getKey().toString());
            buf.writeVarInt(entry.getValue());
        }

        buf.writeVarInt(data.discoveredLore.size());
        for (String loreId : data.discoveredLore) {
            buf.writeUtf(loreId);
        }
        buf.writeVarInt(data.activityLog.size());
        for (String activity : data.activityLog) {
            buf.writeUtf(activity);
        }
    }

    private static EchoIntel readSync(RegistryFriendlyByteBuf buf) {
        EchoIntel data = new EchoIntel();
        data.allIntel.clear();
        data.intelById.clear();
        int intelCount = buf.readVarInt();
        for (int i = 0; i < intelCount; i++) {
            String id = buf.readUtf();
            IntelType type = safeEnum(IntelType.class, buf.readUtf(), IntelType.RECON);
            String title = buf.readUtf();
            String content = buf.readUtf();
            long timestamp = buf.readLong();
            IntelPriority priority = safeEnum(IntelPriority.class, buf.readUtf(), IntelPriority.LOW);
            Identifier faction = buf.readBoolean() ? parseFaction(buf.readUtf()) : null;
            boolean read = buf.readBoolean();
            IntelEntry entry = new IntelEntry(id, type, title, content, timestamp, priority, faction, read);
            data.allIntel.add(entry);
            data.intelById.put(entry.id, entry);
        }

        data.dossierProgress.clear();
        int dossierCount = buf.readVarInt();
        for (int i = 0; i < dossierCount; i++) {
            Identifier faction = parseFaction(buf.readUtf());
            int progress = buf.readVarInt();
            if (faction != null) {
                data.dossierProgress.put(faction, progress);
            }
        }

        data.discoveredLore.clear();
        int loreCount = buf.readVarInt();
        for (int i = 0; i < loreCount; i++) {
            String lore = buf.readUtf();
            if (!lore.isBlank()) {
                data.discoveredLore.add(lore);
            }
        }

        data.activityLog.clear();
        int activityCount = buf.readVarInt();
        for (int i = 0; i < activityCount; i++) {
            String activity = buf.readUtf();
            if (!activity.isBlank()) {
                data.activityLog.add(activity);
            }
        }
        return data;
    }

    private static String makeId(String prefix, String title) {
        String safe = AshfallFactionMap.normalize(title).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return prefix + "_" + safe + "_" + Integer.toHexString((prefix + title).hashCode());
    }

    private static Identifier parseFaction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AshfallFactionMap.resolveFactionId(value);
    }

    private static <T extends Enum<T>> T safeEnum(Class<T> type, String name, T fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

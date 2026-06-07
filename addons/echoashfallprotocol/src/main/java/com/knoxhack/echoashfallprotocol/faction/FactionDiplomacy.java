package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Identifier-keyed Ashfall faction relation state.
 */
public class FactionDiplomacy implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, FactionDiplomacy> STREAM_CODEC = StreamCodec.of(
            FactionDiplomacy::writeSync,
            FactionDiplomacy::readSync
    );

    private final Map<String, Integer> relations = new HashMap<>();
    private final Map<String, Long> lastStateChangeTick = new HashMap<>();
    private final Random random = new Random();

    public FactionDiplomacy() {
        initializeDefaults();
    }

    public enum DiplomaticState {
        ALLIANCE("Alliance", 0xFF4DBAF4, 75, 100),
        TRUCE("Truce", 0xFF8A9BB0, 25, 74),
        COLD_WAR("Cold War", 0xFFAAAAAA, 0, 24),
        TENSION("Tension", 0xFFFFA94D, -25, -1),
        SKIRMISH("Skirmish", 0xFFFF8C42, -50, -26),
        OPEN_WAR("Open War", 0xFFFF3333, -100, -51);

        private final String displayName;
        private final int color;
        private final int minRelation;
        private final int maxRelation;

        DiplomaticState(String displayName, int color, int minRelation, int maxRelation) {
            this.displayName = displayName;
            this.color = color;
            this.minRelation = minRelation;
            this.maxRelation = maxRelation;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }

        public static DiplomaticState fromRelation(int relation) {
            for (DiplomaticState state : values()) {
                if (relation >= state.minRelation && relation <= state.maxRelation) {
                    return state;
                }
            }
            return COLD_WAR;
        }

        public boolean isConflict() {
            return this == TENSION || this == SKIRMISH || this == OPEN_WAR;
        }

        public boolean isCooperative() {
            return this == ALLIANCE || this == TRUCE;
        }
    }

    public static final class FactionPair {
        private static final List<FactionPair> VALUES = buildPairs();
        private final Identifier factionA;
        private final Identifier factionB;

        private FactionPair(Identifier factionA, Identifier factionB) {
            this.factionA = factionA;
            this.factionB = factionB;
        }

        public Identifier getFactionA() {
            return factionA;
        }

        public Identifier getFactionB() {
            return factionB;
        }

        public static List<FactionPair> values() {
            return VALUES;
        }

        public static FactionPair fromFactions(Identifier a, Identifier b) {
            Identifier left = AshfallFactionMap.canonicalFaction(a);
            Identifier right = AshfallFactionMap.canonicalFaction(b);
            if (left == null || right == null || left.equals(right)) {
                return null;
            }
            for (FactionPair pair : VALUES) {
                if (pair.involves(left) && pair.involves(right)) {
                    return pair;
                }
            }
            return null;
        }

        public boolean involves(Identifier faction) {
            return factionA.equals(faction) || factionB.equals(faction);
        }

        public Identifier getOther(Identifier faction) {
            return factionA.equals(faction) ? factionB : factionA;
        }

        public String key() {
            return pairKey(factionA, factionB);
        }

        private static List<FactionPair> buildPairs() {
            List<FactionPair> pairs = new ArrayList<>();
            List<Identifier> factions = AshfallFactionMap.all();
            for (int left = 0; left < factions.size(); left++) {
                for (int right = left + 1; right < factions.size(); right++) {
                    pairs.add(new FactionPair(factions.get(left), factions.get(right)));
                }
            }
            return List.copyOf(pairs);
        }
    }

    public int getRelation(FactionPair pair) {
        return pair == null ? 0 : relations.getOrDefault(pair.key(), 0);
    }

    public int getRelation(Identifier a, Identifier b) {
        return getRelation(FactionPair.fromFactions(a, b));
    }

    public void setRelation(FactionPair pair, int value) {
        if (pair != null) {
            relations.put(pair.key(), clamp(value));
        }
    }

    public void modifyRelation(FactionPair pair, int amount) {
        setRelation(pair, getRelation(pair) + amount);
    }

    public DiplomaticState getState(FactionPair pair) {
        return DiplomaticState.fromRelation(getRelation(pair));
    }

    public DiplomaticState getState(Identifier a, Identifier b) {
        return getState(FactionPair.fromFactions(a, b));
    }

    public boolean isAtWar(Identifier a, Identifier b) {
        DiplomaticState state = getState(a, b);
        return state == DiplomaticState.OPEN_WAR || state == DiplomaticState.SKIRMISH;
    }

    public boolean isAllied(Identifier a, Identifier b) {
        return getState(a, b) == DiplomaticState.ALLIANCE;
    }

    public DiplomaticState getWorstStateForFaction(Identifier faction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        if (canonical == null) {
            return DiplomaticState.COLD_WAR;
        }
        DiplomaticState worst = DiplomaticState.ALLIANCE;
        for (FactionPair pair : FactionPair.values()) {
            if (pair.involves(canonical) && getState(pair).ordinal() > worst.ordinal()) {
                worst = getState(pair);
            }
        }
        return worst;
    }

    public Identifier getMostHostile(Identifier faction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        if (canonical == null) {
            return null;
        }
        Identifier mostHostile = null;
        int lowestRelation = 100;
        for (Identifier other : AshfallFactionMap.all()) {
            if (other.equals(canonical)) {
                continue;
            }
            int relation = getRelation(canonical, other);
            if (relation < lowestRelation) {
                lowestRelation = relation;
                mostHostile = other;
            }
        }
        return mostHostile;
    }

    public void tickRelations(long worldTick) {
        if (worldTick % 24000L != 0L) {
            return;
        }
        for (FactionPair pair : FactionPair.values()) {
            int current = getRelation(pair);
            if (current > 0) {
                modifyRelation(pair, -1);
            } else if (current < 0 && random.nextFloat() < 0.35F) {
                modifyRelation(pair, 1);
            }
        }
    }

    public boolean tryStateChange(FactionPair pair, DiplomaticState targetState, long worldTick, int cooldownTicks) {
        if (pair == null || targetState == null) {
            return false;
        }
        long last = lastStateChangeTick.getOrDefault(pair.key(), 0L);
        if (worldTick - last < cooldownTicks) {
            return false;
        }
        int target = switch (targetState) {
            case ALLIANCE -> 80;
            case TRUCE -> 40;
            case COLD_WAR -> 10;
            case TENSION -> -10;
            case SKIRMISH -> -35;
            case OPEN_WAR -> -75;
        };
        setRelation(pair, target);
        lastStateChangeTick.put(pair.key(), worldTick);
        return true;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("relationCount", relations.size());
        int index = 0;
        for (Map.Entry<String, Integer> entry : relations.entrySet()) {
            output.putString("relation_" + index + "_pair", entry.getKey());
            output.putInt("relation_" + index + "_value", entry.getValue());
            index++;
        }
        output.putInt("stateChangeCount", lastStateChangeTick.size());
        index = 0;
        for (Map.Entry<String, Long> entry : lastStateChangeTick.entrySet()) {
            output.putString("stateChange_" + index + "_pair", entry.getKey());
            output.putLong("stateChange_" + index + "_tick", entry.getValue());
            index++;
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        relations.clear();
        initializeDefaults();
        int relationCount = input.getIntOr("relationCount", -1);
        if (relationCount >= 0) {
            for (int i = 0; i < relationCount; i++) {
                String key = normalizePairKey(input.getStringOr("relation_" + i + "_pair", ""));
                if (!key.isBlank()) {
                    relations.put(key, clamp(input.getIntOr("relation_" + i + "_value", 0)));
                }
            }
        }

        lastStateChangeTick.clear();
        int changeCount = input.getIntOr("stateChangeCount", 0);
        for (int i = 0; i < changeCount; i++) {
            String key = normalizePairKey(input.getStringOr("stateChange_" + i + "_pair", ""));
            if (!key.isBlank()) {
                lastStateChangeTick.put(key, input.getLongOr("stateChange_" + i + "_tick", 0L));
            }
        }
    }

    public static void saveAndSync(ServerPlayer player, FactionDiplomacy diplomacy) {
        player.setData(ModAttachments.FACTION_DIPLOMACY.get(), diplomacy);
        player.syncData(ModAttachments.FACTION_DIPLOMACY.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.FACTION_DIPLOMACY.get());
    }

    private void initializeDefaults() {
        setDefault(AshfallBiomeFactions.RADWARDEN_COMPACT, AshfallBiomeFactions.SPOREBOUND_SANCTUM, -35);
        setDefault(AshfallBiomeFactions.RADWARDEN_COMPACT, AshfallBiomeFactions.CRASHBREAK_SALVAGE, 15);
        setDefault(AshfallBiomeFactions.CRASHBREAK_SALVAGE, AshfallBiomeFactions.SPOREBOUND_SANCTUM, -10);
    }

    private void setDefault(Identifier a, Identifier b, int value) {
        String key = pairKey(a, b);
        if (!key.isBlank()) {
            relations.putIfAbsent(key, value);
        }
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, FactionDiplomacy data) {
        buf.writeVarInt(data.relations.size());
        for (Map.Entry<String, Integer> entry : data.relations.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeVarInt(data.lastStateChangeTick.size());
        for (Map.Entry<String, Long> entry : data.lastStateChangeTick.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }
    }

    private static FactionDiplomacy readSync(RegistryFriendlyByteBuf buf) {
        FactionDiplomacy data = new FactionDiplomacy();
        data.relations.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = normalizePairKey(buf.readUtf());
            int value = clamp(buf.readVarInt());
            if (!key.isBlank()) {
                data.relations.put(key, value);
            }
        }
        data.lastStateChangeTick.clear();
        int changeCount = buf.readVarInt();
        for (int i = 0; i < changeCount; i++) {
            String key = normalizePairKey(buf.readUtf());
            long tick = buf.readLong();
            if (!key.isBlank()) {
                data.lastStateChangeTick.put(key, tick);
            }
        }
        return data;
    }

    private static String pairKey(Identifier a, Identifier b) {
        Identifier canonicalA = AshfallFactionMap.canonicalFaction(a);
        Identifier canonicalB = AshfallFactionMap.canonicalFaction(b);
        if (canonicalA == null || canonicalB == null || canonicalA.equals(canonicalB)) {
            return "";
        }
        String left = canonicalA.toString();
        String right = canonicalB.toString();
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }

    private static String normalizePairKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String[] parts = key.split("\\|");
        if (parts.length != 2) {
            return "";
        }
        try {
            return pairKey(Identifier.parse(parts[0]), Identifier.parse(parts[1]));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int clamp(int value) {
        return Math.max(-100, Math.min(100, value));
    }
}

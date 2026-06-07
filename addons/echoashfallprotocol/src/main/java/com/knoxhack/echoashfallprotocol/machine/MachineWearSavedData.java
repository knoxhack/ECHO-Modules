package com.knoxhack.echoashfallprotocol.machine;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-saved machine wear state.
 */
public class MachineWearSavedData extends SavedData {
    public record Entry(int wear, boolean jammed) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("wear", 0).forGetter(Entry::wear),
                Codec.BOOL.optionalFieldOf("jammed", false).forGetter(Entry::jammed)
        ).apply(instance, Entry::new));
    }

    public static final Codec<MachineWearSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Entry.CODEC).optionalFieldOf("machines", Map.of())
                    .forGetter(data -> Map.copyOf(data.entries))
    ).apply(instance, MachineWearSavedData::fromEntries));

    public static final SavedDataType<MachineWearSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "machine_wear"),
            MachineWearSavedData::new,
            CODEC
    );

    private final Map<String, Entry> entries = new HashMap<>();

    public static MachineWearSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static MachineWearSavedData fromEntries(Map<String, Entry> entries) {
        MachineWearSavedData data = new MachineWearSavedData();
        entries.forEach((key, entry) -> {
            if (entry != null && (entry.wear() > 0 || entry.jammed())) {
                data.entries.put(key, new Entry(clampWear(entry.wear()), entry.jammed()));
            }
        });
        return data;
    }

    public int getWear(BlockPos pos) {
        return entry(pos).wear();
    }

    public boolean isJammed(BlockPos pos) {
        return entry(pos).jammed();
    }

    public void setWear(BlockPos pos, int wear) {
        Entry current = entry(pos);
        put(pos, new Entry(clampWear(wear), current.jammed()));
    }

    public void setJammed(BlockPos pos, boolean jammed) {
        Entry current = entry(pos);
        put(pos, new Entry(current.wear(), jammed));
    }

    public void set(BlockPos pos, int wear, boolean jammed) {
        put(pos, new Entry(clampWear(wear), jammed));
    }

    private Entry entry(BlockPos pos) {
        return entries.getOrDefault(key(pos), new Entry(0, false));
    }

    private void put(BlockPos pos, Entry entry) {
        String key = key(pos);
        Entry normalized = new Entry(clampWear(entry.wear()), entry.jammed());
        if (normalized.wear() <= 0 && !normalized.jammed()) {
            entries.remove(key);
        } else {
            entries.put(key, normalized);
        }
        setDirty();
    }

    private static int clampWear(int wear) {
        return Math.max(0, Math.min(MachineWearData.MAX_WEAR, wear));
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class MultiblockSavedData extends SavedData {
    public static final Codec<MultiblockSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("formed", Map.of())
                    .forGetter(data -> data.formed)
    ).apply(instance, MultiblockSavedData::new));

    public static final SavedDataType<MultiblockSavedData> TYPE = new SavedDataType<>(
            EchoMultiblockCore.id("formed_multiblocks"), MultiblockSavedData::new, CODEC);

    private final Map<String, CompoundTag> formed = new LinkedHashMap<>();

    public MultiblockSavedData() {
    }

    private MultiblockSavedData(Map<String, CompoundTag> formed) {
        this.formed.putAll(copy(formed));
    }

    public static MultiblockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(Identifier definitionId, BlockPos controllerPos, float integrity, String state) {
        if (definitionId == null || controllerPos == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("definition", definitionId.toString());
        tag.putLong("pos", controllerPos.asLong());
        tag.putFloat("integrity", integrity);
        tag.putString("state", state == null ? "FORMED" : state);
        formed.put(key(controllerPos), tag);
        setDirty();
    }

    public void remove(BlockPos controllerPos) {
        if (formed.remove(key(controllerPos)) != null) {
            setDirty();
        }
    }

    public List<Entry> entries() {
        return formed.values().stream()
                .map(Entry::from)
                .filter(entry -> entry.definitionId() != null)
                .toList();
    }

    private static String key(BlockPos pos) {
        return Long.toString(pos == null ? 0L : pos.asLong());
    }

    private static Map<String, CompoundTag> copy(Map<String, CompoundTag> source) {
        Map<String, CompoundTag> copy = new LinkedHashMap<>();
        for (Map.Entry<String, CompoundTag> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return copy;
    }

    public record Entry(Identifier definitionId, BlockPos controllerPos, float integrity, String state) {
        static Entry from(CompoundTag tag) {
            Identifier id = Identifier.tryParse(tag.getStringOr("definition", ""));
            return new Entry(id, BlockPos.of(tag.getLongOr("pos", 0L)), tag.getFloatOr("integrity", 0.0F),
                    tag.getStringOr("state", "FORMED"));
        }
    }
}

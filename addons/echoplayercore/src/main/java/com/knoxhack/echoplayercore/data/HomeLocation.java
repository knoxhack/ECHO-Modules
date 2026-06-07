package com.knoxhack.echoplayercore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HomeLocation(
        String name,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        long createdAt,
        long updatedAt) {

    public static final Codec<HomeLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(HomeLocation::name),
            Codec.STRING.fieldOf("dimension").forGetter(h -> h.dimension().identifier().toString()),
            Codec.DOUBLE.fieldOf("x").forGetter(HomeLocation::x),
            Codec.DOUBLE.fieldOf("y").forGetter(HomeLocation::y),
            Codec.DOUBLE.fieldOf("z").forGetter(HomeLocation::z),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(HomeLocation::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(HomeLocation::pitch),
            Codec.LONG.optionalFieldOf("createdAt", 0L).forGetter(HomeLocation::createdAt),
            Codec.LONG.optionalFieldOf("updatedAt", 0L).forGetter(HomeLocation::updatedAt)
    ).apply(instance, HomeLocation::fromCodec));

    public HomeLocation {
        name = cleanName(name);
        dimension = dimension == null ? Level.OVERWORLD : dimension;
    }

    public static HomeLocation fromCodec(String name, String dimensionId, double x, double y, double z,
                                          float yaw, float pitch, long createdAt, long updatedAt) {
        Identifier id = Identifier.tryParse(dimensionId);
        ResourceKey<Level> key = id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
        return new HomeLocation(name, key, x, y, z, yaw, pitch, createdAt, updatedAt);
    }

    public static String cleanName(String raw) {
        if (raw == null) {
            return "home";
        }
        String stripped = raw.strip().toLowerCase(Locale.ROOT);
        return stripped.isBlank() ? "home" : stripped;
    }

    public static boolean validName(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String v = raw.strip();
        if (v.length() > 32) {
            return false;
        }
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c >= 'a' && c <= 'z') continue;
            if (c >= 'A' && c <= 'Z') continue;
            if (c >= '0' && c <= '9') continue;
            if (c == '_' || c == '-') continue;
            return false;
        }
        return true;
    }
}

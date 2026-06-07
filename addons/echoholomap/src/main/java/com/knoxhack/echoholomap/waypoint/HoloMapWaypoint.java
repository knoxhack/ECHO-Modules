package com.knoxhack.echoholomap.waypoint;

import com.knoxhack.echoholomap.EchoHoloMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public record HoloMapWaypoint(
        Identifier id,
        UUID owner,
        Scope scope,
        String dimension,
        double x,
        double y,
        double z,
        String title,
        int color,
        String icon,
        boolean visible,
        long createdTime,
        long updatedTime) {
    public static final int MAX_TITLE = 64;
    public static final int MAX_ICON = 48;
    public static final UUID NO_OWNER = new UUID(0L, 0L);
    public static final String DEATHPOINT_ICON = "deathpoint";

    public static final Codec<HoloMapWaypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(waypoint -> waypoint.id().toString()),
            Codec.STRING.optionalFieldOf("owner", NO_OWNER.toString()).forGetter(waypoint -> waypoint.owner().toString()),
            Codec.STRING.optionalFieldOf("scope", Scope.LOCAL.name()).forGetter(waypoint -> waypoint.scope().name()),
            Codec.STRING.optionalFieldOf("dimension", Level.OVERWORLD.identifier().toString()).forGetter(HoloMapWaypoint::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(HoloMapWaypoint::x),
            Codec.DOUBLE.optionalFieldOf("y", 64.0D).forGetter(HoloMapWaypoint::y),
            Codec.DOUBLE.fieldOf("z").forGetter(HoloMapWaypoint::z),
            Codec.STRING.optionalFieldOf("title", "Waypoint").forGetter(HoloMapWaypoint::title),
            Codec.INT.optionalFieldOf("color", 0xFF38DFF4).forGetter(HoloMapWaypoint::color),
            Codec.STRING.optionalFieldOf("icon", "diamond").forGetter(HoloMapWaypoint::icon),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(HoloMapWaypoint::visible),
            Codec.LONG.optionalFieldOf("created_time", 0L).forGetter(HoloMapWaypoint::createdTime),
            Codec.LONG.optionalFieldOf("updated_time", 0L).forGetter(HoloMapWaypoint::updatedTime)
    ).apply(instance, (id, owner, scope, dimension, x, y, z, title, color, icon, visible, created, updated) ->
            new HoloMapWaypoint(parseId(id), parseUuid(owner), Scope.byName(scope), dimension,
                    x, y, z, title, color, icon, visible, created, updated)));

    public HoloMapWaypoint {
        id = id == null ? newId(scope == null ? Scope.LOCAL : scope) : id;
        owner = owner == null ? NO_OWNER : owner;
        scope = scope == null ? Scope.LOCAL : scope;
        dimension = dimension == null || dimension.isBlank() ? Level.OVERWORLD.identifier().toString() : dimension.strip();
        title = clean(title, "Waypoint", MAX_TITLE);
        icon = clean(icon, "diamond", MAX_ICON);
        color = 0xFF000000 | color;
        createdTime = Math.max(0L, createdTime);
        updatedTime = Math.max(createdTime, updatedTime);
    }

    public static HoloMapWaypoint create(Scope scope, UUID owner, String dimension, double x, double y, double z,
            String title, int color, long time) {
        long safeTime = Math.max(0L, time);
        return new HoloMapWaypoint(newId(scope), owner, scope, dimension, x, y, z,
                title, color, "diamond", true, safeTime, safeTime);
    }

    public HoloMapWaypoint normalizedForServer(UUID serverOwner, Scope serverScope, long time) {
        long created = createdTime <= 0L ? time : createdTime;
        return new HoloMapWaypoint(id, serverOwner, serverScope, dimension, x, y, z,
                title, color, icon, visible, created, Math.max(time, created));
    }

    public HoloMapWaypoint withUpdatedTime(long time) {
        return new HoloMapWaypoint(id, owner, scope, dimension, x, y, z, title, color, icon,
                visible, createdTime, Math.max(createdTime, time));
    }

    public boolean isDeathpoint() {
        return DEATHPOINT_ICON.equals(icon)
                || id.getPath().startsWith("deathpoint/");
    }

    public boolean inDimension(String dimensionId) {
        return dimension.equals(dimensionId == null || dimensionId.isBlank()
                ? Level.OVERWORLD.identifier().toString()
                : dimensionId.strip());
    }

    public static Identifier newId(Scope scope) {
        Scope safeScope = scope == null ? Scope.LOCAL : scope;
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID,
                "waypoint/" + safeScope.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID());
    }

    private static Identifier parseId(String value) {
        Identifier parsed = Identifier.tryParse(value == null ? "" : value);
        return parsed == null ? newId(Scope.LOCAL) : parsed;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            return NO_OWNER;
        }
    }

    private static String clean(String value, String fallback, int maxLength) {
        String clean = value == null || value.isBlank() ? fallback : value.strip();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength);
    }

    public enum Scope {
        LOCAL,
        PERSONAL,
        SHARED;

        public static Scope byName(String name) {
            try {
                return Scope.valueOf(name == null ? "" : name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return LOCAL;
            }
        }
    }
}

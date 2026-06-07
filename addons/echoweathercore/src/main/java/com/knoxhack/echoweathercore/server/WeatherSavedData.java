package com.knoxhack.echoweathercore.server;

import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherScope;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class WeatherSavedData extends SavedData {
    private static final Codec<Identifier> IDENTIFIER_CODEC = Codec.STRING.xmap(Identifier::parse, Identifier::toString);
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
        Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
        Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
    ).apply(instance, BlockPos::new));

    private static final Codec<WeatherType> WEATHER_TYPE_CODEC = Codec.STRING.xmap(WeatherType::valueOf, WeatherType::name);
    private static final Codec<WeatherSeverity> WEATHER_SEVERITY_CODEC = Codec.STRING.xmap(WeatherSeverity::valueOf, WeatherSeverity::name);
    private static final Codec<WeatherScope> WEATHER_SCOPE_CODEC = Codec.STRING.xmap(WeatherScope::valueOf, WeatherScope::name);
    private static final Codec<WeatherPhase> WEATHER_PHASE_CODEC = Codec.STRING.xmap(WeatherPhase::valueOf, WeatherPhase::name);

    private static final Codec<ActiveWeatherEvent> EVENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUID_CODEC.fieldOf("event_id").forGetter(ActiveWeatherEvent::eventId),
        IDENTIFIER_CODEC.fieldOf("profile_id").forGetter(ActiveWeatherEvent::profileId),
        WEATHER_TYPE_CODEC.fieldOf("type").forGetter(ActiveWeatherEvent::type),
        WEATHER_SEVERITY_CODEC.fieldOf("severity").forGetter(ActiveWeatherEvent::severity),
        WEATHER_SCOPE_CODEC.fieldOf("scope").forGetter(ActiveWeatherEvent::scope),
        WEATHER_PHASE_CODEC.fieldOf("phase").forGetter(ActiveWeatherEvent::phase),
        Codec.LONG.fieldOf("start_tick").forGetter(ActiveWeatherEvent::startTick),
        Codec.LONG.fieldOf("end_tick").forGetter(ActiveWeatherEvent::endTick),
        Codec.LONG.fieldOf("warning_start_tick").forGetter(ActiveWeatherEvent::warningStartTick),
        BLOCK_POS_CODEC.optionalFieldOf("center_pos", BlockPos.ZERO).forGetter(e -> e.centerPos() != null ? e.centerPos() : BlockPos.ZERO),
        Codec.INT.optionalFieldOf("radius", 0).forGetter(ActiveWeatherEvent::radius),
        IDENTIFIER_CODEC.optionalFieldOf("region_id", Identifier.fromNamespaceAndPath("minecraft", "none")).forGetter(e -> e.regionId() != null ? e.regionId() : Identifier.fromNamespaceAndPath("minecraft", "none")),
        Codec.STRING.optionalFieldOf("movement_direction", "").forGetter(e -> e.movementDirection() != null ? e.movementDirection() : ""),
        Codec.STRING.optionalFieldOf("source_reason", "").forGetter(e -> e.sourceReason() != null ? e.sourceReason() : ""),
        IDENTIFIER_CODEC.listOf().optionalFieldOf("generated_resources", List.of()).forGetter(e -> e.generatedResources() != null ? e.generatedResources() : List.of()),
        Codec.STRING.optionalFieldOf("debug_metadata", "").forGetter(e -> e.debugMetadata() != null ? e.debugMetadata() : "")
    ).apply(instance, WeatherSavedData::createEvent));

    private static ActiveWeatherEvent createEvent(UUID eventId, Identifier profileId, WeatherType type, WeatherSeverity severity,
                                                  WeatherScope scope, WeatherPhase phase, long startTick, long endTick, long warningStartTick,
                                                  BlockPos centerPos, int radius, Identifier regionId, String movementDirection,
                                                  String sourceReason, List<Identifier> generatedResources, String debugMetadata) {
        return new ActiveWeatherEvent(eventId, profileId, type, severity, scope, phase, startTick, endTick, warningStartTick,
            centerPos.equals(BlockPos.ZERO) ? null : centerPos, radius,
            regionId.equals(Identifier.fromNamespaceAndPath("minecraft", "none")) ? null : regionId,
            movementDirection.isEmpty() ? null : movementDirection,
            sourceReason.isEmpty() ? null : sourceReason,
            generatedResources, debugMetadata.isEmpty() ? null : debugMetadata);
    }

    public static final Codec<WeatherSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EVENT_CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(WeatherSavedData::getEvents)
    ).apply(instance, WeatherSavedData::fromCodec));

    public static final SavedDataType<WeatherSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(EchoWeatherCore.MODID, "weather"),
        WeatherSavedData::new,
        CODEC
    );

    private final List<ActiveWeatherEvent> events = new ArrayList<>();

    public WeatherSavedData() {}

    private static WeatherSavedData fromCodec(List<ActiveWeatherEvent> events) {
        WeatherSavedData data = new WeatherSavedData();
        data.events.addAll(events);
        return data;
    }

    public List<ActiveWeatherEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public void setEvents(List<ActiveWeatherEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        setDirty();
    }

    public static WeatherSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}

package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class WeatherCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    public WeatherCoreCreatorAdapter() {
        super("weathercore", "echoweathercore", "ECHO: WeatherCore", null,
                Set.of("preview"),
                "WeatherCore not installed; weather_event drafts remain generic JSON templates.",
                "WeatherCore detected; weather preview adapter is stubbed until a public authoring API is available.",
                true);
    }
}

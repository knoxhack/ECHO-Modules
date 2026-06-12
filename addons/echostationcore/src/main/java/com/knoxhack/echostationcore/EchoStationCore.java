
package com.knoxhack.echostationcore;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Station Core.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoStationCore {
    public static final String MODID = "echostationcore";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore");
    public static final List<String> PROVIDES = List.of("foundation.stations", "foundation.station_roles", "foundation.shared_recipe_surfaces");

    public EchoStationCore() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Station Core online: " + String.join(", ", PROVIDES));
    }
}

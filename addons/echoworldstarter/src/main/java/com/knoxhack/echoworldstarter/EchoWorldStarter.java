
package com.knoxhack.echoworldstarter;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO World Starter.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoWorldStarter {
    public static final String MODID = "echoworldstarter";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore", "echostationcore");
    public static final List<String> PROVIDES = List.of("foundation.spawn_safety", "foundation.first_hour", "foundation.shelter_rules", "foundation.starter_items");

    public EchoWorldStarter() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO World Starter online: " + String.join(", ", PROVIDES));
    }
}

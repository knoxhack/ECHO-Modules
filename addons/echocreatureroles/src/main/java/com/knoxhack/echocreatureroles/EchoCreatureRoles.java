
package com.knoxhack.echocreatureroles;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Creature Roles.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoCreatureRoles {
    public static final String MODID = "echocreatureroles";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore");
    public static final List<String> PROVIDES = List.of("foundation.creature_roles", "foundation.spawn_roles", "foundation.ai_pressure_roles");

    public EchoCreatureRoles() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Creature Roles online: " + String.join(", ", PROVIDES));
    }
}

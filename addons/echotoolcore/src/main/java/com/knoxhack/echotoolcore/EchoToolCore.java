
package com.knoxhack.echotoolcore;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Tool Core.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoToolCore {
    public static final String MODID = "echotoolcore";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore");
    public static final List<String> PROVIDES = List.of("foundation.tools", "foundation.tool_roles", "foundation.tool_progression");

    public EchoToolCore() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Tool Core online: " + String.join(", ", PROVIDES));
    }
}


package com.knoxhack.echomaterialcore;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Material Core.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoMaterialCore {
    public static final String MODID = "echomaterialcore";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore");
    public static final List<String> PROVIDES = List.of("foundation.materials", "foundation.generic_blocks", "foundation.generic_items", "foundation.material_tags");

    public EchoMaterialCore() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Material Core online: " + String.join(", ", PROVIDES));
    }
}

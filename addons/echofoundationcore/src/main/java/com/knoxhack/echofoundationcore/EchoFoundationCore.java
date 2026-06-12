
package com.knoxhack.echofoundationcore;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Foundation Core.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoFoundationCore {
    public static final String MODID = "echofoundationcore";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore");
    public static final List<String> PROVIDES = List.of("foundation.core", "foundation.ownership", "foundation.aliases", "foundation.legal_identity", "foundation.registry_contracts");

    public EchoFoundationCore() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Foundation Core online: " + String.join(", ", PROVIDES));
    }
}


package com.knoxhack.echocommonloot;

import java.util.List;

/**
 * Lightweight runtime marker for ECHO Common Loot.
 *
 * <p>The Foundation modules are data-first modules today. Keeping an explicit
 * Java entrypoint gives Gradle, launcher validation, and future registries a
 * stable class to target without forcing game-specific behavior into this layer.</p>
 */
public final class EchoCommonLoot {
    public static final String MODID = "echocommonloot";
    public static final List<String> REQUIRES = List.of("echoadaptercore", "echocore", "echonetcore", "echofoundationcore", "echomaterialcore", "echotoolcore", "echostationcore");
    public static final List<String> PROVIDES = List.of("foundation.common_loot", "foundation.block_drops", "foundation.starter_caches");

    public EchoCommonLoot() {
        bootstrap();
    }

    public void bootstrap() {
        System.out.println("ECHO Common Loot online: " + String.join(", ", PROVIDES));
    }
}

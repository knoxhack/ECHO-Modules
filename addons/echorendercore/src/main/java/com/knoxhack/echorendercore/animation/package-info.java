/**
 * Stable animation timeline and playback records used by RenderCore visual profiles.
 *
 * <p>These types are safe for shared profile authoring, migration, data generation, and deterministic tests. Client
 * renderers consume the resolved animation state from this package but renderer-only helpers stay under
 * {@code com.knoxhack.echorendercore.client}.</p>
 */
package com.knoxhack.echorendercore.animation;

package com.knoxhack.echoashfallprotocol.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render state for drone entities with pulsing light animation.
 */
public class DroneRenderState extends LivingEntityRenderState {

    /** Pulse phase for core glow animation (0.0 to 1.0) */
    public float pulsePhase = 0.0f;

    /** Hover offset for bobbing animation */
    public float hoverOffset = 0.0f;

    // --- ECHO-7 voice linkage ---

    /** Current ECHO-7 mood id (EchoCompanionDrone.MOOD_*). */
    public int mood = 1;

    /** Current speech line shown above drone ("" = silent). */
    public String speechText = "";

    /** Remaining ticks the speech bubble will display (for fade out). */
    public int speechTicks = 0;

    /** Remaining alert flash ticks (particle burst / pulse). */
    public int alertFlash = 0;

    /** Drone mode display name (e.g. "Follow"). */
    public String modeName = "";

    /** Repair level 0-100 (for nametag warning). */
    public int repairLevel = 100;

    /** True if this drone has an owner (for showing ECHO-7 nametag). */
    public boolean owned = false;
}

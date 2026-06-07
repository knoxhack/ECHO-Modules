package com.knoxhack.echosoundcore.api.context;

import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import net.minecraft.resources.Identifier;

public final class SoundCoreContext {
    private SoundCoreChapter chapter = SoundCoreChapter.UNKNOWN;
    private Identifier biome = null;
    private Identifier region = null;
    private Identifier structure = null;
    private Identifier faction = null;
    private Identifier missionId = null;
    private int hazardLevel = 0;
    private SoundCoreCombatIntensity combatIntensity = SoundCoreCombatIntensity.NONE;
    private Identifier bossId = null;
    private float nexusCorruptionLevel = 0.0f;
    private boolean terminalOpen = false;
    private boolean mapOpen = false;
    private boolean lensScanActive = false;
    private boolean underground = false;
    private boolean inVehicle = false;
    private boolean inStationOrbit = false;
    private float panicLevel = 0.0f;
    private int powerGridAlertLevel = 0;

    public SoundCoreContext() {}

    public SoundCoreContext copy() {
        SoundCoreContext c = new SoundCoreContext();
        c.chapter = this.chapter;
        c.biome = this.biome;
        c.region = this.region;
        c.structure = this.structure;
        c.faction = this.faction;
        c.missionId = this.missionId;
        c.hazardLevel = this.hazardLevel;
        c.combatIntensity = this.combatIntensity;
        c.bossId = this.bossId;
        c.nexusCorruptionLevel = this.nexusCorruptionLevel;
        c.terminalOpen = this.terminalOpen;
        c.mapOpen = this.mapOpen;
        c.lensScanActive = this.lensScanActive;
        c.underground = this.underground;
        c.inVehicle = this.inVehicle;
        c.inStationOrbit = this.inStationOrbit;
        c.panicLevel = this.panicLevel;
        c.powerGridAlertLevel = this.powerGridAlertLevel;
        return c;
    }

    public SoundCoreChapter chapter() { return chapter; }
    public SoundCoreContext chapter(SoundCoreChapter chapter) { this.chapter = chapter; return this; }

    public Identifier biome() { return biome; }
    public SoundCoreContext biome(Identifier biome) { this.biome = biome; return this; }

    public Identifier region() { return region; }
    public SoundCoreContext region(Identifier region) { this.region = region; return this; }

    public Identifier structure() { return structure; }
    public SoundCoreContext structure(Identifier structure) { this.structure = structure; return this; }

    public Identifier faction() { return faction; }
    public SoundCoreContext faction(Identifier faction) { this.faction = faction; return this; }

    public Identifier missionId() { return missionId; }
    public SoundCoreContext missionId(Identifier missionId) { this.missionId = missionId; return this; }

    public int hazardLevel() { return hazardLevel; }
    public SoundCoreContext hazardLevel(int hazardLevel) { this.hazardLevel = hazardLevel; return this; }

    public SoundCoreCombatIntensity combatIntensity() { return combatIntensity; }
    public SoundCoreContext combatIntensity(SoundCoreCombatIntensity combatIntensity) { this.combatIntensity = combatIntensity; return this; }

    public Identifier bossId() { return bossId; }
    public SoundCoreContext bossId(Identifier bossId) { this.bossId = bossId; return this; }

    public float nexusCorruptionLevel() { return nexusCorruptionLevel; }
    public SoundCoreContext nexusCorruptionLevel(float nexusCorruptionLevel) { this.nexusCorruptionLevel = nexusCorruptionLevel; return this; }

    public boolean terminalOpen() { return terminalOpen; }
    public SoundCoreContext terminalOpen(boolean terminalOpen) { this.terminalOpen = terminalOpen; return this; }

    public boolean mapOpen() { return mapOpen; }
    public SoundCoreContext mapOpen(boolean mapOpen) { this.mapOpen = mapOpen; return this; }

    public boolean lensScanActive() { return lensScanActive; }
    public SoundCoreContext lensScanActive(boolean lensScanActive) { this.lensScanActive = lensScanActive; return this; }

    public boolean underground() { return underground; }
    public SoundCoreContext underground(boolean underground) { this.underground = underground; return this; }

    public boolean inVehicle() { return inVehicle; }
    public SoundCoreContext inVehicle(boolean inVehicle) { this.inVehicle = inVehicle; return this; }

    public boolean inStationOrbit() { return inStationOrbit; }
    public SoundCoreContext inStationOrbit(boolean inStationOrbit) { this.inStationOrbit = inStationOrbit; return this; }

    public float panicLevel() { return panicLevel; }
    public SoundCoreContext panicLevel(float panicLevel) { this.panicLevel = panicLevel; return this; }

    public int powerGridAlertLevel() { return powerGridAlertLevel; }
    public SoundCoreContext powerGridAlertLevel(int powerGridAlertLevel) { this.powerGridAlertLevel = powerGridAlertLevel; return this; }
}

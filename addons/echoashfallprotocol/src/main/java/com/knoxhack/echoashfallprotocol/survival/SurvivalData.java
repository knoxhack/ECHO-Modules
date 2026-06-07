package com.knoxhack.echoashfallprotocol.survival;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Player survival data attachment.
 * Tracks air filter, radiation, hydration, and mask durability.
 */
public class SurvivalData implements EchoValueIOSerializable {
    public static final int MAX_AIR_FILTER = 1000;
    public static final int MAX_HYDRATION = 100;
    public static final float MAX_RADIATION = 100.0f;
    public static final int HISTORY_COUNT = 60;

    private int airFilterLife = MAX_AIR_FILTER;
    private float radiationLevel = 0.0f;
    private int hydration = MAX_HYDRATION;
    private boolean hasMask = false;
    private int filterTier = 0;
    private long graceStartTick = -1L;
    private boolean graceStartMessageSent = false;
    private boolean graceEndingMessageSent = false;
    private boolean toxicAir = false;
    private boolean radiationZone = false;
    private boolean safeZone = false;
    private String hazardReason = "";
    private int toxicAirWarningTicks = 0;
    private String primaryHazard = HazardZoneManager.HazardType.NONE.name();
    private String hazardSeverity = HazardZoneManager.HazardSeverity.NONE.name();
    private float hazardIntensity = 0.0f;
    private boolean cryoZone = false;
    private boolean acidContact = false;
    private boolean nexusAnomaly = false;
    private boolean radiationStorm = false;
    private boolean stormSheltered = false;
    
    // Modernization - Real-time Telemetry
    private float[] radiationHistory = new float[HISTORY_COUNT];
    private int historyIndex = 0;

    public SurvivalData() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, SurvivalData> STREAM_CODEC = StreamCodec.of(
            SurvivalData::writeSync,
            SurvivalData::readSync
    );

    private static void writeSync(RegistryFriendlyByteBuf buf, SurvivalData data) {
        buf.writeVarInt(data.airFilterLife);
        buf.writeFloat(data.radiationLevel);
        buf.writeVarInt(data.hydration);
        buf.writeBoolean(data.hasMask);
        buf.writeVarInt(data.filterTier);
        buf.writeLong(data.graceStartTick);
        buf.writeBoolean(data.graceStartMessageSent);
        buf.writeBoolean(data.graceEndingMessageSent);
        buf.writeBoolean(data.toxicAir);
        buf.writeBoolean(data.radiationZone);
        buf.writeBoolean(data.safeZone);
        buf.writeUtf(data.hazardReason);
        buf.writeVarInt(data.toxicAirWarningTicks);
        buf.writeUtf(data.primaryHazard);
        buf.writeUtf(data.hazardSeverity);
        buf.writeFloat(data.hazardIntensity);
        buf.writeBoolean(data.cryoZone);
        buf.writeBoolean(data.acidContact);
        buf.writeBoolean(data.nexusAnomaly);
        buf.writeBoolean(data.radiationStorm);
        buf.writeBoolean(data.stormSheltered);
        buf.writeVarInt(data.historyIndex);
        for (int i = 0; i < HISTORY_COUNT; i++) {
            buf.writeFloat(data.radiationHistory[i]);
        }
    }

    private static SurvivalData readSync(RegistryFriendlyByteBuf buf) {
        SurvivalData data = new SurvivalData();
        data.airFilterLife = buf.readVarInt();
        data.radiationLevel = buf.readFloat();
        data.hydration = buf.readVarInt();
        data.hasMask = buf.readBoolean();
        data.filterTier = buf.readVarInt();
        data.graceStartTick = buf.readLong();
        data.graceStartMessageSent = buf.readBoolean();
        data.graceEndingMessageSent = buf.readBoolean();
        data.toxicAir = buf.readBoolean();
        data.radiationZone = buf.readBoolean();
        data.safeZone = buf.readBoolean();
        data.hazardReason = buf.readUtf();
        data.toxicAirWarningTicks = buf.readVarInt();
        data.primaryHazard = buf.readUtf();
        data.hazardSeverity = buf.readUtf();
        data.hazardIntensity = buf.readFloat();
        data.cryoZone = buf.readBoolean();
        data.acidContact = buf.readBoolean();
        data.nexusAnomaly = buf.readBoolean();
        data.radiationStorm = buf.readBoolean();
        data.stormSheltered = buf.readBoolean();
        data.historyIndex = buf.readVarInt();
        for (int i = 0; i < HISTORY_COUNT; i++) {
            data.radiationHistory[i] = buf.readFloat();
        }
        return data;
    }

    // === AIR FILTER ===
    public int getAirFilterLife() { return airFilterLife; }
    public void setAirFilterLife(int value) { airFilterLife = Math.max(0, Math.min(MAX_AIR_FILTER, value)); }
    public void decrementFilter(int amount) { setAirFilterLife(airFilterLife - amount); }
    public boolean isFilterDepleted() { return airFilterLife <= 0; }
    public float getFilterPercent() { return (float) airFilterLife / MAX_AIR_FILTER; }

    // === RADIATION ===
    public float getRadiationLevel() { return radiationLevel; }
    public void setRadiationLevel(float value) { 
        radiationLevel = Math.max(0.0f, Math.min(MAX_RADIATION, value)); 
    }
    public void addRadiation(float amount) { setRadiationLevel(radiationLevel + amount); }
    public void decayRadiation(float amount) { setRadiationLevel(radiationLevel - amount); }

    public void updateRadiationHistory(float currentRad) {
        radiationHistory[historyIndex] = currentRad;
        historyIndex = (historyIndex + 1) % HISTORY_COUNT;
    }

    public float[] getRadiationHistory() {
        float[] sorted = new float[HISTORY_COUNT];
        for (int i = 0; i < HISTORY_COUNT; i++) {
            sorted[i] = radiationHistory[(historyIndex + i) % HISTORY_COUNT];
        }
        return sorted;
    }

    // === HYDRATION ===
    public int getHydration() { return hydration; }
    public void setHydration(int value) { hydration = Math.max(0, Math.min(MAX_HYDRATION, value)); }
    public void decrementHydration(int amount) { setHydration(hydration - amount); }
    public void addHydration(int amount) { setHydration(hydration + amount); }
    public float getHydrationPercent() { return (float) hydration / MAX_HYDRATION; }

    // === MASK ===
    public boolean hasMask() { return hasMask; }
    public void setHasMask(boolean value) { hasMask = value; }
    public int getFilterTier() { return filterTier; }
    public void setFilterTier(int tier) { filterTier = Math.max(0, Math.min(3, tier)); }

    // === NEW PLAYER GRACE ===
    public boolean isGraceInitialized() { return graceStartTick >= 0L; }
    public long getGraceStartTick() { return graceStartTick; }
    public void initializeGrace(long currentTick) {
        if (!isGraceInitialized()) {
            graceStartTick = Math.max(0L, currentTick);
        }
    }
    public boolean isGraceActive(long currentTick, int graceTicks) {
        return graceTicks > 0 && isGraceInitialized() && currentTick >= graceStartTick && currentTick - graceStartTick < graceTicks;
    }
    public long getGraceTicksRemaining(long currentTick, int graceTicks) {
        if (!isGraceActive(currentTick, graceTicks)) return 0L;
        return Math.max(0L, graceTicks - (currentTick - graceStartTick));
    }
    public boolean isGraceStartMessageSent() { return graceStartMessageSent; }
    public void setGraceStartMessageSent(boolean value) { graceStartMessageSent = value; }
    public boolean isGraceEndingMessageSent() { return graceEndingMessageSent; }
    public void setGraceEndingMessageSent(boolean value) { graceEndingMessageSent = value; }

    // === CURRENT HAZARD STATE ===
    public boolean isToxicAirActive() { return toxicAir; }
    public boolean isRadiationZone() { return radiationZone; }
    public boolean isSafeZone() { return safeZone; }
    public String getHazardReason() { return hazardReason; }
    public String getPrimaryHazard() { return primaryHazard; }
    public String getHazardSeverity() { return hazardSeverity; }
    public float getHazardIntensity() { return hazardIntensity; }
    public boolean isCryoZone() { return cryoZone; }
    public boolean isAcidContact() { return acidContact; }
    public boolean isNexusAnomaly() { return nexusAnomaly; }
    public boolean isRadiationStorm() { return radiationStorm; }
    public boolean isStormSheltered() { return stormSheltered; }
    public int getToxicAirWarningTicks() { return toxicAirWarningTicks; }
    public void setToxicAirWarningTicks(int ticks) { toxicAirWarningTicks = Math.max(0, ticks); }
    public boolean setHazardState(boolean toxicAir, boolean radiationZone, boolean safeZone, String hazardReason) {
        String normalizedReason = hazardReason == null ? "" : hazardReason;
        boolean changed = this.toxicAir != toxicAir
                || this.radiationZone != radiationZone
                || this.safeZone != safeZone
                || !this.hazardReason.equals(normalizedReason)
                || cryoZone
                || acidContact
                || nexusAnomaly
                || radiationStorm
                || stormSheltered
                || !primaryHazard.equals(HazardZoneManager.HazardType.NONE.name())
                || !hazardSeverity.equals(HazardZoneManager.HazardSeverity.NONE.name())
                || hazardIntensity != 0.0f;
        this.toxicAir = toxicAir;
        this.radiationZone = radiationZone;
        this.safeZone = safeZone;
        this.hazardReason = normalizedReason;
        this.cryoZone = false;
        this.acidContact = false;
        this.nexusAnomaly = false;
        this.radiationStorm = false;
        this.stormSheltered = false;
        this.primaryHazard = safeZone ? HazardZoneManager.HazardType.SAFE_ZONE.name() : HazardZoneManager.HazardType.NONE.name();
        this.hazardSeverity = HazardZoneManager.HazardSeverity.NONE.name();
        this.hazardIntensity = 0.0f;
        return changed;
    }

    public boolean setHazardSnapshot(HazardZoneManager.HazardSnapshot snapshot) {
        if (snapshot == null) {
            return setHazardState(false, false, false, "");
        }
        String normalizedReason = snapshot.reason() == null ? "" : snapshot.reason();
        String newPrimary = snapshot.primaryType().name();
        String newSeverity = snapshot.severity().name();
        float newIntensity = Math.max(0.0f, snapshot.primaryIntensity());
        boolean changed = toxicAir != snapshot.toxicAir()
                || radiationZone != snapshot.radiationZone()
                || safeZone != snapshot.safeZone()
                || cryoZone != snapshot.cryoCold()
                || acidContact != snapshot.acidContact()
                || nexusAnomaly != snapshot.nexusAnomaly()
                || radiationStorm != snapshot.radiationStorm()
                || stormSheltered != snapshot.stormSheltered()
                || !hazardReason.equals(normalizedReason)
                || !primaryHazard.equals(newPrimary)
                || !hazardSeverity.equals(newSeverity)
                || Float.compare(hazardIntensity, newIntensity) != 0;
        toxicAir = snapshot.toxicAir();
        radiationZone = snapshot.radiationZone();
        safeZone = snapshot.safeZone();
        cryoZone = snapshot.cryoCold();
        acidContact = snapshot.acidContact();
        nexusAnomaly = snapshot.nexusAnomaly();
        radiationStorm = snapshot.radiationStorm();
        stormSheltered = snapshot.stormSheltered();
        hazardReason = normalizedReason;
        primaryHazard = newPrimary;
        hazardSeverity = newSeverity;
        hazardIntensity = newIntensity;
        return changed;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("airFilterLife", airFilterLife);
        output.putFloat("radiationLevel", radiationLevel);
        output.putInt("hydration", hydration);
        output.putBoolean("hasMask", hasMask);
        output.putInt("filterTier", filterTier);
        output.putLong("graceStartTick", graceStartTick);
        output.putBoolean("graceStartMessageSent", graceStartMessageSent);
        output.putBoolean("graceEndingMessageSent", graceEndingMessageSent);
        output.putBoolean("toxicAir", toxicAir);
        output.putBoolean("radiationZone", radiationZone);
        output.putBoolean("safeZone", safeZone);
        output.putString("hazardReason", hazardReason);
        output.putInt("toxicAirWarningTicks", toxicAirWarningTicks);
        output.putString("primaryHazard", primaryHazard);
        output.putString("hazardSeverity", hazardSeverity);
        output.putFloat("hazardIntensity", hazardIntensity);
        output.putBoolean("cryoZone", cryoZone);
        output.putBoolean("acidContact", acidContact);
        output.putBoolean("nexusAnomaly", nexusAnomaly);
        output.putBoolean("radiationStorm", radiationStorm);
        output.putBoolean("stormSheltered", stormSheltered);
        
        // Serialize history
        output.putInt("historyIndex", historyIndex);
        for (int i = 0; i < HISTORY_COUNT; i++) {
            output.putFloat("radH" + i, radiationHistory[i]);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        airFilterLife = input.getIntOr("airFilterLife", MAX_AIR_FILTER);
        radiationLevel = input.getFloatOr("radiationLevel", 0.0f);
        hydration = input.getIntOr("hydration", MAX_HYDRATION);
        hasMask = input.getBooleanOr("hasMask", false);
        filterTier = input.getIntOr("filterTier", 0);
        graceStartTick = input.getLongOr("graceStartTick", -1L);
        graceStartMessageSent = input.getBooleanOr("graceStartMessageSent", false);
        graceEndingMessageSent = input.getBooleanOr("graceEndingMessageSent", false);
        toxicAir = input.getBooleanOr("toxicAir", false);
        radiationZone = input.getBooleanOr("radiationZone", false);
        safeZone = input.getBooleanOr("safeZone", false);
        hazardReason = input.getStringOr("hazardReason", "");
        toxicAirWarningTicks = input.getIntOr("toxicAirWarningTicks", 0);
        primaryHazard = input.getStringOr("primaryHazard", HazardZoneManager.HazardType.NONE.name());
        hazardSeverity = input.getStringOr("hazardSeverity", HazardZoneManager.HazardSeverity.NONE.name());
        hazardIntensity = input.getFloatOr("hazardIntensity", 0.0f);
        cryoZone = input.getBooleanOr("cryoZone", false);
        acidContact = input.getBooleanOr("acidContact", false);
        nexusAnomaly = input.getBooleanOr("nexusAnomaly", false);
        radiationStorm = input.getBooleanOr("radiationStorm", false);
        stormSheltered = input.getBooleanOr("stormSheltered", false);
        
        // Deserialize history
        historyIndex = input.getIntOr("historyIndex", 0);
        for (int i = 0; i < HISTORY_COUNT; i++) {
            radiationHistory[i] = input.getFloatOr("radH" + i, 0.0f);
        }
    }
}

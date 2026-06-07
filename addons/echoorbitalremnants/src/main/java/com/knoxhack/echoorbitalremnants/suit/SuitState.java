package com.knoxhack.echoorbitalremnants.suit;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class SuitState {
    private static final String ROOT = "echoorbitalremnants_suit";
    private static final int BASELINE_STATION_POWER = 12;
    private static final int LOW_ORBIT_STATION_POWER = 28;
    private static final int LIFE_SUPPORT_STATION_POWER = 46;
    private static final int STATION_RELAY_POWER_STEP = 12;
    private static final int RESTORED_STATION_POWER = 82;
    private static final int FULL_STATION_POWER = 100;

    private int oxygen = 100;
    private int pressure = 100;
    private int radiation = 0;
    private boolean helmetSealSecure = true;
    private boolean suitLeak;
    private boolean backupAirAvailable = true;
    private boolean autoSealAvailable = true;
    private int stationPower = BASELINE_STATION_POWER;
    private float gravity = 1.0F;

    public static SuitState get(Player player) {
        return read(player.getPersistentData().getCompoundOrEmpty(ROOT));
    }

    public void save(Player player) {
        player.getPersistentData().put(ROOT, write());
    }

    public void updateEnvironment(boolean fullSuit, boolean magneticBoots, boolean orbitalExposure) {
        gravity = orbitalExposure ? (magneticBoots ? 0.62F : 0.18F) : 1.0F;
        if (orbitalExposure) {
            helmetSealSecure = fullSuit && !suitLeak;
        }
    }

    public void tickVacuum(Player player, boolean fullSuit, boolean magneticBoots, boolean orbitalExposure, boolean oxygenBooster) {
        updateEnvironment(fullSuit, magneticBoots, orbitalExposure);

        if (!orbitalExposure) {
            oxygen = Math.min(100, oxygen + 1);
            pressure = Math.min(100, pressure + 1);
            radiation = Math.max(0, radiation - 1);
            helmetSealSecure = true;
            suitLeak = false;
            return;
        }

        helmetSealSecure = fullSuit;
        if (!fullSuit) {
            pressure = Math.max(0, pressure - 2);
            oxygen = Math.max(0, oxygen - 2);
            suitLeak = true;
            return;
        }

        int oxygenDrain = oxygenBooster && player.tickCount % 2 == 0 ? 0 : 1;
        oxygen = Math.max(0, oxygen - oxygenDrain);
        if (radiation > 70) {
            pressure = Math.max(0, pressure - 1);
        }
    }

    public void addRadiation(boolean shielded) {
        radiation = Math.min(100, radiation + (shielded ? 1 : 3));
    }

    public void drainOxygen(int amount) {
        oxygen = Math.max(0, oxygen - amount);
    }

    public void compromisePressure(int amount) {
        pressure = Math.max(0, pressure - amount);
        helmetSealSecure = false;
        suitLeak = true;
    }

    public void reduceRadiation(int amount) {
        radiation = Math.max(0, radiation - amount);
    }

    public void applyThermalRecovery() {
        pressure = Math.min(100, pressure + 15);
        helmetSealSecure = !suitLeak;
    }

    public void useEmergencyOxygen() {
        oxygen = Math.min(100, oxygen + 35);
        backupAirAvailable = false;
    }

    public void boostOxygen(int amount) {
        oxygen = Math.min(100, oxygen + amount);
    }

    void stabilizeFromStationPower(int oxygenAmount, int pressureAmount, int radiationAmount) {
        recoverOxygen(oxygenAmount);
        recoverPressure(pressureAmount);
        reduceRadiation(radiationAmount);
    }

    public void applySealantPatch() {
        pressure = Math.min(100, pressure + 45);
        helmetSealSecure = true;
        suitLeak = false;
        autoSealAvailable = false;
    }

    void syncStationPower(EchoTerminalProgress progress, boolean hasStationPowerMatrix) {
        if (progress == null) {
            stationPower = clampStationPower(stationPower);
            return;
        }

        int power = BASELINE_STATION_POWER;
        if (progress.lowOrbitReached()) {
            power = Math.max(power, LOW_ORBIT_STATION_POWER);
        }
        if (progress.stationLifeSupportRestored()) {
            power = Math.max(power, LIFE_SUPPORT_STATION_POWER);
        }

        int relayRepairs = Math.min(3, Math.max(0, progress.stationRelayRepairs()));
        if (relayRepairs > 0) {
            power = Math.max(power, LIFE_SUPPORT_STATION_POWER + relayRepairs * STATION_RELAY_POWER_STEP);
        }

        if (progress.stationNetworkRestored()) {
            power = Math.max(power, RESTORED_STATION_POWER);
            if (hasStationPowerMatrix) {
                power = FULL_STATION_POWER;
            }
        }

        stationPower = clampStationPower(power);
    }

    public boolean critical() {
        return oxygen <= 0 || pressure <= 0;
    }

    public int oxygen() {
        return oxygen;
    }

    public int pressure() {
        return pressure;
    }

    public int radiation() {
        return radiation;
    }

    public boolean helmetSealSecure() {
        return helmetSealSecure;
    }

    public boolean suitLeak() {
        return suitLeak;
    }

    public boolean backupAirAvailable() {
        return backupAirAvailable;
    }

    public boolean autoSealAvailable() {
        return autoSealAvailable;
    }

    public int stationPower() {
        return stationPower;
    }

    public float gravity() {
        return gravity;
    }

    public static boolean hasFullPressureSuit(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.PRESSURIZED_HELMET.get()
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.PRESSURIZED_CHESTPLATE.get()
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.PRESSURIZED_LEGGINGS.get()
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MAGNETIC_BOOTS.get();
    }

    public static boolean hasMagneticBoots(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MAGNETIC_BOOTS.get();
    }

    public static boolean hasRadiationVisor(Player player) {
        return hasFullPressureSuit(player) && hasCarriedModule(player, ModItems.RADIATION_VISOR.get());
    }

    public static boolean hasOxygenBooster(Player player) {
        return hasCarriedModule(player, ModItems.OXYGEN_BOOSTER.get());
    }

    public static boolean hasThermalLiner(Player player) {
        return hasCarriedModule(player, ModItems.THERMAL_SPACE_LINER.get());
    }

    private static boolean hasCarriedModule(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private void recoverOxygen(int amount) {
        oxygen = Math.min(100, oxygen + Math.max(0, amount));
    }

    private void recoverPressure(int amount) {
        pressure = Math.min(100, pressure + Math.max(0, amount));
        if (!suitLeak && pressure > 35) {
            helmetSealSecure = true;
        }
    }

    private static int clampStationPower(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("oxygen", oxygen);
        tag.putInt("pressure", pressure);
        tag.putInt("radiation", radiation);
        tag.putBoolean("helmet_seal_secure", helmetSealSecure);
        tag.putBoolean("suit_leak", suitLeak);
        tag.putBoolean("backup_air_available", backupAirAvailable);
        tag.putBoolean("auto_seal_available", autoSealAvailable);
        tag.putInt("station_power", stationPower);
        tag.putDouble("gravity", gravity);
        return tag;
    }

    private static SuitState read(CompoundTag tag) {
        SuitState state = new SuitState();
        state.oxygen = tag.getIntOr("oxygen", 100);
        state.pressure = tag.getIntOr("pressure", 100);
        state.radiation = tag.getIntOr("radiation", 0);
        state.helmetSealSecure = tag.getBooleanOr("helmet_seal_secure", true);
        state.suitLeak = tag.getBooleanOr("suit_leak", false);
        state.backupAirAvailable = tag.getBooleanOr("backup_air_available", true);
        state.autoSealAvailable = tag.getBooleanOr("auto_seal_available", true);
        state.stationPower = clampStationPower(tag.getIntOr("station_power", BASELINE_STATION_POWER));
        state.gravity = (float) tag.getDoubleOr("gravity", 1.0D);
        return state;
    }
}

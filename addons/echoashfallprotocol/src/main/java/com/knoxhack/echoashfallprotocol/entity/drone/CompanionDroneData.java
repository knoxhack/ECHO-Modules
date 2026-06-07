package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneOwnerData;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

public class CompanionDroneData implements EchoValueIOSerializable {
    public static final int SCHEMA_VERSION = 2;
    public static final String DEFAULT_NAME = "Companion Drone";

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionDroneData> STREAM_CODEC = StreamCodec.of(
            CompanionDroneData::writeSync,
            CompanionDroneData::readSync);

    private int schemaVersion = SCHEMA_VERSION;
    private String ownerUuid = "";
    private String droneUuid = "";
    private String customName = DEFAULT_NAME;
    private EchoDroneMode mode = EchoDroneMode.FOLLOW;
    private String taskLabel = EchoDroneMode.FOLLOW.taskLabel();
    private int batteryPercent = 100;
    private int health = 15;
    private int signalQuality = 100;
    private long lastScanTime = Long.MIN_VALUE;
    private long lastScoutTime = Long.MIN_VALUE;
    private long lastWarningTime = Long.MIN_VALUE;
    private long lastCommandTime = Long.MIN_VALUE;
    private String targetDimension = "";
    private long targetPos = BlockPos.ZERO.asLong();
    private boolean hasTargetPosition = false;
    private boolean deployed = false;
    private boolean returningToOwner = false;
    private boolean pathingStuck = false;
    private String upgrades = "";
    private String lastScanSummary = "No scan data.";
    private String lastWarning = "";
    private String warningCooldowns = "";
    private String lastKnownDimension = "";
    private long lastKnownPos = BlockPos.ZERO.asLong();

    public CompanionDroneData() {
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, CompanionDroneData data) {
        buf.writeVarInt(data.schemaVersion);
        buf.writeUtf(data.ownerUuid);
        buf.writeUtf(data.droneUuid);
        buf.writeUtf(data.customName);
        buf.writeUtf(data.mode.name());
        buf.writeUtf(data.taskLabel);
        buf.writeVarInt(data.batteryPercent);
        buf.writeVarInt(data.health);
        buf.writeVarInt(data.signalQuality);
        buf.writeLong(data.lastScanTime);
        buf.writeLong(data.lastScoutTime);
        buf.writeLong(data.lastWarningTime);
        buf.writeLong(data.lastCommandTime);
        buf.writeUtf(data.targetDimension);
        buf.writeLong(data.targetPos);
        buf.writeBoolean(data.hasTargetPosition);
        buf.writeBoolean(data.deployed);
        buf.writeBoolean(data.returningToOwner);
        buf.writeBoolean(data.pathingStuck);
        buf.writeUtf(data.upgrades);
        buf.writeUtf(data.lastScanSummary);
        buf.writeUtf(data.lastWarning);
        buf.writeUtf(data.warningCooldowns);
        buf.writeUtf(data.lastKnownDimension);
        buf.writeLong(data.lastKnownPos);
    }

    private static CompanionDroneData readSync(RegistryFriendlyByteBuf buf) {
        CompanionDroneData data = new CompanionDroneData();
        data.schemaVersion = buf.readVarInt();
        data.ownerUuid = buf.readUtf();
        data.droneUuid = buf.readUtf();
        data.customName = safeString(buf.readUtf(), DEFAULT_NAME);
        data.mode = EchoDroneMode.parse(buf.readUtf(), EchoDroneMode.FOLLOW);
        data.taskLabel = safeString(buf.readUtf(), data.mode.taskLabel());
        data.batteryPercent = clampPercent(buf.readVarInt());
        data.health = clampPercent(buf.readVarInt());
        data.signalQuality = clampPercent(buf.readVarInt());
        data.lastScanTime = buf.readLong();
        data.lastScoutTime = buf.readLong();
        data.lastWarningTime = buf.readLong();
        data.lastCommandTime = buf.readLong();
        data.targetDimension = buf.readUtf();
        data.targetPos = buf.readLong();
        data.hasTargetPosition = buf.readBoolean();
        data.deployed = buf.readBoolean();
        data.returningToOwner = buf.readBoolean();
        data.pathingStuck = buf.readBoolean();
        data.upgrades = buf.readUtf();
        data.lastScanSummary = safeString(buf.readUtf(), "No scan data.");
        data.lastWarning = buf.readUtf();
        data.warningCooldowns = buf.readUtf();
        data.lastKnownDimension = buf.readUtf();
        data.lastKnownPos = buf.readLong();
        return data;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("schemaVersion", schemaVersion);
        output.putString("ownerUuid", ownerUuid);
        output.putString("droneUuid", droneUuid);
        output.putString("customName", customName);
        output.putString("mode", mode.name());
        output.putString("taskLabel", taskLabel);
        output.putInt("batteryPercent", batteryPercent);
        output.putInt("health", health);
        output.putInt("signalQuality", signalQuality);
        output.putLong("lastScanTime", lastScanTime);
        output.putLong("lastScoutTime", lastScoutTime);
        output.putLong("lastWarningTime", lastWarningTime);
        output.putLong("lastCommandTime", lastCommandTime);
        output.putString("targetDimension", targetDimension);
        output.putLong("targetPos", targetPos);
        output.putBoolean("hasTargetPosition", hasTargetPosition);
        output.putBoolean("deployed", deployed);
        output.putBoolean("returningToOwner", returningToOwner);
        output.putBoolean("pathingStuck", pathingStuck);
        output.putString("upgrades", upgrades);
        output.putString("lastScanSummary", lastScanSummary);
        output.putString("lastWarning", lastWarning);
        output.putString("warningCooldowns", warningCooldowns);
        output.putString("lastKnownDimension", lastKnownDimension);
        output.putLong("lastKnownPos", lastKnownPos);
    }

    @Override
    public void deserialize(ValueInput input) {
        schemaVersion = Math.max(0, input.getIntOr("schemaVersion", 0));
        ownerUuid = input.getStringOr("ownerUuid", "");
        droneUuid = input.getStringOr("droneUuid", "");
        customName = safeString(input.getStringOr("customName", DEFAULT_NAME), DEFAULT_NAME);
        mode = EchoDroneMode.parse(input.getStringOr("mode", "FOLLOW"), EchoDroneMode.FOLLOW);
        taskLabel = safeString(input.getStringOr("taskLabel", mode.taskLabel()), mode.taskLabel());
        batteryPercent = clampPercent(input.getIntOr("batteryPercent", 100));
        health = clampPercent(input.getIntOr("health", 15));
        signalQuality = clampPercent(input.getIntOr("signalQuality", 100));
        lastScanTime = input.getLongOr("lastScanTime", Long.MIN_VALUE);
        lastScoutTime = input.getLongOr("lastScoutTime", Long.MIN_VALUE);
        lastWarningTime = input.getLongOr("lastWarningTime", Long.MIN_VALUE);
        lastCommandTime = input.getLongOr("lastCommandTime", Long.MIN_VALUE);
        targetDimension = input.getStringOr("targetDimension", "");
        targetPos = input.getLongOr("targetPos", BlockPos.ZERO.asLong());
        hasTargetPosition = input.getBooleanOr("hasTargetPosition", false);
        deployed = input.getBooleanOr("deployed", false);
        returningToOwner = input.getBooleanOr("returningToOwner", false);
        pathingStuck = input.getBooleanOr("pathingStuck", false);
        upgrades = input.getStringOr("upgrades", "");
        lastScanSummary = safeString(input.getStringOr("lastScanSummary", "No scan data."), "No scan data.");
        lastWarning = input.getStringOr("lastWarning", "");
        warningCooldowns = input.getStringOr("warningCooldowns", "");
        lastKnownDimension = input.getStringOr("lastKnownDimension", "");
        lastKnownPos = input.getLongOr("lastKnownPos", BlockPos.ZERO.asLong());
        if (schemaVersion <= 0) {
            schemaVersion = SCHEMA_VERSION;
        }
    }

    public EchoDroneOwnerData snapshot(UUID ownerId) {
        return new EchoDroneOwnerData(
                ownerId,
                getDroneUuid(),
                customName,
                mode,
                taskLabel,
                batteryPercent,
                health,
                signalQuality,
                lastScanTime,
                lastWarningTime,
                targetDimensionKey(),
                targetPosition(),
                deployed,
                returningToOwner,
                pathingStuck,
                getUpgrades());
    }

    public net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("schemaVersion", schemaVersion);
        tag.putString("ownerUuid", ownerUuid);
        tag.putString("droneUuid", droneUuid);
        tag.putString("customName", customName);
        tag.putString("mode", mode.name());
        tag.putString("taskLabel", taskLabel);
        tag.putInt("batteryPercent", batteryPercent);
        tag.putInt("health", health);
        tag.putInt("signalQuality", signalQuality);
        tag.putLong("lastScanTime", lastScanTime);
        tag.putLong("lastScoutTime", lastScoutTime);
        tag.putLong("lastWarningTime", lastWarningTime);
        tag.putLong("lastCommandTime", lastCommandTime);
        tag.putString("targetDimension", targetDimension);
        tag.putLong("targetPos", targetPos);
        tag.putBoolean("hasTargetPosition", hasTargetPosition);
        tag.putBoolean("deployed", deployed);
        tag.putBoolean("returningToOwner", returningToOwner);
        tag.putBoolean("pathingStuck", pathingStuck);
        tag.putString("upgrades", upgrades);
        tag.putString("lastScanSummary", lastScanSummary);
        tag.putString("lastWarning", lastWarning);
        tag.putString("warningCooldowns", warningCooldowns);
        tag.putString("lastKnownDimension", lastKnownDimension);
        tag.putLong("lastKnownPos", lastKnownPos);
        return tag;
    }

    public void readTag(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        schemaVersion = Math.max(SCHEMA_VERSION, tag.getIntOr("schemaVersion", SCHEMA_VERSION));
        ownerUuid = tag.getStringOr("ownerUuid", ownerUuid);
        droneUuid = tag.getStringOr("droneUuid", droneUuid);
        customName = safeString(tag.getStringOr("customName", customName), DEFAULT_NAME);
        mode = EchoDroneMode.parse(tag.getStringOr("mode", mode.name()), mode);
        taskLabel = safeString(tag.getStringOr("taskLabel", taskLabel), mode.taskLabel());
        batteryPercent = clampPercent(tag.getIntOr("batteryPercent", batteryPercent));
        health = clampPercent(tag.getIntOr("health", health));
        signalQuality = clampPercent(tag.getIntOr("signalQuality", signalQuality));
        lastScanTime = tag.getLongOr("lastScanTime", lastScanTime);
        lastScoutTime = tag.getLongOr("lastScoutTime", lastScoutTime);
        lastWarningTime = tag.getLongOr("lastWarningTime", lastWarningTime);
        lastCommandTime = tag.getLongOr("lastCommandTime", lastCommandTime);
        targetDimension = tag.getStringOr("targetDimension", targetDimension);
        targetPos = tag.getLongOr("targetPos", targetPos);
        hasTargetPosition = tag.getBooleanOr("hasTargetPosition", hasTargetPosition);
        deployed = tag.getBooleanOr("deployed", deployed);
        returningToOwner = tag.getBooleanOr("returningToOwner", returningToOwner);
        pathingStuck = tag.getBooleanOr("pathingStuck", pathingStuck);
        upgrades = tag.getStringOr("upgrades", upgrades);
        lastScanSummary = safeString(tag.getStringOr("lastScanSummary", lastScanSummary), "No scan data.");
        lastWarning = tag.getStringOr("lastWarning", lastWarning);
        warningCooldowns = tag.getStringOr("warningCooldowns", warningCooldowns);
        lastKnownDimension = tag.getStringOr("lastKnownDimension", lastKnownDimension);
        lastKnownPos = tag.getLongOr("lastKnownPos", lastKnownPos);
    }

    public UUID getOwnerUuid() {
        return parseUuid(ownerUuid);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid == null ? "" : ownerUuid.toString();
    }

    public UUID getDroneUuid() {
        return parseUuid(droneUuid);
    }

    public void setDroneUuid(UUID droneUuid) {
        this.droneUuid = droneUuid == null ? "" : droneUuid.toString();
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = safeString(customName, DEFAULT_NAME);
    }

    public EchoDroneMode getMode() {
        return mode;
    }

    public void setMode(EchoDroneMode mode) {
        this.mode = mode == null ? EchoDroneMode.FOLLOW : mode;
        if (taskLabel == null || taskLabel.isBlank()) {
            taskLabel = this.mode.taskLabel();
        }
    }

    public String getTaskLabel() {
        return taskLabel;
    }

    public void setTaskLabel(String taskLabel) {
        this.taskLabel = safeString(taskLabel, mode.taskLabel());
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = clampPercent(batteryPercent);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = clampPercent(health);
    }

    public int getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(int signalQuality) {
        this.signalQuality = clampPercent(signalQuality);
    }

    public long getLastScanTime() {
        return lastScanTime;
    }

    public void setLastScanTime(long lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    public long getLastScoutTime() {
        return lastScoutTime;
    }

    public void setLastScoutTime(long lastScoutTime) {
        this.lastScoutTime = lastScoutTime;
    }

    public long getLastWarningTime() {
        return lastWarningTime;
    }

    public void setLastWarningTime(long lastWarningTime) {
        this.lastWarningTime = lastWarningTime;
    }

    public long getLastCommandTime() {
        return lastCommandTime;
    }

    public void setLastCommandTime(long lastCommandTime) {
        this.lastCommandTime = lastCommandTime;
    }

    public boolean hasTargetPosition() {
        return hasTargetPosition;
    }

    public BlockPos targetPosition() {
        return hasTargetPosition ? BlockPos.of(targetPos) : null;
    }

    public ResourceKey<Level> targetDimensionKey() {
        Identifier id = Identifier.tryParse(targetDimension);
        return id == null ? Level.OVERWORLD : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
    }

    public void setTarget(ResourceKey<Level> dimension, BlockPos pos) {
        hasTargetPosition = pos != null;
        targetPos = pos == null ? BlockPos.ZERO.asLong() : pos.asLong();
        targetDimension = dimension == null ? "" : dimension.identifier().toString();
    }

    public void clearTarget() {
        hasTargetPosition = false;
        targetPos = BlockPos.ZERO.asLong();
        targetDimension = "";
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    public boolean isReturningToOwner() {
        return returningToOwner;
    }

    public void setReturningToOwner(boolean returningToOwner) {
        this.returningToOwner = returningToOwner;
    }

    public boolean isPathingStuck() {
        return pathingStuck;
    }

    public void setPathingStuck(boolean pathingStuck) {
        this.pathingStuck = pathingStuck;
    }

    public String getLastScanSummary() {
        return lastScanSummary;
    }

    public void setLastScanSummary(String lastScanSummary) {
        this.lastScanSummary = safeString(lastScanSummary, "No scan data.");
    }

    public String getLastWarning() {
        return lastWarning;
    }

    public void setLastWarning(String lastWarning) {
        this.lastWarning = lastWarning == null ? "" : lastWarning.strip();
    }

    public long warningTime(String category) {
        String key = commandKey(category);
        if (key.isBlank() || warningCooldowns == null || warningCooldowns.isBlank()) {
            return Long.MIN_VALUE;
        }
        for (String entry : warningCooldowns.split(",")) {
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            if (!key.equals(entry.substring(0, separator))) {
                continue;
            }
            try {
                return Long.parseLong(entry.substring(separator + 1));
            } catch (NumberFormatException ignored) {
                return Long.MIN_VALUE;
            }
        }
        return Long.MIN_VALUE;
    }

    public void setWarningTime(String category, long gameTime) {
        String key = commandKey(category);
        if (key.isBlank()) {
            return;
        }
        java.util.LinkedHashMap<String, Long> times = new java.util.LinkedHashMap<>();
        if (warningCooldowns != null && !warningCooldowns.isBlank()) {
            for (String entry : warningCooldowns.split(",")) {
                int separator = entry.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                try {
                    times.put(entry.substring(0, separator), Long.parseLong(entry.substring(separator + 1)));
                } catch (NumberFormatException ignored) {
                    // Skip malformed legacy entries.
                }
            }
        }
        times.put(key, gameTime);
        warningCooldowns = times.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public BlockPos getLastKnownPos() {
        return BlockPos.of(lastKnownPos);
    }

    public String getLastKnownDimension() {
        return lastKnownDimension;
    }

    public void setLastKnown(ResourceKey<Level> dimension, BlockPos pos) {
        lastKnownDimension = dimension == null ? "" : dimension.identifier().toString();
        lastKnownPos = pos == null ? BlockPos.ZERO.asLong() : pos.asLong();
    }

    public boolean hasUpgrade(EchoDroneUpgrade upgrade) {
        return upgrade != null && getUpgrades().contains(upgrade);
    }

    public Set<EchoDroneUpgrade> getUpgrades() {
        EnumSet<EchoDroneUpgrade> set = EnumSet.noneOf(EchoDroneUpgrade.class);
        if (upgrades == null || upgrades.isBlank()) {
            return set;
        }
        for (String part : upgrades.split(",")) {
            EchoDroneUpgrade upgrade = EchoDroneUpgrade.parse(part);
            if (upgrade != null) {
                set.add(upgrade);
            }
        }
        return set;
    }

    public void addUpgrade(EchoDroneUpgrade upgrade) {
        if (upgrade == null) {
            return;
        }
        EnumSet<EchoDroneUpgrade> set = EnumSet.copyOf(getUpgrades().isEmpty()
                ? EnumSet.noneOf(EchoDroneUpgrade.class) : getUpgrades());
        set.add(upgrade);
        setUpgrades(set);
    }

    public void setUpgrades(Set<EchoDroneUpgrade> upgrades) {
        if (upgrades == null || upgrades.isEmpty()) {
            this.upgrades = "";
            return;
        }
        this.upgrades = upgrades.stream()
                .map(EchoDroneUpgrade::name)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public String upgradesDisplay() {
        Set<EchoDroneUpgrade> set = getUpgrades();
        if (set.isEmpty()) {
            return "None";
        }
        return set.stream()
                .map(EchoDroneUpgrade::displayName)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
    }

    public String signalLabel() {
        if (signalQuality >= 80) {
            return "Stable";
        }
        if (signalQuality >= 45) {
            return "Weak";
        }
        if (signalQuality > 0) {
            return "Critical";
        }
        return "Offline";
    }

    public String statusLine() {
        return "Drone: " + mode.displayName() + " | Battery " + batteryPercent + "% | Signal "
                + signalLabel() + " | Task: " + taskLabel;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String safeString(String value, String fallback) {
        String safe = value == null ? "" : value.strip();
        return safe.isBlank() ? fallback : safe;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static String commandKey(String command) {
        return (command == null ? "" : command.trim().toLowerCase(Locale.ROOT)).replace(' ', '_');
    }
}

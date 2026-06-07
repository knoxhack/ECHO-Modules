package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.gameplay.DifficultyProfile;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Player body temperature tracking for cold survival mechanic.
 * Temperature ranges from 0 (freezing) to 100 (normal).
 * Cold biomes reduce temperature, warmth sources increase it.
 */
public class ColdData implements EchoValueIOSerializable {
    
    // Temperature range: 0-100
    public static final int MAX_TEMPERATURE = 100;
    public static final int MIN_TEMPERATURE = 0;
    public static final int FREEZING_THRESHOLD = 20;  // Below this = hypothermia damage
    public static final int COLD_THRESHOLD = 40;    // Below this = slowed movement
    public static final int NORMAL_THRESHOLD = 60;    // Above this = normal
    public static final int WARM_THRESHOLD = 80;      // Above this = comfortable
    
    private int temperature = 100;  // Start at normal
    private int freezeTickCounter = 0;  // For damage timing
    
    public ColdData() {
        // Default constructor
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ColdData> STREAM_CODEC = StreamCodec.of(
            ColdData::writeSync,
            ColdData::readSync
    );

    private static void writeSync(RegistryFriendlyByteBuf buf, ColdData data) {
        buf.writeVarInt(data.temperature);
        buf.writeVarInt(data.freezeTickCounter);
    }

    private static ColdData readSync(RegistryFriendlyByteBuf buf) {
        ColdData data = new ColdData();
        data.temperature = buf.readVarInt();
        data.freezeTickCounter = buf.readVarInt();
        data.setTemperature(data.temperature);
        return data;
    }
    
    public int getTemperature() {
        return temperature;
    }
    
    public void setTemperature(int temp) {
        this.temperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temp));
    }
    
    public void addTemperature(int amount) {
        setTemperature(temperature + amount);
    }
    
    public void reduceTemperature(int amount) {
        setTemperature(temperature - amount);
    }
    
    /**
     * Check current temperature status
     */
    public TemperatureStatus getStatus() {
        if (temperature <= FREEZING_THRESHOLD) return TemperatureStatus.FREEZING;
        if (temperature <= COLD_THRESHOLD) return TemperatureStatus.COLD;
        if (temperature <= NORMAL_THRESHOLD) return TemperatureStatus.COOL;
        if (temperature <= WARM_THRESHOLD) return TemperatureStatus.NORMAL;
        return TemperatureStatus.WARM;
    }
    
    /**
     * Called every tick to update temperature based on environment
     */
    public boolean update(ServerPlayer player, HazardZoneManager.HazardSnapshot hazard, boolean graceActive) {
        int beforeTemp = temperature;
        int beforeFreeze = freezeTickCounter;
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        int baseChange = 0;

        if (graceActive) {
            baseChange += temperature < NORMAL_THRESHOLD ? 2 : 0;
            freezeTickCounter = 0;
        } else if (hazard != null && hazard.cryoCold()) {
            float baseColdLoss = 2.0f * hazard.coldIntensity()
                    * Math.max(0.0f, Config.CRYO_COLD_LOSS_MULTIPLIER.get().floatValue());
            int coldLoss = Math.max(1, Math.round(DifficultyProfile.scaleSurvivalHazard(level, baseColdLoss)));
            baseChange -= coldLoss;
        } else if (level.getBiome(pos).value().getBaseTemperature() < 0.15f) {
            baseChange -= 1;
        } else {
            baseChange += 1;
        }
        
        if (isNearCampfire(level, pos)) {
            baseChange += 5;
        }
        
        if (HazardZoneManager.hasThermalProtection(player)) {
            baseChange += 3;
        }
        
        if (isIndoors(level, pos)) {
            baseChange += 1;
        }

        if (hazard != null && hazard.safeZone()) {
            baseChange += 2;
        }

        addTemperature(baseChange);

        if (!graceActive) {
            applyTemperatureEffects(player);
        }

        return beforeTemp != temperature || beforeFreeze != freezeTickCounter;
    }

    /**
     * Legacy path retained for older callers.
     */
    public void update(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            update(serverPlayer, HazardZoneManager.scan(serverPlayer), false);
        }
    }

    private void applyTemperatureEffects(ServerPlayer player) {
        TemperatureStatus status = getStatus();
        
        switch (status) {
            case FREEZING:
                // Hypothermia damage
                freezeTickCounter++;
                if (freezeTickCounter >= 60) {  // Damage every 3 seconds
                    sendColdDamageHint(player);
                    float damage = DifficultyProfile.scaleSurvivalHazard(player.level(), 2.0f);
                    player.hurtServer((net.minecraft.server.level.ServerLevel) player.level(), player.damageSources().freeze(), damage);
                    freezeTickCounter = 0;
                }
                // Slow movement
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.8, 1.0, 0.8));
                break;
                
            case COLD:
                // Slowed movement but no damage
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.9, 1.0, 0.9));
                freezeTickCounter = 0;
                break;
                
            case COOL:
            case NORMAL:
            case WARM:
                freezeTickCounter = 0;
                break;
        }
    }
    
    private boolean isNearCampfire(Level level, BlockPos pos) {
        // Check 5-block radius for campfire
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (isLitCampfire(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLitCampfire(BlockState state) {
        return state.hasProperty(CampfireBlock.LIT)
                && state.getValue(CampfireBlock.LIT)
                && (state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)
                || state.is(ModBlocks.ASH_CAMPFIRE.get()));
    }
    
    private boolean isIndoors(Level level, BlockPos pos) {
        // Simple check: has roof above
        for (int y = 1; y <= 10; y++) {
            if (!level.getBlockState(pos.above(y)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private void sendColdDamageHint(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        QuestData quest = QuestData.get(serverPlayer);
        if (quest.hasVisitedLocation("special", "death_hint:cold")) {
            return;
        }
        quest.visitLocation("special", "death_hint:cold");
        QuestData.saveAndSync(serverPlayer, quest);
        serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.death_hint.cold"), true);
    }
    
    @Override
    public void serialize(ValueOutput output) {
        output.putInt("temperature", temperature);
        output.putInt("freezeCounter", freezeTickCounter);
    }
    
    @Override
    public void deserialize(ValueInput input) {
        temperature = input.getIntOr("temperature", 100);
        freezeTickCounter = input.getIntOr("freezeCounter", 0);
        
        // Clamp values
        temperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature));
    }
    
    /**
     * Get or create cold data for a player
     */
    public static ColdData get(Player player) {
        return player.getData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.COLD_DATA.get());
    }
    
    public enum TemperatureStatus {
        FREEZING(0xFF0044AA, "Freezing!", "Hypothermia damage"),
        COLD(0xFF44AAFF, "Cold", "Slowed movement"),
        COOL(0xFF88CCFF, "Cool", "Slightly uncomfortable"),
        NORMAL(0xFF42D67E, "Normal", "Comfortable"),
        WARM(0xFFFFA94D, "Warm", "Comfortable");
        
        private final int color;
        private final String displayName;
        private final String effect;
        
        TemperatureStatus(int color, String displayName, String effect) {
            this.color = color;
            this.displayName = displayName;
            this.effect = effect;
        }
        
        public int getColor() { return color; }
        public String getDisplayName() { return displayName; }
        public String getEffect() { return effect; }
    }
}

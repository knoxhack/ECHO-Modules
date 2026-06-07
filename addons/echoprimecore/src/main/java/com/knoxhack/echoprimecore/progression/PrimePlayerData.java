package com.knoxhack.echoprimecore.progression;

import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echoprimecore.registry.ModAttachments;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PrimePlayerData implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, PrimePlayerData> STREAM_CODEC = StreamCodec.of(
            PrimePlayerData::writeSync,
            PrimePlayerData::readSync);

    private final Set<String> flags = new HashSet<>();
    private boolean firstJoinInitialized;
    private String stage = "Prime Survival: Begin";
    private String objective = "Survive, explore, and bring ECHO systems online.";
    private boolean starterRelayPlaced;
    private int relayX;
    private int relayY;
    private int relayZ;

    public static PrimePlayerData get(Player player) {
        return player == null ? new PrimePlayerData() : player.getData(ModAttachments.PRIME_PLAYER_DATA.get());
    }

    public static void saveAndSync(ServerPlayer player, PrimePlayerData data) {
        if (player == null || data == null) {
            return;
        }
        player.setData(ModAttachments.PRIME_PLAYER_DATA.get(), data);
        try {
            player.syncData(ModAttachments.PRIME_PLAYER_DATA.get());
        } catch (RuntimeException ignored) {
            // Server-side progression still persists even when a client sync is unavailable.
        }
    }

    public boolean firstJoinInitialized() {
        return firstJoinInitialized;
    }

    public void setFirstJoinInitialized(boolean value) {
        firstJoinInitialized = value;
    }

    public boolean hasFlag(Identifier flag) {
        return flag != null && flags.contains(flag.toString());
    }

    public boolean unlockFlag(Identifier flag) {
        return flag != null && flags.add(flag.toString());
    }

    public void resetFlags() {
        flags.clear();
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    public String stage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage == null || stage.isBlank() ? "Prime Survival: Begin" : stage.strip();
    }

    public String objective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective == null || objective.isBlank()
                ? "Survive, explore, and bring ECHO systems online."
                : objective.strip();
    }

    public boolean starterRelayPlaced() {
        return starterRelayPlaced;
    }

    public BlockPos relayPos() {
        return new BlockPos(relayX, relayY, relayZ);
    }

    public void setRelayPos(BlockPos pos) {
        if (pos == null) {
            starterRelayPlaced = false;
            relayX = relayY = relayZ = 0;
            return;
        }
        starterRelayPlaced = true;
        relayX = pos.getX();
        relayY = pos.getY();
        relayZ = pos.getZ();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("firstJoinInitialized", firstJoinInitialized);
        output.putString("stage", stage);
        output.putString("objective", objective);
        output.putInt("flagCount", flags.size());
        int index = 0;
        for (String flag : flags) {
            output.putString("flag_" + index++, flag);
        }
        output.putBoolean("starterRelayPlaced", starterRelayPlaced);
        output.putInt("relayX", relayX);
        output.putInt("relayY", relayY);
        output.putInt("relayZ", relayZ);
    }

    @Override
    public void deserialize(ValueInput input) {
        firstJoinInitialized = input.getBooleanOr("firstJoinInitialized", false);
        stage = input.getStringOr("stage", "Prime Survival: Begin");
        objective = input.getStringOr("objective", "Survive, explore, and bring ECHO systems online.");
        flags.clear();
        int count = input.getIntOr("flagCount", 0);
        for (int i = 0; i < count; i++) {
            String flag = input.getStringOr("flag_" + i, "");
            if (!flag.isBlank()) {
                flags.add(flag);
            }
        }
        starterRelayPlaced = input.getBooleanOr("starterRelayPlaced", false);
        relayX = input.getIntOr("relayX", 0);
        relayY = input.getIntOr("relayY", 0);
        relayZ = input.getIntOr("relayZ", 0);
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, PrimePlayerData data) {
        buf.writeBoolean(data.firstJoinInitialized);
        buf.writeUtf(data.stage);
        buf.writeUtf(data.objective);
        buf.writeVarInt(data.flags.size());
        for (String flag : data.flags) {
            buf.writeUtf(flag);
        }
        buf.writeBoolean(data.starterRelayPlaced);
        buf.writeBlockPos(data.relayPos());
    }

    private static PrimePlayerData readSync(RegistryFriendlyByteBuf buf) {
        PrimePlayerData data = new PrimePlayerData();
        data.firstJoinInitialized = buf.readBoolean();
        data.stage = buf.readUtf();
        data.objective = buf.readUtf();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String flag = buf.readUtf();
            if (!flag.isBlank()) {
                data.flags.add(flag);
            }
        }
        boolean relayPlaced = buf.readBoolean();
        BlockPos relayPos = buf.readBlockPos();
        data.setRelayPos(relayPlaced ? relayPos : null);
        if (!relayPlaced) {
            data.setRelayPos(null);
        }
        return data;
    }
}

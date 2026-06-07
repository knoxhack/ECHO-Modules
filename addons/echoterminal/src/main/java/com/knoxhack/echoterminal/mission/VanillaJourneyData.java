package com.knoxhack.echoterminal.mission;

import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.registry.ModAttachments;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VanillaJourneyData implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, VanillaJourneyData> STREAM_CODEC = StreamCodec.of(
            VanillaJourneyData::writeSync,
            VanillaJourneyData::readSync);

    private final Set<String> claimedRewardIds = new HashSet<>();
    private final Set<String> completedAdvancementIds = new HashSet<>();

    public boolean isClaimed(Identifier advancementId) {
        return claimedRewardIds.contains(advancementId.toString());
    }

    public boolean isCompleted(Identifier advancementId) {
        return completedAdvancementIds.contains(advancementId.toString());
    }

    public void markClaimed(Identifier advancementId) {
        claimedRewardIds.add(advancementId.toString());
    }

    public boolean setCompleted(Collection<Identifier> advancementIds) {
        Set<String> nextCompleted = new HashSet<>();
        for (Identifier id : advancementIds) {
            nextCompleted.add(id.toString());
        }
        if (completedAdvancementIds.equals(nextCompleted)) {
            return false;
        }
        completedAdvancementIds.clear();
        completedAdvancementIds.addAll(nextCompleted);
        return true;
    }

    public Set<String> claimedRewardIds() {
        return Set.copyOf(claimedRewardIds);
    }

    public Set<String> completedAdvancementIds() {
        return Set.copyOf(completedAdvancementIds);
    }

    public static VanillaJourneyData get(net.minecraft.world.entity.player.Player player) {
        if (nativeLoaderClientActive()) {
            return new VanillaJourneyData();
        }
        return player.getData(ModAttachments.VANILLA_JOURNEY_DATA.get());
    }

    public static void saveAndSync(ServerPlayer player, VanillaJourneyData data) {
        player.setData(ModAttachments.VANILLA_JOURNEY_DATA.get(), data);
        try {
            player.syncData(ModAttachments.VANILLA_JOURNEY_DATA.get());
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Vanilla journey data saved without client sync.", exception);
        }
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, VanillaJourneyData data) {
        writeStringSet(buf, data.claimedRewardIds);
        writeStringSet(buf, data.completedAdvancementIds);
    }

    private static VanillaJourneyData readSync(RegistryFriendlyByteBuf buf) {
        VanillaJourneyData data = new VanillaJourneyData();
        readStringSet(buf, data.claimedRewardIds);
        readStringSet(buf, data.completedAdvancementIds);
        return data;
    }

    private static void writeStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }

    private static boolean nativeLoaderClientActive() {
        return Boolean.getBoolean("echo.native.loader")
                && "windowed-native-client".equals(System.getProperty("echo.native.runtime.mode", ""));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("claimedCount", claimedRewardIds.size());
        int index = 0;
        for (String id : claimedRewardIds) {
            output.putString("claimed_" + index++, id);
        }
        output.putInt("completedCount", completedAdvancementIds.size());
        index = 0;
        for (String id : completedAdvancementIds) {
            output.putString("completed_" + index++, id);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        claimedRewardIds.clear();
        int claimedCount = input.getIntOr("claimedCount", 0);
        for (int i = 0; i < claimedCount; i++) {
            String id = input.getStringOr("claimed_" + i, "");
            if (!id.isBlank()) {
                claimedRewardIds.add(id);
            }
        }
        completedAdvancementIds.clear();
        int completedCount = input.getIntOr("completedCount", 0);
        for (int i = 0; i < completedCount; i++) {
            String id = input.getStringOr("completed_" + i, "");
            if (!id.isBlank()) {
                completedAdvancementIds.add(id);
            }
        }
    }
}

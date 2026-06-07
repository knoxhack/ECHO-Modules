package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.RobotPoseSnapshot;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RobotAnimationPacket(
        BlockPos controllerPos,
        BlockPos robotPos,
        String animationId,
        BlockPos targetPos,
        int durationTicks,
        Identifier taskId,
        int packetVersion,
        Identifier animationProfile,
        RobotPoseSnapshot pose,
        int flags) implements CustomPacketPayload {
    public static final Identifier ID = EchoMultiblockCore.id("robot_animation");
    public static final int CURRENT_VERSION = 2;
    public static final Type<RobotAnimationPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RobotAnimationPacket> CODEC =
            StreamCodec.of(RobotAnimationPacket::write, RobotAnimationPacket::read);

    public RobotAnimationPacket(
            BlockPos controllerPos,
            BlockPos robotPos,
            String animationId,
            BlockPos targetPos,
            int durationTicks,
            Identifier taskId) {
        this(controllerPos, robotPos, animationId, targetPos, durationTicks, taskId,
                CURRENT_VERSION, taskId == null ? EchoMultiblockCore.id("default") : taskId,
                RobotPoseSnapshot.idle(), 0);
    }

    public RobotAnimationPacket {
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        animationId = animationId == null || animationId.isBlank() ? "move_to_target" : animationId.strip();
        targetPos = targetPos == null ? robotPos : targetPos.immutable();
        durationTicks = Math.max(1, durationTicks);
        taskId = taskId == null ? EchoMultiblockCore.id("unknown_task") : taskId;
        packetVersion = Math.max(1, packetVersion);
        animationProfile = animationProfile == null
                ? (animationId.contains(":") ? Identifier.tryParse(animationId) : EchoMultiblockCore.id(animationId))
                : animationProfile;
        if (animationProfile == null) {
            animationProfile = EchoMultiblockCore.id("default");
        }
        pose = pose == null ? RobotPoseSnapshot.idle() : pose;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, RobotAnimationPacket packet) {
        buffer.writeBlockPos(packet.controllerPos());
        buffer.writeBlockPos(packet.robotPos());
        buffer.writeUtf(packet.animationId(), 64);
        buffer.writeBlockPos(packet.targetPos());
        buffer.writeVarInt(packet.durationTicks());
        EchoPayloadCodecs.writeIdentifier(buffer, packet.taskId());
        buffer.writeVarInt(packet.packetVersion());
        EchoPayloadCodecs.writeIdentifier(buffer, packet.animationProfile());
        buffer.writeFloat(packet.pose().baseYaw());
        buffer.writeFloat(packet.pose().shoulderPitch());
        buffer.writeFloat(packet.pose().elbowPitch());
        buffer.writeFloat(packet.pose().wristPitch());
        buffer.writeFloat(packet.pose().toolOpenness());
        buffer.writeVarInt(packet.flags());
    }

    private static RobotAnimationPacket read(RegistryFriendlyByteBuf buffer) {
        BlockPos controller = buffer.readBlockPos();
        BlockPos robot = buffer.readBlockPos();
        String animation = buffer.readUtf(64);
        BlockPos target = buffer.readBlockPos();
        int duration = buffer.readVarInt();
        Identifier task = EchoPayloadCodecs.readIdentifier(buffer);
        if (!buffer.isReadable()) {
            return new RobotAnimationPacket(controller, robot, animation, target, duration, task);
        }
        int version = buffer.readVarInt();
        Identifier profile = EchoPayloadCodecs.readIdentifier(buffer);
        return new RobotAnimationPacket(
                controller,
                robot,
                animation,
                target,
                duration,
                task,
                version,
                profile,
                new RobotPoseSnapshot(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                        buffer.readFloat(), buffer.readFloat()),
                buffer.readVarInt());
    }
}

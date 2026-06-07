package com.knoxhack.echomultiblockcore.api;

public record RobotPoseSnapshot(float baseYaw, float shoulderPitch, float elbowPitch, float wristPitch, float toolOpenness) {
    public static RobotPoseSnapshot idle() {
        return new RobotPoseSnapshot(0.0F, -18.0F, 36.0F, -12.0F, 0.0F);
    }
}

package com.knoxhack.echoashfallprotocol.boss;

public record BossHudProfile(
        String bossId,
        String title,
        String subtitle,
        int accentColor,
        String compassLabel,
        String phaseWarningLabel,
        String counterplayLabel,
        BossCategory category,
        float phaseTwoThreshold,
        float phaseThreeThreshold
) {
    public BossHudProfile {
        accentColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
        phaseWarningLabel = phaseWarningLabel == null || phaseWarningLabel.isBlank()
                ? "FINAL PHASE"
                : phaseWarningLabel;
        counterplayLabel = counterplayLabel == null || counterplayLabel.isBlank()
                ? "Hold position"
                : counterplayLabel;
        phaseTwoThreshold = clampThreshold(phaseTwoThreshold);
        phaseThreeThreshold = clampThreshold(phaseThreeThreshold);
    }

    public int phaseForHealth(float healthPercent) {
        float pct = Math.max(0.0F, Math.min(1.0F, healthPercent));
        if (pct <= phaseThreeThreshold) {
            return 3;
        }
        if (pct <= phaseTwoThreshold) {
            return 2;
        }
        return 1;
    }

    private static float clampThreshold(float threshold) {
        return Math.max(0.0F, Math.min(1.0F, threshold));
    }

    public enum BossCategory {
        GUARDIAN,
        WARDEN,
        ORBITAL
    }
}

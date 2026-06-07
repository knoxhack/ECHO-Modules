package com.knoxhack.echo.socialcore;

public enum EchoReputationBand {
    HATED("hated", -1_000, -750),
    HOSTILE("hostile", -749, -250),
    UNEASY("uneasy", -249, -1),
    NEUTRAL("neutral", 0, 99),
    FRIENDLY("friendly", 100, 499),
    ALLIED("allied", 500, 1_000),
    UNKNOWN("unknown", Integer.MIN_VALUE, Integer.MAX_VALUE);

    private final String serializedName;
    private final int minScore;
    private final int maxScore;

    EchoReputationBand(String serializedName, int minScore, int maxScore) {
        this.serializedName = serializedName;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String serializedName() {
        return serializedName;
    }

    public int minScore() {
        return minScore;
    }

    public int maxScore() {
        return maxScore;
    }

    public boolean hostile() {
        return this == HATED || this == HOSTILE;
    }

    public static EchoReputationBand fromScore(int score) {
        int bounded = SocialContractGuards.boundedReputation(score);
        for (EchoReputationBand band : values()) {
            if (band != UNKNOWN && bounded >= band.minScore && bounded <= band.maxScore) {
                return band;
            }
        }
        return UNKNOWN;
    }
}

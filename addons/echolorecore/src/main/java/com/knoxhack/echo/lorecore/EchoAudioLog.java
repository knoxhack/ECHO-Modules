package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoAudioLog(
        String audioLogId,
        EchoLoreFragmentId fragmentId,
        EchoContentReference audioReference,
        EchoContentReference transcriptReference,
        String speakerTranslationKey,
        int durationTicks,
        Map<String, String> attributes
) {
    public EchoAudioLog {
        audioLogId = LoreContractGuards.id(audioLogId, "audio log id");
        speakerTranslationKey = LoreContractGuards.optionalText(speakerTranslationKey);
        durationTicks = LoreContractGuards.nonNegative(durationTicks, "audio log duration ticks");
        attributes = LoreContractGuards.immutableMap(attributes);
    }
}

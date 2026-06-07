package com.knoxhack.echoashfallprotocol.loot;

import com.knoxhack.echo.adaptercore.EchoBackendWikiManualLootModifier;
import com.knoxhack.echoashfallprotocol.integration.AshfallWikiIntegration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class WikiManualLootModifier extends EchoBackendWikiManualLootModifier {
    public static final MapCodec<WikiManualLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(instance.group(
                    Identifier.CODEC.fieldOf("guideBookId").forGetter(WikiManualLootModifier::guideBookId),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(WikiManualLootModifier::chance)))
            .apply(instance, WikiManualLootModifier::new));

    public WikiManualLootModifier(Object conditions, int priority, Identifier guideBookId, float chance) {
        super(conditions, priority, guideBookId, chance);
    }

    public static void configureBackend(Supplier<Object> codecSupplier) {
        configure(AshfallWikiIntegration.ASHFALL_MANUAL_ID, AshfallWikiIntegration::guideBookStack, codecSupplier);
    }
}

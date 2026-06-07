package com.knoxhack.echoscreencore.client.style;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoStyleSheet(Identifier id, List<EchoStyleRule> rules) {
}

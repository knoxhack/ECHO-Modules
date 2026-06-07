package com.knoxhack.echoscreencore.client.parser;

import net.minecraft.resources.Identifier;

public record EchoPageDefinition(Identifier pageId, Identifier resourceId, EchoNode root) {
}

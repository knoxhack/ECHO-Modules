package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeAttachmentService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

public final class GalacticCoreAttachments {
    private GalacticCoreAttachments() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeAttachmentService attachments) {
        for (GalacticCoreContentDefinitions.Registration attachment : GalacticCoreContentDefinitions.ATTACHMENTS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    attachments.attach(GalacticCoreRegistrarSupport.mutation("attachments", "attach", attachment))
            );
        }
    }
}

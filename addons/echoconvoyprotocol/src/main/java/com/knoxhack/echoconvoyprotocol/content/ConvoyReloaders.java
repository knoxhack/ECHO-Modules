package com.knoxhack.echoconvoyprotocol.content;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;

public final class ConvoyReloaders {
   private ConvoyReloaders() {
   }

   public static void addServerReloadListeners(Object event) {
      addListener(event, Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "routes"), new ConvoyJsonReloadListener());
      addListener(event, Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "incidents"), new ConvoyIncidentJsonReloadListener());
   }

   private static void addListener(Object event, Identifier id, Object listener) {
      if (event == null) {
         return;
      }
      try {
         for (Method method : event.getClass().getMethods()) {
            if ("addListener".equals(method.getName()) && method.getParameterCount() == 2) {
               method.invoke(event, id, listener);
               return;
            }
         }
      } catch (ReflectiveOperationException | RuntimeException exception) {
         EchoConvoyProtocol.LOGGER.warn("Convoy reload listener {} could not be registered.", id, exception);
      }
   }
}

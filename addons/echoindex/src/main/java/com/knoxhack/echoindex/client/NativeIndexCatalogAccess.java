package com.knoxhack.echoindex.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echoindex.EchoIndex;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NativeIndexCatalogAccess {
    private static final MarkerCache EMPTY_CACHE = new MarkerCache(Path.of(""), -1L, -1L, List.of());
    private static volatile MarkerCache cache = EMPTY_CACHE;

    private NativeIndexCatalogAccess() {
    }

    public static List<ItemStack> catalogStacks() {
        Path markerPath = markerPath();
        if (markerPath == null || !Files.isRegularFile(markerPath)) {
            return List.of();
        }
        try {
            long modified = Files.getLastModifiedTime(markerPath).toMillis();
            long size = Files.size(markerPath);
            MarkerCache cached = cache;
            if (cached.matches(markerPath, modified, size)) {
                return copy(cached.stacks());
            }
            List<ItemStack> stacks = readStacks(markerPath);
            cache = new MarkerCache(markerPath.toAbsolutePath().normalize(), modified, size, stacks);
            return copy(stacks);
        } catch (IOException | RuntimeException | LinkageError exception) {
            EchoIndex.LOGGER.debug("ECHO: Index could not read Native Loader catalog marker.", exception);
            return List.of();
        }
    }

    private static List<ItemStack> readStacks(Path markerPath) throws IOException {
        JsonObject registryBridge = registryBridge(markerPath);
        if (registryBridge == null) {
            return List.of();
        }
        LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        addStringArray(itemIds, registryBridge.get("visibleItems"));
        addStringArray(itemIds, registryBridge.get("visibleNativeCreativeTabItems"));
        addCreativeTabItems(itemIds, registryBridge.get("registeredCreativeTabs"));
        addStringArray(itemIds, registryBridge.get("registeredContentItems"));
        addStringArray(itemIds, registryBridge.get("registeredBlockItems"));
        addModuleRepresentativeItems(itemIds, registryBridge.get("registeredModuleItems"));

        ArrayList<ItemStack> stacks = new ArrayList<>();
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String rawId : itemIds) {
            Identifier id = Identifier.tryParse(rawId);
            if (id == null || !resolved.add(id.toString())) {
                continue;
            }
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isPresent() && item.get() != Items.AIR) {
                stacks.add(new ItemStack(item.get()));
            }
        }
        return List.copyOf(stacks);
    }

    private static JsonObject registryBridge(Path markerPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(markerPath)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject runtimeBridge = object(root.getAsJsonObject().get("runtimeBridge"));
            return runtimeBridge == null ? null : object(runtimeBridge.get("registryBridge"));
        }
    }

    private static void addCreativeTabItems(LinkedHashSet<String> itemIds, JsonElement element) {
        JsonArray tabs = array(element);
        if (tabs == null) {
            return;
        }
        for (JsonElement tabElement : tabs) {
            JsonObject tab = object(tabElement);
            if (tab == null || !bool(tab.get("registered"))) {
                continue;
            }
            addStringArray(itemIds, tab.get("items"));
            addStringArray(itemIds, tab.get("creativeTabItemsFromNativeRegistry"));
            addStringArray(itemIds, tab.get("creativeTabOutputProofItemIds"));
        }
    }

    private static void addModuleRepresentativeItems(LinkedHashSet<String> itemIds, JsonElement element) {
        JsonArray items = array(element);
        if (items == null) {
            return;
        }
        for (JsonElement itemElement : items) {
            JsonObject item = object(itemElement);
            if (item != null && item.has("itemId")) {
                addString(itemIds, item.get("itemId"));
            }
        }
    }

    private static void addStringArray(LinkedHashSet<String> itemIds, JsonElement element) {
        JsonArray array = array(element);
        if (array == null) {
            return;
        }
        for (JsonElement value : array) {
            addString(itemIds, value);
        }
    }

    private static void addString(LinkedHashSet<String> itemIds, JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return;
        }
        String text = value.getAsString().trim();
        if (!text.isBlank()) {
            itemIds.add(text);
        }
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static boolean bool(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        return stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private static Path markerPath() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.gameDirectory != null) {
                return minecraft.gameDirectory.toPath()
                        .resolve(".echo")
                        .resolve("native-loader")
                        .resolve("module-activation.json");
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        String userDir = System.getProperty("user.dir", "").trim();
        return userDir.isBlank()
                ? null
                : Path.of(userDir).resolve(".echo").resolve("native-loader").resolve("module-activation.json");
    }

    private record MarkerCache(Path path, long modified, long size, List<ItemStack> stacks) {
        private MarkerCache {
            path = path == null ? Path.of("") : path.toAbsolutePath().normalize();
            stacks = NativeIndexCatalogAccess.copy(stacks);
        }

        private boolean matches(Path candidate, long candidateModified, long candidateSize) {
            return candidate != null
                    && path.equals(candidate.toAbsolutePath().normalize())
                    && modified == candidateModified
                    && size == candidateSize;
        }
    }
}

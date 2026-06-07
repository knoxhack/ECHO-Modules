package com.knoxhack.echomultiblockcore.integration.terminal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockTerminalBridge {
    public static final Identifier TAB_ID = EchoMultiblockCore.id("terminal");
    public static final Identifier START_TASK = EchoMultiblockCore.id("start_task");
    public static final Identifier CLEAR_QUEUE = EchoMultiblockCore.id("clear_queue");
    public static final Identifier RETRY_BLOCKED = EchoMultiblockCore.id("retry_blocked");
    public static final Identifier PAUSE_QUEUE = EchoMultiblockCore.id("pause_queue");
    public static final Identifier RESUME_QUEUE = EchoMultiblockCore.id("resume_queue");
    private static final int ACCENT = 0xFF66E8FF;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private MultiblockTerminalBridge() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        Optional<TerminalReflection> terminal = TerminalReflection.load();
        if (terminal.isEmpty()) {
            EchoMultiblockCore.LOGGER.debug("ECHO MultiblockCore terminal bridge skipped; Terminal API is not on the classpath.");
            return;
        }
        try {
            TerminalReflection types = terminal.get();
            types.registerRecipeProvider(recipeProvider(types));
            types.registerAddonInfoProvider(addonInfoProvider(types));
            registerAction(types, START_TASK, MultiblockTerminalBridge::startTask);
            registerAction(types, CLEAR_QUEUE, (player, payload) -> withController(player, payload,
                    controller -> controller.clearQueue(player)));
            registerAction(types, RETRY_BLOCKED, (player, payload) -> withController(player, payload,
                    controller -> controller.retryBlocked(player)));
            registerAction(types, PAUSE_QUEUE, (player, payload) -> withController(player, payload,
                    controller -> controller.pauseQueue(player)));
            registerAction(types, RESUME_QUEUE, (player, payload) -> withController(player, payload,
                    controller -> controller.resumeQueue(player)));
            EchoMultiblockCore.LOGGER.info("ECHO MultiblockCore terminal automation bridge registered.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            REGISTERED.set(false);
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore terminal bridge could not register; Terminal remains optional.",
                    exception);
        }
    }

    private static void startTask(ServerPlayer player, String payload) {
        ActionPayload parsed = ActionPayload.parse(player, payload);
        if (parsed == null || parsed.recipeId() == null) {
            return;
        }
        withController(player, parsed, controller -> controller.queueRecipe(parsed.recipeId(), player));
    }

    private static void registerAction(TerminalReflection types, Identifier actionId, BiConsumer<ServerPlayer, String> action)
            throws ReflectiveOperationException {
        Object handler = Proxy.newProxyInstance(
                types.actionHandler.getClassLoader(),
                new Class<?>[] { types.actionHandler },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args);
                    }
                    if ("handle".equals(method.getName())) {
                        action.accept(args != null && args.length > 0 && args[0] instanceof ServerPlayer player ? player : null,
                                args != null && args.length > 1 && args[1] instanceof String payload ? payload : "");
                    }
                    return null;
                });
        types.registerAction(TAB_ID, actionId, handler);
    }

    private static void withController(ServerPlayer player, String payload, ControllerConsumer consumer) {
        ActionPayload parsed = ActionPayload.parse(player, payload);
        if (parsed == null) {
            return;
        }
        withController(player, parsed, consumer);
    }

    private static void withController(ServerPlayer player, ActionPayload payload, ControllerConsumer consumer) {
        if (player == null || payload == null || payload.controllerPos() == null || consumer == null) {
            return;
        }
        ServerLevel level = payload.level(player);
        if (level == null || !level.isLoaded(payload.controllerPos())) {
            return;
        }
        if (!player.level().dimension().equals(level.dimension()) || player.blockPosition().distManhattan(payload.controllerPos()) > 32) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(payload.controllerPos());
        if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
            consumer.accept(controller);
        }
    }

    private static Object recipeProvider(TerminalReflection types) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }
            return switch (method.getName()) {
                case "id" -> EchoMultiblockCore.id("automation_recipes");
                case "displayName" -> "Multiblock Automation";
                case "categories" -> recipeCategories(types);
                case "recipes" -> recipeEntries(types);
                default -> null;
            };
        };
        return Proxy.newProxyInstance(types.recipeProvider.getClassLoader(), new Class<?>[] { types.recipeProvider }, handler);
    }

    private static List<Object> recipeCategories(TerminalReflection types) throws ReflectiveOperationException {
        Map<Identifier, Object> categories = new LinkedHashMap<>();
        for (MultiblockAutomationRecipe recipe : AutomationRecipeRegistry.all()) {
            categories.putIfAbsent(recipe.category(), types.recipeCategory(
                    recipe.category(),
                    title(recipe.category()),
                    new ItemStack(ModBlocks.MULTIBLOCK_CONTROLLER.asItem()),
                    ACCENT,
                    categories.size() * 10));
        }
        return List.copyOf(categories.values());
    }

    private static List<Object> recipeEntries(TerminalReflection types) throws ReflectiveOperationException {
        java.util.ArrayList<Object> entries = new java.util.ArrayList<>();
        for (MultiblockAutomationRecipe recipe : AutomationRecipeRegistry.all().stream()
                .sorted(Comparator.comparing(value -> value.id().toString()))
                .toList()) {
            entries.add(recipeEntry(types, recipe));
        }
        return List.copyOf(entries);
    }

    private static Object recipeEntry(TerminalReflection types, MultiblockAutomationRecipe recipe)
            throws ReflectiveOperationException {
        java.util.ArrayList<Object> slots = new java.util.ArrayList<>();
        for (AutomationIngredient ingredient : recipe.inputs()) {
            slots.add(inputSlot(types, ingredient));
        }
        for (AutomationOutput output : recipe.outputs()) {
            slots.add(outputSlot(types, output));
        }
        java.util.ArrayList<Object> notes = new java.util.ArrayList<>();
        notes.add(types.recipeNote("Workcell: " + recipe.requiredWorkcell().name(), false));
        notes.add(types.recipeNote("Tools: " + (recipe.requiredTools().isEmpty()
                ? "Any" : recipe.requiredTools().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("Any")), false));
        for (String note : recipe.notes()) {
            notes.add(types.recipeNote(note, false));
        }
        return types.recipeEntry(
                recipe.id(),
                recipe.category(),
                recipe.displayName(),
                new ItemStack(ModBlocks.MULTIBLOCK_CONTROLLER.asItem()),
                List.copyOf(slots),
                List.copyOf(notes),
                recipe.durationTicks(),
                false);
    }

    private static Object inputSlot(TerminalReflection types, AutomationIngredient ingredient) throws ReflectiveOperationException {
        List<ItemStack> examples = ingredient.exampleStacks();
        return types.recipeSlot("INPUT", examples, ingredient.summary());
    }

    private static Object outputSlot(TerminalReflection types, AutomationOutput output) throws ReflectiveOperationException {
        ItemStack stack = output.stack();
        return stack.isEmpty()
                ? types.recipeSlot("OUTPUT", List.of(), output.summary())
                : types.recipeSlot("OUTPUT", List.of(stack), output.summary());
    }

    private static Object addonInfoProvider(TerminalReflection types) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }
            return switch (method.getName()) {
                case "chapterId" -> EchoMultiblockCore.CHAPTER_ID;
                case "info" -> addonInfo(types, args != null && args.length > 0 && args[0] instanceof Player player ? player : null);
                default -> null;
            };
        };
        return Proxy.newProxyInstance(types.addonInfoProvider.getClassLoader(), new Class<?>[] { types.addonInfoProvider }, handler);
    }

    private static Object addonInfo(TerminalReflection types, Player player) throws ReflectiveOperationException {
        List<MultiblockStatusSnapshot> snapshots = MultiblockIntegrationServices.terminalSnapshots(player);
        long active = snapshots.stream().filter(snapshot -> snapshot.state().name().equals("ACTIVE")).count();
        long blocked = snapshots.stream().filter(snapshot -> !snapshot.blockedReasonCode().isBlank()
                || snapshot.currentTasks().stream().anyMatch(task -> task.contains("BLOCKED"))).count();
        long capabilityFailures = MultiblockIntegrationServices.dataSnapshots(player).stream()
                .filter(snapshot -> !snapshot.capabilityRuntime().satisfied()).count();
        int queued = snapshots.stream().mapToInt(snapshot -> snapshot.currentTasks().size()).sum();
        List<Object> metrics = List.of(
                types.addonMetric("Definitions", String.valueOf(com.knoxhack.echomultiblockcore.content.MultiblockContent.definitions().size()), "loaded structures", ACCENT),
                types.addonMetric("Recipes", String.valueOf(AutomationRecipeRegistry.all().size()), "automation tasks", 0xFF92F7A6),
                types.addonMetric("Progression", String.valueOf(MultiblockProgressionRegistry.all().size()), "facility steps", 0xFFB7F7FF),
                types.addonMetric("Formed", String.valueOf(snapshots.size()), "known runtimes", 0xFFFFD166),
                types.addonMetric("Active", String.valueOf(active), "running workcells", 0xFFFF8FA3),
                types.addonMetric("Blocked", String.valueOf(blocked), "queues needing operator action", 0xFFFFB86B),
                types.addonMetric("Capabilities", String.valueOf(capabilityFailures), "facilities below requirement", 0xFFFF6B7A));
        List<Object> sections = List.of(
                types.addonSection("Task Queue", List.of(
                        "Queued tasks: " + queued,
                        "Terminal actions: start, clear, retry, pause, resume",
                        "Payload: dimension + controller_pos + recipe_id")),
                types.addonSection("Facility Progression", progressionLines()),
                types.addonSection("Facility Dashboard", dashboardLines(snapshots)),
                types.addonSection("Next Recommendation", nextRecommendation(snapshots)));
        return types.addonInfo("Shared facility automation, robotics, and multiblock runtime services.",
                metrics, sections, List.of());
    }

    private static List<String> dashboardLines(List<MultiblockStatusSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return List.of("No formed facilities are currently known.", "Build the Signal Tower to start the 1.3 route.");
        }
        return snapshots.stream()
                .limit(8)
                .map(snapshot -> {
                    String route = snapshot.facilityRoute().isBlank() ? snapshot.definitionId().getPath() : snapshot.facilityRoute();
                    String blocked = snapshot.blockedReasonCode().isBlank() ? "" : " / blocked " + snapshot.blockedReasonCode();
                    return "T" + snapshot.facilityTier() + " " + snapshot.name() + " / " + snapshot.state()
                            + " / " + route + blocked;
                })
                .toList();
    }

    private static List<String> nextRecommendation(List<MultiblockStatusSnapshot> snapshots) {
        int highestTier = snapshots.stream().mapToInt(MultiblockStatusSnapshot::facilityTier).max().orElse(0);
        Optional<MultiblockProgressionDefinition> next = MultiblockProgressionRegistry.all().stream()
                .filter(progression -> progression.tier() > highestTier)
                .sorted(Comparator.comparingInt(MultiblockProgressionDefinition::tier))
                .findFirst();
        if (next.isEmpty()) {
            return List.of("All loaded MultiblockCore progression tiers are represented.");
        }
        MultiblockProgressionDefinition progression = next.get();
        return List.of("Next: T" + progression.tier() + " " + progression.title(),
                progression.guideText(),
                progression.featuredRecipeSummary());
    }

    private static List<String> progressionLines() {
        List<String> lines = MultiblockProgressionRegistry.all().stream()
                .limit(8)
                .map(MultiblockTerminalBridge::progressionLine)
                .toList();
        return lines.isEmpty() ? List.of("No progression entries loaded.") : lines;
    }

    private static String progressionLine(MultiblockProgressionDefinition progression) {
        return "T" + progression.tier() + " " + progression.title()
                + (progression.featuredRecipes().isEmpty() ? "" : " // " + progression.featuredRecipeSummary());
    }

    private static String title(Identifier id) {
        return id.getPath().replace('_', ' ');
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "ReflectiveMultiblockTerminalBridge";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> null;
        };
    }

    private record ActionPayload(Identifier dimension, BlockPos controllerPos, Identifier recipeId) {
        static ActionPayload parse(ServerPlayer player, String payload) {
            try {
                JsonObject json = payload == null || payload.isBlank()
                        ? new JsonObject()
                        : JsonParser.parseString(payload).getAsJsonObject();
                Identifier dimension = json.has("dimension")
                        ? Identifier.parse(json.get("dimension").getAsString())
                        : (player == null ? Level.OVERWORLD.identifier() : player.level().dimension().identifier());
                BlockPos pos = pos(json.get("controller_pos"));
                Identifier recipe = json.has("recipe_id") ? Identifier.parse(json.get("recipe_id").getAsString()) : null;
                return new ActionPayload(dimension, pos, recipe);
            } catch (RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("Ignoring malformed MultiblockCore terminal action payload: {}", payload);
                return null;
            }
        }

        ServerLevel level(ServerPlayer player) {
            if (player == null) {
                return null;
            }
            MinecraftServer server = player.level().getServer();
            if (server == null) {
                return null;
            }
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
            return server.getLevel(key);
        }

        private static BlockPos pos(com.google.gson.JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.size() < 3) {
                    return null;
                }
                return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
            }
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return BlockPos.of(element.getAsLong());
            }
            String raw = element.getAsString().replace(',', ' ').trim();
            String[] parts = raw.split("\\s+");
            if (parts.length >= 3) {
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
            return BlockPos.of(Long.parseLong(raw));
        }
    }

    private static final class TerminalReflection {
        private final Class<?> recipeProvider;
        private final Class<?> recipeRegistry;
        private final Class<?> recipeCategory;
        private final Class<?> recipeEntry;
        private final Class<?> recipeNote;
        private final Class<?> recipeSlot;
        private final Class<?> recipeSlotRole;
        private final Class<?> addonInfoProvider;
        private final Class<?> addonInfoRegistry;
        private final Class<?> addonInfo;
        private final Class<?> addonMetric;
        private final Class<?> addonSection;
        private final Class<?> actionRegistry;
        private final Class<?> actionHandler;

        private TerminalReflection() throws ClassNotFoundException {
            recipeProvider = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider");
            recipeRegistry = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry");
            recipeCategory = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory");
            recipeEntry = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry");
            recipeNote = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote");
            recipeSlot = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot");
            recipeSlotRole = Class.forName("com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot$Role");
            addonInfoProvider = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfoProvider");
            addonInfoRegistry = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry");
            addonInfo = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfo");
            addonMetric = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonMetric");
            addonSection = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonSection");
            actionRegistry = Class.forName("com.knoxhack.echoterminal.api.TerminalActionRegistry");
            actionHandler = Class.forName("com.knoxhack.echoterminal.api.TerminalActionHandler");
        }

        private static Optional<TerminalReflection> load() {
            try {
                return Optional.of(new TerminalReflection());
            } catch (ClassNotFoundException | LinkageError exception) {
                return Optional.empty();
            }
        }

        private void registerRecipeProvider(Object provider) throws ReflectiveOperationException {
            recipeRegistry.getMethod("register", recipeProvider).invoke(null, provider);
        }

        private void registerAddonInfoProvider(Object provider) throws ReflectiveOperationException {
            addonInfoRegistry.getMethod("register", addonInfoProvider).invoke(null, provider);
        }

        private void registerAction(Identifier tabId, Identifier actionId, Object handler) throws ReflectiveOperationException {
            actionRegistry.getMethod("register", Identifier.class, Identifier.class, actionHandler)
                    .invoke(null, tabId, actionId, handler);
        }

        private Object recipeCategory(Identifier id, String title, ItemStack icon, int accentColor, int order)
                throws ReflectiveOperationException {
            return recipeCategory.getConstructor(Identifier.class, String.class, ItemStack.class, int.class, int.class)
                    .newInstance(id, title, icon, accentColor, order);
        }

        private Object recipeEntry(Identifier id, Identifier categoryId, String title, ItemStack machine,
                List<Object> slots, List<Object> notes, int processTicks, boolean locked) throws ReflectiveOperationException {
            return recipeEntry.getConstructor(Identifier.class, Identifier.class, String.class, ItemStack.class,
                            List.class, List.class, int.class, boolean.class)
                    .newInstance(id, categoryId, title, machine, slots, notes, processTicks, locked);
        }

        private Object recipeNote(String text, boolean warning) throws ReflectiveOperationException {
            return recipeNote.getConstructor(Component.class, boolean.class)
                    .newInstance(Component.literal(text == null ? "" : text), warning);
        }

        private Object recipeSlot(String roleName, List<ItemStack> stacks, String label) throws ReflectiveOperationException {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object role = Enum.valueOf((Class<? extends Enum>) recipeSlotRole.asSubclass(Enum.class), roleName);
            return recipeSlot.getConstructor(recipeSlotRole, List.class, String.class)
                    .newInstance(role, stacks == null ? List.of() : stacks, label);
        }

        private Object addonMetric(String label, String value, String detail, int color) throws ReflectiveOperationException {
            return addonMetric.getConstructor(String.class, String.class, String.class, int.class)
                    .newInstance(label, value, detail, color);
        }

        private Object addonSection(String title, List<String> lines) throws ReflectiveOperationException {
            return addonSection.getConstructor(String.class, List.class).newInstance(title, lines);
        }

        private Object addonInfo(String summary, List<Object> metrics, List<Object> sections, List<Object> links)
                throws ReflectiveOperationException {
            Constructor<?> constructor = addonInfo.getConstructor(String.class, List.class, List.class, List.class);
            return constructor.newInstance(summary, metrics, sections, links);
        }
    }

    @FunctionalInterface
    private interface ControllerConsumer {
        void accept(MultiblockControllerBlockEntity controller);
    }
}

package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Commands for managing NBT structures:
 * /modstructures create <name> <w> <h> <d> [category] - Export area to NBT
 * /modstructures reload - Reload all NBT structures
 * /modstructures list [category] - List available structures
 */
public class ModStructuresCommand {

    private static final Path ASHFALL_DATA_ROOT = resolveAshfallDataRoot();
    private static final Path STRUCTURES_BASE_PATH = ASHFALL_DATA_ROOT.resolve("structure");
    private static final Path TEMPLATE_POOL_PATH = ASHFALL_DATA_ROOT.resolve("worldgen/template_pool");
    private static final Pattern SAFE_STRUCTURE_TOKEN = Pattern.compile("[a-z0-9_]+");

    private static Path resolveAshfallDataRoot() {
        Path addonDataRoot = Path.of("addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol");
        if (Files.exists(addonDataRoot) || Files.exists(Path.of("addons/echoashfallprotocol"))) {
            return addonDataRoot;
        }
        return Path.of("src/main/resources/data/echoashfallprotocol");
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("modstructures")
                .requires(ModStructuresCommand::hasCommandPermission)
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("width", IntegerArgumentType.integer(1, 64))
                            .then(Commands.argument("height", IntegerArgumentType.integer(1, 64))
                                .then(Commands.argument("depth", IntegerArgumentType.integer(1, 64))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        int width = IntegerArgumentType.getInteger(ctx, "width");
                                        int height = IntegerArgumentType.getInteger(ctx, "height");
                                        int depth = IntegerArgumentType.getInteger(ctx, "depth");
                                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                            return exportStructure(player, name, width, height, depth, "global");
                                        }
                                        return 0;
                                    })
                                    .then(Commands.argument("category", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            int width = IntegerArgumentType.getInteger(ctx, "width");
                                            int height = IntegerArgumentType.getInteger(ctx, "height");
                                            int depth = IntegerArgumentType.getInteger(ctx, "depth");
                                            String category = StringArgumentType.getString(ctx, "category");
                                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                                return exportStructure(player, name, width, height, depth, category);
                                            }
                                            return 0;
                                        })
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            return reloadStructures(player);
                        }
                        return 0;
                    }))
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            return listStructures(player, null);
                        }
                        return 0;
                    })
                    .then(Commands.argument("category", StringArgumentType.word())
                        .executes(ctx -> {
                            String category = StringArgumentType.getString(ctx, "category");
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                return listStructures(player, category);
                            }
                            return 0;
                        })
                    )
                )
                .then(Commands.literal("generate")
                    .executes(ctx -> {
                        EchoAshfallProtocol.LOGGER.info("[ModStructuresCommand] POI structure generation requested in-game; generation is offline-only.");
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                            player.sendSystemMessage(Component.literal(
                                    "\u00a7e[ModStructures]\u00a7r POI generation now runs offline from the canonical Python generator."));
                            player.sendSystemMessage(Component.literal(
                                    "\u00a77[ModStructures]\u00a7r Use ./gradlew.bat generateEchoPoiStructures or python tools/structure_generator/generator.py."));
                        }
                        return 1;
                    })
                )
        );
    }

    private static int exportStructure(ServerPlayer player, String name, int width, int height, int depth, String category) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos startPos = player.blockPosition();
        if (!isSafeStructureToken(name) || !isSafeStructureToken(category)) {
            player.sendSystemMessage(Component.literal(
                    "[ModStructures] Names and categories must be lowercase letters, numbers, or underscores.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, startPos, new Vec3i(width, height, depth), false, java.util.Collections.singletonList(Blocks.STRUCTURE_VOID));

        CompoundTag nbt = template.save(new CompoundTag());
        Path nbtFile = resolveStructureOutputPath(name, category);
        if (!isStructureOutputPathSafe(nbtFile)) {
            player.sendSystemMessage(Component.literal("[ModStructures] Refusing unsafe export path.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        try {
            Files.createDirectories(nbtFile.getParent());
            NbtIo.writeCompressed(nbt, nbtFile);
            player.sendSystemMessage(Component.literal("\u00a7a[ModStructures]\u00a7r Exported '" + name + "' (" + width + "x" + height + "x" + depth + ") to " + category + "/" + name + ".nbt"));

            createTemplatePoolJson(name, category);

            return 1;
        } catch (IOException e) {
            player.sendSystemMessage(Component.literal("\u00a7c[ModStructures]\u00a7r Failed to export: " + e.getMessage()));
            return 0;
        }
    }

    private static void createTemplatePoolJson(String name, String category) {
        String location = category.equals("global")
            ? "echoashfallprotocol:global/" + name
            : "echoashfallprotocol:biomes/" + category + "/" + name;

        String jsonContent = "{\n" +
            "  \"fallback\": \"minecraft:empty\",\n" +
            "  \"elements\": [\n" +
            "    {\n" +
            "      \"weight\": 1,\n" +
            "      \"element\": {\n" +
            "        \"element_type\": \"minecraft:single_pool_element\",\n" +
            "        \"location\": \"" + location + "\",\n" +
            "        \"processors\": \"minecraft:empty\",\n" +
            "        \"projection\": \"rigid\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        String poolName = category.equals("global")
            ? "poi_" + name
            : category + "_" + name;

        Path poolFile = TEMPLATE_POOL_PATH.resolve(poolName + ".json").normalize();

        try {
            Files.createDirectories(TEMPLATE_POOL_PATH);
            Files.write(poolFile, jsonContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            EchoAshfallProtocol.LOGGER.warn("[ModStructuresCommand] Failed to write template pool {}: {}", poolFile, e.getMessage());
        }
    }

    private static int reloadStructures(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("\u00a7e[ModStructures]\u00a7r Note: New NBT files require a game restart to be loaded by the world generator."));
        player.sendSystemMessage(Component.literal("\u00a77[ModStructures]\u00a7r However, exported structures are saved and ready for use."));
        return 1;
    }

    private static int listStructures(ServerPlayer player, String category) {
        if (category != null && !isSafeStructureToken(category)) {
            player.sendSystemMessage(Component.literal(
                    "[ModStructures] Category must be lowercase letters, numbers, or underscores.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!Files.isDirectory(STRUCTURES_BASE_PATH)) {
            player.sendSystemMessage(Component.literal("\u00a7c[ModStructures]\u00a7r Structures directory not found."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("\u00a7b[ModStructures]\u00a7r Available NBT structures:"));

        int totalCount = 0;

        java.io.File globalDir = STRUCTURES_BASE_PATH.resolve("global").toFile();
        if (globalDir.exists() && (category == null || category.equals("global"))) {
            String[] files = globalDir.list((dir, name) -> name.endsWith(".nbt"));
            if (files != null && files.length > 0) {
                player.sendSystemMessage(Component.literal("\u00a76Global:\u00a7r"));
                for (String file : files) {
                    player.sendSystemMessage(Component.literal("  - " + file.replace(".nbt", "")));
                    totalCount++;
                }
            }
        }

        java.io.File biomesDir = STRUCTURES_BASE_PATH.resolve("biomes").toFile();
        if (biomesDir.exists()) {
            java.io.File[] biomeDirs = biomesDir.listFiles(java.io.File::isDirectory);
            if (biomeDirs != null) {
                for (java.io.File biomeDir : biomeDirs) {
                    if (category == null || category.equals(biomeDir.getName())) {
                        String[] files = biomeDir.list((dir, name) -> name.endsWith(".nbt"));
                        if (files != null && files.length > 0) {
                            player.sendSystemMessage(Component.literal("\u00a76" + biomeDir.getName() + ":\u00a7r"));
                            for (String file : files) {
                                player.sendSystemMessage(Component.literal("  - " + file.replace(".nbt", "")));
                                totalCount++;
                            }
                        }
                    }
                }
            }
        }

        if (totalCount == 0) {
            player.sendSystemMessage(Component.literal("\u00a77  No structures found. Use /modstructures create to export structures."));
        } else {
            player.sendSystemMessage(Component.literal("\u00a77Total: " + totalCount + " structure(s)"));
        }

        return 1;
    }

    public static boolean isSafeStructureToken(String value) {
        return value != null && SAFE_STRUCTURE_TOKEN.matcher(value).matches();
    }

    public static boolean hasCommandPermission(net.minecraft.commands.CommandSourceStack source) {
        return source != null && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static Path resolveStructureOutputPath(String name, String category) {
        String normalizedCategory = category == null || category.isBlank() ? "global" : category;
        Path directory = normalizedCategory.equals("global")
                ? STRUCTURES_BASE_PATH.resolve("global")
                : STRUCTURES_BASE_PATH.resolve("biomes").resolve(normalizedCategory);
        return directory.resolve(name + ".nbt").normalize();
    }

    public static boolean isStructureOutputPathSafe(Path outputPath) {
        Path normalizedBase = STRUCTURES_BASE_PATH.toAbsolutePath().normalize();
        Path normalizedOutput = outputPath.toAbsolutePath().normalize();
        return normalizedOutput.startsWith(normalizedBase);
    }

    @SuppressWarnings("unchecked")
    private static void registerUnchecked(CommandDispatcher<?> dispatcher) {
        register((CommandDispatcher<CommandSourceStack>) dispatcher);
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}

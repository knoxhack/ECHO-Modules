package com.knoxhack.echo.creatorcore.session;

import com.knoxhack.echo.creatorcore.api.CreatorPermission;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class CreatorPermissionService {
    private CreatorPermissionService() {
    }

    public static CreatorPermission permissionFor(CommandSourceStack source) {
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ENABLED, true)) {
            return CreatorPermission.BLOCKED;
        }
        if (source.getEntity() == null) {
            return CreatorPermission.DEVELOPER;
        }
        boolean requireOperator = CreatorCoreConfig.bool(CreatorCoreConfig.REQUIRE_OPERATOR, true);
        int operatorLevel = CreatorCoreConfig.integer(CreatorCoreConfig.OPERATOR_PERMISSION_LEVEL, 2);
        if (source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
            return CreatorPermission.DEVELOPER;
        }
        if (source.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
            return CreatorPermission.CREATOR;
        }
        if (hasOperatorLevel(source, operatorLevel)) {
            return CreatorPermission.OPERATOR;
        }
        return requireOperator ? CreatorPermission.BLOCKED : CreatorPermission.VIEWER;
    }

    public static boolean canView(CommandSourceStack source) {
        return permissionFor(source).atLeast(CreatorPermission.VIEWER);
    }

    public static boolean canOperate(CommandSourceStack source) {
        return permissionFor(source).atLeast(CreatorPermission.OPERATOR);
    }

    public static boolean canWriteDrafts(CommandSourceStack source) {
        return permissionFor(source).atLeast(CreatorPermission.CREATOR)
                && CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false);
    }

    public static boolean canExport(CommandSourceStack source) {
        return permissionFor(source).atLeast(CreatorPermission.DEVELOPER)
                && CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false);
    }

    public static String denial(CommandSourceStack source, CreatorPermission required) {
        CreatorPermission actual = permissionFor(source);
        if (actual == CreatorPermission.BLOCKED) {
            return "CreatorCore access is blocked by config or operator permissions.";
        }
        return "CreatorCore requires " + required + " permission; current permission is " + actual + ".";
    }

    public static boolean isLocalPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && source.getServer().getPlayerList().getPlayer(player.getUUID()) != null
                && !source.getServer().isDedicatedServer();
    }

    private static boolean hasOperatorLevel(CommandSourceStack source, int operatorLevel) {
        if (operatorLevel >= 4) {
            return source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
        }
        if (operatorLevel == 3) {
            return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        }
        if (operatorLevel == 2) {
            return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        if (operatorLevel == 1) {
            return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
        }
        return true;
    }
}

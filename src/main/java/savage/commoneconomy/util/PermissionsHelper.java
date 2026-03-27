package savage.commoneconomy.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import savage.commoneconomy.config.ConfigManager;

/**
 * Utility for checking permissions.
 * Reads OP levels from permissions.json config.
 * TODO: Integrate with Fabric Permissions API when it supports 26.1 Mojang mappings.
 */
public class PermissionsHelper {

    /**
     * Checks if a command source has a specific permission.
     * Reads the required OP level from permissions.json.
     * @param source The command source.
     * @param permission The permission node (e.g. "savscommoneconomy.command.bal").
     * @return true if permitted.
     */
    public static boolean check(CommandSourceStack source, String permission) {
        int level = ConfigManager.getPermissions().getLevel(permission);
        if (level <= 0) return true;
        if (level >= 4) {
            return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Overload that accepts a hardcoded fallback OP level.
     * Used when the permission node isn't in the config (shouldn't happen in normal use).
     */
    public static boolean check(CommandSourceStack source, String permission, int fallbackLevel) {
        int level = ConfigManager.getPermissions().permissions.getOrDefault(permission, fallbackLevel);
        if (level <= 0) return true;
        if (level >= 4) {
            return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Overload for boolean default (true = anyone, false = op only).
     * Kept for backwards compatibility but reads from config when available.
     */
    public static boolean check(CommandSourceStack source, String permission, boolean anyone) {
        // If the permission is in the config, use that instead of the boolean default
        if (ConfigManager.getPermissions().permissions.containsKey(permission)) {
            return check(source, permission);
        }
        if (anyone) return true;
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean check(ServerPlayer player, String permission, int fallbackLevel) {
        int level = ConfigManager.getPermissions().permissions.getOrDefault(permission, fallbackLevel);
        if (level <= 0) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}

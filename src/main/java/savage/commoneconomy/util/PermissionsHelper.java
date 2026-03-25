package savage.commoneconomy.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * Utility for checking permissions.
 * Uses vanilla OP level checks for now.
 * TODO: Integrate with Fabric Permissions API when it supports 26.1 Mojang mappings.
 * The permission string parameter is kept for future LuckPerms/permissions mod integration.
 */
public class PermissionsHelper {
    /**
     * Checks if a command source has a specific permission.
     * @param source The source to check.
     * @param permission The permission string (reserved for future permissions mod support).
     * @param defaultOpLevel The OP level required (0=anyone, 2=gamemaster, 4=admin).
     * @return true if permitted.
     */
    public static boolean check(CommandSourceStack source, String permission, int defaultOpLevel) {
        if (defaultOpLevel <= 0) return true;
        if (defaultOpLevel >= 4) {
            return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Overload for boolean default (true = anyone, false = op only).
     */
    public static boolean check(CommandSourceStack source, String permission, boolean anyone) {
        if (anyone) return true;
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean check(ServerPlayer player, String permission, int defaultOpLevel) {
        if (defaultOpLevel <= 0) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}

package savage.commoneconomy.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * Utility for checking permissions.
 * Supports OP levels and future-proofed for permission mods.
 */
public class PermissionsHelper {
    /**
     * Checks if a command source has a specific permission.
     * @param source The source to check.
     * @param permission The permission string.
     * @param defaultOpLevel The default OP level required if no permission mod is present.
     * @return true if permitted.
     */
    public static boolean check(CommandSourceStack source, String permission, int defaultOpLevel) {
        if (defaultOpLevel <= 0) return true;
        if (defaultOpLevel == 2 && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        if (defaultOpLevel == 4 && source.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
            return true;
        }
        // Fallback or generic check if any permission is present
        if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        return false;
    }

    /**
     * Overload for boolean default (true = anyone, false = op only).
     */
    public static boolean check(CommandSourceStack source, String permission, boolean anyone) {
        return anyone || source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean check(ServerPlayer player, String permission, int defaultOpLevel) {
        if (defaultOpLevel <= 0) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}

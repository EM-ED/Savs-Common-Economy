package savage.commoneconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import savage.commoneconomy.EconomyManager;
import savage.commoneconomy.util.PermissionsHelper;
import savage.commoneconomy.util.TransactionLogger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Administrative economy commands.
 */
public class AdminEconomyCommands {

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(Arrays.asList(context.getSource().getServer().getPlayerNames()));
        suggestions.addAll(EconomyManager.getInstance().getAllPlayerNames());
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /givemoney <target> <amount>
        dispatcher.register(Commands.literal("givemoney")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.admin", 2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(AdminEconomyCommands::giveMoney))));

        // /takemoney <target> <amount>
        dispatcher.register(Commands.literal("takemoney")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.admin", 2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(AdminEconomyCommands::takeMoney))));

        // /setmoney <target> <amount>
        dispatcher.register(Commands.literal("setmoney")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.admin", 2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(AdminEconomyCommands::setMoney))));

        // /resetmoney <target>
        dispatcher.register(Commands.literal("resetmoney")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.admin", 2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(AdminEconomyCommands::resetMoney)));
    }

    private static int giveMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "target");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));
        UUID targetUUID = lookupUUID(context, targetName);

        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        EconomyManager.getInstance().addBalance(targetUUID, amount);
        String formatted = EconomyManager.getInstance().format(amount);
        context.getSource().sendSuccess(() -> Component.literal("Gave " + formatted + " to " + targetName), true);
        
        TransactionLogger.log("ADMIN_GIVE", context.getSource().getTextName(), targetName, amount, "Admin Gift");
        notifyTarget(context, targetUUID, "Received " + formatted + " (Admin Gift)");
        return 1;
    }

    private static int takeMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "target");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));
        UUID targetUUID = lookupUUID(context, targetName);

        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        if (EconomyManager.getInstance().removeBalance(targetUUID, amount)) {
            String formatted = EconomyManager.getInstance().format(amount);
            context.getSource().sendSuccess(() -> Component.literal("Took " + formatted + " from " + targetName), true);
            
            TransactionLogger.log("ADMIN_TAKE", context.getSource().getTextName(), targetName, amount, "Admin Take");
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Target has insufficient funds to take this amount."));
            return 0;
        }
    }

    private static int setMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "target");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));
        UUID targetUUID = lookupUUID(context, targetName);

        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        EconomyManager.getInstance().setBalance(targetUUID, amount);
        String formatted = EconomyManager.getInstance().format(amount);
        context.getSource().sendSuccess(() -> Component.literal("Set " + targetName + "'s balance to " + formatted), true);
        
        TransactionLogger.log("ADMIN_SET", context.getSource().getTextName(), targetName, amount, "Admin Set");
        notifyTarget(context, targetUUID, "Your balance has been set to " + formatted + " by an admin.");
        return 1;
    }

    private static int resetMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "target");
        UUID targetUUID = lookupUUID(context, targetName);

        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        EconomyManager.getInstance().resetBalance(targetUUID);
        BigDecimal defaultBal = EconomyManager.getInstance().getBalance(targetUUID);
        String formatted = EconomyManager.getInstance().format(defaultBal);
        context.getSource().sendSuccess(() -> Component.literal("Reset " + targetName + "'s balance to " + formatted), true);
        
        TransactionLogger.log("ADMIN_RESET", context.getSource().getTextName(), targetName, defaultBal, "Admin Reset");
        notifyTarget(context, targetUUID, "Your balance has been reset to " + formatted + " by an admin.");
        return 1;
    }

    private static UUID lookupUUID(CommandContext<CommandSourceStack> context, String name) {
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (target != null) return target.getUUID();
        return EconomyManager.getInstance().getUUIDFromName(name);
    }

    private static void notifyTarget(CommandContext<CommandSourceStack> context, UUID targetUUID, String message) {
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayer(targetUUID);
        if (target != null) {
            target.sendSystemMessage(Component.literal(message));
        }
    }
}

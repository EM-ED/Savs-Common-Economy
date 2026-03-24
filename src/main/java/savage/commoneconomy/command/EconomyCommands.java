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
import savage.commoneconomy.model.AccountData;
import savage.commoneconomy.util.PermissionsHelper;
import savage.commoneconomy.util.TransactionLogger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Player-facing economy commands.
 */
public class EconomyCommands {

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        // Online players
        suggestions.addAll(Arrays.asList(context.getSource().getServer().getPlayerNames()));
        // Offline players from storage
        suggestions.addAll(EconomyManager.getInstance().getAllPlayerNames());
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /bal and /balance
        var balCommand = Commands.literal("bal")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.command.bal", true))
                .executes(EconomyCommands::checkSelfBalance)
                .then(Commands.argument("target", StringArgumentType.string())
                        .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.command.bal.others", true))
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(EconomyCommands::checkOtherBalance));

        dispatcher.register(balCommand);
        dispatcher.register(Commands.literal("balance")
                .requires(balCommand.getRequirement())
                .executes(EconomyCommands::checkSelfBalance)
                .redirect(balCommand.build())); // Alias

        // /pay <target> <amount>
        dispatcher.register(Commands.literal("pay")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.command.pay", true))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(EconomyCommands::pay))));

        // /baltop and /balancetop
        var baltopCommand = Commands.literal("baltop")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.command.baltop", true))
                .executes(EconomyCommands::balTop);

        dispatcher.register(baltopCommand);
        dispatcher.register(Commands.literal("balancetop")
                .requires(baltopCommand.getRequirement())
                .executes(EconomyCommands::balTop));
    }

    private static int checkSelfBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        BigDecimal balance = EconomyManager.getInstance().getBalance(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal("Your balance: " + EconomyManager.getInstance().format(balance)), false);
        return 1;
    }

    private static int checkOtherBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(context, "target");
        UUID targetUUID = lookupUUID(context, targetName);

        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        BigDecimal balance = EconomyManager.getInstance().getBalance(targetUUID);
        context.getSource().sendSuccess(() -> Component.literal(targetName + "'s balance: " + EconomyManager.getInstance().format(balance)), false);
        return 1;
    }

    private static int pay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayer();
        String targetName = StringArgumentType.getString(context, "target");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));

        UUID targetUUID = lookupUUID(context, targetName);
        if (targetUUID == null) {
            context.getSource().sendFailure(Component.literal("Player not found in economy database."));
            return 0;
        }

        if (sender.getUUID().equals(targetUUID)) {
            context.getSource().sendFailure(Component.literal("You cannot pay yourself."));
            return 0;
        }

        if (EconomyManager.getInstance().removeBalance(sender.getUUID(), amount)) {
            EconomyManager.getInstance().addBalance(targetUUID, amount);
            
            String formatted = EconomyManager.getInstance().format(amount);
            context.getSource().sendSuccess(() -> Component.literal("Paid " + formatted + " to " + targetName), false);
            
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(targetUUID);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("Received " + formatted + " from " + sender.getName().getString()));
            }
            
            TransactionLogger.log("PAY", sender.getName().getString(), targetName, amount, "Player Payment");
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Insufficient funds."));
            return 0;
        }
    }

    private static int balTop(CommandContext<CommandSourceStack> context) {
        List<AccountData> top = EconomyManager.getInstance().getTopAccounts(10);
        context.getSource().sendSuccess(() -> Component.literal("--- Top 10 Balances ---"), false);
        for (int i = 0; i < top.size(); i++) {
            AccountData account = top.get(i);
            int rank = i + 1;
            context.getSource().sendSuccess(() -> Component.literal(rank + ". " + account.getName() + ": " + EconomyManager.getInstance().format(account.getBalance())), false);
        }
        return 1;
    }

    private static UUID lookupUUID(CommandContext<CommandSourceStack> context, String name) {
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (target != null) return target.getUUID();
        return EconomyManager.getInstance().getUUIDFromName(name);
    }
}

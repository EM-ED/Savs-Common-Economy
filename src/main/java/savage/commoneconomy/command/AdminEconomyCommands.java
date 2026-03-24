package savage.commoneconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import savage.commoneconomy.EconomyManager;
import savage.commoneconomy.util.PermissionsHelper;
import savage.commoneconomy.util.TransactionLogger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        // /ecolog <target> <time> <unit> [page]
        dispatcher.register(Commands.literal("ecolog")
                .requires(source -> PermissionsHelper.check(source, "savscommoneconomy.admin", 2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("*");
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getPlayerNamesArray(), builder);
                        })
                        .then(Commands.argument("time", IntegerArgumentType.integer(1))
                                .then(Commands.argument("unit", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"s", "m", "h", "d"}, builder))
                                        .executes(context -> executeLogSearch(context, 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> executeLogSearch(context, IntegerArgumentType.getInteger(context, "page"))))))));
    }

    private static final int RESULTS_PER_PAGE = 6;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static int executeLogSearch(CommandContext<CommandSourceStack> context, int page) {
        String target = StringArgumentType.getString(context, "target");
        int time = IntegerArgumentType.getInteger(context, "time");
        String unit = StringArgumentType.getString(context, "unit");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff;

        switch (unit.toLowerCase()) {
            case "s": cutoff = now.minusSeconds(time); break;
            case "m": cutoff = now.minusMinutes(time); break;
            case "h": cutoff = now.minusHours(time); break;
            case "d": cutoff = now.minusDays(time); break;
            default:
                context.getSource().sendFailure(Component.literal("Invalid time unit. Use s, m, h, or d."));
                return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Searching logs for " + target + " in the last " + time + unit + "..."), false);

        // Async execution to avoid blocking server
        new Thread(() -> {
            List<TransactionLogger.LogEntry> results = TransactionLogger.searchLogs(target, cutoff);
            
            if (results.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("No transactions found."), false);
                return;
            }

            int totalPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);
            int currentPage = Math.min(page, totalPages);
            
            context.getSource().sendSuccess(() -> Component.literal("--- Found " + results.size() + " transactions (Page " + currentPage + "/" + totalPages + ") ---")
                    .withStyle(ChatFormatting.GOLD), false);
            
            int startIndex = (currentPage - 1) * RESULTS_PER_PAGE;
            int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, results.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                TransactionLogger.LogEntry entry = results.get(i);
                
                ChatFormatting typeColor = ChatFormatting.WHITE;
                if (entry.type.contains("PAY")) typeColor = ChatFormatting.GREEN;
                else if (entry.type.contains("ADMIN")) typeColor = ChatFormatting.RED;
                else if (entry.type.contains("SHOP")) typeColor = ChatFormatting.GOLD;
                else if (entry.type.contains("WITHDRAW")) typeColor = ChatFormatting.AQUA;

                MutableComponent logText = Component.empty()
                        .append(Component.literal("[" + entry.timestamp.format(TIME_FORMAT) + "] ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("[" + entry.type + "] ")
                                .withStyle(typeColor))
                        .append(Component.literal(entry.source)
                                .withStyle(ChatFormatting.RED))
                        .append(Component.literal(" -> ")
                                .withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(entry.target)
                                .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(": $" + entry.amount.toPlainString() + " ")
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("(" + entry.reason + ")")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

                context.getSource().sendSuccess(() -> logText, false);
            }
            
            if (totalPages > 1) {
                MutableComponent navText = Component.empty();
                if (currentPage > 1) {
                    navText.append(Component.literal("[< Previous] ")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                            .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand( 
                                    "/ecolog " + target + " " + time + " " + unit + " " + (currentPage - 1)))));
                }
                
                if (currentPage < totalPages) {
                    navText.append(Component.literal("[Next >]")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                            .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand( 
                                    "/ecolog " + target + " " + time + " " + unit + " " + (currentPage + 1)))));
                }
                context.getSource().sendSuccess(() -> navText, false);
            }
        }).start();

        return 1;
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

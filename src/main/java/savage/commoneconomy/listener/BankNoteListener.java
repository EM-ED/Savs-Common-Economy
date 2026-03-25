package savage.commoneconomy.listener;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import savage.commoneconomy.EconomyManager;
import savage.commoneconomy.SavsCommonEconomy;
import savage.commoneconomy.util.TransactionLogger;

import java.math.BigDecimal;

/**
 * Handles bank note redemption (right-click to deposit).
 */
public class BankNoteListener {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                var stack = player.getItemInHand(hand);
                if (stack.is(Items.PAPER)) {
                    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.contains("EconomyBankNote") && tag.contains("Value")) {
                            double valueDouble = tag.getDouble("Value").orElse(0.0);
                            BigDecimal value = BigDecimal.valueOf(valueDouble);
                            
                            // Prevent double spend by shrinking immediately before triggered future
                            stack.shrink(1);
                            
                            EconomyManager.getInstance().addBalance(player.getUUID(), value).thenAccept(success -> {
                                if (success) {
                                    player.sendSystemMessage(Component.literal("Redeemed bank note for " + EconomyManager.getInstance().format(value))
                                        .withStyle(ChatFormatting.GREEN));
                                    TransactionLogger.log("DEPOSIT", "Bank Note", player.getName().getString(), value, "Redeemed Note");
                                } else {
                                    // Critical failure: refund the item? For now, log error and notify player
                                    player.sendSystemMessage(Component.literal("Failed to deposit bank note! Please contact an administrator.")
                                        .withStyle(ChatFormatting.RED));
                                    SavsCommonEconomy.LOGGER.error("Failed to deposit bank note for player " + player.getName().getString() + " (" + player.getUUID() + ")");
                                }
                            });
                            
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}

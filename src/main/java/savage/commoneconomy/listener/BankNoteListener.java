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
                            
                            var server = ((net.minecraft.server.level.ServerLevel) world).getServer();
                            EconomyManager.getInstance().addBalance(player.getUUID(), value).thenAccept(success -> {
                                // Must run inventory changes on main server thread
                                server.execute(() -> {
                                    if (success) {
                                        // Only consume the note AFTER balance was successfully added
                                        var currentStack = player.getItemInHand(hand);
                                        if (currentStack.is(Items.PAPER)) {
                                            CustomData cd = currentStack.get(DataComponents.CUSTOM_DATA);
                                            if (cd != null) {
                                                CompoundTag t = cd.copyTag();
                                                if (t.contains("EconomyBankNote")) {
                                                    currentStack.shrink(1);
                                                }
                                            }
                                        }
                                        player.sendSystemMessage(Component.literal("Redeemed bank note for " + EconomyManager.getInstance().format(value))
                                            .withStyle(ChatFormatting.GREEN));
                                        TransactionLogger.log("DEPOSIT", "Bank Note", player.getName().getString(), value, "Redeemed Note");
                                    } else {
                                        player.sendSystemMessage(Component.literal("Failed to deposit bank note! Please try again.")
                                            .withStyle(ChatFormatting.RED));
                                    }
                                });
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

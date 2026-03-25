package savage.commoneconomy.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import eu.pb4.common.economy.api.EconomyCurrency;
import eu.pb4.common.economy.api.EconomyProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import savage.commoneconomy.EconomyManager;
import java.math.BigInteger;

public class SavsEconomyCurrency implements EconomyCurrency {
    private final EconomyProvider provider;

    public SavsEconomyCurrency(EconomyProvider provider) {
        this.provider = provider;
    }

    @Override
    public Component name() {
        return Component.literal("Dollar");
    }

    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath("savs_common_economy", "dollar");
    }

    @Override
    public String formatValue(BigInteger value, boolean full) {
        return EconomyManager.getInstance().format(new java.math.BigDecimal(value));
    }

    @Override
    public Component formatValueComponent(BigInteger value, boolean full) {
        return Component.literal(formatValue(value, full));
    }

    @Override
    public BigInteger parseValue(String value) {
        try {
            return new java.math.BigDecimal(value).toBigInteger();
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    @Override
    public EconomyProvider provider() {
        return provider;
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.EMERALD);
    }
}

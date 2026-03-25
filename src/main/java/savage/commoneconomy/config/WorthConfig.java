package savage.commoneconomy.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores item prices for the /sell, /worth, and /buy system.
 */
public class WorthConfig {
    // Map of Item ID (e.g. "minecraft:diamond") to Sell Price
    public Map<String, BigDecimal> sellPrices = new HashMap<>();
    
    // Map of Item ID to Buy Price
    public Map<String, BigDecimal> buyPrices = new HashMap<>();

    public WorthConfig() {
        // Defaults
        sellPrices.put("minecraft:diamond", new BigDecimal("100.00"));
        sellPrices.put("minecraft:gold_ingot", new BigDecimal("50.00"));
        sellPrices.put("minecraft:iron_ingot", new BigDecimal("10.00"));
        sellPrices.put("minecraft:emerald", new BigDecimal("200.00"));
        
        buyPrices.put("minecraft:diamond", new BigDecimal("200.00"));
        buyPrices.put("minecraft:gold_ingot", new BigDecimal("100.00"));
        buyPrices.put("minecraft:iron_ingot", new BigDecimal("20.00"));
        buyPrices.put("minecraft:emerald", new BigDecimal("400.00"));
    }
}

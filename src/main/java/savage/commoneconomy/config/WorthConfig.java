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
        // Defaults (original match)
        sellPrices.put("minecraft:apple", new BigDecimal("10.00"));
        buyPrices.put("minecraft:apple", new BigDecimal("20.00"));
    }
}

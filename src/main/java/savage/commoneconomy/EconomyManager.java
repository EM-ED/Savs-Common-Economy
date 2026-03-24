package savage.commoneconomy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import savage.commoneconomy.config.ConfigManager;
import savage.commoneconomy.model.AccountData;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The central logic for the Savs Common Economy mod.
 * Handles caching, balance manipulation, and sync orchestration.
 */
public class EconomyManager {
    private static EconomyManager INSTANCE;
    
    // In-memory cache for player accounts
    private final Cache<UUID, AccountData> accountCache;

    private EconomyManager() {
        this.accountCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();
    }

    public static EconomyManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EconomyManager();
        }
        return INSTANCE;
    }

    /**
     * Gets a player's balance. Defaults to configured starting balance if account missing.
     */
    public BigDecimal getBalance(UUID uuid) {
        AccountData account = accountCache.getIfPresent(uuid);
        if (account != null) {
            return account.getBalance();
        }
        
        // This is a placeholder for storage lookup in Phase 2
        return ConfigManager.getConfig().defaultBalance;
    }

    /**
     * Adds balance to a player's account.
     * @return true if successful.
     */
    public boolean addBalance(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        
        AccountData account = getOrCreateAccount(uuid);
        account.setBalance(account.getBalance().add(amount));
        account.incrementVersion();
        
        // Invalidate/Sync logic will go here in Phase 5
        return true;
    }

    /**
     * Removes balance from a player's account.
     * @return true if successful (fails if insufficient funds).
     */
    public boolean removeBalance(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        
        AccountData account = getOrCreateAccount(uuid);
        if (account.getBalance().compareTo(amount) < 0) {
            return false; // Insufficient funds
        }
        
        account.setBalance(account.getBalance().subtract(amount));
        account.incrementVersion();
        return true;
    }

    /**
     * Internal helper to ensure an account exists in the cache.
     */
    private AccountData getOrCreateAccount(UUID uuid) {
        return accountCache.get(uuid, k -> new AccountData("Unknown", ConfigManager.getConfig().defaultBalance));
    }

    /**
     * Sets a player's balance directly.
     */
    public void setBalance(UUID uuid, BigDecimal balance) {
        AccountData account = getOrCreateAccount(uuid);
        account.setBalance(balance);
        account.incrementVersion();
    }

    public boolean hasAccount(UUID uuid) {
        // Will be expanded in Phase 2 with storage checks
        return accountCache.getIfPresent(uuid) != null;
    }

    public void createAccount(UUID uuid, String name) {
        AccountData account = new AccountData(name, ConfigManager.getConfig().defaultBalance);
        accountCache.put(uuid, account);
    }
}

package savage.commoneconomy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import savage.commoneconomy.config.ConfigManager;
import savage.commoneconomy.model.AccountData;
import savage.commoneconomy.storage.EconomyStorage;
import savage.commoneconomy.storage.JsonStorage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The central logic for the Savs Common Economy mod.
 * Handles caching, balance manipulation, and sync orchestration.
 */
public class EconomyManager {
    private static EconomyManager INSTANCE;
    
    // In-memory cache for player accounts
    private final Cache<UUID, AccountData> accountCache;
    private final EconomyStorage storage;

    private EconomyManager() {
        this.storage = new JsonStorage(); // Default to JSON for Phase 2
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
        AccountData account = accountCache.get(uuid, k -> {
            AccountData stored = storage.loadAccount(uuid);
            return stored != null ? stored : new AccountData("Unknown", ConfigManager.getConfig().defaultBalance);
        });
        return account.getBalance();
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
        
        storage.saveAccount(uuid, account);
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
        
        storage.saveAccount(uuid, account);
        return true;
    }

    /**
     * Internal helper to ensure an account exists in the cache and storage.
     */
    public AccountData getOrCreateAccount(UUID uuid) {
        return getOrCreateAccount(uuid, null);
    }

    /**
     * Internal helper to ensure an account exists in the cache and storage.
     * Updates name if provided and different.
     */
    public AccountData getOrCreateAccount(UUID uuid, String name) {
        return accountCache.get(uuid, k -> {
            AccountData stored = storage.loadAccount(uuid);
            if (stored != null) {
                if (name != null && !name.equals(stored.getName())) {
                    stored.setName(name);
                    storage.saveAccount(uuid, stored);
                }
                return stored;
            }
            
            AccountData newAccount = new AccountData(name != null ? name : "Unknown", ConfigManager.getConfig().defaultBalance);
            storage.saveAccount(uuid, newAccount);
            return newAccount;
        });
    }

    /**
     * Sets a player's balance directly.
     */
    public void setBalance(UUID uuid, BigDecimal balance) {
        AccountData account = getOrCreateAccount(uuid);
        account.setBalance(balance);
        account.incrementVersion();
        storage.saveAccount(uuid, account);
    }

    /**
     * Resets a player's balance to the default starting balance.
     */
    public void resetBalance(UUID uuid) {
        setBalance(uuid, ConfigManager.getConfig().defaultBalance);
    }

    public boolean hasAccount(UUID uuid) {
        return accountCache.getIfPresent(uuid) != null || storage.loadAccount(uuid) != null;
    }

    public void createAccount(UUID uuid, String name) {
        AccountData account = new AccountData(name, ConfigManager.getConfig().defaultBalance);
        accountCache.put(uuid, account);
        storage.saveAccount(uuid, account);
    }

    /**
     * Formats a balance with the configured currency symbol.
     */
    public String format(BigDecimal balance) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String symbol = ConfigManager.getConfig().currencySymbol;
        return symbol + df.format(balance);
    }

    /**
     * Returns the top accounts for baltop.
     */
    public List<AccountData> getTopAccounts(int limit) {
        return storage.loadAllAccounts().values().stream()
                .sorted((a, b) -> b.getBalance().compareTo(a.getBalance()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Looks up a UUID by player name from the storage.
     */
    public UUID getUUIDFromName(String name) {
        return storage.loadAllAccounts().entrySet().stream()
                .filter(entry -> entry.getValue().getName().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all known player names (for suggestions).
     */
    public List<String> getAllPlayerNames() {
        return storage.loadAllAccounts().values().stream()
                .map(AccountData::getName)
                .collect(Collectors.toList());
    }

    /**
     * Gracefully shuts down the economy engine.
     */
    public void shutdown() {
        storage.shutdown();
    }
}

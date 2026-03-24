package savage.commoneconomy.storage;

import savage.commoneconomy.model.AccountData;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for economy storage handlers.
 */
public interface EconomyStorage {
    /**
     * Loads an account from storage.
     */
    AccountData loadAccount(UUID uuid);

    /**
     * Saves an account to storage.
     */
    void saveAccount(UUID uuid, AccountData data);

    /**
     * Deletes an account from storage.
     */
    void deleteAccount(UUID uuid);

    /**
     * Performs a graceful shutdown of the storage handler.
     */
    void shutdown();

    /**
     * @return All stored accounts (used for baltop/migrations).
     */
    Map<UUID, AccountData> loadAllAccounts();
}

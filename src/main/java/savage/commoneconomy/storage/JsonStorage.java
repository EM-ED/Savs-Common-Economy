package savage.commoneconomy.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import savage.commoneconomy.SavsCommonEconomy;
import savage.commoneconomy.model.AccountData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JSON-based storage for economy accounts.
 */
public class JsonStorage implements EconomyStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File STORAGE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("savs-common-economy").resolve("balances.json").toFile();

    private Map<UUID, AccountData> cachedData = new HashMap<>();

    public JsonStorage() {
        load();
    }

    private void load() {
        if (!STORAGE_FILE.exists()) return;

        try (FileReader reader = new FileReader(STORAGE_FILE)) {
            Type type = new TypeToken<Map<UUID, AccountData>>() {}.getType();
            Map<UUID, AccountData> data = GSON.fromJson(reader, type);
            if (data != null) {
                this.cachedData = data;
            }
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to load balances from JSON!", e);
        }
    }

    private void save() {
        try {
            File parent = STORAGE_FILE.getParentFile();
            if (!parent.exists()) parent.mkdirs();

            try (FileWriter writer = new FileWriter(STORAGE_FILE)) {
                GSON.toJson(cachedData, writer);
            }
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to save balances to JSON!", e);
        }
    }

    @Override
    public AccountData loadAccount(UUID uuid) {
        return cachedData.get(uuid);
    }

    @Override
    public void saveAccount(UUID uuid, AccountData data) {
        cachedData.put(uuid, data);
        save();
    }

    @Override
    public void deleteAccount(UUID uuid) {
        cachedData.remove(uuid);
        save();
    }

    @Override
    public void shutdown() {
        save();
    }

    @Override
    public Map<UUID, AccountData> loadAllAccounts() {
        return new HashMap<>(cachedData);
    }
}

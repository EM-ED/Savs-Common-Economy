package savage.commoneconomy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import savage.commoneconomy.SavsCommonEconomy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles JSON serialization for EconomyConfig.
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final java.nio.file.Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("savs-common-economy");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("config.json").toFile();
    private static final File WORTH_FILE = CONFIG_DIR.resolve("worth.json").toFile();
    
    private static EconomyConfig currentConfig = new EconomyConfig();
    private static WorthConfig worthConfig = new WorthConfig();

    /**
     * Loads the config from disk, or saves default if it doesn't exist.
     */
    public static void load() {
        loadMain();
        loadWorth();
    }

    private static void loadMain() {
        if (!CONFIG_FILE.exists()) {
            saveMain(); // Save defaults
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            currentConfig = GSON.fromJson(reader, EconomyConfig.class);
            if (currentConfig == null) {
                currentConfig = new EconomyConfig();
                saveMain();
            }
            SavsCommonEconomy.LOGGER.info("Successfully loaded configuration.");
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to load configuration!", e);
        }
    }

    /**
     * Saves all configurations to disk.
     */
    public static void save() {
        saveMain();
        saveWorth();
    }

    private static void saveMain() {
        try {
            File dir = CONFIG_DIR.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(currentConfig, writer);
                SavsCommonEconomy.LOGGER.info("Successfully saved configuration.");
            }
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to save configuration!", e);
        }
    }

    private static void loadWorth() {
        if (!WORTH_FILE.exists()) {
            saveWorth();
            return;
        }

        try (FileReader reader = new FileReader(WORTH_FILE)) {
            // First, check for legacy format migration (old "itemPrices" -> new "sellPrices")
            com.google.gson.JsonObject rawJson = GSON.fromJson(reader, com.google.gson.JsonObject.class);
            
            if (rawJson != null && rawJson.has("itemPrices") && !rawJson.has("sellPrices")) {
                // Migrate old format: copy itemPrices to sellPrices
                SavsCommonEconomy.LOGGER.info("Migrating legacy worth.json: renaming 'itemPrices' to 'sellPrices'...");
                rawJson.add("sellPrices", rawJson.get("itemPrices"));
                rawJson.remove("itemPrices");
                
                // Parse the migrated JSON
                worthConfig = GSON.fromJson(rawJson, WorthConfig.class);
                if (worthConfig == null) {
                    worthConfig = new WorthConfig();
                }
                // Save the migrated file
                saveWorth();
                SavsCommonEconomy.LOGGER.info("Successfully migrated worth.json to new format.");
            } else {
                worthConfig = GSON.fromJson(rawJson, WorthConfig.class);
                if (worthConfig == null) {
                    worthConfig = new WorthConfig();
                    saveWorth();
                }
            }
            
            SavsCommonEconomy.LOGGER.info("Successfully loaded worth.json.");
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to load worth.json!", e);
        }
    }

    private static void saveWorth() {
        try {
            File dir = CONFIG_DIR.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(WORTH_FILE)) {
                GSON.toJson(worthConfig, writer);
                SavsCommonEconomy.LOGGER.info("Successfully saved worth.json.");
            }
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to save worth.json!", e);
        }
    }

    /**
     * @return The active configuration instance.
     */
    public static EconomyConfig getConfig() {
        return currentConfig;
    }

    /**
     * @return The active worth configuration instance.
     */
    public static WorthConfig getWorth() {
        return worthConfig;
    }
}

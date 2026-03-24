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
    
    private static EconomyConfig currentConfig = new EconomyConfig();

    /**
     * Loads the config from disk, or saves default if it doesn't exist.
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Save defaults
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            currentConfig = GSON.fromJson(reader, EconomyConfig.class);
            if (currentConfig == null) {
                currentConfig = new EconomyConfig();
                save();
            }
            SavsCommonEconomy.LOGGER.info("Successfully loaded configuration.");
        } catch (IOException e) {
            SavsCommonEconomy.LOGGER.error("Failed to load configuration!", e);
        }
    }

    /**
     * Saves the current config to disk.
     */
    public static void save() {
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

    /**
     * @return The active configuration instance.
     */
    public static EconomyConfig getConfig() {
        return currentConfig;
    }
}

package savage.commoneconomy.util;

import net.fabricmc.loader.api.FabricLoader;
import savage.commoneconomy.SavsCommonEconomy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple thread-safe logger for economy transactions.
 */
public class TransactionLogger {
    private static final File LOG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("savs-common-economy").resolve("transactions.log").toFile();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Logs a transaction asynchronously to avoid blocking the main server thread.
     */
    public static void log(String type, String sender, String receiver, BigDecimal amount, String reason) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [%s] %s -> %s: %s (Reason: %s)\n",
                timestamp, type, sender, receiver, amount.toString(), reason);

        EXECUTOR.execute(() -> {
            try {
                File parent = LOG_FILE.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    writer.write(logEntry);
                }
            } catch (IOException e) {
                SavsCommonEconomy.LOGGER.error("Failed to write to transaction log!", e);
            }
        });
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}

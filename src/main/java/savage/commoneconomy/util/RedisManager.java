package savage.commoneconomy.util;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import savage.commoneconomy.EconomyManager;
import savage.commoneconomy.SavsCommonEconomy;
import savage.commoneconomy.config.ConfigManager;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages Redis Pub/Sub for cross-server balance synchronization.
 */
public class RedisManager {

    private static RedisManager instance;
    private RedisClient client;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final String channel;

    private RedisManager() {
        var config = ConfigManager.getConfig().redis;
        this.channel = config.channel;
        
        if (config.enabled) {
            init(config);
        }
    }

    public static RedisManager getInstance() {
        if (instance == null) {
            instance = new RedisManager();
        }
        return instance;
    }

    private void init(savage.commoneconomy.config.EconomyConfig.RedisConfig config) {
        try {
            RedisURI uri = RedisURI.Builder.redis(config.host, config.port)
                    .withPassword(config.password.toCharArray())
                    .withTimeout(Duration.ofMillis(config.timeout_ms))
                    .withClientName(config.client_name)
                    .build();

            this.client = RedisClient.create(uri);
            this.pubSubConnection = client.connectPubSub();

            pubSubConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    if (channel.equals(RedisManager.this.channel)) {
                        handleMessage(message);
                    }
                }
            });

            pubSubConnection.sync().subscribe(channel);
            SavsCommonEconomy.LOGGER.info("Connected to Redis for balance synchronization.");
        } catch (Exception e) {
            SavsCommonEconomy.LOGGER.error("Failed to connect to Redis!", e);
        }
    }

    /**
     * Publishes a balance update to other servers.
     */
    public void publishUpdate(UUID uuid) {
        if (pubSubConnection != null && pubSubConnection.isOpen()) {
            // Format: UPDATE:<uuid>
            pubSubConnection.async().publish(channel, "UPDATE:" + uuid.toString());
        }
    }

    /**
     * Handles an incoming balance update message from another server.
     */
    private void handleMessage(String message) {
        if (message.startsWith("UPDATE:")) {
            String uuidStr = message.substring(7);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                // Invalidate the local cache to force a reload from storage on next access
                EconomyManager.getInstance().invalidateCache(uuid);
                if (ConfigManager.getConfig().redis.debugLogging) {
                    SavsCommonEconomy.LOGGER.info("Redis: Invalidated cache for " + uuid);
                }
            } catch (IllegalArgumentException e) {
                SavsCommonEconomy.LOGGER.error("Received malformed Redis message: " + message);
            }
        }
    }

    public void shutdown() {
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }
}

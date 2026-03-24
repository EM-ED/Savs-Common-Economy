package savage.commoneconomy;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import savage.commoneconomy.command.AdminEconomyCommands;
import savage.commoneconomy.command.EconomyCommands;
import savage.commoneconomy.config.ConfigManager;
import savage.commoneconomy.EconomyManager;
import savage.commoneconomy.util.TransactionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SavsCommonEconomy implements ModInitializer {
	public static final String MOD_ID = "savs-common-economy";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Savs Common Economy is initializing for Minecraft 26.1...");
		
		// Load Configuration
		ConfigManager.load();

		// Register Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			EconomyCommands.register(dispatcher);
			AdminEconomyCommands.register(dispatcher);
			savage.commoneconomy.command.LogCommand.register(dispatcher);
		});

		// Register Player Join Hook
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			EconomyManager.getInstance().getOrCreateAccount(handler.getPlayer().getUUID(), handler.getPlayer().getName().getString());
		});

		// Register Shutdown Hook
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Savs Common Economy is shutting down...");
			EconomyManager.getInstance().shutdown();
			TransactionLogger.shutdown();
		});

		// Listeners
		savage.commoneconomy.listener.BankNoteListener.register();
	}
}
package za.dats.bukkit.memorystone;

import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 *
 * @author krinsdeath
 */

public class Utility {
	private final static Pattern COLOR = Pattern.compile("&([a-fA-F0-9])");

	private static Configuration LOCALE;

	private static MemoryStonePlugin plugin;

	public static void init(MemoryStonePlugin inst) {
		plugin = inst;
		configure();
	}

	public static boolean checkPermission(Player p, String field, String key) {
		if (p.getName().equalsIgnoreCase(key) && p.hasPermission("chestsync." + field + ".self")) {
			return true;
		}
		if (p.hasPermission("chestsync." + field + "." + key)) {
			return true;
		}
		if (p.hasPermission("chestsync." + field + ".*")) {
			return true;
		}
		return false;
	}

	public static void success(Player player, String key) {
		String msg = LOCALE.getString("success." + key);
		if (msg != null) {
			msg = color(msg);
			player.sendMessage(msg);
		}
	}

	public static void error(Player player, String key) {
		String msg = LOCALE.getString("error." + key);
		if (msg != null) {
			msg = color(msg);
			player.sendMessage(msg);
		}
	}

	private static void configure() {
		LOCALE = plugin.getConfiguration();
		if (LOCALE.getBoolean("plugin.built", false)) {
			LOCALE.setProperty("plugin.built", true);
			LOCALE.setProperty("success.new_chest", "&7This chest is now synced.");
			LOCALE.setProperty("success.destroyed", "&CSynced Chest destroyed.");
			LOCALE.setProperty("error.permission", "&CYou do not have permission for that.");
			LOCALE.setProperty("error.no_chest", "&CThere is no chest to sync at this location.");
			LOCALE.setProperty("error.duplicate_chest", "&CThere is already a chest at this location.");
			LOCALE.save();
		}
	}

	public static String color(String msg) {
		return COLOR.matcher(msg).replaceAll("\u00A7$1");
	}
}

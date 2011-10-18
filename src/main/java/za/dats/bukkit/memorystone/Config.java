package za.dats.bukkit.memorystone;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class Config {
    private final static String configFile = "configuration.yml";
    private static Configuration conf;

    public enum MemoryEffect {
	LIGHTNING_ON_CREATE("effects.lightningOnCreate"), LIGHTNING_ON_BREAK("effects.lightningOnBreak"), LIGHTNING_ON_TELEPORT_SOURCE(
		"effects.lightningOnTeleportSource"), LIGHTNING_ON_TELEPORT_DEST(
		"effects.lightningOnTeleportDestination");

	private final String effectConf;

	private MemoryEffect(String effectConf) {
	    this.effectConf = effectConf;
	}

    }

    public static void init(JavaPlugin plugin) {
	File file = new File(plugin.getDataFolder(), configFile);
	conf = new Configuration(file);
	if (file.exists()) {
	    conf.load();
	}

	// Make sure we add new configuration options.
	boolean changed = setDefaults();
	if (!file.exists() || changed) {
	    conf.save();
	}
    }

    private static boolean setDefaults() {
	HashMap<String, Object> defaults = new HashMap<String, Object>();

	defaults.put("pointCompassOnly", false);
	defaults.put("compassToUnmemorizedStoneDistance", 32);
	defaults.put("ignoreStructure", false);
	defaults.put("teleportItem", "COMPASS");
	defaults.put("maxUsesPerItem", 50);
	defaults.put("cooldownTime", 10);
	defaults.put("fizzleCooldownTime", 5);
	defaults.put("castingTime", 3);
	defaults.put("sortByDistance", true);
	defaults.put("minProximityToStoneForTeleport", 0);
	defaults.put("automaticMemorizationDistance", 0);

	defaults.put("stonetostone.enabled", "true");
	defaults.put("stonetostone.item", "glowstone_dust");
	defaults.put("stonetostone.maxUses", 10);

	defaults.put("economy.enabled", true);
	defaults.put("economy.ownerGetsPaid", true);
	defaults.put("economy.addCustomValue", true);

	defaults.put("effects.lightningOnCreate", true);
	defaults.put("effects.lightningOnBreak", true);
	defaults.put("effects.lightningOnTeleportSource", true);
	defaults.put("effects.lightningOnTeleportDestination", true);

	defaults.put("teleportKey", "C");

	defaults.put("lang.createConfirm", "&EBuilt <name>!");
	defaults.put("lang.destroyed", "&EDestroyed Memory Stone Structure!");
	defaults.put("lang.signAdded", "&EMemory Stone created.");
	defaults.put("lang.destroyForgotten", "Memory stone: <name> has been destroyed and forgotten.");
	defaults.put("lang.memorize", "Memorized: <name>");
	defaults.put("lang.alreadymemorized", "You have already memorized: <name>");
	defaults.put("lang.notfound", "<name> could not be found");
	defaults.put("lang.cooldown", "Teleport cooling down (<left>s)");
	defaults.put("lang.startrecall", "Starting recall to <name>");
	defaults.put("lang.cancelled", "Recall cancelled");
	defaults.put("lang.chargesleft", "You have <numcharges> left on your <material>");
	defaults.put("lang.consumed", "You have worn your <material> out!");
	defaults.put("lang.teleportingother", "Teleporting <name> to <destination>");
	defaults.put("lang.teleportedbyother", "<name> is teleporting you to <destination>");
	defaults.put("lang.teleporting", "Teleporting to <destination>");
	defaults.put("lang.noteleportzone", "You are in a no teleport zone. Cannot teleport out.");
	defaults.put("lang.teleportitemnotfound", "You need to have a <material> to teleport");

	defaults.put("lang.stonetostone.itemmissing", "You need some <material> to teleport from this memory stone");

	defaults.put("lang.nobuildpermission", "&EYou do not have permission to build memory stones.");
	defaults.put("lang.nobreakpermission", "&EYou do not have permission to break memory stones.");
	defaults.put("lang.selectlocation", "Select a location to teleport to");
	defaults.put("lang.selectotherlocation", "Select a location to teleport <name> to");

	defaults.put("lang.select", "Selecting destination as <name>");
	defaults.put("lang.selectwithcost", "Selecting destination as <name> with a cost of <cost>");
	defaults.put("lang.notexist", "<name> no longer exists as a destination");
	defaults.put("lang.notmemorized", "No Memorized recalling");
	defaults.put("lang.signboard", "&AMemory Stone");
	defaults.put("lang.broken", "&C[Broken]");
	defaults.put("lang.duplicate", "&C[Duplicate]");

	defaults.put("lang.outsideproximity", "You are not close enough to a memory stone to teleport.");
	defaults.put("lang.insidememorizationdistance", "As you approach <name> you take note of its location.");
	// not used yet. needs thinking about how to use this message without spamming
	// defaults.put("lang.insidememorizationdistancenotfree",
	// "You notice that <name> has a memorization cost of  : <cost>");

	defaults.put("lang.cantaffordbuild", "Cannot afford to build this structure at cost of : <cost>");
	defaults.put("lang.cantaffordmemorize", "Cannot afford to memorize <name> at cost of : <cost>");
	defaults.put("lang.cantaffordteleport", "Cannot afford to teleport to <name> with cost of : <cost>");
	defaults.put("lang.compassinterference", "Something strange is happening with your compass");
	defaults.put("lang.compasslostinterference", "Your compass returns to normal");
	boolean changed = false;
	for (String key : defaults.keySet()) {
	    if (conf.getProperty(key) == null) {
		changed = true;
		conf.setProperty(key, defaults.get(key));
	    }
	}

	return changed;
    }

    public static String getLang(String key) {
	return conf.getString("lang." + key);
    }

    public static String getColorLang(String langKey, String... keyMap) {
	String result = getLang(langKey);
	if (result == null) {
	    result = langKey;
	}
	for (int i = 0; i < keyMap.length; i += 2) {
	    if (i + 1 >= keyMap.length) {
		break;
	    }

	    String key = keyMap[i];
	    String value = keyMap[i + 1];

	    result = result.replaceAll("[<]" + key + "[>]", value);
	}

	return Utility.color(result);
    }

    public static int getMaxUsesPerItem() {
	return conf.getInt("maxUsesPerItem", 0);
    }

    public static boolean isEffectEnabled(MemoryEffect effect) {
	return conf.getBoolean(effect.effectConf, true);
    }

    public static Material getTeleportItem() {
	String materialString = conf.getString("teleportItem");
	try {
	    Integer typeId = Integer.parseInt(materialString);
	    return Material.getMaterial(typeId);
	} catch (NumberFormatException e) {
	}

	return Material.getMaterial(materialString);
    }

    public static int getCooldownTime() {
	return conf.getInt("cooldownTime", 10);
    }

    public static int getFizzleCooldownTime() {
	return conf.getInt("fizzleCooldownTime", 5);
    }

    public static int getCastingTime() {
	return conf.getInt("castingTime", 3);
    }

    public static boolean isSortByDistance() {
	return conf.getBoolean("sortByDistance", true);
    }

    public static boolean isEconomyEnabled() {
	return conf.getBoolean("economy.enabled", true);
    }

    public static boolean isEconomyOwnerPaid() {
	return conf.getBoolean("economy.ownerIsPaid", true);
    }

    public static boolean isEconomyAddCustomValue() {
	return conf.getBoolean("economy.addCustomValue", true);
    }

    public static int getMinProximityToStoneForTeleport() {
	return conf.getInt("minProximityToStoneForTeleport", 0);
    }

    public static int getAutomaticMemorizationDistanceSquared() {
	int temp = conf.getInt("automaticMemorizationDistance", 0);
	temp = temp * temp;
	return temp;
    }

    public static String getTeleportKey() {
	return conf.getString("teleportKey", "C");
    }

    public static boolean isStoneToStoneEnabled() {
	return conf.getBoolean("stonetostone.enabled", true);
    }

    public static Material getStoneToStoneItem() {
	String materialString = conf.getString("stonetostone.item");
	try {
	    Integer typeId = Integer.parseInt(materialString);
	    return Material.getMaterial(typeId);
	} catch (NumberFormatException e) {
	}

	return Material.getMaterial(materialString);
    }

    public static int getStoneToStoneMaxUses() {
	return conf.getInt("stonetostone.maxUses", 10);
    }

    public static boolean isPointCompassOnly() {
	return conf.getBoolean("pointCompassOnly", false);
    }

    public static int getCompassToUnmemorizedStoneDistanceSquared() {
	int temp = conf.getInt("compassToUnmemorizedStoneDistance", 32);
	temp = temp * temp;
	return temp;
    }

    public static boolean isIgnoreStructure() {
	return conf.getBoolean("ignoreStructure", true);
    }
}

package za.dats.bukkit.memorystone;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class Config {
    private final static String configFile = "configuration.yml";
    private static Configuration conf;
    
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
	
	
	defaults.put("teleportItem", "COMPASS");
	defaults.put("maxUsesPerItem", 50);
	defaults.put("lightningEffect", true);
	defaults.put("cooldownTime", 10);
	defaults.put("fizzleCooldownTime", 5);
	
	defaults.put("lang.createConfirm", "&EBuilt Memory Stone!");
	defaults.put("lang.destroyed", "&EDestroyed Memory Stone!");
	defaults.put("lang.signAdded", "&EMemory Stone created.");
	defaults.put("lang.destroyForgotten", "Memory stone: <name> has been destroyed and forgotten.");
	defaults.put("lang.memorize", "Memorized: <name>");
	defaults.put("lang.notfound", "<name> could not be found");
	defaults.put("lang.cooldown", "Teleport cooling down (<left>s)");
	defaults.put("lang.startrecall", "Starting recall to <name>");
	defaults.put("lang.cancelled", "Recall cancelled");
	defaults.put("lang.chargesleft", "You have <numcharges> left");
	
	defaults.put("lang.nobuildpermission", "&EYou do not have permission to build memory stones.");
	defaults.put("lang.nobreakpermission", "&EYou do not have permission to break memory stones.");
	
	defaults.put("lang.select", "Selecting destination as <name>");
	defaults.put("lang.notexist", "<name> no longer exists as a destination");
	defaults.put("lang.notmemorized", "No Memorized recalling");
	defaults.put("lang.signboard", "&AMemory Stone");
	defaults.put("lang.broken", "&C[Broken]");
	defaults.put("lang.duplicate", "&C[Duplicate]");
	
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
	return conf.getString("lang."+key);
    }
    
    public static String getColorLang(String langKey, String ... keyMap) {
	String result = getLang(langKey);
	for (int i = 0; i < keyMap.length; i += 2) {
	    if (i+1 >= keyMap.length) {
		break;
	    }
	    
	    String key = keyMap[i];
	    String value = keyMap[i+1];
	    
	    result = result.replaceAll("[<]"+key+"[>]", value);
	}
	
	return Utility.color(result);
    }

    public static int getMaxUsesPerItem() {
	return conf.getInt("maxUsesPerItem", 0);
    }
    
    public static boolean useLightning() {
	return conf.getBoolean("lightningEffect", true);
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
}

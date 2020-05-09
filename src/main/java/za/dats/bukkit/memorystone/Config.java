package za.dats.bukkit.memorystone;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.Configuration;

public class Config {
    private final static String configFile = "configuration.yml";
    private static YamlConfiguration conf;

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
        conf = YamlConfiguration.loadConfiguration(file);

        // Make sure we add new configuration options.
        boolean changed = setDefaults();
        if (!file.exists() || changed) {
            try {
                conf.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean setDefaults() {
        HashMap<String, Object> defaults = new HashMap<String, Object>();

        defaults.put("pointCompassOnly", false);
        defaults.put("compassToUnmemorizedStoneDistance", 128);
        defaults.put("teleportItem", "COMPASS");
        defaults.put("reagentItem", "REDSTONE");
        defaults.put("cooldownTime", 10);
        defaults.put("fizzleCooldownTime", 5);
        defaults.put("castingTime", 3);
        defaults.put("sortByDistance", true);

        // TODO: Considering adding support back in for this.. not sure.
        defaults.put("minProximityToStoneForTeleport", 0);

        defaults.put("effects.lightningOnCreate", true);
        defaults.put("effects.lightningOnBreak", true);
        defaults.put("effects.lightningOnTeleportSource", true);
        defaults.put("effects.lightningOnTeleportDestination", true);

        defaults.put("lang.createConfirm", "&EBuilt <name>!");
        defaults.put("lang.destroyed", "&EDestroyed Memory Stone Structure!");
        defaults.put("lang.signAdded", "&EMemory Stone created.");
        defaults.put("lang.destroyForgotten", "Memory stone: <name> has been destroyed and forgotten.");
        defaults.put("lang.memorize", "Memorized: <name>");
        defaults.put("lang.alreadymemorized", "You have already memorized: <name>");
        defaults.put("lang.notfound", "<name> could not be found");
        defaults.put("lang.notcrossworld", "<name> in a different world, and not a crossworld memory stone.");
        defaults.put("lang.cooldown", "Teleport cooling down (<left>s)");
        defaults.put("lang.startrecall", "Starting recall to <name>");
        defaults.put("lang.cancelled", "Recall cancelled");
        defaults.put("lang.noreagent", "You need a <material> to teleport!");
        defaults.put("lang.teleportingother", "Teleporting <name> to <destination>");
        defaults.put("lang.teleportedbyother", "<name> is teleporting you to <destination>");
        defaults.put("lang.teleporting", "Teleporting to <destination>");
        defaults.put("lang.teleportitemnotfound", "You need to have a <material> to teleport");

        defaults.put("lang.nobuildpermission", "&EYou do not have permission to build memory stones.");
        defaults.put("lang.nobreakpermission", "&EYou do not have permission to break memory stones.");
        defaults.put("lang.selectlocation", "Select a location to teleport to");
        defaults.put("lang.selectotherlocation", "Select a location to teleport <name> to");

        defaults.put("lang.select", "Selecting destination as <name>");
        defaults.put("lang.selectwithcost", "Selecting destination as <name> with a cost of <cost>");
        defaults.put("lang.notexist", "<name> no longer exists as a destination");
        defaults.put("lang.notmemorized", "No Memorized recalling");
        defaults.put("lang.signboard", "&AMemory Stone");
        defaults.put("lang.broken-noname", "&C[No Name]");
        defaults.put("lang.broken", "&C[Broken]");
        defaults.put("lang.duplicate", "&C[Duplicate]");

        defaults.put("lang.compassinterference", "Something strange is happening with your compass");
        defaults.put("lang.compasslostinterference", "Your compass returns to normal");
        boolean changed = false;
        for (String key : defaults.keySet()) {
            if (conf.get(key) == null) {
                changed = true;
                conf.set(key, defaults.get(key));
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

    public static boolean isEffectEnabled(MemoryEffect effect) {
        return conf.getBoolean(effect.effectConf, true);
    }

    public static Material getTeleportItem() {
        String materialString = conf.getString("teleportItem");

        if (materialString == null) { return null; }
        return Material.getMaterial(materialString);
    }

    public static Material getReagentItem() {
        String materialString = conf.getString("reagentItem");

        if (materialString == null) { return null; }
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

    public static int getMinProximityToStoneForTeleport() {
        return conf.getInt("minProximityToStoneForTeleport", 0);
    }

    public static boolean isPointCompassOnly() {
        return conf.getBoolean("pointCompassOnly", false);
    }

    public static int getCompassToUnmemorizedStoneDistanceSquared() {
        int temp = conf.getInt("compassToUnmemorizedStoneDistance", 32);
        temp = temp * temp;
        return temp;
    }
}

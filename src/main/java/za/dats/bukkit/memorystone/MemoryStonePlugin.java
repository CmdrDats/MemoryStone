package za.dats.bukkit.memorystone;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import za.dats.bukkit.memorystone.util.StructureManager;

public class MemoryStonePlugin extends JavaPlugin {
    private PluginDescriptionFile pdf;
    private PluginManager pm;
    private StructureManager structureManager = new StructureManager(this, "[MemoryStone] ");
    private MemoryStoneManager memoryStoneManager = new MemoryStoneManager(this);
    private CompassManager compassManager = new CompassManager(this);
    private static MemoryStonePlugin instance;

    public void onDisable() {
    }

    public void info(String log) {
        getServer().getLogger().info("[MemoryStone] " + log);
    }

    public void warn(String log) {
        getServer().getLogger().warning("[MemoryStone] " + log);
    }


    public void onEnable() {
        instance = this;
        Config.init(this);

        pm = getServer().getPluginManager();
        pdf = getDescription();

        info(pdf.getName() + " version " + pdf.getVersion() + " is enabled!");

        structureManager.addStructureListener(memoryStoneManager);
        structureManager.registerEvents();

        memoryStoneManager.registerEvents();
        compassManager.registerEvents();
    }

    public boolean isSpoutEnabled() {
        if (pm.isPluginEnabled("Spout")) {
            return true;
        }
        return false;
    }

    public StructureManager getStructureManager() {
        return structureManager;
    }

    public MemoryStoneManager getMemoryStoneManager() {
        return memoryStoneManager;
    }

    public CompassManager getCompassManager() {
        return compassManager;
    }

    public static MemoryStonePlugin getInstance() {
        return instance;
    }
}

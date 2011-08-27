package za.dats.bukkit.memorystone;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import za.dats.bukkit.memorystone.util.StructureManager;

public class MemoryStonePlugin extends JavaPlugin {
    private PluginDescriptionFile pdf;
    private PluginManager pm;
    StructureManager structureManager = new StructureManager(this);
    MemoryStoneManager memoryStoneManager = new MemoryStoneManager(this);
    CompassManager compassManager = new CompassManager(this);

    public void onDisable() {
    }

    public void onEnable() {
	Utility.init(this);
	pm = getServer().getPluginManager();
	pdf = getDescription();

	System.out.println(pdf.getName() + " version " + pdf.getVersion() + " is enabled!");
	
	structureManager.addStructureListener(memoryStoneManager);
	structureManager.registerEvents();
	
	memoryStoneManager.registerEvents();
	compassManager.registerEvents();

    }

    public boolean hasSpout() {
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
}

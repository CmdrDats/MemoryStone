package za.dats.bukkit.memorystone;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import za.dats.bukkit.memorystone.commands.EchoCommand;
import za.dats.bukkit.memorystone.util.StructureManager;

public class MemoryStonePlugin extends JavaPlugin {
    private PluginDescriptionFile pdf;
    private PluginManager pm;
    StructureManager structureManager = new StructureManager(this);
    MemoryStoneManager memoryStoneManager = new MemoryStoneManager(this);

    public void onDisable() {
    }

    public void onEnable() {
	Utility.init(this);
	pm = getServer().getPluginManager();
	pdf = getDescription();

	System.out.println(pdf.getName() + " version " + pdf.getVersion() + " is enabled!");
	getCommand("echo").setExecutor(new EchoCommand(this));
	
	structureManager.addStructureListener(memoryStoneManager);
	structureManager.registerEvents();
	
	memoryStoneManager.registerEvents();
	

    }

    public boolean hasSpout() {
	if (getServer().getPluginManager().isPluginEnabled("Spout")) {
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

    
}

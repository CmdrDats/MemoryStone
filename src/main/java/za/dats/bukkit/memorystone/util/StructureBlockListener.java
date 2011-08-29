package za.dats.bukkit.memorystone.util;

import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

/**
 * @author tim (originally HTBlockListener)
 * @author cmdrdats
 * 
 */
public class StructureBlockListener extends BlockListener {

    private final JavaPlugin plugin;
    private final StructureManager structureManager;

    public StructureBlockListener(JavaPlugin plugin, StructureManager structureManager) {
	this.plugin = plugin;
	this.structureManager = structureManager;
    }

    public void registerEvents() {
	PluginManager pm = this.plugin.getServer().getPluginManager();
	pm.registerEvent(Event.Type.BLOCK_PLACE, this, Event.Priority.Normal, this.plugin);
	pm.registerEvent(Event.Type.BLOCK_BREAK, this, Event.Priority.High, this.plugin);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
	if (event.isCancelled())
	    return;

	String owner = event.getPlayer().getName();

	Block placedblock = event.getBlockPlaced();
	List<StructureType> totemtypes = structureManager.getStructureTypes();

	TOTEMBUILD: 
	for (StructureType totemtype : totemtypes) {

	    Structure totem = new Structure(totemtype, placedblock, owner);
	    if (!totem.verifyStructure()) {
		continue;
	    }
	    
	    // check permissions!
	    Player player = event.getPlayer();
	    /*
	     * if (!player.hasPermission("healingtotem.build")) { event.setCancelled(true);
	     * player.sendMessage(ChatColor.RED + "You do not have permission to build totems."); return; }
	     */

	    // check the number of totems
	    /*
	     * Set<Structure> totemset = structureManager.getStructuresFromPlayer(player); if (totemset != null &&
	     * totemset.size() >= this.plugin.getConfigManager().getStructuresPerPlayer() &&
	     * !player.hasPermission("healingtotem.unlimitedbuild")) { event.setCancelled(true);
	     * player.sendMessage(ChatColor.RED + "You have reached the maximum number of totems you can build.");
	     * return; }
	     */

	    for (Block block : totem.getBlocks()) {
		if (structureManager.getStructuresFromBlock(block) != null) {
		    break TOTEMBUILD;
		}
	    }

	    // lightning strike!
	    // if (this.plugin.getConfigManager().isLightning()) {
	    placedblock.getWorld().strikeLightningEffect(placedblock.getLocation());
	    // }

	    structureManager.addStructure(event, totem);
	    structureManager.saveStructures();

	}
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.isCancelled())
	    return;

	Block brokenblock = event.getBlock();
	Set<Structure> totems = structureManager.getStructuresFromBlock(brokenblock);

	if (totems == null)
	    return;

	Player player = event.getPlayer();
	
	// check permissions!
	/*
	if (!player.hasPermission("healingtotem.break")) {
	    event.setCancelled(true);
	    player.sendMessage(ChatColor.RED + "You do not have permission to break structures.");
	    return;
	}
	*/

	// lightning strike!
	//if (this.plugin.getConfigManager().isLightning()) {
	    brokenblock.getWorld().strikeLightningEffect(brokenblock.getLocation());
	//}

	for (Structure structure : totems) {
	    // TODO add REPLACE code?
	    structureManager.removeStructure(event, structure);
	    structureManager.saveStructures();
	}

	//if (!this.plugin.getConfigManager().isQuiet()) {
	//}
    }
}

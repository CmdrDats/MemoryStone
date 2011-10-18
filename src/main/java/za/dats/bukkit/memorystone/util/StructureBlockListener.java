package za.dats.bukkit.memorystone.util;

import java.util.List;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import za.dats.bukkit.memorystone.Config;
import za.dats.bukkit.memorystone.Config.MemoryEffect;
import za.dats.bukkit.memorystone.MemoryStonePlugin;
import za.dats.bukkit.memorystone.economy.EconomyManager;
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
	pm.registerEvent(Event.Type.ENTITY_EXPLODE, new EntityListener() {
	    @Override
	    public void onEntityExplode(EntityExplodeEvent event) {
		StructureBlockListener.this.onEntityExplode(event);
	    }
	}, Event.Priority.High, plugin);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
	if (event.isCancelled()) {
	    return;
	}

	Structure block = checkPlacedBlock(event.getPlayer(), event.getBlock(), event);
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
	if (!player.hasPermission("memorystone.break")) {
	    event.setCancelled(true);
	    player.sendMessage(Config.getColorLang("nobreakpermission"));
	    return;
	}

	// lightning strike!
	if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_BREAK)) {
	    brokenblock.getWorld().strikeLightningEffect(brokenblock.getLocation());
	}

	for (Structure structure : totems) {
	    // TODO add REPLACE code?
	    structureManager.removeStructure(event, structure);
	    structureManager.saveStructures();
	}

	// if (!this.plugin.getConfigManager().isQuiet()) {
	// }
    }

    protected void onEntityExplode(EntityExplodeEvent event) {
	if (event.isCancelled()) {
	    return;
	}

	for (Block brokenBlock : event.blockList()) {
	    Set<Structure> totems = structureManager.getStructuresFromBlock(brokenBlock);
	    if (totems != null) {
		event.setCancelled(true);
		return;
	    }
	}
    }

    public Structure checkPlacedBlock(Player player, Block behind, BlockPlaceEvent event) {
	String owner = player.getName();

	Block placedblock = behind;
	List<StructureType> structureTypes = structureManager.getStructureTypes();

	TOTEMBUILD: for (StructureType structureType : structureTypes) {

	    Structure structure = new Structure(structureType, placedblock, owner);
	    if (!structure.verifyStructure()) {
		continue;
	    }

	    // check permissions!

	    if (!player.hasPermission("memorystone.build")) {
		player.sendMessage(Config.getColorLang("nobuildpermission"));
		if (event != null) {
		    event.setCancelled(true);
		}
		return null;
	    }

	    if (structureType.getPermissionRequired() != null && structureType.getPermissionRequired().length() > 0) {
		if (!player.hasPermission(structureType.getPermissionRequired())) {
		    player.sendMessage(Config.getColorLang("nobuildpermission"));
		    if (event != null) {
			event.setCancelled(true);
		    }
		    return null;
		}
	    }

	    // check the number of totems
	    /*
	     * Set<Structure> totemset = structureManager.getStructuresFromPlayer(player); if (totemset != null &&
	     * totemset.size() >= this.plugin.getConfigManager().getStructuresPerPlayer() &&
	     * !player.hasPermission("healingtotem.unlimitedbuild")) { event.setCancelled(true);
	     * player.sendMessage(ChatColor.RED + "You have reached the maximum number of totems you can build.");
	     * return; }
	     */

	    for (Block block : structure.getBlocks()) {
		if (structureManager.getStructuresFromBlock(block) != null) {
		    break TOTEMBUILD;
		}
	    }

	    EconomyManager economyManager = MemoryStonePlugin.getInstance().getEconomyManager();
	    if (economyManager.isEconomyEnabled() && !player.hasPermission("memorystone.usefree")) {
		if (!economyManager.payBuildCost(player, structureType)) {
		    player.sendMessage(Config.getColorLang("cantaffordbuild", "cost",
			    economyManager.getBuildCostString(structureType)));
		    if (event != null) {
			event.setCancelled(true);
		    }
		    return null;
		}
	    }

	    // lightning strike!
	    if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_CREATE)) {
		placedblock.getWorld().strikeLightningEffect(placedblock.getLocation());
	    }

	    structure.setOwner(player.getName());
	    structureManager.addStructure(player, structure);
	    structureManager.saveStructures();
	    return structure;
	}

	return null;
    }

}

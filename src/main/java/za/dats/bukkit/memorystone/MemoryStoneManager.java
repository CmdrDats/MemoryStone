package za.dats.bukkit.memorystone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.memorystone.util.StructureListener;
import za.dats.bukkit.memorystone.util.structure.Rotator;
import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class MemoryStoneManager extends BlockListener implements StructureListener {
    private final MemoryStonePlugin memoryStonePlugin;
    private HashMap<Structure, MemoryStone> structureMap = new HashMap<Structure, MemoryStone>();
    private HashMap<String, MemoryStone> namedMap = new HashMap<String, MemoryStone>();

    public MemoryStoneManager(MemoryStonePlugin memoryStonePlugin) {
	this.memoryStonePlugin = memoryStonePlugin;
    }

    public void registerEvents() {
	PluginManager pm;
	pm = memoryStonePlugin.getServer().getPluginManager();
	pm.registerEvent(Event.Type.SIGN_CHANGE, this, Event.Priority.Normal, memoryStonePlugin);
	pm.registerEvent(Event.Type.BLOCK_BREAK, this, Event.Priority.Normal, memoryStonePlugin);

    }

    public void structurePlaced(BlockPlaceEvent event, Structure structure) {
	MemoryStone stone = new MemoryStone();
	stone.setStructure(structure);
	structureMap.put(structure, stone);
	event.getPlayer().sendMessage("Built Memory Stone!");
    }

    public void structureDestroyed(BlockBreakEvent event, Structure structure) {
	MemoryStone stone = structureMap.get(structure);
	Sign sign = stone.getSign();
	if (stone.getName() != null) {
	    namedMap.remove(stone.getName());
	    memoryStonePlugin.getCompassManager().forgetStone(stone.getName());
	}
	
	if (sign != null) {
	    BlockState state = new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
		    .getState();

	    if (state instanceof Sign) {
		Sign newSign = (Sign) new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
			.getState();
		newSign.setLine(0, Utility.color("&C") + "Memory Stone");
		newSign.setLine(1, Utility.color("&C[Broken]"));
		newSign.update(true);
	    }
	    
	    stone.setSign(null);
	}
	
	structureMap.remove(structure);
	event.getPlayer().sendMessage("Destroyed Memory Stone!");
    }

    public void structureLoaded(Structure structure, ConfigurationNode node) {
	System.out.println("Loading Memory Stone Structure");
	MemoryStone stone = new MemoryStone();
	stone.setStructure(structure);
	structureMap.put(structure, stone);

	stone.setName(node.getString("name", ""));
	if (stone.getName() != null && stone.getName().length() > 0) {
	    namedMap.put(stone.getName(), stone);
	}
	
	if (node.getProperty("signx") != null) {
	    Sign newSign = (Sign) new Location(structure.getWorld(), node.getInt("signx", 0), node.getInt("signy", 0),
		    node.getInt("signz", 0)).getBlock().getState();
	    stone.setSign(newSign);
	}
    }

    public void structureSaving(Structure structure, Map<String, Object> yamlMap) {
	MemoryStone memoryStone = structureMap.get(structure);
	
	yamlMap.put("name", memoryStone.getName());
	if (memoryStone.getSign() != null) {
	    Sign sign = memoryStone.getSign();
	    yamlMap.put("signx", sign.getX());
	    yamlMap.put("signy", sign.getY());
	    yamlMap.put("signz", sign.getZ());
	}
    }

    public void generatingDefaultStructureTypes(List<StructureType> types) {
	StructureType structuretype;
	StructureType.Prototype proto;

	proto = new StructureType.Prototype();
	for (int x=0; x<3; x++) {
	    for (int z=0; z<3; z++) {
		proto.addBlock(x, 0, z, Material.STONE);
	    }
	}
	proto.addBlock(1, 1, 1, Material.OBSIDIAN);
	proto.addBlock(1, 2, 1, Material.OBSIDIAN);
	proto.addBlock(1, 3, 1, Material.OBSIDIAN);
	proto.setName("Memory Stone");
	proto.setRotator(Rotator.NONE);
	structuretype = new StructureType(proto);

	types.add(structuretype);
    }

    public MemoryStone getMemoryStoneAtBlock(Block behind) {
	Set<Structure> structuresFromBlock = memoryStonePlugin.getStructureManager().getStructuresFromBlock(behind);
	if (structuresFromBlock == null || structuresFromBlock.size() != 1) {
	    return null;
	}

	for (Structure structure : structuresFromBlock) {
	    if (!structureMap.containsKey(structure)) {
		break;
	    }

	    MemoryStone result = structureMap.get(structure);
	    return result;
	}

	return null;
    }

    public MemoryStone getMemoryStructureBehind(Sign sign) {
	org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
	Block behind = sign.getBlock().getRelative(aSign.getFacing().getOppositeFace());

	MemoryStone stone = memoryStonePlugin.getMemoryStoneManager().getMemoryStoneAtBlock(behind);
	return stone;
    }

    public MemoryStone getNamedMemoryStone(String name) {
	return namedMap.get(name);
    }
    
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.isCancelled()) {
	    return;
	}

	if (event.getBlock().getState() instanceof Sign) {
	    final Sign state = (Sign) event.getBlock().getState();
	    final MemoryStone stone = getMemoryStructureBehind(state);
	    if (stone != null) {
		namedMap.remove(state.getLine(1));
		stone.setSign(null);
		memoryStonePlugin.getStructureManager().saveStructures();
	    }
	}
    }

    @Override
    public void onSignChange(final SignChangeEvent event) {
	if (event.isCancelled()) {
	    return;
	}

	final Sign state = (Sign) event.getBlock().getState();
	final MemoryStone stone = getMemoryStructureBehind(state);
	if (stone != null) {
	    if (stone.getSign() != null) {
		event.setLine(0, Utility.color("&C") + "Memory Stone");
		event.setLine(1, Utility.color("&C[Broken]"));
		return;
	    }

	    if (event.getLine(0).length() == 0) {
		event.setLine(0, Utility.color("&C") + "Memory Stone");
		event.setLine(1, Utility.color("&C[Broken]"));
		return;
	    }

	    String name = event.getLine(0);
	    if (namedMap.containsKey(name)) {
		event.setLine(0, Utility.color("&C") + "Memory Stone");
		event.setLine(1, Utility.color("&C[Duplicate]"));
		return;
	    }
	    
	    event.setLine(0, Utility.color("&A") + "Memory Stone");
	    event.setLine(1, name);
	    stone.setSign(state);
	    namedMap.put(name, stone);
	    memoryStonePlugin.getServer().getScheduler().scheduleSyncDelayedTask(memoryStonePlugin, new Runnable() {
		public void run() {
		    updateSign(state);
		    Sign newSign = (Sign) new Location(state.getWorld(), state.getX(), state.getY(), state.getZ())
			    .getBlock().getState();
		    stone.setSign(newSign);
		    memoryStonePlugin.getStructureManager().saveStructures();

		}
	    }, 2);
	    event.getPlayer().sendMessage(Utility.color("&EMemory Stone created."));
	}

	super.onSignChange(event);
    }

    public void updateSign(Sign s) {
	if (s.getType() != Material.WALL_SIGN) {
	    String name = s.getLine(1);
	    MaterialData m = s.getData();
	    BlockFace f = ((Directional) m).getFacing();
	    s.setType(Material.WALL_SIGN);
	    m = s.getData();
	    ((Directional) m).setFacingDirection(f);
	    s.setData(m);
	    s.update(true);

	    Sign newSign = (Sign) new Location(s.getWorld(), s.getX(), s.getY(), s.getZ()).getBlock().getState();
	    newSign.setLine(0, Utility.color("&A") + "Memory Stone");
	    newSign.setLine(1, name);
	    newSign.update(true);
	}

    }
}

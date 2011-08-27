package za.dats.bukkit.memorystone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.memorystone.listeners.MemoryStoneSignListener;
import za.dats.bukkit.memorystone.util.StructureListener;
import za.dats.bukkit.memorystone.util.structure.Rotator;
import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class MemoryStoneManager implements StructureListener {
    private MemoryStoneSignListener signListener;
    private final MemoryStonePlugin memoryStonePlugin;
    private static MemoryStone memorized;
    private HashMap<Structure, MemoryStone> structureMap = new HashMap<Structure, MemoryStone>();

    public MemoryStoneManager(MemoryStonePlugin memoryStonePlugin) {
	this.memoryStonePlugin = memoryStonePlugin;
    }

    public void registerEvents() {
	PluginManager pm;
	pm = memoryStonePlugin.getServer().getPluginManager();
	signListener = new MemoryStoneSignListener(memoryStonePlugin);
	pm.registerEvent(Event.Type.SIGN_CHANGE, signListener, Event.Priority.Normal, memoryStonePlugin);
	pm.registerEvent(Event.Type.BLOCK_BREAK, signListener, Event.Priority.Normal, memoryStonePlugin);
	pm.registerEvent(Event.Type.PLAYER_INTERACT, new PlayerListener() {
	    @Override
	    public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getPlayer().getItemInHand().getType() != Material.COMPASS) {
		    return;
		}

		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
		    if (event.getClickedBlock().getState() instanceof Sign) {
			Sign state = (Sign) event.getClickedBlock().getState();
			memorized = getMemoryStructureBehind(state);
			if (memorized != null && memorized.getSign() != null) {
			    event.getPlayer().sendMessage("Memorized " + state.getLine(1));
			}
			return;
		    }
		}

		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)
			|| event.getAction().equals(Action.LEFT_CLICK_AIR)) {

		    if (memorized != null) {
			Sign sign = (Sign) memorized.getSign();
			if (sign == null) {
			    return;
			}

			event.getPlayer().sendMessage("Recalling");
			org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
			Block infront = sign.getBlock().getRelative(aSign.getFacing());
			// new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
			event.getPlayer().teleport(infront.getLocation());
		    } else {
			event.getPlayer().sendMessage("No Memorized recalling");

		    }

		}

		super.onPlayerInteract(event);
	    }
	}, Priority.Normal, memoryStonePlugin);
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

	if (node.getProperty("signx") != null) {
	    Sign newSign = (Sign) new Location(structure.getWorld(), node.getInt("signx", 0), node.getInt("signy", 0),
		    node.getInt("signz", 0)).getBlock().getState();
	    stone.setSign(newSign);
	}
    }

    public void structureSaving(Structure structure, Map<String, Object> yamlMap) {
	MemoryStone memoryStone = structureMap.get(structure);

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
	proto.addBlock(0, 0, 0, Material.COBBLESTONE);
	proto.addBlock(0, 1, 0, Material.COBBLESTONE);
	proto.addBlock(0, 2, 0, Material.DIRT);
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

}

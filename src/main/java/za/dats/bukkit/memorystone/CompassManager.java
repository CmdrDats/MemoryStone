package za.dats.bukkit.memorystone;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.inventory.CraftingInventory;
import org.getspout.spoutapi.player.SpoutPlayer;

public class CompassManager extends PlayerListener {
    private static class Teleport {
	boolean started;
	boolean cancelled;
	int taskId = -1;
	long lastTeleportTime;
    }
    
    private final MemoryStonePlugin plugin;
    private final Map<String, Set<String>> memorized = new HashMap<String, Set<String>>();
    private final Map<String, String> selected = new HashMap<String, String>();
    private final Map<String, Teleport> teleporting = new HashMap<String, Teleport>();
    private final String locationsFile = "locations.yml";

    public CompassManager(MemoryStonePlugin plugin) {
	this.plugin = plugin;
    }

    public void forgetStone(String name) {
	for (String player : selected.keySet()) {
	    if (name.equals(selected.get(player))) {
		selected.put(player, null);
	    }
	}

	for (String player : memorized.keySet()) {
	    Set<String> list = memorized.get(player);
	    if (list != null && list.contains(name)) {
		list.remove(name);
		Player p = plugin.getServer().getPlayer(player);
		if (p != null) {
		    p.sendMessage("Memory stone: " + name + " has been destroyed and forgotten.");
		}
	    }
	}

	saveLocations();
    }

    public void registerEvents() {
	PluginManager pm;
	pm = plugin.getServer().getPluginManager();
	pm.registerEvent(Event.Type.PLAYER_INTERACT, this, Priority.Normal, plugin);
	pm.registerEvent(Event.Type.PLAYER_MOVE, this, Priority.Normal, plugin);
	loadLocations();
    }



    public boolean memorizeStone(PlayerInteractEvent event) {
	Sign state = (Sign) event.getClickedBlock().getState();
	MemoryStone stone = plugin.getMemoryStoneManager().getMemoryStructureBehind(state);

	if (stone != null && stone.getSign() != null) {
	    selected.put(event.getPlayer().getName(), stone.getName());
	    Set<String> set = memorized.get(event.getPlayer().getName());
	    if (set == null) {
		set = new TreeSet<String>();
		memorized.put(event.getPlayer().getName(), set);
	    }
	    set.add(stone.getName());

	    event.getPlayer().sendMessage("Memorized " + stone.getName());
	    saveLocations();
	    return true;
	}
	return false;
    }

    public void loadLocations() {
	File file = new File(this.plugin.getDataFolder(), this.locationsFile);
	if (!file.exists()) {
	    return;
	}
	Configuration conf = new Configuration(file);
	conf.load();

	Map<String, Set<String>> memLoad = (Map<String, Set<String>>) conf.getProperty("memorized");
	for (String player : memLoad.keySet()) {
	    memorized.put(player, memLoad.get(player));
	}

	Map<String, String> selLoad = (Map<String, String>) conf.getProperty("selected");
	for (String player : selLoad.keySet()) {
	    selected.put(player, selLoad.get(player));
	}

	System.out.println(conf.getProperty("memorized").getClass());
	System.out.println(conf.getProperty("selected").getClass());
    }

    public void saveLocations() {
	File file = new File(this.plugin.getDataFolder(), this.locationsFile);
	Configuration conf = new Configuration(file);

	conf.setProperty("memorized", memorized);
	conf.setProperty("selected", selected);
	conf.save();
    }
    
    public Teleport getTeleport(Player player) {
	Teleport result = teleporting.get(player.getName());
	if (result == null) {
	    result = new Teleport();
	    teleporting.put(player.getName(), result);
	}
	
	return result;
    }

    public void startTeleport(final MemoryStone stone, PlayerInteractEvent event) {
	final String name = selected.get(event.getPlayer().getName());
	final Sign sign = (Sign) stone.getSign();
	if (sign == null) {
	    event.getPlayer().sendMessage(name + " sign could not be found");
	    return;
	}
	
	long now = new Date().getTime();
	Teleport teleport = getTeleport(event.getPlayer());
	if (now - teleport.lastTeleportTime < 10000) {
	    event.getPlayer().sendMessage("Teleport cooling down ("+(10-(now-teleport.lastTeleportTime)/1000)+"s)");
	    return;
	}

	event.getPlayer().playEffect(event.getPlayer().getLocation(), Effect.SMOKE, 0);
	event.getPlayer().sendMessage("Starting recall to " + stone.getName());
	final Player player = event.getPlayer();

	int task = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		Teleport teleport = getTeleport(player);
		if (teleport.cancelled) {
		    return;
		}
		teleport.cancelled = true;
		teleport.taskId = -1;
		teleport.started = false;
		teleport.lastTeleportTime = new Date().getTime();
		
		player.playEffect(player.getLocation(), Effect.SMOKE, 0);
		org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
		Block infront = sign.getBlock().getRelative(aSign.getFacing());
		player.teleport(infront.getLocation());
	    }
	}, 30);
	
	teleport.cancelled = false;
	teleport.taskId = task;
	teleport.started = true;
    }
    
    public void cancelTeleport(Player player) {
	Teleport teleport = getTeleport(player);
	if (teleport.started && teleport.taskId > -1) {
	    player.sendMessage("Teleport cancelled");
	    teleport.cancelled = true;
	    plugin.getServer().getScheduler().cancelTask(teleport.taskId);
	    teleport.started = false;
	}
    }
    
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
	cancelTeleport(event.getPlayer());
	
	if (event.getPlayer().getItemInHand().getType() != Material.COMPASS) {
	    return;
	}

	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
	    if (event.getClickedBlock().getState() instanceof Sign) {
		if (memorizeStone(event)) {
		    return;
		}
	    }
	}

	// Temporary 'scrolling' through until I implement a better UI based way of selecting destination
	if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
		|| event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
	    Set<String> memory = memorized.get(event.getPlayer().getName());
	    if (memory == null || memory.size() == 0) {
		return;
	    }

	    String selectedName = selected.get(event.getPlayer().getName());
	    if (selectedName == null) {
		selectedName = memory.iterator().next();
	    } else {
		boolean next = false;
		boolean found = false;
		for (String name : memory) {
		    if (next) {
			selectedName = name;
			found = true;
			break;
		    }
		    if (name.equals(selectedName)) {
			next = true;
		    }
		}

		// wrap around
		if (!found) {
		    selectedName = memory.iterator().next();
		}
	    }

	    event.getPlayer().sendMessage("Selecting destination as " + selectedName);
	    selected.put(event.getPlayer().getName(), selectedName);
	    return;
	}

	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)
		|| event.getAction().equals(Action.LEFT_CLICK_AIR)) {

	    String name = selected.get(event.getPlayer().getName());

	    if (name != null) {
		MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
		if (stone == null) {
		    event.getPlayer().sendMessage(name + " no longer exists as a destination");
		    forgetStone(name);
		    return;
		}

		startTeleport(stone, event);

	    } else {
		event.getPlayer().sendMessage("No Memorized recalling");

	    }

	}

	super.onPlayerInteract(event);
    }
    
    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
	if ((event.getFrom().getBlockX() != event.getTo().getBlockX()) ||
		(event.getFrom().getBlockY() != event.getTo().getBlockY()) ||
		(event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {
	    
		cancelTeleport(event.getPlayer());
	}
    }
    
    

}

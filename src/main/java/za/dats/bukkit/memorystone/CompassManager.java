package za.dats.bukkit.memorystone;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.inventory.CraftingInventory;
import org.getspout.spoutapi.player.SpoutPlayer;

import sun.security.action.GetLongAction;

import za.dats.bukkit.memorystone.ui.SpoutLocationPopupManager;
import za.dats.bukkit.memorystone.ui.SpoutLocationPopupManager.LocationPopupListener;

public class CompassManager extends PlayerListener {
    private static class Teleport {
	boolean started;
	boolean cancelled;
	int taskId = -1;
	long lastTeleportTime;
	long lastFizzleTime;
	long lastEventTime;
	Player teleportEntity;
    }

    private final MemoryStonePlugin plugin;
    private final Map<String, Set<String>> memorized = new HashMap<String, Set<String>>();
    private final Map<String, String> selected = new HashMap<String, String>();
    private final Map<String, Teleport> teleporting = new HashMap<String, Teleport>();
    private final String locationsFile = "locations.yml";
    private final List<Material> skippedInteractionBlocks = new ArrayList<Material>();

    public CompassManager(MemoryStonePlugin plugin) {
	this.plugin = plugin;
	skippedInteractionBlocks.add(Material.BED_BLOCK);
	skippedInteractionBlocks.add(Material.BED);
	skippedInteractionBlocks.add(Material.CHEST);
	skippedInteractionBlocks.add(Material.WOOD_DOOR);
	skippedInteractionBlocks.add(Material.IRON_DOOR);
	skippedInteractionBlocks.add(Material.IRON_DOOR_BLOCK);
	skippedInteractionBlocks.add(Material.LOCKED_CHEST);
	skippedInteractionBlocks.add(Material.BOAT);
	skippedInteractionBlocks.add(Material.STONE_BUTTON);
	skippedInteractionBlocks.add(Material.LEVER);
	skippedInteractionBlocks.add(Material.WOODEN_DOOR);
	skippedInteractionBlocks.add(Material.TRAP_DOOR);
	skippedInteractionBlocks.add(Material.FURNACE);
	skippedInteractionBlocks.add(Material.WORKBENCH);
    }

    public Set<String> getPlayerLocations(String playerName) {
	TreeSet<String> result = new TreeSet<String>(new Comparator<String>() {
	    public int compare(String o1, String o2) {
		return o1.compareToIgnoreCase(o2);
	    }
	});

	result.addAll(memorized.get(playerName));
	result.addAll(plugin.getMemoryStoneManager().getGlobalStones());
	return result;
    }

    public void forgetStone(String name, boolean showMessage) {
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
		if (p != null && showMessage) {
		    p.sendMessage(Config.getColorLang("destroyForgotten", "name", name));
		}
	    }
	}

	saveLocations();
    }

    public void registerEvents() {
	PluginManager pm;
	pm = plugin.getServer().getPluginManager();
	pm.registerEvent(Event.Type.PLAYER_INTERACT, this, Priority.Normal, plugin);
	pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, this, Priority.Normal, plugin);
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

	    event.getPlayer().sendMessage(Config.getColorLang("memorize", "name", stone.getName()));

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

    public Location getDestinationLocation(MemoryStone stone, Player player) {
	Block sign = stone.getSign().getBlock();

	org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) stone.getSign().getData();
	Block infront = sign.getRelative(aSign.getFacing());

	Location result = infront.getLocation();
	result.setX(result.getX() + 0.5);
	result.setZ(result.getZ() + 0.5);

	result.setPitch(player.getLocation().getPitch());

	byte signData = sign.getData();
	if (sign.getType() == Material.SIGN_POST) {
	    result.setYaw(signData < 0x8 ? signData * 22.5f + 180 : signData * 22.5f - 180);
	} else if (sign.getType() == Material.WALL_SIGN) {
	    if (signData == 0x2) {// East
		result.setYaw(180);
	    } else if (signData == 0x3) {// West
		result.setYaw(0);
	    } else if (signData == 0x4) {// North
		result.setYaw(90);
	    } else {// South
		result.setYaw(270);
	    }
	} else {
	    result.setYaw(player.getLocation().getYaw());
	}
	
	return result;
    }

    public void startTeleport(final MemoryStone stone, PlayerEvent event, Player other) {
	final ItemStack item = event.getPlayer().getItemInHand();
	final Player player = event.getPlayer();

	// check permissions!
	if (!event.getPlayer().hasPermission("memorystone.usefree")) {
	    if ((Config.getMaxUsesPerItem() > 0)
		    && (item.getDurability() == 0 || item.getDurability() > Config.getMaxUsesPerItem())) {
		item.setDurability((short) Config.getMaxUsesPerItem());
	    }
	}

	final String name = stone.getName();
	final Sign sign = (Sign) stone.getSign();
	if (sign == null) {
	    event.getPlayer().sendMessage(Config.getColorLang("notfound", "name", name));
	    return;
	}

	Teleport teleport = getTeleport(event.getPlayer());

	if (!player.hasPermission("memorystone.usewithoutcooldown")) {
	    long now = new Date().getTime();
	    if (now - teleport.lastTeleportTime < Config.getCooldownTime() * 1000) {
		long left = Config.getCooldownTime() - ((now - teleport.lastTeleportTime) / 1000);
		event.getPlayer().sendMessage(Config.getColorLang("cooldown", "left", "" + left));
		return;
	    }

	    if (now - teleport.lastFizzleTime < Config.getFizzleCooldownTime() * 1000) {
		long left = Config.getFizzleCooldownTime() - ((now - teleport.lastFizzleTime) / 1000);
		event.getPlayer().sendMessage(Config.getColorLang("cooldown", "left", "" + left));
		return;
	    }
	}

	if (other.equals(event.getPlayer())) {
	    event.getPlayer().sendMessage(Config.getColorLang("startrecall", "name", stone.getName()));
	} else {
	    event.getPlayer().sendMessage(
		    Config.getColorLang("teleportingother", "name", other.getName(), "destination", name));
	    other.sendMessage(Config.getColorLang("teleportedbyother", "name", event.getPlayer().getName(),
		    "destination", name));

	}

	if (Config.getMaxUsesPerItem() > 0 && (!player.hasPermission("memorystone.usefree"))) {
	    item.setDurability((short) (item.getDurability() - 1));

	    if (item.getDurability() == 0) {
		if (item.getAmount() == 1) {
		    player.setItemInHand(null);
		    // item.setDurability((short)11);
		    // player.getInventory().remove(item);
		} else {
		    item.setDurability((short) 3);
		    item.setAmount(item.getAmount() - 1);
		}
		player.sendMessage(Config.getColorLang("consumed"));
	    } else {
		player.sendMessage(Config.getColorLang("chargesleft", "numcharges", "" + item.getDurability()));

	    }
	}

	int waitTime = Config.getCastingTime() * 10;
	if (player.hasPermission("memorystone.useinstantly")) {
	    waitTime = 1;
	}

	int task = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		Teleport teleport = getTeleport(player);
		teleport.taskId = -1;
		teleport.started = false;
		if (teleport.cancelled) {
		    return;
		}

		if (!teleport.teleportEntity.equals(player)) {
		    Teleport other = getTeleport((Player) teleport.teleportEntity);
		    other.taskId = -1;
		    other.started = false;
		    if (other.cancelled) {
			return;
		    }

		    other.lastTeleportTime = new Date().getTime();
		}

		teleport.lastTeleportTime = new Date().getTime();

		Location destination = getDestinationLocation(stone, player);

		teleport.teleportEntity.teleport(destination);
	    }
	}, waitTime);

	teleport.cancelled = false;
	teleport.taskId = task;
	teleport.started = true;
	teleport.teleportEntity = other;

	if (!teleport.teleportEntity.equals(player)) {
	    Teleport otherTeleport = getTeleport((Player) teleport.teleportEntity);
	    otherTeleport.cancelled = false;
	    otherTeleport.taskId = task;
	    otherTeleport.started = true;
	    otherTeleport.teleportEntity = other;
	}
    }

    public void cancelTeleport(Player player) {
	Teleport teleport = getTeleport(player);
	if (teleport.started && teleport.taskId > -1) {
	    player.sendMessage(Config.getColorLang("cancelled"));
	    teleport.cancelled = true;
	    plugin.getServer().getScheduler().cancelTask(teleport.taskId);
	    teleport.started = false;
	    teleport.lastFizzleTime = new Date().getTime();
	}
    }

    @Override
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
	if (event.getPlayer().getItemInHand().getType() != Config.getTeleportItem()) {
	    return;
	}

	// check permissions!
	if (!event.getPlayer().hasPermission("memorystone.useonothers")) {
	    return;
	}

	if (!(event.getRightClicked() instanceof HumanEntity)) {
	    return;
	}

	Teleport teleport = getTeleport(event.getPlayer());
	long now = new Date().getTime();
	if (now - teleport.lastEventTime < 100) {
	    return;
	}
	teleport.lastEventTime = now;

	final SpoutPlayer p = (SpoutPlayer) event.getPlayer();
	if (p.isSpoutCraftEnabled()) {
	    plugin.getSpoutLocationPopupManager().showPopup(p, getPlayerLocations(p.getName()),
		    "Select location to teleport to.", new LocationPopupListener() {
			public void selected(String name) {
			    tryTeleportOther(event, p, name);
			}
		    });
	} else {
	    String name = selected.get(event.getPlayer().getName());
	    tryTeleportOther(event, p, name);
	}

    }

    private void tryTeleportOther(final PlayerInteractEntityEvent event, final SpoutPlayer p, String name) {
	if (name != null) {
	    MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
	    if (stone == null) {
		p.sendMessage(Config.getColorLang("notexist", "name", name));
		forgetStone(name, false);
		return;
	    }
	    cancelTeleport(p);

	    Player other = (Player) event.getRightClicked();
	    cancelTeleport(other);

	    startTeleport(stone, event, other);
	} else {
	    event.getPlayer().sendMessage(Config.getColorLang("notmemorized"));

	}
    }

    @Override
    public void onPlayerInteract(final PlayerInteractEvent event) {
	if (event.getPlayer().getItemInHand().getType() != Config.getTeleportItem()) {
	    return;
	}

	// check permissions!
	if (!event.getPlayer().hasPermission("memorystone.use")) {
	    return;
	}

	Teleport teleport = getTeleport(event.getPlayer());
	long now = new Date().getTime();
	if (now - teleport.lastEventTime < 100) {
	    return;
	}
	teleport.lastEventTime = now;

	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
	    if (event.getClickedBlock().getState() instanceof Sign) {
		if (teleport.started) {
		    return;
		}

		if (memorizeStone(event)) {
		    return;
		}
	    }
	}

	// Temporary 'scrolling' through until I implement a better UI based way of selecting destination
	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_AIR)) {
	    // Make interaction with interactable blocks cleaner
	    if (event.getClickedBlock() != null && skippedInteractionBlocks.contains(event.getClickedBlock().getType())) {
		return;
	    }

	    if (teleport.started) {
		return;
	    }

	    Set<String> memory = getPlayerLocations(event.getPlayer().getName());
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

	    SpoutPlayer p = (SpoutPlayer) event.getPlayer();
	    if (!p.isSpoutCraftEnabled()) {
		event.getPlayer().sendMessage(Config.getColorLang("select", "name", selectedName));
		selected.put(event.getPlayer().getName(), selectedName);
		event.setCancelled(true);
	    }

	    return;
	}

	if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
	    // Make interaction with interactable blocks cleaner
	    if (event.getClickedBlock() != null && skippedInteractionBlocks.contains(event.getClickedBlock().getType())) {
		return;
	    }

	    final SpoutPlayer p = (SpoutPlayer) event.getPlayer();
	    if (p.isSpoutCraftEnabled()) {
		plugin.getSpoutLocationPopupManager().showPopup(p, getPlayerLocations(p.getName()),
			"Select location to teleport to", new LocationPopupListener() {
			    public void selected(String name) {
				tryTeleport(event, p, name);
			    }

			    public void cancelled() {
			    }
			});
	    } else {
		String name = selected.get(event.getPlayer().getName());
		tryTeleport(event, p, name);
	    }

	}

	super.onPlayerInteract(event);
    }

    private void tryTeleport(final PlayerInteractEvent event, SpoutPlayer p, String name) {
	if (name != null) {
	    MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
	    if (stone == null) {
		p.sendMessage(Config.getColorLang("notexist", "name", name));
		forgetStone(name, false);
		return;
	    }

	    cancelTeleport(p);
	    startTeleport(stone, event, p);

	} else {
	    p.sendMessage(Config.getColorLang("notmemorized"));
	}
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
	if ((event.getFrom().getBlockX() != event.getTo().getBlockX())
		|| (event.getFrom().getBlockY() != event.getTo().getBlockY())
		|| (event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {

	    cancelTeleport(event.getPlayer());
	}
    }

}

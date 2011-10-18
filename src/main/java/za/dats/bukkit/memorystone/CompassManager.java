package za.dats.bukkit.memorystone;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.event.input.InputListener;
import org.getspout.spoutapi.event.input.KeyReleasedEvent;
import org.getspout.spoutapi.gui.ScreenType;
import org.getspout.spoutapi.keyboard.Keyboard;
import za.dats.bukkit.memorystone.Config.MemoryEffect;
import za.dats.bukkit.memorystone.economy.EconomyManager;
import za.dats.bukkit.memorystone.ui.LocationPopupListener;

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

    final MemoryStonePlugin plugin;
    private final Map<String, Set<MemoryStone>> memorized;
    private final Map<String, String> selected;
    private final Map<String, Teleport> teleporting;
    private final String locationsFile = "locations.yml";
    private final List<Material> skippedInteractionBlocks;
    private final Map<String, Interference> interferences;

    public CompassManager(MemoryStonePlugin plugin) {
	this.plugin = plugin;
	memorized = new HashMap<String, Set<MemoryStone>>();
	selected = new HashMap<String, String>();
	teleporting = new HashMap<String, Teleport>();
	interferences = new HashMap<String, Interference>();

	skippedInteractionBlocks = new ArrayList<Material>();
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

    public Set<MemoryStone> getPlayerLocations(final String world, final Player player) {
	TreeSet<MemoryStone> result;
	if (Config.isSortByDistance()) {
	    result = new TreeSet<MemoryStone>(new Comparator<MemoryStone>() {
		public int compare(MemoryStone o1, MemoryStone o2) {
		    if (o1.equals(o2)) {
			return 0;
		    }

		    if (o1.getStructure().getWorld().getName().equals(o2.getStructure().getWorld().getName())) {
			double o1Distance = player.getLocation().distanceSquared(o1.getSign().getBlock().getLocation());
			double o2Distance = player.getLocation().distanceSquared(o2.getSign().getBlock().getLocation());

			if (o1Distance < o2Distance) {
			    return -1;
			}
			return 1;
		    }

		    if (world.equals(o1.getStructure().getWorld().getName())) {
			return -1;
		    } else if (world.equals(o2.getStructure().getWorld().getName())) {
			return 1;
		    }

		    return o1.getName().compareToIgnoreCase(o2.getName());
		}
	    });
	} else {
	    result = new TreeSet<MemoryStone>();
	}

	String playerName = player.getName();
	boolean hasPermission = player.hasPermission("memorystone.allmemorized");
	if (hasPermission) {
	    Collection<? extends MemoryStone> localStones = plugin.getMemoryStoneManager().getLocalStones(world);
	    for (MemoryStone memoryStone : localStones) {
		if (memoryStone.getSign() != null) {
		    result.add(memoryStone);
		}
	    }
	} else {
	    Set<MemoryStone> set = memorized.get(playerName);
	    if (set == null) {
		set = new TreeSet<MemoryStone>();
		memorized.put(playerName, set);
	    }

	    for (MemoryStone memoryStone : set) {
		if (memoryStone.isCrossWorld()) {
		    result.add(memoryStone);
		} else if (world.equals(memoryStone.getStructure().getWorld().getName())) {
		    addIfWithinDistance(player, result, memoryStone);
		}
	    }
	}

	for (MemoryStone memoryStone : plugin.getMemoryStoneManager().getGlobalStones()) {
	    if (memoryStone.isCrossWorld()) {
		result.add(memoryStone);
	    } else if (world.equals(memoryStone.getStructure().getWorld().getName())) {
		addIfWithinDistance(player, result, memoryStone);
	    }
	}

	return result;
    }

    private void addIfWithinDistance(final Player player, TreeSet<MemoryStone> result, MemoryStone memoryStone) {
	if (memoryStone.getDistanceLimit() <= 0) {
	    result.add(memoryStone);
	} else {
	    if (memoryStone.getSign() == null) {
		return;
	    }

	    if (memoryStone.getSign().getBlock() == null) {
		return;
	    }
	    double distance = player.getLocation().distanceSquared(memoryStone.getSign().getBlock().getLocation());
	    if (distance < memoryStone.getDistanceLimit()) {
		result.add(memoryStone);
	    }
	}
    }

    public void forgetStone(String name, boolean showMessage) {
	MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
	for (String player : selected.keySet()) {
	    if (name.equals(selected.get(player))) {
		selected.put(player, null);
	    }
	}

	for (String player : memorized.keySet()) {
	    Set<MemoryStone> list = memorized.get(player);
	    if (list != null && list.contains(stone)) {
		list.remove(stone);
		Player p = plugin.getServer().getPlayer(player);
		if (p != null && showMessage) {
		    p.sendMessage(Config.getColorLang("destroyForgotten", "name", stone.getName()));
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

	if (plugin.isSpoutEnabled() && Config.getTeleportKey().length() > 0) {
	    final Keyboard teleportKey = Keyboard.valueOf("KEY_" + Config.getTeleportKey());
	    InputListener inputListener = new InputListener() {
		@Override
		public void onKeyReleasedEvent(KeyReleasedEvent event) {
		    if (Config.getTeleportItem() == null) {
			return;
		    }

		    if (event.getKey().equals(teleportKey) && event.getScreenType().equals(ScreenType.GAME_SCREEN)) {
			Player p = event.getPlayer();

			int index = p.getInventory().first(Config.getTeleportItem());
			if (index == -1) {
			    p.sendMessage(Config.getColorLang("teleportitemnotfound", "material", Config
				    .getTeleportItem().toString().toLowerCase()));
			    return;
			}

			ItemStack[] contents = p.getInventory().getContents();
			ItemStack item = contents[index];
			fireTeleportWithItem(item, Config.getMaxUsesPerItem(), index, null, p);
		    }
		}
	    };
	    pm.registerEvent(Event.Type.CUSTOM_EVENT, inputListener, Event.Priority.Normal, plugin);

	}
	loadLocations();
    }

    public boolean memorizeStone(PlayerInteractEvent event) {
	Sign state = (Sign) event.getClickedBlock().getState();
	MemoryStone stone = plugin.getMemoryStoneManager().getMemoryStructureForSign(state);
	return memorizeStone(event.getPlayer(), stone);
    }
    
    public boolean memorizeStone(Player player, MemoryStone stone) {
    	
    	if (player!=null && stone != null && stone.getSign() != null) {
    	    if (stone.isGlobal()) {
    		if (Config.isStoneToStoneEnabled()) {
    		    return false;
    		}

    		player.sendMessage(Config.getColorLang("alreadymemorized", "name", stone.getName()));
    		return true;
    	    }

    	    EconomyManager economyManager = MemoryStonePlugin.getInstance().getEconomyManager();
    	    if (economyManager.isEconomyEnabled() && (!player.hasPermission("memorystone.usefree"))
    		    && !economyManager.payMemorizeCost(player, stone)) {
    	    player.sendMessage(
    			Config.getColorLang("cantaffordmemorize", "name", stone.getName(), "cost",
    				economyManager.getFormattedCost(stone.getMemorizeCost())));
    		return true;
    	    }

    	    Set<MemoryStone> set = memorized.get(player.getName());
    	    if (set == null) {
    		set = new TreeSet<MemoryStone>();
    		memorized.put(player.getName(), set);
    	    }

    	    if (set.contains(stone)) {
    		if (Config.isStoneToStoneEnabled()) {
    		    return false;
    		}

    		player.sendMessage(Config.getColorLang("alreadymemorized", "name", stone.getName()));
    		return true;
    	    }
    	    set.add(stone);
    	    // selected.put(player.getName(), stone.getName());

    	    player.sendMessage(Config.getColorLang("memorize", "name", stone.getName()));

    	    saveLocations();
    	    return true;
    	}
    	return false;
        }

    
    public boolean isMemorized(Player player, MemoryStone stone) {
	Set<MemoryStone> set = memorized.get(player.getName());
	if (set == null) {
	    return false;
	}
	
	if (set.contains(stone)) {
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
	    Set<String> stones = memLoad.get(player);
	    Set<MemoryStone> stoneList = new TreeSet<MemoryStone>();
	    for (String stoneName : stones) {
		MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(stoneName);
		if (stone != null) {
		    stoneList.add(stone);
		}
	    }
	    memorized.put(player, stoneList);
	}

	Map<String, String> selLoad = (Map<String, String>) conf.getProperty("selected");
	for (String player : selLoad.keySet()) {
	    selected.put(player, selLoad.get(player));
	}

    }

    public void saveLocations() {
	File file = new File(this.plugin.getDataFolder(), this.locationsFile);
	Configuration conf = new Configuration(file);

	Map<String, Set<String>> memorizedNames = new HashMap<String, Set<String>>();
	for (String playerName : memorized.keySet()) {
	    Set<MemoryStone> stoneList = memorized.get(playerName);

	    Set<String> stoneNameList = new TreeSet<String>();
	    for (MemoryStone memoryStone : stoneList) {
		stoneNameList.add(memoryStone.getName());
	    }

	    memorizedNames.put(playerName, stoneNameList);
	}
	conf.setProperty("memorized", memorizedNames);
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

    public void startTeleport(final MemoryStone stone, final Player caster, ItemStack item, int maxUses, int itemIndex,
	    Player other) {
	// check permissions!
	if (!caster.hasPermission("memorystone.usefree") && item != null) {
	    if ((maxUses > 0) && (item.getDurability() == 0 || item.getDurability() > maxUses)) {
		item.setDurability((short) Config.getMaxUsesPerItem());
	    }
	}

	final String name = stone.getName();
	final Sign sign = (Sign) stone.getSign();
	if (sign == null) {
	    caster.sendMessage(Config.getColorLang("notfound", "name", name));
	    return;
	}

	Teleport teleport = getTeleport(caster);

	if (!caster.hasPermission("memorystone.usewithoutcooldown")) {
	    long now = new Date().getTime();
	    if (now - teleport.lastTeleportTime < Config.getCooldownTime() * 1000) {
		long left = Config.getCooldownTime() - ((now - teleport.lastTeleportTime) / 1000);
		caster.sendMessage(Config.getColorLang("cooldown", "left", "" + left));
		return;
	    }

	    if (now - teleport.lastFizzleTime < Config.getFizzleCooldownTime() * 1000) {
		long left = Config.getFizzleCooldownTime() - ((now - teleport.lastFizzleTime) / 1000);
		caster.sendMessage(Config.getColorLang("cooldown", "left", "" + left));
		return;
	    }
	}

	if (!caster.hasPermission("memorystone.usefree")) {
	    EconomyManager economyManager = MemoryStonePlugin.getInstance().getEconomyManager();
	    if (economyManager.isEconomyEnabled() && !economyManager.payTeleportCost(caster, stone)) {
		caster.sendMessage(Config.getColorLang("cantaffordteleport", "name", name, "cost",
			economyManager.getFormattedCost(stone.getTeleportCost())));
		return;
	    }
	}

	if (other.equals(caster)) {
	    caster.sendMessage(Config.getColorLang("startrecall", "name", stone.getName()));
	} else {
	    caster.sendMessage(Config.getColorLang("teleportingother", "name", other.getName(), "destination", name));
	    other.sendMessage(Config.getColorLang("teleportedbyother", "name", caster.getName(), "destination", name));

	}

	if (maxUses > 0 && (!caster.hasPermission("memorystone.usefree")) && item != null) {
	    item.setDurability((short) (item.getDurability() - 1));

	    if (item.getDurability() == 0) {
		if (item.getAmount() == 1) {
		    // caster.setItemInHand(null);
		    caster.getInventory().setItem(itemIndex, null);
		    // item.setDurability((short)11);
		    // player.getInventory().remove(item);
		} else {
		    item.setDurability((short) maxUses);
		    item.setAmount(item.getAmount() - 1);
		}
		caster.sendMessage(Config.getColorLang("consumed", "material", item.getType().toString().toLowerCase()));
	    } else {
		caster.sendMessage(Config.getColorLang("chargesleft", "numcharges", "" + item.getDurability(),
			"material", item.getType().toString().toLowerCase()));
	    }
	}

	int waitTime = Config.getCastingTime() * 10;
	if (caster.hasPermission("memorystone.useinstantly")) {
	    waitTime = 1;
	}

	int task = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		Teleport teleport = getTeleport(caster);
		teleport.taskId = -1;
		teleport.started = false;
		if (teleport.cancelled) {
		    return;
		}

		if (!teleport.teleportEntity.equals(caster)) {
		    Teleport other = getTeleport((Player) teleport.teleportEntity);
		    other.taskId = -1;
		    other.started = false;
		    if (other.cancelled) {
			return;
		    }

		    other.lastTeleportTime = new Date().getTime();
		}

		teleport.lastTeleportTime = new Date().getTime();

		Location destination = getDestinationLocation(stone, caster);
		if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_TELEPORT_SOURCE)) {
		    teleport.teleportEntity.getWorld().strikeLightningEffect(teleport.teleportEntity.getLocation());
		}

		if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_TELEPORT_DEST)) {
		    teleport.teleportEntity.getWorld().strikeLightningEffect(destination);
		}

		if (Config.isPointCompassOnly()) {
		    teleport.teleportEntity.setCompassTarget(destination);
		} else {
		    teleport.teleportEntity.teleport(destination);
		}

	    }
	}, waitTime);

	teleport.cancelled = false;
	teleport.taskId = task;
	teleport.started = true;
	teleport.teleportEntity = other;

	if (!teleport.teleportEntity.equals(caster)) {
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
	if (Config.getTeleportItem() == null) {
	    // Cannot teleport another without a teleport item set.
	    return;
	}

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

	if (isInNoTeleportZone(event.getPlayer())) {
	    return;
	}

	if (!(event.getPlayer().hasPermission("memorystone.useanywhere") || Config.getMinProximityToStoneForTeleport() == 0)) {
	    if (!withinDistanceOfAnyStone(event.getPlayer(), Config.getMinProximityToStoneForTeleport())) {
		event.getPlayer().sendMessage(Config.getColorLang("outsideproximity"));
		return;
	    }
	}

	if (plugin.isSpoutEnabled()) {
	    if (((org.getspout.spoutapi.player.SpoutPlayer) event.getPlayer()).isSpoutCraftEnabled()) {
		String name = ((HumanEntity) event.getRightClicked()).getName();
		plugin.getSpoutLocationPopupManager().showPopup(event.getPlayer(),
			getPlayerLocations(event.getPlayer().getWorld().getName(), event.getPlayer()),
			Config.getColorLang("selectotherlocation", "name", name), new LocationPopupListener() {
			    public void selected(MemoryStone stone) {
				tryTeleportOther(event, event.getPlayer(), stone.getName());
			    }
			});
	    } else {
		String name = selected.get(event.getPlayer().getName());
		tryTeleportOther(event, event.getPlayer(), name);
	    }
	} else {
	    String name = selected.get(event.getPlayer().getName());
	    tryTeleportOther(event, event.getPlayer(), name);
	}
    }

    private boolean isInNoTeleportZone(Player player) {
	for (MemoryStone stone : plugin.getMemoryStoneManager().getNoTeleportStones()) {
	    if (!stone.getStructure().getRootBlock().getWorld().getName().equals(player.getWorld().getName())) {
		continue;
	    }
	    double distance = player.getLocation().distanceSquared(stone.getStructure().getRootBlock().getLocation());
	    if (distance < stone.getDistanceLimit()) {
		player.sendMessage(Config.getColorLang("noteleportzone"));
		return true;
	    }
	}

	return false;
    }

    private void tryTeleportOther(final PlayerInteractEntityEvent event, final Player p, String name) {
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

	    startTeleport(stone, event.getPlayer(), p.getItemInHand(), Config.getMaxUsesPerItem(), p.getInventory()
		    .getHeldItemSlot(), other);
	} else {
	    event.getPlayer().sendMessage(Config.getColorLang("notmemorized"));

	}
    }

    public void fireTeleportFromBlock(Block clickedBlock, Player player) {
	MemoryStone clickedStone = null;
	if (Config.isStoneToStoneEnabled() && player.hasPermission("memorystone.usestonetostone")
		&& (clickedBlock != null) && (clickedBlock.getState() instanceof Sign)) {
	    Sign state = (Sign) clickedBlock.getState();
	    clickedStone = plugin.getMemoryStoneManager().getMemoryStructureForSign(state);
	}

	ItemStack consumeItem = null;
	int maxUses = 0;
	int itemIndex = -1;
	if (clickedStone == null) {
	    consumeItem = player.getItemInHand();
	    maxUses = Config.getMaxUsesPerItem();
	    itemIndex = player.getInventory().getHeldItemSlot();

	    if (Config.getTeleportItem() == null || !Config.getTeleportItem().equals(consumeItem.getType())) {
		return;
	    }
	} else if (Config.getStoneToStoneItem() != null) {
	    itemIndex = player.getInventory().first(Config.getStoneToStoneItem());
	    if (itemIndex >= 0) {
		consumeItem = player.getInventory().getItem(itemIndex);
		maxUses = Config.getStoneToStoneMaxUses();
	    }

	    if (consumeItem == null) {
		player.sendMessage(Config.getColorLang("stonetostone.itemmissing", "material", Config
			.getStoneToStoneItem().toString().toLowerCase()));
		return;
	    }
	}

	fireTeleportWithItem(consumeItem, maxUses, itemIndex, clickedStone, player);
    }

    public void fireTeleportWithItem(final ItemStack item, final int maxUses, final int itemIndex,
	    final MemoryStone ignoreStone, final Player player) {
	if (!(player.hasPermission("memorystone.useanywhere") || Config.getMinProximityToStoneForTeleport() == 0)) {
	    if (!withinDistanceOfAnyStone(player, Config.getMinProximityToStoneForTeleport())) {
		player.sendMessage(Config.getColorLang("outsideproximity"));
		return;
	    }
	}

	if (plugin.isSpoutEnabled()) {
	    if (((org.getspout.spoutapi.player.SpoutPlayer) player).isSpoutCraftEnabled()) {
		Set<MemoryStone> playerLocations = getPlayerLocations(player.getWorld().getName(), player);
		if (playerLocations != null && ignoreStone != null) {
		    playerLocations.remove(ignoreStone);
		}

		if (playerLocations == null || playerLocations.size() == 0) {
		    player.sendMessage(Config.getColorLang("notmemorized"));
		    return;
		}

		plugin.getSpoutLocationPopupManager().showPopup(player, playerLocations,
			Config.getColorLang("selectlocation"), new LocationPopupListener() {
			    public void selected(MemoryStone stone) {
				tryTeleport(player, item, maxUses, itemIndex, stone.getName());
			    }
			});
	    } else {
		String name = selected.get(player.getName());
		tryTeleport(player, item, maxUses, itemIndex, name);
	    }
	} else {
	    String name = selected.get(player.getName());
	    tryTeleport(player, item, maxUses, itemIndex, name);
	}
    }

    @Override
    public void onPlayerInteract(final PlayerInteractEvent event) {
	Player player = event.getPlayer();

	// check permissions!
	if (!player.hasPermission("memorystone.use")) {
	    return;
	}

	Teleport teleport = getTeleport(player);
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

	if (isInNoTeleportZone(player)) {
	    return;
	}

	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_AIR)) {
	    if (plugin.isSpoutEnabled()) {
		org.getspout.spoutapi.player.SpoutPlayer p = (org.getspout.spoutapi.player.SpoutPlayer) player;
		if (p.isSpoutCraftEnabled()) {
		    return;
		}
	    }
	    cycleTeleport(event, player, teleport);

	}

	if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
	    // Make interaction with interactable blocks cleaner
	    if (event.getClickedBlock() != null && skippedInteractionBlocks.contains(event.getClickedBlock().getType())) {
		return;
	    }

	    fireTeleportFromBlock(event.getClickedBlock(), player);

	}

	super.onPlayerInteract(event);
    }

    private void cycleTeleport(final PlayerInteractEvent event, Player player, Teleport teleport) {
	// Make interaction with interactable blocks cleaner
	if (event.getClickedBlock() != null && skippedInteractionBlocks.contains(event.getClickedBlock().getType())) {
	    return;
	}

	if (teleport.started) {
	    return;
	}

	MemoryStone clickedStone = null;
	if (event.getPlayer().getItemInHand().getType() != Config.getTeleportItem()) {
	    if (Config.isStoneToStoneEnabled() && (event.getClickedBlock() != null)
		    && (event.getClickedBlock().getState() instanceof Sign)
		    && player.hasPermission("memorystone.usestonetostone")) {
		Sign state = (Sign) event.getClickedBlock().getState();
		clickedStone = plugin.getMemoryStoneManager().getMemoryStructureForSign(state);
		if (clickedStone == null) {
		    return;
		}
	    } else {
		return;
	    }
	}

	Set<MemoryStone> memory = getPlayerLocations(player.getWorld().getName(), player);
	if (memory == null || memory.size() == 0) {
	    return;
	}

	if (clickedStone != null) {
	    memory.remove(clickedStone);
	    if (memory.size() == 0) {
		return;
	    }
	}

	String selectedName = selected.get(player.getName());
	if (selectedName == null) {
	    selectedName = memory.iterator().next().getName();
	} else {
	    boolean next = false;
	    boolean found = false;
	    for (MemoryStone currentStone : memory) {
		String name = currentStone.getName();
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
		selectedName = memory.iterator().next().getName();
	    }
	}

	boolean messageSent = false;
	if (!player.hasPermission("memorystone.usefree")) {
	    EconomyManager economyManager = MemoryStonePlugin.getInstance().getEconomyManager();
	    if (economyManager.isEconomyEnabled()) {
		MemoryStone stone = MemoryStonePlugin.getInstance().getMemoryStoneManager()
			.getNamedMemoryStone(selectedName);

		player.sendMessage(Config.getColorLang("selectwithcost", "name", selectedName, "cost",
			economyManager.getFormattedCost(stone.getTeleportCost())));
		messageSent = true;
	    }
	}

	if (!messageSent) {
	    player.sendMessage(Config.getColorLang("select", "name", selectedName));
	}

	selected.put(player.getName(), selectedName);
	event.setCancelled(true);
    }

    private boolean withinDistanceOfAnyStone(Player player, int minProximityToStoneForTeleport) {
	minProximityToStoneForTeleport = minProximityToStoneForTeleport * minProximityToStoneForTeleport;
	for (MemoryStone stone : plugin.getMemoryStoneManager().getStones()) {
	    if (stone.getSign() == null) {
		continue;
	    }

	    if (!stone.getSign().getWorld().getName().equals(player.getWorld().getName())) {
		continue;
	    }

	    if (player.getLocation().distanceSquared(stone.getSign().getBlock().getLocation()) < minProximityToStoneForTeleport) {
		return true;
	    }
	}
	return false;
    }

    private void tryTeleport(Player p, ItemStack item, int maxUses, int itemIndex, String name) {
	if (name != null) {
	    MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
	    if (stone == null) {
		p.sendMessage(Config.getColorLang("notexist", "name", name));
		forgetStone(name, false);
		return;
	    }

	    cancelTeleport(p);
	    startTeleport(stone, p, item, maxUses, itemIndex, p);

	} else {
	    p.sendMessage(Config.getColorLang("notmemorized"));
	}
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
	Teleport teleport = getTeleport(event.getPlayer());
	if (teleport.started) {
	    if ((event.getFrom().getBlockX() != event.getTo().getBlockX())
		    || (event.getFrom().getBlockY() != event.getTo().getBlockY())
		    || (event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {

		cancelTeleport(event.getPlayer());
	    }
	}

	if (Config.getCompassToUnmemorizedStoneDistanceSquared() == 0) {
	    return;
	}

	Player player = event.getPlayer();
	Interference interference = interferences.get(player.getName());
	if (interference == null) {
	    interference = new Interference();
	    interferences.put(player.getName(), interference);
	}

	if (!interference.isTooClose(event.getTo())) {
	    interference.update(player, event);
	}
    }

}

package za.dats.bukkit.memorystone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.ConfigurationNode;
import za.dats.bukkit.memorystone.MemoryStone.StoneType;
import za.dats.bukkit.memorystone.economy.EconomyManager;
import za.dats.bukkit.memorystone.util.StructureListener;
import za.dats.bukkit.memorystone.util.structure.Rotator;
import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class MemoryStoneManager extends BlockListener implements StructureListener {
    private final MemoryStonePlugin memoryStonePlugin;
    private HashMap<Structure, MemoryStone> structureMap = new HashMap<Structure, MemoryStone>();
    private HashMap<String, MemoryStone> namedMap = new HashMap<String, MemoryStone>();
    private HashMap<String, Set<MemoryStone>> worldStones = new HashMap<String, Set<MemoryStone>>();
    private List<MemoryStone> globalStones = new ArrayList<MemoryStone>();
    private List<MemoryStone> noTeleportStones = new ArrayList<MemoryStone>();

    public MemoryStoneManager(MemoryStonePlugin memoryStonePlugin) {
	this.memoryStonePlugin = memoryStonePlugin;
    }

    public void registerEvents() {
	PluginManager pm;
	pm = memoryStonePlugin.getServer().getPluginManager();
	pm.registerEvent(Event.Type.SIGN_CHANGE, this, Event.Priority.Normal, memoryStonePlugin);
	pm.registerEvent(Event.Type.BLOCK_BREAK, this, Event.Priority.Normal, memoryStonePlugin);
    }

    public void structurePlaced(Player player, Structure structure) {
	MemoryStone stone = new MemoryStone();
	stone.setStructure(structure);

	structureMap.put(structure, stone);

	if (stone.getType().equals(StoneType.NOTELEPORT)) {
	    noTeleportStones.add(stone);
	}
	player.sendMessage(Utility.color(Config.getColorLang("createConfirm", "name", structure.getStructureType()
		.getName())));
    }

    public void structureDestroyed(Player player, Structure structure) {
	MemoryStone stone = structureMap.get(structure);
	if (stone.getType().equals(StoneType.NOTELEPORT)) {
	    noTeleportStones.remove(stone);
	}

	if (stone.getName() != null) {
	    memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), true);
	    globalStones.remove(stone);
	    removeWorldStone(stone);
	    namedMap.remove(stone.getName());
	}

	Sign sign = stone.getSign();
	if (sign != null) {
	    BlockState state = new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
		    .getState();

	    if (state instanceof Sign) {
		Sign newSign = (Sign) new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
			.getState();
		newSign.setLine(0, Config.getColorLang("signboard"));
		newSign.setLine(1, Config.getColorLang("broken"));
		newSign.update(true);
	    }

	    stone.setSign(null);
	}

	structureMap.remove(structure);
	player.sendMessage(Utility.color(Config.getColorLang("destroyed")));
    }

    public void structureLoaded(Structure structure, ConfigurationNode node) {
	MemoryStone stone = new MemoryStone();
	stone.setStructure(structure);

	if (stone.getType().equals(StoneType.NOTELEPORT)) {
	    noTeleportStones.add(stone);
	}

	structureMap.put(structure, stone);

	stone.setName(node.getString("name", ""));
	if (stone.getName() != null && stone.getName().length() > 0) {
	    namedMap.put(stone.getName(), stone);
	    if (stone.isGlobal()) {
		globalStones.add(stone);
	    }

	    addWorldStone(stone);
	}

	if (node.getProperty("teleportCost") != null) {
	    try {
		stone.setTeleportCost(Double.parseDouble(node.getString("teleportCost")));
	    } catch (NumberFormatException e) {
	    }
	}

	if (node.getProperty("memorizeCost") != null) {
	    try {
		stone.setMemorizeCost(Double.parseDouble(node.getString("memorizeCost")));
	    } catch (NumberFormatException e) {
	    }
	}

	if (node.getProperty("signx") != null) {
	    try {
		Sign newSign = (Sign) new Location(structure.getWorld(), node.getInt("signx", 0), node.getInt("signy",
			0), node.getInt("signz", 0)).getBlock().getState();

		// update price, if needed
		EconomyManager economyManager = memoryStonePlugin.getEconomyManager();
		if (economyManager.isEconomyEnabled()) {
		    newSign.setLine(2, economyManager.getFormattedCost(stone.getMemorizeCost()));
		    newSign.setLine(3, economyManager.getFormattedCost(stone.getTeleportCost()));
		} else {
		    newSign.setLine(2, "");
		    newSign.setLine(3, "");
		}
		stone.setSign(newSign);

	    } catch (Exception e) {
		memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), false);
		stone.setName("");
	    }
	}
    }

    private void addWorldStone(MemoryStone stone) {
	String worldName = stone.getStructure().getWorld().getName();
	Set<MemoryStone> set = worldStones.get(worldName);
	if (set == null) {
	    set = new TreeSet<MemoryStone>();
	    worldStones.put(worldName, set);
	}

	set.add(stone);
    }

    private void removeWorldStone(MemoryStone stone) {
	String worldName = stone.getStructure().getWorld().getName();
	Set<MemoryStone> set = worldStones.get(worldName);
	if (set == null) {
	    return;
	}

	set.remove(stone);
    }

    public void structureSaving(Structure structure, Map<String, Object> yamlMap) {
	MemoryStone memoryStone = structureMap.get(structure);

	yamlMap.put("name", memoryStone.getName());
	if (memoryStone.getSign() != null) {
	    Sign sign = memoryStone.getSign();
	    yamlMap.put("signx", sign.getX());
	    yamlMap.put("signy", sign.getY());
	    yamlMap.put("signz", sign.getZ());
	    yamlMap.put("teleportCost", memoryStone.getRawTeleportCost());
	    yamlMap.put("memorizeCost", memoryStone.getRawMemorizetCost());
	}
    }

    public void generatingDefaultStructureTypes(List<StructureType> types) {
	StructureType structuretype;
	StructureType.Prototype proto;

	proto = new StructureType.Prototype();
	for (int x = 0; x < 3; x++) {
	    for (int z = 0; z < 3; z++) {
		proto.addBlock(x, 0, z, Material.STONE);
	    }
	}
	proto.addBlock(1, 1, 1, Material.OBSIDIAN);
	proto.addBlock(1, 2, 1, Material.OBSIDIAN);
	proto.addBlock(1, 3, 1, Material.OBSIDIAN);
	proto.setName("Memory Stone");
	proto.setRotator(Rotator.NONE);
	proto.addMetadata("type", "MEMORYSTONE");
	proto.addMetadata("global", "false");
	proto.addMetadata("permissionRequired", "memorystone.create.local");
	proto.addMetadata("distanceLimit", "0");
	proto.addMetadata("teleportcost", "50");
	proto.addMetadata("memorizecost", "200");
	proto.addMetadata("buildcost", "1000");
	structuretype = new StructureType(proto);
	types.add(structuretype);

	proto = new StructureType.Prototype();
	for (int x = 0; x < 3; x++) {
	    for (int z = 0; z < 3; z++) {
		proto.addBlock(x, 0, z, Material.IRON_BLOCK);
	    }
	}
	proto.addBlock(1, 1, 1, Material.OBSIDIAN);
	proto.addBlock(1, 2, 1, Material.OBSIDIAN);
	proto.addBlock(1, 3, 1, Material.OBSIDIAN);
	proto.setName("Crossworld Stone");
	proto.setRotator(Rotator.NONE);
	proto.addMetadata("type", "MEMORYSTONE");
	proto.addMetadata("crossworld", "true");
	proto.addMetadata("permissionRequired", "memorystone.create.crossworld");
	proto.addMetadata("distanceLimit", "0");
	proto.addMetadata("teleportcost", "75");
	proto.addMetadata("memorizecost", "750");
	proto.addMetadata("buildcost", "3000");
	structuretype = new StructureType(proto);
	types.add(structuretype);

	proto = new StructureType.Prototype();
	for (int x = 0; x < 3; x++) {
	    for (int z = 0; z < 3; z++) {
		proto.addBlock(x + 1, 0, z + 1, Material.GOLD_BLOCK);
	    }
	}

	for (int x = 0; x < 5; x++) {
	    if (x == 2) {
		continue;
	    }
	    proto.addBlock(x, 0, 0, Material.STEP);
	    proto.addBlock(x, 0, 4, Material.STEP);
	}
	for (int z = 1; z < 4; z++) {
	    if (z == 2) {
		continue;
	    }
	    proto.addBlock(0, 0, z, Material.STEP);
	    proto.addBlock(4, 0, z, Material.STEP);
	}

	proto.addBlock(2, 1, 2, Material.OBSIDIAN);
	proto.addBlock(2, 2, 2, Material.OBSIDIAN);
	proto.addBlock(2, 3, 2, Material.OBSIDIAN);
	proto.addBlock(2, 4, 2, Material.DIAMOND_BLOCK);
	proto.setName("Global Stone");
	proto.setRotator(Rotator.NONE);
	proto.addMetadata("type", "MEMORYSTONE");
	proto.addMetadata("global", "true");
	proto.addMetadata("permissionRequired", "memorystone.create.global");
	proto.addMetadata("distanceLimit", "0");
	proto.addMetadata("teleportcost", "65");
	proto.addMetadata("memorizecost", "500");
	proto.addMetadata("buildcost", "2000");
	structuretype = new StructureType(proto);
	types.add(structuretype);

	proto = new StructureType.Prototype();
	for (int x = 0; x < 3; x++) {
	    for (int z = 0; z < 3; z++) {
		proto.addBlock(x + 1, 0, z + 1, Material.GOLD_BLOCK);
	    }
	}

	for (int x = 0; x < 5; x++) {
	    if (x == 2) {
		continue;
	    }
	    proto.addBlock(x, 0, 0, Material.STEP);
	    proto.addBlock(x, 0, 4, Material.STEP);
	}
	for (int z = 1; z < 4; z++) {
	    if (z == 2) {
		continue;
	    }
	    proto.addBlock(0, 0, z, Material.STEP);
	    proto.addBlock(4, 0, z, Material.STEP);
	}

	proto.addBlock(2, 1, 2, Material.DIAMOND_BLOCK);
	proto.addBlock(2, 2, 2, Material.DIAMOND_BLOCK);
	proto.addBlock(2, 3, 2, Material.DIAMOND_BLOCK);
	proto.addBlock(2, 4, 2, Material.DIAMOND_BLOCK);
	proto.setName("Global Crossworld Stone");
	proto.setRotator(Rotator.NONE);
	proto.addMetadata("type", "MEMORYSTONE");
	proto.addMetadata("crossworld", "true");
	proto.addMetadata("global", "true");
	proto.addMetadata("permissionRequired", "memorystone.create.crossworldglobal");
	proto.addMetadata("distanceLimit", "0");
	proto.addMetadata("teleportcost", "150");
	proto.addMetadata("memorizecost", "1000");
	proto.addMetadata("buildcost", "5000");
	structuretype = new StructureType(proto);
	types.add(structuretype);

	proto = new StructureType.Prototype();
	proto.addBlock(0, 0, 0, Material.GOLD_BLOCK);
	proto.addBlock(0, 0, 2, Material.GOLD_BLOCK);
	proto.addBlock(2, 0, 0, Material.GOLD_BLOCK);
	proto.addBlock(2, 0, 2, Material.GOLD_BLOCK);
	proto.addBlock(1, 0, 1, Material.DIAMOND_BLOCK);
	proto.addBlock(1, 1, 1, Material.DIAMOND_BLOCK);
	proto.setName("NoTeleport Stone");
	proto.setRotator(Rotator.NONE);
	proto.addMetadata("type", "NOTELEPORT");
	proto.addMetadata("global", "false");
	proto.addMetadata("permissionRequired", "memorystone.create.noteleport");
	proto.addMetadata("distanceLimit", "128");
	proto.addMetadata("buildcost", "10000");
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

    public MemoryStone getMemoryStructureForSign(Sign sign) {
	for (MemoryStone stone : namedMap.values()) {
	    if (stone.getSign() == null) {
		continue; // Next to impossible.. anyway. moving along
	    }

	    if (sign.getBlock().equals(stone.getSign().getBlock())) {
		return stone;
	    }
	}

	return null;
    }

    public MemoryStone getNamedMemoryStone(String name) {
	return namedMap.get(name);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.getBlock().getState() instanceof Sign) {
	    final Sign state = (Sign) event.getBlock().getState();
	    final MemoryStone stone = getMemoryStructureForSign(state);
	    if (stone != null) {
		// check permissions!
		if (!event.getPlayer().hasPermission("memorystone.break")) {
		    event.setCancelled(true);
		    event.getPlayer().sendMessage(Config.getColorLang("nobreakpermission"));

		    state.setLine(0, Config.getColorLang("signboard"));
		    state.setLine(1, stone.getName());
		    state.update(true);
		    return;
		}

		if (!state.getLine(1).equals(stone.getName())) {
		    return;
		}

		memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), true);
		namedMap.remove(stone.getName());
		globalStones.remove(stone.getName());
		removeWorldStone(stone);
		stone.setSign(null);
		memoryStonePlugin.getStructureManager().saveStructures();

	    }
	}
    }

    private double getCostPart(String priceLine, int part) {
	if (priceLine == null || priceLine.length() == 0) {
	    return 0;
	}

	try {
	    String[] split = priceLine.split("[/]");
	    if (part == 0) {
		return Double.parseDouble(split[0].trim());
	    }

	    if (part == 1) {
		if (split.length == 2) {
		    return Double.parseDouble(split[1].trim());
		}

	    }
	} catch (NumberFormatException e) {
	}

	return 0;
    }

    @Override
    public void onSignChange(final SignChangeEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	memoryStonePlugin.info("Processing sign change event");

	if (!(event.getBlock().getState() instanceof Sign)) {
	    return;
	}
	final Sign state = (Sign) event.getBlock().getState();
	MemoryStone stone = getMemoryStructureBehind(state);
	if (stone == null) {
	    // If stone isn't found, try initiate a 'create' on the block behind it.
	    org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) state.getData();
	    Block behind = state.getBlock().getRelative(aSign.getFacing().getOppositeFace());

	    Structure structure = memoryStonePlugin.getStructureManager().checkForStone(event.getPlayer(), behind);
	    if (structure != null) {
		stone = getMemoryStructureBehind(state);
	    }
	}

	if (stone != null && stone.getType().equals(StoneType.MEMORYSTONE)) {
	    // check permissions!
	    if (!event.getPlayer().hasPermission("memorystone.build")) {
		event.getPlayer().sendMessage(Config.getColorLang("nobuildpermission"));
		return;
	    }

	    if (stone.getSign() != null) {
		event.setLine(0, Config.getColorLang("signboard"));
		event.setLine(1, Config.getColorLang("broken"));
		return;
	    }

	    if (event.getLine(0).length() == 0) {
		event.setLine(0, Config.getColorLang("signboard"));
		event.setLine(1, Config.getColorLang("broken"));
		return;
	    }

	    String name = event.getLine(0);
	    String price = event.getLine(1);

	    if (namedMap.containsKey(name)) {
		event.setLine(0, Config.getColorLang("signboard"));
		event.setLine(1, Config.getColorLang("duplicate"));
		return;
	    }

	    event.setLine(0, Config.getColorLang("signboard"));
	    event.setLine(1, name);

	    stone.setSign(state);
	    stone.setName(name);

	    EconomyManager economyManager = memoryStonePlugin.getEconomyManager();
	    if (economyManager.isEconomyEnabled()) {
		if (Config.isEconomyAddCustomValue()) {
		    stone.setMemorizeCost(getCostPart(price, 0));
		    stone.setTeleportCost(getCostPart(price, 1));
		}

		event.setLine(2, economyManager.getFormattedCost(stone.getMemorizeCost()));
		event.setLine(3, economyManager.getFormattedCost(stone.getTeleportCost()));
	    }

	    namedMap.put(name, stone);
	    addWorldStone(stone);
	    if ("true".equals(stone.getStructure().getStructureType().getMetadata().get("global"))) {
		globalStones.add(stone);
	    }

	    final MemoryStone finalStone = stone;
	    memoryStonePlugin.getServer().getScheduler().scheduleSyncDelayedTask(memoryStonePlugin, new Runnable() {
		public void run() {
		    updateSign(state);
		    Sign newSign = (Sign) new Location(state.getWorld(), state.getX(), state.getY(), state.getZ())
			    .getBlock().getState();
		    finalStone.setSign(newSign);
		    memoryStonePlugin.getStructureManager().saveStructures();

		}
	    }, 2);
	    event.getPlayer().sendMessage(Utility.color(Config.getLang("signAdded")));
	}

	super.onSignChange(event);
    }

    public void updateSign(Sign s) {
	if (s.getType() != Material.WALL_SIGN) {
	    String name = s.getLine(1);
	    String price1 = s.getLine(2);
	    String price2 = s.getLine(3);
	    MaterialData m = s.getData();
	    BlockFace f = ((Directional) m).getFacing();
	    s.setType(Material.WALL_SIGN);
	    m = s.getData();
	    ((Directional) m).setFacingDirection(f);
	    s.setData(m);
	    s.update(true);

	    Sign newSign = (Sign) new Location(s.getWorld(), s.getX(), s.getY(), s.getZ()).getBlock().getState();
	    newSign.setLine(0, Config.getColorLang("signboard"));
	    newSign.setLine(1, name);
	    newSign.setLine(2, price1);
	    newSign.setLine(3, price2);

	    newSign.update(true);
	}

    }

    public List<MemoryStone> getGlobalStones() {
	return globalStones;
    }

    public Collection<? extends MemoryStone> getLocalStones(String world) {
	return worldStones.get(world);
    }

    public List<MemoryStone> getNoTeleportStones() {
	return noTeleportStones;
    }

    public Collection<MemoryStone> getStones() {
	return namedMap.values();
    }
}

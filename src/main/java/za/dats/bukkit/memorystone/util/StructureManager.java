package za.dats.bukkit.memorystone.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.memorystone.util.structure.BlockOffset;
import za.dats.bukkit.memorystone.util.structure.Rotator;
import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

/**
 * 
 * @author tim (Originally HTTotemManager)
 * @author cmdrdats
 */
public class StructureManager {
    private static final Logger log = Logger.getLogger("Minecraft");

    private final JavaPlugin plugin;
    private final StructureBlockListener blockListener;

    private final String structureTypes_filename = "structuretypes.yml";
    private final String structures_filename = "structures.yml";

    private List<StructureType> structureTypes;
    private List<Structure> structures;
    private List<StructureListener> listeners = new ArrayList<StructureListener>();

    HashMap<BlockHashable, Set<Structure>> blockhash;
    HashMap<String, Set<Structure>> ownerhash;

    private final String logPrefix;

    public StructureManager(JavaPlugin plugin, String logPrefix) {
	this.plugin = plugin;
	this.logPrefix = logPrefix;
	this.structureTypes = new ArrayList<StructureType>();
	this.structures = new ArrayList<Structure>();
	this.blockhash = new HashMap<BlockHashable, Set<Structure>>();
	this.ownerhash = new HashMap<String, Set<Structure>>();
	blockListener = new StructureBlockListener(plugin, this);
    }

    public void registerEvents() {
	blockListener.registerEvents();
	loadStructureTypesOrDefault();
	loadStructures();
    }

    public List<Structure> getStructures() {
	return new ArrayList<Structure>(this.structures);
    }

    public List<StructureType> getStructureTypes() {
	return new ArrayList<StructureType>(this.structureTypes);
    }

    public void addStructure(Player player, Structure structure) {
	this.structures.add(structure);

	// add to block hash
	for (Block block : structure.getBlocks()) {
	    BlockHashable bh = new BlockHashable(block);
	    Set<Structure> existing = this.blockhash.get(bh);
	    if (existing == null) {
		this.blockhash.put(bh, new HashSet<Structure>(Arrays.asList(structure)));
	    } else {
		existing.add(structure);
	    }
	}

	// add to owner hash
	String owner = structure.getOwner();
	Set<Structure> existing = this.ownerhash.get(owner);
	if (existing == null) {
	    this.ownerhash.put(owner, new HashSet<Structure>(Arrays.asList(structure)));
	} else {
	    existing.add(structure);
	}

	if (player != null) {
	    for (StructureListener listener : listeners) {
		listener.structurePlaced(player, structure);
	    }
	}
    }

    public void removeStructure(BlockBreakEvent event, Structure structure) {
	this.structures.remove(structure);

	// remove from block hash
	for (Block block : structure.getBlocks()) {
	    BlockHashable bh = new BlockHashable(block);
	    Set<Structure> existing = this.blockhash.get(bh);
	    existing.remove(structure);
	    if (existing.isEmpty()) {
		this.blockhash.remove(bh);
	    }
	}

	// remove from owner hash
	String owner = structure.getOwner();
	Set<Structure> existing = this.ownerhash.get(owner);
	existing.remove(structure);
	if (existing.isEmpty()) {
	    this.ownerhash.remove(owner);
	}

	for (StructureListener listener : listeners) {
	    listener.structureDestroyed(event.getPlayer(), structure);
	}

    }

    public Set<Structure> getStructuresFromBlock(Block block) {
	BlockHashable bh = new BlockHashable(block);
	Set<Structure> structureSet = this.blockhash.get(bh);
	if (structureSet == null)
	    return null;
	return new HashSet<Structure>(structureSet);
    }

    public Set<Structure> getStructuresFromPlayer(Player player) {
	String owner = player.getName();
	Set<Structure> structureSet = this.ownerhash.get(owner);
	if (structureSet == null)
	    return null;
	return new HashSet<Structure>(structureSet);
    }

    public StructureType getStructureType(String name) {
	for (StructureType type : this.structureTypes) {
	    if (type.getName().equals(name)) {
		return type;
	    }
	}
	return null;
    }

    // ---------- Structure type loading/saving ------------------
    public void loadStructureTypesOrDefault() {

	File structureTypesFile = new File(this.plugin.getDataFolder(), this.structureTypes_filename);
	if (!structureTypesFile.isFile()) {
	    try {
		structureTypesFile.getParentFile().mkdirs();
		structureTypesFile.createNewFile();
		this.saveDefaultStructureTypes();
	    } catch (Exception ex) {
		log.warning(logPrefix+"could not create file " + structureTypesFile.getName());
	    }
	}

	this.loadStructureTypes();
    }

    private void loadStructureTypes() {

	File structureTypesFile = new File(this.plugin.getDataFolder(), this.structureTypes_filename);
	Configuration conf = new Configuration(structureTypesFile);
	conf.load();

	List<ConfigurationNode> nodelist = conf.getNodeList("structuretypes", new ArrayList<ConfigurationNode>());

	for (ConfigurationNode node : nodelist) {
	    StructureType structureType = this.yaml2StructureType(node);
	    if (structureType == null) {
		log.warning(logPrefix+"a structure type couldn't be loaded");
	    } else {
		this.structureTypes.add(structureType);
	    }
	}

	/*
	 * Sort the StructureTypes by structure size. This way, larger totems will be found before smaller totems (and
	 * possibly subtotems).
	 */
	Collections.sort(this.structureTypes, new Comparator<StructureType>() {
	    public int compare(StructureType o1, StructureType o2) {
		return o1.getBlockCount() - o2.getBlockCount();
	    }
	});
	Collections.reverse(this.structureTypes);

	log.info(logPrefix+"loaded " + this.structureTypes.size() + " structure types");
    }

    private void saveDefaultStructureTypes() {

	File structureTypesFile = new File(this.plugin.getDataFolder(), this.structureTypes_filename);
	Configuration conf = new Configuration(structureTypesFile);

	List<StructureType> types = new ArrayList<StructureType>();
	for (StructureListener listener : listeners) {
	    listener.generatingDefaultStructureTypes(types);
	}

	List<Object> yamllist = new ArrayList<Object>();
	for (StructureType structureType : types) {
	    yamllist.add(structureType2yaml(structureType));
	}

	conf.setProperty("structuretypes", yamllist);
	conf.save();
    }

    private Map<String, Object> structureType2yaml(StructureType structuretype) {
	HashMap<String, Object> yamlmap = new HashMap<String, Object>();
	yamlmap.put("name", structuretype.getName());
	yamlmap.put("rotator", structuretype.getRotator().toString());
	yamlmap.put("structure", this.structureTypePattern2yaml(structuretype));
	yamlmap.put("metadata", structuretype.getMetadata());
	return yamlmap;
    }

    private List<Object> structureTypePattern2yaml(StructureType structuretype) {
	List<Object> yamllist = new ArrayList<Object>();
	for (BlockOffset offset : structuretype.getPattern().keySet()) {
	    Material material = structuretype.getPattern().get(offset);
	    HashMap<String, Object> part = new HashMap<String, Object>();
	    part.put("x", offset.getX());
	    part.put("y", offset.getY());
	    part.put("z", offset.getZ());
	    part.put("material", material.toString());
	    yamllist.add(part);
	}
	return yamllist;
    }

    private StructureType yaml2StructureType(ConfigurationNode node) {
	StructureType.Prototype prototype = new StructureType.Prototype();
	String name = node.getString("name", null);
	if (name == null) {
	    log.warning(logPrefix+"Structure type's name is not set");
	    return null;
	}
	prototype.setName(name);

	String rotatorstr = node.getString("rotator", null);
	if (rotatorstr == null) {
	    log.warning(logPrefix+"Structure type's rotator is not set");
	    rotatorstr = ":(";
	}

	Rotator rotator = Rotator.matchRotator(rotatorstr);
	if (rotator == null) {
	    log.warning(logPrefix+"Structure type's rotator is not valid, using default");
	    rotator = Rotator.getDefault();
	}
	prototype.setRotator(rotator);

	List<ConfigurationNode> structuretypenodes = node.getNodeList("structure", new ArrayList<ConfigurationNode>());
	if (structuretypenodes.isEmpty()) {
	    log.warning(logPrefix+"Structure type's structure is not set");
	    return null;
	}

	for (ConfigurationNode structureNode : structuretypenodes) {
	    int x = structureNode.getInt("x", Integer.MIN_VALUE);
	    int y = structureNode.getInt("y", Integer.MIN_VALUE);
	    int z = structureNode.getInt("z", Integer.MIN_VALUE);
	    if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
		log.warning(logPrefix+"Structure's x, y, or z is not set");
	    }

	    String materialstr = structureNode.getString("material", null);
	    if (materialstr == null) {
		log.warning(logPrefix+"Structure's material is not set");
	    }

	    Material material = Material.matchMaterial(materialstr);
	    if (material == null) {
		log.warning(logPrefix+"Structure's material is not recognized");
	    }

	    prototype.addBlock(x, y, z, material);
	}

	prototype.setMetadata((Map<String, String>) node.getProperty("metadata"));

	if (prototype.getBlockCount() < 3) {
	    log.warning(logPrefix+"For technical reasons, the structure's block count must be at least 3");
	    return null;
	}

	return new StructureType(prototype);
    }

    // ---------- Structure loading/saving -----------------------
    public void loadStructures() {

	File structuresFile = new File(this.plugin.getDataFolder(), this.structures_filename);
	Configuration conf = new Configuration(structuresFile);
	conf.load();

	List<ConfigurationNode> nodelist = conf.getNodeList("structures", new ArrayList<ConfigurationNode>());

	for (ConfigurationNode node : nodelist) {
	    Structure structure = this.yaml2Structure(node);

	    if (structure == null) {
		log.warning(logPrefix+"A structure couldn't be loaded");
	    } else {
		for (StructureListener listener : listeners) {
		    listener.structureLoaded(structure, node);
		}

		this.addStructure(null, structure);
	    }
	}

	log.info(logPrefix+"Loaded " + this.structures.size() + " structure(s)");
    }

    public void saveStructures() {

	File structuresfile = new File(this.plugin.getDataFolder(), this.structures_filename);
	Configuration conf = new Configuration(structuresfile);

	List<Object> yamllist = new ArrayList<Object>();

	for (Structure structure : this.structures) {
	    Map<String, Object> structure2yaml = this.structure2yaml(structure);

	    for (StructureListener listener : listeners) {
		listener.structureSaving(structure, structure2yaml);
	    }

	    yamllist.add(structure2yaml);
	}

	log.info(logPrefix+"Saved " + this.structures.size() + " structures");

	conf.setProperty("structures", yamllist);
	conf.save();
    }

    private Map<String, Object> structure2yaml(Structure structure) {
	HashMap<String, Object> yamlmap = new HashMap<String, Object>();
	yamlmap.put("world", structure.getRootBlock().getWorld().getName());
	yamlmap.put("x", structure.getRootBlock().getX());
	yamlmap.put("y", structure.getRootBlock().getY());
	yamlmap.put("z", structure.getRootBlock().getZ());
	yamlmap.put("type", structure.getStructureType().getName());

	String owner = structure.getOwner();
	if (structure.getOwner() != null) {
	    yamlmap.put("owner", owner);
	}

	return yamlmap;
    }

    private Structure yaml2Structure(ConfigurationNode node) {
	String name = node.getString("name", "structure");
	String worldstr = node.getString("world", null);
	if (worldstr == null) {
	    log.warning(logPrefix+name+": world is not set");
	    return null;
	}

	int x = node.getInt("x", Integer.MIN_VALUE);
	int y = node.getInt("y", Integer.MIN_VALUE);
	int z = node.getInt("z", Integer.MIN_VALUE);
	if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
	    log.warning(logPrefix+name+": x, y, or z is not set");
	    return null;
	}

	String structureTypeStr = node.getString("type", null);
	if (structureTypeStr == null) {
	    log.warning(logPrefix+name+": type is not set");
	    return null;
	}

	String owner = node.getString("owner", null);
	if (owner == null) {
	    // log.warning("totem's owner is not set");
	    // do nothing
	}

	World world = this.plugin.getServer().getWorld(worldstr);
	if (world == null) {
	    log.warning(logPrefix+name+": world is not recognized");
	    return null;
	}

	StructureType structureType = this.getStructureType(structureTypeStr);
	if (structureType == null) {
	    log.warning(logPrefix+name+": type of "+structureTypeStr+" is not recognized");
	    return null;
	}

	Block block = world.getBlockAt(x, y, z);
	Structure structure = new Structure(structureType, block, owner);
	if (!structure.verifyStructure()) {
	    log.warning(logPrefix+name+": structure was bad");
	    return null;
	}

	return structure;
    }

    public void addStructureListener(StructureListener listener) {
	listeners.add(listener);
    }

    public void removeStructureListener(StructureListener listener) {
	listeners.remove(listener);
    }

    public Structure checkForStone(Player player, Block behind) {
	return blockListener.checkPlacedBlock(player, behind, null);
    }
}

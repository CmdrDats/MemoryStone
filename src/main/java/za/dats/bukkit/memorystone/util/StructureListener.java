package za.dats.bukkit.memorystone.util;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

/**
 * This is technically a bit more of a visitor than just a listener.. oh well.
 * 
 * @author cmdrdats
 *
 */
public interface StructureListener {
    /**
     * A structure has been physically placed in the world
     * 
     * @param structure
     */
    public void structurePlaced(Player player, Structure structure);
    
    /**
     * A structure has been destroyed
     * 
     * @param structure
     */
    public void structureDestroyed(Player player, Structure structure);
    
    /**
     * A structure was loaded from disk. Use the configuration node to load
     * the rest of this structures properties as if placed
     * 
     * @param structure
     */
    public void structureLoaded(Structure structure, ConfigurationNode node);

    /**
     * Structure is being saved to disk, add extra structure information to save with it
     * so that you can load it again later.
     * 
     * @param structure
     * @param yamlMap 
     */
    public void structureSaving(Structure structure, Map<String, Object> yamlMap);
    
    /**
     * Add your own default structure types in here.
     * 
     * @param types
     */
    public void generatingDefaultStructureTypes(List<StructureType> types);
    
}

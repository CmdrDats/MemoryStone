package za.dats.bukkit.memorystone.util.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * @author tim
 * @author CmdrDats
 */
public class Structure {

    protected final StructureType structuretype;
    protected final Block rootblock;
    protected final Set<Block> blocks;
    protected String owner;

    public Structure(StructureType possiblestructuretype, Block block, String owner) {
        this(new ArrayList<StructureType>(Arrays.asList(possiblestructuretype)), block, owner);
    }

    public Structure(List<StructureType> structuretypes, Block block, String owner) {

        for (StructureType possiblestructuretype : structuretypes) {
            Map<BlockOffset, Material> pattern = possiblestructuretype.getPattern();

            if (pattern.containsValue(block.getType())) {
                Map<Material, List<BlockOffset>> reversepattern = possiblestructuretype.getReversePattern();
                List<BlockOffset> offsets = reversepattern.get(block.getType());

                for (BlockOffset offset : offsets) {
                    Block possiblerootblock = block.getRelative(-offset.x, -offset.y, -offset.z);
                    Set<Block> possibleblocks = this.verifyStructure(possiblestructuretype, possiblerootblock);

                    if (possibleblocks != null) {
                        this.structuretype = possiblestructuretype;
                        this.rootblock = possiblerootblock;
                        this.blocks = possibleblocks;
                        return;
                    }
                }
            }
        }

        this.structuretype = null;
        this.rootblock = null;
        this.blocks = null;
        this.owner = owner;
    }

    public StructureType getStructureType() {
        return this.structuretype;
    }

    public Block getRootBlock() {
        return this.rootblock;
    }

    public Set<Block> getBlocks() {
        // defend against outisde adding/removing
        return new HashSet<Block>(this.blocks);
    }

    public World getWorld() {
        return this.rootblock.getWorld();
    }

    public boolean containsBlock(Block block) {
        return this.blocks.contains(block);
    }

    public boolean verifyStructure() {
        return (this.structuretype != null && this.rootblock != null && this.blocks != null);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * TODO THIS SHOULD GET MOVED INTO THE CONSTRUCTOR.
     * <p>
     * Verify that a structure exists at this root block.
     *
     * @param rootblock The origin of the structure.
     * @return If the structure is valid return the list of blocks, or if the structure was not valid return null.
     */
    private Set<Block> verifyStructure(StructureType structuretype, Block rootblock) {

        if (rootblock == null)
            return null;

        Map<BlockOffset, Material> pattern = structuretype.getPattern();
        Set<Block> possibleblocks = new HashSet<Block>();

        for (BlockOffset offset : pattern.keySet()) {
            Block block = rootblock.getRelative(offset.x, offset.y, offset.z);

            Material material = pattern.get(offset);
            if (block.getType() != material) {
                return null;
            } else {
                possibleblocks.add(block);
            }
        }

        return possibleblocks;
    }
}

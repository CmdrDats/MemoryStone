package za.dats.bukkit.memorystone.util;

import org.bukkit.block.Block;

/**
 * TODO merge with BlockOffset?
 * 
 * Immutable and ok to use as a hash key.
 * 
 * @author tim
 */
public final class BlockHashable {

    protected final String world;
    protected final int x;
    protected final int y;
    protected final int z;

    public BlockHashable(Block block) {
	this.world = block.getWorld().getName();
	this.x = block.getX();
	this.y = block.getY();
	this.z = block.getZ();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof BlockHashable))
	    return false;
	BlockHashable bh = (BlockHashable) o;
	return (this.x == bh.x && this.y == bh.y && this.z == bh.z && this.world.equals(bh.world));
    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 53 * hash + (this.world != null ? this.world.hashCode() : 0);
	hash = 53 * hash + this.x;
	hash = 53 * hash + this.y;
	hash = 53 * hash + this.z;
	return hash;
    }

    @Override
    public String toString() {
	return "(" + this.world + ") <" + this.x + ", " + this.y + ", " + this.z + ">";
    }
}
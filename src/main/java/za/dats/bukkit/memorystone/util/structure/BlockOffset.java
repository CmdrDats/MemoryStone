package za.dats.bukkit.memorystone.util.structure;

import org.bukkit.block.Block;

/**
 * An immutable type representing a block offset. Simply an integer triple of
 * <x, y, z>. There is no "world" member. Mostly used for getting "relative"
 * blocks.
 *
 * @author tim
 */
public final class BlockOffset {

	protected final int x;
	protected final int y;
	protected final int z;

	public BlockOffset(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockOffset(BlockOffset offset){
		this.x = offset.x;
		this.y = offset.y;
		this.z = offset.z;
	}

	public BlockOffset(Block block){
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
	}

	public int getX(){
		return this.x;
	}

	public int getY(){
		return this.y;
	}

	public int getZ(){
		return this.z;
	}

	public BlockOffset add(BlockOffset o){
		return new BlockOffset(this.x + o.x, this.y + o.y, this.z + o.z);
	}

	public BlockOffset subtract(BlockOffset o){
		return new BlockOffset(this.x - o.x, this.y - o.y, this.z - o.z);
	}

	@Override
	public String toString(){
		return "<" + this.x + ", " + this.y + ", " + this.z + ">";
	}
}

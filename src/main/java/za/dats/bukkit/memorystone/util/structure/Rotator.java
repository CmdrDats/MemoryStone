package za.dats.bukkit.memorystone.util.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tim
 */
public enum Rotator {
	NONE(Arrays.asList(
	new BlockOffset(0, 0, 0))),
	Y_ONLY(Arrays.asList(
	new BlockOffset(0, 0, 0),
	new BlockOffset(0, 1, 0),
	new BlockOffset(0, 2, 0),
	new BlockOffset(0, 3, 0))),
	ALL(Arrays.asList(
	new BlockOffset(0, 0, 0),
	new BlockOffset(0, 0, 1),
	new BlockOffset(0, 0, 2),
	new BlockOffset(0, 0, 3),
	new BlockOffset(0, 1, 0),
	new BlockOffset(0, 1, 1),
	new BlockOffset(0, 1, 2),
	new BlockOffset(0, 1, 3),
	new BlockOffset(0, 2, 0),
	new BlockOffset(0, 2, 1),
	new BlockOffset(0, 2, 2),
	new BlockOffset(0, 2, 3),
	new BlockOffset(0, 3, 0),
	new BlockOffset(0, 3, 1),
	new BlockOffset(0, 3, 2),
	new BlockOffset(0, 3, 3),
	new BlockOffset(1, 0, 0),
	new BlockOffset(1, 0, 1),
	new BlockOffset(1, 0, 2),
	new BlockOffset(1, 0, 3),
	new BlockOffset(1, 2, 0),
	new BlockOffset(1, 2, 1),
	new BlockOffset(1, 2, 2),
	new BlockOffset(1, 2, 3)));

	/*
	 * This is a little speedup I'm using for the rotations. If we were to brute
	 * force all the possible rotations / orientations, we would get alot of
	 * duplicates. Since we only care about pi/2 rotations, it's easy enough to
	 * just list them all. As before 0 = 0, 1 = pi/2, 2 = pi, 3 = 3pi/2.
	 *
	 * It's also important that the rotations are apllied in order of: X, Y, and
	 * then Z. Or as rotations: R = Rz * Ry * Rx, pnew = R * p.
	 */

	private static final Map<String, Rotator> lookupname = new HashMap<String, Rotator>();

	static {
		for(Rotator rotator : Rotator.values()){
			Rotator.lookupname.put(rotator.name(), rotator);
		}
	}

	// i'm using BlockOffset to double as an int trio here...
	private final List<BlockOffset> rotlist;

	Rotator(List<BlockOffset> rotlist){
		this.rotlist = rotlist;
	}

	/**
	 * Get the default Rotator.
	 *
	 * @return The default rotator.
	 */
	public static Rotator getDefault(){
		return Rotator.NONE;
	}

	/**
	 * Get a rotator by its enum name.
	 *
	 * @param name The rotator's name (in the enum).
	 * @return The requested rotator, or null.
	 */
	public static Rotator getRotator(String name){
		return lookupname.get(name);
	}

	/**
	 * Try to match a rotator name.
	 *
	 * @param name The rotator's name.
	 * @return The requested rotator, or null.
	 */
	public static Rotator matchRotator(String name){
		String filtered = name.toUpperCase();
		filtered = filtered.replaceAll("\\s+", "_").replaceAll("\\W", "");
		return Rotator.lookupname.get(filtered);
	}

	/**
	 * Given an offset, find all the rotated offsets.
	 *
	 * @param offset The offset to rotate.
	 * @return A list of rotated offsets (including the original).
	 */
	public List<BlockOffset> getRotated(BlockOffset offset){
		List<BlockOffset> rotoffsets = new ArrayList<BlockOffset>();

		for(BlockOffset rotcount : this.rotlist){
			rotoffsets.add(this.rotateXYZ(offset,
							rotcount.x, rotcount.y, rotcount.z));
		}

		return rotoffsets;
	}

	/**
	 * Get the number of rotations for this rotator.
	 *
	 * @return The number of rotations.
	 */
	public int getNumberOfRotations(){
		return this.rotlist.size();
	}

	/**
	 * This function is a little different. It rotates (in pi/2 increments) the
	 * BlockVector about the X, Y, and Z axes. This method should be pretty quick
	 * because it's just a bunch of assignments.
	 *
	 * This is equal to: R = Rz * Ry * Rx, bo_new = R * bo
	 *
	 * @param xcount The number of pi/2 rotations about the x axis.
	 * @param ycount The number of pi/2 rotations about the y axis.
	 * @param zcount The number of pi/2 rotations about the z axis.
	 * @return A new rotated BlockVector.
	 */
	private BlockOffset rotateXYZ(BlockOffset offset, int xcount, int ycount, int zcount){

		int xp = offset.x;
		int yp = offset.y;
		int zp = offset.z;
		int xtmp = xp;
		int ytmp = yp;
		int ztmp = zp;

		for(int i = 0; i < xcount; i++){
			// rotate pi/2 about x axis
			zp = ytmp;
			yp = -ztmp;
			ztmp = zp;
			ytmp = yp;
		}

		for(int j = 0; j < ycount; j++){
			// rotate pi/2 about y axis
			zp = -xtmp;
			xp = ztmp;
			ztmp = zp;
			xtmp = xp;
		}

		for(int k = 0; k < zcount; k++){
			// rotate pi/2 about z axis
			yp = xtmp;
			xp = -ytmp;
			xtmp = xp;
			ytmp = yp;
		}

		return new BlockOffset(xp, yp, zp);
	}
}

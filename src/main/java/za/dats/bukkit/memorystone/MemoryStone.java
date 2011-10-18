package za.dats.bukkit.memorystone;

import org.bukkit.block.Sign;

import za.dats.bukkit.memorystone.util.structure.Structure;

public class MemoryStone implements Comparable<MemoryStone> {
    public enum StoneType {
	MEMORYSTONE, NOTELEPORT
    }

    private Structure structure;
    private Sign sign;
    private String name;
    private double distanceLimit;
    private StoneType type = StoneType.MEMORYSTONE;
    private double teleportCost;
    private double memorizeCost;

    public Structure getStructure() {
	return structure;
    }

    public void setStructure(Structure structure) {
	this.structure = structure;
	try {
	    if (structure.getStructureType().getMetadata().containsKey("distanceLimit")) {
		distanceLimit = Double.parseDouble(structure.getStructureType().getMetadata().get("distanceLimit"));
		distanceLimit = distanceLimit * distanceLimit; // pre square it.
	    } else {
		distanceLimit = 0;
	    }

	} catch (NumberFormatException nfe) {
	    distanceLimit = 0;
	}

	if (structure.getStructureType().getMetadata().containsKey("type")) {

	    StoneType newType = StoneType.valueOf(structure.getStructureType().getMetadata().get("type"));
	    if (newType != null) {
		type = newType;
	    }
	}
    }

    public Sign getSign() {
	return sign;
    }

    public void setSign(Sign sign) {
	if (sign != null) {
	    name = sign.getLine(1);
	}
	this.sign = sign;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    @Override
    public int hashCode() {
	if (name == null) {
	    return super.hashCode();
	}
	
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
	if (obj instanceof String) {
	    String other = (String) obj;
	    if (other.equals(name)) {
		return true;
	    }
	} else if (obj instanceof MemoryStone) {
	    MemoryStone other = (MemoryStone) obj;
	    if (other.name != null && other.name.equals(name)) {
		return true;
	    }
	}

	return super.equals(obj);
    }

    public int compareTo(MemoryStone o) {
	if (name != null) {
	    if (o.name == null) {
		return 1;
	    }
	    
	    if (name.equals(o.name)) {
		return 0;
	    }

	    return name.compareToIgnoreCase(o.name);
	}
	return -1;
    }

    public boolean isGlobal() {
	if ("true".equals(structure.getStructureType().getMetadata().get("global"))) {
	    return true;
	}

	return false;
    }

    public boolean isCrossWorld() {
	if ("true".equals(structure.getStructureType().getMetadata().get("crossworld"))) {
	    return true;
	}

	return false;
    }

    public double getDistanceLimit() {
	return distanceLimit;
    }

    public StoneType getType() {
	return type;
    }

    public double getTeleportCost() {
	String baseCostString = structure.getStructureType().getMetadata().get("teleportcost");
	if (baseCostString != null) {
	    try {
		return teleportCost + Double.parseDouble(baseCostString);
	    } catch (NumberFormatException e) {
	    }
	}
	
        return teleportCost;
    }

    public void setTeleportCost(double teleportCost) {
        this.teleportCost = teleportCost;
    }

    public double getMemorizeCost() {
    	if(!Config.isEconomyEnabled()) return 0;
	String baseCostString = structure.getStructureType().getMetadata().get("memorizecost");
	if (baseCostString != null) {
	    try {
		return memorizeCost + Double.parseDouble(baseCostString);
	    } catch (NumberFormatException e) {
	    }
	}
	
        return memorizeCost;
    }

    public void setMemorizeCost(double memorizeCost) {
        this.memorizeCost = memorizeCost;
    }

    public double getRawTeleportCost() {
	return teleportCost;
    }

    public double getRawMemorizetCost() {
	return memorizeCost;
    }

    
}

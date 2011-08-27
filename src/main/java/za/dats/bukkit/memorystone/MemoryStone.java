package za.dats.bukkit.memorystone;

import org.bukkit.block.Sign;

import za.dats.bukkit.memorystone.util.structure.Structure;

public class MemoryStone {
    private Structure structure;
    private Sign sign;
    private String name;

    public Structure getStructure() {
	return structure;
    }

    public void setStructure(Structure structure) {
	this.structure = structure;
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

    
}

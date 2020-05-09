package za.dats.bukkit.memorystone;

import org.bukkit.block.Sign;

import za.dats.bukkit.memorystone.util.structure.Structure;

public class MemoryStone implements Comparable<MemoryStone> {
    public enum StoneType {
        MEMORYSTONE
    }

    private Structure structure;
    private Sign sign;
    private String name;
    private StoneType type = StoneType.MEMORYSTONE;

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;

        if (structure.getStructureType().getMetadata().containsKey("type")) {
            type = StoneType.valueOf(structure.getStructureType().getMetadata().get("type"));
        }
    }

    public Sign getSign() {
        return sign;
    }

    public void setSign(Sign sign) {
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
        return "true".equals(structure.getStructureType().getMetadata().get("global"));
    }

    public boolean isCrossWorld() {
        return "true".equals(structure.getStructureType().getMetadata().get("crossworld"));
    }

    public StoneType getType() {
        return type;
    }

}

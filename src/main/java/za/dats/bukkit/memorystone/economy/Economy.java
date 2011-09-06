package za.dats.bukkit.memorystone.economy;

import org.bukkit.entity.Player;

import za.dats.bukkit.memorystone.MemoryStone;

public class Economy {
    public void load() {
	
    }
   
    public boolean payFrom(Player player, double cost) {
	return true;
    }

    public void payTo(String playerName, double cost) {
    }

    public String format(double cost) {
	return ""+((int)cost);
    }
    
}

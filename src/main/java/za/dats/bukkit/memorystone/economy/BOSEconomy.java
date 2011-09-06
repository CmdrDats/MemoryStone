package za.dats.bukkit.memorystone.economy;

import org.bukkit.plugin.Plugin;

import za.dats.bukkit.memorystone.MemoryStonePlugin;

public class BOSEconomy extends Economy {
    cosine.boseconomy.BOSEconomy economy = null;
    
    public BOSEconomy(Plugin plugin) {
	economy = (cosine.boseconomy.BOSEconomy)plugin;
	MemoryStonePlugin.getInstance().info("Hooked into BOSEconomy");
    }
    
    @Override
    public String format(double cost) {
	return economy.getMoneyFormatted(cost);
    }
    
    public boolean payFrom(org.bukkit.entity.Player player, double cost) {
	if (economy.getPlayerMoneyDouble(player.getName()) >= cost) {
	    return economy.addPlayerMoney(player.getName(), 0-cost, true);
	}
	
	return false;
    };
    
    public void payTo(String playerName, double cost) {
	economy.addPlayerMoney(playerName, cost, false);
    };
    
}

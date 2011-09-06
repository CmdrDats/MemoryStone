package za.dats.bukkit.memorystone.economy;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import za.dats.bukkit.memorystone.Config;
import za.dats.bukkit.memorystone.MemoryStone;
import za.dats.bukkit.memorystone.MemoryStonePlugin;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class EconomyManager {
    private Economy economy;

    public void loadEconomy() {
	economy = null;

	loadPlugin();

	if (economy != null) {
	    economy.load();
	}
    }

    private void loadPlugin() {
	Plugin plugin = MemoryStonePlugin.getInstance().getServer().getPluginManager().getPlugin("iConomy");
	if (plugin != null && plugin.isEnabled()) {
	    economy = new IConomy(plugin);
	    return;
	}

	plugin = MemoryStonePlugin.getInstance().getServer().getPluginManager().getPlugin("BOSEconomy");
	if (plugin != null && plugin.isEnabled()) {
	    economy = new BOSEconomy(plugin);
	    return;
	}

    }

    public boolean isEconomyEnabled() {
	if (Config.isEconomyEnabled() && economy != null) {
	    return true;
	}

	return false;
    }

    public String getFormattedCost(double cost) {
	return economy.format(cost);
    }

    public String getBuildCostString(StructureType structureType) {
	return economy.format(getBuildCost(structureType));
    }

    private double getBuildCost(StructureType structureType) {
	Map<String, String> meta = structureType.getMetadata();
	double result = 0;
	if (meta.containsKey("buildcost")) {
	    try {
		result += Double.parseDouble(meta.get("buildcost"));
	    } catch (NumberFormatException e) {
	    }
	}

	return result;
    }

    public boolean payTeleportCost(Player player, MemoryStone stone) {
	double cost = stone.getTeleportCost();
	if (economy.payFrom(player, cost)) {
	    if (Config.isEconomyOwnerPaid()) {
		String owner = stone.getStructure().getOwner();
		if (owner == null || stone.getStructure().getOwner().length() == 0) {
		    return true;
		}

		economy.payTo(owner, cost);

		return true;
	    }
	    return true;
	}

	return false;
    }

    public boolean payMemorizeCost(Player player, MemoryStone stone) {
	double cost = stone.getMemorizeCost();
	if (economy.payFrom(player, cost)) {
	    if (Config.isEconomyOwnerPaid()) {
		String owner = stone.getStructure().getOwner();
		if (owner == null || stone.getStructure().getOwner().length() == 0) {
		    return true;
		}

		economy.payTo(owner, cost);

		return true;
	    }
	    return true;
	}

	return false;
    }

    public boolean payBuildCost(Player player, StructureType stone) {
	return economy.payFrom(player, getBuildCost(stone));
    }
}

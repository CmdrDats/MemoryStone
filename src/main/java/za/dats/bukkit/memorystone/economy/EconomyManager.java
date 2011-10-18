package za.dats.bukkit.memorystone.economy;

import java.util.Map;
import org.bukkit.entity.Player;
import za.dats.bukkit.memorystone.Config;
import za.dats.bukkit.memorystone.MemoryStone;
import za.dats.bukkit.memorystone.MemoryStonePlugin;
import za.dats.bukkit.memorystone.economy.payment.Method.MethodAccount;
import za.dats.bukkit.memorystone.economy.payment.Methods;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class EconomyManager {
    public void loadEconomy() {
	loadPlugin();
    }

    public void unloadEconomy() {
	Methods.reset();
    }

    private void loadPlugin() {
	Methods.setVersion(MemoryStonePlugin.getInstance().getDescription().getVersion());

	Methods.setMethod(MemoryStonePlugin.getInstance().getServer().getPluginManager());

	if (Methods.getMethod() == null) {
	    MemoryStonePlugin.getInstance().warn("No Register Method Found. Economy Disabled");
	} else {
	    MemoryStonePlugin.getInstance().info("Hooked into: "+Methods.getMethod().getName()+" "+Methods.getMethod().getVersion());
	}
    }

    public boolean isEconomyEnabled() {
	if (Config.isEconomyEnabled() && Methods.getMethod() != null) {
	    return true;
	}

	return false;
    }

    public String getFormattedCost(double cost) {
	return Methods.getMethod().format(cost);
    }

    public String getBuildCostString(StructureType structureType) {
	return Methods.getMethod().format(getBuildCost(structureType));
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
	MethodAccount account = Methods.getMethod().getAccount(player.getName());
	if (account == null) {
	    return false;
	}

	double cost = stone.getTeleportCost();
	if (!account.hasEnough(cost)) {
	    return false;
	}

	if (account.subtract(cost)) {
	    if (Config.isEconomyOwnerPaid()) {
		String owner = stone.getStructure().getOwner();
		if (owner == null || stone.getStructure().getOwner().length() == 0) {
		    return true;
		}

		MethodAccount ownerAccount = Methods.getMethod().getAccount(owner);
		if (ownerAccount != null) {
		    ownerAccount.add(cost);
		}
	    }

	    return true;
	}

	return false;
    }

    public boolean payMemorizeCost(Player player, MemoryStone stone) {
	MethodAccount account = Methods.getMethod().getAccount(player.getName());
	if (account == null) {
	    return false;
	}

	double cost = stone.getMemorizeCost();
	if (!account.hasEnough(cost)) {
	    return false;
	}

	if (account.subtract(cost)) {
	    if (Config.isEconomyOwnerPaid()) {
		String owner = stone.getStructure().getOwner();
		if (owner == null || stone.getStructure().getOwner().length() == 0) {
		    return true;
		}

		MethodAccount ownerAccount = Methods.getMethod().getAccount(owner);
		if (ownerAccount != null) {
		    ownerAccount.add(cost);
		}
	    }

	    return true;
	}

	return false;
    }

    public boolean payBuildCost(Player player, StructureType stone) {
	MethodAccount account = Methods.getMethod().getAccount(player.getName());
	if (account == null) {
	    return false;
	}

	double cost = getBuildCost(stone);
	if (!account.hasEnough(cost)) {
	    return false;
	}

	if (account.subtract(cost)) {
	    return true;
	}

	return false;
    }
}

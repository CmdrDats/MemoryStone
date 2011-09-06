package za.dats.bukkit.memorystone.economy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.iConomy.system.Account;
import com.iConomy.system.Holdings;

import za.dats.bukkit.memorystone.MemoryStonePlugin;

public class IConomy extends Economy {
    private com.iConomy.iConomy iConomy = null;

    public IConomy(Plugin plugin) {
	IConomy.this.iConomy = (com.iConomy.iConomy) plugin;
	MemoryStonePlugin.getInstance().info("Hooked into iConomy");
    }

    @Override
    public String format(double cost) {
	return iConomy.format(cost);
    }

    @Override
    public boolean payFrom(Player player, double cost) {
	Account account = iConomy.getAccount(player.getName());
	if (account == null) {
	    return false;
	}

	Holdings holdings = account.getHoldings();
	if (!holdings.hasEnough(cost)) {
	    return false;
	}

	holdings.subtract(cost);
	return true;
    }

    @Override
    public void payTo(String playerName, double cost) {
	Account account = iConomy.getAccount(playerName);
	if (account == null) {
	    return;
	}

	Holdings holdings = account.getHoldings();

	holdings.add(cost);
    }
}

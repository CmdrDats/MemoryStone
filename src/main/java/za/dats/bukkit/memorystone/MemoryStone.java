package za.dats.bukkit.memorystone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import za.dats.bukkit.memorystone.commands.EchoCommand;
import za.dats.bukkit.memorystone.listeners.MemoryStoneSignListener;

public class MemoryStone extends JavaPlugin {
    private PluginDescriptionFile pdf;
    private PluginManager pm;
    private MemoryStoneSignListener signListener = new MemoryStoneSignListener(this);
    private static Block memorized;

    public void onDisable() {
    }

    public void onEnable() {
	Utility.init(this);
	pm = getServer().getPluginManager();
	pdf = getDescription();

	System.out.println(pdf.getName() + " version " + pdf.getVersion() + " is enabled!");
	getCommand("echo").setExecutor(new EchoCommand(this));

	pm.registerEvent(Event.Type.SIGN_CHANGE, signListener, Event.Priority.Normal, this);
	pm.registerEvent(Event.Type.PLAYER_INTERACT, new PlayerListener() {
	    @Override
	    public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getPlayer().getItemInHand().getType() != Material.COMPASS) {
		    return;
		}

		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
		    if (event.getClickedBlock().getState() instanceof Sign) {
			event.getPlayer().sendMessage(
				"Memorized " + ((Sign) event.getClickedBlock().getState()).getLine(1));
		 	memorized = event.getClickedBlock();
		 	return;
		    }
		}
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {

		    if (memorized != null) {
			event.getPlayer().sendMessage("Recalling");

			event.getPlayer()
				.teleport(
					new Location(memorized.getWorld(), memorized.getX(), memorized.getY(),
						memorized.getZ()));
		    } else {
			event.getPlayer().sendMessage("No Memorized recalling");

		    }

		}

		super.onPlayerInteract(event);
	    }
	}, Priority.Normal, this);
    }

    public boolean hasSpout() {
	if (getServer().getPluginManager().isPluginEnabled("Spout")) {
	    return true;
	}
	return false;
    }

}

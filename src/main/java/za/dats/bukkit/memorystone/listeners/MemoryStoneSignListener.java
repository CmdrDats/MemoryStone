package za.dats.bukkit.memorystone.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import za.dats.bukkit.memorystone.MemoryStone;
import za.dats.bukkit.memorystone.Utility;

public class MemoryStoneSignListener extends BlockListener {

    private final MemoryStone memoryStone;

    public MemoryStoneSignListener(MemoryStone memoryStone) {
	this.memoryStone = memoryStone;
    }

    @Override
    public void onSignChange(final SignChangeEvent event) {
	event.getPlayer().sendMessage("Placed sign!");
	if (!event.getLine(0).equals("MemoryStone")) {
	    return;
	}

	if (event.getLine(1).length() == 0) {
	    return;
	}

	if (checkMemoryStructureBehind((Sign) event.getBlock().getState())) {
	    event.setLine(0, Utility.color("&A") + "[MemoryStone]");
	    event.setLine(1, Utility.color("&E") + event.getLine(1));
	    memoryStone.getServer().getScheduler().scheduleSyncDelayedTask(memoryStone, new Runnable() {
		public void run() {
		    updateSign((Sign) event.getBlock().getState());
		}
	    }, 2);
	    event.getPlayer().sendMessage(Utility.color("&EMemory Stone created."));
	} else {
	    event.getPlayer().sendMessage(Utility.color("&CIncorrect structure for Memory Stone."));
	    event.setLine(0, Utility.color("&C[Broken]"));
	    return;
	}

	super.onSignChange(event);
    }

    private boolean checkMemoryStructureBehind(Sign sign) {
	org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
	Block behind = sign.getBlock().getRelative(aSign.getFacing().getOppositeFace());
	if (behind.getType() == Material.DIRT) {
	    return true;
	}
	return false;
    }

    public void updateSign(Sign s) {
	if (s.getType() != Material.WALL_SIGN) {
	    String name = s.getLine(1);
	    MaterialData m = s.getData();
	    BlockFace f = ((Directional) m).getFacing();
	    s.setType(Material.WALL_SIGN);
	    m = s.getData();
	    ((Directional) m).setFacingDirection(f);
	    s.setData(m);
	    s.update(true);
	    
	    Sign newSign = (Sign) new Location(s.getWorld(), s.getX(), s.getY(), s.getZ()).getBlock().getState();
	    newSign.setLine(0, Utility.color("&A") + "[MemoryStone]");
	    newSign.setLine(1, Utility.color("&E") + name);
	    newSign.update(true);
	}

    }
}

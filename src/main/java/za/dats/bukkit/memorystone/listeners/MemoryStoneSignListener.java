package za.dats.bukkit.memorystone.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import za.dats.bukkit.memorystone.MemoryStone;
import za.dats.bukkit.memorystone.MemoryStonePlugin;
import za.dats.bukkit.memorystone.Utility;

public class MemoryStoneSignListener extends BlockListener {

    private final MemoryStonePlugin memoryStone;

    public MemoryStoneSignListener(MemoryStonePlugin memoryStone) {
	this.memoryStone = memoryStone;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	if (event.getBlock().getState() instanceof Sign) {
	    final Sign state = (Sign) event.getBlock().getState();
	    final MemoryStone stone = getMemoryStructureBehind(state);
	    if (stone != null) {
		stone.setSign(null);
	    }
	}
    }

    @Override
    public void onSignChange(final SignChangeEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	final Sign state = (Sign) event.getBlock().getState();
	final MemoryStone stone = getMemoryStructureBehind(state);
	if (stone != null) {
	    if (stone.getSign() != null) {
		event.setLine(0, Utility.color("&C") + "Memory Stone");
		event.setLine(1, Utility.color("&C[Broken]"));
		return;
	    }

	    if (event.getLine(1).length() == 0) {
		event.setLine(0, Utility.color("&C") + "Memory Stone");
		event.setLine(1, Utility.color("&C[Broken]"));
		return;
	    }

	    event.setLine(0, Utility.color("&A") + "Memory Stone");
	    event.setLine(1, Utility.color("&E") + event.getLine(1));
	    stone.setSign(state);
	    memoryStone.getServer().getScheduler().scheduleSyncDelayedTask(memoryStone, new Runnable() {
		public void run() {
		    updateSign(state);
		    Sign newSign = (Sign) new Location(state.getWorld(), state.getX(), state.getY(), state.getZ())
			    .getBlock().getState();
		    stone.setSign(newSign);

		}
	    }, 2);
	    event.getPlayer().sendMessage(Utility.color("&EMemory Stone created."));
	}

	super.onSignChange(event);
    }

    private MemoryStone getMemoryStructureBehind(Sign sign) {
	org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
	Block behind = sign.getBlock().getRelative(aSign.getFacing().getOppositeFace());

	MemoryStone stone = memoryStone.getMemoryStoneManager().getMemoryStoneAtBlock(behind);
	return stone;
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
	    newSign.setLine(0, Utility.color("&A") + "Memory Stone");
	    newSign.setLine(1, Utility.color("&E") + name);
	    newSign.update(true);
	}

    }
}

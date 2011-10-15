package za.dats.bukkit.memorystone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Interference {
    boolean active;
    Location oldLocation; // Where did the compass point to before interference
    MemoryStone stone; // Which stone are we currently pointing to?
    Location lastPosition; // Only update when we are far enough away from the last time, prevent spam checking on
			   // every move.

    public boolean isTooClose(Location to) {
	return false;
    }

    public void update(Player player, PlayerMoveEvent event) {
	// Check that the player actually has a compas..
	int index = player.getInventory().first(Material.COMPASS);
	if (index == -1) {
	    // No compass.. nothing to see here, move on.
	    return;
	}

	// First check that we aren't spamming this - so check every 2 blocks worth of movement
	if (lastPosition != null && event.getTo().getWorld().equals(lastPosition.getWorld())
		&& event.getTo().distanceSquared(lastPosition) < 2) {
	    return;
	}
	lastPosition = event.getTo();

	int distance = Config.getCompassToUnmemorizedStoneDistance();
	distance = distance * distance; // Just to make things slightly quicker

	MemoryStone closestStone = null;
	double closestDistance = 0;
	for (MemoryStone stone : MemoryStonePlugin.getInstance().getMemoryStoneManager().getStones()) {
	    if (stone.getSign() == null) {
		continue;
	    }

	    if (!stone.getSign().getWorld().getName().equals(player.getWorld().getName())) {
		continue;
	    }

	    if (MemoryStonePlugin.getInstance().getCompassManager().isMemorized(player, stone)) {
		continue;
	    }

	    double currentDistance = event.getTo().distanceSquared(stone.getSign().getBlock().getLocation());
	    if (player.getLocation().distanceSquared(stone.getSign().getBlock().getLocation()) < distance) {
		if ((closestStone == null) || (closestDistance > currentDistance)) {
		    closestStone = stone;
		    closestDistance = currentDistance;
		}
	    }
	}

	// Point compass
	if (closestStone != null) {

	    // Should only set old location if we aren't moving from stone to stone.
	    if (stone == null) {
		oldLocation = player.getCompassTarget();
	    }

	    stone = closestStone;
	    player.setCompassTarget(stone.getSign().getBlock().getLocation());

	    if (!active) {
		// Interference message was not sent yet - send it
		player.sendMessage(Config.getColorLang("compassinterference"));
		active = true;
	    }
	} else if (oldLocation != null) {
	    // Unset compass
	    if (active) {
		player.sendMessage(Config.getColorLang("compasslostinterference"));
	    }
	    active = false;
	    stone = null;
	    player.setCompassTarget(oldLocation);
	    oldLocation = null;
	}

    }

}
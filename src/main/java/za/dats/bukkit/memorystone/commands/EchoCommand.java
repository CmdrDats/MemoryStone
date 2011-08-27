package za.dats.bukkit.memorystone.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.getspout.spoutapi.player.SpoutPlayer;

import za.dats.bukkit.memorystone.MemoryStonePlugin;

public class EchoCommand implements CommandExecutor {

    private final MemoryStonePlugin memoryStone;

    public EchoCommand(MemoryStonePlugin memoryStone) {
	this.memoryStone = memoryStone;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	StringBuilder build = new StringBuilder();
	build.append("You are "+sender.getClass()+" saying: ");
	for (String string : args) {
	    build.append(string+" ");
	}
	
	if (memoryStone.hasSpout()) {
	    SpoutPlayer player = (SpoutPlayer)sender;
	    //player.getMainScreen().attachWidget(memoryStone, Widget);
	    sender.sendMessage("Spout enabled");
	}
	sender.sendMessage(build.toString());
	return true;
    }

}

package za.dats.bukkit.memorystone;

import java.util.regex.Pattern;

/**
 *
 * @author krinsdeath
 */

public class Utility {
	private final static Pattern COLOR = Pattern.compile("&([a-fA-F0-9])");

	public static String color(String msg) {
		return COLOR.matcher(msg).replaceAll("\u00A7$1");
	}
}

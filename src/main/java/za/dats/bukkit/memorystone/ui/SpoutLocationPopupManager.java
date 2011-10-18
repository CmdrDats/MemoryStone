package za.dats.bukkit.memorystone.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.event.screen.ScreenListener;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import za.dats.bukkit.memorystone.MemoryStone;
import za.dats.bukkit.memorystone.MemoryStonePlugin;
import za.dats.bukkit.memorystone.economy.EconomyManager;

public class SpoutLocationPopupManager extends ScreenListener {
    private static final int PAGE_COLUMNS = 2;
    private static final int PAGE_ROWS = 5;
    private static final int PAGE_SIZE = PAGE_COLUMNS * PAGE_ROWS;

    private class LocationPopup {
	int page = 0;
	List<MemoryStone> locations;
	Map<UUID, MemoryStone> locationButtons = new HashMap<UUID, MemoryStone>();
	GenericPopup popup;
	UUID cancelId;
	UUID nextId;
	UUID prevId;
	SpoutPlayer player;
	String heading;
	LocationPopupListener listener;

	void updatePage() {
	    for (Widget widget : popup.getAttachedWidgets()) {
		popup.removeWidget(widget);
	    }
	    locationButtons.clear();

	    int center = player.getMainScreen().getWidth() / 2;
	    int widthScale = player.getMainScreen().getWidth() / 100;
	    int heightScale = player.getMainScreen().getHeight() / 100;

	    popup.setBgVisible(true);

	    GenericLabel label = new GenericLabel(heading);
	    label.setAlign(WidgetAnchor.CENTER_CENTER);
	    label.setX(center).setY(heightScale * 7);
	    popup.attachWidget(plugin, label);

	    String pageText = "" + (page + 1) + " / " + (((locations.size() - 1) / PAGE_SIZE) + 1);
	    GenericLabel pageLabel = new GenericLabel(pageText);
	    pageLabel.setAlign(WidgetAnchor.CENTER_CENTER);
	    pageLabel.setX(center).setY(heightScale * 12);
	    popup.attachWidget(plugin, pageLabel);

	    EconomyManager economyManager = MemoryStonePlugin.getInstance().getEconomyManager();
	    boolean economyEnabled = economyManager.isEconomyEnabled() && !player.hasPermission("memorystone.usefree");

	    for (int i = 0; i < PAGE_SIZE; i++) {
		int currentEntry = i + (page * PAGE_SIZE);
		if (currentEntry >= locations.size()) {
		    break;
		}

		int row = i % PAGE_ROWS;
		int col = i / PAGE_ROWS;

		MemoryStone stone = locations.get(currentEntry);
		String name = stone.getName();
		int buttonWidth = 30;
		if (economyEnabled) {
		    buttonWidth = 45;
		    name = stone.getName() + " ("+economyManager.getFormattedCost(stone.getTeleportCost())+")";
		}
		GenericButton locationButton = new GenericButton(name);
		locationButton
			.setX(center - (widthScale * (buttonWidth + 5)) + (widthScale * col * (buttonWidth + 10)))
			.setY(heightScale * (row + 2) * 13);
		locationButton.setWidth(widthScale * buttonWidth).setHeight(heightScale * 10);
		popup.attachWidget(plugin, locationButton);

		locationButtons.put(locationButton.getId(), stone);
	    }

	    if (page > 0) {
		GenericButton previousButton = new GenericButton("<<");
		previousButton.setX(center - (widthScale * 25)).setY(heightScale * 90);
		previousButton.setWidth(widthScale * 10).setHeight(heightScale * 10);
		popup.attachWidget(plugin, previousButton);
		prevId = previousButton.getId();
	    }

	    GenericButton declineButton = new GenericButton("Cancel");
	    declineButton.setX(center - (widthScale * 10)).setY(heightScale * 94);
	    declineButton.setWidth(widthScale * 20).setHeight(heightScale * 10);
	    popup.attachWidget(plugin, declineButton);
	    cancelId = declineButton.getId();

	    if ((page + 1) * PAGE_SIZE < locations.size()) {
		GenericButton nextButton = new GenericButton(">>");
		nextButton.setX(center + (widthScale * 15)).setY(heightScale * 90);
		nextButton.setWidth(widthScale * 10).setHeight(heightScale * 10);
		popup.attachWidget(plugin, nextButton);
		nextId = nextButton.getId();
	    }

	    popup.setDirty(true);
	}

	void createPopup(SpoutPlayer sPlayer, Set<MemoryStone> locationSet, String text, LocationPopupListener listener) {
	    popup = new GenericPopup();
	    this.listener = listener;
	    this.locations = new ArrayList<MemoryStone>(locationSet);

	    player = sPlayer;
	    heading = text;
	    updatePage();

	    player.getMainScreen().closePopup();
	    player.getMainScreen().attachPopupScreen(popup);

	}

    }

    private final JavaPlugin plugin;
    private HashMap<UUID, LocationPopup> popups = new HashMap<UUID, SpoutLocationPopupManager.LocationPopup>();
    private HashMap<String, LocationPopup> playerPopups = new HashMap<String, SpoutLocationPopupManager.LocationPopup>();

    public SpoutLocationPopupManager(JavaPlugin plugin) {
	this.plugin = plugin;
    }

    public void registerEvents() {
	plugin.getServer().getPluginManager().registerEvent(Type.CUSTOM_EVENT, this, Priority.Normal, plugin);
    }

    public void showPopup(Player player, Set<MemoryStone> locations, String text, LocationPopupListener listener) {
	SpoutPlayer sPlayer = (SpoutPlayer) player;
	closePopup(popups.get(playerPopups));
	LocationPopup newPopup = new LocationPopup();
	newPopup.createPopup(sPlayer, locations, text, listener);
	popups.put(newPopup.popup.getId(), newPopup);
	playerPopups.put(sPlayer.getName(), newPopup);
    }

    private void closePopup(LocationPopup popup) {
	if (popup == null || popup.popup == null) {
	    return;
	}

	popup.popup.close();
	popups.remove(popup);
	playerPopups.remove(popup);
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
	LocationPopup locationPopup = popups.get(event.getScreen().getId());
	if (locationPopup == null) {
	    return;
	}

	UUID id = event.getButton().getId();
	if (id.equals(locationPopup.nextId)) {
	    locationPopup.page++;
	    locationPopup.updatePage();
	} else if (event.getButton().getId().equals(locationPopup.prevId)) {
	    locationPopup.page--;
	    locationPopup.updatePage();
	} else if (locationPopup.locationButtons.containsKey(id)) {
	    closePopup(locationPopup);
	    locationPopup.listener.selected(locationPopup.locationButtons.get(id));
	} else {
	    closePopup(locationPopup);
	}
    }
}

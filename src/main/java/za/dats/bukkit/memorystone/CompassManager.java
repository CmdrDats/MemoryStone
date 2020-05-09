package za.dats.bukkit.memorystone;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import za.dats.bukkit.memorystone.Config.MemoryEffect;

public class CompassManager implements Listener {
    private static class Teleport {
        boolean started;
        boolean cancelled;
        int taskId = -1;
        long lastTeleportTime;
        long lastFizzleTime;
        long lastEventTime;
        Player teleportEntity;
    }

    final MemoryStonePlugin plugin;
    private final Map<String, Set<MemoryStone>> memorized;
    private final Map<String, String> selected;
    private final Map<String, Teleport> teleporting;
    private final String locationsFile = "locations.yml";
    private final Map<String, Interference> interferences;

    public CompassManager(MemoryStonePlugin plugin) {
        this.plugin = plugin;
        memorized = new HashMap<String, Set<MemoryStone>>();
        selected = new HashMap<String, String>();
        teleporting = new HashMap<String, Teleport>();
        interferences = new HashMap<String, Interference>();
    }

    public Set<MemoryStone> getPlayerLocations(final String world, final Player player) {
        TreeSet<MemoryStone> result;
        if (Config.isSortByDistance()) {
            result = new TreeSet<MemoryStone>(new Comparator<MemoryStone>() {
                public int compare(MemoryStone o1, MemoryStone o2) {
                    if (o1.equals(o2)) {
                        return 0;
                    }

                    if (o1.getStructure().getWorld().getName().equals(o2.getStructure().getWorld().getName())) {
                        double o1Distance = player.getLocation().distanceSquared(o1.getSign().getBlock().getLocation());
                        double o2Distance = player.getLocation().distanceSquared(o2.getSign().getBlock().getLocation());

                        if (o1Distance < o2Distance) {
                            return -1;
                        }
                        return 1;
                    }

                    if (world.equals(o1.getStructure().getWorld().getName())) {
                        return -1;
                    } else if (world.equals(o2.getStructure().getWorld().getName())) {
                        return 1;
                    }

                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        } else {
            result = new TreeSet<MemoryStone>();
        }

        String playerName = player.getName();
        if (player.hasPermission("memorystone.allmemorized")) {
            Collection<? extends MemoryStone> localStones = plugin.getMemoryStoneManager().getLocalStones(world);
            if (localStones != null) {
                for (MemoryStone memoryStone : localStones) {
                    if (memoryStone.getSign() != null) {
                        result.add(memoryStone);
                    }
                }
            }
        } else {
            Set<MemoryStone> set = memorized.get(playerName);
            if (set == null) {
                set = new TreeSet<MemoryStone>();
                memorized.put(playerName, set);
            }

            for (MemoryStone memoryStone : set) {
                if (memoryStone.isCrossWorld()
                        && memoryStone.getStructure().getWorld() != player.getWorld()) {
                    result.add(memoryStone);
                } else if (world.equals(memoryStone.getStructure().getWorld().getName())) {
                    result.add(memoryStone);
                }
            }
        }

        for (MemoryStone memoryStone : plugin.getMemoryStoneManager().getGlobalStones()) {
            if (memoryStone.isCrossWorld()) {
                result.add(memoryStone);
            } else if (world.equals(memoryStone.getStructure().getWorld().getName())) {
                result.add(memoryStone);
            }
        }

        return result;
    }

    public void forgetStone(String name, boolean showMessage) {
        MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
        if (stone == null) {
            return;
        }

        for (String player : selected.keySet()) {
            if (name.equals(selected.get(player))) {
                selected.put(player, null);
            }
        }

        for (String player : memorized.keySet()) {
            Set<MemoryStone> list = memorized.get(player);
            if (list != null && list.contains(stone)) {
                list.remove(stone);
                Player p = plugin.getServer().getPlayer(player);
                if (p != null && showMessage) {
                    p.sendMessage(Config.getColorLang("destroyForgotten", "name", stone.getName()));
                }
            }
        }

        saveLocations();
    }

    public void registerEvents() {
        PluginManager pm;
        pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);

        loadLocations();
    }

    public boolean memorizeStone(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return false;
        }

        Sign state = (Sign) event.getClickedBlock().getState();
        MemoryStone stone = plugin.getMemoryStoneManager().getMemoryStructureForSign(state);
        return memorizeStone(event.getPlayer(), stone);
    }

    public boolean memorizeStone(Player player, MemoryStone stone) {
        if (player != null && stone != null && stone.getSign() != null) {
            if (stone.isGlobal()) {
                player.sendMessage(Config.getColorLang("alreadymemorized", "name", stone.getName()));
                return true;
            }

            Set<MemoryStone> set = memorized.get(player.getName());
            if (set == null) {
                set = new TreeSet<MemoryStone>();
                memorized.put(player.getName(), set);
            }

            if (set.contains(stone)) {
                player.sendMessage(Config.getColorLang("alreadymemorized", "name", stone.getName()));
                return true;
            }
            set.add(stone);
            // selected.put(player.getName(), stone.getName());

            player.sendMessage(Config.getColorLang("memorize", "name", stone.getName()));

            saveLocations();
            return true;
        }
        return false;
    }


    public boolean isMemorized(Player player, MemoryStone stone) {
        Set<MemoryStone> set = memorized.get(player.getName());
        if (set == null) {
            return false;
        }

        return set.contains(stone);
    }

    public void loadLocations() {
        File file = new File(this.plugin.getDataFolder(), this.locationsFile);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection memorizedSection = conf.getConfigurationSection("memorized");
        if (memorizedSection != null) {
            for (String player : memorizedSection.getKeys(false)) {
                Set<String> stones = (Set<String>) memorizedSection.get(player);
                if (stones == null) {
                    stones = new TreeSet<String>();
                }

                Set<MemoryStone> stoneList = new TreeSet<MemoryStone>();
                for (String stoneName : stones) {
                    MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(stoneName);
                    if (stone != null) {
                        stoneList.add(stone);
                    }
                }
                memorized.put(player, stoneList);
            }
        }

        ConfigurationSection selLoad = conf.getConfigurationSection("selected");
        if (selLoad != null) {
            for (String player : selLoad.getKeys(false)) {
                selected.put(player, selLoad.getString(player));
            }
        }
    }

    public void saveLocations() {
        File file = new File(this.plugin.getDataFolder(), this.locationsFile);
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

        Map<String, Set<String>> memorizedNames = new HashMap<String, Set<String>>();
        for (String playerName : memorized.keySet()) {
            Set<MemoryStone> stoneList = memorized.get(playerName);

            Set<String> stoneNameList = new TreeSet<String>();
            for (MemoryStone memoryStone : stoneList) {
                stoneNameList.add(memoryStone.getName());
            }

            memorizedNames.put(playerName, stoneNameList);
        }
        conf.set("memorized", memorizedNames);
        conf.set("selected", selected);
        try {
            conf.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Teleport getTeleport(Player player) {
        Teleport result = teleporting.get(player.getName());
        if (result == null) {
            result = new Teleport();
            teleporting.put(player.getName(), result);
        }

        return result;
    }

    public Location getDestinationLocation(MemoryStone stone, Player player) {
        // TODO: So very many deprecated stuffs. I don't even know how to get
        // at the data I need.
        Sign sign = stone.getSign();
        org.bukkit.block.data.Directional signData = (org.bukkit.block.data.Directional) sign.getBlockData();
        BlockFace attached = signData.getFacing();
        Block infront = sign.getBlock().getRelative(attached);

        Location result = infront.getLocation();
        result.setX(result.getX() + 0.5);
        result.setZ(result.getZ() + 0.5);

        result.setPitch(player.getLocation().getPitch());

        // TODO: This isn't going to work.
        /*
        byte signData = sign.getData();
        if (sign.getType().getData() == org.bukkit.material.Sign.class) {
            result.setYaw(signData < 0x8 ? signData * 22.5f + 180 : signData * 22.5f - 180);
        } else if (aSign.isWallSign()) {
            if (signData == 0x2) {// East
                result.setYaw(180);
            } else if (signData == 0x3) {// West
                result.setYaw(0);
            } else if (signData == 0x4) {// North
                result.setYaw(90);
            } else {// South
                result.setYaw(270);
            }
        } else {
        }
        */
        result.setYaw(player.getLocation().getYaw());

        return result;
    }

    public boolean checkCooldown(Player caster, Teleport teleport) {
        long now = new Date().getTime();
        if (now - teleport.lastTeleportTime < Config.getCooldownTime() * 1000) {
            long left = Config.getCooldownTime() - ((now - teleport.lastTeleportTime) / 1000);
            caster.sendMessage(Config.getColorLang("cooldown", "left", "" + left));
            return false;
        }

        if (now - teleport.lastFizzleTime < Config.getFizzleCooldownTime() * 1000) {
            long left = Config.getFizzleCooldownTime() - ((now - teleport.lastFizzleTime) / 1000);
            caster.sendMessage(Config.getColorLang("cooldown", "left", "" + left));
            return false;
        }
        return true;
    }

    public boolean consumeReagent(Player caster) {
        if (Config.getReagentItem() == null) {
            return true;
        }

        int reagent = caster.getInventory().first(Config.getReagentItem());
        if (reagent == -1) {
            caster.sendMessage(Config.getColorLang("noreagent", "material", Config.getReagentItem().toString().toLowerCase()));
            return false;
        }

        ItemStack item = caster.getInventory().getItem(reagent);
        if (item == null) {
            caster.sendMessage(Config.getColorLang("noreagent", "material", Config.getReagentItem().toString().toLowerCase()));
            return false;
        }

        if (item.getAmount() == 1) {
            caster.getInventory().setItem(reagent, null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        return true;
    }

    public void startTeleport(final MemoryStone stone, final Player caster, Player target) {
        // check permissions!

        final String name = stone.getName();
        final Sign sign = stone.getSign();
        if (sign == null) {
            caster.sendMessage(Config.getColorLang("notfound", "name", name));
            return;
        }

        if (!stone.isCrossWorld() && !caster.getWorld().equals(stone.getSign().getBlock().getWorld())) {
            caster.sendMessage(Config.getColorLang("notcrossworld", "name", name));
            return;
        }

        Teleport teleport = getTeleport(caster);

        if (!checkCooldown(caster, teleport)) {
            return;
        }
        if (!consumeReagent(caster)) {
            return;
        }

        if (target.equals(caster)) {
            caster.sendMessage(Config.getColorLang("startrecall", "name", stone.getName()));
        } else {
            caster.sendMessage(Config.getColorLang("teleportingother", "name", target.getName(), "destination", name));
            target.sendMessage(Config.getColorLang("teleportedbyother", "name", caster.getName(), "destination", name));
        }

        int waitTime = Config.getCastingTime() * 10;

        target.getWorld().playEffect(caster.getLocation(), Effect.SMOKE, BlockFace.UP);
        target.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1F, 1F);

        int task = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Teleport teleport = getTeleport(caster);
                teleport.taskId = -1;
                teleport.started = false;
                if (teleport.cancelled) {
                    return;
                }

                if (!teleport.teleportEntity.equals(caster)) {
                    Teleport other = getTeleport(teleport.teleportEntity);
                    other.taskId = -1;
                    other.started = false;
                    if (other.cancelled) {
                        return;
                    }

                    other.lastTeleportTime = new Date().getTime();
                }

                teleport.lastTeleportTime = new Date().getTime();

                Location destination = getDestinationLocation(stone, caster);
                if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_TELEPORT_SOURCE)) {
                    teleport.teleportEntity.getWorld().strikeLightningEffect(teleport.teleportEntity.getLocation());
                }

                if (Config.isEffectEnabled(MemoryEffect.LIGHTNING_ON_TELEPORT_DEST)) {
                    teleport.teleportEntity.getWorld().strikeLightningEffect(destination);
                }

                teleport.teleportEntity.setCompassTarget(destination);
                if (!Config.isPointCompassOnly()) {
                    teleport.teleportEntity.teleport(destination);
                }

            }
        }, waitTime);

        teleport.cancelled = false;
        teleport.taskId = task;
        teleport.started = true;
        teleport.teleportEntity = target;

        if (!teleport.teleportEntity.equals(caster)) {
            Teleport otherTeleport = getTeleport(teleport.teleportEntity);
            otherTeleport.cancelled = false;
            otherTeleport.taskId = task;
            otherTeleport.started = true;
            otherTeleport.teleportEntity = target;
        }
    }


    public void cancelTeleport(Player player) {
        Teleport teleport = getTeleport(player);
        if (teleport.started && teleport.taskId > -1) {
            player.sendMessage(Config.getColorLang("cancelled"));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1F, 1F);

            teleport.cancelled = true;
            plugin.getServer().getScheduler().cancelTask(teleport.taskId);
            teleport.started = false;
            teleport.lastFizzleTime = new Date().getTime();
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (Config.getTeleportItem() == null) {
            // Cannot teleport another without a teleport item set.
            return;
        }

        if (!event.getPlayer().getInventory().getItemInMainHand().getType().equals(Config.getTeleportItem())) {
            return;
        }

        // check permissions!
        if (!event.getPlayer().hasPermission("memorystone.useonothers")) {
            return;
        }

        if (!(event.getRightClicked() instanceof HumanEntity)) {
            return;
        }

        Teleport teleport = getTeleport(event.getPlayer());
        long now = new Date().getTime();
        if (now - teleport.lastEventTime < 100) {
            return;
        }
        teleport.lastEventTime = now;

        String name = selected.get(event.getPlayer().getName());
        tryTeleportOther(event, event.getPlayer(), name);
    }

    private void tryTeleportOther(final PlayerInteractEntityEvent event, final Player p, String name) {
        if (name != null) {
            MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(name);
            if (stone == null) {
                p.sendMessage(Config.getColorLang("notexist", "name", name));
                forgetStone(name, false);
                return;
            }
            cancelTeleport(p);

            Player other = (Player) event.getRightClicked();
            cancelTeleport(other);

            startTeleport(stone, event.getPlayer(), other);
        } else {
            event.getPlayer().sendMessage(Config.getColorLang("notmemorized"));

        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // check permissions!
        if (!player.hasPermission("memorystone.use")) {
            return;
        }

        if (!player.getInventory().getItemInMainHand().getType().equals(Config.getTeleportItem())) {
            return;
        }

        Teleport teleport = getTeleport(player);
        long now = new Date().getTime();
        if (now - teleport.lastEventTime < 100) {
            return;
        }
        teleport.lastEventTime = now;

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (event.getClickedBlock() == null) {
                return;
            }

            if (event.getClickedBlock().getState() instanceof Sign) {
                if (teleport.started) {
                    return;
                }

                if (memorizeStone(event)) {
                    return;
                }
            }
        }

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_AIR)) {
            cycleTeleport(event, player, teleport);
        }

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            // Make interaction with interactable blocks cleaner
            if (event.getClickedBlock() != null
                    && event.getClickedBlock().getType().isInteractable()) {
                return;
            }

            String name = selected.get(player.getName());
            tryTeleport(player, name);
        }
    }

    private void cycleTeleport(final PlayerInteractEvent event, Player player, Teleport teleport) {
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Config.getTeleportItem()) {
            return;
        }

        // Make interaction with interactable blocks cleaner
        if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        if (teleport.started) {
            return;
        }

        Set<MemoryStone> memory = getPlayerLocations(player.getWorld().getName(), player);
        if (memory == null || memory.size() == 0) {
            return;
        }

        String selectedName = selected.get(player.getName());
        if (selectedName == null) {
            selectedName = memory.iterator().next().getName();
        } else {
            boolean next = false;
            boolean found = false;
            for (MemoryStone currentStone : memory) {
                String name = currentStone.getName();
                if (next) {
                    selectedName = name;
                    found = true;
                    break;
                }
                if (name.equals(selectedName)) {
                    next = true;
                }
            }

            // wrap around
            if (!found) {
                selectedName = memory.iterator().next().getName();
            }
        }

        player.sendMessage(Config.getColorLang("select", "name", selectedName));

        selected.put(player.getName(), selectedName);
        event.setCancelled(true);
    }

    private void tryTeleport(Player p, String stoneName) {
        if (stoneName != null) {
            MemoryStone stone = plugin.getMemoryStoneManager().getNamedMemoryStone(stoneName);
            if (stone == null) {
                p.sendMessage(Config.getColorLang("notexist", "name", stoneName));
                forgetStone(stoneName, false);
                return;
            }

            cancelTeleport(p);
            startTeleport(stone, p, p);
        } else {
            p.sendMessage(Config.getColorLang("notmemorized"));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Teleport teleport = getTeleport(event.getPlayer());
        if (teleport.started) {
            if (event.getTo() == null
                    || (event.getFrom().getBlockX() != event.getTo().getBlockX())
                    || (event.getFrom().getBlockY() != event.getTo().getBlockY())
                    || (event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {

                cancelTeleport(event.getPlayer());
            }
        }

        if (Config.getCompassToUnmemorizedStoneDistanceSquared() == 0) {
            return;
        }

        Player player = event.getPlayer();
        Interference interference = interferences.get(player.getName());
        if (interference == null) {
            interference = new Interference();
            interferences.put(player.getName(), interference);
        }

        interference.update(player, event);
    }

}

package za.dats.bukkit.memorystone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.PluginManager;
import za.dats.bukkit.memorystone.MemoryStone.StoneType;
import za.dats.bukkit.memorystone.util.StructureListener;
import za.dats.bukkit.memorystone.util.structure.Rotator;
import za.dats.bukkit.memorystone.util.structure.Structure;
import za.dats.bukkit.memorystone.util.structure.StructureType;

public class MemoryStoneManager implements StructureListener, Listener {
    private final MemoryStonePlugin memoryStonePlugin;
    private HashMap<Structure, MemoryStone> structureMap = new HashMap<Structure, MemoryStone>();
    private HashMap<String, MemoryStone> namedMap = new HashMap<String, MemoryStone>();
    private HashMap<String, Set<MemoryStone>> worldStones = new HashMap<String, Set<MemoryStone>>();
    private List<MemoryStone> globalStones = new ArrayList<MemoryStone>();

    public MemoryStoneManager(MemoryStonePlugin memoryStonePlugin) {
        this.memoryStonePlugin = memoryStonePlugin;
    }

    public void registerEvents() {
        PluginManager pm;
        pm = memoryStonePlugin.getServer().getPluginManager();
        pm.registerEvents(this, memoryStonePlugin);
    }

    public void structurePlaced(Player player, Structure structure) {
        MemoryStone stone = new MemoryStone();
        stone.setStructure(structure);

        structureMap.put(structure, stone);

        player.sendMessage(Utility.color(Config.getColorLang("createConfirm", "name", structure.getStructureType()
                .getName())));
    }

    public void structureDestroyed(Player player, Structure structure) {
        MemoryStone stone = structureMap.get(structure);
        if (stone.getName() != null) {
            memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), true);
            globalStones.remove(stone);
            removeWorldStone(stone);
            namedMap.remove(stone.getName());
        }

        Sign sign = stone.getSign();
        if (sign != null) {
            BlockState state = new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
                    .getState();

            if (state instanceof Sign) {
                Sign newSign = (Sign) new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()).getBlock()
                        .getState();
                newSign.setLine(0, Config.getColorLang("signboard"));
                newSign.setLine(1, Config.getColorLang("broken"));
                newSign.update(true);
            }

            stone.setSign(null);
        }

        structureMap.remove(structure);
        player.sendMessage(Utility.color(Config.getColorLang("destroyed")));
    }

    public void structureLoaded(Structure structure, Map<String, Object> node) {
        MemoryStone stone = new MemoryStone();
        stone.setStructure(structure);

        structureMap.put(structure, stone);

        stone.setName((String)node.get("name"));
        if (stone.getName() != null && stone.getName().length() > 0) {
            namedMap.put(stone.getName(), stone);
            if (stone.isGlobal()) {
                globalStones.add(stone);
            }

            addWorldStone(stone);
        }

        if (node.containsKey("signx")) {
            try {
                Sign newSign = (Sign) new Location(structure.getWorld(),
                        (Integer)node.get("signx"),
                        (Integer)node.get("signy"),
                        (Integer)node.get("signz")).getBlock().getState();

                newSign.setLine(2, "");
                newSign.setLine(3, "");
                stone.setSign(newSign);

            } catch (Exception e) {
                memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), false);
                stone.setName("");
            }
        }
    }

    private void addWorldStone(MemoryStone stone) {
        String worldName = stone.getStructure().getWorld().getName();
        Set<MemoryStone> set = worldStones.get(worldName);
        if (set == null) {
            set = new TreeSet<MemoryStone>();
            worldStones.put(worldName, set);
        }

        set.add(stone);
    }

    private void removeWorldStone(MemoryStone stone) {
        String worldName = stone.getStructure().getWorld().getName();
        Set<MemoryStone> set = worldStones.get(worldName);
        if (set == null) {
            return;
        }

        set.remove(stone);
    }

    public void structureSaving(Structure structure, Map<String, Object> yamlMap) {
        MemoryStone memoryStone = structureMap.get(structure);

        yamlMap.put("name", memoryStone.getName());
        if (memoryStone.getSign() != null) {
            Sign sign = memoryStone.getSign();
            yamlMap.put("signx", sign.getX());
            yamlMap.put("signy", sign.getY());
            yamlMap.put("signz", sign.getZ());
        }
    }

    public StructureType baseMemoryStone(String name, Material base, boolean global, boolean crossworld) {
        StructureType structuretype;
        StructureType.Prototype proto;

        proto = new StructureType.Prototype();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                proto.addBlock(x, 0, z, base);
            }
        }
        proto.addBlock(1, 1, 1, Material.OBSIDIAN);
        proto.addBlock(1, 2, 1, Material.OBSIDIAN);
        proto.addBlock(1, 3, 1, Material.OBSIDIAN);
        proto.setName(name);
        proto.setRotator(Rotator.NONE);
        proto.addMetadata("type", "MEMORYSTONE");
        proto.addMetadata("global", global ? "true" : "false");
        proto.addMetadata("crossworld", crossworld ? "true" : "false");
        structuretype = new StructureType(proto);

        return structuretype;
    }

    public void generatingDefaultStructureTypes(List<StructureType> types) {
        types.add(baseMemoryStone("Memory Stone", Material.IRON_BLOCK, false, false));
        types.add(baseMemoryStone("Global Memory Stone", Material.GOLD_BLOCK, true, false));
        types.add(baseMemoryStone("Global Crossworld Stone", Material.DIAMOND_BLOCK, true, true));
    }

    public MemoryStone getMemoryStoneAtBlock(Block behind) {
        Set<Structure> structuresFromBlock = memoryStonePlugin.getStructureManager().getStructuresFromBlock(behind);
        if (structuresFromBlock == null || structuresFromBlock.size() != 1) {
            return null;
        }

        for (Structure structure : structuresFromBlock) {
            if (!structureMap.containsKey(structure)) {
                break;
            }

            MemoryStone result = structureMap.get(structure);
            return result;
        }

        return null;
    }

    public MemoryStone getMemoryStructureBehind(Sign sign) {
        org.bukkit.block.data.Directional signData = (org.bukkit.block.data.Directional) sign.getBlockData();
        BlockFace attached = signData.getFacing().getOppositeFace();
        Block behind = sign.getBlock().getRelative(attached);
        return memoryStonePlugin.getMemoryStoneManager().getMemoryStoneAtBlock(behind);
    }

    public MemoryStone getMemoryStructureForSign(Sign sign) {
        for (MemoryStone stone : namedMap.values()) {
            if (stone.getSign() == null) {
                continue; // Next to impossible.. anyway. moving along
            }

            if (sign.getBlock().equals(stone.getSign().getBlock())) {
                return stone;
            }
        }

        return null;
    }

    public MemoryStone getNamedMemoryStone(String name) {
        return namedMap.get(name);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Sign) {
            final Sign state = (Sign) event.getBlock().getState();
            final MemoryStone stone = getMemoryStructureForSign(state);
            if (stone != null) {
                // check permissions!
                if (!event.getPlayer().hasPermission("memorystone.break")
                        && !event.getPlayer().getName().equals(stone.getStructure().getOwner())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Config.getColorLang("nobreakpermission"));

                    state.setLine(0, Config.getColorLang("signboard"));
                    state.setLine(1, stone.getName());
                    state.update(true);
                    return;
                }

                if (!state.getLine(1).equals(stone.getName())) {
                    return;
                }

                memoryStonePlugin.getCompassManager().forgetStone(stone.getName(), true);
                namedMap.remove(stone.getName());
                globalStones.remove(stone);
                removeWorldStone(stone);
                stone.setSign(null);
                memoryStonePlugin.getStructureManager().saveStructures();
            }
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        memoryStonePlugin.info("Processing sign change event");

        if (!(event.getBlock().getState() instanceof Sign)) {
            return;
        }
        final Sign sign = (Sign) event.getBlock().getState();

        memoryStonePlugin.info("Event Line 0: " + event.getLine(0));

        MemoryStone stone = getMemoryStructureBehind(sign);
        if (stone == null) {
            org.bukkit.block.data.Directional signData = (org.bukkit.block.data.Directional) sign.getBlockData();
            BlockFace attached = signData.getFacing().getOppositeFace();
            Block behind = sign.getBlock().getRelative(attached);

            Structure structure = memoryStonePlugin.getStructureManager().checkForStone(event.getPlayer(), behind);
            if (structure != null) {
                stone = getMemoryStructureBehind(sign);
            }
        }

        if (stone != null && stone.getType().equals(StoneType.MEMORYSTONE)) {
            // check permissions!
            if (!event.getPlayer().hasPermission("memorystone.build")) {
                event.getPlayer().sendMessage(Config.getColorLang("nobuildpermission"));
                return;
            }

            if (stone.getSign() != null) {
                event.setLine(0, Config.getColorLang("signboard"));
                event.setLine(1, Config.getColorLang("broken"));
                return;
            }

            if (event.getLine(0) == null || event.getLine(0).length() == 0) {
                event.setLine(0, Config.getColorLang("signboard"));
                event.setLine(1, Config.getColorLang("broken-noname"));
                return;
            }

            String name = event.getLine(0);

            if (namedMap.containsKey(name)) {
                event.setLine(0, Config.getColorLang("signboard"));
                event.setLine(1, Config.getColorLang("duplicate"));
                return;
            }

            event.setLine(0, Config.getColorLang("signboard"));
            event.setLine(1, name);

            stone.setSign(sign);
            stone.setName(name);

            namedMap.put(name, stone);
            addWorldStone(stone);
            if ("true".equals(stone.getStructure().getStructureType().getMetadata().get("global"))) {
                globalStones.add(stone);
            }

            final MemoryStone finalStone = stone;
            memoryStonePlugin.getServer().getScheduler().scheduleSyncDelayedTask(memoryStonePlugin, new Runnable() {
                public void run() {
                    updateSign(finalStone, sign);
                    /*
                    Sign newSign = (Sign) new Location(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ())
                            .getBlock().getState();
                     */
                    memoryStonePlugin.getStructureManager().saveStructures();
                }
            }, 2);
            event.getPlayer().sendMessage(Utility.color(Config.getLang("signAdded")));
        }
    }

    public void updateSign(MemoryStone s, Sign sign) {
        sign.setLine(0, Config.getColorLang("signboard"));
        sign.setLine(1, s.getName());
        sign.update(true);
    }

    public List<MemoryStone> getGlobalStones() {
        return globalStones;
    }

    public Collection<? extends MemoryStone> getLocalStones(String world) {
        return worldStones.get(world);
    }

    public Collection<MemoryStone> getStones() {
        return namedMap.values();
    }
}

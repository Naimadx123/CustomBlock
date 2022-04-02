package de.jeff_media.customblocks;

import de.jeff_media.customblocks.implentation.HeadBlock;
import de.jeff_media.customblocks.implentation.ItemsAdderBlock;
import de.jeff_media.customblocks.implentation.VanillaBlock;
import de.jeff_media.jefflib.exceptions.InvalidBlockDataException;
import de.jeff_media.jefflib.exceptions.MissingPluginException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public abstract class CustomBlock implements ConfigurationSerializable {

    protected Block block;
    protected BlockData originalBlockData;
    protected List<UUID> entities = new ArrayList<>();

    public static CustomBlock fromStringOrDefault(String fullId, Material fallback) {
        try {
            return fromStringOrThrow(fullId);
        } catch (MissingPluginException | InvalidBlockDataException e) {
            return new VanillaBlock(fallback);
        }
    }

    public static CustomBlock fromStringOrThrow(String fullId) throws InvalidBlockDataException, MissingPluginException {
            if (fullId.startsWith("minecraft:") || !fullId.contains(":")) {
                return new VanillaBlock(fullId);
            }

            String[] split = fullId.split(":",2);
            if(split.length==1) {
                throw new InvalidBlockDataException("Could not parse custom block data: " + fullId);
            }

            String namespace = split[0];
            String id = split[1];

            switch (namespace.toLowerCase(Locale.ROOT)) {
                case "head":
                    return new HeadBlock(id);
                case "itemsadder":
                    checkForPlugin("itemsadder","ItemsAdder");
                    return new ItemsAdderBlock(id);
                /*case "oraxen":
                    checkForPlugin("oraxen","Oraxen");
                    return new OraxenBlock(id);*/
            }

            throw new InvalidBlockDataException("Could not parse custom block data: " + fullId);
    }

    private static void checkForPlugin(String namespace, String pluginName) throws MissingPluginException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if(plugin == null || !plugin.isEnabled()) {
            throw new MissingPluginException(String.format("Placing custom blocks from namespace \"%s\" requires the following plugin to be installed: \"%s\"",
                    namespace, pluginName));
        }
    }

    public void place(Block block) {
        place(block,null);
    }

    public void place(Block block, OfflinePlayer player) {
        this.block = block;
        this.originalBlockData = block.getBlockData();
    }

    public void remove() {
        if(block != null) {
            if (originalBlockData != null) {
                block.setBlockData(originalBlockData);
            } else {
                block.setType(Material.AIR);
            }
        }
        entities.forEach(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if(entity != null) entity.remove();
        });
        entities.clear();
    }

    public CustomBlock(String id) {
        this.id = id;
    };

    public abstract String getNamespace();

    @Getter private final String id;

    public abstract Material getMaterial();

    public Map<String,Object> serialize() {
        Map<String,Object> map = new HashMap<>();
        map.put("id",getNamespace() + ":" + id);
        Map<String,Object> location = null;
        if(block != null) {
            location = new HashMap<>();
            location.put("worldid",block.getWorld().getUID().toString());
            location.put("x",block.getX());
            location.put("y",block.getY());
            location.put("z",block.getZ());
        }
        map.put("location",location);
        map.put("originalBlockData",originalBlockData.getAsString());
        map.put("entities",entities.stream().map(UUID::toString).collect(Collectors.toList()));
        return map;
    }

    public static CustomBlock deserialize(Map<String,Object> map) throws MissingPluginException, InvalidBlockDataException {
        CustomBlock cb = CustomBlock.fromStringOrThrow((String) map.get("id"));
        Object locationObject = map.get("location");
        Location location = null;
        if(locationObject instanceof Location) {
            location = (Location) map.get("location");
        } else if(locationObject instanceof Map) {
            Map<String,Object> locMap = (Map<String,Object>) locationObject;
            UUID worldUuid = UUID.fromString((String)locMap.get("worldid"));
            int x = (int) locMap.get("x");
            int y = (int) locMap.get("y");
            int z = (int) locMap.get("z");
            World world = Bukkit.getWorld(worldUuid);
            if(world == null) throw new IllegalArgumentException("World with UID " + worldUuid + " is not loaded.");
            location = new Location(world, x,y,z,0,0);
        }
        if(location != null) {
            cb.block = location.getBlock();
        }
        cb.originalBlockData = Bukkit.createBlockData((String) map.get("originalBlockData"));
        cb.entities = ((List<String>) map.get("entities")).stream().map(UUID::fromString).collect(Collectors.toList());
        return cb;
    }

}

package de.jeff_media.customblocks.implentation;

import de.jeff_media.customblocks.CustomBlock;
import de.jeff_media.jefflib.exceptions.InvalidBlockDataException;
import org.bukkit.block.Block;

public class ItemsAdderBlock extends CustomBlock {

    dev.lone.itemsadder.api.CustomBlock iaBlock;

    public ItemsAdderBlock(String id) throws InvalidBlockDataException {
        super(id);
        iaBlock = dev.lone.itemsadder.api.CustomBlock.getInstance(id);
        if(iaBlock == null) throw new InvalidBlockDataException("Could not find ItemsAdder block: " + id);
    }

    @Override
    public void place(Block block) {
        iaBlock.place(block.getLocation());
    }

    @Override
    public String getNamespace() {
        return "itemsadder";
    }
}

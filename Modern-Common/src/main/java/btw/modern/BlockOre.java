package btw.modern;

import java.util.Random;

/**
 * 1.5.2 BlockOre (vanilla/server BlockOre.java): the drops FC's ores inherit. FC overrides idDropped on every ore it
 * replaced except the nether quartz ore (FCBlockNetherQuartzOre), and quantityDroppedWithBonus only on the redstone ore,
 * so Fortune on BTW's ores and quartz from quartz ore come from here, as in 4.B. No experience: 4.B removed block XP
 * (vanilla/server Block.java:827-842), and Block.dropXpOnBlockBreak is already empty.
 */
// FCMOD-INTEGRATION(1.20.1): strata: 1.5.2 BlockOre's drop rules: Fortune on BTW's ores, quartz from nether quartz ore (4.B parity)
public class BlockOre extends Block {

    protected BlockOre(int id) {
        super(id, Material.rock);
    }

    @Override
    public int idDropped(int metadata, Random random, int fortune) {
        Item dropped = is(Block.oreCoal) ? Item.coal : is(Block.oreDiamond) ? Item.diamond
                : is(Block.oreLapis) ? Item.dyePowder : is(Block.oreEmerald) ? Item.emerald
                : is(Block.oreNetherQuartz) ? Item.netherQuartz : null;
        return dropped != null ? dropped.itemID : this.blockID;
    }

    @Override
    public int quantityDropped(Random random) {
        return is(Block.oreLapis) ? 4 + random.nextInt(5) : 1;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune > 0 && this.blockID != this.idDropped(0, random, fortune)) {
            int bonus = random.nextInt(fortune + 2) - 1;
            if (bonus < 0) bonus = 0;
            return this.quantityDropped(random) * (bonus + 1);
        }
        return this.quantityDropped(random);
    }

    @Override
    public int damageDropped(int metadata) {
        return is(Block.oreLapis) ? 4 : 0;
    }

    /** The 1.5.2 ore {@code block} by id: FC replaced the instances at those ids with its own ore classes. */
    private boolean is(Block block) {
        return block != null && block.blockID == this.blockID;
    }
}

package btw.modern;

public class BlockLeaves extends Block {

    public int[] adjacentTreeBlocks;
    public boolean graphicsLevel;

    protected BlockLeaves(int id) {
        super(id, Material.leaves);
        // 1.5.2 BlockLeaves constructor (vanilla/server BlockLeaves.java:28): every leaf random-ticks, so the ones
        // flagged below get FCBlockLeaves.updateTick's decay check.
        setTickRandomly(true);
    }

    /**
     * 1.5.2 BlockLeaves.breakBlock (vanilla/server BlockLeaves.java:35-59): a removed leaf flags (metadata bit 8,
     * flag 4) every leaves block (Block.leaves, id 18) in the 3x3x3 around it. Only flagged leaves decay
     * (FCBlockLeaves.updateTick), and a decaying leaf is removed in turn, so the flag spreads through a canopy
     * that has lost its logs. FCBlockBloodWoodLeaves flags its own leaves, then calls this.
     */
    @Override
    public void breakBlock(World world, int i, int j, int k, int iBlockID, int iMetadata) {
        byte range = 1;
        int checkRange = range + 1;

        if (world.checkChunksExist(i - checkRange, j - checkRange, k - checkRange,
                i + checkRange, j + checkRange, k + checkRange)) {
            for (int offsetI = -range; offsetI <= range; ++offsetI) {
                for (int offsetJ = -range; offsetJ <= range; ++offsetJ) {
                    for (int offsetK = -range; offsetK <= range; ++offsetK) {
                        int iTempID = world.getBlockId(i + offsetI, j + offsetJ, k + offsetK);

                        if (iTempID == Block.leaves.blockID) {
                            int iTempMetadata = world.getBlockMetadata(i + offsetI, j + offsetJ, k + offsetK);
                            world.setBlockMetadata(i + offsetI, j + offsetJ, k + offsetK, iTempMetadata | 8, 4);
                        }
                    }
                }
            }
        }
    }

    // --- 1.5.2 BlockLeaves drops (vanilla/server BlockLeaves.java:184-287) ---
    // FCBlockLeaves and FCBlockBloodWoodLeaves inherit these. With Block's defaults FC's own harvest, decay, machines
    // and block dispenser gave oak leaves (id 18, damage 0) for every species, never a sapling, and blood wood leaves
    // dropped their sapling on every break. dropBlockAsItemWithChance (:200-244, Fortune and apples) is not ported:
    // both FC subclasses replace it (Server FCBlockLeaves.java:62-85, Client FCBlockBloodWoodLeaves.java:97-118).

    /**
     * 1.5.2 BlockLeaves.quantityDropped (vanilla/server BlockLeaves.java:184-187): 1 in 20. FCBlockBloodWoodLeaves
     * reads it through quantityDroppedWithBonus; FCBlockLeaves rolls its own 1/20 (1/40 jungle) instead.
     */
    @Override
    public int quantityDropped(java.util.Random random) {
        return random.nextInt(20) == 0 ? 1 : 0;
    }

    /** 1.5.2 BlockLeaves.idDropped (vanilla/server BlockLeaves.java:192-195): the sapling (id 6). */
    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        return Block.sapling.blockID;
    }

    /** 1.5.2 BlockLeaves.damageDropped (vanilla/server BlockLeaves.java:266-269): the wood species. */
    @Override
    public int damageDropped(int metadata) {
        return metadata & 3;
    }

    /**
     * 1.5.2 BlockLeaves.harvestBlock (vanilla/server BlockLeaves.java:250-260): shears take the species' leaves and
     * nothing else; any other harvest is Block's (silk touch, then the sapling roll). 1.5.2 Item.shears is item 359,
     * which the modern shears convert to (ProxyRegistry). FCBlockBloodWoodLeaves handles its own shears first.
     */
    @Override
    public void harvestBlock(World world, EntityPlayer player, int i, int j, int k, int metadata) {
        if (!world.isRemote && player != null && player.getCurrentEquippedItem() != null
                && player.getCurrentEquippedItem().itemID == Item.shears.itemID) {
            player.addStat(StatList.mineBlockStatArray[this.blockID], 1);
            this.dropBlockAsItem_do(world, i, j, k, new ItemStack(Block.leaves.blockID, 1, metadata & 3));
        } else {
            super.harvestBlock(world, player, i, j, k, metadata);
        }
    }

    /**
     * 1.5.2 BlockLeaves.createStackedBlock (vanilla/server BlockLeaves.java:284-287): this block with the species bits,
     * not damageDropped (blood wood's is its sapling subtype). It is the silk-touch drop (Block.harvestBlock) and
     * what FC's block dispenser takes (FCBlockLeaves.GetStackRetrievedByBlockDispenser).
     */
    @Override
    public ItemStack createStackedBlock(int metadata) {
        return new ItemStack(this.blockID, 1, metadata & 3);
    }

    /**
     * 1.5.2's value for leaves: Block.canSilkHarvest() is renderAsNormalBlock() && !isBlockContainer (vanilla/server
     * Block.java:1182-1185), true for leaves (:551-554; BlockLeavesBase overrides only isOpaqueCube). Modern-Common
     * Block answers false for every block (Block.java:378); restoring 1.5.2's general rule there needs its own
     * silk-touch audit of every FC full cube (the Modern-Common drop-parity follow-up), which then removes this.
     */
    @Override
    public boolean canSilkHarvest() {
        return true;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockLeaves' icon members (vanilla/client BlockLeaves.java:23-25,
    // :352-355, :390-401): a fancy and a fast (opaque) set of oak, spruce, birch and jungle leaves, by species.
    // FCBlockLeaves (18) inherits both; a held leaves block drew Block's default, the unlocalized "leaves" (oak),
    // for every species. iconType stays 0, the fancy set: nothing in the port calls 1.5.2's setGraphicsLevel, which
    // also wrote the opacity this class answers from the server (ruling Q-R2 (a)). Both sets name the same 1.20.1
    // sprites.
    public static final String[][] field_94396_b = new String[][] {
            {"leaves", "leaves_spruce", "leaves", "leaves_jungle"},
            {"leaves_opaque", "leaves_spruce_opaque", "leaves_opaque", "leaves_jungle_opaque"}};
    private int iconType;
    private Icon[][] iconArray = new Icon[2][];

    // 1.5.2 BlockLeaves.getIcon (vanilla/client BlockLeaves.java:352-355); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        Icon[] icons = this.iconArray[this.iconType];
        if (icons == null) {
            return super.getIcon(side, metadata);
        }
        return (metadata & 3) == 1 ? icons[1] : ((metadata & 3) == 3 ? icons[3] : icons[0]);
    }

    // 1.5.2 BlockLeaves.registerIcons (vanilla/client BlockLeaves.java:390-401)
    @Override
    public void registerIcons(IconRegister register) {
        for (int set = 0; set < field_94396_b.length; ++set) {
            Icon[] icons = new Icon[field_94396_b[set].length];
            for (int i = 0; i < icons.length; ++i) {
                icons[i] = register.registerIcon(field_94396_b[set][i]);
            }
            this.iconArray[set] = icons;
        }
    }
}

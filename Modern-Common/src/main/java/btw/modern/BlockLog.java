package btw.modern;

public class BlockLog extends Block {

    protected BlockLog(int id) {
        super(id, Material.wood);
    }

    protected BlockLog(int id, Material material) {
        super(id, material);
    }

    public static final String[] woodType = new String[] {"oak", "spruce", "birch", "jungle"};
    public static int limitToValidMetadata(int metadata) { return metadata & 3; }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockLog's icon members (vanilla/client BlockLog.java:24-26,
    // :122-127, :169-178): the one log end on the axis faces and the species bark on the others. FCBlockLog (17)
    // reads both through super (its trunk metas 12-15 answer their own); a carried or held log drew Block's default,
    // the unlocalized "log", which names no texture. Log2 (165) is answered by FCBlockLogWoodMixin.
    public static final String[] treeTextureTypes = new String[] {"tree_side", "tree_spruce", "tree_birch",
            "tree_jungle"};
    private Icon[] iconArray;
    private Icon treeTopIcon;

    // 1.5.2 BlockLog.getIcon (vanilla/client BlockLog.java:122-127); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null || this.treeTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        int axis = metadata & 12;
        int species = metadata & 3;
        return axis == 0 && (side == 1 || side == 0) ? this.treeTopIcon
                : (axis == 4 && (side == 5 || side == 4) ? this.treeTopIcon
                : (axis == 8 && (side == 2 || side == 3) ? this.treeTopIcon : this.iconArray[species]));
    }

    // 1.5.2 BlockLog.registerIcons (vanilla/client BlockLog.java:169-178)
    @Override
    public void registerIcons(IconRegister register) {
        this.treeTopIcon = register.registerIcon("tree_top");
        Icon[] icons = new Icon[treeTextureTypes.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(treeTextureTypes[i]);
        }
        this.iconArray = icons;
    }

    // Wood species lives in metadata bits 0-1 (orientation in 2-3). Without this the drop
    // stack carries damage 0, so a chopped spruce/birch/jungle log dropped as OAK. The bridge
    // (ItemStackHelper + ProxyRegistry variant table) turns this damage back into the right
    // 1.20.1 log block.
    @Override
    public int damageDropped(int metadata) {
        return metadata & 3;
    }

    /**
     * 1.5.2 BlockLog.onBlockPlaced (vanilla/server BlockLog.java:90-113): the axis follows the clicked face, as 1.20.1
     * RotatedPillarBlock.getStateForPlacement does: faces 0/1 (bottom, top) give a vertical log (0), faces 2/3
     * (north, south) a north-south log (8), faces 4/5 (west, east) an east-west log (4), with the species bits 0-1 of
     * the placed metadata kept. FCBlockLog inherits it (log 17, and log2 165 for the four modern woods). Without it the
     * FC route (Modern-Common ItemBlock.onItemUse) placed every log vertical; WorldBridge's log codec reads bits 2-3 as
     * the axis.
     * FCMOD-INTEGRATION(1.20.1): placement_validity: decision 3's log row, the 1.5.2 base hook FCBlockLog inherits
     */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
                             int metadata) {
        int species = metadata & 3;
        int orientation = 0;

        switch (side) {
            case 0:
            case 1:
                orientation = 0;
                break;

            case 2:
            case 3:
                orientation = 8;
                break;

            case 4:
            case 5:
                orientation = 4;
                break;

            default:
                break;
        }

        return species | orientation;
    }

    /**
     * 1.5.2 BlockLog.breakBlock (vanilla/server BlockLog.java:57-85): a removed log flags (metadata bit 8, flag 4)
     * every leaves block (Block.leaves, id 18) in the 9x9x9 around it that is not flagged yet.
     * FCBlockLeaves.updateTick then decays the flagged leaves that no live log within 4 steps still holds.
     */
    @Override
    public void breakBlock(World world, int i, int j, int k, int iBlockID, int iMetadata) {
        byte range = 4;
        int checkRange = range + 1;

        if (world.checkChunksExist(i - checkRange, j - checkRange, k - checkRange,
                i + checkRange, j + checkRange, k + checkRange)) {
            for (int offsetI = -range; offsetI <= range; ++offsetI) {
                for (int offsetJ = -range; offsetJ <= range; ++offsetJ) {
                    for (int offsetK = -range; offsetK <= range; ++offsetK) {
                        int iTempID = world.getBlockId(i + offsetI, j + offsetJ, k + offsetK);

                        if (iTempID == Block.leaves.blockID) {
                            int iTempMetadata = world.getBlockMetadata(i + offsetI, j + offsetJ, k + offsetK);

                            if ((iTempMetadata & 8) == 0) {
                                world.setBlockMetadata(i + offsetI, j + offsetJ, k + offsetK, iTempMetadata | 8, 4);
                            }
                        }
                    }
                }
            }
        }
    }

    // 1.5.2 BlockLog.getRenderType (vanilla/client BlockLog.java:37-40). FCBlockLog (17, and log2 at 165) does not
    // override it.
    @Override
    public int getRenderType() {
        return 31;
    }
}

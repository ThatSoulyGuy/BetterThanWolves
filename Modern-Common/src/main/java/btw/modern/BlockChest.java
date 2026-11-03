package btw.modern;

import java.util.List;

public class BlockChest extends BlockContainer {

    public int isTrapped;

    // vanilla/client BlockChest.java:31 sets tabDecorations in the (only) 1.5.2 constructor: FC's chest 1035
    // (FCBlockChest, no tab of its own) is listed under Decorations through it. FC hides the vanilla-id chest 54
    // afterwards with setCreativeTab(null) (vanilla/client Block.java:223).
    protected BlockChest(int id) {
        super(id, Material.wood);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    protected BlockChest(int id, int chestType) {
        super(id, Material.wood);
        this.isTrapped = chestType;
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    // 1.5.2 BlockChest.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockChest.java:39-50). FCBlockChest (id 54
    // and 1035) extends this and overrides neither. With the inherited true, Block.isNormalCube(54) and so
    // World.isBlockNormalCube reported the inset chest as a full solid cube, so an FC mob narrower than ~0.625
    // pressed against a chest had its eye sample inside it (Entity.isEntityInsideOpaqueBlock) and took an inWall
    // hit every tick.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z) {
        if (blockAccess.getBlockId(x, y, z - 1) == this.blockID) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0F, 0.9375F, 0.875F, 0.9375F);
        } else if (blockAccess.getBlockId(x, y, z + 1) == this.blockID) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 1.0F);
        } else if (blockAccess.getBlockId(x - 1, y, z) == this.blockID) {
            this.setBlockBounds(0.0F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        } else if (blockAccess.getBlockId(x + 1, y, z) == this.blockID) {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 1.0F, 0.875F, 0.9375F);
        } else {
            this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        int facing;
        switch (dir) {
            case 0: facing = 2; break;
            case 1: facing = 5; break;
            case 2: facing = 3; break;
            case 3: facing = 4; break;
            default: facing = 2;
        }

        // Check for adjacent chests to form double-chest — align facing
        int northId = world.getBlockId(x, y, z - 1);
        int southId = world.getBlockId(x, y, z + 1);
        int westId = world.getBlockId(x - 1, y, z);
        int eastId = world.getBlockId(x + 1, y, z);

        if (northId != this.blockID && southId != this.blockID
                && westId != this.blockID && eastId != this.blockID) {
            // Single chest — just set facing
            world.setBlockMetadataWithNotify(x, y, z, facing);
        } else {
            // Adjacent chest exists — align facing perpendicular to the double-chest axis
            if ((northId == this.blockID || southId == this.blockID) && (facing == 4 || facing == 5)) {
                if (northId == this.blockID) {
                    world.setBlockMetadataWithNotify(x, y, z - 1, facing);
                }
                if (southId == this.blockID) {
                    world.setBlockMetadataWithNotify(x, y, z + 1, facing);
                }
                world.setBlockMetadataWithNotify(x, y, z, facing);
            }
            if ((westId == this.blockID || eastId == this.blockID) && (facing == 2 || facing == 3)) {
                if (westId == this.blockID) {
                    world.setBlockMetadataWithNotify(x - 1, y, z, facing);
                }
                if (eastId == this.blockID) {
                    world.setBlockMetadataWithNotify(x + 1, y, z, facing);
                }
                world.setBlockMetadataWithNotify(x, y, z, facing);
            }
        }
    }

    public void unifyAdjacentChests(World world, int x, int y, int z) {}

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        IInventory inventory = this.getInventory(world, x, y, z);
        if (inventory != null) {
            player.displayGUIChest(inventory);
        }
        return true;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.5.2 BlockChest.getInventory (vanilla/client BlockChest.java:433-488) in
    // full: a chest does not open (and reads 0 to a comparator) when a normal cube or a sitting ocelot is on it or on the
    // other half of its double chest
    public IInventory getInventory(World world, int x, int y, int z) {
        Object inv = world.getBlockTileEntity(x, y, z);
        if (inv == null) return null;
        if (!(inv instanceof IInventory)) return null;

        // Block above is solid — can't open
        if (world.isBlockNormalCube(x, y + 1, z)) return null;
        if (isOcelotBlockingChest(world, x, y, z)) return null;

        // The other half of a double chest blocked the same way
        if (world.getBlockId(x - 1, y, z) == this.blockID
                && (world.isBlockNormalCube(x - 1, y + 1, z) || isOcelotBlockingChest(world, x - 1, y, z))) {
            return null;
        }
        if (world.getBlockId(x + 1, y, z) == this.blockID
                && (world.isBlockNormalCube(x + 1, y + 1, z) || isOcelotBlockingChest(world, x + 1, y, z))) {
            return null;
        }
        if (world.getBlockId(x, y, z - 1) == this.blockID
                && (world.isBlockNormalCube(x, y + 1, z - 1) || isOcelotBlockingChest(world, x, y, z - 1))) {
            return null;
        }
        if (world.getBlockId(x, y, z + 1) == this.blockID
                && (world.isBlockNormalCube(x, y + 1, z + 1) || isOcelotBlockingChest(world, x, y, z + 1))) {
            return null;
        }

        // Check for adjacent chest and merge into double-chest inventory
        if (world.getBlockId(x - 1, y, z) == this.blockID) {
            inv = new InventoryLargeChest("container.chestDouble",
                    (IInventory) world.getBlockTileEntity(x - 1, y, z), (IInventory) inv);
        }
        if (world.getBlockId(x + 1, y, z) == this.blockID) {
            inv = new InventoryLargeChest("container.chestDouble",
                    (IInventory) inv, (IInventory) world.getBlockTileEntity(x + 1, y, z));
        }
        if (world.getBlockId(x, y, z - 1) == this.blockID) {
            inv = new InventoryLargeChest("container.chestDouble",
                    (IInventory) world.getBlockTileEntity(x, y, z - 1), (IInventory) inv);
        }
        if (world.getBlockId(x, y, z + 1) == this.blockID) {
            inv = new InventoryLargeChest("container.chestDouble",
                    (IInventory) inv, (IInventory) world.getBlockTileEntity(x, y, z + 1));
        }
        return (IInventory) inv;
    }

    /** 1.5.2 BlockChest.isOcelotBlockingChest (vanilla/client BlockChest.java:539-557): a sitting ocelot on the chest. */
    private static boolean isOcelotBlockingChest(World world, int x, int y, int z) {
        List ocelots = world.getEntitiesWithinAABB(EntityOcelot.class, AxisAlignedBB.getAABBPool().getAABB(
                (double) x, (double) (y + 1), (double) z, (double) (x + 1), (double) (y + 2), (double) (z + 1)));
        for (Object o : ocelots) {
            if (((EntityOcelot) o).isSitting()) {
                return true;
            }
        }
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.5.2's comparator answers (vanilla/client BlockChest.java:563-575): a
    // comparator reads the BTW chest 1035 (FCBlockChest, a ProxyBlock) as a vanilla chest, the inventory getInventory
    // opens (ruling Q-RS3 (a)); the vanilla-id chest 54 keeps 1.20.1's ChestBlock answers
    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        return Container.calcRedstoneFromInventory(this.getInventory(world, x, y, z));
    }

    // 1.5.2 BlockChest.getRenderType (vanilla/client BlockChest.java:55-58): FCBlockChest (54, 1035) does not
    // override it.
    @Override
    public int getRenderType() {
        return 22;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockChest.registerIcons (vanilla/client BlockChest.java:581-584)
    // registers "wood", the oak planks the chest's particles and FC captures draw. FCBlockChest (54, 1035) inherits
    // it; Block's default registered the unlocalized "chest", which only a map entry sent to the same oak planks.
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("wood");
    }
}

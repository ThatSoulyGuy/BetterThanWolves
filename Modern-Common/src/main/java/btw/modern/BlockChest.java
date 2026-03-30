package btw.modern;

public class BlockChest extends BlockContainer {

    public int isTrapped;

    protected BlockChest(int id) {
        super(id, Material.wood);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    protected BlockChest(int id, int chestType) {
        super(id, Material.wood);
        this.isTrapped = chestType;
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
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

    public IInventory getInventory(World world, int x, int y, int z) {
        Object inv = world.getBlockTileEntity(x, y, z);
        if (inv == null) return null;
        if (!(inv instanceof IInventory)) return null;

        // Block above is solid — can't open
        if (world.isBlockNormalCube(x, y + 1, z)) return null;

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
}

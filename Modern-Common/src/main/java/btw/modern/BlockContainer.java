package btw.modern;

public abstract class BlockContainer extends Block {

    protected BlockContainer(int id, Material material) {
        super(id, material);
    }

    public abstract TileEntity createNewTileEntity(World world);

    protected static void dropInventory(World world, int x, int y, int z) {
        if (!(world.getBlockTileEntity(x, y, z) instanceof IInventory inventory)) return;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;
            float ox = world.rand.nextFloat() * 0.8F + 0.1F;
            float oy = world.rand.nextFloat() * 0.8F + 0.1F;
            float oz = world.rand.nextFloat() * 0.8F + 0.1F;
            while (stack.stackSize > 0) {
                EntityItem item = new EntityItem(world, x + ox, y + oy, z + oz,
                        stack.splitStack(Math.min(world.rand.nextInt(21) + 10, stack.stackSize)));
                item.motionX = world.rand.nextGaussian() * 0.05F;
                item.motionY = world.rand.nextGaussian() * 0.05F + 0.2F;
                item.motionZ = world.rand.nextGaussian() * 0.05F;
                world.spawnEntityInWorld(item);
            }
        }
    }
}

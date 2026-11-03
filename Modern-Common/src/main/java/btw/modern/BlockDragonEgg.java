package btw.modern;

public class BlockDragonEgg extends Block {

    protected BlockDragonEgg(int id) {
        super(id, Material.dragonEgg);
    }

    // 1.5.2 BlockDragonEgg.onBlockAdded / onNeighborBlockChange (vanilla/server BlockDragonEgg.java:29-41): schedule
    // the fall check. FCBlockDragonEgg.updateTick runs it as Block.CheckForFall. The port reaches the neighbour hook
    // (DragonEggBlock does not override neighborChanged); the schedule merges with the one 1.20.1 FallingBlock already
    // makes at the same delay, since scheduled ticks are de-duplicated per (pos, block).
    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        world.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate(world));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
        world.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate(world));
    }

    // 1.5.2 BlockDragonEgg.tickRate (vanilla/server BlockDragonEgg.java:145-148); the port default is 10
    @Override
    public int tickRate(World world) {
        return 5;
    }

    // 1.5.2 BlockDragonEgg.isOpaqueCube / renderAsNormalBlock / getRenderType (vanilla/client
    // BlockDragonEgg.java:155-166, :180-183). FCBlockDragonEgg (122) overrides none of them.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 27;
    }
}

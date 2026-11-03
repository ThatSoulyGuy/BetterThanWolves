package btw.modern;

import java.util.Random;

public abstract class BlockBasePressurePlate extends Block {

    // vanilla/client BlockBasePressurePlate.java:27 sets tabRedstone in the constructor (BlockPressurePlate chains to it):
    // FC's soulforged pressure plate 230 (FCBlockPressurePlate, no tab of its own) is listed under Redstone through it.
    // The same constructor sets the random-tick flag (vanilla/server BlockBasePressurePlate.java:27). On the vanilla-id
    // plates 70 and 72 the flag stays inert: TickPolicy keeps their random ticks off (1.20.1 re-checks a pressed plate
    // itself, every 20 ticks), so this class's updateTick runs for them only from FC's own calls.
    protected BlockBasePressurePlate(int id, Material material) {
        super(id, material);
        this.setCreativeTab(CreativeTabs.tabRedstone);
        this.setTickRandomly(true);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2's only constructor (vanilla/client
    // BlockBasePressurePlate.java:21-30) keeps the plate's icon name, and registerIcons (:277-280) registers it.
    // BlockPressurePlate passes FC's names up: the soulforged steel plate 230 registers
    // "fcBlockPressurePlateSoulforgedSteel" (FCBetterThanWolves.java:1047-1049), where Block's default registered its
    // unlocalized name "pressurePlate", drawn as oak planks.
    private String pressurePlateIconName;

    protected BlockBasePressurePlate(int id, String iconName, Material material) {
        this(id, material);
        this.pressurePlateIconName = iconName;
    }

    // 1.5.2 BlockBasePressurePlate.registerIcons (vanilla/client BlockBasePressurePlate.java:277-280). A plate made
    // through the port-only constructor (no name) keeps Block's default.
    @Override
    public void registerIcons(IconRegister register) {
        if (this.pressurePlateIconName == null) {
            super.registerIcons(register);
            return;
        }
        this.blockIcon = register.registerIcon(this.pressurePlateIconName);
    }

    // 1.5.2 BlockBasePressurePlate.getCollisionBoundingBoxFromPool (vanilla/client BlockBasePressurePlate.java:67-70):
    // pressure plates have no collision box (FCBlockPressurePlate overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockBasePressurePlate.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockBasePressurePlate.java:76-87).
    // With the inherited true, World.isBlockNormalCube reported this passable block as solid, so an FC mob's
    // eye inside the cell counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    public void func_94353_c_(int metadata) {}

    /**
     * 1.5.2 BlockBasePressurePlate.getMetaFromWeight (abstract there): the metadata for a weight 0-15.
     * BlockPressurePlate answers 1.5.2's on/off value.
     */
    public int getMetaFromWeight(int weight) { return 0; }

    /**
     * 1.5.2 BlockBasePressurePlate.getPowerSupply (abstract there): the power 0-15 of a metadata.
     * BlockPressurePlate answers 1.5.2's on/off value.
     */
    public int getPowerSupply(int metadata) { return 0; }

    /**
     * 1.5.2 BlockBasePressurePlate.getPlateState (abstract there): the weight 0-15 of what is on the plate.
     * BlockPressurePlate answers it by its mob type.
     */
    public int getPlateState(World world, int x, int y, int z) { return 0; }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: the 1.5.2 plate mechanism (vanilla/client BlockBasePressurePlate.java:58-61,
    // :125-235) the base class lacked: FC's contact (ProxyBlock.entityInside for the soulforged plate 230; FC's own
    // Entity.doBlockCollisions for FC mobs on the vanilla plates 70 and 72, whose puppets never run 1.20.1's
    // entityInside), FC's scheduled re-check and the power answers 1.20.1 asks through ProxyBlock

    // 1.5.2 BlockBasePressurePlate.tickRate (vanilla/client BlockBasePressurePlate.java:58-61)
    @Override
    public int tickRate(World world) {
        return 20;
    }

    // 1.5.2 BlockBasePressurePlate.updateTick (vanilla/client BlockBasePressurePlate.java:125-136): a pressed plate
    // re-checks what is on it
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote) {
            int power = this.getPowerSupply(world.getBlockMetadata(x, y, z));
            if (power > 0) {
                this.setStateIfMobInteractsWithPlate(world, x, y, z, power);
            }
        }
    }

    // 1.5.2 BlockBasePressurePlate.onEntityCollidedWithBlock (vanilla/client BlockBasePressurePlate.java:141-152): a
    // released plate checks what is on it
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote) {
            int power = this.getPowerSupply(world.getBlockMetadata(x, y, z));
            if (power == 0) {
                this.setStateIfMobInteractsWithPlate(world, x, y, z, power);
            }
        }
    }

    /**
     * 1.5.2 BlockBasePressurePlate.setStateIfMobInteractsWithPlate (vanilla/client BlockBasePressurePlate.java:157-180):
     * the plate takes the weight of what is on it (metadata, flag 2, then the neighbour notification), clicks when it
     * changes, and re-checks itself every tickRate while pressed.
     */
    public void setStateIfMobInteractsWithPlate(World world, int x, int y, int z, int oldPower) {
        int newPower = this.getPlateState(world, x, y, z);
        boolean wasPressed = oldPower > 0;
        boolean isPressed = newPower > 0;

        if (oldPower != newPower) {
            world.setBlockMetadata(x, y, z, this.getMetaFromWeight(newPower), 2);
            this.func_94354_b_(world, x, y, z);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }

        if (!isPressed && wasPressed) {
            world.playSoundEffect((double) x + 0.5D, (double) y + 0.1D, (double) z + 0.5D, "random.click", 0.3F, 0.5F);
        } else if (isPressed && !wasPressed) {
            world.playSoundEffect((double) x + 0.5D, (double) y + 0.1D, (double) z + 0.5D, "random.click", 0.3F, 0.6F);
        }

        if (isPressed) {
            world.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate(world));
        }
    }

    /** 1.5.2 BlockBasePressurePlate.getSensitiveAABB (vanilla/client BlockBasePressurePlate.java:185-189). */
    public AxisAlignedBB getSensitiveAABB(int x, int y, int z) {
        float border = 0.125F;
        return AxisAlignedBB.getAABBPool().getAABB((double) ((float) x + border), (double) y,
                (double) ((float) z + border), (double) ((float) (x + 1) - border), (double) y + 0.25D,
                (double) ((float) (z + 1) - border));
    }

    // 1.5.2 BlockBasePressurePlate.breakBlock (vanilla/client BlockBasePressurePlate.java:194-202): a pressed plate
    // tells its neighbours it is gone
    @Override
    public void breakBlock(World world, int x, int y, int z, int oldBlockID, int oldMetadata) {
        if (this.getPowerSupply(oldMetadata) > 0) {
            this.func_94354_b_(world, x, y, z);
        }
        super.breakBlock(world, x, y, z, oldBlockID, oldMetadata);
    }

    /** 1.5.2 BlockBasePressurePlate.func_94354_b_ (vanilla/client BlockBasePressurePlate.java:204-208). */
    public void func_94354_b_(World world, int x, int y, int z) {
        world.notifyBlocksOfNeighborChange(x, y, z, this.blockID);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this.blockID);
    }

    // 1.5.2 BlockBasePressurePlate.isProvidingWeakPower (vanilla/client BlockBasePressurePlate.java:215-218)
    @Override
    public int isProvidingWeakPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return this.getPowerSupply(blockAccess.getBlockMetadata(x, y, z));
    }

    // 1.5.2 BlockBasePressurePlate.isProvidingStrongPower (vanilla/client BlockBasePressurePlate.java:224-227): the
    // block under the plate (side 1)
    @Override
    public int isProvidingStrongPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return side == 1 ? this.getPowerSupply(blockAccess.getBlockMetadata(x, y, z)) : 0;
    }

    // 1.5.2 BlockBasePressurePlate.canProvidePower (vanilla/client BlockBasePressurePlate.java:232-235)
    @Override
    public boolean canProvidePower() {
        return true;
    }

    // 1.5.2 BlockBasePressurePlate.getBlocksMovement / getMobilityFlag (vanilla/client
    // BlockBasePressurePlate.java:89-92, :252-255; getBlocksMovement is misnamed, true = passable): a plate's cell is
    // open to FC's path finder, and FC's piston rules break a plate. FCBlockPressurePlate (70, 72, 230) overrides
    // neither.
    @Override
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) {
        return true;
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }
}

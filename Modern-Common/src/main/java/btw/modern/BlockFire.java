package btw.modern;

public class BlockFire extends Block {

    public static int[] chanceToEncourageFire = new int[4096];
    public static int[] abilityToCatchFire = new int[4096];

    private Icon[] fireIcons = new Icon[2];

    protected BlockFire(int id) {
        super(id, Material.fire);
        setTickRandomly(true);
    }

    @Override
    public void registerIcons(IconRegister register) {
        fireIcons[0] = register.registerIcon("fire_layer_0");
        fireIcons[1] = register.registerIcon("fire_layer_1");
        this.blockIcon = fireIcons[0];
    }

    @Override
    public Icon func_94438_c(int index) {
        return fireIcons[index & 1];
    }

    // 1.5.2 BlockFire.getCollisionBoundingBoxFromPool (vanilla/client BlockFire.java:90-93): fire has no
    // collision box (FCBlockFire / FCBlockFireStoked do not override it)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockFire.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockFire.java:99-110).
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

    // 1.5.2 BlockFire.tickRate (vanilla/server BlockFire.java:130-133). FCBlockFire.updateTick reschedules itself at
    // tickRate + nextInt(10); the port default 10 made FC fire tick about 2.4x too fast. 1.20.1 FireBlock schedules
    // the same 30 + nextInt(10). FCBlockFireStoked overrides it (42).
    @Override
    public int tickRate(World world) {
        return 30;
    }

    // 1.5.2 BlockFire.isCollidable (vanilla/client BlockFire.java:331-334)
    @Override
    public boolean isCollidable() {
        return false;
    }

    // 1.5.2 BlockFire.canBlockCatchFire — called by FCBlockFire.updateTick (super.canBlockCatchFire)
    // and Block.GetCanBlockBeIncinerated; reads the array populated by Block.SetFireProperties
    public boolean canBlockCatchFire(IBlockAccess blockAccess, int i, int j, int k) {
        return chanceToEncourageFire[blockAccess.getBlockId(i, j, k)] > 0;
    }

    // 1.5.2 FCBlockFire.CanBlockBeDestroyedByFire — mirrors the FC static so shim-side callers
    // (Block.IsIncineratedInCrucible in vanilla) see real flammability
    public static boolean CanBlockBeDestroyedByFire(int blockID) {
        return abilityToCatchFire[blockID] > 0;
    }

    // 1.5.2 BlockFire.canNeighborBurn — called by FCBlockFire.updateTick (not overridden there);
    // fire extinguishes immediately without it
    public boolean canNeighborBurn(World world, int x, int y, int z) {
        return this.canBlockCatchFire(world, x + 1, y, z)
            || this.canBlockCatchFire(world, x - 1, y, z)
            || this.canBlockCatchFire(world, x, y - 1, z)
            || this.canBlockCatchFire(world, x, y + 1, z)
            || this.canBlockCatchFire(world, x, y, z - 1)
            || this.canBlockCatchFire(world, x, y, z + 1);
    }

    // 1.5.2 BlockFire.canPlaceBlockAt (vanilla/server BlockFire.java:361-364): a solid top below, or a burnable neighbour
    // in FC's fire tables. FCBlockFire.updateTick and FCBlockFireStoked.ValidateState put the fire out when this fails,
    // and FCEntityLightningBolt places fire only where it holds. The inherited true kept every fire alive.
    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.doesBlockHaveSolidTopSurface(x, y - 1, z) || this.canNeighborBurn(world, x, y, z);
    }

    // 1.5.2 BlockFire.onNeighborBlockChange (vanilla/server BlockFire.java:370-376). Reached for fire (FireBlock does not
    // override neighborChanged) and for stoked fire (ProxyBlock.neighborChanged); 1.20.1 FireBlock.updateShape still
    // runs its own survival check next to it.
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
        if (!world.doesBlockHaveSolidTopSurface(x, y - 1, z) && !this.canNeighborBurn(world, x, y, z)) {
            world.setBlockToAir(x, y, z);
        }
    }

    // 1.5.2 BlockFire.onBlockAdded (vanilla/server BlockFire.java:381-394): on obsidian outside the End, try to open a
    // portal (Block.portal is FCBlockPortal; the bridge answers its tryToCreatePortal with 1.20.1's PortalShape,
    // btw.forge.NetherPortalShapes); otherwise put out unsupported fire or schedule the first tick. FCBlockFireStoked
    // overrides it. Port note: for vanilla fire (51) this is not reached today. FireBlock.onPlace ->
    // BaseFireBlock.onPlace never calls BlockBehaviour.onPlace, where the FC hook sits, and BaseFireBlock.onPlace
    // already lights 1.20.1 portals (BaseFireBlock.java:123-139) and schedules the first tick (FireBlock.onPlace,
    // 30 + nextInt(10)). A future onPlace route for fire must drop one of the two portal attempts.
    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (world.provider.dimensionId > 0 || world.getBlockId(x, y - 1, z) != Block.obsidian.blockID
                || !Block.portal.tryToCreatePortal(world, x, y, z)) {
            if (!world.doesBlockHaveSolidTopSurface(x, y - 1, z) && !this.canNeighborBurn(world, x, y, z)) {
                world.setBlockToAir(x, y, z);
            } else {
                world.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate(world) + world.rand.nextInt(10));
            }
        }
    }

    // 1.5.2 BlockFire.randomDisplayTick (vanilla/client BlockFire.java:400-479): the crackle and the smoke. FCBlockFire and
    // FCBlockFireStoked do not override it, so stoked fire (251, a ProxyBlock: ProxyBlock.animateTick) was silent and
    // smokeless on Block's empty body. The sound goes through the client FC world's playSound. Block.fire is FC's
    // FCBlockFire after BTWLifecycle replaces id 51 (as 1.5.2 Block.java:220); this block stands in if it is unset.
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, java.util.Random random) {
        if (random.nextInt(24) == 0) {
            world.playSound((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F),
                    "fire.fire", 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }

        BlockFire fireBlock = Block.fire != null ? Block.fire : this;
        int i;
        float fx;
        float fy;
        float fz;

        if (!world.doesBlockHaveSolidTopSurface(x, y - 1, z) && !fireBlock.canBlockCatchFire(world, x, y - 1, z)) {
            if (fireBlock.canBlockCatchFire(world, x - 1, y, z)) {
                for (i = 0; i < 2; ++i) {
                    fx = (float) x + random.nextFloat() * 0.1F;
                    fy = (float) y + random.nextFloat();
                    fz = (float) z + random.nextFloat();
                    world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
                }
            }

            if (fireBlock.canBlockCatchFire(world, x + 1, y, z)) {
                for (i = 0; i < 2; ++i) {
                    fx = (float) (x + 1) - random.nextFloat() * 0.1F;
                    fy = (float) y + random.nextFloat();
                    fz = (float) z + random.nextFloat();
                    world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
                }
            }

            if (fireBlock.canBlockCatchFire(world, x, y, z - 1)) {
                for (i = 0; i < 2; ++i) {
                    fx = (float) x + random.nextFloat();
                    fy = (float) y + random.nextFloat();
                    fz = (float) z + random.nextFloat() * 0.1F;
                    world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
                }
            }

            if (fireBlock.canBlockCatchFire(world, x, y, z + 1)) {
                for (i = 0; i < 2; ++i) {
                    fx = (float) x + random.nextFloat();
                    fy = (float) y + random.nextFloat();
                    fz = (float) (z + 1) - random.nextFloat() * 0.1F;
                    world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
                }
            }

            if (fireBlock.canBlockCatchFire(world, x, y + 1, z)) {
                for (i = 0; i < 2; ++i) {
                    fx = (float) x + random.nextFloat();
                    fy = (float) (y + 1) - random.nextFloat() * 0.1F;
                    fz = (float) z + random.nextFloat();
                    world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            for (i = 0; i < 3; ++i) {
                fx = (float) x + random.nextFloat();
                fy = (float) y + random.nextFloat() * 0.5F + 0.5F;
                fz = (float) z + random.nextFloat();
                world.spawnParticle("largesmoke", (double) fx, (double) fy, (double) fz, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // 1.5.2 BlockFire.getRenderType (vanilla/client BlockFire.java:115-118). FCBlockFire (51) and FCBlockFireStoked
    // (251) do not override it.
    @Override
    public int getRenderType() {
        return 3;
    }
}

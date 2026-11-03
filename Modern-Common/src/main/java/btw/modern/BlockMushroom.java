package btw.modern;

import java.util.Random;

public class BlockMushroom extends BlockFlower {

    protected BlockMushroom(int id) {
        super(id, Material.plants);
        // 1.5.2 BlockMushroom constructor (vanilla/server BlockMushroom.java:28)
        setTickRandomly(true);
    }

    protected BlockMushroom(int id, String iconName) {
        super(id, Material.plants);
        // 1.5.2 BlockMushroom constructor (vanilla/server BlockMushroom.java:28)
        setTickRandomly(true);
        this.mushroomIconName = iconName;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2's constructor keeps the icon name (field_94374_a,
    // vanilla/client BlockMushroom.java:21-26) and registerIcons (:155-158) registers it: "mushroom_brown" (39) and
    // "mushroom_red" (40), as BTWLifecycle replays FC's constructors. Block's default registered the unlocalized
    // "mushroom", the red cap block. The name-less constructor keeps Block's default.
    private String mushroomIconName;

    @Override
    public void registerIcons(IconRegister register) {
        if (this.mushroomIconName == null) {
            super.registerIcons(register);
            return;
        }
        this.blockIcon = register.registerIcon(this.mushroomIconName);
    }

    /**
     * 1.5.2 BlockMushroom.updateTick (vanilla/server BlockMushroom.java:34-86): 1 in 25 random ticks, unless 5 of
     * this mushroom already stand in the 9x3x9 around it, a short random walk picks an empty cell where
     * canBlockStay holds and a new mushroom grows there. FCBlockMushroom reaches it through super.updateTick (red
     * mushrooms off mycelium, not in the End); FCBlockMushroomBrown runs its own darkness-only copy.
     */
    @Override
    public void updateTick(World world, int i, int j, int k, Random rand) {
        if (rand.nextInt(25) == 0) {
            byte range = 4;
            int countdown = 5;
            int tempI;
            int tempJ;
            int tempK;

            for (tempI = i - range; tempI <= i + range; ++tempI) {
                for (tempK = k - range; tempK <= k + range; ++tempK) {
                    for (tempJ = j - 1; tempJ <= j + 1; ++tempJ) {
                        if (world.getBlockId(tempI, tempJ, tempK) == blockID) {
                            --countdown;

                            if (countdown <= 0) {
                                return;
                            }
                        }
                    }
                }
            }

            tempI = i + rand.nextInt(3) - 1;
            tempJ = j + rand.nextInt(2) - rand.nextInt(2);
            tempK = k + rand.nextInt(3) - 1;

            for (int step = 0; step < 4; ++step) {
                if (world.isAirBlock(tempI, tempJ, tempK) && canBlockStay(world, tempI, tempJ, tempK)) {
                    i = tempI;
                    j = tempJ;
                    k = tempK;
                }

                tempI = i + rand.nextInt(3) - 1;
                tempJ = j + rand.nextInt(2) - rand.nextInt(2);
                tempK = k + rand.nextInt(3) - 1;
            }

            if (world.isAirBlock(tempI, tempJ, tempK) && canBlockStay(world, tempI, tempJ, tempK)) {
                world.setBlock(tempI, tempJ, tempK, blockID);
            }
        }
    }

    /**
     * 1.5.2 BlockMushroom.fertilizeMushroom (vanilla/server BlockMushroom.java:124-148): the mushroom is removed and
     * a huge mushroom of its colour is generated in its place; if that fails, the mushroom is put back.
     * FCBlockMushroom and FCBlockMushroomBrown call it for 1 in 50 random ticks on mycelium. The caps land as ids
     * 99/100, which FCBlockMushroomCapLegacy.onBlockAdded turns into FC's flammable caps, as in 4.B.
     */
    public boolean fertilizeMushroom(World world, int i, int j, int k, Random rand) {
        int iMetadata = world.getBlockMetadata(i, j, k);
        world.setBlockToAir(i, j, k);
        WorldGenBigMushroom generator = null;

        if (blockID == Block.mushroomBrown.blockID) {
            generator = new WorldGenBigMushroom(0);
        } else if (blockID == Block.mushroomRed.blockID) {
            generator = new WorldGenBigMushroom(1);
        }

        if (generator != null && generator.generate(world, rand, i, j, k)) {
            return true;
        } else {
            world.setBlock(i, j, k, blockID, iMetadata, 3);
            return false;
        }
    }
}

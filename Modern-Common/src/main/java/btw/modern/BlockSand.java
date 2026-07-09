package btw.modern;

public class BlockSand extends Block {
    public static boolean fallInstantly = false;

    public BlockSand(int id) {
        super(id, Material.sand);
    }

    public BlockSand(int id, Material material) {
        super(id, material);
    }

    // 1.5.2 BlockSand.canFallBelow (vanilla BlockSand.java:113) — air/fire/fluid below lets
    // a falling block continue. Used by the falling-block update path.
    public static boolean canFallBelow(World world, int x, int y, int z) {
        int belowId = world.getBlockId(x, y, z);
        if (belowId == 0 || belowId == Block.fire.blockID) {
            return true;
        }
        Material material = Block.blocksList[belowId].blockMaterial;
        return material == Material.water || material == Material.lava;
    }
}

package btw.modern;

public class BlockSilverfish extends Block {

    protected BlockSilverfish(int id) {
        super(id, Material.clay);
    }

    // 1.5.2 BlockSilverfish.getPosingIdByMetadata (vanilla BlockSilverfish.java:57) — the
    // mutant silverfish born from FC cow breeding (FCEntityCow.BirthMutant) infests these.
    public static boolean getPosingIdByMetadata(int blockId) {
        return blockId == Block.stone.blockID
                || blockId == Block.cobblestone.blockID
                || blockId == Block.stoneBrick.blockID;
    }
}

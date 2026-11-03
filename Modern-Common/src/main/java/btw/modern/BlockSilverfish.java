package btw.modern;

public class BlockSilverfish extends Block {

    protected BlockSilverfish(int id) {
        super(id, Material.clay);
    }

    // 1.5.2 BlockSilverfish.quantityDropped (vanilla/server BlockSilverfish.java:46-52): an infested block drops nothing
    // (FCBlockSilverfish keeps it). Without it, FC's harvest dropped the infested block itself (the drop-parity audit D7).
    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    // 1.5.2 BlockSilverfish.getPosingIdByMetadata (vanilla BlockSilverfish.java:57) — the
    // mutant silverfish born from FC cow breeding (FCEntityCow.BirthMutant) infests these.
    public static boolean getPosingIdByMetadata(int blockId) {
        return blockId == Block.stone.blockID
                || blockId == Block.cobblestone.blockID
                || blockId == Block.stoneBrick.blockID;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockSilverfish's icon members (vanilla/client
    // BlockSilverfish.java:35-44): the stone, cobblestone or stone brick it hides in, and no icon of its own.
    // FCBlockSilverfish (97) answers getIcon itself.
    @Override
    public Icon getIcon(int side, int metadata) {
        return metadata == 1 ? Block.cobblestone.getBlockTextureFromSide(side)
                : (metadata == 2 ? Block.stoneBrick.getBlockTextureFromSide(side)
                : Block.stone.getBlockTextureFromSide(side));
    }

    @Override
    public void registerIcons(IconRegister register) {}
}

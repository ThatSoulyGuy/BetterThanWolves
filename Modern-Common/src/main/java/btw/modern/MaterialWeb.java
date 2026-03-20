package btw.modern;

final class MaterialWeb extends Material {
    MaterialWeb(MapColor mapColor) {
        super(mapColor);
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }
}

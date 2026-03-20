package btw.modern;

public class MaterialPortal extends Material {
    public MaterialPortal(MapColor mapColor) {
        super(mapColor);
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean getCanBlockGrass() {
        return false;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }
}

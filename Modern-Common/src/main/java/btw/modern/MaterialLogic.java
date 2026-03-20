package btw.modern;

public class MaterialLogic extends Material {
    public MaterialLogic(MapColor mapColor) {
        super(mapColor);
        this.setAlwaysHarvested();
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

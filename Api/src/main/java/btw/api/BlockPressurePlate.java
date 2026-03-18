package btw.api;

public class BlockPressurePlate extends BlockBasePressurePlate {

    protected BlockPressurePlate(int id, Material material) {
        super(id, material);
    }

    protected BlockPressurePlate(int id, String iconName, Material material, EnumMobType mobType) {
        super(id, material);
    }
}

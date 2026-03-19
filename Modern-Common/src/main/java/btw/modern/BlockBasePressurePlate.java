package btw.modern;

public abstract class BlockBasePressurePlate extends Block {

    protected BlockBasePressurePlate(int id, Material material) {
        super(id, material);
    }

    public void func_94353_c_(int metadata) {}
    public int getMetaFromWeight(int weight) { return 0; }
    public int getPowerSupply(int metadata) { return 0; }
}

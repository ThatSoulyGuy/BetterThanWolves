package btw.api;

public class BlockCocoa extends BlockDirectional {

    protected BlockCocoa(int id) {
        super(id, Material.plants);
    }

    public static int func_72219_c(int metadata) {
        return (metadata >> 2) & 3;
    }
}

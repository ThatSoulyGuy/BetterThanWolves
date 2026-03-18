package btw.api;

public abstract class BlockButton extends Block {

    protected BlockButton(int id) {
        super(id, Material.circuits);
    }

    protected BlockButton(int id, boolean sensitive) {
        super(id, Material.circuits);
    }
}

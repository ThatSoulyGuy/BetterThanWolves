package btw.modern;

public class NBTTagDouble extends NBTBase {
    public double data;

    public NBTTagDouble(String name) {}

    public NBTTagDouble(String name, double data) {
        this.data = data;
    }

    @Override
    public byte getId() { return 6; }

    @Override
    public NBTBase copy() {
        return new NBTTagDouble("", this.data);
    }
}

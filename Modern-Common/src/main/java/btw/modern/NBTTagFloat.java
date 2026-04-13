package btw.modern;

public class NBTTagFloat extends NBTBase {
    public float data;

    public NBTTagFloat(String name) {}

    public NBTTagFloat(String name, float data) {
        this.data = data;
    }

    @Override
    public byte getId() { return 5; }

    @Override
    public NBTBase copy() {
        return new NBTTagFloat("", this.data);
    }
}

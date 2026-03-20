package btw.api;

public class NBTTagString extends NBTBase {
    public String data;

    public NBTTagString(String name) {}

    public NBTTagString(String name, String data) {
        this.data = data;
    }

    @Override
    public byte getId() { return 8; }

    @Override
    public NBTBase copy() {
        return new NBTTagString("", this.data);
    }
}

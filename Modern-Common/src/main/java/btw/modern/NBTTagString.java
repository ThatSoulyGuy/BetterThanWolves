package btw.modern;

public class NBTTagString extends NBTBase {
    public String data;

    public NBTTagString(String name) {}

    public NBTTagString(String name, String data) {
        this.data = data;
    }
}

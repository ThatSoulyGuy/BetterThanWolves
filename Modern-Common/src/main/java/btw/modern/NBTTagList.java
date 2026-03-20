package btw.modern;

import java.util.ArrayList;
import java.util.List;

public class NBTTagList extends NBTBase {
    private List tagList = new ArrayList();
    private byte tagType;

    public NBTTagList() {}

    public NBTTagList(String name) {}

    public NBTBase tagAt(int index) {
        return (NBTBase) tagList.get(index);
    }

    public int tagCount() {
        return tagList.size();
    }

    public void appendTag(NBTBase tag) {
        tagList.add(tag);
    }

    public NBTBase removeTag(int index) {
        return (NBTBase) tagList.remove(index);
    }

    @Override
    public byte getId() { return 9; }

    @Override
    public String getName() { return ""; }

    @Override
    public NBTBase copy() {
        NBTTagList copy = new NBTTagList();
        copy.tagType = this.tagType;
        for (int i = 0; i < this.tagList.size(); i++) {
            NBTBase tag = (NBTBase) this.tagList.get(i);
            copy.tagList.add(tag.copy());
        }
        return copy;
    }
}

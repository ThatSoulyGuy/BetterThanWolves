package btw.api;

import java.util.LinkedList;
import java.util.List;

public abstract class StructureStart {
    protected List components = new LinkedList();
    public StructureBoundingBox boundingBox;

    public StructureBoundingBox getBoundingBox() { return boundingBox; }
    public List getComponents() { return components; }

    public void generateStructure(World world, java.util.Random rand, StructureBoundingBox box) {}
    public boolean isSizeableStructure() { return true; }
}

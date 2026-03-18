package btw.api;

import java.util.List;

public class MapGenNetherBridge extends StructureStart {

    public java.util.Map structureMap = new java.util.HashMap();

    public List getSpawnList() { return null; }
    public boolean hasStructureAt(int x, int y, int z) { return false; }
    public boolean HasStructureAtLoose(int x, int y, int z) { return false; }
    public StructureStart GetClosestStructureWithinRangeSq(double x, double z, int rangeSq) { return null; }
}

package btw.api;

public class SpawnListEntry extends WeightedRandomItem {
    public Class entityClass;
    public int minGroupCount;
    public int maxGroupCount;

    public SpawnListEntry(Class entityClass, int weight, int min, int max) {
        super(weight);
        this.entityClass = entityClass;
        this.minGroupCount = min;
        this.maxGroupCount = max;
    }
}

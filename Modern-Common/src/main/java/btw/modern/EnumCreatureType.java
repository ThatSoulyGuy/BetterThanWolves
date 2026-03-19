package btw.modern;

public enum EnumCreatureType {

    monster(90, false, false),
    creature(10, true, true),
    ambient(15, true, false),
    waterCreature(5, true, false);

    private final int maxNumberOfCreature;
    private final boolean isPeacefulCreature;
    private final boolean isAnimal;

    EnumCreatureType(int max, boolean peaceful, boolean animal) {
        this.maxNumberOfCreature = max;
        this.isPeacefulCreature = peaceful;
        this.isAnimal = animal;
    }

    public int getMaxNumberOfCreature() {
        return this.maxNumberOfCreature;
    }

    public boolean getPeacefulCreature() {
        return this.isPeacefulCreature;
    }

    public boolean getAnimal() {
        return this.isAnimal;
    }

    public Class getCreatureClass() { return null; }
    public Material getCreatureMaterial() { return null; }
}

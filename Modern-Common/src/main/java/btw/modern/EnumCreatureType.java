package btw.modern;

public enum EnumCreatureType {

    monster(EntityLiving.class, 90, Material.air, false, false),
    creature(EntityLiving.class, 10, Material.air, true, true),
    ambient(EntityLiving.class, 15, Material.air, true, false),
    waterCreature(EntityLiving.class, 5, Material.water, true, false);

    private final Class creatureClass;
    private final int maxNumberOfCreature;
    private final Material creatureMaterial;
    private final boolean isPeacefulCreature;
    private final boolean isAnimal;

    EnumCreatureType(Class creatureClass, int max, Material material, boolean peaceful, boolean animal) {
        this.creatureClass = creatureClass;
        this.maxNumberOfCreature = max;
        this.creatureMaterial = material;
        this.isPeacefulCreature = peaceful;
        this.isAnimal = animal;
    }

    public Class getCreatureClass() {
        return this.creatureClass;
    }

    public int getMaxNumberOfCreature() {
        return this.maxNumberOfCreature;
    }

    public Material getCreatureMaterial() {
        return this.creatureMaterial;
    }

    public boolean getPeacefulCreature() {
        return this.isPeacefulCreature;
    }

    public boolean getAnimal() {
        return this.isAnimal;
    }
}

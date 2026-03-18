package btw.api;

public class EntityVillager extends EntityAgeable {

    public Village villageObj;
    public MerchantRecipeList buyingList;
    public int m_iTradeLevel;

    public EntityVillager(World world) {
        super(world);
    }

    public EntityVillager(World world, int profession) {
        super(world);
    }

    public int getMaxHealth() {
        return 20;
    }

    public int getProfession() {
        return 0;
    }

    public void setProfession(int profession) {}

    public void entityInit() {}
}

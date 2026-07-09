package btw.modern;

public class EntityPotion extends EntityThrowable {

    // 1.5.2 stores the thrown potion as an ItemStack whose damage value
    // encodes the potion type; EntityWitch.attackEntityWithRangedAttack
    // calls setPotionDamage(32698/32660/32696) right after construction.
    private ItemStack potionDamage;

    public EntityPotion(World world) { super(world); }

    public EntityPotion(World world, EntityLiving thrower, int damage) {
        super(world);
        this.setPotionDamage(damage);
    }

    public EntityPotion(World world, double x, double y, double z, int damage) {
        super(world);
        this.setPotionDamage(damage);
    }

    public EntityPotion(World world, double x, double y, double z, ItemStack stack) {
        super(world);
        this.potionDamage = stack;
    }

    // 1.5.2 EntityPotion.setPotionDamage
    public void setPotionDamage(int damage) {
        if (this.potionDamage == null) {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }
        this.potionDamage.setItemDamage(damage);
    }

    // 1.5.2 EntityPotion.getPotionDamage
    public int getPotionDamage() {
        if (this.potionDamage == null) {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }
        return this.potionDamage.getItemDamage();
    }

    public void entityInit() {}

    // 1.5.2 EntityPotion.onImpact (vanilla/server EntityPotion.java:92) — splash the
    // potion's effects onto EntityLivings in a 4x2x4 box, scaled by distance (full on
    // the directly-hit entity); instant effects apply immediately, others as timed
    // potion effects (dropped below the 20-tick floor). Live via FCEntityWitch throws.
    protected void onImpact(MovingObjectPosition result) {
        if (!this.worldObj.isRemote) {
            java.util.List effects = Item.potion.getEffects(this.potionDamage);

            if (effects != null && !effects.isEmpty()) {
                AxisAlignedBB box = this.boundingBox.expand(4.0D, 2.0D, 4.0D);
                java.util.List nearby = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, box);

                if (nearby != null && !nearby.isEmpty()) {
                    java.util.Iterator it = nearby.iterator();

                    while (it.hasNext()) {
                        EntityLiving target = (EntityLiving) it.next();
                        double distSq = this.getDistanceSqToEntity(target);

                        if (distSq < 16.0D) {
                            double effectiveness = 1.0D - Math.sqrt(distSq) / 4.0D;

                            if (target == result.entityHit) {
                                effectiveness = 1.0D;
                            }

                            java.util.Iterator effIt = effects.iterator();

                            while (effIt.hasNext()) {
                                PotionEffect effect = (PotionEffect) effIt.next();
                                int id = effect.getPotionID();

                                if (Potion.potionTypes[id].isInstant()) {
                                    Potion.potionTypes[id].affectEntity(this.getThrower(), target, effect.getAmplifier(), effectiveness);
                                } else {
                                    int duration = (int) (effectiveness * (double) effect.getDuration() + 0.5D);

                                    if (duration > 20) {
                                        target.addPotionEffect(new PotionEffect(id, duration, effect.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.worldObj.playAuxSFX(2002, (int) Math.round(this.posX), (int) Math.round(this.posY), (int) Math.round(this.posZ), this.getPotionDamage());
            this.setDead();
        }
    }
}

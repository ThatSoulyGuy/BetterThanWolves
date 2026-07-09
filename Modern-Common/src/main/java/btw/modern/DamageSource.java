package btw.modern;

public class DamageSource {

    public static DamageSource inFire = new DamageSource("inFire").setFireDamage();
    public static DamageSource onFire = new DamageSource("onFire").setDamageBypassesArmor().setFireDamage();
    public static DamageSource lava = new DamageSource("lava").setFireDamage();
    public static DamageSource inWall = new DamageSource("inWall").setDamageBypassesArmor();
    public static DamageSource drown = new DamageSource("drown").setDamageBypassesArmor();
    public static DamageSource starve = new DamageSource("starve").setDamageBypassesArmor();
    public static DamageSource cactus = new DamageSource("cactus");
    public static DamageSource fall = new DamageSource("fall").setDamageBypassesArmor();
    public static DamageSource outOfWorld = new DamageSource("outOfWorld").setDamageBypassesArmor();
    public static DamageSource generic = new DamageSource("generic").setDamageBypassesArmor();
    public static DamageSource magic = new DamageSource("magic").setDamageBypassesArmor();
    public static DamageSource wither = new DamageSource("wither").setDamageBypassesArmor();
    public static DamageSource anvil = new DamageSource("anvil");
    public static DamageSource fallingBlock = new DamageSource("fallingBlock");

    public String damageType;
    private boolean isUnblockable;
    private boolean fireDamage;
    private boolean projectile;
    private boolean difficultyScaled;
    private boolean magicDamage;
    private boolean explosion;

    public DamageSource(String type) {
        this.damageType = type;
    }

    // 1.5.2 DamageSource.causeArrowDamage — called by EntityArrow.onUpdate's
    // hit path (FC broadhead/rotten/infinite arrows and FC skeleton shots).
    public static DamageSource causeArrowDamage(EntityArrow arrow, Entity shooter) {
        return new EntityDamageSourceIndirect("arrow", arrow, shooter).setProjectile();
    }

    // 1.5.2 DamageSource.causeThornsDamage — referenced by the now-included
    // EnchantmentThorns.func_92096_a retaliation branch.
    public static DamageSource causeThornsDamage(Entity entity) {
        return new EntityDamageSource("thorns", entity).setMagicDamage();
    }

    // 1.5.2 DamageSource.causeFireballDamage — called by EntitySmallFireball /
    // EntityLargeFireball.onImpact (FC blaze and ghast shots).
    public static DamageSource causeFireballDamage(EntityFireball fireball, Entity shooter) {
        return shooter == null
            ? new EntityDamageSourceIndirect("onFire", fireball, fireball).setFireDamage().setProjectile()
            : new EntityDamageSourceIndirect("fireball", fireball, shooter).setFireDamage().setProjectile();
    }

    public DamageSource setDamageBypassesArmor() {
        this.isUnblockable = true;
        return this;
    }

    public DamageSource setFireDamage() {
        this.fireDamage = true;
        return this;
    }

    public DamageSource setProjectile() {
        this.projectile = true;
        return this;
    }

    public DamageSource setDifficultyScaled() {
        this.difficultyScaled = true;
        return this;
    }

    public DamageSource setMagicDamage() {
        this.magicDamage = true;
        return this;
    }

    public DamageSource setExplosion() {
        this.explosion = true;
        return this;
    }

    public Entity getEntity() {
        return null;
    }

    public Entity getSourceOfDamage() {
        return getEntity();
    }

    public boolean isUnblockable() {
        return this.isUnblockable;
    }

    public boolean isFireDamage() {
        return this.fireDamage;
    }

    public boolean isProjectile() {
        return this.projectile;
    }

    public boolean isDifficultyScaled() {
        return this.difficultyScaled;
    }

    public boolean isMagicDamage() {
        return this.magicDamage;
    }

    public boolean isExplosion() {
        return this.explosion;
    }

    public boolean canHarmInCreative() {
        return false;
    }

    public String getDamageType() {
        return this.damageType;
    }

    // --- Factory methods ---

    public static DamageSource causeMobDamage(EntityLiving mob) {
        return new EntityDamageSource("mob", mob);
    }

    public static DamageSource causePlayerDamage(EntityPlayer player) {
        return new EntityDamageSource("player", player);
    }

    public static DamageSource causeThrownDamage(Entity projectile, Entity thrower) {
        return new EntityDamageSourceIndirect("thrown", projectile, thrower);
    }

    public static DamageSource causeIndirectMagicDamage(Entity source, Entity indirectSource) {
        return new EntityDamageSourceIndirect("indirectMagic", source, indirectSource).setDamageBypassesArmor().setMagicDamage();
    }

    // 1.5.2 DamageSource.setExplosionSource — FCExplosionMining.DamageEntities
    // (mining charges) calls this; the exploder branch attributes the kill and
    // both branches are difficulty-scaled.
    public static DamageSource setExplosionSource(Explosion explosion) {
        return explosion != null && explosion.func_94613_c() != null
            ? new EntityDamageSource("explosion.player", explosion.func_94613_c()).setDifficultyScaled().setExplosion()
            : new DamageSource("explosion").setDifficultyScaled().setExplosion();
    }

    public String getDeathMessage(EntityLiving entity) {
        return "";
    }
}

package btw.modern;

public interface IEntitySelector {

    // 1.5.2 declares these two selector constants on the interface
    // (EntitySelectorAlive / EntitySelectorInventory); the frozen
    // EntityMinecartHopper references selectAnything when sucking items.
    IEntitySelector selectAnything = new IEntitySelector() {
        public boolean isEntityApplicable(Entity entity) {
            return entity.isEntityAlive();
        }
    };

    IEntitySelector selectInventories = new IEntitySelector() {
        public boolean isEntityApplicable(Entity entity) {
            return entity instanceof IInventory && entity.isEntityAlive();
        }
    };

    boolean isEntityApplicable(Entity entity);
}

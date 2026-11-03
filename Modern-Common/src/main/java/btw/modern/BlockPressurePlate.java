package btw.modern;

import java.util.List;

public class BlockPressurePlate extends BlockBasePressurePlate {

    /**
     * 1.5.2 BlockPressurePlate.triggerMobType (vanilla/client BlockPressurePlate.java:22): what presses the plate. The
     * two-argument constructor has no 1.5.2 counterpart; it keeps 1.5.2's widest rule.
     */
    private final EnumMobType triggerMobType;

    protected BlockPressurePlate(int id, Material material) {
        super(id, material);
        this.triggerMobType = EnumMobType.everything;
    }

    // 1.5.2 BlockPressurePlate (vanilla/client BlockPressurePlate.java:24-29) passes the icon name up (legacy_icons,
    // BlockBasePressurePlate) and keeps the mob type. FCMOD-INTEGRATION(1.20.1): fc_redstone: with the mob type the
    // soulforged plate 230 answers players only, the stone plate 70 living entities and the wooden plate 72 everything
    // (Src FCBetterThanWolves.java:1032-1034)
    protected BlockPressurePlate(int id, String iconName, Material material, EnumMobType mobType) {
        super(id, iconName, material);
        this.triggerMobType = mobType != null ? mobType : EnumMobType.everything;
    }

    // 1.5.2 BlockPressurePlate.getMetaFromWeight (vanilla/client BlockPressurePlate.java:33-36)
    @Override
    public int getMetaFromWeight(int weight) {
        return weight > 0 ? 1 : 0;
    }

    // 1.5.2 BlockPressurePlate.getPowerSupply (vanilla/client BlockPressurePlate.java:41-44)
    @Override
    public int getPowerSupply(int metadata) {
        return metadata == 1 ? 15 : 0;
    }

    // 1.5.2 BlockPressurePlate.getPlateState (vanilla/client BlockPressurePlate.java:50-86): 15 when an entity of the
    // plate's mob type that triggers plates is in its sensitive box
    @Override
    public int getPlateState(World world, int x, int y, int z) {
        List list = null;

        if (this.triggerMobType == EnumMobType.everything) {
            list = world.getEntitiesWithinAABBExcludingEntity((Entity) null, this.getSensitiveAABB(x, y, z));
        }

        if (this.triggerMobType == EnumMobType.mobs) {
            list = world.getEntitiesWithinAABB(EntityLiving.class, this.getSensitiveAABB(x, y, z));
        }

        if (this.triggerMobType == EnumMobType.players) {
            list = world.getEntitiesWithinAABB(EntityPlayer.class, this.getSensitiveAABB(x, y, z));
        }

        if (list != null && !list.isEmpty()) {
            for (Object o : list) {
                Entity entity = (Entity) o;
                if (!entity.doesEntityNotTriggerPressurePlate()) {
                    return 15;
                }
            }
        }

        return 0;
    }
}

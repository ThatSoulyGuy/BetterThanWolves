package btw.modern;

public class BlockBed extends BlockDirectional {

    protected BlockBed(int id) {
        super(id, Material.cloth);
    }

    // 1.5.2 BlockBed.isBlockHeadOfBed (vanilla BlockBed.java:212) — EntityAIOcelotSit
    // checks this when a (live, replaced) FC ocelot looks for a bed head to sit on.
    public static boolean isBlockHeadOfBed(int metadata) {
        return (metadata & 8) != 0;
    }
}

package btw.api;

public class EntityPainting extends Entity {



    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int hangingDirection;
    public EnumArt art;
    public EntityPainting(World world) { super(world); }
    public EntityPainting(World world, int x, int y, int z, int dir) { super(world); }


    public void entityInit() {}
}

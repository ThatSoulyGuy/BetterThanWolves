package btw.modern;
public class RenderVillager extends RenderLiving {
    public RenderVillager() { super(new ModelVillager(0.0F), 0.5F); }
    public RenderVillager(ModelBase model, float shadow) { super(model, shadow); }
}

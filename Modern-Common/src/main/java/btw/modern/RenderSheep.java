package btw.modern;
public class RenderSheep extends RenderLiving {
    protected ModelBase renderPassModel2;
    public RenderSheep(ModelBase model, ModelBase model2, float shadow) { super(model, shadow); this.renderPassModel2 = model2; }
    public RenderSheep(ModelBase model, float shadow) { super(model, shadow); }
    public RenderSheep() { super(null, 0); }
}

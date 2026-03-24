package btw.modern;
public class RenderPig extends RenderLiving {
    protected ModelBase renderPassModel2;
    public RenderPig(ModelBase model, ModelBase model2, float shadow) { super(model, shadow); this.renderPassModel2 = model2; }
    public RenderPig(ModelBase model, float shadow) { super(model, shadow); }
    public RenderPig() { super(null, 0); }
}

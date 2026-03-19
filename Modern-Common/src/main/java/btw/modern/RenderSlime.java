package btw.modern;

public class RenderSlime extends RenderLiving {

    private ModelBase scaleAmount;

    public RenderSlime(ModelBase model, ModelBase scaleModel, float shadowSize) {
        super(model, shadowSize);
        this.scaleAmount = scaleModel;
    }
}

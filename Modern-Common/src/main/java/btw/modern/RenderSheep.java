package btw.modern;

public class RenderSheep extends RenderLiving {

    public RenderSheep(ModelBase model, ModelBase renderPassModel, float shadowSize) {
        super(model, shadowSize);
        this.setRenderPassModel(renderPassModel);
    }
}

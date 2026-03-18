package btw.api;

import java.util.ArrayList;
import java.util.List;

public class ModelRenderer {

    /** The size of the texture file's width in pixels. */
    public float textureWidth;

    /** The size of the texture file's height in pixels. */
    public float textureHeight;

    private int textureOffsetX;
    private int textureOffsetY;

    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;

    public boolean mirror;
    public boolean showModel;
    public boolean isHidden;
    public List cubeList;
    public List childModels;
    public final String boxName;
    private ModelBase baseModel;

    public float field_82906_o;
    public float field_82908_p;
    public float field_82907_q;

    public ModelRenderer(ModelBase model, String name) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.mirror = false;
        this.showModel = true;
        this.isHidden = false;
        this.cubeList = new ArrayList();
        this.baseModel = model;
        model.boxList.add(this);
        this.boxName = name;
        this.setTextureSize(model.textureWidth, model.textureHeight);
    }

    public ModelRenderer(ModelBase model) {
        this(model, (String) null);
    }

    public ModelRenderer(ModelBase model, int texOffX, int texOffY) {
        this(model);
        this.setTextureOffset(texOffX, texOffY);
    }

    public void addChild(ModelRenderer child) {
        if (this.childModels == null) {
            this.childModels = new ArrayList();
        }
        this.childModels.add(child);
    }

    public ModelRenderer setTextureOffset(int x, int y) {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
        return this;
    }

    public ModelRenderer addBox(String name, float x, float y, float z, int width, int height, int depth) {
        return this;
    }

    public ModelRenderer addBox(float x, float y, float z, int width, int height, int depth) {
        return this;
    }

    public void addBox(float x, float y, float z, int width, int height, int depth, float scaleFactor) {}

    public void setRotationPoint(float x, float y, float z) {
        this.rotationPointX = x;
        this.rotationPointY = y;
        this.rotationPointZ = z;
    }

    public void render(float scale) {}

    public void renderWithRotation(float scale) {}

    public void postRender(float scale) {}

    public ModelRenderer setTextureSize(int width, int height) {
        this.textureWidth = (float) width;
        this.textureHeight = (float) height;
        return this;
    }

    public void RenderWithScaleToBaseModel(float scale, float scaleX, float scaleY, float scaleZ) {}
}

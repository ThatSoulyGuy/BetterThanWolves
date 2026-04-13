package btw.modern;

import java.util.ArrayList;
import java.util.List;

public class ModelRenderer {

    public float textureWidth;
    public float textureHeight;
    public int textureOffsetX;
    public int textureOffsetY;
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
    public float field_82906_o;
    public float field_82908_p;
    public float field_82907_q;
    private ModelBase baseModel;

    public ModelRenderer(ModelBase model, String name) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.mirror = false;
        this.showModel = true;
        this.cubeList = new ArrayList();
        this.boxName = name;
        this.baseModel = model;
        if (model != null) {
            this.setTextureSize((int) model.textureWidth, (int) model.textureHeight);
            model.boxList.add(this);
        }
    }

    public ModelRenderer(ModelBase model) {
        this(model, null);
    }

    public ModelRenderer(ModelBase model, int texX, int texY) {
        this(model);
        this.setTextureOffset(texX, texY);
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

    /**
     * Creates a real ModelBox with correct UV mapping from vanilla 1.5.2's
     * ModelBox/TexturedQuad/PositionTextureVertex pipeline.
     */
    public ModelRenderer addBox(String name, float x, float y, float z, int width, int height, int depth) {
        this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY,
                x, y, z, width, height, depth, 0.0F));
        return this;
    }

    public ModelRenderer addBox(float x, float y, float z, int width, int height, int depth) {
        this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY,
                x, y, z, width, height, depth, 0.0F));
        return this;
    }

    public void addBox(float x, float y, float z, int width, int height, int depth, float scaleFactor) {
        this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY,
                x, y, z, width, height, depth, scaleFactor));
    }

    public void setRotationPoint(float x, float y, float z) {
        this.rotationPointX = x;
        this.rotationPointY = y;
        this.rotationPointZ = z;
    }

    /**
     * Renders all ModelBoxes through GL11 transforms and Tessellator quads.
     * Uses the real vanilla 1.5.2 ModelBox → TexturedQuad rendering pipeline
     * for correct UV mapping. Skips GL display lists (our GL11 stub doesn't
     * support them) and renders directly each frame instead.
     */
    public void render(float scale) {
        if (isHidden || !showModel) return;

        GL11.glPushMatrix();
        GL11.glTranslatef(field_82906_o, field_82908_p, field_82907_q);

        if (rotateAngleX == 0.0F && rotateAngleY == 0.0F && rotateAngleZ == 0.0F) {
            if (rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F) {
                GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
            }
            renderBoxes(scale);
            if (childModels != null) {
                for (int i = 0; i < childModels.size(); i++) {
                    ((ModelRenderer) childModels.get(i)).render(scale);
                }
            }
        } else {
            GL11.glPushMatrix();
            GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
            if (rotateAngleZ != 0.0F) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
            if (rotateAngleY != 0.0F) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
            if (rotateAngleX != 0.0F) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
            renderBoxes(scale);
            if (childModels != null) {
                for (int i = 0; i < childModels.size(); i++) {
                    ((ModelRenderer) childModels.get(i)).render(scale);
                }
            }
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }

    private void renderBoxes(float scale) {
        for (int i = 0; i < cubeList.size(); i++) {
            ((ModelBox) cubeList.get(i)).render(Tessellator.instance, scale);
        }
    }

    public void renderWithRotation(float scale) {
        if (isHidden || !showModel) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleY != 0.0F) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        if (rotateAngleX != 0.0F) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
        if (rotateAngleZ != 0.0F) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        renderBoxes(scale);
        GL11.glPopMatrix();
    }

    public void postRender(float scale) {
        if (rotateAngleX == 0.0F && rotateAngleY == 0.0F && rotateAngleZ == 0.0F) {
            if (rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F) {
                GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
            }
        } else {
            GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
            if (rotateAngleZ != 0.0F) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
            if (rotateAngleY != 0.0F) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
            if (rotateAngleX != 0.0F) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
        }
    }

    public ModelRenderer setTextureSize(int width, int height) {
        this.textureWidth = (float) width;
        this.textureHeight = (float) height;
        return this;
    }

    public void RenderWithScaleToBaseModel(float scale, float scaleX, float scaleY, float scaleZ) {
        if (isHidden || !showModel) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleZ != 0.0F) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        if (rotateAngleY != 0.0F) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        if (rotateAngleX != 0.0F) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
        GL11.glScalef(scaleX, scaleY, scaleZ);
        renderBoxes(scale);
        if (childModels != null) {
            for (Object child : childModels) {
                ((ModelRenderer) child).render(scale);
            }
        }
        GL11.glPopMatrix();
    }
}

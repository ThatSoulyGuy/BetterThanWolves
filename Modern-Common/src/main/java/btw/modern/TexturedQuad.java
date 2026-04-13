package btw.modern;

/**
 * Vanilla 1.5.2 TexturedQuad — draws a quad with UV-mapped vertices
 * through the Tessellator, computing the face normal from vertex positions.
 */
public class TexturedQuad {
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;
    private boolean invertNormal;

    public TexturedQuad(PositionTextureVertex[] verts) {
        this.nVertices = 0;
        this.invertNormal = false;
        this.vertexPositions = verts;
        this.nVertices = verts.length;
    }

    public TexturedQuad(PositionTextureVertex[] verts, int u1, int v1, int u2, int v2, float tw, float th) {
        this(verts);
        float uPad = 0.0F / tw;
        float vPad = 0.0F / th;
        verts[0] = verts[0].setTexturePosition((float) u2 / tw - uPad, (float) v1 / th + vPad);
        verts[1] = verts[1].setTexturePosition((float) u1 / tw + uPad, (float) v1 / th + vPad);
        verts[2] = verts[2].setTexturePosition((float) u1 / tw + uPad, (float) v2 / th - vPad);
        verts[3] = verts[3].setTexturePosition((float) u2 / tw - uPad, (float) v2 / th - vPad);
    }

    public void flipFace() {
        PositionTextureVertex[] flipped = new PositionTextureVertex[this.vertexPositions.length];
        for (int i = 0; i < this.vertexPositions.length; i++) {
            flipped[i] = this.vertexPositions[this.vertexPositions.length - i - 1];
        }
        this.vertexPositions = flipped;
    }

    public void draw(Tessellator tess, float scale) {
        Vec3 v0 = this.vertexPositions[1].vector3D.subtract(this.vertexPositions[0].vector3D);
        Vec3 v1 = this.vertexPositions[1].vector3D.subtract(this.vertexPositions[2].vector3D);
        Vec3 normal = v1.crossProduct(v0).normalize();

        tess.startDrawingQuads();
        if (this.invertNormal) {
            tess.setNormal(-((float) normal.xCoord), -((float) normal.yCoord), -((float) normal.zCoord));
        } else {
            tess.setNormal((float) normal.xCoord, (float) normal.yCoord, (float) normal.zCoord);
        }
        for (int i = 0; i < 4; i++) {
            PositionTextureVertex v = this.vertexPositions[i];
            tess.addVertexWithUV(
                    (double) ((float) v.vector3D.xCoord * scale),
                    (double) ((float) v.vector3D.yCoord * scale),
                    (double) ((float) v.vector3D.zCoord * scale),
                    (double) v.texturePositionX,
                    (double) v.texturePositionY);
        }
        tess.draw();
    }
}

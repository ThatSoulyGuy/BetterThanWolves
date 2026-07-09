import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Regenerates the stoked-fire (Hibachi) animated flame textures.
 *
 * FC's stoked fire uses a procedural animated texture (FCClientTextureFireStoked) that the
 * bridge can't run; only two static 16x16 frames ship (fcblockfirestokedstub_0/1.png), so in
 * the bridge the stoked-fire flame rendered as a dead, static flame. This stitches the two
 * frames into a 2-frame 16x32 vertical strip (each variant animating into the other) and
 * writes the accompanying .mcmeta, so MC 1.20.1's block atlas animates them — giving the
 * flickering "stoked" flame. FCBakedModel already resolves these textures (atlas directory
 * source) and captures the block's custom flame geometry, so no code change is needed.
 *
 * Idempotent: skips if the textures are already animated (height != 16). Run:
 *   javac -d out tools/GenStokedFireAnim.java && java -cp out GenStokedFireAnim
 */
public class GenStokedFireAnim {
    static final String DIR = "D:/IntelliJ/IDEA/Projects/BetterThanWolves/Forge/src/main/resources/"
            + "assets/betterthanwolves/textures/block";
    static final String MCMETA = "{\n  \"animation\": {\n    \"frametime\": 2,\n    \"interpolate\": false\n  }\n}\n";

    public static void main(String[] args) throws IOException {
        BufferedImage f0 = ImageIO.read(new File(DIR, "fcblockfirestokedstub_0.png"));
        BufferedImage f1 = ImageIO.read(new File(DIR, "fcblockfirestokedstub_1.png"));
        if (f0 == null || f1 == null) {
            System.out.println("Missing stoked-fire frame(s); nothing to do.");
            return;
        }
        if (f0.getHeight() != 16 || f1.getHeight() != 16) {
            System.out.println("Stoked-fire textures already animated (height="
                    + f0.getHeight() + "); skipping.");
            return;
        }
        int w = f0.getWidth();

        // Each variant animates from itself into the other frame (2-frame flicker), keeping the
        // original as frame 0 so the checkerboard variety RenderBlock draws still reads.
        writeStrip(new File(DIR, "fcblockfirestokedstub_0.png"), f0, f1, w);
        writeStrip(new File(DIR, "fcblockfirestokedstub_1.png"), f1, f0, w);
        Files.write(Paths.get(DIR, "fcblockfirestokedstub_0.png.mcmeta"), MCMETA.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(DIR, "fcblockfirestokedstub_1.png.mcmeta"), MCMETA.getBytes(StandardCharsets.UTF_8));

        // Verify.
        BufferedImage v = ImageIO.read(new File(DIR, "fcblockfirestokedstub_0.png"));
        System.out.println("Wrote 2-frame stoked-fire strips (" + w + "x" + v.getHeight() + ") + mcmeta.");
    }

    static void writeStrip(File out, BufferedImage top, BufferedImage bottom, int w) throws IOException {
        BufferedImage strip = new BufferedImage(w, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = strip.createGraphics();
        g.drawImage(top, 0, 0, null);
        g.drawImage(bottom, 0, 16, null);
        g.dispose();
        ImageIO.write(strip, "PNG", out);
    }
}

package io.github.mrmiumo.mi3engine;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Image or portion of an image used to be paint over model faces.
 * @param name the name of this texture, only used for debug
 * @param source the image to use for this texture
 * @param pixels internal variable used for read source pixels faster
 * @param x the x coordinate of the top-left corner of the uv box
 * @param y the y coordinate of the top-left corner of the uv box
 * @param w the width of the uv box
 * @param h the height of the uv box
 * @param rotate the rotation of the texture (1 = 90° CW and so on)
 * @param isTransparent whether this texture has transparency.
 *     DO NOT SET THIS VALUE BY YOURSELF
 */
public class Texture {

    /** Iterator over each default texture colorSet available */
    private static int nextColorSet = 0;

    public final String name;
    private final BufferedImage source;
    private final int sourceW;
    private final int sourceH;
    private final int[] pixels;
    private final int x;
    private final int y;
    private final float[] rotate;
    private final boolean isTransparent;

    private Texture(
        String name, BufferedImage source, int[] pixels, float x, float y,
        float w, float h, int rotate, boolean isTransparent
    ) {
        this.name = name;
        this.source = source;
        this.sourceW = source.getWidth();
        this.sourceH = source.getHeight();
        this.pixels = pixels;
        this.x = (int)(w < 0 ? x - 1 : x);
        this.y = (int)(h < 0 ? y - 1 : y);
        this.rotate = switch (rotate) {
            // u = [0]u + [1]v + [2]; v = [3]u + [4]v + [5]
            case 0 ->  new float[]{ 1*w,    0,      this.x,    0, -1*h,  1*h+this.y};
            case 1 ->  new float[]{   0, -1*w,  1*w+this.x, -1*h,    0,  1*h+this.y};
            case 2 ->  new float[]{-1*w,    0,  1*w+this.x,    0,  1*h,      this.y};
            default -> new float[]{   0,  1*w,      this.x,  1*h,    0,      this.y};
        };
        this.isTransparent = isTransparent;
    }

    /**
     * Creates a new texture from the given image file. The uv box uses
     * the full image and no rotation is applied. The texture name is
     * automatically taken from the image file name.<p>
     * This function is the only one that supports loading animated
     * textures (to avoid weird texture rendering).
     * @param path the image to load as texture
     * @return the corresponding texture
     * @throws IOException in case of error while reading the file
     */
    public static Texture from(Path path) throws IOException {
        var name = path.getFileName().toString();
        var img = ImageIO.read(Files.newInputStream(path));
        var w = img.getWidth();
        var h = img.getHeight();
        if (Files.exists(Path.of(path.toString() + ".mcmeta"))) {
            /* Animated texture */
            h = w;
            var crop = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            var g = crop.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = crop;
        }
        var pixels = getPixels(img);
        if (isEmpty(pixels, w, 0, 0, w, h)) {
            return null;
        }
        return new Texture(name, img, pixels, 0, 0, w, h, 0, isTransparent(img, pixels, 0, 0, w, h));
    }

    /**
     * Creates a new texture from the given image data. The uv box uses
     * the full image and no rotation is applied. The texture name can
     * be null since only used for debug.<p>
     * WARNING: This function does NOT support animated textures
     * @param img the image to load as texture
     * @return the corresponding texture
     * @see #from(Path)
     */
    public static Texture from(BufferedImage img) {
        var w = img.getWidth();
        var h = img.getHeight();
        var pixels = getPixels(img);
        if (isEmpty(pixels, w, 0, 0, w, h)) {
            return null;
        }
        return new Texture(null, img, pixels, 0, 0, w, h, 0, isTransparent(img, pixels, 0, 0, w, h));
    }

    /**
     * Creates a new texture from this one by changing the UV and
     * rotation but keeping the same image.<p>
     * This enables to not recalculate the transparency of the texture.
     * @param x the x coordinate of the top-left corner of the uv box
     * @param y the y coordinate of the top-left corner of the uv box
     * @param w the width of the uv box
     * @param h the height of the uv box
     * @param rotate the rotation of the texture (1 = 90° and so on)
     * @return the new texture with the given UV box and rotation
     */
    public Texture uv(float x, float y, float w, float h, int rotate) {
        if (w < 0) {
            w = -w;
            x -= w;
        }
        if (h < 0) {
            h = -h;
            y -= h;
        }
        /* Adapt uv from 16x16 to the real size */
        if (sourceW != 16 || sourceH != 16) {
            x = x * sourceW / 16f;
            y = y * sourceH / 16f;
            w = w * sourceW / 16f;
            h = h * sourceH / 16f;
        }

        /* Make sure the face has pixels */
        if (isEmpty(pixels, sourceW, x, y, w, h)) {
            return null; // No need for texture!
        }

        var transparency = isTransparent && isTransparent(source, pixels, x, y, w, h);
        return new Texture(name, source, pixels, x, y, w, h, rotate, transparency);
    }

    /**
     * Gets the pixels of the source texture.
     * @return the pixels of the texture
     */
    public int[] pixels() {
        return pixels;
    }

    /**
     * Gets the original image used for this texture (before UVs)
     * @return the source image
     */
    public BufferedImage source() {
        return source;
    }

    /**
     * Gets the width in pixels of the source image.
     * @return the width in pixels of the source image
     */
    public int sourceWidth() {
        return sourceW;
    }

    /**
     * Gets the height in pixels of the source image.
     * @return the height in pixels of the source image
     */
    public int sourceHeight() {
        return sourceH;
    }

    /**
     * Gets whether this texture contains transparent pixels or not
     * @return true if the texture contains transparent pixels
     */
    public boolean isTransparent() {
        return isTransparent;
    }

    /**
     * Gets the texel [XY] that enable to precompute color pixel
     * from UV to texture.
     * Those values take into account the position of the pixel in the
     * texture, texture rotation, and uv.
     * <p>
     * Both u and v are normalized, so their values are between 0 and 1,
     * where 1 is the full width/height of the texture (the pixel at
     * the very top/right).
     * </p>
     * @param u the position of the pixel to get on the horizontal axis
     * @param v the position of the pixel to get on the vertical axis
     * @return the ARGB code of the pixel
     */
    public int[] getRGBTexel(double u, double v) {
        var tx = (int)(rotate[0]*u + rotate[1]*v + rotate[2]);
        var ty = (int)(rotate[3]*u + rotate[4]*v + rotate[5]);
        if (tx < 0) tx = 0; else if (tx >= sourceW) tx = sourceW - 1;
        if (ty < 0) ty = 0; else if (ty >= sourceH) ty = sourceH - 1;
        return new int[] { tx, ty };
    }

    /**
     * Generates a random colored default texture
     * @return the default texture
     */
    public static Texture generateDefault() {
        var texture = DefaultTexture.values()[nextColorSet++].texture();
        nextColorSet %= DefaultTexture.values().length;
        return texture;
    }

    /**
     * Tests if the given image data and UV box contains at least one
     * not transparent pixel.
     * @param pixels the pixels of the image
     * @param srcW the initial width of the full image
     * @param x the x coordinate of the top left corner of the UV box
     * @param y the y coordinate of the top left corner of the UV box
     * @param w the width of the UV box
     * @param h the height of the UV box
     * @return true if no visible pixel exists in the UV box, false otherwise
     */
    private static boolean isEmpty(int[] pixels, int srcW, float x, float y, float w, float h) {
        final int maxScan = 2000;

        if (w == 0 || h == 0) return false;

        /* Handles negative sizes */
        if (w < 0) { w *= -1; x -= w; }
        if (h < 0) { h *= -1; y -= h; }
        var uvX = (int)Math.floor(x);
        var uvY = (int)Math.floor(y);
        var uvW = (int)Math.ceil(w);
        var uvH = (int)Math.ceil(h);

        /* Scan the image */
        if (uvW * uvH <= maxScan) {
            /* Full Scan */
            for (int j = uvY ; j < uvY + uvH ; j++) {
                for (int i = uvX ; i < uvX + uvW ; i++) {
                    if ((pixels[j * srcW + i] >>> 24) > 0) {
                        return false;
                    }
                }
            }
        } else {
            /* Random scan */
            Random rnd = new Random(12345);
            for (int i = 0 ; i < maxScan ; i++) {
                int rx = uvX + rnd.nextInt(uvW);
                int ry = uvY + rnd.nextInt(uvH);
                if ((pixels[ry * srcW + rx] >>> 24) > 0) return false;
            }
        }
        return true;
    }

    /**
     * Cheap on-time scan to detect quickly if the given image have
     * any pixel with alpha < 255.
     * @param source the image to analyze
     * @param pixels the pixels of the image
     * @param x the position of the top-left corner of the region to analyze
     * @param y the position of the top-left corner of the region to analyze
     * @param w the width of the region to analyze
     * @param h the height of the region to analyze
     * @return true if at least one transparent pixel got detected,
     *     false otherwise
     */
    private static boolean isTransparent(BufferedImage source, int[] pixels, float x, float y, float w, float h) {
        /* No alpha channel = not transparent */
        if (!source.getColorModel().hasAlpha()) {
            return false;
        }

        /* Handles negative sizes */
        if (w < 0) { w *= -1; x -= w; }
        if (h < 0) { h *= -1; y -= h; }
        var uvX = (int)Math.floor(x);
        var uvY = (int)Math.floor(y);
        var uvW = (int)Math.ceil(w);
        var uvH = (int)Math.ceil(h);

        final int maxScan = 2000;
        final int width = source.getWidth();
        if (uvW * uvH <= maxScan) {
            /* Full Scan */
            for (int j = uvY ; j < uvY + uvH ; j++) {
                for (int i = uvX ; i < uvX + uvW ; i++) {
                    if ((pixels[j * width + i] >>> 24) < 255) return true;
                }
            }
        } else {
            /* Random scan */
            Random rnd = new Random(12345);
            for (int i = 0 ; i < maxScan ; i++) {
                int rx = uvX + rnd.nextInt(uvW);
                int ry = uvY + rnd.nextInt(uvH);
                if ((pixels[ry * width + rx] >>> 24) < 255) return true;
            }
        }
        return false;
    }

    /**
     * Gets the pixels of the image as an array. This is sightly
     * faster than calling {@link BufferedImage#getRGB(int, int)} each
     * time.
     * @param image the image to get pixels from
     * @return all the pixels of the image
     */
    private static int[] getPixels(BufferedImage image) {
        var img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    }

    /**
     * Lists of all different default colored textures.
     */
    public enum DefaultTexture {
        GREEN(new int[]{ 0x62cc82, 0x5abb78, 0x58b675, 0x50a66a }),
        PINK( new int[]{ 0xcc84aa, 0xbb799c, 0xb67698, 0xa66c8b }),
        RED(  new int[]{ 0xcc7c79, 0xbb726f, 0xb66f6c, 0xa66562 }),
        BLUE( new int[]{ 0x81bccc, 0x77acbb, 0x74a8b6, 0x6a99a6 }),
        GOLD( new int[]{ 0xccc67a, 0xbbb670, 0xb6b16d, 0xa6a264 });

        private final int[] colors;

        DefaultTexture(int[] colors) {
            this.colors = colors;
        }

        /**
         * Gets the texture with this color. This method computes the
         * texture without any cache. Calling this method twice will
         * cost 2x more than storing its result!
         * @return the texture
         */
        public Texture texture() {
            var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            var canvas = image.getGraphics();

            /* Set background color */
            canvas.setColor(new Color(colors[1]));
            canvas.fillRect(0, 0, 16, 16);

            /** Add check board */
            canvas.setColor(new Color(colors[2]));
            for (var i = 0 ;  i < 15 * 15 ; i += 2) {
                canvas.fillRect(i % 15 + 1, i / 15 + 1, 1, 1);
            }

            /** Add light edges and icon */
            canvas.setColor(new Color(colors[0]));
            canvas.fillRect(0, 0, 1, 15);
            canvas.fillRect(0, 0, 15, 1);
            canvas.fillRect(4, 8, 2, 5);
            canvas.fillRect(7, 10, 2, 3);
            canvas.fillRect(10, 3, 2, 10);

            /** Add dark edges */
            canvas.setColor(new Color(colors[3]));
            canvas.fillRect(15, 1, 1, 15);
            canvas.fillRect(1, 15, 15, 1);

            canvas.dispose();
            return new Texture("default#" + name(), image, getPixels(image), 0, 0, 16, 16, 0, false);
        }
    }
}
package io.github.mrmiumo.mi3engine;

import static io.github.mrmiumo.mi3engine.RenderUtils.clamp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

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
 * @param isTransparent whether or not this texture has transparency.
 *     DO NOT SET THIS VALUE BY YOURSELF
 */
public record Texture(
    String name, BufferedImage source, int[] pixels, float x, float y,
    float w, float h, int rotate, boolean isTransparent
) {

    /** Iterator over each default texture colorSet available */
    private static int nextColorSet = 0;

    /**
     * Creates a new texture from the given image. The uv box uses the
     * full image and no rotation is applied.
     * @param name the name of this texture (for debug only)
     * @param img the image to use as texture
     */
    public static Texture from(String name, BufferedImage img) {
        var w = img.getWidth();
        var h = img.getHeight();
        var pixels = getPixels(img);
        if (isEmpty(pixels, w, 0, 0, w, h)) {
            return null;
        }
        return new Texture(name, img, pixels, 0, 0, w, h, 0, isTransparent(img, pixels, 0, 0, w, h));
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
        /* Adapt uv from 16x16 to the real size */
        if (source.getWidth() != 16 || source.getHeight() != 16) {
            x = x * source.getWidth() / 16f;
            y = y * source.getHeight() / 16f;
            w = w * source.getWidth() / 16f;
            h = h * source.getHeight() / 16f;
        }

        var uvX = (int)x;
        var uvY = (int)y;
        var uvW = (int)w;
        var uvH = (int)h;

        /* Make sure the face have pixels */
        if (isEmpty(pixels, source.getWidth(), uvX, uvY, uvW, uvH)) {
            return null; // No need for texture!
        }

        var transparency = isTransparent ? isTransparent(source, pixels, uvX, uvY, uvW, uvH) : false;
        return new Texture(name, source, pixels, x, y, w, h, rotate, transparency);
    }

    /**
     * Gets the color of the pixel matching the given position in the
     * texture, taking care of texture rotation, and uv.
     * <p>
     * Both u and v are normalized, so their values are between 0 and 1,
     * where 1 is the full width/height of the texture (the pixel at
     * the very top/right).
     * </p>
     * @param u the position of the pixel to get on the horizontal axis
     * @param v the position of the pixel to get on the vertical axis
     * @return the ARGB code of the pixel
     */
    public int getRGB(double u, double v) {
        u = clamp(u, 0, 1);
        v = 1 - clamp(v, 0, 1);
        if (rotate == 1) {
            var t = u;
            u = v;
            v = 1 - t;
        } else if (rotate == 2) {
            u = 1 - u;
            v = 1 - v;
        } else if (rotate == 3) {
            var t = u;
            u = 1 - v;
            v = t;
        }
        var tx = (int)clamp(x + (int)(u * w), 0, source.getWidth() - 1);
        var ty = (int)clamp(y + (int)(v * h), 0, source.getHeight() - 1);
        return pixels[ty * source.getWidth() + tx];
    }

    /**
     * Generates a random colored default texture
     * @return the default texture
     */
    public static Texture generateDefault() {
        final var colorsSets = new int[][]{
            new int[]{ 0x62cc82, 0x5abb78, 0x58b675, 0x50a66a },      // Green
            new int[]{ 0xcc84aa, 0xbb799c, 0xb67698, 0xa66c8b },      // Pink
            new int[]{ 0xcc7c79, 0xbb726f, 0xb66f6c, 0xa66562 },      // Red
            new int[]{ 0x81bccc, 0x77acbb, 0x74a8b6, 0x6a99a6 },      // Blue
            new int[]{ 0xccc67a, 0xbbb670, 0xb6b16d, 0xa6a264 }       // Gold
        };
        var colors = colorsSets[nextColorSet];
        nextColorSet = ++nextColorSet % colorsSets.length;
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
        return new Texture("default#" + (nextColorSet - 1), image, getPixels(image), 0, 0, 16, 16, 0, false);
    
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
    private static boolean isEmpty(int[] pixels, int srcW, int x, int y, int w, int h) {
        final int maxScan = 2000;

        if (w == 0 || h == 0) return false;

        /* Handles negative sizes */
        if (w < 0) {
            w *= -1;
            x -= w;
        }
        if (h < 0) {
            h *= -1;
            y -= h;
        }

        /* Scan the image */
        if (w * h <= maxScan) {
            /* Full Scan */
            for (int j = y ; j < y + h ; j++) {
                for (int i = x ; i < x + w ; i++) {
                    if ((pixels[j * srcW + i] >>> 24) > 0) {
                        return false;
                    }
                }
            }
        } else {
            /* Random scan */
            Random rnd = new Random(12345);
            for (int i = 0 ; i < maxScan ; i++) {
                int rx = x + rnd.nextInt(w);
                int ry = y + rnd.nextInt(h);
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
    private static boolean isTransparent(BufferedImage source, int[] pixels, int x, int y, int w, int h) {
        /* No alpha channel = not transparent */
        if (!source.getColorModel().hasAlpha()) {
            return false;
        }

        /* Handles negative sizes */
        if (w < 0) {
            w *= -1;
            x -= w;
        }
        if (h < 0) {
            h *= -1;
            y -= h;
        }

        final int maxScan = 2000;
        final int width = source.getWidth();
        if (w * h <= maxScan) {
            /* Full Scan */
            for (int j = y ; j < y + h ; j++) {
                for (int i = x ; i < x + w ; i++) {
                    if ((pixels[j * width + i] >>> 24) < 255) return true;
                }
            }
        } else {
            /* Random scan */
            Random rnd = new Random(12345);
            for (int i = 0 ; i < maxScan ; i++) {
                int rx = x + rnd.nextInt(w);
                int ry = y + rnd.nextInt(h);
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
        var img = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        var g = img.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    }
}
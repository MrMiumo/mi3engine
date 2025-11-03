package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import io.github.mrmiumo.mi3engine.Element.Face;

/**
 * Orthographic headless renderer for textures cubes. Each cube can
 * have customized texture with UVs for each face. Also transparent
 * textures are supported!<p>
 * This minimalist engine designed for Minecraft is primarily designed
 * to render models as BlockBench would render them.
 * @author Miumo
 */
class RenderCore implements RenderEngine {

    /** Correspondence table between triangle vertices and original rectangle */
    static final int[][] TRIANGLES = {{0, 1, 2}, {0, 2, 3}};

    /** List of all cubes to render */
    final List<Element> elements = new ArrayList<>();

    /** The camera settings */
    private Camera config = new Camera();

    /** The width of the image to generate */
    private final int width;

    /** The height of the image to generate */
    private final int height;

    /** Array containing z index of pixels being drawn */
    private final float[] zBuffer;

    /** Array containing all pixels being drawn */
    private int[] pixels;

    /**
     * Creates a new engine. One engine can be created to generate
     * pictures of a given size. Once created, one engine can be used
     * to generate multiple objects. Usage:
     * <code>
     * var engine = new RenderEngine(1920, 1080);
     * engine.camera()            // Setup camera position, angle and zoom
     *       .setZoom(0.32);
     * engine.addCube(myCube);    // Add scene elements
     * var img = engine.render(); // Render the scene
     * ImageIO.write(img, "PNG", myOutputStream);
     * </code>
     * Included tools can be used to generate cubes automatically for
     * some Minecraft elements such as models with {@link ModelParser}.
     * @param width the width of the image to generate in px (horizontal)
     * @param height the height of the image to generate in px (vertical)
     */
    RenderCore(int width, int height) {
        this.width = width;
        this.height = height;
        zBuffer = new float[width * height];
    }

    @Override
    public Camera camera() {
        return config;
    }

    @Override
    public RenderEngine setCamera(Camera camera) {
        this.config = camera;
        return this;
    }

    @Override
    public RenderEngine addElement(Element cube) {
        elements.add(cube);
        return this;
    }

    @Override
    public RenderEngine addElements(Collection<? extends Element> cubes) {
        this.elements.addAll(cubes);
        return this;
    }

    @Override
    public RenderEngine clear() {
        elements.clear();
        return this;
    }

    @Override
    public List<Element> getElements() {
        return List.copyOf(elements);
    }

    @Override
    public Vec2 size() {
        return new Vec2(width, height);
    }

    @Override
    public BufferedImage render() {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        Arrays.fill(zBuffer, Float.NEGATIVE_INFINITY);

        final Vec2 center = new Vec2(width / 2, height / 2);

        List<Triangle> trianglesOpaque = new ArrayList<>();
        List<Triangle> trianglesTransparent = new ArrayList<>();
        var zoom = config.zoom() * Math.min(width, height) / 1287;

        /* Create triangles from boxes */
        for (var element : elements) {
            final Vec[] localVerts = element.localVertices();
            final var modelPoint = RenderEngine.modelToWorld(element);

            /* Generates triangles for each face */
            for (var face : Face.values()) {
                Texture tex = element.getTexture(face);
                if (tex == null) continue;
                int[] idxs = FACES[face.ordinal()];

                Vec[] worldVerts = new Vec[4];
                Vec2[] screenVerts = new Vec2[4];
                double[] depthVals = new double[4];
                for (int k = 0 ; k < 4 ; k++) {
                    Vec world = modelPoint.apply(localVerts[idxs[k]]);
                    Vec view = world.rotate(config.rotation()).sub(config.translation());
                    double sxPix = center.x() - (view.x() * zoom);
                    double syPix = center.y() - (view.y() * zoom);
                    screenVerts[k] = new Vec2(sxPix, syPix);
                    worldVerts[k] = world;
                    depthVals[k] = -view.z();
                }
                
                for (int[] points : TRIANGLES) {
                    var triangle = Triangle.from(config, screenVerts, worldVerts, depthVals, points, tex);
                    if (triangle == null) continue;
                    if (triangle.opaque()) trianglesOpaque.add(triangle);
                    else trianglesTransparent.add(triangle);
                }
            }
        }

        /* Render opaque triangles first */
        for (Triangle T : trianglesOpaque) {
            renderTriangle(T);
        }

        /* Render transparent triangles back-to-front */
        trianglesTransparent.sort(Comparator.comparingDouble(t -> t.avgDepth()));
        for (Triangle T : trianglesTransparent) {
            renderTriangle(T);
        }

        return out;
    }

    /**
     * Render the given triangle.
     * @param triangle the triangle to print on the final image
     */
    private void renderTriangle(Triangle triangle) {
        TriVertex v0 = triangle.a(), v1 = triangle.b(), v2 = triangle.c();
        int minX = (int)Math.floor(RenderUtils.min(v0.x(), v1.x(), v2.x()));
        int maxX = (int)Math.ceil(RenderUtils.max(v0.x(), v1.x(), v2.x()));
        int minY = (int)Math.floor(RenderUtils.min(v0.y(), v1.y(), v2.y()));
        int maxY = (int)Math.ceil(RenderUtils.max(v0.y(), v1.y(), v2.y()));
        if (maxX < 0 || maxY < 0 || minX >= width || minY >= height) return;
        minX = RenderUtils.clamp(minX, 0, width - 1);
        maxX = RenderUtils.clamp(maxX, 0, width - 1);
        minY = RenderUtils.clamp(minY, 0, height - 1);
        maxY = RenderUtils.clamp(maxY, 0, height - 1);

        /* Make sur the face is visible */
        double area = edgeFunction(v0, v1, v2.x(), v2.y());
        if (Math.abs(area) < 1e-9) return;
        
        /* Try to draw each pixel within the triangle bounding box */
        for (int py = minY ; py <= maxY ; py++) {
            int rowOffset = py * width;
            for (int px = minX ; px <= maxX ; px++) {
                renderTrianglePixel(px, py, area, triangle, rowOffset);
            }
        }
    }

    /**
     * Render the pixel at the given coordinates on the final image
     * from the given triangle.
     * @param x the x coordinate of the pixel to draw on the screen
     * @param y the y coordinate of the pixel to draw on the screen
     * @param area the area of the face
     * @param triangle the triangle to draw the pixel from
     * @param rowOffset the offset of the row in the pixels array
     */
    private void renderTrianglePixel(int x, int y, double area, Triangle triangle, int rowOffset) {
        TriVertex v0 = triangle.a(), v1 = triangle.b(), v2 = triangle.c();

        // sample position in screen space (pixel center)
        double sx = x + 0.5;
        double sy = y + 0.5;
        // compute barycentric numerators
        double w0 = edgeFunction(v1, v2, sx, sy);
        if (w0 < 0) return;
        double w1 = edgeFunction(v2, v0, sx, sy);
        if (w1 < 0) return;
        double w2 = edgeFunction(v0, v1, sx, sy);
        if (w2 < 0) return;
        w0 /= area;
        w1 /= area;
        w2 /= area;

        double depth = w0 * v0.depth() + w1 * v1.depth() + w2 * v2.depth();
        int idx = rowOffset + x;
        if (depth <= zBuffer[idx]) return; // not visible compared to current zBuffer

        // interpolate normalized UV inside the texture region (u,v in [0..1])
        double u = w0 * v0.u() + w1 * v1.u() + w2 * v2.u();
        double v = w0 * v0.v() + w1 * v1.v() + w2 * v2.v();

        pixels[idx] = computeColor(u, v, triangle, idx);
        zBuffer[idx] = (float)depth;
    }

    /**
     * Computes the color to put on the final image.
     * For some reason, keeping this function enables to make the code
     * run faster!
     * @param u the position of the pixel to pick from the texture (X)
     * @param v the position of the pixel to pick form the texture (Y)
     * @param triangle the triangle being drawn
     * @param idx the id of the pixel to set
     * @return the color to put in the pixel grid
     */
    private int computeColor(double u, double v, Triangle triangle, int idx) {
        return RenderUtils.transformColor(
            triangle.texture().getRGB(u, v),
            triangle.intensity(),
            pixels[idx]
        );
    }

    /**
     * Checks if a point (C) is inside a 2D triangle.
     *
     * <p>This function works by calculating a value that indicates which side
     * of the line (from A to B) the point C is on. A positive, negative,
     * or zero result means the point is to the left, right, or on the line.</p>
     *
     * @param a The coordinates of the start of the line (A).
     * @param b The coordinates of the end of the line (B).
     * @param cx The x-coordinate of the point to check (C).
     * @param cy The y-coordinate of the point to check (C).
     * @return A signed value indicating the point's position relative to the line.
     */
    private static double edgeFunction(TriVertex a, TriVertex b, double cx, double cy) {
        return (cx - a.x()) * (b.y() - a.y()) - (cy - a.y()) * (b.x() - a.x());
    }
}

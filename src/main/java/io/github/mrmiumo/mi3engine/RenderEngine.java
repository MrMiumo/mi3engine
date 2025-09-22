package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Orthographic headless renderer for textures cubes. Each cube can
 * have customized texture with UVs for each face. Also transparent
 * textures are supported!<p>
 * This minimalist engine designed for Minecraft is primarily designed
 * to render models as BlockBench would render them.<p>
 * This interface enables tools to add layers over the Core to extends
 * its capabilities.
 * @author Miumo
 */
public interface RenderEngine {

    /** Faces defined explicitly any element */
    static final int[][] FACES = {
        {1,5,7,3}, // +X
        {0,2,6,4}, // -X
        {2,3,7,6}, // +Y
        {0,4,5,1}, // -Y
        {4,6,7,5}, // +Z
        {0,1,3,2}  // -Z
    };
    
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
     * @return the engine created for the given image size
     */
    public static RenderEngine from(int width, int height) {
        return new RenderCore(width, height);
    }

    /**
     * Enables to put in place a custom camera to replace the default one.
     * <p>
     * In most of the cases, you should use the default camera and
     * customize it!
     * @param camera the camera to set
     * @return this engine
     */
    public RenderEngine setCamera(Camera camera);

    /**
     * Gets the object that enables camera configuration
     * @return the camera configuration object
     */
    public Camera camera();

    /**
     * Adds a new cube to the scene
     * @param cube the cube to add
     * @return this engine
     */
    public RenderEngine addElement(Element cube);

    /**
     * Adds multiple new cubes to the scene
     * @param cubes the cubes to add
     * @return this engine
     */
    public RenderEngine addElements(Collection<Element> cubes);

    /**
     * Reset the scene by removing all the cubes
     * @return this engine
     */
    public RenderEngine clear();

    /**
     * Enables to get a copy of all the cubes currently in the engine.
     * @return the list of cubes set in the engine
     */
    public List<Element> getElements();
    
    /**
     * Gets the size of the output image currently setup
     * @return the width and height of the output image
     */
    public Vec2 size();

    /**
     * Convert all current cubes into triangles, order them and render
     * them depending on their depth and transparency.
     * @return the generated image
     */
    public BufferedImage render();
    
    /**
     * Simple 2D vector data object.
     * @param x the x coordinate of the vector
     * @param y the y coordinate of the vector
     */
    public static record Vec2(double x, double y) { }

    /**
     * Creates a lambda that converts a vector from the model coordinates
     * to the world coordinates.
     * Aka, applies rotation and position of the given element to the
     * given point to compute its transformed position.
     * @param cube the cube to generate the conversion function for
     * @return the convert function
     */
    static Function<Vec, Vec> modelToWorld(Element cube) {
        final double rx = Math.toRadians(cube.rotation().x());
        final double ry = Math.toRadians(cube.rotation().y());
        final double rz = Math.toRadians(cube.rotation().z());
        final double crx = Math.cos(rx), srx = Math.sin(rx);
        final double cry = Math.cos(ry), sry = Math.sin(ry);
        final double crz = Math.cos(rz), srz = Math.sin(rz);

        return (Vec p) -> {
            double x = p.x() - cube.pivot().x();
            double y = p.y() - cube.pivot().y();
            double z = p.z() - cube.pivot().z();
            // Rx
            double x1 = x;
            double y1 = y * crx - z * srx;
            double z1 = y * srx + z * crx;
            // Ry
            double x2 = x1 * cry + z1 * sry;
            double y2 = y1;
            double z2 = -x1 * sry + z1 * cry;
            // Rz
            double x3 = x2 * crz - y2 * srz;
            double y3 = x2 * srz + y2 * crz;
            double z3 = z2;
            return new Vec(x3 + cube.pivot().x() + cube.position().x(),
                            y3 + cube.pivot().y() + cube.position().y(),
                            z3 + cube.pivot().z() + cube.position().z());
        };
    }
}

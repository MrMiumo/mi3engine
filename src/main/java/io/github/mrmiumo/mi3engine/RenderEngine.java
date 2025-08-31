package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

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
    public static RenderEngine from(int width, int height) {
        return new RenderCore(width, height);
    }

    /**
     * Gets the object that enables camera configuration
     * @return the camera configuration object
     */
    public Camera camera();

    /**
     * Adds a new cube to the scene
     * @param cube the cube to add
     */
    public void addCube(Cube cube);

    /**
     * Adds multiple new cubes to the scene
     * @param cubes the cubes to add
     */
    public void addCubes(Collection<Cube> cubes);

    /**
     * Reset the scene by removing all the cubes
     */
    public void clearScene();

    /**
     * Enables to get a copy of all the cubes currently in the engine.
     * @return the list of cubes set in the engine
     */
    public List<Cube> getCubes();
    
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
    
    public static record Vec2(double x, double y) { }
}

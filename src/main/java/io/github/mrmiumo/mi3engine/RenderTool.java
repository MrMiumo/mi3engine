package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

/**
 * Intermediary class used to add inheritance for creating render
 * engine tools. This class enable to implements RenderEngine
 * without specifying all methods.
 * @see RenderEngine
 */
class RenderTool implements RenderEngine {

    /** The original render engine to use for image generation */
    final RenderEngine engine;

    /**
     * Intermediary class used to add inheritance for creating render
     * engine tools. This class enable to implements RenderEngine
     * without specifying all methods.
     * @param engine the engine to build the tool onto
     */
    public RenderTool(RenderEngine engine) {
        this.engine = engine;
    }

    @Override
    public Camera camera() {
        return engine.camera();
    }

    @Override
    public RenderEngine setCamera(Camera camera) {
        return engine.setCamera(camera);
    }

    @Override
    public RenderEngine addCube(Cube cube) {
        return engine.addCube(cube);
    }

    @Override
    public RenderEngine addCubes(Collection<Cube> cubes) {
        return engine.addCubes(cubes);
    }

    @Override
    public RenderEngine clear() {
        return engine.clear();
    }

    @Override
    public List<Cube> getCubes() {
        return engine.getCubes();
    }

    @Override
    public Vec2 size() {
        return engine.size();
    }

    @Override
    public BufferedImage render() {
        return engine.render();
    }
}

package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Group implements Element {

    private ArrayList<Cube> elements = new ArrayList<>();
    private Vec scale = new Vec(1, 1, 1);
    private Vec rotate = Vec.ZERO;
    private Vec pivot = Vec.ZERO;
    private Vec position = Vec.ZERO;

    public Group() {}

    /**
     * Adds the given cube to the group.
     * @param cube the cube to add to the group
     * @return this group
     */
    public Group add(Cube cube) {
        elements.add(cube);
        return this;
    }

    /**
     * Creates a "fake" engine from this group that can be used to
     * collect elements that will be added directly into this group.
     * <p>
     * WARNING: this engine CANNOT be used to actually render its
     * elements. The render() method is NOT implemented.
     * @return the engine
     */
    public RenderEngine asEngine() {
        return new DummyEngine();
    }

    public Group scale(Vec scale) {
        this.scale = scale; // TODO may need .div(1.6)
        return this;
    }

    public Group rotate(Vec rotation) {
        this.rotate = rotation;
        return this;
    }

    @Override
    public Vec rotation() { return rotate; }

    public Group pivot(Vec pivot) {
        this.pivot = pivot;
        return this;
    }

    @Override
    public Vec pivot() {  return pivot; }

    public Group position(Vec position) {
        this.position = position;
        return this;
    }

    @Override
    public Vec position() { return position; }

    @Override
    public Texture getTexture(Face face) { return null;  }

    @Override
    public Vec[] localVertices() { return new Vec[]{}; }

    public Collection<Cube> getElements() {
        var convert = RenderEngine.modelToWorld(this);
        return elements.stream()
            .map(this::fixPivot)
            .map(cube -> {
                var position = convert.apply(cube.position().mul(scale));
                return new Cube(
                    cube.size().mul(scale),
                    position,
                    cube.rotation().add(rotate),
                    position, // pivot
                    cube.textures()
                );
            })
            .toList();
    }

    /**
     * Forces the cube to use a pivot equals to its position by shifting
     * the cube position if needed to compensate.
     * @param cube the cube to fix the pivot from
     * @return the fixed cube
     */
    private Cube fixPivot(Cube cube) {
        if (cube.rotation().equals(Vec.ZERO) || cube.pivot().equals(cube.position())) return cube;
        var position = RenderEngine.modelToWorld(cube).apply(Vec.ZERO);
        return new Cube(cube.size(), position, cube.rotation(), position, cube.textures());
    }

    public static Vec rotate(Vec p, double angle) {
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));
        return new Vec(
            p.x() * cos - p.y() * sin,
            p.x() * sin + p.y() * cos,
            p.z()
        );
    }

    /**
     * "Fake" engine that can be used to collect elements only. This
     * engine cannot be used to create renders!
     */
    class DummyEngine implements RenderEngine{
        private Camera camera = new Camera();

        @Override
        public RenderEngine setCamera(Camera camera) {
            this.camera = camera;
            return this;
        }

        @Override
        public Camera camera() {
            return camera;
        }

        @Override
        public RenderEngine addElement(Element cube) {
            if (cube instanceof Cube c) {
                elements.add(c);
            }
            return this;
        }

        @Override
        public RenderEngine addElements(Collection<? extends Element> cubes) {
            cubes.forEach(this::addElement);
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
            return new Vec2();
        }

        @Override
        public BufferedImage render() {
            throw new UnsupportedOperationException("render() not supported by DummyEngine");
        }
    }
}

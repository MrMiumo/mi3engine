package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

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
     * Adds all the cube contained in the given list. Any element that
     * is not {@link Cube} will be ignored.
     * @param elements
     * @return
     */
    public Group add(Collection<? extends Element> elements) {
        for (var element : elements) {
            if (element instanceof Cube cube) this.elements.add(cube);
        }
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
        this.scale = scale;
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
    public Element move(Vec offset) {
        position = position.add(offset);
        pivot = pivot.add(offset);
        return this;
    }

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
                    rotate(cube, convert),
                    position, // pivot
                    cube.textures()
                );
            })
            .toList();
    }

    /**
     * Add current group rotation to the given cube rotation.
     * This function works by computing points positions, rotating them
     * using {@link RenderEngine#modelToWorld(Element)} and then deducing
     * final rotation value as an Euler rotation.
     * @param cube the cube to compute the rotation for
     * @param convert the current group "modelToWorld" function
     * @return the rotation of the cube after this group transformation
     */
    private Vec rotate(Cube cube, Function<Vec, Vec> convert) {
        var cubeRotate = RenderEngine.modelToWorld(cube);
        Function<Vec, Vec> rotate = v -> convert.apply(cubeRotate.apply(v));

        var base = rotate.apply(new Vec(0, 0, 0));
        var v1 = rotate.apply(new Vec(1, 0, 0)).sub(base);
        var v2 = rotate.apply(new Vec(0, 1, 0)).sub(base);
        var v3 = rotate.apply(new Vec(0, 0, 1)).sub(base);

        /* Measure rotation on X Y and Z */
        var x = Math.toDegrees(Math.atan2(v2.z(), v3.z())); // Φ
        var y = Math.toDegrees(Math.asin(v1.z())); // θ
        var z = Math.toDegrees(Math.atan2(v1.y(), v1.x())); // Ψ
        if (Math.abs(v1.z()) == 1) { // Singularity!
            z = 0;
            x = 90 - Math.toDegrees(Math.atan2(v2.y(), v2.x()));
        }

        return new Vec(x, y, z);
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
            return new Vec2(0, 0);
        }

        @Override
        public BufferedImage render() {
            throw new UnsupportedOperationException("render() not supported by DummyEngine");
        }

        public Group group() {
            return Group.this;
        }
    }
}

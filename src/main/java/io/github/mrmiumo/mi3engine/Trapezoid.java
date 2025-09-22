package io.github.mrmiumo.mi3engine;

/**
 * A trapezoidal prism for rendering bent limbs (e.g. Minecraft arms).
 * Similar to Cube, but the top face can be tapered inwards.
 * @param size the dimensions of the base of the trapezoid (the cube
 *     before being tapped)
 * @param position the coordinates of the base cube
 * @param rotation the local rotation in degrees around the pivot
 * @param pivot the point around which the element is rotated
 * @param taper how much faces expands
 * @param textures array of 6 textures for faces (+X, -X, +Y, -Y, +Z, -Z)
 */
public record Trapezoid(Vec size, Vec position, Vec rotation, Vec pivot, Vec taper, Texture[] textures) implements Element {

    private final static Vec CENTER_OFFSET = new Vec(8, -8, -8);

    /**
     * Creates a new trapezoid.
     * @param size the dimensions of the base of the trapezoid (the cube
     *     before being tapped)
     * @param position the coordinates of the base cube
     * @param rotation the local rotation in degrees around the pivot
     * @param pivot the point around which the element is rotated
     * @param taper how much faces expands
     * @param textures array of 6 textures for faces (+X, -X, +Y, -Y, +Z, -Z)
     */
    public Trapezoid {
        size = new Vec(-size.x(), size.y(), size.z());
        position = position.add(CENTER_OFFSET);
    }

    @Override
    public Texture getTexture(Face face) {
        return textures != null ? textures[face.ordinal()] : null;
    }
    
    @Override
    public Vec[] localVertices() {
        Vec s = size;
        Vec t = taper;
        Vec[] v = new Vec[8];

        // LEFT
        v[0] = new Vec(0,     0           ,     0); // bottom-back
        v[1] = new Vec(0,           -t.y(), s.z()); // bottom-front
        v[2] = new Vec(0,     s.y()       ,     0); // top-back
        v[3] = new Vec(0,     s.y() +t.y(), s.z()); // top-front
        // RIGHT
        v[4] = new Vec(s.x(), 0           ,     0); // bottom-back
        v[5] = new Vec(s.x(),       -t.y(), s.z()); // bottom-front
        v[6] = new Vec(s.x(), s.y()       ,     0); // top-back
        v[7] = new Vec(s.x(), s.y() +t.y(), s.z()); // top-front

        return v;
    }
}
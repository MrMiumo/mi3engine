package io.github.mrmiumo.mi3engine;

import io.github.mrmiumo.mi3engine.RenderEngine.Vec2;

/**
 * Triangle data structure to represent a vertex.
 */
public record TriVertex(double x, double y, double depth, double u, double v) {
    
    /** Default UVs */
    private static final Vec2[] UV = new Vec2[] { new Vec2(0,0), new Vec2(1,0), new Vec2(1,1), new Vec2(0,1) };

    /**
     * Creates a new triangle vertex from the given position list, depths
     * list at the given index.
     * @param pos the list of positions
     * @param depths the list of depths
     * @param i the index of the position and depth to take
     * @return the corresponding TriVertex
     */
    public static TriVertex from(Vec2[] pos, double[] depths, int i) {
        return new TriVertex(pos[i].x(), pos[i].y(), depths[i], UV[i].x(), UV[i].y());
    }

    /**
     * Converts this triangle vertex in a vector.
     * @return the vector corresponding to this vertex.
     */
    public Vec vec() {
        return new Vec(x, y, depth);
    }
}
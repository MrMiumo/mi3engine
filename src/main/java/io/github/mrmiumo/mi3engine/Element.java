package io.github.mrmiumo.mi3engine;

/**
 * Represent any 3D mathematical shape made of 6 regular faces.
 */
public interface Element {

    /**
     * Gets the angle of the element in degrees
     * @return the angle of the element
     */
    public Vec rotation();

    /**
     * Gets the rotation origin point (nullable)
     * @return the rotation origin point
     */
    public Vec pivot();

    /**
     * Gets the position of the element origin in the 3D space
     * @return the position of the element
     */
    public Vec position();

    /**
     * Gets the texture of the given face
     * @param face the face to get the texture from
     * @return the associated texture, null if not set
     */
    public Texture getTexture(Face face);

    /**
     * Computes the local vertices for this element.
     * @return the 8 vertices
     */
    public Vec[] localVertices();

    /**
     * Names of the cube faces
     */
    public static enum Face {
        /** The front face */
        SOUTH,
        /** The back face */
        NORTH,
        /** The top face */
        UP,
        /** The bottom face */
        DOWN,
        /** The left face */
        EAST,
        /** The right face */
        WEST;
    }

    /**
     * Represent each of the 3 axis.
     */
    public static enum Axis {

        /** The X axis (left/right) */
        X,

        /** The Y axis (top/bottom) */
        Y,

        /** The Z axis (front/back) */
        Z;
    }

}

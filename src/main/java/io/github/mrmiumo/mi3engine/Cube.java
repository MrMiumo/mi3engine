package io.github.mrmiumo.mi3engine;

import java.util.EnumMap;

/**
 * Data of a model's cube.
 * @param size Size of the cube in McModel unit (16 = 1 bloc)
 * @param position Position of the cube after applying CENTER_OFFSET
 * @param rotation The angle of the cube in degrees
 * @param pivot the rotation origin point (nullable)
 * @param textures the texture applied to each face of the cube
 */
record Cube(Vec size, Vec position, Vec rotation, Vec pivot, EnumMap<Face, Texture> textures) {
    private final static Vec CENTER_OFFSET = new Vec(8, -8, -8);

    /**
     * Creates a new cube builder defined by its size only.
     * @param from coordinates of one corner of the cube
     * @param to coordinates of the opposite corner
     */
    public static Builder from(Vec from, Vec to) {
        return Builder.from(from, to);
    }

    public Cube {
        position = position.add(CENTER_OFFSET);
        pivot = pivot == null ? new Vec(0, 0, 0) : pivot;
    }

    /**
     * Creates a new cube defined by its size, position and rotation
     * @param size the size of the element (width, height and depth)
     * @param position the position of the box in space
     * @param rotation the angle of the box
     * @param pivot the rotation origin point (nullable)
     */
    public Cube(Vec size, Vec position, Vec rotation, Vec pivot) {
        this(
            size,
            position,
            rotation,
            pivot,
            new EnumMap<>(Face.class)
        );
    }

    /**
     * Adds a texture to the given face
     * @param face the face to set the texture onto
     * @param texture the texture to set
     */
    public void setFaceTexture(Face face, Texture texture){
        textures.put(face, texture);
    }

    /**
     * Gets the texture of the given face
     * @param face the face to get the texture from
     * @return the associated texture, null if not set
     */
    public Texture getTexture(Face face) {
        return textures.get(face);
    }

    /**
     * Names of the cube faces
     */
    public static enum Face {
        SOUTH, NORTH, UP, DOWN, EAST, WEST;
    }

    public static enum Axis {
        X, Y, Z;
    }

    public static class Builder {
        /** Position of the cube in the space */
        private final Vec position;

        /** Size of the cube on each axis */
        private final Vec size;

        /** Rotation angle of each axis */
        private Vec rotation = Vec.ZERO;

        /** Coordinates of the pivot point (point on which the cube rotates) */
        private Vec pivot = null;

        /** Store textures for each face */
        private EnumMap<Face, Texture> textures = new EnumMap<>(Face.class);

        /**
         * Creates a new cube builder defined by its size only.
         * @param from coordinates of one corner of the cube
         * @param to coordinates of the opposite corner
         */
        public static Builder from(Vec from, Vec to) {
            var x = from.x();
            var y = from.y();
            var z = from.z();

            var dx = to.x() - from.x();
            var dy = to.y() - from.y();
            var dz = to.z() - from.z();

            if (dx < 0) { x += dx; }
            if (dy < 0) { y += dy; }
            if (dz < 0) { z += dz; }
            
            return new Builder(
                new Vec(-x, y, z),
                new Vec(-Math.abs(dx), Math.abs(dy), Math.abs(dz))
            );
        }

        /**
         * Creates a new cube builder defined by its size only.
         * @param from coordinates of one corner of the cube
         * @param to coordinates of the opposite corner
         */
        private Builder(Vec position, Vec size) {
            this.position = position;
            this.size = size;
        }

        /**
         * Sets a rotation value for this cube. Only one rotation on one
         * axis is permitted by MineCraft.
         * @param angle the angle in degrees of the rotation
         * @param axis the axis to rotate onto
         * @return this builder
         */
        public Builder rotation(double angle, Axis axis) {
            switch (axis) {
                case X -> rotation = new Vec(angle, 0, 0);
                case Y -> rotation = new Vec(0, -angle, 0);
                case Z -> rotation = new Vec(0, 0, -angle);
            }
            return this;
        }

        /**
         * Sets a rotation pivot point for this cube.
         * @param pivot the coordinates of the pivot point
         * @return this builder
         */
        public Builder pivot(double x, double y, double z) {
            this.pivot = new Vec(-x - position.x(), y - position.y(), z - position.z());
            return this;
        }

        /**
         * Sets a rotation pivot point for this cube.
         * @param origin the coordinates of the pivot point
         * @return this builder
         */
        public Builder origin(Vec origin) {
            return pivot(origin.x(), origin.y(), origin.z());
        }

        /**
         * Sets the texture to be used to pain the all faces. When
         * using a BufferedImage, the following code can be used. Make
         * sure to not recreate the same texture object twice for performances.
         * <pre>
         *     new Texture(myBufferedImage);
         * </pre>
         * @param texture the texture to set
         * @return this builder
         */
        public Builder texture(Texture texture) {
            for (var face : Face.values()) {
                textures.put(face, texture);
            }
            return this;
        }
        
        /**
         * Sets the texture to be used to pain the given face.
         * When using a BufferedImage for the texture, the following
         * code can be used. Make sure to not recreate the same texture
         * object twice for performances.
         * <pre>
         *     new Texture(myBufferedImage);
         * </pre>
         * @param face the face to apply the texture to
         * @param texture the texture to set
         * @param x1 the x coordinate of the top left corner of the UV box
         * @param y1 the y coordinate of the top left corner of the UV box
         * @param x2 the x coordinate of the bottom right corner of the box
         * @param y2 the y coordinate of the bottom right corner of the box
         * @return this builder
         */
        public Builder texture(Face face, Texture texture, int rotate, float x1, float y1, float x2, float y2) {
            rotate += switch (face) {
                case SOUTH -> 0;
                case NORTH -> 1;
                case EAST -> 1;
                case WEST -> 0;
                case UP -> 3;
                case DOWN -> 0;
            };
            textures.put(face, texture.uv(x1, y1, x2 - x1, y2 - y1, rotate % 4));
            return this;
        }

        /**
         * Builds the cube with the data given using builder's methods.
         * @return the cube
         */
        public Cube build() {
            return new Cube(size, position, rotation, pivot, textures);
        }

        /**
         * Adds this cube to the given engine
         * @param engine the engine to add the cube to
         */
        public void render(RenderEngine engine) {
            engine.addCube(build());
        }
    }
}
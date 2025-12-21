package io.github.mrmiumo.mi3engine;

import java.util.EnumMap;

/**
 * Data of a model's cube.
 * @param size Size of the cube in McModel unit (16 = 1 bloc)
 * @param position Position of the cube
 * @param rotation The angle of the cube in degrees
 * @param pivot the rotation origin point (nullable)
 * @param textures the texture applied to each face of the cube
 */
public record Cube(Vec size, Vec position, Vec rotation, Vec pivot, EnumMap<Face, Texture> textures) implements Element {

    /**
     * Creates a new cube builder defined by its size only.
     * @param from coordinates of one corner of the cube
     * @param to coordinates of the opposite corner
     * @return a builder to configure the cube
     */
    public static Builder from(Vec from, Vec to) {
        return Builder.from(from, to);
    }

    /**
     * Creates a new cube.
     * @param size the size of the cube (width and height)
     * @param position the position of the cube in the space (x, y, z)
     * @param rotation the angle of the cube (x, y, z)
     * @param pivot the point on which the rotation must be applied
     * @param textures the texture to use for each face
     */
    public Cube {
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

    @Override
    public Texture getTexture(Face face) {
        return textures.get(face);
    }

    @Override
    public Vec[] localVertices() {
        Vec[] v = new Vec[8];
        v[0] = new Vec(0,        0       , 0       ); // bottom-left
        v[1] = new Vec(0,        0       , size.z()); // bottom-right
        v[2] = new Vec(0,        size.y(), 0       ); // top-left
        v[3] = new Vec(0,        size.y(), size.z()); // top-right
        // FRONT
        v[4] = new Vec(size.x(), 0       , 0       ); // bottom-left
        v[5] = new Vec(size.x(), 0       , size.z()); // bottom-right
        v[6] = new Vec(size.x(), size.y(), 0       ); // top-left
        v[7] = new Vec(size.x(), size.y(), size.z()); // top-right
        return v;
    }

    @Override
    public Element move(Vec offset) {
        return new Cube(size, position.add(offset), rotation, pivot.add(offset), textures);
    }

    /**
     * Cube builder that enable to create new cubes easily.
     */
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
         * @return a new builder to configure the cube of the given size
         */
        public static Builder from(Vec from, Vec to) {
            var x = from.x();
            var y = from.y();
            var z = from.z();

            var dx = to.x() - from.x();
            var dy = to.y() - from.y();
            var dz = to.z() - from.z();
            
            return new Builder(
                new Vec(x, y, z),
                new Vec(dx, dy, dz)
            );
        }

        /**
         * Creates a new cube builder defined by its position and size.
         * @param position coordinates of one corner of the cube
         * @param size the size on each axis of the cube
         */
        public Builder(Vec position, Vec size) {
            this.position = position;
            this.size = new Vec(size.x(), size.y(), size.z());
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
                case Z -> rotation = new Vec(0, 0, angle);
            }
            return this;
        }

        /**
         * Sets a rotation value for this cube. This method enables to
         * set rotation on multiple axis. This should NOT be used with
         * Minecraft models since they do not allow multiple axis rotation.
         * @param rot the rotation to set
         * @return this builder
         */
        public Builder rotation(Vec rot) {
            rotation = new Vec(rot.x(), -rot.y(), rot.z());
            return this;
        }

        /**
         * Sets a rotation pivot point for this cube.
         * Info: the pivot point is NOT linked to the position
         * @param x the x coordinate of the pivot point
         * @param y the y coordinate of the pivot point
         * @param z the z coordinate of the pivot point
         * @return this builder
         */
        public Builder pivot(double x, double y, double z) {
            return pivot(new Vec(x, y, z));
        }
        
        /**
         * Sets a rotation pivot point for this cube.
         * Info: the pivot point is NOT linked to the position
         * @param v the vector containing all pivot coordinates
         * @return this builder
         */
        public Builder pivot(Vec v) {
            this.pivot = v;
            return this;
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
         * Sets the texture to be used to paint the given face.
         * When using a BufferedImage for the texture, the following
         * code can be used. Make sure to not recreate the same texture
         * object twice for performances.
         * <pre>
         *     new Texture(myBufferedImage);
         * </pre>
         * @param face the face to apply the texture to
         * @param texture the texture to set
         * @param rotate to rotate the texture (0, 1, 2 or 3)
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
         * Sets the texture to be used to paint the given face.
         * When using a BufferedImage for the texture, the following
         * code can be used. Make sure to not recreate the same texture
         * object twice for performances.
         * <pre>
         *     new Texture(myBufferedImage);
         * </pre>
         * @param face the face to apply the texture to
         * @param texture the texture to set
         * @return this builder
         */
        Builder texture(Face face, Texture texture) {
            textures.put(face, texture);
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
        public void addTo(RenderEngine engine) {
            engine.addElement(build());
        }
    }
}
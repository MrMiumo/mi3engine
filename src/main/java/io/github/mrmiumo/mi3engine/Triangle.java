package io.github.mrmiumo.mi3engine;

import io.github.mrmiumo.mi3engine.RenderEngine.Vec2;

/**
 * Represent a triangle that compose a face, defines by 3 vertex, a
 * texture and an average depth. The triangle is able to compute its
 * light intensity using {@link Camera}.
 * @param a the vertex of the first corner
 * @param b the vertex of the second corner
 * @param c the vertex of the third corner
 * @param texture the image to apply on the triangle
 * @param intensity the amount of light received by this face (computed automatically)
 * @param opaque whether the texture has transparency or not
 * @param avgDepth average depth of the triangle
 */
public record Triangle(TriVertex a, TriVertex b, TriVertex c, Texture texture, double intensity, boolean opaque, double avgDepth) {

    /**
     * Creates a new triangle from the given screen vertices, z depths,
     * triangle points and the texture.
     * @param cam the camera configuration needed to compute light
     * @param screenVerts the list of screen-space vertices of the original rectangle
     * @param worldVerts the list of world-space vertices of the original rectangle
     * @param depths the list of depths
     * @param points the points of the triangle to use
     * @param texture the texture to apply
     * @return the triangle
     */
    public static Triangle from(Camera cam, Vec2[] screenVerts, Vec[] worldVerts, double[] depths, int[] points, Texture texture) {
        var a = TriVertex.from(screenVerts, depths, points[0]);
        var b = TriVertex.from(screenVerts, depths, points[1]);
        var c = TriVertex.from(screenVerts, depths, points[2]);

        Vec worldA = worldVerts[points[0]];
        Vec worldB = worldVerts[points[1]];
        Vec worldC = worldVerts[points[2]];
        var normal = getNormal(worldA, worldB, worldC).rotate(cam.rotation());
        if (normal.z() > 0) return null;

        return new Triangle(
            a, b, c,
            texture,
            computeIntensity(cam, normal),
            !texture.isTransparent(),
            RenderUtils.max(a.depth(), b.depth(), c.depth())
        );
    }

    /**
     * Computes the normal vector (direction of the face) to
     * compute the corresponding light intensity of the face.
     * @param cam the camera configuration needed to compute light
     * @param normal the normal vector of this triangle
     * @return the intensity of the face
     */
    private static double computeIntensity(Camera cam, Vec normal) {
        /* Computes the light from the normal */
        var spot = cam.spotDirection();
        double diffuseFactor = normal.dot(new Vec(spot.x(), spot.y(), spot.z()));
        double clampedDiffuseFactor = Math.max(0, diffuseFactor);
        double finalIntensity = cam.ambientLight() + cam.spotLight() * clampedDiffuseFactor;
        if (normal.y() < 0) finalIntensity /= 1 -normal.y();

        return Math.min(1.0, finalIntensity);
    }

    /**
     * Computes the normal vector of this triangle - the direction at
     * which the triangle is facing.
     * @param a the vertex of the first corner
     * @param b the vertex of the second corner
     * @param c the vertex of the third corner
     * @return the normal vector
     */
    private static Vec getNormal(Vec a, Vec b, Vec c) {
        Vec ab = b.sub(a);
        Vec ac = c.sub(a);
        return ab.cross(ac).normalize();
    }
}
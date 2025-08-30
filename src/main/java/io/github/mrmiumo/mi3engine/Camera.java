package io.github.mrmiumo.mi3engine;

/**
 * Configuration of the renderer.
 */
public class Camera {

    /** The intensity of the ambient light */
    public static final float AMBIENT = 0.6f;

    /** The intensity of the diffuse light (spot light) */
    public static final float DIFFUSE = 9f;

    /** The position of the light source */
    public static final Vec LIGHT_DIRECTION = new Vec(0, 1, 0).normalize();

    /** The zoom level of the camera (scale factor) */
    private double zoom = 1.0;

    /** The angle of the camera */
    private Vec camRotation = new Vec(0,0,0);

    /** The position of the camera (never changed) */
    private Vec camTranslation = new Vec(0,0,0);

    /**
     * Default constructor that create a new default camera.
     */
    public Camera() { }

    /**
     * Defines the camera rotation. Each rotation is given in degrees
     * between -180 and 180.
     * @param x the rotation on the x axis (tilt: up/down)
     * @param y the rotation on the y axis (pan: left/right)
     * @param z the rotation on the z axis (roll)
     * @return this config
     */
    public Camera setRotation(int x, int y, int z) {
        camRotation = new Vec(-x, 180 - y, z);
        return this;
    }

    /**
     * Gets the rotation of the camera (the angle at which its pointing)
     * @return the rotation of the camera in degrees
     */
    public Vec camRotation() {
        return camRotation;
    }

    /**
     * Translate the object in the view.
     * @param x move the object on the horizontal axis (left/right)
     * @param y move the object on the vertical axis (up/down)
     * @return this config
     */
    public Camera setTranslation(double x, double y) {
        camTranslation = new Vec(-x * 5.25, -y * 5.25, 0);
        return this;
    }

    /**
     * Gets the translation of the camera (the position in the space)
     * @return the translation of the camera
     */
    public Vec camTranslation() {
        return camTranslation;
    }

    /**
     * Changes the object size on the view.
     * @param scale the new scale of the object (1 is default)
     * @return this config
     */
    public Camera setZoom(double scale) {
        zoom = scale * 80.5;
        return this;
    }
    /**
     * Gets the zoom of the camera (the scale)
     * @return the zoom
     */
    public double zoom() {
        return zoom;
    }
}
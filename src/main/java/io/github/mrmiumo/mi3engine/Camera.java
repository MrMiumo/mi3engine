package io.github.mrmiumo.mi3engine;

/**
 * Configuration of the renderer.
 */
public class Camera {

    /** The intensity of the ambient light */
    private float ambient = 0.5f;

    /** The intensity of the spot light (diffuse) */
    private float spot = 2f;

    /** The position of the light source */
    private Vec spotDirection = new Vec(0, 1, .6).normalize();

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
        camRotation = new Vec(-x, 180 - y, -z);
        return this;
    }

    /**
     * Gets the configured camera rotation. This method return the
     * value given by the user, not the transformed value. To get the
     * one transformed to be usable by the engine, use {@link #rotation()}.
     * @return the rotation currently set up
     */
    public Vec getRotation() {
        return new Vec(- camRotation.x(), 180 - camRotation.y(), - camRotation.z());
    }

    /**
     * Gets the rotation of the camera (the angle at which its pointing)
     * @return the rotation of the camera in degrees
     */
    public Vec rotation() {
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
     * Gets the configured camera translation. This method return the
     * value given by the user, not the transformed value. To get the
     * one transformed to be usable by the engine, use {@link #translation()}.
     * @return the translation currently set up
     */
    public Vec getTranslation() {
        return new Vec(- camTranslation.x() / 5.25, - camTranslation.y() / 5.25, 0);
    }

    /**
     * Gets the translation of the camera (the position in the space)
     * @return the translation of the camera
     */
    public Vec translation() {
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
     * Gets the configured camera zoom. This method return the
     * value given by the user, not the transformed value. To get the
     * one transformed to be usable by the engine, use {@link #zoom()}.
     * @return the zoom currently set up
     */
    public double getZoom() {
        return zoom / 80.5;
    }

    /**
     * Gets the zoom of the camera (the scale)
     * @return the zoom
     */
    public double zoom() {
        return zoom;
    }

    /**
     * Changes the default ambient light intensity. Ambient light
     * corresponds to the minimum light intensity for areas that does
     * not receive any light.
     * @param light the intensity of the ambient light
     * @return this config
     */
    public Camera setAmbientLight(float light) {
        this.ambient = light;
        return this;
    }

    /**
     * Gets the configured ambient light intensity.
     * @return the ambient light
     */
    public float ambientLight() {
        return ambient;
    }

    /**
     * Changes the default spot light intensity. Spot light
     * corresponds to a diffuse source that illuminate elements faces
     * based on their orientation like a real light source would do.
     * @param light the intensity of the spot light
     * @return this config
     */
    public Camera setSpotLight(float light) {
        this.spot = light;
        return this;
    }

    /**
     * Gets the configured spot light intensity.
     * @return the spot light
     */
    public float spotLight() {
        return spot;
    }

    /**
     * Changes the default spot light position direction.
     * @param direction the orientation of the spot light
     * @return this config
     */
    public Camera setSpotDirection(Vec direction) {
        this.spotDirection = direction.normalize();
        return this;
    }

    /**
     * Gets the configured spot light direction.
     * @return the spot light
     */
    public Vec spotDirection() {
        return spotDirection;
    }
}
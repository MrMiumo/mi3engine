package io.github.mrmiumo.mi3engine;

/**
 * Tools functions shared for the rendering.
 */
public final class RenderUtils {

    /**
     * Private constructor to prevent usage of default constructor
     */
    private RenderUtils() { }

    /**
     * Finds the smaller value among the 3 given numbers and returns it.0
     * @param a the first number to compare
     * @param b the second number to compare
     * @param c the second number to compare
     * @return the smaller of the 3 numbers
     */
    public static double min(double a,double b,double c) {
        return Math.min(a, Math.min(b, c));
    }

    /**
     * Finds the greater value among the 3 given numbers and returns it.0
     * @param a the first number to compare
     * @param b the second number to compare
     * @param c the second number to compare
     * @return the greater of the 3 numbers
     */
    public static double max(double a,double b,double c) {
        return Math.max(a, Math.max(b, c));
    }

    /**
     * Ensures that the given value falls between the lo and hi values 
     * ​or sets it to the nearest limit.
     * @param v the value to clamp
     * @param lo the lowest allowed value
     * @param hi the highest allowed value
     * @return the clamped value
     */
    public static int clamp(long v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return (int)v;
    }

    /**
     * Ensures that the given value falls between the lo and hi values 
     * ​or sets it to the nearest limit.
     * @param v the value to clamp
     * @param lo the lowest allowed value
     * @param hi the highest allowed value
     * @return the clamped value
     */
    public static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /**
     * Applies lightning to the given argb, and blends it with the
     * given color (if different from 0).
     * @param argb the color to convert
     * @param intensity the intensity of the light to apply
     * @param color the color to blend with
     * @return the transformed argb
     */
    static int transformColor(int argb, double intensity, int color) { // FIXME Hotspot
        int a = (argb >>> 24); // Alpha
        int r = (argb >> 16) & 0xFF;  // Red
        int g = (argb >> 8) & 0xFF;   // Green
        int b = argb & 0xFF;          // Blue

        /* Set lightning */
        r *= intensity;
        g *= intensity;
        b *= intensity;

        var aColor = (color >>> 24);
        if (aColor < 254 && aColor == a) color = 0;

        /* Blend with other color */
        if (a < 255) {
            int ca = (color >>> 24); // Alpha
            int cr = (color >> 16) & 0xFF;  // Red
            int cg = (color >> 8) & 0xFF;   // Green
            int cb = color & 0xFF;          // Blue
            float alpha = a / 255f;
            a = (int) (alpha * 255 + (1 - alpha) * ca);
            r = (int) (r * alpha + cr * (1 - alpha));
            g = (int) (g * alpha + cg * (1 - alpha));
            b = (int) (b * alpha + cb * (1 - alpha));
        }

        /* Merge color */
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

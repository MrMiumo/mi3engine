package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

class TestsUtils {
    public static final Path PACK = Path.of("src/test/resources/assets/minecraft");
    private static final int SIZE = 1287;
    private static boolean firstTest = true;

    /**
     * Tests if the image rendered by the given engine is the same as
     * the reference one.
     * @param engine the engine containing the scene to compare
     * @throws IOException in case of error while reading reference
     */
    public static void assertRender(RenderEngine engine) throws IOException {
        if (firstTest) init();
        var img = engine.render();
        var reference = getTestName() + ".png";
        var path = Path.of("src/test/resources/references/").resolve(reference);
        var ref = ImageIO.read(Files.newInputStream(path));
        if (ref.getHeight() != img.getHeight() || ref.getWidth() != img.getWidth()) {
            fail("Images sizes does not match!", img, path);
        }
        for (var y = 0 ; y < img.getHeight() ; y++) {
            for (var x = 0 ; x < img.getHeight() ; x++) {
                if (img.getRGB(x, y) != ref.getRGB(x, y)) {
                    fail("Color does not match at x:" + x + " y:" + y, img, path);
                }
            }
        }
    }

    /**
     * Tests if the image rendered by the given engine is the same as
     * the reference one. This test relies on {@link #AutoFramer} to
     * ignore the object position and scale.
     * @param engine the engine containing the scene to compare
     * @throws IOException in case of error while reading reference
     * @see #assertRender(RenderEngine)
     */
    public static void assertRenderFramed(RenderEngine engine) throws IOException {
        assertRender(new AutoFramer(engine));
    }

    /**
     * Creates the reference image with the given engine. This method
     * must be called from another method annotated with @Test
     * @param engine the engine to generate the image with
     * @throws IOException in case of error while saving the reference
     */
    public static void generateReference(RenderEngine engine) throws IOException {
        var img = engine.render();
        var reference = getTestName() + ".png";
        var path = Path.of("src/test/resources/references/").resolve(reference);
        Files.deleteIfExists(path);
        ImageIO.write(img, "PNG", Files.newOutputStream(path));
    }

    /**
     * Creates the reference image with the given engine. This method
     * must be called from another method annotated with @Test. This
     * method relies on {@link #AutoFramer} to ignore the object
     * position and scale.
     * @param engine the engine to generate the image with
     * @throws IOException in case of error while saving the reference
     */
    public static void generateReferenceFramed(RenderEngine engine) throws IOException {
        generateReference(new AutoFramer(engine));
        throw new AssertionError("Image got generated. This method should not be called to check if the render is successful!!");
    }

    /**
     * Generates a default render engine with predefined size and
     * camera settings.
     * @return the default engine
     */
    public static RenderEngine newEngine() {
        var engine = new RenderCore(SIZE, SIZE);
        engine.camera()
            .setRotation(25, 35, 0)
            .setTranslation(0, 0)
            .setZoom(1.0)
            .setSpotDirection(new Vec(0, 1, .6))
            .setSpotLight(2f)
            .setAmbientLight(0.5f);
        return engine;
    }




    public static void init() {
        try (var path = Files.walk(Path.of("src/test/resources"))) {
            path.filter(p -> p.getFileName().toString().startsWith("err_"))
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {}
                });
        } catch (IOException e) {}
        firstTest = false;
    }

    /**
     * Finds the name of the calling test method.
     * @return the name of the test method
     * @throws IllegalStateException if no annotated method can be found
     */
    private static String getTestName() {
        var stack = Thread.currentThread().getStackTrace();
        for (var frame : stack) {
            try {
                Method method = Class.forName(frame.getClassName()).getDeclaredMethod(frame.getMethodName());
                if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                    return method.getName();
                }
            } catch (ClassNotFoundException|NoSuchMethodException e) {}
        }
        throw new IllegalStateException("Missing @Test annotated method in stacktrace");
    }

    private static void fail(String msg, BufferedImage actual, Path expected) {
        var test = getTestName();
        var path = Path.of("src/test/resources/err_" + test + ".png");
        try {
            ImageIO.write(actual, "png", Files.newOutputStream(path));
        } catch (IOException e) { e.printStackTrace(); }
        msg = "\n\nTEST FAIL - " + test + "\n"
            + "Expected '" + expected.toAbsolutePath() + "' but was '" + path.toAbsolutePath() + "'\n>>> "
            + msg + "\n";
        var error = new AssertionError(msg);
        error.setStackTrace(new StackTraceElement[]{});
        throw error;
    }
}

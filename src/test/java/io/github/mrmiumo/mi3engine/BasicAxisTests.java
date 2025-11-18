package io.github.mrmiumo.mi3engine;

import static io.github.mrmiumo.mi3engine.TestsUtils.assertRenderFramed;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mrmiumo.mi3engine.Element.Axis;
import io.github.mrmiumo.mi3engine.Element.Face;

public class BasicAxisTests {

    private static final Texture TEXTURE = Texture.generateDefault();

    /**
     * Make sure the bases of the render engine are working fine.
     * This function makes sure each axis works well, basic position
     * and size are also tested for (0-0-0). Any issue with camera
     * or lightning might break this test.
     */
    @Test
    public void testBase() throws IOException {
        var engine = initScene().addElement(getCube().build());
        engine.camera().setRotation(25, 0, 0);
        assertRenderFramed(engine);
    }

    /**
     * Make sure the position of a cube is working fine.
     */
    @Test
    public void testPosition() throws IOException {
        var engine = initScene().addElement(getCube(new Vec(0, 0, 0), null).build());
        engine.camera().setRotation(25, 0, 0);
        assertRenderFramed(engine);
    }

    /**
     * Make sure the position is not affected by the pivot.
     */
    @Test
    public void testPositionPivot() throws IOException {
        var engine = initScene().addElement(getCube().pivot(12, -5, 1).build());
        engine.camera().setRotation(25, 0, 0);
        assertRenderFramed(engine);
    }

    /**
     * Make sure the size of a cube is working fine.
     */
    @Test
    public void testSize() throws IOException {
        var engine = initScene().addElement(getCube(null, new Vec(2, 3, 4)).build());
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure that negative sizes are handled correctly
     */
    @Test
    public void testNegativeSize() throws IOException {
        var engine = initScene().addElement(getCube(null, new Vec(-12, -12, -12)).build());
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the rotation on the X axis is working fine.
     */
    @Test
    public void testRotateX() throws IOException {
        var cube = getCube()
            .pivot(0, 0, 0)
            .rotation(15, Axis.X)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the rotation on the Y axis is working fine.
     */
    @Test
    public void testRotateY() throws IOException {
        var cube = getCube()
            .pivot(0, 0, 0)
            .rotation(15, Axis.Y)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the rotation on the Z axis is working fine.
     */
    @Test
    public void testRotateZ() throws IOException {
        var cube = getCube()
            .pivot(0, 0, 0)
            .rotation(15, Axis.Z)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the rotation on all axis is working fine.
     */
    @Test
    public void testRotateAll() throws IOException {
        var cube = getCube()
            .pivot(0, 0, 0)
            .rotation(new Vec(10, 50, 13))
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }
    
    /**
     * Make sure the X attribute of the pivot point does not affect
     * rotation on X axis
     */
    @Test
    public void testPivotXX() throws IOException {
        var cube = getCube()
            .pivot(15, 0, 0)
            .rotation(15, Axis.X)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the Y attribute of the pivot point does not affect
     * rotation on Y axis
     */
    @Test
    public void testPivotYY() throws IOException {
        var cube = getCube()
            .pivot(0, 15, 0)
            .rotation(15, Axis.Y)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the Y attribute of the pivot point does not affect
     * rotation on Y axis
     */
    @Test
    public void testPivotZZ() throws IOException {
        var cube = getCube()
            .pivot(0, 0, 15)
            .rotation(15, Axis.Z)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }
    
    /**
     * Make sure the Y and Z attribute of the pivot point do affect
     * rotation on X axis correctly
     */
    @Test
    public void testPivotX() throws IOException {
        var cube = getCube()
            .pivot(0, 10, 13)
            .rotation(15, Axis.X)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the X and Z attribute of the pivot point do affect
     * rotation on Y axis correctly
     */
    @Test
    public void testPivotY() throws IOException {
        var cube = getCube()
            .pivot(8, 0, 35)
            .rotation(18, Axis.Y)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the X and Y attribute of the pivot point do affect
     * rotation on Z axis correctly
     */
    @Test
    public void testPivotZ() throws IOException {
        var cube = getCube()
            .pivot(45, 31, 0)
            .rotation(32, Axis.Z)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    /**
     * Make sure the position does not affect the pivot.
     */
    @Test
    public void testPivotPosition() throws IOException {
        var cube = getCube(new Vec(1.6, 1.6, 7), null)
            .pivot(0, 15, 0)
            .rotation(25, Axis.X)
            .build();
        var engine = initScene().addElement(cube);
        engine.camera();
        assertRenderFramed(engine);
    }

    private RenderEngine initScene() throws IOException {
        var engine = TestsUtils.newEngine();

        /* Origin bloc */
        var texture = Texture.from(TestsUtils.PACK.resolve("textures/debug.png"));
        new Cube.Builder(new Vec(-0.25, -0.25, -0.25), new Vec(0.5, 0.5, 0.5))
            .rotation(new Vec(0, 0, 0))
            .pivot(0, 0, 0)
            .texture(Face.NORTH, texture, 0, 0.5f, 4f, 0.75f, 4.25f)
            .texture(Face.EAST, texture, 0, 4f, 0.5f, 4.25f, 0.75f)
            .texture(Face.SOUTH, texture, 0, 0.5f, 0.5f, 0.75f, 0.75f)
            .texture(Face.WEST, texture, 0, 4f, 4f, 4.25f, 4.25f)
            .texture(Face.UP, texture, 0, 7.75f, 0.75f, 7.5f, 0.5f)
            .texture(Face.DOWN, texture, 0, 7.75f, 4f, 7.5f, 4.25f)
            .addTo(engine);

        /* Long bloc */
        new Cube.Builder(new Vec(0, 1, 13), new Vec(2, 2, 9))
            .rotation(new Vec(0, 0, 0))
            .pivot(0, 1, 13)
            .texture(TEXTURE)
            .addTo(engine);
        return engine;
    }

    private Cube.Builder getCube() throws IOException { return getCube(null, null); }

    private Cube.Builder getCube(Vec position, Vec size) throws IOException {
        if (position == null) position = new Vec(1.6, 1.6, 1.6);
        if (size == null) size = new Vec(12.8, 12.8, 12.8);
        var texture = Texture.from(TestsUtils.PACK.resolve("textures/debug.png"));
        return new Cube.Builder(position, size)
            .rotation(new Vec(0, 0, 0))
            .pivot(0, 0, 0)
            .texture(Face.NORTH, texture, 0, 0f, 3.5f, 3.5f, 7f)
            .texture(Face.EAST, texture, 0, 3.5f, 0f, 7f, 3.5f)
            .texture(Face.SOUTH, texture, 0, 0f, 0f, 3.5f, 3.5f)
            .texture(Face.WEST, texture, 0, 3.5f, 3.5f, 7f, 7f)
            .texture(Face.UP, texture, 0, 7f, 0f, 10.5f, 3.5f)
            .texture(Face.DOWN, texture, 0, 7f, 3.5f, 10.5f, 7f);
    }

}

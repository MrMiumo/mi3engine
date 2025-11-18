package io.github.mrmiumo.mi3engine;

import static io.github.mrmiumo.mi3engine.TestsUtils.assertRenderFramed;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mrmiumo.mi3engine.ModelParser.Display;
import io.github.mrmiumo.mi3engine.SkinRender.Slot;

public class SkinRenderTests {

    /**
     * Make sure that the hat follows correctly head movements.
     */
    @Test
    public void testEquipHeadSize() throws IOException {
        var model = getEquipment("headSize.json")
            .setDisplay(Display.Type.HEAD, new Display(null, new Vec(0, 12.8, 0), null));
        var engine = getEngine()
            .head(new Vec(-25, -80, 35))
            .equip(Slot.HEAD, model);
        assertRenderFramed(engine);
    }

    /**
     * Same as {@link #testEquipHeadSize} but making sure the size is
     * working well.
     */
    @Test
    public void testEquipHeadScale() throws IOException {
        var model = getEquipment("headScale.json")
            .setDisplay(Display.Type.HEAD, new Display(null, new Vec(0, 12.8, 0), new Vec(2, 2, 2)));
        var engine = getEngine()
            .head(new Vec(-25, -80, 35))
            .equip(Slot.HEAD, model);
        assertRenderFramed(engine);
    }

    /**
     * Make sure that the model rotation is kept while rotating the head
     */
    @Test
    public void testEquipHeadRotation() throws IOException {
        var model = getEquipment("headSize.json")
            .setDisplay(Display.Type.HEAD, new Display(new Vec(25, 45, 12).localToGlobal(), null, null));
        var engine = getEngine()
            .head(new Vec(-25, -80, 35))
            .equip(Slot.HEAD, model);
        assertRenderFramed(engine);
    }

    /**
     * Test with the real hat Demo1
     */
    @Test
    public void testEquipHeadDemo1() throws IOException {
        var model = getEquipment("hatDemo1.json");
        var engine = getEngine()
            .head(new Vec(5, 8, -3))
            .equip(Slot.HEAD, model);
        assertRenderFramed(engine);
    }

    /**
     * Test with the real hat Demo2
     */
    @Test
    public void testEquipHeadDemo2() throws IOException {
        var model = getEquipment("hatDemo2.json");
        var engine = getEngine()
            .head(new Vec(5, -8, -3))
            .equip(Slot.HEAD, model);
        assertRenderFramed(engine);
    }


    private static ModelParser getEquipment(String file) throws IOException {
        return new ModelParser(new Group().asEngine())
            .parse(TestsUtils.PACK.resolve("models/" + file));
    }

    private static SkinRender getEngine() throws IOException {
        var skin = TestsUtils.PACK.resolve("textures/skinHead.png");
        var engine = new SkinRender(RenderEngine.from(1287, 1287), skin);
        engine.camera()
            .setAmbientLight(0.35f)
            .setSpotLight(7)
            .setSpotDirection(new Vec(-1, 1, -.15))
            .setRotation(25, 35, 0);
        return engine;
    }
}

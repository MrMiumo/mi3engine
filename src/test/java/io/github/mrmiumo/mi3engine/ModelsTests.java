package io.github.mrmiumo.mi3engine;

import static io.github.mrmiumo.mi3engine.TestsUtils.assertRenderFramed;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ModelsTests {

    /**
     * Make sure that the hat follows correctly head movements.
     */
    @Test
    public void testInverted() throws IOException {
        var engine = getEngine("cubeInverted.json");
        engine.camera().setRotation(25, 35, 0);
        assertRenderFramed(engine);
    }

    private static ModelParser getEngine(String model) throws IOException {
        return new ModelParser(RenderEngine.from(1287, 1287))
            .parse(TestsUtils.PACK.resolve("models/" + model));
    }
}

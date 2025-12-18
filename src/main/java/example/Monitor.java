package example;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import io.github.mrmiumo.mi3engine.AutoFramer;
import io.github.mrmiumo.mi3engine.Group;
import io.github.mrmiumo.mi3engine.ModelParser;
import io.github.mrmiumo.mi3engine.RenderEngine;
import io.github.mrmiumo.mi3engine.SkinRender;
import io.github.mrmiumo.mi3engine.SkinRender.Slot;
import io.github.mrmiumo.mi3engine.Vec;

/**
 * Very basic monitor to visualize engine output in real time and
 * manipulate the model in space.
 */
public class Monitor {

    /** No need to instantiate this class!! */
    private Monitor() {}

    /**
     * Opens the monitor to view and manipulate object.
     * @param args unused
     * @throws IOException in case of parsing error
     * @throws InterruptedException in case of error while sleeping
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        var pack = Path.of("src/test/resources/assets/minecraft");
        // benchModels(pack);
        // benchSkin(pack);

        /* Choose the engine */
        // var engine = testModel(pack);
        var engine = testSkin(pack);
        
        /* Monitor launching */
        new Preview<SkinRender>(engine)
            // .print(Path.of("out_transparency.png"))
            .start(Monitor::animateSkin)
            ;
    }

    private static void animateSkin(SkinRender engine, long time) {
        var t = (int)((time * 2) % (45 + 180 + 90));
        int x = 0;
        int y = 0;
        int z = 0;
        if (t <= 45) {
            z = -t;
            x = 0;
            y = 0;
        } else if (t <= 45 + 158) {
            t -= 45;
            z = -45;
            x = 2 * t;
            y = 0;
        } else if (t <= 45 + 158 + 90) {
            t -= 45 + 158;
            z = -45;
            x = -45;
            y = -t;
        }

        engine.rightArm(new Vec(x, y, z), 0);
        engine.leftArm(new Vec(x, y, z), 0);
        engine.rightLeg(new Vec(x, y, z));
        engine.leftLeg(new Vec(x, y, z));
    }

    private static void benchModels(Path pack) throws IOException {
        System.out.print("Loading models...");
        var models = Files.list(pack)
            .filter(Files::isDirectory)
            .flatMap(d -> {
                try { return Files.walk(d); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            })
            .filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".json"))
            .sorted(Comparator.comparingLong(f -> {
                try { return Files.size(f); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            }))
            .limit(50)
            .map(f -> {
                try { return new ModelParser(RenderEngine.from(1287, 1287)).parse(f); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            })
            .toList();
        var out = new BufferedImage[models.size()];
        System.out.print("\rRendering models...");
        var start = System.currentTimeMillis();
        for (var i = 0 ; i < models.size() ; i++) {
            out[i] = new AutoFramer(models.get(i)).render();
        }
        System.out.println("\rModel Bench done in " + (System.currentTimeMillis() - start) + "ms");
        var s = Arrays.stream(out).map(String::valueOf).map(b -> b.charAt(0) + "").collect(Collectors.joining());
        System.out.println(s);
    }


    private static void benchSkin(Path pack) throws IOException {
        var engine = testSkin(pack);
        var out = new BufferedImage[50];
        var start = System.currentTimeMillis();
        for (var i = 0 ; i < out.length ; i++) {
            out[i] = new AutoFramer(engine).render();
        }
        System.out.println("Skin Bench done in " + (System.currentTimeMillis() - start) + "ms");
        var s = Arrays.stream(out).map(String::valueOf).map(b -> b.charAt(0) + "").collect(Collectors.joining());
        System.out.println(s);
    }

    /**
     * Loads a model from a file and sets the camera settings
     * @param pack the pack to get the model from
     * @return a Model engine
     * @throws IOException in case of error while reading the file
     */
    public static ModelParser testModel(Path pack) throws IOException {
        var engine = new ModelParser(RenderEngine.from(1287, 1287));
        engine.parse(pack.resolve("assets/minecraft/models/item/myModel.json"));
        engine.camera()
            .setRotation(25, -145, 0)
            .setTranslation(0, 0)
            .setZoom(0.4);
        return engine;
    }

    /**
     * Loads a skin and equip a model
     * @param pack the pack to get the skin and model from
     * @return a Skin engine
     * @throws IOException in case of error while reading the files
     */
    public static SkinRender testSkin(Path pack) throws IOException {
        var path = pack.resolve("textures/debugSkin.png");
        var skinEngine = new SkinRender(RenderEngine.from(1287, 1287), path);
        skinEngine.camera()
            .setAmbientLight(0.35f)
            .setSpotLight(7)
            .setSpotDirection(new Vec(-1, 1, -.15))
            .setRotation(5, 45, 0);
        skinEngine.head(new Vec(-5, -8, 3))
            .rightArm(new Vec(-70, 75, 30), 75)
            .leftArm(new Vec(0, 25, 10), 35)
            .rightLeg(new Vec(5, -20, -5))
            .leftLeg(new Vec(25, -10, 15));

        var hat = new ModelParser(new Group().asEngine())
            .parse(pack.resolve("models/hatDemo1.json"));
        skinEngine.equip(Slot.HEAD, hat);
        return skinEngine;
    }
}

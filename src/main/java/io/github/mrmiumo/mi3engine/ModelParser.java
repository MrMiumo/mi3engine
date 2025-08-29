package io.github.mrmiumo.mi3engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mrmiumo.mi3engine.Cube.Axis;
import io.github.mrmiumo.mi3engine.Cube.Face;

/**
 * Parse a model file inside a Minecraft resource pack: loads the
 * textures, generates the cubes and prepare them to be rendered with
 * the engine.<p>
 * IMPORTANT: you should (must) define a property 'default.minecraft.pack'
 * in your application.properties file to give the path to a folder
 * corresponding to the default minecraft resource pack.
 */
public class ModelParser {

    /** Lists of all textures with their ID and image */
    private final HashMap<String, Texture> textures = new HashMap<>();

    /** Lists of all elements that composed the model (cubes) */
    private final ArrayList<Cube> elements = new ArrayList<>();

    /** The mapper used to decode JSON */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Path of the textures folder in the pack being parsed */
    private Path texturesFolder;

    /** Path of the default textures if set in 'default.minecraft.pack' */
    private final Path defaultTextures = loadProperty();

    /** Iterator over each default image colorSet available */
    private static int nextColorSet = 0;

    /**
     * Parses the given model file. A valid model file is well formed
     * JSON that respect Minecraft model rules and is located in a
     * resource pack.
     * @param file the model file to parse
     * @return this parser
     * @throws IOException in case of error while parsing the JSON
     */
    public ModelParser parse(Path file) throws IOException {
        var data = Files.readString(file);
        texturesFolder = getTexturesFolder(file);

        var json = MAPPER.readTree(data);
        parseTextures(json.get("textures"));
        json.get("elements").elements().forEachRemaining(element -> parseElement(element));
        return this;
    }

    /**
     * Add all parsed cubes to the given engine and returns it.
     * @param engine the engine to add the cubes to
     * @return the given engine
     */
    public RenderEngine addAll(RenderEngine engine) {
        engine.addCubes(elements);
        return engine;
    }



    /* ************************************************************ *\
     *                           TEXTURES                           *
    \* ************************************************************ */

    /**
     * Fills the {@link #textures} collection with the images loaded
     * from the given JSON file.
     * @param json the "textures" json node from the model file
     */
    private void parseTextures(JsonNode json) {
        json.fields().forEachRemaining(node -> {
            if (!"particle".equals(node.getKey())) {
                try {
                    textures.put("#" + node.getKey(), loadTexture(node.getValue().asText()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    /**
     * Loads the textures with the given name in the current pack or
     * in the default vanilla textures.
     * @param name the name of the texture such as 'block/bricks'
     * @return the texture or null if not found
     * @throws IOException in case of error while reading the files
     */
    private Texture loadTexture(String name) throws IOException {

        /* Try with the pack texture first */
        if (texturesFolder != null) {
            var path = texturesFolder.resolve(name + ".png");
            if (Files.exists(path)) return new Texture(ImageIO.read(Files.newInputStream(path)));
        }

        /* Try with vanilla textures then */
        if (defaultTextures != null) {
            var path = defaultTextures.resolve(name + ".png");
            if (Files.exists(path)) return new Texture(ImageIO.read(Files.newInputStream(path)));
        }
        
        /* Texture not found */
        return defaultTexture();
    }

    /**
     * Generates a random colored default texture
     * @return the default texture
     */
    private static Texture defaultTexture() {
        final var colorsSets = new int[][]{
            new int[]{ 0x62cc82, 0x5abb78, 0x58b675, 0x50a66a },      // Green
            new int[]{ 0xcc84aa, 0xbb799c, 0xb67698, 0xa66c8b },      // Pink
            new int[]{ 0xcc7c79, 0xbb726f, 0xb66f6c, 0xa66562 },      // Red
            new int[]{ 0x81bccc, 0x77acbb, 0x74a8b6, 0x6a99a6 },      // Blue
            new int[]{ 0xccc67a, 0xbbb670, 0xb6b16d, 0xa6a264 }       // Gold
        };
        var colors = colorsSets[nextColorSet];
        nextColorSet = (nextColorSet++) % colorsSets.length;
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        var canvas = image.getGraphics();

        /* Set background color */
        canvas.setColor(new Color(colors[1]));
        canvas.fillRect(0, 0, 16, 16);

        /** Add check board */
        canvas.setColor(new Color(colors[2]));
        for (var i = 0 ;  i < 15 * 15 ; i += 2) {
            canvas.fillRect(i % 15 + 1, i / 15 + 1, 1, 1);
        }

        /** Add light edges and icon */
        canvas.setColor(new Color(colors[0]));
        canvas.fillRect(0, 0, 1, 15);
        canvas.fillRect(0, 0, 15, 1);
        canvas.fillRect(4, 8, 2, 5);
        canvas.fillRect(7, 10, 2, 3);
        canvas.fillRect(10, 3, 2, 10);

        /** Add dark edges */
        canvas.setColor(new Color(colors[3]));
        canvas.fillRect(15, 1, 1, 15);
        canvas.fillRect(1, 15, 15, 1);

        canvas.dispose();
        return new Texture(image);
    }

    /**
     * Tries to find the textures folder from the Minecraft pack
     * containing the given model file.
     * @param file the model file to find the textures folder from
     * @return the path of the textures folder or null
     */
    private static Path getTexturesFolder(Path file) {
        var pathStr = file.toAbsolutePath().toString().replace("\\", "/");
        var pos = pathStr.lastIndexOf("assets/minecraft/");
        if (pos == -1) {
            return null; // The given file is not located in a valid Minecraft Pack
        }
        var texturesPath = Path.of(pathStr.substring(0, pos + 16)).resolve("textures");
        if (!Files.isDirectory(texturesPath)) {
            return null; // The textures folder is missing
        }
        return texturesPath;
    }


    /* ************************************************************ *\
     *                           ELEMENTS                           *
    \* ************************************************************ */

    public static int angle = 0;

    /**
     * Converts the given "elements" child into a Cube and adds it to
     * the {@link #elements} collection.
     * @param element the "element" json node from the model file
     */
    private void parseElement(JsonNode element) {

        /* From node */
        var from = parseVector(element.get("from"));

        /* To node */
        var to = parseVector(element.get("to"));

        var cube = Cube.from(from, to);

        /* Rotation node */
        var rotation = element.get("rotation");
        if (rotation != null) {
            var angle = rotation.get("angle").asDouble();
            var axis = Axis.valueOf(rotation.get("axis").asText().toUpperCase());
            cube.rotation(angle, axis);

            var origin = rotation.get("origin");
            if (origin != null) {
                cube.origin(parseVector(origin));
            }
        }

        /* Faces (textures) */
        var faces = element.get("faces");
        if (faces != null) {
            faces.fields().forEachRemaining(node -> {
                var face = Face.valueOf(node.getKey().toUpperCase());
                var uv = parseUvs(node.getValue().get("uv"));
                var rotate = parseTextureRotation(node.getValue());
                var textureId = node.getValue().get("texture").asText();
                var texture = textures.computeIfAbsent(textureId, s -> defaultTexture());
                cube.texture(face, texture, rotate, uv.get(0), uv.get(1), uv.get(2), uv.get(3));
            });
        }

        elements.add(cube.build());
    }

    /**
     * Gets the "rotation" attribute from the given face definition or
     * returns the default 0 value.
     * @param node the face node to get the rotation from
     * @return the rotation
     */
    private static int parseTextureRotation(JsonNode node) {
        var rotation = node.get("rotation");
        if (rotation != null) {
            return rotation.asInt() / 90;
        }
        return 0;
    }

    /**
     * Reads the given json node that contains 3 numbers and creates
     * a vector from it.
     * @param node the node containing the number
     * @return the corresponding vector
     */
    private static Vec parseVector(JsonNode node) {
        var elements = node.elements();
        return new Vec(
            elements.next().asDouble(),
            elements.next().asDouble(),
            elements.next().asDouble()
        );
    }

    /**
     * Reads and format an array of number from a json node.
     * @param node the node containing the array elements
     * @return the list of the numbers
     */
    private static List<Float> parseUvs(JsonNode node) {
        var elements = node.elements();
        var list = new ArrayList<Float>();
        elements.forEachRemaining(d -> {
            list.add((float)d.asDouble());
        });
        return list;
    }

    /**
     * Loads the 'default.minecraft.pack' property from the file
     * application.properties if available.
     * @return the path of the default pack if set and valid, null otherwise
     */
    private static Path loadProperty() {
        try (InputStream in = ClassLoader.getSystemResourceAsStream("application.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                var prop = props.getProperty("default.minecraft.pack");
                if (prop == null) return null;
                var path = Path.of(prop).resolve("assets/minecraft/textures");
                return Files.exists(path) ? path : null;
            }
        } catch (IOException e) {}
        return null;
    }
}

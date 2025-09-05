package io.github.mrmiumo.mi3engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
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
public class ModelParser extends RenderTool {

    /** The mapper used to decode JSON */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Path of the default textures if set in 'default.minecraft.pack' */
    private static Path defaultTextures = loadProperty();

    /** Lists of all textures with their ID and image */
    private final HashMap<String, TextureHolder> textures = new HashMap<>();

    /** Path of the textures folder in the pack being parsed */
    private Path texturesFolder;

    /**
     * Creates and initializes a new parser. Don't forget to setup
     * the 'default.minecraft.pack' in your application.properties!
     * @param engine the engine to use to generate the model image
     */
    public ModelParser(RenderEngine engine) {
        super(engine);
    }

    /**
     * Parses the given model file. A valid model file is well formed
     * JSON that respect Minecraft model rules and is located in a
     * resource pack.
     * @param file the model file to parse
     * @return this parser
     * @throws IOException in case of error while parsing the JSON
     */
    public ModelParser parse(Path file) throws IOException {
        textures.clear();

        engine.clear();
        parseInternal(file);
        return this;
    }

    /**
     * Internal method that parses the given file without resetting
     * anything.
     * @param file the file to parse
     * @throws IOException in case of error while reading the file
     */
    private void parseInternal(Path file) throws IOException {
        var data = Files.readString(file);
        texturesFolder = getTexturesFolder(file);

        var json = MAPPER.readTree(data);
        parseTextures(json.get("textures"));
        
        var parent = json.get("parent");
        var elements = json.get("elements");
        if (elements == null && parent != null) {
            /* Inheritance: parse the parent and override textures */
            var path = file.toAbsolutePath().toString()
                .replace("\\", "/")
                .replaceFirst("assets/minecraft/models/.*", "assets/minecraft/models/");
            parseInternal(Path.of(path).resolve(parent.asText() + ".json"));
        } else if (elements != null) {
            /* Normal model */
            json.get("elements").elements().forEachRemaining(element -> parseElement(element));
        }
    }

    @Override
    public BufferedImage render() {
        return engine.render();
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
        json.properties().stream()
            .filter(p -> !"particle".equals(p.getKey()))
            .forEach(p -> {
                var value = p.getValue().asText();
                Function<String, TextureHolder> mapper;
                if (value.startsWith("#")) {
                    mapper = k -> new TextureHolder(value);
                } else {
                    mapper = k -> {
                        try {
                            return new TextureHolder(loadTexture(value));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    };
                }
                textures.computeIfAbsent("#" + p.getKey(), mapper);
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
            if (Files.exists(path)) return Texture.from(name, ImageIO.read(Files.newInputStream(path)));
        }

        /* Try with vanilla textures then */
        if (defaultTextures != null) {
            var path = defaultTextures.resolve(name + ".png");
            if (Files.exists(path)) return Texture.from(name, ImageIO.read(Files.newInputStream(path)));
        }
        
        /* Texture not found */
        return Texture.generateDefault();
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

    /**
     * Converts the given "elements" child into a Cube and adds it to
     * the engine's collection.
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
            faces.properties().iterator().forEachRemaining(node -> {
                var face = Face.valueOf(node.getKey().toUpperCase());
                var uv = parseUvs(node.getValue().get("uv"));
                var rotate = parseTextureRotation(node.getValue());
                var textureId = node.getValue().get("texture").asText();
                var texture = textures.computeIfAbsent(textureId, s -> new TextureHolder(Texture.generateDefault()));
                if (texture.get() != null) {
                    cube.texture(face, texture.get(), rotate, uv.get(0), uv.get(1), uv.get(2), uv.get(3));
                }
            });
        }

        engine.addCube(cube.build());
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

    /**
     * Sets the path of the default resource pack folder. If the
     * 'default.minecraft.pack' property is already defined, no need
     * to use this function!
     * @param location the path of the default minecraft resource pack
     */
    public static void setDefaultPack(Path location) {
        if (location == null) return;
        location = location.resolve("assets/minecraft/textures");
        if (!Files.exists(location)) return;
        defaultTextures = location;
    }

    private class TextureHolder {
        /** The real texture if available */
        private final Texture texture;

        /** Another texture ID if the real texture is missing */
        private final String id;

        /**
         * Creates a new texture holder containing a real texture.
         * @param texture the texture to insert in the holder
         */
        public TextureHolder(Texture texture) {
            this.texture = texture;
            this.id = null;
        }

        /**
         * Creates a new texture holder containing texture reference.
         * @param id the ID of the texture to refer to
         */
        public TextureHolder(String id) {
            this.texture = null;
            this.id = id;
        }

        /**
         * Gets the texture contained in this holder. If this holder
         * contains a reference to a missing texture or if the texture
         * has been set to null, this method can return null!
         * @return the texture or null
         */
        public Texture get() {
            if (texture != null) return texture;
            var reference = textures.get(id);
            return reference == null ? null : reference.get();
        }
    }
}

package io.github.mrmiumo.mi3engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mrmiumo.mi3engine.Element.Axis;
import io.github.mrmiumo.mi3engine.Element.Face;

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

    /** Lists of all defined display settings */
    private final HashMap<Display.Type, Display> display = new HashMap<>();

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
        if (elements == null && parent != null && "item/generated".equals(parent.asText())) {
            /* Special parent that generates a model from a single texture */
            parseGenerated();
        } else if (elements == null && parent != null) {
            /* Inheritance: parse the parent and override textures */
            var path = file.toAbsolutePath().toString()
                .replace("\\", "/")
                .replaceFirst("assets/minecraft/models/.*", "assets/minecraft/models/");
            parseInternal(Path.of(path).resolve(parent.asText() + ".json"));
        } else if (elements != null) {
            /* Normal model */
            json.get("elements").elements().forEachRemaining(element -> parseElement(element));
        }
        parseDisplay(json.get("display"));
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
        if (json == null) return;
        json.properties().stream()
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
            if (Files.exists(path)) return Texture.from(path);
        }

        /* Try with vanilla textures then */
        if (defaultTextures != null) {
            var path = defaultTextures.resolve(name + ".png");
            if (Files.exists(path)) return Texture.from(path);
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
                cube.pivot(parseVector(origin));
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

        engine.addElement(cube.build());
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
     * Parse a model using 'item/generated' as a parent. This special
     * parent creates a model out of a single image. This image will
     * be extruded to have a depth of 1.
     */
    private void parseGenerated() {
        var texture = textures.get("#layer0").get();
        if (texture == null) return; // Invalid!

        var pixels = texture.pixels();
        int height = texture.source().getHeight();
        int width = texture.source().getWidth();
        var depth = height / 16;

        var covered = new boolean[height * width];
        var solid = new int[width * height];
        for (var i = 0 ; i < solid.length ; i++) {
            solid[i] = (pixels[i] >>> 24) / 5;
        }

        for (var y = 0 ; y < height ; y++) {
            for (var x = 0 ; x < width ; x++) {
                int offset = y * width + x;
                var alpha = solid[offset];
                if (alpha == 0 || covered[offset]) continue;

                /* Find the widest possible rectangle */
                var recW = 0;
                while (x + recW < width && !covered[offset + recW] && solid[offset + recW] == alpha) {
                    recW++;
                }

                /* Expand downwards */
                int recH = 1;
                int minW = recW;
                while (y + recH < height) {
                    int rowStart = (y + recH) * width + x;
                    var lineOk = true;
                    for (var i = 0 ; i < minW && lineOk ; i++) {
                        if (covered[rowStart + i] || solid[rowStart + i] != alpha) lineOk = false;
                    }
                    if (!lineOk) break;
                    recH++;
                }

                engine.addElement(generateCube(texture, x, y, recW, recH, depth));

                /* Mark all pixels covered by this cube */
                for (int i = y; i < y + recH; i++) {
                    Arrays.fill(covered, i * width + x, i * width + x + recW, true);
                }
                x += minW - 1;
            }
        }
    }

    /**
     * Builds a new textures cube part of a generated item.
     * @param texture the texture to apply on the cube
     * @param x the x coordinate of the cube
     * @param y the y coordinate of the cube
     * @param w the width of the cube to create
     * @param h the height of the cube to create
     * @param depth the depth of the cube (z axis)
     * @return the cube!
     */
    private static Cube generateCube(Texture texture, int x, int y, int w, int h, int depth) {
        var width = texture.source().getWidth();
        var height = texture.source().getHeight();

        var u = 16f / width;
        var v = 16f / height;

        var cube = Cube.from(new Vec(x, - y, 0), new Vec(x + w, -y - h, depth))
            .texture(Face.NORTH, texture, 0, (x + w) * u, y * v, x * u, (y + h) * v)
            .texture(Face.EAST,  texture, 0, (x + w - 1) * u, y * v, (x + w) * u, (y + h) * v)
            .texture(Face.DOWN,  texture, 0, x * u, (y + h) * v, (x + w) * u, (y + h - 1) * v)
            .texture(Face.SOUTH, texture, 0, x * u, y * v, (x + w) * u, (y + h) * v)
            .texture(Face.WEST,  texture, 0, x * u, y * v, (x + 1) * u, (y + h) * v)
            .texture(Face.UP,    texture, 0, x * u, y * v, (x + w) * u, (y + 1) * v);
        return cube.build();
    }


    /* ************************************************************ *\
     *                           DISPLAY                            *
    \* ************************************************************ */

    /**
     * Parse the given display section of the current model. This
     * section defines how the model should be positioned when displayed
     * in the inventory, containers, gui, as equipment, ...
     * @param json the content of the "display" json node
     * @return the list of display with their types
     * @throws IOException in case the display section is malformed
     */
    private void parseDisplay(JsonNode json) throws IOException {
        if (json == null) return;
        json.properties().stream().forEach(d -> {
            var key = Display.Type.from(d.getKey());
            if (key == null) return;
            display.put(key, Display.from(d.getValue()));
        });
    }

    /**
     * Gets the display settings associated with the given slot.
     * @param type the slot to get display settings for
     * @return the display settings or {@link Display#NULL} if not specified in the model
     */
    public Display getDisplay(Display.Type type) {
        return display.getOrDefault(type, Display.NULL);
    }

    /**
     * Sets a custom display for the given slot
     * @param type the type of display to configure (slot)
     * @param value the display settings
     * @return this parser
     */
    public ModelParser setDisplay(Display.Type type, Display value) {
        if (type == null) return this;
        if (display == null) {
            display.remove(type);
        } else {
            display.put(type, value);
        }
        return this;
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

    /**
     * Stores the displays information of a minecraft model.
     */
    public record Display(Vec rotation, Vec pivot, Vec translation, Vec scale) {
        public static final Display NULL = new Display(Vec.ZERO, Vec.ZERO, new Vec(1, 1, 1));
        
        public Display {
            if (rotation == null) rotation = Vec.ZERO;
            if (translation == null) translation = Vec.ZERO;
            if (scale == null) scale = new Vec(1, 1, 1);
        }
        
        public Display(Vec rotation, Vec translation, Vec scale) {
            this(rotation, translation, translation, scale);
        }

        /**
         * Parse a json object and creates the corresponding Display.
         * @param json the json child containing display attributes
         * @return the display
         */
        public static Display from(JsonNode json) {
            var rotation = vecFrom(json.get("rotation"));
            if (rotation != null) rotation = rotation.localToGlobal();
            return new Display(
                rotation,
                vecFrom(json.get("translation")),
                vecFrom(json.get("scale"))
            );
        }

        private static Vec vecFrom(JsonNode json) {
            if (json == null) return null;
            var vals = json.elements();
            return new Vec(
                vals.next().asDouble(),
                vals.next().asDouble(),
                vals.next().asDouble())
            ;
        }

        public enum Type {
            THIRD_PERSON_RIGHT("thirdperson_righthand"),
            THIRD_PERSON_LEFT("thirdperson_lefthand"),
            FIRST_PERSON_RIGHT("firstperson_righthand"),
            FIRST_PERSON_LEFT("firstperson_lefthand"),
            HEAD("head"),
            GROUND("ground"),
            FRAME("fixed"),
            SHELF("on_shelf"),
            GUI("gui");

            private final String key;
            Type(String key) { this.key = key; }

            /**
             * Gets the Type enum entry that corresponds to the given
             * JSON key (from a valid model file, in the "display" section)
             * @param s the key to get the type from
             * @return the type or null if the given key is not known
             */
            public static Type from(String s) {
                for (var t : values()) {
                    if (t.key.equals(s)) return t;
                }
                return null;
            }
        }
    }
}

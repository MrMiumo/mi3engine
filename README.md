# Miumo's Minimalist Minecraft rendering engine
**Mi<sup>3</sup> Engine** is a very basic and lightweight rendering engine written in Java intended to render Minecraft elements such as resource pack models.
## Performances
Performances are not adapted to video or live preview but are still suitable for efficient image generation. Generating an image of 1287x1287 for a large model containing 400 cubes takes about 100ms (depends of the hardware of course).
## Usage
This engine is written in Java 17 (maybe compatible with Java 15 but not tested). It can be added to your project using Maven:
```xml
<dependency>
    <groupId>io.github.mrmiumo</groupId>
    <artifactId>mi3engine</artifactId>
    <version>1.0.0</version>
</dependency>
```
Once imported in your project, you can use it by yourself (creating and adding cubes one by one) or using tools.
### Manual Usage
This technique enabled you to have full control over what is used to generate your images. It is adapted when no tool already exist for your use case.</br>
Here is a small example:
```java
/* Step 1. Create the engine */
RenderEngine engine = new RenderEngine(1920, 1080);

/* Step 2. Setup the camera */
engine.camera()
    .setRotation(25, -145, 0)
    .setTranslation(1, 0)
    .setZoom(0.32);

/* Step 3. Load your textures */
BufferedImage img = ImageIO.read(Files.newInputStream("myTexture.png"));
Texture texture = new Texture(img);

/* Step 4. Create the cubes */
Cube.from(new Vec(16, 0, 16), new Vec(32, 8,  32))
    .rotation(0, Axis.Y)
    .pivot(8, 0, 16)
    .texture(Face.NORTH, texture, 0, 8, 16, 16, 0)
    .texture(Face.EAST, texture, 0, 8, 16, 16, 1)
    .texture(Face.SOUTH, texture, 0, 8, 16, 16, 1)
    .texture(Face.WEST, texture, 0, 8, 16, 16, 1)
    .texture(Face.UP, texture, 0, 0, 16, 16, 1)
    .texture(Face.DOWN, texture, 0, 0, 16, 16, 0)
    .render(engine);

/* Step 5. Render the image */
BufferedImage output = engine.render();

/* Step 6. Save the image */
ImageIO.write(output, "PNG", Files.newOutputStream("MyImage.png"));
```

### Using Tools
This method make the rendering way much easier. Tools are built-in classes that enabled to generate images for some Minecraft components such as pack models. Here is an example:

```java
/* Step 1. Create the engine */
RenderEngine engine = new RenderEngine(1287, 1287);

/* Step 2. Setup the camera */
engine.camera()
    .setRotation(25, -145, 0)
    .setTranslation(1, 0)
    .setZoom(0.32);

/* Step 3. Create a new tool to load models from a resource pack */
ModelParser parser = new ModelParser();

/* Step 4. Use the tool to convert a file and collect the created cubes */
parser.parse(pack.resolve("assets/minecraft/models/item/boat.json"));
parser.addAll(engine);

/* Step 5. Render the image */
BufferedImage output = engine.render();

/* Step 6. Save the image */
ImageIO.write(output, "PNG", Files.newOutputStream("MyImage.png"));
```
**IMPORTANT**: When using the `ModelParser`, you must define a property called 'default.minecraft.pack' in your `application.properties` file to give the path to a folder corresponding to the default minecraft resource pack. If your project does not have such a file yet, add it in the `src/main/resources` folder and add the line:
```properties
default.minecraft.pack=/myApp/assets/minecraftDefaultTextures
```
This configuration is important since most models rely on default game's textures and those are not embedded in the engine.
## Future
A player module is planned to be added to create custom poses and render skins!
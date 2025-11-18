package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import io.github.mrmiumo.mi3engine.Element.Face;

/**
 * Orthographic headless renderer for textures cubes. Each cube can
 * have customized texture with UVs for each face. Also transparent
 * textures are supported!<p>
 * This minimalist engine designed for Minecraft is primarily designed
 * to render models as BlockBench would render them.
 * @author Miumo
 */
public class SkinRender extends RenderTool {

    /** The offset used to compute the second layer */
    private final double OFFSET = .25;

    /** The offset used to compute the second layer "from" point */
    private final Vec FROM_OFFSET = new Vec(-OFFSET, -OFFSET, -OFFSET);

    /** The offset used to compute the second layer "to" point */
    private final Vec TO_OFFSET = new Vec(OFFSET, OFFSET, OFFSET);

    /** The skin texture */
    private final Texture skin;

    /** The arm UV to handle regular and skim skins */
    private final float[] marks;

    /**
     * Array containing all skin elements (mutable) that enable to
     * modify some skin parts without recomputing all the elements.
     * Organization:
     * - [0] HEAD base layer
     * - [1] HEAD second layer
     * - [2] BODY base layer
     * - [3] BODY second layer
     * - [4 : 9] RIGHT ARM base layer
     * - [10:15] RIGHT ARM second layer
     * - [16:21] LEFT ARM base layer
     * - [22:27] LEFT ARM second layer
     * - [28] RIGHT LEG base layer
     * - [29] RIGHT LEG second layer
     * - [30] LEFT LEG base layer
     * - [31] LEFT LEG second layer
     */
    private final Element[] parts = new Element[32];

    /**
     * Creates a new engine based on the given one. This is a tool to
     * generates skin render with custom poses.
     * @param engine the engine to rely on (not another SkinRender)
     * @param skin the path of the skin to display
     * @throws IOException in case of error while reading the skin
     */
    public SkinRender(RenderEngine engine, Path skin) throws IOException {
        super(engine);
        if (!Files.isRegularFile(skin)) {
            throw new IllegalArgumentException("The skin must be a regular file");
        }

        this.skin = Texture.from(skin);
        this.marks = getSkinMarks(this.skin);
        body();
    }
    
    @Override
    public BufferedImage render() {
        engine.clear();
        lazyInit();
        for (var part : parts) {
            if (part == null) continue;
            engine.addElement(part);
        }
        return engine.render();
    }

    /**
     * Initializes any body part that has not been configured yet.
     */
    private void lazyInit() {
        final var zero = new Vec(0, 0, 0);
        if (parts[0]  == null) head(zero);
        if (parts[4]  == null) rightArm(zero, 0);
        if (parts[16] == null) leftArm(zero, 0);
        if (parts[28] == null) rightLeg(zero);
        if (parts[30] == null) leftLeg(zero);
    }

    /**
     * Configures the head rotation using the given angles.
     * @param rotation the rotation of the head
     * @return this skin engine
     */
    public SkinRender head(Vec rotation) {
        var from = new Vec(0, 12, -2);
        var to = new Vec(8, 20, 6);

        /* Base layer */
        parts[0] = Cube.from(from, to)
            .pivot(4, 12, 2)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, 2, 0, 4, 2)
            .texture(Face.DOWN,  skin, 2, 6, 0, 4, 2)
            .texture(Face.WEST,  skin, 0, 0, 2, 2, 4)
            .texture(Face.SOUTH, skin, 0, 2, 2, 4, 4)
            .texture(Face.EAST,  skin, 0, 4, 2, 6, 4)
            .texture(Face.NORTH, skin, 0, 6, 2, 8, 4)
            .build();

        /* Second layer */
        parts[1] = Cube.from(from.add(FROM_OFFSET), to.add(TO_OFFSET))
            .pivot(4, 12, 2)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, 10, 0, 12, 2)
            .texture(Face.DOWN,  skin, 2, 14, 0, 12, 2)
            .texture(Face.WEST,  skin, 0, 8,  2, 10, 4)
            .texture(Face.SOUTH, skin, 0, 10, 2, 12, 4)
            .texture(Face.EAST,  skin, 0, 12, 2, 14, 4)
            .texture(Face.NORTH, skin, 0, 14, 2, 16, 4)
            .build();

        return this;
    }

    /**
     * Create the body cube, adds UVs to each face using {@link #skin}
     * then adds the result to the current engine scene.
     * @return this skin engine
     */
    private SkinRender body() {
        var from = new Vec(0, 0, 0);
        var to = new Vec(8, 12, 4);

        /* Base layer */
        parts[2] = Cube.from(from, to)
            .texture(Face.UP,    skin, 0, 5, 4,  7, 5)
            .texture(Face.DOWN,  skin, 2, 9, 4,  7, 5)
            .texture(Face.WEST,  skin, 0, 4, 5,  5, 8)
            .texture(Face.SOUTH, skin, 0, 5, 5,  7, 8)
            .texture(Face.EAST,  skin, 0, 7, 5,  8, 8)
            .texture(Face.NORTH, skin, 0, 8, 5, 10, 8)
            .build();

        /* Second layer */
        parts[3] = Cube.from(from.add(FROM_OFFSET), to.add(TO_OFFSET))
            .texture(Face.UP,    skin, 0, 5, 8,  7, 9)
            .texture(Face.DOWN,  skin, 2, 9, 8,  7, 9)
            .texture(Face.WEST,  skin, 0, 4, 9,  5, 12)
            .texture(Face.SOUTH, skin, 0, 5, 9,  7, 12)
            .texture(Face.EAST,  skin, 0, 7, 9,  8, 12)
            .texture(Face.NORTH, skin, 0, 8, 9, 10, 12)
            .build();

        return this;
    }

    /**
     * Rotate the right leg using the given angles.
     * @param rotation the rotation of the leg to set
     * @return this skin engine
     */
    public SkinRender rightLeg(Vec rotation) {
        return leg(true, rotation);
    }

    /**
     * Rotate the left leg using the given angles.
     * @param rotation the rotation of the leg to set
     * @return this skin engine
     */
    public SkinRender leftLeg(Vec rotation) {
        return leg(false, rotation);
    }

    /**
     * Creates the elements needed to create a leg having the given
     * rotation.
     * @param true to create the left leg, false for the left one
     * @param rotation the rotation of the leg to set
     * @return this skin engine
     */
    private SkinRender leg(boolean right, Vec rotation) {
        var from = new Vec(right ? 0 : 4, -12, 0);
        var to = new Vec(right ? 4 : 8, 0, 4);
        var i = right ? 0 : 2;

        /* Base layer */
        var tx = right ? 0 : 4;
        var ty = right ? 4 : 12;
        parts[28 + i] = Cube.from(from, to)
            .pivot(right ? 2 : 6, 0, 2)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, tx + 1, ty + 0, tx + 2, ty + 1)
            .texture(Face.DOWN,  skin, 2, tx + 3, ty + 0, tx + 2, ty + 1)
            .texture(Face.WEST,  skin, 0, tx + 0, ty + 1, tx + 1, ty + 4)
            .texture(Face.SOUTH, skin, 0, tx + 1, ty + 1, tx + 2, ty + 4)
            .texture(Face.EAST,  skin, 0, tx + 2, ty + 1, tx + 3, ty + 4)
            .texture(Face.NORTH, skin, 0, tx + 3, ty + 1, tx + 4, ty + 4)
            .build();

        /* Second layer */
        ty = right ? 8 : 12;
        parts[29 + i] = Cube.from(from.add(FROM_OFFSET), to.add(TO_OFFSET))
            .pivot(right ? 2 : 6, 0, 2)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, 1, ty + 0, 2, ty + 1)
            .texture(Face.DOWN,  skin, 2, 3, ty + 0, 2, ty + 1)
            .texture(Face.WEST,  skin, 0, 0, ty + 1, 1, ty + 4)
            .texture(Face.SOUTH, skin, 0, 1, ty + 1, 2, ty + 4)
            .texture(Face.EAST,  skin, 0, 2, ty + 1, 3, ty + 4)
            .texture(Face.NORTH, skin, 0, 3, ty + 1, 4, ty + 4)
            .build();

        return this;
    }

    /**
     * Rotate the right arm using the given angles. It is also possible
     * to set the arm bending with a value in degrees between 0 and 180.
     * @param rotation the rotation of the leg to set
     * @param bending the angle of the arm bending (0 for a straight arm)
     * @return this skin engine
     */
    public SkinRender rightArm(Vec rotation, double bending) {
        return createArm(new Vec(0, 8, 0), rotation, bending);
    }

    /**
     * Rotate the left arm using the given angles. It is also possible
     * to set the arm bending with a value in degrees between 0 and 180.
     * @param rotation the rotation of the leg to set
     * @param bending the angle of the arm bending (0 for a straight arm)
     * @return this skin engine
     */
    public SkinRender leftArm(Vec rotation, double bending) {
        return createArm(new Vec(8, 8, 0), rotation, bending);
    }

    private SkinRender createArm(Vec pos, Vec rot, double angle) {
        angle /= 8;

        var right = pos.x() == 0;
        rot = new Vec(-rot.x(), rot.y(), rot.z());
        Cube root = addShoulder(pos, rot, right);

        var convert = RenderEngine.modelToWorld(root);
        var aRad = angle * (Math.PI / 180.0);
        var b = Math.cos(aRad) * 4;
        var a = Math.tan(aRad) * b;

        var offset = new Vec(0, 0, 0);
        for (var i = 0 ; i < 4 ; i++) {
            var height = (30 - angle) / 30;
            addSkinPart(root, convert, angle, right, a, b, i, offset, height);

            var oRad = angle * (i * 2 + 1) * (Math.PI / 180.0);
            offset = offset.add(0, Math.cos(oRad) * height, -Math.sin(oRad) * height);
        }

        /* Hand */
        addHand(rot, angle, convert, offset, right);

        return this;
    }

    /**
     * Creates, places and add UVS to the arm shoulder to the engine.
     * @param pos the position of the whole arm
     * @param rot the rotation of the whole arm
     * @param right true to add right arm shoulder, false for the left arm
     * @return the element
     */
    private Cube addShoulder(Vec pos, Vec rot, boolean right) {
        var i = right ? 4 : 16;
        var sizeX = marks[1] != 1 ? 3 : 4;
        var position = right ? pos.add(-sizeX, 0, 0) : pos;
        var size     = new Vec(sizeX, 4, 4);
        var pivot    = pos.add(0, 4, 2);

        /* Base layer */
        var tx = right ? 10 : 8;
        var ty = right ? 4 : 12;
        var root = new Cube.Builder(position, size)
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    skin.uv(marks[0] +tx, ty + 0, marks[1],  1, 3))
            .texture(Face.WEST,  skin.uv(marks[4] +tx, ty + 1, marks[5],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[6] +tx, ty + 1, marks[7],  1, 0))
            .texture(Face.EAST,  skin.uv(marks[8] +tx, ty + 1, marks[9],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[10]+tx, ty + 1, marks[11], 1, 1))
            .build();
        parts[i] = root;

        /* Second layer */
        tx = right ? 10 : 12;
        ty = right ? 8 : 12;
        parts[i + 6] = new Cube.Builder(position.add(-OFFSET, 0, -OFFSET), size.add(TO_OFFSET).add(TO_OFFSET))
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    skin.uv(marks[0] +tx, ty + 0, marks[1],  1, 3))
            .texture(Face.WEST,  skin.uv(marks[4] +tx, ty + 1, marks[5],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[6] +tx, ty + 1, marks[7],  1, 0))
            .texture(Face.EAST,  skin.uv(marks[8] +tx, ty + 1, marks[9],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[10]+tx, ty + 1, marks[11], 1, 1))
            .build();

        return root;
    }

    /**
     * Creates, place and add UVs to one part of an arm bent to the engine.
     * @param root the root cube of the arm
     * @param convert function to rotate the hand following the root angle
     * @param angle the bending of the arm in degrees
     * @param right true to add right arm part, false for the left arm
     * @param a the amount of tapper to set to fill the bent
     * @param b the size of the trapezoid back side to compensate for the tapper
     * @param i the index of this part (from 0 to 3)
     * @param offset the offset to position this part, taking previous
     *     ones into account.
     * @param height the height of this part
     */
    private void addSkinPart(
        Cube root, Function<Vec, Vec> convert, double angle, boolean right,
        double a, double b, int i, Vec offset, double height
    ) {
        var slim = marks[1] != 1;
        var partAngle = angle * (i * 2 + 1);
        var position = convert.apply(new Vec(0, 0, 4).sub(offset));
        var pivot = position;
        var rotation = root.rotation().add(180 - partAngle, 0, 0);
        var u = right ? 5 : 17;

        /* Base layer */
        float tx = right ? 10 : 8;
        float ty = (right ? 6 : 14) + i * 0.25f;
        parts[u + i] = new Trapezoid(
            new Vec(slim ? 3 : 4, height, b), // size
            position,
            rotation,
            pivot,
            new Vec(0, a, 0), // taper
            new Texture[] {
                skin.uv(marks[10]+tx, ty, marks[11], 0.25f, 2), // south
                skin.uv(marks[6] +tx, ty, marks[7] , 0.25f, 3), // north
                null, null,
                skin.uv(marks[8]+tx, ty, marks[9], 0.25f, 3), // west
                skin.uv(marks[4]+tx, ty, marks[5], 0.25f, 2), // east
            }
        );

        /* Second layer */
        tx = right ? 10 : 12;
        ty = (right ? 10 : 14) + i * 0.25f;
        position = convert.apply(new Vec(0, 0, 4).sub(offset).add(-OFFSET, 0, OFFSET));
        pivot = position;
        var aRad = angle * (Math.PI / 180.0);
        b = Math.cos(aRad) * (4 + 2*OFFSET);
        a = Math.tan(aRad) * b;
        parts[u + i + 6] = new Trapezoid(
            new Vec(slim ? 3 : 4, height, b).add(2*OFFSET, 0, 0), // size
            position,
            rotation,
            pivot,
            new Vec(0, a, 0), // taper
            new Texture[]{
                skin.uv(marks[10]+tx, ty, marks[11], 0.25f, 2), // south
                skin.uv(marks[6] +tx, ty, marks[7] , 0.25f, 3), // north
                null, null,
                skin.uv(marks[8]+tx, ty, marks[9], 0.25f, 3), // west
                skin.uv(marks[4]+tx, ty, marks[5], 0.25f, 2), // east
            }
        );
    }

    /**
     * Adds the hand on the arm. The hand adapts to the rest of the arm
     * following overall movements and arm bending.
     * @param rot the rotation of the whole arm
     * @param angle the bending of the arm in degrees
     * @param convert function to rotate the hand following the root angle
     * @param offset the offset of the position due to arm bending
     * @param right true to add right arm part, false for the left arm
     */
    private void addHand(Vec rot, double angle, Function<Vec, Vec> convert, Vec offset, boolean right) {
        var slim = marks[1] != 1;
        var size = new Vec(slim ? 3 : 4, 4, 4);
        var partAngle = angle * (3.5 * 2 + 1);
        rot = rot.add(180 - partAngle, 0, 0);
        var position = convert.apply(new Vec(0, 0, 4).sub(offset));
        var pivot = position;
        var i = right ? 9 : 21;

        /* Base layer */
        var tx = right ? 10 : 8;
        var ty = right ? 4 : 12;
        parts[i] = new Cube.Builder(position, size)
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,  skin,2, marks[3] + marks[2] +tx, ty + 0, marks[2] +tx, ty+1)
            .texture(Face.WEST,  skin.uv(marks[4] +tx, ty + 3, marks[5],  1, 2))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 3, marks[7],  1, 3))
            .texture(Face.EAST,  skin.uv(marks[8] +tx, ty + 3, marks[9],  1, 3))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 3, marks[11], 1, 2))
            .build();

        /* Second layer */
        tx = right ? 10 : 12;
        ty = right ? 8 : 12;
        position = convert.apply(new Vec(0, 0, 4).sub(offset).add(-OFFSET, 0, OFFSET));
        pivot = position;
        parts[i + 6] = new Cube.Builder(position, size.add(new Vec(2*OFFSET, OFFSET, 2*OFFSET)))
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,  skin,2, marks[3] + marks[2] +tx, ty + 0, marks[2] +tx, ty+1)
            .texture(Face.WEST,  skin.uv(marks[4] +tx, ty + 3, marks[5],  1, 2))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 3, marks[7],  1, 3))
            .texture(Face.EAST,  skin.uv(marks[8] +tx, ty + 3, marks[9],  1, 3))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 3, marks[11], 1, 2))
            .build();
    }

    /**
     * Computes the arms relative UVs depending on the size of the skin
     * (slim or regular).
     * @param skin the skin to compute marks for
     * @return an array containing offset and size on the X axis for
     *     the top, bottom, left, front, right and back faces.
     */
    private static float[] getSkinMarks(Texture skin) {
        var slim = (skin.source().getRGB(55, 20) >>> 24) <= 10;
        if (slim) {
            return new float[]{
                1,     .75f, // up
                1.75f, .75f, // down
                0,      1,   // left
                1,     .75f, // front
                1.75f,  1,   // right
                2.75f, .75f  // back
            };
        } else {
            return new float[]{
                1, 1, // up
                2, 1, // down
                0, 1, // left
                1, 1, // front
                2, 1, // right
                3, 1  // back
            };
        }
    }
}

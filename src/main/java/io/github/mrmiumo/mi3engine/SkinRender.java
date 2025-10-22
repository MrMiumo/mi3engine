package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

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
        return createArm(new Vec(-4, 8, 0), rotation, bending);
    }

    private SkinRender createArm(Vec pos, Vec rot, double angle) {
        angle /= 8;

        Element root = addShoulder(pos, rot);

        var convert = rotateAroundPivot(root);
        var aRad = angle * (Math.PI / 180.0);
        var b = Math.cos(aRad) * 4;
        var a = Math.tan(aRad) * b;

        var offset = new Vec(0, 0, 0);
        for (var i = 0 ; i < 4 ; i++) {
            var height = (30 - angle) / 30;
            addSkinPart(root, convert, pos, angle, a, b, i, offset, height);

            var oRad = angle * (i * 2 + 1) * (Math.PI / 180.0);
            offset = offset.add(new Vec(0, Math.cos(oRad) * height, Math.sin(oRad) * height));
        }

        /* Hand */
        addHand(pos, rot, angle, convert, offset);

        return this;
    }

    /**
     * Creates, places and add UVS to the arm shoulder to the engine.
     * @param pos the position of the whole arm
     * @param rot the rotation of the whole arm
     * @return the element
     */
    private Element addShoulder(Vec pos, Vec rot) {
        var slim = marks[1] != 1;
        var size = new Vec(slim ? 3 : 4, 4, 4);
        var i = pos.x() == 0 ? 4 : 16;
        var flip = new Vec(0, 180, 0);
        var px = pos.x() == 0 ? 0 : (slim ? -3.5 : -4);

        /* Base layer */
        var tx = pos.x() == 0 ? 10 : 8;
        var ty = pos.x() == 0 ? 4 : 12;
        Element root = new Cube.Builder(pos, size)
            .pivot(new Vec(px, pos.y() - 4, 2))
            .rotation(rot.add(flip))
            .texture(Face.UP,    skin.uv(marks[0] +tx, ty + 0, marks[1],  1, 1))
            .texture(Face.EAST,  skin.uv(marks[4] +tx, ty + 1, marks[5],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 1, marks[7],  1, 1))
            .texture(Face.WEST,  skin.uv(marks[8] +tx, ty + 1, marks[9],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 1, marks[11], 1, 0))
            .build();
        parts[i] = root;

        /* Second layer */
        tx = pos.x() == 0 ? 10 : 12;
        ty = pos.x() == 0 ? 8 : 12;
        parts[i + 6] = new Cube.Builder(pos.add(new Vec(OFFSET, 0, -OFFSET)), size.add(TO_OFFSET).add(TO_OFFSET))
            .pivot(new Vec(px-OFFSET, pos.y() - 4, 2+OFFSET))
            .rotation(rot.add(flip))
            .texture(Face.UP,    skin.uv(marks[0] +tx, ty + 0, marks[1],  1, 1))
            .texture(Face.EAST,  skin.uv(marks[4] +tx, ty + 1, marks[5],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 1, marks[7],  1, 1))
            .texture(Face.WEST,  skin.uv(marks[8] +tx, ty + 1, marks[9],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 1, marks[11], 1, 0))
            .build();

        return root;
    }

    /**
     * Creates, place and add UVs to one part of an arm bent to the engine.
     * @param root the root cube of the arm
     * @param convert function to rotate the hand following the root angle
     * @param pos the position of the whole arm
     * @param angle the bending of the arm in degrees
     * @param a the amount of tapper to set to fill the bent
     * @param b the site of the trapezoid to compensate for the tapper
     * @param i the index of this part (from 0 to 3)
     * @param offset the offset to position this part, taking previous
     *     ones into account.
     * @param height the height of this part
     */
    private void addSkinPart(
        Element root, Adapter convert, Vec pos, double angle,
        double a, double b, int i, Vec offset, double height
    ) {
        var slim = marks[1] != 1;
        var partAngle = angle * (i * 2 + 1);
        var pivot = new Vec(0, height, 0);
        var position = pos.add(convert.rotate(new Vec(0, -height, 0).sub(offset), pivot));
        var u = pos.x() == 0 ? 5 : 17;

        /* Base layer */
        float tx = pos.x() == 0 ? 10 : 8;
        float ty = (pos.x() == 0 ? 6 : 14) + i * 0.25f;
        parts[u + i] = new Trapezoid(
            new Vec(slim ? 3 : 4, height, b), // size
            position,
            new Vec(partAngle, 0, 0).add(root.rotation()),
            pivot,
            new Vec(0, a, 0), // taper
            new Texture[] {
                skin.uv(marks[10]+tx, ty, marks[11], 0.25f, 0), // south
                skin.uv(marks[6] +tx, ty, marks[7] , 0.25f, 1), // north
                null, null,
                skin.uv(marks[4]+tx, ty, marks[5], 0.25f, 1), // east
                skin.uv(marks[8]+tx, ty, marks[9], 0.25f, 0), // west
            }
        );

        /* Second layer */
        tx = pos.x() == 0 ? 10 : 12;
        ty = (pos.x() == 0 ? 10 : 14) + i * 0.25f;
        var posOffset = new Vec(OFFSET, 0, -OFFSET);
        var sizeOffset = new Vec(2*OFFSET, 0, 0);
        position = pos.add(convert.rotate(new Vec(0, -height, 0).add(posOffset).sub(offset), pivot));
        var aRad = angle * (Math.PI / 180.0);
        b = Math.cos(aRad) * (4 + 2*OFFSET);
        a = Math.tan(aRad) * b;
        parts[u + i + 6] = new Trapezoid(
            new Vec(slim ? 3 : 4, height, b).add(sizeOffset), // size
            position,
            new Vec(partAngle, 0, 0).add(root.rotation()),
            pivot,
            new Vec(0, a, 0), // taper
            new Texture[]{
                skin.uv(marks[10]+tx, ty, marks[11], 0.25f, 0), // south
                skin.uv(marks[6] +tx, ty, marks[7] , 0.25f, 1), // north
                null, null,
                skin.uv(marks[4]+tx, ty, marks[5], 0.25f, 1), // east
                skin.uv(marks[8]+tx, ty, marks[9], 0.25f, 0), // west
            }
        );
    }

    /**
     * Adds the hand on the arm. The hand adapts to the rest of the arm
     * following overall movements and arm bending.
     * @param pos the position of the whole arm
     * @param rot the rotation of the whole arm
     * @param angle the bending of the arm in degrees
     * @param convert function to rotate the hand following the root angle
     * @param offset the offset of the position due to arm bending
     */
    private void addHand(Vec pos, Vec rot, double angle, Adapter convert, Vec offset) {
        var slim = marks[1] != 1;
        var size = new Vec(slim ? 3 : 4, 4, 4);
        var partAngle = angle * (3.5 * 2 + 1);
        var pivot = new Vec(0, 4, 0);
        var position = pos.add(convert.rotate(new Vec(0, -4, 0).sub(offset), pivot));
        var rotFix = new Vec(partAngle, 180, 0);
        var i = pos.x() == 0 ? 9 : 21;

        var tx = pos.x() == 0 ? 10 : 8;
        var ty = pos.x() == 0 ? 4 : 12;
        parts[i] = new Cube.Builder(position, size)
            .pivot(pivot)
            .rotation(rot.add(rotFix))
            .texture(Face.DOWN,  skin,0, marks[3] + marks[2] +tx, ty + 0, marks[2] +tx, ty+1)
            .texture(Face.EAST,  skin.uv(marks[4] +tx, ty + 3, marks[5],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 3, marks[7],  1, 1))
            .texture(Face.WEST,  skin.uv(marks[8] +tx, ty + 3, marks[9],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 3, marks[11], 1, 0))
            .build();

        /* Second layer */
        tx = pos.x() == 0 ? 10 : 12;
        ty = pos.x() == 0 ? 8 : 12;
        pivot = new Vec(0, 4 + OFFSET, 0);
        position = pos.add(convert.rotate(new Vec(0, -4, 0).sub(offset).add(new Vec(OFFSET, -OFFSET, -OFFSET)), pivot));
        parts[i + 6] = new Cube.Builder(position, size.add(new Vec(2*OFFSET, OFFSET, 2*OFFSET)))
            .pivot(pivot)
            .rotation(rot.add(rotFix))
            .texture(Face.DOWN,  skin,0, marks[3] + marks[2] +tx, ty + 0, marks[2] +tx, ty+1)
            .texture(Face.EAST,  skin.uv(marks[4] +tx, ty + 3, marks[5],  1, 1))
            .texture(Face.NORTH, skin.uv(marks[6] +tx, ty + 3, marks[7],  1, 1))
            .texture(Face.WEST,  skin.uv(marks[8] +tx, ty + 3, marks[9],  1, 0))
            .texture(Face.SOUTH, skin.uv(marks[10]+tx, ty + 3, marks[11], 1, 0))
            .build();
    }

    private static Adapter rotateAroundPivot(Element cube) {
        double rx = Math.toRadians(cube.rotation().x());
        double ry = Math.toRadians(cube.rotation().y());
        double rz = Math.toRadians(cube.rotation().z());
        double cosX = Math.cos(rx), sinX = Math.sin(rx);
        double cosY = Math.cos(ry), sinY = Math.sin(ry);
        double cosZ = Math.cos(rz), sinZ = Math.sin(rz);

        return (Vec pos, Vec pivot) -> {
            double x = (pos.x() + pivot.x()) - cube.pivot().x();
            double y = (pos.y() + pivot.y()) - cube.pivot().y();
            double z = (pos.z() + pivot.z()) - cube.pivot().z();

            // Rotate around X axis
            double x1 = x;
            double y1 = y * cosX - z * sinX;
            double z1 = y * sinX + z * cosX;
            // Rotate around Y axis
            double x2 = x1 * cosY + z1 * sinY;
            double y2 = y1;
            double z2 = -x1 * sinY + z1 * cosY;
            // Rotate around Z axis
            double x3 = x2 * cosZ - y2 * sinZ;
            double y3 = x2 * sinZ + y2 * cosZ;
            double z3 = z2;

            // Translate back
            return new Vec(
                x3 + cube.pivot().x() - pivot.x(),
                y3 + cube.pivot().y() - pivot.y(),
                z3 + cube.pivot().z() - pivot.z()
            );
        };
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

    @FunctionalInterface
    private interface Adapter {
        public Vec rotate(Vec pos, Vec pivot);
    }
}

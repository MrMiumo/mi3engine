package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

import io.github.mrmiumo.mi3engine.Element.Face;
import io.github.mrmiumo.mi3engine.ModelParser.Display;
import io.github.mrmiumo.mi3engine.ModelParser.Display.Type;

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

    /** List of items to put in the player slots with their positions */
    private final EnumMap<Slot, ModelParser> equipments = new EnumMap<>(Slot.class);

    /** The position and rotation of each slot */
    private final EnumMap<Slot, Display> slotsDisplay = new EnumMap<>(Slot.class);

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
     * - [28:33] RIGHT LEG base layer
     * - [34:39] RIGHT LEG second layer
     * - [40:45] LEFT LEG base layer
     * - [46:51] LEFT LEG second layer
     */
    private final Element[] parts = new Element[52];

    /**
     * Creates a new engine based on the given skin. This is a tool to
     * generates skin render with custom poses.
     * @param engine the engine to rely on (not another SkinRender)
     * @param skin the path of the skin to display
     * @throws IOException in case of error while reading the skin
     */
    public SkinRender(RenderEngine engine, Path skin) throws IOException {
        super(engine);
        engine.setDoubleSided(true);
        if (!Files.isRegularFile(skin)) {
            throw new IllegalArgumentException("The skin must be a regular file");
        }

        this.skin = Texture.from(skin);
        body();
    }

    /**
     * Creates a new engine based on the given skin. This is a tool to
     * generates skin render with custom poses.
     * @param engine the engine to rely on (not another SkinRender)
     * @param skin the path of the skin to display
     * @throws IOException in case of error while reading the skin
     */
    public SkinRender(RenderEngine engine, BufferedImage skin) throws IOException {
        super(engine);
        engine.setDoubleSided(true);
        this.skin = Texture.from(skin);
        body();
    }
    
    @Override
    public BufferedImage render() {
        build();
        return engine.render();
    }

    /**
     * Clear the scene and generate all cubes.
     * This is usually done lazily but it can need to be done manually
     * some time to be used with AutoFramer for example.
     * @return this skinRender
     */
    protected SkinRender build() {
        engine.clear();
        lazyInit();
        for (var part : parts) {
            if (part == null) continue;
            engine.addElement(part);
        }
        for (var equipment : equipments.entrySet()) {
            var slotDisplay = slotsDisplay.get(equipment.getKey());
            if (slotDisplay == null) continue;
            var engine = equipment.getValue();
            if (engine == null) continue;
            addEquipment(equipment.getKey(), slotDisplay, engine);
        }
        return this;
    }

    @Override
    public List<Element> getElements() {
        if (super.getElements().isEmpty()) build();
        return super.getElements();
    }

    private void addEquipment(Slot slot, Display slotDisplay, ModelParser engine) {
        Group dGroup;
        if (engine.engine instanceof Group.DummyEngine dummy) {
            dGroup = dummy.group();
        } else {
            dGroup = new Group().add(engine.getElements());
        }
        
        /* Display group */
        var d = engine.getDisplay(Type.HEAD);
        dGroup.position(new Vec(-8, -8, -8));
        dGroup.pivot(Vec.ZERO);
        dGroup.rotate(new Vec(d.rotation().x(), -d.rotation().y(), d.rotation().z()));

        /* Slot group */
        var scaleFactor = 0.625;
        var sGroup = new Group().add(dGroup.getElements());
        sGroup.position(d.translation().mul(scaleFactor).add(slotDisplay.translation()));
        sGroup.pivot(slotDisplay.pivot());
        sGroup.rotate(new Vec(-slotDisplay.rotation().x(), 180 - slotDisplay.rotation().y(), slotDisplay.rotation().z()));
        sGroup.scale(d.scale().mul(scaleFactor).mul(slotDisplay.scale()));

        this.engine.addElements(sGroup.getElements());
    }

    /**
     * Initializes any body part that has not been configured yet.
     */
    private void lazyInit() {
        final var zero = new Vec(0, 0, 0);
        if (parts[0]  == null) head(zero);
        if (parts[4]  == null) rightArm(zero, 0);
        if (parts[16] == null) leftArm(zero, 0);
        if (parts[28] == null) rightLeg(zero, 0);
        if (parts[40] == null) leftLeg(zero, 0);
    }

    /**
     * Configures the head rotation using the given angles.
     * @param rotation the rotation of the head
     * @return this skin engine
     */
    public SkinRender head(Vec rotation) {
        var pivot = new Vec(4, 12, 2);
        var from = new Vec(0, 12, -2);
        var to = new Vec(8, 20, 6);

        /* Base layer */
        parts[0] = Cube.from(from, to)
            .pivot(pivot)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, 2, 0, 4, 2)
            .texture(Face.DOWN,  skin, 2, 6, 0, 4, 2)
            .texture(Face.WEST,  skin, 0, 0, 2, 2, 4)
            .texture(Face.SOUTH, skin, 0, 2, 2, 4, 4)
            .texture(Face.EAST,  skin, 0, 4, 2, 6, 4)
            .texture(Face.NORTH, skin, 0, 6, 2, 8, 4)
            .build();

        /* Second layer */
        parts[1] = Cube.from(from.add(-.5, -.5, -.5), to.add(.5, .5, .5))
            .pivot(pivot)
            .rotation(rotation)
            .texture(Face.UP,    skin, 0, 10, 0, 12, 2)
            .texture(Face.DOWN,  skin, 2, 14, 0, 12, 2)
            .texture(Face.WEST,  skin, 0, 8,  2, 10, 4)
            .texture(Face.SOUTH, skin, 0, 10, 2, 12, 4)
            .texture(Face.EAST,  skin, 0, 12, 2, 14, 4)
            .texture(Face.NORTH, skin, 0, 14, 2, 16, 4)
            .build();

        slotsDisplay.put(Slot.HEAD, new Display(rotation, pivot, pivot.add(0, 4, 0), null));
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
     * @param bending the angle of the leg bending (0 for a straight leg)
     * @return this skin engine
     */
    public SkinRender rightLeg(Vec rotation, double bending) {
        return addMember(Member.RIGHT_LEG, rotation, bending);
    }

    /**
     * Rotate the left leg using the given angles.
     * @param rotation the rotation of the leg to set
     * @param bending the angle of the leg bending (0 for a straight leg)
     * @return this skin engine
     */
    public SkinRender leftLeg(Vec rotation, double bending) {
        rotation = rotation.mul(new Vec(1, -1, -1));
        return addMember(Member.LEFT_LEG, rotation, bending);
    }

    /**
     * Rotate the right arm using the given angles. It is also possible
     * to set the arm bending with a value in degrees between 0 and 180.
     * @param rotation the rotation of the leg to set
     * @param bending the angle of the arm bending (0 for a straight arm)
     * @return this skin engine
     */
    public SkinRender rightArm(Vec rotation, double bending) {
        return addMember(Member.RIGHT_ARM, rotation, bending);
    }

    /**
     * Rotate the left arm using the given angles. It is also possible
     * to set the arm bending with a value in degrees between 0 and 180.
     * @param rotation the rotation of the leg to set
     * @param bending the angle of the arm bending (0 for a straight arm)
     * @return this skin engine
     */
    public SkinRender leftArm(Vec rotation, double bending) {
        rotation = rotation.mul(new Vec(1, -1, -1));
        return addMember(Member.LEFT_ARM, rotation, bending);
    }

    /**
     * Sets the given model to the skin slot. This will equip a custom
     * 3D model in the hand or armor slot.<p>
     * To un-equip, simply set the slot but keep the model to null
     * @param slot the slot to put the model on
     * @param model the model to set.
     * @return this skin engine
     */
    public SkinRender equip(Slot slot, ModelParser model) {
        if (model == null) equipments.remove(slot);
        else equipments.put(slot, model);
        return this;
    }

    private SkinRender addMember(Member part, Vec rot, double angle) {
        angle /= 8;

        if (part.leg()) rot = zxyToZyx(rot).add(0, 180, 0);
        else rot = zxyToZyx(rot).mul(new Vec(-1, 1, 1));
        Cube root = addShoulder(part, rot);

        var convert = RenderEngine.modelToWorld(root);
        var aRad = angle * (Math.PI / 180.0);
        var baseB = Math.cos(aRad) * 4;
        var baseA = Math.tan(aRad) * baseB;
        var layerB = Math.cos(aRad) * (4 + 2*OFFSET);
        var layerA = Math.tan(aRad) * layerB;

        var baseOffset = new Vec(0, 0, 0);
        var layerOffset = new Vec(0, 0, 0);
        for (var i = 0 ; i < 4 ; i++) {
            var baseHeight = (30 - angle) / 30;
            var layerHeight = baseHeight - 2 * (OFFSET * layerA / layerB);
            addBendPart(
                root, convert, angle, part, i,
                baseA, baseB, baseOffset, baseHeight,
                layerA, layerB, layerOffset, layerHeight
            );

            var oRad = angle * (i * 2 + 1) * (Math.PI / 180.0);
            baseOffset = baseOffset.add(0, Math.cos(oRad) * baseHeight, -Math.sin(oRad) * baseHeight);
            layerOffset = layerOffset.add(0, Math.cos(oRad) * layerHeight, -Math.sin(oRad) * layerHeight);
        }

        /* Hand */
        addHand(rot, angle, convert, baseOffset, layerOffset, part);
        return this;
    }

    /**
     * Creates, places and add UVS to the arm shoulder to the engine.
     * @param part the part being build (used to save element + load UV)
     * @param rot the rotation of the whole arm
     * @return the element
     */
    private Cube addShoulder(Member part, Vec rot) {
        var sizeX = part.slim(skin) ? 3 : 4;
        var position = part == Member.RIGHT_ARM ? part.pos.add(-sizeX, 0, 0) : part.pos;
        var size     = new Vec(sizeX, 4, 4);
        var pivot    = part.pivot;
        var rA = 0;
        var rB = 1;

        /* Base layer */
        var root = new Cube.Builder(position, size)
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    part.uvBase(Face.UP,    skin, 0, 1, 3, 1))
            .texture(Face.WEST,  part.uvBase(Face.WEST,  skin, 1, 1, rA, rB))
            .texture(Face.NORTH, part.uvBase(Face.SOUTH, skin, 1, 1, rA, rB))
            .texture(Face.EAST,  part.uvBase(Face.EAST,  skin, 1, 1, rA, rB))
            .texture(Face.SOUTH, part.uvBase(Face.NORTH, skin, 1, 1, rA, rB))
            .build();
        parts[part.base] = root;

        /* Second layer */
        parts[part.layer] = new Cube.Builder(position.add(-OFFSET, 0, -OFFSET), size.add(TO_OFFSET).add(TO_OFFSET))
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    part.uvLayer(Face.UP,    skin, 0, 1, 3, 1))
            .texture(Face.WEST,  part.uvLayer(Face.WEST,  skin, 1, 1, rA, rB))
            .texture(Face.NORTH, part.uvLayer(Face.SOUTH, skin, 1, 1, rA, rB))
            .texture(Face.EAST,  part.uvLayer(Face.EAST,  skin, 1, 1, rA, rB))
            .texture(Face.SOUTH, part.uvLayer(Face.NORTH, skin, 1, 1, rA, rB))
            .build();

        return root;
    }

    /**
     * Creates, place and add UVs to one part of an arm bent to the engine.
     * @param root the root cube of the arm
     * @param convert function to rotate the hand following the root angle
     * @param angle the bending of the arm in degrees
     * @param part the part being build (used to save element + load UV)
     * @param i the index of this part (from 0 to 3)
     * @param baseA the amount of tapper to set to fill the bent
     * @param baseB the size of the trapezoid back side to compensate for the tapper
     * @param baseOffset the offset to position this part, taking previous
     *     ones into account (base offset)
     * @param baseHeight the height of this part
     * @param layerA the amount of tapper for the layer
     * @param layerB the size of the trapezoid back side for the layer
     * @param layerOffset variant of the offset computed for the layer
     * @param layerHeight variant of the height computed for the layer
     */
    private void addBendPart(
        Cube root, Function<Vec, Vec> convert, double angle, Member part, int i,
        double baseA, double baseB, Vec baseOffset, double baseHeight,
        double layerA, double layerB, Vec layerOffset, double layerHeight
    ) {
        var slim = part.slim(skin);
        var partAngle = angle * (i * 2 + 1);
        var position = convert.apply(new Vec(0, 0, 4).sub(baseOffset));
        var pivot = position;
        var rotation = root.rotation().add(180 - partAngle, 0, 0);
        var rA = 3;
        var rB = 2;

        /* Base layer */
        var y = 2 + i * 0.25f;
        parts[part.base + 1 + i] = new Trapezoid(
            new Vec(slim ? 3 : 4, baseHeight, baseB), // size
            position,
            rotation,
            pivot,
            new Vec(0, baseA, 0), // taper
            new Texture[] {
                part.uvBase(Face.SOUTH, skin, y, .25f, rA, rB),
                part.uvBase(Face.NORTH, skin, y, .25f, rA, rB),
                null, null,
                part.uvBase(Face.WEST,  skin, y, .25f, rA, rB),
                part.uvBase(Face.EAST,  skin, y, .25f, rA, rB)
            }
        );

        /* Second layer */
        y = 2 + i * 0.25f;
        position = convert.apply(new Vec(0, 0, 4).sub(layerOffset).add(-OFFSET, 0, OFFSET));
        pivot = position;
        parts[part.layer + 1 + i] = new Trapezoid(
            new Vec(slim ? 3 : 4, layerHeight, layerB).add(2*OFFSET, 0, 0), // size
            position,
            rotation,
            pivot,
            new Vec(0, layerA, 0), // taper
            new Texture[]{
                part.uvLayer(Face.SOUTH, skin, y, .25f, rA, rB),
                part.uvLayer(Face.NORTH, skin, y, .25f, rA, rB),
                null, null,
                part.uvLayer(Face.WEST,  skin, y, .25f, rA, rB),
                part.uvLayer(Face.EAST,  skin, y, .25f, rA, rB)
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
     * @param layerOffset the offset of the position due to arm bending for the layer
     * @param part the part being build (used to save element + load UV)
     */
    private void addHand(Vec rot, double angle, Function<Vec, Vec> convert, Vec offset, Vec layerOffset, Member part) {
        var slim = part.slim(skin);
        var size = new Vec(slim ? 3 : 4, 4, 4);
        var partAngle = angle * (3.5 * 2 + 1);
        rot = rot.add(180 - partAngle, 0, 0);
        var position = convert.apply(new Vec(0, 0, 4).sub(offset));
        var pivot = position;
        var rA = 2;
        var rB = 3;

        /* Base layer */
        parts[part.base + 5] = new Cube.Builder(position, size)
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    part.uvBase(Face.DOWN,  skin, 0, 1,  1,  3))
            .texture(Face.WEST,  part.uvBase(Face.WEST,  skin, 3, 1, rA, rB))
            .texture(Face.NORTH, part.uvBase(Face.NORTH, skin, 3, 1, rB, rA))
            .texture(Face.EAST,  part.uvBase(Face.EAST,  skin, 3, 1, rA, rB))
            .texture(Face.SOUTH, part.uvBase(Face.SOUTH, skin, 3, 1, rB, rA))
            .build();

        /* Second layer */
        position = convert.apply(new Vec(0, 0, 4).sub(layerOffset).add(-OFFSET, 0, OFFSET));
        pivot = position;
        parts[part.layer + 5] = new Cube.Builder(position, size.add(new Vec(2*OFFSET, OFFSET, 2*OFFSET)))
            .pivot(pivot)
            .rotation(rot)
            .texture(Face.UP,    part.uvLayer(Face.DOWN,  skin, 0, 1,  1,  3))
            .texture(Face.WEST,  part.uvLayer(Face.WEST,  skin, 3, 1, rA, rB))
            .texture(Face.NORTH, part.uvLayer(Face.NORTH, skin, 3, 1, rB, rA))
            .texture(Face.EAST,  part.uvLayer(Face.EAST,  skin, 3, 1, rA, rB))
            .texture(Face.SOUTH, part.uvLayer(Face.SOUTH, skin, 3, 1, rB, rA))
            .build();
    }

    /**
     * Convert from ZXY rotation (used for arms) to XYZ (used by engine).
     * @param zxy the rotation to convert
     * @return the equivalent rotation using XYZ order.
     */
    private static Vec zxyToZyx(Vec zxy) {
        double x = Math.toRadians(zxy.x());
        double y = Math.toRadians(zxy.y());
        double z = Math.toRadians(zxy.z());
        final double cosX = Math.cos(x), sinX = Math.sin(x);
        final double cosY = Math.cos(y), sinY = Math.sin(y);
        final double cosZ = Math.cos(z), sinZ = Math.sin(z);

        var m20 = -cosX * sinY;
        if (Math.abs(m20) < 0.999999) {
            var m00 = cosZ * cosY - sinZ * sinX * sinY;
            var m10 = sinZ * cosY + cosZ * sinX * sinY;
            var m21 = sinX;
            var m22 = cosX * cosY;
            return new Vec(
                Math.toDegrees(Math.atan2(-m21, m22)),
                Math.toDegrees(Math.asin(-m20)),
                Math.toDegrees(Math.atan2(m10, m00))
            );
        } else {
            /* Gimbal lock */
            var m01 = -sinZ * cosX;
            var m02 = cosZ * sinY + sinZ * sinX * cosY;
            var m11 = cosZ * cosX;
            var m12 = sinZ * sinY - cosZ * sinX * cosY;
            if (m20 < 0) m12 = -m12;
            else m02 = -m02;
            return new Vec(
                Math.toDegrees(0),
                Math.toDegrees(Math.asin(-m20)),
                Math.toDegrees(Math.atan2(m01 + m12, m11 + m02))
            );
        }
    }

    private enum Member {
        RIGHT_ARM(new Vec(0,  8, 0), new Vec(0, 12, 2),     4, 10,    40, 16, 40, 32),
        LEFT_ARM( new Vec(8,  8, 0), new Vec(8, 12, 2),    16, 22,    32, 48, 48, 48),
        RIGHT_LEG(new Vec(0, -4, 0), new Vec(2, 0, 2),    28, 34,     0, 16,  0, 32),
        LEFT_LEG( new Vec(4, -4, 0), new Vec(6, 0, 2),    40, 46,    16, 48,  0, 48);

        public final Vec pos;
        public final Vec pivot;
        public final int base;
        public final int layer;
        private final float baseX;
        private final float baseY;
        private final float layerX;
        private final float layerY;

        Member(Vec pos, Vec pivot, int base, int layer, int baseX, int baseY, int layerX, int layerY) {
            this.pos = pos;
            this.pivot = pivot;
            this.base = base;
            this.layer = layer;
            this.baseX = baseX / 4;
            this.baseY = baseY / 4;
            this.layerX = layerX / 4;
            this.layerY = layerY / 4;
        }

        public boolean leg() {
            return this == RIGHT_LEG || this == LEFT_LEG;
        }
        
        public boolean slim(Texture skin) {
            return !leg() && (skin.source().getRGB(55, 20) >>> 24) <= 10;
        }
        
        /**
         * @param x the x coordinate of the top-left corner of the uv box
         * @param y the y coordinate of the top-left corner of the uv box
         * @param w the width of the uv box
         * @param h the height of the uv box
         * @param rotate the rotation of the texture (1 = 90° and so on)
         * @return the new texture with the given UV box and rotation
         */
        public Texture uvBase(Face face, Texture skin, float y, float h, int rA, int rB) {
            return uv(face, skin, y, h, baseX, baseY, rA, rB);
        }

        public Texture uvLayer(Face face, Texture skin, float y, float h, int rA, int rB) {
            return uv(face, skin, y, h, layerX, layerY, rA, rB);
        }

        private Texture uv(Face face, Texture skin, float y, float h, float tx, float ty, int rA, int rB) {
            var leg = leg();
            var slim = !leg && slim(skin);
            var w = 1f;
            var r = 0;

            /* Switch layers for legs */
            if (leg) {
                face = switch (face) {
                    case NORTH -> Face.SOUTH;
                    case SOUTH -> Face.NORTH;
                    case EAST -> Face.WEST;
                    case WEST -> Face.EAST;
                    default -> face;
                };
                var t = rA;
                rA = rB;
                rB = t;
            }

            /* UV setup */
            switch (face) {
                case UP -> {
                    w = slim ? .75f : 1;
                    tx += 1;
                    r = rA;
                }
                case DOWN -> {
                    w  =  -(slim ? .75f : 1);
                    tx += slim ? 2.5f : 3;
                    r = rA;
                }
                case WEST -> {  // Left
                    w = 1;
                    r = rA;
                }
                case NORTH -> {  // Front
                    tx += 1;
                    w = slim ? .75f : 1;
                    r = rA;
                }
                case EAST -> {  // Right
                    tx += slim ? 1.75f : 2;
                    w = 1;
                    r = rB;
                }
                case SOUTH -> {  // Back
                    tx += slim ? 2.75f : 3;
                    w  =  slim ? .75f : 1;
                    r = rB;
                }
            };

            /* Texture creation */
            return skin.uv(tx, ty + y, w,  h, r);
        }
    }

    /**
     * Represent all possible armor/items slots to add to the skin
     * render.
     */
    public enum Slot {
        /** The player head slot (amor head) */
        HEAD
        
        /*, LEFT_ARM, RIGHT_ARM, CHESTPLATE, LEGS, FOOTS */;
    }
}

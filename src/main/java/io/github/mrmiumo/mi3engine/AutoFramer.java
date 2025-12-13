package io.github.mrmiumo.mi3engine;

import java.awt.image.BufferedImage;

import io.github.mrmiumo.mi3engine.Element.Face;

/**
 * Tools that enables to automatically center the object in the final
 * image and adjust the zoom to fill the available space.
 */
public class AutoFramer extends RenderTool {
    
    /** Safety margin to add around the object edges */
    private final double margin;

    /**
     * Adds the Auto-Framer tool to the given engine.
     * <p>
     * This tool enable to automatically zoom and center the object in
     * the available image space. Any configured camera translation and
     * zoom will be ignored when using this tool.
     * <p>
     * A margin can be configured to add space around the object edges.
     * For that, use the other constructor! By default, a margin of 0.1
     * is added.
     * @param engine the engine to add the AutoFramer tool to
     */
    public AutoFramer(RenderEngine engine) {
        super(engine);
        this.margin = 0.1;
    }

    /**
     * Adds the Auto-Framer tool to the given engine.
     * <p>
     * This tool enable to automatically zoom and center the object in
     * the available image space. Any configured camera translation and
     * zoom will be ignored when using this tool.
     * @param engine the engine to add the AutoFramer tool to
     * @param margin space added around the object edges (un-zoom the image)
     */
    public AutoFramer(RenderEngine engine, double margin) {
        super(engine);
        this.margin = margin;
    }

    @Override
    public BufferedImage render() {
        if (super.engine instanceof SkinRender skin) skin.build();
        var bkpZoom = engine.camera().getZoom();
        var bkpRot = engine.camera().getRotation();
        var bkpPan = engine.camera().getTranslation();

        /* Compute the bounding box */
        engine.camera().setTranslation(0, 0);
        engine.camera().setZoom(1);
        var boundingBox = boundingBox();

        /* Correct the pan and zoom*/
        engine.camera().setZoom(correctZoom(boundingBox));
        var pan = correctPan(boundingBox);
        engine.camera().setTranslation(pan.x(), pan.y());

        /* Rerender and clear auto-framing */
        var img = engine.render();
        engine.camera().setZoom(bkpZoom)
            .setRotation((int)bkpRot.x(), (int)bkpRot.y(), (int)bkpRot.z())
            .setTranslation(bkpPan.x(), bkpPan.y());

        return img;
    }

    /**
     * Computes the bounding box of the current engine's cubes. The
     * bounding box is the smallest rectangle in which all the
     * non-empty pixels fits.
     * @return an array containing the XY coordinate of the top-left
     *    corner of the box and the XY of the bottom-right corner.
     */
    private int[] boundingBox() {
        final var box = new int[]{ Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        final var size = engine.size();
        final var centerX = size.x() / 2;
        final var centerY = size.y() / 2;
        final var cam = camera();
        var zoom = cam.zoom() * Math.min(size.x(), size.y()) / 1287;

        /* Create triangles from boxes */
        for (var element : engine.getElements()) {
            element = element.move(RenderCore.OFFSET);
            final Vec[] localVerts = element.localVertices();
            final var modelPoint = RenderEngine.modelToWorld(element);

            /* Generates triangles for each face */
            for (var face : Face.values()) {
                if (element.getTexture(face) == null) continue;
                int[] idxs = FACES[face.ordinal()];

                Vec2[] position = new Vec2[4];
                for (int k = 0 ; k < 4 ; k++) {
                    Vec view = modelPoint
                        .apply(localVerts[idxs[k]])
                        .rotate(cam.rotation())
                        .sub(cam.translation());
                    double sxPix = centerX - (view.x() * zoom);
                    double syPix = centerY - (view.y() * zoom);
                    position[k] = new Vec2(sxPix, syPix);
                }
                
                for (int[] points : RenderCore.TRIANGLES) {
                    var a = position[points[0]];
                    var b = position[points[1]];
                    var c = position[points[2]];
                    updateBoundingBox(a, b, c, box);
                }
            }
        }

        return box;
    }

    /**
     * Updates the given bounding-box if the triangle defined by the
     * 3 vertices is outside the current bounding box.
     * @param a the first corner of the triangle
     * @param b the second corner of the triangle
     * @param c the third corner of the triangle
     * @param box the current bounding box
     */
    private void updateBoundingBox(Vec2 a, Vec2 b, Vec2 c, int[] box) {
        int minX = (int)Math.floor(RenderUtils.min(a.x(), b.x(), c.x()));
        int maxX = (int)Math.ceil( RenderUtils.max(a.x(), b.x(), c.x()));
        int minY = (int)Math.floor(RenderUtils.min(a.y(), b.y(), c.y()));
        int maxY = (int)Math.ceil( RenderUtils.max(a.y(), b.y(), c.y()));

        if (minX < box[0]) box[0] = minX;
        if (minY < box[1]) box[1] = minY;
        if (maxX > box[2]) box[2] = maxX;
        if (maxY > box[3]) box[3] = maxY;
    }

    /**
     * Computes the best zoom settings to fill the available space.
     * @param box the bounding box of the cubes before auto framing
     * @return the new zoom value to use
     */
    private Vec correctPan(int[] box) {
        var size = engine.size();
        var sx = size.x() / 2;
        var sy = size.y() / 2;

        double cx =  box[0] + (box[2] - box[0]) / 2;
        double cy = box[1] + (box[3] - box[1]) / 2;

        /* Compute new center with the new zoom */
        var zoom = engine.camera().getZoom();
        cx = cx + (cx - sx) * (zoom - 1);
        cy = cy + (cy - sy) * (zoom - 1);
        
        /* Compute new pan to center the image */
        var invZoom = 1 / zoom;
        var dx = sx - cx;
        var dy = sy - cy;

        var fx = 422 * size.x() / 1287;
        var fy = 422 * size.y() / 1287;
        return new Vec(dx / fx * invZoom, - dy / fy * invZoom, 0);
    }

    /**
     * Computes the best zoom settings to fill the available space.
     * @param box the bounding box of the cubes before auto framing
     * @return the new zoom value to use
     */
    private double correctZoom(int[] box) {
        var size = engine.size();
        var width =  box[2] - box[0];
        var height = box[3] - box[1];

        /* Compute new zoom to fill the image */
        double zoomW = (size.x() - size.x() * margin) / width;
        double zoomH = (size.y() - size.y() * margin) / height;

        return Math.min(zoomW, zoomH);
    }
}

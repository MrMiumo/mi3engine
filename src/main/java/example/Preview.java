package example;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import io.github.mrmiumo.mi3engine.AutoFramer;
import io.github.mrmiumo.mi3engine.RenderEngine;

/**
 * Very basic monitor to visualize engine output in real time and
 * manipulate the model in space.
 * @param <T> the type of Engine to preview
 */
public class Preview<T extends RenderEngine> extends JFrame {

    /** Executor used to generate one cancellable HQ image at a time */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    /** Task used to generate one cancellable HQ image at a time */
    private Future<?> current;

    /** Whether the setup has changed and requires frame render or not */
    private boolean frameRendered = false;

    /** The window */
    private final JLabel image = new JLabel();

    /** The engine used to render the model */
    private final RenderEngine engineFast;

    /** The engine used to render the model */
    private final T engineHQ;

    /** The position of the last drag event */
    private Point dragStart;

    /** Whether the current drag uses the right or left click */
    private boolean rightClick = true;

    /**
     * Creates and opens a window to display the engine output.
     * @param engine the engine to use for rendering
     * @throws IOException in case of error while parsing the file
     */
    public Preview(T engine) throws IOException {
        var width =  (int)engine.size().x();
        var height = (int)engine.size().y();
        engineHQ = engine;
        engineFast = RenderEngine.from(width / 2, height / 2).addElements(engineHQ.getElements());
        engineFast.setCamera(engineHQ.camera());
        engineHQ.camera()
            .setZoom(0.32);

        /* Setup the window */
        setUpWindow(width, height);

        /* Add zoom and pan */
        var mouseAdapter = setUpPan();
        image.addMouseListener(mouseAdapter);
        image.addMouseMotionListener(mouseAdapter);
        image.addMouseWheelListener(setUpZoom());
    }

    /**
     * Opens the preview window that enables to manipulates the model
     * using the mouse. Using this methods enables to set a custom
     * animation function that can be used to edit the engine content.
     * <p>
     * WARNING: start methods can only be called once.
     * @param animation the function that generate a new frame, null
     *     if no animation is needed. 
     * @throws InterruptedException in case of error during the loop
     */
    public void start(Animation<T> animation) throws InterruptedException {
        setVisible(true);

        var i = 0;
        var frames = 0L;
        var start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(1000 / 30); // Cap FPS to 30
            if (animation != null) {
                animation.requestFrame(engineHQ, frames++);
                engineFast.clear().addElements(engineHQ.getElements());
                renderImage();
            } else if (!frameRendered) {
                renderImage();
            }
            i++;

            /* Display FPS */
            var elapsed = System.currentTimeMillis() - start;
            if (elapsed >= 500) {
                System.out.print("\r" + (int)(1000.0 / (elapsed / i)) + " FPS     ");
                start = System.currentTimeMillis();
                i = 0;
            }
        }
    }

    /**
     * Opens the preview window that enables to manipulates the model
     * using the mouse.
     * <p>
     * WARNING: start methods can only be called once.
     * @throws InterruptedException in case of error during the loop
     */
    public void start() throws InterruptedException {
        start(null);
    }

    /**
     * Saves the current preview frame at the given path under the
     * PNG format.
     * @param dst the path with the file name to save the preview at
     * @return this preview
     * @throws IOException in case of error while writing the file
     */
    public Preview<T> print(Path dst) throws IOException {
        BufferedImage out = new AutoFramer(engineHQ).render();
        ImageIO.write(out, "PNG", Files.newOutputStream(dst));
        System.out.println("Frame saved under " + dst.toAbsolutePath());
        return this;
    }

    /**
     * Create the window with its size, name, ...
     * @param width the width of the image to display
     * @param height the height of the image to display
     */
    private void setUpWindow(int width, int height) {
        setTitle("Mi³ Engine Viewer");
        setSize(width / 2, height / 2);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(image, BorderLayout.CENTER);
        getContentPane().setBackground(new Color(0x282a2e));
    }

    /**
     * Creates a listener for the mouse to detect rotate and pan.
     * @return the adapter to handler rotate and pan.
     */
    private MouseAdapter setUpPan() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rightClick = e.getButton() == 3;
                dragStart = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null; // reset
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    var cam = engineHQ.camera();

                    // Check if horizontal or vertical drag is stronger
                    if (rightClick) {
                        var pos = cam.getTranslation();
                        cam.setTranslation(pos.x() + dx / 50.0, pos.y() - dy / 50.0);
                    } else {
                        var rot = cam.getRotation();
                        cam.setRotation((int)(rot.x() + dy), (int)(rot.y() + dx), 0);
                    }
                    dragStart = e.getPoint();
                }
                frameRendered = false;
            }
        };
    }

    /**
     * Creates a listener for the mouse wheel to detect zoom in and out
     * @return the listener to zoom with the mouse
     */
    private MouseWheelListener setUpZoom() {
        return e -> {
            var zoom = engineHQ.camera().getZoom() / 0.23 * 50;
            if (e.getWheelRotation() < 0) {
                zoom++;
            } else {
                zoom--;
            }
            engineHQ.camera().setZoom(0.23 * (zoom / 50.0));
            frameRendered = false;
        };
    }

    /**
     * Refresh the displayed image with the given one
     * @param img the image to set
     */
    private void setImage(BufferedImage img) {
        var size = engineHQ.size();
        image.setIcon(new ImageIcon(img.getScaledInstance(
            (int)(size.x() / 2),
            (int)(size.y() / 2),
            Image.SCALE_SMOOTH
        )));
    }

    /**
     * Renders the image at the adapted resolution. Fast render is
     * used to refresh the display quickly while moving, the HQ render
     * is used once the scene is still.
     */
    private void renderImage() {
        frameRendered = true;
        setImage(engineFast.render());
        if (frameRendered) {
            if (current != null) current.cancel(true);
            current = executor.submit(() -> {
                var img = engineHQ.render();
                if (frameRendered) setImage(img);
            });
        }
    }

    /**
     * Functional interface to pass animation frame render methods.
     * @param <T> the type of engine to animate
     */
    @FunctionalInterface
    public interface Animation<T> {
        /**
         * Updates the given engine to edit the scene in order to
         * prepare it for the next frame. The animation should rely on
         * the given time to have fluent animations instead of relying
         * on a separate frame counter.
         * @param engine the engine used by the preview that will be updated
         * @param time the animation frame time-code
         */
        public void requestFrame(T engine, long time);
    }
}

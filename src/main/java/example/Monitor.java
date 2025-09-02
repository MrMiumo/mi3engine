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
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import io.github.mrmiumo.mi3engine.ModelParser;
import io.github.mrmiumo.mi3engine.RenderEngine;

/**
 * Very basic monitor to visualize engine output in real time and
 * manipulate the model in space.
 */
public class Monitor extends JFrame {

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
    private final RenderEngine engineHQ;

    /** The position of the last drag event */
    private Point dragStart;

    /** Whether the current drag uses the right or left click */
    private boolean rightClick = true;

    /**
     * Creates and opens a window to display the engine output.
     * @param width the width of the image to generate
     * @param height the height of the image to generate
     * @param model the path of the model to display
     * @throws IOException in case of error while parsing the file
     */
    public Monitor(int width, int height, Path model) throws IOException {
        engineHQ = new ModelParser(RenderEngine.from(width, height)).parse(model);
        engineFast = RenderEngine.from(width / 2, height / 2).addCubes(engineHQ.getCubes());
        engineFast.setCamera(engineHQ.camera());
        engineHQ.camera()
            .setRotation(25, 35, 0)
            .setZoom(0.32);

        /* Setup the window */
        setUpWindow(width, height);

        /* Add zoom and pan */
        var mouseAdapter = setUpPan();
        image.addMouseListener(mouseAdapter);
        image.addMouseMotionListener(mouseAdapter);
        image.addMouseWheelListener(setUpZoom());

        setVisible(true);
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
    public void setImage(BufferedImage img) {
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
    public void renderImage() {
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
     * Opens the monitor to view and manipulate object.
     * @param args unused
     * @throws IOException in case of parsing error
     * @throws InterruptedException in case of error while sleeping
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        var pack = Path.of("C:/Path/to/the/resource-pack");
        var model = pack.resolve("assets/minecraft/models/item/myModel.json");
                
        var window = new Monitor(1287, 1287, model);
        System.out.print("");

        while (true) {
            var start = System.currentTimeMillis();
            if (window.frameRendered) {
                Thread.sleep(1000 / 30);
            } else {
                window.renderImage();
            }
            var elapsed = System.currentTimeMillis() - start;
            System.out.print("\r" + (1000 / elapsed) + " FPS     ");
        }
    }
}

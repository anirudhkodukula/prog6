package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Visualization {
    BoundedVolumeHierarchy boundingVolumeHierarchy;

    private static final int SIZE = 400;
    private static final int MIDDLE = 200;
    private static final int HALF = 10;
    private static final int STEP = 5;

    // square coordinates
    private int squareX;
    private int squareY;

    // square starts at (0, 0)
    public Visualization () {
        squareX = 0;
        squareY = 0;
        //adding a couple triangles around it
        List<Shape> env = new ArrayList<>();
        env.add(new Triangle(new Point(0, 100), new Point(50, 50), new Point(50, 100)));
        env.add(new Triangle(new Point(-100, -50), new Point(-50, -50), new Point(-100, -100)));
        env.add(new Triangle(new Point(50, -100), new Point(100, -100), new Point(100, -50)));

        boundingVolumeHierarchy = new BoundedVolumeHierarchy();
        boundingVolumeHierarchy.buildBVH(env);
    }

    //this allows for tests to use their own BVH
    public void setBoundingVolumeHierarchy(BoundedVolumeHierarchy boundingVolumeHierarchy) {
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    // this moves the square unless it hits a triangle
    public boolean moveSquare (char keyPressed) {
        //where the square would be after key presses
        int newX = squareX;
        int newY = squareY;

        if (keyPressed == 'w' || keyPressed == 'W') {
            newY -= STEP;
        } else if (keyPressed == 's' || keyPressed == 'S') {
            newY += STEP;
        } else if (keyPressed == 'a' || keyPressed == 'A') {
            newX -= STEP;
        } else if (keyPressed == 'd' || keyPressed == 'D') {
            newX += STEP;
        } else {
            return false;
        }

        // check every int spot the square would cover for a triangle
        for (int row = newY - HALF; row <= newY + HALF; row++) {
            for (int col = newX - HALF; col <= newX + HALF; col++) {
                if (!boundingVolumeHierarchy.findCollision(new Point2D.Double(col, row)).isEmpty()) {
                    return false;
                }
            }
        }

        squareX = newX;
        squareY = newY;
        return true;
    }

    // this shoots the ray from the square to the pixels on the edges
    public LinkedList<Point> drawGUI() {
        //finds which pixels the square can see and shoot at
        LinkedList<Point> pixels = new LinkedList<>();
        Point eye = new Point(squareX, squareY);
        Point2D.Double origin = new Point2D.Double(squareX, squareY);

        for (int pos = 0; pos < SIZE; pos++) {
            // pixel coordinates for each of the 4 edges
            int[][] targets = {{pos, 0}, {pos, SIZE - 1}, {0, pos}, {SIZE - 1, pos}};

            for (int[] target : targets) {
                Vector2D direction = new Vector2D(target[0] - MIDDLE, target[1] - MIDDLE);
                Shape hit = boundingVolumeHierarchy.intersectRay(origin, direction);
                if (hit == null) {
                    continue;
                }

                Point2D.Double spot = hit.findIntersection(eye, direction);

                // how far is the square to the ray hit
                // we add 200 since the square's always drawn in the middle ... basically causes us to shift the window
                int guiX = (int) Math.round(spot.getX() - squareX + MIDDLE);
                int guiY = (int) Math.round(spot.getY() - squareY + MIDDLE);
                if (guiX >= 0 && guiX < SIZE && guiY >= 0 && guiY < SIZE) {
                    pixels.add(new Point(guiX, guiY));
                }
            }
        }

        return pixels;
    }
}
package assignment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BVHTest {
    private static Triangle lowerLeft;
    private static Triangle upperLeft;
    private static Triangle upperRight;
    private static Triangle lowerRight;

    private BVH bvh;
    private Random random;

    // the 4 triangles from the handout
    @BeforeAll
    static void setupAll() {
        lowerLeft = new Triangle(new Point(0, 0), new Point(1, 0), new Point(1, 2));
        upperLeft = new Triangle(new Point(1, 5), new Point(2, 3), new Point(3, 5));
        upperRight = new Triangle(new Point(2, 2), new Point(3, 1), new Point(4, 2));
        lowerRight = new Triangle(new Point(4, 5), new Point(5, 3), new Point(5, 5));
    }

    
    @BeforeEach
    void setupEach() {
        bvh = new BoundedVolumeHierarchy();
        random = new Random(314);
    }

    @Test
    void testBVH() {
        
        bvh.buildBVH(new ArrayList<>());
        assertEquals(Set.of(), bvh.findCollision(new Point2D.Double(0, 0)), "found a triangle even w empty tree");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 0)), "ray hit something even w empty tree");
        assertEquals("", bvh.toString().trim(), "should print nothing w empty tree");

        
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        assertEquals(7, bvh.toString().trim().split("\n").length, "should get 7 lines but got:\n" + bvh);
        assertEquals("[(0, 0), (5, 5)]", bvh.toString().split("\n")[0], "should be (0, 0) to (5, 5) but got:\n" + bvh);
        assertEquals(2, height(), "should be 2 levels below the top but got:\n" + bvh);
        assertEquals(Set.of(lowerLeft), bvh.findCollision(new Point2D.Double(0, 0)), "point on the corner should be inside");
        assertEquals(Set.of(upperLeft), bvh.findCollision(new Point2D.Double(2, 4)), "(2, 4) should only be inside upperLeft");
        assertEquals(Set.of(), bvh.findCollision(new Point2D.Double(0, 4)), "(0, 4) is empty space but found triangle");
        assertEquals(upperLeft, bvh.intersectRay(new Point2D.Double(0, 4), new Vector2D(1, 0)), "should hit closer triangle first");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 4), new Vector2D(-1, 0)), "hit something even w ray pointing away");

        
        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MEDIAN);
        List<Shape> shapes = randomTriangles(150);
        bvh.buildBVH(new ArrayList<>(shapes));
        assertEquals(150, leaves(), "median build should keep all 150 triangles");
        bruteForce(shapes, "median build");

        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MIDRANGE);
        shapes = randomTriangles(150);
        bvh.buildBVH(new ArrayList<>(shapes));
        assertEquals(150, leaves(), "midrange build should keep all 150 triangles");
        bruteForce(shapes, "midrange build");

        
        bvh.buildBVH(new ArrayList<>(List.of(new Triangle(new Point(0, 0), new Point(100, 0), new Point(0, 1)),
                new Triangle(new Point(0, 2), new Point(2, 2), new Point(0, 4)),
                new Triangle(new Point(1, 5), new Point(3, 5), new Point(1, 7)))));
        assertEquals(3, leaves(), "should keep all 3 triangles but got:\n" + bvh);

        
        bvh.buildBVH(new ArrayList<>());
        shapes = new ArrayList<>();
        for (int count = 1; count <= 100; count++) {
            Shape shape = randomTriangles(1).get(0);
            bvh.insert(shape);
            shapes.add(shape);
            assertEquals(count, leaves(), "wrong triangle count after insert #" + count);
        }
        assertTrue(height() <= 9, "should be balanced but height is " + height());
        bruteForce(shapes, "inserts");

        
        Collections.shuffle(shapes, random);
        for (int count = 0; count < 50; count++) {
            bvh.remove(shapes.remove(0));
        }
        assertEquals(50, leaves(), "should have 50 triangles left after removing 50");
        assertTrue(height() <= 7, "should be balanced but height is " + height());
        bruteForce(shapes, "removes");

        
        String before = bvh.toString();
        bvh.remove(new Triangle(new Point(900, 900), new Point(901, 900), new Point(900, 901)));
        assertEquals(before, bvh.toString(), "tree changed even w removing triangle that wasn't there");

        
        for (Shape shape : shapes) {
            bvh.remove(shape);
        }
        assertEquals("", bvh.toString().trim(), "should be empty after removing everything");

     
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, lowerLeft, upperRight)));
        bvh.remove(lowerLeft);
        assertEquals(2, leaves(), "should only remove 1 copy but got:\n" + bvh);
    }

    @Test
    void testVisualization() {
        Visualization view = new Visualization();
        BoundedVolumeHierarchy scene = new BoundedVolumeHierarchy();
        view.setBoundingVolumeHierarchy(scene);
        assertTrue(view.drawGUI().isEmpty(), "no triangles but still drew stuff");
        assertFalse(view.moveSquare('x'), "square moved on key that wasn't WASD");

        
        Triangle wall = new Triangle(new Point(-50, 50), new Point(50, 50), new Point(0, 80));
        Triangle blocker = new Triangle(new Point(-20, -30), new Point(20, -30), new Point(0, -16));
        scene.buildBVH(new ArrayList<>(List.of(wall, blocker)));
        assertTrue(view.drawGUI().contains(new Point(200, 250)), "should be at pixel (200, 250)");

        assertTrue(view.moveSquare('w'), "didn't move up even w nothing in the way");
        assertFalse(view.moveSquare('w'), "square moved into the triangle above");
        assertTrue(view.moveSquare('s'), "didn't move down even w nothing in the way");
        assertTrue(view.moveSquare('S'), "capital S didn't move the square");
        assertTrue(view.drawGUI().contains(new Point(200, 245)), "pixel should be (200, 245)");
        assertTrue(view.moveSquare('a'), "didn't move left even w nothing in the way");
        assertTrue(view.moveSquare('d'), "didn't move right even w nothing in the way");

        
        for (int step = 0; step < 6; step++) {
            assertTrue(view.moveSquare('s'), "didn't move down even w nothing in the way (step " + step + ")");
        }
        assertFalse(view.moveSquare('s'), "square moved into the wall below it");
    }

   
    private List<Shape> randomTriangles(int amount) {
        List<Shape> shapes = new ArrayList<>();
        while (shapes.size() < amount) {
            int baseX = random.nextInt(180);
            int baseY = random.nextInt(180);
            Point first = new Point(baseX + random.nextInt(20), baseY + random.nextInt(20));
            Point second = new Point(baseX + random.nextInt(20), baseY + random.nextInt(20));
            Point third = new Point(baseX + random.nextInt(20), baseY + random.nextInt(20));
            if ((second.x - first.x) * (third.y - first.y) != (third.x - first.x) * (second.y - first.y)) {
                shapes.add(new Triangle(first, second, third));
            }
        }
        return shapes;
    }

  
    private void bruteForce(List<Shape> shapes, String when) {
        for (int count = 0; count < 200; count++) {
            Point2D.Double point = new Point2D.Double(random.nextDouble() * 220 - 10, random.nextDouble() * 220 - 10);
            Set<Shape> expected = new HashSet<>();
            for (Shape shape : shapes) {
                if (shape.containsPoint(point)) {
                    expected.add(shape);
                }
            }
            assertEquals(expected, bvh.findCollision(point), "after " + when + ": wrong triangles at " + point);

            Point origin = new Point(random.nextInt(260) - 30, random.nextInt(260) - 30);
            Vector2D direction = new Vector2D(random.nextInt(21) - 10, random.nextInt(21) - 10 + 0.5);
            double best = Double.MAX_VALUE;
            for (Shape shape : shapes) {
                Point2D.Double spot = shape.findIntersection(origin, direction);
                if (spot != null) {
                    best = Math.min(best, BVH.distanceBetweenPoints(origin, spot));
                }
            }

            Shape hit = bvh.intersectRay(new Point2D.Double(origin.x, origin.y), direction);
            String ray = "after " + when + ": ray from " + origin;
            if (best == Double.MAX_VALUE) {
                assertNull(hit, ray + " should miss but hit " + hit);
            } else {
                assertNotNull(hit, ray + " should hit something but missed");
                assertEquals(best, BVH.distanceBetweenPoints(origin, hit.findIntersection(origin, direction)), 1e-6, ray + " should return closest triangle");
            }
        }
    }

   
    private int height() {
        int most = 0;
        for (String line : bvh.toString().split("\n")) {
            int tabs = 0;
            while (tabs < line.length() && line.charAt(tabs) == '\t') {
                tabs++;
            }
            most = Math.max(most, tabs);
        }
        return most;
    }

    
    private int leaves() {
        int count = 0;
        for (String line : bvh.toString().split("\n")) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("[")) {
                count++;
            }
        }
        return count;
    }
}
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
    void testEmpty() {
        bvh.buildBVH(new ArrayList<>());
        assertEquals(Set.of(), bvh.findCollision(new Point2D.Double(0, 0)), "found a triangle even though the tree is empty");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 0)), "ray hit something even though the tree is empty");
        assertEquals("", bvh.toString().trim(), "empty tree should print nothing");
        bvh.remove(lowerLeft);
        assertEquals("", bvh.toString().trim(), "removing from an empty tree shouldn't change anything");
    }

    @Test
    void testOneTriangle() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft)));
        assertEquals(1, leaves(), "should have 1 leaf but got:\n" + bvh);
        assertEquals(0, height(), "a tree with 1 triangle should have height 0 but got:\n" + bvh);
        assertEquals(Set.of(lowerLeft), bvh.findCollision(new Point2D.Double(1, 1)), "(1, 1) should be inside lowerLeft");
    }

    @Test
    void testHandoutBuild() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        assertEquals(7, bvh.toString().trim().split("\n").length, "should have 7 lines but got:\n" + bvh);
        assertEquals("[(0, 0), (5, 5)]", bvh.toString().split("\n")[0], "root box should go from (0, 0) to (5, 5) but got:\n" + bvh);
        assertEquals(2, height(), "tree should be 2 levels below the root but got:\n" + bvh);
    }

    @Test
    void testHandoutCollision() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        assertEquals(Set.of(lowerLeft), bvh.findCollision(new Point2D.Double(0, 0)), "point on the corner should still count as inside");
        assertEquals(Set.of(upperLeft), bvh.findCollision(new Point2D.Double(2, 4)), "(2, 4) should only be inside upperLeft");
        assertEquals(Set.of(), bvh.findCollision(new Point2D.Double(0, 4)), "(0, 4) is empty space but a triangle was found");
    }

    @Test
    void testHandoutRay() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        assertEquals(upperLeft, bvh.intersectRay(new Point2D.Double(0, 4), new Vector2D(1, 0)), "should hit the closer triangle first");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 4), new Vector2D(-1, 0)), "ray pointing away shouldn't hit anything");
        assertEquals(lowerLeft, bvh.intersectRay(new Point2D.Double(1, 1), new Vector2D(0, 1)), "ray starting inside a triangle should still count as a hit");
    }

    @Test
    void testOverlap() {
        Triangle big = new Triangle(new Point(0, 0), new Point(10, 0), new Point(0, 10));
        Triangle small = new Triangle(new Point(1, 1), new Point(3, 1), new Point(1, 3));
        bvh.buildBVH(new ArrayList<>(List.of(big, small, upperRight)));
        assertEquals(Set.of(big, small), bvh.findCollision(new Point2D.Double(1.5, 1.5)), "both overlapping triangles should be found");
    }

    @Test
    void testBuildKeepsList() {
        List<Shape> input = new ArrayList<>(List.of(lowerRight, upperRight, upperLeft, lowerLeft));
        bvh.buildBVH(input);
        assertEquals(List.of(lowerRight, upperRight, upperLeft, lowerLeft), input, "buildBVH shouldn't change the original list");
    }

    @Test
    void testRebuild() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft)));
        assertEquals(1, leaves(), "rebuilding should replace the old tree with just 1 triangle but got:\n" + bvh);
        assertEquals(Set.of(), bvh.findCollision(new Point2D.Double(2, 4)), "an old triangle was still in the tree after rebuilding");
    }

    @Test
    void testMedianRow() {
        List<Shape> row = rowOfTriangles(40);
        List<Shape> shuffled = new ArrayList<>(row);
        Collections.shuffle(shuffled, random);
        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MEDIAN);
        bvh.buildBVH(shuffled);
        assertEquals(40, leaves(), "median build should keep all 40 triangles");
        assertEquals(6, height(), "median build with 40 triangles should have height 6 but got:\n" + bvh);
        checkRow(row, false, "median build");
    }

    @Test
    void testMidrangeRow() {
        List<Shape> row = rowOfTriangles(40);
        List<Shape> shuffled = new ArrayList<>(row);
        Collections.shuffle(shuffled, random);
        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MIDRANGE);
        bvh.buildBVH(shuffled);
        assertEquals(40, leaves(), "midrange build should keep all 40 triangles");
        checkRow(row, false, "midrange build");
    }

    @Test
    void testMidrangeOneSide() {
        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MIDRANGE);
        bvh.buildBVH(new ArrayList<>(List.of(new Triangle(new Point(0, 0), new Point(100, 0), new Point(0, 1)),
                new Triangle(new Point(0, 2), new Point(2, 2), new Point(0, 4)),
                new Triangle(new Point(1, 5), new Point(3, 5), new Point(1, 7)))));
        assertEquals(3, leaves(), "all 3 triangles should still be in the tree but got:\n" + bvh);
    }

    @Test
    void testInsertIntoEmpty() {
        bvh.insert(lowerLeft);
        assertEquals(1, leaves(), "inserting into an empty tree should give 1 leaf but got:\n" + bvh);
        assertEquals(Set.of(lowerLeft), bvh.findCollision(new Point2D.Double(1, 1)), "couldn't find the triangle after inserting it");
    }

    @Test
    void testInsertRow() {
        List<Shape> row = rowOfTriangles(40);
        List<Shape> shuffled = new ArrayList<>(row);
        Collections.shuffle(shuffled, random);
        for (int count = 0; count < shuffled.size(); count++) {
            bvh.insert(shuffled.get(count));
            assertEquals(count + 1, leaves(), "wrong number of triangles after insert #" + (count + 1));
        }
        assertTrue(height() <= 7, "tree should be balanced but its height is " + height());
        checkRow(row, false, "inserts");
    }

    @Test
    void testRemoveRow() {
        List<Shape> row = rowOfTriangles(40);
        List<Shape> shuffled = new ArrayList<>(row);
        Collections.shuffle(shuffled, random);
        bvh.buildBVH(new ArrayList<>(shuffled));
        for (Shape shape : shuffled) {
            if (row.indexOf(shape) % 2 == 0) {
                bvh.remove(shape);
            }
        }
        assertEquals(20, leaves(), "should have 20 triangles left after removing 20");
        assertTrue(height() <= 5, "tree should still be balanced but its height is " + height());
        checkRow(row, true, "removes");
    }

    @Test
    void testRemoveAll() {
        List<Shape> row = rowOfTriangles(20);
        bvh.buildBVH(new ArrayList<>(row));
        for (Shape shape : row) {
            bvh.remove(shape);
        }
        assertEquals("", bvh.toString().trim(), "tree should be empty after removing all the triangles");
        bvh.insert(lowerLeft);
        assertEquals(1, leaves(), "inserting after removing everything should work but got:\n" + bvh);
    }

    @Test
    void testRemoveMissing() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, upperLeft, upperRight, lowerRight)));
        String before = bvh.toString();
        bvh.remove(new Triangle(new Point(900, 900), new Point(901, 900), new Point(900, 901)));
        assertEquals(before, bvh.toString(), "tree shouldn't change when removing a triangle that isn't there");
    }

    @Test
    void testRemoveDuplicate() {
        bvh.buildBVH(new ArrayList<>(List.of(lowerLeft, lowerLeft, upperRight)));
        bvh.remove(lowerLeft);
        assertEquals(2, leaves(), "should only remove 1 copy of the triangle but got:\n" + bvh);
        assertEquals(Set.of(lowerLeft), bvh.findCollision(new Point2D.Double(1, 1)), "the other copy of the triangle should still be there");
    }

    @Test
    void testDefaultScene() {
        Visualization view = new Visualization();
        assertFalse(view.drawGUI().isEmpty(), "starting scene should have something to draw");
    }

    @Test
    void testDrawEmpty() {
        Visualization view = new Visualization();
        view.setBoundingVolumeHierarchy(new BoundedVolumeHierarchy());
        assertTrue(view.drawGUI().isEmpty(), "drawGUI() drew something even though there are no triangles");
        assertFalse(view.moveSquare('x'), "square shouldn't move when a key other than WASD is pressed");
    }

    @Test
    void testDrawWall() {
        Visualization view = new Visualization();
        BoundedVolumeHierarchy scene = new BoundedVolumeHierarchy();
        scene.buildBVH(new ArrayList<>(List.of(new Triangle(new Point(-50, 50), new Point(50, 50), new Point(0, 80)))));
        view.setBoundingVolumeHierarchy(scene);
        assertTrue(view.drawGUI().contains(new Point(200, 250)), "wall should be drawn at pixel (200, 250)");
        for (Point pixel : view.drawGUI()) {
            assertEquals(250, pixel.y, "only the top edge of the wall should be drawn but got " + pixel);
        }
        assertTrue(view.moveSquare('s'), "should be able to move down when nothing is in the way");
        assertTrue(view.drawGUI().contains(new Point(200, 245)), "pixel should have moved to (200, 245)");
    }

    @Test
    void testMoveSquare() {
        Visualization view = new Visualization();
        view.setBoundingVolumeHierarchy(new BoundedVolumeHierarchy());
        assertTrue(view.moveSquare('w'), "w should move the square up");
        assertTrue(view.moveSquare('a'), "a should move the square left");
        assertTrue(view.moveSquare('s'), "s should move the square down");
        assertTrue(view.moveSquare('d'), "d should move the square right");
        assertTrue(view.moveSquare('W'), "capital W should work too");
        assertTrue(view.moveSquare('D'), "capital D should work too");
    }

    @Test
    void testMoveBlocked() {
        Visualization view = new Visualization();
        BoundedVolumeHierarchy scene = new BoundedVolumeHierarchy();
        Triangle wall = new Triangle(new Point(-50, 50), new Point(50, 50), new Point(0, 80));
        Triangle blocker = new Triangle(new Point(-20, -30), new Point(20, -30), new Point(0, -16));
        scene.buildBVH(new ArrayList<>(List.of(wall, blocker)));
        view.setBoundingVolumeHierarchy(scene);
        assertTrue(view.moveSquare('w'), "should be able to move up before reaching the triangle");
        assertFalse(view.moveSquare('w'), "square shouldn't be able to move into the triangle above it");
        assertTrue(view.moveSquare('s'), "should be able to move back down");
        for (int step = 0; step < 7; step++) {
            assertTrue(view.moveSquare('s'), "should be able to keep moving down (step " + step + ")");
        }
        assertFalse(view.moveSquare('s'), "square shouldn't be able to move into the wall below it");
    }

    private List<Shape> rowOfTriangles(int amount) {
        List<Shape> row = new ArrayList<>();
        for (int count = 0; count < amount; count++) {
            int baseX = 10 * count;
            row.add(new Triangle(new Point(baseX, 0), new Point(baseX + 4, 0), new Point(baseX, 4)));
        }
        return row;
    }

    private void checkRow(List<Shape> row, boolean evensGone, String when) {
        for (int count = 0; count < row.size(); count++) {
            boolean present = !evensGone || count % 2 == 1;

            Point2D.Double inside = new Point2D.Double(10 * count + 1, 1);
            Set<Shape> expected = new HashSet<>();
            if (present) {
                expected.add(row.get(count));
            }
            assertEquals(expected, bvh.findCollision(inside), when + ": wrong triangles found at " + inside);

            Point2D.Double gap = new Point2D.Double(10 * count + 7, 1);
            assertEquals(Set.of(), bvh.findCollision(gap), when + ": found a triangle in the gap at " + gap);

            if (present) {
                assertEquals(row.get(count), bvh.intersectRay(gap, new Vector2D(-1, 0)), when + ": ray going left from " + gap + " should hit triangle #" + count);

                int next;
                if (evensGone) {
                    next = count + 2;
                } else {
                    next = count + 1;
                }

                Shape hit = bvh.intersectRay(gap, new Vector2D(1, 0));
                if (next < row.size()) {
                    assertEquals(row.get(next), hit, when + ": ray going right from " + gap + " should hit triangle #" + next);
                } else {
                    assertNull(hit, when + ": ray going right from the last gap should miss, but hit " + hit);
                }
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

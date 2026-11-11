package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BoundedVolumeHierarchy implements BVH {
    private Node root;
    // median/midRange that buildBVH should use to cut
    private SplitMethod splitMethod = SPLIT_DEFAULT;
    // helps insert
    private Random random = new Random();
    // so remove only kicks out one copy
    private boolean found;

    @Override
    public void setSplitMethod(SplitMethod splitMethod) {
        this.splitMethod = splitMethod;
    }

    //builds new tree
    @Override
    public void buildBVH(List<Shape> shapeList) {
        if (shapeList.isEmpty()) {
            root = null;
            return;
        }

        // making a copy so sorting doesn't mess it up
        root = build(new ArrayList<>(shapeList), 0);
    }

    
    @Override
    public void insert(Shape shape) {
        if (root == null) {
            root = new Node(shape);
        } else {
            root = insertInto(root, shape);
        }
    }

    @Override
    public void remove(Shape shape) {
        if (root == null) {
            return;
        }

        found = false;
        root = removeFrom(root, shape);
    }

    // we finding every triangle where the point is inside
    @Override
    public Set<Shape> findCollision(Point2D.Double point) {
        Set<Shape> hits = new HashSet<>();
        if (root != null) {
            collect(root, point, hits);
        }
        return hits;
    }

    // see which triangle the ray hits first
    @Override
    public Shape intersectRay(Point2D.Double origin, Vector2D direction) {
        if (root == null) {
            return null;
        }

    
        Point start = new Point((int) Math.round(origin.getX()), (int) Math.round(origin.getY()));
        return closestHit(root, start, direction);
    }


    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        if (root != null) {
            appendNode(root, 0, out);
        }
        return out.toString();
    }

    // we split the pile in half till every triangle has its own leaf
    private Node build(List<Shape> shapes, int depth) {
        if (shapes.size() == 1) {
            return new Node(shapes.get(0));
        }

        // this is to alternate between x and y splits
        boolean onX = depth % 2 == 0;
        List<Shape> left = new ArrayList<>();
        List<Shape> right = new ArrayList<>();

        if (splitMethod == SplitMethod.SPLIT_MIDRANGE) {
            // find the outer edges of everything 
            double lower = Double.MAX_VALUE;
            double upper = -Double.MAX_VALUE;
            for (Shape shape : shapes) {
                if (onX) {
                    lower = Math.min(lower, shape.getMinSurroundingPoint().getX());
                    upper = Math.max(upper, shape.getMaxSurroundingPoint().getX());
                } else {
                    lower = Math.min(lower, shape.getMinSurroundingPoint().getY());
                    upper = Math.max(upper, shape.getMaxSurroundingPoint().getY());
                }
            }
            double cut = (lower + upper) / 2;

            // figuring if it is left or right child based on center
            List<Shape> ties = new ArrayList<>();
            for (Shape shape : shapes) {
                double pos;
                if (onX) {
                    pos = shape.getCenter().getX();
                } else {
                    pos = shape.getCenter().getY();
                }

                if (pos < cut) {
                    left.add(shape);
                } else if (pos > cut) {
                    right.add(shape);
                } else {
                    ties.add(shape);
                }
            }

            // on the line goes to the smaller side
            for (Shape shape : ties) {
                if (left.size() <= right.size()) {
                    left.add(shape);
                } else {
                    right.add(shape);
                }
            }

            
            if (left.isEmpty() || right.isEmpty()) {
                List<Shape> from;
                List<Shape> to;
                if (left.isEmpty()) {
                    from = right;
                    to = left;
                } else {
                    from = left;
                    to = right;
                }

                Shape closest = null;
                double minGap = Double.MAX_VALUE;
                for (Shape shape : from) {
                    double pos;
                    if (onX) {
                        pos = shape.getCenter().getX();
                    } else {
                        pos = shape.getCenter().getY();
                    }

                    double gap = Math.abs(pos - cut);
                    if (gap < minGap) {
                        minGap = gap;
                        closest = shape;
                    }
                }
                from.remove(closest);
                to.add(closest);
            }
        } else {
            // sort by center and chop the list in half
            shapes.sort(new Comparator<Shape>() {
                @Override
                public int compare(Shape one, Shape two) {
                    if (onX) {
                        return Double.compare(one.getCenter().getX(), two.getCenter().getX());
                    } else {
                        return Double.compare(one.getCenter().getY(), two.getCenter().getY());
                    }
                }
            });

            int mid = shapes.size() / 2;
            left.addAll(shapes.subList(0, mid));
            right.addAll(shapes.subList(mid, shapes.size()));
        }

        return new Node(build(left, depth + 1), build(right, depth + 1));
    }

    //// finds the best leaf to pair the new triangle with, then rebalances on the way up
    private Node insertInto(Node node, Shape shape) {
        if (node.shape != null) {
            return new Node(node, new Node(shape));
        }

        boolean leftLeaf = node.left.shape != null;
        boolean rightLeaf = node.right.shape != null;
        boolean goLeft;
        //covers the 4 rules from the spec we had to do
        if (leftLeaf && !rightLeaf) {
            goLeft = true;
        } else if (rightLeaf && !leftLeaf) {
            goLeft = false;
        } else {
            double leftGrowth = growth(node.left.box, shape);
            double rightGrowth = growth(node.right.box, shape);

            if (leftGrowth < rightGrowth) {
                goLeft = true;
            } else if (rightGrowth < leftGrowth) {
                goLeft = false;
            } else if (node.left.height < node.right.height) {
                goLeft = true;
            } else if (node.right.height < node.left.height) {
                goLeft = false;
            } else {
                goLeft = random.nextBoolean();
            }
        }

        if (goLeft) {
            node.left = insertInto(node.left, shape);
        } else {
            node.right = insertInto(node.right, shape);
        }
        return rebalance(node);
    }

   
    private Node removeFrom(Node node, Shape shape) {
        if (node.shape != null) {
            if (!found && node.shape.equals(shape)) {
                found = true;
                return null;
            }
            return node;
        }

        node.left = removeFrom(node.left, shape);
        if (!found) {
            node.right = removeFrom(node.right, shape);
        }


        if (node.left == null) {
            return node.right;
        }
        if (node.right == null) {
            return node.left;
        }
        return rebalance(node);
    }

    // grabs every triangle under here that has the point in it
    private void collect(Node node, Point2D.Double point, Set<Shape> hits) {
        // point's not in this box so nothing below can have it
        if (!node.box.containsPoint(point)) {
            return;
        }

        if (node.shape != null) {
            if (node.shape.containsPoint(point)) {
                hits.add(node.shape);
            }
            return;
        }

        collect(node.left, point, hits);
        collect(node.right, point, hits);
    }

    // closest triangle under here that the ray hits
    private Shape closestHit(Node node, Point start, Vector2D direction) {
        if (node.shape != null) {
            if (node.shape.findIntersection(start, direction) != null) {
                return node.shape;
            }
            return null;
        }

        // ray never touches this box, skip it
        if (!node.box.doesRayIntersect(start, direction)) {
            return null;
        }

        Shape leftHit = closestHit(node.left, start, direction);
        Shape rightHit = closestHit(node.right, start, direction);
        if (leftHit == null) {
            return rightHit;
        }
        if (rightHit == null) {
            return leftHit;
        }

        // both sides hit, keep the closer one
        double leftDist = BVH.distanceBetweenPoints(start, leftHit.findIntersection(start, direction));
        double rightDist = BVH.distanceBetweenPoints(start, rightHit.findIntersection(start, direction));
        if (leftDist <= rightDist) {
            return leftHit;
        }
        return rightHit;
    }

    // writes this node + everything under it, tabs for depth
    private void appendNode(Node node, int depth, StringBuilder out) {
        for (int tab = 0; tab < depth; tab++) {
            out.append('\t');
        }

        // Triangle's toString already adds the newline
        if (node.shape != null) {
            out.append(node.shape.toString());
            return;
        }

        Point min = node.box.minPos;
        Point max = node.box.maxPos;
        out.append("[(" + min.x + ", " + min.y + "), (" + max.x + ", " + max.y + ")]\n");
        appendNode(node.left, depth + 1, out);
        appendNode(node.right, depth + 1, out);
    }

    
    private double growth(Rectangle box, Shape shape) {
        Point low = shape.getMinSurroundingPoint();
        Point high = shape.getMaxSurroundingPoint();

        double before = (double) (box.maxPos.x - box.minPos.x) * (box.maxPos.y - box.minPos.y);
        double width = Math.max(high.x, box.maxPos.x) - Math.min(low.x, box.minPos.x);
        double height = Math.max(high.y, box.maxPos.y) - Math.min(low.y, box.minPos.y);
        return width * height - before;
    }

    // rebalances the node if it is unbalanced
    private Node rebalance(Node node) {
        node.refresh();
        int balance = node.left.height - node.right.height;

        if (balance > 1) {
        
            if (node.left.right.height > node.left.left.height) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }

        if (balance < -1) {

            if (node.right.left.height > node.right.right.height) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }

        return node;
    }
    // does a right rotation around the given node
    private Node rotateRight(Node node) {
        Node pivot = node.left;
        node.left = pivot.right;
        pivot.right = node;
        node.refresh();
        pivot.refresh();
        return pivot;
    }

    // does a left rotation around the given node
    private Node rotateLeft(Node node) {
        Node pivot = node.right;
        node.right = pivot.left;
        pivot.left = node;
        node.refresh();
        pivot.refresh();
        return pivot;
    }

    // either a leaf with one triangle or a box with two kids
    private static class Node {
        Rectangle box;
        Shape shape;
        Node left;
        Node right;
        int height;

        // makes a leaf, just one triangle and a box hugging it
        Node(Shape shape) {
            this.shape = shape;
            box = new Rectangle(shape.getMinSurroundingPoint(), shape.getMaxSurroundingPoint());
            height = 0;
        }

        // makes a box that sits on top of two kids
        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            refresh();
        }

        // updates the box and height based on the current children
        void refresh() {
            Point low = new Point(Math.min(left.box.minPos.x, right.box.minPos.x), Math.min(left.box.minPos.y, right.box.minPos.y));
            Point high = new Point(Math.max(left.box.maxPos.x, right.box.maxPos.x), Math.max(left.box.maxPos.y, right.box.maxPos.y));
            box = new Rectangle(low, high);
            height = 1 + Math.max(left.height, right.height);
        }
    }
}
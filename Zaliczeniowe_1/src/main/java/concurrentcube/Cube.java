package concurrentcube;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;

public class Cube {

    // Useful 'aliases'
    private static final int SIDE_CNT = 6;
    private static final int TOP = 0;
    private static final int LEFT = 1;
    private static final int FRONT = 2;
    private static final int RIGHT = 3;
    private static final int BACK = 4;
    private static final int BOTTOM = 5;


    public enum Direction {
        CLOCKWISE, COUNTER_CLOCKWISE
    }

    // DEFAULT is a placeholder for following array.
    public enum Orientation {
        HORIZONTAL, VERTICAL, DEFAULT
    }

    // DEFAULT is a placeholder for following array.
    public enum Order {
        IN_ORDER, REVERSE, DEFAULT
    }

    // DEFAULT is a placeholder for following array.
    public enum Layer {
        CORRECT, MIRRORED, DEFAULT
    }

    // looking at the i-th face, what face is in the direction dir of it?
    // for example looking at the 0 (top) face, in the direction 0 (top) is 4 (back), to the 1 (left) is 1 (left) etc.
    // in this array, it would be: looking at the 0 (top) face, in the direction 0 (top) is 4 (back), so arr[0][0] = 4.
    public final static int[][] correlations = {
        {4, 1, 0, 3, 5, 2}, // looking at the 0 (top)    face
        {0, 4, 1, 2, 3, 5}, // looking at the 1 (left)   face
        {0, 1, 2, 3, 4, 5}, // looking at the 2 (front)  face
        {0, 2, 3, 4, 1, 5}, // looking at the 3 (right)  face
        {0, 3, 4, 1, 2, 5}, // looking at the 4 (back)   face
        {2, 1, 5, 3, 0, 4}  // looking at the 5 (bottom) face
    };

    // rotating a layer of the i-th face, what is the orientation of that layer on the face in the direction dir of that face?
    // for example rotating a layer of the 2 (front) face, the orientation of that layer on the face
    // in the direction 0 (top) is HORIZONTAL, in the direction 1 (left) is VERTICAL etc.
    public final static Orientation[][] orientations = {
        {Orientation.HORIZONTAL, Orientation.HORIZONTAL, Orientation.DEFAULT, Orientation.HORIZONTAL, Orientation.DEFAULT, Orientation.HORIZONTAL}, // looking at the 0 (top)    face
        {Orientation.VERTICAL, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL},         // looking at the 1 (left)   face
        {Orientation.HORIZONTAL, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.HORIZONTAL},     // looking at the 2 (front)  face
        {Orientation.VERTICAL, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL},         // looking at the 3 (right)  face
        {Orientation.HORIZONTAL, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.VERTICAL, Orientation.DEFAULT, Orientation.HORIZONTAL},     // looking at the 4 (back)   face
        {Orientation.HORIZONTAL, Orientation.HORIZONTAL, Orientation.DEFAULT, Orientation.HORIZONTAL, Orientation.DEFAULT, Orientation.HORIZONTAL}, // looking at the 5 (bottom) face
    };

    // rotating a layer of the i-th face, are the rotated pieces in clockwise order with the indexes of pieces on the face in the direction dir of that face?
    // for example, rotating a layer of the 2 (front) face, the order of pieces to be rotated on the face
    // in direction 1 (left) is REVERSE, but in the direction 3 (right) is IN_ORDER
    public final static Order[][] orders = {
        {Order.REVERSE, Order.REVERSE, Order.DEFAULT, Order.REVERSE, Order.DEFAULT, Order.REVERSE},    // looking at the 0 (top)    face
        {Order.IN_ORDER, Order.REVERSE, Order.DEFAULT, Order.IN_ORDER, Order.DEFAULT, Order.IN_ORDER}, // looking at the 1 (left)   face
        {Order.IN_ORDER, Order.REVERSE, Order.DEFAULT, Order.IN_ORDER, Order.DEFAULT, Order.REVERSE},  // looking at the 2 (front)  face
        {Order.REVERSE, Order.REVERSE, Order.DEFAULT, Order.IN_ORDER, Order.DEFAULT, Order.REVERSE},   // looking at the 3 (right)  face
        {Order.REVERSE, Order.REVERSE, Order.DEFAULT, Order.IN_ORDER, Order.DEFAULT, Order.IN_ORDER},  // looking at the 4 (back)   face
        {Order.IN_ORDER, Order.IN_ORDER, Order.DEFAULT, Order.IN_ORDER, Order.DEFAULT, Order.IN_ORDER} // looking at the 5 (bottom) face
    };

    // rotating a k-th layer of the i-th face, is the corresponding column / row of the face in the direction dir
    // also k-th (CORRECT), or is it symmetrical and should be MIRRORED?
    // for example, rotating k-th layer of the 2 (front) face, we take k-th row from the face in the direction 5 (bottom),
    // but from the face in the direction 0 (top) we take the (size - k - 1)-st row.
    public final static Layer[][] layers = {
        {Layer.CORRECT, Layer.CORRECT, Layer.DEFAULT, Layer.CORRECT, Layer.DEFAULT, Layer.CORRECT},    // looking at the 0 (top)    face
        {Layer.CORRECT, Layer.MIRRORED, Layer.DEFAULT, Layer.CORRECT, Layer.DEFAULT, Layer.CORRECT},   // looking at the 1 (left)   face
        {Layer.MIRRORED, Layer.MIRRORED, Layer.DEFAULT, Layer.CORRECT, Layer.DEFAULT, Layer.CORRECT},  // looking at the 2 (front)  face
        {Layer.MIRRORED, Layer.MIRRORED, Layer.DEFAULT, Layer.CORRECT, Layer.DEFAULT, Layer.MIRRORED}, // looking at the 3 (right)  face
        {Layer.CORRECT, Layer.MIRRORED, Layer.DEFAULT, Layer.CORRECT, Layer.DEFAULT, Layer.MIRRORED},  // looking at the 4 (back)   face
        {Layer.MIRRORED, Layer.MIRRORED, Layer.DEFAULT, Layer.MIRRORED, Layer.DEFAULT, Layer.MIRRORED} // looking at the 5 (bottom) face
    };

    // Data acquired from constructor
    private int size = 0;
    private BiConsumer<Integer, Integer> beforeRotation;
    private BiConsumer<Integer, Integer> afterRotation;
    private Runnable beforeShowing;
    private Runnable afterShowing;

    // Data about sides of this cube
    private final ArrayList<Side> sides = new ArrayList<>();

    // Semaphores and variables used for solution's concurrency
    private final static Semaphore mutex = new Semaphore(1, true);
    private final static Semaphore guardian = new Semaphore(0, true);
    private final static ArrayList<Semaphore> layer_queue = new ArrayList<>();

    private static int previous_side = -1;
    private static int rotating_previous_axis = 0;


    public Cube() {}

    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {

        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;

        for (int color = 0; color < SIDE_CNT; color++) {
            sides.add(new Side(size, color));
        }

        for (int i = 0; i < size; i++) {
            layer_queue.add(new Semaphore(1, true)); // could be false, but this way it is a lot easier
                                                                // to debug and see the ordering of rotations
        }
    }

    private void perform_rotate(int side, int layer) {
        beforeRotation.accept(side, layer);

        if (layer == 0) {
            sides.get(correlations[side][FRONT]).rotate_front_layer(Direction.CLOCKWISE);
        }

        if (layer == size - 1) {
            sides.get(correlations[side][BACK]).rotate_front_layer(Direction.COUNTER_CLOCKWISE);
        }

        ArrayList<Piece> left_pieces = sides.get(correlations[side][LEFT]).get_layer_pieces(side, layer, LEFT);
        ArrayList<Piece> top_pieces = sides.get(correlations[side][TOP]).get_layer_pieces(side, layer, TOP);
        ArrayList<Piece> right_pieces = sides.get(correlations[side][RIGHT]).get_layer_pieces(side, layer, RIGHT);
        ArrayList<Piece> bottom_pieces = sides.get(correlations[side][BOTTOM]).get_layer_pieces(side, layer, BOTTOM);

        sides.get(side).rotate_layer_pieces(left_pieces, top_pieces, right_pieces, bottom_pieces);

        afterRotation.accept(side, layer);
    }

    private String perform_show() {
        beforeShowing.run();

        String s = "";
        for (Side side : sides) {
            s = s.concat(side.get_side());
        }

        afterShowing.run();
        return s;
    }

    public void rotate(int side, int layer) throws InterruptedException {
        try {
            mutex.acquire();
            if (previous_side != side && previous_side != correlations[side][BACK]) {
                try {
                    guardian.acquire(rotating_previous_axis);
                    previous_side = side;
                    rotating_previous_axis = 0;
                } catch (InterruptedException e) {
                    throw new InterruptedException();
                }
            }
            rotating_previous_axis++;
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            mutex.release();
        }
        try {
            layer_queue.get(previous_side == side ? layer : size - layer - 1).acquire();
            perform_rotate(side, layer);
            layer_queue.get(previous_side == side ? layer : size - layer - 1).release();
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            guardian.release();
        }
    }

    public String show() throws InterruptedException {
        try {
            mutex.acquire();
            if (previous_side != -1) {
                try {
                    guardian.acquire(rotating_previous_axis);
                    previous_side = -1;
                    rotating_previous_axis = 0;
                } catch (InterruptedException e) {
                    throw new InterruptedException();
                }
            }
            rotating_previous_axis++;
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            mutex.release();
        }
        String result = perform_show();
        guardian.release();
        return result;
    }

}
package concurrentcube;

import java.util.ArrayList;

public class Side extends Cube {

    private final int size;
    private final Piece[][] pieces;

    public Side(int size, int color) {
        this.size = size;
        this.pieces = new Piece[size][size];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                this.pieces[row][col] = new Piece(color);
            }
        }
    }

    private void rotate_pieces(Piece p1, Piece p2, Piece p3, Piece p4, Direction direction) {

        int buffer = p1.get_color();
        switch (direction) {
            case CLOCKWISE:
                p1.set_color(p4.get_color());
                p4.set_color(p3.get_color());
                p3.set_color(p2.get_color());
                p2.set_color(buffer);
                break;

            case COUNTER_CLOCKWISE:
                p1.set_color(p2.get_color());
                p2.set_color(p3.get_color());
                p3.set_color(p4.get_color());
                p4.set_color(buffer);
                break;
        }
    }

    // direction here answers the question "in which from the requesting side am I"?
    public ArrayList<Piece> get_layer_pieces(int requesting_side, int layer_number, int direction) {

        Orientation orientation  = orientations[requesting_side][direction];
        Order order = orders[requesting_side][direction];
        Layer symmetry = layers[requesting_side][direction];

        ArrayList<Piece> layer_pieces = new ArrayList<>();
        int layer = (symmetry == Layer.CORRECT ? layer_number : size - layer_number - 1);

        for (int i = 0; i < size; i++) {
            int idx = (order == Order.IN_ORDER ? i : size - i - 1);
            Piece p = (orientation == Orientation.HORIZONTAL ? pieces[layer][idx] : pieces[idx][layer]);
            layer_pieces.add(p);
        }

        return layer_pieces;
    }

    public void rotate_layer_pieces(
            ArrayList<Piece> left_pieces,
            ArrayList<Piece> top_pieces,
            ArrayList<Piece> right_pieces,
            ArrayList<Piece> bottom_pieces) {

        for (int i = 0; i < size; i++) {
            Piece p1 = left_pieces.get(i);
            Piece p2 = top_pieces.get(i);
            Piece p3 = right_pieces.get(i);
            Piece p4 = bottom_pieces.get(i);

            rotate_pieces(p1, p2, p3, p4, Direction.CLOCKWISE);
        }
    }

    // the pieces are rotated in a spiral pattern, from the outside to the inside
    public void rotate_front_layer(Direction direction) {
        int start_col = 0;

        for (int row = 0; row < size / 2; row++, start_col++) {
            for (int col = start_col; col < size - start_col - 1; col++) {
                Piece p1 = pieces[row][col];
                Piece p2 = pieces[col][size - row - 1];
                Piece p3 = pieces[size - row - 1][size - col - 1];
                Piece p4 = pieces[size - col - 1][row];

                rotate_pieces(p1, p2, p3, p4, direction);
            }
        }
    }

    public String get_side() {
        StringBuilder builder = new StringBuilder();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                builder.append(pieces[row][col].get_color());
            }
        }

        return builder.toString();
    }

}

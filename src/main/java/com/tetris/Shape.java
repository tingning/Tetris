package com.tetris;

/**
 * Represents a single tetromino: its type and the (x, y) coordinates
 * of its four blocks relative to a rotation pivot.
 */
public class Shape {

    public enum Tetromino {
        NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, JShape
    }

    private Tetromino pieceShape;
    private final int[][] coords;

    // Block offsets per Tetromino ordinal (NoShape, ZShape, SShape, LineShape,
    // TShape, SquareShape, LShape, JShape), in the same order as the enum.
    private static final int[][][] COORDS_TABLE = {
        {{0, 0}, {0, 0}, {0, 0}, {0, 0}},
        {{0, -1}, {0, 0}, {-1, 0}, {-1, 1}},
        {{0, -1}, {0, 0}, {1, 0}, {1, 1}},
        {{0, -1}, {0, 0}, {0, 1}, {0, 2}},
        {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},
        {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        {{-1, -1}, {0, -1}, {0, 0}, {0, 1}},
        {{1, -1}, {0, -1}, {0, 0}, {0, 1}}
    };

    public Shape() {
        coords = new int[4][2];
        setShape(Tetromino.NoShape);
    }

    public void setShape(Tetromino shape) {
        for (int i = 0; i < 4; i++) {
            System.arraycopy(COORDS_TABLE[shape.ordinal()][i], 0, coords[i], 0, 2);
        }
        pieceShape = shape;
    }

    public Tetromino getShape() {
        return pieceShape;
    }

    public int x(int index) {
        return coords[index][0];
    }

    public int y(int index) {
        return coords[index][1];
    }

    private void setX(int index, int x) {
        coords[index][0] = x;
    }

    private void setY(int index, int y) {
        coords[index][1] = y;
    }

    public int minX() {
        int m = coords[0][0];
        for (int i = 1; i < 4; i++) {
            m = Math.min(m, coords[i][0]);
        }
        return m;
    }

    public int maxX() {
        int m = coords[0][0];
        for (int i = 1; i < 4; i++) {
            m = Math.max(m, coords[i][0]);
        }
        return m;
    }

    public int minY() {
        int m = coords[0][1];
        for (int i = 1; i < 4; i++) {
            m = Math.min(m, coords[i][1]);
        }
        return m;
    }

    public int maxY() {
        int m = coords[0][1];
        for (int i = 1; i < 4; i++) {
            m = Math.max(m, coords[i][1]);
        }
        return m;
    }

    public Shape rotateLeft() {
        // Squares look identical after rotation, so skip the transform.
        if (pieceShape == Tetromino.SquareShape) {
            return this;
        }
        Shape result = new Shape();
        result.pieceShape = pieceShape;
        // 90-degree rotation of (x, y) -> (y, -x) about the pivot.
        for (int i = 0; i < 4; i++) {
            result.setX(i, y(i));
            result.setY(i, -x(i));
        }
        return result;
    }

    public Shape rotateRight() {
        if (pieceShape == Tetromino.SquareShape) {
            return this;
        }
        Shape result = new Shape();
        result.pieceShape = pieceShape;
        // 90-degree rotation of (x, y) -> (-y, x) about the pivot.
        for (int i = 0; i < 4; i++) {
            result.setX(i, -y(i));
            result.setY(i, x(i));
        }
        return result;
    }
}

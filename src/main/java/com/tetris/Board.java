package com.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Game board panel: owns the falling-piece state, the settled grid, scoring,
 * and rendering. Logical rows grow upward (row 0 is the floor), but they are
 * painted top-down, so drawing code flips the row index.
 */
public class Board extends JPanel implements ActionListener {

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 22;
    private static final int INITIAL_DELAY = 400;
    private static final int FLASH_DELAY = 100;
    // Number of visibility toggles the completed rows blink through before being removed.
    private static final int FLASH_TOTAL_TICKS = 6;

    private Timer timer;
    private Timer flashTimer;
    private boolean isPaused = false;
    private boolean isStarted = false;
    private int curX = 0;
    private int curY = 0;
    private int numLinesRemoved = 0;
    private int score = 0;
    private int level = 1;

    // Rows currently blinking after a line clear, and whether this tick renders them lit.
    private List<Integer> flashingRows = new ArrayList<>();
    private boolean flashVisible = true;
    private int flashTicks = 0;

    private Shape curPiece;
    // A fresh Shape instance each time it's picked, so promoting it to curPiece
    // never aliases an object that a later pick could still mutate.
    private Shape nextPiece;
    // Flattened BOARD_WIDTH x BOARD_HEIGHT grid of settled squares, indexed via shapeAt().
    private Shape.Tetromino[] board;
    private final Random random = new Random();
    private final Tetris parent;

    public Board(Tetris parent) {
        this.parent = parent;
        setFocusable(true);
        curPiece = new Shape();
        timer = new Timer(INITIAL_DELAY, this);
        board = new Shape.Tetromino[BOARD_WIDTH * BOARD_HEIGHT];
        addKeyListener(new TAdapter());
        clearBoard();
        nextPiece = randomShape();
    }

    private int squareWidth() {
        return getSize().width / BOARD_WIDTH;
    }

    private int squareHeight() {
        return getSize().height / BOARD_HEIGHT;
    }

    private Shape.Tetromino shapeAt(int x, int y) {
        return board[y * BOARD_WIDTH + x];
    }

    public void start() {
        if (isPaused) {
            return;
        }
        isStarted = true;
        numLinesRemoved = 0;
        score = 0;
        level = 1;
        timer.setDelay(INITIAL_DELAY);
        clearBoard();
        nextPiece = randomShape();
        newPiece();
        timer.start();
        updateStatus();
    }

    private void pause() {
        if (!isStarted) {
            return;
        }
        isPaused = !isPaused;
        if (isPaused) {
            timer.stop();
            parent.setStatusText("Paused");
        } else {
            timer.start();
            updateStatus();
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension size = getSize();
        int boardTop = size.height - BOARD_HEIGHT * squareHeight();

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            // Logical row 0 is the floor; flip so it paints at the bottom of the panel.
            int row = BOARD_HEIGHT - i - 1;
            boolean isFlashingRow = flashingRows.contains(row);
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Shape.Tetromino shape = shapeAt(j, row);
                if (shape != Shape.Tetromino.NoShape) {
                    if (isFlashingRow) {
                        // Blink: lit tick draws white, unlit tick draws nothing, giving a flash effect.
                        if (flashVisible) {
                            drawFlashSquare(g, j * squareWidth(), boardTop + i * squareHeight());
                        }
                    } else {
                        drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
                    }
                }
            }
        }

        if (curPiece.getShape() != Shape.Tetromino.NoShape) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, x * squareWidth(), boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(), curPiece.getShape());
            }
        }

        if (isPaused) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString("PAUSED", size.width / 2 - 60, size.height / 2);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        oneLineDown();
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_WIDTH * BOARD_HEIGHT; i++) {
            board[i] = Shape.Tetromino.NoShape;
        }
    }

    private Shape randomShape() {
        // Skip index 0 (NoShape) so only real tetrominoes are ever chosen.
        Shape.Tetromino[] values = Shape.Tetromino.values();
        int r = random.nextInt(values.length - 1) + 1;
        Shape shape = new Shape();
        shape.setShape(values[r]);
        return shape;
    }

    private void newPiece() {
        curPiece = nextPiece;
        nextPiece = randomShape();
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Shape.Tetromino.NoShape);
            timer.stop();
            isStarted = false;
            parent.setStatusText("Game over. Final score: " + score);
        }
        parent.repaintNext(nextPiece);
    }

    /**
     * Checks whether newPiece fits at (newX, newY) without going out of bounds
     * or overlapping settled squares, and commits the move if so.
     */
    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }
            if (shapeAt(x, y) != Shape.Tetromino.NoShape) {
                return false;
            }
        }

        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1)) {
            pieceDropped();
        }
    }

    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1)) {
                break;
            }
            newY--;
        }
        pieceDropped();
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[y * BOARD_WIDTH + x] = curPiece.getShape();
        }
        // Hide the now-settled piece immediately; this also blocks input via the
        // TAdapter's NoShape guard while the completed-line flash plays out.
        curPiece.setShape(Shape.Tetromino.NoShape);

        List<Integer> full = detectFullLines();
        if (full.isEmpty()) {
            newPiece();
        } else {
            startLineFlash(full);
        }
    }

    // Blinks the completed rows a few times before they're actually removed.
    private void startLineFlash(List<Integer> full) {
        SoundUtil.playLineClearSound();
        timer.stop();
        flashingRows = full;
        flashVisible = true;
        flashTicks = 0;
        repaint();

        flashTimer = new Timer(FLASH_DELAY, e -> onFlashTick(full));
        flashTimer.start();
    }

    private void onFlashTick(List<Integer> full) {
        flashVisible = !flashVisible;
        flashTicks++;
        repaint();

        if (flashTicks >= FLASH_TOTAL_TICKS) {
            flashTimer.stop();
            flashingRows = new ArrayList<>();
            removeLines(full);
            newPiece();
            if (isStarted && !isPaused) {
                timer.start();
            }
        }
    }

    private List<Integer> detectFullLines() {
        List<Integer> full = new ArrayList<>();
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Shape.Tetromino.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                full.add(i);
            }
        }
        return full;
    }

    // Removes the given (already-detected) full rows and settles rows above down to fill the gap.
    private void removeLines(List<Integer> full) {
        int numFullLines = full.size();

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Shape.Tetromino.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                // Shift every row above i down by one, then clear the vacated top row.
                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[k * BOARD_WIDTH + j] = shapeAt(j, k + 1);
                    }
                }
                for (int j = 0; j < BOARD_WIDTH; j++) {
                    board[(BOARD_HEIGHT - 1) * BOARD_WIDTH + j] = Shape.Tetromino.NoShape;
                }
                // Row i now holds what used to be row i+1; re-examine it before continuing downward.
                i++;
            }
        }

        numLinesRemoved += numFullLines;
        // Standard Tetris-style scoring: reward multi-line clears disproportionately, scaled by level.
        score += switch (numFullLines) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
        } * level;

        // Level up every 10 lines; each level speeds the drop timer up, floored at 100ms.
        int newLevel = 1 + numLinesRemoved / 10;
        if (newLevel != level) {
            level = newLevel;
            timer.setDelay(Math.max(100, INITIAL_DELAY - (level - 1) * 30));
        }

        updateStatus();
    }

    private void updateStatus() {
        parent.setStatusText("Score: " + score + "   Lines: " + numLinesRemoved + "   Level: " + level);
    }

    private void drawFlashSquare(Graphics g, int x, int y) {
        int w = squareWidth();
        int h = squareHeight();
        g.setColor(Color.WHITE);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
    }

    private void drawSquare(Graphics g, int x, int y, Shape.Tetromino shape) {
        Color[] colors = {
            new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
            new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0)
        };

        Color color = colors[shape.ordinal()];
        int w = squareWidth();
        int h = squareHeight();

        g.setColor(color);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + h - 1, x, y);
        g.drawLine(x, y, x + w - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y + h - 1, x + w - 1, y + 1);
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!isStarted || curPiece.getShape() == Shape.Tetromino.NoShape) {
                return;
            }

            int keycode = e.getKeyCode();

            // Pause must be handled before the isPaused guard below, or it could never unpause.
            if (keycode == 'P' || keycode == 'p') {
                pause();
                return;
            }
            if (isPaused) {
                return;
            }

            switch (keycode) {
                case KeyEvent.VK_LEFT -> tryMove(curPiece, curX - 1, curY);
                case KeyEvent.VK_RIGHT -> tryMove(curPiece, curX + 1, curY);
                case KeyEvent.VK_UP -> tryMove(curPiece.rotateLeft(), curX, curY);
                case KeyEvent.VK_DOWN -> oneLineDown();
                case KeyEvent.VK_SPACE -> dropDown();
                default -> {
                }
            }
        }
    }
}

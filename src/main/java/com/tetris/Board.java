package com.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

public class Board extends JPanel implements ActionListener {

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 22;
    private static final int INITIAL_DELAY = 400;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isPaused = false;
    private boolean isStarted = false;
    private int curX = 0;
    private int curY = 0;
    private int numLinesRemoved = 0;
    private int score = 0;
    private int level = 1;

    private Shape curPiece;
    private Shape nextPiece;
    private Shape.Tetromino[] board;
    private final Random random = new Random();
    private final Tetris parent;

    public Board(Tetris parent) {
        this.parent = parent;
        setFocusable(true);
        curPiece = new Shape();
        nextPiece = new Shape();
        timer = new Timer(INITIAL_DELAY, this);
        board = new Shape.Tetromino[BOARD_WIDTH * BOARD_HEIGHT];
        addKeyListener(new TAdapter());
        clearBoard();
        pickNextPiece();
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
        isFallingFinished = false;
        numLinesRemoved = 0;
        score = 0;
        level = 1;
        timer.setDelay(INITIAL_DELAY);
        clearBoard();
        pickNextPiece();
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
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Shape.Tetromino shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Shape.Tetromino.NoShape) {
                    drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
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
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_WIDTH * BOARD_HEIGHT; i++) {
            board[i] = Shape.Tetromino.NoShape;
        }
    }

    private void pickNextPiece() {
        Shape.Tetromino[] values = Shape.Tetromino.values();
        int r = random.nextInt(values.length - 1) + 1;
        nextPiece.setShape(values[r]);
    }

    private void newPiece() {
        curPiece = nextPiece;
        pickNextPiece();
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
        removeFullLines();
        if (!isFallingFinished) {
            newPiece();
        }
    }

    private void removeFullLines() {
        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Shape.Tetromino.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                numFullLines++;
                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[k * BOARD_WIDTH + j] = shapeAt(j, k + 1);
                    }
                }
                for (int j = 0; j < BOARD_WIDTH; j++) {
                    board[(BOARD_HEIGHT - 1) * BOARD_WIDTH + j] = Shape.Tetromino.NoShape;
                }
                i++;
            }
        }

        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;
            score += switch (numFullLines) {
                case 1 -> 100;
                case 2 -> 300;
                case 3 -> 500;
                case 4 -> 800;
                default -> 0;
            } * level;

            int newLevel = 1 + numLinesRemoved / 10;
            if (newLevel != level) {
                level = newLevel;
                timer.setDelay(Math.max(100, INITIAL_DELAY - (level - 1) * 30));
            }

            isFallingFinished = true;
            curPiece.setShape(Shape.Tetromino.NoShape);
            updateStatus();
            repaint();
        }
    }

    private void updateStatus() {
        parent.setStatusText("Score: " + score + "   Lines: " + numLinesRemoved + "   Level: " + level);
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

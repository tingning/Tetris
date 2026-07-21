package com.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class NextPiecePanel extends JPanel {

    private static final int CELL = 36;
    private Shape piece = new Shape();

    public NextPiecePanel() {
        setPreferredSize(new Dimension(CELL * 6, CELL * 6));
        setBackground(new Color(144, 238, 144));
    }

    public void setPiece(Shape piece) {
        this.piece = piece;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (piece == null || piece.getShape() == Shape.Tetromino.NoShape) {
            return;
        }

        Color[] colors = {
            new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
            new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0)
        };
        Color color = colors[piece.getShape().ordinal()];

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        for (int i = 0; i < 4; i++) {
            int x = centerX + piece.x(i) * CELL - CELL / 2;
            int y = centerY - piece.y(i) * CELL - CELL / 2;
            g.setColor(color);
            g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
            g.setColor(color.brighter());
            g.drawLine(x, y + CELL - 1, x, y);
            g.drawLine(x, y, x + CELL - 1, y);
            g.setColor(color.darker());
            g.drawLine(x + 1, y + CELL - 1, x + CELL - 1, y + CELL - 1);
            g.drawLine(x + CELL - 1, y + CELL - 1, x + CELL - 1, y + 1);
        }
    }
}

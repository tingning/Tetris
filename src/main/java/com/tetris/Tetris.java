package com.tetris;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Top-level window: wires together the board, side panel, and status bar.
public class Tetris extends JFrame {

    private final JLabel statusBar = new JLabel(" Press Enter to start");
    private final NextPiecePanel nextPiecePanel = new NextPiecePanel();
    private Board board;

    public Tetris() {
        setTitle("Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // The board owns the game loop and reports back to this frame via
        // setStatusText/repaintNext, so it must be constructed with a reference to it.
        board = new Board(this);
        setLayout(new BorderLayout());
        add(board, BorderLayout.CENTER);

        // Side panel: shows the next piece preview plus a static controls legend.
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setBackground(Color.DARK_GRAY);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel nextLabel = new JLabel("Next", JLabel.CENTER);
        nextLabel.setForeground(Color.WHITE);
        nextLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel helpLabel = new JLabel("<html>Controls:<br>&larr; &rarr; move<br>&uarr; rotate<br>&darr; soft drop<br>Space hard drop<br>P pause<br>Enter restart</html>");
        helpLabel.setForeground(Color.LIGHT_GRAY);
        helpLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        helpLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        helpLabel.setVerticalAlignment(JLabel.TOP);

        sidePanel.add(nextLabel, BorderLayout.NORTH);
        sidePanel.add(nextPiecePanel, BorderLayout.CENTER);
        sidePanel.add(helpLabel, BorderLayout.SOUTH);

        add(sidePanel, BorderLayout.EAST);

        statusBar.setForeground(Color.WHITE);
        statusBar.setBackground(Color.BLACK);
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusBar, BorderLayout.SOUTH);

        // Enter starts (or restarts) the game; Board handles its own listener
        // for in-game controls, so this one only needs to cover the start action.
        board.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    board.start();
                    board.requestFocusInWindow();
                }
            }
        });

        setSize(600, 900);
        setLocationRelativeTo(null);
        // Board must hold keyboard focus, not the frame, or key events won't reach it.
        board.requestFocusInWindow();
    }

    public void setStatusText(String text) {
        statusBar.setText(" " + text);
    }

    public void repaintNext(Shape piece) {
        nextPiecePanel.setPiece(piece);
    }

    public static void main(String[] args) {
        // Swing components must be created/mutated on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            Tetris game = new Tetris();
            game.setVisible(true);
        });
    }
}

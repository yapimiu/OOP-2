package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BoardPanel extends JPanel {
    private static final int S = 72;
    private final CheckersGame game;
    private final JFrame frame;
    private final GameMode mode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BoardPanel(CheckersGame game, JFrame frame, GameMode mode) {
        this.game = game;
        this.frame = frame;
        this.mode = mode;
        setPreferredSize(new Dimension(8 * S + 40, 8 * S + 40));
        setBackground(new Color(139, 115, 85));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isHumanTurn()) {
                    int x = e.getX() - 20;
                    int y = e.getY() - 20;
                    if (x >= 0 && y >= 0) {
                        int c = x / S;
                        int r = y / S;
                        if (c < 8 && r < 8) {
                            game.click(r, c);
                            updateTitle();
                            repaint();
                            checkAIMove();
                        }
                    }
                }
            }
        });

        updateTitle();
        if (!isHumanTurn()) {
            SwingUtilities.invokeLater(this::makeAIMoveDelayed);
        }
    }

    private boolean isHumanTurn() {
        return mode == GameMode.HUMAN_VS_HUMAN ||
                (mode == GameMode.AI_VS_HUMAN_WHITE && game.isWhiteTurn()) ||
                (mode == GameMode.AI_VS_HUMAN_BLACK && !game.isWhiteTurn());
    }

    private void checkAIMove() {
        if (!isHumanTurn() && game.getWinner() == null) {
            makeAIMoveDelayed();
        }
    }

    private void makeAIMoveDelayed() {
        executor.submit(() -> {
            try { Thread.sleep(800); } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> {
                game.makeAIMove();
                updateTitle();
                repaint();
                if (mode == GameMode.AI_VS_AI && game.getWinner() == null) {
                    makeAIMoveDelayed();
                }
            });
        });
    }

    private void updateTitle() {
        String title = "Русские шашки — " + game.getTurnText();
        if (mode == GameMode.AI_VS_AI) title += " (ИИ vs ИИ)";
        else if (mode == GameMode.AI_VS_HUMAN_WHITE) title += " (вы за белых)";
        else if (mode == GameMode.AI_VS_HUMAN_BLACK) title += " (вы за чёрных)";
        frame.setTitle(title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offsetX = 20, offsetY = 20;

        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            g2.setColor((r + c) % 2 == 0 ? new Color(240, 217, 181) : new Color(181, 136, 99));
            g2.fillRect(offsetX + c * S, offsetY + r * S, S, S);
        }

        // выбранная шашка — зелёная обводка
        Move sel = game.getSelected();
        if (sel != null && sel.fromRow >= 0) {
            int x = offsetX + sel.fromCol * S + 8;
            int y = offsetY + sel.fromRow * S + 8;
            int size = S - 16;
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(x - 4, y - 4, size + 8, size + 8);
            g2.setStroke(new BasicStroke(1));
        }

        // возможные ходы
        for (Move m : game.getPossibleMoves()) {
            int tx = offsetX + m.toCol * S + 12;
            int ty = offsetY + m.toRow * S + 12;
            g2.setColor(new Color(255, 255, 0, 220));
            g2.fillOval(tx, ty, S - 24, S - 24);

            if (Math.abs(m.toRow - m.fromRow) > 1) {
                int dr = Integer.signum(m.toRow - m.fromRow);
                int dc = Integer.signum(m.toCol - m.fromCol);
                int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));
                for (int i = 1; i < steps; i++) {
                    int pr = m.fromRow + dr * i;
                    int pc = m.fromCol + dc * i;
                    int px = offsetX + pc * S + 20;
                    int py = offsetY + pr * S + 20;
                    g2.setColor(new Color(100, 255, 100, 80));
                    g2.fillRect(px, py, S - 40, S - 40);
                }
            }
        }

        // шашки
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = game.get(r, c);
            if (p == Piece.EMPTY) continue;

            int x = offsetX + c * S + 8;
            int y = offsetY + r * S + 8;
            int size = S - 16;

            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(x + 4, y + 4, size, size);

            g2.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
            g2.fillOval(x, y, size, size);

            g2.setColor(p.isWhite() ? new Color(40, 40, 40) : Color.WHITE);
            for (int i = 0; i < 3; i++) g2.drawOval(x - i, y - i, size + 2 * i, size + 2 * i);

            if (p.isKing()) {
                g2.setColor(p.isWhite() ? Color.BLACK : Color.YELLOW);
                g2.setFont(new Font("Serif", Font.BOLD, 44));
                g2.drawString("K", x + 16, y + 48);
            }
        }

        g2.setColor(new Color(50, 50, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        for (int i = 0; i < 8; i++) {
            g2.drawString(String.valueOf((char) ('a' + i)), offsetX + i * S + 26, offsetY + 8 * S + 18);
            g2.drawString(String.valueOf(8 - i), offsetX - 18, offsetY + i * S + S / 2 + 8);
        }
    }
}
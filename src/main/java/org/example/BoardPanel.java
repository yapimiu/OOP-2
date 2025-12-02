package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel {
    private static final int S = 72;
    private final CheckersGame game;
    private final JFrame frame;

    public BoardPanel(CheckersGame game, JFrame frame) {
        this.game = game;
        this.frame = frame;
        setPreferredSize(new Dimension(8 * S + 40, 8 * S + 40));
        setBackground(new Color(139, 115, 85));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX() - 20;
                int y = e.getY() - 20;
                if (x >= 0 && y >= 0) {
                    int c = x / S;
                    int r = y / S;
                    if (c < 8 && r < 8) {
                        game.click(r, c);
                        frame.setTitle("Русские шашки — " + game.getTurnText());
                        repaint();
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offsetX = 20;
        int offsetY = 20;

        // доска
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                g2.setColor(light ? new Color(240, 217, 181) : new Color(181, 136, 99));
                g2.fillRect(offsetX + c * S, offsetY + r * S, S, S);
            }
        }

        // выбранная шашка
        Move sel = game.getSelected();
        if (sel != null && sel.fromRow >= 0 && sel.fromCol >= 0) {
            int x = offsetX + sel.fromCol * S;
            int y = offsetY + sel.fromRow * S;
            g2.setColor(new Color(0, 255, 0, 130));
            g2.fillRoundRect(x + 4, y + 4, S - 8, S - 8, 20, 20);
        }

        // возможные ходы и путь дамки
        for (Move m : game.getPossibleMoves()) {
            int tx = offsetX + m.toCol * S;
            int ty = offsetY + m.toRow * S;

            // жёлтый круг назначения
            g2.setColor(new Color(255, 255, 0, 220));
            g2.fillOval(tx + 12, ty + 12, S - 24, S - 24);

            // подсветка пути дамки
            if (Math.abs(m.toRow - m.fromRow) > 1 || Math.abs(m.toCol - m.fromCol) > 1) {
                int dr = Integer.signum(m.toRow - m.fromRow);
                int dc = Integer.signum(m.toCol - m.fromCol);
                int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));

                for (int i = 1; i < steps; i++) {
                    int pr = m.fromRow + dr * i;
                    int pc = m.fromCol + dc * i;
                    int px = offsetX + pc * S;
                    int py = offsetY + pr * S;
                    g2.setColor(new Color(100, 255, 100, 80));
                    g2.fillRect(px + 20, py + 20, S - 40, S - 40);
                }
            }
        }

        // шашки
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.get(r, c);
                if (p == Piece.EMPTY) continue;

                int x = offsetX + c * S + 8;
                int y = offsetY + r * S + 8;
                int size = S - 16;

                // тень
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(x + 4, y + 4, size, size);

                // шашка
                g2.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
                g2.fillOval(x, y, size, size);

                // обводка (делаем толще через несколько кругов)
                g2.setColor(p.isWhite() ? new Color(40, 40, 40) : Color.WHITE);
                for (int i = 0; i < 3; i++) {
                    g2.drawOval(x - i, y - i, size + 2 * i, size + 2 * i);
                }

                // корона для дамки
                if (p.isKing()) {
                    g2.setColor(p.isWhite() ? Color.BLACK : new Color(255, 215, 0));
                    g2.setFont(new Font("Serif", Font.BOLD, 44));
                    String crown = "K";
                    FontMetrics fm = g2.getFontMetrics();
                    int cw = fm.stringWidth(crown);
                    g2.drawString(crown, x + (size - cw) / 2, y + 48);
                }
            }
        }

        // подписи a-h, 1-8
        g2.setColor(new Color(50, 50, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        for (int i = 0; i < 8; i++) {
            g2.drawString(String.valueOf((char) ('a' + i)), offsetX + i * S + 26, offsetY + 8 * S + 18);
            g2.drawString(String.valueOf(8 - i), offsetX - 18, offsetY + i * S + S / 2 + 8);
        }
    }
}
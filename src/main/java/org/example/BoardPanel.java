package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BoardPanel extends JPanel {
    private static final int S = 72; // Размер клетки
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
                // Ход человека возможен только если сейчас его очередь и игра не закончилась
                if (isHumanTurn() && game.getWinner() == null) {
                    int x = e.getX() - 20;
                    int y = e.getY() - 20;
                    if (x >= 0 && y >= 0) {
                        int c = x / S;
                        int r = y / S;
                        if (c < 8 && r < 8) {
                            game.click(r, c);
                            updateTitle();
                            repaint();

                            // Проверяем победу и ход ИИ
                            checkGameOver();
                            checkAIMove();
                        }
                    }
                }
            }
        });

        updateTitle();
        // Если игра начинается с хода ИИ
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
                // Ещё раз проверяем победителя перед ходом, чтобы не ходить по пустой доске
                if (game.getWinner() != null) return;

                game.makeAIMove();
                updateTitle();
                repaint();

                checkGameOver();

                // Если режим ИИ против ИИ — продолжаем цикл
                if (mode == GameMode.AI_VS_AI && game.getWinner() == null) {
                    makeAIMoveDelayed();
                } else {
                    // Если сейчас снова очередь ИИ (например, серия взятий или баг), проверяем ещё раз
                    checkAIMove();
                }
            });
        });
    }

    /**
     * Основная логика окончания игры.
     * Показывает диалог и обрабатывает выбор пользователя.
     */
    private void checkGameOver() {
        String winner = game.getWinner();
        if (winner != null) {
            int option = JOptionPane.showConfirmDialog(frame,
                    winner + " победили!\nХотите начать заново?",
                    "Игра окончена",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                game.resetGame();
                updateTitle();
                repaint();

                // Если при рестарте первым должен ходить ИИ — запускаем его
                if (!isHumanTurn()) {
                    makeAIMoveDelayed();
                }
            } else {
                System.exit(0);
            }
        }
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

        // Доска
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            g2.setColor((r + c) % 2 == 0 ? new Color(240, 217, 181) : new Color(181, 136, 99));
            g2.fillRect(offsetX + c * S, offsetY + r * S, S, S);
        }

        // Координаты
        g2.setColor(new Color(50, 50, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        for (int i = 0; i < 8; i++) {
            g2.drawString(String.valueOf((char) ('a' + i)), offsetX + i * S + S/2 - 5, offsetY + 8 * S + 18);
            g2.drawString(String.valueOf(8 - i), offsetX - 15, offsetY + i * S + S / 2 + 5);
        }

        // Выделенная шашка
        Move sel = game.getSelected();
        if (sel != null && sel.fromRow >= 0) {
            int x = offsetX + sel.fromCol * S + 2;
            int y = offsetY + sel.fromRow * S + 2;
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(x, y, S - 4, S - 4);
            g2.setStroke(new BasicStroke(1));
        }

        // Возможные ходы
        for (Move m : game.getPossibleMoves()) {
            int tx = offsetX + m.toCol * S + S/2;
            int ty = offsetY + m.toRow * S + S/2;
            g2.setColor(new Color(0, 255, 0, 150));
            g2.fillOval(tx - 8, ty - 8, 16, 16);

            // Если это взятие — подсвечиваем путь
            if (m.isCapture) {
                g2.setColor(new Color(255, 0, 0, 100));
                g2.fillRect(offsetX + m.toCol * S, offsetY + m.toRow * S, S, S);
            }
        }

        // Шашки
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = game.get(r, c);
            if (p == Piece.EMPTY) continue;

            int x = offsetX + c * S + 8;
            int y = offsetY + r * S + 8;
            int size = S - 16;

            // Тень
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval(x + 3, y + 3, size, size);

            g2.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
            g2.fillOval(x, y, size, size);

            g2.setColor(p.isWhite() ? Color.GRAY : Color.DARK_GRAY);
            g2.drawOval(x, y, size, size);
            g2.drawOval(x + 5, y + 5, size - 10, size - 10);

            if (p.isKing()) {
                g2.setColor(p.isWhite() ? Color.BLACK : Color.YELLOW);
                g2.setFont(new Font("Serif", Font.BOLD, 40));
                FontMetrics fm = g2.getFontMetrics();
                int kw = fm.stringWidth("K");
                int kh = fm.getAscent();
                g2.drawString("K", x + (size - kw)/2, y + (size + kh)/2 - 5);
            }
        }
    }
}
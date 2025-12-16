package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] options = {
                    "1. Человек vs Человек",
                    "2. Компьютер vs Человек (вы за белых)",
                    "3. Компьютер vs Человек (вы за чёрных)",
                    "4. Компьютер vs Компьютер"
            };

            int choice = JOptionPane.showOptionDialog(null,
                    "Выберите режим игры:",
                    "Русские шашки",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);

            GameMode mode = switch (choice) {
                case 1 -> GameMode.AI_VS_HUMAN_WHITE;
                case 2 -> GameMode.AI_VS_HUMAN_BLACK;
                case 3 -> GameMode.AI_VS_AI;
                default -> GameMode.HUMAN_VS_HUMAN;
            };

            CheckersGame game = new CheckersGame();
            JFrame frame = new JFrame("Русские шашки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new BoardPanel(game, frame, mode));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
package org.example;

import javax.swing.*;
import java.util.*;

public class CheckersGame {
    // Доска
    private final Piece[][] board = new Piece[8][8];
    // Чей ход
    // true - белые
    // flase - черные
    private boolean whiteTurn = true;
    // Выбранная шашка
    private Move selected = null;
    // Все доступные ходы
    private final Set<Move> possibleMoves = new HashSet<>();
    // Находится ли игрок в режиме цепочки взятий
    private boolean inCaptureSequence = false;

    public CheckersGame() {
        initBoard();
        updatePossibleMoves();
    }

    private void initBoard() {
        for (int r = 0; r < 8; r++) Arrays.fill(board[r], Piece.EMPTY);
        for (int r = 0; r < 3; r++)
            // Шашки будут именно на черных клетках
            for (int c = (r + 1) % 2; c < 8; c += 2)
                board[r][c] = Piece.BLACK_MAN;
        for (int r = 5; r < 8; r++)
            for (int c = (r + 1) % 2; c < 8; c += 2)
                board[r][c] = Piece.WHITE_MAN;
    }

    public Piece get(int r, int c) { return board[r][c]; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public Move getSelected() { return selected; }
    public Set<Move> getPossibleMoves() { return possibleMoves; }
    public String getTurnText() { return whiteTurn ? "Ход белых" : "Ход чёрных"; }

    public void click(int row, int col) {
        // Если ничего не выбрано, то выбираем шашку
        if (selected == null) {
            selectPiece(row, col);
            // Шашка выбрана, попытка сделать код
        } else {
            if (!tryMove(row, col)) {
                selectPiece(row, col);
            }
        }
        checkWinner();
    }

    // Попытка выбрать шашку
    private void selectPiece(int row, int col) {

        // Если идёт цепочка взятий, игнорируем клик по любой клетке, кроме текущей выбранной шашки (нельзя переключаться на другую).
        if (inCaptureSequence && (selected == null || row != selected.fromRow || col != selected.fromCol)) {
            return;
        }

        Piece p = board[row][col];
        if (p == Piece.EMPTY || p.isWhite() != whiteTurn) {
            selected = null;
            possibleMoves.clear();
            return;
        }

        // Проверяем, есть ли обязательные взятия у кого-либо из шашек текущего игрока
        boolean mustCapture = playerMustCapture();

        if (mustCapture) {
            // Если есть обязательные взятия, показываем только взятия для этой шашки
            List<Move> captures = p.isKing() ? findKingCaptures(row, col) : findManCaptures(row, col);
            if (!captures.isEmpty()) {
                possibleMoves.clear();
                possibleMoves.addAll(captures);
                selected = new Move(row, col, -1, -1);
            } else {
                // Эта шашка не может бить, но есть шашки, которые могут - не позволяем выбрать её
                selected = null;
                possibleMoves.clear();
            }
        } else {
            // Если нет обязательных взятий, показываем все ходы для этой шашки
            List<Move> captures = p.isKing() ? findKingCaptures(row, col) : findManCaptures(row, col);
            List<Move> quietMoves = p.isKing() ? findKingQuietMoves(row, col) : findManQuietMoves(row, col);

            possibleMoves.clear();
            possibleMoves.addAll(captures);
            possibleMoves.addAll(quietMoves);

            if (!possibleMoves.isEmpty()) {
                selected = new Move(row, col, -1, -1);
            }
        }
    }

    private boolean tryMove(int toRow, int toCol) {
        if (selected == null) return false;

        // Ищем выбранный ход среди возможных
        Move chosenMove = null;
        for (Move move : possibleMoves) {
            if (move.fromRow == selected.fromRow && move.fromCol == selected.fromCol &&
                    move.toRow == toRow && move.toCol == toCol) {
                chosenMove = move;
                break;
            }
        }

        if (chosenMove == null) return false;

        // Запоминаем, был ли это ход со взятием
        boolean wasCapture = chosenMove.isCapture;

        // Выполняем ход
        executeMove(chosenMove);

        // Если это был ход со взятием, проверяем возможность продолжения взятия
        if (wasCapture) {
            Piece movedPiece = board[toRow][toCol];
            List<Move> furtherCaptures = movedPiece.isKing() ?
                    findKingCaptures(toRow, toCol) : findManCaptures(toRow, toCol);

            if (!furtherCaptures.isEmpty()) {
                // Можно продолжать бить
                selected = new Move(toRow, toCol, -1, -1);
                possibleMoves.clear();
                possibleMoves.addAll(furtherCaptures);
                inCaptureSequence = true;
                return true;
            }
        }

        // Если нельзя продолжать бить или это был обычный ход, заканчиваем ход
        whiteTurn = !whiteTurn;
        inCaptureSequence = false;
        selected = null;
        updatePossibleMoves();
        return true;
    }

    private void executeMove(Move m) {
        Piece p = board[m.fromRow][m.fromCol];
        board[m.fromRow][m.fromCol] = Piece.EMPTY;
        board[m.toRow][m.toCol] = p;

        // Если это ход со взятием, убираем побитую шашку
        if (m.isCapture) {
            int dr = Integer.signum(m.toRow - m.fromRow);
            int dc = Integer.signum(m.toCol - m.fromCol);
            int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));

            for (int i = 1; i < steps; i++) {
                int r = m.fromRow + dr * i;
                int c = m.fromCol + dc * i;
                if (board[r][c] != Piece.EMPTY) {
                    board[r][c] = Piece.EMPTY;
                    break; // В русских шашках бьется только одна шашка за ход
                }
            }
        }

        // Превращение в дамку
        if ((p == Piece.WHITE_MAN && m.toRow == 0) || (p == Piece.BLACK_MAN && m.toRow == 7)) {
            board[m.toRow][m.toCol] = p == Piece.WHITE_MAN ? Piece.WHITE_KING : Piece.BLACK_KING;
        }
    }

    // Проверяем, должен ли текущий игрок бить
    private boolean playerMustCapture() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    List<Move> captures = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                    if (!captures.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Простые шашки - тихие ходы
    private List<Move> findManQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int dir = board[r][c] == Piece.WHITE_MAN ? -1 : 1; // Белые идут вверх, черные вниз

        for (int dc = -1; dc <= 1; dc += 2) {
            int nr = r + dir;
            int nc = c + dc;
            if (isValid(nr, nc) && board[nr][nc] == Piece.EMPTY) {
                moves.add(new Move(r, c, nr, nc, false));
            }
        }
        return moves;
    }

    // Простые шашки - ходы со взятием
    private List<Move> findManCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];

        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int mr = r + dr;  // клетка с вражеской шашкой
                int mc = c + dc;
                int nr = r + dr * 2; // клетка куда прыгаем
                int nc = c + dc * 2;

                if (isValid(mr, mc) && isValid(nr, nc)) {
                    Piece middle = board[mr][mc];
                    if (middle != Piece.EMPTY && middle.isWhite() != current.isWhite() &&
                            board[nr][nc] == Piece.EMPTY) {
                        moves.add(new Move(r, c, nr, nc, true));
                    }
                }
            }
        }
        return moves;
    }

    // Дамки - тихие ходы
    private List<Move> findKingQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist;
                    int nc = c + dc * dist;
                    if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                    moves.add(new Move(r, c, nr, nc, false));
                }
            }
        }
        return moves;
    }

    // Дамки - ходы со взятием
    private List<Move> findKingCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];

        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int enemyR = -1, enemyC = -1;

                // Ищем вражескую шашку по диагонали
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist;
                    int nc = c + dc * dist;

                    if (!isValid(nr, nc)) break;

                    Piece target = board[nr][nc];
                    if (target == Piece.EMPTY) continue;

                    if (target.isWhite() == current.isWhite()) {
                        break; // Своя шашка - преграда
                    } else {
                        enemyR = nr;
                        enemyC = nc;
                        break; // Нашли вражескую шашку
                    }
                }

                // Если нашли вражескую шашку, ищем пустую клетку за ней
                if (enemyR != -1) {
                    for (int dist = 1; dist < 8; dist++) {
                        int nr = enemyR + dr * dist;
                        int nc = enemyC + dc * dist;

                        if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                        moves.add(new Move(r, c, nr, nc, true));
                    }
                }
            }
        }
        return moves;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private void updatePossibleMoves() {
        possibleMoves.clear();
        selected = null;
        inCaptureSequence = false;

        boolean mustCapture = playerMustCapture();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    if (mustCapture) {
                        List<Move> captures = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                        possibleMoves.addAll(captures);
                    } else {
                        List<Move> captures = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                        List<Move> quietMoves = p.isKing() ? findKingQuietMoves(r, c) : findManQuietMoves(r, c);
                        possibleMoves.addAll(captures);
                        possibleMoves.addAll(quietMoves);
                    }
                }
            }
        }
    }

    private void checkWinner() {
        boolean hasWhite = false, hasBlack = false;
        for (Piece[] row : board) {
            for (Piece p : row) {
                if (p.isWhite()) hasWhite = true;
                if (p.isBlack()) hasBlack = true;
            }
        }

        if (!hasWhite || !hasBlack) {
            String msg = hasWhite ? "Белые победили!" : "Чёрные победили!";
            JOptionPane.showMessageDialog(null, msg, "Игра окончена", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    // ИИ
    public void makeAIMove() {
        // Сначала проверяем обязательные взятия
        List<Move> captureMoves = new ArrayList<>();
        List<Move> quietMoves = new ArrayList<>();

        for (Move move : possibleMoves) {
            if (move.isCapture) {
                captureMoves.add(move);
            } else {
                quietMoves.add(move);
            }
        }

        Move chosenMove = null;
        if (!captureMoves.isEmpty()) {
            // Выбираем случайный ход со взятием
            chosenMove = captureMoves.get(new Random().nextInt(captureMoves.size()));
        } else if (!quietMoves.isEmpty()) {
            // Выбираем случайный тихий ход
            chosenMove = quietMoves.get(new Random().nextInt(quietMoves.size()));
        }

        if (chosenMove != null) {
            // Выбираем шашку
            selected = new Move(chosenMove.fromRow, chosenMove.fromCol, -1, -1);
            // Выполняем ход
            tryMove(chosenMove.toRow, chosenMove.toCol);
        }
    }

    public String getWinner() {
        boolean hasWhite = false, hasBlack = false;
        for (Piece[] row : board) {
            for (Piece p : row) {
                if (p.isWhite()) hasWhite = true;
                if (p.isBlack()) hasBlack = true;
            }
        }
        if (!hasWhite) return "Чёрные";
        if (!hasBlack) return "Белые";
        return null;
    }
}
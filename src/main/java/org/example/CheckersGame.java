package org.example;

import java.util.*;

public class CheckersGame {
    private final Piece[][] board = new Piece[8][8];
    private boolean whiteTurn = true;
    private Move selected = null;
    private final Set<Move> possibleMoves = new HashSet<>();
    private boolean inCaptureSequence = false;

    public CheckersGame() {
        initBoard();
        updatePossibleMoves();
    }

    private void initBoard() {
        for (int r = 0; r < 8; r++) Arrays.fill(board[r], Piece.EMPTY);
        for (int r = 0; r < 3; r++)
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
        if (selected == null) {
            selectPiece(row, col);
        } else {
            if (!tryMove(row, col)) {
                selectPiece(row, col);
            }
        }
        checkWinner();
    }

    private void selectPiece(int row, int col) {
        if (inCaptureSequence && (selected == null || row != selected.fromRow || col != selected.fromCol)) return;

        Piece p = board[row][col];
        if (p == Piece.EMPTY || p.isWhite() != whiteTurn) {
            selected = null;
            possibleMoves.clear();
            return;
        }

        List<Move> captures = p.isKing() ? findKingCaptures(row, col) : findManCaptures(row, col);
        if (!captures.isEmpty()) {
            possibleMoves.clear();
            possibleMoves.addAll(captures);
            selected = new Move(row, col, -1, -1);
        } else if (!inCaptureSequence) {
            List<Move> quiet = p.isKing() ? findKingQuietMoves(row, col) : findManQuietMoves(row, col);
            if (!quiet.isEmpty()) {
                possibleMoves.clear();
                possibleMoves.addAll(quiet);
                selected = new Move(row, col, -1, -1);
            }
        }
    }

    private boolean tryMove(int toRow, int toCol) {
        if (selected == null) return false;
        Move move = new Move(selected.fromRow, selected.fromCol, toRow, toCol);
        if (!possibleMoves.contains(move)) return false;

        executeMove(move);

        Piece p = board[toRow][toCol];
        List<Move> further = p.isKing() ? findKingCaptures(toRow, toCol) : findManCaptures(toRow, toCol);

        if (!further.isEmpty()) {
            selected = new Move(toRow, toCol, -1, -1);
            possibleMoves.clear();
            possibleMoves.addAll(further);
            inCaptureSequence = true;
        } else {
            whiteTurn = !whiteTurn;
            inCaptureSequence = false;
            updatePossibleMoves();
        }
        return true;
    }

    private void executeMove(Move m) {
        Piece p = board[m.fromRow][m.fromCol];
        board[m.fromRow][m.fromCol] = Piece.EMPTY;
        board[m.toRow][m.toCol] = p;

        int dr = Integer.signum(m.toRow - m.fromRow);
        int dc = Integer.signum(m.toCol - m.fromCol);
        int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));

        for (int i = 1; i < steps; i++) {
            int r = m.fromRow + dr * i;
            int c = m.fromCol + dc * i;
            if (board[r][c] != Piece.EMPTY) {
                board[r][c] = Piece.EMPTY;
            }
        }

        if ((p == Piece.WHITE_MAN && m.toRow == 0) || (p == Piece.BLACK_MAN && m.toRow == 7)) {
            board[m.toRow][m.toCol] = p == Piece.WHITE_MAN ? Piece.WHITE_KING : Piece.BLACK_KING;
        }
    }

    // === ПРОСТЫЕ ШАШКИ ===
    private List<Move> findManQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int dir = board[r][c] == Piece.WHITE_MAN ? -1 : 1;
        for (int dc = -1; dc <= 1; dc += 2) {
            int nr = r + dir;
            int nc = c + dc;
            if (isValid(nr, nc) && board[nr][nc] == Piece.EMPTY) {
                moves.add(new Move(r, c, nr, nc));
            }
        }
        return moves;
    }

    private List<Move> findManCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int nr = r + dr * 2;
                int nc = c + dc * 2;
                if (isValid(nr, nc) && board[nr][nc] == Piece.EMPTY) {
                    int mr = r + dr;
                    int mc = c + dc;
                    Piece mid = board[mr][mc];
                    if (mid != Piece.EMPTY && mid.isWhite() != whiteTurn) {
                        moves.add(new Move(r, c, nr, nc));
                    }
                }
            }
        }
        return moves;
    }

    // === ДАМКИ ===
    private List<Move> findKingQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist;
                    int nc = c + dc * dist;
                    if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                    moves.add(new Move(r, c, nr, nc));
                }
            }
        }
        return moves;
    }

    private List<Move> findKingCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int enemyR = -1, enemyC = -1;
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist;
                    int nc = c + dc * dist;
                    if (!isValid(nr, nc)) break;
                    if (board[nr][nc] != Piece.EMPTY) {
                        if (board[nr][nc].isWhite() == whiteTurn) break;
                        enemyR = nr; enemyC = nc;
                        break;
                    }
                }
                if (enemyR == -1) continue;

                for (int dist = 1; dist < 8; dist++) {
                    int nr = enemyR + dr * dist;
                    int nc = enemyC + dc * dist;
                    if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                    moves.add(new Move(r, c, nr, nc));
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

        boolean hasCapture = false;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    List<Move> caps = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                    if (!caps.isEmpty()) hasCapture = true;
                }
            }
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    List<Move> moves = hasCapture
                            ? (p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c))
                            : (p.isKing() ? findKingQuietMoves(r, c) : findManQuietMoves(r, c));
                    possibleMoves.addAll(moves);
                }
            }
        }
    }

    private void checkWinner() {
        boolean hasWhite = false, hasBlack = false;
        for (Piece[] row : board)
            for (Piece p : row)
                if (p.isWhite()) hasWhite = true;
                else if (p.isBlack()) hasBlack = true;

        if (!hasWhite || !hasBlack) {
            String msg = hasWhite ? "Белые победили!" : "Чёрные победили!";
            javax.swing.JOptionPane.showMessageDialog(null, msg, "Игра окончена", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
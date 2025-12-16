package org.example;

import java.util.*;

public class CheckersGame {
    private final Piece[][] board = new Piece[8][8];
    private boolean whiteTurn = true;
    private Move selected = null;
    private final Set<Move> possibleMoves = new HashSet<>();
    private boolean inCaptureSequence = false;

    public CheckersGame() {
        resetGame();
    }

    public void resetGame() {
        whiteTurn = true;
        selected = null;
        inCaptureSequence = false;
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
    }

    private void selectPiece(int row, int col) {
        if (inCaptureSequence && (selected == null || row != selected.fromRow || col != selected.fromCol)) return;

        Piece p = board[row][col];
        if (p == Piece.EMPTY || p.isWhite() != whiteTurn) {
            selected = null;
            possibleMoves.clear();
            return;
        }

        boolean mustCapture = playerMustCapture();
        List<Move> captures = p.isKing() ? findKingCaptures(row, col) : findManCaptures(row, col);

        possibleMoves.clear();
        if (mustCapture) {
            if (!captures.isEmpty()) {
                possibleMoves.addAll(captures);
                selected = new Move(row, col, -1, -1);
            } else {
                selected = null;
            }
        } else {
            possibleMoves.addAll(captures);
            possibleMoves.addAll(p.isKing() ? findKingQuietMoves(row, col) : findManQuietMoves(row, col));
            if (!possibleMoves.isEmpty()) selected = new Move(row, col, -1, -1);
        }
    }

    private boolean tryMove(int toRow, int toCol) {
        if (selected == null) return false;
        Move chosenMove = null;
        for (Move move : possibleMoves) {
            if (move.fromRow == selected.fromRow && move.fromCol == selected.fromCol &&
                    move.toRow == toRow && move.toCol == toCol) {
                chosenMove = move;
                break;
            }
        }
        if (chosenMove == null) return false;

        boolean wasCapture = chosenMove.isCapture;
        executeMove(chosenMove);

        if (wasCapture) {
            Piece movedPiece = board[toRow][toCol];
            List<Move> furtherCaptures = movedPiece.isKing() ? findKingCaptures(toRow, toCol) : findManCaptures(toRow, toCol);
            if (!furtherCaptures.isEmpty()) {
                selected = new Move(toRow, toCol, -1, -1);
                possibleMoves.clear();
                possibleMoves.addAll(furtherCaptures);
                inCaptureSequence = true;
                return true;
            }
        }

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

        if (m.isCapture) {
            int dr = Integer.signum(m.toRow - m.fromRow);
            int dc = Integer.signum(m.toCol - m.fromCol);
            int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));
            for (int i = 1; i < steps; i++) {
                board[m.fromRow + dr * i][m.fromCol + dc * i] = Piece.EMPTY;
            }
        }

        if ((p == Piece.WHITE_MAN && m.toRow == 0) || (p == Piece.BLACK_MAN && m.toRow == 7)) {
            board[m.toRow][m.toCol] = (p == Piece.WHITE_MAN) ? Piece.WHITE_KING : Piece.BLACK_KING;
        }
    }

    private boolean playerMustCapture() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    if (!(p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c)).isEmpty()) return true;
                }
            }
        }
        return false;
    }

    // --- Вспомогательные методы поиска ходов (как были) ---
    private List<Move> findManQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int dir = (board[r][c] == Piece.WHITE_MAN) ? -1 : 1;
        for (int dc = -1; dc <= 1; dc += 2) {
            if (isValid(r + dir, c + dc) && board[r + dir][c + dc] == Piece.EMPTY)
                moves.add(new Move(r, c, r + dir, c + dc, false));
        }
        return moves;
    }

    private List<Move> findManCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                if (isValid(r + dr * 2, c + dc * 2)) {
                    Piece mid = board[r + dr][c + dc];
                    if (mid != Piece.EMPTY && mid.isWhite() != current.isWhite() &&
                            board[r + dr * 2][c + dc * 2] == Piece.EMPTY) {
                        moves.add(new Move(r, c, r + dr * 2, c + dc * 2, true));
                    }
                }
            }
        }
        return moves;
    }

    private List<Move> findKingQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist, nc = c + dc * dist;
                    if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                    moves.add(new Move(r, c, nr, nc, false));
                }
            }
        }
        return moves;
    }

    private List<Move> findKingCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int eR = -1, eC = -1;
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist, nc = c + dc * dist;
                    if (!isValid(nr, nc)) break;
                    Piece t = board[nr][nc];
                    if (t == Piece.EMPTY) continue;
                    if (t.isWhite() == current.isWhite()) break;
                    eR = nr; eC = nc; break;
                }
                if (eR != -1) {
                    for (int dist = 1; dist < 8; dist++) {
                        int nr = eR + dr * dist, nc = eC + dc * dist;
                        if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                        moves.add(new Move(r, c, nr, nc, true));
                    }
                }
            }
        }
        return moves;
    }

    private boolean isValid(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    private void updatePossibleMoves() {
        possibleMoves.clear();
        selected = null;
        inCaptureSequence = false;
        boolean mustCapture = playerMustCapture();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    List<Move> cMoves = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                    List<Move> qMoves = p.isKing() ? findKingQuietMoves(r, c) : findManQuietMoves(r, c);
                    if (mustCapture) possibleMoves.addAll(cMoves);
                    else { possibleMoves.addAll(cMoves); possibleMoves.addAll(qMoves); }
                }
            }
        }
    }

    // ИИ: Простой случайный ход
    public void makeAIMove() {
        if (possibleMoves.isEmpty()) return;
        List<Move> captures = new ArrayList<>();
        List<Move> quiets = new ArrayList<>();
        for (Move m : possibleMoves) {
            if (m.isCapture) captures.add(m); else quiets.add(m);
        }
        Move chosen = !captures.isEmpty()
                ? captures.get(new Random().nextInt(captures.size()))
                : quiets.get(new Random().nextInt(quiets.size()));

        selected = new Move(chosen.fromRow, chosen.fromCol, -1, -1);
        tryMove(chosen.toRow, chosen.toCol);
    }

    /** Возвращает имя победителя или null, если игра продолжается. */
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
        // Также можно добавить условие: если у текущего игрока нет ходов - он проиграл
        if (possibleMoves.isEmpty()) {
            return whiteTurn ? "Чёрные" : "Белые";
        }
        return null;
    }
}
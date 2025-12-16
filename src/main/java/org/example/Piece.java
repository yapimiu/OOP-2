package org.example;

public enum Piece {
    EMPTY,
    WHITE_MAN,
    BLACK_MAN,
    WHITE_KING,
    BLACK_KING;

    public boolean isWhite() {
        return this == WHITE_MAN || this == WHITE_KING;
    }

    public boolean isBlack() {
        return this == BLACK_MAN || this == BLACK_KING;
    }

    public boolean isKing() {
        return this == WHITE_KING || this == BLACK_KING;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
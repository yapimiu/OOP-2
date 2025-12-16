package org.example;

import java.util.Objects;

public class Move {
    public int fromRow, fromCol, toRow, toCol;
    public boolean isCapture;

    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean isCapture) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.isCapture = isCapture;
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, false);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Move move = (Move) obj;
        return fromRow == move.fromRow && fromCol == move.fromCol &&
                toRow == move.toRow && toCol == move.toCol &&
                isCapture == move.isCapture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRow, fromCol, toRow, toCol, isCapture);
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)->(%d,%d) %s", fromRow, fromCol, toRow, toCol,
                isCapture ? "[захват]" : "");
    }
}
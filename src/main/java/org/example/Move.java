package org.example;

public class Move {
    public final int fromRow, fromCol, toRow, toCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move m)) return false;
        return fromRow == m.fromRow && fromCol == m.fromCol && toRow == m.toRow && toCol == m.toCol;
    }

    @Override
    public int hashCode() {
        int result = fromRow;
        result = 31 * result + fromCol;
        result = 31 * result + toRow;
        result = 31 * result + toCol;
        return result;
    }
}
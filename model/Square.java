package hazzard_chess.model;

import hazzard_chess.model.piece.Piece;

public class Square {
    private final int row;
    private final int col;

    private boolean hasMine;
    private boolean isRevealed;
    private int adjacentMines;
    private boolean isFlagged;

    private Piece piece;
    
    public Square(int row, int col){
        this.row = row;
        this.col = col;
        this.hasMine = false;
        this.isRevealed = false;
        this.adjacentMines = 0;
        this.piece = null;
    }

    public int getRow() {return row;}
    public int getCol() {return col;}

    public boolean hasMine() {return hasMine;}
    public void setMine(boolean hasMine) {this.hasMine = hasMine;}

    public boolean isRevealed() {return isRevealed;}
    public void reveal() {this.isRevealed = true;}

    public int getAdjacentMines() {return adjacentMines;}
    public void setAdjacentMines(int count) {this.adjacentMines = count;}

    public boolean isFlagged() {return isFlagged;}
    public void setFlagged(boolean flagged) {this.isFlagged = flagged;}

    public Piece getPiece() {return piece;}
    public void setPiece(Piece piece) {this.piece = piece;}
    public boolean isEmpty() {return piece == null;}
}

package hazzard_chess.model.piece;

public abstract class Piece {
    private String color;
    private int row;
    private int col;

    public Piece(String color, int row, int col){
        this.color = color;
        this.row = row;
        this.col = col;
    }

    public String getColor() {return color;}
        public int getRow() {return row;}
        public int getCol() {return col;}

        public abstract char getSymbol();
}

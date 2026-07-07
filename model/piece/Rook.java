package hazzard_chess.model.piece;

public class Rook extends Piece {
    public Rook(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'R' : 'r';
    }
}

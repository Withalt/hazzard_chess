package hazzard_chess.model.piece;

public class Pawn extends Piece {
    public Pawn(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'P' : 'p';
    }
}

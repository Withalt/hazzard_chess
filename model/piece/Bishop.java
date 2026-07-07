package hazzard_chess.model.piece;

public class Bishop extends Piece {
    public Bishop(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'B' : 'b';
    }
}

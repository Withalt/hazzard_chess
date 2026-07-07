package hazzard_chess.model.piece;

public class Queen extends Piece {
    public Queen(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'Q' : 'q';
    }
}

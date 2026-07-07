package hazzard_chess.model.piece;

public class King extends Piece{
    public King(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'K' : 'k';
    }
}

package hazzard_chess.model.piece;

public class Knight extends Piece {
    public Knight(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'N' : 'n';
    }
}

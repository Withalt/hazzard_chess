package hazzard_chess.model.piece;

import java.util.ArrayList;
import java.util.List;

import hazzard_chess.model.Board;
import hazzard_chess.model.Square;

public class King extends Piece{
    public King(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'K' : 'k';
    }

    @Override
    public List<int[]> getValidMoves(Board board){
        List<int[]> moves = new ArrayList<>();
        int[][] steps = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        for(int[] step : steps){
            int r = getRow() + step[0];
            int c = getCol() + step[1];

            if (r>=0 && r<Board.SIZE && c>=0 && c<Board.SIZE){
                Square target = board.getSquare(r, c);
                if (target.isEmpty() || !target.getPiece().getColor().equals(getColor())) {
                    moves.add(new int[]{r, c});
                }
            }
        }
        return moves;
    }
}

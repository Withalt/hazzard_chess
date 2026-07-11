package hazzard_chess.model.piece;
import java.util.ArrayList;
import java.util.List;

import hazzard_chess.model.Board;
import hazzard_chess.model.Square;

public class Knight extends Piece {
    public Knight(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'N' : 'n';
    }

    @Override
    public List<int[]> getValidMoves(Board board){
        List<int[]> moves = new ArrayList<>();
        int[][] jumps = {{-2,-1}, {-2,1}, {-1,-2}, {-1,2}, {1,-2}, {1,2}, {2,-1}, {2,1}};
        
        for(int[] jump : jumps){
            int r = getRow() + jump[0];
            int c = getCol() + jump[1];

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

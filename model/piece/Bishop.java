package hazzard_chess.model.piece;
import java.util.ArrayList;
import java.util.List;

import hazzard_chess.model.Board;
import hazzard_chess.model.Square;

public class Bishop extends Piece {
    public Bishop(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'B' : 'b';
    }
    
    @Override
    public List<int[]> getValidMoves(Board board){
        List<int[]> moves = new ArrayList<>();
        int[][] directions = {{-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        for(int[] dir : directions){
            int r = getRow() + dir[0];
            int c = getCol() + dir[1];

            while (r>=0 && r<Board.SIZE && c>=0 && c<Board.SIZE){
                Square target = board.getSquare(r, c);

                if (target.isEmpty()) {
                    moves.add(new int[]{r, c});
                }else{
                    if(!target.getPiece().getColor().equals(getColor())){
                        moves.add(new int[]{r, c});
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return moves;
    }
}

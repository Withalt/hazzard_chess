package hazzard_chess.model.piece;
import java.util.ArrayList;
import java.util.List;

import hazzard_chess.model.Board;
import hazzard_chess.model.Square;

public class Pawn extends Piece {
    public Pawn(String color, int row, int col){
        super(color, row, col);
    }

    @Override
    public char getSymbol(){
        return getColor().equals("white") ? 'P' : 'p';
    }

    @Override
    public List<int[]> getValidMoves(Board board){
        List<int[]> moves = new ArrayList<>();
        boolean isWhite = getColor().equals("white");
        int direction = isWhite ? -1 : 1;
        int startingRow = isWhite ? 6 : 1;

        int oneStepRow = getRow() + direction;
        if (oneStepRow>=0 && oneStepRow<Board.SIZE){
            Square oneStep = board.getSquare(oneStepRow, getCol());
            if(oneStep.isEmpty()){
                moves.add(new int[]{oneStepRow, getCol()});

                int twoStepRow = getRow() + direction * 2;
                if(getRow() == startingRow){
                    Square twoStep = board.getSquare(twoStepRow, getCol());
                    if(twoStep.isEmpty()){
                        moves.add(new int[]{twoStepRow, getCol()});
                    }
                }
            }
        }

        int[] diagonalCols = {getCol() - 1, getCol() + 1};
        for(int diagCol : diagonalCols){
            if(diagCol>=0 && diagCol<Board.SIZE && oneStepRow>=0 && oneStepRow<Board.SIZE){
                Square diagSquare = board.getSquare(oneStepRow, diagCol);
                if(!diagSquare.isEmpty() && !diagSquare.getPiece().getColor().equals(getColor())){
                    moves.add(new int[]{oneStepRow, diagCol});
                }
            }
        }
        return moves;
    }
}

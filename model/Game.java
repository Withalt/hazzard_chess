package hazzard_chess.model;
import java.util.List;

import hazzard_chess.model.piece.Piece;

public class Game {
    private Board board;
    private String currentTurn;

    public Game(Board board){
        this.board = board;
        this.currentTurn = "white";
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol){
        Square fromSquare = board.getSquare(fromRow, fromCol);
        Piece piece = fromSquare.getPiece();

        if(piece == null){
            System.out.println("Square does not contain a piece.");
            return false;
        }

        if(!piece.getColor().equals(currentTurn)){
            System.out.println("It is not this piece's turn.");
            return false;
        }

        List<int[]> validMoves = piece.getValidMoves(board);
        
        //Debug Line
        // System.out.println("Current piece: " + piece.getSymbol() + "at (" + fromRow + "," + fromCol + ")");
        // System.out.print("Valid moves: ");
        // for (int[] m : validMoves){
        //     System.out.print("(" + m[0] + "," + m[1] + ") ");
        // }
        // System.out.println();
        
        boolean isValidMove = false;
        for (int[] move : validMoves){
            if (move[0] == toRow && move[1] == toCol){
                isValidMove = true;
                break;
            }
        }
        if(!isValidMove){
            System.out.println("Invalid move.");
            return false;
        }

        Square toSquare = board.getSquare(toRow, toCol);
        toSquare.setPiece(piece);
        fromSquare.setPiece(null);
        board.reveal(toRow, toCol);

        currentTurn = currentTurn.equals("white") ? "black" : "white";
        return true;
    }

    public String getCurrentTurn(){
        return currentTurn;
    }
}

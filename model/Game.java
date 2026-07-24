package hazzard_chess.model;
import java.util.List;
import java.util.Scanner;

import hazzard_chess.model.piece.Bishop;
import hazzard_chess.model.piece.King;
import hazzard_chess.model.piece.Knight;
import hazzard_chess.model.piece.Pawn;
import hazzard_chess.model.piece.Piece;
import hazzard_chess.model.piece.Queen;
import hazzard_chess.model.piece.Rook;

public class Game {
    private Board board;
    private String currentTurn;
    private int enPassantRow = -1;
    private int enPassantCol = -1;
    private boolean gameOver = false;
    private String winner = null;

    public Game(Board board){
        this.board = board;
        this.currentTurn = "white";
    }

    public boolean isGameOver(){ return gameOver; }
    public String getWinner(){ return winner; }


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
        
        //Debug Line 1
        // System.out.println("Current piece: " + piece.getSymbol() + "at (" + fromRow + "," + fromCol + ")");
        // System.out.print("Valid moves: ");
        // for (int[] m : validMoves){
        //     System.out.print("(" + m[0] + "," + m[1] + ") ");
        // }
        // System.out.println();

        //Debug Line 2
        // System.out.println("Piece's stored position: (" + piece.getRow() + "," + piece.getCol() + ")");
        // System.out.println("Requested from position: (" + fromRow + "," + fromCol + ")");
        
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
        
        if(toSquare.isFlagged()){
            System.out.println("Cannot move to an enemy square that contains a flag.");
            return false;
        }
        
        boolean wasRevealed = board.getSquare(toRow, toCol).isRevealed();

        toSquare.setPiece(piece);
        fromSquare.setPiece(null);
        piece.setPosition(toRow, toCol);
        board.reveal(toRow, toCol);
        piece.setHasMoved(true);
        checkPromotion(piece, toRow, toCol);

        if(!wasRevealed && toSquare.hasMine()){
            System.out.println("Mine triggered and the piece has been removed.");
            boolean wasKing = piece instanceof King;
            toSquare.setPiece(null);

            if(wasKing){
                gameOver = true;
                winner = currentTurn.equals("white") ? "black" : "white";
                System.out.println("The King stepped on a mine! " + winner + " wins!");
            }
        }else{
            board.tryChording(toRow, toCol);
        }

        // int oldEnPassantRow = enPassantRow;
        // int oldEnPassantCol = enPassantCol;
        enPassantRow = -1;
        enPassantCol = -1;

        if(piece instanceof Pawn && Math.abs(toRow - fromRow) == 2){
            enPassantRow = (fromRow + toRow) / 2;
            enPassantCol = toCol;
        }

        currentTurn = currentTurn.equals("white") ? "black" : "white";

        if(board.isCheckmate(currentTurn)){
            gameOver = true;
            winner = currentTurn.equals("white") ? "black" : "white";
            System.out.println("Checkmate! " + winner + " wins!");
        }
        return true;
    }

    public String getCurrentTurn(){
        return currentTurn;
    }

    public boolean castle(String side){
        boolean isKingside = side.equals("kingside");
        boolean canCastle = isKingside
            ? board.canCastleKingside(currentTurn)
            : board.canCastleQueenside(currentTurn);
        if(!canCastle){
            System.out.println("Can't castle.");
            return false;
        }

        King king = board.findKing(currentTurn);
        int row = king.getRow();
        int rookCol = isKingside ? 7 : 0;
        int kingDestCol = isKingside ? 6 : 2;
        int rookDestCol = isKingside ? 5 : 3;

        Square kingSquare = board.getSquare(row, 4);
        Square rookSquare = board.getSquare(row, rookCol);
        Piece rookPiece = rookSquare.getPiece();

        board.getSquare(row, kingDestCol).setPiece(king);
        kingSquare.setPiece(null);
        king.setPosition(row, kingDestCol);
        board.reveal(row, kingDestCol);
        king.setHasMoved(true);

        board.getSquare(row, rookDestCol).setPiece(rookPiece);
        rookSquare.setPiece(null);
        rookPiece.setPosition(row, rookDestCol);
        board.reveal(row, rookDestCol);
        rookPiece.setHasMoved(true);

        currentTurn = currentTurn.equals("white") ? "black" : "white";

        if(board.isCheckmate(currentTurn)){
            gameOver = true;
            winner = currentTurn.equals("white") ? "black" : "white";
            System.out.println("Checkmate! " + winner + " wins!");
        }
        return true;
    }

    public boolean enPassantCapture(int fromRow, int fromCol, int toRow, int toCol){
        Square fromSquare = board.getSquare(fromRow, fromCol);
        Piece piece = fromSquare.getPiece();

        if(piece == null || !(piece instanceof Pawn)){
            System.out.println("Piece is not a pawn.");
            return false;
        }

        if(!piece.getColor().equals(currentTurn)){
            System.out.println("It is not this piece's turn.");
            return false;
        }

        if(toRow != enPassantRow || toCol != enPassantCol){
            System.out.println("En Passant is not available at this position.");
            return false;
        }

        int capturedPawnRow = fromRow;
        int capturedPawnCol = toCol;
        Square capturedSquare = board.getSquare(capturedPawnRow, capturedPawnCol);

        Square toSquare = board.getSquare(toRow, toCol);
        toSquare.setPiece(piece);
        fromSquare.setPiece(null);
        piece.setPosition(toRow, toCol);
        piece.setHasMoved(true);

        capturedSquare.setPiece(null);

        enPassantRow = -1;
        enPassantCol = -1;

        currentTurn = currentTurn.equals("white") ? "black" : "white";

        if(board.isCheckmate(currentTurn)){
            gameOver = true;
            winner = currentTurn.equals("white") ? "black" : "white";
            System.out.println("Checkmate! " + winner + " wins!");
        }
        return true;
    }

    private void checkPromotion(Piece piece, int row, int col){
        if(!(piece instanceof Pawn)){
            return;
        }

        boolean isWhitePromotion = piece.getColor().equals("white") && row == 0;
        boolean isBlackPromotion = piece.getColor().equals("black") && row == 7;

        if(isWhitePromotion || isBlackPromotion){
            System.out.print("Pawn promotion! Choose piece (Q/R/B/N): ");
            Scanner scanner = new Scanner(System.in);
            String choice = scanner.next();

            Piece newPiece;
            switch (choice.toUpperCase()) {
                case "R": newPiece = new Rook(piece.getColor(), row, col); break;
                case "B": newPiece = new Bishop(piece.getColor(), row, col); break;
                case "N": newPiece = new Knight(piece.getColor(), row, col); break;
                default: newPiece = new Queen(piece.getColor(), row, col); break;
            }
            board.getSquare(row, col).setPiece(newPiece);
        }
    }
}

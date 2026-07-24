package hazzard_chess;

import java.util.Scanner;

import hazzard_chess.model.Board;
import hazzard_chess.model.Game;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();

        // Debug White King
        // boolean isInCheck = board.isSquareUnderAttack(7, 4, "black");
        // System.out.println("White king in check: " + isInCheck);
        // King whiteKing = board.findKing("white");
        // boolean isInCheck = board.isSquareUnderAttack(whiteKing.getRow(), whiteKing.getCol(), "black");
        // boolean isInCheck = board.isKingInCheck("white");
        // System.out.println("White king in check: " + isInCheck);
        // System.out.println("Can castle kingside: " + board.canCastleKingside("white"));
        // System.out.println("Can castle queenside: " + board.canCastleQueenside("white"));
        System.out.println("White is checkmated: " + board.isCheckmate("white"));

        Game game = new Game(board);
        board.printSideBySide();

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Turn: " + game.getCurrentTurn());
            System.out.print("Enter move (move: fromRow fromCol toRow toCol) | flag: f row col | castle: castle kingside/queenside): ");
            String command = scanner.next();
            if(command.equals("f")){
                int r = scanner.nextInt();
                int c = scanner.nextInt();
                board.toggleFlag(r, c);
            }else if(command.equals("castle")){
                String side = scanner.next();
                game.castle(side);
            }else if(command.equals("ep")){
                int fr = scanner.nextInt();
                int fc = scanner.nextInt();
                int tr = scanner.nextInt();
                int tc = scanner.nextInt();
                game.enPassantCapture(fr, fc, tr, tc);
            }else{
                int fr = Integer.parseInt(command);
                int fc = scanner.nextInt();
                int tr = scanner.nextInt();
                int tc = scanner.nextInt();
                game.movePiece(fr, fc, tr, tc);
            }
            board.printSideBySide();
            
            if(game.isGameOver()){
                System.out.println("Game Over! " + game.getWinner() + " wins!");
                break;
            }
        }
    }
}

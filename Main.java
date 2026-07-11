package hazzard_chess;

import java.util.Scanner;

import hazzard_chess.model.Board;
import hazzard_chess.model.Game;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        Game game = new Game(board);
        board.printBoard();

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Turn: " + game.getCurrentTurn());
            System.out.print("Enter move (fromRow fromCol toRow toCol): ");
            int fr = scanner.nextInt(), fc = scanner.nextInt(), tr = scanner.nextInt(), tc = scanner.nextInt();
            game.movePiece(fr, fc, tr, tc);
            board.printBoard();
        }
    }
}

package hazzard_chess;

import java.util.Scanner;

import hazzard_chess.model.Board;
import hazzard_chess.model.Game;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        Game game = new Game(board);
        board.printSideBySide();

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Turn: " + game.getCurrentTurn());
            System.out.print("Enter move (move: fromRow fromCol toRow toCol) | flag: f row col): ");
            String command = scanner.next();
            if(command.equals("f")){
                int r = scanner.nextInt();
                int c = scanner.nextInt();
                board.toggleFlag(r, c);
            }else{
                int fr = Integer.parseInt(command);
                int fc = scanner.nextInt();
                int tr = scanner.nextInt();
                int tc = scanner.nextInt();
                game.movePiece(fr, fc, tr, tc);
            }
            board.printSideBySide();
        }
    }
}

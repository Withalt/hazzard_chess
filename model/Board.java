package hazzard_chess.model;

import java.util.Random;

import hazzard_chess.model.piece.Bishop;
import hazzard_chess.model.piece.King;
import hazzard_chess.model.piece.Knight;
import hazzard_chess.model.piece.Pawn;
import hazzard_chess.model.piece.Queen;
import hazzard_chess.model.piece.Rook;

public class Board {
    public static final int SIZE = 8;
    private Square[][] squares;

    public Board() {
        squares = new Square[SIZE][SIZE];
        initializeSquares();
        placePieces();
        placeMines();
        caculateAdjacentMines();
    }

    private void initializeSquares(){
        for(int row = 0; row < SIZE; row++){
            for (int col = 0; col < SIZE; col++){
                squares[row][col] = new Square(row, col);
            }
        }
    }

    public Square getSquare(int row, int col){
        return squares[row][col];
    }

    private void placePieces(){
        for(int col = 0; col < SIZE; col++){
            squares[1][col].setPiece(new Pawn("black", 1, col));
            squares[6][col].setPiece(new Pawn("white", 6, col));
        }

        placeBackRow(0, "black");
        placeBackRow(7, "white");
    }

    private void placeBackRow(int row, String color){
        squares[row][0].setPiece(new Rook(color, row, 0));
        squares[row][1].setPiece(new Knight(color, row, 1));
        squares[row][2].setPiece(new Bishop(color, row, 3));
        squares[row][3].setPiece(new Queen(color, row, 3));
        squares[row][4].setPiece(new King(color, row, 4));
        squares[row][5].setPiece(new Bishop(color, row, 5));
        squares[row][6].setPiece(new Knight(color, row, 6));
        squares[row][7].setPiece(new Rook(color, row, 7));
    }

    // public void printBoard(){
    //     for (int row = 0; row < SIZE; row++){
    //         for (int col = 0; col < SIZE; col++){
    //             Square sq = squares[row][col];
    //             if (sq.isEmpty()){
    //                 System.out.print(". ");
    //             }else{
    //                 System.out.print(sq.getPiece().getSymbol() + " ");
    //             }
    //         }
    //         System.out.println();
    //     }
    // }

    public static final int MINE_COUNT = 10;

    private void placeMines(){
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < MINE_COUNT){
            int row = rand.nextInt(SIZE);
            int col = rand.nextInt(SIZE);

            Square square = squares[row][col];

            if(!square.hasMine()){
                square.setMine(true);
                minesPlaced++;
            }
        }
    }

    // public void printMineDebugView(){
    //     for(int row=0; row<SIZE; row++){
    //         for(int col = 0; col<SIZE; col++){
    //             Square sq = squares[row][col];
    //             System.out.print(sq.hasMine() ? "* " : ". ");
    //         }
    //         System.out.println();
    //     }
    // }

    //Debug View
    public void printSideBySide(){
        System.out.println("Chess View            Minesweeper View");
        for (int row=0; row<SIZE; row++){
            StringBuilder line = new StringBuilder();

            //Chess view
            for(int col=0; col<SIZE; col++){
                Square sq = squares[row][col];
                line.append(sq.isEmpty() ? ". " : sq.getPiece().getSymbol() + " ");
            }

            line.append("       ");

            //Minesweeper view
            for(int col=0; col<SIZE; col++){
                Square sq = squares[row][col];
                if(!sq.isRevealed()){
                    line.append(sq.isFlagged() ? "F " : "? ");
                }else if(sq.hasMine()){
                    line.append("* ");
                }else{
                    line.append(sq.getAdjacentMines() + " ");
                }
            }
            System.out.println(line.toString());
        }
    }

    private void caculateAdjacentMines(){
        int[][] directions={
            {-1,-1}, {-1,0}, {-1,1},
            {0,-1},         {0,1},
            {1,-1}, {1,0}, {1,1}
        };

        for(int row=0; row<SIZE; row++){
            for(int col=0; col<SIZE; col++){
                int count = 0;
                for(int[] dir : directions){
                    int r = row + dir[0];
                    int c = col + dir[1];
                    
                    if(r>=0 && r<SIZE && c>=0 && c<SIZE){
                        if(squares[r][c].hasMine()){
                            count++;
                        }
                    }
                }
                squares[row][col].setAdjacentMines(count);
            }
        }
    }

    public void reveal(int row, int col){
        Square square = squares[row][col];
        if(square.isRevealed()){
            return;
        }
        square.reveal();
        if(square.getAdjacentMines() == 0){
            int[][] directions={
            {-1,-1}, {-1,0}, {-1,1},
            {0,-1},         {0,1},
            {1,-1}, {1,0}, {1,1}
            };

            for(int[] dir : directions){
                int r = row + dir[0];
                int c = col + dir[1];

                if(r>=0 && r<SIZE && c>=0 && c<SIZE){
                    reveal(r, c);
                }
            }
        }
    }

    public boolean toggleFlag(int row, int col){
        Square square = squares[row][col];
        if(square.isRevealed()){
            System.out.println("Cannot flag a revealed square");
            return false;
        }

        square.setFlagged(!square.isFlagged());
        return true;
    }
}

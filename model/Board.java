package hazzard_chess.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hazzard_chess.model.piece.Bishop;
import hazzard_chess.model.piece.King;
import hazzard_chess.model.piece.Knight;
import hazzard_chess.model.piece.Pawn;
import hazzard_chess.model.piece.Queen;
import hazzard_chess.model.piece.Rook;
import hazzard_chess.model.piece.Piece;

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
        squares[row][2].setPiece(new Bishop(color, row, 2));
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
        System.out.println("  Chess View            Minesweeper View");
        
        //Col Number
        System.out.print("  ");
        for(int col=0; col<SIZE; col++){
            System.out.print(col + " ");
        }
        System.out.println();
        
        for (int row=0; row<SIZE; row++){
            StringBuilder line = new StringBuilder();
            //Row Number
            line.append(row + " ");

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

    public List<Piece> getAllPieces(String color){
        List<Piece> pieces = new ArrayList<>();

        for(int row=0; row<SIZE; row++){
            for(int col=0; col<SIZE; col++){
                Square square = squares[row][col];
                if(!square.isEmpty() && square.getPiece().getColor().equals(color)){
                    pieces.add(square.getPiece());
                }
            }
        }
        return pieces;
    }

    public boolean isSquareUnderAttack(int row, int col, String byColor){
        List<Piece> attackers = getAllPieces(byColor);

        for(Piece piece : attackers){
            List<int[]> moves = piece.getValidMoves(this);
            for(int[] move : moves){
                if(move[0] == row && move[1] == col){
                    return true;
                }
            }
        }
        return false;
    }

    public King findKing(String color){
        List<Piece> pieces = getAllPieces(color);

        for(Piece piece : pieces){
            if(piece instanceof King){
                return(King) piece;
            }
        }
        return null;
    }

    public boolean isKingInCheck(String color){
        King king = findKing(color);
        String opponentColor = color.equals("white") ? "black" : "white";
        return isSquareUnderAttack(king.getRow(), king.getCol(), opponentColor);
    }

    public boolean canCastleKingside(String color){
        King king = findKing(color);
        int row = king.getRow();
        String opponentColor = color.equals("white") ? "black" : "white";
        Square rookSquare = squares[row][7];
        if(rookSquare.isEmpty() || !(rookSquare.getPiece() instanceof Rook)){
            return false;
        }
        Piece rook = rookSquare.getPiece();

        boolean neitherMoved = !king.getHasMoved() && !rook.getHasMoved();
        boolean pathClear = squares[row][5].isEmpty() && squares[row][6].isEmpty();
        boolean pathSafe = !isSquareUnderAttack(row, 4, opponentColor)
                        && !isSquareUnderAttack(row, 5, opponentColor)
                        && !isSquareUnderAttack(row, 6, opponentColor);

        return neitherMoved && pathClear && pathSafe;
    }

    public boolean canCastleQueenside(String color){
        King king = findKing(color);
        int row = king.getRow();
        String opponentColor = color.equals("white") ? "black" : "white";
        Square rookSquare = squares[row][0];
        if(rookSquare.isEmpty() || !(rookSquare.getPiece() instanceof Rook)){
            return false;
        }
        Piece rook = rookSquare.getPiece();

        boolean neitherMoved = !king.getHasMoved() && !rook.getHasMoved();
        boolean pathClear = squares[row][1].isEmpty() && squares[row][2].isEmpty() && squares[row][3].isEmpty();
        boolean pathSafe = !isSquareUnderAttack(row, 4, opponentColor)
                        && !isSquareUnderAttack(row, 3, opponentColor)
                        && !isSquareUnderAttack(row, 2, opponentColor);

        return neitherMoved && pathClear && pathSafe;
    }
}

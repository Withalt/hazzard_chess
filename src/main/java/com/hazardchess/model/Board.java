package com.hazardchess.model;

public final class Board {
  public static final int SIZE = 8;
  private final Piece[][] pieces = new Piece[SIZE][SIZE];

  public Piece getPiece(int row, int col) {
    return pieces[row][col];
  }

  public void setPiece(int row, int col, Piece piece) {
    pieces[row][col] = piece;
  }

  public Piece removePiece(int row, int col) {
    Piece piece = pieces[row][col];
    pieces[row][col] = null;
    return piece;
  }

  public boolean isEmpty(int row, int col) {
    return pieces[row][col] == null;
  }

  public boolean isInside(int row, int col) {
    return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
  }

  public Square findKing(PlayerColor color) {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        Piece piece = pieces[row][col];
        if (piece != null && piece.getColor() == color && piece.getType() == PieceType.KING) {
          return new Square(row, col);
        }
      }
    }
    return null;
  }

  public Board copy() {
    Board clone = new Board();
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        Piece piece = pieces[row][col];
        if (piece != null) {
          clone.setPiece(row, col, new Piece(piece.getColor(), piece.getType()));
        }
      }
    }
    return clone;
  }

  public void setupStandard() {
    clear();
    placeBackRank(0, PlayerColor.BLACK);
    placePawns(1, PlayerColor.BLACK);
    placePawns(6, PlayerColor.WHITE);
    placeBackRank(7, PlayerColor.WHITE);
  }

  private void placeBackRank(int row, PlayerColor color) {
    setPiece(row, 0, new Piece(color, PieceType.ROOK));
    setPiece(row, 1, new Piece(color, PieceType.KNIGHT));
    setPiece(row, 2, new Piece(color, PieceType.BISHOP));
    setPiece(row, 3, new Piece(color, PieceType.QUEEN));
    setPiece(row, 4, new Piece(color, PieceType.KING));
    setPiece(row, 5, new Piece(color, PieceType.BISHOP));
    setPiece(row, 6, new Piece(color, PieceType.KNIGHT));
    setPiece(row, 7, new Piece(color, PieceType.ROOK));
  }

  private void placePawns(int row, PlayerColor color) {
    for (int col = 0; col < SIZE; col++) {
      setPiece(row, col, new Piece(color, PieceType.PAWN));
    }
  }

  public void clear() {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        pieces[row][col] = null;
      }
    }
  }
}

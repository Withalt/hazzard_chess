package com.hazardchess.model.io;

import com.hazardchess.model.Move;
import com.hazardchess.model.Square;

public final class MoveSnapshot {
  public int fromRow;
  public int fromCol;
  public int toRow;
  public int toCol;
  public boolean castleKingSide;
  public boolean castleQueenSide;
  public boolean enPassant;
  public com.hazardchess.model.PieceType promotion;

  public Move toMove() {
    return new Move(new Square(fromRow, fromCol), new Square(toRow, toCol), castleKingSide, castleQueenSide,
        enPassant, promotion);
  }
}

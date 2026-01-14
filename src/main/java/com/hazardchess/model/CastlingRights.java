package com.hazardchess.model;

public final class CastlingRights {
  private boolean whiteKingSide = true;
  private boolean whiteQueenSide = true;
  private boolean blackKingSide = true;
  private boolean blackQueenSide = true;

  public CastlingRights copy() {
    CastlingRights clone = new CastlingRights();
    clone.whiteKingSide = whiteKingSide;
    clone.whiteQueenSide = whiteQueenSide;
    clone.blackKingSide = blackKingSide;
    clone.blackQueenSide = blackQueenSide;
    return clone;
  }

  public boolean canWhiteKingSide() {
    return whiteKingSide;
  }

  public boolean canWhiteQueenSide() {
    return whiteQueenSide;
  }

  public boolean canBlackKingSide() {
    return blackKingSide;
  }

  public boolean canBlackQueenSide() {
    return blackQueenSide;
  }

  public void revokeWhiteKingSide() {
    whiteKingSide = false;
  }

  public void revokeWhiteQueenSide() {
    whiteQueenSide = false;
  }

  public void revokeBlackKingSide() {
    blackKingSide = false;
  }

  public void revokeBlackQueenSide() {
    blackQueenSide = false;
  }
}

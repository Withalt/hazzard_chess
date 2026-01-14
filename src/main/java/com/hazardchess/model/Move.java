package com.hazardchess.model;

public final class Move {
  private final Square from;
  private final Square to;
  private final boolean castleKingSide;
  private final boolean castleQueenSide;
  private final boolean enPassant;
  private final PieceType promotion;

  public Move(Square from, Square to) {
    this(from, to, false, false, false, null);
  }

  public Move(Square from, Square to, boolean castleKingSide, boolean castleQueenSide, boolean enPassant,
              PieceType promotion) {
    this.from = from;
    this.to = to;
    this.castleKingSide = castleKingSide;
    this.castleQueenSide = castleQueenSide;
    this.enPassant = enPassant;
    this.promotion = promotion;
  }

  public Square getFrom() {
    return from;
  }

  public Square getTo() {
    return to;
  }

  public boolean isCastleKingSide() {
    return castleKingSide;
  }

  public boolean isCastleQueenSide() {
    return castleQueenSide;
  }

  public boolean isEnPassant() {
    return enPassant;
  }

  public PieceType getPromotion() {
    return promotion;
  }

  public boolean isPromotion() {
    return promotion != null;
  }
}

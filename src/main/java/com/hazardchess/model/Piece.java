package com.hazardchess.model;

public final class Piece {
  private final PlayerColor color;
  private final PieceType type;

  public Piece(PlayerColor color, PieceType type) {
    this.color = color;
    this.type = type;
  }

  public PlayerColor getColor() {
    return color;
  }

  public PieceType getType() {
    return type;
  }
}

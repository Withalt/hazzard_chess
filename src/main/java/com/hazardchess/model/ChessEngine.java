package com.hazardchess.model;

import java.util.ArrayList;
import java.util.List;

public final class ChessEngine {
  private static final PieceType[] PROMOTION_OPTIONS = {
      PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
  };

  public List<Move> legalMoves(GameState state, Square from) {
    Board board = state.getBoard();
    Piece piece = board.getPiece(from.getRow(), from.getCol());
    if (piece == null || piece.getColor() != state.getToMove()) {
      return List.of();
    }
    List<Move> candidates = pseudoMoves(state, from, piece);
    List<Move> legal = new ArrayList<>();
    for (Move move : candidates) {
      if (state.getMinefield().isFlagged(move.getTo().getRow(), move.getTo().getCol(), piece.getColor())
          && !state.getMinefield().isRevealed(move.getTo().getRow(), move.getTo().getCol())) {
        continue;
      }
      if (isLegalAfterMove(state, move, piece.getColor())) {
        legal.add(move);
      }
    }
    return legal;
  }

  public List<Move> legalMoves(GameState state) {
    List<Move> moves = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        Piece piece = state.getBoard().getPiece(row, col);
        if (piece != null && piece.getColor() == state.getToMove()) {
          moves.addAll(legalMoves(state, new Square(row, col)));
        }
      }
    }
    return moves;
  }

  public boolean isInCheck(GameState state, PlayerColor color) {
    Square king = state.getBoard().findKing(color);
    if (king == null) {
      return true;
    }
    PlayerColor attacker = color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
    return isSquareAttacked(state.getBoard(), attacker, king);
  }

  public boolean isCheckmate(GameState state, PlayerColor color) {
    if (!isInCheck(state, color)) {
      return false;
    }
    PlayerColor original = state.getToMove();
    state.setToMove(color);
    boolean hasMoves = !legalMoves(state).isEmpty();
    state.setToMove(original);
    return !hasMoves;
  }

  public boolean isStalemate(GameState state, PlayerColor color) {
    if (isInCheck(state, color)) {
      return false;
    }
    PlayerColor original = state.getToMove();
    state.setToMove(color);
    boolean hasMoves = !legalMoves(state).isEmpty();
    state.setToMove(original);
    return !hasMoves;
  }

  private List<Move> pseudoMoves(GameState state, Square from, Piece piece) {
    List<Move> moves = new ArrayList<>();
    switch (piece.getType()) {
      case PAWN -> addPawnMoves(state, from, piece, moves);
      case KNIGHT -> addKnightMoves(state, from, piece, moves);
      case BISHOP -> addSlidingMoves(state, from, piece, moves, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
      case ROOK -> addSlidingMoves(state, from, piece, moves, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
      case QUEEN -> addSlidingMoves(state, from, piece, moves,
          new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
      case KING -> addKingMoves(state, from, piece, moves);
    }
    return moves;
  }

  private void addPawnMoves(GameState state, Square from, Piece piece, List<Move> moves) {
    Board board = state.getBoard();
    int dir = piece.getColor() == PlayerColor.WHITE ? -1 : 1;
    int startRow = piece.getColor() == PlayerColor.WHITE ? 6 : 1;
    int promotionRow = piece.getColor() == PlayerColor.WHITE ? 0 : 7;

    int oneRow = from.getRow() + dir;
    if (board.isInside(oneRow, from.getCol()) && board.isEmpty(oneRow, from.getCol())) {
      if (oneRow == promotionRow) {
        addPromotionMoves(from, new Square(oneRow, from.getCol()), moves);
      } else {
        moves.add(new Move(from, new Square(oneRow, from.getCol())));
      }
      if (from.getRow() == startRow) {
        int twoRow = from.getRow() + 2 * dir;
        if (board.isInside(twoRow, from.getCol()) && board.isEmpty(twoRow, from.getCol())) {
          moves.add(new Move(from, new Square(twoRow, from.getCol())));
        }
      }
    }

    for (int dc : new int[]{-1, 1}) {
      int targetRow = from.getRow() + dir;
      int targetCol = from.getCol() + dc;
      if (!board.isInside(targetRow, targetCol)) {
        continue;
      }
      Piece target = board.getPiece(targetRow, targetCol);
      if (target != null && target.getColor() != piece.getColor()) {
        if (targetRow == promotionRow) {
          addPromotionMoves(from, new Square(targetRow, targetCol), moves);
        } else {
          moves.add(new Move(from, new Square(targetRow, targetCol)));
        }
      } else if (state.getEnPassantTarget() != null
          && state.getEnPassantTarget().getRow() == targetRow
          && state.getEnPassantTarget().getCol() == targetCol) {
        moves.add(new Move(from, new Square(targetRow, targetCol), false, false, true, null));
      }
    }
  }

  private void addKnightMoves(GameState state, Square from, Piece piece, List<Move> moves) {
    Board board = state.getBoard();
    int[][] offsets = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
    for (int[] offset : offsets) {
      int r = from.getRow() + offset[0];
      int c = from.getCol() + offset[1];
      if (!board.isInside(r, c)) {
        continue;
      }
      Piece target = board.getPiece(r, c);
      if (target == null || target.getColor() != piece.getColor()) {
        moves.add(new Move(from, new Square(r, c)));
      }
    }
  }

  private void addSlidingMoves(GameState state, Square from, Piece piece, List<Move> moves, int[][] directions) {
    Board board = state.getBoard();
    for (int[] dir : directions) {
      int r = from.getRow() + dir[0];
      int c = from.getCol() + dir[1];
      while (board.isInside(r, c)) {
        Piece target = board.getPiece(r, c);
        if (target == null) {
          moves.add(new Move(from, new Square(r, c)));
        } else {
          if (target.getColor() != piece.getColor()) {
            moves.add(new Move(from, new Square(r, c)));
          }
          break;
        }
        r += dir[0];
        c += dir[1];
      }
    }
  }

  private void addKingMoves(GameState state, Square from, Piece piece, List<Move> moves) {
    Board board = state.getBoard();
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = from.getRow() + dr;
        int c = from.getCol() + dc;
        if (!board.isInside(r, c)) {
          continue;
        }
        Piece target = board.getPiece(r, c);
        if (target == null || target.getColor() != piece.getColor()) {
          moves.add(new Move(from, new Square(r, c)));
        }
      }
    }
    addCastlingMoves(state, piece, moves);
  }

  private void addCastlingMoves(GameState state, Piece piece, List<Move> moves) {
    PlayerColor color = piece.getColor();
    int row = color == PlayerColor.WHITE ? 7 : 0;
    if (isSquareAttacked(state.getBoard(), opposite(color), new Square(row, 4))) {
      return;
    }
    if (color == PlayerColor.WHITE) {
      if (state.getCastlingRights().canWhiteKingSide()
          && canCastleThrough(state, row, 5, 6, 7)) {
        moves.add(new Move(new Square(row, 4), new Square(row, 6), true, false, false, null));
      }
      if (state.getCastlingRights().canWhiteQueenSide()
          && canCastleThrough(state, row, 3, 2, 0)) {
        moves.add(new Move(new Square(row, 4), new Square(row, 2), false, true, false, null));
      }
    } else {
      if (state.getCastlingRights().canBlackKingSide()
          && canCastleThrough(state, row, 5, 6, 7)) {
        moves.add(new Move(new Square(row, 4), new Square(row, 6), true, false, false, null));
      }
      if (state.getCastlingRights().canBlackQueenSide()
          && canCastleThrough(state, row, 3, 2, 0)) {
        moves.add(new Move(new Square(row, 4), new Square(row, 2), false, true, false, null));
      }
    }
  }

  private boolean canCastleThrough(GameState state, int row, int stepCol, int destCol, int rookCol) {
    Board board = state.getBoard();
    if (!board.isEmpty(row, stepCol) || !board.isEmpty(row, destCol)) {
      return false;
    }
    Piece rook = board.getPiece(row, rookCol);
    if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != state.getToMove()) {
      return false;
    }
    PlayerColor enemy = opposite(state.getToMove());
    if (isSquareAttacked(board, enemy, new Square(row, stepCol))) {
      return false;
    }
    return !isSquareAttacked(board, enemy, new Square(row, destCol));
  }

  private void addPromotionMoves(Square from, Square to, List<Move> moves) {
    for (PieceType option : PROMOTION_OPTIONS) {
      moves.add(new Move(from, to, false, false, false, option));
    }
  }

  private boolean isLegalAfterMove(GameState state, Move move, PlayerColor color) {
    Board copy = state.getBoard().copy();
    applyMoveOnBoard(copy, move, color);
    Square king = copy.findKing(color);
    if (king == null) {
      return false;
    }
    return !isSquareAttacked(copy, opposite(color), king);
  }

  private void applyMoveOnBoard(Board board, Move move, PlayerColor color) {
    Piece moving = board.getPiece(move.getFrom().getRow(), move.getFrom().getCol());
    if (moving == null) {
      return;
    }
    if (move.isCastleKingSide() || move.isCastleQueenSide()) {
      int row = color == PlayerColor.WHITE ? 7 : 0;
      board.removePiece(row, 4);
      if (move.isCastleKingSide()) {
        board.setPiece(row, 6, moving);
        Piece rook = board.removePiece(row, 7);
        if (rook != null) {
          board.setPiece(row, 5, rook);
        }
      } else {
        board.setPiece(row, 2, moving);
        Piece rook = board.removePiece(row, 0);
        if (rook != null) {
          board.setPiece(row, 3, rook);
        }
      }
      return;
    }
    if (move.isEnPassant()) {
      int captureRow = move.getTo().getRow() + (color == PlayerColor.WHITE ? 1 : -1);
      board.removePiece(captureRow, move.getTo().getCol());
    } else {
      board.removePiece(move.getTo().getRow(), move.getTo().getCol());
    }
    board.removePiece(move.getFrom().getRow(), move.getFrom().getCol());
    Piece toPlace = moving;
    if (move.isPromotion()) {
      toPlace = new Piece(color, move.getPromotion());
    }
    board.setPiece(move.getTo().getRow(), move.getTo().getCol(), toPlace);
  }

  private boolean isSquareAttacked(Board board, PlayerColor attacker, Square target) {
    int dir = attacker == PlayerColor.WHITE ? -1 : 1;
    int pawnRow = target.getRow() - dir;
    for (int dc : new int[]{-1, 1}) {
      int pawnCol = target.getCol() + dc;
      if (board.isInside(pawnRow, pawnCol)) {
        Piece piece = board.getPiece(pawnRow, pawnCol);
        if (piece != null && piece.getColor() == attacker && piece.getType() == PieceType.PAWN) {
          return true;
        }
      }
    }

    int[][] knightOffsets = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
    for (int[] offset : knightOffsets) {
      int r = target.getRow() + offset[0];
      int c = target.getCol() + offset[1];
      if (board.isInside(r, c)) {
        Piece piece = board.getPiece(r, c);
        if (piece != null && piece.getColor() == attacker && piece.getType() == PieceType.KNIGHT) {
          return true;
        }
      }
    }

    if (isAttackedBySliding(board, attacker, target, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}},
        PieceType.ROOK, PieceType.QUEEN)) {
      return true;
    }
    if (isAttackedBySliding(board, attacker, target, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}},
        PieceType.BISHOP, PieceType.QUEEN)) {
      return true;
    }

    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = target.getRow() + dr;
        int c = target.getCol() + dc;
        if (board.isInside(r, c)) {
          Piece piece = board.getPiece(r, c);
          if (piece != null && piece.getColor() == attacker && piece.getType() == PieceType.KING) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean isAttackedBySliding(Board board, PlayerColor attacker, Square target, int[][] directions,
                                      PieceType typeA, PieceType typeB) {
    for (int[] dir : directions) {
      int r = target.getRow() + dir[0];
      int c = target.getCol() + dir[1];
      while (board.isInside(r, c)) {
        Piece piece = board.getPiece(r, c);
        if (piece != null) {
          if (piece.getColor() == attacker && (piece.getType() == typeA || piece.getType() == typeB)) {
            return true;
          }
          break;
        }
        r += dir[0];
        c += dir[1];
      }
    }
    return false;
  }

  private PlayerColor opposite(PlayerColor color) {
    return color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }
}

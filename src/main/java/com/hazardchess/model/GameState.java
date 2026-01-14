package com.hazardchess.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class GameState {
  private static final int DEFAULT_MINES = 12;

  private final Board board = new Board();
  private final Minefield minefield = new Minefield();
  private final List<Move> moveHistory = new ArrayList<>();
  private final Deque<Move> redoStack = new ArrayDeque<>();
  private final List<Piece> capturedWhite = new ArrayList<>();
  private final List<Piece> capturedBlack = new ArrayList<>();
  private final List<Piece> minedWhite = new ArrayList<>();
  private final List<Piece> minedBlack = new ArrayList<>();
  private final List<String> positionHistory = new ArrayList<>();
  private final Map<String, Integer> positionCounts = new HashMap<>();
  private int halfmoveClock;
  private long elapsedSeconds;
  private boolean botEnabled = true;
  private CastlingRights castlingRights = new CastlingRights();
  private Square enPassantTarget;
  private PlayerColor toMove = PlayerColor.WHITE;

  public GameState() {
    this(DEFAULT_MINES, new Random());
  }

  public GameState(int mineCount, Random random) {
    board.setupStandard();
    minefield.generate(mineCount, random, null);
    resetPositionHistory();
  }

  public Board getBoard() {
    return board;
  }

  public Minefield getMinefield() {
    return minefield;
  }

  public PlayerColor getToMove() {
    return toMove;
  }

  public List<Move> getMoveHistory() {
    return List.copyOf(moveHistory);
  }

  public List<Move> getRedoStack() {
    return List.copyOf(redoStack);
  }

  public List<Piece> getCapturedWhite() {
    return List.copyOf(capturedWhite);
  }

  public List<Piece> getCapturedBlack() {
    return List.copyOf(capturedBlack);
  }

  public List<Piece> getMinedWhite() {
    return List.copyOf(minedWhite);
  }

  public List<Piece> getMinedBlack() {
    return List.copyOf(minedBlack);
  }

  public long getElapsedSeconds() {
    return elapsedSeconds;
  }

  public int getHalfmoveClock() {
    return halfmoveClock;
  }

  public List<String> getPositionHistory() {
    return List.copyOf(positionHistory);
  }

  public boolean isBotEnabled() {
    return botEnabled;
  }

  public CastlingRights getCastlingRights() {
    return castlingRights;
  }

  public Square getEnPassantTarget() {
    return enPassantTarget;
  }

  public void setToMove(PlayerColor toMove) {
    this.toMove = toMove;
  }

  public void setMoveHistory(List<Move> history) {
    moveHistory.clear();
    if (history != null) {
      moveHistory.addAll(history);
    }
  }

  public void setRedoStack(List<Move> redoTopFirst) {
    redoStack.clear();
    if (redoTopFirst == null) {
      return;
    }
    for (int i = redoTopFirst.size() - 1; i >= 0; i--) {
      redoStack.push(redoTopFirst.get(i));
    }
  }

  public void setCapturedWhite(List<Piece> pieces) {
    capturedWhite.clear();
    if (pieces != null) {
      capturedWhite.addAll(pieces);
    }
  }

  public void setCapturedBlack(List<Piece> pieces) {
    capturedBlack.clear();
    if (pieces != null) {
      capturedBlack.addAll(pieces);
    }
  }

  public void setMinedWhite(List<Piece> pieces) {
    minedWhite.clear();
    if (pieces != null) {
      minedWhite.addAll(pieces);
    }
  }

  public void setMinedBlack(List<Piece> pieces) {
    minedBlack.clear();
    if (pieces != null) {
      minedBlack.addAll(pieces);
    }
  }

  public void setElapsedSeconds(long elapsedSeconds) {
    this.elapsedSeconds = Math.max(0, elapsedSeconds);
  }

  public void setHalfmoveClock(int halfmoveClock) {
    this.halfmoveClock = Math.max(0, halfmoveClock);
  }

  public void setPositionHistory(List<String> history) {
    positionHistory.clear();
    positionCounts.clear();
    if (history == null || history.isEmpty()) {
      resetPositionHistory();
      return;
    }
    for (String hash : history) {
      addPositionHash(hash);
    }
  }

  public void setBotEnabled(boolean botEnabled) {
    this.botEnabled = botEnabled;
  }

  public void setCastlingRights(CastlingRights castlingRights) {
    this.castlingRights = castlingRights == null ? new CastlingRights() : castlingRights;
  }

  public void setEnPassantTarget(Square enPassantTarget) {
    this.enPassantTarget = enPassantTarget;
  }

  public void applyMove(Move move) {
    Piece moving = board.getPiece(move.getFrom().getRow(), move.getFrom().getCol());
    if (moving == null) {
      return;
    }
    Piece captured = board.getPiece(move.getTo().getRow(), move.getTo().getCol());
    updateCastlingRightsForMove(moving, move);
    if (captured != null) {
      updateCastlingRightsForCapture(captured, move.getTo());
    }

    if (move.isCastleKingSide() || move.isCastleQueenSide()) {
      applyCastlingMove(moving, move);
      boolean exploded = handleMineEffects(move, moving, null);
      updateHalfmoveClock(moving, false, false, exploded);
    } else {
      CapturedPieces capturedPieces = applyStandardMove(moving, move, captured);
      boolean exploded = handleMineEffects(move, moving, captured);
      boolean capturedAny = capturedPieces.atDestination != null || capturedPieces.enPassant != null;
      if (capturedPieces.enPassant != null) {
        recordCapture(capturedPieces.enPassant, moving.getColor());
      }
      if (capturedPieces.atDestination != null) {
        if (exploded) {
          recordMineDeath(capturedPieces.atDestination);
        } else {
          recordCapture(capturedPieces.atDestination, moving.getColor());
        }
      }
      updateHalfmoveClock(moving, capturedAny, capturedPieces.enPassant != null, exploded);
    }

    updateEnPassantTarget(moving, move);

    moveHistory.add(move);
    redoStack.clear();
    toMove = (toMove == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    resolvePendingMines();
    updatePositionHistory();
  }

  public boolean canUndo() {
    return !moveHistory.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  public Move undo() {
    if (moveHistory.isEmpty()) {
      return null;
    }
    Move last = moveHistory.remove(moveHistory.size() - 1);
    redoStack.push(last);
    toMove = (toMove == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    return last;
  }

  public Move redo() {
    if (redoStack.isEmpty()) {
      return null;
    }
    Move next = redoStack.pop();
    moveHistory.add(next);
    toMove = (toMove == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    return next;
  }

  private CapturedPieces applyStandardMove(Piece moving, Move move, Piece captured) {
    Piece capturedEnPassant = null;
    if (move.isEnPassant()) {
      int captureRow = move.getTo().getRow() + (moving.getColor() == PlayerColor.WHITE ? 1 : -1);
      capturedEnPassant = board.removePiece(captureRow, move.getTo().getCol());
    } else if (captured != null) {
      board.removePiece(move.getTo().getRow(), move.getTo().getCol());
    }
    board.removePiece(move.getFrom().getRow(), move.getFrom().getCol());
    Piece toPlace = moving;
    if (move.isPromotion()) {
      toPlace = new Piece(moving.getColor(), move.getPromotion());
    }
    board.setPiece(move.getTo().getRow(), move.getTo().getCol(), toPlace);
    return new CapturedPieces(captured, capturedEnPassant);
  }

  private void applyCastlingMove(Piece moving, Move move) {
    int row = moving.getColor() == PlayerColor.WHITE ? 7 : 0;
    if (move.isCastleKingSide()) {
      board.removePiece(row, 4);
      board.setPiece(row, 6, moving);
      Piece rook = board.removePiece(row, 7);
      if (rook != null) {
        board.setPiece(row, 5, rook);
      }
    } else {
      board.removePiece(row, 4);
      board.setPiece(row, 2, moving);
      Piece rook = board.removePiece(row, 0);
      if (rook != null) {
        board.setPiece(row, 3, rook);
      }
    }
  }

  private void updateEnPassantTarget(Piece moving, Move move) {
    enPassantTarget = null;
    if (moving.getType() != PieceType.PAWN) {
      return;
    }
    int delta = Math.abs(move.getFrom().getRow() - move.getTo().getRow());
    if (delta == 2) {
      int row = (move.getFrom().getRow() + move.getTo().getRow()) / 2;
      enPassantTarget = new Square(row, move.getFrom().getCol());
    }
  }

  private void updateCastlingRightsForMove(Piece moving, Move move) {
    if (moving.getType() == PieceType.KING) {
      if (moving.getColor() == PlayerColor.WHITE) {
        castlingRights.revokeWhiteKingSide();
        castlingRights.revokeWhiteQueenSide();
      } else {
        castlingRights.revokeBlackKingSide();
        castlingRights.revokeBlackQueenSide();
      }
    }
    if (moving.getType() == PieceType.ROOK) {
      if (moving.getColor() == PlayerColor.WHITE) {
        if (move.getFrom().getRow() == 7 && move.getFrom().getCol() == 0) {
          castlingRights.revokeWhiteQueenSide();
        } else if (move.getFrom().getRow() == 7 && move.getFrom().getCol() == 7) {
          castlingRights.revokeWhiteKingSide();
        }
      } else {
        if (move.getFrom().getRow() == 0 && move.getFrom().getCol() == 0) {
          castlingRights.revokeBlackQueenSide();
        } else if (move.getFrom().getRow() == 0 && move.getFrom().getCol() == 7) {
          castlingRights.revokeBlackKingSide();
        }
      }
    }
  }

  private void updateCastlingRightsForCapture(Piece captured, Square at) {
    if (captured.getType() != PieceType.ROOK) {
      return;
    }
    if (captured.getColor() == PlayerColor.WHITE) {
      if (at.getRow() == 7 && at.getCol() == 0) {
        castlingRights.revokeWhiteQueenSide();
      } else if (at.getRow() == 7 && at.getCol() == 7) {
        castlingRights.revokeWhiteKingSide();
      }
    } else {
      if (at.getRow() == 0 && at.getCol() == 0) {
        castlingRights.revokeBlackQueenSide();
      } else if (at.getRow() == 0 && at.getCol() == 7) {
        castlingRights.revokeBlackKingSide();
      }
    }
  }

  private boolean handleMineEffects(Move move, Piece moving, Piece capturedAtDestination) {
    int toRow = move.getTo().getRow();
    int toCol = move.getTo().getCol();
    if (minefield.hasMine(toRow, toCol) && !minefield.isExploded(toRow, toCol)) {
      if (minefield.isArmed(toRow, toCol)) {
        minefield.explode(toRow, toCol);
        Piece removed = board.removePiece(toRow, toCol);
        if (removed != null) {
          recordMineDeath(removed);
        }
        if (capturedAtDestination != null) {
          recordMineDeath(capturedAtDestination);
        }
        return true;
      }
      minefield.armMine(toRow, toCol);
      return false;
    }
    if (!minefield.isRevealed(toRow, toCol)) {
      if (minefield.adjacentMines(toRow, toCol) == 0) {
        minefield.revealFlood(toRow, toCol, moving.getColor());
      } else {
        minefield.reveal(toRow, toCol);
      }
    }
    if (minefield.isRevealed(toRow, toCol) && minefield.adjacentMines(toRow, toCol) > 0) {
      boolean exploded = triggerQuickOpen(toRow, toCol, moving.getColor());
      if (exploded) {
        Piece removed = board.removePiece(toRow, toCol);
        if (removed != null) {
          recordMineDeath(removed);
        }
        return true;
      }
    }
    return false;
  }

  private boolean triggerQuickOpen(int row, int col, PlayerColor moverColor) {
    int flags = minefield.countFlagsAround(row, col, moverColor);
    int required = minefield.adjacentMines(row, col);
    if (flags != required) {
      return false;
    }
    boolean exploded = false;
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (!board.isInside(r, c) || minefield.isFlagged(r, c, moverColor)) {
          continue;
        }
        if (minefield.hasMine(r, c) && !minefield.isExploded(r, c)) {
          minefield.explode(r, c);
          Piece removed = board.removePiece(r, c);
          if (removed != null) {
            recordMineDeath(removed);
          }
          exploded = true;
        } else {
          minefield.revealFlood(r, c, moverColor);
        }
      }
    }
    return exploded;
  }

  private void recordCapture(Piece piece, PlayerColor capturer) {
    if (piece == null) {
      return;
    }
    if (capturer == PlayerColor.WHITE) {
      capturedWhite.add(piece);
    } else {
      capturedBlack.add(piece);
    }
  }

  private void recordMineDeath(Piece piece) {
    if (piece == null) {
      return;
    }
    if (piece.getColor() == PlayerColor.WHITE) {
      minedWhite.add(piece);
    } else {
      minedBlack.add(piece);
    }
    halfmoveClock = 0;
  }

  private void resolvePendingMines() {
    List<int[]> armed = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        if (minefield.isArmed(row, col)) {
          armed.add(new int[]{row, col});
        }
      }
    }
    if (armed.isEmpty()) {
      return;
    }
    minefield.tickArmed();
    for (int[] cell : armed) {
      int row = cell[0];
      int col = cell[1];
      if (minefield.getArmedTurns(row, col) == 0
          && minefield.hasMine(row, col)
          && !minefield.isExploded(row, col)) {
        minefield.explode(row, col);
        Piece removed = board.removePiece(row, col);
        if (removed != null) {
          recordMineDeath(removed);
        }
      }
    }
  }

  public boolean isFiftyMoveDraw() {
    return halfmoveClock >= 100;
  }

  public boolean isThreefoldRepetition() {
    if (positionHistory.isEmpty()) {
      return false;
    }
    String current = positionHistory.get(positionHistory.size() - 1);
    return positionCounts.getOrDefault(current, 0) >= 3;
  }

  public boolean isInsufficientMaterial() {
    int whiteBishops = 0;
    int blackBishops = 0;
    int whiteKnights = 0;
    int blackKnights = 0;
    int whiteOther = 0;
    int blackOther = 0;
    List<Integer> bishopColors = new ArrayList<>();

    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        Piece piece = board.getPiece(row, col);
        if (piece == null || piece.getType() == PieceType.KING) {
          continue;
        }
        boolean white = piece.getColor() == PlayerColor.WHITE;
        switch (piece.getType()) {
          case PAWN, ROOK, QUEEN -> {
            if (white) {
              whiteOther++;
            } else {
              blackOther++;
            }
          }
          case BISHOP -> {
            if (white) {
              whiteBishops++;
            } else {
              blackBishops++;
            }
            bishopColors.add((row + col) % 2);
          }
          case KNIGHT -> {
            if (white) {
              whiteKnights++;
            } else {
              blackKnights++;
            }
          }
          default -> {
          }
        }
      }
    }

    if (whiteOther > 0 || blackOther > 0) {
      return false;
    }
    int totalMinors = whiteBishops + blackBishops + whiteKnights + blackKnights;
    if (totalMinors == 0 || totalMinors == 1) {
      return true;
    }
    if (totalMinors == 2) {
      if (whiteKnights + blackKnights == 2) {
        return true;
      }
      if (whiteBishops + blackBishops == 2
          && bishopColors.size() == 2
          && bishopColors.get(0).equals(bishopColors.get(1))) {
        return true;
      }
    }
    return false;
  }

  public boolean isDraw(ChessEngine engine) {
    return (engine != null && engine.isStalemate(this, toMove))
        || isThreefoldRepetition()
        || isFiftyMoveDraw()
        || isInsufficientMaterial();
  }

  public void resetPositionHistory() {
    positionHistory.clear();
    positionCounts.clear();
    addPositionHash(buildPositionHash());
  }

  private void updatePositionHistory() {
    addPositionHash(buildPositionHash());
  }

  private void addPositionHash(String hash) {
    positionHistory.add(hash);
    positionCounts.put(hash, positionCounts.getOrDefault(hash, 0) + 1);
  }

  private void updateHalfmoveClock(Piece moving, boolean capturedAny, boolean enPassantCaptured, boolean exploded) {
    if (moving.getType() == PieceType.PAWN || capturedAny || enPassantCaptured || exploded) {
      halfmoveClock = 0;
      return;
    }
    halfmoveClock++;
  }

  private String buildPositionHash() {
    StringBuilder sb = new StringBuilder();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        Piece piece = board.getPiece(row, col);
        if (piece == null) {
          sb.append('.');
          continue;
        }
        char code = switch (piece.getType()) {
          case KING -> 'k';
          case QUEEN -> 'q';
          case ROOK -> 'r';
          case BISHOP -> 'b';
          case KNIGHT -> 'n';
          case PAWN -> 'p';
        };
        sb.append(piece.getColor() == PlayerColor.WHITE ? Character.toUpperCase(code) : code);
      }
    }
    sb.append('|');
    sb.append(toMove == PlayerColor.WHITE ? 'w' : 'b');
    sb.append('|');
    sb.append(castlingRights.canWhiteKingSide() ? 'K' : '-');
    sb.append(castlingRights.canWhiteQueenSide() ? 'Q' : '-');
    sb.append(castlingRights.canBlackKingSide() ? 'k' : '-');
    sb.append(castlingRights.canBlackQueenSide() ? 'q' : '-');
    sb.append('|');
    if (enPassantTarget == null) {
      sb.append("--");
    } else {
      sb.append(enPassantTarget.getRow()).append(enPassantTarget.getCol());
    }
    sb.append('|');
    appendBoolGrid(sb, minefield.copyMines());
    sb.append('|');
    appendBoolGrid(sb, minefield.copyRevealed());
    sb.append('|');
    appendBoolGrid(sb, minefield.copyExploded());
    sb.append('|');
    appendBoolGrid(sb, minefield.copyFlaggedWhite());
    sb.append('|');
    appendBoolGrid(sb, minefield.copyFlaggedBlack());
    sb.append('|');
    int[][] armed = minefield.copyArmedTurns();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        sb.append(armed[row][col]);
      }
    }
    return sb.toString();
  }

  private void appendBoolGrid(StringBuilder sb, boolean[][] grid) {
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        sb.append(grid[row][col] ? '1' : '0');
      }
    }
  }

  private static final class CapturedPieces {
    private final Piece atDestination;
    private final Piece enPassant;

    private CapturedPieces(Piece atDestination, Piece enPassant) {
      this.atDestination = atDestination;
      this.enPassant = enPassant;
    }
  }
}



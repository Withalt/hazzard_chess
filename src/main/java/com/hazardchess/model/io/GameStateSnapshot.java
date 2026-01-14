package com.hazardchess.model.io;

import com.hazardchess.model.Board;
import com.hazardchess.model.CastlingRights;
import com.hazardchess.model.GameState;
import com.hazardchess.model.Minefield;
import com.hazardchess.model.Move;
import com.hazardchess.model.Piece;
import com.hazardchess.model.PlayerColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GameStateSnapshot {
  public PlayerColor toMove;
  public PieceSnapshot[][] board;
  public boolean[][] mines;
  public boolean[][] revealed;
  public boolean[][] exploded;
  public boolean[][] flagged;
  public boolean[][] flaggedWhite;
  public boolean[][] flaggedBlack;
  public int[][] armedTurns;
  public Boolean whiteKingSide;
  public Boolean whiteQueenSide;
  public Boolean blackKingSide;
  public Boolean blackQueenSide;
  public Integer enPassantRow;
  public Integer enPassantCol;
  public List<MoveSnapshot> moveHistory;
  public List<MoveSnapshot> redoStack;
  public List<PieceSnapshot> capturedWhite;
  public List<PieceSnapshot> capturedBlack;
  public List<PieceSnapshot> minedWhite;
  public List<PieceSnapshot> minedBlack;
  public Long elapsedSeconds;
  public Boolean botEnabled;
  public Integer halfmoveClock;
  public List<String> positionHistory;

  public static GameStateSnapshot from(GameState state) {
    GameStateSnapshot snapshot = new GameStateSnapshot();
    snapshot.toMove = state.getToMove();
    snapshot.board = captureBoard(state.getBoard());
    snapshot.mines = state.getMinefield().copyMines();
    snapshot.revealed = state.getMinefield().copyRevealed();
    snapshot.exploded = state.getMinefield().copyExploded();
    snapshot.flaggedWhite = state.getMinefield().copyFlaggedWhite();
    snapshot.flaggedBlack = state.getMinefield().copyFlaggedBlack();
    snapshot.armedTurns = state.getMinefield().copyArmedTurns();
    snapshot.whiteKingSide = state.getCastlingRights().canWhiteKingSide();
    snapshot.whiteQueenSide = state.getCastlingRights().canWhiteQueenSide();
    snapshot.blackKingSide = state.getCastlingRights().canBlackKingSide();
    snapshot.blackQueenSide = state.getCastlingRights().canBlackQueenSide();
    if (state.getEnPassantTarget() != null) {
      snapshot.enPassantRow = state.getEnPassantTarget().getRow();
      snapshot.enPassantCol = state.getEnPassantTarget().getCol();
    }
    snapshot.moveHistory = captureMoves(state.getMoveHistory());
    snapshot.redoStack = captureMoves(state.getRedoStack());
    snapshot.capturedWhite = capturePieces(state.getCapturedWhite());
    snapshot.capturedBlack = capturePieces(state.getCapturedBlack());
    snapshot.minedWhite = capturePieces(state.getMinedWhite());
    snapshot.minedBlack = capturePieces(state.getMinedBlack());
    snapshot.elapsedSeconds = state.getElapsedSeconds();
    snapshot.botEnabled = state.isBotEnabled();
    snapshot.halfmoveClock = state.getHalfmoveClock();
    snapshot.positionHistory = new ArrayList<>(state.getPositionHistory());
    return snapshot;
  }

  public GameState toGameState() {
    GameState state = new GameState(0, new Random());
    Board boardModel = state.getBoard();
    boardModel.clear();
    if (board != null) {
      int rows = Math.min(Board.SIZE, board.length);
      for (int row = 0; row < rows; row++) {
        if (board[row] == null) {
          continue;
        }
        int cols = Math.min(Board.SIZE, board[row].length);
        for (int col = 0; col < cols; col++) {
          PieceSnapshot piece = board[row][col];
          if (piece != null) {
            boardModel.setPiece(row, col, new Piece(piece.color, piece.type));
          }
        }
      }
    }
    Minefield minefield = state.getMinefield();
    if (mines != null && revealed != null && exploded != null) {
      boolean[][] whiteFlags = flaggedWhite;
      boolean[][] blackFlags = flaggedBlack;
      int[][] armed = armedTurns;
      if (whiteFlags == null && flagged != null) {
        whiteFlags = flagged;
      }
      if (whiteFlags == null) {
        whiteFlags = new boolean[Board.SIZE][Board.SIZE];
      }
      if (blackFlags == null) {
        blackFlags = new boolean[Board.SIZE][Board.SIZE];
      }
      if (armed == null) {
        armed = new int[Board.SIZE][Board.SIZE];
      }
      minefield.setState(mines, revealed, exploded, whiteFlags, blackFlags, armed);
    }
    CastlingRights rights = new CastlingRights();
    if (Boolean.FALSE.equals(whiteKingSide)) {
      rights.revokeWhiteKingSide();
    }
    if (Boolean.FALSE.equals(whiteQueenSide)) {
      rights.revokeWhiteQueenSide();
    }
    if (Boolean.FALSE.equals(blackKingSide)) {
      rights.revokeBlackKingSide();
    }
    if (Boolean.FALSE.equals(blackQueenSide)) {
      rights.revokeBlackQueenSide();
    }
    state.setCastlingRights(rights);
    if (enPassantRow != null && enPassantCol != null) {
      state.setEnPassantTarget(new com.hazardchess.model.Square(enPassantRow, enPassantCol));
    }
    state.setToMove(toMove == null ? PlayerColor.WHITE : toMove);
    state.setMoveHistory(restoreMoves(moveHistory));
    state.setRedoStack(restoreMoves(redoStack));
    state.setCapturedWhite(restorePieces(capturedWhite));
    state.setCapturedBlack(restorePieces(capturedBlack));
    state.setMinedWhite(restorePieces(minedWhite));
    state.setMinedBlack(restorePieces(minedBlack));
    if (elapsedSeconds != null) {
      state.setElapsedSeconds(elapsedSeconds);
    }
    if (botEnabled != null) {
      state.setBotEnabled(botEnabled);
    }
    if (halfmoveClock != null) {
      state.setHalfmoveClock(halfmoveClock);
    }
    state.setPositionHistory(positionHistory);
    return state;
  }

  private static PieceSnapshot[][] captureBoard(Board boardModel) {
    PieceSnapshot[][] snapshot = new PieceSnapshot[Board.SIZE][Board.SIZE];
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        Piece piece = boardModel.getPiece(row, col);
        if (piece != null) {
          PieceSnapshot copy = new PieceSnapshot();
          copy.color = piece.getColor();
          copy.type = piece.getType();
          snapshot[row][col] = copy;
        }
      }
    }
    return snapshot;
  }

  private static List<MoveSnapshot> captureMoves(List<Move> moves) {
    List<MoveSnapshot> result = new ArrayList<>();
    if (moves == null) {
      return result;
    }
    for (Move move : moves) {
      MoveSnapshot snapshot = new MoveSnapshot();
      snapshot.fromRow = move.getFrom().getRow();
      snapshot.fromCol = move.getFrom().getCol();
      snapshot.toRow = move.getTo().getRow();
      snapshot.toCol = move.getTo().getCol();
      snapshot.castleKingSide = move.isCastleKingSide();
      snapshot.castleQueenSide = move.isCastleQueenSide();
      snapshot.enPassant = move.isEnPassant();
      snapshot.promotion = move.getPromotion();
      result.add(snapshot);
    }
    return result;
  }

  private static List<Move> restoreMoves(List<MoveSnapshot> moves) {
    List<Move> result = new ArrayList<>();
    if (moves == null) {
      return result;
    }
    for (MoveSnapshot move : moves) {
      result.add(move.toMove());
    }
    return result;
  }

  private static List<PieceSnapshot> capturePieces(List<Piece> pieces) {
    List<PieceSnapshot> result = new ArrayList<>();
    if (pieces == null) {
      return result;
    }
    for (Piece piece : pieces) {
      PieceSnapshot snapshot = new PieceSnapshot();
      snapshot.color = piece.getColor();
      snapshot.type = piece.getType();
      result.add(snapshot);
    }
    return result;
  }

  private static List<Piece> restorePieces(List<PieceSnapshot> pieces) {
    List<Piece> result = new ArrayList<>();
    if (pieces == null) {
      return result;
    }
    for (PieceSnapshot piece : pieces) {
      result.add(new Piece(piece.color, piece.type));
    }
    return result;
  }
}

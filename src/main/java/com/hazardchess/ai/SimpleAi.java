package com.hazardchess.ai;

import com.hazardchess.model.Board;
import com.hazardchess.model.ChessEngine;
import com.hazardchess.model.GameState;
import com.hazardchess.model.Minefield;
import com.hazardchess.model.Move;
import com.hazardchess.model.Piece;
import com.hazardchess.model.PieceType;
import com.hazardchess.model.PlayerColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SimpleAi {
  private final ChessEngine engine = new ChessEngine();
  private final Random random = new Random();

  public Move chooseMove(GameState state, AiDifficulty difficulty) {
    List<Move> moves = engine.legalMoves(state);
    if (moves.isEmpty()) {
      return null;
    }
    if (difficulty == AiDifficulty.EASY) {
      return moves.get(random.nextInt(moves.size()));
    }
    int bestScore = Integer.MIN_VALUE;
    List<Move> best = new ArrayList<>();
    for (Move move : moves) {
      int score = scoreMove(state, move, difficulty);
      if (score > bestScore) {
        bestScore = score;
        best.clear();
        best.add(move);
      } else if (score == bestScore) {
        best.add(move);
      }
    }
    return best.get(random.nextInt(best.size()));
  }

  public boolean placeFlags(GameState state, AiDifficulty difficulty) {
    if (difficulty == AiDifficulty.EASY) {
      return false;
    }
    Minefield minefield = state.getMinefield();
    boolean changed = false;
    for (int row = 0; row < Board.SIZE; row++) {
      for (int col = 0; col < Board.SIZE; col++) {
        if (!minefield.isRevealed(row, col) || minefield.isExploded(row, col)) {
          continue;
        }
        int required = minefield.adjacentMines(row, col);
        if (required == 0) {
          continue;
        }
        int flagged = minefield.countFlagsAround(row, col, PlayerColor.BLACK);
        int unknown = countUnknownNeighbors(minefield, row, col);
        if (required - flagged == unknown) {
          changed |= flagUnknownNeighbors(minefield, row, col);
        }
      }
    }
    return changed;
  }

  private int scoreMove(GameState state, Move move, AiDifficulty difficulty) {
    Board board = state.getBoard();
    Piece target = board.getPiece(move.getTo().getRow(), move.getTo().getCol());
    int score = 0;
    if (target != null) {
      score += captureValue(target.getType());
    }
    if (difficulty != AiDifficulty.EASY) {
      Minefield minefield = state.getMinefield();
      if (minefield.isRevealed(move.getTo().getRow(), move.getTo().getCol())) {
        score += difficulty == AiDifficulty.HARD ? 3 : 1;
      } else {
        score -= difficulty == AiDifficulty.HARD ? 3 : 1;
      }
    }
    return score;
  }

  private int captureValue(PieceType type) {
    return switch (type) {
      case QUEEN -> 9;
      case ROOK -> 5;
      case BISHOP, KNIGHT -> 3;
      case PAWN -> 1;
      case KING -> 20;
    };
  }

  private int countUnknownNeighbors(Minefield minefield, int row, int col) {
    int unknown = 0;
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (!isInside(r, c)) {
          continue;
        }
        if (!minefield.isRevealed(r, c) && !minefield.isExploded(r, c)
            && !minefield.isFlagged(r, c, PlayerColor.BLACK)) {
          unknown++;
        }
      }
    }
    return unknown;
  }

  private boolean flagUnknownNeighbors(Minefield minefield, int row, int col) {
    boolean changed = false;
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (!isInside(r, c)) {
          continue;
        }
        if (!minefield.isRevealed(r, c) && !minefield.isExploded(r, c)
            && !minefield.isFlagged(r, c, PlayerColor.BLACK)) {
          minefield.toggleFlag(r, c, PlayerColor.BLACK);
          changed = true;
        }
      }
    }
    return changed;
  }

  private boolean isInside(int row, int col) {
    return row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE;
  }
}

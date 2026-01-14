package com.hazardchess.ui;

import com.hazardchess.model.Board;
import com.hazardchess.model.ChessEngine;
import com.hazardchess.model.GameState;
import com.hazardchess.model.Minefield;
import com.hazardchess.model.Piece;
import com.hazardchess.model.Move;
import com.hazardchess.model.PieceType;
import com.hazardchess.model.PlayerColor;
import com.hazardchess.model.Square;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.util.Duration;

public class BoardView extends GridPane {
  private static final int SIZE = 8;
  private static final double CELL = 64;

  private final ChessEngine engine = new ChessEngine();
  private final CellView[][] cells = new CellView[SIZE][SIZE];
  private GameState state;
  private DisplayMode mode = DisplayMode.CHESS;
  private PlayerColor activeFlagOwner = PlayerColor.WHITE;
  private Square selected;
  private List<Move> highlightMoves = new ArrayList<>();
  private CellClickHandler clickHandler;
  private boolean showBotFlags;
  private boolean showBothFlags;
  private boolean selectedHasNoMoves;
  private final boolean[][] lastRevealed = new boolean[SIZE][SIZE];
  private final boolean[][] lastExploded = new boolean[SIZE][SIZE];
  private Square suppressedSquare;
  private Square whiteKingCheck;
  private Square blackKingCheck;

  public BoardView() {
    setHgap(0);
    setVgap(0);
    setPrefSize(SIZE * CELL, SIZE * CELL);

    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        CellView cell = new CellView(row, col);
        cells[row][col] = cell;
        add(cell, col, row);
      }
    }
  }

  public void setDisplayMode(DisplayMode mode) {
    this.mode = mode;
    refresh();
  }

  public void setGameState(GameState state) {
    this.state = state;
    refresh();
  }

  public void setSelected(Square selected) {
    this.selected = selected;
    refresh();
  }

  public void setShowBotFlags(boolean showBotFlags) {
    this.showBotFlags = showBotFlags;
    refresh();
  }

  public void setShowBothFlags(boolean showBothFlags) {
    this.showBothFlags = showBothFlags;
    refresh();
  }

  public void setActiveFlagOwner(PlayerColor color) {
    if (color == null) {
      return;
    }
    activeFlagOwner = color;
    Color flagColor = activeFlagOwner == PlayerColor.WHITE ? Color.web("#C94B4B") : Color.web("#7C8A96");
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        cells[row][col].setPlayerFlagColor(flagColor);
      }
    }
    refresh();
  }

  public void setHighlightMoves(List<Move> moves) {
    highlightMoves = moves == null ? new ArrayList<>() : new ArrayList<>(moves);
    refresh();
  }

  public void suppressPieceAt(Square square) {
    suppressedSquare = square;
    refresh();
  }

  public void clearSuppressedPiece() {
    suppressedSquare = null;
    refresh();
  }

  public Point2D getCellCenterInScene(int row, int col) {
    CellView cell = cells[row][col];
    double x = cell.getWidth() / 2.0;
    double y = cell.getHeight() / 2.0;
    return cell.localToScene(x, y);
  }

  public void setCellClickHandler(CellClickHandler handler) {
    this.clickHandler = handler;
  }

  public void refresh() {
    selectedHasNoMoves = selected != null && highlightMoves.isEmpty();
    if (state != null) {
      whiteKingCheck = engine.isInCheck(state, PlayerColor.WHITE) ? state.getBoard().findKing(PlayerColor.WHITE) : null;
      blackKingCheck = engine.isInCheck(state, PlayerColor.BLACK) ? state.getBoard().findKing(PlayerColor.BLACK) : null;
    } else {
      whiteKingCheck = null;
      blackKingCheck = null;
    }
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        boolean isSelected = selected != null && selected.getRow() == row && selected.getCol() == col;
        MoveHighlight highlight = findHighlight(row, col);
        boolean nowRevealed = state != null
            && (state.getMinefield().isRevealed(row, col) || state.getMinefield().isArmed(row, col));
        boolean revealTransition = nowRevealed && !lastRevealed[row][col];
        boolean nowExploded = state != null && state.getMinefield().isExploded(row, col);
        boolean explodeTransition = nowExploded && !lastExploded[row][col];
        cells[row][col].update(state, mode, isSelected, highlight, selectedHasNoMoves,
            revealTransition, explodeTransition);
        lastRevealed[row][col] = nowRevealed;
        lastExploded[row][col] = nowExploded;
      }
    }
  }

  public interface CellClickHandler {
    void onCellClicked(int row, int col, boolean rightClick);
  }

  private final class CellView extends StackPane {
    private final Rectangle background = new Rectangle(CELL, CELL);
    private final Rectangle selection = new Rectangle(CELL - 6, CELL - 6);
    private final Rectangle hintBorder = new Rectangle(CELL - 12, CELL - 12);
    private final Rectangle explosionFlash = new Rectangle(CELL, CELL);
    private final Label mineLabel = new Label();
    private final StackPane pieceIcon = new StackPane();
    private final Circle pieceBase = new Circle(CELL * 0.26);
    private final Label pieceText = new Label();
    private final Polygon flagShape = new Polygon(
        3.0, 2.0,
        16.0, 6.0,
        3.0, 10.0
    );
    private final Group flagIcon = buildFlagIcon(flagShape, Color.web("#C94B4B"));
    private final Group botFlagIcon = buildFlagIcon(new Polygon(
        3.0, 2.0,
        16.0, 6.0,
        3.0, 10.0
    ), Color.web("#7C8A96"));
    private final Group bombIcon = buildBombIcon();
    private final Overlay flagOverlay;
    private final Overlay botFlagOverlay;
    private final Overlay bombOverlay;
    private final Overlay numberOverlay;
    private final Line splitLine = new Line();
    private final int row;
    private final int col;

    CellView(int row, int col) {
      this.row = row;
      this.col = col;
      setAlignment(Pos.CENTER);

      boolean dark = (row + col) % 2 == 1;
      background.setFill(dark ? Color.web("#5C6F7B") : Color.web("#E5E0D8"));
      background.setStrokeType(StrokeType.INSIDE);
      selection.setFill(Color.TRANSPARENT);
      selection.setStroke(Color.web("#7FBF7F"));
      selection.setStrokeWidth(3);
      selection.setVisible(false);

      hintBorder.setFill(Color.TRANSPARENT);
      hintBorder.setStroke(Color.web("#66A266"));
      hintBorder.setStrokeWidth(3);
      hintBorder.setVisible(false);

      explosionFlash.setFill(Color.web("#C84545"));
      explosionFlash.setOpacity(0);

      pieceText.setStyle("-fx-font-size: 20px;");
      pieceIcon.getChildren().addAll(pieceBase, pieceText);

      mineLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2F2F2F;");
      StackPane.setAlignment(mineLabel, Pos.CENTER);
      flagOverlay = buildOverlay(flagIcon);
      botFlagOverlay = buildOverlay(botFlagIcon);
      bombOverlay = buildOverlay(bombIcon);
      numberOverlay = buildOverlay(mineLabel);

      splitLine.setStartX(12);
      splitLine.setStartY(12);
      splitLine.setEndX(CELL - 12);
      splitLine.setEndY(CELL - 12);
      splitLine.setStroke(Color.web("#2F2F2F"));
      splitLine.setStrokeWidth(4);
      splitLine.setOpacity(0.4);
      splitLine.setVisible(false);

      flagOverlay.pane.setVisible(false);
      botFlagOverlay.pane.setVisible(false);
      bombOverlay.pane.setVisible(false);
      numberOverlay.pane.setVisible(false);

      getChildren().addAll(background, explosionFlash, selection, hintBorder, pieceIcon, splitLine,
          numberOverlay.pane, bombOverlay.pane, flagOverlay.pane, botFlagOverlay.pane);

      setOnMouseClicked(event -> {
        if (clickHandler == null) {
          return;
        }
        boolean right = event.getButton() == MouseButton.SECONDARY;
        clickHandler.onCellClicked(row, col, right);
      });
    }

    void update(GameState state, DisplayMode mode, boolean isSelected, MoveHighlight highlight,
                boolean noMovesSelected, boolean revealTransition, boolean explodeTransition) {
      selection.setVisible(isSelected);
      selection.setStroke(noMovesSelected && isSelected ? Color.web("#C15A5A") : Color.web("#7FBF7F"));
      pieceIcon.setVisible(false);
      mineLabel.setText("");
      flagOverlay.pane.setVisible(false);
      botFlagOverlay.pane.setVisible(false);
      bombOverlay.pane.setVisible(false);
      numberOverlay.pane.setVisible(false);
      hintBorder.setVisible(false);
      splitLine.setVisible(false);
      if (state == null) {
        return;
      }
      Board board = state.getBoard();
      Piece piece = board.getPiece(row, col);
      boolean suppressed = suppressedSquare != null
          && suppressedSquare.getRow() == row
          && suppressedSquare.getCol() == col;
      Piece displayPiece = suppressed ? null : piece;
      if (displayPiece != null) {
        pieceIcon.setVisible(true);
        applyPieceStyle(displayPiece);
      }

      if (mode == DisplayMode.MINES) {
        Minefield minefield = state.getMinefield();
        applyMineStyle(minefield);
        if (revealTransition) {
          animateReveal();
        }
        boolean exploded = minefield.isExploded(row, col);
        if (minefield.isRevealed(row, col)) {
          if (minefield.isExploded(row, col)) {
            mineLabel.setText("");
            bombOverlay.pane.setVisible(true);
          if (displayPiece != null) {
            bombOverlay.ring.setVisible(false);
            bombOverlay.pane.setScaleX(0.6);
            bombOverlay.pane.setScaleY(0.6);
            StackPane.setAlignment(bombOverlay.pane, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(bombOverlay.pane, new Insets(0, 1, 1, 0));
          } else {
            bombOverlay.ring.setVisible(false);
            bombOverlay.pane.setScaleX(1.0);
            bombOverlay.pane.setScaleY(1.0);
            StackPane.setAlignment(bombOverlay.pane, Pos.CENTER);
            StackPane.setMargin(bombOverlay.pane, Insets.EMPTY);
            bombOverlay.pane.setTranslateX(2.5);
            bombOverlay.pane.setTranslateY(0);
          }
          if (explodeTransition) {
            animateExplosion();
          }
            background.setFill(Color.web("#CFC6B9"));
          } else {
            int count = minefield.adjacentMines(row, col);
            mineLabel.setText(count == 0 ? "" : String.valueOf(count));
            mineLabel.setTextFill(mineNumberColor(count));
          }
          if (displayPiece != null && !mineLabel.getText().isEmpty()) {
            numberOverlay.ring.setVisible(false);
            numberOverlay.pane.setVisible(true);
            numberOverlay.pane.setScaleX(0.75);
            numberOverlay.pane.setScaleY(0.75);
            StackPane.setAlignment(numberOverlay.pane, Pos.TOP_LEFT);
            StackPane.setMargin(numberOverlay.pane, new Insets(2, 0, 0, 2));
          } else {
            numberOverlay.ring.setVisible(false);
            numberOverlay.pane.setVisible(!mineLabel.getText().isEmpty());
            numberOverlay.pane.setScaleX(1.0);
            numberOverlay.pane.setScaleY(1.0);
            StackPane.setAlignment(numberOverlay.pane, Pos.CENTER);
            StackPane.setMargin(numberOverlay.pane, Insets.EMPTY);
          }
        } else {
          mineLabel.setText("");
        }
        boolean showFlag = minefield.isFlagged(row, col, PlayerColor.WHITE)
            && (!minefield.isRevealed(row, col) || exploded);
        boolean showBotFlag = (showBotFlags || showBothFlags)
            && minefield.isFlagged(row, col, PlayerColor.BLACK)
            && (!minefield.isRevealed(row, col) || exploded);
        if (showFlag && showBotFlag) {
          boolean corner = displayPiece != null || exploded || minefield.isRevealed(row, col);
          if (corner) {
            showFlagOverlay(flagOverlay, FlagPosition.TOP_RIGHT, true, 0.58);
            showFlagOverlay(botFlagOverlay, FlagPosition.BOTTOM_LEFT, true, 0.58);
          } else {
            showFlagOverlay(flagOverlay, FlagPosition.BOTTOM_LEFT, true, 0.74);
            showFlagOverlay(botFlagOverlay, FlagPosition.TOP_RIGHT, true, 0.74);
            nudgeOverlayTowardCenter(flagOverlay, FlagPosition.BOTTOM_LEFT);
            nudgeOverlayTowardCenter(botFlagOverlay, FlagPosition.TOP_RIGHT);
            splitLine.setVisible(true);
          }
        } else if (showFlag) {
          boolean corner = displayPiece != null || exploded;
          FlagPosition position = corner
              ? FlagPosition.TOP_RIGHT
              : FlagPosition.CENTER;
          showFlagOverlay(flagOverlay, position, corner, corner ? 0.58 : 1.0);
        } else if (showBotFlag) {
          boolean corner = displayPiece != null || exploded;
          FlagPosition position = corner ? FlagPosition.BOTTOM_LEFT : FlagPosition.CENTER;
          showFlagOverlay(botFlagOverlay, position, corner, corner ? 0.58 : 1.0);
        }
      } else {
        boolean dark = (row + col) % 2 == 1;
        background.setFill(dark ? Color.web("#5C6F7B") : Color.web("#E5E0D8"));
        background.setStroke(null);
      }

      if (displayPiece != null && displayPiece.getType() == PieceType.KING) {
        if ((whiteKingCheck != null && whiteKingCheck.getRow() == row && whiteKingCheck.getCol() == col)
            || (blackKingCheck != null && blackKingCheck.getRow() == row && blackKingCheck.getCol() == col)) {
          background.setFill(Color.web("#B44A4A"));
        }
      }
      if (highlight != null) {
        hintBorder.setVisible(true);
        hintBorder.setStroke(highlight.capture ? Color.web("#C15A5A") : Color.web("#66A266"));
      }
    }

    private void applyPieceStyle(Piece piece) {
      pieceText.setText(pieceSymbol(piece.getType(), piece.getColor()));
      if (piece.getColor() == PlayerColor.WHITE) {
        pieceBase.setFill(Color.web("#F5F1E8"));
        pieceBase.setStroke(Color.web("#2F2F2F"));
        pieceBase.setStrokeWidth(2);
        pieceText.setTextFill(Color.web("#2F2F2F"));
      } else {
        pieceBase.setFill(Color.web("#2F2F2F"));
        pieceBase.setStroke(Color.web("#D9D2C3"));
        pieceBase.setStrokeWidth(2);
        pieceText.setTextFill(Color.web("#F5F1E8"));
      }
    }

    private String pieceSymbol(PieceType type, PlayerColor color) {
      return switch (type) {
        case KING -> color == PlayerColor.WHITE ? "\u2654" : "\u265A";
        case QUEEN -> color == PlayerColor.WHITE ? "\u2655" : "\u265B";
        case ROOK -> color == PlayerColor.WHITE ? "\u2656" : "\u265C";
        case BISHOP -> color == PlayerColor.WHITE ? "\u2657" : "\u265D";
        case KNIGHT -> color == PlayerColor.WHITE ? "\u2658" : "\u265E";
        case PAWN -> color == PlayerColor.WHITE ? "\u2659" : "\u265F";
      };
    }

    private void applyMineStyle(Minefield minefield) {
      boolean revealed = minefield.isRevealed(row, col) || minefield.isArmed(row, col);
      if (revealed) {
        background.setFill(Color.web("#DAD5CB"));
        background.setStroke(Color.web("#BFB8AA"));
        background.setStrokeWidth(1);
      } else {
        background.setFill(Color.web("#B0B0B0"));
        background.setStroke(Color.web("#8D8D8D"));
        background.setStrokeWidth(2);
      }
    }

    private Color mineNumberColor(int count) {
      return switch (count) {
        case 1 -> Color.web("#1E4FA1");
        case 2 -> Color.web("#1E7C3F");
        case 3 -> Color.web("#B12A2A");
        case 4 -> Color.web("#2B3F8C");
        case 5 -> Color.web("#8C2B2B");
        case 6 -> Color.web("#2B8C8C");
        case 7 -> Color.web("#333333");
        case 8 -> Color.web("#707070");
        default -> Color.web("#2F2F2F");
      };
    }

    private void showFlagOverlay(Overlay overlay, FlagPosition position, boolean corner, double scale) {
      overlay.pane.setVisible(true);
      overlay.ring.setVisible(false);
      if (corner) {
        overlay.pane.setScaleX(scale);
        overlay.pane.setScaleY(scale);
        applyFlagPosition(overlay, position);
      } else {
        overlay.pane.setScaleX(scale);
        overlay.pane.setScaleY(scale);
        applyFlagPosition(overlay, position);
      }
    }

    private void applyFlagPosition(Overlay overlay, FlagPosition position) {
      switch (position) {
        case TOP_RIGHT -> {
          StackPane.setAlignment(overlay.pane, Pos.TOP_RIGHT);
          StackPane.setMargin(overlay.pane, new Insets(2, 1, 0, 0));
          overlay.pane.setTranslateX(1);
          overlay.pane.setTranslateY(1);
        }
        case BOTTOM_LEFT -> {
          StackPane.setAlignment(overlay.pane, Pos.BOTTOM_LEFT);
          StackPane.setMargin(overlay.pane, new Insets(0, 0, 2, 1));
          overlay.pane.setTranslateX(1);
          overlay.pane.setTranslateY(1);
        }
        case BOTTOM_RIGHT -> {
          StackPane.setAlignment(overlay.pane, Pos.BOTTOM_RIGHT);
          StackPane.setMargin(overlay.pane, new Insets(0, 2, 2, 0));
          overlay.pane.setTranslateX(1);
          overlay.pane.setTranslateY(1);
        }
        case CENTER -> {
          StackPane.setAlignment(overlay.pane, Pos.CENTER);
          StackPane.setMargin(overlay.pane, Insets.EMPTY);
          overlay.pane.setTranslateX(3);
          overlay.pane.setTranslateY(0);
        }
      }
    }

    private void nudgeOverlayTowardCenter(Overlay overlay, FlagPosition position) {
      double nudge = 6;
      switch (position) {
        case TOP_RIGHT -> {
          overlay.pane.setTranslateX(overlay.pane.getTranslateX() - nudge);
          overlay.pane.setTranslateY(overlay.pane.getTranslateY() + nudge);
        }
        case BOTTOM_LEFT -> {
          overlay.pane.setTranslateX(overlay.pane.getTranslateX() + nudge);
          overlay.pane.setTranslateY(overlay.pane.getTranslateY() - nudge);
        }
        default -> {
        }
      }
    }

    private void setPlayerFlagColor(Color color) {
      flagShape.setFill(color);
    }

    private void animateReveal() {
      background.setOpacity(0.6);
      FadeTransition fade = new FadeTransition(Duration.millis(140), background);
      fade.setFromValue(0.6);
      fade.setToValue(1.0);
      fade.play();
    }

    private void animateExplosion() {
      explosionFlash.setOpacity(0.65);
      FadeTransition flash = new FadeTransition(Duration.millis(140), explosionFlash);
      flash.setFromValue(0.65);
      flash.setToValue(0.0);
      flash.play();
      bombOverlay.pane.setScaleX(bombOverlay.pane.getScaleX());
      bombOverlay.pane.setScaleY(bombOverlay.pane.getScaleY());
      ScaleTransition pulse = new ScaleTransition(Duration.millis(160), bombOverlay.pane);
      pulse.setFromX(bombOverlay.pane.getScaleX());
      pulse.setFromY(bombOverlay.pane.getScaleY());
      pulse.setToX(bombOverlay.pane.getScaleX() * 1.15);
      pulse.setToY(bombOverlay.pane.getScaleY() * 1.15);
      pulse.setAutoReverse(true);
      pulse.setCycleCount(2);
      pulse.play();
    }
  }

  private enum FlagPosition {
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
  }

  private Group buildFlagIcon(Polygon flag, Color flagColor) {
    Rectangle pole = new Rectangle(3, 20, Color.web("#2F2F2F"));
    flag.setFill(flagColor);
    Group group = new Group(pole, flag);
    return group;
  }

  private Group buildBombIcon() {
    Circle body = new Circle(7, Color.web("#2F2F2F"));
    Circle highlight = new Circle(2.5, Color.web("#5B5B5B"));
    highlight.setTranslateX(-2.5);
    highlight.setTranslateY(-2.5);
    Rectangle fuse = new Rectangle(6, 2, Color.web("#B87A2C"));
    fuse.setTranslateX(6);
    fuse.setTranslateY(-6);
    Circle spark = new Circle(2, Color.web("#E3C16F"));
    spark.setTranslateX(10);
    spark.setTranslateY(-7);
    Group group = new Group(body, highlight, fuse, spark);
    return group;
  }

  private Overlay buildOverlay(Node icon) {
    double radius = CELL * 0.16;
    Circle ring = new Circle(radius);
    ring.setFill(Color.TRANSPARENT);
    ring.setStroke(Color.web("#2F2F2F"));
    ring.setStrokeWidth(2);
    StackPane pane = new StackPane(ring, icon);
    pane.setMinSize(radius * 2, radius * 2);
    pane.setPrefSize(radius * 2, radius * 2);
    pane.setMaxSize(radius * 2, radius * 2);
    return new Overlay(pane, ring);
  }

  private static final class Overlay {
    private final StackPane pane;
    private final Circle ring;

    private Overlay(StackPane pane, Circle ring) {
      this.pane = pane;
      this.ring = ring;
    }
  }

  private MoveHighlight findHighlight(int row, int col) {
    if (state == null) {
      return null;
    }
    for (Move move : highlightMoves) {
      if (move.getTo().getRow() == row && move.getTo().getCol() == col) {
        boolean capture = move.isEnPassant() || state.getBoard().getPiece(row, col) != null;
        return new MoveHighlight(capture);
      }
    }
    return null;
  }

  private static final class MoveHighlight {
    private final boolean capture;

    private MoveHighlight(boolean capture) {
      this.capture = capture;
    }
  }
}

package com.hazardchess.ui;

import com.hazardchess.ai.AiDifficulty;
import com.hazardchess.ai.SimpleAi;
import com.hazardchess.model.ChessEngine;
import com.hazardchess.model.GameState;
import com.hazardchess.model.Minefield;
import com.hazardchess.model.Move;
import com.hazardchess.model.Piece;
import com.hazardchess.model.PieceType;
import com.hazardchess.model.PlayerColor;
import com.hazardchess.model.Square;
import com.hazardchess.model.io.GameStateJson;
import com.hazardchess.model.io.GameStateSnapshot;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainView extends BorderPane {
  private static final double ICON_SIZE = 24;
  private static final double HUD_HEIGHT = 42;
  private final Stage stage;
  private final ToggleButton mineToggle = new ToggleButton("");
  private final Node chessIcon = buildPawnIcon();
  private final Node mineIcon = buildBombIcon();
  private final Label statusLabel = new Label();
  private final Label timerLabel = new Label();
  private final BoardView boardView = new BoardView();
  private final Pane moveLayer = new Pane();
  private final ChessEngine engine = new ChessEngine();
  private final SimpleAi ai = new SimpleAi();
  private final GameStateJson stateJson = new GameStateJson();
  private GameState state;
  private final StackPane endOverlay = new StackPane();
  private final Label endLabel = new Label();
  private final BorderPane capturedTop = new BorderPane();
  private final BorderPane capturedBottom = new BorderPane();
  private final HBox capturedTopLeft = new HBox(4);
  private final HBox capturedTopRight = new HBox(4);
  private final HBox capturedBottomLeft = new HBox(4);
  private final HBox capturedBottomRight = new HBox(4);
  private AiDifficulty difficulty = AiDifficulty.NORMAL;
  private int mineCount = 14;
  private boolean aiEnabled = true;
  private final java.nio.file.Path autosavePath =
      java.nio.file.Paths.get(System.getProperty("user.home"), ".hazard-chess-autosave.json");
  private final Deque<GameStateSnapshot> undoStack = new ArrayDeque<>();
  private final Deque<GameStateSnapshot> redoStack = new ArrayDeque<>();
  private Button undoButton;
  private Button redoButton;
  private ToggleButton botFlagsButton;
  private Square selected;
  private List<Move> selectedMoves = new ArrayList<>();
  private long startMillis = System.currentTimeMillis();
  private final Timeline timer = new Timeline();
  private double dragOffsetX;
  private double dragOffsetY;
  private boolean gameOver;
  private boolean moveAnimating;

  public MainView(Stage stage) {
    this.stage = stage;
    setPadding(Insets.EMPTY);
    setStyle("-fx-background-color: #F2F2F2;");
    setMinWidth(boardView.getPrefWidth());
    setPrefWidth(boardView.getPrefWidth());
    setMaxWidth(boardView.getPrefWidth());

    mineToggle.setFocusTraversable(false);
    mineToggle.setStyle("-fx-font-size: 24px; -fx-background-radius: 4; -fx-min-width: 42px;"
        + " -fx-min-height: 42px; -fx-pref-width: 42px; -fx-pref-height: 42px;"
        + " -fx-background-color: linear-gradient(#F4F4F4, #CFCFCF);"
        + " -fx-border-color: #8E8E8E; -fx-border-width: 1;");
    mineToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    mineToggle.setGraphic(chessIcon);

    statusLabel.setAlignment(Pos.CENTER);
    statusLabel.setStyle("-fx-text-fill: #E03C3C;"
        + " -fx-font-family: 'Digital-7 Mono', 'DS-Digital', 'Consolas';"
        + " -fx-font-size: 26px;");
    timerLabel.setAlignment(Pos.CENTER);
    timerLabel.setStyle("-fx-text-fill: #E03C3C;"
        + " -fx-font-family: 'Digital-7 Mono', 'DS-Digital', 'Consolas';"
        + " -fx-font-size: 26px;");

    buildEndOverlay();
    moveLayer.setMouseTransparent(true);
    moveLayer.setPickOnBounds(false);
    moveLayer.setMinSize(boardView.getPrefWidth(), boardView.getPrefHeight());
    moveLayer.setPrefSize(boardView.getPrefWidth(), boardView.getPrefHeight());
    moveLayer.setMaxSize(boardView.getPrefWidth(), boardView.getPrefHeight());
    StackPane boardHolder = new StackPane(boardView, moveLayer, endOverlay);
    boardHolder.setMinSize(boardView.getPrefWidth(), boardView.getPrefHeight());
    boardHolder.setPrefSize(boardView.getPrefWidth(), boardView.getPrefHeight());
    boardHolder.setMaxSize(boardView.getPrefWidth(), boardView.getPrefHeight());

    configureCapturedBar(capturedTop, capturedTopLeft, capturedTopRight);
    configureCapturedBar(capturedBottom, capturedBottomLeft, capturedBottomRight);
    VBox boardColumn = new VBox(6, capturedTop, boardHolder, capturedBottom);
    boardColumn.setAlignment(Pos.CENTER);

    BorderPane content = new BorderPane();
    VBox topBar = new VBox(buildWindowBar(), buildHudBar());
    topBar.setMinWidth(boardView.getPrefWidth());
    topBar.setPrefWidth(boardView.getPrefWidth());
    topBar.setMaxWidth(boardView.getPrefWidth());
    content.setTop(topBar);
    content.setCenter(boardColumn);
    BorderPane.setAlignment(boardView, Pos.CENTER);
    BorderPane.setMargin(boardView, Insets.EMPTY);

    StackPane chrome = new StackPane(content);
    chrome.setStyle("-fx-background-color: #F2F2F2;");
    chrome.setPadding(Insets.EMPTY);
    setCenter(chrome);

    boardView.setCellClickHandler(this::handleCellClick);

    mineToggle.setOnAction(event -> updateMode());
    mineToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> updateToggleStyle(isSelected));
    updateBotFlagButtonStyle(false);
    botFlagsButton.setFocusTraversable(false);

    if (!loadAutosave()) {
      GameConfig config = showConfigDialog();
      if (config == null) {
        Platform.exit();
        return;
      }
      startNewGame(config);
    }
    updateUndoRedoButtons();
    startTimer();
  }

  private HBox buildWindowBar() {
    Label title = new Label("Hazard Chess");
    title.setStyle("-fx-text-fill: #E6E6E6; -fx-font-size: 12px;");

    Button newButton = new Button("New");
    Button helpButton = new Button("Guide");
    botFlagsButton = new ToggleButton("BOT FLAG: OFF");
    undoButton = new Button("Undo");
    redoButton = new Button("Redo");
    Button minimizeButton = new Button("\u2013");
    Button closeButton = new Button("\u00D7");

    String buttonStyle = "-fx-background-color: #3A3A3A; -fx-text-fill: #E6E6E6; -fx-font-size: 11px;"
        + " -fx-background-radius: 0; -fx-alignment: CENTER;";
    String squareStyle = "-fx-background-radius: 0; -fx-min-width: 36px; -fx-min-height: 24px;"
        + " -fx-pref-width: 36px; -fx-pref-height: 24px;"
        + " -fx-border-color: #4A4A4A; -fx-border-width: 1;";
    newButton.setStyle(buttonStyle);
    helpButton.setStyle(buttonStyle);
    botFlagsButton.setStyle(buttonStyle);
    botFlagsButton.setMinWidth(100);
    botFlagsButton.setPrefWidth(100);
    botFlagsButton.setMaxWidth(100);
    undoButton.setStyle(buttonStyle);
    redoButton.setStyle(buttonStyle);
    minimizeButton.setStyle(buttonStyle + squareStyle);
    closeButton.setStyle("-fx-background-color: #3A3A3A; -fx-text-fill: #FFFFFF; -fx-font-size: 11px;"
        + squareStyle);

    newButton.setOnAction(event -> {
      GameConfig config = showConfigDialog();
      if (config != null) {
        startNewGame(config);
      }
    });
    helpButton.setOnAction(event -> showGuideDialog());
    botFlagsButton.setOnAction(event -> {
      boardView.setShowBotFlags(botFlagsButton.isSelected());
      updateBotFlagButtonStyle(botFlagsButton.isSelected());
    });
    undoButton.setOnAction(event -> undo());
    redoButton.setOnAction(event -> redo());
    minimizeButton.setOnAction(event -> stage.setIconified(true));
    closeButton.setOnAction(event -> {
      if (!gameOver) {
        autosave();
      } else {
        deleteAutosave();
      }
      Platform.exit();
    });

    Pane spacer = new Pane();
    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

    botFlagsButton.setFocusTraversable(false);
    HBox bar = new HBox(8, title, spacer, newButton, helpButton, botFlagsButton,
        undoButton, redoButton, minimizeButton, closeButton);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.setPadding(new Insets(4, 8, 4, 8));
    bar.setStyle("-fx-background-color: #1F1F1F;");
    bar.setMinWidth(boardView.getPrefWidth());
    bar.setPrefWidth(boardView.getPrefWidth());
    bar.setMaxWidth(boardView.getPrefWidth());

    bar.setOnMousePressed(event -> {
      dragOffsetX = event.getSceneX();
      dragOffsetY = event.getSceneY();
    });
    bar.setOnMouseDragged(event -> {
      stage.setX(event.getScreenX() - dragOffsetX);
      stage.setY(event.getScreenY() - dragOffsetY);
    });
    stage.setOnCloseRequest(event -> autosave());
    return bar;
  }

  private void updateMode() {
    DisplayMode mode = mineToggle.isSelected() ? DisplayMode.MINES : DisplayMode.CHESS;
    mineToggle.setGraphic(mode == DisplayMode.MINES ? mineIcon : chessIcon);
    updateToggleStyle(mode == DisplayMode.MINES);
    boardView.setDisplayMode(mode);
  }

  // OS window bar is used instead of a custom bar.

  private HBox buildHudBar() {
    StackPane timerBox = new StackPane(timerLabel);
    timerBox.setAlignment(Pos.CENTER);
    StackPane statusBox = new StackPane(statusLabel);
    statusBox.setAlignment(Pos.CENTER);

    HBox hudBar = new HBox(timerBox, mineToggle, statusBox);
    hudBar.setMinHeight(HUD_HEIGHT);
    hudBar.setPrefHeight(HUD_HEIGHT);
    hudBar.setMaxHeight(HUD_HEIGHT);
    hudBar.setPrefWidth(boardView.getPrefWidth());
    hudBar.setStyle("-fx-background-color: #0F0F0F;");

    var sideWidth = Bindings.max(0, hudBar.widthProperty().subtract(mineToggle.widthProperty()).divide(2));
    timerBox.minWidthProperty().bind(sideWidth);
    timerBox.prefWidthProperty().bind(sideWidth);
    timerBox.maxWidthProperty().bind(sideWidth);
    statusBox.minWidthProperty().bind(sideWidth);
    statusBox.prefWidthProperty().bind(sideWidth);
    statusBox.maxWidthProperty().bind(sideWidth);
    return hudBar;
  }

  private GameConfig showConfigDialog() {
    Dialog<GameConfig> dialog = new Dialog<>();
    dialog.setTitle("New Game");
    dialog.setHeaderText("Game Settings");
    dialog.getDialogPane().setStyle(
        "-fx-header-panel .label { -fx-alignment: center; -fx-max-width: Infinity; }");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    ChoiceBox<AiDifficulty> difficultyBox = new ChoiceBox<>();
    difficultyBox.getItems().addAll(AiDifficulty.EASY, AiDifficulty.NORMAL, AiDifficulty.HARD);
    difficultyBox.setValue(AiDifficulty.NORMAL);

    ChoiceBox<String> modeBox = new ChoiceBox<>();
    modeBox.getItems().addAll("BOT (default)", "Two Players (Debug)");
    modeBox.setValue("BOT (default)");

    ChoiceBox<Integer> minesBox = new ChoiceBox<>();
    minesBox.getItems().addAll(10, 14, 18);
    minesBox.setValue(14);
    minesBox.prefWidthProperty().bind(modeBox.widthProperty());
    difficultyBox.prefWidthProperty().bind(modeBox.widthProperty());
    difficultyBox.setDisable(false);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10));
    Label difficultyLabel = new Label("BOT Difficulty");

    grid.add(new Label("Mode"), 0, 0);
    grid.add(modeBox, 1, 0);
    grid.add(new Label("Mine Count"), 0, 1);
    grid.add(minesBox, 1, 1);
    grid.add(difficultyLabel, 0, 2);
    grid.add(difficultyBox, 1, 2);
    dialog.getDialogPane().setContent(grid);

    modeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
      boolean botEnabled = "BOT (default)".equals(newValue);
      difficultyBox.setDisable(!botEnabled);
      difficultyBox.setOpacity(botEnabled ? 1.0 : 0.5);
      difficultyLabel.setOpacity(botEnabled ? 1.0 : 0.5);
    });

    dialog.setResultConverter(button -> {
      if (button == ButtonType.OK) {
        boolean botEnabled = "BOT (default)".equals(modeBox.getValue());
        return new GameConfig(difficultyBox.getValue(), minesBox.getValue(), botEnabled);
      }
      return null;
    });
    return dialog.showAndWait().orElse(null);
  }

  private void startNewGame(GameConfig config) {
    difficulty = config.difficulty;
    mineCount = config.mineCount;
    aiEnabled = config.botEnabled;
    state = new GameState(mineCount, new java.util.Random());
    state.setElapsedSeconds(0);
    state.setBotEnabled(aiEnabled);
    gameOver = false;
    botFlagsButton.setDisable(!aiEnabled);
    if (!aiEnabled) {
      botFlagsButton.setSelected(false);
      boardView.setShowBotFlags(false);
      updateBotFlagButtonStyle(false);
    }
    boardView.setGameState(state);
    updateFlagOwner();
    clearSelection();
    updateMode();
    updateStatus();
    updateCapturedPanels();
    resetTimer();
    undoStack.clear();
    redoStack.clear();
    updateUndoRedoButtons();
    autosave();
  }
  private boolean loadAutosave() {
    try {
      if (!java.nio.file.Files.exists(autosavePath)) {
        return false;
      }
      state = stateJson.load(autosavePath);
      aiEnabled = state.isBotEnabled();
      botFlagsButton.setDisable(!aiEnabled);
      if (!aiEnabled) {
        botFlagsButton.setSelected(false);
        boardView.setShowBotFlags(false);
        updateBotFlagButtonStyle(false);
      }
      boardView.setGameState(state);
      updateFlagOwner();
      clearSelection();
      updateMode();
      updateStatus();
      updateCapturedPanels();
      syncTimerFromState();
      runAiIfNeeded();
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private void autosave() {
    if (state == null) {
      return;
    }
    syncStateElapsedSeconds();
    try {
      stateJson.save(state, autosavePath);
    } catch (Exception ignored) {
      // ignore autosave failure
    }
  }

  private void deleteAutosave() {
    try {
      java.nio.file.Files.deleteIfExists(autosavePath);
    } catch (Exception ignored) {
      // ignore delete failure
    }
  }

  private Node buildPawnIcon() {
    SVGPath path = new SVGPath();
    path.setContent("M12 3.5c2.1 0 3.8 1.7 3.8 3.8 0 1.4-.8 2.7-2.1 3.3l2.1 4H8.2l2.1-4c-1.3-.6-2.1-1.9-2.1-3.3 0-2.1 1.7-3.8 3.8-3.8zm-5.8 16h11.6l1.4 3H4.8l1.4-3z");
    path.setFill(Color.web("#1F1F1F"));
    double scale = ICON_SIZE / 24.0;
    path.setScaleX(scale);
    path.setScaleY(scale);
    StackPane pane = new StackPane(path);
    pane.setMinSize(ICON_SIZE, ICON_SIZE);
    pane.setPrefSize(ICON_SIZE, ICON_SIZE);
    pane.setMaxSize(ICON_SIZE, ICON_SIZE);
    return pane;
  }

  private Node buildBombIcon() {
    Pane pane = fixedIconPane();
    Circle body = new Circle(7, Color.web("#1F1F1F"));
    body.setCenterX(ICON_SIZE / 2);
    body.setCenterY(14);
    Rectangle fuse = new Rectangle(7, 2, Color.web("#B87A2C"));
    fuse.setX(ICON_SIZE / 2 + 3);
    fuse.setY(6);
    Circle spark = new Circle(2.5, Color.web("#E3C16F"));
    spark.setCenterX(ICON_SIZE / 2 + 9);
    spark.setCenterY(5);
    pane.getChildren().addAll(body, fuse, spark);
    return pane;
  }

  private Pane fixedIconPane() {
    Pane pane = new Pane();
    pane.setMinSize(ICON_SIZE, ICON_SIZE);
    pane.setPrefSize(ICON_SIZE, ICON_SIZE);
    pane.setMaxSize(ICON_SIZE, ICON_SIZE);
    return pane;
  }

  private void updateToggleStyle(boolean pressed) {
    String base = "-fx-font-size: 24px; -fx-background-radius: 4; -fx-min-width: 42px;"
        + " -fx-min-height: 42px; -fx-pref-width: 42px; -fx-pref-height: 42px;"
        + " -fx-border-color: #8E8E8E; -fx-border-width: 1;";
    if (pressed) {
      mineToggle.setStyle(base + " -fx-background-color: linear-gradient(#BFBFBF, #E4E4E4);"
          + " -fx-effect: innershadow(two-pass-box, rgba(0,0,0,0.35), 4, 0.0, 0, 1);");
    } else {
      mineToggle.setStyle(base + " -fx-background-color: linear-gradient(#F4F4F4, #CFCFCF);");
    }
  }

  private void updateBotFlagButtonStyle(boolean enabled) {
    String base = "-fx-background-color: #3A3A3A; -fx-text-fill: #E6E6E6; -fx-font-size: 11px;"
        + " -fx-background-radius: 0; -fx-alignment: CENTER;"
        + " -fx-focus-color: transparent; -fx-faint-focus-color: transparent;"
        + " -fx-background-insets: 0; -fx-border-color: transparent;";
    if (enabled) {
      botFlagsButton.setText("BOT FLAG: ON");
      botFlagsButton.setStyle(base + " -fx-background-color: #2E2E2E;");
    } else {
      botFlagsButton.setText("BOT FLAG: OFF");
      botFlagsButton.setStyle(base);
    }
  }


  private void handleCellClick(int row, int col, boolean rightClick) {
    if (state == null) {
      return;
    }
    if (gameOver) {
      return;
    }
    if (moveAnimating) {
      return;
    }
    if (aiEnabled && state.getToMove() == PlayerColor.BLACK) {
      return;
    }
    if (rightClick) {
      handleFlagToggle(row, col);
      return;
    }
    Square clicked = new Square(row, col);
    Piece piece = state.getBoard().getPiece(row, col);
    if (selected == null) {
      if (piece != null && piece.getColor() == state.getToMove()) {
        selectSquare(clicked);
      }
      return;
    }

    if (selected.getRow() == row && selected.getCol() == col) {
      clearSelection();
      return;
    }

    Move move = resolveMove(clicked);
    if (move == null) {
      if (piece != null && piece.getColor() == state.getToMove()) {
        selectSquare(clicked);
      }
      return;
    }

    Piece movingPiece = state.getBoard().getPiece(move.getFrom().getRow(), move.getFrom().getCol());
    pushUndoSnapshot();
    state.applyMove(move);
    redoStack.clear();
    clearSelection();
    animateMove(move, movingPiece, () -> {
      updateStatus();
      updateCapturedPanels();
      autosave();
      updateUndoRedoButtons();
      runAiIfNeeded();
    });
  }

  private void handleFlagToggle(int row, int col) {
    if (mineToggle.isSelected() == false) {
      return;
    }
    if (state == null) {
      return;
    }
    if (gameOver) {
      return;
    }
    pushUndoSnapshot();
    redoStack.clear();
    Minefield minefield = state.getMinefield();
    if (minefield.isRevealed(row, col) && !minefield.isExploded(row, col)) {
      return;
    }
    PlayerColor owner = aiEnabled ? PlayerColor.WHITE : state.getToMove();
    minefield.toggleFlag(row, col, owner);
    boardView.refresh();
    autosave();
    updateUndoRedoButtons();
  }

  private void selectSquare(Square square) {
    selected = square;
    selectedMoves = engine.legalMoves(state, square);
    boardView.setSelected(square);
    boardView.setHighlightMoves(selectedMoves);
  }

  private void clearSelection() {
    selected = null;
    selectedMoves = List.of();
    boardView.setSelected(null);
    boardView.setHighlightMoves(selectedMoves);
  }

  private Move resolveMove(Square target) {
    List<Move> candidates = new ArrayList<>();
    for (Move move : selectedMoves) {
      if (move.getTo().equals(target)) {
        candidates.add(move);
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    if (candidates.size() == 1) {
      return candidates.get(0);
    }
    return choosePromotion(candidates);
  }

  private Move choosePromotion(List<Move> candidates) {
    List<PieceType> options = List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT);
    ChoiceDialog<PieceType> dialog = new ChoiceDialog<>(PieceType.QUEEN, options);
    dialog.setHeaderText("Promotion");
    dialog.setContentText("Select piece:");
    Optional<PieceType> choice = dialog.showAndWait();
    if (choice.isEmpty()) {
      return null;
    }
    for (Move move : candidates) {
      if (move.getPromotion() == choice.get()) {
        return move;
      }
    }
    return candidates.get(0);
  }

  private void updateStatus() {
    if (state == null) {
      statusLabel.setText("");
      return;
    }
    updateFlagOwner();
    if (state.getBoard().findKing(PlayerColor.BLACK) == null) {
      statusLabel.setText("");
      handleGameOver(PlayerColor.WHITE);
      return;
    }
    if (state.getBoard().findKing(PlayerColor.WHITE) == null) {
      statusLabel.setText("");
      handleGameOver(PlayerColor.BLACK);
      return;
    }
    PlayerColor toMove = state.getToMove();
    String turn = toMove == PlayerColor.WHITE ? "White" : "Black";
    if (engine.isCheckmate(state, toMove)) {
      PlayerColor winner = toMove == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
      statusLabel.setText("");
      handleGameOver(winner);
      return;
    }
    if (state.isDraw(engine)) {
      statusLabel.setText("");
      handleDraw();
      return;
    }
    gameOver = false;
    updateEndOverlay(null);
    if (engine.isInCheck(state, toMove)) {
      statusLabel.setText("Check!");
      return;
    }
    statusLabel.setText(turn + " turn");
  }

  private void updateFlagOwner() {
    if (state == null) {
      return;
    }
    boardView.setActiveFlagOwner(PlayerColor.WHITE);
    boardView.setShowBothFlags(!aiEnabled);
  }

  private void buildEndOverlay() {
    endLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 36px; -fx-font-weight: bold;");
    StackPane backdrop = new StackPane();
    backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
    backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    endOverlay.getChildren().setAll(backdrop, endLabel);
    endOverlay.setAlignment(Pos.CENTER);
    endOverlay.setPickOnBounds(true);
    endOverlay.setVisible(false);
  }

  private void updateEndOverlay(PlayerColor winner) {
    if (winner == null) {
      endOverlay.setVisible(false);
      return;
    }
    boolean playerWon = winner == PlayerColor.WHITE;
    endLabel.setText(playerWon ? "YOU WIN" : "YOU LOSE");
    endOverlay.setVisible(true);
  }

  private void showGuideDialog() {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("How to Play");
    dialog.setHeaderText("Game Guide");
    dialog.getDialogPane().setStyle(
        "-fx-header-panel .label { -fx-alignment: center; -fx-max-width: Infinity; }");
    dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(12);
    grid.setPadding(new Insets(12));

    int row = 0;
    grid.add(buildGuideSection("Chess Rules",
        "Standard rules apply: check, checkmate, castling, en passant, promotion.",
        "Draw rules: stalemate, threefold repetition, 50-move rule, insufficient material."),
        0, row++);
    grid.add(buildGuideSection("Mines and Timing",
        "Every move opens a minesweeper cell.",
        "Mines can exist under pieces at the start.",
        "Stepping onto a mine arms it; it explodes on the next turn if the piece is still there.",
        "If the opponent captures a piece standing on an armed mine, both pieces explode.",
        "Exploded mine cells are safe to enter."),
        0, row++);
    grid.add(buildGuideSection("Flags and Quick Open",
        "Right click in Minesweeper view to place/remove a flag on any hidden cell.",
        "You cannot move onto your own flagged hidden cell unless you remove the flag.",
        "Quick open: move onto a revealed number when surrounding flags match the number.",
        "Wrong flags during quick open can destroy your moving piece."),
        0, row++);
    grid.add(buildGuideSection("Views and Controls",
        "Chess view hides minesweeper numbers and bombs.",
        "Minesweeper view shows numbers, flags, and revealed bombs.",
        "Left click moves a piece. Right click toggles a flag (Minesweeper view only)."),
        0, row);

    dialog.getDialogPane().setContent(grid);
    dialog.showAndWait();
  }

  private VBox buildGuideSection(String heading, String... lines) {
    Label title = new Label(heading);
    title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E2E2E;");

    StringBuilder bodyText = new StringBuilder();
    for (String line : lines) {
      bodyText.append("- ").append(line).append("\n");
    }
    Label body = new Label(bodyText.toString().trim());
    body.setWrapText(true);
    body.setStyle("-fx-text-fill: #3A3A3A;");

    VBox box = new VBox(4, title, body);
    box.setPadding(new Insets(6, 2, 6, 2));
    return box;
  }

  private void runAiIfNeeded() {
    if (!aiEnabled || state == null) {
      return;
    }
    if (gameOver) {
      return;
    }
    if (moveAnimating) {
      return;
    }
    if (state.getToMove() != PlayerColor.BLACK) {
      return;
    }
    if (engine.isCheckmate(state, PlayerColor.BLACK)) {
      return;
    }
    PauseTransition pause = new PauseTransition(Duration.millis(aiDelayMillis()));
    pause.setOnFinished(event -> {
      if (state == null || state.getToMove() != PlayerColor.BLACK) {
        return;
      }
      if (gameOver) {
        return;
      }
      boolean flagged = ai.placeFlags(state, difficulty);
      if (flagged) {
        boardView.refresh();
        autosave();
      }
      Move aiMove = ai.chooseMove(state, difficulty);
      if (aiMove == null) {
        return;
      }
      Piece movingPiece = state.getBoard().getPiece(aiMove.getFrom().getRow(), aiMove.getFrom().getCol());
      pushUndoSnapshot();
      redoStack.clear();
      state.applyMove(aiMove);
      animateMove(aiMove, movingPiece, () -> {
        updateStatus();
        updateCapturedPanels();
        autosave();
        updateUndoRedoButtons();
      });
    });
    pause.play();
  }

  private void startTimer() {
    timer.getKeyFrames().setAll(new KeyFrame(Duration.seconds(1), event -> updateTimer()));
    timer.setCycleCount(Timeline.INDEFINITE);
    if (state != null && state.getElapsedSeconds() > 0) {
      syncTimerFromState();
    } else {
      resetTimer();
    }
    timer.play();
  }

  private void resetTimer() {
    startMillis = System.currentTimeMillis();
    if (state != null) {
      state.setElapsedSeconds(0);
    }
    updateTimer();
    if (timer.getStatus() != Timeline.Status.RUNNING) {
      timer.play();
    }
  }

  private void updateTimer() {
    long elapsed = (System.currentTimeMillis() - startMillis) / 1000;
    long minutes = elapsed / 60;
    long seconds = elapsed % 60;
    timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
  }

  private void animateMove(Move move, Piece movingPiece, Runnable onFinished) {
    if (movingPiece == null) {
      boardView.refresh();
      if (onFinished != null) {
        onFinished.run();
      }
      return;
    }
    Point2D startScene = boardView.getCellCenterInScene(move.getFrom().getRow(), move.getFrom().getCol());
    Point2D endScene = boardView.getCellCenterInScene(move.getTo().getRow(), move.getTo().getCol());
    Point2D start = moveLayer.sceneToLocal(startScene);
    Point2D end = moveLayer.sceneToLocal(endScene);
    Node floating = buildFloatingPiece(movingPiece);
    double size = 36;
    floating.setLayoutX(start.getX() - size / 2.0);
    floating.setLayoutY(start.getY() - size / 2.0);
    moveLayer.getChildren().add(floating);
    boardView.suppressPieceAt(move.getTo());
    moveAnimating = true;

    TranslateTransition transition = new TranslateTransition(Duration.millis(220), floating);
    transition.setToX(end.getX() - start.getX());
    transition.setToY(end.getY() - start.getY());
    transition.setOnFinished(event -> {
      moveLayer.getChildren().remove(floating);
      boardView.clearSuppressedPiece();
      boardView.refresh();
      moveAnimating = false;
      if (onFinished != null) {
        onFinished.run();
      }
    });
    transition.play();
  }

  private Node buildFloatingPiece(Piece piece) {
    double radius = 16;
    Circle base = new Circle(radius);
    Label icon = new Label(pieceSymbol(piece.getType(), piece.getColor()));
    icon.setStyle("-fx-font-size: 20px;");
    if (piece.getColor() == PlayerColor.WHITE) {
      base.setFill(Color.web("#F5F1E8"));
      base.setStroke(Color.web("#2F2F2F"));
      base.setStrokeWidth(2);
      icon.setTextFill(Color.web("#2F2F2F"));
    } else {
      base.setFill(Color.web("#2F2F2F"));
      base.setStroke(Color.web("#D9D2C3"));
      base.setStrokeWidth(2);
      icon.setTextFill(Color.web("#F5F1E8"));
    }
    StackPane pane = new StackPane(base, icon);
    pane.setPrefSize(radius * 2, radius * 2);
    pane.setMinSize(radius * 2, radius * 2);
    pane.setMaxSize(radius * 2, radius * 2);
    return pane;
  }

  private void pushUndoSnapshot() {
    if (state == null) {
      return;
    }
    undoStack.push(GameStateSnapshot.from(state));
  }

  private void undo() {
    if (state == null || undoStack.isEmpty()) {
      return;
    }
    redoStack.push(GameStateSnapshot.from(state));
    GameStateSnapshot snapshot = undoStack.pop();
    state = snapshot.toGameState();
    boardView.setGameState(state);
    clearSelection();
    updateMode();
    updateStatus();
    updateCapturedPanels();
    syncTimerFromState();
    autosave();
    updateUndoRedoButtons();
  }

  private void redo() {
    if (state == null || redoStack.isEmpty()) {
      return;
    }
    undoStack.push(GameStateSnapshot.from(state));
    GameStateSnapshot snapshot = redoStack.pop();
    state = snapshot.toGameState();
    boardView.setGameState(state);
    clearSelection();
    updateMode();
    updateStatus();
    updateCapturedPanels();
    syncTimerFromState();
    autosave();
    updateUndoRedoButtons();
  }

  private void updateUndoRedoButtons() {
    if (undoButton != null) {
      undoButton.setDisable(undoStack.isEmpty());
    }
    if (redoButton != null) {
      redoButton.setDisable(redoStack.isEmpty());
    }
  }

  private void syncStateElapsedSeconds() {
    if (state == null) {
      return;
    }
    long elapsed = (System.currentTimeMillis() - startMillis) / 1000;
    state.setElapsedSeconds(elapsed);
  }

  private void syncTimerFromState() {
    if (state == null) {
      return;
    }
    startMillis = System.currentTimeMillis() - (state.getElapsedSeconds() * 1000);
    updateTimer();
    if (timer.getStatus() != Timeline.Status.RUNNING) {
      timer.play();
    }
  }

  private void handleGameOver(PlayerColor winner) {
    gameOver = true;
    updateEndOverlay(winner);
    timer.stop();
    deleteAutosave();
  }

  private void handleDraw() {
    gameOver = true;
    endLabel.setText("DRAW");
    endOverlay.setVisible(true);
    timer.stop();
    deleteAutosave();
  }

  private void configureCapturedBar(BorderPane bar, HBox left, HBox right) {
    left.setAlignment(Pos.CENTER_LEFT);
    right.setAlignment(Pos.CENTER_RIGHT);
    left.setSpacing(6);
    right.setSpacing(6);
    bar.setPadding(new Insets(3, 10, 3, 10));
    bar.setMinHeight(28);
    bar.setPrefHeight(28);
    bar.setMaxHeight(28);
    bar.setMinWidth(boardView.getPrefWidth());
    bar.setPrefWidth(boardView.getPrefWidth());
    bar.setMaxWidth(boardView.getPrefWidth());
    bar.setStyle("-fx-background-color: #ECE7DD;"
        + " -fx-border-color: #C9C2B4; -fx-border-width: 1;"
        + " -fx-effect: innershadow(two-pass-box, rgba(0,0,0,0.15), 4, 0.0, 0, 1);");
    bar.setLeft(left);
    bar.setRight(right);
    Rectangle divider = new Rectangle(1, 18, Color.web("#BEB6A6"));
    bar.setCenter(divider);
    BorderPane.setAlignment(divider, Pos.CENTER);
  }

  private void updateCapturedPanels() {
    if (state == null) {
      capturedTopLeft.getChildren().clear();
      capturedTopRight.getChildren().clear();
      capturedBottomLeft.getChildren().clear();
      capturedBottomRight.getChildren().clear();
      return;
    }
    capturedTopLeft.getChildren().setAll(buildCapturedIcons(state.getMinedBlack()));
    capturedTopRight.getChildren().setAll(buildCapturedIcons(state.getCapturedBlack()));
    capturedBottomLeft.getChildren().setAll(buildCapturedIcons(state.getCapturedWhite()));
    capturedBottomRight.getChildren().setAll(buildCapturedIcons(state.getMinedWhite()));
  }

  private List<Node> buildCapturedIcons(List<Piece> pieces) {
    List<Node> nodes = new ArrayList<>();
    if (pieces == null) {
      return nodes;
    }
    for (Piece piece : pieces) {
      nodes.add(buildCapturedIcon(piece));
    }
    return nodes;
  }

  private Node buildCapturedIcon(Piece piece) {
    double radius = 11;
    Circle base = new Circle(radius);
    Label icon = new Label(pieceSymbol(piece.getType(), piece.getColor()));
    icon.setStyle("-fx-font-size: 14px;");
    if (piece.getColor() == PlayerColor.WHITE) {
      base.setFill(Color.web("#F5F1E8"));
      base.setStroke(Color.web("#2F2F2F"));
      base.setStrokeWidth(1.2);
      icon.setTextFill(Color.web("#2F2F2F"));
    } else {
      base.setFill(Color.web("#2F2F2F"));
      base.setStroke(Color.web("#D9D2C3"));
      base.setStrokeWidth(1.2);
      icon.setTextFill(Color.web("#F5F1E8"));
    }
    StackPane pane = new StackPane(base, icon);
    pane.setMinSize(radius * 2, radius * 2);
    pane.setPrefSize(radius * 2, radius * 2);
    pane.setMaxSize(radius * 2, radius * 2);
    return pane;
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

  private int aiDelayMillis() {
    return switch (difficulty) {
      case EASY -> 400;
      case NORMAL -> 600;
      case HARD -> 900;
    };
  }

  private static final class GameConfig {
    private final AiDifficulty difficulty;
    private final int mineCount;
    private final boolean botEnabled;

    private GameConfig(AiDifficulty difficulty, int mineCount, boolean botEnabled) {
      this.difficulty = difficulty;
      this.mineCount = mineCount;
      this.botEnabled = botEnabled;
    }
  }
}

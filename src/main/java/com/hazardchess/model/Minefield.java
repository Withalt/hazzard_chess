package com.hazardchess.model;

public final class Minefield {
  public static final int SIZE = 8;
  private final boolean[][] mines = new boolean[SIZE][SIZE];
  private final boolean[][] revealed = new boolean[SIZE][SIZE];
  private final boolean[][] exploded = new boolean[SIZE][SIZE];
  private final boolean[][] flaggedWhite = new boolean[SIZE][SIZE];
  private final boolean[][] flaggedBlack = new boolean[SIZE][SIZE];
  private final int[][] adjacentCounts = new int[SIZE][SIZE];
  private final int[][] armedTurns = new int[SIZE][SIZE];

  public void generate(int mineCount, java.util.Random random, boolean[][] reserved) {
    clear();
    int placed = 0;
    while (placed < mineCount) {
      int row = random.nextInt(SIZE);
      int col = random.nextInt(SIZE);
      if (mines[row][col]) {
        continue;
      }
      if (reserved != null && reserved[row][col]) {
        continue;
      }
      mines[row][col] = true;
      placed++;
    }
    computeAdjacentCounts();
  }

  public boolean hasMine(int row, int col) {
    return mines[row][col];
  }

  public boolean isRevealed(int row, int col) {
    return revealed[row][col];
  }

  public boolean isExploded(int row, int col) {
    return exploded[row][col];
  }

  public int adjacentMines(int row, int col) {
    return adjacentCounts[row][col];
  }

  public void reveal(int row, int col) {
    revealed[row][col] = true;
    flaggedWhite[row][col] = false;
    flaggedBlack[row][col] = false;
    armedTurns[row][col] = 0;
  }

  public void explode(int row, int col) {
    exploded[row][col] = true;
    revealed[row][col] = true;
    flaggedWhite[row][col] = false;
    flaggedBlack[row][col] = false;
    armedTurns[row][col] = 0;
  }

  public boolean isFlagged(int row, int col, PlayerColor color) {
    return color == PlayerColor.WHITE ? flaggedWhite[row][col] : flaggedBlack[row][col];
  }

  public boolean isFlaggedAny(int row, int col) {
    return flaggedWhite[row][col] || flaggedBlack[row][col];
  }

  public boolean isArmed(int row, int col) {
    return armedTurns[row][col] > 0;
  }

  public int getArmedTurns(int row, int col) {
    return armedTurns[row][col];
  }

  public void armMine(int row, int col) {
    if (!mines[row][col] || exploded[row][col]) {
      return;
    }
    if (armedTurns[row][col] == 0) {
      armedTurns[row][col] = 2;
    }
  }

  public void tickArmed() {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        if (armedTurns[row][col] > 0) {
          armedTurns[row][col]--;
        }
      }
    }
  }

  public void toggleFlag(int row, int col, PlayerColor color) {
    if (revealed[row][col] && !exploded[row][col]) {
      return;
    }
    if (color == PlayerColor.WHITE) {
      flaggedWhite[row][col] = !flaggedWhite[row][col];
    } else {
      flaggedBlack[row][col] = !flaggedBlack[row][col];
    }
  }

  public int countFlagsAround(int row, int col, PlayerColor color) {
    int count = 0;
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (r >= 0 && r < SIZE && c >= 0 && c < SIZE && isFlagged(r, c, color)) {
          count++;
        }
      }
    }
    return count;
  }

  public void revealFlood(int startRow, int startCol, PlayerColor color) {
    if (!isInside(startRow, startCol) || revealed[startRow][startCol] || mines[startRow][startCol]) {
      return;
    }
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    queue.add(new int[]{startRow, startCol});
    while (!queue.isEmpty()) {
      int[] cell = queue.removeFirst();
      int row = cell[0];
      int col = cell[1];
      boolean isStart = row == startRow && col == startCol;
      if (revealed[row][col] || mines[row][col] || (!isStart && isFlagged(row, col, color))) {
        continue;
      }
      reveal(row, col);
      if (adjacentCounts[row][col] != 0) {
        continue;
      }
      for (int dr = -1; dr <= 1; dr++) {
        for (int dc = -1; dc <= 1; dc++) {
          if (dr == 0 && dc == 0) {
            continue;
          }
          int r = row + dr;
          int c = col + dc;
          if (isInside(r, c) && !revealed[r][c] && !mines[r][c] && !isFlagged(r, c, color)) {
            queue.add(new int[]{r, c});
          }
        }
      }
    }
  }

  private boolean isInside(int row, int col) {
    return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
  }

  public boolean[][] copyMines() {
    return copy(mines);
  }

  public boolean[][] copyRevealed() {
    return copy(revealed);
  }

  public boolean[][] copyExploded() {
    return copy(exploded);
  }

  public boolean[][] copyFlaggedWhite() {
    return copy(flaggedWhite);
  }

  public boolean[][] copyFlaggedBlack() {
    return copy(flaggedBlack);
  }

  public int[][] copyArmedTurns() {
    int[][] clone = new int[SIZE][SIZE];
    for (int row = 0; row < SIZE; row++) {
      System.arraycopy(armedTurns[row], 0, clone[row], 0, SIZE);
    }
    return clone;
  }

  public void setState(boolean[][] mines, boolean[][] revealed, boolean[][] exploded,
                       boolean[][] flaggedWhite, boolean[][] flaggedBlack, int[][] armedTurns) {
    if (!isValid(mines) || !isValid(revealed) || !isValid(exploded)
        || !isValid(flaggedWhite) || !isValid(flaggedBlack) || !isValid(armedTurns)) {
      return;
    }
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        this.mines[row][col] = mines[row][col];
        this.revealed[row][col] = revealed[row][col];
        this.exploded[row][col] = exploded[row][col];
        this.flaggedWhite[row][col] = flaggedWhite[row][col];
        this.flaggedBlack[row][col] = flaggedBlack[row][col];
        this.armedTurns[row][col] = armedTurns[row][col];
      }
    }
    computeAdjacentCounts();
  }

  private boolean[][] copy(boolean[][] source) {
    boolean[][] clone = new boolean[SIZE][SIZE];
    for (int row = 0; row < SIZE; row++) {
      System.arraycopy(source[row], 0, clone[row], 0, SIZE);
    }
    return clone;
  }

  private boolean isValid(boolean[][] source) {
    if (source == null || source.length < SIZE) {
      return false;
    }
    for (int row = 0; row < SIZE; row++) {
      if (source[row] == null || source[row].length < SIZE) {
        return false;
      }
    }
    return true;
  }

  private boolean isValid(int[][] source) {
    if (source == null || source.length < SIZE) {
      return false;
    }
    for (int row = 0; row < SIZE; row++) {
      if (source[row] == null || source[row].length < SIZE) {
        return false;
      }
    }
    return true;
  }

  private void computeAdjacentCounts() {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        adjacentCounts[row][col] = countAdjacent(row, col);
      }
    }
  }

  private int countAdjacent(int row, int col) {
    int count = 0;
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (r >= 0 && r < SIZE && c >= 0 && c < SIZE && mines[r][c]) {
          count++;
        }
      }
    }
    return count;
  }

  private void clear() {
    for (int row = 0; row < SIZE; row++) {
      for (int col = 0; col < SIZE; col++) {
        mines[row][col] = false;
        revealed[row][col] = false;
        exploded[row][col] = false;
        flaggedWhite[row][col] = false;
        flaggedBlack[row][col] = false;
        armedTurns[row][col] = 0;
        adjacentCounts[row][col] = 0;
      }
    }
  }
}

# ♟️Hazard Chess💣

Hazard Chess is a chess variant that incorporates minesweeper elements. In this game, you must think strategically and calculate to avoid losses from hidden mines on the board.

### 🕹️ How to Play

The game follows basic chess rules, but adds a new minesweeper mechanism. Starting with a regular chessboard display, players can switch between chessboard and minesweeper modes using an icon button.

**⚙️ Game Mechanism:**

- Each move of a chess piece is equivalent to opening a hidden square in the minesweeper game.

- You can place a flag (using the right mouse button) to mark the location of suspected hidden squares containing bombs. Chess pieces can be placed on squares containing chess pieces, but only on squares where the piece is currently hidden; they cannot be placed on squares that are already open (except for squares where a bomb has exploded).
- The chess placement function only works when the Minesweeper board display is enabled.
- When enough chess pieces are placed around a numbered square (an open square), you can move a piece to that square to quickly open it. If the number of chess pieces does not match the number on the square, this feature will not work.

**📜 Rules:**

- A piece moved into a mined square will be in a mined state (it will explode on the next turn).

- A piece will be immediately destroyed if it opens a square but the wrong piece is placed.

- If an opponent captures a piece standing on a mined square, the bomb will explode and both pieces will be eliminated.

- A square with an exploded bomb is safe; pieces can be moved into it without being affected.

**Display Note:** The basic chess board will not display numbered squares. Only when switching to Minesweeper mode will you see the numbered squares indicating the number of bombs around you.

### ⭐ Key Features

- **Tactical:** You can use low-value pieces to scout the terrain before moving important pieces.

- **Unexpected:** Mine locations are randomly generated each game; no two games are the same (probably :)).

- **Challenge:** Requires players to be able to play both chess and minesweeper.

**Victory is achieved when you checkmate your opponent's king, just like traditional chess. But be careful because the board is full of mines. Even if all the mines on the board explode, the game continues until a winner is found.**
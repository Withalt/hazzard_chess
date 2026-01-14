package com.hazardchess.model.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazardchess.model.GameState;
import java.io.IOException;
import java.nio.file.Path;

public final class GameStateJson {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public void save(GameState state, Path path) throws IOException {
    GameStateSnapshot snapshot = GameStateSnapshot.from(state);
    MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), snapshot);
  }

  public GameState load(Path path) throws IOException {
    GameStateSnapshot snapshot = MAPPER.readValue(path.toFile(), GameStateSnapshot.class);
    return snapshot.toGameState();
  }
}

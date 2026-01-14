package com.hazardchess;

import com.hazardchess.ui.MainView;
import javafx.application.Application;
import java.io.InputStream;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {
  @Override
  public void start(Stage stage) {
    loadFonts();
    stage.initStyle(StageStyle.UNDECORATED);
    MainView root = new MainView(stage);
    Scene scene = new Scene(root);
    scene.setFill(Color.web("#F2F2F2"));

    stage.setTitle("Hazard Chess");
    stage.setScene(scene);
    stage.sizeToScene();
    stage.setResizable(false);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }

  private void loadFonts() {
    loadFont("/fonts/digital-7.ttf");
    loadFont("/fonts/digital-7 (mono).ttf");
  }

  private void loadFont(String resource) {
    try (InputStream stream = App.class.getResourceAsStream(resource)) {
      if (stream != null) {
        Font.loadFont(stream, 16);
      }
    } catch (Exception ignored) {
      // ignore font load failure
    }
  }
}

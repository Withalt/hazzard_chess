module com.hazardchess {
  requires javafx.controls;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.annotation;

  exports com.hazardchess;
  exports com.hazardchess.ai;
  exports com.hazardchess.ui;
  exports com.hazardchess.model;
  exports com.hazardchess.model.io;
}

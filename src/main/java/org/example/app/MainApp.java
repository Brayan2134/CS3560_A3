package org.example.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.app.controller.WritingController;
import org.example.app.model.WritingModel;
import org.example.app.suggest.CompositeSuggestionEngine;
import org.example.app.suggest.NoopSuggestionEngine;
import org.example.app.view.WritingView;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        WritingModel model = new WritingModel();
        WritingView view = new WritingView();

        new WritingController(model, view);

        var engine = new CompositeSuggestionEngine(new NoopSuggestionEngine());

        stage.setTitle("Intelligent Writing Assistant");
        stage.setScene(new Scene(view.getRoot(), 1920, 1080));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
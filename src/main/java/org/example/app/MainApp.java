package org.example.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.app.controller.WritingController;
import org.example.app.model.WritingModel;
import org.example.app.view.WritingView;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        WritingModel model = new WritingModel();
        WritingView view = new WritingView();
        new WritingController(model, view);

        stage.setTitle("Intelligent Writing Assistant");
        stage.setScene(new Scene(view.getRoot(), 1000, 600));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
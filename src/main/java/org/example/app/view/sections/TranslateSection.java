package org.example.app.view.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class TranslateSection implements SectionView {
    private final TitledPane root = new TitledPane();
    private final ComboBox<String> lang = new ComboBox<>();
    private final SectionEvents events;

    public TranslateSection(SectionEvents events) {
        this.events = events;
        lang.getItems().addAll("English", "Spanish", "French", "Italian", "Japanese", "Mandarin", "Korean");
        lang.getSelectionModel().select("English");

        VBox box = new VBox(6, lang);
        box.setPadding(new Insets(6));
        root.setText("Translate");
        root.setExpanded(true);
        root.setContent(box);

        lang.valueProperty().addListener((o, p, n) -> events.onLanguageChanged(n));
    }

    @Override public Node getNode() { return root; }
    public void select(String language) {
        if (!lang.getItems().contains(language)) lang.getItems().add(language);
        lang.getSelectionModel().select(language);
    }

    public String getSelected() {
        String v = lang.getValue();
        return v == null ? "English" : v;
    }

}
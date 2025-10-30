package org.example.app.view.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class StyleSection implements SectionView {
    private final TitledPane root = new TitledPane();
    private final ComboBox<String> style = new ComboBox<>();
    private final SectionEvents events;

    public StyleSection(SectionEvents events) {
        this.events = events;
        style.getItems().addAll("none", "APA", "MLA", "Chicago");
        style.getSelectionModel().select("none");

        VBox box = new VBox(6, style);
        box.setPadding(new Insets(6));
        root.setText("Style");
        root.setExpanded(true);
        root.setContent(box);

        style.valueProperty().addListener((o, p, n) -> events.onStyleChanged(n));
    }

    @Override public Node getNode() { return root; }
    public void select(String key) {
        if (!style.getItems().contains(key)) style.getItems().add(key);
        style.getSelectionModel().select(key);
    }

    public String getSelected() {
        String v = style.getValue();
        return v == null ? "none" : v;
    }
}
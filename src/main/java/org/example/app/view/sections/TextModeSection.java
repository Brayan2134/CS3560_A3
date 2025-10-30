package org.example.app.view.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class TextModeSection implements SectionView {
    private final TitledPane root = new TitledPane();
    private final ComboBox<String> mode = new ComboBox<>();
    private final SectionEvents events;

    public TextModeSection(SectionEvents events) {
        this.events = events;
        mode.getItems().addAll("summarize", "same", "expand");
        mode.getSelectionModel().select("same");

        VBox box = new VBox(6, mode);
        box.setPadding(new Insets(6));
        root.setText("Text mode");
        root.setExpanded(true);
        root.setContent(box);

        mode.valueProperty().addListener((o, p, n) -> events.onTextModeChanged(n));
    }

    @Override public Node getNode() { return root; }
    public void select(String key) { mode.getSelectionModel().select(key); }

    public String getSelected() {
        String v = mode.getValue();
        return v == null ? "same" : v;
    }

}
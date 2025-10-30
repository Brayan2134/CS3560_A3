package org.example.app.view.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ToneSection implements SectionView {
    private final TitledPane root = new TitledPane();
    private final ToggleGroup group = new ToggleGroup();
    private final RadioButton informal = new RadioButton("Informal");
    private final RadioButton neutral  = new RadioButton("Neutral");
    private final RadioButton formal   = new RadioButton("Formal");
    private final SectionEvents events;

    public ToneSection(SectionEvents events) {
        this.events = events;

        informal.setToggleGroup(group);
        neutral.setToggleGroup(group);
        formal.setToggleGroup(group);
        neutral.setSelected(true);

        VBox box = new VBox(6, informal, neutral, formal);
        box.setPadding(new Insets(6));

        root.setText("Tone");
        root.setExpanded(true);
        root.setContent(box);

        group.selectedToggleProperty().addListener((o, prev, cur) -> {
            if (cur == informal) events.onToneChanged("informal");
            else if (cur == formal) events.onToneChanged("formal");
            else events.onToneChanged("neutral");
        });
    }

    @Override public Node getNode() { return root; }
    public void select(String key) {
        switch (key) {
            case "informal" -> informal.setSelected(true);
            case "formal"   -> formal.setSelected(true);
            default         -> neutral.setSelected(true);
        }
    }

    public String getSelectedKey() {
        if (informal.isSelected()) return "informal";
        if (formal.isSelected())   return "formal";
        return "neutral";
    }
}
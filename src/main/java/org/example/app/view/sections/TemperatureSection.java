package org.example.app.view.sections;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TemperatureSection implements SectionView {
    private final TitledPane root = new TitledPane();
    private final Slider slider = new Slider(0, 2, 0.5);
    private final SectionEvents events;

    public TemperatureSection(SectionEvents events) {
        this.events = events;

        slider.setBlockIncrement(0.1);
        slider.setMajorTickUnit(0.5);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(false);

        Tooltip tip = new Tooltip(
                "Temperature controls randomness/creativity:\n" +
                        "• 0.0 = deterministic (precise)\n" +
                        "• ~0.5 = balanced\n" +
                        "• 1.0+ = more creative/surprising"
        );
        Tooltip.install(slider, tip);

        Label value = new Label();
        value.textProperty().bind(Bindings.format("Current: %.2f", slider.valueProperty()));

        Label left  = new Label("0 (more focused)");
        Label mid   = new Label("balanced");
        Label right = new Label("2 (more creative)");
        Region sL = new Region(); Region sR = new Region();
        HBox.setHgrow(sL, Priority.ALWAYS); HBox.setHgrow(sR, Priority.ALWAYS);

        HBox axis = new HBox(8, left, sL, mid, sR, right);
        axis.setAlignment(Pos.BASELINE_LEFT);

        VBox content = new VBox(6, slider, axis, value);
        content.setPadding(new Insets(6));
        root.setText("Temperature");
        root.setExpanded(true);
        root.setContent(content);

        slider.valueProperty().addListener((o, ov, nv) -> events.onTemperatureChanged(nv.doubleValue()));
    }

    @Override public Node getNode() { return root; }
    public void set(double v) { slider.setValue(v); }
    public double get() { return slider.getValue(); }
}
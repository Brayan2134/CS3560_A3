package org.example.app.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class WritingView {

    private final BorderPane root = new BorderPane();

    // Left: input/output
    public final TextArea input = new TextArea();
    public final TextArea output = new TextArea();

    // Right: controls
    public final Slider temperature = new Slider(0, 2, 0.5);
    public final ToggleGroup toneGroup = new ToggleGroup();
    public final RadioButton rbInformal = new RadioButton("Informal");
    public final RadioButton rbNeutral = new RadioButton("Neutral");
    public final RadioButton rbFormal = new RadioButton("Formal");
    public final ComboBox<String> style = new ComboBox<>();
    public final ComboBox<String> textMode = new ComboBox<>();
    public final Button btnGenerate = new Button("Generate");
    public final Label status = new Label("Idle");

    public WritingView() {
        // Text areas
        input.setPromptText("Type your text here…");
        output.setEditable(false);

        // ✅ wrap-around for long lines
        input.setWrapText(true);
        output.setWrapText(true);

        HBox io = new HBox(10, wrap("Input", input), wrap("Output", output));
        io.setPadding(new Insets(10));
        HBox.setHgrow(input, Priority.ALWAYS);
        HBox.setHgrow(output, Priority.ALWAYS);

        // Tone radios
        rbInformal.setToggleGroup(toneGroup);
        rbNeutral.setToggleGroup(toneGroup);
        rbFormal.setToggleGroup(toneGroup);
        rbNeutral.setSelected(true);

        // Style + Text mode
        style.getItems().addAll("none", "APA", "MLA", "Chicago");
        style.getSelectionModel().select("none");

        textMode.getItems().addAll("summarize", "same", "expand");
        textMode.getSelectionModel().select("same");

        // Temperature slider
        temperature.setBlockIncrement(0.1);
        temperature.setMajorTickUnit(0.5);
        temperature.setShowTickMarks(true);
        temperature.setShowTickLabels(true);

        // ✅ helpful tooltip for temperature
        Tooltip tempTip = new Tooltip(
                "Temperature controls randomness/creativity:\n" +
                        "• 0.0 = deterministic (precise, consistent)\n" +
                        "• 0.2–0.4 = formal/professional writing\n" +
                        "• ~0.5 = balanced\n" +
                        "• 0.6–0.8 = creative/varied phrasing\n" +
                        "• 1.0+ = very playful, more surprising\n" +
                        "Tip: lower for summaries & formal docs; higher for brainstorming."
        );
        Tooltip.install(temperature, tempTip);

        // ✅ small label showing the live numeric value
        Label tempValue = new Label();
        tempValue.textProperty().bind(Bindings.format("Current: %.2f", temperature.valueProperty()));
        VBox tempBox = new VBox(6, temperature, tempValue);

        // Right control panel
        VBox right = new VBox(12,
                titled("Temperature", tempBox),
                titled("Tone", new VBox(6, rbInformal, rbNeutral, rbFormal)),
                titled("Style", style),
                titled("Text mode", textMode),
                btnGenerate,
                new Separator(),
                status
        );
        right.setAlignment(Pos.TOP_LEFT);
        right.setPadding(new Insets(12));
        right.setPrefWidth(260);

        root.setCenter(io);
        root.setRight(right);
    }

    private Node wrap(String title, Node n) {
        VBox box = new VBox(6, new Label(title), n);
        VBox.setVgrow(n, Priority.ALWAYS);
        return box;
    }

    private TitledPane titled(String title, Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setExpanded(true);
        return tp;
    }

    public BorderPane getRoot() { return root; }
}
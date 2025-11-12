package org.example.app.view.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.app.preset.Preset;
import org.example.app.preset.PresetRegistry;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.Map;

public class PresetTabsView implements SectionView {
    private final TabPane tabs = new TabPane();
    private final Map<String, TextArea> editors = new LinkedHashMap<>();

    public PresetTabsView() {
        tabs.setTabMinWidth(100);
        tabs.setTabMaxWidth(Double.MAX_VALUE);
        tabs.setPrefWidth(400);
        tabs.setMinWidth(40);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        PresetRegistry.all().forEach((k, p) -> {
            TextArea ta = new TextArea(p.defaultInstruction().trim());
            ta.setWrapText(true);
            ta.setPrefRowCount(10);
            ta.setPrefColumnCount(60);
            VBox.setVgrow(ta, Priority.ALWAYS);

            VBox content = new VBox(8, new Label("Preset Prompt (editable):"), ta);
            content.setPadding(new Insets(10));
            Tab tab = new Tab(p.title(), content);
            tab.setId(p.key());
            tab.setClosable(false);

            tabs.getTabs().add(tab);
            editors.put(k, ta);
        });

        if (!tabs.getTabs().isEmpty()) tabs.getSelectionModel().select(0);
    }

    public String currentKey() {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        return (t != null) ? t.getId() : PresetRegistry.all().keySet().iterator().next();
    }

    public String currentInstruction() {
        var ta = editors.get(currentKey());
        return ta != null ? ta.getText() : "";
    }

    public void resetCurrentToDefault() {
        Preset p = PresetRegistry.all().get(currentKey());
        if (p != null) {
            var ta = editors.get(currentKey());
            if (ta != null) ta.setText(p.defaultInstruction().trim());
        }
    }

    public void setCurrentInstruction(String text) { editors.get(currentKey()).setText(text); }

    public TabPane getTabs() { return tabs; }
    @Override public Node getNode() { return tabs; }
}
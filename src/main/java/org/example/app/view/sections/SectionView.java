package org.example.app.view.sections;

import javafx.scene.Node;

/** Minimal contract for a right-pane section (Temp, Tone, Style, etc.). */
public interface SectionView {
    Node getNode();
    default void setVisible(boolean visible) {
        getNode().setManaged(visible);
        getNode().setVisible(visible);
    }
}
package org.example.app.model;

/** Simple observer so the controller/view can react to model state. */
public interface WritingModelListener {
    default void onRequestStart(String prompt) {}
    default void onRequestComplete(String output) {}
    default void onRequestError(String code, String message) {}
}
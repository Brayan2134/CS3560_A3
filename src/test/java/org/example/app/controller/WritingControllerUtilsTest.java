package org.example.app.controller;

import org.example.app.controller.WritingController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class WritingControllerUtilsTest {

    private static String invokeSafe(String in) {
        try {
            Method m = WritingController.class.getDeclaredMethod("safe", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, in); // static
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String invokeCombine(String a, String b) {
        try {
            Method m = WritingController.class.getDeclaredMethod("combineNonBlank", String.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, a, b); // static
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("safe(String)")
    class Safe {
        @Test
        @DisplayName("returns empty string for null")
        void nullReturnsEmpty() {
            assertEquals("", invokeSafe(null));
        }

        @Test
        @DisplayName("passes through non-null")
        void passesThrough() {
            assertEquals("abc", invokeSafe("abc"));
            assertEquals("", invokeSafe("")); // already empty
            assertEquals("   ", invokeSafe("   ")); // preserves whitespace
        }
    }

    @Nested
    @DisplayName("combineNonBlank(String, String)")
    class CombineNonBlank {
        @Test
        @DisplayName("both blank → empty")
        void bothBlank() {
            assertEquals("", invokeCombine("", ""));
            assertEquals("", invokeCombine("   ", " \n "));
            assertEquals("", invokeCombine(null, null));
        }

        @Test
        @DisplayName("only A non-blank → A")
        void onlyANonBlank() {
            assertEquals("A", invokeCombine("A", "   "));
            assertEquals("A", invokeCombine("A", null));
        }

        @Test
        @DisplayName("only B non-blank → B")
        void onlyBNonBlank() {
            assertEquals("B", invokeCombine("   ", "B"));
            assertEquals("B", invokeCombine(null, "B"));
        }

        @Test
        @DisplayName("both non-blank → A + newline + B")
        void bothNonBlank() {
            assertEquals("A\nB", invokeCombine("A", "B"));
            assertEquals("First line\nSecond line", invokeCombine("First line", "Second line"));
        }
    }
}
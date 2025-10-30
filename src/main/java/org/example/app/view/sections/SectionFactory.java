package org.example.app.view.sections;

import java.util.ArrayList;
import java.util.List;

public final class SectionFactory {
    private SectionFactory() {}

    public static List<SectionView> buildRightColumn(
            SectionEvents events, boolean showStyle, boolean showTextMode, boolean showTranslation) {

        List<SectionView> sections = new ArrayList<>();
        sections.add(new TemperatureSection(events));
        sections.add(new ToneSection(events));
        if (showStyle)       sections.add(new StyleSection(events));
        if (showTextMode)    sections.add(new TextModeSection(events));
        if (showTranslation) sections.add(new TranslateSection(events));
        return sections;
    }
}
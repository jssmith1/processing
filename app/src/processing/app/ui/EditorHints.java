package processing.app.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorHints extends JScrollPane {
    private final List<Hint> HINTS;

    private final JLabel PROBLEM_TITLE_LABEL;
    private final JLabel SUGGESTION_TITLE_LABEL;

    private int hintIndex;

    public EditorHints() {
        HINTS = new ArrayList<>();

        setBorder(BorderFactory.createEmptyBorder());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder());
        setViewportView(panel);

        PROBLEM_TITLE_LABEL = new JLabel();
        SUGGESTION_TITLE_LABEL = new JLabel();

        Font probFont = PROBLEM_TITLE_LABEL.getFont();
        Font boldFont = probFont.deriveFont(probFont.getStyle() ^ Font.BOLD);
        PROBLEM_TITLE_LABEL.setFont(boldFont);

        panel.add(PROBLEM_TITLE_LABEL);
        panel.add(SUGGESTION_TITLE_LABEL);

        ArrayList<Hint> testingList = new ArrayList<>();
        testingList.add(new Hint("You did not return a value of type int like the definition of method doSomething().",
                "You may have forgotten the return statement for the method doSomething()."));
        setCurrentHints(testingList);
    }

    private void setCurrentHints(List<Hint> newHints) {
        HINTS.clear();
        HINTS.addAll(newHints);
        setVisibleHint(0);
    }

    private void setVisibleHint(int index) {
        hintIndex = index;
        Hint visibleHint = HINTS.get(index);

        PROBLEM_TITLE_LABEL.setText(visibleHint.getProblemText());
        SUGGESTION_TITLE_LABEL.setText(visibleHint.getSuggestionText());
    }

    private static class Hint {
        private final String PROBLEM_TEXT;
        private final String SUGGESTION_TEXT;

        public Hint(String problemText, String suggestionText) {
            PROBLEM_TEXT = problemText;
            SUGGESTION_TEXT = suggestionText;
        }

        public String getProblemText() {
            return PROBLEM_TEXT;
        }

        public String getSuggestionText() {
            return SUGGESTION_TEXT;
        }
    }

}

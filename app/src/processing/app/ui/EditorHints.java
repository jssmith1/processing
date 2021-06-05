package processing.app.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorHints extends JScrollPane {
    private static final Border EMPTY_SPACING = BorderFactory.createEmptyBorder(
            8, 8, 8, 8
    );
    private static final Border GREEN_BORDER = BorderFactory.createLineBorder(
            new Color(71, 151, 97), 2
    );
    private static final Border RED_BORDER = BorderFactory.createLineBorder(
            new Color(232, 90, 79), 2
    );

    private final List<Hint> HINTS;

    private final JLabel PROBLEM_TITLE_LABEL;
    private final JLabel SUGGESTION_TITLE_LABEL;
    private final JLabel SUGGESTION_COUNTER;
    private final Box BAD_CODE_BOX;
    private final Box GOOD_CODE_BOX;

    private int hintIndex;

    public EditorHints() {
        HINTS = new ArrayList<>();

        setBorder(BorderFactory.createEmptyBorder());

        // Create the panel
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder());
        setViewportView(panel);

        // Create title labels
        PROBLEM_TITLE_LABEL = new JLabel();
        SUGGESTION_TITLE_LABEL = new JLabel();

        Font probFont = PROBLEM_TITLE_LABEL.getFont();
        Font boldFont = probFont.deriveFont(probFont.getStyle() ^ Font.BOLD);
        PROBLEM_TITLE_LABEL.setFont(boldFont);

        Box titleBox = Box.createVerticalBox();
        titleBox.add(PROBLEM_TITLE_LABEL);
        titleBox.add(SUGGESTION_TITLE_LABEL);

        // Create suggestion counter
        SUGGESTION_COUNTER = new JLabel();

        // Add header layout
        Box headerBox = Box.createHorizontalBox();
        headerBox.add(titleBox);
        headerBox.add(Box.createHorizontalGlue());
        headerBox.add(SUGGESTION_COUNTER);

        // Create a split box to hold code examples
        Box codeBox = Box.createHorizontalBox();
        BAD_CODE_BOX = Box.createVerticalBox();
        GOOD_CODE_BOX = Box.createVerticalBox();

        BAD_CODE_BOX.setBorder(EMPTY_SPACING);
        GOOD_CODE_BOX.setBorder(EMPTY_SPACING);

        codeBox.add(BAD_CODE_BOX);
        codeBox.add(GOOD_CODE_BOX);

        // Create navigation button
        JButton navButton = new JButton("View Next Hint");
        navButton.setFocusable(false); // Stop the button from glowing on press
        navButton.addActionListener(
                (event) -> setVisibleHint((hintIndex + 1) % HINTS.size())
        );

        Box navBox = Box.createHorizontalBox();
        navBox.add(Box.createHorizontalGlue());
        navBox.add(navButton);

        panel.add(headerBox, BorderLayout.NORTH);
        panel.add(codeBox, BorderLayout.CENTER);
        panel.add(navBox, BorderLayout.SOUTH);

        ArrayList<Hint> testingList = new ArrayList<>();
        Hint testHint = new Hint("You did not return a value of type int like the definition of method doSomething().",
                "You may have forgotten the return statement for the method doSomething().");
        testHint.addGoodCode("// Good code");
        testHint.addBadCode("// Bad code");
        testHint.addGoodCode("// Good code");
        testHint.addBadCode("// Bad code");
        testingList.add(testHint);
        testingList.add(testHint);

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
        SUGGESTION_COUNTER.setText("Hint " + (hintIndex + 1)
                + "/" + HINTS.size());

        BAD_CODE_BOX.removeAll();
        GOOD_CODE_BOX.removeAll();

        BAD_CODE_BOX.add(new JLabel("Incorrect Code"));
        GOOD_CODE_BOX.add(new JLabel("Good Code"));

        for (String badCode : visibleHint.getBadCode()) {
            addCodeBox(badCode, BAD_CODE_BOX, RED_BORDER);
        }

        for (String goodCode : visibleHint.getGoodCode()) {
            addCodeBox(goodCode, GOOD_CODE_BOX, GREEN_BORDER);
        }

        // Scroll to the top so user sees suggestion title
        scrollRectToVisible(PROBLEM_TITLE_LABEL.getBounds());

    }

    private void addCodeBox(String example, JComponent parent, Border border) {
        parent.add(Box.createVerticalStrut(8));

        JTextArea textArea = new JTextArea(example);
        textArea.setEditable(false);
        textArea.setBorder(BorderFactory.createCompoundBorder(border, EMPTY_SPACING));
        parent.add(textArea);
    }

    private static class Hint {
        private final String PROBLEM_TEXT;
        private final String SUGGESTION_TEXT;
        private final List<String> GOOD_CODE;
        private final List<String> BAD_CODE;

        public Hint(String problemText, String suggestionText) {
            PROBLEM_TEXT = problemText;
            SUGGESTION_TEXT = suggestionText;
            GOOD_CODE = new ArrayList<>();
            BAD_CODE = new ArrayList<>();
        }

        public void addGoodCode(String goodCode) {
            GOOD_CODE.add(goodCode);
        }

        public void addBadCode(String badCode) {
            BAD_CODE.add(badCode);
        }

        public String getProblemText() {
            return PROBLEM_TEXT;
        }

        public String getSuggestionText() {
            return SUGGESTION_TEXT;
        }

        public List<String> getBadCode() {
            return BAD_CODE;
        }

        public List<String> getGoodCode() {
            return GOOD_CODE;
        }

    }

}

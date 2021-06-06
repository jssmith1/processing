package processing.mode.java.pdex;

import org.eclipse.jdt.core.compiler.IProblem;
import processing.app.ui.EditorHints;

import java.util.ArrayList;
import java.util.List;

public class JavaHint implements EditorHints.Hint {
    private final String PROBLEM_TEXT;
    private final String SUGGESTION_TEXT;
    private final List<String> GOOD_CODE;
    private final List<String> BAD_CODE;

    public static List<EditorHints.Hint> fromIProblem(IProblem compilerError) {
        ArrayList<EditorHints.Hint> testingList = new ArrayList<>();
        JavaHint testHint = new JavaHint("You did not return a value of type int like the definition of method doSomething().",
                "You may have forgotten the return statement for the method doSomething().");
        testHint.addGoodCode("// Good code");
        testHint.addBadCode("// Bad code");
        testHint.addGoodCode("// Good code");
        testHint.addBadCode("// Bad code");
        testingList.add(testHint);
        testingList.add(testHint);
        return testingList;
    }

    public JavaHint(String problemText, String suggestionText) {
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

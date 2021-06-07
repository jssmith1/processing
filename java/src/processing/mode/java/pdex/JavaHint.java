package processing.mode.java.pdex;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import processing.app.ui.EditorHints;

import java.util.ArrayList;
import java.util.List;

public class JavaHint implements EditorHints.Hint {
    private final String PROBLEM_TEXT;
    private final String SUGGESTION_TEXT;
    private final List<String> GOOD_CODE;
    private final List<String> BAD_CODE;

    public static List<EditorHints.Hint> fromIProblem(IProblem compilerError, ASTNode ast) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String[] problemArguments = compilerError.getArguments();
        ASTNode problemNode = ASTUtils.getASTNodeAt(
                ast,
                compilerError.getSourceStart(),
                compilerError.getSourceEnd()
        );

        if (compilerError.getID() == IProblem.TypeMismatch) {
            String varName = ((VariableDeclarationFragment) problemNode.getParent()).getName().toString();
            String providedType = problemArguments[0];
            String requiredType = problemArguments[1];

            String problemTitle = "You are trying to use the variable " + varName + " of type "
                    + requiredType + " as a " + providedType + "-type variable.";

            JavaHint hint = new JavaHint(problemTitle,
                    "You may have assigned a " + providedType + " value to variable " + varName +
                            " of type " + requiredType + "."
            );

            // TODO: provide different code samples based on provided type
            // For number literals, the provided value is available after casting the node to a NumberLiteral
            hint.addBadCode(requiredType + " " + varName + " = 5.0;");
            hint.addGoodCode(providedType + " " + varName + " = 5.0;");

            hints.add(hint);
        }

        return hints;
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

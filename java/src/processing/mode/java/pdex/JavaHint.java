package processing.mode.java.pdex;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import processing.app.ui.EditorHints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JavaHint implements EditorHints.Hint {
    private final String PROBLEM_TEXT;
    private final String SUGGESTION_TEXT;
    private final List<String> GOOD_CODE;
    private final List<String> BAD_CODE;

    public static List<EditorHints.Hint> fromIProblem(IProblem compilerError, ASTNode ast) {
        String[] problemArguments = compilerError.getArguments();
        ASTNode problemNode = ASTUtils.getASTNodeAt(
                ast,
                compilerError.getSourceStart(),
                compilerError.getSourceEnd()
        );

        if (compilerError.getID() == IProblem.TypeMismatch) {
            String providedType = truncateClass(problemArguments[0]);
            String requiredType = truncateClass(problemArguments[1]);
            return getTypeMismatchHints(providedType, requiredType, problemNode);
        }

        return Collections.emptyList();
    }

    private static List<EditorHints.Hint> getTypeMismatchHints(String providedType, String requiredType,
                                                               ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String varName = ((VariableDeclarationFragment) problemNode.getParent()).getName().toString();
        String problemTitle = "You are trying to use the " + getVarDescription(requiredType)
                + " " + varName + " as a " + getVarDescription(providedType) + ".";

        JavaHint changeVarDec = new JavaHint(problemTitle,
                "You might need to change the variable declaration of "
                        + varName + " to type " + providedType + "."
        );
        hints.add(changeVarDec);

        JavaHint changeValue = new JavaHint(problemTitle,
                "You might need to change the value of "
                        + varName + " to a " + requiredType + "."
        );
        hints.add(changeValue);

        JavaHint changeReturnType = new JavaHint(problemTitle,
                "You might need to change the method's return type to "
                       + providedType + "."
        );
        hints.add(changeReturnType);

        // Clarify numerical expressions where a float result is assigned to an int
        if (providedType.equals("float") && requiredType.equals("int")) {
            JavaHint changeOpType = new JavaHint(problemTitle,
                    "You may have used an int-type variable " + varName
                            + " in an operation involving the float type."
            );
            hints.add(changeOpType);
        }

        // TODO: provide different code samples based on provided type
        // For number literals, the provided value is available after casting the node to a NumberLiteral
        //hint.addBadCode(requiredType + " " + varName + " = 5.0;");
        //hint.addGoodCode(providedType + " " + varName + " = 5.0;");

        return hints;
    }

    private static String getVarDescription(String typeName) {
        List<String> primitives = Arrays.asList("byte", "short", "int", "long",
                "float", "double", "boolean", "char");

        if (primitives.contains(typeName)) {
            return typeName + "-type variable";
        }

        return typeName + " object";
    }

    private static String truncateClass(String qualifiedName) {
        int lastPeriodIndex = qualifiedName.lastIndexOf('.');

        if (lastPeriodIndex == -1) {
            return qualifiedName;
        }

        return qualifiedName.substring(lastPeriodIndex + 1);
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

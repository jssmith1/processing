package processing.mode.java.pdex;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import processing.app.ui.EditorHints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JavaHint implements EditorHints.Hint {
    private static final List<String> PRIMITIVES = Arrays.asList(
            "byte", "short", "int", "long",
            "float", "double", "boolean", "char"
    );

    public static List<EditorHints.Hint> fromIProblem(IProblem compilerError, ASTNode ast) {
        String[] problemArguments = compilerError.getArguments();
        ASTNode problemNode = ASTUtils.getASTNodeAt(
                ast,
                compilerError.getSourceStart(),
                compilerError.getSourceEnd()
        );

        switch (compilerError.getID()) {
            case IProblem.MustDefineEitherDimensionExpressionsOrInitializer:
                return getArrDimHints(problemNode);
            case IProblem.IllegalDimension:
                return getTwoDimArrHints(problemNode);
            case IProblem.TypeMismatch:
                String providedType = truncateClass(problemArguments[0]);
                String requiredType = truncateClass(problemArguments[1]);
                return getTypeMismatchHints(providedType, requiredType, problemNode);
        }

        return Collections.emptyList();
    }

    private static List<EditorHints.Hint> getArrDimHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String arrType = problemNode.toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent().getParent())
                .getName().toString();
        String problemTitle = "You have not given the array a certain size.";

        // Suggest adding array dimension
        JavaHint addDim = new JavaHint(problemTitle,
                "You may have forgotten to type the size "
                        + "of the array inside the brackets."
        );
        addDim.addBadCode(arrType + "[] " + arrName + " = new " + arrType + "[];");
        addDim.addGoodCode(arrType + "[] " + arrName + " = new " + arrType + "[5];");
        hints.add(addDim);

        return hints;
    }

    private static List<EditorHints.Hint> getTwoDimArrHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String arrType = ((ArrayCreation) problemNode.getParent()).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent())
                .getName().toString();
        String problemTitle = "In a 2D array, you have not given the "
                + "innermost array a certain size.";

        // Suggest adding array dimension
        JavaHint addDim = new JavaHint(problemTitle,
                "Specify the size of the innermost array."
        );
        addDim.addBadCode(arrType + "[][] " + arrName + " = new " + arrType + "[][5];");
        addDim.addGoodCode(arrType + "[][] " + arrName + " = new " + arrType + "[5][5];");
        addDim.addGoodCode(arrType + "[][] " + arrName + " = new " + arrType + "[5][];");
        hints.add(addDim);

        return hints;
    }

    private static List<EditorHints.Hint> getTypeMismatchHints(String providedType, String requiredType,
                                                               ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String varName = ((VariableDeclarationFragment) problemNode.getParent()).getName().toString();
        String problemTitle = "You are trying to use the " + getVarDescription(requiredType)
                + " " + varName + " as a " + getVarDescription(providedType) + ".";

        // Suggest changing variable declaration
        JavaHint changeVarDec = new JavaHint(problemTitle,
                "You might need to change the variable declaration of "
                        + varName + " to type " + providedType + "."
        );
        changeVarDec.addBadCode(getDemoDeclaration(requiredType, varName, providedType));
        changeVarDec.addGoodCode(getDemoDeclaration(providedType, varName));
        hints.add(changeVarDec);

        // Suggest changing variable value
        JavaHint changeValue = new JavaHint(problemTitle,
                "You might need to change the value of "
                        + varName + " to a " + requiredType + "."
        );
        changeValue.addBadCode(getDemoDeclaration(requiredType, varName, providedType));
        changeValue.addGoodCode(getDemoDeclaration(requiredType, varName));
        hints.add(changeValue);

        // Suggest changing return type
        JavaHint changeReturnType = new JavaHint(problemTitle,
                "You might need to change the method's return type to "
                       + providedType + "."
        );
        changeReturnType.addBadCode(requiredType + " doSomething() {\n"
                + "\t" + getDemoDeclaration(providedType, varName) + "\n"
                + "\t" + "return " + varName + ";\n"
                + "}");
        changeReturnType.addGoodCode(providedType + " doSomething() {\n"
                + "\t" + getDemoDeclaration(providedType, varName) + "\n"
                + "\t" + "return " + varName + ";\n"
                + "}");
        hints.add(changeReturnType);

        // Clarify numerical expressions where a float result is assigned to an int
        if (providedType.equals("float") && requiredType.equals("int")) {
            JavaHint changeOpType = new JavaHint(problemTitle,
                    "You may have used an int-type variable " + varName
                            + " in an operation involving the float type."
            );
            changeOpType.addBadCode(getDemoDeclaration(requiredType, varName) + "\n"
                    + varName + " = " + varName + " + 3.14;");
            changeOpType.addGoodCode(getDemoDeclaration(providedType, varName) + "\n"
                    + varName + " = " + varName + " + 3.14;");
            hints.add(changeOpType);
        }

        return hints;
    }

    private static String getVarDescription(String typeName) {
        if (PRIMITIVES.contains(typeName) || typeName.equals("String")) {
            return typeName + "-type variable";
        }

        return typeName + " object";
    }

    private static String getDemoDeclaration(String decType, String varName) {
        return getDemoDeclaration(decType, varName, decType);
    }

    private static String getDemoDeclaration(String decType, String varName, String valType) {
        return decType + " " + varName + " = " + getDemoValue(valType) + ";";
    }

    private static String getDemoValue(String typeName) {
        switch (typeName) {
            case "byte":
            case "short":
            case "int":
            case "long":
                return "5";
            case "float":
            case "double":
                return "5.0";
            case "boolean":
                return "true";
            case "char":
                return "'a'";
            case "String":
                return "\"hello world\"";
            default:
                return "new " + typeName + "()";
        }
    }

    private static String truncateClass(String qualifiedName) {
        int lastPeriodIndex = qualifiedName.lastIndexOf('.');

        if (lastPeriodIndex == -1) {
            return qualifiedName;
        }

        return qualifiedName.substring(lastPeriodIndex + 1);
    }

    private final String PROBLEM_TEXT;
    private final String SUGGESTION_TEXT;
    private final List<String> GOOD_CODE;
    private final List<String> BAD_CODE;

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

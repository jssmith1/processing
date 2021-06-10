package processing.mode.java.pdex;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import processing.app.ui.EditorHints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JavaHint implements EditorHints.Hint {
    private static final List<String> PRIMITIVES = Arrays.asList(
            "byte", "short", "int", "long",
            "float", "double", "boolean", "char"
    );
    private static final Random RANDOM = new Random();
    private static final String[] ALPHABET = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "x", "y", "z"
    };

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
            case IProblem.CannotDefineDimensionExpressionsWithInit:
                return getTwoInitializerArrHints(problemNode);
            case IProblem.UndefinedMethod:
                return getMissingMethodHints(problemNode);
            case IProblem.ParameterMismatch:
                return getParamMismatchHints(problemNode);
            case IProblem.ShouldReturnValue:
                return getMissingReturnHints(problemNode);
            case IProblem.TypeMismatch:
                String providedType = truncateClass(problemArguments[0]);
                String requiredType = truncateClass(problemArguments[1]);
                return getTypeMismatchHints(providedType, requiredType, problemNode);
            case IProblem.UndefinedType:
                return getMissingTypeHints(problemArguments[0], problemNode);
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

    private static List<EditorHints.Hint> getTwoInitializerArrHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        String arrType = ((ArrayCreation) problemNode.getParent()).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent())
                .getName().toString();
        String problemTitle = "You defined an array twice.";

        // Suggest adding array dimension
        JavaHint chooseInitMethod = new JavaHint(problemTitle,
                "You may have used both methods to construct an array together."
        );

        String initList = buildInitializerList(arrType, 5);
        chooseInitMethod.addBadCode(arrType + "[] " + arrName + " = new " + arrType
                + "[5] " + initList + ";");
        chooseInitMethod.addGoodCode(arrType + "[] " + arrName + " = new " + arrType + "[5];");
        chooseInitMethod.addGoodCode(arrType + "[] " + arrName + " = " + initList + ";");
        hints.add(chooseInitMethod);

        return hints;
    }

    private static List<EditorHints.Hint> getMissingMethodHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        MethodInvocation invoc = (MethodInvocation) problemNode.getParent();
        List<String> providedParams = ((List<?>) invoc.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invoc.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());

        /* We don't know the desired return type, so use a
           familiar one like "int" instead of one like "void." */
        String dummyReturnType = "int";

        String methodName = invoc.getName().toString();
        String nameWithParens = methodName + "()";
        String currMethodCall = methodName + "(" + String.join(", ", providedParams) + ")";
        String renamedMethodCall = "correctName(" + String.join(", ", providedParams) + ")";

        String problemTitle = "You are trying to use a function, "
                + nameWithParens
                + ", which Processing does not recognize. (\"Method\" "
                + "and \"function\" are used interchangeably here.)";

        // Suggest using correct Java name
        JavaHint useJavaName = new JavaHint(problemTitle,
                "If you are trying to use an existing Java function, "
                        + "make sure you match the name of " + nameWithParens
                        + " with the function."

        );
        useJavaName.addBadCode("String str = " + getDemoValue("String") + ";\n"
                + "str." + currMethodCall + ";");
        useJavaName.addGoodCode("String str = " + getDemoValue("String") + ";\n"
                + "str." + renamedMethodCall + ";");
        hints.add(useJavaName);

        // Suggest using correct user-given name
        JavaHint useDeclarationName = new JavaHint(problemTitle,
                "You may need to change the name of "
                        + nameWithParens + " to the method you created."
        );
        useDeclarationName.addBadCode(currMethodCall + ";");
        useDeclarationName.addGoodCode(currMethodCall + ";\n"
                + getMethodDec(methodName, dummyReturnType, providedParamTypes) + " {\n"
                + "  ...\n"
                + "}");
        hints.add(useDeclarationName);

        // Suggest calling method on object
        JavaHint callOnObj = new JavaHint(problemTitle,
                "You may need to create an object of a class "
                        + "and call the method " + nameWithParens + " on it."
        );
        callOnObj.addBadCode("class YourClass {\n  "
                + getMethodDec(methodName, dummyReturnType, providedParamTypes) + " {\n"
                + "    ...\n"
                + "  }\n}\n"
                + currMethodCall + ";");
        callOnObj.addGoodCode("class YourClass {\n  "
                + getMethodDec(methodName, dummyReturnType, providedParamTypes) + " {\n"
                + "    ...\n"
                + "  }\n}\n"
                + getDemoDeclaration("YourClass", "myObject")
                + "\nmyObject." + currMethodCall + ";");
        hints.add(callOnObj);

        // Suggest creating class method
        JavaHint createClassMethod = new JavaHint(problemTitle,
                "You may need to create the method "
                        + nameWithParens + " in a class."
        );
        createClassMethod.addBadCode("class YourClass {\n}\n"
                + getDemoDeclaration("YourClass", "myObject")
                + "\nmyObject." + currMethodCall + ";");
        createClassMethod.addGoodCode("class YourClass {\n  "
                + getMethodDec(methodName, dummyReturnType, providedParamTypes) + " {\n"
                + "    ...\n"
                + "  }\n}\n"
                + getDemoDeclaration("YourClass", "myObject")
                + "\nmyObject." + currMethodCall + ";");
        hints.add(createClassMethod);

        return hints;
    }

    private static List<EditorHints.Hint> getParamMismatchHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        MethodInvocation invoc = (MethodInvocation) problemNode.getParent();
        List<String> providedParams = ((List<?>) invoc.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invoc.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        List<String> requiredParamTypes = Arrays.stream(
                invoc.resolveMethodBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());

        String methodName = invoc.getName().toString();
        String methodReturnType = invoc.resolveMethodBinding().getReturnType().toString();
        String methodSig = getMethodSig(methodName, requiredParamTypes);
        String methodDec = getMethodDec(methodName, methodReturnType, requiredParamTypes);
        String problemTitle = "You are trying to use the method " + methodSig
                + " but with incorrect parameters.";

        String badCode = methodDec + " {\n  ...\n}\n"
                + "void setup() {\n  "
                + methodName + "(" + String.join(", ", providedParams) + ");\n"
                + "}\n";

        // Suggest changing provided parameter
        JavaHint changeParam = new JavaHint(problemTitle,
                "You might need to change a parameter of " + methodSig
                        + " to the expected type."
        );
        changeParam.addBadCode(badCode);
        changeParam.addGoodCode(methodDec + " {\n  ...\n}\n"
                + "void setup() {\n  "
                + getMethodCall(methodName, requiredParamTypes) + ";\n"
                + "}\n");
        hints.add(changeParam);

        // Suggest changing definition parameter
        JavaHint changeDef = new JavaHint(problemTitle,
                "You might need to change a parameter of " + methodSig
                        + " in the method declaration to the expected type."
        );
        changeDef.addBadCode(badCode);
        changeDef.addGoodCode(getMethodDec(methodName, methodReturnType, providedParamTypes)
                + " {\n  ...\n}\n"
                + "void setup() {\n  "
                + methodName + "(" + String.join(", ", providedParams) + ");\n"
                + "}\n");
        hints.add(changeDef);

        if (providedParamTypes.size() != requiredParamTypes.size()) {

            // Suggest changing number of provided parameters
            JavaHint changeNumParams = new JavaHint(problemTitle,
                    "You may need to change the number of parameters to the "
                            + "expected amount when calling " + methodSig + "."
            );
            changeNumParams.addBadCode(badCode);
            changeNumParams.addGoodCode(methodDec + " {\n  ...\n}\n"
                    + "void setup() {\n  "
                    + getMethodCall(methodName, requiredParamTypes) + ";\n"
                    + "}\n");
            hints.add(changeNumParams);

            // Suggest changing number of definition parameters
            JavaHint changeNumDefParams = new JavaHint(problemTitle,
                    "Change the number of parameters in the " + methodSig
                            + " method declaration."
            );
            changeNumDefParams.addBadCode(badCode);
            changeNumDefParams.addGoodCode(
                    getMethodDec(methodName, methodReturnType, providedParamTypes)
                            + " {\n  ...\n}\n"
                            + "void setup() {\n  "
                            + methodName + "(" + String.join(", ", providedParams) + ");\n"
                            + "}\n");
            hints.add(changeNumDefParams);

        }

        return hints;
    }

    private static List<EditorHints.Hint> getMissingReturnHints(ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        MethodDeclaration invoc = (MethodDeclaration) problemNode.getParent();
        List<String> requiredParamTypes = Arrays.stream(
                invoc.resolveBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());
        String methodName = invoc.getName().toString();
        String methodReturnType = invoc.getReturnType2().toString();
        String methodDec = getMethodDec(methodName, methodReturnType, requiredParamTypes);
        String nameWithParens = methodName + "()";

        String problemTitle = "You did not return a value of type " + methodReturnType
                + " like the definition of method " + nameWithParens + ".";

        // Suggest adding return at end
        JavaHint returnEnd = new JavaHint(problemTitle,
                "You may need to add a return statement of type " + methodReturnType
                        + " at the end of the method " + nameWithParens + "."
        );
        returnEnd.addBadCode(methodDec + " {\n"
                + "  ...\n"
                + "}");
        returnEnd.addGoodCode(methodDec + " {\n"
                + "  ...\n"
                + "  return " + getDemoValue(methodReturnType) + ";\n"
                + "}");
        hints.add(returnEnd);

        // Suggest adding return in all branches
        JavaHint returnBranch = new JavaHint(problemTitle,
                "Make sure all branches of conditionals in " + nameWithParens
                + " return a value of type " + methodReturnType + "."
        );
        returnBranch.addBadCode(methodDec + " {\n"
                + "  if (...) {\n"
                + "    ...\n"
                + "    return " + getDemoValue(methodReturnType) + ";\n"
                + "  } else {\n"
                + "    ...\n"
                + "  }\n"
                + "}");
        returnBranch.addGoodCode(methodDec + " {\n"
                + "  if (...) {\n"
                + "    ...\n"
                + "    return " + getDemoValue(methodReturnType) + ";\n"
                + "  } else {\n"
                + "    ...\n"
                + "    return " + getDemoValue(methodReturnType) + ";\n"
                + "  }\n"
                + "}");
        returnBranch.addGoodCode(methodDec + " {\n"
                + "  if (...) {\n"
                + "    ...\n"
                + "    return " + getDemoValue(methodReturnType) + ";\n"
                + "  } \n"
                + "  ...\n"
                + "  return " + getDemoValue(methodReturnType) + ";\n"
                + "}");
        hints.add(returnBranch);

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
                + "  " + getDemoDeclaration(providedType, varName) + "\n"
                + "  " + "return " + varName + ";\n"
                + "}");
        changeReturnType.addGoodCode(providedType + " doSomething() {\n"
                + "  " + getDemoDeclaration(providedType, varName) + "\n"
                + "  " + "return " + varName + ";\n"
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

    private static List<EditorHints.Hint> getMissingTypeHints(String missingType, ASTNode problemNode) {
        List<EditorHints.Hint> hints = new ArrayList<>();

        ASTNode grandparent = problemNode.getParent().getParent();
        if (!(grandparent instanceof VariableDeclarationStatement)) {
            return Collections.emptyList();
        }

        // All variables in the statement will be the same type, so use the first as an example
        VariableDeclarationStatement varStatement = (VariableDeclarationStatement) problemNode.getParent().getParent();
        VariableDeclarationFragment firstVar = (VariableDeclarationFragment) varStatement.fragments().get(0);

        String varName = firstVar.getName().toString();
        String problemTitle = "You are trying to declare a variable of type "
        + missingType + ", which Processing does not recognize.";

        // Suggest fixing a typo in the type name
        JavaHint fixTypo = new JavaHint(problemTitle,
                "You may need to correct the name of " + missingType
                        + " if you mistyped it."
        );
        fixTypo.addBadCode(getDemoDeclaration(missingType, varName));
        fixTypo.addGoodCode(getDemoDeclaration("CorrectName", varName));
        hints.add(fixTypo);

        // Suggest importing the class from library
        JavaHint importLib = new JavaHint(problemTitle,
                "You may need to correct the name of " + missingType
                        + " if you mistyped it."
        );
        importLib.addBadCode(getDemoDeclaration(missingType, varName));
        importLib.addGoodCode("import path.to.library." + missingType + ";\n"
                + getDemoDeclaration(missingType, varName));
        hints.add(importLib);

        // Suggest importing the class from another file
        JavaHint importFile = new JavaHint(problemTitle,
                "You may need to correct the name of " + missingType
                        + " if you mistyped it."
        );
        importFile.addBadCode(getDemoDeclaration(missingType, varName));
        importFile.addGoodCode("import OtherFile." + missingType + ";\n"
                + getDemoDeclaration(missingType, varName));
        hints.add(importFile);

        // Suggest creating the class
        JavaHint createClass = new JavaHint(problemTitle,
                "You may need to correct the name of " + missingType
                        + " if you mistyped it."
        );
        createClass.addBadCode(getDemoDeclaration(missingType, varName));
        createClass.addGoodCode("class " + missingType + " {\n  ...\n}\n"
                + getDemoDeclaration(missingType, varName));
        hints.add(createClass);

        return hints;
    }

    private static String getMethodSig(String methodName, List<String> paramTypes) {
        return methodName + "(" + String.join(", ", paramTypes) + ")";
    }

    private static String getMethodDec(String name, String returnType, List<String> paramTypes) {
        IntStream indices = IntStream.range(0, paramTypes.size());
        List<String> typesWithNames = indices.mapToObj(
                (index) -> paramTypes.get(index) + " param" + (index + 1)
        ).collect(Collectors.toList());

        return returnType + " " + name + "(" + String.join(", ", typesWithNames) + ")";
    }

    private static String getMethodCall(String name, List<String> paramTypes) {
        List<String> paramValues = paramTypes.stream().map(
                JavaHint::getDemoValue
        ).collect(Collectors.toList());

        return name + "(" + String.join(", ", paramValues) + ")";
    }

    private static String buildInitializerList(String type, int size) {
        StringBuilder initializerList = new StringBuilder("{");
        String separator = ", ";

        for (int item = 0; item < size; item++) {
            initializerList.append(getDemoValue(type)).append(separator);
        }

        int lastSeparatorIndex = initializerList.lastIndexOf(separator);
        return initializerList.substring(0, lastSeparatorIndex) + "}";
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
                return Integer.toString(RANDOM.nextInt(100));
            case "float":
            case "double":
                return String.format("%1$,.2f", Math.random() * 10);
            case "boolean":
                return Boolean.toString(Math.random() > 0.5);
            case "char":
                return ALPHABET[RANDOM.nextInt(ALPHABET.length)];
            case "String":

                // Hard-code this to avoid inappropriate random strings
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

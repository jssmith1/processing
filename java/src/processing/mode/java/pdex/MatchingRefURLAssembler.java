package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import processing.app.SketchException;
import processing.app.syntax.JEditTextArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Creates URLs for MatchingRef errors based on the AST.
 * @author soir20
 */
public class MatchingRefURLAssembler {
    private static final String URL = "http://139.147.9.247/";

    /**
     * Gets the MatchingRef URL for an extra right curly brace.
     * @param textAboveError    all text in the editor at and above the 
     *                          line with the extra brace
     * @return the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getClosingCurlyBraceURL(String textAboveError) {

        // We want to find a block before the extraneous brace
        int endIndex = textAboveError.lastIndexOf('}');
        int rightBraceIndex = textAboveError.lastIndexOf('}', endIndex - 1);
        int leftBraceIndex = findMatchingBrace(textAboveError, rightBraceIndex);

        int startIndex = textAboveError.lastIndexOf('}', leftBraceIndex) + 1;
        String mismatchedSnippet = textAboveError.substring(startIndex, leftBraceIndex + 1)
                + "\n  ...\n" + textAboveError.substring(rightBraceIndex, endIndex + 1);
        String correctedSnippet = mismatchedSnippet.substring(0, mismatchedSnippet.length() - 1);

        return Optional.of(URL + "extraneousclosingcurlybrace?classname=Thing&methodname=doSomething");
    }

    /**
     * Gets the MatchingRef URL for an incorrect variable declaration.
     * @param textArea      text area for the file that contains the error
     * @param exception     incorrect declaration exception from compilation
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getIncorrectVarDeclarationURL(JEditTextArea textArea, SketchException exception) {
        int errorIndex = textArea.getLineStartOffset(exception.getCodeLine()) + exception.getCodeColumn() - 1;
        String code = textArea.getText();
        String declarationStatement = code.substring(errorIndex);
        int statementEndIndex = declarationStatement.indexOf(';', errorIndex);
        if (statementEndIndex >= 0) {
            declarationStatement = declarationStatement.substring(0, statementEndIndex);
        }

        List<String> declaredArrays = getDeclaredArrays(declarationStatement);

        String pattern = "\\s*[\\w\\d$]+\\s*=\\s*(new\\s*[\\w\\d$]+\\s*\\[\\d+]|\\{.*})\\s*[,;]";
        Optional<String> firstInvalidDeclarationOptional =
                declaredArrays.stream().filter((declaration) -> !declaration.matches(pattern)).findFirst();

        if (!firstInvalidDeclarationOptional.isPresent()) {
            return Optional.empty();
        }

        String firstInvalidDeclaration = firstInvalidDeclarationOptional.get();
        String arrName = firstInvalidDeclaration.trim().split("[^\\w\\d$]")[0];

        // Get array type
        String beforeErrorText = code.substring(0, errorIndex);
        int currentIndex = beforeErrorText.length() - 1;

        boolean hasIdentifierEnded = false;
        StringBuilder arrType = new StringBuilder();
        while (currentIndex >= 0 && !hasIdentifierEnded) {
            String currentChar = beforeErrorText.substring(currentIndex, currentIndex + 1);

            if (!currentChar.matches("[\\s\\[\\]]")) {
                arrType.insert(0, currentChar);
            } else if (arrType.length() > 0) {
                hasIdentifierEnded = true;
            }

            currentIndex--;
        }

        return Optional.of(URL + "incorrectvariabledeclaration?typename=" + arrType
                + "&foundname=" + arrName);
    }

    /**
     * Gets the MatchingRef URL for an incorrect method declaration.
     * @param textAboveError      all text in the editor at and above the
     *                            line with error
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getIncorrectMethodDeclarationURL(String textAboveError) {
        int lastOpenParenthesisIndex = textAboveError.lastIndexOf('(');

        int currentCharIndex = lastOpenParenthesisIndex;
        char currentChar;
        do {
            currentCharIndex--;
            currentChar = textAboveError.charAt(currentCharIndex);
        } while (currentCharIndex > 0 && Character.isJavaIdentifierPart(currentChar));

        /* The method name starts one character ahead of the current one if
           we didn't reach the start of the string */
        if (currentCharIndex > 0) currentCharIndex++;

        String methodName = textAboveError.substring(currentCharIndex, lastOpenParenthesisIndex);

        if (methodName.equals("draw") || methodName.equals("setup")) {

        } else {

        }

        return Optional.of(URL + "incorrectmethoddeclaration?setupmethodname=size&drawmethodname=rect");
    }

    /**
     * Gets the MatchingRef URL for a missing array dimension.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getArrDimURL(ASTNode problemNode) {
        ASTNode greatGrandparent = problemNode.getParent().getParent().getParent();
        if (!(greatGrandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = problemNode.toString();
        String arrName = ((VariableDeclarationFragment) greatGrandparent).getName().toString();

        return Optional.of(URL + "incorrectdimensionexpression1?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef URL when the first of two array dimensions is missing.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTwoDimArrURL(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();
        if (!(parent instanceof ArrayCreation) || !(grandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = ((ArrayCreation) parent).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) grandparent).getName().toString();

        return Optional.of(URL + "incorrectdimensionexpression2?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef URL for the use of two array initializers at once.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTwoInitializerArrURL(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();
        if (!(parent instanceof ArrayCreation) || !(grandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = ((ArrayCreation) parent).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) grandparent).getName().toString();

        return Optional.of(URL + "incorrectdimensionexpression3?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef URL for a missing method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingMethodURL(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodInvocation)) {
            return Optional.empty();
        }

        MethodInvocation invocation = (MethodInvocation) problemNode.getParent();
        List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invocation.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        String methodName = invocation.getName().toString();

        /* We don't know the desired return type, so use a
           familiar one like "int" instead of one like "void." */
        String dummyReturnType = "int";
        String dummyCorrectName = "correctName";

        return Optional.of(URL + "methodnotfound?methodname=" + methodName
                + "&correctmethodname=" + dummyCorrectName
                + "&typename=" + dummyReturnType);
    }

    /**
     * Gets the MatchingRef URL for a parameter mismatch in a method call.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getParamMismatchURL(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodInvocation)) {
            return Optional.empty();
        }

        MethodInvocation invocation = (MethodInvocation) parent;
        List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invocation.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        List<String> requiredParamTypes = Arrays.stream(
                invocation.resolveMethodBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());

        String methodName = invocation.getName().toString();
        String methodReturnType = invocation.resolveMethodBinding().getReturnType().toString();

        return Optional.of(URL + "parametermismatch?methodname=" + methodName
                + "&methodtypename=" + methodReturnType
                + "&typeonename=int&typetwoname=String");
    }

    /**
     * Gets the MatchingRef URL for a missing return statement in a method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingReturnURL(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodDeclaration)) {
            return Optional.empty();
        }

        MethodDeclaration declaration = (MethodDeclaration) parent;
        List<String> requiredParamTypes = Arrays.stream(
                declaration.resolveBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());
        String methodName = declaration.getName().toString();
        String methodReturnType = declaration.getReturnType2().toString();

        return Optional.of(URL + "returnmissing?methodname=" + methodName
                + "&typename=" + methodReturnType);
    }

    /**
     * Gets the MatchingRef URL for a mismatch between a variable's type and its assigned value.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTypeMismatchURL(String providedType, String requiredType, ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String varName = ((VariableDeclarationFragment) parent).getName().toString();

        return Optional.of(URL + "typemismatch?typeonename=" + providedType
                + "&typetwoname=" + requiredType
                + "&varname=" + varName);
    }

    /**
     * Gets the MatchingRef URL for a missing type.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingTypeURL(String missingType, ASTNode problemNode) {
        ASTNode grandparent = problemNode.getParent().getParent();

        // All variables in the statement will be the same type, so use the first as an example
        VariableDeclarationFragment firstVar;
        if (grandparent instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement varStatement = (VariableDeclarationStatement) grandparent;
            firstVar = (VariableDeclarationFragment) varStatement.fragments().get(0);
        } else if (grandparent.getParent() instanceof VariableDeclarationFragment) {
            firstVar = (VariableDeclarationFragment) grandparent.getParent();
        } else {
            return Optional.empty();
        }

        String varName = firstVar.getName().toString();
        String dummyCorrectName = "CorrectName";

        return Optional.of(URL + "typenotfound?classname=" + missingType
                + "&correctclassname=" + dummyCorrectName
                + "&varname=" + varName);
    }

    /**
     * Gets the MatchingRef URL for a missing variable.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingVarURL(String varName, ASTNode problemNode) {
        String params = "?";
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();

        System.out.println(varName);
        System.out.println(problemNode.getClass());
        System.out.println(parent.getClass());
        System.out.println(grandparent.getClass());

        if (parent instanceof MethodInvocation) {
            MethodInvocation invocation = (MethodInvocation) parent;
            List<String> requiredParamTypes = Arrays.stream(
                    invocation.resolveMethodBinding().getParameterTypes()
            ).map(ITypeBinding::getName).collect(Collectors.toList());
            List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                    .map(Object::toString).collect(Collectors.toList());

            String varType = requiredParamTypes.get(providedParams.indexOf(varName));
            params += "classname=" + varType + "&varname=" + varName;

        } else if (parent instanceof ArrayAccess && grandparent instanceof VariableDeclarationFragment) {
            ArrayAccess arrAccess = (ArrayAccess) parent;
            String length = arrAccess.getIndex().toString();

            // The "varName" is actually the declared type when an array is being created
            VariableDeclarationFragment declaration = (VariableDeclarationFragment) grandparent;
            String arrName = declaration.getName().toString();

            params += "classname=" + varName + "&varname=" + arrName;
        } else {
            return Optional.empty();
        }

        return Optional.of(URL + "variablenotfound" + params);
    }

    /**
     * Gets the MatchingRef URL for an uninitialized variable.
     * @param problemNode       node of the AST where the problem occurred
     * @return the the URL with path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getUninitializedVarURL(String varName, ASTNode problemNode) {
        String params = "?varname=" + varName;
        ASTNode parent = problemNode.getParent();

        Expression expressionNode;
        if (parent instanceof Expression) {
            expressionNode = (Expression) parent;
        } else if (parent instanceof ExpressionStatement) {
            expressionNode = ((ExpressionStatement) parent).getExpression();
        } else {
            return Optional.empty();
        }

        String type = expressionNode.resolveTypeBinding().getName();
        params += "&typename=" + type;

        return Optional.of(URL + "variablenotinit" + params);
    }

    /**
     * Finds the index for a brace matching the one at the index provided.
     * @param code          code to search in
     * @param startIndex    index where the the brace to find the match for is
     * @return the index of the matching brace or -1 if there is no matching brace
     */
    private int findMatchingBrace(String code, int startIndex) {
        AtomicInteger previousIndex = new AtomicInteger(startIndex);
        int neededLeftBraces = 0;

        char startChar = code.charAt(startIndex);
        boolean findRightBrace;
        Runnable moveToNextIndex;

        // Count the initial brace
        if (startChar == '{') {
            neededLeftBraces--;
            findRightBrace = true;
            moveToNextIndex = previousIndex::getAndIncrement;
        } else if (startChar == '}') {
            neededLeftBraces++;
            findRightBrace = false;
            moveToNextIndex = previousIndex::getAndDecrement;
        } else {
            throw new IllegalArgumentException("Character at index "
                    + startIndex + " is not a brace.");
        }

        // Find the matching brace
        while (neededLeftBraces != 0 && previousIndex.get() > 0 && previousIndex.get() < code.length() - 1) {
            moveToNextIndex.run();

            char nextChar = code.charAt(previousIndex.get());
            if (nextChar == '{') {
                neededLeftBraces--;
            } else if (nextChar == '}') {
                neededLeftBraces++;
            }
        }

        char lastCharacter = code.charAt(previousIndex.get());
        boolean isMatchingRight = findRightBrace && lastCharacter == '}';
        boolean isMatchingLeft = !findRightBrace && lastCharacter == '{';
        if (!isMatchingLeft && !isMatchingRight) {
            return -1;
        }

        return previousIndex.get();
    }

    /**
     * Extracts array declarations from a declaration statement.
     * @param declarationStatement  the statement to extract from
     * @return the individual declarations of all arrays in the statement
     *         of the form (identifier = new Type[size],)
     */
    private List<String> getDeclaredArrays(String declarationStatement) {
        List<String> declaredArrays = new ArrayList<>();
        int currentIndex = 0;
        int lastCommaIndex = -1;
        while (currentIndex < declarationStatement.length()) {
            char currentChar = declarationStatement.charAt(currentIndex);
            if (currentChar == '{') {

                // Skip array initializers
                int matchingBraceIndex = findMatchingBrace(declarationStatement, currentIndex);
                currentIndex = matchingBraceIndex == -1 ? declarationStatement.length() - 1 : matchingBraceIndex;

            } else if (currentChar == ',') {
                declaredArrays.add(declarationStatement.substring(lastCommaIndex + 1, currentIndex + 1));
                lastCommaIndex = currentIndex;
                currentIndex++;
            } else {
                currentIndex++;
            }
        }

        declaredArrays.add(declarationStatement.substring(lastCommaIndex + 1));

        return declaredArrays;
    }

}

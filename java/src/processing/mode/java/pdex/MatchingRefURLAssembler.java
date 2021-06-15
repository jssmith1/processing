package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates URLs for MatchingRef errors based on the AST.
 * @author soir20
 */
public class MatchingRefURLAssembler {

    /**
     * Gets the MatchingRef URL for a missing array dimension.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getArrDimURL(ASTNode problemNode) {
        String arrType = problemNode.toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent().getParent())
                .getName().toString();

        return "incorrectdimensionexpression1?typename=" + arrType;
    }

    /**
     * Gets the MatchingRef URL when the first of two array dimensions is missing.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getTwoDimArrURL(ASTNode problemNode) {
        String arrType = ((ArrayCreation) problemNode.getParent()).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent())
                .getName().toString();

        return "incorrectdimensionexpression2?typename=" + arrType;
    }

    /**
     * Gets the MatchingRef URL for the use of two array initializers at once.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getTwoInitializerArrURL(ASTNode problemNode) {
        String arrType = ((ArrayCreation) problemNode.getParent()).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) problemNode.getParent().getParent())
                .getName().toString();

        return "incorrectdimensionexpression3?typename=" + arrType;
    }

    /**
     * Gets the MatchingRef URL for a missing method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getMissingMethodURL(ASTNode problemNode) {
        MethodInvocation invoc = (MethodInvocation) problemNode.getParent();
        List<String> providedParams = ((List<?>) invoc.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invoc.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        String methodName = invoc.getName().toString();

        /* We don't know the desired return type, so use a
           familiar one like "int" instead of one like "void." */
        String dummyReturnType = "int";
        String dummyCorrectName = "correctName";

        return "methodnotfound?methodname=" + methodName
                + "&correctmethodname=" + dummyCorrectName
                + "&typename=" + dummyReturnType;
    }

    /**
     * Gets the MatchingRef URL for a parameter mismatch in a method call.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getParamMismatchURL(ASTNode problemNode) {
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

        return "parametermismatch?methodname=" + methodName
                + "&methodtypename=" + methodReturnType
                + "&typeonename=int&typetwoname=String";
    }

    /**
     * Gets the MatchingRef URL for a missing return statement in a method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getMissingReturnURL(ASTNode problemNode) {
        MethodDeclaration invoc = (MethodDeclaration) problemNode.getParent();
        List<String> requiredParamTypes = Arrays.stream(
                invoc.resolveBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());
        String methodName = invoc.getName().toString();
        String methodReturnType = invoc.getReturnType2().toString();

        return "returnmissing?methodname=" + methodName
                + "&typename=" + methodReturnType;
    }

    /**
     * Gets the MatchingRef URL for a mismatch between a variable's type and its assigned value.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getTypeMismatchURL(String providedType, String requiredType, ASTNode problemNode) {
        String varName = ((VariableDeclarationFragment) problemNode.getParent()).getName().toString();

        return "typemismatch?typeonename=" + providedType
                + "&typetwoname=" + requiredType
                + "&varname=" + varName;
    }

    /**
     * Gets the MatchingRef URL for a missing type.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public String getMissingTypeURL(String missingType, ASTNode problemNode) {

        // All variables in the statement will be the same type, so use the first as an example
        VariableDeclarationStatement varStatement = (VariableDeclarationStatement) problemNode.getParent().getParent();
        VariableDeclarationFragment firstVar = (VariableDeclarationFragment) varStatement.fragments().get(0);

        String varName = firstVar.getName().toString();
        String dummyCorrectName = "CorrectName";

        return "typenotfound?classname=" + missingType
                + "&correctclassname=" + dummyCorrectName
                + "&varname=" + varName;
    }

}
